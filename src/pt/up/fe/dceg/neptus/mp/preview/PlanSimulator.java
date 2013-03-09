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
package pt.up.fe.dceg.neptus.mp.preview;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * This class simulates the execution of plan (roughly)
 * 
 * @author zp
 */
public class PlanSimulator {

    SimulationEngine engine = null;
    
    protected Thread simulationThread = null;
    protected boolean finished = false;
    protected PlanSimulationOverlay simulatedPath = null;
    protected double timestep = 0.25;
    protected PlanType plan;
    protected String vehicleId;
    
    
    
    /**
     * Class constructor
     * 
     * @param plan The plan to be simulated. It will simulate the plan from its initial maneuver
     */
    public PlanSimulator(final PlanType plan, SystemPositionAndAttitude start) {
        this.plan = plan;
        this.vehicleId = plan.getVehicle();
        engine = new SimulationEngine(plan);
        Maneuver[] mans = plan.getGraph().getManeuversSequence();
        
        if (start == null) {
            LocationType loc = new LocationType(plan.getMissionType().getStartLocation());
            start = new SystemPositionAndAttitude(loc, 0,0,0);
            for (int i = 0; i < mans.length; i++) {
                if (mans[i] instanceof LocatedManeuver) {
                    ManeuverLocation l = ((LocatedManeuver)mans[i]).getManeuverLocation();
                    start.setPosition(l);
                }
            }
        }
        
        simulatedPath = new PlanSimulationOverlay(plan, 0, 4, start);
    }
    
    public SystemPositionAndAttitude getState() {
        return engine.getState();
    }
    
    public void setState(SystemPositionAndAttitude state) {
        engine.setState(state);
    }
    
    public PlanType getPlan() {
        return plan;
    }
    
    public String getManId() {
        return engine.manId;
    }
    
    public void setManId(String manId) {
        engine.setManeuverId(manId);
    }
    
    
    /**
     * Start simulation thread
     */
    public void startSimulation() {

        if (simulationThread != null)
            simulationThread.interrupt();

        simulationThread = new Thread("Plan Execution Simulator") {
            public void run() {
                while (!finished) {
                    engine.simulationStep();
                    try {
                        Thread.sleep((long) (1000 * timestep));
                    }
                    catch (InterruptedException e) {
                        return;
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
                simulationThread = null;
            };
        };
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    public void setPositionEstimation(EstimatedState state) {
        
        LocationType loc = new LocationType();
        loc.setLatitudeRads(state.getLat());
        loc.setLongitudeRads(state.getLon());
        loc.setDepth(state.getDepth());
        loc.translatePosition(state.getX(), state.getY(), state.getZ());

        SystemPositionAndAttitude s = new SystemPositionAndAttitude(getState());
        s.setPosition(loc);
        
        SimulationState newState = simulatedPath.nearestState(s, 5);
        if (newState != null)
            engine.setSimulationState(state, newState);
    }
    
    public void setEstimatedState() {
        
    }

    public void stopSimulation() {
        if (simulationThread != null)
            simulationThread.interrupt();
        simulationThread = null;
    }

    /**
     * @return the timestep
     */
    public double getTimestep() {
        return timestep;
    }

    /**
     * @param timestep the timestep to set
     */
    public void setTimestep(double timestep) {
        this.timestep = timestep;
    }

    /**
     * @return the finished
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Verify if the simulator is currently simulating states
     * 
     * @return <b>true</b> if the simulator is currently executing
     */
    public boolean isRunning() {
        return simulationThread != null;
    }

    /**
     * @return the vehicleId
     */
    public final String getVehicleId() {
        return vehicleId;
    }

    /**
     * @param vehicleId the vehicleId to set
     */
    public final void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }
}
