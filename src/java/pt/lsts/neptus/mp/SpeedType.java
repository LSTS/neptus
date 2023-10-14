/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 09/05/2017
 */
package pt.lsts.neptus.mp;

import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.neptus.mp.maneuvers.ManeuverWithSpeed;
import pt.lsts.neptus.mp.preview.SpeedConversion;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.UnitsUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 * @author pdias
 *
 */
public class SpeedType {

    public enum Units {
        MPS("m/s"),
        Knots("kn"),
        KPH("km/h"),
        MPH("MPH"),
        RPM("RPM"),
        Percentage("%");
        
        protected String name;
        Units(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        /**
         * This will parse the units, tests more cases than {@link #valueOf(String)}
         * and always return a value (defaults to {@value #MPS}.
         * 
         * @param name
         * @return
         */
        public static Units parse(String name) {
            @SuppressWarnings("unused")
            IllegalArgumentException exp = null;
            try {
                return valueOf(name);
            }
            catch (IllegalArgumentException e) {
                exp = e;
            }

            String v = name.toUpperCase();

            for (Units u : Units.values()) {
                if (u.name.toUpperCase().equals(v))
                    return u;
                if (u.name().toUpperCase().equals(v))
                    return u;
            }
            
            switch (v) {
                case "KPH":
                    return Units.KPH;
                case "knot":
                case "kt":
                    return Units.Knots;
                case "METERS_PS":
                default:
                    break;
            }
            
            return Units.MPS;
            //throw exp;
        }
    }

    private Units units = GeneralPreferences.speedUnits;
    private double value;
    
    public SpeedType(double value, Units units) {
        this.value = value;
        this.units = units;
    }
    
    public SpeedType() {
        this.value = 0;
    }
    
    public SpeedType(SpeedType other) {
        this.value = other.value;
        this.units = other.units;
    }
    
    /**
     * @return the units
     */
    public final Units getUnits() {
        return units;
    }
    
    /**
     * @param units the units to set
     */
    public final void setUnits(Units units) {
        this.units = units;
    }
    
    public double getValue() {
        return this.value;
    }
    
    public void setValue(double value) {
        this.value = value;
    }
    
    public void set(SpeedType speed) {
        setValue(speed.getValue());
        setUnits(speed.getUnits());
    }

    public double getMPS() {
        switch (units) {
            case KPH:
                return value / UnitsUtil.MS_TO_KMH;
            case MPH:
                return value / UnitsUtil.MS_TO_MPH;
            case Knots:
                return value / UnitsUtil.MS_TO_KNOT;
            case RPM:
                return SpeedConversion.convertRpmtoMps(value);
            case Percentage:
                return SpeedConversion.convertPercentageToMps(value);
            default:
                return value;
        }
    }
    
    public void setMPS(double value) {
        this.value = value;
        this.units = Units.MPS;
    }
    
    public double getRPM() {
        switch (units) {
            case RPM:
                return value;
            case Percentage:
                return SpeedConversion.convertPercentagetoRpm(value);
            default:
                return SpeedConversion.convertMpstoRpm(getMPS());
        }
    }
    
    public void setRPM(double value) {
        this.value = value;
        this.units = Units.RPM;
    }
    
    public double getPercentage() {
        return SpeedConversion.convertMpsToPercentage(value);
    }
    
    public void setPercentage(double value) {
        this.value = value;
        this.units = Units.Percentage;
    }
    
    public double getMPH() {
        return getMPS() * UnitsUtil.MS_TO_MPH;
    }
    
    public void setMPH(double value) {
        this.value = value;
        this.units = Units.MPH;
    }
    
    
    public double getKPH() {
        return getMPS() *  UnitsUtil.MS_TO_KMH;
    }
    
    public void setKPH(double value) {
        this.value = value;
        this.units = Units.KPH;
    }    
    
    public double getKnots() {
        return getMPS() *  UnitsUtil.MS_TO_KNOT;
    }
    
    public void setKnots(double value) {
        this.value = value;
        this.units = Units.Knots;
    }  
    
    public static SpeedType parseImcSpeed(IMCMessage message) {
        return parseImcSpeed(message, "speed", "speed_units");        
    }
    
    public static SpeedType parseImcSpeed(IMCMessage message, String field, String unitsField) {
        try {
            switch (message.getString(unitsField).toUpperCase()) {
                case "RPM":
                    return new SpeedType(message.getDouble(field), Units.RPM);
                case "PERCENTAGE":
                    return new SpeedType(message.getDouble(field), Units.Percentage);
                default:
                    return new SpeedType(message.getDouble(field), Units.MPS);            
            }    
        }
        catch (Exception e) {
            e.printStackTrace();
            return new SpeedType();
        }        
    }
    
    public static <M extends ManeuverWithSpeed> SpeedType parseManeuverSpeed(Element root, M maneuver) throws Exception {
        Node speedNode = root.selectSingleNode("//speed");
        if (speedNode == null)
            speedNode = root.selectSingleNode("//velocity"); // Is deprecated but to load old defs
        double speed = Double.parseDouble(speedNode.getText());
        String units = speedNode.valueOf("@unit");
        SpeedType parsed = new SpeedType(speed, SpeedType.parseUnits(units));
        maneuver.setSpeed(parsed);
        return parsed;
    }
    
    public static <M extends ManeuverWithSpeed> Element addSpeedElement(Element root, M maneuver) {
        Element velocity = root.addElement("speed");
        velocity.addAttribute("type", "float");
        velocity.addAttribute("unit", maneuver.getSpeed().getUnits().name());
        velocity.setText(String.valueOf(maneuver.getSpeed().getValue()));        
        return velocity;
    }

    public void setSpeedToMessage(IMCMessage message, String field, String unitsField) {
        switch (units) {
            case RPM:
                message.setValue(unitsField, SpeedUnits.RPM.value());
                message.setValue(field, getRPM());
                break;
            case Percentage:
                message.setValue(unitsField, SpeedUnits.PERCENTAGE.value());
                message.setValue(field, getPercentage());
                break;
            default:
                message.setValue(unitsField, SpeedUnits.METERS_PS.value());
                message.setValue(field, getMPS());
                break;
        }       
    }
    
    public void setSpeedToMessage(IMCMessage message) {
         setSpeedToMessage(message, "speed", "speed_units");
    }
    
    public static Units parseUnits(String units) {
        return Units.parse(units);
    }
    
    @Override
    public String toString() {
        return GuiUtils.getNeptusDecimalFormat(2).format(value) + " " + units.name;
    }

    public String toStringAsDefaultUnits() {
        Units defaultUnits = GeneralPreferences.speedUnits;
        return GuiUtils.getNeptusDecimalFormat(2).format(getAs(defaultUnits)) + " " + defaultUnits.name;
    }

    public static SpeedType valueOf(String text) throws Exception {
        String[] parts = text.split(" ");
        if (parts.length != 2) {
            throw new Exception("Invalid format");
        }
        
        return new SpeedType(Double.parseDouble(parts[0]), parseUnits(parts[1]));        
    }
    
    public void convertTo(Units units) {
        switch (units) {
            case Knots:
                setValue(getKnots());
                break;
            case KPH:
                setValue(getKPH());
                break;
            case MPH:
                setValue(getMPH());
                break;
            case RPM:
                setValue(getRPM());
                break;
            case Percentage:
                setValue(getPercentage());
                break;
            default:
                setValue(getMPS());
                break;
        }
        setUnits(units);
    }

    public void convertToDefaultUnits() {
        convertTo(GeneralPreferences.speedUnits);
    }

    /**
     * @param newUnits
     * @return
     */
    public double getAs(Units newUnits) {
        switch (newUnits) {
            case Knots:
                return getKnots();
            case KPH:
                return getKPH();
            case MPH:
                return getMPH();
            case RPM:
                return getRPM();
            case Percentage:
                return getPercentage();
            default:
                return getMPS();
        }
    }

    public static void main(String[] args) throws Exception {
        Object o = new Object() {
             @NeptusProperty
             SpeedType speed1 = new SpeedType(600, Units.RPM);
             
             @NeptusProperty
             SpeedType speed2 = new SpeedType(1.25, Units.MPS);
             
             @NeptusProperty
             double x = 10;
             
             @NeptusProperty
             String y = "10";
        };
        System.out.println(GeneralPreferences.forceSpeedUnits);
        
        PluginUtils.editPluginProperties(o, true);
        System.out.println(PluginUtils.getConfigXml(o));
        
        System.out.println(Units.parse("KPH"));
    }
}
