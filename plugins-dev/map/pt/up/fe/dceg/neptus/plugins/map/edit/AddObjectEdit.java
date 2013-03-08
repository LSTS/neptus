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
 * Nov 15, 2011
 * $Id:: AddObjectEdit.java 9615 2012-12-30 23:08:28Z pdias                     $:
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
public class AddObjectEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected AbstractElement element;
    
    public AddObjectEdit(AbstractElement element) {
        this.element = element;
    }
    
    @Override
    public void undo() throws CannotUndoException {
        element.getParentMap().remove(element.getId());
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED);
        mce.setSourceMap(element.getParentMap());
        mce.setChangedObject(element);
        mce.setMapGroup(element.getParentMap().getMapGroup());
        element.getParentMap().warnChangeListeners(mce);
    }
    
    @Override
    public void redo() throws CannotRedoException {
        element.getParentMap().addObject(element);
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
        mce.setSourceMap(element.getParentMap());
        mce.setChangedObject(element);
        mce.setMapGroup(element.getParentMap().getMapGroup());        
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
        return I18n.textf("Add the %mapelement '%elem'",element.getType(), element.getId());
    }
}
