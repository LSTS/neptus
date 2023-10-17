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
        // The data points are logarithmically compressed.
        // So we need to decompress them.
        double[] doubleData = new double[data.length];
        for(int i = 0; i < doubleData.length; i++) {
            doubleData[i] = Byte.toUnsignedInt(data[i]);
            doubleData[i] = Math.pow(1.025, doubleData[i]);
        }
        return doubleData;
    }

    public int getLength() {
        return data.length;
    }
}
