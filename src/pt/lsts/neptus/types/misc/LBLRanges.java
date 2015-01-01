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
 * Author: Margarida Faria
 * Feb 19, 2013
 */
package pt.lsts.neptus.types.misc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

/**
 * @author Margarida Faria
 *
 */
public class LBLRanges {

    public final static int maxTime = 300;
    private int time;

    private final Timer timer;

    public LBLRanges() {
        setTime(-1);
        ActionListener taskPerformer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                incrementTime();
                if (getTime() > maxTime) { // after 10 minutes with no reset it stops
                    stopTimer();
                }
            }
        };
        int delay = 1000; // milliseconds
        timer = new Timer(delay, taskPerformer);
    }

    /**
     * @return the time
     */
    public synchronized int getTime() {
        return time;
    }

    /**
     * Increment time by 1.
     */
    public synchronized void incrementTime() {
        time += 1;
    }

    public void resetTime(){
        if(!timer.isRunning()) {
            startTimer();
        }
        else{
            setTime(0);
        }
    }

    private synchronized void setTime(int i) {
        time = i;
    }

    public synchronized void startTimer() {
        time = 0;
        timer.start();
    }

    public synchronized void stopTimer() {
        time = -1;
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

}
