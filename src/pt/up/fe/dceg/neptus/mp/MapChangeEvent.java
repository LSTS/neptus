/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * Mar 4, 2005
 * $Id:: MapChangeEvent.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.mp;

import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;

/**
 * @author zecarlos
 */
public class MapChangeEvent {

	public static final int 
		OBJECT_CHANGED = 0, 
		OBJECT_REMOVED = 1, 
		OBJECT_ADDED = 2,
		OTHER = 3,
		SENSOR_BLINK = 4,
		MAP_RESET = -1;
	
	public static final String 
		UNKNOWN_CHANGE = "UNKNOWN_CHANGE",
		OBJECT_MOVED = "OBJECT_MOVED",
		OBJECT_ROTATED = "OBJECT_ROTATED",
		OBJECT_SCALED = "OBJECT_SCALED";
	
	private int eventType = OTHER;
	private AbstractElement changedObject = null;
	private MapType sourceMap = null;
	private MapGroup mapGroup;
	private String changeType = UNKNOWN_CHANGE;
	
	public MapChangeEvent(int EventType) {
		this.eventType = EventType;
	}
	
	/**
	 * @return Returns the changedObject.
	 */
	public AbstractElement getChangedObject() {
		return changedObject;
	}
	
	/**
	 * @param changedObject The changedObject to set.
	 */
	public void setChangedObject(AbstractElement changedObject) {
		this.changedObject = changedObject;
	}
	
	/**
	 * @return Returns the sourceMap.
	 */
	public MapType getSourceMap() {
		return sourceMap;
	}
	
	/**
	 * @param sourceMap The sourceMap to set.
	 */
	public void setSourceMap(MapType sourceMap) {
		this.sourceMap = sourceMap;
	}
	/**
	 * @return Returns the eventType.
	 */
	public int getEventType() {
		return eventType;
	}
	/**
	 * @param eventType The eventType to set.
	 */
	public void setEventType(int eventType) {
		this.eventType = eventType;
	}
	public MapGroup getMapGroup() {
		return mapGroup;
	}
	public void setMapGroup(MapGroup mapGroup) {
		this.mapGroup = mapGroup;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}
}
