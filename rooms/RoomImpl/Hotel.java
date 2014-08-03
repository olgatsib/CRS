// -------------------------------// adapted from Kevin T. Manley// CSE 593// -------------------------------package RoomImpl;import java.io.IOException;import java.io.ByteArrayInputStream;import java.io.ByteArrayOutputStream;import java.io.ObjectOutputStream;import java.io.ObjectInputStream;public class Hotel extends ReservableItem{		public Hotel( String location, int count, int price )		{			super( location, count, price );		}		public String getKey()		{			return Hotel.getKey( getLocation() );		}		public static String getKey( String location )		{			String s = "room-" + location  ;			return s.toLowerCase();		}		 // creates a deep copy of the object	    public static Hotel copy(Hotel other) {		    Object obj = null;		    try {	            // Write the object out to a byte array	            ByteArrayOutputStream bos = new ByteArrayOutputStream();	            ObjectOutputStream out = new ObjectOutputStream(bos);	            out.writeObject(other);	            out.flush();	            out.close();	            // Make an input stream from the byte array and read	            // a copy of the object back in.	            ObjectInputStream in = new ObjectInputStream(	                new ByteArrayInputStream(bos.toByteArray()));	            obj = in.readObject();	        }		    catch(IOException e) {	            e.printStackTrace();	        }	        catch(ClassNotFoundException cnfe) {	            cnfe.printStackTrace();	        }		    return (Hotel)obj;	    }	}