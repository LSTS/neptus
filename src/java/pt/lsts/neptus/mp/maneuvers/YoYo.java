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
 * Author: Paulo Dias
 * 2010/06/05
 */
package pt.lsts.neptus.mp.maneuvers;

import java.text.NumberFormat;
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
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.editor.AngleEditorRads;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Paulo Dias
 */
public class YoYo extends Maneuver implements IMCSerialization, LocatedManeuver, ManeuverWithSpeed {

    protected double amplitude = 2;
    protected float pitchAngle = (float) (Math.PI/4);
    protected ManeuverLocation destination = new ManeuverLocation();
    protected static final String DEFAULT_ROOT_ELEMENT = "YoYo";
	protected SpeedType speed = new SpeedType(1000, Units.RPM);
	
	public String getType() {
		return "YoYo";
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
	    radTolerance.setText("0");
	   
	    SpeedType.addSpeedElement(root, this);
	    Element amplitude = root.addElement("amplitude");
	    amplitude.setText(String.valueOf(getAmplitude()));	    
	    Element pitchAngle = root.addElement("pitch");
	    pitchAngle.setText(String.valueOf(getPitchAngle()));

	    return document;
    }
	
	public void loadManeuverFromXML(String xml) {
	    try {
	        Document doc = DocumentHelper.parseText(xml);
	        Node node = doc.selectSingleNode("YoYo/finalPoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc); 
	        setAmplitude(Double.parseDouble(doc.selectSingleNode("YoYo/amplitude").getText()));
	        setPitchAngle(Float.parseFloat(doc.selectSingleNode("YoYo/pitch").getText()));
	        SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
	    }
	    catch (Exception e) {
	        NeptusLog.pub().error(this, e);
	        return;
	    }
    }
	
	public Object clone() {  
	    YoYo clone = new YoYo();
	    super.clone(clone);
	    clone.setManeuverLocation(destination.clone());
	    clone.setAmplitude(getAmplitude());
	    clone.setPitchAngle(getPitchAngle());
	    clone.setSpeed(getSpeed());
	    
	    return clone;
	}

    public double getAmplitude() {
        return amplitude;
    }
    
    public void setAmplitude(double amplitude) {
        this.amplitude = amplitude;
    }
    
    /**
	 * @return the pitchAngle
	 */
	public float getPitchAngle() {
		return pitchAngle;
	}
	
	/**
	 * @param pitchAngle the pitchAngle to set
	 */
	public void setPitchAngle(float pitchAngle) {
		this.pitchAngle = pitchAngle;
	}
    
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {    
    	destination.translatePosition(offsetNorth, offsetEast, offsetDown);
    }
    
    @Override
    protected Vector<DefaultProperty> additionalProperties() {
    	Vector<DefaultProperty> properties = new Vector<DefaultProperty>();
    	properties.add(PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, getSpeed(), true));
    	DefaultProperty ampProp = PropertiesEditor.getPropertyInstance("Amplitude", Double.class, getAmplitude(), true);
    	ampProp.setShortDescription("(m)");
        properties.add(ampProp);
        DefaultProperty ap = PropertiesEditor.getPropertyInstance("Pitch angle", Float.class, getPitchAngle(), true);
        // ap.setShortDescription("(\u00B0)");
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(ap, AngleEditorRads.class);
    	properties.add(ap);
    	
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
    		else if (p.getName().equalsIgnoreCase("Amplitude")) {
    			setAmplitude((Double)p.getValue());
    		}    		
    		else if (p.getName().equalsIgnoreCase("Pitch angle")) {
    			setPitchAngle((Float)p.getValue());
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
		
    	NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
		
		return super.getTooltipText()+"<hr>"+
		I18n.text("speed") + ": <b>"+getSpeed()+"</b>"+
		"<br>"+I18n.text(destination.getZUnits().toString())+": <b>"+nf.format(destination.getZ())+" " + I18n.textc("m", "meters") + "</b>" +
		"<br>" + I18n.text("amplitude") + ": <b>"+nf.format(getAmplitude())+" " + I18n.textc("m", "meters") + "</b>"+
		"<br>" + I18n.text("pitch") + ": <b>"+nf.format(Math.toDegrees(getPitchAngle()))+" \u00B0</b>";
	}
    
    @Override
    public void parseIMCMessage(IMCMessage message) {
    	setMaxTime((int)message.getDouble("timeout"));
    	setAmplitude(message.getDouble("amplitude"));
    	setPitchAngle((float)message.getDouble("pitch"));
    	
    	ManeuverLocation pos = new ManeuverLocation();
    	pos.setLatitudeRads(message.getDouble("lat"));
    	pos.setLongitudeRads(message.getDouble("lon"));
    	pos.setZ(message.getDouble("z"));
    	pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(message.getString("z_units")));
    	
    	speed = SpeedType.parseImcSpeed(message);
    	
    	setManeuverLocation(pos);
    	
		
		setCustomSettings(message.getTupleList("custom"));
    }
    
    
	public IMCMessage serializeToIMC() {
		//double[] latLonDepth = this.getManeuverLocation().getAbsoluteLatLonDepth();
		pt.lsts.imc.YoYo yoyo = new pt.lsts.imc.YoYo();
		LocationType loc = getManeuverLocation();
		loc.convertToAbsoluteLatLonDepth();
		yoyo.setTimeout(getMaxTime());
		yoyo.setLat(loc.getLatitudeRads());
		yoyo.setLon(loc.getLongitudeRads());
		yoyo.setZ(getManeuverLocation().getZ());
		yoyo.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));
		yoyo.setAmplitude(getAmplitude());
		yoyo.setPitch(getPitchAngle());
		speed.setSpeedToMessage(yoyo);
		yoyo.setCustom(getCustomSettings());

		return yoyo;
	}
	
	@Override
	public SpeedType getSpeed() {
	    return new SpeedType(speed);
	}

	@Override
	public void setSpeed(SpeedType speed) {
	    this.speed = new SpeedType(speed);       
	}

    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }

    public static void main(String[] args) {
        YoYo g = new YoYo();
        PropertiesEditor.editProperties(g, true);
        PropertiesEditor.editProperties(g, true);   
    }    
}
