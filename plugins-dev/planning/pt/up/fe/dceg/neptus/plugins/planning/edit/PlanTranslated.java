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
 * $Id:: PlanTranslated.java 9615 2012-12-30 23:08:28Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.plugins.planning.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class PlanTranslated extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected PlanType plan;
    protected double deltaEast, deltaNorth;

    public PlanTranslated(PlanType plan, double deltaEast, double deltaNorth) {
        this.plan = plan;     
        this.deltaEast = deltaEast;
        this.deltaNorth = deltaNorth;
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
        return "Translate the plan";
    }

    @Override
    public void undo() throws CannotUndoException {
        for (Maneuver man : plan.getGraph().getAllManeuvers())
            if (man != null && man instanceof LocatedManeuver) {
                ((LocatedManeuver)man).translate(-deltaNorth, -deltaEast, 0);
            }
    }

    @Override
    public void redo() throws CannotRedoException {      
        for (Maneuver man : plan.getGraph().getAllManeuvers())
            if (man != null && man instanceof LocatedManeuver) {
                ((LocatedManeuver)man).translate(deltaNorth, deltaEast, 0);
            }        
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }       
}
