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
 * 19/05/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Rows;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.XMLUtil;

/**
 * @author pdias
 */
public class RowsCoverage extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, StateRendererInteraction,
IMCSerialization, StatisticsProvider, PathProvider {

    protected double latDegs = 0;
    protected double lonDegs = 0;
    protected double z = 0;
    protected ManeuverLocation.Z_UNITS zunits = ManeuverLocation.Z_UNITS.NONE;

    @NeptusProperty(name = "Speed")
    protected SpeedType speed = new SpeedType(1, Units.MPS);
    @NeptusProperty(name = "Bearing", units = "\u00B0")
    protected double bearingDegs = 0;
    @NeptusProperty(name = "Width", units = "m")
    protected double width = 100;
    @NeptusProperty(name = "Length", units = "m")
    protected double length = 200;
    @NeptusProperty(name = "Cross Angle", units = "\u00B0")
    protected double crossAngleDegs = 0;
    @NeptusProperty(name = "Curve Offset", units = "m")
    protected double curvOff = 15;
    @NeptusProperty(name = "Angle Aperture", units = "\u00B0")
    protected double angleApertureDegs = 120;
    @NeptusProperty(name = "Max. Range", units = "m")
    protected int range = 30;
    @NeptusProperty(name = "Overlap Percentage", units = "%")
    protected short overlapPercentage = 0;
    @NeptusProperty(name = "Square Curve")
    protected boolean squareCurve = true;
    @NeptusProperty(name = "First Curve Right")
    protected boolean firstCurveRight = true;

    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected Point2D lastDragPoint = null;

    protected boolean editing = false;

    protected Vector<double[]> points = new Vector<double[]>();

    public RowsCoverage() {
        super();
        recalcPoints();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#getType()
     */
    @Override
    public String getType() {
        return "RowsCoverage";
    }
    
    protected ManeuverLocation calculatePosition() {
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitudeDegs(latDegs);
        loc.setLongitudeDegs(lonDegs);
        loc.setZ(z);
        loc.setZUnits(zunits);
        return loc;
    }

    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            
            ManeuversXMLUtil.parseLocation(doc.getRootElement(), this);
            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);

            bearingDegs = Double.parseDouble(doc.selectSingleNode("//bearing").getText());

            // area
            width = Math.abs(Double.parseDouble(doc.selectSingleNode("//width").getText()));
            Node node = doc.selectSingleNode("//length");
            if (node != null)
                length = Math.abs(Double.parseDouble(node.getText()));
            else
                length = width;

            node = doc.selectSingleNode("//crossAngle");
            if (node != null)
                crossAngleDegs = Double.parseDouble(node.getText());
            else
                crossAngleDegs = 0;

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

            node = doc.selectSingleNode("//angleAperture");
            if (node != null)
                angleApertureDegs = Double.parseDouble(node.getText());
            else
                angleApertureDegs = 120;

            node = doc.selectSingleNode("//range");
            if (node != null)
                range = Short.parseShort(node.getText());
            else
                range = 30;

            node = doc.selectSingleNode("//overlapPercentage");
            if (node != null)
                overlapPercentage = Short.parseShort(node.getText());
            else
                overlapPercentage = 0;
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
    public Object clone() {
        RowsCoverage clone = new RowsCoverage();
        super.clone(clone);
        clone.latDegs = latDegs;
        clone.lonDegs = lonDegs;
        clone.z = z;
        clone.zunits = zunits;
        clone.bearingDegs = bearingDegs;
        clone.length = length;
        clone.width = width;
        clone.speed = getSpeed();
        

        clone.angleApertureDegs = angleApertureDegs;
        clone.range = range;
        
        clone.overlapPercentage = overlapPercentage;
        clone.crossAngleDegs = crossAngleDegs;
        clone.curvOff = curvOff;
        clone.squareCurve = squareCurve;
        clone.firstCurveRight = firstCurveRight;

        clone.recalcPoints();
        return clone;
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document doc = ManeuversXMLUtil.createBaseDoc(getType());
        ManeuversXMLUtil.addLocation(doc.getRootElement(), this);
        SpeedType.addSpeedElement(doc.getRootElement(), this);

        Element root = doc.getRootElement();

        root.addElement("width").setText(""+width);
        root.addElement("length").setText(""+length);
        root.addElement("bearing").setText("" + bearingDegs);

        if (crossAngleDegs != 0)
            root.addElement("crossAngle").setText("" + crossAngleDegs);

        if (angleApertureDegs != 0)
            root.addElement("angleAperture").setText("" + angleApertureDegs);

        if (range != 0)
            root.addElement("range").setText("" + range);

        if (overlapPercentage != 0)
            root.addElement("overlapPercentage").setText("" + overlapPercentage);

        if (curvOff != 15)
            root.addElement("curveOffset").setText("" + curvOff);

        if (!squareCurve)
            root.addElement("squareCurve").setText("" + squareCurve);

        if (!firstCurveRight)
            root.addElement("firstCurveRight").setText("" + firstCurveRight);

        return doc;
    }

    @Override
    public String getName() {
        return getType();
    }

    @Override
    public Image getIconImage() {
        return adapter.getIconImage();
    }

    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        adapter.mouseClicked(event, source);        
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
        lastDragPoint = event.getPoint();
    }

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
            double nx = norm * Math.cos(Math.toRadians(bearingDegs) - angle);
            double ny = norm * Math.sin(Math.toRadians(bearingDegs) - angle);
            
            width += nx / (Math.abs(nx) < 30 ? 10 : 2);
            length += ny / (Math.abs(ny) < 30 ? 10 : 2);

            width = MathMiscUtils.round(width, 1);
            width = Math.max(1, width);
            length = MathMiscUtils.round(length, 1);
            length = Math.max(1, length);
            recalcPoints();
        }
        else if (event.isShiftDown()) {
            bearingDegs += yammount / (Math.abs(yammount) < 30 ? 10 : 2);
            bearingDegs = AngleUtils.nomalizeAngleDegrees360(bearingDegs);
            recalcPoints();
        }
        else if (event.isAltDown() || event.isAltGraphDown()) {
            crossAngleDegs += yammount / (Math.abs(yammount) < 30 ? 10 : 2);
            crossAngleDegs = AngleUtils.nomalizeAngleDegrees180(crossAngleDegs);
            recalcPoints();
        }
        else {
            adapter.mouseDragged(event, source);
        }
        lastDragPoint = event.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
        lastDragPoint = null;
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }
    
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }
    
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);        
    }

    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        editing = mode;
        adapter.setActive(mode, source);
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        return calculatePosition();
    }

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

    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        double absoluteLatLonDepth[] = location.getAbsoluteLatLonDepth(); 
        latDegs = absoluteLatLonDepth[0];
        lonDegs = absoluteLatLonDepth[1];
        z = location.getZ();
        zunits = location.getZUnits();
        recalcPoints();
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        ManeuverLocation loc = calculatePosition();
        loc.translatePosition(offsetNorth, offsetEast, offsetDown);
        setManeuverLocation(loc);
    }

    @Override
    public List<double[]> getPathPoints() {
        return Collections.unmodifiableList(points);
    }

    @Override
    public List<LocationType> getPathLocations() {
        Vector<LocationType> locs = new Vector<>();
        List<double[]> lst = Collections.unmodifiableList(points);
        LocationType start = new LocationType(getManeuverLocation());
        if (getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.DEPTH)
            start.setDepth(getManeuverLocation().getZ());
        else if (getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.DEPTH)
            start.setDepth(-getManeuverLocation().getZ());
        for (double[] ds : lst) {
            LocationType loc = new LocationType(start);
            loc.translatePosition(ds);
            loc.convertToAbsoluteLatLonDepth();
            locs.add(loc);
        }
        return locs;
    }
    
    /**
     * @return the bearingDegs
     */
    public double getBearingDegs() {
        return bearingDegs;
    }

    /**
     * @param bearingDegs the bearingDegs to set
     */
    public void setBearingDegs(double bearingDegs) {
        this.bearingDegs = bearingDegs;
        recalcPoints();
    }

    /**
     * @return the width
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return the overlapPercent
     */
    public short getOverlapPercent() {
        return overlapPercentage;
    }

    /**
     * @return the curvOff
     */
    public double getCurvOff() {
        return curvOff;
    }

    /**
     * @return the squareCurve
     */
    public boolean isSquareCurve() {
        return squareCurve;
    }

    /**
     * @return the crossAngleDegs
     */
    public double getCrossAngleDegs() {
        return crossAngleDegs;
    }

    /**
     * @return the angleAppertureDegs
     */
    public double getAngleApertureDegs() {
        return angleApertureDegs;
    }
    
    /**
     * @return the range
     */
    public int getRange() {
        return range;
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
            String txt = I18n.text("Ctrl+Click to grow | Shift+Click to rotate | Alt+Click to skew");
            g3.setColor(Color.BLACK);
            g3.drawString(txt, 55, 15 + 20);
            g3.setColor(COLOR_HELP);
            g3.drawString(txt, 54, 14 + 20);
            g3.dispose();
        }
        
        g2d.setColor(Color.white);
        
        double zoom = renderer.getZoom();
        g2d.rotate(-renderer.getRotation());

        g2d.rotate(-Math.PI/2);
        ManeuversUtil.paintBox(g2d, zoom, width, length, 0, 0, Math.toRadians(bearingDegs),
                Math.toRadians(crossAngleDegs), true, !firstCurveRight, editing);
        ManeuversUtil.paintPointLineList(g2d, zoom, points, editing, calcCov() / 2, editing);
        g2d.rotate(+Math.PI/2);
    }


    /**
     * Call this to update the maneuver points.
     */
    private void recalcPoints() {
        double hstep = calcHStep();
        Vector<double[]> newPoints = ManeuversUtil.calcRowsPoints(width, length, hstep,
                1, curvOff, squareCurve, Math.toRadians(bearingDegs), Math.toRadians(crossAngleDegs),
                !firstCurveRight);

        points = newPoints;
    }

    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.RowsCoverage man = new pt.lsts.imc.RowsCoverage();
        man.setLat(Math.toRadians(latDegs));
        man.setLon(Math.toRadians(lonDegs));
        man.setZ(z);
        man.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));        
        speed.setSpeedToMessage(man);
        man.setWidth(width);
        man.setLength(length);
        man.setBearing(Math.toRadians(bearingDegs));
        man.setCrossAngle(Math.toRadians(crossAngleDegs));
        man.setCoff((short)curvOff);
        man.setAngAperture(Math.toRadians(angleApertureDegs));
        man.setRange((short) range);
        man.setOverlap(overlapPercentage);
        man.setCustom(getCustomSettings());
        man.setFlags((short) ((squareCurve ? pt.lsts.imc.RowsCoverage.FLG_SQUARE_CURVE : 0) 
                + (firstCurveRight ? pt.lsts.imc.RowsCoverage.FLG_CURVE_RIGHT : 0)));
        return man;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        pt.lsts.imc.RowsCoverage man = null;
        try {
            man = pt.lsts.imc.RowsCoverage.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        latDegs = Math.toDegrees(man.getLat());
        lonDegs = Math.toDegrees(man.getLon());
        z = man.getZ();
        zunits = ManeuverLocation.Z_UNITS.valueOf(man.getZUnits().toString());
        width = man.getWidth();
        length = man.getLength();
        bearingDegs = Math.toDegrees(man.getBearing());

        speed = SpeedType.parseImcSpeed(message);
        crossAngleDegs = Math.toDegrees(man.getCrossAngle());
        curvOff = man.getCoff();
        overlapPercentage = man.getOverlap();
        
        angleApertureDegs = Math.toDegrees(man.getAngAperture());
        range = man.getRange();
        
        firstCurveRight = (man.getFlags() & Rows.FLG_CURVE_RIGHT) != 0;
        squareCurve = (man.getFlags() & Rows.FLG_SQUARE_CURVE) != 0;

        setCustomSettings(man.getCustom());
        recalcPoints();
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        return ManeuversUtil.getPropertiesFromManeuver(this);
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

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        ManeuversUtil.setPropertiesToManeuver(this, properties);
        recalcPoints();
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        return getDistanceTravelled(initialPosition) / speed.getMPS();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.StatisticsProvider#getDistanceTravelled(pt.lsts.neptus.types.coord.LocationType)
     */
    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = getStartLocation().getDistanceInMeters(initialPosition);

        if (points.size() == 0) {
            double hstep = calcHStep();
            int numrows = (int) Math.floor(width / hstep);
            double planeDistance = numrows * length + numrows * hstep;

            meters += planeDistance;
            return meters;
        }
        else {
            for (int i = 0; i < points.size(); i++) {
                double[] pointI = points.get(i);
                double[] pointF;
                try {
                    pointF = points.get(i+1);
                }
                catch (Exception e) {
                    break;
                }
                double[] offsets = {pointF[0]-pointI[0], pointF[1]-pointI[1]}; 
                double sum = offsets[0] * offsets[0] + offsets[1] * offsets[1];
                double planeDistance = Math.sqrt(sum);
                meters += planeDistance;
            }
            return meters;
        }
    }

    @Override
    public double getMaxDepth() {
        return z;
    }

    @Override
    public double getMinDepth() {
        return z;
    }

    private double calcCov() {
        double cov;
        if (angleApertureDegs <= 0)
          cov = 2 * range;
        else
          cov = 2 * range * Math.sin(Math.toRadians(angleApertureDegs / 2));
        
        return cov;
    }

    private double calcHStep() {
        double hstep = calcCov();
        hstep = hstep * (1 - overlapPercentage / 200.);
        return hstep;
    }
    
    @Override
    public String getTooltipText() {
        NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
        return super.getTooltipText()+"<hr/>"+
        I18n.text("length") + ": <b>"+nf.format(length)+" " + I18n.textc("m", "meters") + "</b><br/>"+
        I18n.text("width") + ": <b>"+nf.format(width)+" " + I18n.textc("m", "meters") + "</b><br/>"+
        I18n.text("overlap") + ": <b>"+overlapPercentage+" %</b><br/>"+
        I18n.text("hstep") + ": <b>"+nf.format(calcHStep())+" " + I18n.textc("m", "meters") + "</b><br/>"+
        I18n.text("bearing") + ": <b>"+nf.format(MathMiscUtils.round(bearingDegs, 1))+" \u00B0</b><br/>"+
        I18n.text("cross angle") + ": <b>"+nf.format(MathMiscUtils.round(crossAngleDegs, 1))+" \u00B0</b><br/>"+
        I18n.text("speed") + ": <b>"+getSpeed()+"</b><br/>"+
        I18n.text("distance") + ": <b>"+MathMiscUtils.parseToEngineeringNotation(getDistanceTravelled((LocationType)getStartLocation()), 2)+I18n.textc("m", "meters") + "</b><br/>"+
        "<br>" + I18n.text("depth") + ": <b>"+nf.format(z)+" " + I18n.textc("m", "meters") + "</b>";    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
    }
    
    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        Vector<ManeuverLocation> locs = new Vector<>();
        List<double[]> lst = Collections.unmodifiableList(points);
        ManeuverLocation start = new ManeuverLocation(getManeuverLocation());
        for (double[] ds : lst) {
            ManeuverLocation loc = new ManeuverLocation(start);
            loc.translatePosition(ds);
            loc.convertToAbsoluteLatLonDepth();
            locs.add(loc);
        }
        return locs;
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
        RowsCoverage rc = new RowsCoverage();
        PluginUtils.editPluginProperties(rc, true);
        
        System.out.println(XMLUtil.getAsPrettyPrintFormatedXMLString(rc.asXML().substring(39)));

        RowsCoverage rc1 = new RowsCoverage();
        rc1.loadManeuverFromXML(XMLUtil.getAsPrettyPrintFormatedXMLString(rc.asXML().substring(39)));
        PluginUtils.editPluginProperties(rc1, true);
    }
}
