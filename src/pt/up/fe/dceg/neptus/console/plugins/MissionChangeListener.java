/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by RJPG
 * 2006/06/02
 */
package pt.up.fe.dceg.neptus.console.plugins;

import pt.up.fe.dceg.neptus.types.mission.MissionType;

public interface MissionChangeListener {    
	public void missionReplaced(MissionType mission);
	public void missionUpdated(MissionType mission);
}
