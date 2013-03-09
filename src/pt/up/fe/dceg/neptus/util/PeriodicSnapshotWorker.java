/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2008/10/15
 */
package pt.up.fe.dceg.neptus.util;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import pt.up.fe.dceg.neptus.console.plugins.BlinkStatusLed;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 * 
 */
public class PeriodicSnapshotWorker {

    protected Timer runner = null;
    protected TimerTask ttask = null;

    protected Component componentToSnapshot = null;
    protected String prefix = "";

    protected BlinkStatusLed blinkLed = null;



    /**
     * 
     */
    public PeriodicSnapshotWorker() {
    }

    /**
     * @param componentToSnapshot
     * @param prefix to set on the snapshot (if null the prefix will be empty)
     * @param blinkLed null if you don't want to use it
     */
    public PeriodicSnapshotWorker(Component componentToSnapshot, String prefix, BlinkStatusLed blinkLed) {
        setComponentToSnapshot(componentToSnapshot);
        setPrefix(prefix);
        setBlinkLed(blinkLed);
    }

    /**
     * @return the ComponentToSnapshot
     */
    public Component getComponentToSnapshot() {
        return componentToSnapshot;
    }

    /**
     * @param componentToSnapshot the ComponentToSnapshot
     */
    public void setComponentToSnapshot(Component componentToSnapshot) {
        this.componentToSnapshot = componentToSnapshot;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set (it adds a "-" suffix)
     */
    public void setPrefix(String prefix) {
        if (prefix == null)
            this.prefix = "";
        else if (!"".equalsIgnoreCase(prefix))
            this.prefix = prefix + "-";
    }

    /**
     * @return the blinkLed
     */
    public BlinkStatusLed getBlinkLed() {
        return blinkLed;
    }

    /**
     * @param blinkLed
     */
    public void setBlinkLed(BlinkStatusLed blinkLed) {
        this.blinkLed = blinkLed;
    }

    public void startWorking() {
        stopWorking();
        ttask = new TimerTask() {
            @Override
            public void run() {
                // FIXME Vefficar quando o componente está tapado por outro ou não
                if (componentToSnapshot != null) {
                    if (componentToSnapshot.isVisible() && componentToSnapshot.isShowing()) {
                        // Toolkit.getDefaultToolkit().
                        Window wd;
                        if (componentToSnapshot instanceof Window)
                            wd = (Window) componentToSnapshot;
                        else
                            wd = SwingUtilities.getWindowAncestor(componentToSnapshot);
                        if (wd != null)
                            if (wd instanceof Frame)
                                if ((((Frame) wd).getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED)
                                    return;

                        GuiUtils.takeSnapshot(componentToSnapshot, prefix);

                    }
                }
            }
        };

//        long pr = Long.parseLong(GeneralPreferences.getProperty(GeneralPreferences.AUTO_SNAPSHOT_PERIOD)) * 1000;
        long pr = GeneralPreferences.autoSnapshotPeriodSeconds * 1000;
        if (runner == null)
            runner = new Timer("Snapshot Runner" + ": " + prefix);

        runner.scheduleAtFixedRate(ttask, 500, pr);
    }

    public void stopWorking() {
        if (ttask != null)
            ttask.cancel();

        if (runner != null)
            runner.cancel();

        runner = null;
    }
}
