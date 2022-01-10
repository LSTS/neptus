/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 2, 2014
 */
package pt.lsts.neptus.types.mission.plan;

import java.util.Collection;
import java.util.HashSet;

import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.ManeuverPayloadConfig;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * This class provides some utility methods for testing vehicle-compatibility for given plans. <br/>
 * Example:
 * <pre>
 * {@code
 * if (!PlanConpability.isVehicleCompatible(vehicle, plan))
 *  System.err.println("The vehicle "+vehicle.getId()+" is not compatible with "+plan.getId);
 * }
 * </pre> 
 * @author zp
 */
public class PlanCompatibility {

    /**
     * Checks vehicle compatibility with a plan
     * @param vehicle The vehicle to be checked for compatibility
     * @param plan The plan to test for compatibility
     * @return <code>true</code> if the vehicle is compatible with given plan or <code>false</code> if
     * the vehicle and plan are not compatible.
     */
    public static boolean isVehicleCompatible(VehicleType vehicle, PlanType plan) {
        try {
            testCompatibility(vehicle, plan);
            return true;
        }
        catch (Exception e) {
            return false;
        }        
    }
    
     /**
     * Checks vehicle compatibility with a plan
     * @param vehicle The vehicle to be checked for compatibility
     * @param plan The plan to test for compatibility
     * @throws PayloadNotSupportedException If the plan contains a payload configuration not supported by the vehicle
     * @throws ManeuverNotSupportedException If the plan contains a maneuver not supported by the vehicle
     */
    public static void testCompatibility(VehicleType vehicle, PlanType plan) throws PayloadNotSupportedException, ManeuverNotSupportedException {
        Collection<String> mansMissing = maneuversMissing(vehicle, plan);
        if (!mansMissing.isEmpty())
            throw new ManeuverNotSupportedException(mansMissing.iterator().next());
        
//        Collection<String> payloadsMissing = payloadsMissing(vehicle, plan);
//        if (!payloadsMissing.isEmpty())
//            throw new PayloadNotSupportedException(payloadsMissing.iterator().next());
    }
    
    private static Collection<String> maneuversMissing(VehicleType v, PlanType plan) {
        Maneuver[] mans = plan.getGraph().getAllManeuvers();
        
        HashSet<String> manNames = new HashSet<>();
        for (Maneuver m : mans)
            manNames.add(m.getType());
        
        HashSet<String> result = new HashSet<>();
        
        for (String man : manNames) {
            if (!v.getFeasibleManeuvers().containsKey(man))
                result.add(man);
        }
        return result;
     }
    
    /**
     * Retrieve a list of payloads required by a vehicle that do not exist on the plan
     * @param vehicle The vehicle to check for payloads
     * @param plan The plan that lists required (active) payloads
     * @return Names of the payloads like "Sidescan", "Multibeam", ...
     */
    public static Collection<String> payloadsMissing(VehicleType vehicle, PlanType plan) {
        HashSet<String> missing = new HashSet<>();
        Collection<String> needed = payloadsRequired(plan);
        Collection<String> existing = availablePayloads(vehicle);
        
        for (String p : needed) {
            if (!existing.contains(p))
                missing.add(p);
        }

        return missing;
    }
    
    /**
     * Calculates the payloads required by a plan
     * @param plan Plan where to parse required payloads
     * @return Names of the payloads like "Sidescan", "Multibeam", ...
     */
    public static Collection<String> payloadsRequired(PlanType plan) {
        Maneuver[] mans = plan.getGraph().getAllManeuvers();
        String vehicle = plan.getVehicle();
        HashSet<String> needed = new HashSet<>();
        
        for (Maneuver m : mans) {
            ManeuverPayloadConfig cfg = new ManeuverPayloadConfig(vehicle, m, null);
            for (Property p : cfg.getProperties()) {
                if ("Active".equals(p.getName()) && Boolean.TRUE.equals(p.getValue())) {
                    needed.add(p.getCategory());
                }
            }
        }
        
        return needed;
    }
    
    /**
     * Calculate the payloads provided by a certain vehicle
     * @param vehicle The vehicle where to look for payloads
     * @return Names of the payloads like "Sidescan", "Multibeam", ...
     */
    public static Collection<String> availablePayloads(VehicleType vehicle) {
        HashSet<String> existing = new HashSet<>();
        for (SystemProperty p : ConfigurationManager.getInstance().getClonedProperties(vehicle.getId(), Visibility.USER,
                Scope.MANEUVER)) {
            existing.add(p.getCategory());
        }
        return existing;
    }
    
    public static void main(String[] args) {
        MissionType mt = new MissionType("/home/zp/workspace/neptus/missions/APDL/apdl-necsave.nmisz");
        PlanType pt = mt.getIndividualPlansList().get("go_in");
        System.out.println(payloadsRequired(pt));
    }
}
