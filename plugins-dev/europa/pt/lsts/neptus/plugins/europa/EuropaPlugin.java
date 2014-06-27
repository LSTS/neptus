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
 * Jun 27, 2014
 */
package pt.lsts.neptus.plugins.europa;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JFrame;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.europa.gui.SolverPanel;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author zp
 *
 */
@PluginDescription
public class EuropaPlugin extends ConsolePanel {

    private static final long serialVersionUID = -9214738581059126395L;

    /**
     * @param console
     */
    public EuropaPlugin(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {
        addMenuItem("Tools>Mission Planner>Generate Mission", null, new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                generateMission();
            }
        });
    }
    
    private Collection<VehicleType> getVehicles() {
        HashSet<VehicleType> ret = new HashSet<>();
        for (ImcSystem sys : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            ret.add(sys.getVehicle());
        }
        ret.add(VehiclesHolder.getVehicleById(getConsole().getMainSystem()));
        return ret;
    }
    
    private Collection<PlanType> getPlans() {        
        return getConsole().getMission().getIndividualPlansList().values();
    }
    
    private void generateMission() {
        SolverPanel sp = new SolverPanel(getConsole(), getVehicles(), getPlans());
        JFrame frame = new JFrame("Mission Planner");
        frame.setContentPane(sp);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setVisible(true);        
    }
}
