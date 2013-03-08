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
 * $Id:: RemoveSelectionEdit.java 9616 2012-12-30 23:23:22Z pdias         $:
 */
package pt.up.fe.dceg.neptus.graph.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.graph.NeptusEdgeElement;
import pt.up.fe.dceg.neptus.graph.NeptusGraph;
import pt.up.fe.dceg.neptus.graph.NeptusNodeElement;

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
