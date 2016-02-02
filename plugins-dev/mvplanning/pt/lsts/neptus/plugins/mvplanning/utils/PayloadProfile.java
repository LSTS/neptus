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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import pt.lsts.neptus.plugins.mvplanning.utils.jaxb.PayloadParametersAdapter;

/**
 * @author tsmarques
 *
 */

@XmlRootElement (name="Profile")
@XmlAccessorType(XmlAccessType.NONE)
public class PayloadProfile {
    private static final String PAYLOADS_DIR = System.getProperty("user.dir") + "/plugins-dev/mvplanning/etc/";
    private String type; /* sidescan, multibeam, etc */
    

    @XmlJavaTypeAdapter(PayloadParametersAdapter.class)
    private Map<String, String> parameters; /* <parameter, value> */
    
    @XmlElement(name = "vehicle")
    @XmlElementWrapper(name = "vehicles")
    private List<String> vehicles; /* vehicles where this profile applies */
    
    public PayloadProfile() {
        type = "";
        parameters = new HashMap<String, String>();
    }
        
    public PayloadProfile(String type) {
        this.type = type;
    }
       
    public String getPayloadType() {
        return type;
    }
    
    public boolean appliesToVehicle(String vehicleId) {
        return vehicles.contains(vehicleId);
    }
    
    public List<String> getPayloadVehicles() {
        if(vehicles == null)
            vehicles = new ArrayList<String>();
        
        return vehicles;
    }
    
    
    public void setPayloadVehicles(ArrayList<String> vehicles) {
        this.vehicles = vehicles;
    }
    
    
    public void addPayloadVehicle(String vehicleId) {
        if(vehicles == null)
            vehicles = new ArrayList<String>();
        vehicles.add(vehicleId);
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
}
