/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 11 de Dez de 2010
 */
package pt.lsts.neptus.plugins.uavs;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

//selected for termination

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
