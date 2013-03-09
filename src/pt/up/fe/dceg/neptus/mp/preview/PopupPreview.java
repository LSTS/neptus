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

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.maneuvers.PopUp;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class PopupPreview implements IManeuverPreview<PopUp> {

    protected LocationType destination;
    protected double speed;
    protected boolean finished = false;
    protected double totalTime = 0;
    protected double surfaceTime = 0;
    protected double maxTime, duration;
    
    UnicycleModel model = new UnicycleModel();
    @Override
    public boolean init(String vehicleId, PopUp man, SystemPositionAndAttitude state, Object manState) {
        destination = new LocationType(man.getManeuverLocation());
        destination.setAbsoluteDepth(0);
        maxTime = man.getMaxTime();
        duration = man.getDuration();
        speed = man.getSpeed();
        model.setMaxSteeringRad(Math.toRadians(9));
        if (man.getUnits().equals("RPM")) 
            speed = SpeedConversion.convertRpmtoMps(speed);
        else if (man.getUnits().equals("%")) // convert to RPM and then to m/s
            speed = SpeedConversion.convertPercentageToMps(speed);

        speed = Math.min(speed, SpeedConversion.MAX_SPEED);           
        
        model.setState(state);        
        return true;
    }
    

    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep) {
        
        model.setState(state);        
        boolean there = model.guide(destination, speed);
        if (totalTime >= maxTime || surfaceTime >= duration)
            finished = true;
        else
            model.advance(timestep);
        
        totalTime += timestep;
        if (there && model.getDepth() <= 0.1)
            surfaceTime += timestep;
        
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
