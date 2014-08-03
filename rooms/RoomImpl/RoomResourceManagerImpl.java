// -------------------------------
// adapated from Kevin T. Manley
// CSE 593
//

package RoomImpl;

import RoomInterface.*;
import LockManager.*;
import TManager.*;

import java.net.Socket;
import java.net.ServerSocket;
import java.lang.ClassNotFoundException;
import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class RoomResourceManagerImpl extends Thread
    implements RoomResourceManager {
    
	static Registry registry;
    protected RMHashtable m_itemHT = new RMHashtable();
    
    // storage map, Integer = txnID
  	private Map<Integer, RMHashtable> storageMap = new HashMap<Integer, RMHashtable>();
  	
    private static int port;
    private static boolean tcp = false;
    private static String mode;
    
    private Socket socket;
	ObjectInputStream in = null;
	ObjectOutputStream out = null; 
	
	private LockManager lm = new LockManager();
	private BufferedWriter log;
	
	private File master = new File("masterRoom.txt");
	private File versionA = new File("aRoom.dat");
	private File versionB = new File("bRoom.dat");
	private File storage = new File("storageRoom.dat");
	private File fileLog = new File("RoomLog.txt");
	
	private Map<Integer, Session> sessionMap = new HashMap<Integer, Session>();
	private Session txnSession;
	private boolean aVersion = false;
	private String version = null;
	private TManager tm;
	
    public static void main(String[] args) throws IOException, TransactionAbortedExceptionRoom,
			InvalidTransactionExceptionRoom {
    	
    	String server = "localhost";
		if (args.length == 2) {
			port = Integer.parseInt(args[1]);
			if (args[0].equals("TCP"))
				tcp = true;
		}  
		
		else {
			System.err.println ("Wrong usage");
			System.out.println("Usage: java ResImpl.ResourceManagerImpl RMI port or "
					+ "java RoomImpl.RoomResourceManagerImpl TCP port");
			System.exit(1);
		}
		RoomResourceManagerImpl obj = null;
		if (tcp) {
			ServerSocket serverSocket = null;
			boolean listening = true;
			mode = "TCP";
			
			try { 
				serverSocket = new ServerSocket(port);
				System.out.println("Room server ready");
			} catch (IOException e) {
				System.err.println("Could not listen on port");
				e.printStackTrace();
				System.exit(1);
			}
							
			while (listening) {
				new RoomResourceManagerImpl(serverSocket.accept()).start();
				System.out.println("Room client arrived");
			}
			serverSocket.close();
		}
		else {
			mode = "RM";
			try {
		    // create a new Server object
				obj = new RoomResourceManagerImpl(null);
		    // dynamically generate the stub (client proxy)
				RoomResourceManager rm = (RoomResourceManager) UnicastRemoteObject.exportObject(obj, 0);
		    
		    // Bind the remote object's stub in the registry
				registry = LocateRegistry.getRegistry(port);
				registry.rebind("group11Room", rm);
		    
				System.err.println("Server ready");
			} 
			catch (Exception e) { 
				System.err.println("RoomServer exception: " + e.toString());
				e.printStackTrace();
			}
	 
			// Create and install a security manager
			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new RMISecurityManager());
			}
		}
		try {
			if (obj != null) {
				if (!obj.restoreTable() || !obj.recover())
					return;
			}
			else
				return;
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
    }	
  
 // restore the main table m_itemHT
    private synchronized boolean restoreTable() throws IOException, ClassNotFoundException {
    	// read master record file to get the last commited state
    	if (!master.exists()) {
    		if (!master.createNewFile()) {
    			throw new IOException("Error creating new file: " + master.getName());
    		}
    		if (!versionA.exists()) {
	    		if (!versionA.createNewFile()){
	    	   		throw new IOException("Error creating new file: " + versionA.getName());
	    		}
	    	}
    		if (!versionB.exists()) {
	    		if (!versionB.createNewFile()){
	    	   		throw new IOException("Error creating new file: " + versionB.getName());
	    		}
	    	}
    	}
    	else {
    		// open master and read the last version name
    		BufferedReader br = null;
    		try {
	    	    br = new BufferedReader(new FileReader(master));
                String line = null;
                if ((line = br.readLine()) != null) {
                	version = line;
                }
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	} finally {
	    		try {
	    			if (br != null)
	    				br.close();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    	}
    		if (version != null) {
		    	if (version.equals("A")) {
			    	aVersion = true;
			    	if (!versionA.exists()) {
			    		if (!versionA.createNewFile()){
			    	   		throw new IOException("Error creating new file: " + versionA.getName());
			    		}
			    	}
				    else {
				    	// recover from A
				    	 ObjectInputStream in = new ObjectInputStream(new FileInputStream(versionA));
				    	 m_itemHT = (RMHashtable)in.readObject();
				    }
		    	}
		    	else {
			    	if (!versionB.exists()) {
			    		if (!versionB.createNewFile()) {
			    			throw new IOException("Error creating new file: " + versionB.getName());
			    		}
			    	}
				    else {
				    	// recover from B
				    	 ObjectInputStream in = new ObjectInputStream(new FileInputStream(versionB));
				    	 m_itemHT = (RMHashtable)in.readObject();
				    }
		    	}
    	    	// check if another copy exists and if not create it
		    	if (aVersion) {
		    		if (!versionB.exists() && !versionB.createNewFile()){
		    			throw new IOException("Error creating new file: " + versionA.getName());
		    		}		
		    	}
		    	else {
		    		if (!versionA.exists() && !versionA.createNewFile()){
		    			throw new IOException("Error creating new file: " + versionA.getName());
		    		}
		    	}
    		}
    		else { // master is empty
    			if (!versionA.exists()) {
    	    		if (!versionA.createNewFile()){
    	    	   		throw new IOException("Error creating new file: " + versionA.getName());
    	    		}
    	    	}
        		if (!versionB.exists()) {
    	    		if (!versionB.createNewFile()){
    	    	   		throw new IOException("Error creating new file: " + versionB.getName());
    	    		}
    	    	}
    		}
    	} // end of file exists
    	return true;
    }
    // recover the local table (storageMap)
    private synchronized boolean recoverStorage() {
    	if (version != null) {
    		try {
    			ObjectInputStream in = new ObjectInputStream(new FileInputStream(storage));
    			
    			Object obj = null;

    		    if ((obj = in.readObject()) != null) 
    		    	storageMap = (HashMap<Integer, RMHashtable>)obj;
    		    in.close();
    		    // iterate through the storageMap and add timer to each transaction
    		    Iterator it = storageMap.entrySet().iterator();
    		    while (it.hasNext()) {
    		        Map.Entry<Integer, RMHashtable> pairs = (Map.Entry<Integer, RMHashtable>) it.next();
    		        txnSession = new Session(this, pairs.getKey());
    		        sessionMap.put(pairs.getKey(), txnSession);
    		        txnSession.schedule();
    		    }
    		}
    		catch (IOException e) {
    			System.out.println("Can't read storage file");
    		}
    		catch (ClassNotFoundException e) {
    			System.out.println("Class not found when read storage file");
    		}
    		return true;
    	}
    	else
    		return false;
    }
  
    // read log and act accordingly
    private boolean recover() throws IOException, TransactionAbortedExceptionRoom, 
			InvalidTransactionExceptionRoom {
    	//prepare writer to write into log
    	
		
	    // map to get info about all transactions written in log
		Map<Integer, String> mapLog = new HashMap<Integer, String>();
		// Read log to know what to do
		
		if (!fileLog.exists() && !fileLog.createNewFile()) {
			throw new IOException("Error creating new file: " + fileLog.getName());
		}
		else {
			FileReader reader = new FileReader(fileLog);
			BufferedReader br = new BufferedReader(reader);
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(",");
				int idTxn = Integer.parseInt(split[0]);
				mapLog.put(idTxn, split[1]);
			}
			br.close();
			// read mapLog to know what to do with each transaction
			Iterator it = mapLog.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry<Integer, String> pairs = (Map.Entry<Integer, String>)it.next();
		        int id = pairs.getKey();
		        String action = pairs.getValue();
		      	if (action.contains("ABORT") || action.contains("COMMIT")) {
		    		//do nothing, storage should not contain that transaction
		    	}
		    	else if (action.contains("YES")) {
		    		// get an object of TM to call its method to ask what to do
		    		try {
		    			Registry registry = LocateRegistry.getRegistry("teaching.cs.mcgill.ca", 2011);
		    			// get the proxy and the remote reference by rmiregistry lookup
		    			tm = (TManager) registry.lookup("group11TM");
		    			if(tm != null) {
		    				System.out.println("Successful");
		    				System.out.println("Connected to TM");
		    				if (tm.requestAction(id)) {
				    			recoverStorage();
				    			commitTxn(id);
				    			tm.commitDone(id, 2);
				    		}
				    		else {
				    			recoverStorage();
				    			if (storageMap.containsKey(id)) 
				    				abortTxn(id);
				    		}
		    	 		}
		    			else {
		    				System.out.println("Unsuccessful");
		    	 		}
		    	 	} 
		    		catch (Exception e) {	
		    			System.err.println("TM exception: " + e.toString());
		    			e.printStackTrace();
		    	 	}
		    	}
		    	else {
		    		// should abort and send vote-no, but if the server crashed, then
		    		// everything (lock manager, storage, etc) were cleared.
		    		// TM will abort on the timeout if I do not send a vote
		    		// So, do I have to do anything?
		    		// One case - I wanted to send yes, saved storage and crashed.
		    		// Then recover storage and remove the id if exists.
		    		recoverStorage();
		    		if (storageMap.containsKey(id)) 
		    			abortTxn(id);
		    	}
		    }
		}
	
		return true;
    }
    
    private synchronized void writeStorage() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(storage));
			out.writeObject(storageMap);
			out.close();
		}
		catch (IOException e) {
			System.out.println("Can't write to storage file");
		}
	}
	
	private synchronized boolean writeTable() {
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter("tempRoom.txt"));
			ObjectOutputStream out;
			if (aVersion) {
				out = new ObjectOutputStream(new FileOutputStream(versionB));
				br.write("B");
			}
			else {
				out = new ObjectOutputStream(new FileOutputStream(versionA));
				br.write("A");
			}
			out.writeObject(m_itemHT);
			out.close();
			br.close();
		}
		catch (IOException e) {
			System.out.println("Can't write to version file");
		}
		return true;
	}
   
	
	public void startT(int id) throws RemoteException {
		RMHashtable storage = new RMHashtable();
		storageMap.put(id, storage);
		txnSession = new Session(this, id);
		sessionMap.put(id, txnSession);
		txnSession.schedule();
	}
	
	//If an exception was thrown for an action the local storage table would be wiped clean
	//if it is then the RM sends a No to the VOTE-REQ
	public boolean vote(int transactionId) throws RemoteException, 
		TransactionAbortedExceptionRoom, InvalidTransactionExceptionRoom  {
		RMHashtable storage = storageMap.get(transactionId);
		if (storage != null) {
			try {
				FileWriter fileWriter = new FileWriter(fileLog, true);
				log = new BufferedWriter(fileWriter);
				log.write("YES" + transactionId + "\n");
				log.close();
				// stop timer when sending YES
				sessionMap.get(transactionId).cancel();
				sessionMap.remove(transactionId);
				writeStorage();
			}
			catch (IOException e) {
				System.out.println("Can't write to log");
			}
			return true;
		} 
		else {
			return false;
		}
	}
	
	public void selfDestruct() throws RemoteException {
		System.exit(0);
	}
	
	public boolean commitTxn(int id) throws RemoteException, 
			TransactionAbortedExceptionRoom, InvalidTransactionExceptionRoom {
		RMHashtable storage = storageMap.get(id);
		if (storage != null) {
			if (sessionMap != null && sessionMap.containsKey(id)) {
				sessionMap.get(id).cancel();
				sessionMap.remove(id);
			}
			Iterator<Map.Entry<String, RMItem>> it = storage.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, RMItem> entry = it.next();
				String key = entry.getKey();
				if (m_itemHT.containsKey(key)) {
					if (!entry.getValue().toDelete())
						// update value;
						m_itemHT.put(key, entry.getValue());
					else {
						// if value is toDelete, then remove that item
						m_itemHT.remove(key);
					}
				}
				// new item has been added
				else {
					m_itemHT.put(key, entry.getValue());
				}
			}
			if (writeTable()) {
				// try to make atomic write to the master file
				/*Path original = Paths.get("temp.txt");
				Path destination = Paths.get("master.txt");
				try {
					Files.move(original, destination, StandardCopyOption.ATOMIC_MOVE);
				} catch (IOException x) {
					//catch all for IO problems
				}*/
				File file = new File("tempRoom.txt");
				if (file.renameTo(new File("masterRoom.txt"))) {
					System.out.println("The file was moved successfully to the new folder");
				} 
				else {
					System.out.println("Can't move file to master.");
				}
			}
			
			storageMap.remove(id);
			lm.unlockAll(id);
			try {
				FileWriter fileWriter = new FileWriter(fileLog, true);
				log = new BufferedWriter(fileWriter);
				log.write(id + ",COMMIT\n");
				log.close();
			}
			catch (IOException e) {
				System.out.println("Can't write to log");
			}
		}
		else {
			throw new InvalidTransactionExceptionRoom(id);
		}
		return true; 
	}
		
	public void abortTxn(int id) throws RemoteException, InvalidTransactionExceptionRoom {
		if (storageMap.containsKey(id)) {
			sessionMap.get(id).cancel();
			sessionMap.remove(id);
			storageMap.remove(id);
			// update storage file
			writeStorage();
			lm.unlockAll(id);
			try {
				FileWriter fileWriter = new FileWriter(fileLog, true);
				log = new BufferedWriter(fileWriter);
				log.write(id + ",ABORT\n");
				log.close();
			}
			catch (IOException e) {
				System.out.println("Room. Can't write to log " + id);
			}
		}
		else {
			throw new InvalidTransactionExceptionRoom(id);
		}
	} 


  
	// Reads a data item
	private RMItem readData( int id, String key ) 
				throws InvalidTransactionExceptionRoom, 
				TransactionAbortedExceptionRoom, DeadlockException {
			RMItem item = null;
			RMHashtable storage = storageMap.get(id);
			if (storage != null) {
				sessionMap.get(id).reschedule();
				// if it is in the local table, then it is either new or already locked
				if (storage.containsKey(key)) {
					item = (RMItem)storage.get(key);
					if (item.toDelete()) {
						return null;
					}
					// if exists in the local table with toDelete flag, 
					// then it has been marked for deletion, return null
				}
				else {
					lm.lock(id, key, LockManager.READ);
					item = (RMItem)m_itemHT.get(key);
				}
			}
			else {
				throw new InvalidTransactionExceptionRoom(id);
			}
			// return a deep copy of the object
			if (item != null) {
				Hotel f = Hotel.copy((Hotel)item);
				return f;
			}
			else
				return null;
		}

		
		// Writes a data item
		private void writeData( int id, String key, RMItem value ) 
				throws InvalidTransactionExceptionRoom, 
				TransactionAbortedExceptionRoom, DeadlockException {
			// until commit is called, write everything to the storageMap
			RMHashtable storage = storageMap.get(id);
			if (storage != null) {
				sessionMap.get(id).reschedule();
				// lock even if write into the local table
				lm.lock(id, key, LockManager.WRITE);
				// already read before calling writeData, 
				// so know that the customer not null and exists either in 
				// the local or in the global table
				storage.put(key, value);
			
			}
			else {
				throw new InvalidTransactionExceptionRoom(id);
			}
		}
		
		
		// Remove the item out of storage
		protected RMItem removeData(int id, String key) 
				throws InvalidTransactionExceptionRoom, 
				TransactionAbortedExceptionRoom, DeadlockException {
			RMItem item = null;
			RMHashtable storage = storageMap.get(id);
			// tnx exists in the map
			if (storage != null) {
				sessionMap.get(id).reschedule();
				// update item in the local table
				// if already in the local table, then it must be locked
				if (storage.containsKey(key)) {
					if (storage.get(key) != null)
						// exists in the local table and is not marked for deletion
						item = (RMItem)storage.remove(key);
				}
				// item with that key doesn't exist in the local table, 
				// write it into the table as null to remove when commit
				else {
					lm.lock(id, key, LockManager.WRITE);
					item = (RMItem)m_itemHT.get(key);
					if (item != null) {
						item.setDelete();
						storage.put(key, item);
					}
					
				}
			}
			else {
				throw new InvalidTransactionExceptionRoom(id);
			}
			return item;
		}
    // deletes the entire item
    protected boolean deleteItem(int id, String key) 
    		throws TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException {
    	Trace.info(mode + "::deleteItem(" + id + ", " + key + ") called" );
    	ReservableItem curObj = (ReservableItem) readData( id, key );
    	// Check if there is such an item in the storage
    	if( curObj == null ) {
    		Trace.warn(mode + "::deleteItem(" + id + ", " + key + ") failed--item doesn't exist" );
    		return false;
    	} 
    	else {
    		if(curObj.getReserved()==0){
    			lm.lock(id, key, LockManager.WRITE);
    			removeData(id, curObj.getKey());
    			Trace.info(mode + "::deleteItem(" + id + ", " + key + ") item deleted" );
    			return true;
    			
    		}
    		else{
    			Trace.info(mode + "::deleteItem(" + id + ", " + key + ") item can't be deleted because some customers reserved it" );
    			return false;
    		}
    	} 
    }
	

    // query the number of available seats/rooms/Rooms
    protected int queryNum(int id, String key)  
    		throws TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException {
		Trace.info(mode + "::queryNum(" + id + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData( id, key);
		int value = 0;  
		if( curObj != null ) {
		    value = curObj.getCount();
		} // else
		Trace.info(mode + "::queryNum(" + id + ", " + key + ") returns count=" + value);
		return value;
    }	
	
    // query the price of an item
    protected int queryPrice(int id, String key) 
    		throws TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException {
		Trace.info(mode + "::queryRoomPrice(" + id + ", " + key + ") called" );
		ReservableItem curObj = (ReservableItem) readData( id, key);
		int value = 0; 
		if( curObj != null ) {
		    value = curObj.getPrice();
		} 
		Trace.info(mode + "::queryRoomPrice(" + id + ", " + key + ") returns cost=$" + value );
		return value;		
    }
    
    // reserve an item
    protected int reserveItem(int id, String key, String location)  
    		throws TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException {
		// check if the item is available
		ReservableItem item = (ReservableItem)readData(id, key);
		if(item == null){
			return -1;
		}
		else if(item.getCount()==0){
			return -2;
		}
		else {			
			// decrease the number of available items in the storage
			lm.lock(id, key, LockManager.WRITE);
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			RMHashtable storage = storageMap.get(id);
			// update local table with the new state of the object
			storage.put(key, item);
			
		}
	    return item.getPrice();
    }
   
    // Create a new Room, or add seats to existing Room
    //  NOTE: if RoomPrice <= 0 and the Room already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
    		throws RemoteException, TransactionAbortedExceptionRoom,
    		InvalidTransactionExceptionRoom, DeadlockException {
    	
		Trace.info(mode + "::addRoom(" + id + ", " + location + ", $" + price + ", " + count + ") called" );
		
		Hotel curObj = (Hotel) readData( id, Hotel.getKey(location) );
		if( curObj == null ) {
			// doesn't exist...add it
			curObj = new Hotel( location, count, price );
			writeData( id, curObj.getKey(), curObj );
			Trace.info(mode + "::addRoom(" + id + ") created new Room " + location + ", seats=" +
					count + ", price=$" + price );
		} else {
			
			// add seats to existing Room and update the price...
			lm.lock(id, curObj.getKey(), LockManager.WRITE);
			curObj.setCount( curObj.getCount() + count );
			if( price > 0 ) {
				curObj.setPrice( price );
			} 
			writeData( id, curObj.getKey(), curObj );
			
		}
		   
		Trace.info(mode + "::addRoom(" + id + ") modified existing Room " + location + ", seats=" + curObj.getCount() + ", price=$" + price );
		
		return true;
    }
    
       
    public boolean deleteRooms(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException {
    	return deleteItem(id, Hotel.getKey(location));
    }

    // Returns the number of empty seats on this Room
    public int queryRooms(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException {
    	return queryNum(id, Hotel.getKey(location));
    }

  	// Returns price of this Room
    public int queryRoomsPrice(int id, String location )
    		throws RemoteException, TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException
    {
    	return queryPrice(id, Hotel.getKey(location));
    }

	
    // Adds Room reservation to this customer.  
    public int reserveRoom(int id, String location)
    		throws RemoteException, TransactionAbortedExceptionRoom,
    		InvalidTransactionExceptionRoom, DeadlockException {
    	return reserveItem(id, Hotel.getKey(location), String.valueOf(location));
    }
    
    // customerID sent only to print the info
    public boolean cancelReservation(int id, int customerID, String key, int number)
    		throws RemoteException, TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException {
    	
    	ReservableItem item  = (ReservableItem) readData(id, key);
    	
    	Trace.info(mode + "::deleteCustomer(" + id + ", " + customerID + ") has reserved " 
    			+ key + " " +  number +  " times" );
    	
    	lm.lock(id, key, LockManager.WRITE);
    	item.setReserved(item.getReserved() - number);
    	System.out.println("set reserved");
    	item.setCount(item.getCount() + number);
    	RMHashtable storage = storageMap.get(id);
    	// update local table with the new state of the object
    	storage.put(key, item);
    	
		return true;
    }
    public boolean shutdown() throws RemoteException {
    	Trace.info("Shutting down the system");
 	    try {
 		    registry.unbind("group11Room");
 		    UnicastRemoteObject.unexportObject(this,true);  
 	    }
 	    catch (Exception e) {
 	    	
 	    }
 	    
 		return true;
    }
    
    // TCP stuff
    public RoomResourceManagerImpl(Socket socket) throws RemoteException {	
    	if (socket != null && tcp) {
			this.socket = socket;
			try {
				out = new ObjectOutputStream(socket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				System.out.println("Error with streams");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	 
	public void run() {
		while (true) {
			String method = null;
			LinkedList<Object> args = new LinkedList<Object>();
			try {
				method = (String)in.readObject();
				Object[] array = (Object[]) in.readObject(); 
				
				for (int i = 0; i < array.length; i++)
					args.add(array[i]);
				
						
			} catch (ClassNotFoundException e) {
				System.err.println("Error reading objects - class not found");
				e.printStackTrace();
				System.exit(1);
			} catch (EOFException e) {
				System.out.println("Client left");
			} catch (IOException e) {
				System.err.println("Error reading objects");
				
			}
			try {
				// mostly boolean, but can be int in case of reservation
				// so better work with objects
				Object res = processRequests(method, args);
				out.writeObject(res);
			} catch (IOException e) {
				System.err.println();
				e.printStackTrace();
				System.exit(1);
			}
			catch (TransactionAbortedExceptionRoom e) {
				System.err.println();
				e.printStackTrace();
				System.exit(1);
			}
			catch (InvalidTransactionExceptionRoom e) {
				System.err.println();
				e.printStackTrace();
				System.exit(1);
			}
			catch (DeadlockException e) {
				System.err.println();
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	private Object processRequests(String method, LinkedList<Object> args) 
			throws TransactionAbortedExceptionRoom, 
			InvalidTransactionExceptionRoom, DeadlockException {
		try {
			if (method.equals("addRoom")) 
				return (Boolean) addRooms((Integer)args.get(0), (String)args.get(1), (Integer)args.get(2), (Integer)args.get(3));
		
			else if (method.equals("deleteRoom"))
				return (Boolean) deleteRooms((Integer)args.get(0), (String)args.get(1));
			
			else if (method.equals("queryRoom"))
				return (Integer) queryRooms((Integer) args.get(0), (String)args.get(1));
			
			else if (method.equals("queryRoomPrice"))
				return (Integer) queryRoomsPrice((Integer) args.get(0), (String)args.get(1));
			
			else if (method.equals("reserveRoom"))
				return (Integer) reserveRoom((Integer) args.get(0), (String)args.get(1));
			
			else if (method.equals("cancelReservation"))
				return (Boolean) cancelReservation((Integer) args.get(0), (Integer) args.get(1), (String) args.get(2), (Integer) args.get(3));
			
			else {
				
			}
		
		}
		catch (RemoteException e) {
			System.err.println("remote exception");
			e.printStackTrace();
		}
		return false;
	}
	
}
