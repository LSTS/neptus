/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2004/09/21
 */
package pt.lsts.neptus.mp.maneuvers;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Zé Carlos
 */

public class Goto extends Maneuver implements IMCSerialization, LocatedManeuver, ManeuverWithSpeed {

    double speedTolerance = 0, radiusTolerance = 2;
    //double speed;
    //Maneuver.SPEED_UNITS speedUnits = SPEED_UNITS.METERS_PS;
    SpeedType speed = new SpeedType();
    ManeuverLocation destination = new ManeuverLocation();
    protected static final String DEFAULT_ROOT_ELEMENT = "Goto";
	
	protected double targetAngle, rotateIncrement;
	protected double roll, pitch, yaw;
	
	LinkedHashMap<String, String> custom = new LinkedHashMap<>();
	
	public String getType() {
		return "Goto";
	}
	
	public Document getManeuverAsDocument(String rootElementName) {
        
	    Document document = DocumentHelper.createDocument();
	    Element root = document.addElement( rootElementName );
	    root.addAttribute("kind", "automatic");
	    Element finalPoint = root.addElement("finalPoint");
	    finalPoint.addAttribute("type", "pointType");
	    Element point = destination.asElement("point");
	    finalPoint.add(point);

	    Element radTolerance = finalPoint.addElement("radiusTolerance");
	    radTolerance.setText(String.valueOf(getRadiusTolerance()));
	    SpeedType.addSpeedElement(root, this);
	    Element trajectoryTolerance = root.addElement("trajectoryTolerance");
	    Element radiusTolerance = trajectoryTolerance.addElement("radiusTolerance");
	    radiusTolerance.setText(String.valueOf(getRadiusTolerance()));

	    return document;
    }
	
	
	public void loadManeuverFromXML(String xml) {  
	    try {
	        Document doc = DocumentHelper.parseText(xml);
	        Node node = doc.selectSingleNode(getType()+"/finalPoint/point");
	        if (node != null) {
	            ManeuverLocation loc = new ManeuverLocation();
	            loc.load(node.asXML());
	            setManeuverLocation(loc);
	        }
	        SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
	    }
	    catch (Exception e) {
	        NeptusLog.pub().info("<###> "+I18n.text("Error while loading the XML:")+"{" + xml + "}");
	        NeptusLog.pub().error(this, e);
	        return;
	    }
    }
	
	public Object clone() {  
	    Goto clone = new Goto();
	    super.clone(clone);
	    clone.setManeuverLocation(getManeuverLocation());
	    clone.setRadiusTolerance(getRadiusTolerance());
	    clone.setSpeed(getSpeed());
	    clone.setSpeedTolerance(getSpeedTolerance());
	    
	    return clone;
	}

    public double getRadiusTolerance() {
        return radiusTolerance;
    }
    
    public void setRadiusTolerance(double radiusTolerance) {
        this.radiusTolerance = radiusTolerance;
    }
    
    
    public SpeedType getSpeed() {
        return new SpeedType(speed);
    }
    
    public double getSpeedTolerance() {
        return speedTolerance;
    }
    
    public void setSpeedTolerance(double speedTolerance) {
        this.speedTolerance = speedTolerance;
    }
        
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {    
    	destination.translatePosition(offsetNorth, offsetEast, offsetDown);
    }
    
    @Override
    protected Vector<DefaultProperty> additionalProperties() {
    	Vector<DefaultProperty> properties = new Vector<DefaultProperty>();

    	
    	DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, getSpeed(), true);
    	speed.setDisplayName(I18n.text("Speed"));
    	speed.setShortDescription(I18n.text("The speed"));
    	properties.add(speed);

    	return properties;
    }
    
    
    public String getPropertiesDialogTitle() {    
    	return getId()+" parameters";
    }
    
    public void setProperties(Property[] properties) {
    	
    	super.setProperties(properties);
    	
    	for (Property p : properties) {
//    		if (p.getName().equalsIgnoreCase("Speed units")) {
//    		    if (p.getValue() instanceof Maneuver.SPEED_UNITS)
//    		        setSpeedUnits(((Maneuver.SPEED_UNITS)p.getValue()));
////    		    else
////    		        setSpeedUnits((String) p.getValue());
//    		}
    		if (p.getName().equalsIgnoreCase("Speed tolerance")) {
    			setSpeedTolerance((Double)p.getValue());
    		}
    		else if (p.getName().equalsIgnoreCase("Speed")) {
    			setSpeed((SpeedType)p.getValue());
    		}
    		else if (p.getName().equalsIgnoreCase("Radius tolerance")) {
    			setRadiusTolerance((Double)p.getValue());
    		}
    		else {
    		    NeptusLog.pub().debug("Property "+p.getName()+" ignored.");
    		}
    	}
    }
    
	public String[] getPropertiesErrors(Property[] properties) {
		return super.getPropertiesErrors(properties);
	}
    
    @Override
	public ManeuverLocation getManeuverLocation() {
    	return destination.clone();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocationProvider#getFirstPosition()
     */
    @Override
    public ManeuverLocation getStartLocation() {
        return destination.clone();
    }
    
    @Override
    public ManeuverLocation getEndLocation() {
        return destination.clone();
    }
    
    public void setManeuverLocation(ManeuverLocation location) {
    	destination = location.clone();
    }
    
    @Override
	public String getTooltipText() {
    	NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
		
		return super.getTooltipText()+"<hr>"+
		I18n.text("speed") + ": <b>"+getSpeed().toString()+"</b>"+
		"<br>"+I18n.text(destination.getZUnits().toString())+": <b>"+nf.format(destination.getZ())+" " + I18n.textc("m", "meters") + "</b>";
	}
    
    @Override
    public void parseIMCMessage(IMCMessage message) {
        try {
            pt.lsts.imc.Goto msg = pt.lsts.imc.Goto.clone(message);
            
            setMaxTime(msg.getTimeout());
            speed = SpeedType.parseImcSpeed(message);
            ManeuverLocation pos = new ManeuverLocation();
            pos.setLatitudeRads(msg.getLat());
            pos.setLongitudeRads(msg.getLon());
            pos.setZ(msg.getZ());
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(msg.getZUnits().toString()));
            setManeuverLocation(pos);
            roll = msg.getRoll();
            pitch = msg.getPitch();
            yaw = msg.getYaw();            
            setCustomSettings(msg.getCustom());
            
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
    
	public IMCMessage serializeToIMC() {
		pt.lsts.imc.Goto gotoManeuver = new pt.lsts.imc.Goto();
		gotoManeuver.setTimeout(this.getMaxTime());
		LocationType l = getManeuverLocation();
		l.convertToAbsoluteLatLonDepth();
		
		gotoManeuver.setLat(l.getLatitudeRads());
		gotoManeuver.setLon(l.getLongitudeRads());
		gotoManeuver.setZ(getManeuverLocation().getZ());
		gotoManeuver.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().name()));
		getSpeed().setSpeedToMessage(gotoManeuver);
		gotoManeuver.setRoll(roll);
		gotoManeuver.setPitch(pitch);
		gotoManeuver.setYaw(yaw);
		gotoManeuver.setCustom(getCustomSettings());

		return gotoManeuver;
	}   
	
    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }


    public static void main(String[] args) {
    	Goto g = new Goto();
		PropertiesEditor.editProperties(g, true);
		PropertiesEditor.editProperties(g, true);
	}

    @Override
    public void setSpeed(SpeedType speed) {
        this.speed = new SpeedType(speed);
    }

}
