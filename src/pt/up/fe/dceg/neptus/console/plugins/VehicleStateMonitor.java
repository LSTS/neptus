/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 22, 2012
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.Teleoperation;
import pt.up.fe.dceg.neptus.imc.VehicleState;
import pt.up.fe.dceg.neptus.imc.VehicleState.OP_MODE;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Vehicle State Monitor")
public class VehicleStateMonitor extends SimpleSubPanel implements IPeriodicUpdates {

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
                    post(new ConsoleEventVehicleStateChanged(system, I18n.text("No communication received for more than 10 seconds"), STATE.DISCONNECTED));
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
        String src = msg.getSourceName();
        if (src == null)
            return;
        String text = "";
        if (!msg.getLastError().isEmpty())
            text += msg.getLastError() + "\n";
        text += msg.getErrorEnts();
        VehicleState oldState = systemStates.get(src);
        if (oldState == null) {// first time
            post(new ConsoleEventVehicleStateChanged(src, text, STATE.valueOf(msg.getOpMode().toString())));
            systemStates.put(src, msg);
        }
        else {
            OP_MODE last = oldState.getOpMode();
            OP_MODE current = msg.getOpMode();
            if (last != current) {
                systemStates.put(src, msg);
                if (msg.getManeuverType() == Teleoperation.ID_STATIC) {
                    post(new ConsoleEventVehicleStateChanged(src, text, STATE.TELEOPERATION));
                }
                if (last == OP_MODE.CALIBRATION && current == OP_MODE.SERVICE) {
                    return; // ignore
                }
                else {
                    post(new ConsoleEventVehicleStateChanged(src, text, STATE.valueOf(msg.getOpMode().toString())));
                }
            }
        }
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
    }

}
