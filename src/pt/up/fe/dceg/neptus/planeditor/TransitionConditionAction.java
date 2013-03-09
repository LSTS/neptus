/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 5 de Jun de 2011
 */
package pt.up.fe.dceg.neptus.planeditor;

import pt.up.fe.dceg.neptus.types.mission.ActionType;
import pt.up.fe.dceg.neptus.types.mission.ConditionType;

/**
 * @author pdias
 *
 */
public class TransitionConditionAction {

    ConditionType condition;
    ActionType action;
    
    /**
     * 
     */
    public TransitionConditionAction() {
        condition = new ConditionType();
        condition.setCondition("ManeuverIsDone");
        action = new ActionType();
    }
    
    /**
     * @return the condition
     */
    public ConditionType getCondition() {
        return condition;
    }
    
    /**
     * @param condition the condition to set
     */
    public void setCondition(ConditionType condition) {
        this.condition = condition;
    }
    
    /**
     * @return the action
     */
    public ActionType getAction() {
        return action;
    }
    
    /**
     * @param action the action to set
     */
    public void setAction(ActionType action) {
        this.action = action;
    }
}
