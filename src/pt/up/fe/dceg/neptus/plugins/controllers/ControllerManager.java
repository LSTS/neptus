/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Oct 6, 2013
 */
package pt.up.fe.dceg.neptus.plugins.controllers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.FollowReference;
import pt.up.fe.dceg.neptus.imc.PlanControl;
import pt.up.fe.dceg.neptus.imc.PlanManeuver;
import pt.up.fe.dceg.neptus.imc.PlanSpecification;
import pt.up.fe.dceg.neptus.imc.PlanControl.OP;
import pt.up.fe.dceg.neptus.imc.PlanControl.TYPE;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * This class is used to associate external controllers with existing vehicles and manage their control loops
 * @author zp
 */
public class ControllerManager extends Thread {

    // Currently active controllers
    protected LinkedHashMap<String, IController> activeControllers = new LinkedHashMap<>();
    
    // Available controller classes
    protected Vector<Class<? extends IController>> controllers = new Vector<>();    
    
    protected boolean debug = true;
    
    {
        setDaemon(true);
    }
    
    /**
     * Add a new controller class
     * @param cClass The class of the controller
     */
    public void addController(Class<? extends IController> cClass) {
        if (!controllers.contains(cClass))
            controllers.add(cClass);
    }
    
    /**
     * Create a control loop
     * @param controller The controller in charge
     * @param vehicle The vehicle being controlled
     * @param controlLatencySeconds 
     * @throws Exception
     */
    public void associateControl(IController controller, VehicleType vehicle, int controlLatencySeconds) throws Exception {
        EstimatedState lastState = ImcMsgManager.getManager().getState(vehicle).lastEstimatedState();
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
        man.setAltitudeInterval(2);
        man.setTimeout(controlLatencySeconds * 5);

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
        
        if (debug)
            System.out.println(controller.getControllerName()+" is now controlling "+vehicle.getId());
        
        controller.startControlling(vehicle, lastState);
        activeControllers.put(vehicle.getName(), controller);
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
    }    
    
    
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                System.err.println("Controller manager stopped.");
                return;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            
//            for (IController c : activeControllers.values()) {
                
//            }            
        }
    }
}
