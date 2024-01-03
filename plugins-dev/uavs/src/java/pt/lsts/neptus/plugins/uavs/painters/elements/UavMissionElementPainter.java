/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Sérgio Ferreira
 * 5 de Mar de 2012
 */
package pt.lsts.neptus.plugins.uavs.painters.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;

import pt.lsts.neptus.plugins.uavs.UavVehicleIcon;
import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * Vehicle painter designed for use in conjunctions with Neptus' UavAltitudePanel. Its primary function is the side-ways representation of
 * the UAVs active on the field.
 * 
 * <p>Accepted arguments in <code>LinkedHashMap< String,Object ></code> <b>receivedArgs</b></p> 
 * <ul>
 *  <li><b>Key:</b> name+".MaxAlt" <b>Value:</b> int </li>
 *  <li><b>Key:</b> name+".VehicleList" <b>Value:</b> LinkedHashMap< String,Integer > </li>
 * </ul>
 * 
 * @author canastaman
 * @version 3.0
 * @category UavPainter  
 * 
 */
public class UavMissionElementPainter implements IUavPainter{
   
    private String name;
    
    //predetermined width for each ruler mark
    public static final int MARK_MIN_THICKNESS= 2;
    
    //predetermined spacing between each ruler marks
    public static final int MARK_MIN_SPACING = 6;
    
    //predetermined number of marks per ruler section
    public static final int MARKS_PER_SECTION = 10;
    
    //predetermined value to be added to the maximum altitude calculated for drawing purposes
    public static final int MAX_ALTITUDE_BUFFER = 100;
    
    //altitude corresponding to the highest UAV
    private int vehicleMaxAtl;
    
    //maximum drawable altitude using default range
    private int maxDrawAlt;
    
    //predetermined ruler scale based on an always "double up" system starting at 100
    private int scale;
    
    private int rulerSections;
    
    public static int WIDTH_RACIO = 5;
    public static int HEIGHT_RACIO = 7;
        
    private LinkedHashMap<String,Object> receivedArgs;    
    private LinkedHashMap<String,Integer> vehicleAltitudes;
    
    
    private LinkedHashMap<String,UavVehicleIcon> vehicleIconTable = new LinkedHashMap<String,UavVehicleIcon>();
    
    public UavMissionElementPainter(String name) {
        this.name = name;
        this.receivedArgs = new LinkedHashMap<String,Object>();
        this.vehicleAltitudes = new LinkedHashMap<String,Integer>();
        this.vehicleMaxAtl = 0;
        this.maxDrawAlt = 0;
        this.scale = 100;
        this.rulerSections = 0;
    }
    
    //------Implemented Interfaces------//
    
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {
        
        receivedArgs = (LinkedHashMap<String, Object>) args;
        
        if(receivedArgs.get(name+".MaxAlt") != null){
            vehicleMaxAtl = (int) receivedArgs.get(name+".MaxAlt");
            vehicleMaxAtl = (int) Math.floor(vehicleMaxAtl / 100) * 100 + MAX_ALTITUDE_BUFFER;
        }
        
        //standard initiation based on the premised that every draw cycle we check if the ruler scale is accurate, from scratch
        scale = 100;
        rulerSections = height / ((MARK_MIN_THICKNESS+MARK_MIN_SPACING)*MARKS_PER_SECTION);
        maxDrawAlt = scale * rulerSections;
                
        while(maxDrawAlt < vehicleMaxAtl){
            scale = scale << 1;
            maxDrawAlt = scale * rulerSections;
        }
        
        if(receivedArgs.get(name+".VehicleList") != null){
            vehicleAltitudes = (LinkedHashMap<String, Integer>) receivedArgs.get(name+".VehicleList");
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

            int altitude = vehicleAltitudes.get(vehicle) * ((MARK_MIN_THICKNESS+MARK_MIN_SPACING)*MARKS_PER_SECTION) / scale;

            // translate the drawing position to the correct altitude value
            g.translate(0, altitude - (height / 2));

            g.scale(1, -1);
            drawVehicleLabel(g, vehicle);
            g.scale(1, -1);

            // draws a line which marks the altitude across the ruler
            g.setColor(Color.white.darker());
            g.drawRect((-width / 2), 0, height, 0);

            drawVehicleIcon(g, vehicle);

            g.translate(0, -(altitude - (height / 2)));
        }     
    }

    //------Specific Methods------//
    
    private void determineDrawingOriginPoint(Graphics2D g, int height, int width) {
        g.translate(width / 2, height / 2);
        g.scale(1, -1);
    }
    
    private void drawVehicleLabel(Graphics2D g, String vehicle) {
        g.drawString(vehicle, vehicleIconTable.get(vehicle).getWidth()/2, -2);
        g.drawString(vehicleAltitudes.get(vehicle) + " m", vehicleIconTable.get(vehicle).getWidth()/2, 12);
    }
    
    private void drawVehicleIcon(Graphics2D g, String vehicle) {
        
        g.setColor(vehicleIconTable.get(vehicle).getAlertLevel());
        g.fill(vehicleIconTable.get(vehicle).getIcon());
        g.setColor(Color.black);
        g.draw(vehicleIconTable.get(vehicle).getIcon());
    }
}
