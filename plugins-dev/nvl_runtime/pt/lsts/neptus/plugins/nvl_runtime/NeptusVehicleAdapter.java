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
 * Author: keila
 * 08/03/2017
 */
package pt.lsts.neptus.plugins.nvl_runtime;

import java.util.ArrayList;
import java.util.List;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanCompatibility;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.nvl.runtime.NVLVehicle;
import pt.lsts.nvl.runtime.PayloadComponent;
import pt.lsts.nvl.runtime.Position;
import pt.lsts.nvl.runtime.tasks.Task;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

public class NeptusVehicleAdapter implements NVLVehicle {

    private final ImcSystem imcsystem;
    private final List<PayloadComponent> availablePayload;
    private final String  acousticOPservice="acoustic/operation";

    public NeptusVehicleAdapter(ImcSystem imcData) {
        List<PayloadComponent> ps = new ArrayList<>();
        imcsystem = imcData;
        if(! imcData.getName().equals("caravela-aux"))
            for(String payload : PlanCompatibility.availablePayloads(VehiclesHolder.getVehicleById(getId()))) {
                ps.add(new NeptusPayloadAdapter(payload));
                //System.out.println("PlanCompatibility cicle: "+payload);
            } 
        if(! imcData.getName().equals("caravela-aux"))
            for(CommMean com : VehiclesHolder.getVehicleById(getId()).getCommunicationMeans().values()) { //IMC | HTTP | IRIDIUM | GSM
                ps.add(new NeptusPayloadAdapter(com.getName()));
                //System.out.println("Communications means: "+com.getName());
            }
        if(! imcData.getName().equals("caravela-aux"))
            if(hasAcoustics()){
                ps.add(new NeptusPayloadAdapter("Acoustics"));
                //System.out.println("Acoustic op service.");
            }
        availablePayload = ps;
    }

    public boolean hasAcoustics(){

        boolean activeSystems = false; //defines the vehicle payload even if it's not active
        ImcSystem[] vehicles = ImcSystemsHolder.lookupSystemByService(acousticOPservice,SystemTypeEnum.VEHICLE,activeSystems);
        for(ImcSystem vehicle: vehicles)
            if(vehicle.getName().equals(imcsystem.getName()))
                return true;
        return false;
    }


    public void sendTo(IMCMessage message) {
        ImcMsgManager.getManager().sendMessageToSystem(message, getId()); 
    }
    
    /* (non-Javadoc)
     * @see nvl.Vehicle#getId()
     */
    @Override
    public String getId() {
        return imcsystem.getName();
    }

    /* (non-Javadoc)
     * @see nvl.Vehicle#getType()
     */
    @Override
    public String getType() {
        return imcsystem.getTypeVehicle().toString();

    }


    /* (non-Javadoc)
     * @see nvl.Vehicle#getPosition()
     */
    @Override
    public Position getPosition() {
        LocationType loc = imcsystem.getLocation();
        return new Position(loc.getLatitudeRads(), loc.getLongitudeRads(), loc.getHeight());
    }

    /* (non-Javadoc)
     * @see nvl.Vehicle#getPayload()
     */
    @Override
    public List<PayloadComponent> getPayload() {
        return availablePayload;

    }

    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.NVLVehicle#getRunningTask()
     */
    @Override
    public Task getRunningTask() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.NVLVehicle#setRunningTask(pt.lsts.nvl.runtime.tasks.Task)
     */
    @Override
    public void setRunningTask(Task arg0) {
        // TODO Auto-generated method stub

    }
    
    public final ImcSystem getSystemHandle() {
        return imcsystem;
    }
}
