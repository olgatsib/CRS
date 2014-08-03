package CarInterface;

public class InvalidTransactionExceptionCar extends Exception {
      	
	public InvalidTransactionExceptionCar(int tID) {
    	super("Transaction " + tID + " is invalid");
    }
}