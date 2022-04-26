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
 * 28/04/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 *
 */
public class ExpandingSquarePattern extends FollowPath {

    @NeptusProperty(name = "Width", description = "Width of the volume to cover, in meters", units = "m")
    private double width = 100;
    @NeptusProperty(name = "Horizontal Step", description = "Horizontal distance between rows, in meters", units = "m")
    private double hstep = 50;
    @NeptusProperty(name = "Bearing", description = "The outgoing bearing (from starting location) in degrees", units = "\u00B0")
    private double bearingDeg = 0;
    @NeptusProperty(name = "First Curve Right", description = "If the first curve should be to the right or left")
    private boolean firstCurveRight = true;
    
    static {
        registerPattern(ExpandingSquarePattern.class);
    }
    
    public ExpandingSquarePattern() {
        super();
        editingHelpText = I18n.text("Ctrl+Click to grow | Shift+Click to rotate");
        recalcPoints();
    }

    @Override
    public String getName() {
        return "ExpandingSquarePattern";
    }

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);

        double zoom = renderer.getZoom();
        g2d.rotate(-renderer.getRotation());
        g2d.rotate(-Math.PI/2);
        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, Math.toRadians(bearingDeg), 0, false, false, editing);
        ManeuversUtil.paintPointLineList(g2d, zoom, points, false, 0, editing);
        g2d.rotate(Math.PI/2);
        g2d.rotate(renderer.getRotation());
    }

    /**
     * Call this to update the maneuver points.
     */
    protected void recalcPoints() {
        Vector<double[]> newPoints = ManeuversUtil.calcExpansiveSquarePatternPointsMaxBox(width, hstep,
                Math.toRadians(bearingDeg), !firstCurveRight);
        points = newPoints;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocationProvider#getFirstPosition()
     */
    @Override
    public ManeuverLocation getStartLocation() {
        try {
            double[] first = points.firstElement();
            ManeuverLocation loc = getManeuverLocation().clone();
            loc.translatePosition(first[X], first[Y], first[Z]);
            return loc;
        }
        catch (Exception e) {
            return getManeuverLocation();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocationProvider#getLastPosition()
     */
    @Override
    public ManeuverLocation getEndLocation() {
        try {
            double[] last = points.lastElement();
            ManeuverLocation loc = getManeuverLocation().clone();
            loc.translatePosition(last[X], last[Y], last[Z]);
            return loc;
        }
        catch (Exception e) {
            return getManeuverLocation();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocationProvider#setPosition(pt.lsts.neptus.types.coord.AbstractLocationPoint)
     */
    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        super.setManeuverLocation(location);
        recalcPoints();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseClicked(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        adapter.mouseClicked(event, source);        
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
        double xammount = event.getPoint().getX() - lastDragPoint.getX();
        double yammount = event.getPoint().getY() - lastDragPoint.getY();
        yammount = -yammount;
        if (event.isControlDown()) {
            double norm = Math.sqrt(xammount * xammount + yammount * yammount);
            double angle = AngleUtils.calcAngle(lastDragPoint.getY(), lastDragPoint.getX(), event.getPoint().getY(),
                    event.getPoint().getX());
            double nx = norm * Math.cos(Math.toDegrees(bearingDeg) - angle);
            // double ny = norm * Math.sin(Math.toDegrees(bearingDeg) - angle);

            width += nx / (Math.abs(nx) < 30 ? 10 : 2);
            width = MathMiscUtils.round(width, 1);
            width = Math.max(1, width);
            recalcPoints();
        }
        else if (event.isShiftDown()) {
            bearingDeg = MathMiscUtils.round(Math.toDegrees(Math.toRadians(bearingDeg) 
                    + Math.toRadians(yammount / (Math.abs(yammount) < 30 ? 10 : 2))), 2);

            while (bearingDeg > 360)
                bearingDeg -= 360;            
            while (bearingDeg < 0)
                bearingDeg += 360;
            recalcPoints();
        }
        else {
            adapter.mouseDragged(event, source);
        }
        lastDragPoint = event.getPoint();
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        super.translate(offsetNorth, offsetEast, offsetDown);
        recalcPoints();
    }

    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            Node node = doc.selectSingleNode("//basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);

            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
            bearingDeg = Double.parseDouble(doc.selectSingleNode("//bearing").getText());

            // area
            width = Math.abs(Double.parseDouble(doc.selectSingleNode("//width").getText()));
            width = width <= 0 ? 1 : width;
            //steps
            hstep = Math.abs(Double.parseDouble(doc.selectSingleNode("//hstep").getText()));
            hstep = hstep <= 0 ? 1 : hstep;

            node = doc.selectSingleNode("//firstCurveRight");
            if (node != null)
                firstCurveRight = Boolean.parseBoolean(node.getText());
            else
                firstCurveRight = true;
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
        finally {
            recalcPoints();
        }
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
        basePoint.addAttribute("type", "pointType");

        root.addElement("width").setText(""+width);
        root.addElement("hstep").setText(""+hstep);
        root.addElement("bearing").setText("" + bearingDeg);

        if (!firstCurveRight)
            root.addElement("firstCurveRight").setText(""+firstCurveRight);

        SpeedType.addSpeedElement(root, this);

        return document;
    }

    @Override
    public IMCMessage serializeToIMC() {
        IMCMessage msg = super.serializeToIMC();

        LinkedHashMap<String, Object> customValues = new LinkedHashMap<String, Object>();
        customValues.put("Pattern", getName());
        customValues.put("bearingRad", Math.toRadians(bearingDeg));
        customValues.put("width", width);
        customValues.put("hstep", hstep);
        customValues.put("firstCurveRight", firstCurveRight);
        String customValuesTL = IMCMessage.encodeTupleList(customValues);
        msg.setValue("custom", customValuesTL);

        return msg;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        super.parseIMCMessage(message);

        LinkedHashMap<String, String> customValues = customSettings; // message.getTupleList("custom");

        String pattern = customValues.remove("Pattern");
        if (!getName().equalsIgnoreCase(pattern))
            return;

        String value = customValues.remove("bearingRad");
        try {
            bearingDeg = Math.toDegrees(Double.parseDouble(value));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value = customValues.remove("width");
        try {
            width = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value = customValues.remove("hstep");
        try {
            hstep = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value = customValues.remove("firstCurveRight");
        firstCurveRight = Boolean.parseBoolean(value);

        recalcPoints();
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = ManeuversUtil.getPropertiesFromManeuver(this);

        DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, this.speed, true);
        speed.setShortDescription(I18n.text("The vehicle's desired speed"));
        props.add(speed);

        return props;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        ManeuversUtil.setPropertiesToManeuver(this, properties);
        recalcPoints();
    }

    public String validateWidth(double value) {
        if (value <= 0)
            return "Keep it above 0";
        return null;
    }

    // Validate for additional parameters
    public String validateHstep(double value) {
        if (value <= 0)
            return "Keep it above 0";
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#clone()
     */
    @Override
    public Object clone() {
        ExpandingSquarePattern clone = (ExpandingSquarePattern) super.clone();
        clone.bearingDeg = bearingDeg;
        clone.width = width;
        clone.hstep = hstep;

        clone.firstCurveRight = firstCurveRight;

        clone.recalcPoints();
        return clone;
    }
}
