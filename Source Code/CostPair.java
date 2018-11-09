/**
 *   Class to hold cost and retries values when downloading a file
 */
public class CostPair {
    private NodeInfo node;
    private LoadInfo loadInfo;
    private int retry;

    public CostPair(NodeInfo node, LoadInfo loadInfo) {
        this.node = node;
        this.loadInfo = loadInfo;
        this.retry = CONSTANTS.MAX_RETRIES;
    }

    public NodeInfo getNode() {
        return node;
    }

    public LoadInfo getLoadInfo() {
        return loadInfo;
    }

    public int getRetry() {
        return retry;
    }

    public void decRetry() {
        this.retry--;
    }
}
