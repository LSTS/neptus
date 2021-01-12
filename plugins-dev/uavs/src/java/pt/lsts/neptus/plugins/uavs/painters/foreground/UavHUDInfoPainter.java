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
 * Author: canasta
 * 4 de Jun de 2012
 */
package pt.lsts.neptus.plugins.uavs.painters.foreground;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.math.BigDecimal;
import java.util.Hashtable;

import pt.lsts.neptus.plugins.uavs.UavLib;
import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * @author canasta
 *
 */
public class UavHUDInfoPainter implements IUavPainter{

    private boolean needsCreation;
    private static final int RIGHTSIDED = 0;
    private static final int LEFTSIDED = 1;
    private static final int NO_ORIENTATION = 3;
    
    private Hashtable<String,Object> receivedArgs;   
    private double roll = 0;
    private double pitch = 0;
    private double yaw = 0;
    private double indicatedSpeed = 0;
    private int altitude = 0;
    
    private AffineTransform identity = new AffineTransform();
    private Shape pitchReadOut;
    private Shape rollReadOut;
    private Shape yawReadOut;
    private Shape indicatedSpeedReadOut;
    private Shape altitudeReadOut;
    
    private Shape pitchOuterBox;
    private Shape rollOuterBox;
    private Shape yawOuterBox;
    private Shape indicatedSpeedOuterBox;
    private Shape altitudeOuterBox;
    
    private Shape triangleMarker;
    private TextLayout text;
    
    public UavHUDInfoPainter(){
        super();        
        setNeedsCreation(true);
        setTriangleMarker(createTriangleMarker(10, 10));
    }
    
    private void setNeedsCreation(boolean needsCreation) {
        this.needsCreation = needsCreation;
    }

    public boolean isNeedsCreation() {
        return needsCreation;
    }

    public void setTriangleMarker(Shape triangleMarker) {
        this.triangleMarker = triangleMarker;
    }

    public Shape getTriangleMarker() {
        return triangleMarker;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {
        
        receivedArgs = (Hashtable<String, Object>) args;
        
        if(!receivedArgs.isEmpty()){        
            roll = ((Number) receivedArgs.get("roll")).doubleValue();
            pitch = ((Number) receivedArgs.get("pitch")).doubleValue();
            yaw = ((Number) receivedArgs.get("yaw")).doubleValue();
            if (receivedArgs.get("indicatedSpeed") != null)
                indicatedSpeed = ((Number) receivedArgs.get("indicatedSpeed")).doubleValue();
            altitude = ((Number) receivedArgs.get("altitude")).intValue();
        }
        
        //anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        pitchReadOut = createVisualReadOut(g,pitch,"SansSerif", Font.PLAIN, 14);
        rollReadOut = createVisualReadOut(g,roll,"SansSerif", Font.PLAIN, 14);
        yawReadOut = createVisualReadOut(g,yaw,"SansSerif", Font.PLAIN, 14);
        indicatedSpeedReadOut = createVisualReadOut(g,indicatedSpeed,"SansSerif", Font.PLAIN, 14);
        altitudeReadOut = createVisualReadOut(g,altitude,"SansSerif", Font.PLAIN, 14);
        
        if(needsCreation){
            pitchOuterBox = createPitchOuterBox(pitchReadOut.getBounds().width, pitchReadOut.getBounds().height, 90,10);
            rollOuterBox = createDefaultOuterBox(rollReadOut.getBounds().width, rollReadOut.getBounds().height, 80, 10, NO_ORIENTATION);
            yawOuterBox = createDefaultOuterBox(yawReadOut.getBounds().width, yawReadOut.getBounds().height, 80, 10, NO_ORIENTATION);
            indicatedSpeedOuterBox = createDefaultOuterBox(indicatedSpeedReadOut.getBounds().width, indicatedSpeedReadOut.getBounds().height, 80, 10, LEFTSIDED);
            altitudeOuterBox = createDefaultOuterBox(altitudeReadOut.getBounds().width, altitudeReadOut.getBounds().height, 80, 10, RIGHTSIDED);
            setNeedsCreation(false);
        }
                
        drawPitch(g, width, height);
          
        drawRoll(g, width, height);
         
        drawYaw(g, width, height);
        
        drawSpeed(g, width, height);
                
        drawAltitude(g, width, height);
        
    }

    /**
     * @param g
     * @param width
     * @param height
     */
    private void drawAltitude(Graphics2D g, int width, int height) {
        
        g.translate(3*width/4, 0);
        
        //sets up background panel for the altitude ruler         
        drawRulerBackgrounds(g, width, height, width/4, height, 0.6);
        
        
        //draws the ruler with altitude values
        drawRuler(g, width, height, width/4, 4*height/6, altitude, 25, 8, RIGHTSIDED);
        g.translate(width/8, height/2);
        g.draw(altitudeOuterBox);
        g.setColor(Color.gray.brighter());
        g.fill(altitudeOuterBox);
        g.setColor(Color.black);
        drawReadOut(g,altitudeReadOut);
        g.translate(-width/8, -height/2);
        
        g.translate(-3*width/4, 0);
    }

    /**
     * @param g
     * @param width
     * @param height
     */
    private void drawSpeed(Graphics2D g, int width, int height) {
        
        g.translate(0, 0);
        
        //sets up background panel for the speed ruler         
        drawRulerBackgrounds(g, width, height, width/4, height, 0.6);
                
        //draws the ruler with speed values
        drawRuler(g, width, height, width/4, 4*height/6, indicatedSpeed, 10, 5, LEFTSIDED);
        
        g.translate(width/8, height/2);
        g.setColor(Color.black);
        g.draw(indicatedSpeedOuterBox);
        g.setColor(Color.gray.brighter());
        g.fill(indicatedSpeedOuterBox);
        g.setColor(Color.black);
        drawReadOut(g,indicatedSpeedReadOut);
        g.translate(-width/8, -height/2);
        
        g.translate(0, 0);
    }

    /**
     * @param g
     * @param width
     * @param height
     */
    private void drawYaw(Graphics2D g, int width, int height) {
        g.translate(width/2, height/8);
        g.setColor(Color.black);
        g.draw(yawOuterBox);
        g.setColor(Color.gray.brighter());
        g.fill(yawOuterBox);
        g.setColor(Color.black);
        drawReadOut(g,yawReadOut);
        g.translate(-width/2, -height/8);
    }

    /**
     * @param g
     * @param width
     * @param height
     */
    private void drawRoll(Graphics2D g, int width, int height) {
        g.translate(width/2, 5*height/6);
        g.setColor(Color.black);
        g.draw(rollOuterBox);
        
        g.translate(0, -2*height/6);
        int i = 0;
        while(i<360){
            
            g.setColor(Color.black);
            
            if(i == 0){
                g.setColor(Color.red);
                g.translate(2*height/5*Math.sin(Math.toRadians(i)), 2*height/5*Math.cos(Math.toRadians(i)));
                g.scale(1, -1);
                g.fill(triangleMarker);
                g.scale(1, -1);
                g.translate(-2*height/5*Math.sin(Math.toRadians(i)), -2*height/5*Math.cos(Math.toRadians(i)));
            }
            else if(i < 50 || i > 310){
                g.translate(2*height/5*Math.sin(Math.toRadians(i)), 2*height/5*Math.cos(Math.toRadians(i)));
                g.fillRect(-1, -1, 2, 2);
                g.translate(-2*height/5*Math.sin(Math.toRadians(i)), -2*height/5*Math.cos(Math.toRadians(i)));
            }
           
            i+=5;
        }
        g.setColor(Color.yellow);
        g.translate(2*height/5*Math.sin(Math.toRadians(roll)), 2*height/5*Math.cos(Math.toRadians(roll)));
        g.rotate(-Math.toRadians(roll));
        g.fill(triangleMarker);
        g.rotate(Math.toRadians(roll));
        g.translate(-2*height/5*Math.sin(Math.toRadians(roll)), -2*height/5*Math.cos(Math.toRadians(roll)));
        
        g.translate(0, 2*height/6);

        g.setColor(Color.gray.brighter());
        g.fill(rollOuterBox);
        g.setColor(Color.black);
        drawReadOut(g,rollReadOut);
        g.translate(-width/2, -5*height/6);
    }

    /**
     * @param g
     * @param width
     * @param height
     */
    private void drawPitch(Graphics2D g, int width, int height) {
        g.translate(width/2, height/2);
        g.setColor(Color.black);
               
        g.draw(pitchOuterBox);
        g.setColor(Color.gray.brighter());
        g.fill(pitchOuterBox);
        g.setColor(Color.black);
        drawReadOut(g,pitchReadOut);
        g.translate(-width/2, -height/2);
    }
    
    /**
     * @param g
     * @param hostWidth
     * @param hostHeight
     * @param desiredWidth 
     * @param desiredHeight 
     * @param value 
     * @param scale
     * @param numMarks
     * @param orientation
     */
    private void drawRuler(Graphics2D g, int hostWidth, int hostHeight, int desiredWidth, int desiredHeight, double value, int scale, int numMarks, int orientation) {
       
        if(orientation == LEFTSIDED){        
            g.translate(0,(hostHeight-desiredHeight)/2);

            int markValue = BigDecimal.valueOf(value*10).intValue()/scale + (numMarks/2);
            int markSeparation = desiredHeight / numMarks;
            int alignment = ( BigDecimal.valueOf(value*10%scale).intValue() * markSeparation) / scale;
            int i = ((desiredHeight/2) + alignment) - ((numMarks/2)*markSeparation);       
            
            g.setColor(Color.black);

            while(i < desiredHeight){

                if(i > 0){
                    g.fillRect(0,i-2,desiredWidth/6,4);
                    if(i < (desiredHeight/2)-10 || i > (desiredHeight/2)+10){
                        g.drawString(String.valueOf(BigDecimal.valueOf(markValue).doubleValue()), 
                                (desiredWidth/6)+3, 
                                i+g.getFontMetrics().getHeight()/4); 
                    }
                }

                i +=markSeparation;
                markValue--;
            }
            
            g.fillRect(0, 0, 3, desiredHeight);

            g.translate(0,-(hostHeight-desiredHeight)/2);  
        }
        else if(orientation == RIGHTSIDED){
            g.translate(0,(hostHeight-desiredHeight)/2);

            int markValue = BigDecimal.valueOf(value).intValue()/scale + (numMarks/2);
            int markSeparation = desiredHeight / numMarks;
            int alignment = ( BigDecimal.valueOf(value%scale).intValue() * markSeparation) / scale;
            int i = ((desiredHeight/2) + alignment) - ((numMarks/2)*markSeparation);       
            
            g.setColor(Color.black);
            
            while(i < desiredHeight){

                if(i > 0){
                     g.fillRect(5*desiredWidth/6,i-2,desiredWidth/6,4);
                    
                    if(i < (desiredHeight/2)-10 || i > (desiredHeight/2)+10){
                        g.drawString(String.valueOf(markValue*scale), 
                                (5*desiredWidth/6)-(g.getFontMetrics().stringWidth(String.valueOf(BigDecimal.valueOf(markValue).doubleValue()))), 
                                i+g.getFontMetrics().getHeight()/4);     
                    }
                }

                i +=markSeparation;
                markValue--;
            }

            g.fillRect(desiredWidth-3, 0, 3, desiredHeight);
            
            g.translate(0,-(hostHeight-desiredHeight)/2);  
        }
    }

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
    }
    
    /**
     * @param g
     */
    private void drawReadOut(Graphics2D g, Shape readOut) {
        g.translate(-readOut.getBounds().width/2, readOut.getBounds().height/2);
        g.fill(readOut);
        g.translate(readOut.getBounds().width/2, -readOut.getBounds().height/2);
    }

    /**
     * @param g
     * @param pitch
     * @param size
     * @return
     */
    private Shape createVisualReadOut(Graphics2D g, double readOut, String fontName, int fontType, int size) {
                
        text = new TextLayout((BigDecimal.valueOf(readOut).setScale(1,BigDecimal.ROUND_UP)).toPlainString(), 
                                new Font(fontName,fontType,size), 
                                g.getFontRenderContext());
        
        Area ret = new Area(text.getOutline(identity));
        
        return ret;
    }

    /**
     * @param g
     * @param pitch
     * @param size
     * @return
     */
    private Shape createVisualReadOut(Graphics2D g, int readOut, String fontName, int fontType, int size) {
                
        text = new TextLayout((BigDecimal.valueOf(readOut).toPlainString()), 
                                new Font(fontName,fontType,size), 
                                g.getFontRenderContext());
        
        Area ret = new Area(text.getOutline(identity));
        
        return ret;
    }

    /**
     * @param width
     * @param height
     * @return
     */
    private Shape createPitchOuterBox (int width, int height, int widthOffSet, int hightOffSet) {

        GeneralPath ret = new GeneralPath();
        ret.moveTo((-(width+widthOffSet)/8)     ,   (-(height+hightOffSet)/2));
        ret.lineTo((-(width+widthOffSet)/4)     ,   (0));
        ret.lineTo((-(width+widthOffSet)/2)     ,   (0));
        ret.lineTo((-(width+widthOffSet)/4)     ,   (0));
        ret.lineTo((-(width+widthOffSet)/8)     ,   ((height+hightOffSet)/2));
        ret.lineTo(((width+widthOffSet)/8)      ,   ((height+hightOffSet)/2));
        ret.lineTo(((width+widthOffSet)/4)      ,   (0));
        ret.lineTo(((width+widthOffSet)/2)      ,   (0));
        ret.lineTo(((width+widthOffSet)/4)      ,   (0));
        ret.lineTo(((width+widthOffSet)/8)      ,   (-(height+hightOffSet)/2));
        ret.closePath(); 
        
        return ret;
    }
    
    /**
     * @param width
     * @param height
     * @return
     */
    private Shape createDefaultOuterBox (int width, int height, int widthOffSet, int hightOffSet, int orientation) {

        GeneralPath ret = new GeneralPath();
        
        if(orientation == LEFTSIDED){        
            ret.moveTo((-(width+widthOffSet)/8)     ,   (-(height+hightOffSet)/2));
            ret.lineTo((-(width+widthOffSet)/4)     ,   (0));
            ret.lineTo((-(width+widthOffSet)/8)     ,   ((height+hightOffSet)/2));
            ret.lineTo(((width+widthOffSet)/5)      ,   ((height+hightOffSet)/2));
            ret.lineTo(((width+widthOffSet)/5)      ,   (-(height+hightOffSet)/2));
        }
        else if(orientation == RIGHTSIDED){            
            ret.moveTo((-(width+widthOffSet)/5)     ,   (-(height+hightOffSet)/2));
            ret.lineTo((-(width+widthOffSet)/5)     ,   ((height+hightOffSet)/2));
            ret.lineTo(((width+widthOffSet)/8)      ,   ((height+hightOffSet)/2));
            ret.lineTo(((width+widthOffSet)/4)      ,   (0));
            ret.lineTo(((width+widthOffSet)/8)      ,   (-(height+hightOffSet)/2));
        }
        else{
            ret.moveTo((-(width+widthOffSet)/6)     ,   (-(height+hightOffSet)/2));
            ret.lineTo((-(width+widthOffSet)/4)     ,   (0));
            ret.lineTo((-(width+widthOffSet)/6)     ,   ((height+hightOffSet)/2));
            ret.lineTo(((width+widthOffSet)/6)      ,   ((height+hightOffSet)/2));
            ret.lineTo(((width+widthOffSet)/4)      ,   (0));
            ret.lineTo(((width+widthOffSet)/6)      ,   (-(height+hightOffSet)/2));            
        }
        
        ret.closePath(); 
        return ret;
    }
    
    /**
     * @param width
     * @param height
     * @return
     */
    private Shape createTriangleMarker (int width, int height) {

        GeneralPath ret = new GeneralPath();
        
        ret.moveTo(-width/2     ,   0);
        ret.lineTo(0            ,   height);
        ret.lineTo(width/2      ,   (0));    
        
        ret.closePath(); 
        return ret;
    }
}
