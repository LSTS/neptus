/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Apr 9, 2013
 */
package pt.lsts.neptus.plugins.ais;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
public class AisShip implements Comparable<AisShip> {

    protected String name, country;
    protected double speed = 0, course = 0, latitude = 0, longitude = 0, length = 10;
    protected int mmsi = 0;
    
    protected long lastUpdate = System.currentTimeMillis();
    
    public void update(double lat, double lon, double course, double speed) {
        setLatitude(lat);
        setLongitude(lon);
        setCourse(course);
        setSpeed(speed);
        lastUpdate = System.currentTimeMillis();
    }
    
    public LocationType getLocation() {
        return new LocationType(latitude, longitude);
    }
    
    public double getSpeedMps() {
        return 0.514 * speed;
    }
    
    public double getHeadingRads() {        
        return Math.toRadians(course);
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }
    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }
    /**
     * @return the speed
     */
    public double getSpeedKnots() {
        return speed;
    }
    /**
     * @param speed the speed to set
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    /**
     * @return the course
     */
    public double getCourse() {
        return course;
    }
    /**
     * @param course the course to set
     */
    public void setCourse(double course) {
        this.course = course;
    }
    /**
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }
    /**
     * @param latitude the latitude to set
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    /**
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }
    /**
     * @param longitude the longitude to set
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    /**
     * @return the length
     */
    public double getLength() {
        return length;
    }
    /**
     * @param length the length to set
     */
    public void setLength(double length) {
        this.length = length;
    }
    
    public void setMMSI(int mmsi) {
        this.mmsi = mmsi;
    }
    
    public int getMMSI() {
        return mmsi;
    }
    
    public String getShipInfoURL() {
        return "http://www.marinetraffic.com/ais/shipdetails.aspx?mmsi="+getMMSI()+"&header=false";
    }
    
    @Override
    public int compareTo(AisShip o) {
        return getName().compareTo(o.getName());
    }
    
}
