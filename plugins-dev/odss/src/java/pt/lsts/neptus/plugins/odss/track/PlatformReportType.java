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
 * Author: Paulo Dias
 * 7/07/2012
 */
package pt.lsts.neptus.plugins.odss.track;

import java.util.Date;

import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author pdias
 *
 */
public class PlatformReportType implements Cloneable{
    public enum PlatformType { SHIP, AIS, AUV, DRIFTER, MOORING, GLIDER }

    private PlatformType type = PlatformType.AIS;
    private String name = null;
    private double epochSeconds = -1;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private String source = null;
    private long mmsi = -1;
    private long imei = -1;
    private String isoDatetime = null; // ISO-8601 "yyyy-MM-dd'T'HH:mm:ss'Z'"
    
    /**
     * 
     */
    public PlatformReportType(String name, PlatformType type) {
        this.name = name;
        this.type = type;
    }
    
    /**
     * 
     */
    public void setLocation(double latitude, double longitude, double epoch_seconds) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.epochSeconds = epoch_seconds;
        this.isoDatetime = DateTimeUtil.dateFormatterXMLNoMillisUTC.format(new Date(Math.round(epoch_seconds * 1000d)));
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return the type
     */
    public PlatformType getType() {
        return type;
    }
    
    /**
     * @param type the type to set
     */
    public void setType(PlatformType type) {
        this.type = type;
    }
    
    /**
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }
    
    /**
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }
    
    /**
     * @return 
     * 
     */
    public LocationType getHasLocation() {
        return new LocationType(latitude, longitude);
    }
    
    /**
     * @return the epoch_seconds
     */
    public double getEpochSeconds() {
        return epochSeconds;
    }
    
    /**
     * @return the isoDatetime
     */
    public String getIsoDatetime() {
        return isoDatetime;
    }
    
    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * @return the mmsi
     */
    public long getMmsi() {
        return mmsi;
    }
    
    /**
     * @param mmsi the mmsi to set
     */
    public void setMmsi(long mmsi) {
        this.mmsi = mmsi;
    }
    
    /**
     * @return the imei
     */
    public long getImei() {
        return imei;
    }
    
    /**
     * @param imei the imei to set
     */
    public void setImei(long imei) {
        this.imei = imei;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name + " @ " + getIsoDatetime() + " @ " + " loc: " + getHasLocation() + "   ----   " + getType();
    }

    /**
     * @param newName
     * @return
     */
    public PlatformReportType cloneWithName(String newName) {
        PlatformReportType newPR = null;
        try {
            newPR = (PlatformReportType) this.clone();
            newPR.name = newName;
        }
        catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newPR;
    }
}
