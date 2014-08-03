package RoomInterface;

public class InvalidTransactionExceptionRoom extends Exception {
      	
	public InvalidTransactionExceptionRoom(int tID) {
    	super("Transaction " + tID + " is invalid");
    }
}