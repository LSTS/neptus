/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Dec 14, 2012
 */
package pt.up.fe.dceg.neptus.util.cfg;

import java.util.Collection;
import java.util.Vector;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.Parameter;

import com.l2fprod.common.propertysheet.DefaultProperty;

/**
 * @author zp
 */
public class ConfigParameter {

    // boolean, integer, string, real, list:boolean, list:integer, list:string, list:real
    public enum TYPE {
        BOOLEAN,
        INTEGER,
        STRING,        
        REAL,        
        INTEGER_LIST,
        REAL_LIST,
        STRING_LIST
    }

    protected TYPE type = TYPE.STRING;
    protected String section, name, value, defaultValue;    

    public ConfigParameter(String section, String name, String value) {
        this.name = name;
        this.section = section;
        this.value = value;
        this.defaultValue = value;
    }

    public ConfigParameter() {

    }

    public void setValue(String value, TYPE type) {
        this.value = value;
        this.type = type;
    }

    public ConfigParameter(Parameter msg) {
        setMessage(msg);
    }

    public void setMessage(Parameter msg) {
        this.section = msg.getSection();
        this.value = msg.getValue();
        this.name = msg.getParam();
    }

    public Parameter getMessage() {
        Parameter msg = new Parameter();
        msg.setSection(getSection());
        msg.setParam(getName());
        msg.setValue(getValue());
        return msg;
    }

    /**
     * @return the type
     */
    public final TYPE getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public final void setType(TYPE type) {
        this.type = type;
    }

    public DefaultProperty asProperty() {
        switch (type) {
            case INTEGER:
                return PropertiesEditor.getPropertyInstance(getName(), getSection(), Integer.class, Integer.parseInt(getValue()), true, getName());
            case REAL:
                return PropertiesEditor.getPropertyInstance(getName(), getSection(), Double.class, Double.parseDouble(getValue()), true, getName());
            default:
                return PropertiesEditor.getPropertyInstance(getName(), getSection(), String.class, getValue(), true, getName());
        }
    }

    public void setProperty(DefaultProperty prop) {
        setSection(prop.getCategory());
        setName(prop.getName());
        setValue(prop.getValue().toString());
    }

    public int getIntegerValue() {
        try {
            return Integer.parseInt(getValue());
        }
        catch (Exception e) {
            return 0;
        }        
    }

    public double getRealValue() {
        try {
            return Double.parseDouble(getValue());
        }
        catch (Exception e) {
            return Double.NaN;
        }        
    }

    public boolean getBooleanValue() {
        try {
            return Boolean.parseBoolean(getValue());
        }
        catch (Exception e) {
            return false;
        }  
    }

    public Collection<String> getStringListValue() {
        try {
            return java.util.Arrays.asList(getValue().split(","));
        }
        catch (Exception e) {
            return new Vector<String>();
        }    
    }

    public Collection<Boolean> getBooleanListValue() {
        Vector<Boolean> bools = new Vector<>();

        try {
            String[] parts = getValue().split(",");
            for (String part : parts) {
                bools.add(Boolean.parseBoolean(part));
            }
            return bools;
        }
        catch (Exception e) {
            return new Vector<Boolean>();
        }    
    }
    
    public Collection<Integer> getIntegerListValue() {
        Vector<Integer> ints = new Vector<>();

        try {
            String[] parts = getValue().split(",");
            for (String part : parts) {
                ints.add(Integer.parseInt(part));
            }
            return ints;
        }
        catch (Exception e) {
            return new Vector<Integer>();
        }
    }
    
    public Collection<Double> getRealListValue() {
        Vector<Double> reals = new Vector<>();

        try {
            String[] parts = getValue().split(",");
            for (String part : parts) {
                reals.add(Double.parseDouble(part));
            }
            return reals;
        }
        catch (Exception e) {
            return new Vector<Double>();
        }
    }

    /**
     * @return the section
     */
    public final String getSection() {
        return section;
    }


    /**
     * @param section the section to set
     */
    public final void setSection(String section) {
        this.section = section;
    }


    /**
     * @return the name
     */
    public final String getName() {
        return name;
    }


    /**
     * @param paramName the name to set
     */
    public final void setName(String name) {
        this.name = name;
    }


    /**
     * @return the value
     */
    public final String getValue() {
        return ""+value;
    }


    /**
     * @param value the value to set
     */
    public final void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the defaultValue
     */
    public final String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    public final void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
