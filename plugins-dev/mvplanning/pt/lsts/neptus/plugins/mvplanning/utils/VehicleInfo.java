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
    private Map<String, List<Payload>> vehicleCapabilities;

    /* Properties variables */
    private final Scope scope = Scope.GLOBAL;
    private final Visibility vis = Visibility.USER;

    public VehicleInfo(String id, Map<String, Profile> allProfiles) {
        vId = id;
        setVehicleCapabilities(vId, allProfiles);
    }

    public String vehicleId() {
        return vId;
    }

    /* From the available payload (in mvplanning/etc/ )
     * retrieve all profiles that apply to this vehicle */
    public void setVehicleCapabilities(String vId, Map<String, Profile> allProfiles) {
//        ArrayList<SystemProperty> prList = ConfigurationManager.getInstance().getProperties(vId, vis, scope);
//        vehicleCapabilities = new HashMap<String, List<Payload>>();
//
//        for(SystemProperty pr : prList) {
//            String cap = pr.getCategory();
//            
//            /* if 'cap' is considered payload/capability */
//            if(allProfiles.containsKey(cap))
//                vehicleCapabilities.put(cap, allProfiles.get(cap).getVehicleProfiles(vId));
//        }
    }

    public List<Payload> getVehicleProfiles(String payloadType) {
        return vehicleCapabilities.get(payloadType);
    }

    public boolean hasCapabilities(LinkedList<String> neededCapabilities) {
        return vehicleCapabilities.keySet().containsAll(neededCapabilities);
    }
    
    /* for debugging */
    public void printCapabilities() {
//        System.out.println("[" + vId + "]");
//        for(String cap : vehicleCapabilities.keySet()) {
//            System.out.println("[" + cap + " profiles]");
//            
//            for(Payload profile : vehicleCapabilities.get(cap)) {
//                System.out.println("[" + profile.getProfileId() + "]");
//                profile.printPayloadParameters();
//                System.out.println();
//            }
//            System.out.println("\n");
//        }
    }
}