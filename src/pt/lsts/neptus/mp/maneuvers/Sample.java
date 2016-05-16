/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Manuel Ribeiro
 * May 8, 2015
 */
package pt.lsts.neptus.mp.maneuvers;

import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Sample.SYRINGE0;
import pt.lsts.imc.Sample.SYRINGE1;
import pt.lsts.imc.Sample.SYRINGE2;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Manuel Ribeiro
 *
 */
public class Sample extends Goto {

    protected static final String DEFAULT_ROOT_ELEMENT = "Sample";
    
    @NeptusProperty(name="Syringe 0", description="Take sample using syringe 0")
    private boolean useSyringe0 = false;
    @NeptusProperty(name="Syringe 1", description="Take sample using syringe 1")
    private boolean useSyringe1 = false;
    @NeptusProperty(name="Syringe 2", description="Take sample using syringe 2")
    private boolean useSyringe2 = false;
    
    @Override
    public String getType() {
        return "Sample";
    }

    @Override
    public Object clone() {  
        Sample clone = new Sample();
        super.clone(clone);
        clone.params = params;
        clone.setManeuverLocation(getManeuverLocation());
        clone.setSpeedUnits(getUnits());
        clone.setSpeed(getSpeed());
        clone.setSpeedTolerance(getSpeedTolerance());
        clone.setUseSyringe0(useSyringe0);
        clone.setStateSyringe1(useSyringe1);
        clone.setStateSyringe2(useSyringe2);
        
        return clone;
    }
    
    @Override
    public void parseIMCMessage(IMCMessage message) {
        try {
            pt.lsts.imc.Sample msg = pt.lsts.imc.Sample.clone(message);
            
            setMaxTime(msg.getTimeout());
            setSpeed(msg.getSpeed());
            switch (msg.getSpeedUnits()) {
                case METERS_PS:
                    setSpeedUnits("m/s");
                    break;
                case PERCENTAGE:
                    setSpeedUnits("%");
                    break;
                case RPM:
                    setSpeedUnits("RPM");
                    break;
            }
            ManeuverLocation pos = new ManeuverLocation();
            pos.setLatitudeRads(msg.getLat());
            pos.setLongitudeRads(msg.getLon());
            pos.setZ(msg.getZ());
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(msg.getZUnits().toString()));
            
            switch (msg.getSyringe0()) {
                case TRUE:
                    setUseSyringe0(true);
                    break;
                case FALSE:
                    setUseSyringe0(false);
                    break;
            }
    
            switch (msg.getSyringe1()) {
                case TRUE:
                    setStateSyringe1(true);
                    break;
                case FALSE:
                    setStateSyringe1(false);
                    break;
            }
            
            switch (msg.getSyringe2()) {
                case TRUE:
                    setStateSyringe2(true);
                    break;
                case FALSE:
                    setStateSyringe2(false);
                    break;
            }
            setManeuverLocation(pos);
            setCustomSettings(msg.getCustom());
            
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public IMCMessage serializeToIMC() {
        pt.lsts.imc.Sample sampleManeuver = new pt.lsts.imc.Sample();
        sampleManeuver.setTimeout(this.getMaxTime());
        LocationType l = getManeuverLocation();
        l.convertToAbsoluteLatLonDepth();
        
        sampleManeuver.setLat(l.getLatitudeRads());
        sampleManeuver.setLon(l.getLongitudeRads());
        sampleManeuver.setZ(getManeuverLocation().getZ());
        sampleManeuver.setZUnits(pt.lsts.imc.Sample.Z_UNITS.valueOf(getManeuverLocation().getZUnits().name()));
        sampleManeuver.setSpeed(this.getSpeed());

        switch (this.getUnits()) {
            case "m/s":
                sampleManeuver.setSpeedUnits(pt.lsts.imc.Sample.SPEED_UNITS.METERS_PS);
                break;
            case "RPM":
                sampleManeuver.setSpeedUnits(pt.lsts.imc.Sample.SPEED_UNITS.RPM);
                break;
            case "%":
                sampleManeuver.setSpeedUnits(pt.lsts.imc.Sample.SPEED_UNITS.PERCENTAGE);
                break;
            default:
                sampleManeuver.setSpeedUnits(pt.lsts.imc.Sample.SPEED_UNITS.RPM);
                break;
        }
       
        sampleManeuver.setSyringe0(getStateSyringe0() ? SYRINGE0.TRUE : SYRINGE0.FALSE);
        sampleManeuver.setSyringe1(getStateSyringe1() ? SYRINGE1.TRUE : SYRINGE1.FALSE);
        sampleManeuver.setSyringe2(getStateSyringe2() ? SYRINGE2.TRUE : SYRINGE2.FALSE);

        sampleManeuver.setCustom(getCustomSettings());

        return sampleManeuver;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        return ManeuversUtil.getPropertiesFromManeuver(this);     
    }
    
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        ManeuversUtil.setPropertiesToManeuver(this, properties);
    }
    /**
     * @return syringe0 state
     */
    public boolean getStateSyringe0() {
        return useSyringe0;
    }
    
    /**
     * @param syringe0
     */
    public void setUseSyringe0(boolean syringe0) {
        this.useSyringe0 = syringe0;
    }

    /**
     * @return syringe1 state
     */
    public boolean getStateSyringe1() {
        return useSyringe1;
    }

    /**
     * @param syringe1
     */
    public void setStateSyringe1(boolean syringe1) {
        this.useSyringe1 = syringe1;
    }

    /**
     * @return syringe2 state
     */
    public boolean getStateSyringe2() {
        return useSyringe2;
    }

    /**
     * @param syringe2
     */
    public void setStateSyringe2(boolean syringe2) {
        this.useSyringe2 = syringe2;
    }
}