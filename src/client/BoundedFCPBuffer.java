package client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import lib.FCpacket;

public class BoundedFCPBuffer {

    // Counters
    /** lowest not-yet-acknowledged packet number */
    private long sendBase = 0;
    /** FCpacket sequence counter - corresponds with number of packets that have been read into the buffer */
    private long packetCount = 0;
    private long ackCount = 0;
    /** Packets above these index are yet to be sent */
    private long sentOnceIndex = 0;

    /**
     * maximal number of packages allowed to be sent before the lowest one has
     * been acknowledged
     */
    private final Integer WINDOW_SIZE;

    /** Stream from source file */
    private FileInputStream fis;
    
    /** indicates that source data is exhausted */
    private boolean sourceExhausted = false;

    /** internal representation of the packet buffer */
    private Map<Long, FCpacket> buffer;

    public BoundedFCPBuffer(String path, Integer windowSize) {
        WINDOW_SIZE = windowSize;
        buffer = new LinkedHashMap<Long, FCpacket>(WINDOW_SIZE);
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            FileCopyClient.logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize buffer with data from the given source
     */
    public void initialize() {
        buffer.put(0L, new FCpacket(0L, new byte[0], 0)); // Add dummy for control
                                                   // packet
        packetCount++;
        sentOnceIndex++;
        loadNextChunks();
    }

    /**
     * return next slice of the source data, wrapped in an FCpacket<br/>
     * 
     * @return <code>FCpacket</code> with subsequent number or <code>null</code>
     *         of all data has been returned
     */
    public synchronized FCpacket nextPacket() {
        while (sentOnceIndex == sendBase + WINDOW_SIZE) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted during wait...");
                return null;
            }
        }
        FCpacket ret = buffer.get(sentOnceIndex++);
        notify();
        return ret;
    }
    
    public FCpacket get(long seqNumber) {
        return buffer.get(seqNumber);
    }

    /**
     * Set the FCpacket with the given sequence number to acknowledged
     * 
     * @param seqNum
     * @return false if all packets have been acknowledged
     */
    public boolean setACK(long seqNum) {
        buffer.get(seqNum).setValidACK(true);
        ackCount++;
        System.out.println("ackCount: " +ackCount+ "packetCount: " + packetCount);
        if (refreshWindow()) // implicitly adjusting window position
            return true;
        else {
            if (ackCount < packetCount)
                return true;
            else
                return false;
        }
    }

    /**
     * Adjusts the current window to newly acknowledged packets
     * 
     * @return false if the window has reached the end of the file
     */
    private synchronized boolean refreshWindow() {
        while ((sendBase < packetCount) && buffer.get(sendBase).isValidACK()) {
            FCpacket removed = buffer.remove(sendBase);
            FileCopyClient.logger.log(Level.INFO, "Chunk "+ removed.getSeqNum() +" removed from buffer");
            sendBase++;
        }
        notifyAll();
        return loadNextChunks();
    }

    /**
     * Fills the buffer with as many allowed or available data chunks (
     * <code>FCpackets</code>) as possible.
     * 
     * @return true if there is still more available <br/>
     *         false if the input data is exhausted
     * @throws IOException
     */
    private boolean loadNextChunks() {
        FCpacket fcp;
        try {
            int bytesRead;
            byte[] bytes;
            bytes = new byte[FileCopyClient.UDP_PACKET_SIZE];

            while ((packetCount < (sendBase + WINDOW_SIZE))) {
                bytesRead = fis.read(bytes, 0, bytes.length);

                // Catch EOF
                if (bytesRead == -1)
                    return false;

                // Add new packet
                fcp = new FCpacket(packetCount, bytes, bytesRead);
                buffer.put(packetCount, fcp);
                FileCopyClient.logger.log(Level.INFO, "Chunk "+ fcp.getSeqNum() +" loaded to buffer");
                
                // Update counter
                packetCount++;
                if (bytesRead < FileCopyClient.UDP_PACKET_SIZE)
                    return false;
            }
        } catch (IOException e) {
            FileCopyClient.logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}