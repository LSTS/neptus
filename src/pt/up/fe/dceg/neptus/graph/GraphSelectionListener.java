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
 * $Id:: GraphSelectionListener.java 9616 2012-12-30 23:23:22Z pdias      $:
 */
package pt.up.fe.dceg.neptus.graph;

public interface GraphSelectionListener {

	public void selectionChanged(NeptusGraphElement<?>[] selection);
}
