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
