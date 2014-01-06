/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: canasta
 * 22 de Jun de 2012
 */
package pt.lsts.neptus.plugins.uavs.painters.foreground;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.Hashtable;

import pt.lsts.neptus.plugins.uavs.UavLib;
import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * @author canasta
 *
 */
public class UavCameraOrientationPainter implements IUavPainter{

    private Hashtable<String,Object> receivedArgs; 
    
    protected double cameraRoll = 0;
    protected double cameraPitch = 0;
    protected double cameraFov = 0;
    
    
    //------Implemented Interfaces------//
    
    //IUavPainter_BEGIN
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {

        receivedArgs = (Hashtable<String, Object>) args;   
        
        if(!receivedArgs.isEmpty()){
            
            if(receivedArgs.get("camera roll") != null)
                cameraRoll = ((Number)receivedArgs.get("camera roll")).doubleValue();
            
            if(receivedArgs.get("camera pitch")!= null)
                cameraPitch = ((Number)receivedArgs.get("camera pitch")).doubleValue();
            
            //anti-aliasing
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            drawHorizontalOrientation(g, width, height, 0.5);  
            
            drawVerticalOrientation(g, width, height, 0.5); 
        }        
    }
    /**
     * @param g
     * @param width
     * @param height
     * @param d
     */
    private void drawVerticalOrientation(Graphics2D g, int width, int height, double transparency) {

        //transparent background to enhance visibility
        g.translate(width-width/7-10, 10);
        drawRulerBackgrounds(g,width,height,width/7,height/7,transparency);
        
        g.translate((width/7)/2, (height/7)/2);
        g.scale(1, -1);
        
        drawVisionCone(g, height, 45, cameraRoll, transparency);                       
        drawVehicleIcon(g, width, height, transparency); 
        
        g.scale(1, -1);
        g.translate(-(width/7)/2, -(height/7)/2);
        
        g.translate(-(width-width/7-10), -10);        
    }
    /**
     * @param g
     * @param width
     * @param height
     */
    private void drawHorizontalOrientation(Graphics2D g, int width, int height, double transparency) {
       
        //transparent background to enhance visibility
        g.translate(10, 10);
        drawRulerBackgrounds(g,width,height,width/7,height/7,transparency);
        
        g.translate((width/7)/2, (height/7)/2);
        g.scale(1, -1);
        
        drawVisionCone(g, height, 45, cameraRoll, transparency);                       
        drawVehicleIcon(g, width, height, transparency); 
        
        g.scale(1, -1);
        g.translate(-(width/7)/2, -(height/7)/2);
        
        g.translate(-10, -10);
    }
    /**
     * @param g
     * @param height
     */
    private void drawVisionCone(Graphics2D g, int height, double fov, double rotation, double transparency) {
        g.setColor(Color.yellow);
        g.setComposite(UavLib.makeTransparent((float) transparency)); 
        g.rotate(-Math.toRadians(rotation));
        g.fill(createViewCone(((height/7)/2),fov));
        g.rotate(Math.toRadians(rotation));
        g.setComposite(UavLib.makeTransparent((float) 1));
    }
    /**
     * @param g
     * @param width
     * @param height
     */
    private void drawVehicleIcon(Graphics2D g, int width, int height, double transparency) {
        g.setColor(Color.green);
        g.setComposite(UavLib.makeTransparent((float) transparency)); 
        g.fill(createUavIcon((width/7)/2, (height/7)/2));
        g.setComposite(UavLib.makeTransparent((float) 1));
    }
    //IUavPainter_END

    /**
     * @param g
     * @param hostWidth
     * @param hostHeight
     * @param desiredWidth
     * @param desiredHeight
     * @param transparency
     */
    private void drawRulerBackgrounds(Graphics2D g, int hostWidth, int hostHeight, int desiredWidth, int desiredHeight, double transparency) {
        g.setPaint(UavLib.gradientColorer(hostWidth, hostHeight, Color.gray, Color.gray));
        g.setComposite(UavLib.makeTransparent((float) transparency));        
        g.fill(new Rectangle.Double(0, 0, desiredWidth, desiredHeight));
        g.setComposite(UavLib.makeTransparent((float) 1));
        g.setColor(Color.black);
        g.drawRect(0, 0, hostWidth/7, hostHeight/7);
    }
    
    private Shape createUavIcon(int width, int height){

        GeneralPath ret = new GeneralPath();
        
        ret.moveTo((-width/2)    ,   (-height/2));
        ret.lineTo(0             ,   (height/2));
        ret.lineTo((width/2)     ,   (-height/2));
        ret.lineTo(0             ,   ((-height/2)*3/5));
        
        ret.closePath();
        
        return ret;
    }
    
    private Shape createViewCone(int height, double angle){

        GeneralPath ret = new GeneralPath();
        
        ret.moveTo(0                                                              ,   0);
        ret.lineTo(Math.tan(Math.toRadians(angle/2))*(2.0/3.0*height)             ,   (2.0/3.0*height));
        ret.curveTo(Math.tan(Math.toRadians(angle/2))*(2.0/3.0*height)            ,   (2.0/3.0*height),                
                    0                                                             ,   height,
                    -Math.tan(Math.toRadians(angle/2))*(2.0/3.0*height)           ,   (2.0/3.0*height));
        
        ret.closePath();
        
        return ret;
    }
}
