/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by rasm
 * Apr 12, 2011
 */
package pt.up.fe.dceg.neptus.plugins.gps.device;

/**
 * Class representing a broken down UTC time.
 * 
 * @author Ricardo Martins
 */
public class Time {
    /** Hour of the day. */
    private int hour;
    /** Minute of the hour. */
    private int minutes;
    /** Seconds of the minute. */
    private double seconds;

    /**
     * Get the hour of the day.
     * 
     * @return hour of the day
     */
    public int getHour() {
        return hour;
    }

    /**
     * Set the hour of the day.
     * 
     * @param aHour
     *            hour of the day.
     */
    public void setHour(int aHour) {
        hour = aHour;
    }

    /**
     * Get minutes of the hour.
     * 
     * @return minutes of the hour.
     */
    public int getMinutes() {
        return minutes;
    }

    /**
     * Set minutes of the hour.
     * 
     * @param m
     *            minutes of the hour.
     */
    public void setMinutes(int m) {
        minutes = m;
    }

    /**
     * Get seconds of the minute.
     * 
     * @return seconds of the minute.
     */
    public double getSeconds() {
        return seconds;
    }

    /**
     * Set seconds of the minute.
     * 
     * @param s
     *            seconds of the minute.
     */
    public void setSeconds(double s) {
        seconds = s;
    }
}
