package pt.lsts.neptus.plugins.deepvision;

import pt.lsts.neptus.NeptusLog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.util.List;

public class DvsIndex implements Serializable {
    private static final long serialVersionUID = 1L;
    private DvsHeader dvsHeader;
    private long totalPings;

    public DvsIndex(DvsHeader dvsHeader, long totalPings) {
        this.dvsHeader = dvsHeader;
        this.totalPings = totalPings;
    }

    public void save(String filePath) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))){
            out.writeObject(this);
        }
        catch (IOException e) {
            NeptusLog.pub().error("Could not save index to " + filePath);
            e.printStackTrace();
        }
    }

    public static DvsIndex restore(String filePath) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))){
            DvsIndex dvsIndex = (DvsIndex) in.readObject();
            return dvsIndex;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Could not restore index file at " + filePath);
            e.printStackTrace();
        }
        return null;
    }

    public long getFirstTimestamp() {
        return 0;
    }

    public long getLastTimestamp() {
        return (long)(totalPings / (dvsHeader.getLineRate() / 1000));
    }

    public List<Long> getTimestampsBetween(long startTimestamp, long stopTimestamp) {
        int startIndex = findTimestamp(startTimestamp);
        int stopIndex = findTimestamp(stopTimestamp);
        ArrayList<Long> timestamps = new ArrayList<>();
        for(int i = startIndex; i < stopIndex; i++) {
            timestamps.add((long)(i / (dvsHeader.getLineRate() / 1000)));
        }
        return timestamps;
    }

    private int findTimestamp(long timestamp) {
        return (int)(timestamp * (dvsHeader.getLineRate() /1000));
    }
}
