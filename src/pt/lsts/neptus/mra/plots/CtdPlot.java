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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Nov 13, 2012
 */
package pt.lsts.neptus.mra.plots;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 *
 */
@PluginDescription(name="CTD chart", active=false)
public class CtdPlot extends MRACombinedPlot {

    public CtdPlot(MRAPanel panel) {
        super(panel);
    }
    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("Conductivity");
    }

    @Override
    public String getName() {
        return I18n.text("CTD");
    }

    @Override
    public void process(LsfIndex source) {
        
        for (IMCMessage c : source.getIterator("Conductivity")) {
            String entity = source.getEntityName(c.getSrc(), c.getSrcEnt());
            if ("CTD".equals(entity))
                addValue(c.getTimestampMillis(), "Conductivity."+c.getSourceName(), c.getDouble("value"));
        }

        
        for (IMCMessage c : source.getIterator("Temperature")) {
            String entity = source.getEntityName(c.getSrc(), c.getSrcEnt());
            if ("CTD".equals(entity))
                addValue(c.getTimestampMillis(), "Temperature."+c.getSourceName(), c.getDouble("value"));
        }

        for (IMCMessage c : source.getIterator("Pressure")) {
            String entity = source.getEntityName(c.getSrc(), c.getSrcEnt());
            if ("CTD".equals(entity))
                addValue(c.getTimestampMillis(), "Pressure."+c.getSourceName(), c.getDouble("value"));
        }
    }
}
