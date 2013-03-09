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
 * Nov 20, 2012
 */
package pt.up.fe.dceg.neptus.mp.preview;

import java.util.Vector;

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.up.fe.dceg.neptus.mp.maneuvers.RowsManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class RowsManeuverPreview implements IManeuverPreview<RowsManeuver> {

    protected Vector<LocationType> locs = new Vector<>();
    protected int locIndex = 0;
    protected String vehicleId = null;
    protected double speed;
    protected UnicycleModel model = new UnicycleModel();
    protected boolean finished = false;
    
    @Override
    public boolean init(String vehicleId, RowsManeuver man, SystemPositionAndAttitude state, Object manState) {
        locs.addAll(man.getPathLocations());
        for (LocationType loc : locs) {
            if (man.getManeuverLocation().getZUnits() == Z_UNITS.DEPTH)
                loc.setDepth(man.getManeuverLocation().getZ());
            else if (man.getManeuverLocation().getZUnits() == Z_UNITS.ALTITUDE)
                loc.setDepth(SimulationEngine.BOTTOM_DEPTH-man.getManeuverLocation().getZ());
        }        
        this.vehicleId = vehicleId;
        this.locIndex = 0;
        speed = man.getSpeed();
        if (man.getUnits().equals("RPM")) 
            speed = SpeedConversion.convertRpmtoMps(speed);
        else if (man.getUnits().equals("%")) // convert to RPM and then to m/s
            speed = SpeedConversion.convertPercentageToMps(speed);

        speed = Math.min(speed, SpeedConversion.MAX_SPEED);    
        
        model.setState(state);

        if (manState != null && manState instanceof Integer)
            locIndex = (Integer)manState;
        
        return true;
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
    
    public void reset(SystemPositionAndAttitude state) {
        //not supported for this maneuver
    };


    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep) {
        
        if (locIndex >= locs.size()) {
            finished = true;
            return state;
        }
        model.setState(state);
        LocationType destination = locs.get(locIndex);
        
        if (model.guide(destination, speed)) {
            locIndex++;
        }
        else
            model.advance(timestep);            
        
        return model.getState();
    }
    
    @Override
    public Object getState() {
        return new Integer(locIndex);
    }
    
}
