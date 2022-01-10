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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 22/05/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Graphics2D;
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
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.XMLUtil;

/**
 * @author pdias
 *
 */
public class Takeoff extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization {
    
    protected double latDegs = 0;
    protected double lonDegs = 0;
    protected double z = 0;
    protected ManeuverLocation.Z_UNITS zUnits = ManeuverLocation.Z_UNITS.NONE;

    @NeptusProperty(name = "Speed")
    protected SpeedType speed = new SpeedType(17, Units.MPS);
   
    @NeptusProperty(name = "Takeoff Pitch Angle", description = "Minimum pitch angle during automatic takeoff.", units = "\u00B0")
    protected double takeoffPitchAngleDegs = 10;

    public Takeoff() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#getType()
     */
    @Override
    public String getType() {
        return "Takeoff";
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
        return getManeuverLocation();
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

    /**
     * @return the takeoffPitchAngleDegs
     */
    public double getTakeoffPitchAngleDegs() {
        return takeoffPitchAngleDegs;
    }
    /**
     * @param takeoffPitchAngleDegs the takeoffPitchAngleDegs to set
     */
    public void setTakeoffPitchAngleDegs(double takeoffPitchAngleDegs) {
        this.takeoffPitchAngleDegs = takeoffPitchAngleDegs;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#loadFromXML(java.lang.String)
     */
    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
    
            ManeuversXMLUtil.parseLocation(doc.getRootElement(), this);
            SpeedType.addSpeedElement(doc.getRootElement(), this);
            
            Node node = doc.selectSingleNode("//takeoffPitch");
            if (node != null)
                takeoffPitchAngleDegs = Double.parseDouble(node.getText());

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
        SpeedType.addSpeedElement(doc.getRootElement(), this);

        Element root = doc.getRootElement();
        root.addElement("takeoffPitch").setText(String.valueOf(takeoffPitchAngleDegs));
        
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
        
        if (takeoffPitchAngleDegs < 0 || takeoffPitchAngleDegs > 360)
            takeoffPitchAngleDegs = AngleUtils.nomalizeAngleDegrees360(takeoffPitchAngleDegs);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#clone()
     */
    @Override
    public Object clone() {
        Takeoff clone = new Takeoff();
        super.clone(clone);
        clone.latDegs = latDegs;
        clone.lonDegs = lonDegs;
        clone.z = z;
        clone.zUnits = zUnits;
        clone.speed = getSpeed();
        clone.takeoffPitchAngleDegs = takeoffPitchAngleDegs;
        return clone;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.IMCSerialization#serializeToIMC()
     */
    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.Takeoff man = new pt.lsts.imc.Takeoff();
        man.setLat(Math.toRadians(latDegs));
        man.setLon(Math.toRadians(lonDegs));
        man.setZ(z);
        man.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));        
        speed.setSpeedToMessage(man);

        man.setTakeoffPitch(Math.toRadians(takeoffPitchAngleDegs));
        
        return man;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.IMCSerialization#parseIMCMessage(pt.lsts.imc.IMCMessage)
     */
    @Override
    public void parseIMCMessage(IMCMessage message) {
        pt.lsts.imc.Takeoff man = null;
        try {
            man = pt.lsts.imc.Takeoff.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        latDegs = Math.toDegrees(man.getLat());
        lonDegs = Math.toDegrees(man.getLon());
        z = man.getZ();
        zUnits = ManeuverLocation.Z_UNITS.valueOf(man.getZUnits().toString());
        speed = SpeedType.parseImcSpeed(message);
        
        takeoffPitchAngleDegs = Math.toDegrees(man.getTakeoffPitch());
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#paintOnMap(java.awt.Graphics2D, pt.lsts.neptus.types.map.PlanElement, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        
        Color color1 = Color.CYAN.brighter();
        
        g2d.setColor(color1);
        g2d.draw(new Line2D.Double(0, 0, 30, 0));
        double angleRad = Math.toRadians(takeoffPitchAngleDegs);
        g2d.draw(new Line2D.Double(0, 0, 30, -30 * Math.sin(angleRad)));
        g2d.draw(new Line2D.Double(30, 0, 30, -30 * Math.sin(angleRad)));
        
        String str = ((int) MathMiscUtils.round(takeoffPitchAngleDegs, 0)) + "\u00B0";
        g2d.setColor(Color.BLACK);
        g2d.drawString(str, 10, -10);
        g2d.setColor(color1);
        g2d.drawString(str, 11, -11);
    }
    
    @Override
    public SpeedType getSpeed() {
        return new SpeedType(speed);
    }
    
    @Override
    public void setSpeed(SpeedType speed) {
        this.speed = new SpeedType(speed);       
    }
    
    public static void main(String[] args) {
        Takeoff rc = new Takeoff();
        System.out.println(XMLUtil.getAsPrettyPrintFormatedXMLString(rc.asXML().substring(39)));
    }
}
