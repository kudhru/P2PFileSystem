import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 *      TCP connection class
 */
public class TCPConnection {

    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public TCPConnection(NodeInfo server) throws IOException {
        this.socket = new Socket(InetAddress.getByName(server.getIP()), server.getPort());
        this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        this.objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectInputStream getObjectInputStream() {
        return objectInputStream;
    }

    public ObjectOutputStream getObjectOutputStream() {
        return objectOutputStream;
    }

    public void close() throws IOException{
        // Exiting the connection and closing the connection
        Utils.writeString(this.objectOutputStream, "Exit");
        this.objectInputStream.close();
        this.objectOutputStream.close();
        this.socket.close();
    }
}