package pt.lsts.neptus.mra.importers.sdf;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class SdfTimestampList {
    private Long[] list;
    private int indexCache = 0;

    public SdfTimestampList() {
        list = new Long[0];
    }

    public SdfTimestampList(Long[] list) {
        this.list = list;
        init();
    }

    public SdfTimestampList(ArrayList<Long[]> timestampLists) {
        int totalSize = 0;
        for (Long[] list : timestampLists) {
            totalSize += list.length;
        }

        list = new Long[totalSize];
        int i = 0;
        for (Long[] timestampList : timestampLists) {
            for (Long timestamp : timestampList) {
                list[i++] = timestamp;
            }
        }

        init();
    }

    public void add(Long[] timestampList) {
        list = (Long[]) ArrayUtils.addAll(list, timestampList);
        Arrays.sort(list);
    }

    // Call after constructor
    private void init() {
        Arrays.sort(list);
    }

    public Long[] getTimestampsBetween(long timestamp1, long timestamp2) {
        int index1 = findTimestampIndex(timestamp1);
        int index2 = index1;
        while (list[index2] < timestamp2) {
            index2++;
        }
        return Arrays.copyOfRange(list, index1, index2);
    }

    private int findTimestampIndex(long timestamp) {
        if (timestamp < list[0] || timestamp > list[list.length - 1]) {
            return -1;
        }

        if (list[indexCache] == timestamp) {
            return indexCache;
        }

        int index = indexCache;
        if (list[indexCache] < timestamp) {
            while (list[index] < timestamp) {
                index++;
            }
        }
        else {
            while (list[index] > timestamp) {
                index--;
            }
            index++;
        }
        indexCache = index;
        return index;
    }
}
