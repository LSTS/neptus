/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 2, 2013
 */
package pt.lsts.neptus.util;

import java.util.ArrayList;

import pt.lsts.neptus.NeptusLog;

/**
 * @author pdias (taken from DUNE, by Ricardo Martins)
 * 
 */
public class MovingAverage {

    /**
     * Accumulator
     */
    private double accum = 0;
    /**
     * Window size.
     */
    private short windowSize = 0;
    /**
     * Window.
     */
    private ArrayList<Double> window = new ArrayList<>();
    /**
     * Index of oldest value.
     */
    private int oldest = 0;

    /**
     * 
     */
    public MovingAverage(short windowSize) {
        this.windowSize = windowSize < 1 ? 1 : windowSize;
        clear();
    }

    /**
     * Clear sample.
     */
    public void clear() {
        accum = 0;
        oldest = 0;
        window.clear();
    }

    /**
     * Update sample with new value.
     * 
     * @param value
     * @return
     */
    public double update(double value) {
        if (window.size() < windowSize) {
            window.add(value);
            accum += value;
            return accum / window.size();
        }

        accum += value - window.get(oldest);
        window.set(oldest, value);
        oldest = (oldest + 1) % windowSize;
        return accum / windowSize;
    }

    /**
     * Extract mean value of the sample.
     * 
     * @return
     */
    public double mean() {
        if (window.size() > 0)
            return accum / window.size();
        else
            return 0;
    }

    /**
     * Extract standard deviation of the sample.
     * 
     * @return
     */
    public double stdev() {
        int size = window.size();

        if (size == 0)
            return 0;

        double sdev = 0;
        double u = mean();

        for (int i = 0; i < size; i++)
            sdev += (window.get(i) - u) * (window.get(i) - u);

        return Math.sqrt(sdev / size);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "mean=" + mean() + "    \t\t stdev=" + stdev() + "         \t" + window;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        MovingAverage ma = new MovingAverage((short) 5);

        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2.3);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(5.1);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(1.2);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(4);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(3.2);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(10.3);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(30.2);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(4.3);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2.4);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(32);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(12);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(3.4);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2.3);
        NeptusLog.pub().info("<###>mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
    }
}
