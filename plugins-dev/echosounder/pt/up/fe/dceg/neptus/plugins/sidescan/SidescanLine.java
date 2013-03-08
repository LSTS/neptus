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
 * Oct 26, 2012
 * $Id:: SidescanLine.java 9880 2013-02-07 15:23:52Z jqcorreia                  $:
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

import java.awt.Image;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 *
 */
public class SidescanLine {
    public int xsize;
    public int ysize;
    
    public int ypos;
    
    public float range;
    public IMCMessage state;
    
    public Image image;
   
    /**
     * @param xsize
     * @param ysize
     * @param ypos
     * @param ping
     * @param state
     */
    public SidescanLine(int xsize, int ysize, int ypos, float range, IMCMessage state, Image image) {
        super();
        this.xsize = xsize;
        this.ysize = ysize;
        this.ypos = ypos;
        this.range = range;
        this.state = state;
        this.image = image;
    }
    
    /**
     *  Based on a 'x' position within a scan line calculate the proper location
     * @param x the x position
     * @return a LocationType object containing the absolute GPS location of the point
     */
    public SidescanPoint calcPointForCoord(int x) {
        LocationType location = new LocationType();
        
        // Set the EstimatedState lat/lon as the center point
        location.setLatitude(Math.toDegrees(state.getDouble("lat")));
        location.setLongitude(Math.toDegrees(state.getDouble("lon")));
        
        double distance = x * (range * 2 / (float)xsize) - (range);
        double angle = -state.getDouble("psi") + (x < (xsize / 2) ? Math.PI : 0);
        double offsetNorth = Math.abs(distance) * Math.sin(angle);
        double offsetEast = Math.abs(distance) * Math.cos(angle);
//        System.out.println(x + " " + range + " " + distance + " " + Math.toDegrees(state.getDouble("psi"))+ " " + Math.toDegrees(angle)+ " " + offsetNorth+ " " + offsetEast);
        
        // Add the original vehicle offset to the calculated offset
        location.setOffsetNorth(state.getDouble("x") + offsetNorth);
        location.setOffsetEast(state.getDouble("y") + offsetEast);
        
        // Return new absolute location        
        return new SidescanPoint(x,ypos,location.getNewAbsoluteLatLonDepth());
    }
}
