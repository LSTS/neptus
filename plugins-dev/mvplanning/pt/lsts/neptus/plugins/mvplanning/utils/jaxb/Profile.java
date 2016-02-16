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
 * 28 Jan 2016
 */
package pt.lsts.neptus.plugins.mvplanning.utils.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import pt.lsts.neptus.plugins.mvplanning.utils.Payload;

/**
 * @author tsmarques
 *
 */
@XmlRootElement (name="Profile")
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({Payload.class})
public class Profile {
    @XmlElement(name = "Altitude")
    private double altitude;
    @XmlElement(name = "Velocity")
    private double velocity;
    
    @XmlElementWrapper(name = "Payloads")
    @XmlElement(name = "Payload")
    private List<Payload> payloads;
    
    @XmlAttribute(name = "Type")
    private String profilesId;
    
    @XmlElement(name = "vehicle")
    @XmlElementWrapper(name = "vehicles")
    private List<String> vehicles; /* vehicles where this profile applies */
    
    public Profile() {
        velocity = -1;
        altitude = -1;
    }
    
    public Profile(String pType) {
        payloads = new ArrayList<Payload>();
        vehicles = new ArrayList<String>();
        profilesId = pType;
        
        velocity = -1;
        altitude = -1;
    }
    
    public double getProfileAltitude() {
        return altitude;
    }
    
    public List<String> getProfileVehicles() {
        return vehicles;
    }
    
    public void setProfileVehicles(List<String> vehicles) {
        this.vehicles = vehicles; 
    }
    
    public double getProfileVelocity() {
        return velocity;
    }
    
    public void setProfileVelocity(double velocity) {
        this.velocity = velocity;
    }
    
    public void setProfileAltitude(double altitude) {
        this.altitude = altitude;
    }
    
    public String getId() {
        return profilesId;
    }
       
    
    public void addPayload(Payload payload) {
        payloads.add(payload);
    }
    
    public List<Payload> getPayload() {
        return payloads;
    }
    
    public void addVehicle(String vehicleId) {
        if(!vehicles.contains(vehicleId))
            vehicles.add(vehicleId);
    }
    
    public void removeVehicle(String vehicleId) {
        vehicles.remove(vehicleId);
    }
}
