/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 11, 2011
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventPositionEstimation;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.preview.PlanSimulationOverlay;
import pt.lsts.neptus.mp.preview.PlanSimulator;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.Property;

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
        String src = msg.getSourceName();
        if (src == null)
            return;

        if (simulators.containsKey(src)) {
            simulators.get(src).setEstimatedState(msg);
            lastStates.put(src, msg);
            lastStateTimes.put(src, System.currentTimeMillis());
        }
    }

    protected long lastEstimateTime = 0;

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
        }
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

    @Subscribe
    public synchronized void consume(PlanControlState msg) {
        String src = msg.getSourceName();
        boolean main = src == getConsole().getMainSystem();
        
        if (msg.getPlanId().isEmpty())
            return;
        
        if (msg.getState() != PlanControlState.STATE.EXECUTING) {

            if (forceSimVisualization && main)
                return;
            else {
                //stopSimulator();
                if (simulators.containsKey(src)) {
                    simulators.get(src).stopSimulation();
                    simulators.remove(src);
                }

                if (main) {
                    mainSimulator = null;
                }
            }
        }
        else {
            try {
                String planid = msg.getPlanId();

                PlanSimulator simulator = simulators.get(src);

                if (simulator == null || simulator.isFinished() || !planid.equals(simulator.getPlan().getId())) {
                    if (simulator != null)
                        simulator.stopSimulation();
                    PlanType plan = getConsole().getMission().getIndividualPlansList().get(planid);
                    if (plan != null) {

                        EstimatedState last = ImcMsgManager.getManager().getState(msg.getSourceName()).last(EstimatedState.class);
                        if (last != null)
                            simulator = new PlanSimulator(plan, new SystemPositionAndAttitude(last));
                        else
                            simulator = new PlanSimulator(plan, null);
                        simulator.setManId(msg.getManId());
                        simulator.setVehicleId(src);
                        simulator.setTimestep(timestep);
                        if (activated)
                            simulator.startSimulation();

                        if (main) {
                            mainSimulator = simulator;
                        }
                        simulators.put(src, simulator);
                    }
                }

                if (simulator != null)
                    simulator.setManId(msg.getManId());
            }
            catch (Exception e) {
                e.printStackTrace();
                NeptusLog.pub().error(e);
            }
        }
    }

    public void paintVerticalProfile(Graphics2D g, StateRenderer2D renderer) {
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
                        DateTimeUtil.milliSecondsToFormatedString(simTime)) + "]");                
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
                Color c = type.getIconColor();
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
