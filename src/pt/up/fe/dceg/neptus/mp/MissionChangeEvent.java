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
 * 20??/??/??
 * $Id:: MissionChangeEvent.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.mp;

public class MissionChangeEvent {

	public static final int
		TYPE_MAP_ADDED = 0, 
		TYPE_MAP_REMOVED = 1,
		TYPE_PLAN_ADDED = 2,
		TYPE_PLAN_REMOVED = 3,
		TYPE_CHECKLIST_ADDED = 4,
		TYPE_CHECKLIST_REMOVED = 5,
		TYPE_MAPOBJECT_ADDED = 6,
		TYPE_MAPOBJECT_REMOVED = 7,
		TYPE_MAPOBJECT_CHANGED = 8,
		TYPE_MANEUVER_ADDED = 9,
		TYPE_MANEUVER_REMOVED = 10,
		TYPE_MANEUVER_CHANGED = 11,
		TYPE_PLANEDGE_ADDED = 12,
		TYPE_PLANEDGE_REMOVED = 13,
		TYPE_PLANEDGE_CHANGED = 14,
		TYPE_MISSION_CLOSED = 15,
		TYPE_MISSION_OPENED = 16,
		TYPE_MISSION_SAVED = 17,
		TYPE_UNKNOWN = Integer.MAX_VALUE;
	
	private int type = TYPE_UNKNOWN;
	private Object data = null;
	
	public MissionChangeEvent(int type, Object data) {
		setType(type);
		setData(data);
	}
	
	public MissionChangeEvent(int type) {
		this(type, null);
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
