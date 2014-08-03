package MiddleImpl;

import LockManager.*;
import ResInterface.*;
import FlightInterface.*;
import CarInterface.*;
import RoomInterface.*;
import TManager.*;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MiddlewareImpl extends Thread implements ResourceManager {
	
	static Registry registry;
	protected RMHashtable m_itemHT = new RMHashtable();
	//Olga: storage map, Integer = txnID
	private Map<Integer, RMHashtable> storageMap = new HashMap<Integer, RMHashtable>();
	
	static FlightResourceManager rmFlight = null;
	static CarResourceManager rmCar = null;
	static RoomResourceManager rmRoom = null;
		
	static FlightTCP tcpFlight = null;
	static CarTCP tcpCar = null;
	static RoomTCP tcpRoom = null;
	
	//!!!!!
	static TransactionManager txnM = null;
	 
	private Socket socket;
	private ObjectInputStream in = null;
	private ObjectOutputStream out = null; 
	
	private static boolean tcp = false;
	private static String mode = null;
	
	private LockManager lm = new LockManager();
	private BufferedWriter log;
	
	private File master = new File("masterCustomer.txt");
	private File versionA = new File("aCustomer.dat");
	private File versionB = new File("bCustomer.dat");
	private File storage = new File("storageCustomer.dat");
	private File fileLog = new File("CustomerLog.txt");

	private Map<Integer, Session> sessionMap = new HashMap<Integer, Session>();
	private Session txnSession;
	private boolean aVersion = false;
	private String version = null;
	private TManager tm;
	
	public static void main(String[] args) throws IOException, TransactionAbortedException,
			InvalidTransactionException {
       
		// each RM runs on different servers
		String[] server = new String[3];
		
		
		if (args.length == 4) {
			server[0] = args[1] + ".cs.mcgill.ca"; // flight
			server[1] = args[2] + ".cs.mcgill.ca"; // car
			server[2] = args[3] + ".cs.mcgill.ca"; // room
			if (args[0].equals("TCP"))
				tcp = true;
		} 
			
		else {
			System.err.println ("Wrong usage");
			System.out.println("Usage: java -Djava.security.policy="
					+ "file:$HOME/comp512/middleware/java.policy "
					+ "-Djava.rmi.server.codebase=file:$HOME/comp512/middleware/"
					+ " MiddleImpl.MiddlewareImpl RMI server_flight server_car server_room or "
					+ "java MiddleImpl.MiddlewareImpl TCP server_flight server_car server_room");
			System.exit(1);
	    }
		MiddlewareImpl obj = null;
		if (tcp) {
			mode = "TCP";
			tcpFlight = new FlightTCP(server[0], 3011);
			if (tcpFlight != null)
				System.out.println("Connected to TCP Flight");
			tcpCar = new CarTCP(server[1], 3011);
			if (tcpCar != null)
				System.out.println("Connected to TCP Car");
			tcpRoom = new RoomTCP(server[2], 3011);
			if (tcpRoom != null)
				System.out.println("Connected to TCP Room");
			
			
			// create server
			ServerSocket serverSocket = null;
			boolean listening = true;
			
			try { 
				serverSocket = new ServerSocket(3011);
				System.out.println("Server ready");
				
			} catch (IOException e) {
				System.err.println("Could not listen on port");
				e.printStackTrace();
				System.exit(1);
			}
							
			while (listening) {
				new MiddlewareImpl(serverSocket.accept()).start();
				System.out.println("Client arrived");
			}
			serverSocket.close();
		}
		// RMI
		else {
			mode = "RM";
			
			try {
				txnM = new TransactionManager();
				TManager tm = (TManager) UnicastRemoteObject.exportObject(txnM, 0);
				// create a new Server object
				obj = new MiddlewareImpl();
				// dynamically generate the stub (client proxy)
				ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);
		 
				// Bind the remote object's stub in the registry
				registry = LocateRegistry.getRegistry(2011);
				registry.rebind("group11", rm);
				registry.rebind("group11TM", tm);
				
				System.err.println("Server ready");
			} 
			catch (Exception e) {
				System.err.println("Server exception: " + e.toString());
				e.printStackTrace();
			}
		
			/* create new Client objects */
			try {
				// get a reference to the rmiregistry
				Registry registryFlight = LocateRegistry.getRegistry(server[0], 2011);
				Registry registryCar = LocateRegistry.getRegistry(server[1], 2011);
				Registry registryRoom = LocateRegistry.getRegistry(server[2], 2011);
			   
				// get the proxy and the remote reference by rmiregistry lookup
				rmFlight = (FlightResourceManager) registryFlight.lookup("group11Flight");
				rmCar = (CarResourceManager) registryCar.lookup("group11Car");
				rmRoom = (RoomResourceManager) registryRoom.lookup("group11Room");
			
				if(rmFlight != null) {
					System.out.println("Connected to RM Flight");
				}
				else {
					System.out.println("RM Flight Unsuccessful");
				}
				if(rmCar != null) {
					System.out.println("Connected to RM Car");
				}
				else {
					System.out.println("RM Car Unsuccessful");
				}
				if (rmRoom != null) {
					System.out.println("Connected to RM Room");
				}
				else {
					System.out.println("RM Room Unsuccessful");
				}
			} 
			catch (Exception e) { 
				System.err.println("Middleware as a client exception: " + e.toString());
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
    private boolean recover() throws IOException, TransactionAbortedException, 
    		InvalidTransactionException {
    	
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
		    		if (txnM.requestAction(id)) {
		    			recoverStorage();
		    			commitTxn(id);
		    			txnM.commitDone(id, 3);
		    		}
		    		else {
		    			recoverStorage();
		    			if (storageMap.containsKey(id)) 
		    				abortTxn(id);
		    		}
		    	}
		    	else {
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
			BufferedWriter br = new BufferedWriter(new FileWriter("tempCustomer.txt"));
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
	
	
	public MiddlewareImpl() 
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {}
	
	// gets the id from TM, initializes a new storage table
	public int startTxn() throws RemoteException {
		int id = txnM.startTxn();	
		return id;
	}
	
	// uses as a RM
	public void startT(int id) throws RemoteException {
		RMHashtable storage = new RMHashtable();
		storageMap.put(id, storage);
		txnSession = new Session(this, id);
		sessionMap.put(id, txnSession);
		txnSession.schedule();
		System.out.println("started");
	}
	
	//If an exception was thrown for an action the local storage table would be wiped clean
	//if it is then the RM sends a No to the VOTE-REQ
	public boolean vote(int transactionId) throws RemoteException, 
			TransactionAbortedException, InvalidTransactionException {
		RMHashtable storage = storageMap.get(transactionId);
		if (storage != null) {
			try {
				FileWriter fileWriter = new FileWriter(fileLog, true);
				log = new BufferedWriter(fileWriter);
				log.write(transactionId + ",YES\n");
				log.close();
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
	
	// MW has 2 methods commit and commitTxn. It uses commit as a MW and commitTxn as a RM
	public boolean commit(int transactionId) 
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		try {
			txnM.prepare(transactionId);
		}
		catch (RemoteException e) {
			Trace.warn("Remote exception in commit MW");
		}
		return false;
	}
	
	// MW has 2 methods abort and abortTxn. It uses abort as a MW and abortTxn as a RM
	public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
		txnM.abort(transactionId);
	}
	
	public boolean commitTxn(int id) throws RemoteException, 
		TransactionAbortedException, InvalidTransactionException {
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
				File file = new File("tempCustomer.txt");
				if (file.renameTo(new File("masterCustomer.txt"))) {
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
			throw new InvalidTransactionException(id);
		}
		return true; 
	}
		
	public void abortTxn(int id) throws RemoteException, InvalidTransactionException {
		if (storageMap.containsKey(id)) {
			if (sessionMap != null && sessionMap.get(id) != null) {
				sessionMap.get(id).cancel();
				sessionMap.remove(id);
			}
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
				System.out.println("Customer. Can't write to log " + id);
			}
		}
		else {
			throw new InvalidTransactionException(id);
		}
	} 
	
	// Reads a data item
	private RMItem readData( int id, String key ) 
			throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		RMItem item = null;
		RMHashtable storage = storageMap.get(id);
		if (storage != null) {
			// if it is in the local table, then it is either new or already locked
			if (storage.containsKey(key)) {
				item = (RMItem)storage.get(key);
				if (item.toDelete())
					return null;
				// if exists in the local table with toDelete flag, 
				// then it has been marked for deletion, return null
			}
			else {
				try {
					lm.lock(id, key, LockManager.READ);
					item = (RMItem)m_itemHT.get(key);
				}
				catch (DeadlockException e) {
					txnM.abort(id);
					throw new TransactionAbortedException(id);
				}
			}
		}
		else {
			throw new InvalidTransactionException(id);
		}
		// return a deep copy of the object
		if (item != null) {
			Customer cust = Customer.copy((Customer)item);
			return cust;
		}
		else
			return null;
	}

	
	// Writes a data item
	private void writeData( int id, String key, RMItem value ) 
			throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		// until commit is called, write everything to the storageMap
		RMHashtable storage = storageMap.get(id);
		if (storage != null) {
			try {
				// lock even if write into the local table
				lm.lock(id, key, LockManager.WRITE);
				// already read before calling writeData, 
				// so know that the customer not null and exists either in 
				// the local or in the global table
				storage.put(key, value);
			}
			catch (DeadlockException e) {
				txnM.abort(id);
				throw new TransactionAbortedException(id);
			}
		}
		else {
			throw new InvalidTransactionException(id);
		}
	}
	
	
	// Remove the item out of storage
	protected RMItem removeData(int id, String key) 
			throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		RMItem item = null;
		RMHashtable storage = storageMap.get(id);
		// tnx exists in the map
		if (storage != null) {
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
				try {
					lm.lock(id, key, LockManager.WRITE);
					item = (RMItem)m_itemHT.get(key);
					if (item != null) {
						item.setDelete();
						storage.put(key, item);
					}
				}
				catch (DeadlockException e) {
					txnM.abort(id);
					throw new TransactionAbortedException(id);
				}
			}
		}
		else {
			throw new InvalidTransactionException(id);
		}
		return item;
	}
	
	
	protected Customer existsCustomer(int id, int customerID, String message) 
			throws RemoteException, TransactionAbortedException, InvalidTransactionException{
		// Read customer object if it exists (and read lock it)
		txnM.setRMCustomer(this);
		txnM.enlist(id, 3);
		Customer cust = (Customer) readData(id, Customer.getKey(customerID) );	
		if( cust == null ) {
			Trace.warn(message + " --customer doesn't exist");
		} 
		return cust;
	}
	
	
	// reserve an item
	protected void reserveItem(int id, Customer cust, String key, String location, int price)
			throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		// Does lock manager checks if the lock was already given to the transaction?
		// because it will ask for the lock again in writeData
		try {
			lm.lock(id, key, LockManager.WRITE); 
			cust.reserve( key, location, price);
			writeData( id, cust.getKey(), cust );
		}
		catch (DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
		Trace.info(mode + "::reserveItem( " + id + ", " + cust.getID() + ", " + key + ", " +location+") succeeded" );
	}
	
	
	// Create a new flight, or add seats to existing flight
	//  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 0);
		try {
			if (tcp) 
				return tcpFlight.addFlight(id, flightNum, flightSeats, flightPrice);
			else 
				return rmFlight.addFlight(id, flightNum, flightSeats, flightPrice);
		}
		catch ( TransactionAbortedExceptionFlight e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionFlight e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}

	public boolean deleteFlight(int id, int flightNum) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 0);
		try {	
			if (tcp)
				return tcpFlight.deleteFlight(id, flightNum);
			else			
				return rmFlight.deleteFlight(id, flightNum);
		}
		catch ( TransactionAbortedExceptionFlight e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionFlight e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}

	public boolean addRooms(int id, String location, int count, int price)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 2);
		try {
			if (tcp)
				return tcpRoom.addRooms(id, location, count, price);
			else
				return rmRoom.addRooms(id, location, count, price);
		}
		catch ( TransactionAbortedExceptionRoom e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionRoom e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}

	// Delete rooms from a location
	public boolean deleteRooms(int id, String location)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 2);
		try {
			if (tcp)
				return tcpRoom.deleteRooms(id, location);
			else
				return rmRoom.deleteRooms(id, location);
		}
		catch ( TransactionAbortedExceptionRoom e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionRoom e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}

	// Create a new car location or add cars to an existing location
	//  NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int id, String location, int count, int price)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 1);
		try {
			if (tcp)
				return tcpCar.addCars(id, location, count, price);
			else
				return rmCar.addCars(id, location, count, price);
		}
		catch ( TransactionAbortedExceptionCar e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionCar e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}

	// Delete cars from a location
	public boolean deleteCars(int id, String location) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 1);
		try {
			if (tcp)
				return tcpCar.deleteCars(id, location);
			else
				return rmCar.deleteCars(id, location);
		}
		catch ( TransactionAbortedExceptionCar e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionCar e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}

	// Returns the number of empty seats on this flight
	public int queryFlight(int id, int flightNum) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 0);
		try {
			if (tcp)
				return tcpFlight.queryFlight(id, flightNum);
			else
				return rmFlight.queryFlight(id, flightNum);
		}
		catch ( TransactionAbortedExceptionFlight e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionFlight e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}

	// Returns price of this flight
	public int queryFlightPrice(int id, int flightNum )	
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 0);
		try {
			if (tcp)
				return tcpFlight.queryFlightPrice(id, flightNum);
			else
				return rmFlight.queryFlightPrice(id, flightNum);
		}
		catch ( TransactionAbortedExceptionFlight e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionFlight e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}

	// Returns the number of rooms available at a location
	public int queryRooms(int id, String location) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 2);
		try {
			if (tcp)
				return tcpRoom.queryRooms(id, location);
			else
				return rmRoom.queryRooms(id, location);
		}
		catch ( TransactionAbortedExceptionRoom e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionRoom e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}


	// Returns room price at this location
	public int queryRoomsPrice(int id, String location) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 2);
		try {
			if (tcp)
				return tcpRoom.queryRoomsPrice(id, location);
			else
				return rmRoom.queryRoomsPrice(id, location);
		}
		catch ( TransactionAbortedExceptionRoom e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionRoom e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}


	// Returns the number of cars available at a location
	public int queryCars(int id, String location) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 1);
		try {
			if (tcp)
				return tcpCar.queryCars(id, location);
			else
				return rmCar.queryCars(id, location);
		}
		catch ( TransactionAbortedExceptionCar e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionCar e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}


	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		txnM.enlist(id, 1);
		try {
			if (tcp)
				return tcpCar.queryCarsPrice(id, location);
			else
				return rmCar.queryCarsPrice(id, location);
		}
		catch ( TransactionAbortedExceptionCar e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionCar e) {
			throw new InvalidTransactionException(id);
		}
		catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
	}
	
	
	

	// Returns data structure containing customer reservation info. Returns null if the
	//  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
	//  reservations.
	public RMHashtable getCustomerReservations(int id, int customerID)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		Trace.info(mode + "::getCustomerReservations(" + id + ", " + customerID + ") called" );
		String message = mode + "::getCustomerReservations (" + id + ", " + customerID + ")";
		txnM.setRMCustomer(this);
		txnM.enlist(id, 3);
		Customer cust = existsCustomer(id, customerID, message);
		if( cust == null ) {
			return null;
		} 
		else {
			return cust.getReservations();
		} 
		
	}

	// return a bill
	public String queryCustomerInfo(int id, int customerID)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		Trace.info(mode + "::queryCustomerInfo(" + id + ", " + customerID + ") called" );
		String message = mode + "::queryCustomerInfo(" + id + ", " + customerID + ")";
		txnM.setRMCustomer(this);
		txnM.enlist(id, 3);
		Customer cust = existsCustomer(id, customerID, message);
		if( cust == null ) {
			return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
		} 
		else {
			String st = cust.printBill();
			Trace.info(mode + "::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );
			System.out.println( st );
			return st;
		} 
		
	}


	// customer functions
	// new customer just returns a unique customer identifier
	
	public int newCustomer(int id) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		Trace.info("INFO: " + mode + "::newCustomer(" + id + ") called" );
		txnM.setRMCustomer(this);
		txnM.enlist(id, 3);
		// Generate a globally unique ID for the new customer
		
		int cid = Integer.parseInt( String.valueOf(id) +
								String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
								String.valueOf( Math.round( Math.random() * 100 + 1 )));
	
		Customer cust = new Customer( cid );
		writeData( id, cust.getKey(cid), cust );
		System.out.println("Customer: " + cust.getKey(cid) + " " + cid);
		Trace.info(mode + "::newCustomer(" + cid + ") returns ID=" + cid );
		
		return cid;
	}

	// I opted to pass in customerID instead. This makes testing easier
	public boolean newCustomer(int id, int customerID )
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		Trace.info("INFO: " + mode + "::newCustomer(" + id + ", " + customerID + ") called" );
		txnM.setRMCustomer(this);
		txnM.enlist(id, 3);
		//Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
		String message = mode + "::newCustomer(" + id + ", " + customerID + ")";
		Customer cust = existsCustomer(id, customerID, message);
		if( cust == null ) {
			cust = new Customer(customerID);
			writeData( id, cust.getKey(), cust );
			Trace.info("INFO: " + mode + "::newCustomer(" + id + ", " + customerID + ") created a new customer" );
			return true;
		} else {
			Trace.info("INFO: " + mode + "::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
			return false;
		} 
	}


	// Deletes customer from the database. 
	public boolean deleteCustomer(int id, int customerID) 
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		Trace.info(mode + "::deleteCustomer(" + id + ", " + customerID + ") called" );
		String message = mode + "::deleteCustomer(" + id + ", " + customerID + ")";
		
		txnM.setRMCustomer(this);
		txnM.enlist(id, 3);
		Customer cust = existsCustomer(id, customerID, message);
		if( cust == null ) {
			return false;
		} 
		else {			
			// Increase the reserved numbers of all reservable items which the customer reserved. 
			// Olga: get lock, have to add timer and loop
			String key = cust.getKey();
			try {
				lm.lock(id, key, LockManager.WRITE); 
				// job to be done: correctly write everything in the local table
				RMHashtable reservationHT = cust.getReservations();
				for(Enumeration e = reservationHT.keys(); e.hasMoreElements();){		
					String reservedkey = (String) (e.nextElement());
					ReservedItem reserveditem = cust.getReservedItem(reservedkey);
					int number = reserveditem.getCount();
					if (reservedkey.contains("flight")) {
						try {
							txnM.enlist(id, 0);
							if (tcp) 
								tcpFlight.cancelReservation(id, customerID, reserveditem.getKey(), number);
							else
								rmFlight.cancelReservation(id, customerID, reserveditem.getKey(), number);
						}
						catch ( TransactionAbortedExceptionFlight exp) {
							throw new TransactionAbortedException(id);
						}
						catch ( InvalidTransactionExceptionFlight exp) {
							throw new InvalidTransactionException(id);
						}
					}
					else if (reservedkey.contains("car")) {
						txnM.enlist(id, 1);
						try {
							if (tcp)
								tcpCar.cancelReservation(id, customerID, reserveditem.getKey(), number);
							else
								rmCar.cancelReservation(id, customerID, reserveditem.getKey(), number);
						}
						catch ( TransactionAbortedExceptionCar ec) {
							throw new TransactionAbortedException(id);
						}
						catch ( InvalidTransactionExceptionCar ec) {
							throw new InvalidTransactionException(id);
						}
					}
					else if (reservedkey.contains("room")) {
						txnM.enlist(id, 2);
						try {
							if (tcp)
								tcpRoom.cancelReservation(id, customerID, reserveditem.getKey(), number);
							else
								rmRoom.cancelReservation(id, customerID, reserveditem.getKey(), number);
						}
						catch ( TransactionAbortedExceptionRoom er) {
							throw new TransactionAbortedException(id);
						}
						catch ( InvalidTransactionExceptionRoom er) {
							throw new InvalidTransactionException(id);
						}
					}
					else {
						// should not happen
					}
				}
			}
			catch (DeadlockException e) {
				txnM.abort(id);
				throw new TransactionAbortedException(id);
			}
			// remove the customer from the storage
			removeData(id, cust.getKey());
			
			Trace.info(mode + "::deleteCustomer(" + id + ", " + customerID + ") succeeded" );
			return true;
		} 
	}


	// Adds car reservation to this customer. 
	public boolean reserveCar(int id, int customerID, String location)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		String key = "car-" + location;
		String message = mode + "::reserveCar( " + id + ", " + customerID + ", " + key + ", "+ location + ")";
		txnM.enlist(id, 1);
		Customer cust = existsCustomer(id, customerID, message);
		if (cust == null)
			return false;
		else {
			Trace.info(mode + "::reserveItem( " + id + ", customer=" + customerID + ", " 
					+ key + ", "+ location+" ) called" );
			int price;
			try {
				if (tcp)
					price = tcpCar.reserveCar(id, location);
				else
					price = rmCar.reserveCar(id, location);
				/* reserve successful on the cars server */
				if (finishReservation(id, cust, key, location, price))
					return true;
			}
			catch ( TransactionAbortedExceptionCar e) {
				throw new TransactionAbortedException(id);
			}
			catch ( InvalidTransactionExceptionCar e) {
				throw new InvalidTransactionException(id);
			}
			catch ( DeadlockException e) {
				txnM.abort(id);
				throw new TransactionAbortedException(id);
			}
			
		}
		return false;
	}


	// Adds room reservation to this customer. 
	public boolean reserveRoom(int id, int customerID, String location)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		String key = "room-" + location;
		String message = mode + "::reserveRoom( " + id + ", " + customerID + ", " + key + ", "+ location + ")";
		txnM.enlist(id, 2);
		Customer cust = existsCustomer(id, customerID, message);
		if (cust == null)
			return false;
		else {
			Trace.info(mode + "::reserveItem( " + id + ", customer=" + customerID + ", " 
					+ key + ", "+ location+" ) called" );
		
			int price;
			try {
				if (tcp)
					price = tcpRoom.reserveRoom(id, location); 
				else	
					price = rmRoom.reserveRoom(id, location);
				/* reserve successful on the rooms server */
				if (finishReservation(id, cust, key, location, price))
					return true;
			}
			catch ( TransactionAbortedExceptionRoom e) {
				throw new TransactionAbortedException(id);
			}
			catch ( InvalidTransactionExceptionRoom e) {
				throw new InvalidTransactionException(id);
			}
			catch ( DeadlockException e) {
				txnM.abort(id);
				throw new TransactionAbortedException(id);
			}
			
		}
		return false;
	}
	
	// Adds flight reservation to this customer.  
	public boolean reserveFlight(int id, int customerID, int flightNum)
			throws RemoteException, TransactionAbortedException, 
			InvalidTransactionException {
		String key = "flight-" + flightNum;
		String location = String.valueOf(flightNum);
		String message = mode + "::reserveFlight( " + id + ", " + customerID + ", " + key + ", "+ location + ")";
		txnM.enlist(id, 0);
		Customer cust = existsCustomer(id, customerID, message);
		if (cust == null)
			return false;
		else {
			Trace.info(mode + "::reserveItem( " + id + ", customer=" + customerID + ", " 
					+ key + ", "+ location+" ) called" );
			int price;
			try {
				if (tcp)
					price = tcpFlight.reserveFlight(id, flightNum);
				else
					price = rmFlight.reserveFlight(id, flightNum);
				System.out.println("Customer : " + customerID);
				if (finishReservation(id, cust, key, location, price))
					return true;
			}
			catch ( TransactionAbortedExceptionFlight e) {
				throw new TransactionAbortedException(id);
			}
			catch ( InvalidTransactionExceptionFlight e) {
				throw new InvalidTransactionException(id);
			}
			catch ( DeadlockException e) {
				txnM.abort(id);
				throw new TransactionAbortedException(id);
			}
		
		}
		return false;	
	}
	private boolean finishReservation(int id, Customer cust, String key, String location, int price) 
			throws RemoteException, InvalidTransactionException, TransactionAbortedException, DeadlockException {
		/* reserve successful on the flights server */
		if (price >= 0) {
			reserveItem(id, cust, key, location, price);
			Trace.info(mode + "::reserveItem( " + id + ", " + cust.getID() + ", " 
						+ key + ", " +location+") succeeded" );
			return true;
		}
		else if (price == -1) {
			Trace.warn(mode + "::reserveItem( " + id + ", " + cust.getID() + ", " + 
				 key +", " + location +") failed--item doesn't exist" );
		
		}
		else if (price == -2) {
			Trace.warn(mode + "::reserveItem( " + id + ", " + cust.getID() + ", " + 
					key +", " + location+") failed--No more items" );
		
		}
		return false;
	}
	/* reserve an itinerary */
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car, boolean Room)
    			throws RemoteException, TransactionAbortedException, 
    			InvalidTransactionException {
    	try {
	    	String message = mode + "::Itinerary( " + id + ", " + customer + ", " + location + ")";
	    	Customer cust = existsCustomer(id, customer, message);
			if (cust == null)
				return false;
			else {
				boolean reserved = false;
				if (Car) {
					txnM.enlist(id, 1);
	   		 		if (! reserveCar(id, customer, location))
	   		 			return false;
				}
				if (Room) {
					if (! reserveRoom(id, customer, location)) {
						txnM.enlist(id, 2);
						if (Car) {
							String key = "car-" + location;
							removeData(id, key);
							if (tcp)
								tcpCar.cancelReservation(id, customer, key, 1);
							else
								rmCar.cancelReservation(id, customer, key, 1);
						}
						return false;
					}
				}
				txnM.enlist(id, 0);
				int i = 0;
				for (i = 0; i < flightNumbers.size(); i++) {
					if (! reserveFlight(id, customer, Integer.parseInt((String)flightNumbers.get(i)))) 
						break;
				}
				// could not reserve all flights, cancel reservation
	    	
				if (i < flightNumbers.size()) {
					int number = i; // have registred before failure
					--i;
					while (i >= 0 ) {
						String key = "flight-" + flightNumbers.get(i--);
						removeData(id, key);
						if (tcp)
							tcpFlight.cancelReservation(id, customer, key, number);
						else
							rmFlight.cancelReservation(id, customer, key, number);
					}
					if (Car) {
						String key = "car-" + location;
						removeData(id, key);
						if (tcp)
							tcpCar.cancelReservation(id, customer, key, 1);
						else
							rmCar.cancelReservation(id, customer, key, 1);
					}
					if (Room) {
						String key = "room-" + location;
						removeData(id, key);
						if (tcp)
							tcpRoom.cancelReservation(id, customer, key, 1);
						else
							rmRoom.cancelReservation(id, customer, key, 1);
					}
					return false;
				}
				return true;
			}
    	}
    	catch ( TransactionAbortedExceptionFlight e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionFlight e) {
			throw new InvalidTransactionException(id);
		}
    	catch ( TransactionAbortedExceptionCar e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionCar e) {
			throw new InvalidTransactionException(id);
		}
    	catch ( TransactionAbortedExceptionRoom e) {
			throw new TransactionAbortedException(id);
		}
		catch ( InvalidTransactionExceptionRoom e) {
			throw new InvalidTransactionException(id);
		}
    	catch ( DeadlockException e) {
			txnM.abort(id);
			throw new TransactionAbortedException(id);
		}
    }
    public boolean shutdown() throws RemoteException {
    	rmFlight.shutdown();
    	rmCar.shutdown();
    	rmRoom.shutdown();
    	Trace.info("Shutting down the system");
    
 	    try {
 		    registry.unbind("group11");
 		    UnicastRemoteObject.unexportObject(this,true);  
 	    }
 	    catch (Exception e) {
 	    	
 	    }
 	  
    	return true;
    }
    
    // TCP stuff
    public MiddlewareImpl(Socket socket) 
			throws RemoteException { 
		if (socket != null && tcp) {
			this.socket = socket;
			try {
				out = new ObjectOutputStream(socket.getOutputStream());
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
				// get the request
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
				// send an answer
				out.writeObject(processRequests(method, args));
			} catch (IOException e) {
				System.err.println();
				e.printStackTrace();
				System.exit(1);
			}
			catch (TransactionAbortedException e) {
				System.err.println();
				e.printStackTrace();
				System.exit(1);
			}
			catch (InvalidTransactionException e) {
				System.err.println();
				e.printStackTrace();
				System.exit(1);
			}
					
		}
	}
	
	
	private Object processRequests(String method, LinkedList<Object> args) 
			throws TransactionAbortedException, InvalidTransactionException {
		try {
			if (method.equals("addFlight")) 
				return (Boolean) addFlight((Integer)args.get(0),(Integer)args.get(1),(Integer)args.get(2),(Integer)args.get(3));
			
			else if (method.equals("addCars"))
				return (Boolean) addCars((Integer)args.get(0),(String)args.get(1),(Integer)args.get(2),(Integer)args.get(3)); 
	
			else if (method.equals("addRooms"))
				return (Boolean) addRooms((Integer)args.get(0),(String)args.get(1),(Integer)args.get(2),(Integer)args.get(3)); 
			
			else if (method.equals("newCustomer") && args.size() == 1)
				return (Integer) newCustomer((Integer) args.get(0));
			
			else if (method.equals("newCustomer") && args.size() == 2)
				return (Boolean) newCustomer((Integer) args.get(0), (Integer) args.get(1));
			
			else if (method.equals("deleteFlight"))
				return (Boolean) deleteFlight((Integer)args.get(0), (Integer) args.get(1));
			
			else if (method.equals("deleteCars"))
				return (Boolean) deleteCars((Integer)args.get(0), (String) args.get(1));
			
			else if (method.equals("deleteRooms"))
				return (Boolean) deleteRooms((Integer)args.get(0), (String) args.get(1));
			
			else if (method.equals("deleteCustomer"))
				return (Boolean) deleteFlight((Integer)args.get(0), (Integer) args.get(1));
			
			else if (method.equals("queryFlight"))
				return (Integer) queryFlight((Integer) args.get(0), (Integer) args.get(1));
			
			else if (method.equals("queryCars"))
				return (Integer) queryCars((Integer) args.get(0), (String) args.get(1));
			
			else if (method.equals("queryRooms"))
				return (Integer) queryRooms((Integer) args.get(0), (String) args.get(1));
			
			else if (method.equals("queryCustomerInfo"))
				return (String) queryCustomerInfo((Integer) args.get(0), (Integer) args.get(1));
			
			else if (method.equals("queryFlightPrice"))
				return (Integer) queryFlightPrice((Integer) args.get(0), (Integer) args.get(1));
			
			else if (method.equals("queryCarsPrice"))
				return (Integer) queryCarsPrice((Integer) args.get(0), (String) args.get(1));
			
			else if (method.equals("queryRoomsPrice"))
				return (Integer) queryRoomsPrice((Integer) args.get(0), (String) args.get(1));
			
			else if (method.equals("reserveFlight"))
				return (Boolean) reserveFlight((Integer) args.get(0), (Integer) args.get(1), (Integer) args.get(2));
			
			else if (method.equals("reserveCar"))
				return (Boolean) reserveCar((Integer) args.get(0), (Integer) args.get(1), (String) args.get(2));
			
			else if (method.equals("reserveRoom"))
				return (Boolean) reserveRoom((Integer) args.get(0), (Integer) args.get(1), (String) args.get(2));
			
			else if (method.equals("itinerary"))
				return (Boolean) itinerary((Integer) args.get(0), (Integer) args.get(1), (Vector) args.get(2), (String) args.get(3), (Boolean) args.get(4), (Boolean) args.get(5));
						
			else {
				// error
			}
		}
		catch (RemoteException e) {
			System.err.println("remote exception");
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean crash(String which) throws RemoteException {
		if (which.equalsIgnoreCase("flight")) {
			rmFlight.selfDestruct();
		}
		else if (which.equalsIgnoreCase("car")) {
			rmCar.selfDestruct();
		}
		else if (which.equalsIgnoreCase("room")) {
			rmRoom.selfDestruct();
		}
		else if (which.equalsIgnoreCase("tm")) {
			System.exit(0);
		}
		return true;
	}
	public boolean setDieAfterPrepare(String which) throws RemoteException {
		if (which.equalsIgnoreCase("flight"))
			txnM.setKillAfterPrepare(0);
		else if (which.equalsIgnoreCase("car"))
			txnM.setKillAfterPrepare(1);
		else if (which.equalsIgnoreCase("room"))
			txnM.setKillAfterPrepare(2);
		return true;
	}

}


