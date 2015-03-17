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
 * Author: Paulo Dias
 * 2004/01/??
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Loiter.DIRECTION;
import pt.lsts.imc.Loiter.TYPE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.editor.AngleEditorRads;
import pt.lsts.neptus.gui.editor.ComboEditor;
import pt.lsts.neptus.gui.editor.SpeedUnitsEditor;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.renderer2d.LoiterPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

public class Loiter extends Maneuver implements LocatedManeuver, StatisticsProvider, IMCSerialization {

	public static final int INFINITY_DURATION = -1;
	
	private int loiterDuration = 60;
	private double radius = 15, radiusTolerance = 5, length = 1, bearing = 0,
			speed = 30, speedTolerance = 5;
	private String direction = "Clockwise";
	private String loiterType = "Circular";
	private String speedUnits = "m/s";
	private ManeuverLocation location = new ManeuverLocation();	 
	protected static final LinkedHashMap<Long, String> wpLoiterTypeConstantsMap = new LinkedHashMap<Long, String>();
	protected static final LinkedHashMap<Long, String> loiterDirectionConstantsMap = new LinkedHashMap<Long, String>();
    
	static {
	    wpLoiterTypeConstantsMap.put(1l, I18n.textmark("Circular"));
	    wpLoiterTypeConstantsMap.put(2l, I18n.textmark("Racetrack"));
	    wpLoiterTypeConstantsMap.put(3l, I18n.textmark("Figure 8"));
	    // wpLoiterTypeConstantsMap.put(4l, "Hover");
	    
	    loiterDirectionConstantsMap.put(0l, I18n.textmark("Vehicle Dependent"));
        loiterDirectionConstantsMap.put(1l, I18n.textmark("Clockwise"));
        loiterDirectionConstantsMap.put(2l, I18n.textmark("Counter-Clockwise"));
        loiterDirectionConstantsMap.put(3l, I18n.textmark("Into the Wind"));
	}

	@Override
	public Object clone() {
		Loiter l = new Loiter();
		super.clone(l);
		l.setBearing(getBearing());
		l.setDirection(getDirection());
		l.setLength(getLength());
		l.setLoiterDuration(getLoiterDuration());
		l.setLoiterType(getLoiterType());
		l.setManeuverLocation(getManeuverLocation());
		l.setRadius(getRadius());
		l.setRadiusTolerance(getRadiusTolerance());
		l.setSpeed(getSpeed());
		l.setSpeedTolerance(getSpeedTolerance());
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
	    radTolerance.setText(String.valueOf(getRadiusTolerance()));	   
	    basePoint.addAttribute("type", "pointType");
	    
	    //duration
	    root.addElement("duration").setText(""+getLoiterDuration());
	    
	    //trajectory
	    Element trajectory = root.addElement("trajectory");	    
	    Element trajRadius = trajectory.addElement("radius");
	    trajRadius.setText(String.valueOf(getRadius()));
	    trajRadius.addAttribute("type", "float");	    
	    Element trajRadiusTolerance = trajectory.addElement("radiusTolerance");
	    trajRadiusTolerance.setText(String.valueOf(getRadiusTolerance()));
	    trajRadiusTolerance.addAttribute("type", "float");	    
	    trajectory.addElement("type").setText(getLoiterType());	    
	    Element trajLength = trajectory.addElement("length");
	    trajLength.setText(String.valueOf(getLength()));
	    trajLength.addAttribute("type", "float");	    
	    Element trajBearing = trajectory.addElement("bearing");
	    trajBearing.setText(String.valueOf(getBearing()));
	    trajBearing.addAttribute("type", "float");
	    trajectory.addElement("direction").setText(getDirection());	    

	    //velocity
	    Element velocity = root.addElement("speed");
	    velocity.addAttribute("tolerance", String.valueOf(getSpeedTolerance()));
	    velocity.addAttribute("type", "float");
	    velocity.addAttribute("unit", getSpeedUnits());
	    velocity.setText(String.valueOf(getSpeed()));
	    
	    return document;
	}
	
	@Override
	public String getType() {
		return "Loiter";
	}

	@Override
	public void loadFromXML(String XML) {
		try {
	        Document doc = DocumentHelper.parseText(XML);
	        
	        // basePoint
	        Node node = doc.selectSingleNode("Loiter/basePoint/point");
	        ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);
	        setRadiusTolerance(Double.parseDouble(doc.selectSingleNode("Loiter/basePoint/radiusTolerance").getText()));
	        
	        // Velocity
	        Node speedNode = doc.selectSingleNode("Loiter/speed");
	        if (speedNode == null)
	        	speedNode = doc.selectSingleNode("Loiter/velocity");
	        setSpeed(Double.parseDouble(speedNode.getText()));
	        setSpeedUnits(speedNode.valueOf("@unit"));
	        setSpeedTolerance(Double.parseDouble(speedNode.valueOf("@tolerance")));
	        
	        // Duration
	        setLoiterDuration(Integer.parseInt(doc.selectSingleNode("Loiter/duration").getText()));
	        
	        // Trajectory
	        setRadius(Double.parseDouble(doc.selectSingleNode("Loiter/trajectory/radius").getText()));
	        setRadiusTolerance(Double.parseDouble(doc.selectSingleNode("Loiter/trajectory/radiusTolerance").getText()));
	        setLoiterType(doc.selectSingleNode("Loiter/trajectory/type").getText());
	        setDirection(doc.selectSingleNode("Loiter/trajectory/direction").getText());	        
	        setBearing(Double.parseDouble(doc.selectSingleNode("Loiter/trajectory/bearing").getText()));	        		    
	       
	        if (doc.selectSingleNode("Loiter/trajectory/length") != null)	        
	        	setLength(Double.parseDouble(doc.selectSingleNode("Loiter/trajectory/length").getText()));
	        
	        if (doc.selectSingleNode("Loiter/trajectory/lenght") != null)	        
	        	setLength(Double.parseDouble(doc.selectSingleNode("Loiter/trajectory/lenght").getText()));
	        
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
		
		DefaultProperty duration = PropertiesEditor.getPropertyInstance("Duration", Integer.class, this.loiterDuration, true);
		duration.setShortDescription("The Loiter's duration, in seconds (0 means +Infinity)");		
		props.add(duration);
		
		DefaultProperty direction = PropertiesEditor.getPropertyInstance("Direction", String.class, this.direction, true);
		direction.setShortDescription("The direction the vehicle should take when performing this maneuver");		
		PropertiesEditor.getPropertyEditorRegistry().registerEditor(direction, new ComboEditor<String>(loiterDirectionConstantsMap.values().toArray(new String[]{})));
		PropertiesEditor.getPropertyRendererRegistry().registerRenderer(direction, new I18nCellRenderer());
		props.add(direction);
		
		DefaultProperty type = PropertiesEditor.getPropertyInstance("Loiter Type", String.class, this.loiterType, true);
		type.setShortDescription("How to perform this maneuver. Note that some parameters only make sense in some Loiter types.");
		PropertiesEditor.getPropertyEditorRegistry().registerEditor(type, new ComboEditor<String>(wpLoiterTypeConstantsMap.values().toArray(new String[]{})));
		PropertiesEditor.getPropertyRendererRegistry().registerRenderer(type, new I18nCellRenderer());
		props.add(type);
		
		DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", Double.class, this.speed, true);
		speed.setShortDescription("The vehicle's desired speed when loitering");
		props.add(speed);
		
		DefaultProperty speedUnits = PropertiesEditor.getPropertyInstance("Speed Units", String.class, this.speedUnits, true);
		speedUnits.setShortDescription("The units to consider in the speed parameters");
		PropertiesEditor.getPropertyEditorRegistry().registerEditor(speedUnits, new SpeedUnitsEditor());
		PropertiesEditor.getPropertyRendererRegistry().registerRenderer(speedUnits, new I18nCellRenderer());
		props.add(speedUnits);
		
		DefaultProperty radius = PropertiesEditor.getPropertyInstance("Radius", Double.class, this.radius, true);
		radius.setShortDescription("If its not a hover loiter, sets the radius of the trajectory");
		props.add(radius);
		
		DefaultProperty length = PropertiesEditor.getPropertyInstance("Length", Double.class, this.length, true);
		length.setShortDescription("If its a 'racetrack' or 'figure8' loiter, sets the distance between the focuses");
		props.add(length);
		
		DefaultProperty bearing = PropertiesEditor.getPropertyInstance("Bearing", Double.class, this.bearing, true);
		bearing.setShortDescription("The angle to bear when loitering. 0 = Absolute North");
		PropertiesEditor.getPropertyEditorRegistry().registerEditor(bearing, AngleEditorRads.class);
		props.add(bearing);
		
		return props;
	}
	
	@Override
	public void setProperties(Property[] properties) {
		super.setProperties(properties);
	
		for (Property p : properties) {
			
			if (p.getName().equals("Duration")) {
				setLoiterDuration((Integer)p.getValue());
				continue;
			}
			
			if (p.getName().equals("Direction")) {				
				setDirection((String)p.getValue());
				continue;
			}
			
			if (p.getName().equals("Loiter Type")) {
				setLoiterType((String)p.getValue());
				continue;
			}
			
			if (p.getName().equals("Speed")) {
				setSpeed((Double)p.getValue());
				continue;
			}
			
			if (p.getName().equalsIgnoreCase("Speed Tolerance")) {
				setSpeedTolerance((Double)p.getValue());
				continue;
			}
			
			if (p.getName().equalsIgnoreCase("Speed Units")) {
				setSpeedUnits((String)p.getValue());
				continue;
			}
			
			if (p.getName().equals("Radius")) {
				setRadius((Double)p.getValue());
				continue;
			}
			
			if (p.getName().equalsIgnoreCase("Radius Tolerance")) {
				setRadiusTolerance((Double)p.getValue());
				continue;
			}
			
			if (p.getName().equals("Length")) {
				setLength((Double)p.getValue());
				continue;
			}
			
			if (p.getName().equals("Bearing")) {
				setBearing((Double)p.getValue());
				continue;
			}
		}
	}
	
	@Override
	public String getTooltipText() {
		return super.getTooltipText()+"<hr>"+
		I18n.text("loiter type") + ": <b>"+I18n.text(loiterType)+"</b>"+
		"<br>"+I18n.text(location.getZUnits().toString())+": <b>"+location.getZ()+" "+I18n.textc("m", "meters")+"</b>"+
		"<br>" + I18n.text("speed") + ": <b>"+(int)speed+" "+I18n.text(speedUnits)+"</b>"+
		"<br>" + I18n.text("radius") + ": <b>"+radius+" " + I18n.textc("m", "meters") + "</b>"+
		"<br>" + I18n.text("length") + ": <b>"+length+" " + I18n.textc("m", "meters") + "</b>"+
		"<br>" + I18n.text("direction") + ": <b>"+I18n.text(direction)+"</b>"+
		"<br>" + I18n.text("duration") + ": <b>"+loiterDuration+" " + I18n.textc("s", "seconds") + "</b><br>";
	}
	
	public double getBearing() {
		return bearing;
	}

	public void setBearing(double bearing) {
		this.bearing = bearing;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public LocationType getLocation() {
		return location;
	}

	public int getLoiterDuration() {
		return loiterDuration;
	}

	public void setLoiterDuration(int loiterDuration) {
		this.loiterDuration = loiterDuration;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getRadiusTolerance() {
		return radiusTolerance;
	}

	public void setRadiusTolerance(double radiusTolerance) {
		this.radiusTolerance = radiusTolerance;
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

	public String getSpeedUnits() {
		return speedUnits;
	}

	public void setSpeedUnits(String speedUnits) {
		this.speedUnits = speedUnits;
	}

	public void setLoiterType(String loiterType) {
		this.loiterType = loiterType;
	}
	
	public String getLoiterType() {
		return loiterType;
	}
	
	protected double getVelocityInMetersPerSeconds() {
		if (speedUnits.equals("m/s"))
			return speed;
		if (speedUnits.equalsIgnoreCase("Km/h"))
			return speed * (1.0/3.6);
		if (speedUnits.equalsIgnoreCase("MPH"))
			return speed * 0.4471;
		
		return 0;		
	}
	
	private static LoiterPainter painter = new LoiterPainter();
	
	@Override
	public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {

    	super.paintOnMap(g2d, planElement, renderer);		
			AffineTransform at = g2d.getTransform();
			painter.setLoiterManeuver(this);	
			painter.paint(g2d, renderer);
			g2d.setTransform(at);
		
	}

	@Override
	public void parseIMCMessage(IMCMessage message) {
		setMaxTime((int)message.getDouble("timeout"));
    	setSpeed(message.getDouble("speed"));
    	setLoiterDuration(message.getInteger("duration"));
        setRadiusTolerance(Double.isNaN(message.getDouble("radius_tolerance")) ? 2 : message
                .getDouble("radius_tolerance"));
    	
    	ManeuverLocation pos = new ManeuverLocation();
    	pos.setLatitudeRads(message.getDouble("lat"));
    	pos.setLongitudeRads(message.getDouble("lon"));
    	pos.setZ(message.getDouble("z"));
    	String zunits = message.getString("z_units");
    	if (zunits != null)
    	    pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(zunits));
    	setManeuverLocation(pos);
    	
    	String speed_units = message.getString("speed_units");
		if (speed_units.equals("METERS_PS"))
			setSpeedUnits("m/s");
		else if (speed_units.equals("RPM"))
			setSpeedUnits("RPM");
		else
			setSpeedUnits("%");
		
		setBearing(message.getDouble("bearing"));
		setLoiterDuration((int)message.getDouble("duration"));
		setLength(message.getDouble("length"));
		setRadius(message.getDouble("radius"));
		
    	String type = message.getString("type");
		if (type.equals("EIGHT"))
			setLoiterType("Figure 8");
		else
			setLoiterType(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase());
		
		String direction = message.getString("direction");
		if (direction.equals("VDEP"))
			setDirection("Vehicle Dependent");
		else if (direction.equals("CLOCKW"))
			setDirection("Clockwise");
		else if (direction.equals("CCLOCKW"))
			setDirection("Counter Clockwise");
		else if (direction.equals("IWINDCURR"))
			setDirection("Into the wind");
		
		setCustomSettings(message.getTupleList("custom"));
	}
	
	@Override
	public IMCMessage serializeToIMC() {
	    
	    pt.lsts.imc.Loiter loiter = new pt.lsts.imc.Loiter();
        loiter.setTimeout(this.getMaxTime());
        
        LocationType loc = getManeuverLocation();
        loc.convertToAbsoluteLatLonDepth();
        
        loiter.setLat(loc.getLatitudeRads());
        loiter.setLon(loc.getLongitudeRads());
        loiter.setZ(getManeuverLocation().getZ());
        loiter.setZUnits(pt.lsts.imc.Loiter.Z_UNITS.valueOf(getManeuverLocation().getZUnits().name()));
        loiter.setSpeed(this.getSpeed());
        loiter.setDuration(getLoiterDuration());
       
        switch (this.getSpeedUnits()) {
            case "m/s":
                loiter.setSpeedUnits(pt.lsts.imc.Loiter.SPEED_UNITS.METERS_PS);
                break;
            case "RPM":
                loiter.setSpeedUnits(pt.lsts.imc.Loiter.SPEED_UNITS.RPM);
                break;
            case "PERCENTAGE":
                loiter.setSpeedUnits(pt.lsts.imc.Loiter.SPEED_UNITS.PERCENTAGE);
                break;
            default:
                break;
        }
        
        

        String loiterType = this.getLoiterType();
		try {
			if ("Default".equalsIgnoreCase(loiterType))
				loiter.setType(TYPE.DEFAULT);
			else if ("Circular".equalsIgnoreCase(loiterType))
			    loiter.setType(TYPE.CIRCULAR);
			else if ("Racetrack".equalsIgnoreCase(loiterType))
			    loiter.setType(TYPE.RACETRACK);
			else if ("Figure 8".equalsIgnoreCase(loiterType))
			    loiter.setType(TYPE.EIGHT);
			else if ("Hover".equalsIgnoreCase(loiterType))
			    loiter.setType(TYPE.HOVER);
		} catch (Exception ex) {
			NeptusLog.pub().error(this, ex);
		}
		
		loiter.setRadius(getRadius());
		loiter.setLength(getLength());
		loiter.setBearing(getBearing());
		
		String lDirection = this.getDirection();

		try {
			if ("Vehicle Dependent".equalsIgnoreCase(lDirection))
			    loiter.setDirection(DIRECTION.VDEP);
			else if ("Clockwise".equalsIgnoreCase(lDirection))
			    loiter.setDirection(DIRECTION.CLOCKW);
			else if ("Counter Clockwise".equalsIgnoreCase(lDirection))
			    loiter.setDirection(DIRECTION.CCLOCKW);
			else if ("Counter-Clockwise".equalsIgnoreCase(lDirection))
			    loiter.setDirection(DIRECTION.CCLOCKW);
			else if (lDirection.startsWith("Into the wind"))
			    loiter.setDirection(DIRECTION.IWINDCURR);
		} catch (Exception ex) {
			NeptusLog.pub().error(this, ex);
		}

        loiter.setCustom(getCustomSettings());

		return loiter;
	}

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        double speed = this.speed;
        if (this.speedUnits.equalsIgnoreCase("RPM")) {
            speed = speed/769.230769231; //1.3 m/s for 1000 RPMs
        }
        else if (this.speedUnits.equalsIgnoreCase("%")) {
            speed = speed/76.923076923; //1.3 m/s for 100% speed
        }

        double time = getDistanceTravelled(initialPosition) / speed;

        return /*getLoiterDuration() == 0 ? Double.POSITIVE_INFINITY :*/ getLoiterDuration() + time;
    }

    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = getStartLocation().getDistanceInMeters(initialPosition);
        return meters;
    }

    @Override
    public double getMaxDepth() {
        return getManeuverLocation().getAllZ();
    }

    @Override
    public double getMinDepth() {
        return getManeuverLocation().getAllZ();
    }   
    
    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }

}
