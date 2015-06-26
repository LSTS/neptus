/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Correia
 * Nov 16, 2012
 */
package pt.lsts.neptus.mra;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 *
 */
public class LogMarker implements Serializable, Comparable<LogMarker> {
    private static final long serialVersionUID = 1L;

    private String label;
    private String description;
    
    /**
     * Time stamp in milliseconds
     */
    private double timestamp;

    /**
     * Latitude in radians
     */
    private double lat;
    /**
     * Longitude in radians
     */
    private double lon;
    
    /**
     * @param label Text to associate with the marker
     * @param timestamp in milliseconds
     * @param lat Latitude, in radians of the marker. Use 0 if not available.
     * @param lon Longitude, in radians of the marker. Use 0 if not available.
     */
    public LogMarker(String label, String description, double timestamp, double lat, double lon) {
        super();
        this.label = label;
        this.description = description;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
    }
    
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int compareTo(LogMarker o) {
        if (o.timestamp > timestamp)
            return -1;
        else if (o.timestamp < timestamp)
            return 1;
        else
            return 0;
    }
    
    @SuppressWarnings("unchecked")
    public static Collection<LogMarker> load(IMraLogGroup source) {
        ArrayList<LogMarker> logMarkers = new ArrayList<LogMarker>();
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(source.getFile("Data.lsf").getParent()
                    + "/marks.dat"));
            for (LogMarker marker : (ArrayList<LogMarker>) ois.readObject()) {
                logMarkers.add(marker);                
            }
            ois.close();
            
        }
        catch (Exception e) {
            NeptusLog.pub().info("No markers for this log, or erroneous mark file");
        }
        return logMarkers;
    }
    
    public static void save(ArrayList<LogMarker> logMarkers, IMraLogGroup source) {
        try {
            ObjectOutputStream dos = new ObjectOutputStream(new FileOutputStream(source.getFile(".").getParent()
                    + "/marks.dat"));
            dos.writeObject(logMarkers);
            dos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
   
    
    public LocationType getLocation() {
        return new LocationType(Math.toDegrees(lat), Math.toDegrees(lon));
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the timestamp
     */
    public double getTimestamp() {
        return timestamp;
    }

    /**
     * @return the lat
     */
    public double getLat() {
        return lat;
    }

    /**
     * @param lat the lat to set
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

    /**
     * @return the lon
     */
    public double getLon() {
        return lon;
    }

    /**
     * @param lon the lon to set
     */
    public void setLon(double lon) {
        this.lon = lon;
    }
    
    public Date getDate() {
        return new Date((long)timestamp);
    }


}