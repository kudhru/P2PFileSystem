import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;

/**
 * 	Driver code to switch communication and control flow
 *
 *
 * 	Implements system communication protocol
 *
 */
public class PeerRequestHandler extends Thread {
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;
    private Peer peer;
    final Socket socket;

    public PeerRequestHandler(Peer peer, Socket socket, ObjectInputStream inputStream,
                              ObjectOutputStream outputStream) {
        this.peer = peer;
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
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

                String[] action = received.split(":");
                String function_name = action[0];
                switch (function_name.toUpperCase()) {
                    case "DOWNLOAD":    // DOWNLOAD:FILE_NAME
                        peer.incUpload(action[1]);      // Increase num of uploads
                        peer.upload(outputStream, action[1]);
                        peer.decUpload(action[1]);      // Decrease num of uploads
                        break;

                    case "GET_LOAD":    // GET_LOAD:FILE_NAME   ** OR **  GET_LOAD
                        /** Send current load on peer along with file size and latency to peer **/
                        String fileName = action[1];    // By default empty
                        String load = "" + peer.currentLoad();
                        load += ":" + peer.getFileSize(fileName);
                        load += (":" + peer.getLatencyInfo().getOrDefault(
                                new NodeInfo(socket.getInetAddress().getHostAddress(),
                                        socket.getPort()),0));
                        Utils.writeString(outputStream, load);
                        break;

                    case "SEND_INFO":
                        /** Send directory information **/
                        Set<String> sharedFiles = Utils.getFilesList(peer.getDirectoryPath());
                        String file_list = "";
                        for (String f : sharedFiles) {
                            file_list += (f + "|" + peer.getCheckSum(f) + ",");
                        }
                        if (file_list.length() > 0) {
                            file_list = file_list.substring(0, file_list.length() - 1);
                        }
                        Utils.writeString(outputStream, file_list);
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