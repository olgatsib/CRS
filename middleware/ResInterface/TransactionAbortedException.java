package ResInterface;

public class TransactionAbortedException extends Exception {
      	
	public TransactionAbortedException(int tID) {
    	super("Transaction " + tID + " aborted");
    }
}
