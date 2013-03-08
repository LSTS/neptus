/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by canasta
 * 25 de Jan de 2013
 * $Id:: UavLabelPainter.java 9863 2013-02-05 12:08:29Z sergioferreira          $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.elements;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * Neptus Uav painter which is intended to be used by Uav panels to draw necessary information on the screen. This painter functions as a custom label for Uav Panels.
 * 
 * <p>Accepted arguments in <code>LinkedHashMap< String,Object ></code> <b>receivedArgs</b></p> 
 * <ul>
 *  <li><b>Key:</b> name+".Color" <b>Value:</b> Color[2] </li>
 *  <li><b>Key:</b> name+".DrawPoint" <b>Value:</b> int[2] </li>
 *  <li><b>Key:</b> name+".Size" <b>Value:</b> int[2] </li>
 *  <li><b>Key:</b> name+".Text" <b>Value:</b> String </li>
 * </ul>
 * 
 * @author Sergio Ferreira
 * @version 1.0
 * @category UavPainter 
 * 
 */
public class UavLabelPainter implements IUavPainter{
    
    private LinkedHashMap<String,Object> receivedArgs;
    
    private Point2D[] gradientPoints;
    private Shape shape;
    private GradientPaint backgroundColor;    
    private Color[] colors;
    
    private String name;
    private String text;
    
    //X | Y
    private int[] drawPoints;
    private int[] textDrawPoints;
    
    //Width | Height
    private int[] size;
    
    public UavLabelPainter(String name){
        receivedArgs = new LinkedHashMap<String,Object>();
        this.name = name;
        size = new int[2];
        gradientPoints = new Point2D[2];
        backgroundColor = null;
        colors = new Color[2];
        drawPoints = new int[2];
        textDrawPoints = new int[2];
    }
    
    // ------Setters and Getters------//
    
    // ------Specific Methods------//

    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {
        
        boolean update = false;
        receivedArgs = (LinkedHashMap<String, Object>) args;
        
        //checks for updates regarding window size
        if(receivedArgs.get(name+".Size") != null){
            size = (int[]) receivedArgs.get(name+".Size");
            gradientPoints[0] = new Point2D.Double(size[0]/2,size[1]);
            gradientPoints[1] = new Point2D.Double(size[0]/2,0);
            update = true;
        }
        
        //checks for updates regarding window size
        if(receivedArgs.get(name+".DrawPoint") != null){
            drawPoints = (int[]) receivedArgs.get(name+".DrawPoint");
            update = true;
        }
        
        //checks for updates regarding color change
        if(receivedArgs.get(name+".Color") != null){
            colors = (Color[]) receivedArgs.get(name+".Color"); 
            update = true;
        }
        
        //checks for updates regarding label text
        if(receivedArgs.get(name+".Text") != null){
            text = (String) receivedArgs.get(name+".Text");
            update = true;
        } 
        
        if(update){
            backgroundColor = new GradientPaint(gradientPoints[0], colors[0], gradientPoints[1], colors[1]);
            shape = new Rectangle(drawPoints[0], drawPoints[1], size[0], size[1]); 
            FontMetrics fm = g.getFontMetrics(g.getFont());
            textDrawPoints[0] = drawPoints[0] + (size[0] - fm.stringWidth(text))/2;
            textDrawPoints[1] = (drawPoints[1] + fm.getHeight()) + (size[1] - fm.getHeight())/2;
        }
        
        g.setPaint(backgroundColor);                
        g.fill(shape);  
        g.setPaint(Color.black);
        g.drawString(text, textDrawPoints[0], textDrawPoints[1]);
    }
}
