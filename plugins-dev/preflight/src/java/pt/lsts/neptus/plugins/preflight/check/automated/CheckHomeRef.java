/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 31 Mar 2015
 */
package pt.lsts.neptus.plugins.preflight.check.automated;

import java.util.Collection;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.preflight.Preflight;
import pt.lsts.neptus.plugins.preflight.check.WithinRangeCheck;
import pt.lsts.neptus.plugins.preflight.utils.PlanState;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class CheckHomeRef extends WithinRangeCheck {
    private HomeReference home;
    private double homeLat;
    private double homeLong;
    private boolean validated;
    
    public CheckHomeRef() {
        super("Home distance", "Planning");
        home = Preflight.CONSOLE.getMission().getHomeRef();
        homeLat = home.getLatitudeRads();
        homeLong = home.getLongitudeRads();
        validated = false;
    }

    @Override
    @Periodic(millisBetweenUpdates = 5000)
    public void validateCheck() {
        if(!PlanState.existsLocally("lost_comms")) {
            setValuesLabelText("?");
            setState(NOT_VALIDATED);
            validated = false;
        }
        else if(!PlanState.isSynchronized("lost_comms")) {
            setValuesLabelText("Not synchronised");
            setState(NOT_VALIDATED);
            validated = false;
        }
        else {
            if(PlanState.isEmpty("lost_comms")) {
                setValuesLabelText("?");
                setState(NOT_VALIDATED);
                validated = false;
            }
            else {
                if(homeRefChanged() || !validated) {
                    if(maneuversWithinRange()) {
                        setValuesLabelText("[<" + getMaxValue() + "m]");
                        setState(VALIDATED);
                    }
                    else { /* lost_comms too far from home reference */
                        setValuesLabelText("[>" + getMaxValue() + "m]");
                        setState(NOT_VALIDATED);
                        validated = false;
                    }
                    updateHomeRef();
                    validated = true;
                }                    
            }
        }
    }
    
    @Override
    protected double getMaxValue() {
        return 600; /* meters */
    }

    @Override
    protected double getMinValue() {
        return 0;
    }
    
    @Override
    protected boolean isWithinRange(double value) {
        return(value >= getMinValue() && value <= getMaxValue());
    }

    /* Check distance of each lost_comms maneuver to home reference */
    private boolean maneuversWithinRange() {
        PlanType lostComms = Preflight.CONSOLE.
                getMission().
                    getIndividualPlansList().
                        get("lost_comms");
          
        Collection<ManeuverLocation> planPath = PlanUtil.getPlanWaypoints(lostComms);

        for(LocationType loc : planPath)
            if(!isWithinRange(home.getDistanceInMeters(loc)))
                return false;

        return true;
    }
    
    private boolean homeRefChanged() {
        HomeReference currHome = Preflight.CONSOLE.getMission().getHomeRef();
        double currHomeLat = currHome.getLatitudeRads();
        double currHomeLong = currHome.getLongitudeRads();
        
        if((currHomeLat != homeLat) || (currHomeLong != homeLong))
            return true;
        return false;
            
    }
    
    private void updateHomeRef() {
        HomeReference currHome = Preflight.CONSOLE.getMission().getHomeRef();
        homeLat = currHome.getLatitudeRads();
        homeLong = currHome.getLongitudeRads();
    }
}
