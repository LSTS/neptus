/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.editor.SpeedUnitsEditor;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

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
        ManeuversUtil.paintBox(g2d, zoom, width, length, 0, 0, bearingRad, crossAngleRadians, !firstCurveRight, editing);
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
            width += xammount / (Math.abs(yammount) < 30 ? 10 : 2);
            width = Math.max(1, width);
            if (!ignoreLength) {
                length += yammount / (Math.abs(yammount) < 30 ? 10 : 2);
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
    public void loadFromXML(String xml) {
        // TODO FIXME 
        try {
            Document doc = DocumentHelper.parseText(xml);
            //NeptusLog.pub().info("<###> "+doc.asXML());
            // basePoint
            Node node = doc.selectSingleNode("//basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);
            //            double latRad = getPosition().getLatitudeAsDoubleValueRads();
            //            double lonRad = getPosition().getLongitudeAsDoubleValueRads();
            //            double z = getPosition().getDepth();            

            // Speed
            Node speedNode = doc.selectSingleNode("//speed");
            speed = Double.parseDouble(speedNode.getText());
            speed_units = speedNode.valueOf("@unit");

            bearingRad = Math.toRadians(Double.parseDouble(doc.selectSingleNode("//bearing").getText()));

            // area
            width = Double.parseDouble(doc.selectSingleNode("//width").getText());
            if (!ignoreLength) {
                node = doc.selectSingleNode("//length");
                if (node != null)
                    length = Double.parseDouble(node.getText());
                else
                    length = width;
            }
            //steps
            hstep = Double.parseDouble(doc.selectSingleNode("//hstep").getText());

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

        //speed
        Element speedElem = root.addElement("speed");        
        speedElem.addAttribute("unit", speed_units);
        speedElem.setText(""+speed);

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

        LinkedHashMap<String, String> customValues = message.getTupleList("custom");

        String pattern = customValues.get("Pattern");
        if (!getName().equalsIgnoreCase(pattern))
            return;

        String value = customValues.get("bearingRad");
        try {
            bearingRad = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value = customValues.get("width");
        try {
            width = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (!ignoreLength) {
            value = customValues.get("length");
            try {
                length = Double.parseDouble(value);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        value = customValues.get("hstep");
        try {
            hstep = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        value = customValues.get("sRange");
        try {
            sRange = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (!ignoreCrossAngle) {
            value = customValues.get("crossAngleRadians");
            try {
                crossAngleRadians = Double.parseDouble(value);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        value = customValues.get("curvOff");
        try {
            curvOff = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (!ignoreAlternationPercentage) {
            value = customValues.get("alternationPercentage");
            try {
                alternationPercentage = (float) Float.parseFloat(value);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        value = customValues.get("squareCurve");
        squareCurve = Boolean.parseBoolean(value);

        if (!ignoreFirstCurveRight) {
            value = customValues.get("firstCurveRight");
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
                continue;
            }

            if (!ignoreLength) {
                if (p.getName().equals("Length")) {
                    length = (Double)p.getValue();
                    continue;
                }
            }

            //            if (p.getName().equals("Height")) {
            //                height = (Double)p.getValue();
            //                continue;
            //            }

            if (p.getName().equals("Horizontal Alternation")) {
                alternationPercentage = ((Short) p.getValue()) / 100f;
                continue;
            }

            if (p.getName().equals("Speed")) {
                speed = (Double)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("Speed Units")) {
                speed_units = (String)p.getValue();
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

            //            if (p.getName().equals("Vertical Step")) {
            //                vstep = (Double)p.getValue();
            //                continue;
            //            }

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
            length.setShortDescription("The length of the volume to cover, in meters");
            props.add(length);
        }

        DefaultProperty width = PropertiesEditor.getPropertyInstance("Width", Double.class, this.width, true);
        width.setShortDescription("Width of the volume to cover, in meters");
        props.add(width);

        //        DefaultProperty height = PropertiesEditor.getPropertyInstance("Height", Double.class, this.height, false);
        //        height.setShortDescription("Height of the volume to cover, in meters. Use 0 for 2D rows");
        //        props.add(height);        

        DefaultProperty halt = PropertiesEditor.getPropertyInstance("Horizontal Alternation", Short.class, (short)(this.alternationPercentage*100), true);
        halt.setShortDescription("Horizontal alternation in percentage. 100 will make all rows separated by the Horizontal Step");
        props.add(halt);

        DefaultProperty hstep = PropertiesEditor.getPropertyInstance("Horizontal Step", Double.class, this.hstep, true);
        hstep.setShortDescription("Horizontal distance between rows, in meters");
        props.add(hstep);

        //        DefaultProperty vstep = PropertiesEditor.getPropertyInstance("Vertical Step", Double.class, this.vstep, false);
        //        vstep.setShortDescription("Vertical distance between rows, in meters");
        //        props.add(vstep);

        DefaultProperty direction = PropertiesEditor.getPropertyInstance("Bearing", Double.class, Math.toDegrees(bearingRad), true);
        direction.setShortDescription("The outgoing bearing (from starting location) in degrees");       
        props.add(direction);

        if (!ignoreCrossAngle) {
            DefaultProperty cross = PropertiesEditor.getPropertyInstance("Cross Angle", Double.class, Math.toDegrees(crossAngleRadians), true);
            cross.setShortDescription("The tilt angle of the search box in degrees");       
            props.add(cross);
        }

        DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", Double.class, this.speed, true);
        speed.setShortDescription("The vehicle's desired speed");
        props.add(speed);

        DefaultProperty speedUnits = PropertiesEditor.getPropertyInstance("Speed Units", String.class, speed_units, true);
        speedUnits.setShortDescription("The units to consider in the speed parameters");
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(speedUnits, new SpeedUnitsEditor());
        PropertiesEditor.getPropertyRendererRegistry().registerRenderer(speedUnits, new I18nCellRenderer());
        props.add(speedUnits);

        DefaultProperty curvOffset = PropertiesEditor.getPropertyInstance("Curve Offset", Double.class, curvOff, true);
        curvOffset.setShortDescription("The extra length to use for the curve");       
        props.add(curvOffset);

        DefaultProperty squareCurveP = PropertiesEditor.getPropertyInstance("Square Curve", Boolean.class, squareCurve, true);
        squareCurveP.setShortDescription("If the curve should be square or direct");       
        props.add(squareCurveP);

        if (!ignoreFirstCurveRight) {
            DefaultProperty firstCurveRightP = PropertiesEditor.getPropertyInstance("First Curve Right", Boolean.class, firstCurveRight, true);
            firstCurveRightP.setShortDescription("If the first curve should be to the right or left");       
            props.add(firstCurveRightP);
        }

        //        for (DefaultProperty p : props) {
        //            NeptusLog.pub().info("<###>* "+p.getName()+"="+p.getValue());
        //        }

        return props;
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
        man.setSpeed(1);
        man.setSpeedUnits("m/s");        
        //        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(man.getManeuverAsDocument("RIPattern")));

        man.setSpeed(2);
        man.setSpeedUnits("m/s");        
        //        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(man.getManeuverAsDocument("RIPattern")));


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
