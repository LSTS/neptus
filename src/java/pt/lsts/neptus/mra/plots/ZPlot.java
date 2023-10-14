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

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;

/**
 * @author zp
 * 
 */
@PluginDescription
public class ZPlot extends MRATimeSeriesPlot {

    public ZPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public String getTitle() {
        return "Z plot";
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {

        TidePredictionFinder tide = TidePredictionFactory.create(source);

        EstimatedState firstState = source.getFirst(EstimatedState.class);

        // check if its an UAV...
        if (firstState != null && "Autopilot".equals(firstState.getEntityName())) {
            for (EstimatedState state : source.getIterator(EstimatedState.class, (long) (timestep * 1000))) {
                double alt = state.getHeight() - state.getZ();
                double depth = state.getDouble("depth");
                addValue(state.getTimestampMillis(), state.getSourceName() + "." + I18n.text("Altitude"), alt);
                // ardusub systems are suppose to report depth values
                if (depth != -1 && !Double.isNaN(depth))
                    addValue(state.getTimestampMillis(), state.getSourceName() + "." + I18n.text("Depth"), depth);
            }
            
            for (GpsFix fix : source.getIterator(GpsFix.class, (long) (timestep * 1000))) {
                double alt = fix.getHeight();
                addValue(fix.getTimestampMillis(), fix.getSourceName() + "." + I18n.text("GPS Height"), alt);
            }
            
        }
        else if (source.getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
            for (IMCMessage es : source.getIterator("EstimatedState", 0, (long) (timestep * 1000))) {
                double depth = es.getDouble("depth");
                double alt = es.getDouble("alt");

                if (depth != -1)
                    addValue(es.getTimestampMillis(), es.getSourceName() + "." + I18n.text("Depth"), depth);

                if (alt != -1) {
                    addValue(es.getTimestampMillis(), es.getSourceName() + "." + I18n.text("Altitude"), alt);
                }
                if (depth != -1 && alt != -1) {
                    if (tide == null)
                        addValue(es.getTimestampMillis(), es.getSourceName() + "." + I18n.text("Bathymetry"),
                                Math.max(0, depth) + Math.max(0, alt));
                    else {
                        double tHeight = 0;

                        try {
                            if (tide != null) {
                                tHeight = tide.getTidePrediction(es.getDate(), false);
                                addValue(es.getTimestampMillis(), I18n.text("Tide Level"), tHeight);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        addValue(es.getTimestampMillis(), es.getSourceName() + "." + I18n.text("Bathymetry"),
                                Math.max(0, depth) + Math.max(0, alt) - tHeight);
                    }
                }
            }
        }
        else {
            for (IMCMessage es : source.getIterator("depth")) {
                addValue(es.getTimestampMillis(), es.getSourceName() + "." + I18n.text("Depth"), es.getDouble("value"));
            }
            for (int i = source.getFirstMessageOfType("BottomDistance"); i != -1; i = source.getNextMessageOfType(
                    "BottomDistance", i)) {
                IMCMessage m = source.getMessage(i);
                String entity = source.getEntityName(m.getSrc(), m.getSrcEnt());
                if (entity.equals("DVL")) {
                    addValue(m.getTimestampMillis(), m.getSourceName() + "." + I18n.text("Altitude"),
                            m.getDouble("value"));
                }
            }
        }
    }

    @Override
    public String getHorizontalAxisName() {
        return I18n.text("meters");
    }

    @Override
    public String getName() {
        return I18n.text("Z");
    }
}
