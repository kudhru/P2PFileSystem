import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestCorruptedFileDownload {

    private static Server server;

    private static List<Peer> createPeers(List<NodeInfo> nodeInfoList,
                                          NodeInfo serverInfo, String parentDirectory) {
        List<Peer> peerList = new ArrayList<>();
        for(int index = 0; index < nodeInfoList.size(); index++) {
            Path peerDirectoryPath = Paths.get(parentDirectory).resolve(String.format("peer%d", index));
            Peer peer = new Peer(
                    peerDirectoryPath.toString(),
                    serverInfo,
                    nodeInfoList.get(index).getPort()
            );
            peerList.add(peer);
        }
        return peerList;
    }

    private static void createFiles(List<Peer> peerList, int numFiles) throws IOException {
        for(Peer peer: peerList) {
            for(int i=0; i<numFiles; i++) {
                File file = File.createTempFile("file_", ".txt",
                        new File(peer.getDirectoryPath()));
                Utils.writeDummyContentToFile(file.getAbsolutePath());
            }
            peer.updateList();
        }
    }

    private static void createAndRunClients(List<Peer> peerList, int numClients,
                                            int numOpsPerClient, boolean peerSelectionNovice) {
        List<String> peerDirectoryList = peerList
                .stream()
                .map(Peer::getDirectoryPath)
                .collect(Collectors.toList());

        TestCorruptedFileDownload.server.printList();

        List<TestClient> threadList = new ArrayList<>();
        // Starting all threads on peers
        for(Peer peer: peerList) {
            for (int i = 0; i < numClients; i++) {
                TestClient testClient = new TestClient(numOpsPerClient, peer, peerDirectoryList, peerSelectionNovice);
                threadList.add(testClient);
                testClient.start();
            }
        }

        // Waiting for all threads to end
        for (TestClient thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!peerSelectionNovice) {
            System.out.println("Algorithm :: All operations completed.");
        } else {
            System.out.println("Novice :: All operations completed.");
        }
    }

    public static void main(String[] args) throws IOException {

        // start the server
        NodeInfo serverInfo = Utils.getTrackingServerInfo();
        TestCorruptedFileDownload.server = new Server(serverInfo.getPort());

        // create peers
        List<NodeInfo> nodeInfoList = Utils.getPeerListFromProperties("IP.properties");
        List<Peer> peerList = createPeers(
                nodeInfoList, serverInfo, System.getProperty("user.dir")
        );

        // create 5 files in each peer's directory.
        createFiles(peerList, 5);


        // copy the files of peer 0 to a dummy_dir
        Path dummy_dir = Paths.get(System.getProperty("user.dir")).resolve(String.format("dummy_dir"));
        copyFilesToDir(peerList.get(0).getDirectoryPath(), dummy_dir.toAbsolutePath() + "/");

        // corrupt all the files in Peer 1 directory
        corruptFiles(peerList.get(0));

        // try to download that corrupted file.
        File dir = new File(peerList.get(0).getDirectoryPath());
        String[] fileNames = dir.list();
        peerList.get(2).downloadFile(fileNames[0]);
        peerList.get(2).downloadFile(fileNames[1]);
        peerList.get(2).downloadFile(fileNames[2]);
        peerList.get(2).downloadFile(fileNames[3]);
        peerList.get(2).downloadFile(fileNames[4]);

        // copy the original files of peer 1 in peer 2
        copyFilesToDir( dummy_dir.toAbsolutePath() + "/", peerList.get(1).getDirectoryPath());
        peerList.get(1).updateList();


        // Peer 3 downloads all the files from Peer 2
        peerList.get(2).downloadFile(fileNames[0]);
        peerList.get(2).downloadFile(fileNames[1]);
        peerList.get(2).downloadFile(fileNames[2]);
        peerList.get(2).downloadFile(fileNames[3]);
        peerList.get(2).downloadFile(fileNames[4]);

        System.exit(0);
    }

    private static void corruptFiles(Peer peer) {
        String directory = peer.getDirectoryPath();
        File dir = new File(directory);
        String[] fileNames = dir.list();
        for(String fileName: fileNames) {
            try {
                FileLock fileLock = Utils.acquireFileLock(dir.getAbsolutePath() + "/" + fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsolutePath() + "/" + fileName);
                fileOutputStream.write("Dummy Junk data".getBytes());
                fileOutputStream.close();

                fileLock.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void copyFilesToDir(String from_dir, String dummy_dir) {
        File dir = new File(from_dir);
        String[] fileNames = dir.list();
        Utils.checkAndCreateShareDir(dummy_dir);
        for(String fileName: fileNames) {
            try {
                String contents = Utils.readFile(dir.getAbsolutePath() + "/", fileName);

                FileOutputStream fileOutputStream = new FileOutputStream(dummy_dir + fileName);
                fileOutputStream.write(contents.getBytes());
                fileOutputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
