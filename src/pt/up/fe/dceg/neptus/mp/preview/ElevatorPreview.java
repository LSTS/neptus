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
 * $Id:: ElevatorPreview.java 9880 2013-02-07 15:23:52Z jqcorreia               $:
 */
package pt.up.fe.dceg.neptus.mp.preview;

import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.maneuvers.Elevator;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class ElevatorPreview implements IManeuverPreview<Elevator> {

    protected ManeuverLocation destination;
    protected double speed, radius, startZ, endZ;
    protected boolean finished = false, clockwise = true;
    protected String loiterType;
    UnicycleModel model = new UnicycleModel();
    @Override
    public boolean init(String vehicleId, Elevator man, SystemPositionAndAttitude state, Object manState) {
        
        if (man.startFromCurrentPosition) {
            LocationType loc = new LocationType(state.getPosition());
            loc.translatePosition(0, man.getRadius()+5, 0);
            destination = new ManeuverLocation();
            destination.setLocation(loc);
            destination.setZ(man.getEndLocation().getZ());
            destination.setZUnits(man.getEndLocation().getZUnits());
            startZ = endZ = man.getEndZ();
        }
        else {
            destination = man.getEndLocation().clone();
            startZ = man.getStartZ();
            endZ = man.getEndZ();                            
        }
        
        if (destination.getZUnits() == ManeuverLocation.Z_UNITS.DEPTH)
            destination.setDepth(startZ);
        else if (destination.getZUnits() == ManeuverLocation.Z_UNITS.ALTITUDE)
            destination.setDepth(SimulationEngine.BOTTOM_DEPTH-startZ);
        else
            destination.setDepth(0);
                
        clockwise = true;
        radius = man.getRadius();
        speed = man.getSpeed();

        if (man.getSpeedUnits().equals("RPM")) 
            speed = SpeedConversion.convertRpmtoMps(speed);
        else if (man.getSpeedUnits().equals("%")) // convert to RPM and then to m/s
            speed = SpeedConversion.convertPercentageToMps(speed);

        speed = Math.min(speed, SpeedConversion.MAX_SPEED);

        model.setState(state);        
        return true;
    }

    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep) {
        model.setState(state);
        double distToDestination = state.getPosition().getHorizontalDistanceInMeters(destination);
        if (distToDestination-2 > radius) {
            model.guide(destination, speed);
        }
        else {
            double perpend = state.getPosition().getXYAngle(destination) + Math.PI/2;
            LocationType loc = new LocationType(state.getPosition());
            loc.setDepth(endZ);
            loc.translatePosition(Math.cos(perpend) * -20, Math.sin(perpend) * -20, 0);
            model.guide(loc, speed);
        }

        model.advance(timestep);

        if (Math.abs(state.getPosition().getDepth()-endZ) < 0.5)
            finished = true;
        
        return model.getState();
    }
    
    @Override
    public Object getState() {
        return null;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void reset(SystemPositionAndAttitude state) {
        model.setState(state);
    }
}
