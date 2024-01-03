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
 * 23/02/2017
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.editor.AngleEditorRads;
import pt.lsts.neptus.gui.editor.AngleEditorRadsShowDegrees;
import pt.lsts.neptus.gui.editor.renderer.AngleRadsRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.coord.PolygonType.Vertex;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.XMLUtil;

/**
 * This maneuver (based on FollowPath) is used to cover a polygonal area
 * @author zp
 *
 */
public class AreaSurvey extends FollowPath {

    // maneuver model that stores the polygon points
    protected PolygonType polygon = new PolygonType();
    
    // path generated from polygon and maneuver parameters
    protected ArrayList<LocationType> path = null;
    
    // enumeration used to change select starting location and direction
    enum Corner {
        A, B, C, D
    }

    @NeptusProperty(name = "Horizontal Step", description="Distance, in meters between transects", units = "m")
    protected double horizontalStep = 30;

    @NeptusProperty(name = "Survey Bearing", description="Angle to use for the survey transects",
            editorClass = AngleEditorRadsShowDegrees.class, rendererClass = AngleRadsRenderer.class)
    protected double angle = 0;

    @NeptusProperty(name = "Automatic Bearing", description="Calculate the survey bearing angle automatically (minimizing number of turns)")
    protected boolean calculateAngle = true;

    @NeptusProperty(name = "Starting Point", description="Direction and start point to use for the survey")
    protected Corner corner = Corner.A;

    protected Color editColor = new Color(200, 200, 0, 128);
    protected Color idleColor = new Color(128, 128, 128, 224);

    @Override
    public String getName() {
        return "AreaSurvey";
    }

    public AreaSurvey() {
        editingHelpText = "Right Click to add/remove vertices | Shift+Click to rotate";
    }

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);

        AffineTransform t = g2d.getTransform();
        g2d.setTransform(renderer.getIdentity());

        if (editing) {
            polygon.setColor(editColor);
            polygon.setFilled(true);

            polygon.getVertices().forEach(v -> {
                Point2D pt = renderer.getScreenPosition(v.getLocation());
                Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10);
                Color c = Color.magenta;
                g2d.setColor(new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue(), 200));
                g2d.fill(ellis);
                g2d.setColor(c);
                g2d.draw(ellis);
            });
        }
        else {
            polygon.setColor(idleColor);
            polygon.setFilled(false);
        }

        polygon.paint(g2d, renderer);
        g2d.setTransform(t);
    }

    /**
     * Given a point in the map, checks if there is some vertex intercepted.
     */
    public PolygonType.Vertex intercepted(MouseEvent evt, StateRenderer2D source) {
        for (PolygonType.Vertex v : polygon.getVertices()) {
            Point2D pt = source.getScreenPosition(new LocationType(v.getLatitudeDegs(), v.getLongitudeDegs()));
            
            if (pt.distance(evt.getPoint()) < 5)
                return v;
            
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (!SwingUtilities.isRightMouseButton(event))
            return;
        
        Vertex v = intercepted(event, source);
        JPopupMenu popup = new JPopupMenu();
        if (v != null) {
            popup.add(I18n.text("Edit location")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LocationType l = new LocationType(v.getLatitudeDegs(), v.getLongitudeDegs());
                    LocationType newLoc = LocationPanel.showLocationDialog(source, I18n.text("Edit Vertex Location"), l,
                            null, true);
                    if (newLoc != null) {
                        newLoc.convertToAbsoluteLatLonDepth();
                        v.setLocation(newLoc);
                        polygon.recomputePath();
                        recalcPoints();

                    }
                    source.repaint();
                }
            });
            popup.add(I18n.text("Remove vertex")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    polygon.removeVertex(v);
                    polygon.recomputePath();
                    recalcPoints();
                    source.repaint();
                }
            });
        }
        else
            popup.add(I18n.text("Add vertex")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LocationType loc = source.getRealWorldLocation(event.getPoint());
                    polygon.addVertex(loc.getLatitudeDegs(), loc.getLongitudeDegs());
                    polygon.recomputePath();
                    recalcPoints();
                    source.repaint();
                }
            });

        popup.show(source, event.getX(), event.getY());
    }

    protected PolygonType.Vertex vertex = null;

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        Vertex v = intercepted(event, source);
        if (v != null)
            vertex = v;
    }

    /**
     * @see ConsoleInteraction
     */
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (vertex == null) {
            if (lastDragPoint == null) {
                super.mouseDragged(event, source);
                lastDragPoint = event.getPoint();
                return;
            }
            else if (event.isShiftDown()) {
                double yammount = event.getPoint().getY() - lastDragPoint.getY();
                lastDragPoint = event.getPoint();
                polygon.rotate(Math.toRadians(yammount / 3));
            }
        }
        else {
            vertex.setLocation(source.getRealWorldLocation(event.getPoint()));
            polygon.recomputePath();
        }

        recalcPoints();
    }

    /**
     * @see ConsoleInteraction
     */
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        if (vertex != null)
            polygon.recomputePath();
        else
            super.mouseReleased(event, source);

        vertex = null;
        recalcPoints();
    }

    /**
     * Call this to update the maneuver points.
     */
    public void recalcPoints() {
        ArrayList<LocationType> locs;

        if (calculateAngle)
            angle = polygon.getDiameterAndAngle().second();

        locs = polygon.getCoveragePath(angle, horizontalStep, corner.ordinal());

        Vector<double[]> newPoints = new Vector<>();
        for (LocationType l : locs)
            newPoints.add(l.getOffsetFrom(getManeuverLocation()));

        points = newPoints;
    }

    @Override
    public ManeuverLocation getStartLocation() {

        if (points == null || points.isEmpty())
            return getManeuverLocation();

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

        if (points == null || points.isEmpty())
            return getManeuverLocation();

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

    /**
     * @return the polygon
     */
    public final PolygonType getPolygon() {
        return polygon;
    }

    /**
     * @param polygon the polygon to set
     */
    public final void setPolygon(PolygonType polygon) {
        this.polygon = polygon;
        recalcPoints();
    }

    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        double offsets[] = location.getOffsetFrom(getManeuverLocation());
        polygon.translate(offsets[0], offsets[1]);
        recalcPoints();
        super.setManeuverLocation(location);
    }

    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        polygon.translate(offsetNorth, offsetEast);
        recalcPoints();
        super.translate(offsetNorth, offsetEast, offsetDown);
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = super.additionalProperties();
        props.addAll(Arrays.asList(PluginUtils.getPluginProperties(this)));
        return props;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        PluginUtils.setPluginProperties(this, properties);
        recalcPoints();
    }
    
    public IMCMessage serializeToIMC() {
        IMCMessage msg = super.serializeToIMC();
        
        LinkedHashMap<String, Object> settings = new LinkedHashMap<>();
        settings.put("Pattern", getName());
        String pol = "";
        for (Vertex v : polygon.getVertices()) {
            pol += v.getLatitudeDegs() + ":"+v.getLongitudeDegs()+":";
        }
        settings.put("polygon", pol);
        if (calculateAngle)
            settings.put("angle", ""+angle);
        settings.put("corner", corner.toString());
        settings.put("step", ""+horizontalStep);

        msg.setValue("custom", IMCMessage.encodeTupleList(settings));

        return msg;
    }
    
    @Override
    public void parseIMCMessage(IMCMessage message) {
        super.parseIMCMessage(message);

        LinkedHashMap<String, String> customValues = message.getTupleList("custom");
        customSettings.clear();
        String pattern = customValues.remove("Pattern");
        if (!getName().equalsIgnoreCase(pattern))
            return;

        String pol[] = customValues.get("polygon").split(":");
        for (int i = 0; i < pol.length - 1; i +=2) {
            LocationType loc = new LocationType(Double.valueOf(pol[i]), Double.valueOf(pol[i+1]));
            polygon.addVertex(loc);
        }
        
        if (customValues.get("angle") != null) {
            calculateAngle = false;
            angle = Double.valueOf(customValues.get("angle"));
        }
        
        horizontalStep = Double.valueOf(customValues.get("step"));
        corner = Corner.valueOf(customValues.get("corner"));
        
        recalcPoints();
    }
    
    public Object clone() {
        AreaSurvey clone = (AreaSurvey) super.clone();
        clone.corner = corner;
        clone.polygon = polygon.clone();
        clone.horizontalStep = horizontalStep;
        clone.angle = angle;
        clone.calculateAngle = calculateAngle;
        clone.recalcPoints();
        return clone;
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document doc = super.getManeuverAsDocument(rootElementName);
        doc.remove(doc.getRootElement().element("path"));
        Element poly = doc.getRootElement().addElement("polygon");
        for (Vertex v : polygon.getVertices())
            poly.addElement("vertex").setText(v.getLatitudeDegs() + "," + v.getLongitudeDegs());
        Element horStep = doc.getRootElement().addElement("horizontalStep");
        horStep.setText("" + horizontalStep);

        Element angle = doc.getRootElement().addElement("bearing");
        angle.setText("" + this.angle);

        Element autoAngle = doc.getRootElement().addElement("autoBearing");
        autoAngle.setText("" + this.calculateAngle);

        Element corner = doc.getRootElement().addElement("corner");
        corner.setText("" + this.corner);
        return doc;
    }

    @Override
    public void loadManeuverFromXML(String xml) {
        super.loadManeuverFromXML(xml);
        try {
            Document doc = DocumentHelper.parseText(xml);

            List<?> list = doc.selectNodes("//*/vertex");
            for (Object o : list) {
                Element el = (Element) o;
                String[] parts = el.getText().split(",");
                LocationType loc = new LocationType(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                polygon.addVertex(loc);
            }

            Node horStep = doc.selectSingleNode("//horizontalStep");
            this.horizontalStep = Double.parseDouble(horStep.getText());

            Node corner = doc.selectSingleNode("//corner");
            this.corner = Corner.valueOf(corner.getText());

            Node bearing = doc.selectSingleNode("//bearing");
            this.angle = Double.parseDouble(bearing.getText());

            Node autoBearing = doc.selectSingleNode("//autoBearing");
            this.calculateAngle = Boolean.parseBoolean(autoBearing.getText());

            polygon.recomputePath();
            recalcPoints();
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
        }
    }
    
    public static void main(String[] args) {
        System.out.println(XMLUtil.getAsPrettyPrintFormatedXMLString(
                new AreaSurvey().asDocument().selectSingleNode("//maneuver").asXML()));
    }
}
