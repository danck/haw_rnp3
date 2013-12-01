package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

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

    /**
     * Constructor
     * @param container
     * @param dsock
     */
    protected Listener(BoundedFCPBuffer container, DatagramSocket dsock) {
        this.container = container;
        this.sock = dsock;
    }

    public void run() {
        byte[] receiveData = new byte[FileCopyClient.UDP_PACKET_SIZE];
        // sock.setSoTimeout(0);
        try {
            while (true) {
                DatagramPacket p = new DatagramPacket(receiveData,
                        FileCopyClient.UDP_PACKET_SIZE);
                sock.receive(p);
                FCpacket fcp = new FCpacket(p.getData(), p.getLength());
                container.setACK(fcp.getSeqNum());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}