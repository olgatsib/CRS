package MiddleImpl;

import ResInterface.*;
import FlightInterface.*;
import CarInterface.*;
import RoomInterface.*;
import TManager.*;

import java.io.BufferedWriter;
import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import java.rmi.*;

//new
public class TransactionManager implements TManager {

	private int txnID;
	private Map<Integer, LinkedList<Integer>> transactionList = 
			new HashMap<Integer, LinkedList<Integer>>();
	
	private MiddlewareImpl rmCustomer;
	private Map<Integer, Session> sessionMap = new HashMap<Integer, Session>();
	private BufferedWriter log;
	private File logTM = new File("txnMLog.txt"); 
	// to store the list of transactions
	private File storage = new File("TM_storage.dat");
	int max = 0; // to find the next transaction number
	private int killAfterPrepare = -1;
	
	
	public TransactionManager() throws RemoteException, IOException,
			InvalidTransactionException, TransactionAbortedException  {
		try {
			if (!storage.exists()) {
				storage.createNewFile();
			}
			else if (storage.length() > 0){
				restoreList();
			}
			else {
				// do nothing
			}
				
			if (!logTM.exists()) {
				logTM.createNewFile();
				txnID = 0;
			}
			else if (logTM.length() > 0){
				recover();
			}
			else {
				txnID = 0;
			}
		}
		//prepare writer to write into log
    
		catch (IOException e) {
			Trace.warn("Problems with files at bootstrap");
			return;
		}
		//FileWriter fileWriter = new FileWriter(logTM, true);
    	//log = new BufferedWriter(fileWriter);
	}
	public void setKillAfterPrepare(int n) {
		killAfterPrepare = n;
	}
	private synchronized boolean restoreList() {
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(storage));
			
			Object obj = null;

		    if ((obj = in.readObject()) != null) 
		    	transactionList = (HashMap<Integer, LinkedList<Integer>>)obj;
		    in.close();
		    
		    if (obj != null) {
			    // iterate through the storageMap and add timer to each transaction
			    Iterator it = transactionList.entrySet().iterator();
			    while (it.hasNext()) {
			        Map.Entry<Integer, RMHashtable> pairs = (Map.Entry<Integer, RMHashtable>) it.next();
			        Session txnSession = new Session(this, pairs.getKey());
			        sessionMap.put(pairs.getKey(), txnSession);
			        txnSession.schedule();
			        Trace.info("Reading transaction list TM: " + pairs.getKey() + " " + pairs.getValue());
			        if (pairs.getKey() > max) {
			        	max = pairs.getKey();
			        }
			    }
			}
		    txnID = max;
		}
		catch (IOException e) {
			System.out.println("Can't read storage file");
		}
		catch (ClassNotFoundException e) {
			System.out.println("Class not found when read storage file");
		}
		return true;
	}
	// read log and act accordingly
    private synchronized boolean recover() throws IOException, 
    		InvalidTransactionException, TransactionAbortedException {
    	
	    // map to get info about all transactions written in log
		Map<Integer, String> mapLog = new HashMap<Integer, String>();
		// Read log to know what to do
		FileReader reader = new FileReader(logTM);
		BufferedReader br = new BufferedReader(reader);
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] split = line.split(",");
			int idTxn = Integer.parseInt(split[0]);
			mapLog.put(idTxn, split[1]);
		}
		br.close();
		// read mapLog to know what to do with each transaction
		int id;
		Iterator it = mapLog.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, String> pairs = (Map.Entry<Integer, String>)it.next();
			id = pairs.getKey();
			if (id > max) {
				max = id;
		    }
			String action = pairs.getValue();
			try {
				  if (action.contains("ABORT")) {
				       	abort(id);
					}
				    else if (action.contains("COMMIT")) {
				       	commit(id);
					}
				    else if (action.contains("END-OF-TRANSACTION")) {
				    	//do nothing
				    }
				    else {
				    	//default action
				       	abort(id);
					}
		    }
			catch (InvalidTransactionException e) {
				throw new InvalidTransactionException(id);
			}
			catch (TransactionAbortedException e) {
				throw new TransactionAbortedException(id);
			}
		}
		txnID = max;
		return true;
    }
    
    public void commitDone(int id, int rm) throws RemoteException {
    	if (transactionList.containsKey(id)) {
			LinkedList<Integer> list = transactionList.get(id);
			if (list.contains(rm)) {
				list.remove(list.indexOf(rm));
				writeStorageList();
			}
			if (list.isEmpty()) {
				try {
					FileWriter fileWriter = new FileWriter(logTM, true);
			    	log = new BufferedWriter(fileWriter);
					log.write(id + ",END-OF-TRANSACTION\n");
					log.close();
					transactionList.remove(id);
					writeStorageList();
				}
				catch (IOException e) {
					System.out.println("Can't write to log");
				}	
    		}
    	}
    }
    private synchronized void writeStorageList() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(storage));
			out.writeObject(transactionList);
			out.close();
		}
		catch (IOException e) {
			System.out.println("Can't write to storage file");
		}
	}
	public void setRMCustomer(MiddlewareImpl mw) {
		rmCustomer = mw;
	}
	
	public boolean requestAction(int id) throws RemoteException, IOException {
		// check its log file to know if the transaction has been commited or not
		if (logTM.exists()) {
			FileReader reader = new FileReader(logTM);
			BufferedReader br = new BufferedReader(reader);
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(",");
				int idTxn = Integer.parseInt(split[0]);
				//If the transaction in question is found check status
				if (idTxn == id) {
					if (split[1].contains("ABORT")) {
						br.close();
						return false;
					} else if (split[1].contains("COMMIT")) {
						br.close();
						return true;
					}
				}
			}
			br.close();
		}
		//default action if log is not found, id is not found, or log has something
		//other than commit or abort
		System.out.println("ID: " + id + " no decision recorded, abort");
		return false;
	}
	//Empty stack is initialized
	public int startTxn() throws RemoteException {
		// increment transactions counter
		txnID++;
		transactionList.put(txnID, new LinkedList<Integer>());
		writeStorageList();
		Session tnxSession = new Session(this, txnID);
		sessionMap.put(txnID, tnxSession);
		tnxSession.schedule();
		return txnID;
	}
	
	
	//called when the client requests the middleware to commit
	public void prepare(int transactionId) 
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		boolean abort = false;
		if (transactionList.containsKey(transactionId)) {
			LinkedList<Integer> list = transactionList.get(transactionId);
			if (list != null) {
				try {
					Trace.info("TM writing START to the log");
					FileWriter fileWriter = new FileWriter(logTM, true);
			    	log = new BufferedWriter(fileWriter);
				    log.write(transactionId + ",START\n");
				    log.close();
				}
				catch (IOException e) {
					System.out.println("Can't write to log");
				}
				Session voteSession = new Session(this, transactionId);
				voteSession.scheduleVote();
				for (Integer r : list) {
					switch (r) {
					case 0:
						try {
							if (!MiddleImpl.MiddlewareImpl.rmFlight.vote(transactionId)) {
								//if it is returning false, it will be aborting, don't resend message
								list.remove(r);
								abort = true;
							}
							else {
								if (killAfterPrepare == 0)
									MiddleImpl.MiddlewareImpl.rmFlight.selfDestruct();
							}
						}
						catch (TransactionAbortedExceptionFlight e) {
							throw new TransactionAbortedException(transactionId);
						}
						catch (InvalidTransactionExceptionFlight e) {
							throw new InvalidTransactionException(transactionId);
						}
						finally {
							break;
						}
					case 1:
						try {
							if(!MiddleImpl.MiddlewareImpl.rmCar.vote(transactionId)) {
								Trace.info("Car voted NO");
								list.remove(r);
								abort = true;
							}
							else {
								if (killAfterPrepare == 1) 
									MiddleImpl.MiddlewareImpl.rmCar.selfDestruct();
							}
						}
						catch (TransactionAbortedExceptionCar e) {
							throw new TransactionAbortedException(transactionId);
						}
						catch (InvalidTransactionExceptionCar e) {
							throw new InvalidTransactionException(transactionId);
						}
						finally {
							break;
						}
					case 2:
						try {
							if(!MiddleImpl.MiddlewareImpl.rmRoom.vote(transactionId)) {
								list.remove(r);
								abort = true;
							}
							else {
								if (killAfterPrepare == 2)
									MiddleImpl.MiddlewareImpl.rmRoom.selfDestruct();
							}
						}
						catch (TransactionAbortedExceptionRoom e) {
							throw new TransactionAbortedException(transactionId);
						}
						catch (InvalidTransactionExceptionRoom e) {
							throw new InvalidTransactionException(transactionId);
						}
						finally {
							break;
						}	
					case 3:
						if (rmCustomer != null) {
							if(!rmCustomer.vote(transactionId)) {
								list.remove(r);
								abort = true;
							}
						}
						break;
					}
				}
				voteSession.cancel();
				//update list 
				transactionList.put(transactionId, list);
				if (abort) {
					try {
						FileWriter fileWriter = new FileWriter(logTM, true);
				    	log = new BufferedWriter(fileWriter);
						log.write(transactionId + ",ABORT\n");
						log.close();
					}
					catch (IOException e) {
						System.out.println("Can't write to log");
					}
					abort(transactionId);
				} else {
					try {
						FileWriter fileWriter = new FileWriter(logTM, true);
				    	log = new BufferedWriter(fileWriter);
						log.write(transactionId + ",COMMIT\n");
						log.close();
					}
					catch (IOException e) {
						System.out.println("Can't write to log");
					}
					commit(transactionId);
				}
			}
			else {
				Trace.warn("List of RMs is null");
			}
		}
		
	}
	
	public boolean commit(int transactionId) 
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		boolean committed = true;
		if (transactionList.containsKey(transactionId)) {
			LinkedList<Integer> list = transactionList.get(transactionId);
			if (list != null) {
				for (Integer r : list) {
					if (r == 3) {
						if (rmCustomer != null) {
							if (! rmCustomer.commitTxn(transactionId)) {
								committed = false;
							}
							else
								list.remove(r);
						}
					}
					else if (r == 0){
						try {
							if (MiddleImpl.MiddlewareImpl.rmFlight != null) {
								if (! MiddleImpl.MiddlewareImpl.rmFlight.commitTxn(transactionId))
									committed = false;
								else
									list.remove(r);
							}
						}
						catch (TransactionAbortedExceptionFlight e) {
							throw new TransactionAbortedException(transactionId);
						}
						catch (InvalidTransactionExceptionFlight e) {
							throw new InvalidTransactionException(transactionId);
						}
					}
					else if (r == 1) {
						try {
							if (MiddleImpl.MiddlewareImpl.rmCar != null) {
								if (! MiddleImpl.MiddlewareImpl.rmCar.commitTxn(transactionId))
									committed = false;
								else
									list.remove(r);
							}
						}
						catch (TransactionAbortedExceptionCar e) {
							throw new TransactionAbortedException(transactionId);
						}
						catch (InvalidTransactionExceptionCar e) {
							throw new InvalidTransactionException(transactionId);
						}
					}
					else if (r == 2) {
						try {
							if (MiddleImpl.MiddlewareImpl.rmRoom != null) {
								if (! MiddleImpl.MiddlewareImpl.rmRoom.commitTxn(transactionId))
									committed = false;
								else
									list.remove(r);
							}
						}
						catch (TransactionAbortedExceptionRoom e) {
							throw new TransactionAbortedException(transactionId);
						}
						catch (InvalidTransactionExceptionRoom e) {
							throw new InvalidTransactionException(transactionId);
						}
					}
					else {
						// should not happen
					}
					
				}
			}
			sessionMap.get(transactionId).cancel();
			sessionMap.remove(transactionId);
			writeStorageList();
			if (list.isEmpty()) { 
				transactionList.remove(transactionId);
			
				//method would have returned if one RM failed to commit
				try {
					FileWriter fileWriter = new FileWriter(logTM, true);
			    	log = new BufferedWriter(fileWriter);
					log.write(transactionId + ",END-OF-TRANSACTION\n");
					log.close();
				}
				catch (IOException e) {
					System.out.println("Can't write to log");
				}
			}
			return true;
		}
		else {
			throw new InvalidTransactionException(transactionId);
		}
	}
	
	public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
		if (transactionList.containsKey(transactionId)) {
			LinkedList<Integer> list = transactionList.get(transactionId);
			if (list != null) {
				for (Integer r : list) {
					if (r == 3) {
						if (rmCustomer != null)
							rmCustomer.abortTxn(transactionId);
					}
					else if (r == 0) {
						try {
							if (MiddleImpl.MiddlewareImpl.rmFlight != null) 
								MiddleImpl.MiddlewareImpl.rmFlight.abortTxn(transactionId);
						}
						catch (InvalidTransactionExceptionFlight e) {
							throw new InvalidTransactionException(transactionId);
						}
					}
					else if (r == 1) {
						try {
							if (MiddleImpl.MiddlewareImpl.rmCar != null)
								MiddleImpl.MiddlewareImpl.rmCar.abortTxn(transactionId);
						}
						catch (InvalidTransactionExceptionCar e) {
							throw new InvalidTransactionException(transactionId);
						}
					}
					else if (r == 2) {
						try {
							if (MiddleImpl.MiddlewareImpl.rmRoom != null)
								MiddleImpl.MiddlewareImpl.rmRoom.abortTxn(transactionId);
						}
						catch (InvalidTransactionExceptionRoom e) {
							throw new InvalidTransactionException(transactionId);
						}
					}
					else {
						// should not happen
					}
				}
			}
			sessionMap.get(transactionId).cancel();
			sessionMap.remove(transactionId);
			transactionList.remove(transactionId);
			writeStorageList();
			try {
				FileWriter fileWriter = new FileWriter(logTM, true);
		    	log = new BufferedWriter(fileWriter);
				log.write(transactionId + ",END-OF-TRANSACTION\n");
				log.close();
			}
			catch (IOException e) {
				System.out.println("Can't write to log");
			}
			Trace.info("Transaction " + transactionId + " is aborted");
		}
		else {
			throw new InvalidTransactionException(transactionId);
		}
	}
	
	public void enlist(int id, Integer rm) throws RemoteException, InvalidTransactionException {
		if (transactionList.containsKey(id)) {
		
			Session session = sessionMap.get(id);
			session.reschedule();
			
			LinkedList list = transactionList.get(id);
			if (! list.contains(rm)) {
				list.add(rm);
				if (rm == 3) {
					rmCustomer.startT(id);
				}
				else if (rm == 0) {
					MiddleImpl.MiddlewareImpl.rmFlight.startT(id);
				}
				else if (rm == 1) {
					MiddleImpl.MiddlewareImpl.rmCar.startT(id);
				}
				else if (rm == 2) {
					MiddleImpl.MiddlewareImpl.rmRoom.startT(id);
				}
				else {
					// should not happen
				}
			}
			writeStorageList();
		}
		else {
			throw new InvalidTransactionException(id);
		}
	}
}
