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
 * $Id:: LocatedManeuver.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import pt.up.fe.dceg.neptus.mp.ManeuverLocation;

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
	
	/**
	 * Translate the position of this maneuver
	 * @param offsetNorth north offset amount, in meters
	 * @param offsetEast east offset amount, in meters
	 * @param offsetDown down offset amount, in meters
	 */
	public void translate(double offsetNorth, double offsetEast, double offsetDown);
		
}
