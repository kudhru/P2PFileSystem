import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestMultipleUploadDownload {

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

        TestMultipleUploadDownload.server.printList();

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
        TestMultipleUploadDownload.server = new Server(serverInfo.getPort());

        // create 4 peers
        List<NodeInfo> nodeInfoList = Utils.getPeerListFromProperties("IP.properties");
        List<Peer> peerList = createPeers(
                nodeInfoList, serverInfo, System.getProperty("user.dir")
        );

        // create 5 files in each peer's directory.
        createFiles(peerList, 5);

        // run 4 clients per peer. Each client performs 10 download operations.
        createAndRunClients(peerList, 2, 5, false);

        System.exit(0);
    }
}
