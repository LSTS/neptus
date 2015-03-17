/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.editor.SpeedUnitsEditor;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.NameNormalizer;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author pdias
 * @author Zé Carlos
 */
public class PopUp extends Maneuver implements LocatedManeuver, IMCSerialization {

    double speed = 1000, speedTolerance = 100, radiusTolerance = 2;
    int duration = 5;
    String units = "RPM";
    ManeuverLocation destination = new ManeuverLocation();
    protected static final String DEFAULT_ROOT_ELEMENT = "PopUp";
	
	private final int ANGLE_CALCULATION = -1;
	private final int FIRST_ROTATE = 0;
	private final int HORIZONTAL_MOVE = 1;
	int current_state = ANGLE_CALCULATION;
	
	private double targetAngle, rotateIncrement;
	private boolean currPos = false;
	private boolean waitAtSurface = true, stationKeep = true; // To become deprecated
	
	public String id = NameNormalizer.getRandomID();
	
	
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
	    
	    Element velocity = root.addElement("speed");
	    velocity.addAttribute("tolerance", String.valueOf(getSpeedTolerance()));
	    velocity.addAttribute("type", "float");
	    velocity.addAttribute("unit", getUnits());
	    velocity.setText(String.valueOf(getSpeed()));
	    
	    Element flags = root.addElement("flags");
	    flags.addAttribute("CurrPos", ""+isCurrPos());
	    // flags.addAttribute("StationKeep", ""+isStationKeep());
	    // flags.addAttribute("WaitAtSurface", ""+isWaitAtSurface());        
	    
	    return document;
    }
	
	
	public void loadFromXML(String xml) {
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
	        Node speedNode = doc.selectSingleNode("PopUp/speed");
	        if (speedNode == null) 
	        	speedNode = doc.selectSingleNode("PopUp/velocity");
	        setSpeed(Double.parseDouble(speedNode.getText()));
	        setSpeedUnits(speedNode.valueOf("@unit"));
	        setSpeedTolerance(Double.parseDouble(speedNode.valueOf("@tolerance")));
	        
	        Node flagsNode = doc.selectSingleNode("PopUp/flags");
	        setCurrPos(Boolean.parseBoolean(flagsNode.valueOf("@CurrPos")));
	        // setWaitAtSurface(Boolean.parseBoolean(flagsNode.valueOf("@WaitAtSurface")));
	        // setStationKeep(Boolean.parseBoolean(flagsNode.valueOf("@StationKeep")));
	    }
	    catch (Exception e) {
	        NeptusLog.pub().error(this, e);
	        return;
	    }
    }
	
	private int count = 0;
	
	public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
	    
	 SystemPositionAndAttitude nextVehicleState = (SystemPositionAndAttitude) lastVehicleState.clone();
	 
	 
		switch (current_state) {
		
			case(ANGLE_CALCULATION):
				targetAngle = lastVehicleState.getPosition().getXYAngle(destination);
				
				double angleDiff = (targetAngle - lastVehicleState.getYaw());
				
				while (angleDiff < 0)
					angleDiff += Math.PI*2; //360º
				
				while (angleDiff > Math.PI*2)
					angleDiff -= Math.PI*2;
				
				if (angleDiff > Math.PI)
					angleDiff = angleDiff - Math.PI*2;
				
				rotateIncrement = angleDiff/3;//(-25.0f / 180.0f) * (float) Math.PI;
				count = 0;
				this.current_state = FIRST_ROTATE;
				nextVehicleState = ManeuverFunction(lastVehicleState);
			break;
		
			// Initial rotation towards the target point
			case FIRST_ROTATE:
				if (count++<3)
					nextVehicleState.rotateXY(rotateIncrement);
				else {
					nextVehicleState.setYaw(targetAngle);		
					current_state = HORIZONTAL_MOVE;
				}			
				break;
		
			// The movement between the initial and final point, in the plane xy (horizontal)
			case HORIZONTAL_MOVE:
				double calculatedSpeed = 1;
				
				if (units.equals("m/s"))
					calculatedSpeed = speed;
				else if (units.equals("RPM"))
					calculatedSpeed = speed/500.0;
				double dist = nextVehicleState.getPosition().getHorizontalDistanceInMeters(destination);
				if (dist <= calculatedSpeed) {
					nextVehicleState.setPosition(destination);
					endManeuver();
				}
				else {					
						nextVehicleState.moveForward(calculatedSpeed);
						double depthDiff = destination.getDepth()-nextVehicleState.getPosition().getDepth();
						
						double depthIncr = depthDiff / (dist/calculatedSpeed);
						double curDepth = nextVehicleState.getPosition().getDepth();
						nextVehicleState.getPosition().setDepth(curDepth+depthIncr);
				}
				break;
			
			default:
				endManeuver();
		}
		
		return nextVehicleState;
	}
	

	public Object clone() {  
		
		PopUp clone = new PopUp();
	    super.clone(clone);
		clone.setManeuverLocation(destination.clone());
	    clone.setDuration(getDuration());
	    clone.setRadiusTolerance((getRadiusTolerance()));
	    clone.setSpeedUnits(getUnits());
	    clone.setSpeed(getSpeed());
	    clone.setSpeedTolerance(getSpeedTolerance());
	    clone.setCurrPos(isCurrPos());
//	    clone.setStationKeep(isStationKeep());
//	    clone.setWaitAtSurface(isWaitAtSurface());
	    return clone;
	}

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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

	public String getUnits() {
        return units;
    }
    
    public void setSpeedUnits(String units) {
        this.units = units;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public void setSpeed(double speed) {
        this.speed = speed;
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

    	DefaultProperty units = PropertiesEditor.getPropertyInstance("Speed units", String.class, getUnits(), true);
    	units.setShortDescription("The speed units");
    	PropertiesEditor.getPropertyEditorRegistry().registerEditor(units, new SpeedUnitsEditor());
    	PropertiesEditor.getPropertyRendererRegistry().registerRenderer(units, new I18nCellRenderer());
    
    	properties.add(PropertiesEditor.getPropertyInstance("Speed", Double.class, getSpeed(), true));
    	properties.add(units);
    	
    	properties.add(PropertiesEditor.getPropertyInstance("Radius", Double.class, getRadiusTolerance(), true));
        
    	
    	//properties.add(PropertiesEditor.getPropertyInstance("Speed tolerance", Double.class, getSpeedTolerance(), true));

    	properties.add(PropertiesEditor.getPropertyInstance("Duration", Integer.class, getDuration(), true));
    	
    	properties.add(PropertiesEditor.getPropertyInstance("CURR_POS", "Flags", Boolean.class, isCurrPos(), true));
    	properties.add(PropertiesEditor.getPropertyInstance("STATION_KEEP", "Flags", Boolean.class, isStationKeep(), false)); // To become deprecated
    	properties.add(PropertiesEditor.getPropertyInstance("WAIT_AT_SURFACE", "Flags", Boolean.class, isWaitAtSurface(), false)); // To become deprecated
    	
    	return properties;
    }
    
    
    public String getPropertiesDialogTitle() {    
    	return getId()+" parameters";
    }
    
    public void setProperties(Property[] properties) {
    	
    	super.setProperties(properties);
    	
    	for (Property p : properties) {
    		if (p.getName().equals("Speed units")) {
    			setSpeedUnits((String)p.getValue());
    		}
    		//if (p.getName().equals("Speed tolerance")) {
    		//	setSpeedTolerance((Double)p.getValue());
    		//}
    		if (p.getName().equals("Speed")) {
    			setSpeed((Double)p.getValue());
    		}
    		if (p.getName().equals("Radius")) {
    			setRadiusTolerance((Double)p.getValue());
    		}
    		if (p.getName().equals("Duration")) {
    			setDuration((Integer)p.getValue());
    		}
    		if (p.getName().equals("CURR_POS")) {
    		    setCurrPos((Boolean)p.getValue());
    		}
//            if (p.getName().equals("STATION_KEEP")) {
//                setStationKeep((Boolean)p.getValue());
//            }
//            if (p.getName().equals("WAIT_AT_SURFACE")) {
//                setWaitAtSurface((Boolean)p.getValue());
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

//    /**
//     * @param waitAtSurface the waitAtSurface to set
//     */
//    public void setWaitAtSurface(boolean waitAtSurface) {
//        this.waitAtSurface = waitAtSurface;
//    }

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
		I18n.text("speed") + ": <b>"+getSpeed()+" "+I18n.text(getUnits())+"</b>"+
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
    	setSpeed(msgPopup.getSpeed());

    	switch (msgPopup.getSpeedUnits()) {
    	    case METERS_PS:
    	        setSpeedUnits("m/s");
    	        break;
    	    case RPM:
    	        setSpeedUnits("RPM");
    	        break;
    	    case PERCENTAGE:
    	        setSpeedUnits("%");
    	        break;
    	    default:
    	        break;
    	}
    	
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
//        setWaitAtSurface((msgPopup.getFlags() & pt.lsts.imc.PopUp.FLG_WAIT_AT_SURFACE) == pt.lsts.imc.PopUp.FLG_WAIT_AT_SURFACE);
//        setStationKeep((msgPopup.getFlags() & pt.lsts.imc.PopUp.FLG_STATION_KEEP) == pt.lsts.imc.PopUp.FLG_STATION_KEEP);
        
	}
	
	public IMCMessage serializeToIMC()
	{
	    pt.lsts.imc.PopUp msg = new pt.lsts.imc.PopUp();
	    msg.setTimeout(getMaxTime());
	    //double[] latLonDepth = this.getManeuverLocation().getAbsoluteLatLonDepth();
	    LocationType loc = getManeuverLocation();
	    loc.convertToAbsoluteLatLonDepth();
	    msg.setLat(loc.getLatitudeRads());
	    msg.setLon(loc.getLongitudeRads());
		msg.setZ(getManeuverLocation().getZ());
		
		msg.setZUnits(pt.lsts.imc.PopUp.Z_UNITS.valueOf(getManeuverLocation().getZUnits().toString()));
	    msg.setDuration(getDuration());
	    msg.setSpeed(speed);
	    switch (units) {
            case "RPM":
                msg.setSpeedUnits(pt.lsts.imc.PopUp.SPEED_UNITS.RPM);
                break;
            case "%":
                msg.setSpeedUnits(pt.lsts.imc.PopUp.SPEED_UNITS.PERCENTAGE);
                break;
            case "m/s":
                msg.setSpeedUnits(pt.lsts.imc.PopUp.SPEED_UNITS.METERS_PS);
                break;
            default:
                break;
        }
		
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
        popup.setSpeed(1.2);
        popup.setSpeedUnits("m/s");
        popup.setDuration(300);
        
        popup.setCurrPos(true);
//        popup.setWaitAtSurface(true);
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
        popup.loadFromXML(xml2);
        popup.serializeToIMC().dump(System.out);
        System.out.println(xml1);
        System.out.println(xml2);
        System.out.println(popup.asXML());        
    }
}
