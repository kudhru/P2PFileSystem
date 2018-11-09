import java.io.Serializable;

/**
 *      Class to hold information about a peers and server
 */
public class NodeInfo implements Serializable{
    private String IP;
    private int port;

    NodeInfo(String IP , int port) {
        this.IP = IP;
        this.port = port;
    }

    public String getIP() {
        return IP;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof NodeInfo)) {
            return false;
        }

        NodeInfo client = (NodeInfo) o;

        return client.IP.equals(IP) &&
                client.port == port;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + IP.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return this.IP + ":" + this.port;
    }
}
