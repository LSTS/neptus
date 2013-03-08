/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Christian Fuchs
 * 17.10.2012
 * $Id:: UavVehicleAirspeedPainter.java 9846 2013-02-02 03:32:12Z robot         $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.foreground;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.Hashtable;

import pt.up.fe.dceg.neptus.plugins.uavs.UavVehicleIcon;
import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * @author Christian Fuchs
 * Draws the current airspeed on the airspeed panel. Based on UavVehicleSidePainter.
 */
public class UavVehicleAirspeedPainter implements IUavPainter{

    public static int WIDTH_RACIO = 10;
    public static int HEIGHT_RACIO = 15;
    
    private Hashtable<String,Object> receivedArgs;    
    private Hashtable<String,Integer> vehicleAirspeeds;
    private Hashtable<String,UavVehicleIcon> vehicleIconTable = new Hashtable<String,UavVehicleIcon>();
    private Point pixelsPerMark_markGrade_Pair;
    
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter#paint(java.awt.Graphics2D, int, int, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {

        receivedArgs = (Hashtable<String, Object>) args;
        
        if(!receivedArgs.isEmpty()){
            vehicleAirspeeds = (Hashtable<String, Integer>) receivedArgs.get("vehicles");
            pixelsPerMark_markGrade_Pair = (Point) receivedArgs.get("markInfo");
        }
        
        // Normalizes the graphics transformation and sets the origin at the center of the panel
        determineDrawingOriginPoint(g, height,width);  
        
        //anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for(String vehicle: vehicleAirspeeds.keySet())
        {             
            if(vehicleIconTable.get(vehicle)==null){
                vehicleIconTable.put(vehicle, new UavVehicleIcon(width/HEIGHT_RACIO, width/WIDTH_RACIO, "Side View"));
            }
            
            // setting up text font size
            g.setFont(g.getFont().deriveFont((float) 10));

            int vehicleAirspeed = determineCorrectDrawAltitude(vehicle);

            // translate the drawing position to the correct altitude value
            g.translate(0, vehicleAirspeed - (height / 2));
            
            drawIndicator(g, width);
            
            g.scale(1, -1);
            drawVehicleLabel(g, vehicle);
            g.scale(1, -1);

            g.translate(0, -(vehicleAirspeed - (height / 2)));
        }
        
    }
    
    /**
     * @param vehicle
     * @return
     */
    private int determineCorrectDrawAltitude(String vehicle) {
        int ret = 0;
        ret = vehicleAirspeeds.get(vehicle) * pixelsPerMark_markGrade_Pair.x / pixelsPerMark_markGrade_Pair.y;
        return ret / 10;
    }
    
    private void determineDrawingOriginPoint(Graphics2D g, int height, int width) {
        g.translate(width / 2, height / 2);
        g.scale(1, -1);
    }
    
    private void drawVehicleLabel(Graphics2D g, String vehicle) {
        //g.drawString(vehicle, vehicleIconTable.get(vehicle).getWidth()/2, -2);
        //g.drawString(vehicleAirspeeds.get(vehicle) + " m/s", vehicleIconTable.get(vehicle).getWidth()/2, 12);
        
        // the airspeed needs to be divided by 10 because it is increased in the panel
        g.setColor(Color.white);
        g.drawString(vehicleAirspeeds.get(vehicle) / 10.0 + " ", vehicleIconTable.get(vehicle).getWidth()/2, 4);
    }
    
    private void drawIndicator(Graphics2D g, int width){
        
//        // draws a line which marks the altitude across the ruler
//        g.setColor(Color.white.darker());
//        g.drawRect((-width / 2), 0, width / 2, 0);
        
        g.setColor(Color.black);
        g.fill(createTriangleMarker(width, 20));
    }
    
    private Shape createTriangleMarker (int width, int height) {

        GeneralPath ret = new GeneralPath();
        
        ret.moveTo(-width/8     ,   0);
        ret.lineTo(0            ,   height/2);
        ret.lineTo(7*width/16   ,   height/2);
        ret.lineTo(7*width/16   ,   -height/2);
        ret.lineTo(0            ,   -height/2);
        ret.lineTo(-width/8     ,   0);
        
        ret.closePath(); 
        return ret;
    }
    
}
