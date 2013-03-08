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
 * $Id:: StationKeepingPreview.java 9880 2013-02-07 15:23:52Z jqcorreia         $:
 */
package pt.up.fe.dceg.neptus.mp.preview;

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.maneuvers.StationKeeping;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class StationKeepingPreview implements IManeuverPreview<StationKeeping> {

    protected LocationType destination;
    protected double speed;
    protected boolean finished = false;
    protected double sk_time = -0.1;
    protected double maxTime, duration;
    protected boolean arrived = false;
    UnicycleModel model = new UnicycleModel();
    @Override
    public boolean init(String vehicleId, StationKeeping man, SystemPositionAndAttitude state, Object manState) {
        destination = new LocationType(man.getLocation());
        destination.setDepth(0);
        maxTime = man.getMaxTime();
        duration = man.getDuration();
        speed = man.getSpeed();
        model.setMaxSteeringRad(Math.toRadians(9));
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
  
        if (sk_time >= duration)
            finished = true;
        
        if (arrived) {
            sk_time += timestep;
            return model.getState();
        }
        
        if (state.getPosition().getDepth() > 0) {
            model.guide(destination, 0);
            model.advance(timestep);
        }
        else {
            model.getState().getPosition().setDepth(0);
            arrived = model.guide(destination, speed);
            model.advance(timestep);
            sk_time = 0;
        }
        
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
        return null;
    }
}
