/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.util.llf.chart;

import java.io.File;
import java.util.Vector;

import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.CtdPlot;
import pt.up.fe.dceg.neptus.mra.plots.EstimatedStatePlot;
import pt.up.fe.dceg.neptus.mra.plots.EulerAnglesPlot;
import pt.up.fe.dceg.neptus.mra.plots.LblRangesPlot;
import pt.up.fe.dceg.neptus.mra.plots.ScriptedPlot;
import pt.up.fe.dceg.neptus.mra.plots.XYPlot;
import pt.up.fe.dceg.neptus.mra.plots.ZPlot;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;

/**
 * @author ZP
 */
public class MraChartFactory {

    private static Class<?>[] automaticCharts = new Class<?>[] { EstimatedStatePlot.class, XYPlot.class, ZPlot.class,
            LblRangesPlot.class, EulerAnglesPlot.class, CtdPlot.class
    };

    /**
     * Given a LLFSource, returns all predefined charts that can be applied to that source
     * 
     * @param source An LLFSource
     * @return A list of LLFCharts that can be successfully applied to the given source
     */
    public static MRAVisualization[] getAutomaticCharts(MRAPanel panel) {
        Vector<MRAVisualization> charts = new Vector<MRAVisualization>();

        
        for (int i = 0; i < automaticCharts.length; i++) {
            MRAVisualization chart;
            try {
                chart = (MRAVisualization) automaticCharts[i].getConstructor(MRAPanel.class).newInstance(panel);
                charts.add(chart);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        charts.addAll(getScriptedPlots(panel));
        
        return charts.toArray(new MRAVisualization[charts.size()]);
    }
    
    public static Vector<ScriptedPlot> getScriptedPlots(MRAPanel panel) {
        Vector<ScriptedPlot> plots = new Vector<ScriptedPlot>();

        File[] scripts = new File("conf/mraplots").listFiles();
        for (File f : scripts) {
            if (f.isDirectory() || !f.canRead())
                continue;
            ScriptedPlot plot = new ScriptedPlot(panel, f.getAbsolutePath());
            plots.add(plot);
        }
        
        return plots;
    }

    /**
     * 
     * @param title
     * @param source
     * @param fieldsToPlot
     * @return
     */
    public static LLFChart createTimeSeriesChart(String title, IMraLogGroup source, String[] fieldsToPlot) {
        return null;
    }
}
