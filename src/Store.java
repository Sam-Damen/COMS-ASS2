/**
 * 
 * @author Sam Damen (42636678)
 * Store
 * COMS3200
 * Assignment 2
 * 
 * Inputs = StorePort StockFile NSPort
 * 
 * Simulate Packet loss when communicating with NameServer, Bank & Content
 * 
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Store {
	
	static String[] bank;
	static String[] content;

	public static void main(String[] args) throws Exception {

		TreeMap<Long,Float> stock = new TreeMap<Long,Float> ();
		
		int[] commands = commandParse(args);
		
		//Attempt to read in the Stock File
		String path = System.getProperty("user.dir");
		File file = new File(path + "\\src\\" + args[1]);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(file));
			String temp = null;
			while ( (temp = reader.readLine()) != null) {
				//Split line by spaces and store in TreeMap
				String[] data = temp.split("\\s+");
				stock.put(Long.parseLong(data[0]), Float.parseFloat(data[1]));
			}
			//Close stream
			reader.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("IOException");
			e.printStackTrace();
			System.exit(1);
		}
		
		
		//Try to Register with the NameServer
		try {
			registerNS(commands);
		} catch (Exception e) {
			System.err.print("Registration with NameServerfailed");
			System.exit(1);
		}
		
		//Get Bank & Content Information
		try {
			lookupServers(commands[1]);
		} catch (Exception e) {
			System.err.print("unable to connect with NameServer\n");
			System.exit(1);
		}
		
		//Perform Server functionality
		//Create Datagram Socket
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(commands[0]);
			//Successfully can listen on port
			System.err.print("Store waiting for incoming connections ...\n");
		} catch (SocketException e) {
			System.err.format("Cannot listen on given port number %d\n", commands[0]);
			System.exit(1);
		}
		
		while (true) {
			
			// set buffers
			byte[] receiveData = new byte[1024];
			byte[] sendData = new byte[1024];
			
			
			// receive message from Client
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			
			//ACK the packet
			InetAddress IPAddr = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			sendData = "ACK\n".getBytes();
			DatagramPacket sendPacket = new DatagramPacket (sendData, sendData.length, IPAddr, clientPort);
			serverSocket.send(sendPacket);
			
			
			//Handle the client requests
			int request = Integer.parseInt( new String(receivePacket.getData()).trim() );
			if (request == 0) {
				System.out.println("Got request");
				int i = 0;
				//Iterate over all entries in the stock map
				for (Entry<Long,Float> entry : stock.entrySet() ) {
					i++;
					sendData = ( Integer.toString(i) + ". " + Long.toString(entry.getKey()) + " " + Float.toString(entry.getValue()) ).getBytes();
					sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,clientPort);
					serverSocket.send(sendPacket);
				}
				//"Close" the connection to client
				sendData = null;
				sendData = "DONE".getBytes();
				sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,clientPort);
				serverSocket.send(sendPacket);
				
			} else {
				//Purchase Request
				
			}
			
			
		}
		
		

	}
	
	
	//*******************************************************************
	//
	//							 Extra Methods
	//
	//*******************************************************************
			
	//Perform command line parsing
	private static int[] commandParse(String[] args) {
		
		int[] ports = new int[2];
		
		if(args.length == 3) {
			try {
				ports[0] = Integer.parseInt(args[0]);
				ports[1] = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.err.print("Invalid command line arguments for Store\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments for Store\n");
			System.exit(1);
		}
		return ports;
	}
	
	
	//Client instance to register with the NS
	private static void registerNS(int[] ports) throws Exception{
		
		//buffers
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddr = InetAddress.getByName("127.0.0.1");			
		DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length, IPAddr, ports[1]);
		
		//Send registration to server
		sendData = ("regi,Store,127.0.0.1," + Integer.toString(ports[0])).getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,ports[1]);
		
		//Simulate packet Loss
		if (Math.random() >= 0.5) {
			clientSocket.send(sendPacket);
		}
		
		//Set Timeout to 2 sec			
		clientSocket.setSoTimeout(5000);
		int i = 0;
		
		//Try to receive ACK, will continue to loop until 3 tries
		// 3 failed sending attempts is regarded as "could not connect"
		while (true) {
			
			try {
				clientSocket.receive(receivePacket);
				//Check for ACK
				String msg = new String (receivePacket.getData());
				System.out.println("Message Received");
				if ( msg.contains("ACK") ) {
					//Check for GOOD/BAD
					clientSocket.receive(receivePacket);
					String msg2 = new String (receivePacket.getData());
					//Successful registration
					if (msg2.contains("GOOD")) {
						break;
					} else {
						//Failed registration
						System.err.println("Store registration to NameServer failed");
						clientSocket.close();
						System.exit(1);
					}					
					
				}
			} catch (SocketTimeoutException e) {
				//Did not receive the packet, re-send
				System.out.println("Packet Loss Timeout");
				//Simulate packet Loss
				if (Math.random() >= 0.5) {
					clientSocket.send(sendPacket);
				} 					
			}
			
			if ( i >= 2) {
				System.err.println("Store registration to NameServer failed");
				clientSocket.close();
				System.exit(1);
			}
			i++;
		}
		clientSocket.close();
	}
	
	//Client Instance to get Bank & Content information from NS
	static void lookupServers(int port) throws Exception {
	
		//buffers
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddr = InetAddress.getByName("127.0.0.1");			
		DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length, IPAddr, port);
		
		//Send Lookup request
		sendData = ("look,Bank").getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,port);
		
		//Simulate packet Loss
		if (Math.random() >= 0.5) {
			clientSocket.send(sendPacket);
		}
		
		//Set Timeout to 2 sec			
		clientSocket.setSoTimeout(5000);
		int i =0;
		
		//Try to receive ACK, will continue to loop until 3 tries
		// 3 failed sending attempts is regarded as "could not connect"
		while (true) {
			
			try {
				clientSocket.receive(receivePacket);
				//Check for ACK
				String msg = new String (receivePacket.getData());
				System.out.println("Message Received");
				if ( msg.contains("ACK") ) {
					
					//Check Data received
					clientSocket.receive(receivePacket);
					bank = new String (receivePacket.getData()).split(",");
					
					//Successful registration
					if (! bank[0].equals("Bank")) {
						System.out.print("Bank has not registered\n");
						System.exit(1);
					} 
					
					//Now Also get Content information
					sendData = ("look,Content").getBytes();
					sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,port);
					clientSocket.send(sendPacket);
					
					clientSocket.receive(receivePacket);
					msg = new String (receivePacket.getData());
					System.out.println("Message Received");
					if ( msg.contains("ACK") ) {
						//Check Data received
						clientSocket.receive(receivePacket);
						content = new String (receivePacket.getData()).split(",");
						
						if (! content[0].equals("Content")) {
							System.out.print("Content has not registered\n");
							System.exit(1);
						} 
					}
					break;
					
				}
			} catch (SocketTimeoutException e) {
				//Did not receive the packet, re-send
				System.out.println("Packet Loss Timeout");
				//Simulate packet Loss
				if (Math.random() >= 0.5) {
					clientSocket.send(sendPacket);
				} 					
			}
			
			if ( i >= 2) {
				System.err.println("Store unable to communicate with NameServer");
				clientSocket.close();
				System.exit(1);
			}
			i++;
		}
		clientSocket.close();
		
		
	}
	

}
