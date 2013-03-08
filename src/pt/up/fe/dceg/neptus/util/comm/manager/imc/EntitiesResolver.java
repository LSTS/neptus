/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/10/22
 * $Id:: EntitiesResolver.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.util.comm.manager.imc;

import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.imc.EntityList;

/**
 * @author zp
 *
 */
public class EntitiesResolver {

	protected static LinkedHashMap<String, LinkedHashMap<Integer, String>> entitiesMap = new LinkedHashMap<String, LinkedHashMap<Integer, String>>();
	public static final int DEFAULT_ENTITY = 255;
	
	
	/**
	 * Based on a EntityList message set all the messages for further queries 
	 * @param id Name of the name (String id)
	 * @param message EntityList message
	 */	
	public static void setEntities(String id, EntityList message) {
		LinkedHashMap<String, String> tlist = null;
		tlist = message.getList();
		
		LinkedHashMap<Integer, String> aliases = new LinkedHashMap<Integer, String>();
		for (String key : tlist.keySet())
			aliases.put(Integer.parseInt(tlist.get(key)), key);
		
		entitiesMap.put(id, aliases);
	}
	
	public static final LinkedHashMap<Integer, String> getEntities(Object systemId) {
		return entitiesMap.get(systemId.toString());
	}
	
	/**
     * 
     */
	public static void clearAliases(Object systemId) {
	    if (entitiesMap.containsKey(systemId.toString()))
	        entitiesMap.remove(systemId.toString());
    }
	
	public static String resolveName(String systemId, Integer entityId) {
		return entitiesMap.containsKey(systemId)? 
			entitiesMap.get(systemId).get(entityId) : null;
	}
	
	public static int resolveId(String systemId, String entityName) {
	    if(!entitiesMap.containsKey(systemId)) {
	        return -1;
	    }	    
	    for (Integer i: entitiesMap.get(systemId).keySet()) {
            if(entitiesMap.get(systemId).get(i).equals(entityName)) {
                return i;
            }
        }
	    return -1;
	}
}
