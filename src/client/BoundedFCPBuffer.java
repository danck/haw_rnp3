package client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;

import lib.FCpacket;

public class BoundedFCPBuffer {

    /** lowest not-yet-acknowledged packet number */
    private long sendBase = 0;
    
    /** maximal number of packages allowed to be sent before the lowest one has been acknowledged */
    private final Integer WINDOW_SIZE;
    
    /** internal representation of the packet buffer */
    private Map<Long, FCpacket> buffer;

    public BoundedFCPBuffer(String path, Integer windowSize) {
        WINDOW_SIZE = windowSize;
        buffer = new LinkedHashMap<Long, FCpacket>(WINDOW_SIZE);
    }
    
    /**
     * Initialize buffer with data from the given source
     */
    public void initialize() {
        
    }

    /**
     * return next slice of the source data, wrapped in an FCpacket<br/>
     * @return <code>FCpacket</code> with subsequent number
     *  or <code>null</code> of all data has been returned
     */
    public synchronized FCpacket nextPacket() {
        // TODO Auto-generated method stub

        while (sendBase == sendBase + WINDOW_SIZE) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted during wait...");
                return null;
            }
        }
        return null;
    }

    protected void setACK(long seqNum) {
        // TODO
    }

    private void loadNextChunk() {
        // TODO
        // create new FCpackets from FileInputStream
        // advance
    }

    private void refreshWindow() {
        // TODO
        // check if the window can be advanced
        // if yes, load next chunk, handle semaphores
    }
}