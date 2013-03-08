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
 * $Id:: RotateObjectEdit.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.mme;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.RotatableElement;

public class RotateObjectEdit extends AbstractUndoableEdit {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AbstractElement element = null;	
	private double oldAngle = 0;
	private double newAngle = 0;
	
	public RotateObjectEdit(AbstractElement element, double oldAngle, double newAngle) {		
		this.element = element;
		this.oldAngle = oldAngle;
		this.newAngle = newAngle;
	}
	
	@Override
	public void undo() throws CannotUndoException {
		((RotatableElement)element).rotateRight(oldAngle-newAngle);
		MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
		mce.setSourceMap(element.getParentMap());
		mce.setChangedObject(element);
		mce.setChangeType(MapChangeEvent.OBJECT_ROTATED);
		mce.setMapGroup(element.getMapGroup());
		element.getParentMap().warnChangeListeners(mce);
	}
	
	@Override
	public void redo() throws CannotRedoException {
		((RotatableElement)element).rotateLeft(oldAngle-newAngle);
		MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
		mce.setSourceMap(element.getParentMap());
		mce.setChangedObject(element);
		mce.setMapGroup(element.getMapGroup());
		element.getParentMap().warnChangeListeners(mce);
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
	public String getPresentationName() {
		return "Rotate the "+element.getType()+" '"+element.getId()+"'";
	}
}
