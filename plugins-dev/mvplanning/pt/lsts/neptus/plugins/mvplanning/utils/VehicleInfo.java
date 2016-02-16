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
 * 7 Jan 2016
 */
package pt.lsts.neptus.plugins.mvplanning.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.plugins.mvplanning.utils.jaxb.Profile;

/**
 * @author tsmarques
 *
 */

public class VehicleInfo {
    private String vId;
    //private ArrayList<String> capabilities;
    private Map<String, Profile> vehicleProfiles;

    /* Properties variables */
    private final Scope scope = Scope.GLOBAL;
    private final Visibility vis = Visibility.USER;

    public VehicleInfo(String id, Map<String, Profile> existingProfiles) {
        vId = id;
        fetchVehicleProfiles(existingProfiles);
    }

    public String vehicleId() {
        return vId;
    }


    public void fetchVehicleProfiles(Map<String, Profile> existingProfiles) {
        ArrayList<SystemProperty> payloadList = ConfigurationManager.getInstance().getProperties(vId, vis, scope);
        vehicleProfiles = new HashMap<String, Profile>();

        for(Entry<String, Profile> entry : existingProfiles.entrySet()) {
            Profile profile = entry.getValue();

            if(profile.getProfileVehicles().contains(vId) && canUseProfile(profile, payloadList))
                vehicleProfiles.put(entry.getKey(), profile);
        }
    }

    /* Checks if a vehicle can use a given profile, i.e,
     * if it has, available, all the payload that the profile
     * 'needs' */
    private boolean canUseProfile(Profile profile, List<SystemProperty> vehiclePayload) {
        boolean vehicleHasPayload = false;
        for(Payload pld : profile.getPayload()) {
            for(SystemProperty pr : vehiclePayload) {
                String payloadType = pr.getCategory(); /* sidescan, etc */

                if(payloadType.equals(pld.getPayloadType())) {
                    vehicleHasPayload = true;
                    break;
                }
            }
            if(!vehicleHasPayload)
                return false;
        }
        return true;
    }


    public boolean hasCapabilities(LinkedList<String> neededCapabilities) {
        return vehicleProfiles.keySet().containsAll(neededCapabilities);
    }

    /* for debugging */
    public void printProfiles() {
        for(Entry<String, Profile> entry : vehicleProfiles.entrySet()) {
            System.out.println("  [Profile: " + entry.getKey() + "]");
            for(Payload pld : entry.getValue().getPayload())
                System.out.println("   " + pld.getPayloadType());
        }
    }
}