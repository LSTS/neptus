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
 * Author: José Pinto
 * 15/03/2011
 */
package pt.lsts.neptus.mp.maneuvers;


import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PathPoint;
import pt.lsts.imc.TrajectoryPoint;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.gui.editor.SpeedUnitsEditor;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.AngleCalc;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 * @author pdias
 */
public class FollowTrajectory extends Maneuver implements LocatedManeuver, StatisticsProvider,
StateRendererInteraction, IMCSerialization, PathProvider {

    protected boolean hasTime = true;

    protected static final double RPM_MPS_CONVERSION = (1000/1.3);
    protected static final double RPM_PERCENT_CONVERSION = (1000/100);
    protected static final double PERCENT_MPS_CONVERSION = RPM_MPS_CONVERSION/RPM_PERCENT_CONVERSION;

    protected ManeuverLocation startLoc = new ManeuverLocation();
    LocationType previousLoc = null;
    protected double speed = 1000;
    protected String speed_units = "RPM";

    // points are [x,y,z,t] offsets 
    protected Vector<double[]> points = new Vector<double[]>();

    protected static final int X = 0, Y = 1, Z = 2, T = 3;
    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected Point2D lastDragPoint = null;
    protected boolean editing = false;

    public String getName() {
        return "FollowTrajectory";
    }

    public void setOffsets(Vector<double[]> coordinates) {
        this.points = coordinates;
    }
    
    @Override
    public void loadFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);

            // basePoint
            Node node = doc.selectSingleNode("//basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());

            setManeuverLocation(loc);

            // Velocity
            Node speedNode = doc.selectSingleNode("//speed");
            speed = Double.parseDouble(speedNode.getText());
            speed_units = speedNode.valueOf("@unit");

            List<?> list = doc.selectNodes("//*/nedOffsets");

            for (Object o : list) {
                Element el = (Element) o;
                double[] xyzt = new double[4];
                xyzt[X] = Double.parseDouble(el.selectSingleNode("@northOffset").getText());
                xyzt[Y] = Double.parseDouble(el.selectSingleNode("@eastOffset").getText());
                xyzt[Z] = Double.parseDouble(el.selectSingleNode("@depthOffset").getText());
                if (hasTime)
                    xyzt[T] = Double.parseDouble(el.selectSingleNode("@timeOffset").getText());
                else
                    xyzt[T] = -1;

                points.add(xyzt);                       
            }
        }
        catch (Exception e) {            
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    public Document getManeuverAsDocument(String rootElementName) {
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

        //trajectory
        Element points = root.addElement(hasTime?"trajectory":"path");
        for (double[] pt : this.points) {            
            Element tmp = points.addElement("nedOffsets");
            tmp.addAttribute("northOffset", ""+pt[X]);
            tmp.addAttribute("eastOffset", ""+pt[Y]);
            tmp.addAttribute("depthOffset", ""+pt[Z]);
            if (hasTime)
                tmp.addAttribute("timeOffset", ""+pt[T]);            
        }

        //velocity
        Element velocity = root.addElement("speed");        
        velocity.addAttribute("unit", speed_units);
        velocity.setText(""+speed);

        return document;
    }

    @Override
    public Object clone() {
        FollowTrajectory clone;
        try {
            clone = this.getClass().newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            clone = hasTime ? new FollowTrajectory() : new FollowPath();
        }
        super.clone(clone);
        clone.speed = speed;
        clone.speed_units = speed_units;
        clone.setManeuverLocation(startLoc);
        for (double[] val : points)
            clone.points.add(Arrays.copyOf(val, val.length));
        return clone;
    }

    public Image getIconImage() {
        return adapter.getIconImage();
    }

    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    public boolean isExclusive() {
        return true;
    }

    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        g2d.rotate(-renderer.getRotation());
        g2d.rotate(-Math.PI/2);
        ManeuversUtil.paintPointLineList(g2d, renderer.getZoom(), points, false, 0, editing);
        g2d.rotate(Math.PI/2);
        g2d.rotate(renderer.getRotation());
    }

    protected void editPointsDialog(Window parent) {
        String times = "";
        NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
        for (double[] pt : points) {
            times += nf.format(pt[0]) + ", " + nf.format(pt[1]) + ", " + nf.format(pt[2])
                    + (hasTime ? ", " + nf.format(pt[3]) : "") + "\n";
        }

        final JEditorPane epane = new JEditorPane();
        epane.setText(times);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(epane));
        final JDialog dialog = new JDialog(parent);
        dialog.getContentPane().add(mainPanel);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setTitle((hasTime ? "Trajectory" : "Path") + " points (N, E, D"
                + (hasTime ? ", T" : "") + ")");
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int opt = JOptionPane.showConfirmDialog(dialog, "Do you want to save changes?",
                        "Edit points", JOptionPane.YES_NO_CANCEL_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    String[] lines = epane.getText().split("\n");
                    Vector<double[]> pts = new Vector<double[]>();
                    for (int i = 0; i < lines.length; i++){
                        String[] parts = lines[i].split(",");
                        try {
                            double[] pt = new double[4];
                            pt[0] = Double.parseDouble(parts[0].trim());
                            pt[1] = Double.parseDouble(parts[1].trim());
                            pt[2] = Double.parseDouble(parts[2].trim());
                            pt[3] = hasTime ? Double.parseDouble(parts[3].trim()) : -1;
                            pts.add(pt);
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(dialog, "Error parsing text",
                                    "Invalid syntax on line " + (i + 1));
                            ex.printStackTrace();
                        }
                    }
                    FollowTrajectory.this.points = pts;                                
                    dialog.setVisible(false);
                    dialog.dispose();
                }
                else if (opt == JOptionPane.NO_OPTION) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }                                                           
            }
        });
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setSize(500, 600);
        GuiUtils.centerParent(dialog, parent);
        dialog.setVisible(true);
    }


    public void mouseClicked(MouseEvent event, final StateRenderer2D source) {

        final StateRenderer2D r2d = source;
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            popup.add("Clear " + (hasTime?"trajectory":"path")).addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    points.clear();
                    r2d.repaint();
                }
            });

            popup.add("Edit points as text").addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    editPointsDialog(SwingUtilities.getWindowAncestor(source));//(Component)e.getSource()));
                }
            });

            popup.show(source, event.getX(), event.getY());
        }
        else {
            if (event.getClickCount() == 1) {
                Point2D clicked = event.getPoint();
                LocationType curLoc = source.getRealWorldLocation(clicked);
                double distance, _speed;

                // do any required speed conversions
                _speed = speed;            
                if (speed_units.equalsIgnoreCase("RPM"))
                    _speed /= RPM_MPS_CONVERSION;
                else if (speed_units.equalsIgnoreCase("%"))
                    _speed /= PERCENT_MPS_CONVERSION;

                double[] offsets = source.getRealWorldLocation(clicked).getOffsetFrom(startLoc);

                double xyzt[] = new double[4];
                for (int i = 0; i < 3; i++)
                    xyzt[i] = offsets[i];

                if (hasTime) {
                    // calculate distance and time required from previous point
                    if (previousLoc != null)
                        distance = curLoc.getDistanceInMeters(previousLoc);
                    else 
                        distance = curLoc.getDistanceInMeters(startLoc);
                    xyzt[3] = distance / _speed;
                }
                else
                    xyzt[3] = -1;

                boolean skip = false;
                if (!event.isAltDown() && !event.isAltGraphDown()) {
                    if (points.size() == 0)
                        points.add(new double[] { 0, 0, 0, hasTime ? 0 : -1 });
                }
                else {
                    if (points.size() > 0) {
                        points.remove(points.size() - 1);
                        skip = true;
                    }
                }

                if (!skip) {
                    points.add(xyzt);
                    previousLoc = curLoc;
                }
                source.repaint();
            }
        }    
        adapter.mouseClicked(event, source);
    }

    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
    }

    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (lastDragPoint == null) {
            adapter.mouseDragged(event, source);
            lastDragPoint = event.getPoint();
            return;
        }
        double yammount = event.getPoint().getY() - lastDragPoint.getY();
        yammount = -yammount;
        if (event.isShiftDown()) {
            double bearingRad = Math.toRadians(yammount/10);

            while (bearingRad > Math.PI * 2)
                bearingRad -= Math.PI * 2;            
            while (bearingRad < 0)
                bearingRad += Math.PI * 2;

            for (double[] pt : points) {
                double[] ret = AngleCalc.rotate(bearingRad, pt[X], pt[Y], false);
                pt[X] = ret[X];
                pt[Y] = ret[Y];
            }
        }
        else {
            adapter.mouseDragged(event, source);
        }
        lastDragPoint = event.getPoint();
    }

    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);        
    }

    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);        
        lastDragPoint = null;
    }

    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);        
    }

    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

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

    public void setActive(boolean mode, StateRenderer2D source) {
        editing = mode;
        source.setLockedVehicle(null);
        source.setRotation(0);
        previousLoc = null;
    }
    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) { }


    public double getCompletionTime(LocationType initialPosition) {

        double initialDistance = startLoc.getDistanceInMeters(initialPosition);
        double speed = this.speed;

        if (this.speed_units.equalsIgnoreCase("RPM"))
            speed = speed/RPM_MPS_CONVERSION;

        else if (this.speed_units.equalsIgnoreCase("%"))
            speed = speed/PERCENT_MPS_CONVERSION;

        double time = initialDistance / speed;

        for (double[] p : points)
            time += p[3];

        return time;
    }

    public double getDistanceTravelled(LocationType initialPosition) {
        double sum = startLoc.getDistanceInMeters(initialPosition);

        double[] l = new double[] {0,0,0,0};
        for (double[] p : points) {
            sum += Math.sqrt((p[X]-l[X])*(p[X]-l[X])+(p[Y]-l[Y])*(p[Y]-l[Y])+(p[Z]-l[Z])*(p[Z]-l[Z]));
            l = p;
        }
        return sum;
    }

    public double getMaxDepth() {
        double maxDepth = startLoc.getAllZ(), curDepth = maxDepth;

        for (double[] p : points) {            
            curDepth += p[Z];
            if (curDepth > maxDepth)
                maxDepth = curDepth;
        }
        return maxDepth;
    }

    public double getMinDepth() {
        double minDepth = startLoc.getAllZ(), curDepth = minDepth;

        for (double[] p : points) {            
            curDepth += p[Z];
            if (curDepth < minDepth)
                minDepth = curDepth;
        }
        return minDepth;
    }

    @Override
    public ManeuverLocation getManeuverLocation() {        
        return startLoc.clone();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.LocationProvider#getFirstPosition()
     */
    @Override
    public ManeuverLocation getStartLocation() {
        return getManeuverLocation();
    }

    @Override
    public ManeuverLocation getEndLocation() {
        ManeuverLocation loc = startLoc.clone();
        if (!points.isEmpty())
            loc.translatePosition(points.lastElement());
        return loc;            
    }

    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        startLoc = location.clone();
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        startLoc.translatePosition(offsetNorth, offsetEast, offsetDown);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.PathProvider#getPathPoints()
     */
    @Override
    public List<double[]> getPathPoints() {
        return Collections.unmodifiableList(points);
    }

    @Override
    public List<LocationType> getPathLocations() {
        Vector<LocationType> locs = new Vector<>();
        List<double[]> lst = Collections.unmodifiableList(points);
        LocationType start = new LocationType(getManeuverLocation());
        for (double[] ds : lst) {
            LocationType loc = new LocationType(start);
            loc.translatePosition(ds);
            loc.convertToAbsoluteLatLonDepth();
            locs.add(loc);
        }
        return locs;
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

    public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
        return null;
    }

    public double getRadiusTolerance() {
        return 0;
    }

    public IMCMessage serializeToIMC() {
        double lld[] = startLoc.getAbsoluteLatLonDepth();

        // conversion into absolute times
        double[]  absoluteTimes = new double[points.size()];
        double lastTime = 0;
        for (int i = 0; i < points.size(); i++) {
            try {
                lastTime += points.get(i)[T];
            }
            catch (Exception e) {
                e.printStackTrace();
                lastTime += -1;
            }
            absoluteTimes[i] = lastTime;            
        }

        if (hasTime) {
            Vector<TrajectoryPoint> pointMessages = new Vector<>();

            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);

                TrajectoryPoint point = new TrajectoryPoint();
                point.setX(p[X]);
                point.setY(p[Y]);
                point.setZ(p[Z]);
                point.setT(absoluteTimes[i]);
                pointMessages.add(point);
            }
            pt.lsts.imc.FollowTrajectory trajMessage = new pt.lsts.imc.FollowTrajectory();
            trajMessage.setPoints(pointMessages);
            trajMessage.setLat(Math.toRadians(lld[0]));
            trajMessage.setLon(Math.toRadians(lld[1]));
            trajMessage.setZ(getManeuverLocation().getZ());
            trajMessage.setZUnits(pt.lsts.imc.FollowTrajectory.Z_UNITS.valueOf(
                    getManeuverLocation().getZUnits().toString()));
            trajMessage.setSpeed(speed);
            try {
                String speedU = this.getUnits();
                if ("m/s".equalsIgnoreCase(speedU))
                    trajMessage.setSpeedUnits(pt.lsts.imc.FollowTrajectory.SPEED_UNITS.METERS_PS);
                else if ("RPM".equalsIgnoreCase(speedU))
                    trajMessage.setSpeedUnits(pt.lsts.imc.FollowTrajectory.SPEED_UNITS.RPM);
                else if ("%".equalsIgnoreCase(speedU) )
                    trajMessage.setSpeedUnits(pt.lsts.imc.FollowTrajectory.SPEED_UNITS.PERCENTAGE);
                else if ("percentage".equalsIgnoreCase(speedU))
                    trajMessage.setSpeedUnits(pt.lsts.imc.FollowTrajectory.SPEED_UNITS.PERCENTAGE);
            }
            catch (Exception ex) {
                NeptusLog.pub().error(this, ex);                     
            }
            trajMessage.setCustom(getCustomSettings());
            trajMessage.setTimeout(getMaxTime());
            return trajMessage;
        }
        else {
            Vector<PathPoint> pointMessages = new Vector<>();
            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);

                PathPoint point = new PathPoint();
                point.setX(p[X]);
                point.setY(p[Y]);
                point.setZ(p[Z]);
                pointMessages.add(point);
            }
            pt.lsts.imc.FollowPath pathMessage = new pt.lsts.imc.FollowPath();
            pathMessage.setPoints(pointMessages);
            pathMessage.setLat(Math.toRadians(lld[0]));
            pathMessage.setLon(Math.toRadians(lld[1]));
            pathMessage.setZ(getManeuverLocation().getZ());
            pathMessage.setZUnits(pt.lsts.imc.FollowPath.Z_UNITS.valueOf(
                    getManeuverLocation().getZUnits().toString()));            
            pathMessage.setSpeed(speed);
            try {
                String speedU = this.getUnits();
                if ("m/s".equalsIgnoreCase(speedU))
                    pathMessage.setSpeedUnits(pt.lsts.imc.FollowPath.SPEED_UNITS.METERS_PS);
                else if ("RPM".equalsIgnoreCase(speedU))
                    pathMessage.setSpeedUnits(pt.lsts.imc.FollowPath.SPEED_UNITS.RPM);
                else if ("%".equalsIgnoreCase(speedU))
                    pathMessage.setSpeedUnits(pt.lsts.imc.FollowPath.SPEED_UNITS.PERCENTAGE);
                else if ("percentage".equalsIgnoreCase(speedU))
                    pathMessage.setSpeedUnits(pt.lsts.imc.FollowPath.SPEED_UNITS.PERCENTAGE);
            }
            catch (Exception ex) {
                NeptusLog.pub().error(this, ex);                     
            }
            pathMessage.setCustom(getCustomSettings());
            pathMessage.setTimeout(getMaxTime());
            return pathMessage;
        }           
    }

    public void parseIMCMessage(IMCMessage message) {
        setMaxTime(message.getAsNumber("timeout").intValue());
        startLoc = new ManeuverLocation();
        startLoc.setLatitudeRads(message.getDouble("lat"));
        startLoc.setLongitudeRads(message.getDouble("lon"));
        startLoc.setZ(message.getDouble("z"));
        String units = message.getString("z_units");
        if (units != null)
            startLoc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(units));
        speed = message.getDouble("speed");
        customSettings = message.getTupleList("custom");
        String speed_units = message.getString("speed_units");
        if (speed_units.equals("METERS_PS"))
            this.speed_units = "m/s";
        else if (speed_units.equals("RPM"))
            this.speed_units = "RPM";
        else
            this.speed_units = "%";

        points.clear();
        Vector<IMCMessage> pts = message.getMessageList("points");
        for (IMCMessage pt : pts) {
            if (hasTime)
                points.add(new double[] { pt.getDouble("x"), pt.getDouble("y"), pt.getDouble("z"),
                        pt.getDouble("t") });
            else
                points.add(new double[] { pt.getDouble("x"), pt.getDouble("y"), pt.getDouble("z"), -1 });
        }

        if (hasTime) {
            double lastPTime = 0;
            for (double[] ptt : points) {
                double aux = ptt[T];
                ptt[T] -= lastPTime;
                lastPTime = aux;
            }
        }
    }

    public String getUnits() {
        return speed_units;
    }

    public void setSpeedUnits(String units) {
        this.speed_units = units;
        recalculateTimes();
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    protected void recalculateTimes() {
        if (!hasTime)
            return;

        double dist, _speed;

        // do any required speed conversions
        _speed = speed;            
        if (speed_units.equalsIgnoreCase("RPM"))
            _speed /= RPM_MPS_CONVERSION;
        else if (speed_units.equalsIgnoreCase("%"))
            _speed /= PERCENT_MPS_CONVERSION;

        for (int i = 0; i < points.size(); i++) {
            if (i == 0)
                dist = Math.sqrt(points.get(0)[X]*points.get(0)[X] + points.get(0)[Y]*points.get(0)[Y] + points.get(0)[Z]*points.get(0)[Z]);
            else
                dist = Math.sqrt(
                        Math.pow(points.get(i)[X]-points.get(i-1)[X], 2) +
                        Math.pow(points.get(i)[Y]-points.get(i-1)[Y], 2) +
                        Math.pow(points.get(i)[Z]-points.get(i-1)[Z], 2)
                        );
            points.get(i)[T] = dist / _speed;
        }
    }

    @Override
    public String getTooltipText() {
        NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
        return super.getTooltipText()+"<hr>"+
        I18n.text("speed") + ": <b>"+nf.format(speed)+" "+I18n.text(speed_units)+"</b>"+
        "<br>" + I18n.text("points") + ": <b>"+points.size()+"</b>";
    }

    protected static void test1() {
        FollowTrajectory traj = new FollowTrajectory();
        traj.points.add(new double[]{0,1,2,3});
        traj.points.add(new double[]{4,5,6,7});
        String xml = traj.getManeuverAsDocument("FollowTrajectory").asXML();
        NeptusLog.pub().info("<###> "+traj.getManeuverAsDocument("FollowTrajectory").asXML());
        FollowTrajectory other = new FollowTrajectory();
        other.loadFromXML(xml);
        NeptusLog.pub().info("<###> "+other.getManeuverAsDocument("FollowTrajectory").asXML());
        other.serializeToIMC().dump(System.out);
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> properties = new Vector<DefaultProperty>();

        DefaultProperty units = PropertiesEditor.getPropertyInstance("Speed units", String.class, getUnits(), true);
        units.setShortDescription("The speed units");
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(units, new SpeedUnitsEditor());
        PropertiesEditor.getPropertyRendererRegistry().registerRenderer(units, new I18nCellRenderer());

        properties.add(PropertiesEditor.getPropertyInstance("Speed", Double.class, getSpeed(), true));
        properties.add(units);

        return properties;
    }

    public void setProperties(Property[] properties) {

        super.setProperties(properties);

        for (Property p : properties) {
            if (p.getName().equals("Speed units")) {
                setSpeedUnits((String)p.getValue());
            }
            else if (p.getName().equals("Speed")) {
                setSpeed((Double)p.getValue());
            }            
        }
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
    }

    public static void main(String[] args) {
        // test1();

        FollowTrajectory traj = new FollowTrajectory();
        traj.loadFromXML("<FollowTrajectory kind=\"automatic\"><basePoint type=\"pointType\"><point><id>id_53802104</id><name>id_53802104</name><coordinate><latitude>0N0'0''</latitude><longitude>0E0'0''</longitude><depth>0.0</depth></coordinate></point><radiusTolerance>0.0</radiusTolerance></basePoint><trajectory><nedOffsets northOffset=\"0.0\" eastOffset=\"1.0\" depthOffset=\"2.0\" timeOffset=\"3.0\"/><nedOffsets northOffset=\"4.0\" eastOffset=\"5.0\" depthOffset=\"6.0\" timeOffset=\"7.0\"/></trajectory><speed unit=\"RPM\">1000.0</speed></FollowTrajectory>");
        //NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowTrajectory")));
        traj.setSpeed(1);
        traj.setSpeedUnits("m/s");        
        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowTrajectory")));

        traj.setSpeed(2);
        traj.setSpeedUnits("m/s");        
        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowTrajectory")));
        //test2();
    }

}
