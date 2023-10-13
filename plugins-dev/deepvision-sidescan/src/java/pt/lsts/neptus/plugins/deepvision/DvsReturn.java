package pt.lsts.neptus.plugins.deepvision;

public class DvsReturn {
    private byte[] data;

    public DvsReturn(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public double[] getDataAsDouble() {
        double[] doubleData = new double[data.length];
        for(int i = 0; i < doubleData.length; i++)
            doubleData[i] = data[i];
        return doubleData;
    }

    public int getLength() {
        return data.length;
    }
}
