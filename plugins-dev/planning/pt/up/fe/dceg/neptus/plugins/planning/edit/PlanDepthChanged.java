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

import java.util.LinkedHashMap;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class PlanDepthChanged extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, Double> previousDepths = new LinkedHashMap<String, Double>();
    protected double newDepth;
    protected PlanType plan;
    
    public PlanDepthChanged(PlanType plan, double newDepth, LinkedHashMap<String, Double> previousDepths) {
        this.plan = plan;
        this.newDepth = newDepth;
        this.previousDepths = previousDepths;
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
        return "Set plan depth to "+newDepth;
    }

    @Override
    public void undo() throws CannotUndoException {
        for (String key : previousDepths.keySet()) {
            Maneuver m = plan.getGraph().getManeuver(key);
            if (m instanceof LocatedManeuver) {
                LocationType loc = ((LocatedManeuver)m).getManeuverLocation();
                loc.setAbsoluteDepth(previousDepths.get(key));
                ((LocatedManeuver)m).getManeuverLocation().setLocation(loc);
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {      
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (m instanceof LocatedManeuver) {
                LocationType loc = ((LocatedManeuver)m).getManeuverLocation();
                loc.setAbsoluteDepth(newDepth);
                ((LocatedManeuver) m).getManeuverLocation().setLocation(loc);
            }
        }
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }       
    
    /**
     * @return the newDepth
     */
    public double getNewDepth() {
        return newDepth;
    }
    
    /**
     * @return the previousDepths
     */
    public LinkedHashMap<String, Double> getPreviousDepths() {
        return previousDepths;
    }
}
