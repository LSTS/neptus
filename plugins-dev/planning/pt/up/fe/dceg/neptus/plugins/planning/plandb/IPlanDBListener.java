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
 * Jun 22, 2011
 */
package pt.up.fe.dceg.neptus.plugins.planning.plandb;

import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public interface IPlanDBListener {

    /**
     * This method is called whenever an update is received from the remote state
     * @param updatedInfo The new updated remote PlanDB state
     */
    public void dbInfoUpdated(PlanDBState updatedInfo);

    /**
     * This method is called whenever a plan is received from the vehicle
     * @param updatedInfo The plan that was received from the vehicle
     */
    public void dbPlanReceived(PlanType spec);

    /**
     * This method is called whenever a sent plan was received successfully by the vehicle
     * @param updatedInfo The sent plan id
     */
    public void dbPlanSent(String planId);
    
    /**
     * This method is called whenever a sent plan was received successfully by the vehicle
     * @param updatedInfo The sent plan id
     */    
    public void dbPlanRemoved(String planId);
    public void dbCleared();
}
