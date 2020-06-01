/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 26, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.util.LinkedHashMap;

import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author zp
 *
 */
public class VehicleParams {
    
    private static LinkedHashMap<String, Double> batteryCapacities = new LinkedHashMap<String, Double>();
    static {
        batteryCapacities.put("lauv-seacon-1", 700.0);
        batteryCapacities.put("lauv-seacon-2", 525.0);
        batteryCapacities.put("lauv-seacon-3", 525.0);
        batteryCapacities.put("lauv-xtreme-2", 525.0);
        batteryCapacities.put("lauv-xplore-1", 1400.0);
        batteryCapacities.put("lauv-xplore-2", 1400.0);        
        batteryCapacities.put("lauv-noptilus-1", 700.0);
        batteryCapacities.put("lauv-noptilus-2", 700.0);
        batteryCapacities.put("lauv-noptilus-3", 700.0);
    }
    
    private static LinkedHashMap<String, String> nicknames = new LinkedHashMap<String, String>();
    static {
        String[] vehicles = new String[] {"lauv-seacon-1", "lauv-seacon-2", "lauv-seacon-3", "lauv-xtreme-2", "lauv-xplore-1",
                "lauv-xplore-2", "lauv-noptilus-1", "lauv-noptilus-2", "lauv-noptilus-3" };
        
        for (String v : vehicles) {
            nicknames.put(VehiclesHolder.getVehicleById(v).getNickname(), v);
        }       
    }
    
    public static VehicleType getVehicleFromNickname(String nickname) {
        return VehiclesHolder.getVehicleById(nicknames.get(nickname));
    }
    
    private static LinkedHashMap<String, Double> moveConsumption = new LinkedHashMap<String, Double>();
    static {
        moveConsumption.put("lauv-seacon-1", 14.0);
        moveConsumption.put("lauv-seacon-2", 14.0);
        moveConsumption.put("lauv-seacon-3", 14.0);
        moveConsumption.put("lauv-xtreme-2", 14.0);
        moveConsumption.put("lauv-xplore-1", 14.0);
        moveConsumption.put("lauv-xplore-2", 14.0);
        moveConsumption.put("lauv-noptilus-1", 14.0);
        moveConsumption.put("lauv-noptilus-2", 14.0);
        moveConsumption.put("lauv-noptilus-3", 14.0);
    }


    private static LinkedHashMap<String, PayloadRequirement[]> payloads = new LinkedHashMap<String, PayloadRequirement[]>();
    static {
        payloads.put("lauv-seacon-1", new PayloadRequirement[] { PayloadRequirement.sidescan,
                PayloadRequirement.camera });
        payloads.put("lauv-seacon-2", new PayloadRequirement[] { PayloadRequirement.ctd });
        payloads.put("lauv-seacon-3", new PayloadRequirement[] { PayloadRequirement.ctd });
        payloads.put("lauv-xtreme-2", new PayloadRequirement[] { PayloadRequirement.sidescan });
        payloads.put("lauv-xplore-1", new PayloadRequirement[] { PayloadRequirement.ctd });
        payloads.put("lauv-xplore-2", new PayloadRequirement[] { PayloadRequirement.ctd,
                PayloadRequirement.rhodamine });
        payloads.put("lauv-noptilus-1", new PayloadRequirement[] { PayloadRequirement.sidescan,
                PayloadRequirement.multibeam });
        payloads.put("lauv-noptilus-2", new PayloadRequirement[] { PayloadRequirement.sidescan, 
                PayloadRequirement.edgetech });
        payloads.put("lauv-noptilus-3", new PayloadRequirement[] { 
                PayloadRequirement.sidescan, PayloadRequirement.multibeam, PayloadRequirement.camera });
    }
    
    public static PayloadRequirement[] payloadsFor(VehicleType vehicle) {
        if (payloads.containsKey(vehicle.getId()))
            return payloads.get(vehicle.getId());
        return new PayloadRequirement[0];
    }
    
    public static double maxBattery(VehicleType vehicle) {
        if (batteryCapacities.containsKey(vehicle.getId()))
            return batteryCapacities.get(vehicle.getId());
        return 500;
    }    
    
    public static double moveConsumption(VehicleType vehicle) {
        if (moveConsumption.containsKey(vehicle.getId()))
            return moveConsumption.get(vehicle.getId());
        return 14;
    }
}
