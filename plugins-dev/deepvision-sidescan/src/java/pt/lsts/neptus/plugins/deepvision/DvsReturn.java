package pt.lsts.neptus.plugins.deepvision;

public class DvsReturn {
    private byte[] data;

    public DvsReturn(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return data.length;
    }
}
