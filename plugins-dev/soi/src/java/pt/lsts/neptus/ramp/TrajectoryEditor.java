/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: RicardoSantos
 * March 28,2022
 */
package pt.lsts.neptus.ramp;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.gson.Gson;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.Goto;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.PlanTransition;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.imc.StationKeeping;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.endurance.Waypoint;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author RicardoSantos
 */
@PluginDescription(name = "Trajectory Manager", icon = "images/planning/planning.png",
        author = "Ricardo Santos", version = "1.7", category = PluginDescription.CATEGORY.INTERFACE)
@LayerPriority(priority = 100)
public class TrajectoryEditor extends InteractionAdapter implements Renderer2DPainter {

    @NeptusProperty(name = "Pollution markers endpoint", description = "Endpoint to GET pollution markers from Ripples")
    public String apiPollutionMarkers = "/pollution";

    @NeptusProperty(name = "Pollution trajectories endpoint", description = "Endpoint to GET trajectories from Ripples")
    public String apiPollutionTrajectories = "/pollution/trajectory";

    @NeptusProperty(name = "Speed", description = "Generated plan speed", units = "m/s")
    public float speed = 1.0f;

    @NeptusProperty(name = "Depth", description = "Generated plan depth or altitude if signal is inverted", units = "m")
    public float depth = 0.0f;

    @NeptusProperty(name = "Join trajectories", description="Join trajectories into a single trajectory to visit diferents pollution markers (minimizing number of trajectories)")
    protected boolean joinTrajectories = false;

    private StateRenderer2D renderer;
    private Graphics2D g0;

    private HashMap<Component, Object> componentList = new HashMap<>();

    protected JPanel sidePanel = null;
    protected JPanel controls;

    protected JButton displayTrajectoryBtn = new JButton(I18n.text("View"), ImageUtils.getScaledIcon("images/systems/view-info.png", 16, 16));
    protected JButton cleanTrajectoryBtn = new JButton(I18n.text("Clean"), ImageUtils.getScaledIcon("images/buttons/clear.png", 16, 16));
    protected JButton generateTrajectoryBtn = new JButton(I18n.text("Generate Plan"), ImageUtils.getScaledIcon("images/planning/edit_new.png", 16, 16));

    private String trajectorySelected = "Choose trajectory";

    private PropertySheetPanel propsPanel = null;

    protected  List<TrajectoryInfo> trajectoriesInfo = Collections.synchronizedList(new ArrayList<TrajectoryInfo>());
    protected List<Trajectory> pollutionTrajectories = Collections.synchronizedList(new ArrayList<Trajectory>());
    protected List<TrajectoryLayer.PollutionMarker> pollutionMarkers = Collections.synchronizedList(new ArrayList<TrajectoryLayer.PollutionMarker>());

    protected ArrayList<SystemProperty> oldProps = new ArrayList<>();

    /**
     * @param console
     */
    public TrajectoryEditor(ConsoleLayout console) {
        super(console);
    }

    @Override
    public Image getIconImage() {
        return ImageUtils.getImage(PluginUtils.getPluginIcon(getClass()));
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        this.g0 = g;
        this.renderer = renderer;

        Graphics2D g0 = (Graphics2D) g.create();
        for(Trajectory t : pollutionTrajectories) {
            for(Waypoint wp : t.waypoints) {
                LocationType loc = new LocationType(wp.getLatitude(), wp.getLongitude());
                Point2D pt2d = renderer.getScreenPosition(loc);
                wp.paintCircleWithLabel(g, wp, pt2d, Color.GREEN, 3, renderer.getZoom() > 0.2);
            }
        }
        g0.dispose();

        cleanTrajectoryBtn.setEnabled(pollutionTrajectories.size() > 0);
        generateTrajectoryBtn.setEnabled(pollutionTrajectories.size() > 0);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);

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
                c.add(horizontalSplit);

                c.invalidate();
                c.validate();
                horizontalSplit.setDividerLocation(0.75);
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new LineBorder(Color.orange.darker(), 3));
            }
        }
        else {
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
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

    private Component getSidePanel() {
        if (sidePanel == null) {

            sidePanel = new JPanel(new MigLayout());
            sidePanel.add(getTrajectoriesSelector(), "w 100%, h 5%, wrap");
            sidePanel.add(getOptionsPanel(), "w 100%, h 85%, wrap");

            controls = new JPanel(new GridLayout(0, 2));
            controls.add(displayTrajectoryBtn);
            controls.add(cleanTrajectoryBtn);
            controls.add(generateTrajectoryBtn);

            controls.setBorder(new TitledBorder(I18n.text("Actions")));

            displayTrajectoryBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {

                    // Get trajectory
                    pollutionTrajectories.clear();
                    try {
                        String serverRampApiUrl = GeneralPreferences.ripplesUrl + apiPollutionTrajectories;
                        // String serverRampApiUrl = "http://localhost:9090" + apiPollutionTrajectories;
                        URL url = new URL(serverRampApiUrl);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();

                        JsonArray trajectories = Json.parse(new InputStreamReader(con.getInputStream())).asArray();
                        for(int i=0 ; i< trajectories.size() ; i++) {
                            JsonObject alert = trajectories.get(i).asObject();
                            long alertID = alert.getLong("id", 0);

                            if(alertID == Long.parseLong(trajectorySelected)) {
                                JsonArray alertTrajectories = alert.get("trajectories").asArray();
                                for(int n=0; n<alertTrajectories.size() ; n++){
                                    JsonObject trajectory = alertTrajectories.get(n).asObject();
                                    String id = trajectory.get("pollutionMarkerID").asString();
                                    long duration = Float.valueOf(trajectory.get("duration").asFloat()).longValue();

                                    Trajectory t = new Trajectory(id,duration,trajectory.get("waypoints").asArray());
                                    pollutionTrajectories.add(t);
                                }
                            }
                        }

                    } catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
            });

            cleanTrajectoryBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    pollutionTrajectories.clear();
                }
            });

            generateTrajectoryBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {

                    if(joinTrajectories) {
                        createGlobalTrajectory();
                    } else {
                        createSingleTrajectories();
                    }
                }
            });

            displayTrajectoryBtn.setEnabled(false);
            cleanTrajectoryBtn.setEnabled(false);
            generateTrajectoryBtn.setEnabled(false);

            sidePanel.add(controls, BorderLayout.SOUTH);

            // Get pollution markers
            try {
                String serverRampApiUrl = GeneralPreferences.ripplesUrl + apiPollutionMarkers;
                // String serverRampApiUrl = "http://localhost:9090" + apiPollutionMarkers;
                Gson gson = new Gson();
                URL url = new URL(serverRampApiUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                TrajectoryLayer.PollutionMarker[] markers = gson.fromJson(new InputStreamReader(con.getInputStream()), TrajectoryLayer.PollutionMarker[].class);

                pollutionMarkers = Arrays.asList(markers);

            } catch (Exception e) {
                NeptusLog.pub().error(e);
            }

        }
        return sidePanel;
    }

    private void createSingleTrajectories() {
        int index = 0;
        for(Trajectory traj : pollutionTrajectories) {
            PlanSpecification ps = new PlanSpecification();
            List<PlanManeuver> data = new ArrayList<PlanManeuver>();
            List<PlanTransition> transitions = new ArrayList<PlanTransition>();

            // Add marker visited to plan name
            ps.setPlanId("trajectory_" + index + "-" + traj.id);

            int cleanSampleIndex = getCleanSampleIndex(traj);

            int wp_index = 0;
            for(Waypoint wp : traj.waypoints) {
                PlanManeuver pm = new PlanManeuver();

                // Sample parameters
                SetEntityParameters setParams = new SetEntityParameters();
                setParams.setName("Ramp Sampler");
                EntityParameter p1 = new EntityParameter();
                p1.setName("Type of Sample");

                // Dirty sample
                if(!traj.id.equals("0") && wp_index == traj.waypoints.size() -1 ) {
                    StationKeeping sk = new StationKeeping();
                    sk.setLat(Math.toRadians(wp.getLatitude()));
                    sk.setLon(Math.toRadians(wp.getLongitude()));
                    sk.setZ(depth);
                    sk.setZUnits(ZUnits.DEPTH);
                    sk.setDuration(600);
                    sk.setSpeed(speed);

                    pm.setManeuverId("sk_dirtySample_" + data.size());
                    p1.setValue("Dirty");
                    pm.setData(sk);
                }
                // Clean sample
                else if(cleanSampleIndex != 0 && wp_index == cleanSampleIndex) {
                    StationKeeping sk = new StationKeeping();
                    sk.setLat(Math.toRadians(wp.getLatitude()));
                    sk.setLon(Math.toRadians(wp.getLongitude()));
                    sk.setZ(depth);
                    sk.setZUnits(ZUnits.DEPTH);
                    sk.setDuration(600);
                    sk.setSpeed(speed);

                    pm.setManeuverId("sk_cleanSample_" + data.size());
                    p1.setValue("Clean");
                    pm.setData(sk);
                }
                else {
                    Goto go = new Goto();
                    go.setLat(Math.toRadians(wp.getLatitude()));
                    go.setLon(Math.toRadians(wp.getLongitude()));
                    go.setSpeedUnits(SpeedUnits.METERS_PS);
                    go.setSpeed(speed);
                    if(depth >= 0.0)
                        go.setZUnits(ZUnits.DEPTH);
                    else
                        go.setZUnits(ZUnits.ALTITUDE);
                    go.setZ(Math.abs(depth));

                    pm.setManeuverId("wp_" + data.size());
                    p1.setValue("None");
                    pm.setData(go);
                }

                setParams.setParams(Arrays.asList(p1));
                pm.setStartActions(Arrays.asList(setParams));

                if(!data.isEmpty()) {
                    PlanTransition pt = new PlanTransition();
                    pt.setConditions("ManeuverIsDone");
                    pt.setSourceMan(data.get(data.size()-1).getManeuverId());
                    pt.setDestMan(pm.getManeuverId());
                    transitions.add(pt);
                } else {
                    ps.setStartManId(pm.getManeuverId());
                }
                data.add(pm);
                wp_index++;

            }
            ps.setManeuvers(data);
            ps.setTransitions(transitions);

            PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(),ps);
            plan.setVehicle(getConsole().getMainSystem());
            getConsole().getMission().addPlan(plan);
            getConsole().warnMissionListeners();
            getConsole().getMission().save(false);
            index++;
        }
    }

    private void createGlobalTrajectory() {
        PlanSpecification ps = new PlanSpecification();
        List<PlanManeuver> data = new ArrayList<PlanManeuver>();
        List<PlanTransition> transitions = new ArrayList<PlanTransition>();
        String pollutionMarkerVisited = "";

        for(Trajectory traj : pollutionTrajectories) {

            // Add marker visited to plan name
            if(!traj.id.equals("0"))
                pollutionMarkerVisited = traj.id;

            int cleanSampleIndex = getCleanSampleIndex(traj);

            int wp_index = 0;
            for(Waypoint wp : traj.waypoints) {
                PlanManeuver pm = new PlanManeuver();

                // Sample parameters
                SetEntityParameters setParams = new SetEntityParameters();
                setParams.setName("Ramp Sampler");
                EntityParameter p1 = new EntityParameter();
                p1.setName("Type of Sample");

                // Dirty sample
                if(!traj.id.equals("0") && wp_index == traj.waypoints.size() -1 ) {
                    StationKeeping sk = new StationKeeping();
                    sk.setLat(Math.toRadians(wp.getLatitude()));
                    sk.setLon(Math.toRadians(wp.getLongitude()));
                    sk.setZ(depth);
                    sk.setZUnits(ZUnits.DEPTH);
                    sk.setDuration(600);
                    sk.setSpeed(speed);

                    pm.setManeuverId("sk_dirtySample_" + data.size());
                    p1.setValue("Dirty");
                    pm.setData(sk);
                }
                // Clean sample
                else if(cleanSampleIndex != 0 && wp_index == cleanSampleIndex) {
                    StationKeeping sk = new StationKeeping();
                    sk.setLat(Math.toRadians(wp.getLatitude()));
                    sk.setLon(Math.toRadians(wp.getLongitude()));
                    sk.setZ(depth);
                    sk.setZUnits(ZUnits.DEPTH);
                    sk.setDuration(600);
                    sk.setSpeed(speed);

                    pm.setManeuverId("sk_cleanSample_" + data.size());
                    p1.setValue("Clean");
                    pm.setData(sk);
                }
                else {
                    Goto go = new Goto();
                    go.setLat(Math.toRadians(wp.getLatitude()));
                    go.setLon(Math.toRadians(wp.getLongitude()));
                    go.setSpeedUnits(SpeedUnits.METERS_PS);
                    go.setSpeed(speed);
                    if(depth >= 0.0)
                        go.setZUnits(ZUnits.DEPTH);
                    else
                        go.setZUnits(ZUnits.ALTITUDE);
                    go.setZ(Math.abs(depth));

                    pm.setManeuverId("wp_" + data.size());
                    p1.setValue("None");
                    pm.setData(go);
                }

                setParams.setParams(Arrays.asList(p1));
                pm.setStartActions(Arrays.asList(setParams));

                if(!data.isEmpty()) {
                    PlanTransition pt = new PlanTransition();
                    pt.setConditions("ManeuverIsDone");
                    pt.setSourceMan(data.get(data.size()-1).getManeuverId());
                    pt.setDestMan(pm.getManeuverId());
                    transitions.add(pt);
                } else {
                    ps.setStartManId(pm.getManeuverId());
                }
                data.add(pm);
                wp_index++;
            }
        }

        ps.setPlanId("trajectory_global-" + pollutionMarkerVisited);
        ps.setManeuvers(data);
        ps.setTransitions(transitions);

        PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(),ps);
        plan.setVehicle(getConsole().getMainSystem());
        getConsole().getMission().addPlan(plan);
        getConsole().warnMissionListeners();
        getConsole().getMission().save(false);
    }

    private int getCleanSampleIndex(Trajectory traj) {
        for(int i=0; i< pollutionMarkers.size() ; i++)  {
            if(traj.id.equals(String.valueOf(pollutionMarkers.get(i).id))) {
                LocationType loc = new LocationType(pollutionMarkers.get(i).latitude, pollutionMarkers.get(i).longitude);
                Point2D pt2d = renderer.getScreenPosition(loc);
                float radius = renderer.getZoom() * pollutionMarkers.get(i).radius;
                Ellipse2D circle = new Ellipse2D.Double(pt2d.getX() - radius, pt2d.getY() - radius, radius*2, radius*2);

                for(int n=0 ; n<traj.waypoints.size() ; n++) {
                    LocationType wp_loc = new LocationType(traj.waypoints.get(n).getLatitude(),traj.waypoints.get(n).getLongitude());
                    Point2D wp_pt2d = renderer.getScreenPosition(wp_loc);
                    if(circle.contains(wp_pt2d)) {
                        return n-1;
                    }
                }
            }
        }
        return 0;
    }


    private JPanel getOptionsPanel() {
        JPanel optionsPanel = new JPanel(new MigLayout());
        optionsPanel.add(getTrajectoryPropertiesTable(), "w 100%, h 100%, wrap");
        return optionsPanel;
    }

    private Component getTrajectoriesSelector() {

        //-- Get trajectory info
        try {
            String serverRampApiUrl = GeneralPreferences.ripplesUrl + "/pollution/trajectory/info";
            // String serverRampApiUrl = "http://localhost:9090" + "/pollution/trajectory/info";
            URL url = new URL(serverRampApiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            JsonArray trajs = Json.parse(new InputStreamReader(con.getInputStream())).asArray();

            for(int i = 0 ; i < trajs.size() ; i++) {
                JsonObject wpt = trajs.get(i).asObject();

                long id = wpt.getLong("alertID", 0);
                float duration = wpt.getFloat("duration", 0);
                long timestamp = wpt.getLong("timestamp", 0);
                JsonArray markers = wpt.get("pollutionMarkers").asArray();

                Date date = new Date((long) (timestamp * 1E3));
                SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String departureTime = jdf.format(date);

                String durationTime = DateTimeUtil.milliSecondsToFormatedString((long) (duration * 1000.0));

                TrajectoryInfo trajInfo = new TrajectoryInfo(id, durationTime, departureTime, markers);
                trajectoriesInfo.add(trajInfo);

            }
        } catch (Exception e) {
            NeptusLog.pub().error(e);
        }

        LinkedList<String> myList = new LinkedList<>();
        myList.add("Choose trajectory");
        for(TrajectoryInfo t : trajectoriesInfo) {
            myList.add(String.valueOf(t.id));
        }
        String[] stringArray = myList.toArray(new String[0]);

        JComboBox<String> trajectoryList = new JComboBox<>(stringArray);

        // Add listener for main console trajectory change
        getConsole().addPollutionTrajectoryListener(id -> trajectoryList.getModel().setSelectedItem(id));
        trajectoryList.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            setTrajectory((String) e.getItem());
            updateVehiclePropsConstraints();
        });

        trajectoryList.getModel().setSelectedItem(trajectorySelected);

        return trajectoryList;
    }



    private void setTrajectory(String traj) {
        this.trajectorySelected = traj;
    }

    private PropertySheetPanel getTrajectoryPropertiesTable() {
        propsPanel = new PropertySheetPanel();
        propsPanel.setDescriptionVisible(true);
        propsPanel.setMode(PropertySheetPanel.VIEW_AS_CATEGORIES);

        propsPanel.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());
        propsPanel.setRendererFactory(PropertiesEditor.getPropertyRendererRegistry());
        propsPanel.setToolBarVisible(false);

        updateVehiclePropsConstraints();

        propsPanel.setBorder(new TitledBorder(I18n.text("Trajectory Properties")));

        return propsPanel;
    }

    private void updateVehiclePropsConstraints() {
        // Remove old properties from the PropertySheetPanel
        for(SystemProperty oldProp : oldProps) {
            if(propsPanel != null) {
                propsPanel.removeProperty(oldProp);
            }
        }

        ArrayList<SystemProperty> trajectoryProps = new ArrayList<>();
        for(TrajectoryInfo t : trajectoriesInfo) {

            String trajectoryID = String.valueOf(t.id);
            if (trajectorySelected.equals(trajectoryID)) {
                SystemProperty prop1 = new SystemProperty();
                prop1.setName("Alert ID");
                prop1.setDisplayName("Alert ID");
                prop1.setShortDescription("Alert ID created in Ripples");
                prop1.setValue(t.id);

                SystemProperty prop2 = new SystemProperty();
                prop2.setName("Total duration");
                prop2.setDisplayName("Duration");
                prop2.setShortDescription("Trajectory total duration");
                prop2.setValue(t.duration);

                SystemProperty prop3 = new SystemProperty();
                prop3.setName("Pollution markers");
                prop3.setDisplayName("Pollution markers");
                prop3.setShortDescription("Pollution markers to be visited");
                prop3.setValue(t.pollutionMarkers);

                SystemProperty prop4 = new SystemProperty();
                prop4.setName("Timestamp");
                prop4.setDisplayName("Departure time");
                prop4.setShortDescription("Time to start the trajectory");
                prop4.setValue(t.departureTime);

                trajectoryProps.add(prop1);
                trajectoryProps.add(prop2);
                trajectoryProps.add(prop3);
                trajectoryProps.add(prop4);
            }

        }

        oldProps = trajectoryProps;

        // Add trajectory props to the PropertySheetPanel
        if(trajectorySelected != "Choose trajectory") {
            for (SystemProperty sp : trajectoryProps) {
                sp.resetToDefault();
                if(propsPanel != null) {
                    propsPanel.addProperty(sp);
                }
            }
        }

        // Change btns visibility
        if(trajectorySelected.equals("") || trajectorySelected.equals("Choose trajectory")) {
            displayTrajectoryBtn.setEnabled(false);
            cleanTrajectoryBtn.setEnabled(false);
            generateTrajectoryBtn.setEnabled(false);
        } else {
            displayTrajectoryBtn.setEnabled(true);
            cleanTrajectoryBtn.setEnabled(true);
            generateTrajectoryBtn.setEnabled(true);
        }
    }

    public static class TrajectoryInfo {
        public long id;
        public String duration;
        public Vector<Long> pollutionMarkers;
        public String departureTime;

        public TrajectoryInfo(long trajID, String dur, String time, JsonArray markers) {
            id = trajID;
            duration = dur;
            departureTime = time;
            pollutionMarkers = new Vector<Long>();
            for (JsonValue p : markers) {
                Long markerID = Long.parseLong(p.asString());
                pollutionMarkers.add(markerID);
            }
        }

    }

    public static class Trajectory {
        public final String id;
        public final long duration;
        public Vector<Waypoint> waypoints;

        public Trajectory(String name, long t, JsonArray wpts){
            id = name;
            duration = t;
            waypoints = new Vector<Waypoint>();
            for (JsonValue p : wpts) {
                JsonObject point = p.asObject();

                float lat, lon, time;
                time = point.get("timestamp").asFloat();
                long arrival = Float.valueOf(time).longValue(); //UNIX Timestamp in seconds
                lat = point.get("latitude").asFloat();
                lon = point.get("longitude").asFloat();

                Waypoint wp = new Waypoint(waypoints.size(), lat,lon);
                wp.setArrivalTime(new Date(1000 * arrival));
                waypoints.add(wp);
            }
        }

    }

}
