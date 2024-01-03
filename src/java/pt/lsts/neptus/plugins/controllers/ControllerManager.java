/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 6, 2013
 */
package pt.lsts.neptus.plugins.controllers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.commons.io.output.ByteArrayOutputStream;

import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.FollowReference;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.Reference;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * This class is used to associate external controllers with existing vehicles and manage their control loops
 * @author zp
 */
public class ControllerManager {

    // Currently active controllers
    protected LinkedHashMap<String, IController> activeControllers = new LinkedHashMap<>();
        protected LinkedHashMap<String, Timer> timers = new LinkedHashMap<>();
    
    // Available controller classes
    protected Vector<Class<? extends IController>> controllers = new Vector<>();    
    protected boolean useAcousticComms = false;
    protected boolean debug = true;
    
    
    
    
    /**
     * Add a new controller class
     * @param cClass The class of the controller
     */
    public void addController(Class<? extends IController> cClass) {
        if (!controllers.contains(cClass))
            controllers.add(cClass);
    }
    
    protected void startTimers() {
        
    }
    
    public void stop() {
        
        if (debug)
        System.out.println("Stopping all controller manager timers");
        for (String v : activeControllers.keySet())
            activeControllers.get(v).stopControlling(VehiclesHolder.getVehicleById(v));
        
        activeControllers.clear();
        
        for (Timer t : timers.values()) {
            t.cancel();
            t.purge();
        }
        
        timers.clear();
    }
    
    /**
     * Create a control loop
     * @param controller The controller in charge
     * @param vehicle The vehicle being controlled
     * @param controlLatencySeconds 
     * @throws Exception
     */
    public void associateControl(final IController controller, final VehicleType vehicle, int controlLatencySeconds, int timeoutSeconds) throws Exception {
        EstimatedState lastState = ImcMsgManager.getManager().getState(vehicle).last(EstimatedState.class);
        if (!controller.supportsVehicle(vehicle, lastState)) {
            throw new Exception("The vehicle "+vehicle.getName()+" is not supported by "+controller.getControllerName()+" controller");
        }

        PlanControl startPlan = new PlanControl();
        startPlan.setType(TYPE.REQUEST);
        startPlan.setOp(OP.START);
        startPlan.setPlanId(controller.getControllerName());
        FollowReference man = new FollowReference();
        man.setControlEnt((short)255);
        man.setControlSrc(65535);
        man.setAltitudeInterval(1);
        man.setTimeout(timeoutSeconds);
        man.setLoiterRadius(7.5);
        
        PlanSpecification spec = new PlanSpecification();
        spec.setPlanId(controller.getControllerName());
        spec.setStartManId("external_control");
        PlanManeuver pm = new PlanManeuver();
        pm.setData(man);
        pm.setManeuverId("external_control");
        spec.setManeuvers(Arrays.asList(pm));
        startPlan.setArg(spec);
        int reqId = 0;
        startPlan.setRequestId(reqId);
        startPlan.setFlags(0);

        ImcMsgManager.getManager().sendMessageToSystem(startPlan, vehicle.getId());
        
/*        if (useAcousticComms) {
            AcousticOperation op = new AcousticOperation();
            op.setOp(AcousticOperation.OP.MSG);
            op.setSystem(vehicle.getId());
            op.setMsg(startPlan);
        }*/
        
        if (debug)
            System.out.println(controller.getControllerName()+" is now controlling "+vehicle.getId());
        
        controller.startControlling(vehicle, lastState);
        activeControllers.put(vehicle.getName(), controller);
        TimerTask task = new TimerTask() {            
            @Override
            public void run() {
                EstimatedState state = ImcMsgManager.getManager().getState(vehicle.getId()).last(EstimatedState.class);
                FollowRefState frefState = ImcMsgManager.getManager().getState(vehicle.getId()).last(FollowRefState.class);
                Reference ref = controller.guide(vehicle, state, frefState);
                try {
                    System.out.println("size in bytes of the reference message: "+ ref.serialize(new IMCOutputStream(new ByteArrayOutputStream(256))));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                if (useAcousticComms) {
                    // alwas try to send using wifi
                    ImcMsgManager.getManager().sendMessageToSystem(ref, vehicle.getId());
                    AcousticOperation op = new AcousticOperation();
                    op.setOp(AcousticOperation.OP.MSG);
                    op.setSystem(vehicle.getId());
                    op.setMsg(ref);

                    ImcSystem[] sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                            SystemTypeEnum.ALL, true);

                    if (sysLst.length == 0) {
                        NeptusLog.pub().error("Cannot send reference acoustically because no system is capable of it");
                        return;
                    }
                    
                    int successCount = 0;

                    for (ImcSystem sys : sysLst) {
                        if (ImcMsgManager.getManager().sendMessage(op, sys.getId(), null)) {
                            successCount++;
                            NeptusLog.pub().warn("Sent reference to "+vehicle.getId()+" acoustically via "+ sys.getName());
                        }
                    }
                    if (successCount == 0) {
                        NeptusLog.pub().error("Cannot send reference acoustically because no system is capable of it");
                    }
                }
                else {
                    ImcMsgManager.getManager().sendMessageToSystem(ref, vehicle.getId());
                }
            }
        };
        
        Timer t = new Timer(controller.getControllerName()+" control timer", true);
        long time = controlLatencySeconds * 1000L;
        t.schedule(task, 0, time);
        timers.put(vehicle.getId(), t);
    }
    
    public void dissociateControl(VehicleType vehicle) {
        IController controller = activeControllers.get(vehicle.getId());
        
        if (controller != null) {             
            activeControllers.get(vehicle.getId()).stopControlling(vehicle);
            activeControllers.remove(vehicle.getId());
        }
        else {
            return;
        }

        PlanControl stopPlan = new PlanControl();
        stopPlan.setType(TYPE.REQUEST);
        stopPlan.setOp(OP.STOP);
        
        ImcMsgManager.getManager().sendMessageToSystem(stopPlan, vehicle.getId());
        
        if (debug)
            System.out.println(controller.getControllerName()+" stopped controlling "+vehicle.getId());
        
        if (timers.containsKey(vehicle.getId()))
            timers.get(vehicle.getId()).cancel();
        timers.remove(vehicle.getId());
    }

    public void setUseAcousticComms(boolean useAcousticComms) {
        this.useAcousticComms = useAcousticComms;
    }    
}
