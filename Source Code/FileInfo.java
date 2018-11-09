/**
 *   Class to hold the file information
 *      checkSum , file size, etc
 *
 *   Peer will maintain a information list of all files in share directory
 */
public class FileInfo {
    private int fileSize;
    private String checkSum;

    public FileInfo(int fileSize, String checkSum) {
        this.fileSize = fileSize;
        this.checkSum = checkSum;
    }

    public int getFileSize() {
        return fileSize;
    }

    public String getCheckSum() {
        return checkSum;
    }
}
