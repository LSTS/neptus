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
 * Jul 17, 2015
 */
package pt.lsts.neptus.mp.maneuvers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.sun.beans.editors.EnumEditor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.editor.ComboEditor;
import pt.lsts.neptus.gui.editor.StringPatternEditor;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class ScheduledGoto extends Goto {

    protected static final String DEFAULT_ROOT_ELEMENT = "ScheduledGoto";
    
    Date arrivalTime = new Date();
    ManeuverLocation.Z_UNITS travelUnits = ManeuverLocation.Z_UNITS.DEPTH;
    double travelZ = 0;
    
    @Override
    public String getType() {
        return "ScheduledGoto";
    }
    
    public Object clone() {  
        ScheduledGoto clone = new ScheduledGoto();
        super.clone(clone);
        clone.params = params;
        clone.setManeuverLocation(getManeuverLocation());
        clone.setRadiusTolerance(getRadiusTolerance());
        clone.setSpeedUnits(getUnits());
        clone.setSpeed(getSpeed());
        clone.setSpeedTolerance(getSpeedTolerance());
        
        return clone;
    }
    
    @Override
    public void parseIMCMessage(IMCMessage message) {
        try {
            pt.lsts.imc.ScheduledGoto msg = new pt.lsts.imc.ScheduledGoto(message);
            
            ManeuverLocation pos = new ManeuverLocation();
            pos.setLatitudeRads(msg.getLat());
            pos.setLongitudeRads(msg.getLon());
            pos.setZ(msg.getZ());
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(msg.getZUnits().toString()));
            setManeuverLocation(pos);
            
            setTravelUnits(ManeuverLocation.Z_UNITS.valueOf(msg.getTravelZUnits().toString()));
            setTravelZ(msg.getTravelZ());
            setArrivalTime(new Date((long)(msg.getArrivalTime() * 1000)));            
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
    
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.ScheduledGoto man = new pt.lsts.imc.ScheduledGoto();
        
        LocationType l = getManeuverLocation();
        l.convertToAbsoluteLatLonDepth();
        
        man.setLat(l.getLatitudeRads());
        man.setLon(l.getLongitudeRads());
        
        man.setZ(getManeuverLocation().getZ());
        man.setZUnits(pt.lsts.imc.ScheduledGoto.Z_UNITS.valueOf(getManeuverLocation().getZUnits().name()));
        
        man.setTravelZUnits(pt.lsts.imc.ScheduledGoto.TRAVEL_Z_UNITS.valueOf(getTravelUnits().name()));
        man.setTravelZ(travelZ);
        
        man.setArrivalTime(arrivalTime.getTime()/1000.0);
        
        return man;
    }
    
    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = new Vector<>();
        
        DefaultProperty units = PropertiesEditor.getPropertyInstance("Travel Z units", pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS.class, getTravelUnits(), true);
        units.setDisplayName(I18n.text("Travel Z units"));
        units.setShortDescription(I18n.text("Travel Z units"));
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(units, new ComboEditor<>(pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS.values()));

        DefaultProperty travZ = PropertiesEditor.getPropertyInstance("Travel Z value", Double.class, getTravelZ(), true);
        travZ.setDisplayName(I18n.text("Travel Z value"));
        travZ.setShortDescription(I18n.text("Travel Z value (meters)"));
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        DefaultProperty arrivalTime = PropertiesEditor.getPropertyInstance("Arrival Time", String.class, sdf.format(getArrivalTime()), true);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(arrivalTime, new StringPatternEditor("20[0-9][0-9]\\-[0-1][0-9]\\-[0-3][0-9] [0-2][0-9]\\:[0-5][0-9]\\:[0-5][0-9]"));
        
        props.add(travZ);
        props.add(units);
        props.add(arrivalTime);
        
        return props;        
    }
    
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        for (Property p : properties) {
            if (p.getName().equals("Travel Z units")) {
                setTravelUnits((pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS) p.getValue());                
            }
            else if (p.getName().equals("Travel Z value")) {
                setTravelZ((Double)p.getValue());
            }
            else if (p.getName().equals("Arrival Time")) {
                try {
                    setArrivalTime(sdf.parse(p.getValue().toString()));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }            
            else {
                NeptusLog.pub().debug("Property "+p.getName()+" ignored.");
            }
        }
        
    }
    

    /**
     * @return the arrivalTime
     */
    public Date getArrivalTime() {
        return arrivalTime;
    }

    /**
     * @param arrivalTime the arrivalTime to set
     */
    public void setArrivalTime(Date arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    /**
     * @return the travelUnits
     */
    public ManeuverLocation.Z_UNITS getTravelUnits() {
        return travelUnits;
    }

    /**
     * @param travelUnits the travelUnits to set
     */
    public void setTravelUnits(ManeuverLocation.Z_UNITS travelUnits) {
        this.travelUnits = travelUnits;
    }

    /**
     * @return the travelZ
     */
    public double getTravelZ() {
        return travelZ;
    }

    /**
     * @param travelZ the travelZ to set
     */
    public void setTravelZ(double travelZ) {
        this.travelZ = travelZ;
    }   
}
