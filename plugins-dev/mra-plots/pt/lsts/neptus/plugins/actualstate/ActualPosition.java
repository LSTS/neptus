/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jun 18, 2013
 */
package pt.lsts.neptus.plugins.actualstate;

import java.util.Vector;

import org.jfree.data.xy.XYSeries;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.plots.MRA2DPlot;
import pt.lsts.neptus.mra.plots.TimedXYDataItem;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Corrected position")
public class ActualPosition extends MRA2DPlot {

    private MRAPanel mraPanel;

    public ActualPosition(MRAPanel panel) {
        super(panel);
        this.mraPanel = panel;
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
        // TODO

    }

    @Override
    public void goToMarker(LogMarker marker) {
        // TODO

    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {
        LsfIterator<EstimatedState> it = source.getIterator(EstimatedState.class, (long) (timestep * 1000));

        Vector<EstimatedState> nonAdjusted = new Vector<>();
        Vector<LocationType> nonAdjustedLocs = new Vector<>();

        LocationType lastLoc = null;
        double lastTime = 0;

        for (EstimatedState es = it.next(); es != null; es = it.next()) {
            LocationType thisLoc = new LocationType();
            thisLoc.setLatitudeRads(es.getLat());
            thisLoc.setLongitudeRads(es.getLon());
            if (es.getDepth() > 0)
                thisLoc.setDepth(es.getDepth());
            if (es.getAlt() > 0)
                thisLoc.setDepth(-es.getAlt());
            thisLoc.translatePosition(es.getX(), es.getY(), 0);
            double speed = Math.sqrt(es.getU() * es.getU() + es.getV() * es.getV() + es.getW() * es.getW());

            thisLoc.convertToAbsoluteLatLonDepth();

            if (lastLoc != null) {
                double expectedDiff = speed * (es.getTimestamp() - lastTime);

                lastTime = es.getTimestamp();

                double diff = lastLoc.getHorizontalDistanceInMeters(thisLoc);
                addValue(es.getTimestampMillis(), thisLoc.getLatitudeDegs(), thisLoc.getLongitudeDegs(),
                        es.getSourceName(), "Estimated Position");
                if (diff < expectedDiff * 3) {
                    nonAdjusted.add(es);
                    nonAdjustedLocs.add(thisLoc);

                }
                else {
                    if (!nonAdjusted.isEmpty()) {
                        double[] adjustment = thisLoc.getOffsetFrom(lastLoc);
                        EstimatedState firstNonAdjusted = nonAdjusted.firstElement();
                        double timeOfAdjustment = es.getTimestamp() - firstNonAdjusted.getTimestamp();
                        double xIncPerSec = adjustment[0] / timeOfAdjustment;
                        double yIncPerSec = adjustment[1] / timeOfAdjustment;

                        for (int i = 0; i < nonAdjusted.size(); i++) {
                            EstimatedState adj = nonAdjusted.get(i);
                            LocationType loc = nonAdjustedLocs.get(i);
                            loc.translatePosition(xIncPerSec * (adj.getTimestamp() - firstNonAdjusted.getTimestamp()),
                                    yIncPerSec * (adj.getTimestamp() - firstNonAdjusted.getTimestamp()), 0);

                            loc.convertToAbsoluteLatLonDepth();
                            addValue(adj.getTimestampMillis(), loc.getLatitudeDegs(), loc.getLongitudeDegs(),
                                    adj.getSourceName(), "Actual Position");
                        }
                        nonAdjusted.clear();
                        nonAdjustedLocs.clear();
                        nonAdjusted.add(es);
                        nonAdjustedLocs.add(thisLoc);
                    }
                }
            }
            lastLoc = thisLoc;
            lastTime = es.getTimestamp();
        }
    }
}
