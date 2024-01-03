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
 * Author: José Correia
 * Nov 16, 2012
 */
package pt.lsts.neptus.mra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * This will be serializable, so no name changes of the fields!
 *
 * @author jqcorreia
 */
public class LogMarker implements Serializable, Comparable<LogMarker> {
    private static final long serialVersionUID = 1L;
    private static final String MARKER_FILE = "marks.dat";
    private String label;
    private double timestamp;
    private double lat;
    private double lon;

    /**
     * @param label     Text to associate with the marker
     * @param timestamp in milliseconds
     * @param latRads   Latitude, in radians of the marker. Use 0 if not available.
     * @param lonRads   Longitude, in radians of the marker. Use 0 if not available.
     */
    public LogMarker(String label, double timestamp, double latRads, double lonRads) {
        super();
        this.setLabel(label);
        this.setTimestamp(timestamp);
        this.setLatRads(latRads);
        this.setLonRads(lonRads);
    }

    @Override
    public int compareTo(LogMarker o) {
        if (o.getTimestamp() > getTimestamp())
            return -1;
        else if (o.getTimestamp() < getTimestamp())
            return 1;
        else
            return 0;
    }

    @SuppressWarnings("unchecked")
    public static Collection<LogMarker> load(IMraLogGroup source) {
        ArrayList<LogMarker> logMarkers = new ArrayList<>();
        try {
            String folder = source.getFile("Data.lsf").getParent();
            InputStream stream = new FileInputStream(folder + File.separator + MARKER_FILE);
            ObjectInputStream ois = new ObjectInputStream(stream);
            logMarkers.addAll(((ArrayList<LogMarker>) ois.readObject()));
            ois.close();

        } catch (Exception e) {
            NeptusLog.pub().info("No markers for this log, or erroneous mark file");
        }
        return logMarkers;
    }

    public static void save(ArrayList<LogMarker> logMarkers, IMraLogGroup source) {
        try {
            String folder = source.getFile(".").getParent();
            OutputStream stream = new FileOutputStream(folder + File.separator + MARKER_FILE);
            ObjectOutputStream dos = new ObjectOutputStream(stream);
            dos.writeObject(logMarkers);
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LocationType getLocation() {
        return new LocationType(Math.toDegrees(getLatRads()), Math.toDegrees(getLonRads()));
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatRads() {
        return lat;
    }

    public void setLatRads(double latRads) {
        this.lat = latRads;
    }

    public double getLonRads() {
        return lon;
    }

    public void setLonRads(double lonRads) {
        this.lon = lonRads;
    }

    public Date getDate() {
        return new Date((long) getTimestamp());
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
