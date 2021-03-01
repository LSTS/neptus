/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
public class XYPlot extends MRA2DPlot {

    private LocationType ref;

    /**
     * @param panel
     */
    public XYPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {
        IMCMessage estate = source.getMessage(source.getFirstMessageOfType("EstimatedState"));
        ref = new LocationType(Math.toDegrees(estate.getDouble("lat")), Math.toDegrees(estate.getDouble("lon")));

        for (IMCMessage state : source.getIterator("EstimatedState", 0, (long)(timestep*1000))) {
            LocationType loc = new LocationType();
            loc.setLatitudeRads(state.getDouble("lat"));
            loc.setLongitudeRads(state.getDouble("lon"));
            loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));
            double[] offsets = loc.getOffsetFrom(ref);

            addValue(state.getTimestampMillis(), offsets[0], offsets[1],
                    state.getSourceName(), "position");
        }


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


            double[] offsets = loc.getOffsetFrom(ref);

            addValue(state.getTimestampMillis(), offsets[0], offsets[1],
                    state.getSourceName(), "simulator");

        }

    }

    @Override
    public String getName() {
        return I18n.text("XY");
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        XYSeries markerSeries = getMarkerSeries();
        IMCMessage state = mraPanel.getSource().getLog("EstimatedState").getEntryAtOrAfter(Double.valueOf(marker.getTimestamp()).longValue());
        LocationType loc = marker.getLocation();
        ref = new LocationType(Math.toDegrees(state.getDouble("lat")), Math.toDegrees(state.getDouble("lon")));

        loc.setLatitudeRads(state.getDouble("lat"));
        loc.setLongitudeRads(state.getDouble("lon"));
        loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));

        double[] offsets = loc.getOffsetFrom(ref);

        if(markerSeries != null)
            markerSeries.add(new TimedXYDataItem(offsets[0],offsets[1],state.getTimestampMillis(),marker.getLabel()));

    }

    @Override
    public void removeLogMarker(LogMarker marker) {

    }

    @Override
    public void goToMarker(LogMarker marker) {

    }

}
