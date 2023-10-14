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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 31/10/2016
 */
package org.necsave;

import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.LinkedHashMap;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.plots.MRA2DPlot;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * This plot compares calculates the position error whenever an AUV comes back to the surface.
 * @author zp
 *
 */
@PluginDescription(name="Position Accuracy", description="Plot position error whenever an AUV resurfaces", active=false)
public class MraPositionAccuracyPlot extends MRA2DPlot {

    /**
     * @param panel
     */
    public MraPositionAccuracyPlot(MRAPanel panel) {
        super(panel);
    }

    private LinkedHashMap<String, EstimatedState> lastSurfaceLocations = new LinkedHashMap<>();
    
    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {
        LsfIterator<EstimatedState> it = source.getIterator(EstimatedState.class);
        
        for (EstimatedState state = it.next(); it.hasNext(); state = it.next()) {
            String src = state.getSourceName();
            if (state.getDepth() == 0) {
                if (!lastSurfaceLocations.containsKey(src))
                    lastSurfaceLocations.put(src, state);
                else if (state.getTimestamp() - lastSurfaceLocations.get(src).getTimestamp() > 5) {
                    LocationType prev = IMCUtils.getLocation(lastSurfaceLocations.get(src));
                    LocationType cur = IMCUtils.getLocation(state);
                    addValue(state.getTimestampMillis(), state.getTimestamp() - lastSurfaceLocations.get(src).getTimestamp(), cur.getHorizontalDistanceInMeters(prev), src, "Position error");                    
                }
                lastSurfaceLocations.put(src, state);
            }
        }        
    }

    @Override
    public JFreeChart createChart() {
        JFreeChart chart = super.createChart();
        Ellipse2D.Double ellis = new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0);
        GeneralPath gp = new GeneralPath();
        gp.moveTo(0, 2.5);
        gp.lineTo(2.5, -2.5);
        gp.lineTo(-2.5, -2.5);
        gp.closePath();
        
        //((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setShapesVisible(true);
        //((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setLinesVisible(false);
        for (int i = 0; i < chart.getXYPlot().getSeriesCount(); i++)
            ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setSeriesShape(i, ellis);
        
        return chart;
    }

    @Override
    public String getYAxisName() {
        return "Position error (meters)";
    }
    
    @Override
    public String getXAxisName() {
        return "Time underwater (seconds)";
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void goToMarker(LogMarker marker) {
        // TODO Auto-generated method stub
        
    }
}
