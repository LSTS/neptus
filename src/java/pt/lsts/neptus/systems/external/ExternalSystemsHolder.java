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
 * Author: Paulo Dias
 * 24 de Jun de 2012
 */
package pt.lsts.neptus.systems.external;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

/**
 * @author pdias
 *
 */
public class ExternalSystemsHolder {

    private static Map<String, ExternalSystem> lookupTable = Collections.synchronizedMap(new Hashtable<String, ExternalSystem>());
    
    private static Timer timer = new Timer(ExternalSystemsHolder.class.getSimpleName() + " Timer", true);
    private static TimerTask ttask = new TimerTask() {
        @Override
        public void run() {
            synchronized (lookupTable) {
                if (lookupTable.size() == 0)
                    return;
                
                ImcSystem[] systems = ImcSystemsHolder.lookupAllSystems();
                for (String extSystemName : lookupTable.keySet().toArray(new String[0])) {
                    for (ImcSystem imcSystem : systems) {
                        if (extSystemName.equalsIgnoreCase(imcSystem.getName())) {
                            lookupTable.remove(extSystemName);
                            break;
                        }
                    }
                }
            }
        }
    };
    
    static {
        timer.scheduleAtFixedRate(ttask, 2000, 5000);
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                ttask.cancel();
                timer.cancel();
            }
        }));
        
//        ExternalSystem ext1 = new ExternalSystem("Dorado");
//        LocationType portugal = new LocationType();
//        portugal.setLatitude(38.711233);
//        portugal.setLongitude(-9.18457);
//        ext1.setLocation(portugal);
//        registerSystem(ext1);
    }
    
    private ExternalSystemsHolder() {
    }
    
    public static ExternalSystem registerSystem(ExternalSystem system) {
        synchronized (lookupTable) {
            ExternalSystem resLook = lookupTable.get(system.getId());
            if (resLook != null)
                return resLook;
            
            lookupTable.put(system.getId(), system);
        }
        return system;
    }

    public static ExternalSystem purgeSystem(String id) {
        ExternalSystem resLook =null;
        synchronized (lookupTable) {
            resLook = lookupTable.remove(id);
        }
        if (resLook != null)
            return resLook;
        
        return null;
    }

    public static ExternalSystem lookupSystem(String id) {
        synchronized (lookupTable) {
            ExternalSystem ret = lookupTable.get(id);
            if (ret == null) {
                for (ExternalSystem is : lookupTable.values()) {
                    //NeptusLog.pub().info("<###>... lookupSystemByName()"+is.getName());
                    if (id.equalsIgnoreCase(is.getName())) {
                        ret = is;;
                        break;
                    }
                }
            }
            return ret;
        }
    }
    
    /**
     * @param type
     * @return
     */
    public static final ExternalSystem[] lookupSystemByType (VehicleType.SystemTypeEnum type, boolean onlyActiveSystems) {
        boolean allTypes = false;
        if (type == null)
            allTypes = true;
        LinkedList<ExternalSystem> list = new LinkedList<ExternalSystem>();
        synchronized (lookupTable) {
            for (ExternalSystem is : lookupTable.values()) {
                if (allTypes || type == SystemTypeEnum.ALL || type == is.getType()) {
                    if (onlyActiveSystems && !is.isActive())
                        continue;
                    list.add(is);
                }
            }
        }
        return list.toArray(new ExternalSystem[list.size()]);
    }

    /**
     * @param type
     * @return
     */
    public static final ExternalSystem[] lookupSystemByExternalType (ExternalSystem.ExternalTypeEnum type, boolean onlyActiveSystems) {
        boolean allTypes = false;
        if (type == null)
            allTypes = true;
        LinkedList<ExternalSystem> list = new LinkedList<ExternalSystem>();
        synchronized (lookupTable) {
            for (ExternalSystem is : lookupTable.values()) {
                if (allTypes || type == ExternalSystem.ExternalTypeEnum.ALL || type == is.getTypeExternal()) {
                    if (onlyActiveSystems && !is.isActive())
                        continue;
                    list.add(is);
                }
            }
        }
        return list.toArray(new ExternalSystem[list.size()]);
    }

    public static final ExternalSystem[] lookupAllSystems () {
        return lookupSystemByType(null, false);
    }

    public static final ExternalSystem[] lookupAllActiveSystems () {
        return lookupSystemByType(null, true);
    }

    /**
     * @param type
     * @return
     */
    public static final ExternalSystem[] lookupSystemByType (VehicleType.SystemTypeEnum type) {
        return lookupSystemByType(type, false);
    }

    public static final ExternalSystem[] lookupActiveSystemByType (VehicleType.SystemTypeEnum type) {
        return lookupSystemByType(type, true);
    }

    /**
     * @return
     */
    public static final ExternalSystem[] lookupSystemCCUs () {
        return lookupSystemByType(VehicleType.SystemTypeEnum.CCU);
    }

    public static final ExternalSystem[] lookupActiveSystemCCUs () {
        return lookupSystemByType(VehicleType.SystemTypeEnum.CCU, true);
    }

    /**
     * @return
     */
    public static final ExternalSystem[] lookupSystemVehicles () {
        return lookupSystemByType(VehicleType.SystemTypeEnum.VEHICLE);
    }

    public static final ExternalSystem[] lookupActiveSystemVehicles () {
        return lookupSystemByType(VehicleType.SystemTypeEnum.VEHICLE, true);
    }
}
