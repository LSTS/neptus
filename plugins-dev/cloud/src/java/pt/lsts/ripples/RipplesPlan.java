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
 * Author: ineeve
 * July 15, 2019
 */

package pt.lsts.ripples;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RipplesPlan {
	private String id = "idle";
	private ArrayList<RipplesWaypoint> waypoints = new ArrayList<>();
	private String type = "dune";

	public RipplesPlan() {
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public RipplesPlan(String id, ArrayList<double[]> waypoints) {
		setId(id);
		setWaypoints(waypoints);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ArrayList<RipplesWaypoint> getWaypoints() {
		return waypoints;
	}

	public void setWaypoints(ArrayList<double[]> waypointsLocations) {
		List<RipplesWaypoint> waypoints = waypointsLocations.stream().map(latLng -> new RipplesWaypoint(latLng))
				.collect(Collectors.toList());
		this.waypoints = new ArrayList<RipplesWaypoint>(waypoints);
	}

}