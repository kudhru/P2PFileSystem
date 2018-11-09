/**
 *      Thread class which periodically reads shared directory information
 *      and sends  << required >> updates to server
 */
public class DirectoryThread extends Thread{
    private Peer peer;
    private volatile boolean done;

    public DirectoryThread(Peer peer) {
        this.peer = peer;
        this.done = false;
    }

    public synchronized void close() { this.done = true; }

    @Override
    public void run() {
        try {
            while (!done) {
                peer.updateList();
                Thread.sleep(CONSTANTS.UPDATE_PERIOD);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}