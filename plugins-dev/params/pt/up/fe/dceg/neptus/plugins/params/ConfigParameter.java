package pt.up.fe.dceg.neptus.plugins.params;
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
 * Dec 14, 2012
 * $Id:: ConfigParameter.java 9667 2013-01-04 17:56:59Z pdias                   $:
 */


import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.Parameter;

import com.l2fprod.common.propertysheet.DefaultProperty;

/**
 * @author zp
 */
public class ConfigParameter {

    protected String section, name, description;    
    protected Object value;
    protected Class<?> clazz;
    
    public ConfigParameter(String section, String name, Object value, Class<?> clazz, String description) {
        this.name = name;
        this.section = section;
        this.value = value;
        this.description = description;
        this.clazz = clazz;
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
        msg.setValue((String)getValue());
        return msg;
    }
    
    public DefaultProperty asProperty() {
        return PropertiesEditor.getPropertyInstance(name, section, clazz, value, true, description);
    }
    
    public void setProperty(DefaultProperty prop) {
        setSection(prop.getCategory());
        setName(prop.getName());
        setValue(prop.getValue().toString());
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
    public final Object getValue() {
        return value;
    }


    /**
     * @param value the value to set
     */
    public final void setValue(Object value) {
        this.value = value;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
