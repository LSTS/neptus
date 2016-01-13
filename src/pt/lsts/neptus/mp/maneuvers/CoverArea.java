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
 * Author: 
 * 20??/??/??
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JPopupMenu;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PolygonVertex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.gui.editor.SpeedUnitsEditor;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;

/**
 * @author zp
 * 
 */
public class CoverArea extends Maneuver implements LocatedManeuver, IMCSerialization, StateRendererInteraction {

    protected InteractionAdapter adapter = new InteractionAdapter(null);
    private double speed = 1000, speedTolerance = 0, radiusTolerance = 2;
    private String units = "RPM";
    private ManeuverLocation location = new ManeuverLocation();

//    private final int ANGLE_CALCULATION = -1 ;
//    private final int FIRST_ROTATE = 0 ;
//    private final int HORIZONTAL_MOVE = 1 ;
//    
//    private int current_state = ANGLE_CALCULATION;
    
    protected double targetAngle, rotateIncrement;
    
    @NeptusProperty(name = "Polygon", editable = false)
    public String polygonPoints = "";

    protected Vector<LocationType> points = new Vector<LocationType>();

//    private int count = 0;
    
    public String getType() {
        return "CoverArea";
    }
    
    @Override
    public void parseIMCMessage(IMCMessage message) {
        try {
            pt.lsts.imc.CoverArea area = pt.lsts.imc.CoverArea.clone(message);
            
            setSpeed(area.getSpeed());
            switch (area.getSpeedUnits()) {
                case METERS_PS:
                    setSpeedUnits("m/s");
                    break;
                case PERCENTAGE:
                    setSpeedUnits("%");
                    break;
                case RPM:
                    setSpeedUnits("RPM");
                    break;
            }
            ManeuverLocation pos = new ManeuverLocation();
            pos.setLatitudeRads(area.getLat());
            pos.setLongitudeRads(area.getLon());
            pos.setZ(area.getZ());
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(area.getZUnits().toString()));
            setManeuverLocation(pos);
            
            points.clear();
            Vector<PolygonVertex> vertices = message.getMessageList("polygon", PolygonVertex.class);
            for (PolygonVertex v : vertices)
                points.add(new LocationType(Math.toDegrees(v.getLat()), Math.toDegrees(v.getLon())));
            setCustomSettings(area.getCustom());
        }        
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.CoverArea coverArea = new pt.lsts.imc.CoverArea();
        
        LocationType l = getManeuverLocation();
        l.convertToAbsoluteLatLonDepth();
        
        coverArea.setLat(l.getLatitudeRads());
        coverArea.setLon(l.getLongitudeRads());
        coverArea.setZ(getManeuverLocation().getZ());
        coverArea.setZUnits(pt.lsts.imc.CoverArea.Z_UNITS.valueOf(getManeuverLocation().getZUnits().name()));
        coverArea.setSpeed(this.getSpeed());
       
        switch (this.getUnits()) {
            case "m/s":
                coverArea.setSpeedUnits(pt.lsts.imc.CoverArea.SPEED_UNITS.METERS_PS);
                break;
            case "RPM":
                coverArea.setSpeedUnits(pt.lsts.imc.CoverArea.SPEED_UNITS.RPM);
                break;
            case "%":
                coverArea.setSpeedUnits(pt.lsts.imc.CoverArea.SPEED_UNITS.PERCENTAGE);
                break;
            default:
                coverArea.setSpeedUnits(pt.lsts.imc.CoverArea.SPEED_UNITS.RPM);
                break;
        }
        
        Vector<PolygonVertex> vertices = new Vector<PolygonVertex>();
        
        for (LocationType pt : points )
            vertices.add(PolygonVertex.create("lat", pt.getLatitudeRads(), "lon", pt.getLongitudeRads()));
        coverArea.setMessageList(vertices, "polygon");
        coverArea.setCustom(getCustomSettings());
        
        return coverArea;
    }
    
//    public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
//        
//     SystemPositionAndAttitude nextVehicleState = (SystemPositionAndAttitude) lastVehicleState.clone();
//     
//        switch (current_state) {
//            case(ANGLE_CALCULATION):
//                targetAngle = lastVehicleState.getPosition().getXYAngle(location);
//                
//                double angleDiff = (targetAngle - lastVehicleState.getYaw());
//                
//                while (angleDiff < 0)
//                    angleDiff += Math.PI*2; //360º
//                
//                while (angleDiff > Math.PI*2)
//                    angleDiff -= Math.PI*2;
//                
//                if (angleDiff > Math.PI)
//                    angleDiff = angleDiff - Math.PI*2;
//                
//                rotateIncrement = angleDiff/3;//(-25.0f / 180.0f) * (float) Math.PI;
//                count = 0;
//                this.current_state = FIRST_ROTATE;
//                nextVehicleState = ManeuverFunction(lastVehicleState);
//            break;
//        
//            // Initial rotation towards the target point
//            case FIRST_ROTATE:
//                if (count++<3)
//                    nextVehicleState.rotateXY(rotateIncrement);
//                else {
//                    nextVehicleState.setYaw(targetAngle);       
//                    current_state = HORIZONTAL_MOVE;
//                }           
//                break;
//        
//            // The movement between the initial and final point, in the plane xy (horizontal)
//            case HORIZONTAL_MOVE:
//                double calculatedSpeed = 1;
//                
//                if (units.equals("m/s"))
//                    calculatedSpeed = speed;
//                else if (units.equals("RPM"))
//                    calculatedSpeed = speed/500.0;
//                double dist = nextVehicleState.getPosition().getHorizontalDistanceInMeters(location);
//                if (dist <= calculatedSpeed) {
//                    nextVehicleState.setPosition(location);
//                    endManeuver();
//                }
//                else {                  
//                        nextVehicleState.moveForward(calculatedSpeed);
//                        double depthDiff = location.getDepth()-nextVehicleState.getPosition().getDepth();
//                        
//                        double depthIncr = depthDiff / (dist/calculatedSpeed);
//                        double curDepth = nextVehicleState.getPosition().getDepth();
//                        nextVehicleState.getPosition().setDepth(curDepth+depthIncr);
//                }
//                break;
//            
//            default:
//                endManeuver();
//        }
//        
//        return nextVehicleState;
//    }

    @Override
    public String getName() {
        return "CoverArea maneuver";
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
        return false;
    }
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        
        LocationType man_loc = this.getStartLocation();
        Graphics2D g = (Graphics2D)g2d.create();
        g.translate(-renderer.getScreenPosition(man_loc).getX(), -renderer.getScreenPosition(man_loc).getY());

        List<Integer> x = new ArrayList<Integer>();
        List<Integer> y = new ArrayList<Integer>();
        
        x.add((int)renderer.getScreenPosition(man_loc).getX());
        y.add((int)renderer.getScreenPosition(man_loc).getY());
        
        for (LocationType loc : points) {
            Point2D pt = renderer.getScreenPosition(loc);
            Ellipse2D corners = new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10);
            x.add((int)pt.getX());
            y.add((int)pt.getY());
            g.setColor(Color.black);
            g.fill(corners);
        }
        
        int[] xx = new int[x.size()];
        int[] yy = new int[x.size()];
        
        for(int i = 0;i < xx.length;i++){
            xx[i] = x.get(i);
            yy[i] = y.get(i);
        }
        
        g.drawPolygon(xx, yy, x.size());
        g.dispose();
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        final StateRenderer2D r2d = source;
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            popup.add("Clear polygon").addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    points.clear();
                    r2d.repaint();
                }
            });

            popup.show(source, event.getX(), event.getY());
        }
        else {
            if (event.getClickCount() == 1) {
                Point2D clicked = event.getPoint();
                LocationType curLoc = source.getRealWorldLocation(clicked);
                points.add(curLoc);
                source.repaint();
            }
        }    
        adapter.mouseClicked(event, source);
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        adapter.mouseDragged(event, source);
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
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
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);

        NeptusLog.pub().info("<###>setActive: "+mode);
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
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        return location.clone();
    }

    @Override
    public ManeuverLocation getEndLocation() {
        return location.clone();
    }

    @Override
    public ManeuverLocation getStartLocation() {
        return location.clone();
    }

    @Override
    public void setManeuverLocation(ManeuverLocation loc) {
        double[] offset = location.getOffsetFrom(loc);
        location = loc.clone();
        for (LocationType lc : points) {
            lc.translatePosition(-offset[0], -offset[1], 0);
        }
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        location.translatePosition(offsetNorth, offsetEast, offsetDown);
        for (LocationType loc : points) {
            loc.translatePosition(offsetNorth, offsetEast, offsetDown);
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#loadFromXML(java.lang.String)
     */
    @Override
    public void loadFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            Node node = doc.selectSingleNode(getType()+"/finalPoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);
            setRadiusTolerance(Double.parseDouble(doc.selectSingleNode(getType()+"/finalPoint/radiusTolerance").getText()));
            Node speedNode = doc.selectSingleNode(getType()+"/speed");
            if (speedNode == null) 
                speedNode = doc.selectSingleNode(getType()+"/velocity");
            setSpeed(Double.parseDouble(speedNode.getText()));
            String speedUnit = speedNode.valueOf("@unit");
            setSpeedUnits(speedUnit);
            setSpeedTolerance(Double.parseDouble(speedNode.valueOf("@tolerance")));
            
        }
        catch (Exception e) {
            NeptusLog.pub().info("Error while loading the XML:" + "{" + xml + "}");
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    /**
     * @param parseDouble
     */
    private void setSpeedTolerance(double speedTolerance) {
        this.speedTolerance = speedTolerance;
    }
    
    public double getSpeedTolerance() {
        return speedTolerance;
    }

    /**
     * @param parseDouble
     */
    private void setSpeed(double speed) {
        this.speed = speed;
    }
    
    public double getSpeed() {
        return speed;
    }

    /**
     * @param speedUnit
     */
    private void setSpeedUnits(String units) {
        this.units = units;
    }
    
    public String getUnits() {
        return units;
    }

    /**
     * @param parseDouble
     */
    private void setRadiusTolerance(double radiusTolerance) {
        this.radiusTolerance = radiusTolerance;
    }
    
    public double getRadiusTolerance() {
        return radiusTolerance;
    }

    @Override
    public Object clone() {
        CoverArea clone = new CoverArea();
        try {
            clone = this.getClass().newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        super.clone(clone);
        clone.speed = speed;
        clone.units = units;
        clone.setManeuverLocation(location);
        
        return clone;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#getManeuverAsDocument(java.lang.String)
     */
    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        root.addAttribute("kind", "automatic");
        Element finalPoint = root.addElement("finalPoint");
        finalPoint.addAttribute("type", "pointType");
        Element point = location.asElement("point");
        finalPoint.add(point);

        Element radTolerance = finalPoint.addElement("radiusTolerance");
        radTolerance.setText(String.valueOf(getRadiusTolerance()));
       
        Element velocity = root.addElement("speed");
        velocity.addAttribute("tolerance", String.valueOf(getSpeedTolerance()));
        velocity.addAttribute("type", "float");
        velocity.addAttribute("unit", getUnits());
        velocity.setText(String.valueOf(getSpeed()));
        
        Element trajectoryTolerance = root.addElement("trajectoryTolerance");
        Element radiusTolerance = trajectoryTolerance.addElement("radiusTolerance");
        radiusTolerance.setText(String.valueOf(getRadiusTolerance()));

        return document;
    }
    
    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> properties = new Vector<DefaultProperty>();

        DefaultProperty units = PropertiesEditor.getPropertyInstance("Speed units", String.class, getUnits(), true);
        units.setDisplayName(I18n.text("Speed units"));
        units.setShortDescription(I18n.text("The speed units"));
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(units, new SpeedUnitsEditor());
        PropertiesEditor.getPropertyRendererRegistry().registerRenderer(units, new I18nCellRenderer());
    
        DefaultProperty propertySpeed = PropertiesEditor.getPropertyInstance("Speed", Double.class, getSpeed(), true);
        propertySpeed.setDisplayName(I18n.text("Speed"));
        properties.add(propertySpeed);
        properties.add(units);

        return properties;
    }
    
    
    public String getPropertiesDialogTitle() {    
        return getId()+" parameters";
    }
    
    public void setProperties(Property[] properties) {
        
        super.setProperties(properties);
        
        for (Property p : properties) {
            if (p.getName().equals("Speed units")) {
                setSpeedUnits((String)p.getValue());
            }
            else if (p.getName().equals("Speed tolerance")) {
                setSpeedTolerance((Double)p.getValue());
            }
            else if (p.getName().equals("Speed")) {
                setSpeed((Double)p.getValue());
            }
            else if (p.getName().equals("Radius tolerance")) {
                setRadiusTolerance((Double)p.getValue());
            }
            else {
                NeptusLog.pub().debug("Property "+p.getName()+" ignored.");
            }
        }
    }
    
    public String[] getPropertiesErrors(Property[] properties) {
        return super.getPropertiesErrors(properties);
    }
    
    public static void main(String[] args) {
        
        CoverArea compc = new CoverArea();
        String ccmanXML = compc.getManeuverAsDocument("CoverArea").asXML();
        System.out.println(ccmanXML);
        CoverArea compc1 = new CoverArea();
        compc1.loadFromXML(ccmanXML);
        ccmanXML = compc.getManeuverAsDocument("CoverArea").asXML();
        System.out.println(ccmanXML);
    }
}
