package TManager;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.*;

/** 
 * 
 */

public interface TManager extends Remote 
{
    public boolean requestAction(int id) throws RemoteException, IOException;
    
    public void commitDone(int id, int rm) throws RemoteException;
    
}
