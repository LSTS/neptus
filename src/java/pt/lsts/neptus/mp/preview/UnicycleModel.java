/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 10, 2011
 */
package pt.lsts.neptus.mp.preview;

import java.util.Vector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * This class implements the Unicycle Model dynamics to be used for a rough preview of vehicle's behavior 
 * @author zp
 *
 */
public class UnicycleModel {

    protected double latRad, lonRad, x, y, rollRad, pitchRad, yawRad, depth, speedMPS;
    protected double targetLatRad, targetLonRad, maxSteeringRad = Math.toRadians(7);
    protected boolean arrived = true;
    
    protected double maxPitch = 15, maxSpeed = 2;
    
    public double getCurrentAltitude() {
        double a = SimulationEngine.simBathym.getSimulatedDepth(getCurrentPosition());
        return a - depth;
    }
    
    public LocationType getCurrentPosition() {
        LocationType loc = new LocationType();
        loc.setLatitudeDegs(Math.toDegrees(latRad));
        loc.setLongitudeDegs(Math.toDegrees(lonRad));
        loc.setDepth(depth);
        loc.setOffsetEast(x);
        loc.setOffsetNorth(y);
        return loc;
    }
    
    public SystemPositionAndAttitude getState() {
        LocationType loc = new LocationType();
        loc.setLatitudeDegs(Math.toDegrees(latRad));
        loc.setLongitudeDegs(Math.toDegrees(lonRad));
        loc.setDepth(depth);
        loc.setOffsetEast(x);
        loc.setOffsetNorth(y);
        
        return new SystemPositionAndAttitude(loc, rollRad, pitchRad, yawRad);
        
    }

    public void setState(SystemPositionAndAttitude state) {
        if (state == null) {
            NeptusLog.pub().error("setState(null)");
            return;
        }
        
        LocationType pos = state.getPosition();
        pos.convertToAbsoluteLatLonDepth();
        latRad = pos.getLatitudeRads();
        lonRad = pos.getLongitudeRads();
        x = y = 0;
        depth = pos.getDepth();
        speedMPS = state.getVx();
        rollRad = state.getRoll();
        pitchRad = state.getPitch();
        yawRad = state.getYaw();        
    }

    /**
     * Reset the state of the vehicle to be at the given position
     * @param loc The new vehicle location. New heading will be calculated as a jump from previous location
     */
    public void setLocation(LocationType loc) {
        LocationType old = getCurrentPosition();
        loc.convertToAbsoluteLatLonDepth();
        latRad = loc.getLatitudeRads();
        lonRad = loc.getLongitudeRads();
        depth = loc.getDepth();
        x = y = 0;
        pitchRad = rollRad = 0;
        yawRad = old.getXYAngle(loc);
    }

    /**
     * Advance the given time by integrating the vehicle position (with current heading and speed)
     * @param timestepSecs
     */
    public void advance(double timestepSecs) {
        //NeptusLog.pub().info("<###>speed: "+speedMPS+", yaw: "+yawRad);
        double angle = yawRad;
        x += speedMPS * timestepSecs * Math.sin(angle);
        y += speedMPS * timestepSecs * Math.cos(angle);
        depth += speedMPS * timestepSecs * Math.sin(pitchRad);
        
        if (depth > 0)
            depth -= 0.05* timestepSecs;
            
    }

    /**
     * Guide the vehicle to a certain location. This method will compute a new heading and speed that will guide the vehicle to the given location.<br/>
     * If the vehicle is already at or near the target location, the speed is set to 0 and the method returns <b>true</b>.  
     * @param loc The target location
     * @param speed Desired speed
     * @return <b>true</b> if the vehicle is arrived
     */
    public boolean guide(LocationType loc, double speed, Double altitude) {
        if (loc.getHorizontalDistanceInMeters(getCurrentPosition()) < speed) {
            speedMPS = rollRad = pitchRad = 0;
            return true;            
        }            
        
        speedMPS = speed;

        if (altitude != null) {
            double curBathym = SimulationEngine.simBathym.getSimulatedDepth(getCurrentPosition());
            double curAltitude = curBathym-depth;
            if (curAltitude > altitude)
                pitchRad = Math.toRadians(maxPitch);
            else if (curAltitude < altitude)
                pitchRad = -Math.toRadians(maxPitch);
            else {
                depth = (SimulationEngine.simBathym.getSimulatedDepth(getCurrentPosition())-altitude);
            }
        }
        else {
        
            if (loc.getDepth() > depth+0.1)
                pitchRad = Math.toRadians(maxPitch);
            else if (loc.getDepth() < depth-0.1)
                pitchRad = -Math.toRadians(maxPitch);
            else {
                depth = loc.getDepth();
                pitchRad = 0;
            }
        }
        
        double ang = getCurrentPosition().getXYAngle(loc);
        
        double diffAng = yawRad - ang;
        
        while (diffAng > Math.PI)
            diffAng -= Math.PI * 2;
        while (diffAng < -Math.PI)
            diffAng += Math.PI * 2;
        
        if (Math.abs(diffAng) < maxSteeringRad)
            yawRad = ang;
        else if (diffAng > 0)
            yawRad -= maxSteeringRad;
        else
            yawRad += maxSteeringRad;

        return false;    
    }

    /**
     * @return the latRad
     */
    public double getLatRad() {
        return latRad;
    }

    /**
     * @param latRad the latRad to set
     */
    public void setLatRad(double latRad) {
        this.latRad = latRad;
    }

    /**
     * @return the lonRad
     */
    public double getLonRad() {
        return lonRad;
    }

    /**
     * @param lonRad the lonRad to set
     */
    public void setLonRad(double lonRad) {
        this.lonRad = lonRad;
    }

    /**
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * @return the depth
     */
    public double getDepth() {
        return depth;
    }

    /**
     * @param depth the depth to set
     */
    public void setDepth(double depth) {
        this.depth = depth;
    }

    /**
     * @return the rollRad
     */
    public double getRollRad() {
        return rollRad;
    }

    /**
     * @param rollRad the rollRad to set
     */
    public void setRollRad(double rollRad) {
        this.rollRad = rollRad;
    }

    /**
     * @return the pitchRad
     */
    public double getPitchRad() {
        return pitchRad;
    }

    /**
     * @param pitchRad the pitchRad to set
     */
    public void setPitchRad(double pitchRad) {
        this.pitchRad = pitchRad;
    }

    /**
     * @return the yawRad
     */
    public double getYawRad() {
        return yawRad;
    }

    /**
     * @param yawRad the yawRad to set
     */
    public void setYawRad(double yawRad) {
        this.yawRad = yawRad;
    }



    /**
     * @return the maxSteeringRad
     */
    public double getMaxSteeringRad() {
        return maxSteeringRad;
    }



    /**
     * @param maxSteeringRad the maxSteeringRad to set
     */
    public void setMaxSteeringRad(double maxSteeringRad) {
        this.maxSteeringRad = maxSteeringRad;
    }


   

    public static void main(String[] args) {
        LocationType loc = new LocationType();

        Vector<LocationType> locs = new Vector<LocationType>();

        loc.setLatitudeDegs(41);
        loc.setLongitudeDegs(-8);
        StateRenderer2D r2d = new StateRenderer2D(new LocationType(loc));
        
        locs.add(new LocationType(loc));

        loc.translatePosition(10, -20, 2);
        locs.add(new LocationType(loc));

        loc.translatePosition(30, 30, 0);
        locs.add(new LocationType(loc));

        loc.translatePosition(-30, 0, -2);
        locs.add(new LocationType(loc));

        loc.convertToAbsoluteLatLonDepth();
        loc.setLatitudeDegs(41);
        loc.setLongitudeDegs(-8);
        locs.add(new LocationType(loc));
        
        UnicycleModel model = new UnicycleModel();
        model.setMaxSteeringRad(Math.toRadians(3));
        GuiUtils.testFrame(r2d);
        //model.startSimulation(r2d, locs, 1.3, 0.5);
        
    }
}
