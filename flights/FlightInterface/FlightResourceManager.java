package FlightInterface;

import LockManager.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;
/** 
 * 
 */

public interface FlightResourceManager extends Remote {
    /** Add seats to a flight.  In general this will be used to create a new
     * 	flight, but it should be possible to add seats to an existing flight.
     * 	Adding to an existing flight should overwrite the current price of the
     * 	available seats.
     *
     * 	@return success.
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) 
    		throws RemoteException, TransactionAbortedExceptionFlight, 
    		InvalidTransactionExceptionFlight, DeadlockException; 
    
    /**
     *  Delete the entire flight.
     *  deleteflight implies whole deletion of the flight.  
     *  all seats, all reservations.  If there is a reservation on the flight, 
     *  then the flight cannot be deleted
     *
     * @return success.
     */   
    public boolean deleteFlight(int id, int flightNum) 
    		throws RemoteException, TransactionAbortedExceptionFlight,
    		InvalidTransactionExceptionFlight, DeadlockException; 
    

    /* queryFlight returns the number of empty seats. */
    public int queryFlight(int id, int flightNumber) 
    		throws RemoteException, TransactionAbortedExceptionFlight,
    		InvalidTransactionExceptionFlight, DeadlockException; 

    /* queryFlightPrice returns the price of a seat on this flight. */
    public int queryFlightPrice(int id, int flightNumber) 
    		throws RemoteException, TransactionAbortedExceptionFlight, 
    		InvalidTransactionExceptionFlight, DeadlockException; 

    /* Reserve a seat on this flight*/
    public int reserveFlight(int id, int flightNumber) 
    		throws RemoteException, TransactionAbortedExceptionFlight, 
    		InvalidTransactionExceptionFlight, DeadlockException; 
    
    public boolean cancelReservation(int id, int customerID, String key, int number)
        	throws RemoteException, TransactionAbortedExceptionFlight, 
        	InvalidTransactionExceptionFlight, DeadlockException;
    
    public boolean commitTxn(int id) 
    		throws RemoteException, TransactionAbortedExceptionFlight, 
    		InvalidTransactionExceptionFlight;
    
    public void abortTxn(int id) throws RemoteException, InvalidTransactionExceptionFlight;
    
    public void startT(int id) throws RemoteException; 
    
    public boolean shutdown() throws RemoteException; 
    
    public boolean vote(int transactionId) throws RemoteException, 
			TransactionAbortedExceptionFlight, InvalidTransactionExceptionFlight;
    
    public void selfDestruct() throws RemoteException;
}
