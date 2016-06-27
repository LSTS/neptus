/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsmarques
 * 15 Feb 2016
 */
package pt.lsts.neptus.plugins.mvplanning.planning;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.plugins.mvplanning.jaxb.plans.PlanTaskJaxb;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 */

/* Wrapper around PlanSpecification */
public class PlanTask {
    public static enum TASK_TYPE {
        COVERAGE_AREA("CoverageArea"),
        VISIT_POINT("VisitArea"),
        NEPTUS_PLAN("NeptusPlan");

        protected String value;
        TASK_TYPE(String value) {
            this.value = value;
        }
    };

    private String planId;
    private PlanType plan;
    private Profile planProfile;
    private double timestamp;
    private byte[] md5;
    private double completion;

    private TASK_TYPE taskType;

    public PlanTask(String id, PlanType plan, Profile planProfile, TASK_TYPE taskType) {
        this.planId = id;
        this.plan = plan;
        this.plan.setId(id);
        this.planProfile = planProfile;
        this.timestamp = -1;
        this.taskType = taskType;

        completion = 0;
        md5 = plan.asIMCPlan().payloadMD5();
        /* set a vehicle by default */
        plan.setVehicle("lauv-xplore-1");
    }

    public PlanTask(PlanTaskJaxb ptaskJaxb) {
        load(ptaskJaxb);
    }

    public String getTaskTypeAsString() {
        return taskType.value;
    }

    public String getPlanId() {
        return planId;
    }

    public PlanSpecification getPlanSpecification() {
        return (PlanSpecification) IMCUtils.generatePlanSpecification(this.plan);
    }

    public PlanType asPlanType() {
        return plan;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public Profile getProfile() {
        return planProfile;
    }

    public byte[] getMd5() {
        return md5;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public void setPlanType(TASK_TYPE taskType) {
        this.taskType = taskType;
    }

    public TASK_TYPE getTaskType() {
        return taskType;
    }

    public void setMissionType(MissionType mtype) {
        if(mtype != null)
            plan.setMissionType(mtype);
        else
            NeptusLog.pub().warn("Trying to set a null MissionType");
    }

    public void updatePlanCompletion(double completion) {
        this.completion = completion;
    }

    public double getCompletion() {
        return completion;
    }

    /**
     * If the given vehicle can execute this plan with
     * the needed profile.
     * */
    public boolean containsVehicle(String vehicle) {
        return planProfile.getProfileVehicles().contains(vehicle);
    }

    private void load(PlanTaskJaxb taskJaxb) {
        this.planId = taskJaxb.planId;
        this.plan = taskJaxb.plan;
        this.planProfile = taskJaxb.planProfile;
        this.timestamp = taskJaxb.timestamp;
        this.md5 = taskJaxb.md5;
        this.completion = taskJaxb.completion;
        this.taskType = string2TaskType(taskJaxb.taskType);
    }

    private TASK_TYPE string2TaskType(String str) {
        for(TASK_TYPE type : TASK_TYPE.values())
            return type;

        NeptusLog.pub().warn("Couldn't figure out task type, setting as NeptusPlan");
        return TASK_TYPE.NEPTUS_PLAN;
    }
}
