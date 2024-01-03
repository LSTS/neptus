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
 * 18 de Nov de 2011
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;
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
import pt.lsts.neptus.gui.editor.ZUnitsEditor;
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
import pt.lsts.neptus.types.map.PlanUtil;

/**
 * @author pdias
 *
 */
public class Elevator extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization, StatisticsProvider {

    protected static final String DEFAULT_ROOT_ELEMENT = "Elevator";

    //@NeptusProperty(name="Location")
    public ManeuverLocation location = new ManeuverLocation();

    @NeptusProperty(name="Speed", description="The speed to be used")
    public SpeedType speed = new SpeedType(1000, Units.RPM); 

    @NeptusProperty(name="Start from current position", description="Start from current position or use the location field")
    public boolean startFromCurrentPosition = false;
    
    @NeptusProperty(name="Start Z", units = "m")
    public float startZ = 0;
    
    @NeptusProperty(name="Start Z Units")
    public ManeuverLocation.Z_UNITS startZUnits = ManeuverLocation.Z_UNITS.NONE;

    @NeptusProperty(name="Radius", units = "m")
    public float radius = 5;

    double speedTolerance = 5, radiusTolerance = 2;

    /**
     * 
     */
    public Elevator() {
        // TODO Auto-generated constructor stub
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

        Element radTolerance = finalPoint.addElement("radiusTolerance");
        radTolerance.setText("0");

        Element startZ = root.addElement("startZ");
        startZ.setText(String.valueOf(getStartZ()));
        Element startZUnits = root.addElement("startZUnits");
        startZUnits.setText(String.valueOf(getStartZUnits().toString()));

        Element radius = root.addElement("radius");
        radius.setText(String.valueOf(getRadius()));

        SpeedType.addSpeedElement(root, this);

        Element flags = root.addElement("flags");
        flags.addAttribute("useCurrentLocation", String.valueOf(isStartFromCurrentPosition()));

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
          
            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);

            Node sz = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/startZ");
            if (sz == null)
                doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/endZ"); // to read old elevator specs
            setStartZ(sz == null ? 0 : Float.parseFloat(sz.getText()));
            Node szu = doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/startZUnits");
            setStartZUnits(szu == null ? ManeuverLocation.Z_UNITS.NONE : ManeuverLocation.Z_UNITS.valueOf(szu.getText()));
            setRadius(Float.parseFloat(doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/radius").getText()));

            Element flags = (Element) doc.selectSingleNode(DEFAULT_ROOT_ELEMENT+ "/flags");
            if (flags == null) {
                setStartFromCurrentPosition(false);
            }
            else {
                Node ucl = flags.selectSingleNode("@useCurrentLocation");
                if (ucl == null)
                    setStartFromCurrentPosition(false);
                else
                    setStartFromCurrentPosition(Boolean.parseBoolean(ucl.getText()));
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    /**
     * @return the startFromCurrentPosition
     */
    public boolean isStartFromCurrentPosition() {
        return startFromCurrentPosition;
    }

    /**
     * @param startFromCurrentPosition the startFromCurrentPosition to set
     */
    public void setStartFromCurrentPosition(boolean startFromCurrentPosition) {
        this.startFromCurrentPosition = startFromCurrentPosition;
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
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
        setStartZ((float)loc.getZ());
    }

    /**
     * @return the endZ
     */
    public float getEndZ() {
        return (float)getManeuverLocation().getZ();
    }

    /**
     * @param endZ the endZ to set
     */
    public void setEndZ(float endZ) {
        location.setZ(endZ);
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        getManeuverLocation().translatePosition(offsetNorth, offsetEast, offsetDown);
    }

    @Override
    public String getTooltipText() {
        return super.getTooltipText() + "<hr>" + I18n.text("speed") + ": <b>" + speed + "</b>" + 
                (!startFromCurrentPosition ? "<br>" + I18n.text("cruise depth") + ": <b>" + (int) getStartLocation().getDepth() + " " + I18n.textc("m", "meters") + "</b>":"") + 
                "<br>" + I18n.text("start") + "" + ": <b>" + startZ + " " + I18n.textc("m", "meters") + " (" + I18n.text(startZUnits.toString()) + ")</b>" +
                "<br>" + I18n.text("end z") + ": <b>" + getManeuverLocation().getZ() + " " + I18n.textc("m", "meters") + " (" + I18n.text(getManeuverLocation().getZUnits().toString()) + ")</b>" +
                "<br>" + I18n.text("radius") + ": <b>" + radius + " " + I18n.textc("m", "meters") + "</b>";                
    }

    public String validatePitchAngleDegrees(float value) {
        NeptusLog.pub().info("<###>validate...");
        if (value < 0 || value > (float)45)
            return I18n.text("Pitch angle shoud be bounded between [0\u00B0, 45\u00B0]");
        return null;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> properties = new Vector<DefaultProperty>();
        LinkedHashMap<String, PluginProperty> propList = PluginUtils.getProperties(this, true);
        
        PluginProperty pStartZ = propList.get("startZUnits");
        PropertiesEditor.getPropertyEditorRegistry().unregisterEditor(pStartZ);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(pStartZ, vehicles.isEmpty() ? new ZUnitsEditor()
                : new ZUnitsEditor(PlanUtil.getValidZUnitsForVehicle(vehicles.get(0))));
        
        PluginProperty[] prop = propList.values().toArray(new PluginProperty[0]);
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

        pt.lsts.imc.Elevator elevator = new pt.lsts.imc.Elevator();

        elevator.setTimeout(getMaxTime());
        elevator.setLat(getManeuverLocation().getLatitudeRads());
        elevator.setLon(getManeuverLocation().getLongitudeRads());
        elevator.setStartZ(startZ);
        elevator.setStartZUnits(ZUnits.valueOf(
                startZUnits.toString()));
        elevator.setEndZ(getManeuverLocation().getZ());
        elevator.setEndZUnits(ZUnits.valueOf(
                getManeuverLocation().getZUnits().toString()));
        elevator.setRadius(getRadius());
        
        speed.setSpeedToMessage(elevator);
        
        elevator.setCustom(getCustomSettings());
        
        if (isStartFromCurrentPosition())
            elevator.setFlags(pt.lsts.imc.Elevator.FLG_CURR_POS);
        else
            elevator.setFlags((short)0);

        return elevator;
    }
    @Override
    public void parseIMCMessage(IMCMessage message) {
        if (!DEFAULT_ROOT_ELEMENT.equalsIgnoreCase(message.getAbbrev()))
            return;
        pt.lsts.imc.Elevator elev = null;
        try {
             elev = pt.lsts.imc.Elevator.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        setMaxTime(elev.getTimeout());
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitudeRads(elev.getLat());
        loc.setLongitudeRads(elev.getLon());
        loc.setZ(elev.getEndZ());
        NeptusLog.pub().info("<###> "+elev.getEndZUnits());
//        loc.setZUnits(pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS.valueOf(elev.getEndZUnits().toString()));
        loc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(message.getString("end_z_units").toString()));
        setManeuverLocation(loc);
        startZ = (float)elev.getStartZ();
        startZUnits = ManeuverLocation.Z_UNITS.valueOf(message.getString("start_z_units").toString());
        setRadius((float)elev.getRadius());
        
        setStartFromCurrentPosition((elev.getFlags() & pt.lsts.imc.Elevator.FLG_CURR_POS) != 0);
        setCustomSettings(elev.getCustom());
        setSpeed(SpeedType.parseImcSpeed(elev));
    }

    @Override
    public Object clone() {
        Elevator clone = new Elevator();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation());
        clone.startZ = startZ;
        clone.startZUnits = startZUnits;
        clone.setStartFromCurrentPosition(isStartFromCurrentPosition());
        clone.setRadius(getRadius());
        clone.setSpeed(getSpeed());
        return clone;
    }

    /**
     * @return the radius
     */
    public float getRadius() {
        return radius;
    }

    /**
     * @param radius the radius to set
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public SpeedType getSpeed() {
        return new SpeedType(speed);
    }

    @Override
    public void setSpeed(SpeedType speed) {
        this.speed = new SpeedType(speed);
    }

    /**
     * @return the startZ
     */
    public final float getStartZ() {
        return startZ;
    }

    /**
     * @param startZ the startZ to set
     */
    public final void setStartZ(float startZ) {
        this.startZ = startZ;
    }

    /**
     * @return the startZUnits
     */
    public ManeuverLocation.Z_UNITS getStartZUnits() {
        return startZUnits;
    }
    
    /**
     * @param startZUnits the startZUnits to set
     */
    public void setStartZUnits(ManeuverLocation.Z_UNITS startZUnits) {
        this.startZUnits = startZUnits;
    }
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        g2d = (Graphics2D) g2d.create();
        if (!isStartFromCurrentPosition()) {
            // x marks the spot...
            g2d.drawLine(-4, -4, 4, 4);
            g2d.drawLine(-4, 4, 4, -4);
        }
        double radius = this.getRadius() * renderer.getZoom();
        if (isStartFromCurrentPosition())
            g2d.setColor(new Color(255, 0, 0, 100));
        else
            g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fill(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));
        if (isStartFromCurrentPosition())
            g2d.setColor(Color.RED);
        else
            g2d.setColor(Color.GREEN);
        g2d.draw(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));
        g2d.setColor(new Color(255, 0, 0, 200));
        for (double i = this.getRadius() - 2; i > 0; i = i - 2) {
            double r = i * renderer.getZoom();
            g2d.draw(new Ellipse2D.Double(-r, -r, r * 2, r * 2));
        }

        //        g2d.rotate(Math.PI/2);
        g2d.translate(0, -14);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (isStartFromCurrentPosition()) {
            g2d.drawLine(-5, 0, 5, 0);
        }
        else {
            int m = 1;
            if (getManeuverLocation().getAllZ() < getEndZ())
                m = -1;
            g2d.drawLine(-5, m * -5, 5, m * -5);
            if (getManeuverLocation().getAllZ() < getEndZ() || getManeuverLocation().getAllZ() > getEndZ()) {
                g2d.drawLine(-5, m * 5, 0, 0);
                g2d.drawLine(5, m * 5, 0, 0);
            }
            else
                g2d.drawLine(-5, m * 5, 5, m * 5);
        }

        g2d.dispose();
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        double speed = getSpeed().getMPS();
        return getDistanceTravelled(initialPosition) / speed;
    }

    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = startFromCurrentPosition ? 0 : getStartLocation().getDistanceInMeters(initialPosition);
        double depthDiff = startFromCurrentPosition ? initialPosition.getAllZ() : getStartLocation().getAllZ();
        meters += depthDiff;
        return meters;
    }

    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }
    
    @Override
    public double getMaxDepth() {
        return getManeuverLocation().getAllZ();
    }

    @Override
    public double getMinDepth() {
        return getManeuverLocation().getAllZ();
    }   
}
