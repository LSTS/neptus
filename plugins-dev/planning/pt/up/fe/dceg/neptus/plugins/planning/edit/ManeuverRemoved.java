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
 * Nov 29, 2011
 */
package pt.up.fe.dceg.neptus.plugins.planning.edit;

import java.util.Collection;
import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.types.mission.TransitionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class ManeuverRemoved extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected PlanType plan;
    protected Maneuver maneuver;
    protected Vector<TransitionType> addedTransitions = new Vector<TransitionType>();
    protected Vector<TransitionType> removedTransitions = new Vector<TransitionType>();
    protected boolean initial = false;
    
    public ManeuverRemoved(Maneuver maneuver, PlanType plan, Collection<TransitionType> addedTransitions,  Collection<TransitionType> removedTransitions, boolean wasInitial) {
        this.maneuver = maneuver;
        this.plan = plan;    
        this.addedTransitions.addAll(addedTransitions);
        this.removedTransitions.addAll(removedTransitions);
        initial = wasInitial;
    }
    
    @Override
    public boolean canUndo() {
        return true;
    }
    
    @Override
    public boolean canRedo() {
        return true;
    }
    
    @Override
    public String getPresentationName() {
        return "Remove the maneuver "+maneuver.getId();
    }
    
    @Override
    public void redo() throws CannotUndoException {
        for (TransitionType tt : removedTransitions)
            plan.getGraph().removeTransition(tt);    
        
       plan.getGraph().removeManeuver(maneuver);
        
       for (TransitionType tt : addedTransitions)
            plan.getGraph().addTransition(tt);
    }
    
    @Override
    public void undo() throws CannotRedoException {
        for (TransitionType tt : addedTransitions)
            plan.getGraph().removeTransition(tt);
        
        plan.getGraph().addManeuver(maneuver);         
        
        for (TransitionType tt : removedTransitions)
            plan.getGraph().addTransition(tt);
    }
    
    /**
     * @return the maneuver
     */
    public Maneuver getManeuver() {
        return maneuver;
    }
    
    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }

    
}
