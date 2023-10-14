/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.mp;

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
