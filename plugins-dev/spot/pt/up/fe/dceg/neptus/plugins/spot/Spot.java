/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Mar 25, 2013
 */
package pt.up.fe.dceg.neptus.plugins.spot;

import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * Has information of one SPOT device.
 * 
 * @author Margarida Faria
 * 
 */
public class Spot {
    public static Logger log = Logger.getLogger("SPOT");

    // protected final SpotPageKeys pageInfo;
    private final String id;
    protected Float speed;
    protected Double direction;
    protected LocationType lastLocation;

    /**
     * @param pageInfo
     */
    public Spot(String id) {
        super();
        // this.pageInfo = pageInfo;
        this.id = id;
        lastLocation = null;
        speed = null;
        direction = null;
    }


    /**
     * This is a slow operation and should be called from a background thread. The update of variables is scheduled in
     * the EDT.<br>
     * For each SPOT the messages: are fetched from the page and the location, speed and direction angle set.
     */
    public void update(TreeSet<SpotMessage> messages) {
        // ask for messages
        Spot.log.debug(id + " has " + messages.size() + " messages");
        // calculate direction and speed
        final LocationSpeedDirection speedLocationDirection = setSpeedMpsAndDirection(messages);
        Spot.log.debug("Speed:" + speedLocationDirection.speed + ", direction:" + speedLocationDirection.direction
                + " [" + speedLocationDirection.location.getLatitude() + ","
                + speedLocationDirection.location.getLongitude() + "]");
        // update in EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                speed = speedLocationDirection.speed;
                direction = speedLocationDirection.direction;
                lastLocation = speedLocationDirection.location;
                Spot.log.debug("Gonna update speed and directions variables in EDT "
                        + SwingUtilities.isEventDispatchThread());
            }
        });
    }

    /**
     * Takes into account the data for the last hour, if older date is found it will be removed. Sets the direction
     * angle as the angle of the vector composed by the weighted mean of all the movement vectors.
     * 
     */
    private LocationSpeedDirection setSpeedMpsAndDirection(TreeSet<SpotMessage> messages) {
        long elapsedTime;
        int numMeasurements = 0;
        double sumSpeed = 0;
        double distanceInMeters, speedMeterSecond, latDif, lonDif;
        SpotMessage tmpMsg;
        LocationType tmpLocation, prevLocation;
        SpotMessage prevMsg = null;
        tmpLocation = prevLocation = null;
        DirVector sumDirVector = new DirVector(0, 0);
        for (Iterator<SpotMessage> it = messages.iterator(); it.hasNext();) {
            tmpMsg = it.next();
            tmpLocation = new LocationType(tmpMsg.latitude, tmpMsg.longitude);// tmpMsg.getLocation();
                numMeasurements++;
                if (prevMsg != null) {
                    distanceInMeters = tmpLocation.getDistanceInMeters(prevLocation);
                    elapsedTime = tmpMsg.timestamp - prevMsg.timestamp;
                speedMeterSecond = distanceInMeters / elapsedTime;
                log.debug("Traveled " + distanceInMeters + " in " + elapsedTime + " = " + speedMeterSecond + "  ("
                        + tmpMsg.latitude + ", " + tmpMsg.longitude + " at " + tmpMsg.timestamp);
                    sumSpeed += speedMeterSecond;
                    latDif = tmpLocation.getLatitudeAsDoubleValueRads() - prevLocation.getLatitudeAsDoubleValueRads();
                    lonDif = tmpLocation.getLongitudeAsDoubleValueRads() - prevLocation.getLongitudeAsDoubleValueRads();
                    // weighted sum
                    sumDirVector.latitude += latDif * numMeasurements;
                    sumDirVector.longitude += lonDif * numMeasurements;
                }
                prevMsg = tmpMsg;
                prevLocation = tmpLocation;
        }
        double factorial = gamma(numMeasurements - 1);
        // weighted mean
        sumDirVector.latitude = sumDirVector.latitude / factorial;
        sumDirVector.longitude = sumDirVector.longitude / factorial;
        Float finalSpeed = new Float(sumSpeed / (numMeasurements - 1));
        double finalDirection = Math.atan(sumDirVector.longitude / sumDirVector.latitude);
        log.debug("finalSpeed " + finalSpeed + "direction" + finalDirection);
        return new LocationSpeedDirection(finalSpeed, finalDirection, prevLocation);
    }

    static double gamma(double z) {
        double tmp1 = Math.sqrt(2 * Math.PI / z);
        double tmp2 = z + 1.0 / (12 * z - 1.0 / (10 * z));
        tmp2 = Math.pow(tmp2 / Math.E, z);
        return tmp1 * tmp2;
    }


    public LocationType getLastLocation() {
        return lastLocation;
    }

    public String getName() {
        return id;
    }


    /**
     * @return the speed in meters per second
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * @return the direction in radians
     */
    public Double getDirection() {
        return direction;
    }

    class DirVector {
        public double latitude, longitude;

        /**
         * @param latitude
         * @param longitude
         */
        public DirVector(double latitude, double longitude) {
            super();
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    class LocationSpeedDirection {
        public final double direction;
        public final Float speed;
        public final LocationType location;

        /**
         * @param latitude
         * @param longitude
         */
        public LocationSpeedDirection(float speed, double direction, LocationType location) {
            super();
            this.speed = speed;
            this.direction = direction;
            this.location = location;
        }
    }
}

