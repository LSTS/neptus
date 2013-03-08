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
 * Oct 11, 2011
 * $Id:: IManeuverPreview.java 9880 2013-02-07 15:23:52Z jqcorreia              $:
 */
package pt.up.fe.dceg.neptus.mp.preview;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;

/**
 * @author zp
 *
 */
public interface IManeuverPreview<T extends Maneuver> {

    public boolean init(String vehicleId, T man, SystemPositionAndAttitude state, Object maneuverState);
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep);
    public void reset(SystemPositionAndAttitude state);
    public boolean isFinished();
    public Object getState();
}
