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
 * Author: Paulo Dias
 * 2005/06/15
 */
package pt.lsts.neptus.comm;

import java.awt.Component;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import pt.lsts.neptus.comm.ssh.SSHAdjustDate;
import pt.lsts.neptus.gui.WaitPanel;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * This will sync the the remote clock either by Telnet or SSH connection.
 * 
 * @author Paulo Dias
 */
public class SyncVehicleClock {
    private Timer timer;
    private VehicleType vehicle;
    private boolean ret = false;
    private Component parentComponent = null;

    public SyncVehicleClock(VehicleType vehicle) {
        this.vehicle = vehicle;
    }

    public SyncVehicleClock(VehicleType vehicle, Component parentComponent) {
        this.vehicle = vehicle;
        this.parentComponent = parentComponent;
    }

    /**
     * @param parentComponent The parentComponent to set.
     */
    public void setParentComponent(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    public void sync() {
        timer = new Timer("Synchronize Vehicle Clock");
        timer.schedule(new SyncTask(), 500);
    }

    class SyncTask extends TimerTask {
        public void run() {
            boolean telnetSup = CommUtil.isProtocolSupported(vehicle.getId(), "telnet");
            boolean sshSup = CommUtil.isProtocolSupported(vehicle.getId(), "ssh");
            String syncMode = "";
            if (telnetSup & sshSup) {
                Object[] options = { "ssh", "telnet" };
                int option = JOptionPane.showOptionDialog(parentComponent, "Choose", "Choose",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (option == 0)
                    syncMode = "ssh";
                else if (option == 1)
                    syncMode = "telnet";
            }
            else if (telnetSup & !sshSup)
                syncMode = "telnet";
            else if (!telnetSup & sshSup)
                syncMode = "ssh";

            WaitPanel wait = new WaitPanel();
            // Component parentCompTmp = parentComponent;
            wait.start((JFrame) parentComponent, false);
            parentComponent = wait;
            String respMsg = "";
            if ("telnet".equalsIgnoreCase(syncMode)) {
                ApacheAdjustDate dateAdjust = new ApacheAdjustDate();
                ret = dateAdjust.adjustDateTime(vehicle.getId());
            }
            else if ("ssh".equalsIgnoreCase(syncMode)) {
                // ret = SSHExec.exec(vehicle.getId(), SSHExec.ADJUST_DATE);
                // ret = SSHAdjustDate.adjust(vehicle.getId());
                // ret = SSHAdjustDate.adjust("lauv-seacon-1", SwingUtilities.getWindowAncestor(parentCompTmp));
                // SSHAdjustDate sshAdj = new SSHAdjustDate(vehicle.getId(),
                // SwingUtilities.getWindowAncestor(parentCompTmp));
                SSHAdjustDate sshAdj = new SSHAdjustDate(vehicle.getId());
                ret = sshAdj.exec();
                respMsg = sshAdj.getExecResponse();
            }
            else
                ret = false;
            if (ret) {
                JOptionPane.showMessageDialog(parentComponent, "<html>The clock of the vehicle " + vehicle.getName()
                        + "[" + vehicle.getId() + "] was set" + "with <b>success</b>!</html>");
                ret = true;
            }
            else {
                JOptionPane.showMessageDialog(parentComponent, "<html>The clock of the vehicle " + vehicle.getName()
                        + "[" + vehicle.getId() + "] " + "<b>fail</b> to synchronize!" + "<br>" + respMsg);
                ret = false;
            }

            wait.stop();
            timer.cancel(); // Terminate the timer thread
        }
    }
}
