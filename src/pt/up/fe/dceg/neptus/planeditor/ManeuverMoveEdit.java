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
 * $Id:: ManeuverMoveEdit.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.planeditor;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author ZP
 *
 */
public class ManeuverMoveEdit extends AbstractUndoableEdit {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected PlanType plan;
    protected String manId;
    ManeuverLocation oldLoc, newLoc;
    
    public ManeuverMoveEdit(PlanType plan, Maneuver man, ManeuverLocation oldLoc, ManeuverLocation newLoc) {
        this.plan = plan;
        this.manId = man.getId();
        this.oldLoc = oldLoc;
        this.newLoc = newLoc;
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
        Maneuver m = plan.getGraph().getManeuver(manId);
        if (m != null && m instanceof LocatedManeuver) {
            ((LocatedManeuver)m).setManeuverLocation(newLoc);
        }
        else {
            throw new CannotRedoException();
        }
    }
    
    @Override
    public void undo() throws CannotUndoException {
        Maneuver m = plan.getGraph().getManeuver(manId);
        if (m != null && m instanceof LocatedManeuver) {
            ((LocatedManeuver)m).setManeuverLocation(oldLoc);
        }
        else {
            throw new CannotRedoException();
        }
    }
}
