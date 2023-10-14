/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2008/10/15
 */
package pt.lsts.neptus.util;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import pt.lsts.neptus.console.plugins.BlinkStatusLed;
import pt.lsts.neptus.util.conf.GeneralPreferences;

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
