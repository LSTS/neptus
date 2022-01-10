/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.mp.preview;

import java.util.Date;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * This class simulates the execution of a plan (roughly)
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
    protected double simTime = 0;

    /**
     * Class constructor
     * 
     * @param plan The plan to be simulated. It will simulate the plan from its initial maneuver
     */
    public PlanSimulator(final PlanType plan, SystemPositionAndAttitude start) {
        this.plan = plan.clonePlan();
        this.vehicleId = plan.getVehicle();
        engine = new SimulationEngine(plan);
        Maneuver[] mans = plan.getGraph().getManeuversSequence();

        if (start == null) {
            LocationType loc = new LocationType(plan.getMissionType().getStartLocation());
            start = new SystemPositionAndAttitude(loc, 0, 0, 0);
            for (int i = 0; i < mans.length; i++) {
                if (mans[i] instanceof LocatedManeuver) {
                    ManeuverLocation l = ((LocatedManeuver) mans[i]).getManeuverLocation();
                    start.setPosition(l);
                }
            }
        }

        VehicleType vt = plan.getVehicleType();
        simulatedPath = new PlanSimulationOverlay(plan, 0, vt == null ? VehicleType.MAX_DURATION_H : vt.getMaxDurationHours(), start);
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

    public void setManId(String manId) throws Exception {
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
                    simTime += timestep;

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

    public void setPositionEstimation(EstimatedState state, double distanceThreshold) {

        LocationType loc = new LocationType();
        loc.setLatitudeRads(state.getLat());
        loc.setLongitudeRads(state.getLon());
        loc.setDepth(state.getDepth());
        loc.translatePosition(state.getX(), state.getY(), state.getZ());
        SystemPositionAndAttitude s = new SystemPositionAndAttitude(getState());
        s.setPosition(loc);
        Pair<Integer, SimulationState> nearest = simulatedPath.nearestState(s, distanceThreshold);
        
        if (nearest != null) {
            engine.setSimulationState(state, nearest.second());
            simTime = nearest.first();
        }

    }

    public void setEstimatedState(EstimatedState state) {
        LocationType loc = new LocationType();
        loc.setLatitudeRads(state.getLat());
        loc.setLongitudeRads(state.getLon());
        loc.setDepth(state.getDepth());
        loc.translatePosition(state.getX(), state.getY(), state.getZ());

        SystemPositionAndAttitude s = new SystemPositionAndAttitude(getState());
        s.setPosition(loc);

        Pair<Integer, SimulationState> nearest = simulatedPath.nearestState(s, Integer.MAX_VALUE);

        if (nearest != null) {
            engine.setSimulationState(state, nearest.second());
            simTime = nearest.first();
        }
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

    /**
     * @return the simulatedPath
     */
    public final PlanSimulationOverlay getSimulationOverlay() {
        return simulatedPath;
    }

    public SimulatedFutureState getFutureState() {
        if (simulatedPath == null || simulatedPath.states.isEmpty())
            return null;
        long curtime = System.currentTimeMillis();
        double remainingTime = simulatedPath.getTotalTime() - simTime;
        return new SimulatedFutureState(vehicleId, new Date(curtime + (long) (remainingTime * 1000.0)),
                simulatedPath.getStates().lastElement());
    }
}
