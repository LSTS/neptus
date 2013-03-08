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
 * Feb 2, 2013
 * $Id:: MovingAverage.java 9850 2013-02-04 14:55:36Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.util;

import java.util.ArrayList;

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
        window.remove(oldest);
        window.add(oldest, value);
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

        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2.3);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(5.1);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(1.2);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(4);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(3.2);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(10.3);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(30.2);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(4.3);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2.4);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(32);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(12);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(3.4);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
        ma.update(2.3);
        System.out.println("mean=" + ma.mean() + "\t\t    stdev=" + ma.stdev() + "         " + ma.window);
    }
}
