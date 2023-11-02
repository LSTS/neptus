package pt.lsts.neptus.plugins.deepvision;

import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class DvsIndex {
    private DvsHeader dvsHeader;
    private ArrayList<Long> timestamps;

    public DvsIndex(DvsHeader dvsHeader, ArrayList<Long> timestamps) {
        this.dvsHeader = dvsHeader;
        this.timestamps = timestamps;
    }
}
