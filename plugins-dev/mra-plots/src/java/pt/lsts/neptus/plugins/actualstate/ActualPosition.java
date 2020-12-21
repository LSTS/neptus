/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 18, 2013
 */
package pt.lsts.neptus.plugins.actualstate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.jfree.data.xy.XYSeries;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.def.SystemType;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.api.CorrectedPositionBuilder;
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
        LsfIterator<EstimatedState> it = source.getIterator(EstimatedState.class);
        long stepTime = (long) (timestep * 1000);
        
         Vector<Announce> uuvSys = source.getSystemsOfType(SystemType.UUV);
         Collection<Integer> systemsLst = new ArrayList<>();
         uuvSys.forEach(an -> systemsLst.add(an.getSrc()));
         
         Map<Integer, CorrectedPositionBuilder> cpBuilders = new LinkedHashMap<>(systemsLst.size());
         systemsLst.forEach(src -> cpBuilders.put(src, new CorrectedPositionBuilder()));
         
         Map<Integer, Long> timesLst = new LinkedHashMap<>(systemsLst.size());
         
         for (EstimatedState es = it.next(); es != null; es = it.next()) {
             long prevTime = -1;
             if (timesLst.containsKey(es.getSrc()))
                 prevTime = timesLst.get(es.getSrc());
             
             long diffT = es.getTimestampMillis() - prevTime;
             if (diffT < stepTime)
                 continue;

             timesLst.put(es.getSrc(), es.getTimestampMillis());
             
             LocationType thisLoc = new LocationType();
             thisLoc.setLatitudeRads(es.getLat());
             thisLoc.setLongitudeRads(es.getLon());
             if (es.getDepth() > 0)
                 thisLoc.setDepth(es.getDepth());
             if (es.getAlt() > 0)
                 thisLoc.setDepth(-es.getAlt());
             thisLoc.translatePosition(es.getX(), es.getY(), 0);
             thisLoc.convertToAbsoluteLatLonDepth();
             addValue(es.getTimestampMillis(), thisLoc.getLatitudeDegs(), thisLoc.getLongitudeDegs(),
                     es.getSourceName(), "Estimated Position");
             
             CorrectedPositionBuilder builder = cpBuilders.get(es.getSrc());
             if (builder == null)
                 continue;
             
             builder.update(es);
         }
         
         for (int src : cpBuilders.keySet()) {
             CorrectedPositionBuilder builder = cpBuilders.get(src);
             ArrayList<SystemPositionAndAttitude> posLst = builder.getPositions();
             for (SystemPositionAndAttitude sysPosAtt : posLst) {
                String sysName = source.getSystemName(src);
                LocationType pos = sysPosAtt.getPosition();
                addValue(sysPosAtt.getTime(), pos.getLatitudeDegs(), pos.getLongitudeDegs(),
                        sysName, "Actual Position");
             }
        }
    }
}