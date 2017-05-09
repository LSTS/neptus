/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.neptus.gui.editor.SpeedEditor;
import pt.lsts.neptus.mp.maneuvers.ManeuverWithSpeed;
import pt.lsts.neptus.mp.preview.SpeedConversion;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 *
 */
public class SpeedType {

    public enum Units {
        MPS("m/s"),
        KPH("kph"),
        MPH("mph"),
        Percentage("%"),
        Knots("kt"),
        RPM("rpm");
        
        protected String name;
        Units(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
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
    
    private static final double MPS_TO_MPH = 2.2369362920544;
    private static final double MPS_TO_KPH = 3.6;
    private static final double MPS_TO_KNOTS = 1.9438444924574;
    
    private Units units = GeneralPreferences.speedUnits;
    private double value;
    
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
    
    public double getMPS() {
        switch (units) {
            case KPH:
                return value / MPS_TO_KPH;
            case MPH:
                return value / MPS_TO_MPH;
            case Knots:
                return value / MPS_TO_KNOTS;
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
        return getMPS() * MPS_TO_MPH;
    }
    
    public void setMPH(double value) {
        this.value = value;
        this.units = Units.MPH;
    }
    
    
    public double getKPH() {
        return getMPS() * MPS_TO_KPH;
    }
    
    public void setKPH(double value) {
        this.value = value;
        this.units = Units.KPH;
    }    
    
    public double getKnots() {
        return getMPS() * MPS_TO_KNOTS;
    }
    
    public void setKnots(double value) {
        this.value = value;
        this.units = Units.Knots;
    }  
    
    public DesiredSpeed getImcSpeed() {
        DesiredSpeed speed = new DesiredSpeed();
        switch (units) {
            case RPM:
                speed.setSpeedUnits(SpeedUnits.RPM);
                speed.setValue(getRPM());
                break;
            case Percentage:
                speed.setSpeedUnits(SpeedUnits.PERCENTAGE);
                speed.setValue(getPercentage());
                break;
            default:
                speed.setSpeedUnits(SpeedUnits.METERS_PS);
                speed.setValue(getMPS());
                break;
        }
        
        return speed;
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
        switch (maneuver.getSpeed().getUnits()) {
            case Knots:
                velocity.setText(String.valueOf(maneuver.getSpeed().getKnots()));        
                break;
            case KPH:
                velocity.setText(String.valueOf(maneuver.getSpeed().getKPH()));        
                break;
            case MPH:
                velocity.setText(String.valueOf(maneuver.getSpeed().getMPH()));        
                break;
            case Percentage:
                velocity.setText(String.valueOf(maneuver.getSpeed().getPercentage()));        
                break;
            case RPM:
                velocity.setText(String.valueOf(maneuver.getSpeed().getRPM()));        
                break;
            default:
                velocity.setText(String.valueOf(maneuver.getSpeed().getMPS()));
                break;
        }
        
        return velocity;
    }

    public void setSpeedToMessage(IMCMessage message, String field, String unitsField) {
        switch (units) {
            case RPM:
                message.setValue(unitsField, SpeedUnits.RPM);
                message.setValue(field, getRPM());
                break;
            case Percentage:
                message.setValue(unitsField, SpeedUnits.PERCENTAGE);
                message.setValue(field, getPercentage());
                break;
            default:
                message.setValue(unitsField, SpeedUnits.METERS_PS);
                message.setValue(field, getMPS());
                break;
        }       
    }
    
    public void setSpeedToMessage(IMCMessage message) {
         setSpeedToMessage(message, "speed", "speed_units");
    }
    
    
    public static Units parseUnits(String units) {
        String v = units.toUpperCase();
        
        if (v.equals("METERS_PS"))
            v = "MPS";
        
        for (Units u : Units.values()) {
            if (u.name.toUpperCase().equals(v))
                return u;
            if (u.name().toUpperCase().equals(v))
                return u;
        }
        return Units.MPS;
    }
    
    @Override
    public String toString() {
        return GuiUtils.getNeptusDecimalFormat(2).format(value)+" "+units.name;
    }
    
    public static SpeedType valueOf(String text) throws Exception {
        String[] parts = text.split(" ");
        if (parts.length != 2) {
            throw new Exception("Invalid format");
        }
        
        return new SpeedType(Double.parseDouble(parts[0]), parseUnits(parts[1]));        
    }
    
    public static void main(String[] args) throws Exception {
        String val = "10 km/h";
        System.out.println(SpeedType.valueOf(val));
        SpeedEditor editor = new SpeedEditor();
        editor.setValue(new SpeedType(10, Units.MPS));
        JPanel panel = new JPanel();
        panel.add(editor.getCustomEditor());
        JOptionPane.showConfirmDialog(null,
                editor.getCustomEditor(),
                "JOptionPane Example : ",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        
        
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
        
        PluginUtils.editPluginProperties(o, true);
        System.out.println(PluginUtils.getConfigXml(o));
    }
}
