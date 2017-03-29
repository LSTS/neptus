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

import java.util.Collections;
import java.util.List;

import pt.lsts.neptus.nvl.runtime.Availability;
import pt.lsts.neptus.nvl.runtime.Position;
import pt.lsts.neptus.types.mission.plan.PlanCompatibility;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.nvl.runtime.NVLVehicle;
import pt.lsts.neptus.nvl.runtime.NVLVehicleType;
import pt.lsts.neptus.nvl.runtime.PayloadComponent;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;

/**
 * @author keila
 *
 */
public class NeptusVehicleAdapter implements NVLVehicle {

    private final ImcSystem imcsystem;
    private final STATE state;
  
    public NeptusVehicleAdapter(ImcSystem imcData,STATE s) {
        imcsystem = imcData;
        state = s;
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
    public NVLVehicleType getType() {
        switch(imcsystem.getTypeVehicle()) {
            case UAV:
                return NVLVehicleType.UAV;
            case UUV:
                return NVLVehicleType.AUV;
            case USV:
                return NVLVehicleType.ASV; //TODO ??
            case UGV:
            case ALL:
            case UNKNOWN:
            default:
                return NVLVehicleType.ANY ; //TODO
            
        }
        
    }

    /* (non-Javadoc)
     * @see nvl.Vehicle#getAvailability()
     */
    @Override
    public Availability getAvailability() {
        switch(state) {
            case BOOT:
            case CALIBRATION:
            case MANEUVER:
            case TELEOPERATION:
            case EXTERNAL:
                return Availability.BUSY;
            case SERVICE:
                return Availability.AVAILABLE;               
            case CONNECTED:                
            case DISCONNECTED:                
            case ERROR:
            default:
                return Availability.NOT_OPERATIONAL;

        }
    }

    /* (non-Javadoc)
     * @see nvl.Vehicle#getPosition()
     */
    @Override
    public Position getPosition() {
        return new NeptusPositionAdapter(imcsystem.getLocation());
    }

    /* (non-Javadoc)
     * @see nvl.Vehicle#getPayload()
     */
    @Override
    public List<PayloadComponent> getPayload() {
        //TODO
        PlanCompatibility.availablePayloads(VehiclesHolder.getVehicleById(this.getId()));
        return Collections.emptyList(); 



    }

}
