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
 * 11/Out/2004
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.awt.Color;

import javax.swing.event.ChangeListener;

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;

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
