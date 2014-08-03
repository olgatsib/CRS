package CarImpl;

import CarInterface.*;
import java.util.Timer;
import java.util.TimerTask;
import java.rmi.RemoteException;


public class Session extends Timer {
	private TimerTask timerTask;
	private CarResourceManager tm;
	private int id;
	
	public Session(CarResourceManager tm, int id) {
		this.tm = tm;
		this.id = id;
	}

	public void schedule() {
		timerTask = new TimerTask() { 
			public void run() { 
				try {
					Trace.info("Transaction was inactive for 3 minutes and will be aborted");
					tm.abortTxn(id);
					// TODO: log timeout
					timerTask.cancel();
			    }
				catch (RemoteException e) {
				}
				catch (InvalidTransactionExceptionCar e) {
				}
			} 
		};
		schedule(timerTask, 60000);      // 1mins  
	}
	
	
	public void reschedule() {
		timerTask.cancel();
		timerTask = new TimerTask() { 
			public void run() { 
				try {
					tm.abortTxn(id);
					timerTask.cancel();
		    	}
				catch (RemoteException e) {
				}
				catch (InvalidTransactionExceptionCar e) {
				}
		    }
		};
		schedule(timerTask, 60000);        // 1mins
	}
}