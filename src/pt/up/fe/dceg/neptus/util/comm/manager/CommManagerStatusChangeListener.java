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
 * 2007/05/19
 * $Id:: CommManagerStatusChangeListener.java 9616 2012-12-30 23:23:22Z p#$:
 */
package pt.up.fe.dceg.neptus.util.comm.manager;

import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;

/**
 * @author pdias
 *
 */
public interface CommManagerStatusChangeListener
{

	public abstract void managerStatusChanged(int status, String msg);

	public abstract void managerVehicleAdded(VehicleType vehicle);

	public abstract void managerVehicleRemoved(VehicleType vehicle);

	public abstract void managerVehicleStatusChanged(VehicleType vehicle,
			int status);

	public abstract void managerSystemAdded(String systemId);

	public abstract void managerSystemRemoved(String systemId);

	public abstract void managerSystemStatusChanged(String systemId,
			int status);
	
}