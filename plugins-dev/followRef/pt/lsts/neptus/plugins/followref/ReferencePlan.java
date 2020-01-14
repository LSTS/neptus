/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jun 5, 2013
 */
package pt.lsts.neptus.plugins.followref;

import java.util.Collection;
import java.util.Vector;

import pt.lsts.imc.Reference;

/**
 * @author zp
 *
 */
public class ReferencePlan {

    protected Vector<ReferenceWaypoint> waypoints = new Vector<>();    
    protected String system_id;
    public ReferencePlan(String systemId) {
        this.system_id = systemId;
    }
    
    public ReferenceWaypoint currentWaypoint() {
        if (waypoints.isEmpty())
            return null;
        return waypoints.firstElement();
    }
    
    public Collection<ReferenceWaypoint> getWaypoints() {
        return waypoints;
    }
    
    public void addWaypointAtEnd(Reference ref) {
        waypoints.add(new ReferenceWaypoint(ref));
    }
    
    public void addWaypointAtEnd(ReferenceWaypoint wpt) {
        waypoints.add(wpt);
    }
    
    public ReferenceWaypoint cloneWaypoint(ReferenceWaypoint wpt) {
        if (!waypoints.contains(wpt))
            return null;
        int index = waypoints.indexOf(wpt);
        ReferenceWaypoint clone = new ReferenceWaypoint(wpt.getReference());
        clone.time = wpt.time;
        waypoints.add(index+1, clone);
        return clone;
    }
    
    public ReferenceWaypoint popFirstWaypoint() {
        if (waypoints.size() > 1)
            return waypoints.remove(0);
        else
            return null;        
    }
    
    public void removeWaypoint(ReferenceWaypoint wpt) {
        if (waypoints.size() > 1)
            waypoints.remove(wpt);
    }
}
