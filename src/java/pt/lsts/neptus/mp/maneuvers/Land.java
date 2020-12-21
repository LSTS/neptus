/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
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
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.XMLUtil;
import pt.lsts.neptus.util.conf.IntegerMinMaxValidator;

/**
 * @author pdias
 *
 */
public class Land extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization, StateRendererInteraction {
    
    protected double latDegs = 0;
    protected double lonDegs = 0;
    protected double z = 0;
    protected ManeuverLocation.Z_UNITS zUnits = ManeuverLocation.Z_UNITS.NONE;

    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected Point2D lastDragPoint = null;
    protected boolean editing = false;

    @NeptusProperty(name = "Abort Z", description = "Abort altitude or height. If landing is aborted while executing, the UAV will maintain its course and attempt to climb to the abort z reference.", units = "m")
    protected double zAbort = 20;
    @NeptusProperty(name = "Speed")
    protected SpeedType speed = new SpeedType(17, Units.MPS);
    @NeptusProperty(name = "Bearing", description = "Land bearing angle.", units = "\u00B0")
    protected double bearingDegs = 0;
    @NeptusProperty(name = "Glide Slope", description = "Ratio (%) of the distance from the last waypoint to the landing point (touchdown) and the height difference between them.", units = "%")
    protected short glideSlope = 10;
    @NeptusProperty(name = "Glide Slope Altitude", description = "Height difference between the last waypoint to the landing point (touchdown).", units = "m")
    protected float glideSlopeAltitude = 10;

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

    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        ArrayList<ManeuverLocation> wps = new ArrayList<ManeuverLocation>();
        wps.add(getManeuverLocation());
        return wps;
    }

    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
    
            ManeuversXMLUtil.parseLocation(doc.getRootElement(), this);
            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
            
            Node node = doc.selectSingleNode("//bearing");
            if (node != null)
                bearingDegs = Double.parseDouble(node.getText());

            node = doc.selectSingleNode("//glideSlope");
            if (node != null)
                glideSlope = Short.parseShort(node.getText());
            
            node = doc.selectSingleNode("//glideSlopeAltitude");
            if (node != null)
                glideSlopeAltitude = Float.parseFloat(node.getText());

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document doc = ManeuversXMLUtil.createBaseDoc(getType());
        ManeuversXMLUtil.addLocation(doc.getRootElement(), this);
        SpeedType.addSpeedElement(doc.getRootElement(), this);

        Element root = doc.getRootElement();

        root.addElement("bearing").setText(String.valueOf(bearingDegs));

        root.addElement("glideSlope").setText(String.valueOf(glideSlope));
        root.addElement("glideSlopeAltitude").setText(String.valueOf(glideSlopeAltitude));

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

    @Override
    public Object clone() {
        Land clone = new Land();
        super.clone(clone);
        clone.latDegs = latDegs;
        clone.lonDegs = lonDegs;
        clone.z = z;
        clone.zUnits = zUnits;
        clone.speed = getSpeed();
        clone.bearingDegs = bearingDegs;
        clone.glideSlope = glideSlope;
        clone.glideSlopeAltitude = glideSlopeAltitude;
        return clone;
    }

    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.Land man = new pt.lsts.imc.Land();
        man.setLat(Math.toRadians(latDegs));
        man.setLon(Math.toRadians(lonDegs));
        man.setZ(z);
        man.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));        
        speed.setSpeedToMessage(man);
        man.setAbortZ(zAbort);
        man.setBearing(Math.toRadians(bearingDegs));
        man.setGlideSlope(glideSlope);
        man.setGlideSlopeAlt(glideSlopeAltitude);
        
        return man;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        pt.lsts.imc.Land man = null;
        try {
            man = pt.lsts.imc.Land.clone(message);
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
        zAbort = man.getAbortZ();
        bearingDegs = Math.toDegrees(man.getBearing());
        glideSlope = man.getGlideSlope();
        glideSlopeAltitude = (float) man.getGlideSlopeAlt();
    }

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        
        if (editing) {
            Graphics2D g3 = (Graphics2D) g2d.create();
            Point2D manL = renderer.getScreenPosition(getManeuverLocation());
            Point2D gL = renderer.getScreenPosition(renderer.getTopLeftLocationType());
            g3.translate(gL.getX() - manL.getX(), gL.getY() - manL.getY());
            g3.setFont(new Font("Helvetica", Font.BOLD, 13));
            String txt = I18n.text("Shift+Click to rotate");
            g3.setColor(Color.BLACK);
            g3.drawString(txt, 55, 15 + 20);
            g3.setColor(COLOR_HELP);
            g3.drawString(txt, 54, 14 + 20);
            g3.dispose();
        }

        ManeuverLocation startLLoc = getStartLocation();
        
        double[] dp = getEndLocation().getDistanceInPixelTo(startLLoc, renderer.getLevelOfDetail());
        
        Color color1 = new Color(210, 176, 106); // KHAKI
        Color color2 = new Color(0, 0, 0, 106);
        
        float distance = glideSlope * glideSlopeAltitude;

        Stroke sO = g2d.getStroke();
        Stroke s1 = new BasicStroke(2);
        Stroke s2 = new BasicStroke(16);

        dp = AngleUtils.rotate(renderer.getRotation(), dp[0], dp[1], true);
        
        g2d.setColor(color2);
        g2d.setStroke(s2);
        g2d.draw(new Line2D.Double(0, 0, dp[0], dp[1]));
        g2d.setColor(Color.WHITE);
        g2d.setStroke(s1);
        g2d.draw(new Line2D.Double(0, 0, dp[0], dp[1]));
        g2d.setStroke(sO);
        
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
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getName()
     */
    @Override
    public String getName() {
        return getType();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getIconImage()
     */
    @Override
    public Image getIconImage() {
        return adapter.getIconImage();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getMouseCursor()
     */
    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        return true;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseClicked(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        adapter.mouseClicked(event, source);        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mousePressed(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
        lastDragPoint = event.getPoint();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseDragged(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (lastDragPoint == null) {
            adapter.mouseDragged(event, source);
            lastDragPoint = event.getPoint();
            return;
        }
        // double xammount = event.getPoint().getX() - lastDragPoint.getX();
        double yammount = event.getPoint().getY() - lastDragPoint.getY();
        yammount = -yammount;
        if (event.isShiftDown()) {
            bearingDegs += yammount / (Math.abs(yammount) < 30 ? 10 : 2);
            bearingDegs = (int) (bearingDegs * 10) / 10.;
            bearingDegs = AngleUtils.nomalizeAngleDegrees360(bearingDegs);
        }
        else {
            adapter.mouseDragged(event, source);
        }
        lastDragPoint = event.getPoint();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseMoved(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseReleased(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
        lastDragPoint = null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#wheelMoved(java.awt.event.MouseWheelEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyPressed(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyReleased(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyTyped(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseExited(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#focusGained(java.awt.event.FocusEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#focusLost(java.awt.event.FocusEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#setActive(boolean, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        editing = mode;
        adapter.setActive(mode, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#setAssociatedSwitch(pt.lsts.neptus.gui.ToolbarSwitch)
     */
    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#paintInteraction(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
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
        Land rc = new Land();
        System.out.println(XMLUtil.getAsPrettyPrintFormatedXMLString(rc.asXML().substring(39)));
    }
}
