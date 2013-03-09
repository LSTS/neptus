/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Oct 11, 2011
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventPositionEstimation;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.PlanControlState;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.preview.PlanSimulator;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 */
@PluginDescription(name = "Plan Execution Preview", author = "zp")
@LayerPriority(priority = 60)
public class PlanExecutionPreview extends SimpleSubPanel implements Renderer2DPainter {

    private static final long serialVersionUID = 1L;
    protected boolean debug = false;
    
    protected PlanSimulator simulator = null;
    protected SystemPositionAndAttitude lastVehicleState = null;
    protected long lastStateTime = 0;

    @NeptusProperty(name = "Activated")
    public boolean activated = true;

    @NeptusProperty(name = "Milliseconds to wait before simulation")
    public long millisToWait = 1000;

    @NeptusProperty(name = "Interval between simulated states, in seconds")
    public double timestep = 0.25;

    public PlanExecutionPreview(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }
    
    protected double getVehicleDepth() {
        try {
            return ImcMsgManager.getManager().getState(getConsole().getMainSystem()).lastEstimatedState().getDepth();
        }
        catch (Exception e) {
            return 0;
        }
    }

    @Subscribe
    public void consume(EstimatedState msg) {
      
        if (msg.getSourceName() != null && !msg.getSourceName().equals(getConsole().getMainSystem()))
            return;

        if (getVehicleDepth() > 1 && debug)
            return;
        
        //System.out.println("Got an estimated state");

        lastVehicleState = IMCUtils.parseState(msg);

        if (simulator != null)
            simulator.setState(lastVehicleState);

        lastStateTime = System.currentTimeMillis();
    }

    protected long lastEstimateTime = 0;
    
    @Subscribe
    public void consume(ConsoleEventPositionEstimation estimate) {

        if (System.currentTimeMillis() - lastEstimateTime < 45000 && debug)
            return;
        lastEstimateTime = System.currentTimeMillis();
        
        //System.out.println("Got a position estimation");
        if (System.currentTimeMillis() - lastStateTime < 1000 || simulator == null)
            return;
        else {
            simulator.setPositionEstimation(estimate.getEstimation());
        }
    }

    protected void stopSimulator() {
        if (simulator != null)
            simulator.stopSimulation();
        simulator = null;
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

        if (msg.getState() != PlanControlState.STATE.EXECUTING)
            stopSimulator();
        else {
            String planid = msg.getPlanId();

            if (simulator == null || simulator.isFinished() || !planid.equals(simulator.getPlan().getId())) {
                stopSimulator();
                PlanType plan = getConsole().getMission().getIndividualPlansList().get(planid);
                if (plan != null) {
                    
                    EstimatedState last = ImcMsgManager.getManager().getState(msg.getSourceName()).lastEstimatedState();
                    if (last != null)
                        simulator = new PlanSimulator(plan, new SystemPositionAndAttitude(last));
                    else
                        simulator = new PlanSimulator(plan, null);
                    simulator.setManId(msg.getManId());
                    simulator.setVehicleId(getConsole().getMainSystem());
                    simulator.setState(lastVehicleState);
                    simulator.setTimestep(timestep);
                    if (activated)
                        simulator.startSimulation();
                }
            }

            if (simulator != null)
                simulator.setManId(msg.getManId());
        }
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
        stopSimulator();
        lastStateTime = 0;
        lastVehicleState = null;
    }

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

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
        if (simulator != null && simulator.isRunning()) {
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
        else if (simulator != null && simulator.isRunning()) {
            SystemPositionAndAttitude simulatedState = simulator.getState();
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
                    "(" + simulator.getVehicleId() + ", "
                            + GuiUtils.getNeptusDecimalFormat(1).format(simulatedState.getPosition().getDepth())
                            + " m)", 7, 5);
        }
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        if (simulator != null)
            simulator.setTimestep(timestep);

        if (!activated && simulator != null) {
            stopSimulator();
        }

    }

    @Override
    public void initSubPanel() {

    }

    public int getLayerPriority() {
        return 1;
    }
}
