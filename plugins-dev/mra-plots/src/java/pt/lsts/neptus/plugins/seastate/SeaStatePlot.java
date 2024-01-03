/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Dec 6, 2013
 */
package pt.lsts.neptus.plugins.seastate;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.plots.MRATimeSeriesPlot;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Sea State", experimental = true)
public class SeaStatePlot extends MRATimeSeriesPlot {

    private final double minDepth = 0.3;
    private final double maxSpeed = 0.5;
    private final double maxPeriod = 10.0;
    private final double minPeriod = 0.5;

    public SeaStatePlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        LsfIterator<GpsFix> it = index.getIterator(GpsFix.class, 5000);
        if (it == null)
            return false;
        else {
            for (GpsFix fix : it) {
                if (fix.getSatellites() > 0)
                    return true;
            }
        }
        return false;
    }

    private void addWave(EstimatedState pt1, EstimatedState pt2) {

        double amplitude = Math.abs(pt2.getAlt() - pt1.getAlt());
        double period = pt2.getTimestamp() - pt1.getTimestamp();

        if (period > maxPeriod || period < minPeriod)
            return;

        addValue(pt2.getTimestampMillis(), pt1.getSourceName()+".amplitude", amplitude);
        addValue(pt2.getTimestampMillis(), pt1.getSourceName()+".period", period);
    }

    private double getSpeed(EstimatedState state) {
        return Math.sqrt(state.getVx() * state.getVx() + state.getVy() * state.getVy());
    }

    @Override
    public void process(LsfIndex source) {
        LsfIterator<EstimatedState> it = index.getIterator(EstimatedState.class, 200);

        boolean ascending = false;
        EstimatedState lastMin = it.next();

        while(lastMin.getDepth() > minDepth && it.hasNext() || getSpeed(lastMin) > maxSpeed)
            lastMin = it.next();

        EstimatedState lastMax = lastMin;
        if (lastMin == null || !it.hasNext())
            return;
        ascending = it.next().getAlt() >= lastMin.getAlt();

        while (it.hasNext()) {
            EstimatedState state = it.next();
            if (state.getDepth() > minDepth || getSpeed(state) > maxSpeed)
                continue;
            if (state.getAlt() == -1)
                continue;
            if (ascending) {
                if (state.getAlt() >= lastMax.getAlt())
                    lastMax = state;
                else {
                    addWave(lastMin, lastMax);
                    ascending = false;
                    lastMin = lastMax;
                }
            }
            else {
                if (state.getAlt() <= lastMin.getAlt())
                    lastMin = state;
                else {
                    addWave(lastMax, lastMin);
                    ascending = true;  
                    lastMax = lastMin;
                }
            }            
        }
    }
}
