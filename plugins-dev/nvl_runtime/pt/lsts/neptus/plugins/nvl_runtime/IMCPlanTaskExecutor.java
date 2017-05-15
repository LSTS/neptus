/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: edrdo
 * May 14, 2017
 */
package pt.lsts.neptus.plugins.nvl_runtime;

import java.util.List;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.nvl.runtime.NVLExecutionException;
import pt.lsts.nvl.runtime.NVLVariable;
import pt.lsts.nvl.runtime.NVLVehicle;
import pt.lsts.nvl.runtime.tasks.CompletionState;
import pt.lsts.nvl.runtime.tasks.Task;
import pt.lsts.nvl.runtime.tasks.TaskExecutor;


public class IMCPlanTaskExecutor extends TaskExecutor {

    private static final double WARMUP_TIME = 3.0;
    private static final double PLAN_CONTROL_STATE_TIMEOUT = 5.0;

    private NeptusVehicleAdapter vehicle;
    private NVLVariable<PlanControlState> pcsVar;

    public IMCPlanTaskExecutor(IMCPlanTask theTask) {
        super(theTask);
    }


    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.tasks.TaskExecutor#onInitialize(java.util.Map)
     */
    @Override
    protected void onInitialize(Map<Task, List<NVLVehicle>> allocation) {
        vehicle = (NeptusVehicleAdapter) allocation.get(getTask()).get(0);
        pcsVar = new NVLVariable<>();
    }

    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.tasks.TaskExecutor#onStart()
     */
    @Override
    protected void onStart() {
        IMCPlanTask task = (IMCPlanTask) getTask();
        PlanControl pcMsg = new PlanControl();
        pcMsg.setType(PlanControl.TYPE.REQUEST);
        pcMsg.setOp(PlanControl.OP.START);
        pcMsg.setPlanId(task.getId());
        pcMsg.setArg(task.asIMCPlan());
        pcMsg.setRequestId(IMCSendMessageUtils.getNextRequestId());
        pcMsg.setFlags(PlanControl.FLG_CALIBRATE);
        vehicle.sendTo(pcMsg);
        NeptusEvents.register(this);
        d("Initialized executor for IMC plan '%s'", task.getId());
    }

    @Subscribe
    public void on(PlanControlState pcs) {
        d("Received PCS " + pcs.getSourceName() + " " + vehicle.getId());

        if (pcs.getSourceName().equals(vehicle.getId())) {
            pcsVar.set(pcs, timeElapsed());
        }
    }


    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.tasks.TaskExecutor#onStep()
     */
    @Override
    protected CompletionState onStep() {
        CompletionState completionState =  new CompletionState(CompletionState.Type.IN_PROGRESS);
        if (! pcsVar.hasFreshValue()) {
            if (pcsVar.age(timeElapsed()) >= PLAN_CONTROL_STATE_TIMEOUT) {
                d("PlanControlState timeout!");
                completionState = new CompletionState(CompletionState.Type.ERROR);
            }
        } else {
            PlanControlState pcs = pcsVar.get();
            if (!getTask().getId().equals(pcs.getPlanId())) {
                if (timeElapsed() > WARMUP_TIME) {
                    completionState = new CompletionState(CompletionState.Type.ERROR);
                    d("Wrong plan id: %s != %s", pcs.getPlanId(), getTask().getId());
                }
            } else {
                switch (pcs.getState()) {
                    case BLOCKED:
                        completionState = new CompletionState(CompletionState.Type.ERROR);
                        break;
                    case EXECUTING:
                    case INITIALIZING:
                        break;
                    case READY:
                        d("Terminated %s on %s : %s", getTask().getId(), vehicle.getId(), pcs.getLastOutcome());
                        switch (pcs.getLastOutcome()) {
                            case FAILURE:
                            case NONE:
                                completionState = new CompletionState(CompletionState.Type.ERROR);
                                d("Failure!");
                                break;
                            case SUCCESS:
                                completionState = new CompletionState(CompletionState.Type.DONE);
                                d("IMC plan completed!");
                                break;
                            default:
                                throw new NVLExecutionException();
                        }
                        break;
                    default:
                        throw new NVLExecutionException();
                }
            }
        }
        return completionState;
    }


    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.tasks.TaskExecutor#onCompletion()
     */
    @Override
    protected void onCompletion() {
       //  NeptusEvents.unregister(this, null);
    }

}
