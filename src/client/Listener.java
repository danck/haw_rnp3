package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Level;

import lib.FCpacket;

/**
 * This class defines listeners to server-replies and passes ACKs to the
 * packet-container
 * 
 * @author m215025
 * 
 */
public class Listener extends Thread {

    /** Socket to listen to server ACKs */
    private DatagramSocket sock;
    
    /** container that holds the set of "working packets" */
    private BoundedFCPBuffer container;
    
    private FileCopyClient fcc;

    /**
     * Constructor
     * @param container
     * @param dsock
     */
    protected Listener(BoundedFCPBuffer container, DatagramSocket dsock, FileCopyClient fcc) {
        this.container = container;
        this.sock = dsock;
        this.fcc = fcc;
    }

    public void run() {
        byte[] receiveData = new byte[FileCopyClient.UDP_PACKET_SIZE];
        boolean needsMoreACKs = true;
        // sock.setSoTimeout(0);
        try {
            while (needsMoreACKs) {
                DatagramPacket p = new DatagramPacket(receiveData,
                        FileCopyClient.UDP_PACKET_SIZE);
                sock.receive(p);
                FCpacket fcp = new FCpacket(p.getData(), p.getLength());
                fcc.computeTimeoutValue(System.nanoTime() - fcp.getTimestamp());
                FileCopyClient.logger.log(Level.INFO, "<-- ACK no. " + fcp.getSeqNum());
                needsMoreACKs = container.setACK(fcp.getSeqNum());
            }
        } catch (IOException e) {
            FileCopyClient.logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Listener: All acknowledged");
    }
}