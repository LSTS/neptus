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

import org.jfree.data.xy.XYSeries;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;

/**
 * @author zp
 *
 */
@PluginDescription(name="Salinity Vs Depth plot", active=false)
public class SalinityVsDepthPlot extends XYPlot {

    public SalinityVsDepthPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public String getName() {
        return I18n.text("Salinity VS Depth");
    }

    @Override
    public void process(LsfIndex source) {
        int ctdId = source.getEntityId("CTD");
        LsfIterator<Salinity> tempIt = source.getIterator(Salinity.class);
        for (Salinity temp : tempIt) {
            if (temp.getSrcEnt() != ctdId)
                continue;

            IMCMessage msg = source.getMessageAt("EstimatedState", temp.getTimestamp());            
            if (msg != null && msg.getDouble("depth") > 0.5) {
                addValue(temp.getTimestampMillis(), -msg.getDouble("depth"), temp.getValue(),temp.getSourceName(), "Salinity");
            }
        }
    }

    public void addLogMarker(LogMarker marker) {
        try {
            XYSeries markerSeries = getMarkerSeries();
            IMCMessage es = mraPanel.getSource().getLog("EstimatedState").getEntryAtOrAfter(new Double(marker.getTimestamp()).longValue());
            IMCMessage sal = mraPanel.getSource().getLog("Salinity").getEntryAtOrAfter(new Double(marker.getTimestamp()).longValue());
            if(markerSeries != null)
                markerSeries.add(new TimedXYDataItem(-es.getDouble("depth"), ((Salinity) sal).getValue(), sal.getTimestampMillis(), marker.getLabel()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean canBeApplied(pt.lsts.imc.lsf.LsfIndex index) {        
        return index.containsMessagesOfType("EstimatedState", "Salinity");
    }
}
