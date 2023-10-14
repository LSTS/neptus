/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 23/08/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXB;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.gui.editor.PolygonPropertyEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.coord.PolygonType.Vertex;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author zp
 *
 */
public class AutonomousSection extends Maneuver
        implements ManeuverWithSpeed, IMCSerialization, LocatedManeuver, StateRendererInteraction, StatisticsProvider {
    
    protected double latitudeDegs = 0, longitudeDegs = 0;
    
    @NeptusProperty(name = "Speed")
    protected SpeedType speed = new SpeedType(1, Units.MPS);
    
    @NeptusProperty(name = "Enforce Depth Limit")
    protected boolean enforceDepth = true;
    
    @NeptusProperty(name = "Maximum Depth", units = "m")
    protected double maxDepth = 100;
    
    @NeptusProperty(name = "Enforce Altitude Limit")
    protected boolean enforceAlt = true;

    @NeptusProperty(name = "Minimum Altitude", units = "m")
    protected double minAlt = 2;
    
    @NeptusProperty(name = "Enforce Time Limit")
    protected boolean enforceTime = true;
    
    @NeptusProperty(name = "Timeout", description="Time limit, in seconds, after which the maneuver completes", units = "s")
    protected double timeout = 1800;
    
    @NeptusProperty(name = "Enforce Area Limits")
    protected boolean enforceArea = true;
    
    @NeptusProperty(name="Area limits", editorClass = PolygonPropertyEditor.class)
    protected PolygonType areaLimits = new PolygonType();

    @NeptusProperty(name = "Controlling Agent")
    protected String controller = "";
    
    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected PolygonType.Vertex vertex = null;
    
    @Override
    public double getCompletionTime(LocationType initialPosition) {
        if (enforceTime)
            return timeout;
        return 0;
    }
    
    @Override
    public double getMaxDepth() {
        if (enforceDepth)
            return maxDepth;
        return 0;
    }
    
    @Override
    public double getMinDepth() {
        return 0;
    }
    
    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        return getCompletionTime(initialPosition) * speed.getMPS();
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
        if (!SwingUtilities.isRightMouseButton(event)) {
            adapter.mouseClicked(event, source);
            return;
        }
        
        Vertex v = intercepted(event, source);
        JPopupMenu popup = new JPopupMenu();
        if (v != null) {
            popup.add(I18n.text("Edit location")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LocationType l = new LocationType(v.getLocation());
                    LocationType newLoc = LocationPanel.showLocationDialog(source, I18n.text("Edit Vertex Location"), l, getMissionType(), true);
                    if (newLoc != null) {
                        newLoc.convertToAbsoluteLatLonDepth();
                        v.setLocation(newLoc);
                        areaLimits.recomputePath();
                    }                        
                    source.repaint();                    
                }
            });            
            popup.add(I18n.text("Remove vertex")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    areaLimits.removeVertex(v);
                    source.repaint();
                }
            });            
        }
        else
            popup.add(I18n.text("Add vertex")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LocationType loc = source.getRealWorldLocation(event.getPoint());
                    areaLimits.addVertex(loc.getLatitudeDegs(), loc.getLongitudeDegs());
                    source.repaint();
                }
            });
        
        popup.show(source, event.getX(), event.getY());
    }
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        Vertex v = intercepted(event, source);
        if (v == null)
            adapter.mousePressed(event, source);
        else
            vertex = v;
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (vertex == null)
            adapter.mouseDragged(event, source);
        else {
            LocationType loc = source.getRealWorldLocation(event.getPoint());
            vertex.setLocation(loc);
            areaLimits.recomputePath();     
        }
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
        if (vertex != null)
            areaLimits.recomputePath();            
        vertex = null;
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
        adapter.setAssociatedSwitch(tswitch);
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
        vertex = null;
        adapter.setActive(mode, source);        
    }
    
    public PolygonType.Vertex intercepted(MouseEvent evt, StateRenderer2D source) {
        for (PolygonType.Vertex v : areaLimits.getVertices()) {
            Point2D pt = source.getScreenPosition(new LocationType(v.getLocation()));
            if (pt.distance(evt.getPoint()) < 5) {
                return v;
            }
        }
        return null;
    }

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        g.setTransform(source.getIdentity());
        areaLimits.paint(g, source);        
        areaLimits.getVertices().forEach(v -> {
            Point2D pt = source.getScreenPosition(new LocationType(v.getLocation()));
            Ellipse2D ellis = new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10);
            Color c = Color.yellow;
            g.setColor(new Color(255-c.getRed(),255-c.getGreen(),255-c.getBlue(),200));
            g.fill(ellis);
            g.setColor(c);
            g.draw(ellis);
        });
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        ManeuverLocation ret = new ManeuverLocation();
        ret.setLatitudeDegs(latitudeDegs);
        ret.setLongitudeDegs(longitudeDegs);
        ret.setZ(0);
        ret.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        return ret;
    }

    @Override
    public ManeuverLocation getEndLocation() {
        return getManeuverLocation();
    }

    @Override
    public ManeuverLocation getStartLocation() {
        return getManeuverLocation();
    }

    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        LocationType prevLocation = getManeuverLocation();
        location.convertToAbsoluteLatLonDepth();
        latitudeDegs = location.getLatitudeDegs();
        longitudeDegs = location.getLongitudeDegs();      
        
        areaLimits.getVertices().forEach(v -> {
            LocationType l = new LocationType(v.getLocation());
            double[] offsets = l.getOffsetFrom(prevLocation);
            l.setLocation(location);
            l.translatePosition(offsets).convertToAbsoluteLatLonDepth();
            v.setLocation(l);
        });
        areaLimits.recomputePath();
    }

    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        ArrayList<ManeuverLocation> ret = new ArrayList<>();
        ret.add(getManeuverLocation());
        return ret;
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        ManeuverLocation l = getManeuverLocation();
        l.translatePosition(offsetNorth, offsetEast, offsetDown);
        setManeuverLocation(l);
        areaLimits.translate(offsetNorth, offsetEast);
    }

    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.AutonomousSection maneuver = new pt.lsts.imc.AutonomousSection();
        LocationType l = getManeuverLocation().convertToAbsoluteLatLonDepth();
        maneuver.setLat(l.getLatitudeRads());
        maneuver.setLon(l.getLongitudeRads());
        speed.setSpeedToMessage(maneuver);
        
        maneuver.setMaxDepth(maxDepth);
        maneuver.setMinAlt(minAlt);
        maneuver.setTimeLimit(timeout);
        maneuver.setAreaLimits(IMCUtils.serializePolygon(areaLimits));
        maneuver.setController(controller);
        
        short flags = 0;
        if (enforceAlt)
            flags |= pt.lsts.imc.AutonomousSection.ENFORCE_ALTITUDE;
        if (enforceDepth)
            flags |= pt.lsts.imc.AutonomousSection.ENFORCE_DEPTH;
        if (enforceArea)
            flags |= pt.lsts.imc.AutonomousSection.ENFORCE_AREA2D;
        if (enforceTime)
            flags |= pt.lsts.imc.AutonomousSection.ENFORCE_TIMEOUT;
        maneuver.setLimits(flags);

        return maneuver;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        pt.lsts.imc.AutonomousSection man = null;
        try {
            man = pt.lsts.imc.AutonomousSection.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        latitudeDegs = Math.toDegrees(man.getLat());
        longitudeDegs = Math.toDegrees(man.getLon());
        setSpeed(SpeedType.parseImcSpeed(message));

        timeout = man.getTimeLimit();
        maxDepth = man.getMaxDepth();
        minAlt = man.getMinAlt();
        areaLimits = IMCUtils.parsePolygon(man.getAreaLimits());
        areaLimits.recomputePath();
        controller = man.getController();
        
        short limits = man.getLimits();
        enforceAlt = (limits & pt.lsts.imc.AutonomousSection.ENFORCE_ALTITUDE) != 0;
        enforceDepth = (limits & pt.lsts.imc.AutonomousSection.ENFORCE_DEPTH) != 0;
        enforceArea = (limits & pt.lsts.imc.AutonomousSection.ENFORCE_AREA2D) != 0;
        enforceTime = (limits & pt.lsts.imc.AutonomousSection.ENFORCE_TIMEOUT) != 0;
    }

    public SpeedType getSpeed() {
        return speed;
    }

    public void setSpeed(SpeedType speed) {
        this.speed = speed;
    }

    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            
            ManeuversXMLUtil.parseLocation(doc.getRootElement(), this);
            try {
                SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            maxDepth = Double.parseDouble(doc.selectSingleNode("//maxDepth").getText());
            enforceDepth = Boolean.parseBoolean(doc.selectSingleNode("//maxDepth/@enforced").getText());

            minAlt = Double.parseDouble(doc.selectSingleNode("//minAltitude").getText());
            enforceAlt = Boolean.parseBoolean(doc.selectSingleNode("//minAltitude/@enforced").getText());
            
            timeout = Double.parseDouble(doc.selectSingleNode("//timeout").getText());
            enforceTime = Boolean.parseBoolean(doc.selectSingleNode("//timeout/@enforced").getText());
            try {
                controller = doc.selectSingleNode("//controller").getText();
            }
            catch (Exception e) {
                controller = "";
            }
            
            Node nd = doc.selectSingleNode("//area").detach();
            
            nd.setName("polygonType");
            areaLimits = JAXB.unmarshal(new StringReader(nd.asXML()), PolygonType.class);
            areaLimits.recomputePath();
            vertex = null;
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }        
    }

    @Override
    public Object clone() {
        AutonomousSection sec = new AutonomousSection();
        super.clone(sec);
        sec.loadManeuverFromXML(getManeuverXml());
        return sec;
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        
        Document doc = ManeuversXMLUtil.createBaseDoc(getType());
        ManeuversXMLUtil.addLocation(doc.getRootElement(), this);
        try {
            SpeedType.addSpeedElement(doc.getRootElement(), this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();

        root.addElement("maxDepth").addAttribute("enforced", ""+enforceDepth).setText(""+maxDepth);
        root.addElement("minAltitude").addAttribute("enforced", ""+enforceAlt).setText(""+minAlt);
        root.addElement("timeout").addAttribute("enforced", ""+enforceTime).setText(""+timeout);
        
        if (controller != null && !controller.isEmpty())
            root.addElement("controller").setText(controller);
        
        try {
            StringWriter writer = new StringWriter();
            JAXB.marshal(this.areaLimits, writer);
            Element area = DocumentHelper.parseText(writer.toString()).getRootElement();
            area.setName("area");
            area.addAttribute("enforced", ""+enforceArea);
            root.add(area);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
        areaLimits.recomputePath();
    }
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        AffineTransform t = g2d.getTransform();
        g2d.setTransform(renderer.getIdentity());
        
        if (adapter.isActive())
            paintInteraction(g2d, renderer);
        else {
            if (enforceArea)
                areaLimits.paint(g2d, renderer);
        }
                        
        
        g2d.setTransform(t);
        super.paintOnMap(g2d, planElement, renderer);
    }
    
    public static void main(String[] args) {
        AutonomousSection sec = new AutonomousSection();
        
        sec.latitudeDegs = 41;
        sec.longitudeDegs = -8;
        
        for (int i = 0; i < 6; i++) {
            LocationType l = new LocationType(sec.latitudeDegs, sec.longitudeDegs);
            l.setAzimuth(60 * i);
            l.setOffsetDistance(400);
            l.convertToAbsoluteLatLonDepth();
            sec.areaLimits.addVertex(l.getLatitudeDegs(), l.getLongitudeDegs());
        }
        String xml = sec.getManeuverAsDocument("AutonomousSection").asXML();
        
        System.out.println(FileUtil.getAsPrettyPrintFormatedXMLString(FileUtil.getAsCompactFormatedXMLString(xml)));
        
        sec.loadManeuverFromXML(xml);
    }
}
