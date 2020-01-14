/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Canasta
 * 15 de Dez de 2010
 */
package pt.lsts.neptus.plugins.uavs.painters.foreground;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;

import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * Neptus UavPainter intended to be used by the <b>UavAltitudePanel</b> to draw a vertical ruler which allows the viewing of the UAVs current altitude.
 * This painter functions are limited to positive altitude so it is incompatible with AUV measures.
 * 
 * <p>Accepted arguments in <code>LinkedHashMap< String,Object ></code> <b>receivedArgs</b></p> 
 * <ul>
 *  <li><b>Key:</b> name+".MaxAlt" <b>Value:</b> int </li>
 * </ul>
 * 
 * @author canastaman
 * @version 2.0
 * @category UavPainter  
 * 
 */
public class UavRulerPainter implements IUavPainter{

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
    
	private LinkedHashMap<String,Object> receivedArgs;	
		
	public UavRulerPainter(String name){
	    this.name = name;
	    this.vehicleMaxAtl = 0;
	    this.maxDrawAlt = 0;
	    this.scale = 100;
	    this.rulerSections = 0;
	}
	
    //------Implemented Interfaces------//
	
    //IUavPainter_BEGIN
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
        
        int markCounter = 0;
        int markGrade = 1;
                       
        //anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        while(height>0){         
            
            height -= MARK_MIN_THICKNESS;
            drawMark(g,height,width,markCounter,MARK_MIN_THICKNESS);            
            
            if(markCounter==MARKS_PER_SECTION){
                g.drawString(String.valueOf(markGrade*scale), width/4+3, height+MARK_MIN_THICKNESS); 
                markCounter = 0;
                markGrade++;
            }
                        
            height -= MARK_MIN_SPACING;
            
            markCounter++;                
        }
        
    }
    //IUavPainter_END
   
    
    //------Specific Methods------//

    /**
     * Private method with draws each of the ruler marks with the appropriate in order to give a visual correlation with a real ruler
     *  
     * @param g 
     * @param panelHeight
     * @param panelWidth
     * @param markCounter
     * @param markThickness
     * 
     * @return void
     */
    private void drawMark(Graphics2D g, int panelHeight, int panelWidth, int markCounter, int markThickness) {
        if(markCounter==0 || markCounter== 10)
            g.fillRect(0, panelHeight, panelWidth/5, markThickness);
        else if(markCounter==5)
            g.fillRect(0, panelHeight, panelWidth/8, markThickness);
        else
            g.fillRect(0, panelHeight, panelWidth/16, markThickness);
    }
}
