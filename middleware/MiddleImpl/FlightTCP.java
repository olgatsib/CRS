package MiddleImpl;

import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.RemoteException; // do I need it?

import FlightInterface.*;

public class FlightTCP {

	private Socket socket; 
	private ObjectInputStream in = null; 
	private ObjectOutputStream out = null;
	
	public FlightTCP(String server, int port) {
		
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
	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) 
			throws RemoteException {
		Object[] args = new Object[4];
		args[0] = new Integer(id);
		args[1] = new Integer(flightNum);
		args[2] = new Integer(flightSeats);
		args[3] = new Integer(flightPrice);
		return (Boolean) callServer("addFlight", args);
	}
	 
	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(flightNum);
		return (Boolean) callServer("deleteFlight", args);
	}
		    

	/* queryFlight returns the number of empty seats. */
	public int queryFlight(int id, int flightNumber) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(flightNumber);
		return (Integer) callServer("queryFlight", args);
	}

		    /* queryFlightPrice returns the price of a seat on this flight. */
	public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(flightNumber);
		return (Integer) callServer("queryFlightPrice", args);
	}

	/* Reserve a seat on this flight*/
	public int reserveFlight(int id, int flightNumber) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(flightNumber);
		return (Integer) callServer("reserveFlight", args);
		
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