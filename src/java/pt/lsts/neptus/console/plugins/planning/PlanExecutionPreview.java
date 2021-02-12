/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Oct 11, 2011
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.imc.StateReport;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventFutureState;
import pt.lsts.neptus.console.events.ConsoleEventPositionEstimation;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.preview.PlanSimulationOverlay;
import pt.lsts.neptus.mp.preview.PlanSimulator;
import pt.lsts.neptus.mp.preview.SimulatedFutureState;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 */
@PluginDescription(name = "Plan Simulation Preview", author = "zp", icon="images/planning/robot.png")
@LayerPriority(priority = 60)
public class PlanExecutionPreview extends ConsolePanel implements Renderer2DPainter, ConfigurationListener {

    private static final long serialVersionUID = 1L;

    protected GeneralPath arrow = new GeneralPath();
    {
        arrow.moveTo(-2, -10);
        arrow.lineTo(2, -10);
        arrow.lineTo(2, 0);
        arrow.lineTo(5, 0);
        arrow.lineTo(0, 10);
        arrow.lineTo(-5, 0);
        arrow.lineTo(-2, 0);
        arrow.closePath();
    }

    protected LinkedHashMap<String, PlanSimulator> simulators = new LinkedHashMap<>();
    protected LinkedHashMap<String, EstimatedState> lastStates = new LinkedHashMap<>();
    protected LinkedHashMap<String, Long> lastStateTimes = new LinkedHashMap<>();

    protected PlanSimulator mainSimulator = null;
    protected boolean forceSimVisualization = false;
    protected long lastEstimateTime = 0;

    @NeptusProperty(name = "Active")
    public boolean activated = true;

    @NeptusProperty(name = "Milliseconds to wait before simulation")
    public long millisToWait = 1000;

    @NeptusProperty(name = "Interval between simulated states, in seconds")
    public double timestep = 0.25;

    @NeptusProperty(name = "Simulated (flat) bathymetry")
    public double bathymetry = 10;

    public PlanExecutionPreview(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    protected double getVehicleDepth() {

        EstimatedState state = lastStates.get(getConsole().getMainSystem());

        try {
            if (state != null)            
                return ImcMsgManager.getManager().getState(getConsole().getMainSystem()).last(EstimatedState.class).getDepth();
            return 0;
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Subscribe
    public void consume(EstimatedState msg) {
        try {
        String src = msg.getSourceName();
        if (src == null)
            return;

        if (simulators.containsKey(src)) {
            simulators.get(src).setEstimatedState(msg);
            lastStates.put(src, msg);
            lastStateTimes.put(src, System.currentTimeMillis());
            updateFutureState(src);
        }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void consume(Announce msg) {
        String src = msg.getSourceName();
        if (src == null)
            return;

        if (simulators.containsKey(src) && lastStateTimes.containsKey(getConsole().getMainSystem())) {

            long lastStateTime = lastStateTimes.get(getConsole().getMainSystem());

            if (System.currentTimeMillis() - lastStateTime < 1000)
                return;

            EstimatedState state = new EstimatedState();
            state.setLat(msg.getLat());
            state.setLon(msg.getLon());

            simulators.get(src).setPositionEstimation(state, 15);
            lastStateTimes.put(src, System.currentTimeMillis());
            updateFutureState(src);
        }
    }

    @Subscribe
    public void consume(RemoteSensorInfo msg) {
        String src = msg.getSourceName();
        if (src == null)
            return;

        if (simulators.containsKey(src)) {

            long lastStateTime = lastStateTimes.get(getConsole().getMainSystem());

            if (System.currentTimeMillis() - lastStateTime < 1000)
                return;

            EstimatedState state = new EstimatedState();
            state.setLat(msg.getLat());
            state.setLon(msg.getLon());

            simulators.get(src).setPositionEstimation(state, 50);
            lastStateTimes.put(src, System.currentTimeMillis());
            updateFutureState(src);
        }
    }

    @Subscribe
    public void consume(ConsoleEventPositionEstimation estimate) {
        lastEstimateTime = System.currentTimeMillis();

        if (mainSimulator == null)
            return;

        long lastStateTime = lastStateTimes.get(getConsole().getMainSystem());

        if (System.currentTimeMillis() - lastStateTime < 1000)
            return;
        else {
            mainSimulator.setPositionEstimation(estimate.getEstimation(), 8);
            updateFutureState(getConsole().getMainSystem());
        }
    }
    
    protected void updateFutureState(String system) {
        PlanSimulator simulator = simulators.get(system);
        if (simulator == null)
            return;
        
        SimulatedFutureState future = simulator.getFutureState();
        if (future == null)
            return;
        
        ConsoleEventFutureState futureState = new ConsoleEventFutureState(future);
        getConsole().post(futureState);        
    }

    protected void stopSimulator() {
        for (PlanSimulator s : simulators.values())
            s.stopSimulation();

        simulators.clear();
        mainSimulator = null;
    }
    
    @Override
    public void cleanSubPanel() {
        stopSimulator();
    }
    
    @NeptusMenuItem("Tools>Simulation>Set Simulator State")
    public void forceSimulation() {
        SimulationState state = new SimulationState();
        state.location = LocationType.clipboardLocation();
        if (state.location == null)
            state.location = new LocationType(MapGroup.getMapGroupInstance(getConsole().getMission()).getCoordinateSystem());
        
        if (getConsole().getPlan() != null)
            state.planId = getConsole().getPlan().getId();
        
        PluginUtils.editPluginProperties(state, getConsole(), true);
        
        PlanSimulator sim = setSimulationState(getConsole().getMainSystem(), state.planId, null);
        
        if (sim != null) {
            EstimatedState current = sim.getState().toEstimatedState();
            state.location.convertToAbsoluteLatLonDepth();
            current.setLat(state.location.getLatitudeRads());
            current.setLon(state.location.getLongitudeRads());            
            System.out.println(current.asJSON());
            sim.setPositionEstimation(current, 0);    
        }        
    }
    
    @NeptusMenuItem("Tools>Simulation>Stop Simulator")
    public void stopSimulation() {
        if (simulators.containsKey(getConsole().getMainSystem()))
            simulators.get(getConsole().getMainSystem()).stopSimulation();                
    }


    public PlanSimulator setSimulationState(String vehicleId, String planId, String maneuverId) {
        boolean main = vehicleId == getConsole().getMainSystem();
        if (planId == null) {
            if (forceSimVisualization && main)
                return mainSimulator;
            else {
                if (simulators.containsKey(vehicleId)) {
                    simulators.get(vehicleId).stopSimulation();
                    simulators.remove(vehicleId);
                    // remove future state...
                    ConsoleEventFutureState futureState = new ConsoleEventFutureState(vehicleId, new Date(), null);
                    getConsole().post(futureState);
                }
            }
            return null;
        }
        else {
            if (planId.equals("trex_plan")
                    && getConsole().getMission().getIndividualPlansList()
                    .containsKey("trex_" + vehicleId))
                planId = "trex_" + vehicleId;

            PlanSimulator simulator = simulators.get(vehicleId);

            if (simulator == null || simulator.isFinished() || !planId.equals(simulator.getPlan().getId())) {
                if (simulator != null)
                    simulator.stopSimulation();
                PlanType plan = getConsole().getMission().getIndividualPlansList().get(planId);
                if (plan != null) {

                    EstimatedState last = ImcMsgManager.getManager().getState(vehicleId).last(EstimatedState.class);
                    if (last != null)
                        simulator = new PlanSimulator(plan, new SystemPositionAndAttitude(last));
                    else
                        simulator = new PlanSimulator(plan, null);

                    simulator.setVehicleId(vehicleId);
                    simulator.setTimestep(timestep);
                    if (activated)
                        simulator.startSimulation();

                    if (main) {
                        mainSimulator = simulator;
                    }
                    lastStateTimes.put(vehicleId, System.currentTimeMillis());
                    simulators.put(vehicleId, simulator);                    
                }
            }

            if (simulator != null && maneuverId != null) {
                try {
                    simulator.setManId(maneuverId);                        
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Could not select simulated maneuver: "+maneuverId, e);
                }
            }

            return simulator;
        }
    }
    
    @Subscribe
    public synchronized void consume(PlanControlState msg) {
        String src = msg.getSourceName();
        String plan = msg.getPlanId();
        String maneuver = (msg.getManId() == null || msg.getManId().isEmpty()) ? null : msg.getManId();

        if (msg.getState() == STATE.BLOCKED)
            return;
        
        if (msg.getState() == STATE.READY)
            plan = maneuver = null;

        setSimulationState(src, plan, maneuver);
    }

    private String getPlanFromChecksum(int checksum) {
        if (getConsole().getMission() == null)
            return null;

        for (String planId : getConsole().getMission().getIndividualPlansList().keySet()) {
            byte[] str = planId.getBytes();
            if (IMCUtil.computeCrc16(str, 0, str.length) == checksum)
                return planId;
        }
        return null;
    }

    public void paintVerticalProfile(Graphics2D g, StateRenderer2D renderer) {
        
    }
    
    @Subscribe
    public synchronized void consume(StateReport msg) {

        String src = msg.getSourceName();
        String plan = getPlanFromChecksum(msg.getPlanChecksum());
        if (msg.getExecState() < -1) {
            //setSimulationState(src, null, null);
            return;
        }
        else {
            PlanSimulator sim = setSimulationState(src, plan, null);
            if (sim != null) {
                EstimatedState current = sim.getState().toEstimatedState();
                current.setLat(Math.toRadians(msg.getLatitude()));
                current.setLon(Math.toRadians(msg.getLongitude()));
                current.setDepth(msg.getDepth()/10.0);
                double ellapsedTime = Math.abs(System.currentTimeMillis()/1000.0 - msg.getStime()) * 3;
                sim.setPositionEstimation(current, ellapsedTime);
            }
        }
    }

    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {

        Graphics2D g;
        Vector<String> strs = new Vector<>();

        for (PlanSimulator sim : simulators.values()) {
            g = (Graphics2D)g2.create();

            String vehicle = sim.getVehicleId();
            long lastTime = lastStateTimes.containsKey(vehicle) ? lastStateTimes.get(vehicle) : 0; 
            long simTime = System.currentTimeMillis() - lastTime;
            if (simTime > 1000) {
                strs.add("[" + I18n.textf("Simulating %vehicle for %time", vehicle,
                        DateTimeUtil.milliSecondsToFormatedString(simTime, true)) + "]");                
            }

            long lastStateTime = lastStateTimes.containsKey(vehicle)? lastStateTimes.get(vehicle) : 0;
            if (System.currentTimeMillis() - lastStateTime < millisToWait) {
                continue;
            }
            else if (sim != null && sim.isRunning()) {
                SystemPositionAndAttitude simulatedState = sim.getState();
                if (simulatedState == null)
                    return;
                Point2D pt = renderer.getScreenPosition(simulatedState.getPosition());
                g.translate(pt.getX(), pt.getY());
                g.rotate(Math.PI + simulatedState.getYaw() - renderer.getRotation());
                VehicleType type = VehiclesHolder.getVehicleById(sim.getVehicleId());
                Color c = Color.WHITE;
                if (type != null)
                    c = type.getIconColor();
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
                g.fill(arrow);
                g.setColor(Color.black);
                g.draw(arrow);
                g.rotate(-Math.PI - simulatedState.getYaw() + renderer.getRotation());
                g.setColor(Color.black);
                g.drawString(
                        "(" + vehicle + ", "
                                + GuiUtils.getNeptusDecimalFormat(1).format(simulatedState.getPosition().getDepth())
                                + " m)", 7, 5);
            }
        }
        if (!strs.isEmpty()) {
            Collections.sort(strs);

            g = (Graphics2D)g2.create();
            int ypos = 20;
            double maxWidth = 0;
            for (String str : strs)
                maxWidth = Math.max(maxWidth, g.getFontMetrics().getStringBounds(str, g).getWidth());
            g.setColor(new Color(255,255,255,64));
            g.fill(new RoundRectangle2D.Double(50, 5, maxWidth+10, strs.size() * 15 + 10, 10, 10));

            for (String str : strs) {
                g.setColor(new Color(68,68,68,128));
                g.drawString(str, 56, ypos);
                g.setColor(Color.red.darker());
                g.drawString(str, 55, ypos);
                ypos += 15;
            }
        }
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        if (mainSimulator != null)
            mainSimulator.setTimestep(timestep);

        if (!activated && mainSimulator != null) {
            stopSimulator();
        }
    }

    @Override
    public void propertiesChanged() {
        PlanSimulationOverlay.bottomDepth = bathymetry;
    }

    @Override
    public void initSubPanel() {
    }

    public int getLayerPriority() {
        return 1;
    }
}
