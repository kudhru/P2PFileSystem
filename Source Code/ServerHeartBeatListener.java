import java.io.IOException;

/**
 *  Singleton Thread to check status of tracking server
 *  and is run only when find / updateList needs to block
 */
public class ServerHeartBeatListener extends Thread{

    private static ServerHeartBeatListener serverHeartBeatListener = null;
    private NodeInfo server;
    private static volatile boolean done;

    private ServerHeartBeatListener(NodeInfo server) {
        this.server = server;
        this.done = false;
    }

    public static synchronized void start(NodeInfo server){
        if(serverHeartBeatListener == null) {
            serverHeartBeatListener = new ServerHeartBeatListener(server);
            serverHeartBeatListener.start();
        }
    }

    public static synchronized void close(){
        ServerHeartBeatListener.done = true;
        serverHeartBeatListener = null;
    }

    @Override
    public void run() {
        while (!done) {
            Utils.print_to_screen("Trying to connect to tracking server");
            try {
                TCPConnection connection = new TCPConnection(server);
                Utils.writeString(connection.getObjectOutputStream(), "PING");
                String reply = connection.getObjectInputStream().readUTF();
                if (reply.equalsIgnoreCase("PING")) {
                    Utils.print_to_screen("Tracking Server is back online");
                    done = true;
                    Peer.setServerStatus(true);
                }
                connection.close();
            }  catch (IOException e) {
                try {
                    Peer.setServerStatus(false);
                    Thread.sleep(CONSTANTS.UPDATE_PERIOD);     // Need to retry again
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}