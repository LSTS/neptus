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
package pt.up.fe.dceg.neptus.graph;

import java.util.Vector;

public class IdGenerator {

	
	
	public Vector<Boolean> takenIDs = new Vector<Boolean>();
	private boolean reuse = true;
	
	
	public IdGenerator(boolean reuse) {
		this.reuse = reuse;
	}
	
	public int generateId() {
		
		if (reuse) {
			for (int i = 0; i < takenIDs.size(); i++) {		
				if (takenIDs.get(i) == false) {
					takenIDs.set(i, true);
					return i;
				}
			}
		}
		takenIDs.add(true);
		return takenIDs.size()-1;
	}
	
	public void recycleId(int id) {		
		takenIDs.set(id, false);
	}
}
