/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
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
public class PlanCompability {

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
    
    @SuppressWarnings("unused")
    private static Collection<String> payloadsMissing(VehicleType vehicle, PlanType plan) {
        
        HashSet<String> needed = new HashSet<>();
        HashSet<String> missing = new HashSet<>();
        
        Maneuver[] mans = plan.getGraph().getAllManeuvers();
        
        for (Maneuver m : mans) {
            ManeuverPayloadConfig cfg = new ManeuverPayloadConfig(vehicle.getId(), m, null);
            for (Property p : cfg.getProperties())
                needed.add(p.getCategory());
        }
        
        Collection<String> existing = availablePayloads(vehicle);
        
        for (String p : needed) {
            if (!existing.contains(p))
                missing.add(p);
        }

        return missing;
    }
    
    private static Collection<String> availablePayloads(VehicleType vehicle) {
        HashSet<String> existing = new HashSet<>();
        for (SystemProperty p : ConfigurationManager.getInstance().getClonedProperties(vehicle.getId(), Visibility.USER,
                Scope.MANEUVER)) {
            existing.add(p.getCategory());
        }
        return existing;
    }
}
