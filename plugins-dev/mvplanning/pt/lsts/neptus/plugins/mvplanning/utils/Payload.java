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
 * Author: tsmarques
 * 25 Jan 2016
 */
package pt.lsts.neptus.plugins.mvplanning.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import pt.lsts.neptus.plugins.mvplanning.utils.jaxb.PayloadParametersAdapter;

/**
 * @author tsmarques
 *
 */

@XmlAccessorType(XmlAccessType.NONE)
public class Payload {   
    @XmlAttribute
    private String type; /* sidescan, multibeam, etc */
    

    @XmlJavaTypeAdapter(PayloadParametersAdapter.class)
    private Map<String, String> parameters; /* <parameter, value> */
    
    
    public Payload() {
        type = "";
        parameters = new HashMap<String, String>();
    }
        
    public Payload(String type) {
        this.type = type;
    }
           
    public String getPayloadType() {
        return type;
    }
        
    
    public void setPayloadParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
    
    public void addPayloadParamater(String parameter, String value) {
        if(parameters == null)
            parameters = new HashMap<String, String>();
        parameters.put(parameter, value);
    }
    
    
    public String getPayloadParameter(String parameter) {
        if(parameters == null)
            return null;

        return parameters.get(parameter);
    }
    
    public Map<String, String> getPayloadParameters() {
        if(parameters == null)
            parameters = new HashMap<String, String>();
        return parameters;
    }
    
    public void printPayloadParameters() {
        for(Entry<String, String> param : parameters.entrySet())
            System.out.println(param.getKey() + " : " + param.getValue());
    }
}
