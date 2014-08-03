package CarInterface;

import LockManager.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;
/** 
 * 
 */

public interface CarResourceManager extends Remote {
    /** Add seats to a flight.  In general this will be used to create a new
     * 	flight, but it should be possible to add seats to an existing flight.
     * 	Adding to an existing flight should overwrite the current price of the
     * 	available seats.
     *
     * 	@return success.
     */
    public boolean addCars(int id, String location, int count, int price) 
    		throws RemoteException, TransactionAbortedExceptionCar, 
    		InvalidTransactionExceptionCar, DeadlockException; 
    
    /**
     *  Delete the entire flight.
     *  deleteflight implies whole deletion of the flight.  
     *  all seats, all reservations.  If there is a reservation on the flight, 
     *  then the flight cannot be deleted
     *
     * @return success.
     */   
    public boolean deleteCars(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionCar,
    		InvalidTransactionExceptionCar, DeadlockException; 
    

    /* queryCar returns the number of empty seats. */
    public int queryCars(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionCar,
    		InvalidTransactionExceptionCar, DeadlockException; 

    /* queryCarPrice returns the price of a seat on this flight. */
    public int queryCarsPrice(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionCar, 
    		InvalidTransactionExceptionCar, DeadlockException; 

    /* Reserve a seat on this flight*/
    public int reserveCar(int id, String location) 
    		throws RemoteException, TransactionAbortedExceptionCar, 
    		InvalidTransactionExceptionCar, DeadlockException; 
    
    public boolean cancelReservation(int id, int customerID, String key, int number)
        	throws RemoteException, TransactionAbortedExceptionCar, 
        	InvalidTransactionExceptionCar, DeadlockException;
    
    public boolean commitTxn(int id) 
    		throws RemoteException, TransactionAbortedExceptionCar, 
    		InvalidTransactionExceptionCar;
    
    public void abortTxn(int id) throws RemoteException, InvalidTransactionExceptionCar;
    
    public void startT(int id) throws RemoteException; 
    
    public boolean shutdown() throws RemoteException;
    
    public boolean vote(int transactionId) throws RemoteException, 
			TransactionAbortedExceptionCar, InvalidTransactionExceptionCar;

    public void selfDestruct() throws RemoteException;
}
