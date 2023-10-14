/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 31/01/2017
 */
package pt.lsts.neptus.plugins.mvplanner.api;

import java.util.Date;

import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 * @author tsm 
 *
 */
public class ConsoleEventPlanAllocation {
    protected PlanType plan;
    protected String vehicle;
    protected Date startTime;
    protected Operation op;
    
    
    public ConsoleEventPlanAllocation(ConsoleEventPlanAllocation task, Operation op) {
        this.startTime = task.getStartTime();
        this.plan = task.plan;
        this.op = op;
        this.vehicle = task.vehicle;
    }

    
    public ConsoleEventPlanAllocation(PlanType plan, Date startTime, Operation op) {
        this.startTime = startTime;
        this.vehicle = plan.getVehicle();
        this.plan = plan;
        this.op = op;
    }
    
    /**
     * @return the vehicle
     */
    public final String getVehicle() {
        return plan.getVehicle();
    }
    
    /**
     * @return the task id
     */
    public final String getId() {
        return plan.getId();
    }

    /**
     * @return the startTime
     */
    public final Date getStartTime() {
        return startTime;
    }

    /**
     * @return the op
     */
    public final Operation getOp() {
        return op;
    }

    public final PlanType getPlan() {
        return plan;
    }

    public enum Operation {
        ALLOCATED,
        FINISHED,
        INTERRUPTED,
        CANCELLED
    }
}
