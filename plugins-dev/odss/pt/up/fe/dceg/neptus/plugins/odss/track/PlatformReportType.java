/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 7/07/2012
 * $Id:: PlatformReportType.java 9615 2012-12-30 23:08:28Z pdias                $:
 */
package pt.up.fe.dceg.neptus.plugins.odss.track;

import java.util.Date;

import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;

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
        this.isoDatetime = DateTimeUtil.dateFormaterXMLNoMillisUTC.format(new Date(Math.round(epoch_seconds * 1000d)));
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
