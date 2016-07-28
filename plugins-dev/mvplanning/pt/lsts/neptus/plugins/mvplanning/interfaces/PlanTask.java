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
package pt.lsts.neptus.plugins.mvplanning.interfaces;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.plugins.mvplanning.jaxb.plans.PlanTaskJaxb;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.constraints.*;
import pt.lsts.neptus.plugins.mvplanning.utils.TaskPddlParser;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Base class for the tasks that can be allocated
 * to the vehicles
 * @author tsmarques
 */

/* Wrapper around PlanSpecification */
public abstract class PlanTask {
    public static enum TASK_TYPE {
        COVERAGE_AREA("CoverageArea"),
        VISIT_POINT("VisitPoint"),
        SAFETY("Safety"), /* task used to move a vehicle to a safe position */
        NEPTUS_PLAN("NeptusPlan");

        public String value;
        TASK_TYPE(String value) {
            this.value = value;
        }
    };

    protected String planId;
    protected PlanType plan;
    protected Profile planProfile;
    protected double timestamp;
    protected byte[] md5;
    protected double completion;
    protected TASK_TYPE taskType;
    protected List<TaskConstraint> constraints;

    public PlanTask(String id, PlanType plan, Profile planProfile, TASK_TYPE taskType) {
        this.planId = id;
        this.plan = plan;
        this.plan.setId(id);
        this.planProfile = planProfile;
        this.timestamp = -1;
        this.taskType = taskType;

        completion = 0;
        md5 = plan.asIMCPlan().payloadMD5();

        loadTaskPddlSpecs();
    }

    public PlanTask(String id, Profile profile) {
        planId = id;
        planProfile = profile;

        plan = null;
        timestamp = -1;
        completion = -1;
        md5 = null;
        taskType = null;

        loadTaskPddlSpecs();
    }

    public PlanTask(PlanTaskJaxb ptaskJaxb) {
        load(ptaskJaxb);
        loadTaskPddlSpecs();
    }

    public abstract TASK_TYPE getTaskType();
    public abstract ManeuverLocation getLastLocation();

    public ManeuverLocation getFirstLocation() {
        return ((LocatedManeuver) plan
                .getGraph()
                .getAllManeuvers()[0])
                .getManeuverLocation();
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

    public double getCompletion() {
        return completion;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public void setPlan(PlanType ptype) {
        plan = ptype;
    }

    public void setMd5(byte[] md5) {
        this.md5 = md5;
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

    public List<TaskConstraint> getConstraints() {
        return constraints;
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

    public static TASK_TYPE string2TaskType(String str) {
        for(TASK_TYPE type : TASK_TYPE.values())
            if(str == type.value)
                return type;

        NeptusLog.pub().warn("Couldn't figure out task type, setting as NeptusPlan");
        return TASK_TYPE.NEPTUS_PLAN;
    }

    public List<TaskConstraint> setDefaultTaskConstraints() {
        List<TaskConstraint> constraints = new ArrayList<>();

        constraints.add(new IsAvailable());
        constraints.add(new IsActive());
        constraints.add(new HasPayload(getProfile()));
        constraints.add(new HasTcpOn());
        constraints.add(new HasSafeLocationSet());
        constraints.add(new BatteryLevel(50, BatteryLevel.OPERATION.Gequal));

        return constraints;
    }

    /**
     * From a PDDL domain load the specifications of this task,
     * like duration, constraints, etc
     * */
    private void loadTaskPddlSpecs() {
        /* load task constraints */
        this.constraints = new ArrayList<>();
        Map<String, String> taskConstraints = TaskPddlParser.getTaskConstraints(getTaskType().name());

        if(taskConstraints == null || taskConstraints.isEmpty()) {
            NeptusLog.pub().warn("[" + getTaskType().value + "] No constraints found/parsed. Using default ones...");
            constraints = setDefaultTaskConstraints();
        }
        else {
            for(Map.Entry<String, String> entry : taskConstraints.entrySet()) {
                TaskConstraint.NAME constrName = Arrays.asList(TaskConstraint.NAME.values())
                        .stream()
                        .filter(c -> c.name().equals(entry.getKey()))
                        .findFirst()
                        .get();

                /* instantiate the needed TaskContraint's */
                switch (constrName) {
                    case HasPayload:
                        constraints.add(new HasPayload(entry.getValue(), planProfile));
                        break;
                    default:
                        try {
                            Class<?> clazz = Class.forName("pt.lsts.neptus.plugins.mvplanning.planning.constraints." + constrName);
                            Constructor<?> constructor = clazz.getConstructor(String.class);
                            constraints.add((TaskConstraint) constructor.newInstance(entry.getValue()));
                        } catch (ClassNotFoundException
                                | NoSuchMethodException
                                | IllegalAccessException
                                | InstantiationException
                                | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
    }
}
