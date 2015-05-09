import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * 
 * @author Sam Damen (42636678)
 * NameServer
 * COMS3200
 * Assignment 2
 *
 * Only simulate packet loss on send of data, not ACKs of said data
 *
 */


public class NameServer {
	
	
	public static void main(String[] args) throws Exception {
		
		
		int port = commandParse(args);
		
		//Buffers
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];
		
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
			
			//Block while Receive packet
			DatagramPacket receivePacket= new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			
			//Send ACK to received packet
			InetAddress IPAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			DatagramPacket sendPacket = new DatagramPacket (sendData, sendData.length, IPAddress, clientPort);
			serverSocket.send(sendPacket);
			
			//Parse the sent message
			
			
		}

		

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

}
