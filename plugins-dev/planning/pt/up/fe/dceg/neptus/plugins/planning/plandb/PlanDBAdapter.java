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
public abstract class PlanDBAdapter implements IPlanDBListener {

    @Override
    public void dbInfoUpdated(PlanDBState updatedInfo) {       
        
    }

    @Override
    public void dbPlanReceived(PlanType spec) {  
        
    }

    @Override
    public void dbPlanSent(String planId) {

    }

    @Override
    public void dbPlanRemoved(String planId) {

    }

    @Override
    public void dbCleared() {

    }

}
