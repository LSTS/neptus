/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 15, 2013
 */
package pt.lsts.neptus.mra.plots;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.MRAProperties.depthEntities;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 * 
 */
@PluginDescription
public class TemperatureVsDepthPlot extends XYPlot {

    public TemperatureVsDepthPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public String getName() {
        return I18n.text("Temperature VS Depth");
    }
    
    @Override
    public String getXAxisName() {
        return I18n.text("Depth (meters)");
    }
    
    @Override
    public String getYAxisName() {
        return I18n.text("Temperature (ºC)");
    }


    @Override
    public void process(LsfIndex source) {
        
        String depthEntity = MRAProperties.depthEntity.toString().replaceAll("_", " ");
        
        int ctdId = source.getEntityId(depthEntity);

        if (ctdId == 255) {
            for (depthEntities e : MRAProperties.depthEntities.values()) {
                String ent = e.toString().replaceAll("_", " ");
                ctdId = source.getEntityId(ent);
                if (ctdId != 255)
                    break;
            }
        }
            

        LsfIterator<Temperature> tempIt = source.getIterator(Temperature.class);
        for (Temperature temp : tempIt) {
            if (temp.getSrcEnt() != ctdId)
                continue;

            IMCMessage msg = source.getMessageAt("EstimatedState", temp.getTimestamp());
            if (msg != null) {
                addValue(temp.getTimestampMillis(), - msg.getDouble("depth"), temp.getValue(), temp.getSourceName(),
                        "Temperature");
            }
        }
    }

    public void addLogMarker(LogMarker marker) {
/*
        XYSeries markerSeries = getMarkerSeries();

        String depthEntity = MRAProperties.depthEntity.toString().replaceAll("_", " ");
        int ctdId = mraPanel.getSource().getLsfIndex().getEntityId(depthEntity);

        IMCMessage es = mraPanel.getSource().getLog("EstimatedState").getEntryAtOrAfter(new Double(marker.timestamp).longValue());
        IMCMessage temp = mraPanel.getSource().getLog("Temperature").getEntryAtOrAfter(new Double(marker.timestamp).longValue());

        if(markerSeries != null) {
            while (temp.getSrcEnt() != ctdId) {
                temp = mraPanel.getSource().getLog("Temperature").getEntryAtOrAfter(temp.getTimestampMillis());
            }
            markerSeries.add(new TimedXYDataItem(-es.getDouble("depth"), ((Temperature) temp).getValue(), temp.getTimestampMillis(), marker.label));
        }
*/
    };

    public boolean canBeApplied(pt.lsts.imc.lsf.LsfIndex index) {
        return true;
    };
}
