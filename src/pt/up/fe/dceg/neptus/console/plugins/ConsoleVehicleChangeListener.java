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
 */
package pt.up.fe.dceg.neptus.console.plugins;

import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;

/**
 * 
 * @author rjpg
 *
 */
public interface ConsoleVehicleChangeListener {
	public static final int VEHICLE_ADDED = 0, VEHICLE_REMOVED = 1, VEHICLE_CHANGED = 2 ;
	public void consoleVehicleChange(VehicleType v,int status);
}
