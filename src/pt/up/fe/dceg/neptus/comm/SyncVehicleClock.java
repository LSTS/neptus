/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 2005/06/15
 * $Id:: SyncVehicleClock.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.comm;

import java.awt.Component;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import pt.up.fe.dceg.neptus.gui.WaitPanel;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.comm.ApacheAdjustDate;
import pt.up.fe.dceg.neptus.util.comm.CommUtil;
import pt.up.fe.dceg.neptus.util.comm.ssh.SSHAdjustDate;

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
