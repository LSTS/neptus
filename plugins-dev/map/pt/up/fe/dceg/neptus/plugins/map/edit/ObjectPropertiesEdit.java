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
 */
package pt.up.fe.dceg.neptus.plugins.map.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;

/**
 * @author zp
 *
 */
public class ObjectPropertiesEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected String oldXml;
    protected String newXml;
    protected AbstractElement element;
    
    public ObjectPropertiesEdit(AbstractElement element, String oldXml) {
        this.element = element;
        this.oldXml = oldXml;
        this.newXml = element.asXML();
    }
    
    @Override
    public void undo() throws CannotUndoException {
        element.load(oldXml);
    }
    
    @Override
    public void redo() throws CannotRedoException {
        element.load(newXml);
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
        return I18n.textf("Change '%element'",element.getId());
    }
}
