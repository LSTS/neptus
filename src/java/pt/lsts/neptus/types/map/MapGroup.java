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
 * Author: 
 * Mar 4, 2005
 */
package pt.lsts.neptus.types.map;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.MapChangeListener;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;


/**
 * @author zecarlos
 */
public class MapGroup implements MapChangeListener {

	public Hashtable<String, MapType> maps = new Hashtable<>();
	private static final Hashtable<String, MapGroup> instances = new Hashtable<>();
	public Vector<MapChangeListener> listeners = new Vector<>();
	private CoordinateSystem cs = new CoordinateSystem();
	private HomeReferenceElement HomeRef = new HomeReferenceElement(this, null);
	//static ReentrantLock lock = new ReentrantLock();
	protected MissionType mission = null;
	
	public Vector<AbstractElement> getObstacles() {
	    Vector<AbstractElement> ret = new Vector<>();
	    for (AbstractElement a : getAllObjects())
	        if (a.isObstacle())
	            ret.add(a);
	    return ret;
	}
	
	@SuppressWarnings("unchecked")
    public <T> Vector<T> getAllObjectsOfType(Class<T> type) {
		
		Vector<T> ret = new Vector<>();
		
		for (AbstractElement elem : getAllObjects()) {
			if (elem.getClass().equals(type)) {
				ret.add((T)elem);
			}
		}
		
		return ret;
	}
	
	public static MapGroup getNewInstance(CoordinateSystem cs) {
		
		NeptusLog.pub().debug("[MapGroup] getgetDeclaredConstructor().newInstance() called. Not stored in the hashtable");
		if (cs == null) {
			cs = new CoordinateSystem();
		}
		
		MapGroup mg = new MapGroup();
		mg.setCoordinateSystem(cs);
		
		return mg;
	}
	
    public MapType getPivotMap() {
        MapType m = null;
        for (MapType mt : getMaps())
            if (mt.getHref() != null && mt.getHref().length() > 0)
                m = mt;
        return m != null ? m : getMaps()[0];
    }

	/**
	 * This method will return (possibly creating a new one) the MapGroup related to the given mission
	 * @param mt A MissionType
	 * @return The mt's MapGroup
	 */
	public static MapGroup getMapGroupInstance(MissionType mt) {
		
		if (mt == null)
			return MapGroup.getNewInstance(new CoordinateSystem());
		NeptusLog.pub().debug("[MapGroup] getMapGroupInstance("+mt.getId()+") called.");
		
		if (!instances.containsKey(mt.getId())) {
			MapGroup mg = mt.generateMapGroup();
            mg.mission = mt;
			instances.put(mt.getId(), mg);
			NeptusLog.pub().debug("[MapGroup] new instance has been created. Stored map groups: "+instances.size());
			
		}
		else {
			NeptusLog.pub().debug("[MapGroup] instance found in the hashtable.");
		}
		
		
		
		return instances.get(mt.getId());
	}
	
	
	/**
	 * Verifies if currently exists an instance relative to the given mission id.
	 * @param id A mission id
	 * @return <b>true</b> if there currently exists an instance or <b>false</b> otherwise.
	 */
	public static boolean containsInstance(String id) {
		return instances.containsKey(id);
	}
	
	/**
	 * Resets the mission's map group instance 
	 */
	public static void resetMissionInstance(MissionType newMission) {
		
	    String id = newMission.getId();
	    MapGroup mg = instances.get(id);
	    
		//nothing to do...
		if (mg == null) {
		    return;
		}
		
		mg.mission = newMission;		
		mg.setCoordinateSystem(newMission.getHomeRef());
		
		Object [] maps =  newMission.getMapsList().values().toArray();
        
		Hashtable<String, MapType> oldMaps = mg.maps;
		mg.maps = new Hashtable<>();

        for (Object map : maps) {
            MapMission mm = (MapMission) map;
            MapType mt = mm.getMap();
            mt.setHref(mm.getHref());
            mt.setChanged(false);
            mt.setMission(newMission);
            mt.setMapGroup(mg);

            if (oldMaps.containsKey(mt.getId())) {
                for (MapChangeListener mcl : oldMaps.get(mt.getId()).changeListeners)
                    mt.addChangeListener(mcl);
                oldMaps.remove(mt.getId());
            }
            mg.addMap(mt);
        }
        
        mg.warnListeners(new MapChangeEvent(MapChangeEvent.MAP_RESET));
		//instances.put(id, getNewInstance(null));
		NeptusLog.pub().debug("[MapGroup] resetMissionInstance(\""+id+"\") called.");
	}
	
	/**
	 * This method alters the possibly existing MapGroup instance to be
	 * equal to the given MapGroup.
	 * @param id 
	 * @param mg
	 */
	public static MapGroup setMissionInstance(String id, MapGroup mg) {
		NeptusLog.pub().debug("[MapGroup] setMissionInstance(\""+id+"\",\""+mg+"\") called.");
		if (instances.containsKey(id)) {
			NeptusLog.pub().debug("[MapGroup] instance found in the hashtable, changing the existing instance.");
			MapGroup existingMapGroup = instances.get(id);
			
			if (existingMapGroup == mg)
				return mg;
			
			Object[] oldMaps = existingMapGroup.maps.values().toArray();
			//System.err.println("# of maps to remove: "+oldMaps.length);
            for (Object oldMap : oldMaps) {
                existingMapGroup.removeMap(((MapType) oldMap).getId());
            }
			
			Object[] newMaps = mg.maps.values().toArray();
			//System.err.println("# of maps to add: "+newMaps.length);
            for (Object newMap : newMaps) {
                existingMapGroup.addMap(((MapType) newMap));
            }
			
			existingMapGroup.setCoordinateSystem(mg.getCoordinateSystem());
			existingMapGroup.setHomeRef(mg.getHomeRef());
			
			if (mg.listeners.size() > 0) {
				NeptusLog.pub().debug("New MapGroup listeners size should be 0");
			}			
		}
		else {
			NeptusLog.pub().debug("[MapGroup] instance not found, added to the hashtable.");
			instances.put(id, mg);
		}
			
		return instances.get(id);
	}
	

	private MapGroup() {}


    /**
     * Adds a Map to this group
     *
     * @param map The map to be added to this MapGroup
     */
    public void addMap(MapType map) {
        if (map == null) {
            return;
        }

        if (maps.containsKey(map.getId())) {
            maps.get(map.getId()).removeChangeListener(this);
        }
        maps.put(map.getId(), map);
        map.addChangeListener(this);

        Object[] objNames = map.getObjectIds();
        for (Object objName : objNames) {
            AbstractElement mo = map.getObject((String) objName);
            mo.setMapGroup(this);
            mo.setParentMap(map);
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
            mce.setChangedObject(mo);
            mce.setSourceMap(map);
            warnListeners(mce);
        }
    }
	
	
	/**
	 * Removes the given map from this group
	 * @param mapID The map that is to be removed
	 */
	public void removeMap(String mapID) {
		
		MapType tmp = maps.get(mapID);
		if (tmp == null) {
			//System.err.println("tmp == null!");
			return;
		}
		maps.remove(mapID);
		Object[] objNames =tmp.getObjectIds();
        for (Object objName : objNames) {
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED);
            mce.setMapGroup(this);
            mce.setChangedObject(tmp.getObject((String) objName));
            warnListeners(mce);
        }
		tmp.removeChangeListener(this);
	}
	
	/**
	 * @deprecated
	 * @param map
	 */
	@Deprecated
    public void setMap(MapType map) {
		removeMap(map.getId());
	    addMap(map);
	    map.addChangeListener(this);
	}
	
	/**
	 * Given a MapID, returns the Map identified
	 * @param mapID A String identifier of the map to return
	 * @return The map identified by the given mapID
	 */
	public MapType getMapByID(String mapID) {
		return maps.get(mapID);
	}
	
	
	/**
	 * If any of its maps has changed, warn all the listeners of this map group
	 */
	@Override
    public void mapChanged(MapChangeEvent changeEvent) {
		//if (changeEvent.getSourceMap() != null)
		warnListeners(changeEvent);
	}
	
	/**
	 * Returns a map object given its string representation:
	 * A String representation of an object is [mapID]:[objID]
	 * @param objID A String like "mapID":"objID"
	 * @return The existent map object or null if the object doesn't exist.
	 */
	public AbstractElement getMapObject(String objID) {
		StringTokenizer st = new StringTokenizer(objID, ":");
		if (st.countTokens() < 2) {
			NeptusLog.pub().warn("Tried to get an object without its map id ("+objID+")");
			return null;
		}
			
		
		String mapStr = st.nextToken();
		String objStr = st.nextToken();
		
		MapType tmp = maps.get(mapStr);
		return tmp.getObject(objStr);
	}
	
	public AbstractElement findObject(String objID) {
	    
	    for (MapType map : maps.values()) {
	        if (map.getObject(objID) != null)
	            return map.getObject(objID);
	    }
	    
	    return null;
    }
	
	public void updateObjectIds() {
	    for (MapType m : maps.values())
	        m.updateObjectIds();
	}
	
	public AbstractElement[] getMapObjectsByID(String objID) {
		Vector<AbstractElement> vec = new Vector<>();
		
		for (MapType map : getMaps()) {
			AbstractElement mo = map.getObject(objID);
			if (mo != null)
				vec.add(mo);
		}		
		return vec.toArray(new AbstractElement[] {});
	}
	
	public MapType[] getMaps() {
		return maps.values().toArray(new MapType[] {});
	}
	
	/**
	 * Adds a MapChangeListener to the current list of listeners
	 * All these listeners will be warned whenever a object in any f the current maps
	 * is changed, removed or added.
	 * @param listener
	 */
	public void addChangeListener(MapChangeListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);	
	}
	
	/**
	 * Removes the given MapChangeListener from the current list of listeners
	 * @param listener The MapChangeListener that will be removed from the list of listeners
	 */
	public void removeChangeListener(MapChangeListener listener) {		
		listeners.remove(listener);
	}

	/**
	 * Warns all the current MapChangeListeners that a change in some object has ocurred
	 *
	 */
	public void warnListeners(MapChangeEvent changeEvent) {
        for (MapChangeListener tmp : listeners.toArray(new MapChangeListener[0])) {
            //vNeptusLog.pub().info("<###> "+tmp.getClass());
            tmp.mapChanged(changeEvent);
        }
	}
	
	public AbstractElement[] getObjectsFromOtherMaps(String mapID) {
		Vector<AbstractElement> objs = new Vector<>();
		for (Enumeration<?> e = maps.elements(); e.hasMoreElements();) {
			
			MapType curMap = (MapType) e.nextElement();
			if (curMap.getId().equals(mapID))
				continue;
			objs.addAll(curMap.getObjects());
		}
		
		objs.add(getHomeRef());
		AbstractElement[] mo = objs.toArray(new AbstractElement[] {});

		Arrays.sort(mo);		
		
		return mo;
		
	}

	public AbstractElement[] getObjectsFromMap(String mapID) {
		Vector<AbstractElement> objs = new Vector<>();
		for (Enumeration<?> e = maps.elements(); e.hasMoreElements();) {			
			MapType curMap = (MapType) e.nextElement();
			if (!curMap.getId().equals(mapID))
				continue;
			objs.addAll(curMap.getObjects());
		}
		
		objs.add(getHomeRef());
		AbstractElement[] mo = objs.toArray(new AbstractElement[] {});

		Arrays.sort(mo);		
		
		return mo;
		
	}

	
	
	/**
	 * This method fetches the objects from all maps and returns them as an array
	 * It's very useful for displaying the objects from various maps
	 * @return An array of MapObjects with all the objects contained in all maps
	 */
	public AbstractElement[] getAllObjects() {
		Vector<AbstractElement> objs = new Vector<>();
		for (Enumeration<?> e = maps.elements(); e.hasMoreElements();) {
			MapType curMap = (MapType) e.nextElement();
			objs.addAll(curMap.getObjects());
		}
		
		objs.add(getHomeRef());
		AbstractElement[] mo = objs.toArray(new AbstractElement[] {});

		Arrays.sort(mo);		
		
		return mo;
	}
	
	public int numObjects() {
	    int sum = 1; //Counting with the home referential object...
	    for (Enumeration<?> e = maps.elements(); e.hasMoreElements();) {
	        MapType curMap = (MapType) e.nextElement();
	        sum += curMap.numObjects();
	    }
	    return sum;
	}
	
	
    public CoordinateSystem getCoordinateSystem() {
        return cs;
    }
    
    
    public void setCoordinateSystem(CoordinateSystem cs) {
        this.cs = cs;
		getHomeRef().setCoordinateSystem(cs);
		MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
		mce.setSourceMap(null);
		mce.setChangedObject(getHomeRef());
		warnListeners(mce);
    }
	
    /**
	 * @return Returns the homeRef.
	 */
	public HomeReferenceElement getHomeRef() {
		if (HomeRef == null)
			HomeRef = new HomeReferenceElement(this, null);
		
		return HomeRef;
	}
	
	/**
	 * @param homeRef The homeRef to set.
	 */
	public void setHomeRef(HomeReferenceElement homeRef) {
		getHomeRef().setCoordinateSystem(homeRef.getCoordinateSystem());
		MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
		mce.setChangedObject(homeRef);
		warnListeners(mce);
	}
	
	/**
	 * Creates a MapShape IMC message given an AbstractElement (may return null)
	 * @param elem The AbstractElement that is to be converted to a MapShape message
	 * @return The IMC representation of a given map object
	 */
	private IMCMessage createShape(AbstractElement elem) {
		Vector<LocationType> pts = elem.getShapePoints();
		
		if (pts == null)
			return null;
		
		IMCMessage nextPoint = null;
		
		for (int i = pts.size() - 1; i >= 0; i--) {
			IMCMessage ptMsg = IMCDefinition.getInstance().create("MapPoint");
			double[] lld = pts.get(i).getAbsoluteLatLonDepth();
			
			ptMsg.setValue("lat", Math.toRadians(lld[0]));
			ptMsg.setValue("lon", Math.toRadians(lld[1]));
			ptMsg.setValue("alt", -1 * lld[2]);

            ptMsg.setValue("nextpt", nextPoint); // Can be null
			nextPoint = ptMsg;
		}
		return IMCDefinition.getInstance().create("MapShape", "shapetype", 0, "points", nextPoint);
	}
	
	public IMCMessage getIMCSerialization(String mapId) {
		IMCMessage msg = IMCDefinition.getInstance().create("Map", "id", mapId);
		
		AbstractElement[] elems = getAllObjects(); 
		
		IMCMessage nextShape = null;
		
		for (int i = elems.length-1; i >= 0; i--) {			
			IMCMessage shpMsg = createShape(elems[i]);
			if (shpMsg != null) {
				//NeptusLog.pub().info("<###>Shape for element "+elems[i].getType()+"-"+elems[i].getId()+" is not null");
                shpMsg.setValue("nextshape", nextShape); // Can be null
				
				nextShape = shpMsg;
			}
			//NeptusLog.pub().info("<###>Shape for element "+elems[i].getType()+"-"+elems[i].getId()+" is null");
		}
        msg.setValue("features", nextShape); // Can be null
		
		try {
//			NeptusMessageLogger.getLogger().logMessage("neptus", "pda", msg);
			LsfMessageLogger.log(msg);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		//msg.dump(System.out);
		
		return msg;
	}

    /**
     * @return the mission
     */
    public MissionType getMission() {
        return mission;
    }
}
