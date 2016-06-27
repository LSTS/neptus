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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Aug 7, 2013
 */
package pt.lsts.neptus.mp.preview.payloads;

import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;

/**
 * @author zp
 *
 */
public class CameraFootprint extends PayloadFingerprint {

    private double crossFov, alongFov;
    private Color color;
    private double maxDistance;
    
    /**
     * Create a camera footprint 
     * @param alongTrackFov along track field of view, in radians
     * @param crossTrackFov cross track field of view, in radians
     * @param maxDistance distance after which the images are discarded (unfocused?)
     * @param color Color to use
     */
    public CameraFootprint(double alongTrackFov, double crossTrackFov, double maxDistance, Color color) {
        super("Camera", new Color(192, 192, 0, 128));
        this.alongFov = alongTrackFov;
        this.crossFov = crossTrackFov;
        this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
        this.maxDistance = maxDistance;
    }
    
    /**
     * Create a camera footprint with given color and FOVs
     * @param alongTrackFov along track field of view, in radians
     * @param crossTrackFov cross track field of view, in radians
     * @param color Color to use
     */
    public CameraFootprint(double alongTrackFov, double crossTrackFov, Color color) {
        this(alongTrackFov, crossTrackFov, Double.MAX_VALUE, color); 
    }
    
    /**
     * Create a camera footprint with given color, along track FOV and 16/9 horizontal/vertical ratio
     * @param fov along track field of view, in radians
     * @param color Color to use
     */
    public CameraFootprint(double fov, Color color) {
        this(fov, fov * (16./9.), Double.MAX_VALUE, color); 
    }    
    
    @Override
    public Color getColor() {
        return color;
    }
    
    public Area getFingerprint(SystemPositionAndAttitude pose) {
        if (pose.getAltitude() > maxDistance)
            return new Area(new Rectangle2D.Double(0,0,0,0));
        double width = pose.getAltitude() * (Math.tan(crossFov/2d));
        double height = pose.getAltitude() * (Math.tan(alongFov/2d));
        return new Area (new Rectangle2D.Double(-width/2, -height/2, width, height));
    };
}
