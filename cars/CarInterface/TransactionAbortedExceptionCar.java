package CarInterface;

public class TransactionAbortedExceptionCar extends Exception {
      	
	public TransactionAbortedExceptionCar(int tID) {
    	super("Transaction " + tID + " aborted");
    }
}
