/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 8 Jul, 2022
 */
package pt.lsts.neptus.mra.plots;

import org.jfree.chart.JFreeChart;
import pt.lsts.imc.Depth;
import pt.lsts.imc.HistoricData;
import pt.lsts.imc.HistoricSample;
import pt.lsts.imc.Pressure;
import pt.lsts.imc.RemoteCommand;
import pt.lsts.imc.RemoteData;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "CTD Historic Chart", active = true)
public class CtdHistoricPlot extends MRACombinedPlot {

    public CtdHistoricPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("HistoricData");
    }

    @Override
    public String getName() {
        return I18n.text("CTD Historic Chart");
    }
    
    
    @Override
    public JFreeChart createChart() {
        JFreeChart chart = super.createChart();
        if (getChart("Depth") != null) {
            getChart("Depth").getXYPlot().getRangeAxis().setInverted(true);
        }
        return chart;
    }

    @Override
    public void process(LsfIndex source) {
        for (HistoricData h : source.getIterator(HistoricData.class)) {
            double baseLatDeg = h.getBaseLat();
            double baseLonDeg = h.getBaseLon();
            double baseTimeSec = h.getBaseTime();

            for (RemoteData rd : h.getData()) {
                if (rd instanceof RemoteCommand) {
                    // Do something
                } else if (rd instanceof HistoricSample) {
                    HistoricSample hs = (HistoricSample) rd;
                    try {
                        String sysIdName = ImcSystemsHolder.translateImcIdToSystemName(hs.getSysId());

                        switch (hs.getSample().getMgid()) {
                            case Depth.ID_STATIC:
                                addValue((long) ((baseTimeSec + hs.getT()) * 1E3), "Depth." + sysIdName,
                                        hs.getSample(Depth.class).getValue());
                                break;
                            case Salinity.ID_STATIC:
                                addValue((long) ((baseTimeSec + hs.getT()) * 1E3), "Salinity." + sysIdName,
                                        hs.getSample(Salinity.class).getValue());
                                break;
                            case Temperature.ID_STATIC:
                                addValue((long) ((baseTimeSec + hs.getT()) * 1E3), "Temperature." + sysIdName,
                                        hs.getSample(Temperature.class).getValue());
                                break;
                            case Pressure.ID_STATIC:
                                addValue((long) ((baseTimeSec + hs.getT()) * 1E3), "Pressure." + sysIdName,
                                        hs.getSample(Pressure.class).getValue());
                                break;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
