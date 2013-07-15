/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
package pt.up.fe.dceg.neptus.mra.plots;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.Salinity;
import pt.up.fe.dceg.neptus.imc.Temperature;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIterator;
import pt.up.fe.dceg.neptus.mra.MRAPanel;

/**
 * @author zp
 *
 */
public class TemperatureVsDepthPlot extends XYPlot {

    public TemperatureVsDepthPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public String getName() {
        return "Temperature VS Depth";
    }



    @Override
    public void process(LsfIndex source) {
        int ctdId = source.getEntityId("CTD");
        LsfIterator<Temperature> tempIt = source.getIterator(Temperature.class);
        for (Temperature temp : tempIt) {
            if (temp.getSrcEnt() != ctdId)
                continue;

            IMCMessage msg = source.getMessageAt("EstimatedState", temp.getTimestamp());            
            if (msg != null) {
                addValue(temp.getTimestampMillis(), temp.getValue(), msg.getDouble("depth"), temp.getSourceName(), "Temperature");
            }
        }
    }

    public void addLogMarker(pt.up.fe.dceg.neptus.mra.LogMarker marker) {};

    public boolean canBeApplied(pt.up.fe.dceg.neptus.imc.lsf.LsfIndex index) {        
        return true;
    };
}
