package pt.lsts.neptus.mra.plots;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface IMultiAxisPlots {
    static void createAxisNames(Map<String, List<String>> groupingCollections, Map<String, String> labelsCollections) {
        String[] grpsArr = groupingCollections.keySet().toArray(new String[0]);
        for (int n = 0; n < grpsArr.length; n++) {
            String grp = grpsArr[n];
            String[] elm = grp.split("\\.");
            boolean foundMatch = false;
            for (int i = 0; i < elm.length; i++) {
                String n1 = Arrays.stream(Arrays.copyOf(elm, i + 1)).collect(Collectors.joining("."));
                List<String> ggg = groupingCollections.keySet().stream().skip(n + 1).collect(Collectors.toList());
                foundMatch |= groupingCollections.keySet().stream().skip(n + 1).anyMatch((e) -> e.startsWith(n1));
                foundMatch |= labelsCollections.values().stream().anyMatch((e) -> e.startsWith(n1));
                if (foundMatch) {
                    continue;
                } else {
                    labelsCollections.put(grp, n1);
                    break;
                }
            }
            if (foundMatch) {
                labelsCollections.put(grp, grp);
            }
        }
    }

    static String getSourceDataName(String serName) {
        String[] sp = serName.split("\\.");
        switch (sp.length) {
            case 0:
            case 1:
            case 2:
                return serName;
            case 3:
                return sp[2];
            default:
                return sp[2] + "." + sp[3];
        }
    }
}
