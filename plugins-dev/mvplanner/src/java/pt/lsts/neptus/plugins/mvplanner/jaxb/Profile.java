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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: tsm
 * 27 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * @author tsmarques
 * @date 1/29/17
 */

import pt.lsts.neptus.mp.ManeuverLocation;

/**
 * @author tsmarques
 *
 */
@XmlRootElement (name="Profile")
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({Payload.class})
public class Profile {
    @XmlElement(name = "Z")
    private double z;

    @XmlElement(name = "Z_Units")
    private String zUnits;

    @XmlElement(name = "Speed")
    private double speed;

    @XmlElement(name = "Speed_Units")
    private String speedUnits;

    @XmlElementWrapper(name = "Payloads")
    @XmlElement(name = "Payload")
    private List<Payload> payloads;

    @XmlAttribute(name = "Type")
    private String profilesId;

    @XmlElement(name = "vehicle")
    @XmlElementWrapper(name = "vehicles")
    private List<String> vehicles; /* vehicles where this profile applies */

    public Profile() {
        profilesId = "";
        speed = -1;
        speedUnits = "";
        z = -1;
        zUnits = "";
        payloads = new ArrayList<>();
        vehicles = new ArrayList<>();
    }

    public Profile(String pType) {
        payloads = new ArrayList<>();
        vehicles = new ArrayList<>();
        profilesId = pType;

        speed = -1;
        z = -1;
    }

    public double getProfileZ() {
        return z;
    }

    public List<String> getProfileVehicles() {
        return vehicles;
    }

    public void setProfileVehicles(List<String> vehicles) {
        this.vehicles = vehicles;
    }

    public double getProfileSpeed() {
        return speed;
    }

    public String getSpeedUnits() {
        return speedUnits;
    }

    public void setProfileSpeed(double speed) {
        this.speed = speed;
    }

    public void setProfileZ(double z) {
        this.z = z;
    }

    public void setZUnits(ManeuverLocation.Z_UNITS units) {
        zUnits = units.name();
    }

    public void setSpeedUnits(String units) {
        speedUnits = units;
    }

    public String getZUnits() {
        return zUnits;
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