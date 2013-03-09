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

import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.maneuvers.Loiter;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class LoiterPreview implements IManeuverPreview<Loiter> {

    protected LocationType destination;
    protected double speed, radius, duration;
    protected boolean finished = false, clockwise = true;
    protected String loiterType;
    protected double loiterTime = 0;
    protected boolean enteredLoiter = false;
    
    UnicycleModel model = new UnicycleModel();
    @Override
    public boolean init(String vehicleId, Loiter man, SystemPositionAndAttitude state, Object manState) {
        enteredLoiter = false;
        destination = new LocationType(man.getManeuverLocation());
        if (man.getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.DEPTH)
            destination.setDepth(man.getManeuverLocation().getZ());
        else if (man.getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.ALTITUDE)
            destination.setDepth(SimulationEngine.BOTTOM_DEPTH-man.getManeuverLocation().getZ());
        else
            destination.setDepth(0);
        
        clockwise = man.getDirection().equalsIgnoreCase("Clockwise");
        radius = man.getRadius();
        loiterType = man.getLoiterType();
        duration = man.getLoiterDuration();
        speed = man.getSpeed();

        if (man.getSpeedUnits().equals("RPM")) 
            speed = SpeedConversion.convertRpmtoMps(speed);
        else if (man.getSpeedUnits().equals("%")) // convert to RPM and then to m/s
            speed = SpeedConversion.convertPercentageToMps(speed);

        speed = Math.min(speed, SpeedConversion.MAX_SPEED);

        if (manState != null && manState instanceof Double) {
            loiterTime = (Double) manState;
        }
        model.setState(state);        
        return true;
    }

    
    
    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep) {
        model.setState(state);
        double distToDestination = state.getPosition().getDistanceInMeters(destination);
       if (loiterType.equalsIgnoreCase("circular")) {

            if (distToDestination-2 > radius) {
                model.guide(destination, speed);
                if (!enteredLoiter)
                    loiterTime = 0;
            }
            else {
                enteredLoiter = true;
                double perpend = state.getPosition().getXYAngle(destination) + Math.PI/2;
                LocationType loc = new LocationType(state.getPosition());
                loc.setDepth(destination.getDepth());
                if (clockwise)
                    loc.translatePosition(Math.cos(perpend) * -20, Math.sin(perpend) * -20, 0);
                else
                    loc.translatePosition(Math.cos(perpend) * 20, Math.sin(perpend) * 20, 0);
                model.guide(loc, speed);
                loiterTime += timestep;
            }            
        }
        else {
            if (distToDestination < speed * 2)
                loiterTime += timestep;            
            else
                model.guide(destination, speed);
        }

        model.advance(timestep);

        if (loiterTime > duration)
            finished = true;

        return model.getState();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void reset(SystemPositionAndAttitude state) {
        model.setState(state);
    }
    
    @Override
    public Object getState() {
        return new Double(loiterTime);
    }
}
