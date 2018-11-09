public class LoadInfo {
    private double remoteLoad;
    private double fileSize;
    private int latency;
    private double cost;

    public LoadInfo(double remoteLoad, double fileSize, int latency) {
        this.remoteLoad = remoteLoad;
        this.fileSize = fileSize;
        this.latency = latency;
        this.cost = remoteLoad + (fileSize * latency);      // Cost function
    }

    public double getRemoteLoad() {
        return remoteLoad;
    }

    public double getFileSize() {
        return fileSize;
    }

    public int getLatency() {
        return latency;
    }

    public double getCost(){
        return cost;
    }
}
