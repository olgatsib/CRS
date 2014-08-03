package RoomInterface;

import LockManager.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;
/** 
 * 
 */

public interface RoomResourceManager extends Remote {
    /** Add seats to a flight.  In general this will be used to create a new
     * 	flight, but it should be possible to add seats to an existing flight.
     * 	Adding to an existing flight should overwrite the current price of the
     * 	available seats.
     *
     * 	@return success.
     */
    public boolean addRooms(int id, String location, int count, int price) 
    		throws RemoteException, TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException; 
    
    /**
     *  Delete the entire flight.
     *  deleteflight implies whole deletion of the flight.  
     *  all seats, all reservations.  If there is a reservation on the flight, 
     *  then the flight cannot be deleted
     *
     * @return success.
     */   
    public boolean deleteRooms(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionRoom,
    		InvalidTransactionExceptionRoom, DeadlockException; 
    

    /* queryRoom returns the number of empty seats. */
    public int queryRooms(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionRoom,
    		InvalidTransactionExceptionRoom, DeadlockException; 

    /* queryRoomPrice returns the price of a seat on this flight. */
    public int queryRoomsPrice(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException; 

    /* Reserve a seat on this flight*/
    public int reserveRoom(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom, DeadlockException; 
    
    public boolean cancelReservation(int id, int customerID, String key, int number)
        	throws RemoteException, TransactionAbortedExceptionRoom, 
        	InvalidTransactionExceptionRoom, DeadlockException;
    
    public boolean commitTxn(int id) 
    		throws RemoteException, TransactionAbortedExceptionRoom, 
    		InvalidTransactionExceptionRoom;
    
    public void abortTxn(int id) throws RemoteException, InvalidTransactionExceptionRoom;
    
    public void startT(int id) throws RemoteException; 
    
    public boolean shutdown() throws RemoteException; 
    
    public boolean vote(int transactionId) throws RemoteException, 
			TransactionAbortedExceptionRoom, InvalidTransactionExceptionRoom;
			
	public void selfDestruct() throws RemoteException;
}
