/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Nov 13, 2012
 */
package pt.lsts.neptus.mra.plots;

import org.jfree.data.xy.XYSeries;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
@PluginDescription(active=false)
public class EstimatedStatePlot extends MRA2DPlot implements LogMarkerListener {

    /**
     * @param panel
     */
    public EstimatedStatePlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {
        for (IMCMessage state : source.getIterator("EstimatedState", 0, (long)(timestep*1000))) {
            LocationType loc = new LocationType();
            loc.setLatitudeRads(state.getDouble("lat"));
            loc.setLongitudeRads(state.getDouble("lon"));
            loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));
            loc.convertToAbsoluteLatLonDepth();
            addValue(state.getTimestampMillis(), loc.getLatitudeDegs(), loc.getLongitudeDegs(),
                    state.getSourceName(), "position");
        }


        IMCMessage estate = source.getMessage(source.getFirstMessageOfType("EstimatedState"));
        LocationType ref = new LocationType(Math.toDegrees(estate.getDouble("lat")), Math.toDegrees(estate.getDouble("lon")));

        for (IMCMessage state : source.getIterator("SimulatedState", 0, (long)(timestep*1000))) {
            LocationType loc = new LocationType();
            if (state.getTypeOf("lat") != null) {
                loc.setLatitudeRads(state.getDouble("lat"));
                loc.setLongitudeRads(state.getDouble("lon"));
            }
            else {
                loc.setLocation(ref);
            }
            loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));
            loc.convertToAbsoluteLatLonDepth();
            addValue(state.getTimestampMillis(), loc.getLatitudeDegs(), loc.getLongitudeDegs(),
                    state.getSourceName(), "simulator");
        }
    }

    @Override
    public String getName() {
        return I18n.text("Position");
    }

    @Override
    public String getXAxisName() {
        return "Latitude";
    }

    @Override
    public String getYAxisName() {
        return "Longitude";
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        XYSeries markerSeries = getMarkerSeries();
        IMCMessage state = mraPanel.getSource().getLog("EstimatedState").getEntryAtOrAfter(new Double(marker.getTimestamp()).longValue());
        LocationType loc = new LocationType();
        loc.setLatitudeRads(state.getDouble("lat"));
        loc.setLongitudeRads(state.getDouble("lon"));
        loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));
        loc.convertToAbsoluteLatLonDepth();

        if(markerSeries != null)
            markerSeries.add(new TimedXYDataItem(loc.getLatitudeDegs(), loc.getLongitudeDegs(), new Double(marker.getTimestamp()).longValue(), marker.getLabel()));
    }

    @Override
    public void removeLogMarker(LogMarker marker) {

    }

    @Override
    public void GotoMarker(LogMarker marker) {

    }

}
