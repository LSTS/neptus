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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventPositionEstimation;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.preview.PlanSimulation3D;
import pt.lsts.neptus.mp.preview.PlanSimulationOverlay;
import pt.lsts.neptus.mp.preview.PlanSimulator;
import pt.lsts.neptus.mp.preview.SimulationEngine;
import pt.lsts.neptus.mp.preview.payloads.PayloadFactory;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 */
@PluginDescription(name = "Plan Execution Preview", author = "zp", icon="images/planning/preview.png")
@LayerPriority(priority = 60)
public class PlanExecutionPreview extends SimpleRendererInteraction implements Renderer2DPainter, ConfigurationListener {

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

    protected boolean debug = false;
    protected PlanSimulationOverlay simOverlay = null;
    protected PlanSimulator mainSimulator = null;
    protected SystemPositionAndAttitude lastVehicleState = null;
    protected long lastStateTime = 0;
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

    EstimatedState lastEState = null;

    protected double getVehicleDepth() {
        try {
            if (lastEState != null && lastEState.getSourceName().equals(getConsole().getMainSystem()))            
                return ImcMsgManager.getManager().getState(getConsole().getMainSystem()).lastEstimatedState().getDepth();
            return 0;
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    @Override
    public String getName() {
        return "Plan Simulation";
    }
    
    @Override
    public boolean isExclusive() {
        return true;
    }
    
    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            
            if (mainSimulator != null) {
                popup.add(I18n.text("Locate simulator here")).addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        LocationType loc = source.getRealWorldLocation(event.getPoint());
                        loc.convertToAbsoluteLatLonDepth();
                        if (mainSimulator != null) {
                            SystemPositionAndAttitude curState = mainSimulator.getState();
                            EstimatedState newState = curState.toEstimatedState();
                            
                            newState.setLat(loc.getLatitudeAsDoubleValueRads());
                            newState.setLon(loc.getLongitudeAsDoubleValueRads());
                            
                            mainSimulator.setPositionEstimation(newState, Double.MAX_VALUE);                            
                        }
                    }
                });
                
                JMenu menu = new JMenu(I18n.text("Set current maneuver"));
                for (final Maneuver man : mainSimulator.getPlan().getGraph().getAllManeuvers()) {
                    menu.add(man.getId()+" (" + man.getType() + ")").addActionListener(new ActionListener() {
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (mainSimulator != null) {
                                try {
                                    mainSimulator.setManId(man.getId());
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(getConsole(), ex);
                                }
                            }
                        }
                    });
                }
                
                popup.add(menu);
                
                
            }
            popup.add(I18n.text("Simulate from here")).addActionListener(new ActionListener() {
                
                final LocationType loc = source.getRealWorldLocation(event.getPoint());
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (mainSimulator != null) {
                        mainSimulator.stopSimulation();
                    }
                        mainSimulator = new PlanSimulator(getConsole().getPlan(), new SystemPositionAndAttitude(loc, 0, 0, 0));
                        mainSimulator.setVehicleId(getConsole().getMainSystem());
                        try {
                            mainSimulator.setManId(mainSimulator.getPlan().getGraph().getInitialManeuverId());
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(getConsole(), ex);
                        }
                        mainSimulator.setState(new SystemPositionAndAttitude(loc, 0, 0, 0));
                        mainSimulator.setTimestep(timestep);
                        mainSimulator.startSimulation();
                        simOverlay = mainSimulator.getSimulationOverlay();
                        forceSimVisualization = true;                    
                }
            });
            
            if (simOverlay != null) {
                popup.add(I18n.text("Show 3D simulation")).addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {                        
                        PlanSimulation3D.showSimulation(getConsole(), simOverlay, getConsole().getPlan());
                    }
                });
            }
            
            popup.add(I18n.text("Clear simulation")).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (mainSimulator != null) {
                        mainSimulator.stopSimulation();
                        mainSimulator = null;
                        simOverlay = null;
                    }
                    forceSimVisualization = false;
                }
            });
            popup.addSeparator();
            JMenu simBathym = new JMenu(I18n.text("Simulated bathymetry"));
            simBathym.add(I18n.text("Add depth sounding")).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    String ret = JOptionPane.showInputDialog(getConsole(), I18n.text("Enter simulated depth for this location"));
                    if (ret == null)
                        return;
                    try {
                        double val = Double.parseDouble(ret);
                        LocationType loc = source.getRealWorldLocation(event.getPoint());
                        SimulationEngine.simBathym.addSounding(loc, val);
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(ex);
                    }
                }
            });
            
            simBathym.add(I18n.text("Clear depth soundings")).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    SimulationEngine.simBathym.clearSoundings();
                }
            });
            
            simBathym.add(I18n.text("Show depth here")).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    LocationType loc = source.getRealWorldLocation(event.getPoint());
                    GuiUtils.infoMessage(getConsole(), I18n.text("Show depth"),
                            I18n.textf("Depth is %value m", SimulationEngine.simBathym.getSimulatedDepth(loc)));
                }
            });
            popup.add(simBathym);
            
            popup.add(I18n.text("Calculate payloads")).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (Maneuver m : getConsole().getPlan().getGraph().getAllManeuvers()) {
                        PayloadFactory.getPayloads(m);
                    }
                }
            });
            
            popup.addSeparator();
            if (simOverlay != null) {
                popup.add(I18n.text("Plan statistics")).addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (simOverlay != null)
                            generatePlanStatistics();
                    }
                });
            }
            popup.show(source, event.getX(), event.getY());
        }
        else
            super.mouseClicked(event, source);
    }
    
    
    private void generatePlanStatistics() {
        LinkedHashMap<String, String> stats;
        
        if (mainSimulator != null)
            stats = simOverlay.statistics(mainSimulator.getState());
        else
            stats = simOverlay.statistics(null);
        
        String html = "<html><table>\n";
        for (Entry<String,String> entry : stats.entrySet()) {
            html += "<tr><td><b>"+entry.getKey()+"</b></td><td>"+entry.getValue()+"</td></tr>\n";            
        }
        html +="</table></html>";
        
        GuiUtils.htmlMessage(getConsole(), I18n.text("Plan Statistics"), "", html);        
    }

    @Subscribe
    public void consume(EstimatedState msg) {

        // check if message is coming from main vehicle
        if (msg.getSourceName() != null && !msg.getSourceName().equals(getConsole().getMainSystem()))
            return;

        lastEState = msg;
        if (getVehicleDepth() > 1 && debug)
            return;

        //NeptusLog.pub().info("<###>Got an estimated state");

        lastVehicleState = IMCUtils.parseState(msg);

        if (mainSimulator != null)
            mainSimulator.setEstimatedState(msg);

        lastStateTime = System.currentTimeMillis();
    }

    protected long lastEstimateTime = 0;

    @Subscribe
    public void consume(ConsoleEventPositionEstimation estimate) {

        if (System.currentTimeMillis() - lastEstimateTime < 45000 && debug)
            return;
        lastEstimateTime = System.currentTimeMillis();

        if (System.currentTimeMillis() - lastStateTime < 1000 || mainSimulator == null)
            return;
        else {
            mainSimulator.setPositionEstimation(estimate.getEstimation(), 8);
        }
    }

    protected void stopSimulator() {
        if (mainSimulator != null)
            mainSimulator.stopSimulation();
        mainSimulator = null;
    }

    @Override
    public void cleanSubPanel() {
        stopSimulator();
    }
    
    @Subscribe
    public synchronized void consume(PlanControlState msg) {

        if (getVehicleDepth() > 1 && debug)
            return;

        if (!msg.getSourceName().equals(getConsole().getMainSystem()))
            return;

        if (msg.getState() != PlanControlState.STATE.EXECUTING) {
            if (forceSimVisualization)
                return;
            else
                stopSimulator();                    
        }
        else {
            try {
                String planid = msg.getPlanId();
    
                if (mainSimulator == null || mainSimulator.isFinished() || !planid.equals(mainSimulator.getPlan().getId())) {
                    stopSimulator();
                    PlanType plan = getConsole().getMission().getIndividualPlansList().get(planid);
                    if (plan != null) {
    
                        EstimatedState last = ImcMsgManager.getManager().getState(msg.getSourceName()).lastEstimatedState();
                        if (last != null)
                            mainSimulator = new PlanSimulator(plan, new SystemPositionAndAttitude(last));
                        else
                            mainSimulator = new PlanSimulator(plan, null);
                        mainSimulator.setManId(msg.getManId());
                        mainSimulator.setVehicleId(getConsole().getMainSystem());
                        mainSimulator.setState(lastVehicleState);
                        mainSimulator.setTimestep(timestep);
                        if (activated)
                            mainSimulator.startSimulation();
                        simOverlay = mainSimulator.getSimulationOverlay();
                    }
                }
    
                if (mainSimulator != null)
                    mainSimulator.setManId(msg.getManId());
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
        stopSimulator();
        lastStateTime = 0;
        lastVehicleState = null;
    }

    public void paintVerticalProfile(Graphics2D g, StateRenderer2D renderer) {
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (active)
            SimulationEngine.simBathym.paint((Graphics2D)g.create(), renderer);
        
        if (active && mainSimulator != null)
            mainSimulator.getSimulationOverlay().paint((Graphics2D)g.create(), renderer);
        
        if (mainSimulator != null && mainSimulator.isRunning()) {
            long simTime = System.currentTimeMillis() - lastStateTime;
            if (simTime > 1000) {
                String str = "[" + I18n.textf("Simulating for %time",
                        DateTimeUtil.milliSecondsToFormatedString(simTime)) + "]";
                g.setColor(Color.gray.darker());
                g.drawString(str, 6, 16);
                g.setColor(Color.red.darker());
                g.drawString(str, 5, 15);
            }
        }

        if (System.currentTimeMillis() - lastStateTime < millisToWait) {
            return;
        }
        else if (mainSimulator != null && mainSimulator.isRunning()) {
            SystemPositionAndAttitude simulatedState = mainSimulator.getState();
            if (simulatedState == null)
                return;
            Point2D pt = renderer.getScreenPosition(simulatedState.getPosition());
            g.translate(pt.getX(), pt.getY());
            g.rotate(Math.PI + simulatedState.getYaw() - renderer.getRotation());
            g.setColor(new Color(64, 64, 64, 150));
            g.fill(arrow);
            g.setColor(Color.black);
            g.draw(arrow);
            g.rotate(-Math.PI - simulatedState.getYaw() + renderer.getRotation());
            g.setColor(Color.black);
            g.drawString(
                    "(" + mainSimulator.getVehicleId() + ", "
                            + GuiUtils.getNeptusDecimalFormat(1).format(simulatedState.getPosition().getDepth())
                            + " m)", 7, 5);
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
