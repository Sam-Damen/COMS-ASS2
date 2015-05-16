/**
 * 
 * @author Sam Damen (42636678)
 * Bank
 * COMS3200
 * Assignment 2
 * 
 * Inputs = BankPort NSPort
 *
 * Simulate packet loss when communicating with Nameserver
 *
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class Bank {

	public static void main(String[] args) throws Exception {
		
		int[] ports = commandParse(args);
		
		
		//Start up Server functionality
		//Create Datagram Socket
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(ports[0]);
			//Successfully can listen on port
			System.err.print("Bank waiting for incoming connections ...\n");
		} catch (SocketException e) {
			System.err.format("Cannot listen on given port number %d\n", ports[0]);
			System.exit(1);
		}	
		
		//Try to Register with the nameServer
		registerNS(ports);

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
			receiveData = null;
			sendData = null;
						
			// parse the message from Store
			String id = new String(receivePacket.getData()).substring(0, 10);
			try {
				long num = Long.parseLong(id);
				
				if ( (num & 1) == 0) {
					sendData = "1\n".getBytes();
					System.out.format("%d OK\n", num);
				} else {
					sendData = "0\n".getBytes();
					System.out.format("%d NOT OK\n", num);
				}
			} catch (NumberFormatException e) {
				sendData = "0\n".getBytes();
				System.out.println("Wrong format for id");
			}
			
			//Send the Reply to store
			DatagramPacket sendPacket2 = new DatagramPacket (sendData, sendData.length, IPAddress, clientPort);
			serverSocket.send(sendPacket2);
		}

	}
	
	
//*******************************************************************
//
//						        Extra Methods
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
					System.err.print("Invalid command line arguments for Bank\n");
					System.exit(1);
				}			
			} else {
				System.err.print("Invalid command line arguments for Bank\n");
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
			sendData = ("regi,Bank,127.0.0.1," + ports[0]).getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,ports[1]);
			
			//Simulate packet Loss
			if (Math.random() >= 0.5) {
				clientSocket.send(sendPacket);
			}
			
			//Set Timeout to 2 sec			
			clientSocket.setSoTimeout(2000);
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
							System.err.println("Bank registration to NameServer failed");
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
					System.err.println("Bank registration to NameServer failed");
					clientSocket.close();
					System.exit(1);
				}
				i++;
			}
			clientSocket.close();
		}		
		
		
		/*
		ReSendPacket rs = new ReSendPacket(sendData);
		new Thread(rs).start();
		*/
/*		
		//Thread to re-send packets		
		static class ReSendPacket implements Runnable {
			
			private byte[] sendData;
			
			public ReSendPacket(byte[] data) {
				this.sendData = data;
			}
			
			public void run() {
				
				//DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddr,ports[1]);
				
				//Simulate packet Loss
				if (Math.random() >= 0.5) {
					//clientSocket.send(sendPacket);
				}
			}
		}
*/

}
