/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Feb 19, 2013
 */
package pt.up.fe.dceg.neptus.types.misc;

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
