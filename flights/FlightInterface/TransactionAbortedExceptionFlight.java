package FlightInterface;

public class TransactionAbortedExceptionFlight extends Exception {
      	
	public TransactionAbortedExceptionFlight(int tID) {
    	super("Transaction " + tID + " aborted");
    }
}
