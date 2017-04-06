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
 * 09/03/2017
 */
package pt.lsts.neptus.plugins.nvl_runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.lsts.neptus.nvl.runtime.Availability;
import pt.lsts.neptus.nvl.runtime.PayloadComponent;
import pt.lsts.neptus.nvl.runtime.TaskSpecification;
import pt.lsts.neptus.nvl.runtime.VehicleRequirements;
import pt.lsts.neptus.types.mission.plan.PlanCompatibility;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author keila
 *
 */
public class NeptusTaskSpecificationAdapter implements TaskSpecification {

    private final PlanType plan;
    
   
    /**
     * Create a NVLTaskSpecification from Neptus PlanType
     * @param plan 
     * 
     */
    public NeptusTaskSpecificationAdapter(PlanType plan) {
        this.plan = plan;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.TaskSpecification#getRequirements()
     */
    @Override
    public List<VehicleRequirements> getRequirements() {
        
        List<PayloadComponent> payload = new ArrayList<>();
        for( String p: PlanCompatibility.payloadsRequired(plan)) {
            payload.add(new NeptusPayloadAdapter(p));
        }
        VehicleRequirements requirements = new VehicleRequirements();
        //requirements.setRequiredType(plan.getVehicleType());
        //requirements.setRequiredType(requiredType);
        
        requirements.setRequiredPayload(payload);
        requirements.setRequiredAvailability(Availability.AVAILABLE);
        return Arrays.asList(requirements);
    }
    
    //launch task/Area to Map

    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.TaskSpecification#getId()
     */
    @Override
    public String getId() {
        return plan.getId();
    }

    public PlanType getPlan() {
        return plan;
    }
    
    public PlanType mapArea() { //given some restrictions and a area to map 
                                //-> allocate to the selected vehicles distributing the load according to vehicles capabilities
        //TODO
        return null;
    }
}