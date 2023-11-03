package pt.lsts.neptus.plugins.deepvision;

import pt.lsts.neptus.NeptusLog;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.io.FileOutputStream;

public class DvsIndex implements Serializable {
    private static final long serialVersionUID = 1L;
    private DvsHeader dvsHeader;
    private ArrayList<Long> timestamps;

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
}
