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
 * Oct 26, 2012
 */
package pt.lsts.neptus.mra.api;

import java.awt.image.BufferedImage;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 *
 */
public class SidescanLine {
    public long timestampMillis;
    
    public int xsize;
    public int ysize;
    
    public int ypos;
    
    public float range;
    public SystemPositionAndAttitude state;
    
    
    public BufferedImage image;
    public double data[];

    public float frequency;
    
    /**
     * @param xsize
     * @param ysize
     * @param ypos
     * @param ping
     * @param state
     */
    public SidescanLine(long timestamp, float range, SystemPositionAndAttitude state, float frequency, double data[]) {
        super();
        this.timestampMillis = timestamp;
        this.xsize = data.length;
        this.range = range;
        this.state = state;
        this.data = data;
        this.frequency = frequency;
    }
    
    /**
     *  Based on a 'x' position within a scan line calculate the proper location
     * @param x the x position
     * @return a LocationType object containing the absolute GPS location of the point
     */
    public SidescanPoint calcPointForCoord(int x) {
        
        LocationType location = new LocationType();
        // Set the System lat/lon as the center point
        location.setLatitude(state.getPosition().getLatitude());
        location.setLongitude(state.getPosition().getLongitude());
        
        double distance = x * (range * 2 / (float)xsize) - (range);
        double angle = -state.getYaw() + (x < (xsize / 2) ? Math.PI : 0);
        double offsetNorth = Math.abs(distance) * Math.sin(angle);
        double offsetEast = Math.abs(distance) * Math.cos(angle);
        // Add the original vehicle offset to the calculated offset
        location.setOffsetNorth(state.getPosition().getOffsetNorth() + offsetNorth);
        location.setOffsetEast(state.getPosition().getOffsetEast() + offsetEast);
        
        // Return new absolute location        
        return new SidescanPoint(x,ypos,location.getNewAbsoluteLatLonDepth());
    }
}
