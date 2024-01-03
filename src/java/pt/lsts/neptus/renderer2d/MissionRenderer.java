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
 * Author: 
 * 27/Dez/2004
 */
package pt.lsts.neptus.renderer2d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.renderer3d.Camera3D;
import pt.lsts.neptus.renderer3d.Obj3D;
import pt.lsts.neptus.renderer3d.Object3DCreationHelper;
import pt.lsts.neptus.renderer3d.Renderer3D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.DynamicElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * This class creates a component that will allow the user to preview the 
 * execution of a given mission. 
 * @author ZP
 */
public class MissionRenderer extends JPanel implements ActionListener, ChangeListener, VehicleStateListener {

    static final long serialVersionUID = 14;

    private StateRenderer2D renderer2d;
    private Renderer3D renderer3d;
    private MapGroup myMapGroup = null;

    public static final int R2D_ONLY = 0, R3D_1CAM = 1, R2D_AND_R3D1CAM = 2, R3D_MULTICAMS = 3, R2D_AND_R3D_MULTICAMS = 4;

    private VehicleType vehicle = null;

    private SystemPositionAndAttitude curstate, initState = new SystemPositionAndAttitude(new LocationType(), 0,0,0);
    private Renderer[] renderers;
    private PlanType plan;
    private int currentMode = Renderer.TRANSLATION;
    private ClassLoader cl = this.getClass().getClassLoader();

    private JButton pauseMode, restart, findVehicle, tailClean, selectPainters;
    private ToolbarSwitch interpolate, followMode, tailOnOff;

    private JToggleButton zoomMode, translateMode,
    rotateMode, rulerMode;

    private ButtonGroup bgroup = new ButtonGroup();

    private JToolBar toolbar;
    private JLabel status = new JLabel();
    private JComponent renderPanel;
    private Hashtable<VehicleType, SystemPositionAndAttitude> shownVehicles = new Hashtable<VehicleType, SystemPositionAndAttitude>();
    private Hashtable<VehicleType, EstimatedStateGenerator> stateInterpolators = new Hashtable<VehicleType, EstimatedStateGenerator>();

    private VehicleType mainVehicle = null;
    private MissionType mission = null;		

    private int vehicleIterator = 0;

    boolean generateIntermediateStates = true;
    private long millisBetweenInterpolatedStates = 100;
    private long millisUntilGenerationEnd = 2000;
    private Timer interpolationTimer = null;
    private PlanElement po;
    private MapType localMap = new MapType();



    public void startInterpolatingStates() {
        if (interpolationTimer != null)
            interpolationTimer.cancel();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                for (VehicleType vt : stateInterpolators.keySet()) {

                    if (stateInterpolators.get(vt).timeSinceLastState() > millisUntilGenerationEnd)
                        continue;

                    SystemPositionAndAttitude tmp = stateInterpolators.get(vt).getInterpolatedState();	
                    for (int i = 0; i < renderers.length; i++) {
                        renderers[i].vehicleStateChanged(vt.getId(), tmp);
                    }
                }
            }
        };

        interpolationTimer = new Timer("State Interpolator");
        interpolationTimer.schedule(task, millisBetweenInterpolatedStates, millisBetweenInterpolatedStates);
    }

    public void stopInterpolatingStates() {
        if (interpolationTimer != null)
            interpolationTimer.cancel();
    }

    public void setVehicleState(VehicleType vehicle, SystemPositionAndAttitude state) {
        if (vehicle == null)
            return;

        this.curstate = state;

        for (int i = 0; i < renderers.length; i++) {
            renderers[i].vehicleStateChanged(vehicle.getId(), state);
        }
        shownVehicles.put(vehicle, state);

        if (stateInterpolators.containsKey(vehicle))
            stateInterpolators.get(vehicle).setState(state);
        else {
            EstimatedStateGenerator gen = new EstimatedStateGenerator();
            gen.setState(state);
            stateInterpolators.put(vehicle, gen);
        }

        followMode.setEnabled(true);
        findVehicle.setEnabled(true);
    }

    public MissionRenderer(PlanType plan, MissionType mission, int shownRenderers) {		
        this(plan, MapGroup.getMapGroupInstance(mission), shownRenderers);
        this.mission = mission;
    }

    public MissionRenderer(MissionType mission, int shownRenderers) {
        this(null, MapGroup.getMapGroupInstance(mission), shownRenderers);
    }

    /**
     * Class constructor
     * @param plan The missionplan of the mission to be rendered
     * @param delay The tick time for the execution (in milliseconds)
     */
    public MissionRenderer(PlanType plan, MapGroup map, int shownRenderers) {

        this.plan = plan;	    
        this.myMapGroup = map;
        map.addMap(localMap);

        LocationType startLocation = new LocationType();
        double angle = 0;

        if (map != null && plan!=null) {
            if (map.getMapObjectsByID("start").length > 0) {
                startLocation.setLocation(map.getMapObjectsByID("start")[0].getCenterLocation());
                Maneuver startMan = plan.getGraph().getManeuver(plan.getGraph().getInitialManeuverId());
                if (startMan instanceof Goto) {					
                    Goto g = (Goto) startMan;
                    angle = startLocation.getXYAngle(g.getManeuverLocation());					
                }
            }
            else {
                startLocation.setLocation(map.getCoordinateSystem());
            }
        }

        switch(shownRenderers) {
            case (R2D_ONLY): {
                StateRenderer2D r2d = new StateRenderer2D(map);				
                r2d.setMapGroup(map);
                r2d.focusLocation(map.getCoordinateSystem());
                r2d.addChangeListener(this);
                po = new PlanElement(map, new MapType());
                po.setPlan(plan);
                po.setRenderer(r2d);
                //po.setColor(new Color(255,255,255,128));
                po.setShowDistances(false);
                po.setShowManNames(false);
                r2d.addPostRenderPainter(po, "Plan Painter");
                //r2d.addPainter(new PlanPainter(plan));
                renderers = new Renderer[] {r2d};
                renderPanel = new JPanel();
                renderPanel.setLayout(new BorderLayout());
                renderPanel.add(r2d, BorderLayout.CENTER);
                renderer2d = r2d;
                break;					
            }
            case (R3D_1CAM): {
                Renderer3D r3d;
                Camera3D cam =new Camera3D(Camera3D.USER);
                Camera3D[]  cams={cam};
                r3d = new Renderer3D(cams,(short)1,(short)1);
                r3d.setMapGroup(map);
                r3d.focusLocation(map.getCoordinateSystem());
                r3d.addChangeListener(this);
                r3d.setBackgroundType((short)2);

                if (plan != null) {
                    Obj3D o3d = Object3DCreationHelper.getPlanModel3D(plan); // plan.getModel3D();				
                    if (o3d != null)
                        r3d.addObj3D(o3d);				
                }

                renderers = new Renderer[] {r3d};
                renderPanel = new JPanel();
                renderPanel.setLayout(new BorderLayout());
                renderPanel.add(r3d, BorderLayout.CENTER);
                renderer3d = r3d;
                break;
            }
            case (R3D_MULTICAMS): {
                Renderer3D r3d;
                r3d = new Renderer3D();				
                r3d.setMapGroup(map);
                r3d.focusLocation(map.getCoordinateSystem());
                r3d.addChangeListener(this);
                r3d.setBackgroundType((short)2);

                if (plan != null) {
                    Obj3D o3d = Object3DCreationHelper.getPlanModel3D(plan); // plan.getModel3D();				
                    if (o3d != null)
                        r3d.addObj3D(o3d);				
                }

                renderers = new Renderer[] {r3d};
                renderPanel = new JPanel();
                renderPanel.setLayout(new BorderLayout());
                renderPanel.add(r3d, BorderLayout.CENTER);
                renderer3d = r3d;
                break;
            }
            case (R2D_AND_R3D1CAM): {

                //System.err.println(" I WIIL CREATE AN R2D and AN R3D");
                StateRenderer2D r2d = new StateRenderer2D(map);
                r2d.setMapGroup(map);
                r2d.focusLocation(map.getCoordinateSystem());
                r2d.addChangeListener(this);
                po = new PlanElement(map, new MapType());
                po.setPlan(plan);
                po.setRenderer(r2d);
                //po.setColor(new Color(255,255,255,128));
                po.setShowDistances(false);
                po.setShowManNames(false);
                r2d.addPostRenderPainter(po, "Plan Painter");
                //r2d.addPainter(new PlanPainter(plan));

                Renderer3D r3d;
                Camera3D cam =new Camera3D(Camera3D.USER);
                Camera3D[]  cams={cam};
                r3d = new Renderer3D(cams,(short)1,(short)1);
                r3d.setMapGroup(map);
                r3d.focusLocation(map.getCoordinateSystem());
                r3d.addChangeListener(this);
                r3d.setBackgroundType((short)2);

                if (plan != null) {
                    Obj3D o3d = Object3DCreationHelper.getPlanModel3D(plan); // plan.getModel3D();				
                    if (o3d != null)
                        r3d.addObj3D(o3d);				
                }
                renderers = new Renderer[] {r2d, r3d};

                renderPanel = new JTabbedPane(JTabbedPane.TOP);
                renderPanel.add(r2d, "Renderer 2D");
                renderPanel.add(r3d, "Renderer 3D");
                renderer2d = r2d;
                renderer3d = r3d;
                //System.err.println(" I HAVE CREATED AN R2D and AN R3D");
                break;
            }
            case (R2D_AND_R3D_MULTICAMS): {
                StateRenderer2D r2d = new StateRenderer2D(map);
                r2d.setMapGroup(map);
                r2d.focusLocation(map.getCoordinateSystem());
                r2d.addChangeListener(this);
                Renderer3D r3d;				
                r3d = new Renderer3D();				
                r3d.setMapGroup(map);
                r3d.focusLocation(map.getCoordinateSystem());
                r3d.addChangeListener(this);
                r3d.setBackgroundType((short)2);

                if (plan != null) {
                    Obj3D o3d = Object3DCreationHelper.getPlanModel3D(plan); // plan.getModel3D();				
                    if (o3d != null)
                        r3d.addObj3D(o3d);				
                }			

                renderers = new Renderer[] {r2d, r3d};				
                renderPanel = new JTabbedPane(JTabbedPane.TOP);
                renderPanel.add(r2d, "Renderer 2D");
                renderPanel.add(r3d, "Renderer 3D");

                renderer2d = r2d;
            }			
        }

        this.setLayout(new BorderLayout());
        this.add(buildToolbar(plan != null), BorderLayout.SOUTH);
        this.add(renderPanel, BorderLayout.CENTER);

        if (plan != null) {
            this.initState = new SystemPositionAndAttitude(startLocation, 0,0,0);
            initState.setYaw(angle);

            this.vehicle = plan.getVehicleType();
            for (int i = 0; i < renderers.length; i++) {
                renderers[i].vehicleStateChanged(vehicle.getId(), initState);
            }            
            pauseMode.setIcon(new ImageIcon(cl.getResource("images/buttons/play.png")));
            pauseMode.setActionCommand("resume");
            pauseMode.setToolTipText("Resume the animation");
        }

        NeptusLog.pub().info("Start updating dynamic elements...");
        startUpdatingDynamicElements();
    }


    /**
     * Creates the JToolbar to be added in the interface
     * @return The toolbar with all its sub-components
     */
    public JToolBar buildToolbar(boolean showExecutionControls) {

        toolbar = new JToolBar(JToolBar.HORIZONTAL);
        toolbar.setMargin(new Insets(0,0,0,0));

        //if (showExecutionControls) {
        pauseMode = new ToolbarButton("images/buttons/pause.png", "Pause the animation", "pause");	
        pauseMode.setToolTipText("Pause the animation");
        addButtonToToolbar(pauseMode);

        restart = new ToolbarButton("images/buttons/restart.png", "Restart the animation", "restart");
        addButtonToToolbar(restart);

        if (!showExecutionControls) {
            pauseMode.setVisible(false);
            restart.setVisible(false);
        }

        findVehicle = new ToolbarButton("images/buttons/vehicle.png", "Center map on vehicle position", "findVehicle");
        addButtonToToolbar(findVehicle);
        findVehicle.setEnabled(false);
        bgroup.add(zoomMode);

        followMode = new ToolbarSwitch("images/buttons/lock_vehicle.png", "Follow vehicle", "follow", cl);
        addButtonToToolbar(followMode);
        followMode.setEnabled(false);

        zoomMode = new ToolbarSwitch("images/buttons/zoom_btn.png", "Activate zoom mode", "viewMode", cl);
        addButtonToToolbar(zoomMode);

        translateMode = new ToolbarSwitch("images/buttons/translate_btn.png", "De-activate translation mode", "viewMode", cl);
        translateMode.setSelected(true);
        addButtonToToolbar(translateMode);

        rotateMode = new ToolbarSwitch("images/buttons/rotate_btn.png", "Activate rotation mode", "viewMode", cl);
        addButtonToToolbar(rotateMode);

        rulerMode = new ToolbarSwitch("images/buttons/ruler_btn.png", "Measure distances between two points", "viewMode", cl);
        addButtonToToolbar(rulerMode);

        bgroup.add(zoomMode);
        bgroup.add(translateMode);
        bgroup.add(rotateMode);
        bgroup.add(rulerMode);

        interpolate = new ToolbarSwitch("images/buttons/preview.png", "Interpolate the estimated vehicle states", "interpolate", cl);		

        if (!ConfigFetch.isOnLockedMode())
            addButtonToToolbar(interpolate);

        tailOnOff = new ToolbarSwitch("images/buttons/tailOnOff.png", "Switch on/off vehicle tail", "tailOnOff", cl);
        addButtonToToolbar(tailOnOff);

        tailClean = new ToolbarButton("images/buttons/tailClean.png", "Clean vehicle tail", "tailClean");
        addButtonToToolbar(tailClean);


        selectPainters = new ToolbarButton("images/buttons/tailClean.png", "Map Layers", "selectPainters");
        addButtonToToolbar(selectPainters);

        toolbar.add(status);

        return toolbar;
    }

    public void addButtonToToolbar(AbstractButton button) {
        button.removeActionListener(this);
        button.addActionListener(this);
        toolbar.add(button);
    }

    /**
     * Cleans some memory in use
     */
    public void dispose() {
        cleanup();
    }

    /**
     * This method is run everytime a new action is performed
     */
    public void actionPerformed(ActionEvent action) {

        if ("interpolate".equals(action.getActionCommand())) {
            if (interpolate.isSelected())
                startInterpolatingStates();
            else
                stopInterpolatingStates();
        }

        if ("restart".equals(action.getActionCommand())) {
            pauseMode.setIcon(new ImageIcon(cl.getResource("images/buttons/pause.png")));
            pauseMode.setActionCommand("pause");
            pauseMode.setToolTipText("Pause the animation");
        }

        if ("pause".equals(action.getActionCommand())) {
            pauseMode.setIcon(new ImageIcon(cl.getResource("images/buttons/play.png")));
            pauseMode.setActionCommand("resume");
            pauseMode.setToolTipText("Resume the animation");
        }

        if ("resume".equals(action.getActionCommand())) {
            pauseMode.setIcon(new ImageIcon(cl.getResource("images/buttons/pause.png")));
            pauseMode.setActionCommand("pause");
            pauseMode.setToolTipText("Pause the animation");
        }

        if ("follow".equals(action.getActionCommand())) {
            if (followMode.isSelected()) {
                for (int i = 0; i < renderers.length; i++)  {
                    renderers[i].followVehicle(getMainVehicle().getId());		    		
                }
            }
            else {
                for (int i = 0; i < renderers.length; i++) {
                    renderers[i].followVehicle(null);
                }
            }
        }

        if ("findVehicle".equals(action.getActionCommand())) {
            for (int i = 0; i < renderers.length; i++) {
                SystemPositionAndAttitude state = shownVehicles.get(getShownVehicles()[vehicleIterator]);
                renderers[i].focusLocation(state.getPosition());
            }	    	
        }

        if ("viewMode".equals(action.getActionCommand())) {

            ToolbarSwitch src = (ToolbarSwitch) action.getSource();
            if (!src.isSelected()) {
                currentMode = Renderer.NONE;
                rotateMode.setToolTipText("Activate rotate mode");
                translateMode.setToolTipText("Activate translation mode");
                zoomMode.setToolTipText("Activate zoom mode");
            }
            else {
                if (translateMode.isSelected()) {
                    currentMode = Renderer.TRANSLATION;
                }

                if (zoomMode.isSelected()) {
                    currentMode = Renderer.ZOOM;	
                }

                if (rotateMode.isSelected()) {
                    currentMode = Renderer.ROTATION;
                }

                if (rulerMode.isSelected()) {
                    currentMode = Renderer.RULER;
                }
            }


            for (int i = 0; i < renderers.length; i++) {
                renderers[i].setViewMode(currentMode);
            }
        }


        if ("tailOnOff".equals(action.getActionCommand())) {
            if (tailOnOff.isSelected()) {
                for (Renderer rend : renderers)
                    rend.setVehicleTailOn(null);
            }
            else {
                for (Renderer rend : renderers)
                    rend.setVehicleTailOff(null);
            }
        }

        if ("tailClean".equals(action.getActionCommand())) {
            for (Renderer rend : renderers)
                rend.clearVehicleTail(null);
        }

        if ("selectPainters".equals(action.getActionCommand())) {
            this.add(renderer2d.painters.getSelectionPanel(), BorderLayout.EAST);
        }
    }

    /**
     * @return
     */
    private VehicleType[] getShownVehicles() {
        VehicleType[] vts = new VehicleType[shownVehicles.size()];
        int i = 0;
        for (Enumeration<VehicleType> e = shownVehicles.keys(); e.hasMoreElements(); ) {
            vts[i++] = (VehicleType) e.nextElement();
        }		
        return vts;
    }

    public void stateChanged(ChangeEvent e) {
        Renderer source = (Renderer) e.getSource();
        currentMode = source.getShowMode();

        if (currentMode == Renderer.ZOOM) {
            zoomMode.setSelected(true);
            rotateMode.setSelected(false);
            translateMode.setSelected(false);
        }

        if (currentMode == Renderer.TRANSLATION) {
            zoomMode.setSelected(false);
            rotateMode.setSelected(false);
            translateMode.setSelected(true);
        }

        if (currentMode == Renderer.ROTATION) {
            zoomMode.setSelected(false);
            rotateMode.setSelected(true);
            translateMode.setSelected(false);
        }

        if (renderers.length > 1) {
            for (int i = 0; i < renderers.length; i++) {
                if (renderers[i] != source)
                    renderers[i].setViewMode(currentMode);
            }
        }

    }

    /**
     * This method refreshes the maps shown in all the renderers contained in this class
     * @param mg The new MapGroup
     */
    public void setMapGroup(MapGroup mg) {
        for (int i = 0; i < renderers.length; i++) {
            renderers[i].setMapGroup(mg);
        }
        this.myMapGroup = mg;
    }

    public Renderer[] getRenderers() {
        return renderers;
    }

    public JComponent getRenderPanel() {
        return renderPanel;
    }

    public SystemPositionAndAttitude getCurstate() {
        return curstate;
    }

    public VehicleType getMainVehicle() {
        if (mainVehicle != null)
            return mainVehicle;
        else {
            if(getShownVehicles().length==0) return null;
            return (VehicleType) getShownVehicles()[0];
        }
    }

    public void setMainVehicle(VehicleType mainVehicle) {
        this.mainVehicle = mainVehicle;
        if (followMode.isSelected()) {
            ActionEvent evt = new ActionEvent(followMode, 0, "follow");
            actionPerformed(evt);
        }			
    }

    public void cleanup()
    {
        stopUpdatingDynamicElements();
        stopInterpolatingStates();

        for(int i=0;i<renderers.length;i++)
            if (renderers[i] != null)
                renderers[i].cleanup();


    }

    public StateRenderer2D getRenderer2d() {
        return renderer2d;
    }

    public Renderer3D getRenderer3d() {
        return renderer3d;
    }

    public void setMission(MissionType mission) {
        this.mission = mission;
        if (getMapGroup() != null)
            getMapGroup().removeMap(getLocalMap().getId());

        MapGroup mg = MapGroup.getMapGroupInstance(mission);
        setMapGroup(mg);
        mg.addMap(getLocalMap());

        if (getRenderer2d() != null) {
            getRenderer2d().repaint();
        }
    }

    public MissionType getMission() {		
        return mission;
    }

    public void setPlan(PlanType plan) {
        MapGroup newMapGroup = null;
        if (plan != null)
            newMapGroup = MapGroup.getMapGroupInstance(plan.getMissionType());

        this.plan = plan;

        for (Renderer r : renderers) {
            if (newMapGroup != null && !newMapGroup.equals(r.getMapGroup())) {
                r.setMapGroup(MapGroup.getMapGroupInstance(mission));
                NeptusLog.pub().info("MapGroup has changed!");
            }

            if (r instanceof StateRenderer2D) {
                StateRenderer2D r2d = (StateRenderer2D) r;

                r2d.removePaintersOfType(PlanElement.class);

                if (plan != null) {
                    PlanElement po = new PlanElement(r2d.getMapGroup(), new MapType());
                    po.setPlan(plan);
                    po.setRenderer(r2d);
                    po.setColor(new Color(255,255,255,128));
                    po.setShowDistances(false);
                    po.setShowManNames(false);
                    r2d.addPostRenderPainter(po, "Plan Painter");
                }
            }
            if (r instanceof Renderer3D) {
                Renderer3D r3d = (Renderer3D) r;

                if (plan == null)
                    r3d.setPlanObj(null);
                else
                    r3d.setPlanObj(Object3DCreationHelper.getPlanModel3D(plan)); // plan.getModel3D());
            }
        }
    }

    public PlanType getPlan() {	
        return plan;
    }

    public void showPreviewControls(boolean showPreviewControls) {		
        pauseMode.setVisible(showPreviewControls);
        restart.setVisible(showPreviewControls);
    }

    public void addToolbarAction(AbstractAction action) {
        toolbar.add(new ToolbarButton(action));
        toolbar.revalidate();		
    }

    public void setActiveManeuver(String manId) {
        po.setActiveManeuver(manId);
        status.setText("<html>maneuver: <b>"+manId+"</b>");
    }

    public void setInterpolateStatesVisible(boolean visible) {
        interpolate.setVisible(visible);
    }
    private Timer dynElemsTimer = null;

    public void startUpdatingDynamicElements() {
        if (dynElemsTimer != null) 
            return;
        else {
            NeptusLog.pub().info("Started updating dynamic elements");
            dynElemsTimer = new Timer("Dynamic Elements updater", true);
            dynElemsTimer.schedule(getDynObjectsUpdater(), 0, 1000);			
        }
    }

    public void stopUpdatingDynamicElements() {
        if (dynElemsTimer != null) {
            NeptusLog.pub().debug("Stopped updating dynamic elements");
            dynElemsTimer.cancel();
        }
    }

    public MapType getLocalMap() {
        return localMap;
    }

    protected TimerTask getDynObjectsUpdater() {
        TimerTask t = new TimerTask() {
            public void run() {
                if (myMapGroup == null) {
                    return;
                }
                Vector<DynamicElement> dynElems = myMapGroup.getAllObjectsOfType(DynamicElement.class);
                long curTime = System.currentTimeMillis();

                for (DynamicElement dynElem : dynElems) {					
                    long lastUpdate = dynElem.getLastUpdateTime();

                    if (lastUpdate == -1)
                        continue;
                    else {					
                        dynElem.setIdleTimeSecs((int)((curTime-dynElem.getLastUpdateTime())/1000));
                        if (getRenderer2d() != null)
                            getRenderer2d().repaint();
                    }
                }
            }			
        };
        return t;
    }

    public MapGroup getMapGroup() {
        return myMapGroup;
    }
}
