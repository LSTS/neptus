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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.plugins.mvplanning.jaxb.plans.PlanTypeJaxbAdapter;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 */

/* Wrapper around PlanSpecification */
@XmlRootElement (name="PlanTask")
@XmlAccessorType(XmlAccessType.NONE)
public class PlanTask {
    public static enum TASK_TYPE {
        COVERAGE_AREA,
        VISIT_POINT
    };

    @XmlElement(name = "PlanId")
    private String planId;

    @XmlJavaTypeAdapter(PlanTypeJaxbAdapter.class)
    private PlanType plan;

    @XmlElement
    private Profile planProfile;

    @XmlElement(name = "Timestamp")
    private double timestamp;

    @XmlElement(name = "md5")
    private byte[] md5;

    @XmlElement(name = "Completion")
    private double completion;

    private TASK_TYPE taskType;

    public PlanTask(String id, PlanType plan, Profile planProfile) {
        this.planId = id;
        this.plan = plan;
        this.plan.setId(id);
        this.planProfile = planProfile;
        this.timestamp = -1;

        completion = 0;
        md5 = plan.asIMCPlan().payloadMD5();
        /* set a vehicle by default */
        plan.setVehicle("lauv-xplore-1");
    }

    /**
     * Constructor used by JAXB when marshaling an object of this class
     * */
    public PlanTask() {
    }

    /**
     * Constructor used by JAXB when unmarshalling an object of this class
     * */
    public PlanTask(String id, PlanType plan, Profile planProfile, double timestamp, byte[] md5, double completion, TASK_TYPE taskType) {
        this.planId = id;
        this.plan = plan;
        this.plan.setId(id);
        this.planProfile = planProfile;
        this.timestamp = timestamp;
        this.md5 = md5;
        this.completion = completion;
        this.taskType = taskType;
    }

    @XmlElement(name = "TaskType")
    public String getTaskTypeAsString() {
        if(taskType == TASK_TYPE.COVERAGE_AREA)
            return "CoverageArea";
        else if(taskType == TASK_TYPE.VISIT_POINT)
            return "VisitPoint";

        return "unknown";
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

    /**
     * If the given vehicle can execute this plan with
     * the needed profile.
     * */
    public boolean containsVehicle(String vehicle) {
        return planProfile.getProfileVehicles().contains(vehicle);
    }
}
