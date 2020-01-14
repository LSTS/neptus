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
 * Author: Margarida Faria
 * Feb 19, 2013
 */
package pt.lsts.neptus.types.misc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * @author Margarida Faria
 *
 */
public class LBLRangesTimer {

    // public final static int maxTime = 600;
    private int time;

    private final Timer timer;

    public LBLRangesTimer() {
        setTime(-1);
        ActionListener taskPerformer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                incrementTime();
            }
        };
        int delay = 1000; // milliseconds
        timer = new Timer(delay, taskPerformer);
    }

    /**
     * @return the time
     */
    public int getTime() {
        return time;
    }

    /**
     * Increment time by 1.
     */
    private void incrementTime() {
        // it's the only one not using setTime (SwingUtilities.invokeLater) because is only called in an ActionListener,
        // so it's already on ETD
        time += 1;
        // NeptusLog.pub().error("time is now " + time);
    }

    public void resetTime(){
        if(!timer.isRunning()) {
            startTimer();
        }
        else{
            setTime(0);
        }
    }

    private void setTime(final int i) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                time = i;
            }
        });
    }

    public void startTimer() {
        setTime(0);
        timer.start();
    }

    public void stopTimer() {
        setTime(-1);
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

}
