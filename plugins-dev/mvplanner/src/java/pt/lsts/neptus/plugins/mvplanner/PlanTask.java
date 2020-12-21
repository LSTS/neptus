/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: tsm
 * 19 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Date;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.plugins.mvplanner.jaxb.Profile;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.mission.plan.PlanType;

public abstract class PlanTask {
    /**
     * Possible states that a task can be in.
     *
     * Waiting: Task has been created but not allocated yet.
     * Allocated: Task has been allocated and is being executed.
     * Completed: Task was completed successfully.
     * Interrupted: Task execution was interrupted and needs to
     * be re-allocated.
     * */
    public enum TaskStateEnum {
        Waiting,
        Allocated,
        Completed,
        Interrupted
    }

    /**
     * Which type of task this is
     * */
    public enum TaskTypeEnum {
        Survey,
        NeptusTask
    }

    /** Map object that represents this task **/
    protected PolygonType object;

    /** Display color of this task. It will depend on task state **/
    protected Color objectColor;

    /** Vehicle to which this task has been allocated to. Null if no vehicle yet **/
    protected String vehicleId;

    /** Which state this task is in **/
    protected TaskStateEnum taskState;

    /** Profile associated to this task  **/
    protected Profile taskPofile;

    /** Plan generated for this task **/
    protected PlanType plan;

    /** Completion percentage **/
    protected double completion;

    protected Date startDate;

    public PlanTask(PolygonType object, Profile taskProfile) {
        this.object = object;
        setWaitingState();
        vehicleId = null;
        this.taskPofile = taskProfile;
        completion = 0;
        startDate = null;
    }

    public PlanTask(PlanType plan) {
        this.plan = plan;
        setWaitingState();
        vehicleId = null;
        this.taskPofile = null;
        completion = 0;
        startDate = null;
    }

    /**
     * Display the task on the map according to its type
     * and allocation state.
     *
     * Orange for waiting state
     * Light gray for completed state
     * Dark green for allocated
     * Dark red for interrupted
     * */
    public abstract void paintTask(Graphics2D g, StateRenderer2D source);

    public abstract TaskTypeEnum getTaskType();

    public String getId() {
        if(object != null)
            return object.getId();

        return plan.getId();
    }

    /**
     * Get vehicle ID to which the task
     * has been allocated to
     * */
    public String getAllocatedVehicle() {
        return vehicleId;
    }

    /**
     * Set time at which this task should start
     * */
    public void setStartDate(Date start) {
        startDate = start;
    }

    public TaskStateEnum getState() {
        return taskState;
    }

    /**
     * Get start time of this task.
     * If not value was set it will return
     * the current time.
     * */
    public Date getStartTime() {
        if(startDate == null)
            return new Date();
        return startDate;
    }

    /**
     * Set the current completion percentage of this task
     * */
    public void setCompletion(double completion) {
        this.completion = completion;

        if(completion == 100)
            setCompleted();
    }

    /**
     * Set this task's state as waiting for allocation
     * */
    public void setWaitingState() {
        this.taskState = TaskStateEnum.Waiting;
        Color orange = Color.ORANGE.darker();
        objectColor = new Color(orange.getRed(), orange.getGreen(), orange.getBlue(), 100);
    }

    /**
     * Mark this task as completed
     * */
    public void setCompleted() {
        this.taskState = TaskStateEnum.Completed;
        objectColor = Color.LIGHT_GRAY;
    }

    /**
     * Mark this task as allocated to the given
     * vehicle
     * */
    public void setAllocated(String vehicleId) {
        this.taskState = TaskStateEnum.Allocated;
        this.vehicleId = vehicleId;
        Color green = Color.GREEN.darker();
        objectColor = new Color(green.getRed(), green.getGreen(), green.getBlue(), 100);
    }

    /**
     * Mark this task as interrupted
     * */
    public void setInterrupted() {
        this.taskState = TaskStateEnum.Allocated;
        this.vehicleId = null;
        Color red = Color.RED.darker();
        objectColor = new Color(red.getRed(), red.getGreen(), red.getBlue(), 100);
    }

    /**
     * When time comes to allocate this task, do it
     * to the given vehicle
     * */
    public void associateVehicle(String vehicleId) {
        this.vehicleId = vehicleId;

        if(plan != null)
            plan.setVehicle(vehicleId);
    }

    public void associatePlan(PlanType plan) {
        this.plan = plan;

        if(this.vehicleId != null)
            this.plan.setVehicle(vehicleId);
    }

    public PlanSpecification asIMCPlan() {
        return (PlanSpecification) IMCUtils.generatePlanSpecification(plan);
    }

    public PlanType asPlanType() {
        return plan;
    }

    /**
     * Get this task's location. If the task has more than
     * one point, then the location returned is its center
     * */
    public LocationType getLocation() {
        return object.getCentroid();
    }

    public Profile getProfile() {
        return this.taskPofile;
    }
}
