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
    private int pingBlockSize;
    private ArrayList<Long> timestampList;
    private ArrayList<Integer> positionList;

    public DvsIndex(DvsHeader dvsHeader, ArrayList<Long> timestampList, ArrayList<Integer> positionList) {
        this.dvsHeader = dvsHeader;
        this.pingBlockSize = DvsPos.SIZE + dvsHeader.getnSamples() * dvsHeader.getNumberOfActiveChannels();
        this.timestampList = timestampList;
        this.positionList = positionList;
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
        return timestampList.get(timestampList.size() - 1);
    }

    public DvsHeader getDvsHeader() {
        return dvsHeader;
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

    public List<Integer> getPositionsBetween(long startTimestamp, long stopTimestamp) {
        int startIndex = findTimestamp(startTimestamp);
        int stopIndex = startIndex;
        while(timestampList.get(stopIndex) < stopTimestamp) {
            stopIndex++;
        }
        return positionList.subList(startIndex, stopIndex);
    }

    public int getPingBlockSize() {
        return pingBlockSize;
    }

    private int getPosition(int index) {
        return dvsHeader.HEADER_SIZE + index * pingBlockSize;
    }

    private int findTimestamp(long timestamp) {
        if(timestamp < timestampList.get(0) || timestamp > timestampList.get(timestampList.size() - 1)) {
            return -1;
        }

        int index = 0;
        while(timestampList.get(index) < timestamp) {
            index++;
        }
        return index;
    }
}
