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
 * Author: José Pinto
 * 2009/10/22
 */
package pt.lsts.neptus.comm.manager.imc;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import pt.lsts.imc.EntityList;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.PropertiesLoader;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
public class EntitiesResolver {
    public static final String ENTITIES_RESOLVER_PROPERTIES_FILE = ".cache/db/entities-resolver.properties";

    private static PropertiesLoader properties = null;
    static {
        String propertiesFile = ConfigFetch.resolvePathBasedOnConfigFile(ENTITIES_RESOLVER_PROPERTIES_FILE);
        if (!new File(propertiesFile).exists()) {
            String testFile = ConfigFetch.resolvePathBasedOnConfigFile("../" + ENTITIES_RESOLVER_PROPERTIES_FILE);
            if (new File(testFile).exists())
                propertiesFile = testFile;
        }
        new File(propertiesFile).getParentFile().mkdirs();
        properties = new PropertiesLoader(propertiesFile, PropertiesLoader.PROPERTIES);

        while (properties.keys().hasMoreElements()) {
            String key = properties.keys().nextElement().toString();
            String value = properties.getProperty(key);
            setEntities(key, value, false);
        }
    }

	protected static LinkedHashMap<String, BiMap<Integer, String>> entitiesMap = new LinkedHashMap<String, BiMap<Integer, String>>();
	public static final int DEFAULT_ENTITY = 255;

    public static void saveProperties() {
        try {
            properties.store("EntitiesResolver properties");
        }
        catch (IOException e) {
            NeptusLog.pub().error("saveProperties", e);
        }
    }

	/**
	 * Based on a EntityList message set all the messages for further queries 
	 * @param id Name of the name (String id)
	 * @param message EntityList message
	 */	
	public static void setEntities(String id, EntityList message) {
        LinkedHashMap<String, String> tlist = null;
        tlist = message.getList();
        String listAsStr = IMCMessage.encodeTupleList(tlist);
        setEntities(id, listAsStr, true);
    }

    public static void setEntities(String id, String listAsStr, boolean save) {
        LinkedHashMap<String, String> tlist = null;
        tlist = IMCMessage.decodeTupleList(listAsStr);

        BiMap<Integer, String> aliases = HashBiMap.create();
        for (String key : tlist.keySet()) {
            aliases.put(Integer.parseInt(tlist.get(key)), key);
        }

        if (save) {
            String old = properties.getProperty(id);
            if (old == null || !old.equals(listAsStr)) {
                properties.setProperty(id, listAsStr);
                saveProperties();
            }
        }

        entitiesMap.put(id, aliases);
    }
	public static final Map<Integer, String> getEntities(Object systemId) {
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
	    BiMap<String, Integer> inverted = entitiesMap.get(systemId).inverse();
	    //System.out.println(inverted);
	    if (!inverted.containsKey(entityName))
	        return -1;
	    return inverted.get(entityName);	    
	}
}
