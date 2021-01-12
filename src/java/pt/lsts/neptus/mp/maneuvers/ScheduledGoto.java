/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * 18/04/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.ScheduledGoto.DELAYED;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.editors.ScheduledGotoDateEditor;
import pt.lsts.neptus.mp.maneuvers.editors.ScheduledGotoDateRendererEditor;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.XMLUtil;

/**
 * @author zp
 *
 */
public class ScheduledGoto extends Goto {

    public static final String TIME_FORMAT_STR = "yyyy-MM-dd HH:mm:ss";

    protected static final String DEFAULT_ROOT_ELEMENT = "ScheduledGoto";
    
    @NeptusProperty(name = "Travel Z value")
    private double travelZ = 3;
    @NeptusProperty(name = "Travel Z units")
    private ManeuverLocation.Z_UNITS travelUnits = ManeuverLocation.Z_UNITS.DEPTH;
    @NeptusProperty(name = "Arrival Time", description = "Arrival Time (UTC timezone)", 
            editorClass = ScheduledGotoDateEditor.class, rendererClass = ScheduledGotoDateRendererEditor.class)
    private Date arrivalTime = new Date();
    @NeptusProperty(name = "Delayed Behavior", description = "How to proceed if vehicle doesn't reach the waypoint in time."
            + "\n\tRESUME - Continue until (delayed) arrival,\n\tSKIP - Move on to next maneuver,"
            + "\n\tFAIL - Stop plan with a failure.")
    private pt.lsts.imc.ScheduledGoto.DELAYED delayedBehavior = DELAYED.SKIP;

    @Override
    public String getType() {
        return "ScheduledGoto";
    }

    public Object clone() {  
        ScheduledGoto clone = new ScheduledGoto();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation());
        clone.setRadiusTolerance(getRadiusTolerance());
        clone.setSpeed(getSpeed());
        clone.setSpeedTolerance(getSpeedTolerance());
        clone.setTravelUnits(getTravelUnits());
        clone.setTravelZ(getTravelZ());
        clone.setArrivalTime(getArrivalTime());
        clone.setDelayedBehavior(getDelayedBehavior());
        return clone;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        try {
            pt.lsts.imc.ScheduledGoto msg = new pt.lsts.imc.ScheduledGoto(message);

            ManeuverLocation pos = new ManeuverLocation();
            pos.setLatitudeRads(msg.getLat());
            pos.setLongitudeRads(msg.getLon());
            pos.setZ(msg.getZ());
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(msg.getZUnits().toString()));
            setManeuverLocation(pos);

            setTravelUnits(ManeuverLocation.Z_UNITS.valueOf(msg.getTravelZUnits().toString()));
            setTravelZ(msg.getTravelZ());
            setArrivalTime(new Date((long)(msg.getArrivalTime() * 1000)));    
            setDelayedBehavior(msg.getDelayed());
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public IMCMessage serializeToIMC() {
        pt.lsts.imc.ScheduledGoto man = new pt.lsts.imc.ScheduledGoto();

        LocationType l = getManeuverLocation();
        l.convertToAbsoluteLatLonDepth();

        man.setLat(l.getLatitudeRads());
        man.setLon(l.getLongitudeRads());

        man.setZ(getManeuverLocation().getZ());
        man.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().name()));

        man.setTravelZUnits(ZUnits.valueOf(getTravelUnits().name()));
        man.setTravelZ(travelZ);

        man.setArrivalTime(arrivalTime.getTime()/1000.0);
        man.setDelayed(getDelayedBehavior());
        return man;
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

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document doc = super.getManeuverAsDocument(rootElementName);
        Element root = doc.getRootElement();
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT_STR);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Element arrivalTime = root.addElement("ArrivalTime");
        arrivalTime.setText(sdf.format(getArrivalTime()));
        Element delayedBehavior = root.addElement("DelayedBehavior");
        delayedBehavior.setText(getDelayedBehavior().toString());
        Element travelZ = root.addElement("TravelZ");
        travelZ.setText(""+getTravelZ());
        Element travelZUnits = root.addElement("TravelUnits");
        travelZUnits.setText(getTravelUnits().toString());
        return doc;
    }

    public void loadManeuverFromXML(String xml) {
        super.loadManeuverFromXML(xml);
        try {
            Document doc = DocumentHelper.parseText(xml);
            Node node = doc.selectSingleNode(getType()+"/ArrivalTime");
            if (node != null) {
                SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT_STR);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                setArrivalTime(sdf.parse(node.getText()));
            }           
            node = doc.selectSingleNode(getType()+"/DelayedBehavior");
            if (node != null)
                setDelayedBehavior(DELAYED.valueOf(node.getText()));
            node = doc.selectSingleNode(getType()+"/TravelZ");
            if (node != null)
                setTravelZ(Double.parseDouble(node.getText()));
            node = doc.selectSingleNode(getType()+"/TravelUnits");
            if (node != null)
                setTravelUnits(pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS.valueOf(node.getText()));            
        }
        catch (Exception e) {
            NeptusLog.pub().info(I18n.text("Error while loading the XML:")+"{" + xml + "}");
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        
        long diff = getArrivalTime().getTime() - new Date().getTime();
        
        String text;
        if (diff < 0)
            text = "-"+DateTimeUtil.milliSecondsToFormatedString(-diff, true);
        else
            text = DateTimeUtil.milliSecondsToFormatedString(diff, true);
            
        Rectangle2D rect = g2d.getFontMetrics().getStringBounds(text, g2d);
        g2d.translate(-rect.getWidth()/2, -rect.getHeight()/2-5);
        
        if (planElement.isBeingEdited()) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, -(int) rect.getHeight(), (int) rect.getWidth(), (int) rect.getHeight());
        }
        
        g2d.setColor(Color.black);
        g2d.drawString(text, 0, 0);
        g2d.translate(-1, -1);
        if (getArrivalTime().before(new Date())) {
            g2d.setColor(Color.red.darker());    
        }
        else {
            g2d.setColor(Color.green.darker());
        }

        g2d.drawString(text, 0, 0);
    }

    /**
     * @return the arrivalTime
     */
    public Date getArrivalTime() {
        return arrivalTime;
    }

    /**
     * @param arrivalTime the arrivalTime to set
     */
    public void setArrivalTime(Date arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    /**
     * @return the travelUnits
     */
    public ManeuverLocation.Z_UNITS getTravelUnits() {
        return travelUnits;
    }

    /**
     * @param travelUnits the travelUnits to set
     */
    public void setTravelUnits(ManeuverLocation.Z_UNITS travelUnits) {
        this.travelUnits = travelUnits;
    }

    /**
     * @return the travelZ
     */
    public double getTravelZ() {
        return travelZ;
    }

    /**
     * @param travelZ the travelZ to set
     */
    public void setTravelZ(double travelZ) {
        this.travelZ = travelZ;
    }

    /**
     * @return the delayedBehavior
     */
    public pt.lsts.imc.ScheduledGoto.DELAYED getDelayedBehavior() {
        return delayedBehavior;
    }

    /**
     * @param delayedBehavior the delayedBehavior to set
     */
    public void setDelayedBehavior(pt.lsts.imc.ScheduledGoto.DELAYED delayedBehavior) {
        this.delayedBehavior = delayedBehavior;
    }   
    
    public static void main(String[] args) {
        ScheduledGoto gt = new ScheduledGoto();
        System.out.println(XMLUtil.getAsPrettyPrintFormatedXMLString(gt.asXML().substring(39)));
    }
}
