package FlightInterface;

public class InvalidTransactionExceptionFlight extends Exception {
      	
	public InvalidTransactionExceptionFlight(int tID) {
    	super("Transaction " + tID + " is invalid");
    }
}