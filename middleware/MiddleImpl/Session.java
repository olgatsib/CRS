package MiddleImpl;

import ResInterface.*;
import java.util.Timer;
import java.util.TimerTask;
import java.rmi.RemoteException;


public class Session extends Timer {
	private TimerTask timerTask;
	private TransactionManager tm;
	private ResourceManager rm;
	private int id;
	
	public Session(TransactionManager tm, int id) {
		this.tm = tm;
		this.id = id;
	}

	public Session(ResourceManager rm, int id) {
		this.rm = rm;
		this.id = id;
	}
	
	public void schedule() {
		timerTask = new TimerTask() { 
			public void run() { 
				try {
					Trace.info("Transaction was inactive for 3 minutes and will be aborted");
					tm.abort(id);
					timerTask.cancel();
			    }
				catch (RemoteException e) {
				}
				catch (InvalidTransactionException e) {
				}
			} 
		};
		schedule(timerTask, 180000);      // 3mins  
	}
	
	public void scheduleVote() {
		timerTask = new TimerTask() { 
			public void run() { 
				try {
					Trace.info("Timout for vote responses, transaction will be aborted");
					tm.abort(id);
					timerTask.cancel();
			    }
				catch (RemoteException e) {
				}
				catch (InvalidTransactionException e) {
				}
			} 
		};
		schedule(timerTask, 30000);      // .5mins  		
	}

	public void reschedule() {
		timerTask.cancel();
		timerTask = new TimerTask() { 
			public void run() { 
				try {
					tm.abort(id);
					timerTask.cancel();
		    	}
				catch (RemoteException e) {
				}
				catch (InvalidTransactionException e) {
				}
		    }
		};
		schedule(timerTask, 180000);        // 3mins
	}
}