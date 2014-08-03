import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.RemoteException; // do I need it?

import ResInterface.*;

public class clientTCP {

	private Socket socket; 
	private ObjectInputStream in = null; 
	private ObjectOutputStream out = null;
	
	public clientTCP(String server, int port) {
		
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
		    
		    /* Add cars to a location.  
		     * This should look a lot like addFlight, only keyed on a string location
		     * instead of a flight number.
		     */
	public boolean addCars(int id, String location, int numCars, int price) 
			throws RemoteException {
		Object[] args = new Object[4];
		args[0] = new Integer(id);
		args[1] = location;
		args[2] = new Integer(numCars);
		args[3] = new Integer(price);
		return (Boolean) callServer("addCars", args);
	}
		   
		 
	public boolean addRooms(int id, String location, int numRooms, int price) 
			throws RemoteException {
		Object[] args = new Object[4];
		args[0] = new Integer(id);
		args[1] = location;
		args[2] = new Integer(numRooms);
		args[3] = new Integer(price);
		return (Boolean) callServer("addRooms", args);
	}

					    
		    /* new customer just returns a unique customer identifier */
	public int newCustomer(int id) throws RemoteException {
		Object[] args = new Object[1];
		args[0] = new Integer(id);
		return (Integer) callServer("newCustomer", args);
	}
		    
	/* new customer with providing id */
	public boolean newCustomer(int id, int cid) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(cid);
		return (Boolean) callServer("newCustomer", args);
	}

	
	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(flightNum);
		return (Boolean) callServer("deleteFlight", args);
	}
		    
	
	public boolean deleteCars(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Boolean) callServer("deleteCars", args);
	}

		    /* Delete all Rooms from a location.
		     * It may not succeed if there are reservations for this location.
		     *
		     * @return success
		     */
	public boolean deleteRooms(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Boolean) callServer("deleteRooms", args);
	}
		    
		    /* deleteCustomer removes the customer and associated reservations */
	public boolean deleteCustomer(int id,int customer) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(customer);
		return (Boolean) callServer("deleteCustomer", args);
	}

		    /* queryFlight returns the number of empty seats. */
	public int queryFlight(int id, int flightNumber) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(flightNumber);
		return (Integer) callServer("queryFlight", args);
	}

		    /* return the number of cars available at a location */
	public int queryCars(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Integer) callServer("queryCars", args);
	}

		    /* return the number of rooms available at a location */
	public int queryRooms(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Integer) callServer("queryRooms", args);
	}

		    /* return a bill */
	public String queryCustomerInfo(int id,int customer) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(customer);
		return (String) callServer("queryCustomerInfo", args);
	}
		    
		    /* queryFlightPrice returns the price of a seat on this flight. */
	public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = new Integer(flightNumber);
		return (Integer) callServer("queryFlight", args);
	}

		    /* return the price of a car at a location */
	public int queryCarsPrice(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Integer) callServer("queryCarsPrice", args);
	}

		    /* return the price of a room at a location */
	public int queryRoomsPrice(int id, String location) throws RemoteException {
		Object[] args = new Object[2];
		args[0] = new Integer(id);
		args[1] = location;
		return (Integer) callServer("queryRoomsPrice", args);
	}

		    /* Reserve a seat on this flight*/
	public boolean reserveFlight(int id, int customer, int flightNumber) throws RemoteException {
		Object[] args = new Object[3];
		args[0] = new Integer(id);
		args[1] = new Integer(customer);
		args[2] = new Integer(flightNumber);
		return (Boolean) callServer("reserveFlight", args);
	}

		    /* reserve a car at this location */
	public boolean reserveCar(int id, int customer, String location) 
			throws RemoteException {
		Object[] args = new Object[3];
		args[0] = new Integer(id);
		args[1] = new Integer(customer);
		args[2] = location;
		return (Boolean) callServer("reserveCar", args);
	}

		    /* reserve a room certain at this location */
	public boolean reserveRoom(int id, int customer, String location) 
			throws RemoteException {
		Object[] args = new Object[3];
		args[0] = new Integer(id);
		args[1] = new Integer(customer);
 		args[2] = location;
		return (Boolean) callServer("reserveRoom", args);
	}


	/* reserve an itinerary */
	public boolean itinerary(int id,int customer,Vector flightNumbers,String location, boolean Car, boolean Room)
			throws RemoteException {
		Object[] args = new Object[6];
		args[0] = new Integer(id);
		args[1] = new Integer(customer);
		args[2] = flightNumbers;
		args[3] = location;
		args[4] = new Boolean(Car);
		args[5] = new Boolean(Room);
		return (Boolean) callServer("itinerary", args);
	}
}