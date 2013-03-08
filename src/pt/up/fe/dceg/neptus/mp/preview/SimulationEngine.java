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
 * Nov 26, 2012
 * $Id:: SimulationEngine.java 9880 2013-02-07 15:23:52Z jqcorreia              $:
 */
package pt.up.fe.dceg.neptus.mp.preview;

import java.util.Date;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

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

    public static final double BOTTOM_DEPTH = 10;
    
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

    public void setManeuverId(String manId) {
        if (this.manId.equals(manId))
            return;
        
        Maneuver m = plan.getGraph().getManeuver(manId);
        
        synchronized (this) {
            finished = false;
            this.manId = manId;
            curPreview = ManPreviewFactory.getPreview(m, vehicleId, state , null);
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
                NeptusLog.pub().info(
                        "Unable to create preview for maneuver " + m.getId() + " (" + m.getClass().getSimpleName()
                        + ")");
                Maneuver next = plan.getGraph().getFollowingManeuver(m.getId());
                if (next == null) {
                    finished = true;
                    NeptusLog.pub().info("Plan finished at " + new Date());
                }
                else {                    
                    manId = next.getId();
                    NeptusLog.pub().info("Simulating " + manId);
                }
                return;
            }
            else {
                NeptusLog.pub().info("now simulating using " + curPreview.getClass().getSimpleName());
            }
        }

        state = curPreview.step(state, timestep);
        if (curPreview.isFinished() || curPreview == null) {
            try {
                Maneuver next = plan.getGraph().getFollowingManeuver(m.getId());
                if (next == null) {
                    finished = true;
                    NeptusLog.pub().warn("Plan finished at " + new Date());
                }
                else{
                    manId = next.getId();
                    NeptusLog.pub().info("Simulating " + manId);
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
