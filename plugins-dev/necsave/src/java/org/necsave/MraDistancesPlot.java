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

import java.util.LinkedHashMap;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.plots.MRATimeSeriesPlot;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name="Platform distances", description="Plots the distances between all deployed platforms", active=false)
public class MraDistancesPlot extends MRATimeSeriesPlot {

    /**
     * @param panel
     */
    public MraDistancesPlot(MRAPanel panel) {
        super(panel);
    }

    private LinkedHashMap<String, LocationType> lastLocations = new LinkedHashMap<>();
    
    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {
        LsfIterator<EstimatedState> it = source.getIterator(EstimatedState.class);
        
        for (EstimatedState state = it.next(); it.hasNext(); state = it.next()) {
            String src = state.getSourceName();
            LocationType loc = IMCUtils.getLocation(state);
            lastLocations.put(src, loc);
            for (String sys : lastLocations.keySet()) {
                if (sys.compareTo(src) <= 0)
                    continue;
                
                double distance = lastLocations.get(sys).getHorizontalDistanceInMeters(loc);
                addValue(state.getTimestampMillis(), src+"_"+sys, distance);
            }
        }        
    }
}
