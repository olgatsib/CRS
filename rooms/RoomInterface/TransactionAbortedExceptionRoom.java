package RoomInterface;

public class TransactionAbortedExceptionRoom extends Exception {
      	
	public TransactionAbortedExceptionRoom(int tID) {
    	super("Transaction " + tID + " aborted");
    }
}
