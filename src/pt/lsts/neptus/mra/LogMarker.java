/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * This will be serializable, so no name changes of the fields!
 * @author jqcorreia
 *
 */
public class LogMarker implements Serializable, Comparable<LogMarker> {
    private static final long serialVersionUID = 1L;

    protected String label;
    
    /**
     * Time stamp in milliseconds
     */
    protected double timestamp;

    /**
     * Latitude in radians
     */
    protected double lat;
    /**
     * Longitude in radians
     */
    protected double lon;
    
    /**
     * @param label Text to associate with the marker
     * @param timestamp in milliseconds
     * @param latRads Latitude, in radians of the marker. Use 0 if not available.
     * @param lonRads Longitude, in radians of the marker. Use 0 if not available.
     */
    public LogMarker(String label, double timestamp, double latRads, double lonRads) {
        super();
        this.label = label;
        this.timestamp = timestamp;
        this.lat = latRads;
        this.lon = lonRads;
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
    public double getLatRads() {
        return lat;
    }

    /**
     * @param latRads the lat to set
     */
    public void setLatRads(double latRads) {
        this.lat = latRads;
    }

    /**
     * @return the lon
     */
    public double getLonRads() {
        return lon;
    }

    /**
     * @param lonRads the lon to set
     */
    public void setLonRads(double lonRads) {
        this.lon = lonRads;
    }
    
    public Date getDate() {
        return new Date((long)timestamp);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return label;
    }
}