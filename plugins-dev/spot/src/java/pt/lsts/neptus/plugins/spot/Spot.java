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
 * Author: Margarida Faria
 * Mar 25, 2013
 */
package pt.lsts.neptus.plugins.spot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.lsts.neptus.types.coord.LocationType;


/**
 * Has information of one SPOT device.
 * 
 * @author Margarida Faria
 * 
 */
public class Spot {
    public static Logger log = LogManager.getLogger("SPOT");

    // protected final SpotPageKeys pageInfo;
    private final String id;
    protected Float speed;
    protected Double direction;
    protected ArrayList<LocationType> lastLocations;

    // private final ArrayList<LocationType> lastLocations;

    /**
     * @param pageInfo
     */
    public Spot(String id) {
        super();
        // this.pageInfo = pageInfo;
        this.id = id;
        speed = null;
        direction = null;
        lastLocations = new ArrayList<LocationType>();
    }


    /**
     * This is a slow operation and should be called from a background thread. The update of variables is scheduled in
     * the EDT.<br>
     * For each SPOT the messages: are fetched from the page and the location, speed and direction angle set.
     */
    public void update(final TreeSet<SpotMessage> messages) {
        // ask for messages
        // Spot.log.debug(id + " has " + messages.size() + " messages");
        // calculate direction and speed
        final LocationSpeedDirection speedLocationDirection = setSpeedMpsAndDirection(messages);
        // Spot.log.debug("Speed:" + speedLocationDirection.speed + ", direction:" + speedLocationDirection.direction
        // + " [" + speedLocationDirection.location.getLatitude() + ","
        // + speedLocationDirection.location.getLongitude() + "]");
        // update in EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                speed = speedLocationDirection.speed;
                direction = speedLocationDirection.direction;
                lastLocations = speedLocationDirection.locations;
                // Spot.log.debug("Gonna update " + messages.first().id + " with " + speed + " m/s, at "
                // + lastLocation.toString());
            }
        });
    }

    /**
     * Takes into account the data for the last hour, if older date is found it will be removed. Sets the direction
     * angle as the angle of the vector composed by the weighted mean of all the movement vectors.
     * 
     */
    private LocationSpeedDirection setSpeedMpsAndDirection(TreeSet<SpotMessage> messages) {
        if (messages.size() == 1) {
            // Spot.log.debug("Just one message");
            LocationType location = new LocationType(messages.first().latitude, messages.first().longitude);
            ArrayList<LocationType> locations = new ArrayList<LocationType>();
            locations.add(location);
            return new LocationSpeedDirection(LocationSpeedDirection.NO_VALUE_F, -LocationSpeedDirection.NO_VALUE_D,
                    locations);
        }

        long elapsedTime;
        int numMeasurements = 0;
        double sumSpeed = 0;
        double distanceInMeters, speedMeterSecond;
        SpotMessage tmpMsg;
        LocationType tmpLocation, prevLocation;
        ArrayList<LocationType> locations = new ArrayList<LocationType>();
        SpotMessage prevMsg = null;
        tmpLocation = prevLocation = null;
        Vector3f sumDirVector = new Vector3f(0f, 0f, 0f);
        double[] movementVectorArray;
        Vector3f movementVector;
        for (Iterator<SpotMessage> it = messages.iterator(); it.hasNext();) {
            tmpMsg = it.next();
            tmpLocation = new LocationType(tmpMsg.latitude, tmpMsg.longitude);// tmpMsg.getLocation();
            locations.add(tmpLocation);
            numMeasurements++;
            if (prevMsg != null) {
                distanceInMeters = tmpLocation.getDistanceInMeters(prevLocation);
                elapsedTime = tmpMsg.timestamp - prevMsg.timestamp;
                speedMeterSecond = distanceInMeters / elapsedTime;
                log.debug("Traveled " + distanceInMeters + " in " + elapsedTime + " = " + speedMeterSecond + "  ("
                        + tmpMsg.latitude + ", " + tmpMsg.longitude + " at " + tmpMsg.timestamp);
                sumSpeed += speedMeterSecond;
                movementVectorArray = prevLocation.getOffsetFrom(tmpLocation);
                movementVector = new Vector3f(Float.valueOf(Double.valueOf(movementVectorArray[0]).floatValue()), Float.valueOf(
                        Double.valueOf(movementVectorArray[1]).floatValue()),
                        0f);
                movementVector = movementVector.divide(movementVector.length());
                // Spot.log.debug("Direction: (" + movementVectorArray[0] + ", " + movementVectorArray[1] + ")  --> ("
                // + movementVector.x + ", " + movementVector.y + ")");
                // weighted sum
                sumDirVector.x = movementVector.x * numMeasurements;
                sumDirVector.y = movementVector.y * numMeasurements;


                // latDif = tmpLocation.getLatitudeAsDoubleValueRads() - prevLocation.getLatitudeAsDoubleValueRads();
                // lonDif = tmpLocation.getLongitudeAsDoubleValueRads() - prevLocation.getLongitudeAsDoubleValueRads();
                // // weighted sum
                // sumDirVector.latitude += latDif * numMeasurements;
                // sumDirVector.longitude += lonDif * numMeasurements;
            }
            prevMsg = tmpMsg;
            prevLocation = tmpLocation;
        }
        Float factorial = Float.valueOf(Double.valueOf(gamma(numMeasurements - 1)).floatValue());
        // weighted mean
        sumDirVector.x = sumDirVector.x / factorial;
        sumDirVector.y = sumDirVector.y / factorial;
        Float finalSpeed = Float.valueOf(Double.valueOf(sumSpeed / (numMeasurements - 1)).floatValue());
        double finalDirection = Math.atan(sumDirVector.y / sumDirVector.x);
        log.debug("finalSpeed " + finalSpeed + ", direction: (" + sumDirVector.x + ", " + sumDirVector.y + ")"
                + finalDirection);
        return new LocationSpeedDirection(finalSpeed, finalDirection, locations);
    }

    static double gamma(double z) {
        double tmp1 = Math.sqrt(2 * Math.PI / z);
        double tmp2 = z + 1.0 / (12 * z - 1.0 / (10 * z));
        tmp2 = Math.pow(tmp2 / Math.E, z);
        return tmp1 * tmp2;
    }


    public LocationType getLastLocation() {
        int size = lastLocations.size();
        if (size > 0) {
            return lastLocations.get(size - 1);
        }
        else {
            return null;
        }
    }

    public ArrayList<LocationType> getLastLocations() {
        return lastLocations;
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
        public final static float NO_VALUE_F = -1;
        public final static double NO_VALUE_D = -1;
        public final double direction;
        public final Float speed;
        public final ArrayList<LocationType> locations;

        /**
         * @param latRads
         * @param lonRads
         */
        public LocationSpeedDirection(float speed, double direction, ArrayList<LocationType> locations) {
            super();
            this.speed = speed;
            this.direction = direction;
            this.locations = locations;
        }
    }

}

