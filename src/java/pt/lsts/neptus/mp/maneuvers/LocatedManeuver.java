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
package pt.lsts.neptus.mp.maneuvers;

import java.util.Collection;

import pt.lsts.neptus.mp.ManeuverLocation;

public interface LocatedManeuver {

    /**
     * Retrieve the position of the maneuver (position that can be dragged by the user)
     * @return a LocationType with maneuver's position
     */
	public ManeuverLocation getManeuverLocation();
	
	/**
	 * Retrieve the position where the vehicle will end this maneuver
	 * @return a LocationTypw with the last vehicle's position
	 */
	public ManeuverLocation getEndLocation();
	
	/**
     * Retrieve the position where the vehicle will start this maneuver
     * @return a LocationTypw with the position where vehicle will start the maneuver
     */    
    public ManeuverLocation getStartLocation();
    
    /**
     * Set the maneuver's position
     * @param location The new location for this maneuver
     */
	public void setManeuverLocation(ManeuverLocation location);
	
	
	public Collection<ManeuverLocation> getWaypoints();
	/**
	 * Translate the position of this maneuver
	 * @param offsetNorth north offset amount, in meters
	 * @param offsetEast east offset amount, in meters
	 * @param offsetDown down offset amount, in meters
	 */
	public void translate(double offsetNorth, double offsetEast, double offsetDown);
		
}
