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
 * 2010/07/14
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
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
import pt.lsts.imc.StationKeepingExtended;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;

public class StationKeeping extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization, StatisticsProvider {

    public static final int INFINITY_DURATION = 0;
    public static final double MINIMUM_SK_RADIUS = 10;

    private int duration = 60;
    private double radius = 10;
    private boolean keepSafe = false;
    private int popupPeriod = 120;
    private int popupDuration = 60;    
    private SpeedType speed = new SpeedType(1000, Units.RPM);
    private ManeuverLocation location = new ManeuverLocation();	 

    @Override
    public Object clone() {
        StationKeeping l = new StationKeeping();
        super.clone(l);
        l.setDuration(getDuration());
        l.setManeuverLocation(getManeuverLocation().clone());
        l.setRadius(getRadius());
        l.setSpeed(getSpeed());
        l.setKeepSafe(isKeepSafe());
        l.setPopupDuration(getPopupDuration());
        l.setPopupPeriod(getPopupPeriod());
        return l;		
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        root.addAttribute("kind", "automatic");

        //basePoint
        Element basePoint = root.addElement("basePoint");
        Element point = getManeuverLocation().asElement("point");
        basePoint.add(point);
        Element radTolerance = basePoint.addElement("radiusTolerance");
        radTolerance.setText("0");	   
        basePoint.addAttribute("type", "pointType");

        //duration
        root.addElement("duration").setText(""+getDuration());

        //popup duration
        root.addElement("popupDuration").setText(""+getPopupDuration());

        //popup period
        root.addElement("popupPeriod").setText(""+getPopupPeriod());

        //keep safe
        root.addElement("keepSafe").setText(""+isKeepSafe());

        //trajectory
        Element trajectory = root.addElement("trajectory");	    
        Element trajRadius = trajectory.addElement("radius");
        trajRadius.setText(String.valueOf(getRadius()));
        trajRadius.addAttribute("type", "float");	    

        SpeedType.addSpeedElement(root, this);

        return document;
    }

    @Override
    public String getType() {
        return "StationKeeping";
    }

    @Override
    public void loadManeuverFromXML(String XML) {
        try {
            Document doc = DocumentHelper.parseText(XML);

            // basePoint
            Node node = doc.selectSingleNode("StationKeeping/basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);	       

            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);

            // Duration
            setDuration(Integer.parseInt(doc.selectSingleNode("StationKeeping/duration").getText()));
            try {
                setPopupDuration(Integer.parseInt(doc.selectSingleNode("StationKeeping/popupDuration").getText()));
                setPopupPeriod(Integer.parseInt(doc.selectSingleNode("StationKeeping/popupPeriod").getText()));
                setKeepSafe(Boolean.parseBoolean(doc.selectSingleNode("StationKeeping/keepSafe").getText()));    
            }
            catch (Exception e) {
                NeptusLog.pub().warn("Vehicle does not have defaults for keepSafe parameters.");
            }
            

            // Trajectory
            setRadius(Double.parseDouble(doc.selectSingleNode("StationKeeping/trajectory/radius").getText()));
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        return location.clone();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocationProvider#getFirstPosition()
     */
    @Override
    public ManeuverLocation getStartLocation() {
        return location.clone();
    }

    @Override
    public ManeuverLocation getEndLocation() {
        return location.clone();
    }

    public void setManeuverLocation(ManeuverLocation location) {
        this.location = location.clone();
    }

    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        location.translatePosition(offsetNorth, offsetEast, offsetDown);		
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();

        if (radius < MINIMUM_SK_RADIUS)
            radius = MINIMUM_SK_RADIUS;

        DefaultProperty duration = PropertiesEditor.getPropertyInstance("Duration", Integer.class, this.duration, true);
        duration.setShortDescription("The Station Keeping's duration, in seconds (0 means +Infinity)");		
        props.add(duration);

        DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, this.speed, true);
	speed.setShortDescription(I18n.text("The vehicle's desired speed when Station Keeping"));
	props.add(speed);

        DefaultProperty radius = PropertiesEditor.getPropertyInstance("Radius", Double.class, this.radius, true);
        radius.setShortDescription(
                I18n.textf("Radius of the Station Keeping circle. Lower values default to %radius meters.",
                        MINIMUM_SK_RADIUS) + "<br/>(m)");
		props.add(radius);

        DefaultProperty popDuration = PropertiesEditor.getPropertyInstance("Popup Duration", Integer.class, this.popupDuration, true);
        popDuration.setShortDescription("The duration of the station keeping at surface level when it pops up. Only used if flag KEEP_SAFE is on.");     
        props.add(popDuration);

        DefaultProperty popPeriod = PropertiesEditor.getPropertyInstance("Popup Period", Integer.class, this.popupPeriod, true);
        popPeriod.setShortDescription("The period at which the vehicle will popup to report its position. Only used if flag KEEP_SAFE is on.");     
        props.add(popPeriod);

        DefaultProperty keepSafeOption = PropertiesEditor.getPropertyInstance("KEEP_SAFE", Boolean.class, this.keepSafe, true);
        keepSafeOption.setShortDescription(
                "If this flag is set, the vehicle will hold position underwater, loitering at z reference. It will popup periodically to report position. When it pops up, it will stay at surface in \"normal\" station keeping behaviour for a certain time (popup_duration).");     
        props.add(keepSafeOption);        

        return props;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);

        for (Property p : properties) {

            if (p.getName().equals("Duration")) {
                setDuration((Integer)p.getValue());
                continue;
            }

            if (p.getName().equals("Speed")) {
                setSpeed((SpeedType)p.getValue());
                continue;
            }

            if (p.getName().equals("Popup Duration")) {
                setPopupDuration((Integer)p.getValue());
                continue;
            }

            if (p.getName().equals("Popup Period")) {
                setPopupPeriod((Integer)p.getValue());
                continue;
            }
            
            if (p.getName().equals("KEEP_SAFE")) {
                setKeepSafe((Boolean)p.getValue());
                continue;
            }
            
            if (p.getName().equals("Radius")) {
                setRadius(Math.max(MINIMUM_SK_RADIUS, (Double)p.getValue()));
                continue;
            }
        }
    }

    @Override
    public String getTooltipText() {
        return super.getTooltipText()+"<hr>"+
                "<br>" + I18n.text("speed") + ": <b>"+speed+"</b>"+
                "<br>" + I18n.text("radius") + ": <b>"+radius+" " + I18n.textc("m", "meters") + "</b>"+
                "<br>" + I18n.text("duration") + ": <b>"+duration+" " + I18n.textc("s", "seconds") + "</b><br>";
    }

    public LocationType getLocation() {
        return location;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = Math.max(MINIMUM_SK_RADIUS, radius);
    }

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        AffineTransform at = g2d.getTransform();
        // x marks the spot...
        g2d.drawLine(-4, -4, 4, 4);
        g2d.drawLine(-4, 4, 4, -4);
        double radius = Math.max(MINIMUM_SK_RADIUS, this.getRadius()) * renderer.getZoom();
        g2d.setColor(new Color(255,255,255,100));
        g2d.fill(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
        g2d.setColor(Color.blue.darker());
        g2d.draw(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
        g2d.setTransform(at);
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        
        setMaxTime((int)message.getDouble("timeout"));

        ManeuverLocation pos = new ManeuverLocation();
        pos.setLatitudeRads(message.getDouble("lat"));
        pos.setLongitudeRads(message.getDouble("lon"));
        pos.setZ(message.getDouble("z"));
        String zunits = message.getString("z_units");
        if (zunits != null)
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(zunits));
        setManeuverLocation(pos);

        speed = SpeedType.parseImcSpeed(message);

        setDuration((int)message.getDouble("duration"));
        setRadius(message.getDouble("radius"));
        setCustomSettings(message.getTupleList("custom"));

        if (message.getMgid() == StationKeepingExtended.ID_STATIC) {
            setPopupDuration(message.getInteger("popup_duration"));
            setPopupPeriod(message.getInteger("popup_period"));
            setKeepSafe(true);
        }                
    }

    
    public pt.lsts.imc.StationKeeping serializetoRegularSKeeping() {
        pt.lsts.imc.StationKeeping message = new pt.lsts.imc.StationKeeping();
        LocationType loc = getManeuverLocation();
        loc.convertToAbsoluteLatLonDepth();
        message.setLat(loc.getLatitudeRads());
        message.setLon(loc.getLongitudeRads());
        message.setZ(getManeuverLocation().getZ());
        message.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));
        message.setDuration(getDuration());

        speed.setSpeedToMessage(message);

        message.setRadius(this.getRadius());
        message.setCustom(getCustomSettings());
        return message;
    }
    
    public pt.lsts.imc.StationKeepingExtended serializetoSafeSKeeping() {
        pt.lsts.imc.StationKeepingExtended message = new pt.lsts.imc.StationKeepingExtended();
  
        LocationType loc = getManeuverLocation();
        loc.convertToAbsoluteLatLonDepth();
        message.setLat(loc.getLatitudeRads());
        message.setLon(loc.getLongitudeRads());
        message.setZ(getManeuverLocation().getZ());
        message.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));
        message.setDuration(getDuration());
        message.setPopupDuration(getPopupDuration());
        message.setPopupPeriod(getPopupPeriod());
        
        speed.setSpeedToMessage(message);

        message.setRadius(this.getRadius());
        message.setCustom(getCustomSettings());

        return message;        
    }
    
    @Override
    public IMCMessage serializeToIMC() {
        if (isKeepSafe())
            return serializetoSafeSKeeping();
        else
            return serializetoRegularSKeeping();
        
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        double time = getDistanceTravelled(initialPosition) / speed.getMPS();

        return /*getDuration() == 0 ? Double.POSITIVE_INFINITY :*/ getDuration() + time;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.StatisticsProvider#getDistanceTravelled(pt.lsts.neptus.types.coord.LocationType)
     */
    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = getStartLocation().getDistanceInMeters(initialPosition);
        return meters;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.StatisticsProvider#getMaxDepth()
     */
    @Override
    public double getMaxDepth() {
        return getManeuverLocation().getAllZ();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.StatisticsProvider#getMinDepth()
     */
    @Override
    public double getMinDepth() {
        return getManeuverLocation().getAllZ();
    }   

    /**
     * @return the keepSafe
     */
    public boolean isKeepSafe() {
        return keepSafe;
    }

    /**
     * @param keepSafe the keepSafe to set
     */
    public void setKeepSafe(boolean keepSafe) {
        this.keepSafe = keepSafe;
    }

    /**
     * @return the popupPeriod
     */
    public int getPopupPeriod() {
        return popupPeriod;
    }

    /**
     * @param popupPeriod the popupPeriod to set
     */
    public void setPopupPeriod(int popupPeriod) {
        this.popupPeriod = popupPeriod;
    }

    /**
     * @return the popupDuration
     */
    public int getPopupDuration() {
        return popupDuration;
    }

    /**
     * @param popupDuration the popupDuration to set
     */
    public void setPopupDuration(int popupDuration) {
        this.popupDuration = popupDuration;
    }

    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
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
