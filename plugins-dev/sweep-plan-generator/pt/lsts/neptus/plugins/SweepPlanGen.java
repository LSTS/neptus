package pt.lsts.neptus.plugins;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.PropertiesTable;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.*;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.ws.Location;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

@PluginDescription(name = "Sweep Plan Generator")
public class SweepPlanGen extends InteractionAdapter implements Renderer2DPainter {

    protected PolygonType.Vertex vertex = null;
    protected Vector<MapPanel> maps = new Vector<>();
    protected JPanel sidePanel = null;
    private JPanel controls;
    private HashMap<Component, Object> componentList = new HashMap<>();
    private MultiVehicleDynamicSurveyOptions options = new MultiVehicleDynamicSurveyOptions();
    private double sweepAngle = -Math.PI/180;

    //MAP INTERACTION
    private PolygonType task = null;
    private PolygonType selectedTask = null;
    private PolygonType.Vertex startPoint = null;
    private PolygonType.Vertex endPoint = null;
    private PlanElement planElement = null;
    private PlanType generated = null;

    public SweepPlanGen(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        StateRenderer2D renderer = source;

        JSplitPane horizontalSplit;
        if (mode) {
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                componentList.clear();
                BorderLayout bl = (BorderLayout) c.getLayout();
                for (Component component : c.getComponents()) {
                    Object constraint = bl.getConstraints(component);
                    componentList.put(component, constraint);
                }

                Component comp = bl.getLayoutComponent(BorderLayout.CENTER);

                horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, comp, getSidePanel());
                horizontalSplit.setResizeWeight(1.0);

                c.add(horizontalSplit);

                c.invalidate();
                c.validate();
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new LineBorder(Color.orange.darker(), 3));
            }
        } else {
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                // c.remove(getSidePanel());
                c.removeAll();
                for (Map.Entry<Component, Object> e : componentList.entrySet()) {
                    c.add(e.getKey(), e.getValue());
                }
                componentList.clear();

                c.invalidate();
                c.validate();
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new EmptyBorder(0, 0, 0, 0));
            }
        }
    }

    private JPanel getSidePanel() {

        if (sidePanel == null) {
            sidePanel = new JPanel(new BorderLayout(2, 2));

            JLabel statsLabel = new JLabel();
            statsLabel.setText("my text");

            controls = new JPanel(new GridLayout(0, 3));

            controls.add(new JButton("My Button"));
            controls.add(new JButton("My Button"));
            controls.add(new JButton("My Button"));
            controls.add(new JButton("My Button"));
            controls.add(new JButton("My Button"));
            controls.add(new JButton("My Button"));
            controls.setBorder(new TitledBorder(I18n.text("Plan")));

            sidePanel.add(getVehicleSelector(),BorderLayout.PAGE_START);
            sidePanel.add(getAngleSelector(),BorderLayout.SOUTH);
            //sidePanel.add(controls, BorderLayout.SOUTH);
            //sidePanel.add(statsLabel, BorderLayout.SOUTH);
            sidePanel.add(getOptionsPanel(),BorderLayout.CENTER);
            
            controls.setOpaque(false);
        }
        return sidePanel;
    }

    private JComboBox<String> getVehicleSelector() {

        //JComboBox<VehicleType> vehicleList = new JComboBox<VehicleType>();
        return new JComboBox<String>(VehiclesHolder.getVehiclesArray());

        /*if (plan == null && getConsole().getPlan() != null) {
            setPlan(getConsole().getPlan().clonePlan());
        }
        else if (plan == null) {

            VehicleType choice = null;
            if (getConsole().getMainSystem() != null)
                choice = VehicleChooser.showVehicleDialog(null,
                        VehiclesHolder.getVehicleById(getConsole().getMainSystem()), getConsole());
            else
                choice = VehicleChooser.showVehicleDialog(null, null, getConsole());

            if (choice == null) {
                if (getAssociatedSwitch() != null)
                    getAssociatedSwitch().doClick();

                return;
            }
            PlanType plan = new PlanType(getConsole().getMission());
            plan.setVehicle(choice.getId());
            setPlan(plan);
        }
        else {
            setPlan(plan);
        }*/
    }

    private JPanel getAngleSelector() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Sweep Angle");
        panel.add(label);

        JSpinner angleSpinner;
        SpinnerNumberModel angleSpinnerModel;
        Double current = (double) -1;
        Double min = (double) -1;
        Double max = 365d;
        Double step = 1d;
        angleSpinnerModel = new SpinnerNumberModel(current, min, max, step);
        angleSpinner = new JSpinner(angleSpinnerModel);
        angleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent evt) {
                double newValue = (double)((JSpinner)evt.getSource()).getValue();
                if (task != null){
                    sweepAngle = newValue * Math.PI/180;
                    generatePlan();
                }
            }
        });
        ((JSpinner.DefaultEditor)angleSpinner.getEditor()).getTextField().setColumns(5);

        panel.add(angleSpinner);
        label.setLabelFor(angleSpinner);
        return panel;
    }

    private JPanel getOptionsPanel() {
        PropertiesProvider provider = new PropertiesProvider() {
            @Override
            public DefaultProperty[] getProperties() {
                return PluginUtils.getPluginProperties(options);
            }

            @Override
            public void setProperties(Property[] properties) {
                PluginUtils.setPluginProperties(options, properties);
            }

            @Override
            public String getPropertiesDialogTitle() {
                return PluginUtils.getPluginName(options.getClass());
            }

            @Override
            public String[] getPropertiesErrors(Property[] properties) {
                return null;
            }
        };

        PropertiesTable propTable= new PropertiesTable();
        propTable.editProperties(provider);
        propTable.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (task != null){
                    getConsole().warnMissionListeners();
                    generatePlan();
                }
            }
        });
        return propTable;
    }

    // MAP INTERACTION
    LocationType lastPoint = null;
    Point2D lastScreenPoint = null;
    PolygonType.Vertex clickedVertex = null;

    public boolean containsPoint(LocationType lt, StateRenderer2D renderer,int error){
        if (task.containsPoint(lt))
            return true;

        Point2D screen = renderer.getScreenPosition(lt);
        for (PolygonType.Vertex v : task.getVertices()) {
            Point2D pt = renderer.getScreenPosition(v.getLocation());

            if (pt.distance(screen) < error) {
                return true;
            }
        }
        return false;
    }

    public PolygonType.Vertex getVertexAt(PolygonType poly, LocationType lt, int error, StateRenderer2D renderer){
        Point2D screen = renderer.getScreenPosition(lt);
        for (PolygonType.Vertex v : poly.getVertices()) {
            Point2D pt = renderer.getScreenPosition(v.getLocation());
            if (pt.distance(screen) < error) {
                return v;
            }
        }
        return null;
    }

    public boolean isVertex(StateRenderer2D renderer, LocationType lt, PolygonType.Vertex vertex, int error) {
        Point2D screen = renderer.getScreenPosition(lt);
        Point2D pt = renderer.getScreenPosition(vertex.getLocation());
        return pt.distance(screen) < error;
    }

    @Override
    public void mouseClicked(MouseEvent e, StateRenderer2D source) {
        if (!SwingUtilities.isRightMouseButton(e)) {
            super.mouseClicked(e, source);
            return;
        }

        PolygonType survey = null;
        LocationType lt = source.getRealWorldLocation(e.getPoint());

        if (task != null && task.containsPoint(lt)) {
            survey = task;
        }

        JPopupMenu popup = new JPopupMenu();

        if (startPoint != null && isVertex(source, lt, startPoint,10)) {
           popup.add("<html><b>Remove</b> start point").addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                   startPoint = null;
                   updatePlan(source);
               }
           });
        }

        if(survey != null) {
            popup.add("<html><b>Delete</b> Survey").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    task = null;
                    endPoint = null;
                    startPoint = null;
                    planElement = null;
                }
            });
        }

        if(task != null) {
            // check if a polygon vertex was hit
            PolygonType.Vertex selectedVertex = getVertexAt(task,source.getRealWorldLocation(e.getPoint()),10,source);
            if (selectedVertex != null && task.getVertices().size() > 3) {
                popup.add("<html><b>Remove</b> vertex").addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        task.removeVertex(selectedVertex);
                        task.recomputePath();
                        updatePlan(source);
                    }
                });
            }
            popup.add("<html>Add <b>Vertex</b>").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    task.addVertex(source.getRealWorldLocation(e.getPoint()));
                    task.recomputePath();
                    updatePlan(source);
                }
            });
            popup.add("<html>Add <b>Start</b> Point").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    startPoint = new PolygonType.Vertex(source.getRealWorldLocation(e.getPoint()));
                    updatePlan(source);
                }
            });
        } else {
            popup.add("<html>New <b>Survey</b>").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    // ADD NEW SURVEY
                    task = new PolygonType();
                    task.setColor(Color.red);

                    LocationType clickedLocation = source.getRealWorldLocation(e.getPoint());
                    LocationType nw = new LocationType(clickedLocation), ne = new LocationType(clickedLocation),
                            sw = new LocationType(clickedLocation), se = new LocationType(clickedLocation);

                    nw.translatePosition(60, -60, 0);
                    ne.translatePosition(60, 60, 0);
                    sw.translatePosition(-60, -60, 0);
                    se.translatePosition(-60, 60, 0);
                    task.addVertex(nw);
                    task.addVertex(ne);
                    task.addVertex(se);
                    task.addVertex(sw);
                    task.recomputePath();
                    planElement = new PlanElement();
                    updatePlan(source);
                }
            });
            popup.add("<html>Test Function").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    // TODO: 01/02/2019 remove this
                    //PluginUtils.getPluginProperties(new MultiVehicleSurveyOptions());
                }
            });
        }

        if(task != null && generated != null) {
            popup.add("<html><b>Save</b> Plan to Mission").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    savePlan();
                }
            });
        }

        popup.show(source, e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        if(task != null && startPoint != null && isVertex(source,source.getRealWorldLocation(event.getPoint()),startPoint,10)){
            selectedTask = task;
            lastPoint = source.getRealWorldLocation(event.getPoint());
            lastScreenPoint = event.getPoint();
            clickedVertex = startPoint;
            return;
        }
        if (task != null && containsPoint(source.getRealWorldLocation(event.getPoint()),source,10)) {
            selectedTask = task;
            lastPoint = source.getRealWorldLocation(event.getPoint());
            lastScreenPoint = event.getPoint();

            for (int i = 0; i < task.getVertices().size(); i++) {
                PolygonType.Vertex v = task.getVertices().get(i);
                Point2D pt = source.getScreenPosition(v.getLocation());
                if (pt.distance(event.getPoint()) < 15) {
                    clickedVertex = v;
                    return;
                }
            }

            clickedVertex = null;
        } else {
            super.mousePressed(event, source);
        }
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if(selectedTask == null){
            super.mouseDragged(event,source);
            return;
        }

        if (clickedVertex != null) {
            clickedVertex.setLocation(source.getRealWorldLocation(event.getPoint()));
            task.recomputePath();
        } else if (lastPoint != null) {
            LocationType loc = source.getRealWorldLocation(event.getPoint());
            double offsets[] = loc.getOffsetFrom(lastPoint);
            task.translate(offsets[0], offsets[1]);
        }

        updatePlan(source);

        lastPoint = source.getRealWorldLocation(event.getPoint());
        lastScreenPoint = event.getPoint();
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        if(selectedTask != null) {
            lastPoint = null;
            lastScreenPoint = null;
            clickedVertex = null;

            try {
                updatePlan(source);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        selectedTask = null;
        super.mouseReleased(event, source);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(task != null && planElement != null && isActive()) {
            task.paint(g,renderer);
            g.setTransform(renderer.getIdentity());
            for (PolygonType.Vertex v : task.getVertices()) {
                Point2D pt = renderer.getScreenPosition(v.getLocation());
                g.fill(new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10));
            }
            if(startPoint != null){
                Point2D pt = renderer.getScreenPosition(startPoint.getLocation());
                g.setColor(Color.RED);
                g.fill(new Ellipse2D.Double(pt.getX()-5, pt.getY()-5,10,10));
            }
            g.setTransform(renderer.getIdentity());
            planElement.paint(g,renderer);
        }
    }

    private double getPathLength(ArrayList<LocationType> list) {
        double dist = 0;
        for (int i = 1; i < list.size(); i++) {
            dist += list.get(i - 1).getHorizontalDistanceInMeters(list.get(i));
        }
        return dist;
    }

    private ManeuverLocation createLoc(LocationType loc) {
        ManeuverLocation manLoc;
        manLoc = new ManeuverLocation(loc);
        if (options.depth >= 0) {
            manLoc.setZ(options.depth);
            manLoc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        }
        else {
            manLoc.setZ(/*-options.depth*/4);
            manLoc.setZUnits(ManeuverLocation.Z_UNITS.ALTITUDE);
        }

        return manLoc;
    }

    private void addStartManeuver(PlanType plan){
        if (startPoint == null)
            return;

        LocationType lastLoc;

        Goto start = new Goto();
        start.setId("Start");
        lastLoc = new ManeuverLocation(startPoint.getLocation());
        ManeuverLocation mloc = createLoc(lastLoc);
        mloc.setZ(0);
        mloc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        start.setManeuverLocation(mloc);
        generated.getGraph().addManeuverAtEnd(start);
        start.setManeuverLocation(mloc);
        start.getManeuverLocation().setAbsoluteDepth(0);
        lastLoc = new LocationType(start.getManeuverLocation());

        PopUp man = new PopUp();
        man.setId("Start_Popup");
        man.setManeuverLocation(createLoc(lastLoc));
        man.setDuration(options.popupDuration);
        generated.getGraph().addManeuverAtEnd(man);
    }

    private void addEndManeuver(PlanType plan){
        if (endPoint == null)
            return;

        LocationType lastLoc;

        Goto end = new Goto();
        end.setId("End");
        lastLoc = new ManeuverLocation(endPoint.getLocation());
        ManeuverLocation mloc = createLoc(lastLoc);
        mloc.setZ(0);
        mloc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        end.setManeuverLocation(mloc);
        generated.getGraph().addManeuverAtEnd(end);
        end.setManeuverLocation(mloc);
        end.getManeuverLocation().setAbsoluteDepth(0);
        lastLoc = new LocationType(end.getManeuverLocation());

        PopUp man = new PopUp();
        man.setId("End_Popup");
        man.setManeuverLocation(createLoc(lastLoc));
        man.setDuration(options.popupDuration);
        generated.getGraph().addManeuverAtEnd(man);
    }

    private void generatePlan() {

        Pair<Double, Double> diamAngle = task.getDiameterAndAngle();
        double angle = sweepAngle != -Math.PI/180 ? sweepAngle : diamAngle.second();

        int corner = options.corner;

        /*for (int i = 0; i<4 ; i++){
            System.out.println("Length for i = "+i+" : " + task.getPathLength(options.swathWidth,i));
            //System.out.println("Better Length for i = "+i+" : " + getPathLength(task.getCoveragePath(angle, options.swathWidth, corner)));
        }*/

        generated = new PlanType(getConsole().getMission());

        int manId = 1;
        LocationType lastLoc;
        long curTime = System.currentTimeMillis() + options.startInMins * 60_000;


        double shortestDistance = Double.MAX_VALUE;

        // Find Shortest Distance Corner
        for (int i = 0; i < 4; i++) {
            ArrayList<LocationType> tempCoverage = task.getCoveragePath(angle,options.swathWidth,i);
            LocationType covStart = tempCoverage.get(0);
            LocationType covEnd = tempCoverage.get(tempCoverage.size()-1);
            double distance = 0;
            // add distance to start point
            if(startPoint != null){
                distance += covStart.getDistanceInMeters(startPoint.getLocation());
            }
            //add distance to end point
            if(endPoint != null){
                distance += covEnd.getDistanceInMeters(endPoint.getLocation());
            }
            distance += getPathLength(tempCoverage);
            //System.out.println("Better Length for i = "+i+" : " + distance);
            if(distance < shortestDistance){
                shortestDistance = distance;
                corner = i;
            }
        }

        //System.out.println("Selected Corner: "+corner+" with total distance: "+shortestDistance);

        if(options.corner != -1)
            corner = options.corner;

        ArrayList<LocationType> coverage = task.getCoveragePath(angle, options.swathWidth, corner);

        if (options.reversed)
            Collections.reverse(coverage);

        addStartManeuver(generated);

        if (options.timedPlan) {
            ScheduledGoto m1 = new ScheduledGoto();
            m1.setId("SG"+manId++);
            m1.setManeuverLocation(createLoc(coverage.remove(0)));
            m1.setArrivalTime(new Date(curTime));
            generated.getGraph().addManeuverAtEnd(m1);
            lastLoc = new LocationType(m1.getManeuverLocation());
        }
        else {
            Goto m1 = new Goto();
            m1.setId("Go"+manId++);
            lastLoc = new ManeuverLocation(coverage.remove(0));
            ManeuverLocation mloc = createLoc(lastLoc);
            mloc.setZ(0);
            mloc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
            m1.setManeuverLocation(mloc);
            generated.getGraph().addManeuverAtEnd(m1);
            m1.setManeuverLocation(mloc);
            m1.getManeuverLocation().setAbsoluteDepth(0);
            lastLoc = new LocationType(m1.getManeuverLocation());

            PopUp man = new PopUp();
            man.setId("P"+manId++);
            man.setManeuverLocation(createLoc(lastLoc));
            man.setDuration(options.popupDuration);
            generated.getGraph().addManeuverAtEnd(man);
        }
        long lastPopup = System.currentTimeMillis();

        FollowTrajectory traj = null;

        while(!coverage.isEmpty()) {
            LocationType loc = coverage.remove(0);

            double distanceToTarget = lastLoc.getDistanceInMeters(loc);
            long targetEta = (long) ((distanceToTarget / options.speedMps) * 1000 + curTime);

            if ((targetEta - lastPopup)/60_000.0 > options.popupMins) {
                if (traj != null)
                    generated.getGraph().addManeuverAtEnd(traj);
                traj = null;

                //add popup
                PopUp man = new PopUp();
                man.setId("P"+manId++);
                ManeuverLocation mloc = createLoc(lastLoc);
                man.setManeuverLocation(mloc);
                man.setDuration(options.popupDuration);
                man.setWaitAtSurface(options.popupWaitAtSurface);
                generated.getGraph().addManeuverAtEnd(man);
                lastPopup = curTime + options.popupDuration * 1_000;
                targetEta += options.popupDuration * 1_000;

            }

            if (traj == null) {
                if (options.timedPlan) {
                    traj = new FollowTrajectory();
                    traj.setId("FT"+manId++);
                }
                else {
                    traj = new FollowPath();
                    traj.setId("FP"+manId++);
                }
                traj.setManeuverLocation(createLoc(lastLoc));
                Vector<double[]> curPath = new Vector<>();
                curPath.add(new double[] {0, 0, 0, 0});
                traj.setOffsets(curPath);

            }

            Vector<double[]> curPath = new Vector<>(traj.getPathPoints());
            double[] offsets = loc.getOffsetFrom(traj.getManeuverLocation());
            curPath.add(new double[] {offsets[0], offsets[1], offsets[2], (targetEta - curTime) / 1000.0});
            traj.setOffsets(curPath);
            lastLoc = loc;
            curTime = targetEta;
        }

        if (traj != null)
            generated.getGraph().addManeuverAtEnd(traj);


        StationKeeping man = new StationKeeping();
        man.setId("SK"+manId++);
        ManeuverLocation mloc = createLoc(lastLoc);
        mloc.setZ(0);
        mloc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        man.setManeuverLocation(mloc);
        man.setDuration(0);
        generated.getGraph().addManeuverAtEnd(man);
        man.getManeuverLocation().setAbsoluteDepth(0);

        addEndManeuver(generated);

        PlanUtil.setPlanSpeed(generated, options.speedMps);

        generated.setId(options.planId);
        planElement.setPlan(generated);
    }

    private void updatePlan(StateRenderer2D renderer) {
        generatePlan();
        planElement.recalculateManeuverPositions(renderer);
    }

    private void savePlan() {
        getConsole().getMission().addPlan(generated);
        getConsole().getMission().save(true);
        getConsole().warnMissionListeners();
    }

    public static class MultiVehicleDynamicSurveyOptions {
        @NeptusProperty(name="Swath Width", description="Cross-track region covered by each vehicle")
        public double swathWidth = 180;

        @NeptusProperty(name="Depth", description= "Depth at which to travel (negative for altitude)")
        public double depth = 4;

        @NeptusProperty(name="Speed (m/s)", description="Speed to use while travelling")
        public double speedMps = 1.2;

        @NeptusProperty(name="Minutes till first point", description="Amount of minutes to travel to the first waypoint")
        public int startInMins = 1;

        @NeptusProperty(name="Create timed plan", description="Opt to generate desired ETA for each waypoint")
        public boolean timedPlan = false;

        @NeptusProperty(name="Popup periodicity in minutes", description="Do not stay underwater more than this time (minutes)")
        public int popupMins = 30;

        @NeptusProperty(name="Popup duration in seconds", description="How long to stay at surface when the vehicle pops up")
        public int popupDuration = 45;

        @NeptusProperty(name="Popup Wait at surface", description="If set, the vehicle will wait <duration> seconds before diving, otherwise will dive after GPS fix.")
        public boolean popupWaitAtSurface = true;

        @NeptusProperty(name="Generated plan id", description="Name of the generated plan")
        public String planId = "plan_wiz";

        @NeptusProperty(name="Reversed", description="Reverse plan")
        public boolean reversed = false;

        @NeptusProperty(name="Corner", description="First Corner")
        public int corner = -1;
    }
}
