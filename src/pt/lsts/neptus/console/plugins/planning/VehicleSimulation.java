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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jan 9, 2014
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.preview.PlanSimulator;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class VehicleSimulation implements Renderer2DPainter {

    private PlanSimulator simulator = null;
    private EstimatedState lastState;
    private long lastStateTime = 0;
    private boolean visible = false;
    private double timestep = 0.25;
    private PlanType plan = null;
    private String vehicleId;
    
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
    
    public VehicleSimulation(String vehicle, double timestep) {
        this.timestep = timestep;
    }
    
    public synchronized void setTimestep(double timestep) {
        if (simulator != null)
            simulator.setTimestep(timestep);
    }
    
    public synchronized void setState(EstimatedState state) {
        lastState = state;
        lastStateTime = System.currentTimeMillis();
        
        if (simulator == null && plan != null) {
            SystemPositionAndAttitude s = IMCUtils.parseState(lastState);
            simulator = new PlanSimulator(plan, s);
            simulator.setTimestep(timestep);
            simulator.setVehicleId(vehicleId);
            simulator.startSimulation();
        }
    }
    
    public synchronized void setPositionEstimation(EstimatedState state, double distanceThreshold) {
        if (simulator != null)
            simulator.setPositionEstimation(state, distanceThreshold);
    }
    
    public synchronized void setPlan(PlanType plan) {
        this.plan = plan;
        if (simulator != null) {
            if (!plan.equals(simulator.getPlan()))
                simulator.stopSimulation();
            else
                return;
        }
        
        if (lastState != null) {
            SystemPositionAndAttitude s = IMCUtils.parseState(lastState);
            simulator = new PlanSimulator(plan, s);
            simulator.setTimestep(timestep);
            simulator.setVehicleId(vehicleId);

        }
    }
    
    public synchronized void setManeuverId(String id) throws Exception {
        if (simulator != null)
            simulator.setManId(id);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (visible && simulator != null)
            simulator.getSimulationOverlay().paint((Graphics2D) g.create(), renderer);

        if (simulator != null && simulator.isRunning()) {
            long simTime = System.currentTimeMillis() - lastStateTime;
            if (simTime > 1000) {
                String str = "["
                        + I18n.textf("Simulating for %time", DateTimeUtil.milliSecondsToFormatedString(simTime)) + "]";
                g.setColor(Color.gray.darker());
                g.drawString(str, 6, 16);
                g.setColor(Color.red.darker());
                g.drawString(str, 5, 15);
            }
        }

        if (System.currentTimeMillis() - lastStateTime < 1000) {
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
    
    public synchronized void stop() {
        if (simulator != null) {
            simulator.stopSimulation();
            simulator = null;
        }
    }
    
    public synchronized void start() {
        if (simulator != null)
            simulator.startSimulation();
    }
    
}
