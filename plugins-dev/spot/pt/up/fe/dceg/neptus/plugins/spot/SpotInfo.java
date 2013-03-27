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

import java.util.ArrayList;
import java.util.Iterator;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * Has information of one SPOT device. Everytime a new location is received, the speed and direction are invalidated.
 * When either of them is needed, both will be recalculated if invalid (null).
 * 
 * @author Margarida Faria
 * 
 */
public class SpotInfo {
    protected String name;
    protected Float speed;
    protected Double lat, lon, direction;
    protected ArrayList<TimedLocation> locations;

    public void addData(double lat, double lon, long timestamp) {
        locations.add(new TimedLocation(timestamp, new LocationType(lat, lon)));
        direction = null;
    }

    public LocationType getLastLocation() {
        return locations.get(locations.size() - 1).getLocation();
    }

    public String getName() {
        return name;
    }

    /**
     * Takes into account the data for the last hour, if older date is found it will be removed. Sets the direction
     * angle as the angle of the vector composed by the weighted mean of all the movement vectors.
     * 
     */
    private void setSpeedMpsAndDirection() {
        long currentTime = System.currentTimeMillis();
        long timeWindow = 3600000;
        long elapsedTime;
        int numMeasurements = 0;
        double sumSpeed = 0;
        double distanceInMeters, speedMeterSecond;
        TimedLocation tmpTimedLocation;
        TimedLocation prevTimedLocation = null;
        LocationType tmpLocation, prevLocation;
        DirVector sumDirVector = new DirVector(0, 0);
        for (Iterator<TimedLocation> it = locations.iterator(); it.hasNext();) {
            tmpTimedLocation = it.next();
            if (tmpTimedLocation.getTimestamp() < (currentTime - timeWindow)) {
                it.remove();
            }
            else {
                numMeasurements++;
                if (prevTimedLocation != null) {
                    tmpLocation = tmpTimedLocation.getLocation();
                    prevLocation = prevTimedLocation.getLocation();
                    distanceInMeters = tmpLocation.getDistanceInMeters(prevLocation);
                    elapsedTime = tmpTimedLocation.getTimestamp() - prevTimedLocation.getTimestamp();
                    speedMeterSecond = distanceInMeters / (elapsedTime / 1000);
                    sumSpeed += speedMeterSecond;
                    double latDif = tmpLocation.getLatitudeAsDoubleValueRads()-prevLocation.getLatitudeAsDoubleValueRads();
                    double lonDif = tmpLocation.getLongitudeAsDoubleValueRads()-prevLocation.getLongitudeAsDoubleValueRads();
                    sumDirVector.latitude += latDif * numMeasurements;
                    sumDirVector.longitude += lonDif * numMeasurements;
                }
                prevTimedLocation = tmpTimedLocation;
            }
        }
        double factorial = gamma(numMeasurements - 1);
        sumDirVector.latitude = sumDirVector.latitude / factorial;
        sumDirVector.longitude = sumDirVector.longitude / factorial;
        direction = Math.atan(sumDirVector.longitude / sumDirVector.latitude);
        speed = new Float(sumSpeed / (numMeasurements - 1));
    }

    static double gamma(double z) {
        double tmp1 = Math.sqrt(2 * Math.PI / z);
        double tmp2 = z + 1.0 / (12 * z - 1.0 / (10 * z));
        tmp2 = Math.pow(z / Math.E, z); // ooops; thanks hj
        tmp2 = Math.pow(tmp2 / Math.E, z);
        return tmp1 * tmp2;
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

    /**
     * @return the speed in meters per second
     */
    public float getSpeed() {
        if (speed == null)
            setSpeedMpsAndDirection();
        return speed;
    }

    /**
     * @return the direction in radians
     */
    public Double getDirection() {
        if (direction == null)
            setSpeedMpsAndDirection();
        return direction;
    }
}

