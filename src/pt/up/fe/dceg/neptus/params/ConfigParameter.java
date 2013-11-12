package pt.up.fe.dceg.neptus.params;
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
 * Author: José Pinto
 * Dec 14, 2012
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
