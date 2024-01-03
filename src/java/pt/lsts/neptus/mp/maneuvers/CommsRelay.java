/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2011/05/30
 */
package pt.lsts.neptus.mp.maneuvers;

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
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class CommsRelay extends DefaultManeuver implements IMCSerialization, LocatedManeuver, ManeuverWithSpeed {

    SpeedType speed = new SpeedType(); 
    private int duration = 60;
    private String sys_a = "", sys_b = "";
    private ManeuverLocation startLoc = new ManeuverLocation();
    private double move_threshold = 30;
    
    protected static final String DEFAULT_ROOT_ELEMENT = "CommsRelay";
	
    public CommsRelay() {
    }

	public String getType() {
		return DEFAULT_ROOT_ELEMENT;
	}
	
	public Document getManeuverAsDocument(String rootElementName) {
        
	    Document document = DocumentHelper.createDocument();
	    Element root = document.addElement( rootElementName );
	    root.addAttribute("kind", "automatic");

        Element system_a = root.addElement("sys_a");
        system_a.setText(String.valueOf(getSystemA()));

        Element system_b = root.addElement("sys_b");
        system_b.setText(String.valueOf(getSystemB()));
        Element duration = root.addElement("duration");
	    duration.setText(String.valueOf(getDuration()));
	    
	    Element finalPoint = root.addElement("startPoint");
        finalPoint.addAttribute("type", "pointType");
        Element point = getManeuverLocation().asElement("point");
        finalPoint.add(point);
	   
	    Element move_thresh = root.addElement("move_threshold");
	    move_thresh.setText(String.valueOf(getMoveThreshold()));
       
	    SpeedType.addSpeedElement(root, this);
	    return document;
    }
	
	public void loadManeuverFromXML(String xml) {
	    try {
	        Document doc = DocumentHelper.parseText(xml);
	        
	        Node node = doc.selectSingleNode("CommsRelay/startPoint/point");
            getManeuverLocation().load(node.asXML());
            
	        setSystemA(doc.selectSingleNode("//sys_a").getText());
	        setSystemB(doc.selectSingleNode("//sys_b").getText());
            setDuration(Integer.parseInt(doc.selectSingleNode("//duration").getText()));
            
            setMoveThreshold(Double.parseDouble(doc.selectSingleNode("//move_threshold").getText()));
            
	        SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
	    }
	    catch (Exception e) {
	        NeptusLog.pub().error(this, e);
	        return;
	    }
    }
	
    public String getSystemA() {
        return sys_a;
    }
    
    public void setSystemA(String system) {
        this.sys_a = system;
    }
    
    public String getSystemB() {
        return sys_b;
    }
    
    public void setSystemB(String system) {
        this.sys_b = system;
    }

    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }

	/**
     * @return the move_threshold
     */
    public double getMoveThreshold() {
        return move_threshold;
    }

    /**
     * @param move_threshold the move_threshold to set
     */
    public void setMoveThreshold(double move_threshold) {
        this.move_threshold = move_threshold;
    }

    public Object clone() {  
	    CommsRelay clone = new CommsRelay();
	    super.clone(clone);
	    //clone.params = params;
        clone.setSystemA(getSystemA());
        clone.setSystemB(getSystemB());
	    clone.setDuration(getDuration());
	    clone.setSpeed(getSpeed());
		clone.setManeuverLocation(getStartLocation());
		clone.setMoveThreshold(getMoveThreshold());
		return clone;
	}

    
    @Override
    protected Vector<DefaultProperty> additionalProperties() {
    	Vector<DefaultProperty> properties = new Vector<DefaultProperty>();

    	// @FIXME
    	properties.add(PropertiesEditor.getPropertyInstance("System A", String.class, getSystemA(), true));
    	properties.add(PropertiesEditor.getPropertyInstance("System B", String.class, getSystemB(), true));
    	
    	DefaultProperty durProp = PropertiesEditor.getPropertyInstance("Duration", Integer.class, getDuration(), true);
    	durProp.setShortDescription("(s)");
    	properties.add(durProp);
    	
    	DefaultProperty mvProp = PropertiesEditor.getPropertyInstance("Move threshold", Double.class, getMoveThreshold(), true);
    	mvProp.setShortDescription("(m)");
    	properties.add(mvProp);

    	properties.add(PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, getSpeed(), true));
    
    	return properties;
    }
    
    public String getPropertiesDialogTitle() {    
        return getId() + " parameters";
    }
    
    public void setProperties(Property[] properties) {
    	super.setProperties(properties);
    	
    	for (Property p : properties) {
    		if (p.getName().equals("Speed")) {
    			setSpeed((SpeedType)p.getValue());
    		}
    		else if (p.getName().equals("Duration")) {
    			setDuration((Integer)p.getValue());
    		}
    		else if (p.getName().equals("System A")) {
    			setSystemA((String)p.getValue());
    		}
    		else if (p.getName().equals("System B")) {
                setSystemB((String)p.getValue());
            }
    		else if (p.getName().equals("Move threshold")) {
                setMoveThreshold((Double)p.getValue());
            }           
    	}
    }
    
	public String[] getPropertiesErrors(Property[] properties) {
		return super.getPropertiesErrors(properties);
	}

	@Override
	public String getTooltipText() {
		return super.getTooltipText()+"<hr>"+
		        I18n.text("speed") + ": <b>"+getSpeed()+"</b>"+
        		"<br>" + I18n.text("duration") + ": <b>"+(int)getDuration()+" " + I18n.textc("s", "seconds") + "</b>" +
        		"<br>" + I18n.text("system a") + ": <b>"+getSystemA()+"</b>" +
        		"<br>" + I18n.text("system b") + ": <b>"+getSystemB()+"</b>";
	}
	
	@Override
	public void parseIMCMessage(IMCMessage message) {
		setMaxTime((int)message.getDouble("timeout"));
        //setSystem(message.getAsString("system"));
		setSystemA(IMCUtils.translateImcIdToSystem((int)message.getDouble("sys_a")));
		setSystemB(IMCUtils.translateImcIdToSystem((int)message.getDouble("sys_b")));
		setDuration((int)message.getDouble("duration"));
		startLoc.convertToAbsoluteLatLonDepth();
		startLoc.setLatitudeRads(message.getDouble("lat"));
		startLoc.setLongitudeRads(message.getDouble("lon"));
		startLoc.setZ(0);
		startLoc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
		setMoveThreshold(message.getDouble("move_threshold"));
		switch (message.getString("speed_units").toUpperCase()) {
            case "RPM":
                setSpeed(new SpeedType(message.getDouble("speed"), Units.RPM));
                break;
            case "PERCENTAGE":
                setSpeed(new SpeedType(message.getDouble("speed"), Units.Percentage));
                break;
            default:
                setSpeed(new SpeedType(message.getDouble("speed"), Units.MPS));
                break;            
        }
		
		setMoveThreshold(message.getDouble("move_threshold"));
	}

    public IMCMessage serializeToIMC() {
        
        pt.lsts.imc.CommsRelay msg = new pt.lsts.imc.CommsRelay();
        
        msg.setDuration(getDuration());
        
        int sys_a = -1, sys_b = -1;
        
        try {
            sys_a = Integer.parseInt(getSystemA());
        }
        catch (Exception e) {}
        
        try {
            sys_b = Integer.parseInt(getSystemB());
        }
        catch (Exception e) {}
        
        if (getSystemA().equalsIgnoreCase("me") || getSystemA().equalsIgnoreCase("home") || getSystemA().equalsIgnoreCase("base"))
            sys_a = ImcMsgManager.getManager().getLocalId().intValue();
        
        if (getSystemB().equalsIgnoreCase("me") || getSystemB().equalsIgnoreCase("home") || getSystemB().equalsIgnoreCase("base"))
            sys_b = ImcMsgManager.getManager().getLocalId().intValue();
        
        if (sys_a == -1)
            sys_a = IMCUtils.translateSystemToImcId(this.getSystemA()).intValue();
        
        if (sys_b == -1)
            sys_b = IMCUtils.translateSystemToImcId(this.getSystemB()).intValue();
        
        msg.setSysA(sys_a);       
        msg.setSysB(sys_b);
                
        startLoc.convertToAbsoluteLatLonDepth();
        msg.setLat(startLoc.getLatitudeRads());
        msg.setLon(startLoc.getLongitudeRads());

        speed.setSpeedToMessage(msg);
		
		msg.setMoveThreshold(getMoveThreshold());
		
		return msg;
	}
    
    @Override
    public ManeuverLocation getStartLocation() {
        return getManeuverLocation();
    }
    
    @Override
    public ManeuverLocation getEndLocation() {
        return getManeuverLocation();
    }
    
    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }
    

    @Override
    public ManeuverLocation getManeuverLocation() {
        return startLoc;
    }
    
    @Override
    public void setManeuverLocation(ManeuverLocation location) {
       this.startLoc = location.clone();
        
    }
    
    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        startLoc.translatePosition(offsetNorth, offsetEast, offsetDown);
    }
    
    public static void main(String[] args) {
        CommsRelay cr = new CommsRelay();
        cr.getManeuverLocation().setLocation(new LocationType(41, -8));
        cr.setDuration(3600);
        cr.setSystemA("lauv-xtreme-2");
        cr.setSystemB("lauv-seacon-1");
        cr.setSpeed(new SpeedType(1200, Units.RPM));
        cr.setMoveThreshold(30);
        
        IMCMessage msg = cr.serializeToIMC();
        
        NeptusLog.pub().info("<###> "+cr.asXML());
        cr.serializeToIMC().dump(System.out);
        msg.dump(System.out);
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
