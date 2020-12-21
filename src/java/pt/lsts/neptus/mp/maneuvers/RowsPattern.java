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
 * 15/09/2011
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
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 *
 */
public class RowsPattern extends FollowPath {

    protected double bearingRad = 0, width = 100, length = 200, hstep = 27, sRange = 30;
    protected double crossAngleRadians = 0;
    protected double curvOff = 15;
    protected float alternationPercentage = 1.0f;
    protected boolean squareCurve = true, firstCurveRight = true;

    protected boolean ignoreLength = false, ignoreCrossAngle = false, ignoreAlternationPercentage = false,
            ignoreFirstCurveRight = false;
    protected boolean paintOnlyBasePoint = false;

    public RowsPattern() {
        super();
        editingHelpText = I18n.text("Ctrl+Click to grow | Shift+Click to rotate");
        recalcPoints();
    }

    @Override
    public String getName() {
        return "RowsPattern";
    }

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        if (paintOnlyBasePoint)
            return;

        double zoom = renderer.getZoom();
        g2d.rotate(-renderer.getRotation());
        g2d.rotate(-Math.PI/2);
        ManeuversUtil.paintBox(g2d, zoom, width, length, 0, 0, bearingRad, crossAngleRadians, false, !firstCurveRight, editing);
        ManeuversUtil.paintPointLineList(g2d, zoom, points, true, sRange, editing); // FIXME
        //        ManeuversUtil.paintPointLineList(g2d, zoom, points, false, sRange);
        g2d.rotate(Math.PI/2);
        g2d.rotate(renderer.getRotation());
    }


    /**
     * Call this to update the maneuver points.
     */
    protected void recalcPoints() {
        Vector<double[]> newPoints = ManeuversUtil.calcRowsPoints(width, length, hstep,
                alternationPercentage, curvOff, squareCurve, bearingRad, crossAngleRadians, !firstCurveRight);
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

    /**
     * @param width
     * @param hstep
     * @param alternationPercent
     * @param curvOff
     * @param squareCurve
     * @param bearingRad
     * @param crossAngleRadians
     * @param firstCurveRight
     */
    public void setParams(double width, double hstep,
            double alternationPercent, double curvOff, boolean squareCurve, double bearingRad,
            double crossAngleRadians, boolean firstCurveRight) {
        this.width = width;
        this.hstep = hstep;
        this.alternationPercentage = Double.valueOf(alternationPercent).floatValue();
        this.curvOff = curvOff;
        this.squareCurve = squareCurve;
        this.bearingRad = bearingRad;
        this.crossAngleRadians = crossAngleRadians;
        this.firstCurveRight = firstCurveRight;

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
            double nx = norm * Math.cos(bearingRad - angle);
            double ny = norm * Math.sin(bearingRad - angle);

            width += nx / (Math.abs(nx) < 30 ? 10 : 2);
            width = MathMiscUtils.round(width, 1);
            width = Math.max(1, width);
            if (!ignoreLength) {
                length += ny / (Math.abs(ny) < 30 ? 10 : 2);
                length = MathMiscUtils.round(length, 1);
                length = Math.max(1, length);
            }
            recalcPoints();
        }
        else if (event.isShiftDown()) {
            bearingRad += Math.toRadians(yammount / (Math.abs(yammount) < 30 ? 10 : 2));

            while (bearingRad > Math.PI * 2)
                bearingRad -= Math.PI * 2;            
            while (bearingRad < 0)
                bearingRad += Math.PI * 2;
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
        // TODO FIXME 
        try {
            Document doc = DocumentHelper.parseText(xml);
            //NeptusLog.pub().info("<###> "+doc.asXML());
            // basePoint
            Node node = doc.selectSingleNode("//basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);

            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);

            bearingRad = Math.toRadians(Double.parseDouble(doc.selectSingleNode("//bearing").getText()));

            // area
            width = Math.abs(Double.parseDouble(doc.selectSingleNode("//width").getText()));
            if (!ignoreLength) {
                node = doc.selectSingleNode("//length");
                if (node != null)
                    length = Math.abs(Double.parseDouble(node.getText()));
                else
                    length = width;
            }
            //steps
            hstep = Math.abs(Double.parseDouble(doc.selectSingleNode("//hstep").getText()));
            hstep = hstep <= 0 ? 1 : hstep;

            if (!ignoreCrossAngle) {
                node = doc.selectSingleNode("//crossAngle");
                if (node != null)
                    crossAngleRadians = Math.toRadians(Double.parseDouble(node.getText()));
                else
                    crossAngleRadians = 0;
            }
            else
                crossAngleRadians = 0;

            node = doc.selectSingleNode("//alternationPercentage");
            if (node != null)
                alternationPercentage = Short.parseShort(node.getText())/100f;
            else
                alternationPercentage = 1;

            node = doc.selectSingleNode("//curveOffset");
            if (node != null)
                curvOff = Double.parseDouble(node.getText());
            else
                curvOff = 15;

            node = doc.selectSingleNode("//squareCurve");
            if (node != null)
                squareCurve = Boolean.parseBoolean(node.getText());
            else
                squareCurve = true;

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
        // TODO FIXME 
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        root.addAttribute("kind", "automatic");

        //basePoint
        Element basePoint = root.addElement("basePoint");
        Element point = getManeuverLocation().asElement("point");
        basePoint.add(point);
        Element radTolerance = basePoint.addElement("radiusTolerance");
        radTolerance.setText(String.valueOf(getRadiusTolerance()));    
        basePoint.addAttribute("type", "pointType");

        root.addElement("width").setText(""+width);
        if (!ignoreLength)
            root.addElement("length").setText(""+length);
        root.addElement("hstep").setText(""+hstep);
        root.addElement("bearing").setText(""+Math.toDegrees(bearingRad));

        if (!ignoreCrossAngle) {
            if (crossAngleRadians != 0)
                root.addElement("crossAngle").setText(""+Math.toDegrees(crossAngleRadians));
        }

        if ((short)(alternationPercentage*100f) != 100)
            root.addElement("alternationPercentage").setText(""+(short)(alternationPercentage*100f));

        if (curvOff != 15)
            root.addElement("curveOffset").setText(""+curvOff);

        if (!squareCurve)
            root.addElement("squareCurve").setText(""+squareCurve);

        if (!ignoreFirstCurveRight)
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
        customValues.put("bearingRad", bearingRad);
        customValues.put("width", width);
        if (!ignoreLength)
            customValues.put("length", length);
        customValues.put("hstep", hstep);
        customValues.put("sRange", sRange);
        if (!ignoreCrossAngle)
            customValues.put("crossAngleRadians", crossAngleRadians);
        customValues.put("curvOff", curvOff);
        if (!ignoreAlternationPercentage)
            customValues.put("alternationPercentage", alternationPercentage);
        customValues.put("squareCurve", squareCurve);
        if (!ignoreFirstCurveRight)
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
            bearingRad = Double.parseDouble(value);
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

        value = customValues.remove("length");
        if (!ignoreLength) {
            try {
                length = Double.parseDouble(value);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        value = customValues.remove("hstep");
        try {
            hstep = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value = customValues.remove("sRange");
        try {
            sRange = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value = customValues.remove("crossAngleRadians");
        if (!ignoreCrossAngle) {
            try {
                crossAngleRadians = Double.parseDouble(value);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        value = customValues.remove("curvOff");
        try {
            curvOff = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value = customValues.remove("alternationPercentage");
        if (!ignoreAlternationPercentage) {
            try {
                alternationPercentage = (float) Float.parseFloat(value);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        value = customValues.remove("squareCurve");
        squareCurve = Boolean.parseBoolean(value);

        value = customValues.remove("firstCurveRight");
        if (!ignoreFirstCurveRight) {
            firstCurveRight = Boolean.parseBoolean(value);
        }

        recalcPoints();
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);

        for (Property p : properties) {

            if (p.getName().equals("Width")) {
                width = (Double)p.getValue();
                width = width < 0 ? 1 : width;
                continue;
            }

            if (!ignoreLength) {
                if (p.getName().equals("Length")) {
                    length = (Double)p.getValue();
                    length = length < 0 ? 1 : length;
                    continue;
                }
            }

            if (p.getName().equals("Horizontal Alternation")) {
                alternationPercentage = ((Short) p.getValue()) / 100f;
                continue;
            }

            if (p.getName().equals("Speed")) {
                speed = (SpeedType)p.getValue();
                continue;
            }

            if (p.getName().equals("Bearing")) {
                bearingRad = Math.toRadians((Double)p.getValue());
                continue;
            }

            if (p.getName().equals("Cross Angle")) {
                crossAngleRadians = Math.toRadians((Double)p.getValue());
                continue;
            }

            if (p.getName().equals("Horizontal Step")) {
                hstep = (Double)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("Curve Offset")) {
                curvOff = (Double)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("Square Curve")) {
                squareCurve = (Boolean)p.getValue();
                continue;
            }

            if (!ignoreFirstCurveRight) {
                if (p.getName().equalsIgnoreCase("First Curve Right")) {
                    firstCurveRight = (Boolean)p.getValue();
                    continue;
                }
            }            
        }
        recalcPoints();
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();

        if (!ignoreLength) {
            DefaultProperty length = PropertiesEditor.getPropertyInstance("Length", Double.class, this.length, true);
            length.setShortDescription(I18n.text("The length of the volume to cover, in meters") + "<br/>(m)");
            props.add(length);
        }

        DefaultProperty width = PropertiesEditor.getPropertyInstance("Width", Double.class, this.width, true);
        width.setShortDescription(I18n.text("Width of the volume to cover, in meters") + "<br/>(m)");
        props.add(width);
        
        if (!ignoreAlternationPercentage) {
            DefaultProperty halt = PropertiesEditor.getPropertyInstance("Horizontal Alternation", Short.class, (short)(this.alternationPercentage*100), true);
            halt.setShortDescription(I18n
                    .text("Horizontal alternation in percentage. 100 will make all rows separated by the Horizontal Step")
                    + "<br/>(%)");
            props.add(halt);
        }

        DefaultProperty hstep = PropertiesEditor.getPropertyInstance("Horizontal Step", Double.class, this.hstep, true);
        hstep.setShortDescription(I18n.text("Horizontal distance between rows, in meters") + "<br/>(m)");
        props.add(hstep);

        DefaultProperty direction = PropertiesEditor.getPropertyInstance("Bearing", Double.class, Math.toDegrees(bearingRad), true);
        direction.setShortDescription(I18n.text("The outgoing bearing (from starting location) in degrees") + "<br/>(\u00B0)");       
        props.add(direction);

        if (!ignoreCrossAngle) {
            DefaultProperty cross = PropertiesEditor.getPropertyInstance("Cross Angle", Double.class, Math.toDegrees(crossAngleRadians), true);
            cross.setShortDescription(I18n.text("The tilt angle of the search box in degrees") + "<br/>(\u00B0)");
            props.add(cross);
        }

        DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, this.speed, true);
        speed.setShortDescription(I18n.text("The vehicle's desired speed"));
        props.add(speed);

        DefaultProperty curvOffset = PropertiesEditor.getPropertyInstance("Curve Offset", Double.class, curvOff, true);
        curvOffset.setShortDescription(I18n.text("The extra length to use for the curve") + "<br/>(m)");
        props.add(curvOffset);

        DefaultProperty squareCurveP = PropertiesEditor.getPropertyInstance("Square Curve", Boolean.class, squareCurve, true);
        squareCurveP.setShortDescription(I18n.text("If the curve should be square or direct"));
        props.add(squareCurveP);

        if (!ignoreFirstCurveRight) {
            DefaultProperty firstCurveRightP = PropertiesEditor.getPropertyInstance("First Curve Right", Boolean.class, firstCurveRight, true);
            firstCurveRightP.setShortDescription(I18n.text("If the first curve should be to the right or left"));       
            props.add(firstCurveRightP);
        }

        return props;
    }

    // Validate for additional parameters
    public String validateLength(double value) {
        if (value <= 0)
            return "Keep it above 0";
        return null;
    }

    // Validate for additional parameters
    public String validateWidth(double value) {
        if (value <= 0)
            return "Keep it above 0";
        return null;
    }

    // Validate for additional parameters
    public String validateHorizontalStep(double value) {
        if (value <= 0)
            return "Keep it above 0";
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#clone()
     */
    @Override
    public Object clone() {
        RowsPattern clone = (RowsPattern) super.clone();
        //        super.clone(clone);
        clone.bearingRad = bearingRad;
        clone.width = width;
        clone.length = length;
        clone.hstep = hstep;

        //        clone.speed = speed;
        //        clone.speed_units = speed_units;

        clone.alternationPercentage = alternationPercentage;
        clone.crossAngleRadians = crossAngleRadians;
        clone.curvOff = curvOff;
        clone.squareCurve = squareCurve;
        clone.sRange = sRange;
        clone.firstCurveRight = firstCurveRight;

        //        clone.setPosition(getPosition().getNewAbsoluteLatLonDepth());

        clone.recalcPoints();
        return clone;
    }

    public static void main(String[] args) {
        RowsPattern man = new RowsPattern();
        //man("<FollowPath kind=\"automatic\"><basePoint type=\"pointType\"><point><id>id_53802104</id><name>id_53802104</name><coordinate><latitude>0N0'0''</latitude><longitude>0E0'0''</longitude><depth>0.0</depth></coordinate></point><radiusTolerance>0.0</radiusTolerance></basePoint><path><nedOffsets northOffset=\"0.0\" eastOffset=\"1.0\" depthOffset=\"2.0\" timeOffset=\"3.0\"/><nedOffsets northOffset=\"4.0\" eastOffset=\"5.0\" depthOffset=\"6.0\" timeOffset=\"7.0\"/></path><speed unit=\"RPM\">1000.0</speed></FollowPath>");
        //NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(man.getManeuverAsDocument("FollowTrajectory")));
        man.setSpeed(new SpeedType(1, Units.MPS));
        man.setSpeed(new SpeedType(2, Units.MPS));

        MissionType mission = new MissionType("./missions/rep10/rep10.nmisz");
        StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(mission));
        PlanElement pelem = new PlanElement(MapGroup.getMapGroupInstance(mission), null);
        PlanType plan = new PlanType(mission);
        man.getManeuverLocation().setLocation(r2d.getCenter());
        plan.getGraph().addManeuver(man);        
        pelem.setPlan(plan);
        r2d.addPostRenderPainter(pelem, "Plan");
        GuiUtils.testFrame(r2d);
        RowsManeuver.unblockNewRows = true;
        //      PropertiesEditor.editProperties(man, true);

    }

}
