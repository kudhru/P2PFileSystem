import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Used by server and peers to listen to incoming requests and start handler thread
 */
public class RequestListener extends Thread {
    private ServerSocket serverSocket = null;
    private int listenerPort;
    private Object node;
    NodeType nodeType;
    public RequestListener(NodeType nodeType, Object node, int listenerPort) {
        this.nodeType = nodeType;
        this.node = node;
        this.listenerPort = listenerPort;
    }

    @Override
    public void run() {
        // Server port listening to request
        try {
            serverSocket =  new ServerSocket(listenerPort, CONSTANTS.SOCKET_BACKLOG);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            // Start a new thread to handle this request
            while (true) {
                Socket socket = serverSocket.accept();
                
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                if(this.nodeType == NodeType.SERVER) {
                    new ServerRequestHandler((Server)node, socket , inputStream , outputStream).start();
                }
                else {
                    new PeerRequestHandler((Peer)node, socket , inputStream , outputStream).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}