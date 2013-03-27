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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * Has information of one SPOT device.
 * 
 * @author Margarida Faria
 * 
 */
public class Spot {
    protected final SpotPageKeys pageInfo;
    protected Float speed;
    protected Double direction;
    protected LocationType lastLocation;

    /**
     * @param pageInfo
     */
    public Spot(SpotPageKeys pageInfo) {
        super();
        this.pageInfo = pageInfo;
        lastLocation = null;
        speed = null;
        direction = null;
    }

    /**
     * Get messages on SPOT page.
     * 
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private ArrayList<SpotMessage> get() throws ParserConfigurationException, SAXException, IOException {

        ArrayList<SpotMessage> updates = new ArrayList<SpotMessage>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(pageInfo.pageUrl);
        NodeList nlist = doc.getFirstChild().getChildNodes();

        for (int i = 1; i < nlist.getLength(); i++) {
            String tagName = nlist.item(i).getNodeName();
            if (tagName.equals("message")) {
                double lat = 0, lon = 0;
                String id = "SPOT";
                long timestamp = System.currentTimeMillis();

                NodeList elems = nlist.item(i).getChildNodes();
                for (int j = 0; j < elems.getLength(); j++) {
                    String tag = elems.item(j).getNodeName();

                    if (tag.equals("latitude"))
                        lat = Double.parseDouble(elems.item(j).getTextContent());
                    else if (tag.equals("longitude"))
                        lon = Double.parseDouble(elems.item(j).getTextContent());
                    else if (tag.equals("esnName"))
                        id = elems.item(j).getTextContent();
                    else if (tag.equals("timeInGMTSecond")) {
                        timestamp = Long.parseLong(elems.item(j).getTextContent());
                        timestamp *= 1000; // secs to millis
                    }
                }
                updates.add(new SpotMessage(lat, lon, timestamp, id));
            }
        }
        return updates;
    }

    /**
     * This is a slow operation and should be called from a background thread. The update of variables is scheduled in
     * the EDT.<br>
     * For each SPOT the messages: are fetched from the page and the location, speed and direction angle set.
     */
    public void update() {
        // ask for messages
        try {
            System.out.println("Gonna ask for Spot messages");
            ArrayList<SpotMessage> messages = get();
            System.out.println("Got " + messages.size() + " spot messages");
            // calculate direction and speed
            final LocationSpeedDirection speedLocationDirection = setSpeedMpsAndDirection(messages);
            System.out.println("Speed:" + speedLocationDirection.speed + ", direction:"
                    + speedLocationDirection.direction + " [" + speedLocationDirection.location.getLatitude() + ","
                    + speedLocationDirection.location.getLongitude() + "]");
            // update in EDT
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    speed = speedLocationDirection.speed;
                    direction = speedLocationDirection.direction;
                    lastLocation = speedLocationDirection.location;
                    System.out.println("Gonna update speed and diractions variables in EDT "
                            + SwingUtilities.isEventDispatchThread());
                }
            });
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Takes into account the data for the last hour, if older date is found it will be removed. Sets the direction
     * angle as the angle of the vector composed by the weighted mean of all the movement vectors.
     * 
     */
    private LocationSpeedDirection setSpeedMpsAndDirection(ArrayList<SpotMessage> messages) {
        long currentTime = System.currentTimeMillis();
        long timeWindow = 3600000;
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
            // if (tmpMsg.timestamp > (currentTime - timeWindow)) { TODO fix previous location and msg assigments for
            // case that message is out of timewindow
                numMeasurements++;
                if (prevMsg != null) {
                System.out.println("Processing message " + numMeasurements);
                    distanceInMeters = tmpLocation.getDistanceInMeters(prevLocation);
                    elapsedTime = tmpMsg.timestamp - prevMsg.timestamp;
                    speedMeterSecond = distanceInMeters / (elapsedTime / 1000);
                    sumSpeed += speedMeterSecond;
                    latDif = tmpLocation.getLatitudeAsDoubleValueRads() - prevLocation.getLatitudeAsDoubleValueRads();
                    lonDif = tmpLocation.getLongitudeAsDoubleValueRads() - prevLocation.getLongitudeAsDoubleValueRads();
                    // weighted sum
                    sumDirVector.latitude += latDif * numMeasurements;
                    sumDirVector.longitude += lonDif * numMeasurements;
                }
                prevMsg = tmpMsg;
                prevLocation = tmpLocation;
            // }
        }
        double factorial = gamma(numMeasurements - 1);
        // weighted mean
        sumDirVector.latitude = sumDirVector.latitude / factorial;
        sumDirVector.longitude = sumDirVector.longitude / factorial;
        return new LocationSpeedDirection(new Float(sumSpeed / (numMeasurements - 1)), Math.atan(sumDirVector.longitude
                / sumDirVector.latitude), prevLocation);
    }

    static double gamma(double z) {
        double tmp1 = Math.sqrt(2 * Math.PI / z);
        double tmp2 = z + 1.0 / (12 * z - 1.0 / (10 * z));
        tmp2 = Math.pow(tmp2 / Math.E, z);
        return tmp1 * tmp2;
    }

    // private void addData(double lat, double lon, long timestamp) {
    // locations.add(new TimedLocation(timestamp, new LocationType(lat, lon)));
    // direction = null;
    // }

    public LocationType getLastLocation() {
        return lastLocation;
    }

    public String getName() {
        return pageInfo.id;
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

