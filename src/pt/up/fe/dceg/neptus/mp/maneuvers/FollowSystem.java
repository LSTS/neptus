/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 2011/05/30
 * $Id:: FollowSystem.java 9913 2013-02-11 19:11:17Z pdias                $:
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
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;
import pt.up.fe.dceg.neptus.util.NameNormalizer;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author pdias
 *
 */
public class FollowSystem extends DefaultManeuver implements IMCSerialization {

	/**
	 * 
	 */
	public FollowSystem() {
		// TODO Auto-generated constructor stub
	}

    private double speed = 1000, speedTolerance = 100;
    private String units = "RPM";
    private int duration = 60;
    private String system = "";
    private double xOffset = 1, yOffset = 1, zOffset = 1;
    
    protected static final String DEFAULT_ROOT_ELEMENT = "FollowSystem";
    
	
	public String id = NameNormalizer.getRandomID();
	
	public String getType() {
		return DEFAULT_ROOT_ELEMENT;
	}
	
	public Document getManeuverAsDocument(String rootElementName) {
        
	    Document document = DocumentHelper.createDocument();
	    Element root = document.addElement( rootElementName );
	    root.addAttribute("kind", "automatic");

        Element system = root.addElement("system");
        system.setText(String.valueOf(getSystem()));

        Element duration = root.addElement("duration");
	    duration.setText(String.valueOf(getDuration()));
	   
	    //offsets 
	    Element offsets = root.addElement("offsets");
	    offsets.addAttribute("xOffset", Double.toString(getXOffset()));
	    offsets.addAttribute("yOffset", Double.toString(getYOffset()));
	    offsets.addAttribute("zOffset", Double.toString(getZOffset()));
        
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
	        setSystem(doc.selectSingleNode("//system").getText());
            setDuration(Integer.parseInt(doc.selectSingleNode("//duration").getText()));
	        
            Node offsetNode = doc.selectSingleNode("//offsets");
            setXOffset(Double.parseDouble(offsetNode.valueOf("@xOffset")));
            setYOffset(Double.parseDouble(offsetNode.valueOf("@yOffset")));
            setZOffset(Double.parseDouble(offsetNode.valueOf("@zOffset")));
            
            Node speedNode = doc.selectSingleNode("//speed");
	        if (speedNode == null) 
	        	speedNode = doc.selectSingleNode("//velocity");

	        setSpeed(Double.parseDouble(speedNode.getText()));
	        setUnits(speedNode.valueOf("@unit"));
	        setSpeedTolerance(Double.parseDouble(speedNode.valueOf("@tolerance")));
	        
	    }
	    catch (Exception e) {
	        NeptusLog.pub().error(this, e);
	        return;
	    }
    }
	
	public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
		return lastVehicleState;
	}

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSystem() {
        return system;
    }
    
    public void setSystem(String system) {
        this.system = system;
    }

    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * @return the xOffset
     */
    public double getXOffset() {
        return xOffset;
    }

    /**
     * @param xOffset the xOffset to set
     */
    public void setXOffset(double xOffset) {
        this.xOffset = xOffset;
    }

    /**
     * @return the yOffset
     */
    public double getYOffset() {
        return yOffset;
    }

    /**
     * @param yOffset the yOffset to set
     */
    public void setYOffset(double yOffset) {
        this.yOffset = yOffset;
    }

    /**
     * @return the zOffset
     */
    public double getZOffset() {
        return zOffset;
    }

    /**
     * @param zOffset the zOffset to set
     */
    public void setZOffset(double zOffset) {
        this.zOffset = zOffset;
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

	public Object clone() {  
	    FollowSystem clone = new FollowSystem();
	    super.clone(clone);
	    //clone.params = params;
        clone.setSystem(getSystem());
	    clone.setDuration(getDuration());
        clone.setXOffset(getXOffset());
        clone.setYOffset(getYOffset());
        clone.setZOffset(getZOffset());
	    clone.setUnits(getUnits());
	    clone.setSpeed(getSpeed());
	    clone.setSpeedTolerance(getSpeedTolerance());
		
	    return clone;
	}

    
    @Override
    protected Vector<DefaultProperty> additionalProperties() {
    	Vector<DefaultProperty> properties = new Vector<DefaultProperty>();

    	// @FIXME
    	properties.add(PropertiesEditor.getPropertyInstance("System", String.class, getSystem(), true));
    	
    	properties.add(PropertiesEditor.getPropertyInstance("Duration", Integer.class, getDuration(), true));

        properties.add(PropertiesEditor.getPropertyInstance("x offset", Double.class, getXOffset(), true));
        properties.add(PropertiesEditor.getPropertyInstance("y offset", Double.class, getYOffset(), true));
        properties.add(PropertiesEditor.getPropertyInstance("z offset", Double.class, getZOffset(), true));

    	DefaultProperty units = PropertiesEditor.getPropertyInstance("Speed units", String.class, getUnits(), true);
    	units.setShortDescription("The speed units");
    	PropertiesEditor.getPropertyEditorRegistry().registerEditor(units, new ComboEditor<String>(new String[] {"RPM", "m/s", "%"}));    	
    
    	properties.add(PropertiesEditor.getPropertyInstance("Speed", Double.class, getSpeed(), true));
    	properties.add(units);

    	properties.add(PropertiesEditor.getPropertyInstance("Speed tolerance", Double.class, getSpeedTolerance(), true));
    	
    	return properties;
    }
    
    
    public String getPropertiesDialogTitle() {    
        return getId() + " parameters";
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
    		if (p.getName().equals("Duration")) {
    			setDuration((Integer)p.getValue());
    		}
    		if (p.getName().equals("System")) {
    			setSystem((String)p.getValue());
    		}
            if (p.getName().equals("x offset")) {
                setXOffset((Double)p.getValue());
            }
            if (p.getName().equals("y offset")) {
                setYOffset((Double)p.getValue());
            }
            if (p.getName().equals("z offset")) {
                setZOffset((Double)p.getValue());
            }
    	}
    }
    
	public String[] getPropertiesErrors(Property[] properties) {
		return super.getPropertiesErrors(properties);
	}

	
	@Override
	public String getTooltipText() {
	
		return super.getTooltipText()+"<hr>"+
		"speed: <b>"+getSpeed()+" "+getUnits()+"</b>"+
		"<br>duration: <b>"+(int)getDuration()+" s</b>" +
		"<br>system: <b>"+getSystem()+"</b>" +
		"<br>system: <b>nOff=</b>'" + MathMiscUtils.round(getXOffset(), 1) +
		"' <b>eOff=</b>'" + MathMiscUtils.round(getYOffset(), 1) +
		"' <b>dOff=</b>'" + MathMiscUtils.round(getZOffset(), 1) +
		"'";
	}
	
	@Override
	public void parseIMCMessage(IMCMessage message) {
		setMaxTime((int)message.getDouble("timeout"));
        //setSystem(message.getAsString("system"));
		setSystem(IMCUtils.translateImcIdToSystem((int)message.getDouble("system")));
		setDuration((int)message.getDouble("duration"));

		setXOffset(message.getDouble("x"));
		setYOffset(message.getDouble("y"));
		setZOffset(message.getDouble("z"));
		
		setSpeed(message.getDouble("speed"));
		String speed_units = message.getString("speed_units");
		if (speed_units.equals("METERS_PS"))
			setUnits("m/s");
		else if (speed_units.equals("RPM"))
			setUnits("RPM");
		else
			setUnits("%");
	}

    public IMCMessage serializeToIMC() {
        IMCMessage msgManeuver = IMCDefinition.getInstance().create(
                DEFAULT_ROOT_ELEMENT);
		msgManeuver.setValue("timeout", this.getMaxTime());
	
        //msgManeuver.setValue("system", this.getSystem());
		msgManeuver.setValue("system", IMCUtils.translateSystemToImcId(this.getSystem()));

        msgManeuver.setValue("duration", getDuration());
		
        msgManeuver.setValue("x", this.getXOffset());
        msgManeuver.setValue("y", this.getYOffset());
        msgManeuver.setValue("z", this.getZOffset());
        
		//msgManeuver.setValue("velocity", new NativeFLOAT(this.getSpeed()));
		msgManeuver.setValue("speed",this.getSpeed());
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
		//msgManeuver.setValue("velocity_units", enumerated);
		msgManeuver.setValue("speed_units", enumerated);

		
		//NativeTupleList ntl = new NativeTupleList();
        //FIXME commented line above for translation to new Message TupleList format
        LinkedHashMap<String, Object> tl = new LinkedHashMap<String, Object>();
        
		for (String key : getCustomSettings().keySet())
            tl.put(key, getCustomSettings().get(key));
        msgManeuver.setValue("custom",IMCMessage.encodeTupleList(tl));
		
		return msgManeuver;
	}
}
