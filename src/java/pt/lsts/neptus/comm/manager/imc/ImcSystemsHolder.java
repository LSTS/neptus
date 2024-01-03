/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 2009/03/14
 */
package pt.lsts.neptus.comm.manager.imc;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

/**
 * @author pdias
 *
 */
public class ImcSystemsHolder {
	
	private static Map<ImcId16, ImcSystem> lookupTable = (Map<ImcId16, ImcSystem>) Collections.synchronizedMap(new Hashtable<ImcId16, ImcSystem>());
	private static Map<String, ImcSystem> namesTable = (Map<String, ImcSystem>) Collections.synchronizedMap(new Hashtable<String, ImcSystem>());
    
	public static boolean registerSystem(ImcSystem system) {
	    synchronized (lookupTable) {
	        ImcSystem resLook = lookupTable.get(system.getId());
	        if (resLook != null){
	            namesTable.put(system.getName(), system);
	            return true; //false;
	        }
	        
	        lookupTable.put(system.getId(), system);
	        namesTable.put(system.getName(), system);
        }
		
		return true;
	}
	
	public static ImcSystem lookupSystem(ImcId16 id) {
        synchronized (lookupTable) {
            return id == null ? null : lookupTable.get(id);
        }
	}
	
	public static ImcSystem lookupSystem(int imcid) {
	    return lookupSystem(new ImcId16(imcid));
	}
	
	public static ImcSystem getSystemWithName(String name) {
	    synchronized (lookupTable) {
	        return name == null ? null : namesTable.get(name);
	    }
	}	
	
	/**
	 * @param name
	 * @return
	 */
	public static ImcSystem lookupSystemByName(String name) {
		if (name == null)
			return null; // new ImcSystem[0];
		else if ("".equalsIgnoreCase(name))
			return null; // new ImcSystem[0];
		LinkedList<ImcSystem> list = new LinkedList<ImcSystem>();
		synchronized (lookupTable) {
		    for (ImcSystem is : lookupTable.values()) {
		        //NeptusLog.pub().info("<###>... lookupSystemByName()"+is.getName());
		        if (name.equalsIgnoreCase(is.getName())) {
		            list.add(is);
		            break;
		        }
		    }
		}
		return list.isEmpty() ? null : list.getFirst();
	}
	
	/**
	 * @param type
	 * @return
	 */
	public static final ImcSystem[] lookupSystemByType (VehicleType.SystemTypeEnum type, boolean onlyActiveSystems) {
		boolean allTypes = false;
		if (type == null)
			allTypes = true;
		LinkedList<ImcSystem> list = new LinkedList<ImcSystem>();
        synchronized (lookupTable) {
            for (ImcSystem is : lookupTable.values()) {
                if (allTypes || type == SystemTypeEnum.ALL || type == is.getType()) {
                    if (onlyActiveSystems && !is.isActive())
                        continue;
                    list.add(is);
                }
            }
        }
		return list.toArray(new ImcSystem[list.size()]);
	}
	
	public static final ImcSystem[] lookupAllSystems () {
		return lookupSystemByType(null, false);
	}

	public static final ImcSystem[] lookupAllActiveSystems () {
		return lookupSystemByType(null, true);
	}

	/**
	 * @param type
	 * @return
	 */
	public static final ImcSystem[] lookupSystemByType (VehicleType.SystemTypeEnum type) {
		return lookupSystemByType(type, false);
	}

	public static final ImcSystem[] lookupActiveSystemByType (VehicleType.SystemTypeEnum type) {
		return lookupSystemByType(type, true);
	}

	/**
	 * @return
	 */
	public static final ImcSystem[] lookupSystemCCUs () {
		return lookupSystemByType(VehicleType.SystemTypeEnum.CCU);
	}
	
	/**
	 * @return
	 */
	public static final ImcSystem[] lookupSystemVehicles () {
		return lookupSystemByType(VehicleType.SystemTypeEnum.VEHICLE);
	}

	public static final ImcSystem[] lookupActiveSystemVehicles () {
		return lookupSystemByType(VehicleType.SystemTypeEnum.VEHICLE, true);
	}
	
	// Lookup systems by services
	public static final ImcSystem[] lookupSystemByService (String service, 
			VehicleType.SystemTypeEnum type, boolean onlyActiveSystems) {
		boolean allTypes = false;
		if (type == null)
			allTypes = true;
		LinkedList<ImcSystem> list = new LinkedList<ImcSystem>();
        synchronized (lookupTable) {
            for (ImcSystem is : lookupTable.values()) {
                if (allTypes || type == SystemTypeEnum.ALL || type == is.getType()) {
                    if (onlyActiveSystems && !is.isActive())
                        continue;
                    if (is.isServiceProvided(service))
                        list.add(is);
                }
            }
        }
		return list.toArray(new ImcSystem[list.size()]);
	}

	public static String translateImcIdToSystemName(long id) {
	    return translateImcIdToSystemName(new ImcId16(id));
	}

	/**
	 * @param id
	 * @return The name of the system or the IMC Id in pretty format if system not yet known.
	 */
	public static String translateImcIdToSystemName(ImcId16 id) {
	    ImcSystem sys = lookupSystem(id);
	    String ret = id.toPrettyString();
	    if (sys != null)
	        ret = sys.getName();
	    return ret;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ImcSystem imc1 = new ImcSystem(new ImcId16("22:10"));
		ImcSystem imc2 = new ImcSystem(new ImcId16("22:11"));
		
		ImcSystemsHolder.registerSystem(imc1);
		ImcSystemsHolder.registerSystem(imc2);
		
		NeptusLog.pub().info("<###> "+ImcSystemsHolder.lookupSystem(new ImcId16("22:10")));
	}
}
