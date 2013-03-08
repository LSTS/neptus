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
 * $Id:: HideObjectEdit.java 9615 2012-12-30 23:08:28Z pdias                    $:
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
public class HideObjectEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected AbstractElement element;
    protected boolean wasVisible;    
    protected int normalTransparency;
    
    public HideObjectEdit(AbstractElement element, boolean hide, int normalTransparency) {
        this.element = element;
        this.normalTransparency = normalTransparency;
        this.wasVisible = !hide;
    }
    
    @Override
    public void undo() throws CannotUndoException {
        if (wasVisible)
            element.setTransparency(normalTransparency);
        else
            element.setTransparency(0);
    }
    
    @Override
    public void redo() throws CannotRedoException {
        if (!wasVisible)
            element.setTransparency(normalTransparency);
        else
            element.setTransparency(0);
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
        if (wasVisible)
            return I18n.textf("Hide '%element'",element.getId());
        else
            return I18n.textf("Show '%element'",element.getId());
    }
    
}
