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
package pt.lsts.neptus.mp;

import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;

/**
 * @author zecarlos
 */
public class MapChangeEvent {

    public enum ElementType {
        BEACON,
        ANY;
    }

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

    public final ElementType element;
	
	public MapChangeEvent(int EventType) {
		this.eventType = EventType;
        element = ElementType.ANY;
	}
	
    public MapChangeEvent(int EventType, ElementType element) {
        this.eventType = EventType;
        this.element = element;
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
