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
 * $Id:: PlanSettingsChanged.java 9615 2012-12-30 23:08:28Z pdias               $:
 */
package pt.up.fe.dceg.neptus.plugins.planning.edit;

import java.util.LinkedHashMap;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

import com.l2fprod.common.propertysheet.DefaultProperty;

/**
 * @author zp
 *
 */
public class PlanSettingsChanged extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, DefaultProperty> previousSettings;
    protected DefaultProperty newSetting;
    protected PlanType plan;
    
    public PlanSettingsChanged(PlanType plan, DefaultProperty newSetting, LinkedHashMap<String, DefaultProperty> previousSettings) {
        this.plan = plan;
        this.newSetting = newSetting;
        this.previousSettings = previousSettings;
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
        return "Set plan "+newSetting.getName()+" to "+newSetting.getValue();
    }

    @Override
    public void undo() throws CannotUndoException {
        for (String key : previousSettings.keySet()) {
            plan.getGraph().getManeuver(key).setProperties(
                    new DefaultProperty[] {previousSettings.get(key)});
        }
    }

    @Override
    public void redo() throws CannotRedoException {      
        for (String key : previousSettings.keySet()) {
            plan.getGraph().getManeuver(key).setProperties(
                    new DefaultProperty[] {newSetting});
        }
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }       
    
}
