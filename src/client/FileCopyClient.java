package client;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.*;
import java.net.*;
import java.util.LinkedList;

import lib.FC_Timer;
import lib.FCpacket;

public class FileCopyClient extends Thread {

	// -------- Constants
	public final static boolean TEST_OUTPUT_MODE = true;

	public final int SERVER_PORT = 23000;

	public final static int UDP_PACKET_SIZE = 1008;

	// -------- Public parms
	public String servername;

	public String sourcePath;

	public String destPath;

	public int windowSize;

	public long serverErrorRate;

	// -------- Variables
	// current default timeout in nanoseconds
	private long timeoutValue = 100000000L;

	// current estimated round trip time in nanoseconds
	private long estimatedRTT = 0L;

	// deviation of current round trip time estimation
	private long deviation = 0L;

	// -------- Socket structures
	private DatagramSocket clientSocket;
	private int artificialDelay = 1; //milliseconds 

	// -------- Streams
	private FileInputStream inFromFile;

	// Protocol variables
	public long sendBase;
	
	// Listens for server replies
	private Thread listener;
	
	// Holds and manages FCpackets
	private BoundedFCPBuffer container;

	// Test error production
	private long sentPacketCounter;

	// statistics
	public int numberTimeouts = 0;

	// Constructor
	public FileCopyClient(String serverArg, String sourcePathArg,
			String destPathArg, String windowSizeArg, String errorRateArg) {
		servername        = serverArg;
		sourcePath        = sourcePathArg;
		destPath          = destPathArg;
		windowSize        = Integer.parseInt(windowSizeArg);
		serverErrorRate   = Long.parseLong(errorRateArg);
		container         = new BoundedFCPBuffer(sourcePath, SERVER_PORT);
		listener          = new Listener(container, clientSocket);
	}

	public void runFileCopyClient() {
	    
	    
		try {
			clientSocket
					.connect(InetAddress.getByName(servername), SERVER_PORT);
			testOut("Connected to " + servername);
		} catch (UnknownHostException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} finally {
			clientSocket.close();
		}
		
		container.initialize();
		listener.start();

		FCpacket packet = container.nextPacket();
		while(packet != null ) {
		    sendPackage(packet);
		    packet = container.nextPacket();
		}

		// wait for listener to receive ACKs for every packet
		try {
            listener.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

	/**
	 * 
	 * Timer Operations
	 */
	public void startTimer(FCpacket packet) {
	    /* Create, save and start timer for the given FCpacket */
		FC_Timer timer = new FC_Timer(timeoutValue, this, packet.getSeqNum());
		packet.setTimer(timer);
		timer.start();
	}

	public void cancelTimer(FCpacket packet) {
		/* Cancel timer for the given FCpacket */
		testOut("Cancel Timer for packet" + packet.getSeqNum());

		if (packet.getTimer() != null) {
			packet.getTimer().interrupt();
		}
	}

	/**
	 * Implementation specific task performed at timeout
	 */
	public void timeoutTask(long seqNum) {
		numberTimeouts++;
		// TODO resend seqNum
	}

	/**
	 * 
	 * Computes the current timeout value (in nanoseconds)
	 */
	public void computeTimeoutValue(long sampleRTT) {
		// Kap. 3 Folie 56
		double x = 0.1;
		this.estimatedRTT = (long) ((1 - x) * estimatedRTT + x * sampleRTT);
		this.deviation = (long) ((1 - x) * deviation + Math.abs(sampleRTT
				- estimatedRTT));
		this.timeoutValue = estimatedRTT + 4 * deviation;
	}

	/**
	 * 
	 * Return value: FCPacket with (0 destPath;windowSize;errorRate)
	 */
	public FCpacket makeControlPacket() {
		/*
		 * Create first packet with seq num 0. Return value: FCPacket with (0
		 * destPath ; windowSize ; errorRate)
		 */
		String sendString = destPath + ";" + windowSize + ";" + serverErrorRate;
		byte[] sendData = null;
		try {
			sendData = sendString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return new FCpacket(0, sendData, sendData.length);
	}

	public void testOut(String out) {
		if (TEST_OUTPUT_MODE) {
			System.err.printf("%,d %s: %s\n", System.nanoTime(), Thread
					.currentThread().getName(), out);
		}
	}
	
    private void sendPackage(FCpacket fcp) {
        fcp.setTimestamp(System.nanoTime());
        fcp.setTimer(new FC_Timer(timeoutValue, this, fcp.getSeqNum()));
        DatagramPacket dp = new DatagramPacket(fcp.getData(), fcp.getLen());
        try {
            clientSocket.send(dp);
            startTimer(fcp);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

	public static void main(String argv[]) throws Exception {
		FileCopyClient myClient = new FileCopyClient(argv[0], argv[1], argv[2],
				argv[3], argv[4]);
		myClient.runFileCopyClient();
	}

}
