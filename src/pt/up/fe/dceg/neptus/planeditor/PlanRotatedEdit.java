/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 12 de Ago de 2011
 */
package pt.up.fe.dceg.neptus.planeditor;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import pt.up.fe.dceg.neptus.types.map.PlanElement;

/**
 * @author ZP
 *
 */
public class PlanRotatedEdit extends AbstractUndoableEdit {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected PlanElement plan;
    protected double dragN, dragE;    
    
    public PlanRotatedEdit(PlanElement plan, double dragN, double dragE) {
        this.plan = plan;
        this.dragE = dragE;
        this.dragN = dragN;
    }
    
    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        if (anEdit instanceof PlanRotatedEdit && ((PlanRotatedEdit)anEdit).plan == plan) {            
            PlanRotatedEdit other = (PlanRotatedEdit)anEdit;
            dragE += other.dragE;
            dragN += other.dragN;
            return true;
        }
        return false;
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
    public void redo() throws CannotRedoException {
       plan.translatePlan(dragN, dragE, 0);
    }
    
    @Override
    public void undo() throws CannotUndoException {
        plan.translatePlan(-dragN, -dragE, 0);
    }
}
