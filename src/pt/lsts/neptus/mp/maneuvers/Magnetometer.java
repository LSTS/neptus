//**************************************************************************************************
// Copyright (C) 2018 OceanScan - Marine Systems & Technology, Lda.                                *
//**************************************************************************************************
//                                                                                                 *
// All information contained herein is, and remains the property of OceanScan - Marine             *
// Systems & Technology, Lda. Dissemination of this information or reproduction of this material   *
// is strictly forbidden unless prior written permission is obtained from OceanScan - Marine       *
// Systems & Technology, Lda.                                                                      *
//                                                                                                 *
// This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part   *
// of this source code package.                                                                    *
//                                                                                                 *
//**************************************************************************************************

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;

public class Magnetometer extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, StateRendererInteraction,
        IMCSerialization, StatisticsProvider, PathProvider {

    /** Log handle. */
    private final static Logger LOG = LoggerFactory.getLogger(Magnetometer.class);

    protected static final String DEFAULT_ROOT_ELEMENT = "Magnetometer";

    ManeuverLocation destination = new ManeuverLocation();
    protected double bearingRad = 0, width = 100;
    protected boolean firstClockwise = true;
    protected SpeedType speed = new SpeedType(1000, Units.RPM);

    protected InteractionAdapter adapter = new InteractionAdapter(null);
    private Point2D lastDragPoint = null;

    protected boolean editing = false;

    protected Vector<double[]> points = new Vector<>();

    public Magnetometer() {
        super();
        recalcPoints();
    }

    /**
     * Call this to update the maneuver points.
     */
    private void recalcPoints() {
        points = calcMagnetometerPoints(width, bearingRad, firstClockwise);
    }

    public static Vector<double[]> calcMagnetometerPoints(double width, double bearingRad, boolean clockwise) {

        width = Math.abs(width);

        Vector<double[]> newPoints = new Vector<>();
        double[] pointULeft = {0, 0, 0, -1};
        newPoints.add(pointULeft);

        double[] pointURight = {0, width, 0, -1};
        double[] pointDRight = {-width, width, 0, -1};
        double[] pointDLeft = {-width, 0, 0, -1};

        newPoints.add(clockwise ? pointURight.clone() : pointDLeft.clone());
        newPoints.add(clockwise ? pointDRight.clone() : pointDRight.clone());
        newPoints.add(clockwise ? pointDLeft.clone() : pointURight.clone());
        newPoints.add(clockwise ? pointULeft.clone() : pointULeft.clone());

        newPoints.add(!clockwise ? pointURight.clone() : pointDLeft.clone());
        newPoints.add(!clockwise ? pointDRight.clone() : pointDRight.clone());
        newPoints.add(!clockwise ? pointDLeft.clone() : pointURight.clone());
        newPoints.add(!clockwise ? pointULeft.clone() : pointULeft.clone());

        for (double[] pt : newPoints) {
            double[] res = AngleUtils.rotate(bearingRad, pt[X], pt[Y], false);
            pt[X] = res[0];
            pt[Y] = res[1];
        }

        return newPoints;
    }

    
    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            Node node = doc.selectSingleNode("//basePoint/point");
            if (node != null) {
                ManeuverLocation loc = new ManeuverLocation();
                loc.load(node.asXML());
                setManeuverLocation(loc);
            }

            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);

            bearingRad = Math.toRadians(Double.parseDouble(doc.selectSingleNode("//bearing").getText()));

            // area
            width = Double.parseDouble(doc.selectSingleNode("//width").getText());

            node = doc.selectSingleNode("//clockwise");
            firstClockwise = node == null || Boolean.parseBoolean(node.getText());
        } catch (Exception e) {
            LOG.error(I18n.textf("error loading %maneuver from xml", getType()), e);
        } finally {
            recalcPoints();
        }
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        return destination.clone(); }

    @Override
    public ManeuverLocation getEndLocation() {
        try {
            double[] last = points.lastElement();
            ManeuverLocation loc = getManeuverLocation().clone();
            loc.translatePosition(last[X], last[Y], last[Z]);
            return loc;
        } catch (Exception e) {
            return getManeuverLocation();
        }
    }

    @Override
    public ManeuverLocation getStartLocation() {
        try {
            double[] first = points.firstElement();
            ManeuverLocation loc = getManeuverLocation().clone();
            loc.translatePosition(first[X], first[Y], first[Z]);
            return loc;
        } catch (Exception e) {
            return getManeuverLocation();
        }
    }

    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        destination = location.clone();
        recalcPoints();
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
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        destination.translatePosition(offsetNorth, offsetEast, offsetDown);
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
            String txt = I18n.text("Ctrl+Click to grow | Shift+Click to rotate");
            g3.setColor(Color.BLACK);
            g3.drawString(txt, 55, 15 + 20);
            g3.setColor(COLOR_HELP);
            g3.drawString(txt, 54, 14 + 20);
            g3.dispose();
        }

        g2d.setColor(Color.white);

        double zoom = renderer.getZoom();
        g2d.rotate(-renderer.getRotation());

        g2d.rotate(-Math.PI / 2);

        ManeuversUtil.paintPointLineList(g2d, zoom, points, false, 0, editing);
        g2d.rotate(Math.PI / 2);
    }

    @Override
    public Object clone() {
        Magnetometer clone = new Magnetometer();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation());
        clone.bearingRad = bearingRad;
        clone.speed = new SpeedType(speed);
        clone.width = width;
        clone.firstClockwise = firstClockwise;
        clone.recalcPoints();
        return clone;
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);
        root.addAttribute("kind", "automatic");

        //basePoint
        Element basePoint = root.addElement("basePoint");
        Element point = getManeuverLocation().asElement("point");
        basePoint.add(point);
        basePoint.addAttribute("type", "pointType");

        root.addElement("width").setText("" + width);
        root.addElement("bearing").setText("" + Math.toDegrees(bearingRad));

        if (!firstClockwise)
            root.addElement("clockwise").setText("" + firstClockwise);

        SpeedType.addSpeedElement(root, this);

        return document;
    }

    @Override
    public String getType() {
        return DEFAULT_ROOT_ELEMENT;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = new Vector<>();

        DefaultProperty width = PropertiesEditor.getPropertyInstance("Width (m)", Double.class, this.width, true);
        width.setShortDescription("Width of the volume to cover (meters)");
        props.add(width);

        DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, this.speed, true);
        speed.setShortDescription("The vehicle's desired speed");
        props.add(speed);

        DefaultProperty direction = PropertiesEditor.getPropertyInstance("Bearing (\u00B0)", Double.class, Math.toDegrees(bearingRad), true);
        direction.setShortDescription("The outgoing bearing from starting location (degrees)");
        props.add(direction);

        DefaultProperty firstCurveRightP = PropertiesEditor.getPropertyInstance("First Clockwise", Boolean.class, firstClockwise, true);
        firstCurveRightP.setShortDescription("First Clockwise or not");
        props.add(firstCurveRightP);
        return props;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);

        for (Property p : properties) {
            if (p.getName().equals("Width (m)")) {
                width = (Double) p.getValue();
            } else if (p.getName().equals("Speed")) {
                speed = (SpeedType) p.getValue();
            } else if (p.getName().equals("Bearing (\u00B0)")) {
                bearingRad = Math.toRadians((Double) p.getValue());
            } else if (p.getName().equalsIgnoreCase("First Clockwise")) {
                firstClockwise = (Boolean) p.getValue();
            }
        }
        recalcPoints();
    }

    @Override
    public String getTooltipText() {
        NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
        return super.getTooltipText() + "<hr/>" +
                I18n.text("width") + ": <b>" + nf.format(width) + " " + I18n.textc("m", "meters") + "</b><br/>" +
                I18n.text("bearing") + ": <b>" + nf.format(Math.toDegrees(bearingRad)) + " \u00B0</b><br/>" +
                I18n.text("speed") + ": <b>" + getSpeed().toStringAsDefaultUnits() + "</b><br/>" +
                I18n.text("distance") + ": <b>" +
                MathMiscUtils.parseToEngineeringNotation(getDistanceTravelled(getStartLocation()), 2) + I18n.textc("m", "meters") + "</b><br/>" +
                "<br>" + I18n.text(destination.getZUnits().toString()) + ": <b>" + nf.format(destination.getZ()) + " " + I18n.textc("m", "meters") + "</b>";
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
            double nx = norm * Math.cos(bearingRad - angle);
            Math.sin(bearingRad - angle);

            width += nx / (Math.abs(nx) < 30 ? 10 : 2);

            width = MathMiscUtils.round(width, 1);
            width = Math.max(1, width);
            recalcPoints();
        } else if (event.isShiftDown()) {
            bearingRad += Math.toRadians(yammount / (Math.abs(yammount) < 30 ? 10 : 2));

            while (bearingRad > Math.PI * 2)
                bearingRad -= Math.PI * 2;
            while (bearingRad < 0)
                bearingRad += Math.PI * 2;
            recalcPoints();
        } else {
            adapter.mouseDragged(event, source);
        }
        lastDragPoint = event.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
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
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {

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
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }

    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        editing = mode;
        adapter.setActive(mode, source);
    }

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
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
        else if (getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.ALTITUDE)
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
     * @return the bearingRad
     */
    public double getBearingRad() {
        return bearingRad;
    }

    /**
     * @param bearingRad the bearingRad to set
     */
    public void setBearingRad(double bearingRad) {
        this.bearingRad = bearingRad;
        recalcPoints();
    }

    /**
     * @return the width
     */
    public double getWidth() {
        return width;
    }

    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.Magnetometer man = new pt.lsts.imc.Magnetometer();

        man.setTimeout(getMaxTime());

        LocationType l = getManeuverLocation();
        l.convertToAbsoluteLatLonDepth();
        man.setLat(l.getLatitudeRads());
        man.setLon(l.getLongitudeRads());

        man.setZ(getManeuverLocation().getZ());
        man.setZUnitsStr(getManeuverLocation().getZUnits().name());
        man.setSpeed(speed.getMPS());
        man.setWidth(width);
        man.setBearing(bearingRad);
        man.setCustom(getCustomSettings());
        man.setDirection(firstClockwise ? pt.lsts.imc.Magnetometer.DIRECTION.CLOCKW_FIRST : pt.lsts.imc.Magnetometer.DIRECTION.CCLOCKW_FIRST);

        return man;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        if (!getType().equalsIgnoreCase(message.getAbbrev()))
            return;

        pt.lsts.imc.Magnetometer man;
        try {
            man = pt.lsts.imc.Magnetometer.clone(message);
        } catch (Exception e) {
            LOG.error(I18n.textf("error parsing %maneuver", getType()), e);
            return;
        }

        ManeuverLocation pos = new ManeuverLocation();
        pos.setLatitudeRads(man.getLat());
        pos.setLongitudeRads(man.getLon());
        pos.setZ(man.getZ());
        try {
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(man.getZUnits().toString()));
        } catch (IllegalArgumentException e) {
            LOG.info(I18n.text("error parsing z-units, setting to none"));
            pos.setZUnits(ManeuverLocation.Z_UNITS.NONE);
        }
        setManeuverLocation(pos);

        setMaxTime(man.getTimeout());
        speed = SpeedType.parseImcSpeed(message);
        width = man.getWidth();
        bearingRad = man.getBearing();

        firstClockwise = man.getDirection() == pt.lsts.imc.Magnetometer.DIRECTION.CLOCKW_FIRST;

        setCustomSettings(man.getCustom());
        recalcPoints();
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        return getDistanceTravelled(initialPosition) / speed.getMPS();
    }

    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = getStartLocation().getDistanceInMeters(initialPosition);

        if (points.size() == 0) {
            meters += width * 8;
            return meters;
        } else {
            for (int i = 0; i < points.size(); i++) {
                double[] pointI = points.get(i);
                double[] pointF;
                try {
                    pointF = points.get(i + 1);
                } catch (Exception e) {
                    break;
                }
                double[] offsets = {pointF[0] - pointI[0], pointF[1] - pointI[1]};
                double sum = offsets[0] * offsets[0] + offsets[1] * offsets[1];
                double planeDistance = Math.sqrt(sum);
                meters += planeDistance;
            }
            return meters;
        }
    }

    @Override
    public double getMaxDepth() {
        return destination.getZ();
    }

    @Override
    public double getMinDepth() {
        return destination.getZ();
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
        Magnetometer man = new Magnetometer();
        
        System.out.println(man.asXML("Magnetometer"));
    }
}
