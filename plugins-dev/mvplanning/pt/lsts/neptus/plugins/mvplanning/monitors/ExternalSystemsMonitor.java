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
 * 19 July 2016
 */

package pt.lsts.neptus.plugins.mvplanning.monitors;

import com.jogamp.common.util.ArrayHashSet;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.plugins.mvplanning.PlanAllocator;
import pt.lsts.neptus.plugins.mvplanning.PlanGenerator;
import pt.lsts.neptus.plugins.mvplanning.events.MvPlanningNoSafeDistanceEvent;
import pt.lsts.neptus.plugins.mvplanning.interfaces.AbstractSupervisor;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.utils.ExternalSystemsSimulator;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;

import java.util.List;

/**
 * Module that monitors external systems and issues
 * a warning, to the allocator, if any one gets too close
 * to a current plan
 * @author tsmarques
 * @date 6/19/16
 */
public class ExternalSystemsMonitor extends AbstractSupervisor implements IPeriodicUpdates {
    /* in meters */
    private final double SAFE_DISTANCE = 100;
    private List<String> quarantine;
    private ExternalSystemsSimulator extSysSimulator;

    public ExternalSystemsMonitor(ConsoleAdapter console, PlanAllocator pAlloc, PlanGenerator pGen, boolean debugMode) {
        super(console, pAlloc, pGen);
        quarantine = new ArrayHashSet<>();

        if(debugMode) {
            extSysSimulator = new ExternalSystemsSimulator();
            console.registerToEventBus(extSysSimulator);
        }
    }

    @Override
    public long millisBetweenUpdates() {
        return 5000;
    }

    @Override
    public boolean update() {
        for(ExternalSystem extSys : ExternalSystemsHolder.lookupActiveSystemVehicles()) {
            for(ImcSystem sys : ImcSystemsHolder.lookupActiveSystemVehicles()) {
                boolean safeDistance = iswithinSafeDistance(extSys, sys);
                boolean inQuarantine = quarantine.contains(extSys.getId());

                if(!safeDistance && !inQuarantine) {
                    console.post(new MvPlanningNoSafeDistanceEvent(extSys, sys));
                    NeptusLog.pub().warn("External system with id " + extSys.getId() + " too close to " + sys.getName());

                    quarantine.add(extSys.getId());
                }
                else if(safeDistance && inQuarantine)
                    quarantine.remove(extSys.getId());
            }
        }
        return true;
    }

    /**
     * If the given external system is not
     * too close to the given vehicle
     * */
    private boolean iswithinSafeDistance(ExternalSystem extSys, ImcSystem vehicle) {
        return extSys.getLocation().getDistanceInMeters(vehicle.getLocation()) > SAFE_DISTANCE;
    }
}
