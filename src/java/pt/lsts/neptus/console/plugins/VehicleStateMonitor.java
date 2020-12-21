/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 22, 2012
 */
package pt.lsts.neptus.console.plugins;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Teleoperation;
import pt.lsts.imc.VehicleState;
import pt.lsts.imc.VehicleState.OP_MODE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Vehicle State Monitor")
public class VehicleStateMonitor extends ConsolePanel implements IPeriodicUpdates {

    private static final long serialVersionUID = 1L;
    protected ConcurrentMap<String, VehicleState> systemStates = new ConcurrentHashMap<String, VehicleState>();

    public VehicleStateMonitor(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    @Override
    public long millisBetweenUpdates() {
        return 1500;
    }

    @Override
    public boolean update() {
        Iterator<String> it = systemStates.keySet().iterator();
        while (it.hasNext()) {
            String system = it.next();
            try {
                if (!ImcSystemsHolder.getSystemWithName(system).isActive()) {
                    systemStates.remove(system);
                    post(new ConsoleEventVehicleStateChanged(system,
                            I18n.text("No communication received for more than 10 seconds"), STATE.DISCONNECTED));
                    getConsole().getSystem(system).setVehicleState(STATE.DISCONNECTED);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().debug(
                        VehicleStateMonitor.class.getSimpleName() + " for " + system + " gave an error: "
                                + e.getMessage());
            }
        }
        return true;
    }

    @Subscribe
    public void consume(VehicleState msg) {
        try {
            String src = msg.getSourceName();
            ConsoleSystem consoleSystem = getConsole().getSystem(src);
            if (src == null || consoleSystem == null)
                return;
            STATE systemState = STATE.valueOf(msg.getOpMode().toString());
            String text = "";
            if (!msg.getLastError().isEmpty() && systemState != STATE.SERVICE){
                text += "Last error: " + msg.getLastError() + "<br>";
            }
            if(!msg.getErrorEnts().isEmpty()){
                text += "Entities in error: " + msg.getErrorEnts();
            }
            
            
            VehicleState oldState = systemStates.get(src);
            if (oldState == null) {// first time
                post(new ConsoleEventVehicleStateChanged(src, text, systemState));
                consoleSystem.setVehicleState(systemState);
                systemStates.put(src, msg);
            }
            else {
                OP_MODE last = oldState.getOpMode();
                OP_MODE current = msg.getOpMode();
                int lastType = oldState.getManeuverType();
                int currentType = msg.getManeuverType();
                if (last != current || lastType != currentType) {
                    systemStates.put(src, msg);
                    if (msg.getManeuverType() == Teleoperation.ID_STATIC) {
                        post(new ConsoleEventVehicleStateChanged(src, text, STATE.TELEOPERATION));
                        consoleSystem.setVehicleState(STATE.TELEOPERATION);
                    } else {
                        post(new ConsoleEventVehicleStateChanged(src, text, systemState));
                        consoleSystem.setVehicleState(systemState);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
    }

}
