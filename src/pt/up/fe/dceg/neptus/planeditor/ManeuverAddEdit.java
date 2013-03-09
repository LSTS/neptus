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

import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.types.mission.TransitionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author ZP
 *
 */
public class ManeuverAddEdit extends AbstractUndoableEdit {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected PlanType plan;
    protected Maneuver added;
    protected Vector<TransitionType> transitions = new Vector<TransitionType>();
    public ManeuverAddEdit(PlanType plan, Maneuver man) {
        this.plan = plan;
        this.added = man;
        transitions.addAll(plan.getGraph().getExitingTransitions(man));
        transitions.addAll(plan.getGraph().getIncomingTransitions(man));
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
        if (plan.getGraph().getManeuver(added.getId()) == null) {
            plan.getGraph().addManeuver((Maneuver)added.clone());  
            for (TransitionType t : transitions)
                plan.getGraph().getTransitions().put(t.getId(), t);            
        }
        else
            throw new CannotRedoException();
    }
    
    @Override
    public void undo() throws CannotUndoException {
        plan.getGraph().removeManeuver(plan.getGraph().getManeuver(added.getId()));
    }
}
