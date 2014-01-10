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

import java.awt.Graphics2D;
import java.util.LinkedHashMap;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventPositionEstimation;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="Multi-Vehicle Simulator")
public class MultiVehicleSimulator extends SimpleSubPanel implements Renderer2DPainter {
    
    private static final long serialVersionUID = -4232167598121740724L;

    @NeptusProperty(name = "Milliseconds to wait before simulation")
    public long millisToWait = 1000;

    @NeptusProperty(name = "Interval between simulated states, in seconds")
    public double timestep = 0.25;
    
    protected LinkedHashMap<String, VehicleSimulation> simulations = new LinkedHashMap<>();
    
    public MultiVehicleSimulator(ConsoleLayout console) {
        super(console);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        synchronized (simulations) {
            for (VehicleSimulation s : simulations.values())
                s.paint(g, renderer);    
        }                
    }
    
    @Subscribe
    public void consume(PlanControlState pcs) {
        String source = pcs.getSourceName();
        PlanType plan = null;
        
        if(pcs.getState() == PlanControlState.STATE.EXECUTING) {
            String planid = pcs.getPlanId();
            plan = getConsole().getMission().getIndividualPlansList().get(planid);
            if (plan == null) {
                NeptusLog.pub().warn("Unable to simulate unvailable plan: "+planid);
                return;
            }
        }
        
        if(plan == null) {
            synchronized (simulations) {
                if (simulations.containsKey(source)) {
                    simulations.get(source).stop();
                    simulations.remove(source);
                }
            }
        }
        else {
            synchronized (simulations) {
                if (!simulations.containsKey(source))
                    simulations.put(source, new VehicleSimulation(source, timestep));
                
                simulations.get(source).setPlan(plan);
                
                EstimatedState last = ImcMsgManager.getManager().getState(source).lastEstimatedState();
                simulations.get(source).setState(last);
                try {
                    simulations.get(source).setManeuverId(pcs.getManId());                    
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
                simulations.get(source).start();
            }
        }
    }
    
    @Subscribe
    public void consume(EstimatedState state) {
        String source = state.getSourceName();
        synchronized (simulations) {
            if (simulations.containsKey(source))
                simulations.get(source).setState(state);    
        }
    }
    
    @Subscribe
    public void consume(ConsoleEventPositionEstimation estimation) {
        synchronized (simulations) {
            if (simulations.containsKey(getMainVehicleId()))
                simulations.get(getMainVehicleId()).setPositionEstimation(estimation.getEstimation(), 8d);    
        }
    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public void cleanSubPanel() {
        synchronized (simulations) {
            for (VehicleSimulation s : simulations.values())
                s.stop();
        }
    }

}
