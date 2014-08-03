package MiddleImpl;

import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.RemoteException; // do I need it?

import CarInterface.*;

public class CarTCP {

	private Socket socket; 
	private ObjectInputStream in = null; 
	private ObjectOutputStream out = null;
	
	public CarTCP(String server, int port) {
		
		try {
			socket = new Socket(server, 3011);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: " + server);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O");
			System.exit(1);
		}
	}
	public Object callServer(String method, Object[] args) {
		Object result = null;
		try {
			out.writeObject(method);
			out.writeObject(args);
			result = in.readObject();
	    } catch (Exception e) {
	    	System.err.println("Error communication with the server");
	    	e.printStackTrace();
	    	System.exit(1);
	    }
		return result;
	}
	public boolean addCars(int id, String location, int numCars, int price) 
			throws RemoteException {
		Object[] args = new Object[4];
		args[0] = new Integer(id);
		args[1] = location;
		args[2] = new Integer(numCars);
		args[3] = new Integer(price);
		return (Boolean) callServer("addCars", args);
	}
	 
	public boolean deleteCars(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Boolean) callServer("deleteCars", args);
	}
		    

	/* queryCar returns the number of empty seats. */
	public int queryCars(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Integer) callServer("queryCars", args);
	}

	/* queryCarPrice returns the price of a seat on this Car. */
	public int queryCarsPrice(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Integer) callServer("queryCarsPrice", args);
	}

			    /* Reserve a seat on this Car*/
	public int reserveCar(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Integer) callServer("reserveCar", args);
		
	}
	public boolean cancelReservation(int id, int customerID, String key, int number)
	    	throws RemoteException {
		Object[] args = new Object[4];
		args[0] = new Integer(id);
		args[1] = new Integer(customerID);
		args[2] = key;
		args[3] = new Integer(number);
		return (Boolean) callServer("cancelReservation", args);
	}
}