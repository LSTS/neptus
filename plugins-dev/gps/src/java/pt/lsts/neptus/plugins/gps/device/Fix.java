/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: rasm
 * Apr 14, 2011
 */
package pt.lsts.neptus.plugins.gps.device;

/**
 * Class representing a GPS fix (solution).
 * 
 * @author Ricardo Martins
 */
public class Fix {

    /**
     * Fix type.
     */
    public enum Type {
        /** Standalone solution (no external aiding). */
        STANDALONE,
        /** Differential solution. */
        DIFFERENTIAL
    }

    /** True if solution is valid. */
    private boolean valid;
    /** Solution type. */
    private Type type;
    /** WGS-84 latitude in degrees. */
    private double latitude;
    /** WGS-84 longitude in degrees. */
    private double longitude;
    /** Height above WGS-84 ellipsoid. */
    private double height;
    /** Number of satellites in view. */
    private int satellites;
    /** Horizontal accuracy estimate. */
    private double horizontalAccuracy;
    /** Vertical accuracy estimate */
    private double verticalAccuracy;
    /** Horizontal dilution of precision. */
    private double horizontalDilution;
    /** Vertical dilution of precision. */
    private double verticalDilution;
    /** UTC date. */
    private Date date;
    /** UTC time. */
    private Time time;
    /** Ground speed. */
    private double speedOverGround;
    /** Course over ground. */
    private double courseOverGround;

    /**
     * Test if the solution if valid.
     * 
     * @return true if the solution is valid, false otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Set the validity of the solution.
     * 
     * @param value
     *            true if the solution is valid, false otherwise.
     */
    public void setValid(boolean value) {
        valid = value;
    }

    /**
     * Get the type of the solution.
     * 
     * @return type of the solution.
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the type of the solution.
     * 
     * @param value
     *            type of the solution.
     */
    public void setType(Type value) {
        type = value;
    }

    /**
     * Get the WGS-84 latitude in degrees.
     * 
     * @return WGS-84 latitude in degrees.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Set the WGS-84 latitude in degrees.
     * 
     * @param value
     *            WGS-84 latitude in degrees.
     */
    public void setLatitude(double value) {
        latitude = value;
    }

    /**
     * Get the WGS-84 longitude in degrees.
     * 
     * @return WGS-84 longitude in degrees.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Set the WGS-84 longitude in degrees.
     * 
     * @param value
     *            WGS-84 longitude in degrees.
     */
    public void setLongitude(double value) {
        longitude = value;
    }

    /**
     * Get height above the WGS-84 ellipsoid.
     * 
     * @return height above the WGS-84 ellipsoid.
     */
    public double getHeight() {
        return height;
    }

    /**
     * Set height above the WGS-84 ellipsoid.
     * 
     * @param value
     *            height above the WGS-84 ellipsoid.
     */
    public void setHeight(double value) {
        height = value;
    }

    /**
     * Get the number of satellites used to calculate the solution.
     * 
     * @return number of satellites used to calculate the solution.
     */
    public int getSatellites() {
        return satellites;
    }

    /**
     * Set the number of satellites used to calculate the solution.
     * 
     * @param count
     *            number of satellites used to calculate the solution.
     */
    public void setSatellites(int count) {
        satellites = count;
    }

    /**
     * Get the estimated horizontal accuracy of the solution.
     * 
     * @return estimated horizontal accuracy of the solution.
     */
    public double getHorizontalAccuracy() {
        return horizontalAccuracy;
    }

    /**
     * Set the estimated horizontal accuracy of the solution.
     * 
     * @param value
     *            estimated horizontal accuracy of the solution.
     */
    public void setHorizontalAccuracy(double value) {
        horizontalAccuracy = value;
    }

    /**
     * Get the estimated vertical accuracy of the solution.
     * 
     * @return estimated vertical accuracy of the solution.
     */
    public double getVerticalAccuracy() {
        return verticalAccuracy;
    }

    /**
     * Set the estimated vertical accuracy of the solution.
     * 
     * @param value
     *            estimated vertical accuracy of the solution.
     */
    public void setVerticalAccuracy(double value) {
        verticalAccuracy = value;
    }

    /**
     * Get the horizontal dilution of precision of the solution.
     * 
     * @return horizontal dilution of precision.
     */
    public double getHorizontalDilution() {
        return horizontalDilution;
    }

    /**
     * Set the horizontal dilution of precision.
     * 
     * @param value
     *            horizontal dilution of precision.
     */
    public void setHorizontalDilution(double value) {
        horizontalDilution = value;
    }

    /**
     * Get the vertical dilution of precision of the solution.
     * 
     * @return vertical dilution of precision.
     */
    public double getVerticalDilution() {
        return verticalDilution;
    }

    /**
     * Set vertical dilution of precision.
     * 
     * @param value
     *            vertical dilution of precision.
     */
    public void setVerticalDilution(double value) {
        verticalDilution = value;
    }

    /**
     * Set UTC date.
     * 
     * @return UTC date.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Set UTC date.
     * 
     * @param aDate
     *            UTC date.
     */
    public void setDate(Date aDate) {
        date = aDate;
    }

    /**
     * Get UTC time.
     * 
     * @return UTC time.
     */
    public Time getTime() {
        return time;
    }

    /**
     * Set UTC time.
     * 
     * @param time
     *            UTC time.
     */
    public void setTime(Time aTime) {
        time = aTime;
    }

    /**
     * Get ground speed in m/s.
     * 
     * @return ground speed in m/s.
     */
    public double getSog() {
        return speedOverGround;
    }

    /**
     * Set ground speed in m/s.
     * 
     * @param value
     *            ground speed in m/s.
     */
    public void setSog(double value) {
        speedOverGround = value;
    }

    /**
     * Get course over ground in degrees.
     * 
     * @return course over ground in degrees.
     */
    public double getCog() {
        return courseOverGround;
    }

    /**
     * Set course over ground in degrees.
     * 
     * @param value
     *            course over ground in degrees.
     */
    public void setCog(double value) {
        courseOverGround = value;
    }
}
