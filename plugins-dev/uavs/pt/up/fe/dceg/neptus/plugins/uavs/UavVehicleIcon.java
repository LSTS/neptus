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
 * 11 de Dez de 2010
 * $Id:: UavVehicleIcon.java 9615 2012-12-30 23:08:28Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

/**
 * @author Canasta
 *
 */
public class UavVehicleIcon {
    
	private String drawMode;
	private Shape icon;
	private Color alertLevel;
	private Rectangle iconContactSurface;
	private int height;
	private int width;
	
	public UavVehicleIcon(int height,int width, String drawMode){
		setHeight(height);
		setWidth(width);
		setAlertLevel(Color.green.darker());
		generateIconContactSurface();
		setDrawMode(drawMode);

		buildIcon();
	}
	
	public UavVehicleIcon(String drawMode){
	    setHeight(1);
	    setWidth(1);
	    setAlertLevel(Color.green.darker());
	    generateIconContactSurface();
	    setDrawMode(drawMode);

	    buildIcon();
	}
	
	//------Setters and Getters------/
		
	//Icon
	public Shape getIcon() {
		return icon;
	}

	//DrawMode
	public void setDrawMode(String drawMode) {
        this.drawMode = drawMode;
    }

    public String getDrawMode() {
        return drawMode;
    }

	//AlertLevel
	public void setAlertLevel(Color alertLevel) {
        this.alertLevel = alertLevel;
    }
    
	public Color getAlertLevel() {
		return alertLevel;
	}
	
    //IconContactSurface
    public void generateIconContactSurface() {
        this.iconContactSurface = new Rectangle(height,width);
    }

    public Rectangle getIconContactSurface() {
        return iconContactSurface;
    }

	//Height
	public void setHeight(int height) {
		this.height = height;
	}

	public int getHeight() {
		return height;
	}

	//Width
	public void setWidth(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}
	
	//------Implemented Interfaces------/

	//------Specific Methods------/
	
	
    public void updateIconContactSurface(double x, double y){       
        this.iconContactSurface.setLocation((int)(x - iconContactSurface.width/2), (int)(y - iconContactSurface.height/2));     
    }
    
	public void buildIcon() {
		GeneralPath ret = new GeneralPath();
		
		if(drawMode.equalsIgnoreCase("Side View")){
			
			//plane's nose
			ret.moveTo((-width/4)	, 	(-height/3));
			ret.lineTo((-width/2)	, 	(-height/8));
			ret.lineTo((-width/2)	, 	0);
			ret.lineTo((-width/4)	, 	(height/8));
			
			//plane's tail
			ret.lineTo((width/4)	, 	(height/8));
			ret.lineTo((width*5/12)	, 	(height/2));
			ret.lineTo((width/2)	, 	(height/2));
			ret.lineTo((width/2)	, 	0);
			ret.lineTo((width/4)	, 	(-height/3));
			
			ret.closePath();
		}
		else{
		    
			ret.moveTo((-width/2)    ,   (-height/2));
			ret.lineTo(0             ,   (height/2));
			ret.lineTo((width/2)     ,   (-height/2));
			ret.lineTo(0             ,   ((-height/2)*3/5));
			
			ret.closePath();
		}
				
		this.icon = ret;
	}
}
