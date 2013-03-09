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

import java.awt.geom.Point2D;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.graph.NeptusGraph;
import pt.up.fe.dceg.neptus.graph.NeptusNodeElement;

/**
 * @author zp
 */
@SuppressWarnings("rawtypes")
public class MoveSelectionEdit extends AbstractUndoableEdit {

	private static final long serialVersionUID = 1L;
	private NeptusGraph<?, ?> graph;
	private NeptusNodeElement[] movedNodes;
	private Point2D dragDifference;
	
	public MoveSelectionEdit(NeptusGraph<?, ?> graph, NeptusNodeElement[] movedNodes, Point2D dragDifference) {
		this.graph = graph;
		this.movedNodes = movedNodes;
		this.dragDifference = dragDifference;		
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
		for (NeptusNodeElement<?> node : movedNodes) {
			double dX = dragDifference.getX();
			double dY = dragDifference.getY();
			node.setPosition(new Point2D.Double(node.getPosition().getX()-dX,node.getPosition().getY()-dY));
		}
		graph.repaint();
	}
	
	@Override
	public void redo() throws CannotRedoException {
		for (NeptusNodeElement<?> node : movedNodes) {
			double dX = dragDifference.getX();
			double dY = dragDifference.getY();
			node.setPosition(new Point2D.Double(node.getPosition().getX()+dX,node.getPosition().getY()+dY));
		}
		graph.repaint();
	}
	
	@Override
	public String getPresentationName() {
		if (movedNodes.length == 1)
			return "Move the node '"+movedNodes[0].getID()+"'";
		else
			return "Move selection";
	}	
}
