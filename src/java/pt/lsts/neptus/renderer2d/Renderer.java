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
 * 11/Out/2004
 */
package pt.lsts.neptus.renderer2d;

import java.awt.Color;

import javax.swing.event.ChangeListener;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;

/**
 * @author ZP
 * @author Paulo Dias
 */
public interface Renderer {
	
	static final int TRANSLATION = 1, ZOOM = 2, ROTATION = 3, RULER = 4, GRAB = 5, NONE = -1; 	
	
	static final Color[] vehicleColors = new Color[] {Color.red, Color.blue, Color.yellow, Color.pink, Color.cyan, Color.orange, Color.green, Color.white, Color.black};	
    /**
     * This method is called whenever the vehicle state has changed or a new vehicle
     * has been added.
     * @param systemId The vehicle whose state 
     */
	public abstract void vehicleStateChanged(String systemId, SystemPositionAndAttitude state);
	
	/**
	 * Called for initialization of the renderers
	 * @param mapGroup The Group of Maps containing all the object to be rendered
	 */
	public abstract void setMapGroup(MapGroup mapGroup);
	
	/**
	 * Gets the currently loaded maps
	 * @return the current loaded mapgroup
	 */
	public abstract MapGroup getMapGroup();
	
	/**
	 * Re-center the renderer in a way that the given object is centered on screen
	 * @param mo The object to be focused
	 */
	public abstract void focusObject(AbstractElement mo);
	
	/**
	 * Recenter the renderer in a way that the given location is centered on screen
	 * @param location The location to be focused
	 */
	public abstract void focusLocation(LocationType location);
	
	/**
	 * Sets the current view mode:
	 * 1 -> Translation
	 * 2 -> Zoom
	 * 3 -> Rotation
	 * 4 -> Ruler
	 * 5 -> Grab/Move
	 * -1 -> None
	 * @param mode
	 */
	public abstract void setViewMode(int mode);
	
	public abstract void followVehicle(String systemId);
	
	public abstract void addChangeListener(ChangeListener cl);
	
	public abstract void removeChangeListener(ChangeListener cl);
	
	public abstract int getShowMode();
	
	public abstract String getLockedVehicle();
	
	public abstract void cleanup();
	
	/**
	 * Sets the vehicles tail on.
	 * @param vehicles If null sets all.
	 */
	public abstract void setVehicleTailOn(String[] vehicles);
	
	/**
	 * Sets the vehicles tail off.
	 * @param vehicles If null sets all.
	 */
	public abstract void setVehicleTailOff(String[] vehicles);
	
	/**
	 * Clears the vehicles tail.
	 * @param vehicles If null clears all.
	 */
	public abstract void clearVehicleTail(String[] vehicles);

}
