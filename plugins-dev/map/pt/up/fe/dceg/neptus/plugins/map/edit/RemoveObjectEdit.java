/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 14, 2011
 */
package pt.up.fe.dceg.neptus.plugins.map.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;

/**
 * @author zp
 *
 */
public class RemoveObjectEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected AbstractElement element = null;
    
    public RemoveObjectEdit(AbstractElement element) {
        this.element = element;
    }
    
    @Override
    public void undo() throws CannotUndoException {
        element.getParentMap().addObject(element);
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
        mce.setSourceMap(element.getParentMap());
        mce.setChangedObject(element);
        mce.setMapGroup(element.getMapGroup());
        element.getParentMap().warnChangeListeners(mce);
    }
    
    @Override
    public void redo() throws CannotRedoException {
        element.getParentMap().remove(element.getId());
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED);
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
        return I18n.textf("Remove '%element' (%element_type)",element.getId(), element.getType());
    }
    
    
}
