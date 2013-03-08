/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Nov 16, 2012
 * $Id:: LogMarker.java 9615 2012-12-30 23:08:28Z pdias                         $:
 */
package pt.up.fe.dceg.neptus.mra;

import java.io.Serializable;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 *
 */
public class LogMarker implements Serializable, Comparable<LogMarker> {
    private static final long serialVersionUID = 1L;

    public String label;
    public double timestamp;
    public int x;
    public int y;
    public int w;
    public int h;
    /**
     * Latitude in radians
     */
    public double lat;
    /**
     * Longitude in radians
     */
    public double lon;
    
    /**
     * @param label
     * @param timestamp
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public LogMarker(String label, double timestamp, double lat, double lon, int x, int y, int w, int h) {
        super();
        this.label = label;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
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
    
    public LocationType getLocation() {
        return new LocationType(Math.toDegrees(lat), Math.toDegrees(lon));
    }
}