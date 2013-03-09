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

import java.util.Vector;

import org.jfree.chart.JFreeChart;

import pt.up.fe.dceg.neptus.mra.LogStatisticsItem;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;

public interface LLFChart extends MRAVisualization {
	public JFreeChart getChart(IMraLogGroup source, double timestep);
	public Vector<LogStatisticsItem> getStatistics();
}
