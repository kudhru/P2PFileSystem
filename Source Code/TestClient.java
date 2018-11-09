import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 *  Driver to randomly read from server
 */
public class TestClient extends Thread{
    private final int numOps;
    private final Peer peer;
    private final List<String> peerDirectoryList;
    private boolean peerSelectionNovice;

    TestClient(int numOps, Peer peer, List<String> peerDirectoryList, boolean peerSelectionNovice) {
        this.numOps = numOps;
        this.peer = peer;
        this.peerSelectionNovice = peerSelectionNovice;
        this.peerDirectoryList = this.constructPeerDirectoryList(peerDirectoryList);
    }

    private List<String> constructPeerDirectoryList(List<String> peerDirectoryList) {
        List<String> thisPeerDirectoryList = new ArrayList<>();
        for(String peerDir:peerDirectoryList) {
            if(!peerDir.equals(this.peer.getDirectoryPath())) {
                thisPeerDirectoryList.add(peerDir);
            }
        }
        return thisPeerDirectoryList;
    }


    private String selectFileFromDir(String directory) {
        File dir = new File(directory);
        Random random = new Random();
        String[] fileNames = dir.list();
        return fileNames[random.nextInt(fileNames.length)];
    }


    public void run(){
        for(int i=0;i<numOps;i++) {
            // choose a directory at random
            Random random = new Random();
            String directory = this.peerDirectoryList.get(random.nextInt(this.peerDirectoryList.size()));

            // download a file at random from the above chosen directory
            String fileName = this.selectFileFromDir(directory);
            String content_received = null;
            if(!this.peerSelectionNovice) {
                content_received = peer.downloadFile(fileName);
            }else {
                content_received = peer.downloadFileNovice(fileName);
            }
            String contents_expected = Utils.readFile(directory, fileName);
            if(!contents_expected.equals(content_received)) {
                System.out.println(String.format("File %s at directory %s not matched", fileName, directory));
                System.out.println(String.format("Contents expected: %s", contents_expected));
                System.out.println(String.format("Contents received: %s", content_received));
            }
        }
    }


}