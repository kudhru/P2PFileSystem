import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 	Driver code to switch communication and control flow
 *
 *
 * 	Implements system communication protocol
 *
 */
public class ServerRequestHandler extends Thread {
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;
    final Socket socket;
    Server server;

    public ServerRequestHandler(Server server, Socket socket, ObjectInputStream inputStream,
                          ObjectOutputStream outputStream) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.server = server;
    }

    @Override
    public void run() {
        String received = "";
        while (true) {
            try {
                // read from input stream
                received = inputStream.readUTF();
                Utils.print_to_screen(String.format("Request Received ## %s", received));

                if (received.equalsIgnoreCase("Exit")) {
                    break;
                }
                String[] action = received.split(":", -1);
                String function_name = action[0];
                switch (function_name.toUpperCase()) {
                    //FIND:FILE_NAME
                    case "FIND":
                        Set<NodeInfo> peers_list = this.server.find(action[1]);
                        Utils.writeObject(outputStream, peers_list);
                        Utils.writeString(outputStream, server.getCheckSum(action[1]));
                        break;

                    //UPDATE_LIST:IP:PORT:file1|checkSum1,file2|checkSum2,file3|checkSum3 ....
                    case "UPDATE_LIST":
                        NodeInfo remoteNodeInfo = new NodeInfo(action[1], Integer.parseInt(action[2]));
                        Set<String> files = new HashSet<>(Arrays.asList(action[3].split(",")));
                        this.server.updateList(remoteNodeInfo, files);
                        Utils.writeString(outputStream,"OK");
                        break;

                    case "PING":
                        Utils.writeString(outputStream,"PING");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // close connections
        try {
            this.inputStream.close();
            this.outputStream.close();
            this.socket.close();
        } catch (IOException e) { }
    }
}