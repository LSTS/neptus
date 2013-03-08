/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias e ZePinto
 * 2007/07/08
 * $Id:: PopUp.java 9913 2013-02-11 19:11:17Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import java.util.LinkedHashMap;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.editor.ComboEditor;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.util.NameNormalizer;

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
	
	private final int ANGLE_CALCULATION = -1 ;
	private final int FIRST_ROTATE = 0 ;
	private final int HORIZONTAL_MOVE = 1 ;
	int current_state = ANGLE_CALCULATION;
	
	private double targetAngle, rotateIncrement;
	
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
	        setUnits(speedNode.valueOf("@unit"));
	        setSpeedTolerance(Double.parseDouble(speedNode.valueOf("@tolerance")));
	        
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
	    clone.setUnits(getUnits());
	    clone.setSpeed(getSpeed());
	    clone.setSpeedTolerance(getSpeedTolerance());
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
    
    public void setUnits(String units) {
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
    	PropertiesEditor.getPropertyEditorRegistry().registerEditor(units, new ComboEditor<String>(new String[] {"RPM", "m/s", "%"}));    	
    
    	properties.add(PropertiesEditor.getPropertyInstance("Speed", Double.class, getSpeed(), true));
    	properties.add(units);

    	properties.add(PropertiesEditor.getPropertyInstance("Speed tolerance", Double.class, getSpeedTolerance(), true));

    	properties.add(PropertiesEditor.getPropertyInstance("Duration", Integer.class, getDuration(), true));
    	
    	return properties;
    }
    
    
    public String getPropertiesDialogTitle() {    
    	return getId()+" parameters";
    }
    
    public void setProperties(Property[] properties) {
    	
    	super.setProperties(properties);
    	
    	for (Property p : properties) {
    		if (p.getName().equals("Speed units")) {
    			setUnits((String)p.getValue());
    		}
    		if (p.getName().equals("Speed tolerance")) {
    			setSpeedTolerance((Double)p.getValue());
    		}
    		if (p.getName().equals("Speed")) {
    			setSpeed((Double)p.getValue());
    		}
    		if (p.getName().equals("Radius tolerance")) {
    			setRadiusTolerance((Double)p.getValue());
    		}
    		if (p.getName().equals("Duration")) {
    			setDuration((Integer)p.getValue());
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
	
		return super.getTooltipText()+"<hr>"+
		"speed: <b>"+getSpeed()+" "+getUnits()+"</b>"+
		"<br>cruise depth: <b>"+(int)destination.getDepth()+" m</b>"+
		"<br>duration: <b>"+getDuration()+" s</b>";
	}

	
	@Override
	public void parseIMCMessage(IMCMessage message) {
	
		setMaxTime((int)message.getDouble("timeout"));
    	setSpeed(message.getDouble("speed"));

        setRadiusTolerance(Double.isNaN(message.getDouble("radius_tolerance")) ? 2 : message
                .getDouble("radius_tolerance"));
    	
    	ManeuverLocation pos = new ManeuverLocation();
    	pos.setLatitude(Math.toDegrees(message.getDouble("lat")));
    	pos.setLongitude(Math.toDegrees(message.getDouble("lon")));
    	pos.setZ(message.getDouble("z"));
        pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(message.getString("z_units").toString()));
    	setManeuverLocation(pos);
    	
    	String speed_units = message.getString("speed_units");
		if (speed_units.equals("METERS_PS"))
			setUnits("m/s");
		else if (speed_units.equals("RPM"))
			setUnits("RPM");
		else
			setUnits("%");
		
	}
	
	public IMCMessage serializeToIMC()
	{
		IMCMessage msgManeuver = IMCDefinition.getInstance().create(
				"Popup");
		msgManeuver.setValue("timeout", this.getMaxTime());

		double[] latLonDepth = this.getManeuverLocation().getAbsoluteLatLonDepth();

		msgManeuver.setValue("lat", Math.toRadians(latLonDepth[0]));
		msgManeuver.setValue("lon", Math.toRadians(latLonDepth[1]));
		msgManeuver.setValue("depth",latLonDepth[2]);

		msgManeuver.setValue("duration", getDuration());

		msgManeuver.setValue("velocity",this.getSpeed());
		msgManeuver.setValue("speed", this.getSpeed());
        String enumerated = "";
		String speedU = this.getUnits();
		try {
			if ("m/s".equalsIgnoreCase(speedU))
                enumerated= "METERS_PS";
			else if ("RPM".equalsIgnoreCase(speedU))
                enumerated= "RPM";
			else if ("%".equalsIgnoreCase(speedU))
                enumerated= "PERCENTAGE";
			else if ("percentage".equalsIgnoreCase(speedU))
                enumerated= "PERCENTAGE";
		}
		catch (Exception ex) {
			NeptusLog.pub().error(this, ex);						
		}
		msgManeuver.setValue("speed_units", enumerated);

//		msgManeuver.setValue("velocity", new NativeFLOAT(this.getSpeed()));
//		Enumerated enumerated = (Enumerated) msgManeuver.getValueAsObject("velocity_units");
//		String velU = this.getUnits();
//		try {
//			if ("m/s".equalsIgnoreCase(velU))
//				enumerated.setCurrentValue("METERS_PS");
//			else if ("RPM".equalsIgnoreCase(velU))
//				enumerated.setCurrentValue("RPM");
//			else if ("%".equalsIgnoreCase(velU))
//				enumerated.setCurrentValue("PERCENTAGE");
//			else if ("percentage".equalsIgnoreCase(velU))
//				enumerated.setCurrentValue("PERCENTAGE");
//		}
//		catch (Exception ex) {
//			NeptusLog.pub().error(this, ex);						
//		}
//		msgManeuver.setValue("velocity_units", enumerated);
		
		msgManeuver.setValue("radius_tolerance", this.getRadiusTolerance());
		
        //NativeTupleList ntl = new NativeTupleList();
        // TupleList tl = new TupleList();
        //FIXME commented line above for translation to new Message TupleList format
        LinkedHashMap<String, Object> tl = new LinkedHashMap<String, Object>();
        
		for (String key : getCustomSettings().keySet())
            tl.put(key, getCustomSettings().get(key));
        msgManeuver.setValue("custom",IMCMessage.encodeTupleList(tl));


		return msgManeuver;
	}
}
