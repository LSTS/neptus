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
 * 2009/03/14
 * $Id:: ImcSystemsHolder.java 10013 2013-02-21 14:27:23Z pdias           $:
 */
package pt.up.fe.dceg.neptus.util.comm.manager.imc;

import java.util.Hashtable;
import java.util.LinkedList;

import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;

/**
 * @author pdias
 *
 */
public class ImcSystemsHolder {
	
	private static Hashtable<ImcId16, ImcSystem> lookupTable = new Hashtable<ImcId16, ImcSystem>();
	private static Hashtable<String, ImcSystem> namesTable = new Hashtable<String, ImcSystem>();
    
	public static boolean registerSystem(ImcSystem system) {
		ImcSystem resLook = lookupTable.get(system.getId());
		if (resLook != null)
			return true; //false;
		
		lookupTable.put(system.getId(), system);
		namesTable.put(system.getName(), system);
		
		//System.out.println("$$$$$$$$$$$$$$ register " + system.getId());
		return true;
	}
	
	public static ImcSystem lookupSystem(ImcId16 id) {
		return id == null ? null : lookupTable.get(id);
	}
	
	public static ImcSystem lookupSystem(int imcid) {
	    return lookupSystem(new ImcId16(imcid));
	}
	
	public static ImcSystem getSystemWithName(String name) {
	    return name == null ? null : namesTable.get(name);
	}	
	
	/**
	 * @param name
	 * @return
	 */
	public static ImcSystem lookupSystemByName(String name) {
		//System.out.println("... lookupSystemByName()"+name);
		if (name == null)
			return null; // new ImcSystem[0];
		else if ("".equalsIgnoreCase(name))
			return null; // new ImcSystem[0];
		LinkedList<ImcSystem> list = new LinkedList<ImcSystem>();
		for (ImcSystem is : lookupTable.values()) {
			//System.out.println("... lookupSystemByName()"+is.getName());
			if (name.equalsIgnoreCase(is.getName())) {
				list.add(is);
				break;
			}
		}
		//System.out.println("... lookupSystemByName()"+list.size());
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
		for (ImcSystem is : lookupTable.values()) {
			//System.out.println("... lookupSystemByName()"+is.getName());
			if (allTypes || type == SystemTypeEnum.ALL || type == is.getType()) {
				if (onlyActiveSystems && !is.isActive()) {
					continue;
				}
				list.add(is);
			}
		}
		//System.out.println("... lookupSystemByName()"+list.size());
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

	public static final ImcSystem[] lookupActiveSystemCCUs () {
		return lookupSystemByType(VehicleType.SystemTypeEnum.CCU, true);
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
		for (ImcSystem is : lookupTable.values()) {
			//System.out.println("... lookupSystemByService()"+is.getName());
			if (allTypes || type == SystemTypeEnum.ALL || type == is.getType()) {
				if (onlyActiveSystems && !is.isActive()) {
					continue;
				}
				if (is.isServiceProvided(service))
					list.add(is);
			}
		}
		//System.out.println("... lookupSystemByService()"+list.size());
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
	    String ret = id.toPrettyString();
	    ImcSystem sys = lookupSystem(id);
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
		
		System.out.println(ImcSystemsHolder.lookupSystem(new ImcId16("22:10")));
	}
}
