/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 2010/07/14
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;

public class StationKeeping extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization, StatisticsProvider {

	public static final int INFINITY_DURATION = 0;
	public static final double MINIMUM_SK_RADIUS = 10;
	
	private int duration = 60;
	private double radius = 10, speed = 30;
	private Maneuver.SPEED_UNITS speedUnits = Maneuver.SPEED_UNITS.METERS_PS;
	private ManeuverLocation location = new ManeuverLocation();	 

	@Override
	public Object clone() {
		StationKeeping l = new StationKeeping();
		super.clone(l);
		l.setDuration(getDuration());
		l.setManeuverLocation(getManeuverLocation().clone());
		l.setRadius(getRadius());
		l.setSpeed(getSpeed());
		l.setSpeedUnits(getSpeedUnits());
		return l;		
	}

	@Override
	public Document getManeuverAsDocument(String rootElementName) {
		Document document = DocumentHelper.createDocument();
	    Element root = document.addElement( rootElementName );
	    root.addAttribute("kind", "automatic");
	    
	    //basePoint
	    Element basePoint = root.addElement("basePoint");
	    Element point = getManeuverLocation().asElement("point");
	    basePoint.add(point);
	    Element radTolerance = basePoint.addElement("radiusTolerance");
	    radTolerance.setText("0");	   
	    basePoint.addAttribute("type", "pointType");
	    
	    //duration
	    root.addElement("duration").setText(""+getDuration());
	    
	    //trajectory
	    Element trajectory = root.addElement("trajectory");	    
	    Element trajRadius = trajectory.addElement("radius");
	    trajRadius.setText(String.valueOf(getRadius()));
	    trajRadius.addAttribute("type", "float");	    

	    //speed
	    Element velocity = root.addElement("speed");
	    //velocity.addAttribute("tolerance", String.valueOf(getSpeedTolerance()));
	    velocity.addAttribute("type", "float");
	    velocity.addAttribute("unit", getSpeedUnits().getString());
	    velocity.setText(String.valueOf(getSpeed()));
	    
	    return document;
	}
	
	@Override
	public String getType() {
		return "StationKeeping";
	}

	@Override
	public void loadFromXML(String XML) {
		try {
	        Document doc = DocumentHelper.parseText(XML);
	        
	        // basePoint
	        Node node = doc.selectSingleNode("StationKeeping/basePoint/point");
	        ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);	       
	        
	        // Speed
	        Node speedNode = doc.selectSingleNode("StationKeeping/speed");
	        setSpeed(Double.parseDouble(speedNode.getText()));
//	        setSpeedUnits(speedNode.valueOf("@unit"));
	        SPEED_UNITS sUnits = ManeuversXMLUtil.parseSpeedUnits((Element) speedNode);
            setSpeedUnits(sUnits);
	        
	        // Duration
	        setDuration(Integer.parseInt(doc.selectSingleNode("StationKeeping/duration").getText()));
	        
	        // Trajectory
	        setRadius(Double.parseDouble(doc.selectSingleNode("StationKeeping/trajectory/radius").getText()));
	    }
	    catch (Exception e) {
	        NeptusLog.pub().error(this, e);
	        return;
	    }
	}
	
	@Override
	public ManeuverLocation getManeuverLocation() {
		return location.clone();
	}
	
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocationProvider#getFirstPosition()
     */
    @Override
    public ManeuverLocation getStartLocation() {
        return location.clone();
    }

	@Override
	public ManeuverLocation getEndLocation() {
	    return location.clone();
	}
	
	public void setManeuverLocation(ManeuverLocation location) {
		this.location = location.clone();
	}
	
	public void translate(double offsetNorth, double offsetEast, double offsetDown) {
		location.translatePosition(offsetNorth, offsetEast, offsetDown);		
	}
	
	@Override
	protected Vector<DefaultProperty> additionalProperties() {
		Vector<DefaultProperty> props = new Vector<DefaultProperty>();
		
		if (radius < MINIMUM_SK_RADIUS)
		    radius = MINIMUM_SK_RADIUS;
		
		DefaultProperty duration = PropertiesEditor.getPropertyInstance("Duration", Integer.class, this.duration, true);
		duration.setShortDescription("The Station Keeping's duration, in seconds (0 means +Infinity)");		
		props.add(duration);
				
		DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", Double.class, this.speed, true);
		speed.setShortDescription("The vehicle's desired speed when Station Keeping");
		props.add(speed);
		
		DefaultProperty speedUnits = PropertiesEditor.getPropertyInstance("Speed Units", Maneuver.SPEED_UNITS.class, this.speedUnits, true);
		speedUnits.setShortDescription("The units to consider in the speed parameters");
		props.add(speedUnits);
		
		DefaultProperty radius = PropertiesEditor.getPropertyInstance("Radius", Double.class, this.radius, true);
		radius.setShortDescription("Radius of the Station Keeping circle. Lower values default to "+MINIMUM_SK_RADIUS+" meters.");
		props.add(radius);
		
		return props;
	}
	
	@Override
	public void setProperties(Property[] properties) {
		super.setProperties(properties);
	
		for (Property p : properties) {
			
			if (p.getName().equals("Duration")) {
				setDuration((Integer)p.getValue());
				continue;
			}
			
			if (p.getName().equals("Speed")) {
				setSpeed((Double)p.getValue());
				continue;
			}
			
//			if (p.getName().equalsIgnoreCase("Speed Units")) {
//				setSpeedUnits((String)p.getValue());
//				continue;
//			}
			
			if (p.getName().equals("Radius")) {
				setRadius(Math.max(MINIMUM_SK_RADIUS, (Double)p.getValue()));
				continue;
			}
			
			// "Speed Units" parsing
            SPEED_UNITS speedUnits = ManeuversUtil.getSpeedUnitsFromPropertyOrNullIfInvalidName(p);
            if (speedUnits != null)
                setSpeedUnits(speedUnits);
		}
	}
	
	@Override
	public String getTooltipText() {
		return super.getTooltipText()+"<hr>"+
		"<br>" + I18n.text("speed") + ": <b>"+(int)speed+" "+I18n.text(speedUnits.getString())+"</b>"+
		"<br>" + I18n.text("radius") + ": <b>"+radius+" " + I18n.textc("m", "meters") + "</b>"+
		"<br>" + I18n.text("duration") + ": <b>"+duration+" " + I18n.textc("s", "seconds") + "</b><br>";
	}

	public LocationType getLocation() {
		return location;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = Math.max(MINIMUM_SK_RADIUS, radius);
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public SPEED_UNITS getSpeedUnits() {
		return speedUnits;
	}

	public void setSpeedUnits(SPEED_UNITS speedUnits) {
		this.speedUnits = speedUnits;
	}
	
//	protected double getVelocityInMetersPerSeconds() {
//		if (speedUnits == Maneuver.SPEED_UNITS.METERS_PS)
//			return speed;
//		if (speedUnits.equalsIgnoreCase("Km/h"))
//			return speed * (1.0/3.6);
//		if (speedUnits.equalsIgnoreCase("MPH"))
//			return speed * 0.4471;
//		
//		return 0;		
//	}
	
	
	@Override
	public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
    	super.paintOnMap(g2d, planElement, renderer);
    	AffineTransform at = g2d.getTransform();
    	// x marks the spot...
		g2d.drawLine(-4, -4, 4, 4);
		g2d.drawLine(-4, 4, 4, -4);
    	double radius = Math.max(MINIMUM_SK_RADIUS, this.getRadius()) * renderer.getZoom();
		g2d.setColor(new Color(255,255,255,100));
		g2d.fill(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
		g2d.setColor(Color.blue.darker());
		g2d.draw(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
    	g2d.setTransform(at);
	}

	@Override
	public void parseIMCMessage(IMCMessage message) {
		setMaxTime((int)message.getDouble("timeout"));
    	setSpeed(message.getDouble("speed"));
    	
    	ManeuverLocation pos = new ManeuverLocation();
    	pos.setLatitudeRads(message.getDouble("lat"));
    	pos.setLongitudeRads(message.getDouble("lon"));
    	pos.setZ(message.getDouble("z"));
    	String zunits = message.getString("z_units");
    	if (zunits != null)
    	    pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(zunits));
    	setManeuverLocation(pos);
    	
		try {
            String speedUnits = message.getString("speed_units");
            setSpeedUnits(Maneuver.SPEED_UNITS.parse(speedUnits));
        }
        catch (Exception e) {
            setSpeedUnits(Maneuver.SPEED_UNITS.RPM);
            e.printStackTrace();
        }
		
		setDuration((int)message.getDouble("duration"));
		setRadius(message.getDouble("radius"));
		setCustomSettings(message.getTupleList("custom"));
	}
	
	@Override
	public IMCMessage serializeToIMC() {
	    pt.lsts.imc.StationKeeping message = new pt.lsts.imc.StationKeeping();
		LocationType loc = getManeuverLocation();
		loc.convertToAbsoluteLatLonDepth();
		message.setLat(loc.getLatitudeRads());
		message.setLon(loc.getLongitudeRads());
		message.setZ(getManeuverLocation().getZ());
		message.setZUnits(pt.lsts.imc.StationKeeping.Z_UNITS.valueOf(getManeuverLocation().getZUnits().toString()));
		message.setDuration(getDuration());
		message.setSpeed(this.getSpeed());
		try {
            switch (this.getSpeedUnits()) {
                case METERS_PS:
                    message.setSpeedUnits(pt.lsts.imc.StationKeeping.SPEED_UNITS.METERS_PS);
                    break;
                case PERCENTAGE:
                    message.setSpeedUnits(pt.lsts.imc.StationKeeping.SPEED_UNITS.PERCENTAGE);
                    break;
                case RPM:
                default:
                    message.setSpeedUnits(pt.lsts.imc.StationKeeping.SPEED_UNITS.RPM);
                    break;
            }
        }
        catch (Exception ex) {
            NeptusLog.pub().error(this, ex);                     
        }
        
		message.setRadius(this.getRadius());
		message.setCustom(getCustomSettings());
        
		return message;
	}
	  
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.StatisticsProvider#getCompletionTime(pt.lsts.neptus.types.coord.LocationType)
     */
    @Override
    public double getCompletionTime(LocationType initialPosition) {
        double speed = this.speed;
        if (this.speedUnits == Maneuver.SPEED_UNITS.RPM) {
            speed = speed/769.230769231; //1.3 m/s for 1000 RPMs
        }
        else if (this.speedUnits == Maneuver.SPEED_UNITS.PERCENTAGE) {
            speed = speed/76.923076923; //1.3 m/s for 100% speed
        }

        double time = getDistanceTravelled(initialPosition) / speed;

        return /*getDuration() == 0 ? Double.POSITIVE_INFINITY :*/ getDuration() + time;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.StatisticsProvider#getDistanceTravelled(pt.lsts.neptus.types.coord.LocationType)
     */
    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = getStartLocation().getDistanceInMeters(initialPosition);
        return meters;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.StatisticsProvider#getMaxDepth()
     */
    @Override
    public double getMaxDepth() {
        return getManeuverLocation().getAllZ();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.StatisticsProvider#getMinDepth()
     */
    @Override
    public double getMinDepth() {
        return getManeuverLocation().getAllZ();
    }   
    
    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }

}
