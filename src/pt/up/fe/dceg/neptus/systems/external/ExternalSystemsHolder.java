/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 24 de Jun de 2012
 * $Id:: ExternalSystemsHolder.java 9615 2012-12-30 23:08:28Z pdias             $:
 */
package pt.up.fe.dceg.neptus.systems.external;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

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
    
    public static boolean registerSystem(ExternalSystem system) {
        ExternalSystem resLook = lookupTable.get(system.getId());
        if (resLook != null)
            return true;
        
        lookupTable.put(system.getId(), system);
        return true;
    }

    public static ExternalSystem lookupSystem(String id) {
        return lookupTable.get(id);
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
        for (ExternalSystem is : lookupTable.values()) {
            //System.out.println("... lookupSystemByName()"+is.getName());
            if (allTypes || type == SystemTypeEnum.ALL || type == is.getType()) {
                if (onlyActiveSystems && !is.isActive()) {
                    continue;
                }
                list.add(is);
            }
        }
        //System.out.println("... lookupSystemByName()"+list.size());
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
        for (ExternalSystem is : lookupTable.values()) {
            //System.out.println("... lookupSystemByName()"+is.getName());
            if (allTypes || type == ExternalSystem.ExternalTypeEnum.ALL || type == is.getTypeExternal()) {
                if (onlyActiveSystems && !is.isActive()) {
                    continue;
                }
                list.add(is);
            }
        }
        //System.out.println("... lookupSystemByName()"+list.size());
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
