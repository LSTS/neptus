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
 * $Id:: ManeuverRemoveEdit.java 9615 2012-12-30 23:08:28Z pdias                $:
 */
package pt.up.fe.dceg.neptus.planeditor;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author ZP
 *
 */
public class ManeuverRemoveEdit extends AbstractUndoableEdit {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected PlanType plan;
    protected Maneuver removed;
    
    public ManeuverRemoveEdit(PlanType plan, Maneuver man) {
        this.plan = plan;
        this.removed = man;
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
        plan.getGraph().removeManeuver(plan.getGraph().getManeuver(removed.getId()));
        
    }
    
    @Override
    public void undo() throws CannotUndoException {
        if (plan.getGraph().getManeuver(removed.getId()) == null) {
            plan.getGraph().addManeuver((Maneuver)removed.clone());            
        }
        else
            throw new CannotRedoException();
    }
}
