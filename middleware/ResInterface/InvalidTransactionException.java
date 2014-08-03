package ResInterface;

public class InvalidTransactionException extends Exception {
      	
	public InvalidTransactionException(int tID) {
    	super("Transaction " + tID + " is invalid");
    }
}