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
    private ArrayList<Long> timestamps;
    private int searchCache = 0;

    public DvsIndex(DvsHeader dvsHeader, ArrayList<Long> timestamps) {
        this.dvsHeader = dvsHeader;
        this.timestamps = timestamps;
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

    public List<Long> getTimestampsBetween(long startTimestamp, long stopTimestamp) {
        int startIndex = findTimestamp(startTimestamp);
        int stopIndex = startIndex;
        while(timestamps.get(stopIndex) < stopTimestamp) {
            stopIndex++;
        }
        return timestamps.subList(startIndex, stopIndex);
    }

    private int findTimestamp(long timestamp) {
        if (timestamp < timestamps.get(0) || timestamp > timestamps.get(timestamps.size() - 1)) {
            return -1;
        }

        if (timestamps.get(searchCache) == timestamp) {
            return searchCache;
        }

        int index = searchCache;
        if (timestamps.get(searchCache) < timestamp) {
            while (timestamps.get(index) < timestamp) {
                index++;
            }
        }
        else {
            while (timestamps.get(index) > timestamp) {
                index--;
            }
            index++;
        }
        searchCache = index;
        return index;
    }
}
