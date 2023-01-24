/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 24 Jan, 2023
 */
package pt.lsts.neptus.mra.plots;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author pdias
 *
 */
public class GenericMultiAxisPlot extends GenericPlot {

    public GenericMultiAxisPlot(String[] fieldsToPlot, MRAPanel panel) {
        super(fieldsToPlot, panel , "Compare Plot");
    }

    // public ImageIcon getIcon() {
    //     return ImageUtils.getIcon("images/menus/graph2.png");
    // }

    @Override
    public String getTitle() {
        StringBuilder sb = new StringBuilder(getName());
        sb.append(" plot multi");
        return sb.toString();
    }

    @Override
    public JFreeChart getChart(IMraLogGroup source, double timestep) {
        super.getChart(source, timestep);

        // Process to add multiple axis
        if (series.size() > 1) {
            Map<String, List<String>> groupingCollections = new LinkedHashMap<>();
            String firstSer = null;

            for (String serName : series.keySet()) {
                if (firstSer == null) {
                    firstSer = serName;
                }
                String sourceName = getSourceDataName(serName);
                if (groupingCollections.containsKey(sourceName)) {
                    groupingCollections.get(sourceName).add(serName);
                } else {
                    List<String> sn = new ArrayList<>();
                    sn.add(serName);
                    groupingCollections.put(sourceName, sn);
                }
            }

            if (groupingCollections.size() > 1) {
                Map<String, String> labelsCollections = new LinkedHashMap<>();
                Map<String, TimeSeriesCollection> tsers = new HashMap<>();
                String firstGrp = null;

                int idx = 0;
                for (String grpName : groupingCollections.keySet()) {
                    if (firstGrp == null) {
                        firstGrp = grpName;
                        continue;
                    }

                    TimeSeriesCollection ntsc = new TimeSeriesCollection();
                    tsers.put(grpName, ntsc);
                    for (String ser : groupingCollections.get(grpName)) {
                        tsc.removeSeries(series.get(ser));
                        ntsc.addSeries(series.get(ser));
                    }
                    final NumberAxis axis2 = new NumberAxis(grpName);
                    axis2.setAutoRangeIncludesZero(false);
                    axis2.setLowerMargin(0.02);  // reduce the default margins
                    axis2.setUpperMargin(0.02);
                    chart.getXYPlot().setRangeAxis(++idx, axis2);
                    chart.getXYPlot().setDataset(idx, ntsc);
                    chart.getXYPlot().mapDatasetToRangeAxis(idx, 1);
                }
                chart.getXYPlot().getRangeAxis().setLabel(firstGrp);

                createAxisNames(groupingCollections, labelsCollections);
                for (int i = 0; i < chart.getXYPlot().getRangeAxisCount(); i++) {
                    String curLbl = chart.getXYPlot().getRangeAxis(i).getLabel();
                    chart.getXYPlot().getRangeAxis(i).setLabel(labelsCollections.get(curLbl));
                }

                // Fix series color
                int col = 0;
                idx = 0;
                for (String grpName : groupingCollections.keySet()) {
                    XYItemRenderer r1 = null;
                    TimeSeriesCollection t1 = null;
                    if (firstGrp.equalsIgnoreCase(grpName)) {
                        r1 = chart.getXYPlot().getRenderer();
                        t1 = tsc;
                    } else {
                        r1 = new StandardXYItemRenderer();
                        chart.getXYPlot().setRenderer(++idx, r1);
                        t1 = tsers.get(grpName);
                    }

                    if (r1 != null) {
                        for (int i = 0; i < t1.getSeriesCount(); i++) {
                            r1.setSeriesPaint(i, seriesColors[col++ % seriesColors.length]);
                        }
                    }
                }
            }
        }

        return chart;
    }

    private void createAxisNames(Map<String, List<String>> groupingCollections, Map<String, String> labelsCollections) {
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

    private String getSourceDataName(String serName) {
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
