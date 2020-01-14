/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.graph.edit;

import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.graph.NeptusEdgeElement;
import pt.lsts.neptus.graph.NeptusGraph;
import pt.lsts.neptus.graph.NeptusNodeElement;

/**
 * @author zp
 */
@SuppressWarnings("rawtypes")
public class RemoveNodeEdit extends AbstractUndoableEdit {

	private static final long serialVersionUID = 1L;
	private NeptusGraph<NeptusNodeElement<?>, NeptusEdgeElement<?>> graph;
	private NeptusNodeElement<?> node;
	private Vector<NeptusEdgeElement> referredEdges = new Vector<NeptusEdgeElement>();
	
	public RemoveNodeEdit(NeptusGraph<NeptusNodeElement<?>, NeptusEdgeElement<?>> graph, NeptusNodeElement<?> node) {
		this.graph = graph;
		this.node = node;
		
		referredEdges.addAll(node.getIncomingEdges().values());
		referredEdges.addAll(node.getOutgoingEdges().values());
		
		NeptusLog.pub().info("<###> "+referredEdges.size()+" edges referred");
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
		graph.addNode(node);
		for (NeptusEdgeElement<?> edge : referredEdges) {
			graph.addEdge(edge);
		}
		graph.repaint();
		
	}
	
	@Override
	public void redo() throws CannotRedoException {
		graph.removeNode(node.getID());
		graph.repaint();
	}
	
	@Override
	public String getPresentationName() {
		return "Remove node '"+node.getID()+"'";
	}	
}
