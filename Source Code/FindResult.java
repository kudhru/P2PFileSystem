import java.util.HashSet;

/**
 *    Stores the result of a find operation for a file name
 */
public class FindResult {
    private HashSet<NodeInfo> nodes;        // Info about all the peers containing the file
    private String checkSum;                // Checksum of file

    public FindResult(HashSet<NodeInfo> nodes, String checkSum) {
        this.nodes = nodes;
        this.checkSum = checkSum;
    }

    public HashSet<NodeInfo> getNodes() {
        return nodes;
    }

    public String getCheckSum() {
        return checkSum;
    }
}
