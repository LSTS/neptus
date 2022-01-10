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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * May 19, 2013
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.CompassCalibration.DIRECTION;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;

/**
 * @author pdias
 *
 */
public class CompassCalibration extends Maneuver
        implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization, StatisticsProvider {

    protected static final String DEFAULT_ROOT_ELEMENT = "CompassCalibration";

    //@NeptusProperty(name="Location")
    public ManeuverLocation location = new ManeuverLocation();

    @NeptusProperty(name="Speed", description="The speed to be used")
    public SpeedType speed = new SpeedType(1.3, Units.MPS); 

    @NeptusProperty(name="Pitch", description="The Pitch angle used to perform the maneuver.", units = "\u00B0")
    public double pitchDegs = 15;

    @NeptusProperty(name="Amplitude", description="Yoyo motion amplitude.", units = "m")
    public double amplitude = 1;

    @NeptusProperty(name="Duration", description="The duration in seconds of this maneuver. Use '0' for unlimited duration time.", units = "s")
    public int duration = 300;

    @NeptusProperty(name="Radius", description="Radius of the maneuver.", units = "m")
    public float radius = 5;

    @NeptusProperty(name="Direction", description="Direction of the maneuver.")
    public DIRECTION direction = DIRECTION.CLOCKW;

    public CompassCalibration() {
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        //        root.addAttribute("kind", "automatic");
        Element finalPoint = root.addElement("finalPoint");
        finalPoint.addAttribute("type", "pointType");
        Element point = getManeuverLocation().asElement("point");
        finalPoint.add(point);

        //Element radTolerance = finalPoint.addElement("radiusTolerance");
        //radTolerance.setText("0");

        Element pitchEl = root.addElement("pitch");
        pitchEl.setText(String.valueOf(pitchDegs));

        Element amplitudeEl = root.addElement("amplitude");
        amplitudeEl.setText(String.valueOf(amplitude));

        Element durationEl = root.addElement("duration");
        durationEl.setText(String.valueOf(duration));

        Element speedEl = root.addElement("speed");
        speedEl.addAttribute("type", "float");
        speedEl.addAttribute("unit", speed.getUnits().name());
        speedEl.setText(String.valueOf(speed.getValue()));

        Element radiusEl = root.addElement("radius");
        radiusEl.setText(String.valueOf(radius));

        Element directionEl = root.addElement("direction");
        directionEl.setText(String.valueOf(direction.value()));

        return document;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#loadFromXML(java.lang.String)
     */
    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            Node node = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/finalPoint/point");
            if (node == null)
                node = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/initialPoint/point"); // to read old elevator specs
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);

            Node pitchNode = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/pitch");
            pitchDegs = Double.parseDouble(pitchNode.getText());

            Node amplitudeNode = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/amplitude");
            amplitude = Double.parseDouble(amplitudeNode.getText());

            Node durationNode = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/duration");
            duration = Integer.parseInt(durationNode.getText());
            
            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);            

            radius = Float.parseFloat(doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/radius").getText());

            direction = DIRECTION.CLOCKW;
            try {
                int dirVal = (int) Long.parseLong(doc.selectSingleNode(DEFAULT_ROOT_ELEMENT + "/direction").getText());
                direction = Arrays.asList(DIRECTION.values()).stream().filter(p -> p.value() == dirVal).findFirst()
                        .orElse(DIRECTION.CLOCKW);
            }
            catch (Exception e) {
                NeptusLog.pub().warn("Error parsing direction from maneuver");
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        location.setRadius(radius);
        return location.clone();
    }

    @Override
    public ManeuverLocation getEndLocation() {
        return getManeuverLocation();
    }

    @Override
    public ManeuverLocation getStartLocation() {
        return getManeuverLocation();
    }

    @Override
    public void setManeuverLocation(ManeuverLocation loc) {
        location = loc.clone();
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        getManeuverLocation().translatePosition(offsetNorth, offsetEast, offsetDown);
    }

    @Override
    public String getTooltipText() {
        return super.getTooltipText() + "<hr>" + 
                I18n.text("speed") + ": <b>" + speed + "</b>" + 
                ("<br>" + I18n.text("cruise depth") + ": <b>" + (int) getStartLocation().getDepth() + " " + I18n.textc("m", "meters") + "</b>") + 
                "<br>" + I18n.text("end z") + ": <b>" + getManeuverLocation().getZ() + " " + I18n.textc("m", "meters") + " (" + I18n.text(getManeuverLocation().getZUnits().toString()) + ")</b>" +
                "<br>" + I18n.text("pitch") + ": <b>" + pitchDegs + " " + I18n.textc("m", "meters") + "</b>" +
                "<br>" + I18n.text("amplitude") + ": <b>" + amplitude + " " + I18n.textc("m", "meters") + "</b>" +                
                "<br>" + I18n.text("radius") + ": <b>" + radius + " " + I18n.textc("m", "meters") + "</b>" +
                "<br>" + I18n.text("duration") + ": <b>" + duration + " " + I18n.textc("m", "meters") + "</b>";
    }

    public String validatePitchDegs(double value) {
        NeptusLog.pub().info("<###>validate...");
        if (value < 0 || value > (float)45)
            return I18n.text("Pitch angle shoud be bounded between [0\u00B0, 45\u00B0]");
        return null;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> properties = new Vector<DefaultProperty>();
        PluginProperty[] prop = PluginUtils.getPluginProperties(this);
        properties.addAll(Arrays.asList(prop));
        return properties;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public IMCMessage serializeToIMC() {
        getManeuverLocation().convertToAbsoluteLatLonDepth();

        pt.lsts.imc.CompassCalibration man = new pt.lsts.imc.CompassCalibration();

        man.setTimeout(getMaxTime());
        LocationType loc = getManeuverLocation();
        loc.convertToAbsoluteLatLonDepth();
        man.setLat(loc.getLatitudeRads());
        man.setLon(loc.getLongitudeRads());
        man.setZ(getManeuverLocation().getZ());
        man.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));
        man.setPitch(Math.toRadians(pitchDegs));
        man.setAmplitude(amplitude);
        man.setDuration(duration);
        man.setRadius(radius);
        
        man.setDirection(direction);
        man.setCustom(getCustomSettings());
        
        speed.setSpeedToMessage(man);

        return man;
    }
    @Override
    public void parseIMCMessage(IMCMessage message) {
        if (!DEFAULT_ROOT_ELEMENT.equalsIgnoreCase(message.getAbbrev())) {
            NeptusLog.pub().error("Unable to parse message of type "+message.getAbbrev());
            return;
        }
        pt.lsts.imc.CompassCalibration man = null;
        try {
             man = pt.lsts.imc.CompassCalibration.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        setMaxTime(man.getTimeout());
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitudeRads(man.getLat());
        loc.setLongitudeRads(man.getLon());
        loc.setZ(man.getZ());
        loc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(man.getZUnits().toString()));
        setManeuverLocation(loc);
        pitchDegs = Math.toDegrees(man.getPitch());
        amplitude = man.getAmplitude();
        duration = man.getDuration();
        radius = (float) man.getRadius();
        
        speed = SpeedType.parseImcSpeed(message);
        
        direction = man.getDirection();

        setCustomSettings(man.getCustom());       
    }

    @Override
    public Object clone() {
        CompassCalibration clone = new CompassCalibration();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation());
        clone.pitchDegs = pitchDegs;
        clone.amplitude = amplitude;
        clone.duration = duration;
        clone.radius = radius;
        clone.setSpeed(getSpeed());
        clone.direction = direction;
        return clone;
    }

    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);

        float radiusCorrected = radius * renderer.getZoom();

        Graphics2D g2 = (Graphics2D) g2d.create();
        
        // X Marks the Spot.
        g2.drawLine(-4, -4, 4, 4);
        g2.drawLine(-4, 4, 4, -4);

        g2.setColor(new Color(255, 255, 255, 100));
        g2.fill(new Ellipse2D.Double(-radiusCorrected, -radiusCorrected, radiusCorrected * 2, radiusCorrected * 2));
        g2.setColor(Color.blue.darker());
        g2.draw(new Ellipse2D.Double(-radiusCorrected, -radiusCorrected, radiusCorrected * 2, radiusCorrected * 2));

        // Clockwise Arrow.
        g2.translate(0, -radiusCorrected);

        if (direction == DIRECTION.CCLOCKW) {
            g2.drawLine(5, 5, 0, 0);
            g2.drawLine(5, -5, 0, 0);
        }
        else {
            g2.drawLine(-5, 5, 0, 0);
            g2.drawLine(-5, -5, 0, 0);
        }

        g2.dispose();
    }

    
    @Override
    public double getCompletionTime(LocationType initialPosition) {
        return getDistanceTravelled(initialPosition) / getSpeed().getMPS();
    }

    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = getStartLocation().getDistanceInMeters(initialPosition);
        double depthDiff = initialPosition.getAllZ();
        meters += depthDiff;
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

    public static void main(String[] args) {
        CompassCalibration compc = new CompassCalibration();
        String ccmanXML = compc.getManeuverAsDocument("CompassCalibration").asXML();
        System.out.println(ccmanXML);
        CompassCalibration compc1 = new CompassCalibration();
        compc1.loadManeuverFromXML(ccmanXML);
        ccmanXML = compc.getManeuverAsDocument("CompassCalibration").asXML();
        System.out.println(ccmanXML);
        
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
