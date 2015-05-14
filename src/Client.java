/**
 * 
 * @author Sam Damen (42636678)
 * Client
 * COMS3200
 * Assignment 2
 * 
 * Inputs = Request NSPort
 *
 * Simulate packet loss on communication to the NameServer and Store
 *
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Client {

	public static void main(String[] args) throws Exception {
		
		int[] commands = commandParse(args);
		String[] store = null;
		
		//Get Store Info from NS
		try {
			store = lookupServer(commands[1]);
		} catch (Exception e) {
			System.err.print("Client unable to communicate with NameServer");
		}
		
		
		//Send Requests to the Store
		//buffers
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddr = InetAddress.getByName(store[1]);			
		DatagramPacket receivePacket = new DatagramPacket( receiveData,receiveData.length, IPAddr, Integer.parseInt(store[2]) );
		
		
		//Send requests to Store
		if (commands[0] > 0) {
			sendData = (Integer.toString(commands[0]) + " 3200456712304478").getBytes(); //Command number + credit card
		} else {
			sendData = Integer.toString(commands[0]).getBytes();
		}

		DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,Integer.parseInt(store[2]));
		
		//Simulate Packet Loss
		if (Math.random() >= 0.5) {
			clientSocket.send(sendPacket);
		}
		
		//Set Timeout to 2 sec			
		clientSocket.setSoTimeout(2000);
		int i = 0;
		
		//Try to receive ACK, will continue to loop until 3 tries
		// 3 failed sending attempts is regarded as "could not connect"
		while (true) {
			
			try {
				clientSocket.receive(receivePacket);
				//Check for ACK
				String msg = new String (receivePacket.getData());
				//System.out.println(msg);
				if ( msg.contains("ACK") ) {
					//Receive & print All data until "connection closed"					
					String msg2 = null;
					while ( ! msg2.contains("DONE") ) {
						
						clientSocket.receive(receivePacket);
						msg2 = new String (receivePacket.getData());	
						
						System.out.println(msg2);						
					}					
				}
			} catch (SocketTimeoutException e) {
				//Did not receive the packet, re-send
				//Simulate packet Loss
				if (Math.random() >= 0.5) {
					clientSocket.send(sendPacket);
				} 					
			}
			
			if ( i >= 2) {
				System.err.println("Client unable to Connect with Store");
				clientSocket.close();
				System.exit(1);
			}
			i++;
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
		
		if(args.length == 2) {
			try {
				ports[0] = Integer.parseInt(args[0]);
				ports[1] = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.print("Invalid command line arguments\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments\n");
			System.exit(1);
		}
		return ports;
	}
	
	
	//Get Store information from the NameServer
	private static String[] lookupServer(int port) throws Exception {
		
		//buffers
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddr = InetAddress.getByName("127.0.0.1");			
		DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length, IPAddr, port);
		
		//Send lookup request to NameServer
		sendData = ("look,Store").getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,port);
		
		//Simulate packet Loss
		if (Math.random() >= 0.5) {
			clientSocket.send(sendPacket);
		}
		
		//Set Timeout to 2 sec			
		clientSocket.setSoTimeout(2000);
		int i = 0;
		
		//Try to receive ACK, will continue to loop until 3 tries
		// 3 failed sending attempts is regarded as "could not connect"
		while (true) {
			
			try {
				clientSocket.receive(receivePacket);
				//Check for ACK
				String msg = new String (receivePacket.getData());
				//System.out.println(msg);
				if ( msg.contains("ACK") ) {
					//Check for Error or Store Info
					clientSocket.receive(receivePacket);
					String[] msg2 = new String (receivePacket.getData()).split(",");
					//Successful Lookup
					if (msg2[0].contains("Error")) {
						//Failed to find Store on NS
						System.out.println("Store has not registered");
						clientSocket.close();
						System.exit(1);
					} else {
						return msg2;
					}
					
					
				}
			} catch (SocketTimeoutException e) {
				//Did not receive the packet, re-send
				//System.out.println("Resend");
				//Simulate packet Loss
				if (Math.random() >= 0.5) {
					clientSocket.send(sendPacket);
				} 					
			}
			
			if ( i >= 2) {
				System.err.println("Client unable to connect with NameServer");
				clientSocket.close();
				System.exit(1);
			}
			i++;
		}
		
	}
	

}
