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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 31/10/2016
 */
package org.necsave;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import pt.lsts.imc.Current;
import pt.lsts.imc.Voltage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.plots.MRATimeSeriesPlot;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Consumption", description="Plot the battery consumption", active=false)
public class MraConsumptionPlot extends MRATimeSeriesPlot {

    /**
     * @param panel
     */
    public MraConsumptionPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("Current", "Voltage");
    }

    private LinkedHashMap<Integer, Voltage> lastVoltages = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Current> lastCurrents = new LinkedHashMap<>();

    private LinkedHashMap<Integer, Double> totalConsumption = new LinkedHashMap<>();
    private ArrayList<Double> consumptionValues = new ArrayList<>();
    
    
    @Override
    public void process(LsfIndex source) {

        int entity = source.getEntityId("Batteries");

        for (int i = 0; i < source.getNumberOfMessages(); i++) {
            int src = source.sourceOf(i);

            if (source.typeOf(i) == Current.ID_STATIC) {
                if (source.entityOf(i) != entity)
                    continue;
                Current msg = null;
                try {
                    msg = source.getMessage(i, Current.class);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                Voltage lastVoltage = lastVoltages.get(src);
                if (lastVoltage != null) {
                    if (!totalConsumption.containsKey(src))
                        totalConsumption.put(src, 0.0);

                    Current lastCurrent = lastCurrents.get(src);
                    if (lastCurrent == null)
                        lastCurrent = msg;

                    long ilast = (long) lastCurrent.getTimestamp();
                    long ithis = (long) msg.getTimestamp();

                    if (ilast == ithis || consumptionValues.isEmpty())
                        consumptionValues.add(msg.getValue());
                    else {
                        double average = consumptionValues.stream().mapToDouble(a -> a).average().getAsDouble();
                        consumptionValues.clear();
                        ilast = ithis;
                        double watts = lastVoltage.getValue() * average;
                        double total = totalConsumption.get(src) + watts;

                        totalConsumption.put(src, total);
                        addValue(msg.getTimestampMillis(), source.getSystemName(src), watts);
                        addValue(msg.getTimestampMillis(), source.getSystemName(src) + ".total",
                                (totalConsumption.get(src) + watts) / 3600.0);

                    }
                }
                lastCurrents.put(src, msg);

            }
            else if (source.typeOf(i) == Voltage.ID_STATIC) {
                if (source.entityOf(i) != entity)
                    continue;

                try {
                    lastVoltages.put(src, source.getMessage(i, Voltage.class));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
