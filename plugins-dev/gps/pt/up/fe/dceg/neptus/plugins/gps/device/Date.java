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
 * Class representing a broken down UTC date.
 * 
 * @author Ricardo Martins
 */
public class Date {
    /** UTC day of the month. */
    private int day;
    /** UTC month of the year. */
    private int month;
    /** UTC year. */
    private int year;

    /**
     * Get the day of the month.
     * 
     * @return UTC day of the month.
     */
    public int getDay() {
        return day;
    }

    /**
     * Set the day of the month.
     * 
     * @param aDay
     *            day of the month.
     */
    public void setDay(int aDay) {
        day = aDay;
    }

    /**
     * Get the month of the year.
     * 
     * @return month of the year.
     */
    public int getMonth() {
        return month;
    }

    /**
     * Set the month of the year.
     * 
     * @param aMonth
     *            month of the year.
     */
    public void setMonth(int aMonth) {
        month = aMonth;
    }

    /**
     * Get the year.
     * 
     * @return year.
     */
    public int getYear() {
        return year;
    }

    /**
     * Set the year.
     * 
     * @param aYear
     *            year.
     */
    public void setYear(int aYear) {
        year = aYear;
    }
}
