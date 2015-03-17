/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.graph.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.lsts.neptus.graph.NeptusEdgeElement;
import pt.lsts.neptus.graph.NeptusGraph;
import pt.lsts.neptus.graph.NeptusNodeElement;

/**
 * @author zp
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RemoveSelectionEdit extends AbstractUndoableEdit {

	private static final long serialVersionUID = 1L;
	private NeptusGraph<NeptusNodeElement<?>, NeptusEdgeElement<?>> graph;
	private NeptusNodeElement[] removedNodes;
	private NeptusEdgeElement[] removedEdges;
	
	public RemoveSelectionEdit(NeptusGraph graph) {
		this.graph = graph;
		this.removedNodes = graph.getSelectedNodes();
		this.removedEdges = graph.getSelectedEdges(true);
	}
	
	@Override
	public boolean canRedo() {
		return true;
	}
	
	@Override
	public boolean canUndo() {
		return true;
	}
	
	@Override
	public void undo() throws CannotUndoException {
		for (NeptusNodeElement<?> node : removedNodes)
			graph.addNode(node);
		
		for (NeptusEdgeElement<?> edge : removedEdges) {
			graph.addEdge(edge);
		}
		
		graph.repaint();
	}
	
	@Override
	public void redo() throws CannotRedoException {
		for (NeptusNodeElement<?> node : removedNodes)
			graph.removeNode(node.getID());	
		
		for (NeptusEdgeElement<?> edge : removedEdges)
			graph.removeEdge(edge.getID());
		
		graph.repaint();
	}
	
	@Override
	public String getPresentationName() {
		return "Remove various elements";
	}	
}
