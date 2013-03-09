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
 * Apr 27, 2010
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class CoverageCell {
	public LocationType realWorldLoc = null;
	public boolean desired = false;
	public boolean visited = false;
	public boolean active = false;
	public CoverageCell next = null;
	public CoverageCell previous = null;
	
	public int i, j;
	
	public char rep() {
		if (active)
			return 'A';
		if (!desired)
			return '.';
		if (!visited)
			return ' ';
		return '*';		
	}
}
