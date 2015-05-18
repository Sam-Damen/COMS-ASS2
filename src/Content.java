/**
 * 
 * @author Sam Dame (42636678)
 * Content
 * COMS3200
 * Assignment 2
 * 
 * Inputs = ContentPort ContentFile NSPort
 * 
 * Simulate packet loss when communicating with NameServer
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
import java.util.HashMap;


public class Content {

	public static void main(String[] args) throws Exception {
		
		int[] ports = commandParse(args);
		
		HashMap<Long,String> content = new HashMap<Long,String> ();
		
		//Attempt to read in Content File
		String path = System.getProperty("user.dir");
		File file = new File(path + "\\src\\" + args[1]);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader (file));
			String temp = null;
			while ( (temp = reader.readLine()) != null) {
				//split the line by spaces and store in HashMap
				String[] data = temp.split("\\s+");
				content.put(Long.parseLong(data[0]), data[1]);
			}
			//Close the file stream
			reader.close();
		} catch (FileNotFoundException e) {
			System.out.println("File Not Found");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("IOException");
			System.exit(1);
		}		
		
		
		//Try to register with NameServer
		registerNS(ports);
		
		//Start server functions
		//Create Datagram Socket
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(ports[0]);
			//Successfully can listen on port
			System.err.print("Content waiting for incoming connections ...\n");
		} catch (SocketException e) {
			System.err.format("Cannot listen on given port number %d\n", ports[0]);
			System.exit(1);
		}	
		

		
		while (true) {
			
			// set buffers
			byte[] receiveData = new byte[1024];
			byte[] sendData = new byte[1024];			
			
			// receive message from store
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			
			//Send ACK to received packet
			InetAddress IPAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			sendData = "ACK\n".getBytes();
			DatagramPacket sendPacket = new DatagramPacket (sendData, sendData.length, IPAddress, clientPort);
			serverSocket.send(sendPacket);
			
			//Clear Buffers
			//receiveData = null;
			sendData = null;
			
			//Handle requests from Store
			String temp = new String(receivePacket.getData()); //TODO catch number format exception?
			Long id = Long.parseLong(temp.trim());
			if (content.containsKey(id)) {
				sendData = (id + " " + content.get(id)).getBytes();
			} else {
				sendData = "BAD\n".getBytes();
			}
			
			//Send the Reply to store
			DatagramPacket sendPacket2 = new DatagramPacket (sendData, sendData.length, IPAddress, clientPort);
			serverSocket.send(sendPacket2);
			
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
				System.err.print("Invalid command line arguments for Content\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments for Content\n");
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
		sendData = ("regi,Content,127.0.0.1," + ports[0]).getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,ports[1]);
		
		//Simulate packet Loss
		if (Math.random() >= 0.5) {
			clientSocket.send(sendPacket);
		}
		
		//Set Timeout to 1 sec			
		clientSocket.setSoTimeout(1000);
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
					//Check for GOOD/BAD
					clientSocket.receive(receivePacket);
					String msg2 = new String (receivePacket.getData());
					//Successful registration
					if (msg2.contains("GOOD")) {
						break;
					} else {
						//Failed registration
						System.err.println("Content registration to NameServer failed");
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
			
			if ( i >= 5) {
				System.err.println("Content registration to NameServer failed");
				clientSocket.close();
				System.exit(1);
			}
			i++;
		}
		clientSocket.close();
	}	
	

}
