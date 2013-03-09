/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Canasta
 * 15 de Dez de 2010
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.foreground;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * @author Canasta
 *
 */
public class UavRulerPainter implements IUavPainter{

    //predetermined base height for each ruler mark
    public static final int MARK_MIN_HEIGHT = 4;
    
    //default number of marks per ruler section
    public static final int DEFAULT_MARKS_PER_SECTION = 10;
    
    //number of marks per ruler that will be used
    public int marksPerSection;
			
	private LinkedHashMap<String,Object> receivedArgs;	
	private Point pixelsPerMark_markGrade_Pair;
	
	public UavRulerPainter(int marks_per_section){
	    marksPerSection = marks_per_section;
	}
	
	public UavRulerPainter(){
	    marksPerSection = DEFAULT_MARKS_PER_SECTION;
	}
	
    //------Implemented Interfaces------//
	
	//IUavPainter_BEGIN
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {

        receivedArgs = (LinkedHashMap<String, Object>) args;    
        
        if(!receivedArgs.isEmpty()){
            pixelsPerMark_markGrade_Pair = (Point) receivedArgs.get("markInfo"); 
        }
        
        int markHeight = pixelsPerMark_markGrade_Pair.x/4;
        int intervalSpace = pixelsPerMark_markGrade_Pair.x-(2*markHeight);        
        int markCounter = 0;
        int markGrade = 0;
               
        //anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        while(height>0){         
            
            height -= markHeight;
            drawMark(g,height,width,markCounter,markHeight);
            
            height -= intervalSpace;
            
            markCounter++;
            if(markCounter>marksPerSection)
                markCounter=1;
            
            markGrade += pixelsPerMark_markGrade_Pair.y;  
            
            height -= markHeight;            
            drawMark(g,height,width,markCounter,markHeight);
            
            drawText(g,height,width,markCounter,markHeight,markGrade);                  
        }
        
    }
    //IUavPainter_END
   
    
    //------Specific Methods------//
        
    /**
     * @param g 
     * @param height
     * @param i
     * @param markHeight
     * @param pixelsPerMark 
     */
    private void drawText(Graphics2D g, int height, int width, int i, int markHeight, int markGrade) {
        if(i==marksPerSection)
            g.drawString(String.valueOf(markGrade), width/4+3, height+markHeight);        
    }

    /**
     * @param g 
     * @param height
     * @param i
     * @param markHeight
     */
    private void drawMark(Graphics2D g, int height, int width, int i, int markHeight) {
        if(i==0 || i== 10)
            g.fillRect(0, height, width/5, markHeight);
        else if(i==5)
            g.fillRect(0, height, width/8, markHeight);
        else
            g.fillRect(0, height, width/16, markHeight);
    }
}
