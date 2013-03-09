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
package pt.up.fe.dceg.neptus.graph.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.graph.NeptusEdgeElement;
import pt.up.fe.dceg.neptus.graph.NeptusGraph;

/**
 * @author zp
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AddEdgeEdit extends AbstractUndoableEdit {

	private static final long serialVersionUID = 1L;
	private NeptusGraph<?, NeptusEdgeElement<?>> graph;
	private NeptusEdgeElement<?> edge;
	
	public AddEdgeEdit(NeptusGraph graph, NeptusEdgeElement edge) {
		this.graph = graph;
		this.edge = edge;
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
		graph.removeEdge(edge.getID());
		graph.repaint();
	}
	
	@Override
	public void redo() throws CannotRedoException {
		graph.addEdge(edge);
		graph.repaint();
	}
	
	@Override
	public String getPresentationName() {
		return "Add edge '"+edge.getID()+"'";
	}	
}
