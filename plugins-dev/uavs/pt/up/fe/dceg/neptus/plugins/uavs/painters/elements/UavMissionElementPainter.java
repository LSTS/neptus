/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by sergioferreira
 * 5 de Mar de 2012
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.plugins.uavs.UavVehicleIcon;
import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * Customizable element native to Neptus's Uav Plugin. Used to represent vehicles or typical mission elements like communication nodes or gateways on 
 * Uav Panels.
 *  
 * @author Sergio Ferreira
 * @version 1.0
 * @category UavPainter  
 */
public class UavMissionElementPainter implements IUavPainter{
   
    public static int WIDTH_RACIO = 10;
    public static int HEIGHT_RACIO = 15;
    
    private LinkedHashMap<String,Object> receivedArgs;    
    private LinkedHashMap<String,Integer> vehicleAltitudes;
    private LinkedHashMap<String,UavVehicleIcon> vehicleIconTable = new LinkedHashMap<String,UavVehicleIcon>();
    private Point pixelsPerMark_markGrade_Pair;
    
    public UavMissionElementPainter() {
        receivedArgs = new LinkedHashMap<String,Object>();
    }
    
    //------Implemented Interfaces------//
    
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {
        
        receivedArgs = (LinkedHashMap<String, Object>) args;
               
        if(!receivedArgs.isEmpty()){
            vehicleAltitudes = (LinkedHashMap<String, Integer>) receivedArgs.get("vehicles");
            pixelsPerMark_markGrade_Pair = (Point) receivedArgs.get("markInfo");
        }
        
        // Normalizes the graphics transformation and sets the origin at the center of the panel
        determineDrawingOriginPoint(g, height,width);  
        
        //anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for(String vehicle: vehicleAltitudes.keySet())
        {             
            if(vehicleIconTable.get(vehicle)==null){
                vehicleIconTable.put(vehicle, new UavVehicleIcon(width/HEIGHT_RACIO, width/WIDTH_RACIO, "Side View"));
            }
            
            // setting up text font size
            g.setFont(g.getFont().deriveFont((float) 10));

            int vehicleAltitudes = determineCorrectDrawAltitude(vehicle);

            // translate the drawing position to the correct altitude value
            g.translate(0, vehicleAltitudes - (height / 2));

            g.scale(1, -1);
            drawVehicleLabel(g, vehicle);
            g.scale(1, -1);

            // draws a line which marks the altitude across the ruler
            g.setColor(Color.white.darker());
            g.drawRect((-width / 2), 0, height, 0);

            double drawingPitch = 0;

            // draws the vehicle icon taking into account climb/decent pitch
            g.rotate(drawingPitch);
            drawVehicleIcon(g, vehicle);
            g.rotate(-drawingPitch);

            g.translate(0, -(vehicleAltitudes - (height / 2)));
        }     
    }

    /**
     * @param vehicle
     * @return
     */
    private int determineCorrectDrawAltitude(String vehicle) {
        int ret = 0;
        ret = vehicleAltitudes.get(vehicle) * pixelsPerMark_markGrade_Pair.x / pixelsPerMark_markGrade_Pair.y;
        return ret;
    }

    //------Specific Methods------//
    
    private void determineDrawingOriginPoint(Graphics2D g, int height, int width) {
        g.translate(width / 2, height / 2);
        g.scale(1, -1);
    }
    
    private void drawVehicleLabel(Graphics2D g, String vehicle) {
        g.drawString(vehicle, vehicleIconTable.get(vehicle).getWidth()/2, -2);
        g.drawString(-vehicleAltitudes.get(vehicle) + " m", vehicleIconTable.get(vehicle).getWidth()/2, 12);
    }
    
    private void drawVehicleIcon(Graphics2D g, String vehicle) {
        
        g.setColor(vehicleIconTable.get(vehicle).getAlertLevel());
        g.fill(vehicleIconTable.get(vehicle).getIcon());
        g.setColor(Color.black);
        g.draw(vehicleIconTable.get(vehicle).getIcon());
    }
}
