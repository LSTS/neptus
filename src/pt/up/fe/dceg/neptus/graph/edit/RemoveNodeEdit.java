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
 * $Id:: RemoveNodeEdit.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.graph.edit;

import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.graph.NeptusEdgeElement;
import pt.up.fe.dceg.neptus.graph.NeptusGraph;
import pt.up.fe.dceg.neptus.graph.NeptusNodeElement;

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
		
		System.out.println(referredEdges.size()+" edges referred");
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
