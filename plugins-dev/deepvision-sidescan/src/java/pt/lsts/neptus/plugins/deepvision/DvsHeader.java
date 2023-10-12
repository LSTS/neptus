package pt.lsts.neptus.plugins.deepvision;

public class DvsHeader {
    /* HEADER STRUCTURE
    Version, UINT32, Must be 0x00000001
    File Header, V1_FileHeader

    Where V1_FileHeader is

    struct V1_FileHeader {
        float sampleRes;
        float lineRate;
        int nSamples;
        bool left;
        bool right;
    };

    Plus 2 padding bytes to align at the word boundary
    */
    public final int HEADER_SIZE = 20;      // Bytes
    public final int VERSION = 1;           // VERSION

    private float sampleResolution;         // sampleRes [m]
    private float lineRate;                 // lineRate [ ping/s ]
    private int nSamples;                   // nSamples: Number of samples per side
    private boolean leftChannelActive;      // left: true if left/port side active
    private boolean rightChannelActive;     // right: true if right/starboard side active

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
        if(lineRate > 0) {
            this.lineRate = lineRate;
        } else {
            this.lineRate = (float)(16.717382321);
        }
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

    public int getNumberOfActiveChannels() {
        return (leftChannelActive ? 1 : 0) + (rightChannelActive ? 1 : 0);
    }
}
