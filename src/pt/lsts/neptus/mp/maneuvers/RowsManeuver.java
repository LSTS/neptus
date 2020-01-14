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
 * Author: José Pinto, pdias
 * 11/03/2011
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

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Rows;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
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
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author zp
 * @author pdias
 */
public class RowsManeuver extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, StateRendererInteraction,
IMCSerialization, StatisticsProvider, PathProvider {

    static boolean unblockNewRows = false;

    static {
        if (IMCDefinition.getInstance().create("Rows") != null)
            unblockNewRows = true;
    }

    protected double latRad = 0, lonRad = 0, z = 2, bearingRad = 0, width = 100,
            length = 200, hstep = 27, ssRangeShadow = 30;
    protected double crossAngleRadians = 0;
    protected double curvOff = 15;
    protected float alternationPercentage = 1.0f;
    protected boolean squareCurve = true, firstCurveRight = true;
    protected boolean paintSSRangeShadow = true;
    protected SpeedType speed = new SpeedType(1000, Units.RPM);
    protected ManeuverLocation.Z_UNITS zunits = ManeuverLocation.Z_UNITS.NONE;

    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected Point2D lastDragPoint = null;

    protected boolean editing = false;

    protected Vector<double[]> points = new Vector<double[]>();

    /**
     * 
     */
    public RowsManeuver() {
        super();
        recalcPoints();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#getType()
     */
    @Override
    public String getType() {
        return "Rows";
    }
    
    protected ManeuverLocation calculatePosition() {
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitudeDegs(Math.toDegrees(latRad));
        loc.setLongitudeDegs(Math.toDegrees(lonRad));
        loc.setZ(z);
        loc.setZUnits(zunits);
        return loc;
    }

    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            //NeptusLog.pub().info("<###> "+doc.asXML());
            // basePoint
            Node node = doc.selectSingleNode("//basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);            
            latRad = getManeuverLocation().getLatitudeRads();
            lonRad = getManeuverLocation().getLongitudeRads();
            z = getManeuverLocation().getZ();
            zunits = getManeuverLocation().getZUnits();
            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
            bearingRad = Math.toRadians(Double.parseDouble(doc.selectSingleNode("//bearing").getText()));

            // area
            width = Math.abs(Double.parseDouble(doc.selectSingleNode("//width").getText()));
            node = doc.selectSingleNode("//length");
            if (node != null)
                length = Math.abs(Double.parseDouble(node.getText()));
            else
                length = width;

            //steps
            hstep = Math.abs(Double.parseDouble(doc.selectSingleNode("//hstep").getText()));
            hstep = hstep <= 0 ? 1 : hstep;

            node = doc.selectSingleNode("//crossAngle");
            if (node != null)
                crossAngleRadians = Math.toRadians(Double.parseDouble(node.getText()));
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

            //            ssRangeShadow = 30;
            //            paintSSRangeShadow = true;
            node = doc.selectSingleNode("//ssRangeShadow");
            if (node != null) {
                try {
                    ssRangeShadow = Short.parseShort(node.getText());
                    ssRangeShadow = ssRangeShadow < 0 ? 0 : ssRangeShadow;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    ssRangeShadow = 30;
                }
            }
            else
                ssRangeShadow = 30;
            node = doc.selectSingleNode("//paintSSRangeShadow");
            if (node != null)
                paintSSRangeShadow = Boolean.parseBoolean(node.getText());
            else
                paintSSRangeShadow = true;

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
        RowsManeuver clone = new RowsManeuver();
        super.clone(clone);
        //clone.setManeuverLocation(getManeuverLocation());
        clone.latRad = latRad;
        clone.lonRad = lonRad;
        clone.z = z;
        clone.zunits = zunits;
        clone.bearingRad = bearingRad;
        clone.hstep = hstep;        
        clone.length = length;
        clone.speed = getSpeed();
        clone.width = width;

        clone.alternationPercentage = alternationPercentage;
        clone.crossAngleRadians = crossAngleRadians;
        clone.curvOff = curvOff;
        clone.squareCurve = squareCurve;
        clone.ssRangeShadow = ssRangeShadow;
        clone.paintSSRangeShadow = paintSSRangeShadow;
        clone.firstCurveRight = firstCurveRight;

        clone.recalcPoints();
        return clone;
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

        root.addElement("width").setText(""+width);
        root.addElement("length").setText(""+length);
        root.addElement("hstep").setText(""+hstep);
        root.addElement("bearing").setText(""+Math.toDegrees(bearingRad));

        if (crossAngleRadians != 0)
            root.addElement("crossAngle").setText(""+Math.toDegrees(crossAngleRadians));

        if ((short)(alternationPercentage*100f) != 100)
            root.addElement("alternationPercentage").setText(""+(short)(alternationPercentage*100f));

        if (curvOff != 15)
            root.addElement("curveOffset").setText(""+curvOff);

        if (!squareCurve)
            root.addElement("squareCurve").setText(""+squareCurve);

        if (!firstCurveRight)
            root.addElement("firstCurveRight").setText(""+firstCurveRight);

        SpeedType.addSpeedElement(root, this);
        
        if (!paintSSRangeShadow) {
            root.addElement("paintSSRangeShadow").setText(""+paintSSRangeShadow);
        }
        if (ssRangeShadow != 30) {
            root.addElement("ssRangeShadow").setText(""+Double.valueOf(ssRangeShadow).shortValue());
        }

        return document;
    }

    @Override
    public String getName() {
        return "Rows";
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
            double ny = norm * Math.sin(bearingRad - angle);
            
            width += nx / (Math.abs(nx) < 30 ? 10 : 2);
            length += ny / (Math.abs(ny) < 30 ? 10 : 2);

            width = MathMiscUtils.round(width, 1);
            width = Math.max(1, width);
            length = MathMiscUtils.round(length, 1);
            length = Math.max(1, length);
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
        latRad = Math.toRadians(absoluteLatLonDepth[0]);
        lonRad = Math.toRadians(absoluteLatLonDepth[1]);
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
     * At the end call {@link #recalcPoints()} to update maneuver points
     * @param width
     * @param length
     * @param hstep
     * @param alternationPercent
     * @param curvOff
     * @param squareCurve
     * @param bearingRad
     * @param crossAngleRadians
     * @param firstCurveRight
     */
    public void setParams(double width, double length, double hstep,
            double alternationPercent, double curvOff, boolean squareCurve, double bearingRad,
            double crossAngleRadians, boolean firstCurveRight, boolean paintSSRangeShadow, short ssRangeShadow) {
        this.width = width;
        this.length = length;
        this.hstep = hstep;
        this.alternationPercentage = Double.valueOf(alternationPercent).floatValue();
        this.curvOff = curvOff;
        this.squareCurve = squareCurve;
        this.bearingRad = bearingRad;
        this.crossAngleRadians = crossAngleRadians;
        this.firstCurveRight = firstCurveRight;
        this.paintSSRangeShadow = paintSSRangeShadow;
        this.ssRangeShadow = ssRangeShadow;

        recalcPoints();
    }

    /**
     * @return the width
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return the hstep
     */
    public double getHstep() {
        return hstep;
    }

    /**
     * @return the alternationPercent
     */
    public float getAlternationPercent() {
        return alternationPercentage;
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
     * @return the crossAngleRadians
     */
    public double getCrossAngleRadians() {
        return crossAngleRadians;
    }

    /**
     * @return the paintSSRangeShadow
     */
    public boolean isPaintSSRangeShadow() {
        return paintSSRangeShadow;
    }

    /**
     * @param paintSSRangeShadow the paintSSRangeShadow to set
     */
    public void setPaintSSRangeShadow(boolean paintSSRangeShadow) {
        this.paintSSRangeShadow = paintSSRangeShadow;
    }

    /**
     * @return the ssRangeShadow
     */
    public double getSsRangeShadow() {
        return ssRangeShadow;
    }

    /**
     * @param ssRangeShadow the ssRangeShadow to set
     */
    public void setSsRangeShadow(double ssRangeShadow) {
        this.ssRangeShadow = ssRangeShadow;
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

        g2d.rotate(-Math.PI/2);
        //        recalcPoints();
        ManeuversUtil.paintBox(g2d, zoom, width, length, 0, 0, bearingRad, crossAngleRadians, false, !firstCurveRight, editing);
        ManeuversUtil.paintPointLineList(g2d, zoom, points, paintSSRangeShadow, ssRangeShadow, editing);
        //        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad, crossAngleRadians);
        //        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad+Math.toRadians(-60), crossAngleRadians);
        //        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad+Math.toRadians(-120), crossAngleRadians);
        //        ManeuversUtil.paintPointLineList(g2d, zoom, points, false, sRange);
        g2d.rotate(+Math.PI/2);
    }


    /**
     * Call this to update the maneuver points.
     */
    private void recalcPoints() {
        Vector<double[]> newPoints = ManeuversUtil.calcRowsPoints(width, length, hstep,
                alternationPercentage, curvOff, squareCurve, bearingRad, crossAngleRadians,
                !firstCurveRight);

        points = newPoints;
    }

    @Override
    public IMCMessage serializeToIMC() {
        Rows man = new Rows();
        man.setTimeout(getMaxTime());
        man.setLat(latRad);
        man.setLon(lonRad);
        man.setZ(z);
        man.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().toString()));        
        
        man.setWidth(width);
        man.setLength(length);
        man.setBearing(bearingRad);
        man.setHstep(hstep);
        man.setCrossAngle(crossAngleRadians);
        man.setCoff((short)curvOff);
        man.setAlternation((short)(alternationPercentage*100));
        man.setCustom(getCustomSettings());
        man.setFlags((short) ((squareCurve ? Rows.FLG_SQUARE_CURVE : 0) + (firstCurveRight ? Rows.FLG_CURVE_RIGHT : 0)));
        speed.setSpeedToMessage(man);
        return man;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        Rows man = null;
        try {
            man = Rows.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }


        setMaxTime(man.getTimeout());
        latRad = man.getLat();
        lonRad = man.getLon();
        z = man.getZ();
        zunits = ManeuverLocation.Z_UNITS.valueOf(man.getZUnits().toString());
        
        width = man.getWidth();
        length = man.getLength();
        bearingRad = man.getBearing();
        hstep = man.getHstep();
        speed = SpeedType.parseImcSpeed(message);
        crossAngleRadians = man.getCrossAngle();
        curvOff = man.getCoff();
        alternationPercentage = man.getAlternation()/100f;
        
        firstCurveRight = (man.getFlags() & Rows.FLG_CURVE_RIGHT) != 0;
        squareCurve = (man.getFlags() & Rows.FLG_SQUARE_CURVE) != 0;

        setCustomSettings(man.getCustom());
        recalcPoints();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);

        for (Property p : properties) {

            if (p.getName().equals("Width")) {
                width = (Double)p.getValue();
                width = width < 0 ? 1 : width;
                continue;
            }

            if (p.getName().equals("Length")) {
                length = (Double)p.getValue();
                length = length < 0 ? 1 : length;
                continue;
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
                hstep = hstep <= 0 ? 1 : hstep;
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

            if (p.getName().equalsIgnoreCase("First Curve Right")) {
                firstCurveRight = (Boolean)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("Payload Shadow")) {
                paintSSRangeShadow = (Boolean)p.getValue();
                continue;
            }
            if (p.getName().equalsIgnoreCase("Shadow Size")) {
                ssRangeShadow = (Short)p.getValue();
                ssRangeShadow = ssRangeShadow < 0 ? 0 : ssRangeShadow;
                continue;
            }            
        }
        recalcPoints();
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();

        DefaultProperty length = PropertiesEditor.getPropertyInstance("Length", Double.class, this.length, true);
        length.setShortDescription(I18n.text("The length of the volume to cover, in meters") + "<br/>(m)");
        props.add(length);

        DefaultProperty width = PropertiesEditor.getPropertyInstance("Width", Double.class, this.width, true);
        width.setShortDescription(I18n.text("Width of the volume to cover, in meters") + "<br/>(m)");
        props.add(width);

        DefaultProperty halt = PropertiesEditor.getPropertyInstance("Horizontal Alternation", Short.class, (short)(this.alternationPercentage*100), unblockNewRows);
        halt.setShortDescription(I18n
                .text("Horizontal alternation in percentage. 100 will make all rows separated by the Horizontal Step")
                + "<br/>(%)");
        props.add(halt);

        DefaultProperty hstep = PropertiesEditor.getPropertyInstance("Horizontal Step", Double.class, this.hstep, true);
        hstep.setShortDescription(I18n.text("Horizontal distance between rows, in meters") + "<br/>(m)");
        props.add(hstep);

        DefaultProperty direction = PropertiesEditor.getPropertyInstance("Bearing", Double.class, Math.toDegrees(bearingRad), true);
        direction.setShortDescription(I18n.text("The outgoing bearing (from starting location) in degrees") + "<br/>(\u00B0)");
        props.add(direction);

        DefaultProperty cross = PropertiesEditor.getPropertyInstance("Cross Angle", Double.class, Math.toDegrees(crossAngleRadians), unblockNewRows);
        cross.setShortDescription(I18n.text("The tilt angle of the search box in degrees") + "<br/>(\u00B0)");
        props.add(cross);

        DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, this.speed, true);
        speed.setShortDescription(I18n.text("The vehicle's desired speed"));
        props.add(speed);

        DefaultProperty curvOffset = PropertiesEditor.getPropertyInstance("Curve Offset", Double.class, curvOff, true);
        curvOffset.setShortDescription(I18n.text("The extra length to use for the curve") + "<br/>(m)");
        props.add(curvOffset);

        DefaultProperty squareCurveP = PropertiesEditor.getPropertyInstance("Square Curve", Boolean.class, squareCurve, unblockNewRows);
        squareCurveP.setShortDescription(I18n.text("If the curve should be square or direct"));
        props.add(squareCurveP);

        DefaultProperty firstCurveRightP = PropertiesEditor.getPropertyInstance("First Curve Right", Boolean.class, firstCurveRight, unblockNewRows);
        firstCurveRightP.setShortDescription(I18n.text("If the first curve should be to the right or left"));       
        props.add(firstCurveRightP);

        DefaultProperty paintSSRangeShadowP = PropertiesEditor.getPropertyInstance("Payload Shadow", Boolean.class, paintSSRangeShadow, unblockNewRows);
        paintSSRangeShadowP.setShortDescription(I18n.text("If the sidescan range shadow is painted"));
        props.add(paintSSRangeShadowP);
        DefaultProperty ssRangeShadowtP = PropertiesEditor.getPropertyInstance("Shadow Size", Short.class, Double.valueOf(ssRangeShadow).shortValue(), unblockNewRows);
        ssRangeShadowtP.setShortDescription(I18n.text("The sidescan range") + "<br/>(m)");
        props.add(ssRangeShadowtP);

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
    
    // Validate for additional parameters
    public String validateShadowSize(short value) {
        if (value < 0)
            return "Keep it 0 or above";
        return null;
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
            int numrows = (int) Math.floor(width/hstep);
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

    @Override
    public String getTooltipText() {
        NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
        return super.getTooltipText()+"<hr/>"+
        I18n.text("length") + ": <b>"+nf.format(length)+" " + I18n.textc("m", "meters") + "</b><br/>"+
        I18n.text("width") + ": <b>"+nf.format(width)+" " + I18n.textc("m", "meters") + "</b><br/>"+
        I18n.text("alt") + ": <b>"+(short)(alternationPercentage*100)+" %</b><br/>"+
        I18n.text("hstep") + ": <b>"+nf.format(hstep)+" " + I18n.textc("m", "meters") + "</b><br/>"+
        I18n.text("bearing") + ": <b>"+nf.format(Math.toDegrees(bearingRad))+" \u00B0</b><br/>"+
        I18n.text("cross angle") + ": <b>"+nf.format(Math.toDegrees(crossAngleRadians))+" \u00B0</b><br/>"+
        I18n.text("speed") + ": <b>"+getSpeed()+"</b><br/>"+
        I18n.text("distance") + ": <b>"+MathMiscUtils.parseToEngineeringNotation(getDistanceTravelled((LocationType)getStartLocation()), 2)+I18n.textc("m", "meters") + "</b><br/>"+
        (paintSSRangeShadow ? I18n.textc("ss range", "sidescan range") + ": <b>"+(short)(ssRangeShadow)+" " + I18n.textc("m", "meters") + "</b><br/>" : "") +
        "<br>" + I18n.text("depth") + ": <b>"+nf.format(z)+" " + I18n.textc("m", "meters") + "</b>";    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {

    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        // TODO Auto-generated method stub
        
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
        
        RowsManeuver rows = new RowsManeuver();
        NeptusLog.pub().info("<###> "+rows.getManeuverLocation());
        NeptusLog.pub().info("<###> "+rows.getStartLocation());
        NeptusLog.pub().info("<###> "+rows.getEndLocation());

        RowsManeuver man = new RowsManeuver();
        //        man.latRad = Math.toRadians(38.45);
        //        man.lonRad = Math.toRadians(-8.90);
        //        man.z = 2;
        //        man.bearingRad = Math.toRadians(45);
        //        man.width = 90;
        //        man.length = 200;
        //        man.hstep = 20;
        //        man.vstep = 0;
        //        man.height = 0;
        //        man.speed = 1000;
        //        man.speed_units = "RPM";
        String xml = man.getManeuverAsDocument("Rows").asXML();
        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(xml));
        //        RowsManeuver clone = (RowsManeuver) man.clone();
        //        NeptusLog.pub().info("<###> "+xml);
        //        NeptusLog.pub().info("<###> "+clone.getManeuverAsDocument("Rows").asXML());
        //        
        //        RowsManeuver tmp = new RowsManeuver();
        //        tmp.loadFromXML(clone.getManeuverAsDocument("Rows").asXML());
        //        NeptusLog.pub().info("<###> "+tmp.getManeuverAsDocument("Rows").asXML());
        //        
        //                MissionType mission = new MissionType("./missions/rep10/rep10.nmisz");
        //                StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(mission));
        //                PlanElement pelem = new PlanElement(MapGroup.getMapGroupInstance(mission), null);
        //                PlanType plan = new PlanType(mission);
        //                man.setPosition(r2d.getCenter());
        //                man.setBearingRad(Math.toRadians(20));
        //                man.setParams(200, 300, 27, .5, 15, true, Math.toRadians(20), Math.toRadians(10), true);
        //                plan.getGraph().addManeuver(man);        
        //                pelem.setPlan(plan);
        //                r2d.addPostRenderPainter(pelem, "Plan");
        //                GuiUtils.testFrame(r2d);
        RowsManeuver.unblockNewRows = true;
        //                PropertiesEditor.editProperties(man, true);

        ConsoleParse.consoleLayoutLoader("./conf/consoles/seacon-basic.ncon");
        //        
        //        NeptusLog.pub().info("<###> "+new RowsManeuver().getManeuverAsDocument("Rows").asXML());
        //        
    }
}
