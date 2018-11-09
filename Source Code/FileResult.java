public class FileResult {
    private byte[] file;
    private String status;

    public FileResult(byte[] file, String status) {
        this.file = file;
        this.status = status;
    }

    public byte[] getFile() {
        return file;
    }

    public String getStatus() {
        return status;
    }
}
