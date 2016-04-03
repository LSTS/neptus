/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * 2011/05/30
 */
package pt.lsts.neptus.mp.maneuvers;

import java.util.LinkedHashMap;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.editor.SpeedUnitsEditor;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 *
 */
public class FollowSystem extends DefaultManeuver implements IMCSerialization {

    private double speed = 1000, speedTolerance = 100;
    private String units = "RPM";
    private int duration = 60;
    private String system = "";
    private double xOffset = 1, yOffset = 1, zOffset = 1;
    
    protected static final String DEFAULT_ROOT_ELEMENT = "FollowSystem";
	
    public FollowSystem() {
    }

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
    	PropertiesEditor.getPropertyEditorRegistry().registerEditor(units, new SpeedUnitsEditor());  
    	PropertiesEditor.getPropertyRendererRegistry().registerRenderer(units, new I18nCellRenderer());
    
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
		"speed" + ": <b>"+getSpeed()+" "+I18n.text(getUnits())+"</b>"+
		"<br>" + I18n.text("duration") + ": <b>"+(int)getDuration()+" " + I18n.textc("s", "seconds") + "</b>" +
		"<br>" + I18n.text("system") + ": <b>"+getSystem()+"</b>" +
		"<br>" + I18n.text("system") + ": <b>" + I18n.textc("nOff", "north offset") + "=</b>'" + MathMiscUtils.round(getXOffset(), 1) +
		"' <b>" + I18n.textc("eOff", "east offset") + "=</b>'" + MathMiscUtils.round(getYOffset(), 1) +
		"' <b>" + I18n.textc("dOff", "down offset") + "=</b>'" + MathMiscUtils.round(getZOffset(), 1) +
		" (" + I18n.textc("m", "meters") + ")'";
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
