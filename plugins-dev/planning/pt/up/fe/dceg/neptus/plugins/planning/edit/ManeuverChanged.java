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
 * $Id:: ManeuverChanged.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.plugins.planning.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class ManeuverChanged extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected PlanType plan;
    protected Maneuver maneuver;
    protected String beforeXml, afterXml;

    public ManeuverChanged(Maneuver maneuver, PlanType plan, String beforeXml) {
        this.maneuver = maneuver;
        this.plan = plan;
        this.beforeXml = beforeXml;
        this.afterXml = maneuver.getManeuverXml();        
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
        return "Change the maneuver "+maneuver.getId();
    }

    @Override
    public void undo() throws CannotUndoException {
        maneuver.loadManeuverXml(beforeXml);
    }

    @Override
    public void redo() throws CannotRedoException {
        maneuver.loadManeuverXml(afterXml);
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
