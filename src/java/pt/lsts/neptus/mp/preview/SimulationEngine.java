/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 26, 2012
 */
package pt.lsts.neptus.mp.preview;

import java.util.Date;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.planning.SimulatedBathymetry;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class SimulationEngine {

    /**
     * The plan being simulated
     */
    protected PlanType plan;

    /**
     * Time step between state updates
     */
    protected double timestep = 0.25;
    protected String vehicleId, manId;
    protected SystemPositionAndAttitude state;
    protected boolean finished = false;
    protected IManeuverPreview<?> curPreview = null;
    public static SimulatedBathymetry simBathym = SimulatedBathymetry.getInstance();
    protected double ellapsedTime = 0;
    
    public SimulationEngine(final PlanType plan) {
        this.plan = plan;
        this.vehicleId = plan.getVehicle();
        this.manId = plan.getGraph().getInitialManeuverId();
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            if (m instanceof LocatedManeuver) {
                LocationType loc = ((LocatedManeuver)m).getStartLocation();
                state = new SystemPositionAndAttitude(loc, 0, 0, 0);
                break;
            }
        }
        if (state == null)
            state = new SystemPositionAndAttitude(plan.getMissionType().getStartLocation(), 0, 0, 0);
        
        state.setAltitude(SimulatedBathymetry.getInstance().getSimulatedDepth(state.getPosition())-state.getDepth());
    }

    public boolean isFinished() {
        return finished;
    }

    protected void setSimulationState(EstimatedState state, SimulationState simState) {
        Maneuver m = plan.getGraph().getManeuver(simState.getCurrentManeuver());
        synchronized (this) {
            finished = false;
            this.manId = simState.getCurrentManeuver();
            setState(simState.getSysState());
            curPreview = ManPreviewFactory.getPreview(m, vehicleId, getState(), simState.getManeuverState());
            
            simulationStep();
        }
    }

    public void setManeuverId(String manId) throws Exception {
        if (this.manId.equals(manId))
            return;
        
        Maneuver m = plan.getGraph().getManeuver(manId);
        
        if (m == null)
            return;
        
        synchronized (this) {
            finished = false;
            this.manId = manId;
            curPreview = ManPreviewFactory.getPreview(m, vehicleId, state , null);
            if (curPreview == null)
                throw new Exception(manId +" cannot be previewed");
            simulationStep();
        }
    }


    public void simulationStep() {

        Maneuver m = plan.getGraph().getManeuver(manId);
        
        if (m == null)
            return;
        
        if (curPreview == null) {
            synchronized (this) {
                curPreview = ManPreviewFactory.getPreview(m, vehicleId, state, null);
            }
            if (curPreview == null) {
                Maneuver next = plan.getGraph().getFollowingManeuver(m.getId());
                if (next == null)
                    finished = true;
                else {                    
                    manId = next.getId();
                    NeptusLog.pub().debug("Simulating " + manId);
                }
                return;
            }
            else {
                NeptusLog.pub().debug("now simulating using " + curPreview.getClass().getSimpleName());
            }
        }
        state.setAltitude(SimulatedBathymetry.getInstance().getSimulatedDepth(state.getPosition()) - state.getDepth());
        if (curPreview != null)
            state = curPreview.step(state, timestep, ellapsedTime);
        
        ellapsedTime += timestep;
        
        if (curPreview == null || curPreview.isFinished()) {
            try {
                Maneuver next = plan.getGraph().getFollowingManeuver(m.getId());
                if (next == null) {
                    finished = true;
                    NeptusLog.pub().debug("Plan finished at " + new Date());
                }
                else{
                    manId = next.getId();
                    NeptusLog.pub().debug("Simulating " + manId);
                }
            }
            catch (Exception e) {
                finished = true;
            }
            curPreview = null;
        }
    }

    /**
     * @return the timestep
     */
    public double getTimestep() {
        return timestep;
    }

    /**
     * @return the state
     */
    public final SystemPositionAndAttitude getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public final void setState(SystemPositionAndAttitude state) {
        this.state = state;
    }

    /**
     * @return the manId
     */
    public final String getManId() {
        return manId;
    }

    /**
     * @return the curPreview
     */
    public final IManeuverPreview<?> getCurPreview() {
        return curPreview;
    }

}
