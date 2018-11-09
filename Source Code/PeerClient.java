import java.util.Scanner;

/**
 *      Terminal code for peer
 */
public class PeerClient {
    public static void main(String[] args) {
        NodeInfo server = Utils.getTrackingServerInfo();
        System.out.println("Tracking Server :: " + server.toString());

        String dir = args[0];
        int port = Integer.parseInt(args[1]);
        Peer peer = new Peer(dir, server, port);

        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("\n\n1. find file_name -- Find the peers this file is located on. \n" +
                    "2. download file_name -- Download the file.\n" +
                    "3. updatelist -- Forcefully send updates to server.\n" +
                    "4. stop -- End\n\n");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("Stop")) {
                System.exit(0);
            } else if (input.startsWith("find")){
                String fileName = input.split(" ")[1];
                FindResult findResult = peer.find(fileName);
                if(findResult.getNodes().size() == 0) {
                    System.out.println("File Not found at any peer. Please check that the filename is correct");
                } else {
                    System.out.println("File found at the following peers:");
                    for (NodeInfo node : findResult.getNodes()) {
                        System.out.println(node.toString());
                    }
                    System.out.println("--------------");
                }
            } else if (input.startsWith("download")){
                String fileName = input.split(" ")[1];
                peer.downloadFile(fileName);
            } else if (input.equalsIgnoreCase("updatelist")){
                peer.updateList();
            }
        }

    }
}
