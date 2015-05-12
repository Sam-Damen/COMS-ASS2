/**
 * 
 * @author Sam Damen (42636678)
 * NameServer
 * COMS3200
 * Assignment 2
 *
 * No-packetloss simulated for NameServer
 *
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

public class NameServer {
	
	
	//Store of Names and IP/Port of the registered servers
	private static HashMap<String, ArrayList<String>> serverTable = new HashMap<String, ArrayList<String>>();
	
	
	public static void main(String[] args) throws Exception {
		
		
		int port = commandParse(args);
		
		//Buffers
//		byte[] receiveData = new byte[1024];
//		byte[] sendData = new byte[1024];
		
		//Create Datagram Socket
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(port);
			//Successfully can listen on port
			System.err.print("Name Server waiting for incoming connections ...\n");
		} catch (SocketException e) {
			System.err.format("Cannot listen on given port number %d\n", port);
			System.exit(1);
		}		

		
		//Handle Requests
		while(true) {
			
			//Buffers
			byte[] receiveData = new byte[1024];
			byte[] sendData = new byte[1024];
			
			//Block while Receive packet
			DatagramPacket receivePacket= new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			
			//Send ACK to received packet
			InetAddress IPAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			sendData = "ACK\n".getBytes();
			DatagramPacket sendPacket = new DatagramPacket (sendData, sendData.length, IPAddress, clientPort);
			serverSocket.send(sendPacket);
			
			//Clear Buffers
			receiveData = null;
			sendData = null;
			
			//Parse the sent message
			String message = new String(receivePacket.getData());
			
			try {
				switch (message.substring(0, 4)) {
				
				case "regi":
					//Register Request
					if( regQuery(message.trim()) ) {
						//send confirm to client
						sendData = "GOOD\n".getBytes();
					} else {
						sendData = "BAD\n".getBytes();
						//Close the connection
						serverSocket.close();
					}
					break;
					
				case "look":
					//Lookup Request
					if ( lookQuery(message.trim()) ) {
						String name = message.substring(5, (message.trim().length()) ); //line.length includes \r\n in eclipse so -2
						ArrayList<String> list = serverTable.get(name); 
						//Return the requested name and information to client
						sendData = (name + "," + list.get(0) + "," + list.get(1) + "\n").getBytes();

					} else {
						sendData = "Error: Process has not registered with the Name Server\n".getBytes();
					}
					break;
					
				default:
					//Rubbish
					sendData = "BAD\n".getBytes();	//Close the connection in UDP?
					break;
				}				
				
			} catch (StringIndexOutOfBoundsException e) {
				sendData = "BAD\n".getBytes();
				serverSocket.close();
				break;
			}
			
			//Send the reply message to the client
			DatagramPacket sendPacket2 = new DatagramPacket (sendData, sendData.length, IPAddress, clientPort);
			serverSocket.send(sendPacket2);
			
			//Clear Buffers
			receiveData = null;
			sendData = null;
			
		}
		
		//Broke from loop
		serverSocket.close();
		System.exit(1);

	}
	
	
	//*******************************************************************
	//
	//					           Extra Methods
	//
	//*******************************************************************
	
	
	//Perform command line parsing
	private static int commandParse(String[] args) {
		
		int port = 0;
		
		if(args.length == 1) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.print("Invalid command line arguments for NameServer\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments for NameServer\n");
			System.exit(1);
		}
		return port;
	}
	
	//Check a message for correct format to register
	private static boolean regQuery(String message) {
		String[] data = message.split(",");
		
		//TODO Check if valid Name,IP,Port, correct number of items (3)
		
		ArrayList<String> list = new ArrayList<String>();
		list.add(data[2]);
		list.add(data[3]);
		serverTable.put(data[1], list);		
		
		return true;
	}
	
	//Check if a message is in the Table
	private static boolean lookQuery(String message) {
		String[] data = message.split(",");
		
		//TODO Check if valid name?		
		return serverTable.containsKey(data[1]);	
	}	

}
