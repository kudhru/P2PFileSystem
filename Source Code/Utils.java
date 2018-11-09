import java.io.*;
import java.net.InetAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *      Utils class to implement all the common functionality
 */
public class Utils {

    // Logging function for system related messages
    public static void print_to_screen(String string) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        System.out.println(String.format(
                "%s %s: %s",
                dateFormat.format(date),
                Thread.currentThread().getName(),
                string
        ));
    }

    public static void writeString(ObjectOutputStream outputStream,
                                   String content) throws IOException {
        outputStream.writeUTF(content);
        outputStream.flush();
    }

    public static void writeObject(ObjectOutputStream outputStream,
                                   Object content) throws IOException {
        outputStream.writeObject(content);
        outputStream.flush();
    }

    public static void writeBytes(ObjectOutputStream outputStream, byte[] data) throws IOException {
        outputStream.write(data);
        outputStream.flush();
    }

    public static void writeInt(ObjectOutputStream outputStream, int num) throws IOException {
        outputStream.writeInt(num);
        outputStream.flush();
    }

    // Gets all the file names in a directory
    public static Set<String> getFilesList(String path) {
        Set<String> files = new HashSet<>();
        File directory = new File(path);
        for (final File fileEntry : directory.listFiles()) {
            files.add(fileEntry.getName());
        }
        return files;
    }

    // Reads a file and returns the content
    public static String readFile(String path, String fileName) {
        try {
            FileLock fileLock = Utils.acquireFileLock(path + fileName);

            File file = new File(path + fileName);
            FileInputStream inputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            inputStream.read(data);
            inputStream.close();

            fileLock.release();

            String content = new String(data, "UTF-8");
            return content;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Reads the properties file and
    // returns the information about all the nodes in file
    public static List<NodeInfo>  getPeerListFromProperties(String name) {
        List<NodeInfo> peerInfoList = new ArrayList<>();
        try {
            File file = new File(name);
            FileInputStream fileInput = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(fileInput);
            fileInput.close();

            Enumeration enuKeys = properties.keys();
            while (enuKeys.hasMoreElements()) {
                String key = (String) enuKeys.nextElement();
                String[] values = properties.getProperty(key).split(":");
                String nodeName = values[0];
                if (nodeName.equalsIgnoreCase("localhost")) {
                    nodeName = InetAddress.getLocalHost().getHostAddress();
                } else {
                    nodeName = InetAddress.getByName(nodeName).getHostAddress();
                }
                NodeInfo peerInfo = new NodeInfo(nodeName, Integer.parseInt(values[1]));
                peerInfoList.add(peerInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return peerInfoList;
    }

    // Reads configuration file and gets tracking server info
    public static NodeInfo getTrackingServerInfo() {
        List<NodeInfo> list = getPeerListFromProperties("SERVER.properties");
        if (!list.isEmpty()) {
            return list.get(0);
        } else  {
            return null;
        }
    }

    // For the current peer, randomly initialize latencies from this peer to all other peers
    // ** We are assuming non symmetric latencies **
    public static Map<NodeInfo, Integer> initializeLatency(NodeInfo current_peer) {
        HashMap<NodeInfo, Integer> latency_map = new HashMap<>();
        final int MIN_DELAY = 100;
        final int MAX_DELAY = 5000;
        List<NodeInfo>  peers_list = getPeerListFromProperties("IP.properties");
        Random rand = new Random();
        for(NodeInfo peer : peers_list) {
            if(peer.equals(current_peer)) {
                latency_map.put(peer, 0);
            } else {
                latency_map.put(peer, rand.nextInt(MAX_DELAY) + MIN_DELAY);
            }
        }
        return latency_map;
    }

    public static void writeDummyContentToFile(String fileName) throws IOException {
        Random rand = new Random();
        String str = String.format("Hello world  - random number: %d", rand.nextInt(50));
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
        writer.append(' ');
        writer.append(str);
        writer.flush();
        writer.close();
    }

    public static FileLock acquireFileLock(String filePath) throws FileNotFoundException {
        FileChannel channel = new RandomAccessFile(
                new File(filePath),
                "rw"
        ).getChannel();
        FileLock fileLock = null;
        while(fileLock == null) {
            try {
                fileLock = channel.tryLock();
            } catch (IOException | OverlappingFileLockException e) {
                // don't print the trace since this error is expected sometimes.
//                e.printStackTrace();
            }
        }
        return fileLock;
    }

    public static void checkAndCreateShareDir(String directoryPath) {
        File dir = new File(directoryPath);
        if(!dir.exists()) {
            dir.mkdirs();
        }
    }
}
