import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *    Tracking server class and terminal for same
 */
public class Server {
    // Map to store file name to peer list
    private ConcurrentHashMap<String, Set<NodeInfo>> files_servers_map;

    // Map to store checkSum for each file
    private Map<String, String> checkSumMap;

    private RequestListener requestListener;

    public Server(int port) {
        this.files_servers_map = new ConcurrentHashMap<>();
        this.checkSumMap = new ConcurrentHashMap<>();

        // Start the TCP server for accepting incoming connections
        this.requestListener = new RequestListener(NodeType.SERVER, this, port);
        requestListener.start();

        // On start regenerate soft state by asking all peers
        getInfoFromNodes();
    }

    // Getting directory information from all peers and constructing soft state
    private void getInfoFromNodes() {
        List<NodeInfo> nodeInfoList = Utils.getPeerListFromProperties("IP.properties");
        for (NodeInfo nodeInfo : nodeInfoList) {
            try {
                TCPConnection connection = new TCPConnection(nodeInfo);
                Utils.writeString(connection.getObjectOutputStream(), "SEND_INFO");
                String received = connection.getObjectInputStream().readUTF();
                if (received.length() > 0){
                    Set<String> files = new HashSet<>(Arrays.asList(received.split(",")));
                    updateList(nodeInfo, files);        // Update state from this peer
                }
                connection.close();
            } catch (IOException e) {
                //e.printStackTrace();  // No issues if a peer is down
                System.out.println(CONSTANTS.PEER_DOWN + " : "+ nodeInfo.toString());
            }
        }
    }

    // returns set of all peers having this file
    public Set<NodeInfo> find(String file_name) {
        if(files_servers_map.containsKey(file_name)) {
            return files_servers_map.get(file_name);
        }
        return new HashSet<>();
    }

    // Gives checksum for a specific file
    public String getCheckSum(String fileName) {
        return checkSumMap.getOrDefault(fileName, "");
    }

    // Update list of files currently on a peer and remove stale entries
    public synchronized void updateList(NodeInfo peerInfo, Set<String> files) {
        Set<String> file_names = new HashSet<>();

        // Adding new files from peer
        for(String file : files) {
            String[] temp = file.split("\\|");
            String file_name = temp[0];
            String checkSum =  temp[1];
            file_names.add(file_name);

            // add peer-file mapping
            if (file_name == null || file_name.isEmpty()) {
                continue;
            } else if(!files_servers_map.containsKey(file_name)) {
                files_servers_map.put(file_name, new HashSet<>());
            }
            files_servers_map.get(file_name).add(peerInfo);

            // add checksum to map
            if(!checkSumMap.containsKey(file_name)) {
                checkSumMap.put(file_name, checkSum);
            }
        }

        // Removing stale files / removed files from peer
        for (String key : files_servers_map.keySet()) {
            if (!file_names.contains(key)) {
                files_servers_map.get(key).remove(peerInfo);
                if(files_servers_map.get(key).isEmpty()) {
                    files_servers_map.remove(key);
                    checkSumMap.remove(key);
                }
            }
        }
    }

    // Prints the full server mapping on screen
    public synchronized void printList(){
        System.out.println("Printing Server File Map");
        for(String key : files_servers_map.keySet()) {
            System.out.print(key + "\t");
            for (NodeInfo nodeInfo : files_servers_map.get(key)) {
                System.out.print(nodeInfo.toString() + "  ");
            }
            System.out.println();
        }
        System.out.println("-------------------------------------------------");
    }

    // Main Driver for server
    public static void main(String[] args) {
        NodeInfo serverInfo = Utils.getTrackingServerInfo();
        Server server = new Server(serverInfo.getPort());
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Type ' print ' for printing server map or ' stop ' to end the server");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("Stop")) {
                System.exit(0);
            } else if (input.equalsIgnoreCase("Print")){
                server.printList();
            }
        }
    }
}