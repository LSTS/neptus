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
 * Author: Paulo Dias e ZePinto
 * 2007/07/08
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
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
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;

/**
 * @author pdias
 * @author Zé Carlos
 */
public class PopUp extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization {

    protected double radiusTolerance = 2;
    protected int duration = 5;
    protected SpeedType speed = new SpeedType(1000, Units.RPM);
    protected ManeuverLocation destination = new ManeuverLocation();
    protected static final String DEFAULT_ROOT_ELEMENT = "PopUp";
	
	private boolean currPos = false;
	private boolean waitAtSurface = false;
	private boolean stationKeep = false; // To become deprecated

	public String getType() {
		return "PopUp";
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
	   
	    Element duration = root.addElement("duration");
	    duration.setText(String.valueOf(getDuration()));
	    
	    
	    SpeedType.addSpeedElement(root, this);
	    Element flags = root.addElement("flags");
	    flags.addAttribute("CurrPos", ""+isCurrPos());
	    flags.addAttribute("WaitAtSurface", ""+isWaitAtSurface());
	    // flags.addAttribute("StationKeep", ""+isStationKeep());
	    
	    return document;
    }
	
	public void loadManeuverFromXML(String xml) {
	    try {
	        Document doc = DocumentHelper.parseText(xml);
	        Node node = doc.selectSingleNode("PopUp/finalPoint/point");
	        ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);	        
	        setRadiusTolerance(Double.parseDouble(doc.selectSingleNode("PopUp/finalPoint/radiusTolerance").getText()));
	        Node durNode = doc.selectSingleNode("PopUp/duration");
	        if (durNode != null)
	        	setDuration(Integer.parseInt(durNode.getText()));
	       
	        SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
	        
	        Node flagsNode = doc.selectSingleNode("PopUp/flags");
	        setCurrPos(Boolean.parseBoolean(flagsNode.valueOf("@CurrPos")));
	        setWaitAtSurface(Boolean.parseBoolean(flagsNode.valueOf("@WaitAtSurface")));
	        // setStationKeep(Boolean.parseBoolean(flagsNode.valueOf("@StationKeep")));
	    }
	    catch (Exception e) {
	        NeptusLog.pub().error(this, e);
	        return;
	    }
    }

	public Object clone() {  
		PopUp clone = new PopUp();
	    super.clone(clone);
		clone.setManeuverLocation(destination.clone());
	    clone.setDuration(getDuration());
	    clone.setRadiusTolerance((getRadiusTolerance()));
	    clone.setSpeed(getSpeed());
	    clone.setCurrPos(isCurrPos());
	    clone.setWaitAtSurface(isWaitAtSurface());
//	    clone.setStationKeep(isStationKeep());
	    return clone;
	}

    public double getRadiusTolerance() {
        return radiusTolerance;
    }
    
    public void setRadiusTolerance(double radiusTolerance) {
        this.radiusTolerance = radiusTolerance;
    }
    
    /**
	 * @return the duration
	 */
	public int getDuration()
	{
		return duration;
	}

	/**
	 * @param duration the duration to set
	 */
	public void setDuration(int duration)
	{
		this.duration = duration;
	}

    public void translate(double offsetNorth, double offsetEast, double offsetDown) {    
    	destination.translatePosition(offsetNorth, offsetEast, offsetDown);
    }
    
    @Override
    protected Vector<DefaultProperty> additionalProperties() {
    	Vector<DefaultProperty> properties = new Vector<DefaultProperty>();

    	properties.add(PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, getSpeed(), true));
    	
    	DefaultProperty radProp = PropertiesEditor.getPropertyInstance("Radius", Double.class, getRadiusTolerance(), true);
    	radProp.setShortDescription("(m)");
    	properties.add(radProp);

    	DefaultProperty durProp = PropertiesEditor.getPropertyInstance("Duration", Integer.class, getDuration(), true);
    	durProp.setShortDescription("(s)");
    	properties.add(durProp);
    	
    	properties.add(PropertiesEditor.getPropertyInstance("CURR_POS", "Flags", Boolean.class, isCurrPos(), true));
    	properties.add(PropertiesEditor.getPropertyInstance("WAIT_AT_SURFACE", "Flags", Boolean.class, isWaitAtSurface(), true));
    	properties.add(PropertiesEditor.getPropertyInstance("STATION_KEEP", "Flags", Boolean.class, isStationKeep(), false)); // To become deprecated
    	
    	return properties;
    }
    
    public String getPropertiesDialogTitle() {    
    	return getId()+" parameters";
    }
    
    public void setProperties(Property[] properties) {
    	
    	super.setProperties(properties);
    	
    	for (Property p : properties) {
    		if (p.getName().equalsIgnoreCase("Speed")) {
    			setSpeed((SpeedType)p.getValue());
    		}
    		else if (p.getName().equalsIgnoreCase("Radius")) {
    			setRadiusTolerance((Double)p.getValue());
    		}
    		else if (p.getName().equalsIgnoreCase("Duration")) {
    			setDuration((Integer)p.getValue());
    		}
    		else if (p.getName().equalsIgnoreCase("CURR_POS")) {
    		    setCurrPos((Boolean)p.getValue());
    		}
    		else if (p.getName().equalsIgnoreCase("WAIT_AT_SURFACE")) {
    		    setWaitAtSurface((Boolean)p.getValue());
    		}
//            if (p.getName().equals("STATION_KEEP")) {
//                setStationKeep((Boolean)p.getValue());
//            }
    	}
    }
    
	public String[] getPropertiesErrors(Property[] properties) {
		return super.getPropertiesErrors(properties);
	}
    
	@Override
    public ManeuverLocation getManeuverLocation() {
    	return destination.clone();
    }
    
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
    
	/**
     * @return the currPos
     */
    public boolean isCurrPos() {
        return currPos;
    }

    /**
     * @param currPos the currPos to set
     */
    public void setCurrPos(boolean currPos) {
        this.currPos = currPos;
    }

    /**
     * @return the waitAtSurface
     */
    public boolean isWaitAtSurface() {
        return waitAtSurface;
    }

    /**
     * @param waitAtSurface the waitAtSurface to set
     */
    public void setWaitAtSurface(boolean waitAtSurface) {
        this.waitAtSurface = waitAtSurface;
    }

    /**
     * @return the stationKeep
     */
    public boolean isStationKeep() {
        return stationKeep;
    }

//    /**
//     * @param stationKeep the stationKeep to set
//     */
//    public void setStationKeep(boolean stationKeep) {
//        this.stationKeep = stationKeep;
//    }

    @Override
	public String getTooltipText() {
		return super.getTooltipText()+"<hr>"+
		I18n.text("speed") + ": <b>"+getSpeed()+"</b>"+
		"<br>" + I18n.text("cruise depth") + ": <b>"+(int)destination.getDepth()+" " + I18n.textc("m", "meters") + "</b>"+
		"<br>" + I18n.text("duration") + ": <b>"+getDuration()+" " + I18n.textc("s", "seconds") + "</b>";
	}
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        AffineTransform at = g2d.getTransform();
        // x marks the spot...
        g2d.drawLine(-4, -4, 4, 4);
        g2d.drawLine(-4, 4, 4, -4);
        double radius = this.getRadiusTolerance() * renderer.getZoom();
        g2d.setColor(new Color(255,255,255,100));
        g2d.fill(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] {3,3}, 0));
        g2d.setColor(Color.blue.darker());
        g2d.draw(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
        g2d.setStroke(new BasicStroke());
        g2d.setTransform(at);
    }

	
	@Override
	public void parseIMCMessage(IMCMessage message) {
	    
	    pt.lsts.imc.PopUp msgPopup = null; 
	    try {
	        msgPopup = pt.lsts.imc.PopUp.clone(message);
	    }
	    catch (Exception e) {
	        e.printStackTrace();
	        return;
	    }
	    
		setMaxTime(msgPopup.getTimeout());
    	
		speed = SpeedType.parseImcSpeed(message);
    	
        setRadiusTolerance(msgPopup.getRadius());
    	setDuration(msgPopup.getDuration());
    	ManeuverLocation pos = new ManeuverLocation();
    	pos.setLatitudeRads(msgPopup.getLat());
    	pos.setLongitudeRads(msgPopup.getLon());
    	pos.setZ(msgPopup.getZ());
        pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(msgPopup.getZUnits().toString()));
        setManeuverLocation(pos);
        
        // flags
        setCurrPos((msgPopup.getFlags() & pt.lsts.imc.PopUp.FLG_CURR_POS) == pt.lsts.imc.PopUp.FLG_CURR_POS);
	setWaitAtSurface((msgPopup.getFlags() & pt.lsts.imc.PopUp.FLG_WAIT_AT_SURFACE) == pt.lsts.imc.PopUp.FLG_WAIT_AT_SURFACE);
//        setStationKeep((msgPopup.getFlags() & pt.lsts.imc.PopUp.FLG_STATION_KEEP) == pt.lsts.imc.PopUp.FLG_STATION_KEEP);
        
	}
	
	public IMCMessage serializeToIMC() {
	    pt.lsts.imc.PopUp msg = new pt.lsts.imc.PopUp();
	    msg.setTimeout(getMaxTime());
	    //double[] latLonDepth = this.getManeuverLocation().getAbsoluteLatLonDepth();
	    LocationType loc = getManeuverLocation();
	    loc.convertToAbsoluteLatLonDepth();
	    msg.setLat(loc.getLatitudeRads());
	    msg.setLon(loc.getLongitudeRads());
		msg.setZ(getManeuverLocation().getZ());
		
		msg.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));
	    msg.setDuration(getDuration());
	    
	    speed.setSpeedToMessage(msg);
		
	    msg.setRadius(getRadiusTolerance());
		
	    LinkedHashMap<String, Object> tl = new LinkedHashMap<String, Object>();
        
		for (String key : getCustomSettings().keySet())
            tl.put(key, getCustomSettings().get(key));
        msg.setCustom(getCustomSettings());

        short flags = 0;
        if (isStationKeep())
            flags |= pt.lsts.imc.PopUp.FLG_STATION_KEEP;
        if (isCurrPos())
            flags |= pt.lsts.imc.PopUp.FLG_CURR_POS;
        if (isWaitAtSurface())
            flags |= pt.lsts.imc.PopUp.FLG_WAIT_AT_SURFACE;
        
        msg.setFlags(flags);
        
		return msg;
	}

    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }

	public static void main(String[] args) {
        PopUp popup = new PopUp();
        popup.setRadiusTolerance(10);
        popup.setSpeed(new SpeedType(1.2, Units.MPS));
        
        popup.setDuration(300);
        
        popup.setCurrPos(true);
        popup.setWaitAtSurface(true);
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitudeDegs(0);
        loc.setLongitudeDegs(0);
        loc.setDepth(3);
        popup.setManeuverLocation(loc);
        String xml1 = popup.asXML();
        IMCMessage msg1 = popup.serializeToIMC();
        msg1.dump(System.out);
        popup.parseIMCMessage(msg1);
        String xml2 = popup.asXML();
        popup.loadManeuverFromXML(xml2);
        popup.serializeToIMC().dump(System.out);
        System.out.println(xml1);
        System.out.println(xml2);
        System.out.println(popup.asXML());        
    }
	
	@Override
	public SpeedType getSpeed() {
	    return new SpeedType(speed);
	}
	
	@Override
	public void setSpeed(SpeedType speed) {
	    this.speed = new SpeedType(speed);	    
	}
}
