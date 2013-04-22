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
 * Apr 22, 2013
 */
package pt.up.fe.dceg.neptus.mra.plots;

import pt.up.fe.dceg.neptus.imc.IMCFieldType;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfGenericIterator;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.MRAPanel;

/**
 * @author zp
 * 
 */
public class VehicleGanttPlot extends MraGanttPlot {

    @Override
    public String getName() {
        return "Vehicle Timeline";
    }
    
    public VehicleGanttPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return true;
    }

    @Override
    public void process(LsfIndex source) {
        LsfGenericIterator it = source.getIterator("VehicleState");
        
        for (IMCMessage s : it)
            startActivity(s.getTimestamp(), "Vehicle State", s.getString("op_mode"));
        
        endActivity(source.getEndTime(), "Vehicle State");
        
        it = source.getIterator("PlanControlState");
        IMCFieldType type = source.getDefinitions().getType("PlanControlState").getFieldType("man_id");

        String field = type != null ? "man_id" : "node_id";
        
        for (IMCMessage s : it) {
            if(s.getString(field) == null || s.getString(field).isEmpty())
                continue;
            startActivity(s.getTimestamp(), "Maneuver", s.getString(field));
        }
        
        endActivity(source.getEndTime(), "Maneuver");
        
        it = source.getIterator("VehicleMedium");
        
        for (IMCMessage medium : it)
            startActivity(medium.getTimestamp(), "Vehicle Medium", medium.getString("medium"));
        
        endActivity(source.getEndTime(), "Vehicle Medium");
    }
}
