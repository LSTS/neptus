/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * May 25, 2010
 * $Id:: VehicleStateListener.java 9880 2013-02-07 15:23:52Z jqcorreia          $:
 */
package pt.up.fe.dceg.neptus.renderer2d;

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;

/**
 * @author zp
 */
public interface VehicleStateListener {
	public void setVehicleState(VehicleType vehicle, SystemPositionAndAttitude state);
}
