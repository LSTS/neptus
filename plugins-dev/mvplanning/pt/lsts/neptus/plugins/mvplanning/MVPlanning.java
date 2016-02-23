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
 * Author: tsmarques
 * 4 Nov 2015
 */
package pt.lsts.neptus.plugins.mvplanning;


import pt.lsts.imc.QueryEntityState;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.mvplanning.utils.VehicleAwareness;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 *
 */
@PluginDescription(name = "Multi-Vehicle Planning")
public class MVPlanning extends ConsolePanel implements PlanChangeListener {
    public static final String PROFILES_DIR = MVPlanning.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "etc/";


    private String system;
    private ConsoleLayout console;
    private VehicleAwareness vawareness;

    public MVPlanning(ConsoleLayout console) {
        super(console);
        this.console = getConsole();
        vawareness = new VehicleAwareness();
    }

    void requestEntitiesState() {
        QueryEntityState qEntityState = new QueryEntityState();
        ImcMsgManager.getManager().sendMessageToSystem(qEntityState, system);
    }

    @Override
    public void planChange(PlanType plan) {
//        printPlanCapabilitiesNeeds(plan);
    }

    @Override
    public void cleanSubPanel() {        
    }


    @Override
    public void initSubPanel() {
        this.console = getConsole();
        NeptusEvents.register(vawareness, console);
    }
}
