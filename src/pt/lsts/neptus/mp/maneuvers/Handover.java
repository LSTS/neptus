/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * 06/07/2017
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.Handover.DIRECTION;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.def.Boolean;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.XMLUtil;

/**
 * @author pdias
 *
 */
public class Handover extends Goto implements LocatedManeuver, ManeuverWithSpeed, StatisticsProvider, IMCSerialization {

    protected static final String DEFAULT_ROOT_ELEMENT = "Handover";

    @NeptusProperty(name = "Direction")
    private pt.lsts.imc.Handover.DIRECTION direction = pt.lsts.imc.Handover.DIRECTION.CLOCKW;
    @NeptusProperty(name = "Radius")
    private double radius = 100;
    @NeptusProperty(name = "RC Handover")
    private boolean rcHandover = true;

    public Handover() {
    }
    
    @Override
    public String getType() {
        return "Handover";
    }

    @Override
    public Object clone() {  
        Handover clone = new Handover();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation());
        clone.setSpeed(getSpeed());
        clone.setSpeedTolerance(getSpeedTolerance());
        clone.setSpeedUnits(getSpeedUnits());
        clone.direction = direction;
        clone.radius = radius;
        clone.rcHandover = rcHandover;
        return clone;
    }
    
    @Override
    public void parseIMCMessage(IMCMessage message) {
        try {
            pt.lsts.imc.Handover msg = pt.lsts.imc.Handover.clone(message);
            
            setSpeed(msg.getSpeed());
            switch (msg.getSpeedUnits()) {
                case METERS_PS:
                    setSpeedUnits(SPEED_UNITS.METERS_PS);
                    break;
                case PERCENTAGE:
                    setSpeedUnits(SPEED_UNITS.PERCENTAGE);
                    break;
                case RPM:
                    setSpeedUnits(SPEED_UNITS.RPM);
                    break;
            }
            ManeuverLocation pos = new ManeuverLocation();
            pos.setLatitudeRads(msg.getLat());
            pos.setLongitudeRads(msg.getLon());
            pos.setZ(msg.getZ());
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(msg.getZUnits().toString()));
            
            setManeuverLocation(pos);
            
            radius = msg.getRadius();
            direction = msg.getDirection();
            rcHandover = msg.getRcHandover().compareTo(Boolean.TRUE) == 0 ? true : false;
            
            setCustomSettings(msg.getCustom());
            
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public IMCMessage serializeToIMC() {
        pt.lsts.imc.Handover imcMan = new pt.lsts.imc.Handover();
        
        LocationType loc = getManeuverLocation();
        loc.convertToAbsoluteLatLonDepth();
        
        imcMan.setLat(loc.getLatitudeRads());
        imcMan.setLon(loc.getLongitudeRads());
        imcMan.setZ(getManeuverLocation().getZ());
        imcMan.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().name()));
        imcMan.setSpeed(this.getSpeed());
       
        try {
            switch (this.getSpeedUnits()) {
                case METERS_PS:
                    imcMan.setSpeedUnits(SpeedUnits.METERS_PS);
                    break;
                case PERCENTAGE:
                    imcMan.setSpeedUnits(SpeedUnits.PERCENTAGE);
                    break;
                case RPM:
                default:
                    imcMan.setSpeedUnits(SpeedUnits.RPM);
                    break;
            }
        }
        catch (Exception ex) {
            NeptusLog.pub().error(this, ex);                     
        }

        imcMan.setRadius(Math.toRadians(radius));
        imcMan.setDirection(direction);
        imcMan.setRcHandover(rcHandover ? Boolean.TRUE : Boolean.FALSE);
        
        imcMan.setCustom(getCustomSettings());

        return imcMan;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        return ManeuversUtil.getPropertiesFromManeuver(this);     
    }
    
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        ManeuversUtil.setPropertiesToManeuver(this, properties);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.Goto#loadFromXML(java.lang.String)
     */
    @Override
    public void loadManeuverFromXML(String xml) {
        super.loadManeuverFromXML(xml);
        
        try {
            Document doc = DocumentHelper.parseText(xml);

            try {
                radius = Double.valueOf(doc.selectSingleNode("//radius").getText());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            
            try {
                direction = DIRECTION.valueOf(doc.selectSingleNode("//direction").getText());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                rcHandover = java.lang.Boolean.valueOf(doc.selectSingleNode("//rcHandover").getText());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }
        catch (Exception e) {
            NeptusLog.pub().info(I18n.text("Error while loading the XML:")+"{" + xml + "}");
            NeptusLog.pub().error(this, e);
        }
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.Goto#getManeuverAsDocument(java.lang.String)
     */
    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document doc = super.getManeuverAsDocument(rootElementName);
        Element root = doc.getRootElement();
        
        root.addElement("radius").setText("" + radius);
        root.addElement("direction").setText(direction.toString());
        root.addElement("rcHandover").setText("" + rcHandover);;
        
        return doc;
    }
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);

        AffineTransform at = g2d.getTransform();

        double radius = this.radius * renderer.getZoom();
        boolean isClockwise = direction.compareTo(DIRECTION.CLOCKW) == 0 ? true : false;
        double rt = 0;

        g2d.setColor(new Color(255, 255, 255, 100));
        Area outer = new Area(new Ellipse2D.Double(-radius - rt, -radius - rt, (radius + rt) * 2, (radius + rt) * 2));
        Area inner = new Area(new Ellipse2D.Double(-radius + rt, -radius + rt, (radius - rt) * 2, (radius - rt) * 2));

        outer.subtract(inner);

        g2d.fill(outer);
        g2d.setColor(Color.RED);

        g2d.draw(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));

        g2d.translate(0, -radius);
        if (isClockwise) {
            g2d.drawLine(-5, 5, 0, 0);
            g2d.drawLine(-5, -5, 0, 0);
        }
        else {
            g2d.drawLine(5, 5, 0, 0);
            g2d.drawLine(5, -5, 0, 0);
        }

        g2d.setTransform(at);
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        double speed = this.speed;
        if (this.speedUnits == SPEED_UNITS.RPM) {
            speed = speed/769.230769231; //1.3 m/s for 1000 RPMs
        }
        else if (this.speedUnits == SPEED_UNITS.PERCENTAGE) {
            speed = speed/76.923076923; //1.3 m/s for 100% speed
        }

        double time = getDistanceTravelled(initialPosition) / speed;

        return /*getLoiterDuration() == 0 ? Double.POSITIVE_INFINITY :*/ 0 + time;
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
    
    public static void main(String[] args) {
        Handover rc = new Handover();
        System.out.println(XMLUtil.getAsPrettyPrintFormatedXMLString(rc.asXML().substring(39)));
    }
}
