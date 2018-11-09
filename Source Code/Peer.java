import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *      Class which implements all the functionality of a peer
 */
public class Peer {
    private String directoryPath;       // shared directory path
    private NodeInfo serverInfo;        // tracking server info
    private NodeInfo currentPeerInfo;   // info about this peer

    private Map<NodeInfo, Integer> latencyInfo;     // latency from all other peers
    private Map<String, FileInfo> fileInfoMap;      // Map to collect file info from shared directory
    private RequestListener requestListener;        // Thread to handle incoming connections
    private DirectoryThread directoryThread;        // Thread to send updates to server

    private int numDownloads;                       // Current number of downloads
    private int numUploads;                         // Current number of uploads
    private Map<String, Integer> uploadList;        // Files being uploaded right now
    private static boolean isServerUp;              // Maintains the online status of tracking server

    private double totalDownloadSize;

    public Peer(String directoryPath, NodeInfo serverInfo, int port) {
        try {
            // Get node ip and instantiate current node info
            this.currentPeerInfo = new NodeInfo(InetAddress.getLocalHost().getHostAddress(), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // directory sanity check
        if (!directoryPath.endsWith("/")) {
            directoryPath += "/";
        }
        // create directory if not present
        Utils.checkAndCreateShareDir(directoryPath);
        this.directoryPath = directoryPath;

        // create latencies
        this.latencyInfo = Utils.initializeLatency(currentPeerInfo);

        this.serverInfo = serverInfo;
        this.numDownloads = 0;
        this.numUploads = 0;
        Peer.isServerUp = false;
        this.fileInfoMap = new ConcurrentHashMap<>();
        this.uploadList = new ConcurrentHashMap<>();
        this.totalDownloadSize = 0;

        // Start the TCP server for accepting incoming connections
        this.requestListener = new RequestListener(NodeType.PEER, this, port);
        requestListener.start();

        // Start the directory thread to send directory status
        this.directoryThread = new DirectoryThread(this);
        directoryThread.start();
    }

    public Map<NodeInfo, Integer> getLatencyInfo() {
        return latencyInfo;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public NodeInfo getServerInfo() {
        return serverInfo;
    }

    private synchronized void decDownloadSize(double size){
        this.numDownloads--;
        this.totalDownloadSize -= size;
    }

    private synchronized void incDownloadSize(double size){
        this.numDownloads++;
        this.totalDownloadSize += size;
    }

    public double getTotalDownloadSize() {
        return totalDownloadSize;
    }

    // Decrement number of uploads and remove file name from uploadList
    public synchronized void decUpload(String fileName){
        if (uploadList.containsKey(fileName)) {
            if (uploadList.get(fileName) == 1) {
                uploadList.remove(fileName);
            } else {
                uploadList.put(fileName, uploadList.get(fileName) - 1);
            }
        }
        this.numUploads--;
    }

    // Increment number of uploads and add file name in uploadList
    public synchronized void incUpload(String fileName){
        if (uploadList.containsKey(fileName)) {
            uploadList.put(fileName, uploadList.get(fileName) + 1);
        } else if (!fileName.isEmpty()){
            uploadList.put(fileName, 1);
        }
        this.numUploads++;
    }

    public synchronized int getNumDownloads() {
        return numDownloads;
    }

    public synchronized int getNumUploads() {
        return numUploads;
    }

    public synchronized static void setServerStatus(boolean status){
        Peer.isServerUp = status;
    }

    public synchronized static boolean getServerStatus(){
        return Peer.isServerUp;
    }

    // get the checksum for a file in current peer
    public String getCheckSum(String fileName) {
        FileInfo fileInfo = fileInfoMap.getOrDefault(fileName, null);
        if (fileInfo != null) {
            return fileInfo.getCheckSum();
        } else {
            return "";
        }
    }

    // get the size for a file in current peer
    public int getFileSize(String fileName) {
        FileInfo fileInfo = fileInfoMap.getOrDefault(fileName, null);
        if (fileInfo != null) {
            return fileInfo.getFileSize();
        } else {
            return 0;
        }
    }

    /**
     *      Cost function which adds up all the file sizes which needs to be served
     *      and the number of downloads which are currently happening
     */
    public synchronized double currentLoad() {
        double currLoad = 0;
        // Add all uploads
        for (String upload : uploadList.keySet()) {
            currLoad += (uploadList.get(upload) * getFileSize(upload));
        }
        // Add all download
        currLoad += getTotalDownloadSize();

        int totalRuns = getNumDownloads() + getNumUploads();
        int cores = Runtime.getRuntime().availableProcessors();

        double averageSingleJobCost = (currLoad / totalRuns) * Math.ceil(totalRuns/cores);

        return averageSingleJobCost;
    }

    /**
     *     Get the load, file size and latency from remote peer
     *     use this info to finally calculate cost information for getting file from remote peer
     */
    public LoadInfo getLoad(NodeInfo node, String fileName) {
        try {
            TCPConnection tcpConnection =  new TCPConnection(node);
            Utils.writeString(tcpConnection.getObjectOutputStream(), "GET_LOAD:" + fileName);
            String reply = tcpConnection.getObjectInputStream().readUTF();
            String [] arr = reply.split(":");
            tcpConnection.close();

            double remoteLoad = Double.parseDouble(arr[0]);
            double fileSize = Double.parseDouble(arr[1]);
            int latency = Integer.parseInt(arr[2]);

            return new LoadInfo(remoteLoad, fileSize, latency);

        } catch (IOException e) {
            Utils.print_to_screen(node.toString() + " not reachable");
        }
        return new LoadInfo(Double.MAX_VALUE, 0 , 0);
    }

    /**
     *
     * File a file in whole file system using tracking server
     *
     * @param file_name    file name to find
     * @return  < Set of peers having this file and checksum of file >
     */
    public FindResult find(String file_name) {
        HashSet<NodeInfo> list_of_peers = new HashSet<>();
        String checkSum = "";

        // Add this peer in result if it contains a file in case of any failures
        if (fileInfoMap.containsKey(file_name)){
            list_of_peers.add(this.currentPeerInfo);
            checkSum = getCheckSum(file_name);
        }

        try {
            TCPConnection connection = new TCPConnection(serverInfo);
            Utils.writeString(connection.getObjectOutputStream(), "FIND:" + file_name);
            list_of_peers.addAll((HashSet<NodeInfo>) connection.getObjectInputStream().readObject() );
            checkSum = connection.getObjectInputStream().readUTF();
            connection.close();

            FindResult findResult = new FindResult(list_of_peers, checkSum);
            return findResult;
        } catch (IOException e) {
            // Blocking here to wait for server to come back
            Utils.print_to_screen("Sever Down :: Find :: Waiting for Server to come back!");

            setServerStatus(false);
            // start a heartbeat listener thread
            ServerHeartBeatListener.start(this.serverInfo);
            // Block current thread
            while (getServerStatus() == false) {
                try {
                    Utils.print_to_screen("Find blocked");
                    Thread.sleep(CONSTANTS.SLEEP_PERIOD);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            Utils.print_to_screen("Find :: Server back online");
            // Close the heartbeat thread
            ServerHeartBeatListener.close();

            return find(file_name);     // recursive call to retry !!

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new FindResult(list_of_peers, checkSum);
    }

    /**
     *      Send updated list of files from directory to server
     *      Updates are only sent when a change is detected
     *
     *      ** Download calls this method explicitly **
     *
     */
    public synchronized void updateList() {
        // get all the file names
        Set<String> currFiles = Utils.getFilesList(this.directoryPath);

        // Check if there has been a change
        if (currFiles.size() != fileInfoMap.keySet().size() || !fileInfoMap.keySet().containsAll(currFiles)) {
            try {
                // Remove information of files deleted
                for (String fileName : fileInfoMap.keySet()) {
                    if(!currFiles.contains(fileName)){
                        fileInfoMap.remove(fileName);
                    }
                }

                // Add information of files added
                for (String fileName : currFiles) {
                    if(!fileInfoMap.containsKey(fileName)){
                        String content = Utils.readFile(directoryPath, fileName);
                        if (content == null){
                            continue;
                        }
                        String checksum = findCheckSum(content.getBytes());
                        fileInfoMap.put(fileName, new FileInfo(content.length(), checksum));
                    }
                }

                // Send Update to Tracking Server
                TCPConnection tcpConnection = new TCPConnection(serverInfo);
                String file_list = "";
                // Send file name and check sums to tracking server
                for (String f : fileInfoMap.keySet()) {
                    file_list += (f + "|" + getCheckSum(f) + ",");
                }
                if (file_list.length() > 0) {
                    file_list = file_list.substring(0, file_list.length() - 1);
                }
                Utils.writeString(tcpConnection.getObjectOutputStream(), "UPDATE_LIST:" +
                        currentPeerInfo.toString() + ":" + file_list);

                String reply = tcpConnection.getObjectInputStream().readUTF();
                if (reply.equalsIgnoreCase("OK")) {
                    setServerStatus(true);
                }
                tcpConnection.close();
                Utils.print_to_screen("Updated List of files sent successfully to the server.");
            } catch (IOException e) {
                // Blocking here to wait for server to come back
                Utils.print_to_screen("Sever Down :: UpdateList :: Waiting for Server to come back!");

                setServerStatus(false);
                // start a heartbeat listener thread
                ServerHeartBeatListener.start(this.serverInfo);
                // Block current thread
                while (!getServerStatus()) {
                    try {
                        Utils.print_to_screen("Update list blocked");
                        Thread.sleep(CONSTANTS.SLEEP_PERIOD);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                Utils.print_to_screen("UpdateList :: Server back online");
                // Close the heartbeat thread
                ServerHeartBeatListener.close();

                updateList();        // recursive call to retry !!
            }
        }
    }

    // Comparator function for priority queue to get lowest cost
    private static Comparator<CostPair> costComparator = new Comparator<CostPair>() {
        @Override
        public int compare(CostPair c1, CostPair c2) {
            return (int) (c1.getLoadInfo().getCost() - c2.getLoadInfo().getCost());
        }
    };

    public String downloadFile(String fileName) {
        return downloadFile(fileName, false);
    }

    public String downloadFileNovice(String fileName) {
        return downloadFile(fileName, true);
    }

    /**
     *    Searches and download a file from file system
     *    It retries in case of checksum and peer failure
     *
     * @param fileName      Name of the file to be downloaded
     * @return      File Contents
     */
    private String downloadFile(String fileName, boolean chooseRandom) {

        Set<String> file_names = Utils.getFilesList(this.getDirectoryPath());
        if (file_names.contains(fileName)) {
            System.out.println("File already exists at peer.");
            return Utils.readFile(this.getDirectoryPath(), fileName);
        }

        // Find the peers containing the file
        FindResult findResult = find(fileName);

        Set<NodeInfo> peers = findResult.getNodes();  // Peers having the file

        if(peers.isEmpty()) {
            System.out.println("File does not exist in any of the peers. " +
                    "Please check that the file name is correct.");
            return CONSTANTS.FILE_NOT_EXISTS;
        }

        // Priority queue to get the lowest cost peer always
        Queue<CostPair> queue = null;

        if (!chooseRandom){     // Algorithm based
            queue = new PriorityQueue<>(peers.size() , costComparator);
        } else {                // Simple Novice case
            queue = new LinkedList<>();
        }

        for (NodeInfo node : peers) {
            queue.offer(new CostPair(node, getLoad(node, fileName)));
        }

        // Go thorough the peers by cost order to download the file
        while (!queue.isEmpty()) {
            /** Get the lowest cost peer **/
            CostPair nodePair = queue.poll();
            if (nodePair.getLoadInfo().getCost() == Double.MAX_VALUE) {
                continue;
            }
            NodeInfo node = nodePair.getNode();
            /** Download from peer **/
            FileResult content = downloadFromPeer(fileName, node, nodePair.getLoadInfo().getFileSize(),
                    findResult.getCheckSum() , nodePair.getLoadInfo().getLatency());

            /** Handle failure cases
             * Checksum was invalid and retry from same peer **/
            if (content.getStatus().equalsIgnoreCase(CONSTANTS.CHECK_SUM_INVALID) && nodePair.getRetry() > 0) {
                nodePair.decRetry();
                queue.offer(nodePair);
            }
            /** File does not exist on peer, or the peer is down **/
            else if (content == null || content.getStatus().equalsIgnoreCase(CONSTANTS.FILE_NOT_EXISTS)
                                     || content.getStatus().equalsIgnoreCase(CONSTANTS.PEER_DOWN)) {
                continue;
            } else {
                if (content.getStatus().equalsIgnoreCase(CONSTANTS.CHECK_SUM_INVALID)) {
                    System.out.println("File corrupted in peer : " + node.toString());
                    continue;
                }
                /**
                 *   Got the file content and now write file to disk
                 */
                try {
                    FileLock fileLock = Utils.acquireFileLock(directoryPath + fileName);
                    FileOutputStream fileOutputStream = new FileOutputStream(directoryPath + fileName);
                    fileOutputStream.write(content.getFile());
                    fileOutputStream.close();

                    fileLock.release();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                updateList();   /** Updating the tracking server that peer has the file now **/

                System.out.println("File successfully downloaded. " +
                        "Please check in \" " + getDirectoryPath() +" \"");
                return new String(content.getFile());
            }
        }
        System.out.println("File not found in any of the peers although it seems to exist in some peer.");
        return CONSTANTS.FILE_NOT_FOUND;
    }

    /**
     *    Download file from a specific peer and validate the fileCheckSum
     */
    private FileResult downloadFromPeer(String fileName, NodeInfo node, double fileSize, String fileCheckSum, int latency) {
        incDownloadSize(fileSize);  // Increase num of downloads
        try {
            Thread.sleep(latency);        // Emulating latency

            // Download file
            TCPConnection tcpConnection =  new TCPConnection(node);

            Utils.writeString(tcpConnection.getObjectOutputStream(), "DOWNLOAD:" + fileName);

            // Streaming file from peer
            int len = tcpConnection.getObjectInputStream().readInt();
            //String content = "";
            byte[] content = new byte[len];
            byte[] data = new byte[10];
            int totalRead = 0;
            int remain = len;
            int read_now = 0;
            int count = 0;
            StringBuilder builder = new StringBuilder(len);
            while ((read_now =
                    tcpConnection.getObjectInputStream().read(data, 0, Math.min(data.length, remain))) > 0){
                count++;
                totalRead += read_now;
                remain -= read_now;
                //builder.append(new String(data));
                System.arraycopy(data, 0, content,totalRead - read_now ,read_now);
                if (count % 100 == 0) {
                    Utils.print_to_screen("Total Read Bytes :: " + totalRead);
                }
            }
            tcpConnection.close();

            if (remain > 0) {
                return new FileResult(null, CONSTANTS.PEER_DOWN);
            }

            // validate checksum
            boolean isCheckSumValid = validateCheckSum(fileCheckSum, content);
            if (!isCheckSumValid) {
                return new FileResult(null, CONSTANTS.CHECK_SUM_INVALID);
            }
            return new FileResult(content, CONSTANTS.OK);
        } catch (IOException e) {
            Utils.print_to_screen("Peer disconnected in between.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        decDownloadSize(fileSize);  // Decrease num of downloads
        return new FileResult(null, CONSTANTS.PEER_DOWN);
    }

    // Read and send file data back
    public String upload(ObjectOutputStream outputStream, String fileName) {
        String content = Utils.readFile(directoryPath, fileName);
        if (content == null){
            content = CONSTANTS.FILE_NOT_EXISTS;
        }
        byte[] data = content.getBytes();
        int len = content.length();
        try {
            Utils.writeInt(outputStream, len);
            Utils.writeBytes(outputStream, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    // generate and validate checksum of data
    private boolean validateCheckSum(String fileCheckSum, byte[] content) {
        String checkSum = findCheckSum(content);
        if(fileCheckSum.equals(checkSum)) {
            return true;
        } else {
            return false;
        }
    }

    // Get MD5 checksum of data
    private String findCheckSum(byte[] data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(data);
            byte[] digest = md.digest();
            String checksum = DatatypeConverter.printHexBinary(digest).toUpperCase();

            return checksum;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}