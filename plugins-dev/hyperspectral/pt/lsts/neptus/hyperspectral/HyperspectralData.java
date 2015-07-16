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
 * Author: coop
 * 11 Jul 2015
 */
package pt.lsts.neptus.hyperspectral;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.HyperSpecData;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * Hyperspectral data and information to draw it on the map
 * @author tsmarques
 *
 */
public class HyperspectralData {
    private double rotationAngle; 
    public BufferedImage data;
    public LocationType dataLocation;

    public HyperspectralData(HyperSpecData msg, EstimatedState state, boolean overlapped) {
        data = HyperspecUtils.rawToBuffImage(msg.getData());
        
        dataLocation = IMCUtils.parseLocation(state);
        
        if(overlapped)
            translateDataPosition(msg, state);
        
        data = getScaledData(1, 0.25);
        
        rotationAngle = setRotationAngle(state.getPsi());
        data = rotateData();
    }
    
    /* 
       If some data is overlapped with another over an
       Estimated State point, make an estimate of its position
       using the vehicle's speed and difference between timestamps 
       i.e., calculate how much the vehicle moved since the last 
       EstimatedState, and draw it there, instead of in the position
       given by getEntryAtOrAfter()
     */
    private void translateDataPosition(HyperSpecData data, EstimatedState state) {
        double deltaTime = data.getTimestamp() - state.getTimestamp();
        double speedX = state.getVx();
        double speedY = state.getVy();

        double deltaX = speedX * deltaTime;
        double deltaY = speedY * deltaTime;

        dataLocation.setOffsetNorth(deltaX);
        dataLocation.setOffsetEast(deltaY);
    }
    
    private BufferedImage rotateData() {
        double sin = Math.abs(Math.sin(rotationAngle));
        double cos = Math.abs(Math.cos(rotationAngle));
        int w = data.getWidth();
        double h = data.getHeight();
        
        int rw =(int) Math.floor(cos * w + sin * h);
        int rh = (int) Math.floor(cos * h + sin * w);
        
        BufferedImage rotatedImage = new BufferedImage((int)rw, (int)rh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) rotatedImage.getGraphics();
        
        g.translate((rw-w)/2, (rh-h)/2);
        g.rotate(rotationAngle, w/2, h/2);
        g.drawRenderedImage(data, null);
        g.dispose();
        
        return rotatedImage;
    }
    
    public BufferedImage getScaledData(double scalex, double scaley) {
        return (BufferedImage)ImageUtils.getFasterScaledInstance(data, 
                (int)(data.getWidth() * scalex), (int)(scaley * data.getHeight()));
    }

    /* Get angle so that the frame is perpendicular to the vehicle's heading */
    private double setRotationAngle(double psi) {
        double angle;

        psi = (Math.toDegrees(psi)) - 90; /* -90 to make angle perpendicular */
        if(psi < 0)
            angle = 360 + psi;
        else
            angle = psi;

        return Math.toRadians(angle);
    }
}

