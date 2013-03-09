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
package pt.up.fe.dceg.neptus.mme;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.MapType;

public class AddObjectEdit extends AbstractUndoableEdit {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AbstractElement element = null;
	private MapType map = null;
	private MissionMapEditor mme = null;
	
	public AddObjectEdit(MissionMapEditor mme, AbstractElement element) {
		this.mme = mme;
		this.map = mme.getMap();
		this.element = element;
	}
	
	@Override
	public void undo() throws CannotUndoException {
		map.remove(element.getId());
		mme.removeObjectFromTree(element);
		MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED);
		mce.setSourceMap(map);
		mce.setChangedObject(element);
		mce.setMapGroup(map.getMapGroup());
		map.warnChangeListeners(mce);
	}
	
	@Override
	public void redo() throws CannotRedoException {
		map.addObject(element);
		mme.addObjectToTree(element);
		MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
		mce.setSourceMap(map);
		mce.setChangedObject(element);
		mce.setMapGroup(map.getMapGroup());
		map.warnChangeListeners(mce);
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
		return "Add the "+element.getType()+" '"+element.getId()+"'";
	}
}
