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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 22/05/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Takeoff.UAV_TYPE;
import pt.lsts.neptus.gui.editor.SpeedUnitsEnumEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.XMLUtil;
import pt.lsts.neptus.util.conf.IntegerMinMaxValidator;

/**
 * @author pdias
 *
 */
public class Land extends Maneuver implements LocatedManeuver, IMCSerialization {
    
    protected double latDegs = 0;
    protected double lonDegs = 0;
    protected double z = 0;
    protected ManeuverLocation.Z_UNITS zUnits = ManeuverLocation.Z_UNITS.NONE;

    @NeptusProperty(name = "Abort Z", description = "Abort altitude or height. If landing is aborted while executing, the UAV will maintain its course and attempt to climb to the abort z reference.")
    protected double zAbort = 20;
    @NeptusProperty(name = "Speed")
    protected double speed = 17;
    @NeptusProperty(name = "Speed Units", editorClass = SpeedUnitsEnumEditor.class)
    protected SPEED_UNITS speedUnits = SPEED_UNITS.METERS_PS;
    @NeptusProperty(name = "Bearing", description = "Land bearing angle.")
    protected double bearingDegs = 0;
    @NeptusProperty(name = "Glide Slope", description = "Ratio (%) of the distance from the last waypoint to the landing point (touchdown) and the height difference between them.")
    protected short glideSlope = 10;
    @NeptusProperty(name = "Glide Slope Altitude", description = "Height difference between the last waypoint to the landing point (touchdown).")
    protected float glideSlopeAltitude = 10;
    @NeptusProperty(name = "UAV Type")
    protected UAV_TYPE uavType = UAV_TYPE.FIXEDWING;

    public Land() {
    }

    protected String validateGlideSlope(short value) {
        return new IntegerMinMaxValidator(0, 10, true, true).validate(value);
    }

    protected String validateGlideSlopeAltitude(short value) {
        return new IntegerMinMaxValidator(10, false, true).validate(value);
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        ManeuverLocation manLoc = new ManeuverLocation();
        manLoc.setLatitudeDegs(latDegs);
        manLoc.setLongitudeDegs(lonDegs);
        manLoc.setZ(z);
        manLoc.setZUnits(zUnits);
        return manLoc;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocatedManeuver#setManeuverLocation(pt.lsts.neptus.mp.ManeuverLocation)
     */
    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        double absoluteLatLonDepth[] = location.getAbsoluteLatLonDepth(); 
        latDegs = absoluteLatLonDepth[0];
        lonDegs = absoluteLatLonDepth[1];
        z = location.getZ();
        zUnits = location.getZUnits();
    }

    @Override
    public ManeuverLocation getStartLocation() {
        ManeuverLocation loc = getManeuverLocation();
        if (glideSlopeAltitude <= 0)
            return loc;
        
        float distance = glideSlope * glideSlopeAltitude;
        double bearingRad = Math.toRadians(bearingDegs);
        loc.translatePosition(-distance * Math.cos(bearingRad), -distance * Math.sin(bearingRad), -glideSlopeAltitude);
        loc.convertToAbsoluteLatLonDepth();
        return loc;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocatedManeuver#getEndLocation()
     */
    @Override
    public ManeuverLocation getEndLocation() {
        return getManeuverLocation();
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        ManeuverLocation loc = getManeuverLocation();
        loc.translatePosition(offsetNorth, offsetEast, offsetDown);
        setManeuverLocation(loc);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocatedManeuver#getWaypoints()
     */
    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        ArrayList<ManeuverLocation> wps = new ArrayList<ManeuverLocation>();
        wps.add(getManeuverLocation());
        return wps;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#loadFromXML(java.lang.String)
     */
    @Override
    public void loadFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
    
            ManeuversXMLUtil.parseLocation(doc.getRootElement(), this);
            try {
                ManeuversXMLUtil.parseSpeed(doc.getRootElement(), this);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            Node node = doc.selectSingleNode("//bearing");
            if (node != null)
                bearingDegs = Double.parseDouble(node.getText());

            node = doc.selectSingleNode("//glideSlope");
            if (node != null)
                glideSlope = Short.parseShort(node.getText());
            
            node = doc.selectSingleNode("//glideSlopeAltitude");
            if (node != null)
                glideSlopeAltitude = Float.parseFloat(node.getText());

            node = doc.selectSingleNode("//uavType");
            if (node != null) {
                String typeStr = node.getText();
                try {
                    uavType = UAV_TYPE.valueOf(typeStr);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    uavType = UAV_TYPE.FIXEDWING;
                }
            }
            else {
                uavType = UAV_TYPE.FIXEDWING;
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#getManeuverAsDocument(java.lang.String)
     */
    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document doc = ManeuversXMLUtil.createBaseDoc(getType());
        ManeuversXMLUtil.addLocation(doc.getRootElement(), this);
        try {
            ManeuversXMLUtil.addSpeed(doc.getRootElement(), this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();

        root.addElement("bearing").setText(String.valueOf(bearingDegs));

        root.addElement("glideSlope").setText(String.valueOf(glideSlope));
        root.addElement("glideSlopeAltitude").setText(String.valueOf(glideSlopeAltitude));

        root.addElement("uavType").setText(String.valueOf(uavType.name()));

        return doc;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        return ManeuversUtil.getPropertiesFromManeuver(this);
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        ManeuversUtil.setPropertiesToManeuver(this, properties);
        
        if (bearingDegs < 0 || bearingDegs > 360)
            bearingDegs = AngleUtils.nomalizeAngleDegrees360(bearingDegs);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#clone()
     */
    @Override
    public Object clone() {
        Land clone = new Land();
        super.clone(clone);
        clone.latDegs = latDegs;
        clone.lonDegs = lonDegs;
        clone.z = z;
        clone.zUnits = zUnits;
        clone.speed = speed;
        clone.speedUnits = speedUnits;
        clone.bearingDegs = bearingDegs;
        clone.glideSlope = glideSlope;
        clone.glideSlopeAltitude = glideSlopeAltitude;
        return clone;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.IMCSerialization#serializeToIMC()
     */
    @Override
    public IMCMessage serializeToIMC() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.IMCSerialization#parseIMCMessage(pt.lsts.imc.IMCMessage)
     */
    @Override
    public void parseIMCMessage(IMCMessage message) {
        // TODO Auto-generated method stub

    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#paintOnMap(java.awt.Graphics2D, pt.lsts.neptus.types.map.PlanElement, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        ManeuverLocation startLLoc = getStartLocation();
        
        double[] dp = getEndLocation().getDistanceInPixelTo(startLLoc, renderer.getLevelOfDetail());
        
        Color color1 = new Color(210, 176, 106); // KHAKI
        Color color2 = new Color(0, 0, 0, 106);
        
        float distance = glideSlope * glideSlopeAltitude;

        Stroke sO = g2d.getStroke();
        Stroke s1 = new BasicStroke(2);
        Stroke s2 = new BasicStroke(16);
        //Stroke sR = new BasicStroke((float) (2 * sRange * zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        g2d.setColor(color2);
        g2d.setStroke(s2);
        g2d.draw(new Line2D.Double(0, 0, dp[0], dp[1]));
        g2d.setColor(Color.WHITE);
        g2d.setStroke(s1);
        g2d.draw(new Line2D.Double(0, 0, dp[0], dp[1]));

        String str = I18n.textfc("d=%value m", "This means distance in meters.",
                MathMiscUtils.round(distance, 1));
        g2d.setColor(Color.BLACK);
        g2d.drawString(str, 10, -10);
        g2d.setColor(color1);
        g2d.drawString(str, 11, -11);

        if (glideSlopeAltitude > 0) {
            str = I18n.textfc("alt=%alt m | abort=%z m (%typeZRef)", "This means altitude in meters, and abort z",
                    MathMiscUtils.round(glideSlopeAltitude, 1), MathMiscUtils.round(zAbort, 1),
                    zUnits.toString());
            g2d.setColor(Color.BLACK);
            g2d.drawString(str, (int) dp[0] + 10, (int) dp[1] + -10);
            g2d.setColor(color1);
            g2d.drawString(str, (int) dp[0] + 11, (int) dp[1] + -11);
        }
    }
    
    public static void main(String[] args) {
        Land rc = new Land();
        System.out.println(XMLUtil.getAsPrettyPrintFormatedXMLString(rc.asXML().substring(39)));
    }
}
