package pt.lsts.neptus.plugins.deepvision;

public class DvsHeader {
    public final int HEADER_SIZE = 18; // Bytes
    public final int VERSION = 1;     // VERSION
    private float sampleResolution;    // sampleRes [m]
    private float lineRate;     // lineRate [ ping/s ]
    private int nSamples;       // nSamples: Number of samples per side
    private boolean leftChannelActive;       // left: true if left/port side active
    private boolean rightChannelActive;      // right: true if right/starboard side active

    public DvsHeader() {
    }

    public float getSampleResolution() {
        return sampleResolution;
    }

    public void setSampleResolution(float sampleResolution) {
        this.sampleResolution = sampleResolution;
    }

    public float getLineRate() {
        return lineRate;
    }

    public void setLineRate(float lineRate) {
        this.lineRate = lineRate;
    }

    public int getnSamples() {
        return nSamples;
    }

    public void setnSamples(int nSamples) {
        this.nSamples = nSamples;
    }

    public boolean isLeftChannelActive() {
        return leftChannelActive;
    }

    public void setLeftChannelActive(boolean leftChannelActive) {
        this.leftChannelActive = leftChannelActive;
    }

    public boolean isRightChannelActive() {
        return rightChannelActive;
    }

    public void setRightChannelActive(boolean rightChannelActive) {
        this.rightChannelActive = rightChannelActive;
    }


    public boolean versionMatches(int version) {
        return VERSION == version;
    }
}
