/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 14/05/2016
 */
package pt.lsts.neptus.mp.preview.payloads;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class CameraFOV {

    private double hAOV, vAOV, roll, pitch, yaw, altitude;
    private LocationType loc = new LocationType();
    
    public CameraFOV(double horAOVrads, double verAOVrads) {
        this.hAOV = horAOVrads;
        this.vAOV = verAOVrads;
    }
    
    public double getDiagAOV() {
        double hDist = Math.tan(hAOV/2);
        double vDist = Math.tan(vAOV/2);
        double hyp = Math.sqrt(hDist*hDist + vDist * vDist);
        return Math.atan(hyp) * 2;
    }
    
    public void setRoll(double rollRads) {
        this.roll = rollRads;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }
    
    public void setAltitude(double alt) {
        this.altitude = alt;
    }
    
    public void setLocation(LocationType loc) {
        this.loc.setLocation(loc);
    }
    
    public double getAltitude() {
        return altitude;
    }

    public double getRoll() {
        return roll;
    }

    public double getPitch() {
        return pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public LocationType getLocation() {
        return loc;
    }

    public void setState(EstimatedState state) {
        setLocation(IMCUtils.parseLocation(state));
        setAltitude(state.getAlt());
        setRoll(state.getPhi());
        setPitch(state.getTheta());
        setYaw(state.getPsi());
    }
    
            
    public LocationType getLookAt() {
        LocationType loc = new LocationType(this.loc);
        double rightOffset = -Math.tan(getRoll()) * getAltitude();
        double topOffset = Math.tan(getPitch()) * getAltitude();
        double northOffset = Math.cos(getYaw()) * topOffset - Math.sin(getYaw()) * rightOffset;
        double eastOffset = Math.cos(getYaw()) * rightOffset + Math.sin(getYaw()) * topOffset;
        loc.translatePosition(northOffset, eastOffset, 0);
        return loc;
    }
    
    public static void main(String[] args) {
        CameraFOV fov = new CameraFOV(Math.toRadians(60), Math.toRadians(45));
        fov.setPitch(Math.toRadians(30));
        fov.setAltitude(100);
        System.out.println(fov.getLookAt());

    }
}
