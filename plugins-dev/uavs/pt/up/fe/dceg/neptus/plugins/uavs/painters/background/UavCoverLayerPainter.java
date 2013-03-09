/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Canasta
 * 19 de Dez de 2010
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.background;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * Neptus Uav painter which is intended to be used by Uav panels to draw necessary information on the screen. This painter functions as a background setter for Uav panels. It allows the
 * creation of gradient backgrounds with up to 2 colors.
 * 
 * <p>Accepted arguments in <code>LinkedHashMap< String,Object ></code> <b>receivedArgs</b></p> 
 * <ul>
 *  <li><b>Key:</b> name+".Color" <b>Value:</b> Color[2] </li>
 *  <li><b>Key:</b> name+".DrawPoint" <b>Value:</b> int[2] </li>
 *  <li><b>Key:</b> name+".Size" <b>Value:</b> int[2] </li>
 * </ul>
 * 
 * @author Sergio Ferreira
 * @version 2.0
 * @category UavPainter 
 * 
 */
public class UavCoverLayerPainter implements IUavPainter{
    
    private String name;
    
    private Point2D[] gradientPoints;
    private Shape shape;
    private GradientPaint backgroundColor;    
    private Color[] colors;

    //X | Y
    private int[] drawPoints;
    
    //Width | Height
    private int[] size;
    
    private LinkedHashMap<String,Object> receivedArgs;
    
    public UavCoverLayerPainter(String name) {
        this.name = name;
        size = null;
        gradientPoints = new Point2D[2];
        backgroundColor = null;
        colors = null;
        drawPoints = null;
        receivedArgs = new LinkedHashMap<String,Object>();
    }
    
    //------Implemented Interfaces------//
    
	//IUavPainter_BEGIN
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
                       
        //checks for updates regarding color change
        if(receivedArgs.get(name+".Color") != null){
            colors = (Color[]) receivedArgs.get(name+".Color"); 
            update = true;
        }
        
        //checks for updates drawing start point
        if(receivedArgs.get(name+".DrawPoint") != null){
            drawPoints = (int[]) receivedArgs.get(name+".DrawPoint");
            update = true;
        }
        
        if(update){
            backgroundColor = new GradientPaint(gradientPoints[0], colors[0], gradientPoints[1], colors[1]);
            shape = new Rectangle(drawPoints[0], drawPoints[1], size[0], size[1]); 
        }
                
        g.setPaint(backgroundColor);                
        g.fill(shape);   
        
    }
    //IUavPainter_END
    
}
