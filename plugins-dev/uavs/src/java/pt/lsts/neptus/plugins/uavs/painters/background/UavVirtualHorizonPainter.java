/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: canasta
 * 18 de Abr de 2012
 */
package pt.lsts.neptus.plugins.uavs.painters.background;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.util.Hashtable;

import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * @author canasta
 *
 */
public class UavVirtualHorizonPainter implements IUavPainter{
    
    private Hashtable<String,Object> receivedArgs;   
    private double roll = 0;
    private double pitch = 0;
    
    private static final int VERTICAL_FOV = 50;
    private int currentHorizonHeight = 0;
    
    private Shape ground;
        
    //------Implemented Interfaces------//
    
    //IUavPainter_BEGIN
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {
        
        receivedArgs = (Hashtable<String, Object>) args;
          
        if(!receivedArgs.isEmpty()){
            roll = ((Number) receivedArgs.get("roll")).doubleValue();
            pitch = ((Number) receivedArgs.get("pitch")).doubleValue();
        }
                
        //sets up the sky box background         
        g.setPaint(gradientColorer(width, height, Color.cyan.darker(), Color.blue.darker()));   
        g.fillRect(0, 0, width, height);  
                
        ground = createGround(width, height);
        
        //updates the horizon level through pitch analysis
        currentHorizonHeight = (int) ((height/2) + (height/2*pitch/VERTICAL_FOV)); 
        
        //anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        //normalizes the graphics transformation and sets the origin at the center of the panel
        g.translate(width / 2, currentHorizonHeight);
        g.scale(-1, 1);
        g.setPaint(gradientColorer(width, height, Color.gray.darker(), Color.orange.darker()));
        g.rotate(Math.toRadians(roll));
        g.fill(ground);
        drawPitchRuler(g,width,height,20,4);
        g.rotate(-Math.toRadians(roll));
        g.scale(-1, 1);        
        g.translate(-(width / 2), -currentHorizonHeight);       
    }


    /**
     * @param g
     * @param width
     * @param height
     * @param scale
     * @param numMarks
     */
    private void drawPitchRuler(Graphics2D g, int width, int height, int scale, int numMarks) {
        
        int markHeight;
        
        g.setColor(Color.black);
        
        while(numMarks>0){
            
            if(scale%10 !=0){
                markHeight = scale*(height/2)/VERTICAL_FOV;
                g.fill(new Rectangle(-width/16, markHeight, width/8, 1));                 
            }
            else if(scale!=0){
                markHeight = scale*(height/2)/VERTICAL_FOV;
                g.fill(new Rectangle(-width/8, markHeight, width/4, 1));   
                g.scale(-1,1);
                g.drawString(String.valueOf(BigDecimal.valueOf(-scale).intValue()), 
                        width/8, 
                        markHeight);  
                g.scale(-1,1);
                numMarks--;
            }          
            
            scale -=2;
        }                
    }


    /**
     * @param width
     * @param height
     */
    private GradientPaint gradientColorer(int width, int height, Color color1, Color color2) {
        return new GradientPaint(new Point2D.Double(width/2,height), color1, new Point2D.Double(width/2,0), color2);
    }

    //IUavPainter_END

    /**
     * @param width
     * @param height
     * @return
     */
    private Shape createGround(int width, int height) {
        
        int newWidth = (int) Math.sqrt(Math.pow(width, 2)+Math.pow(height, 2));
        GeneralPath ret = new GeneralPath();

        ret.moveTo((-newWidth/2)    ,   0);
        ret.lineTo((-newWidth/2)    ,   height);
        ret.lineTo((newWidth/2)     ,   height);
        ret.lineTo((newWidth/2)     ,   0);

        ret.closePath();
        
        return ret;
    }
}
