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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 13/10/2016
 */
package org.necsave;

import java.util.LinkedHashMap;
import java.util.Vector;

import info.necsave.msgs.Area;
import info.necsave.msgs.Behavior;
import info.necsave.msgs.BehaviorScanArea;
import info.necsave.msgs.MeshState;
import info.necsave.msgs.MissionArea;
import info.necsave.msgs.Plan;
import info.necsave.msgs.PlatformPlan;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class MeshStateWrapper {

    public final int numPlatforms;
    public final int numAreas;
    public final int allocation[];
    
    public MeshStateWrapper(MeshState state) {
        byte[] data = state.getState();
        numPlatforms = data[0];
        numAreas = data[1];
        allocation = new int[numAreas];
        for (int i = 0; i < numAreas; i++)
            allocation[i] = data[i+2];        
    }
    
    public Plan generatePlan(MissionArea area, int subareas) {
        Plan p = new Plan();
        p.setPlanId(1);
        LinkedHashMap<Integer, Vector<Behavior>> plans = new LinkedHashMap<>();
        for (int i = 0; i < numAreas; i++) {
            int platform = allocation[i];
            if (platform < 0)
                continue;
            
            if (!plans.containsKey(platform)) {
                plans.put(platform, new Vector<>());
            }
            
            Vector<Behavior> behaviors = plans.get(platform);
            LocationType loc = new LocationType();
            loc.setLatitudeRads(area.getArea().getLatitude());
            loc.setLongitudeRads(area.getArea().getLongitude());
            
            double ang = area.getArea().getBearing();
            double width = (i/subareas) * area.getArea().getWidth() / (numAreas/subareas);
            double length = (i%subareas) * area.getArea().getLength() / subareas;
            LocationType another = new LocationType(loc);
            another.setAzimuth(Math.toDegrees(ang));
            another.setOffsetDistance(length);
            another.convertToAbsoluteLatLonDepth();
            another.setAzimuth(Math.toDegrees(ang+(Math.PI/2)));
            another.setOffsetDistance(width);
            another.convertToAbsoluteLatLonDepth();
            
            BehaviorScanArea scanArea = new BehaviorScanArea();
            Area a = new Area();
            a.setAreaId(i);
            a.setLatitude(another.getLatitudeRads());
            a.setLongitude(another.getLongitudeRads());
            a.setWidth(area.getArea().getWidth() / (numAreas/subareas));
            a.setLength(area.getArea().getLength() / subareas);
            a.setBearing(area.getArea().getBearing());
            scanArea.setScanArea(a);
            scanArea.setPlatformId(platform);
            scanArea.setBehaviorId(i);
            behaviors.add(scanArea);
        }
        
        Vector<PlatformPlan> planList = new Vector<>();
        
        for (int platform : plans.keySet()) {
            PlatformPlan plan = new PlatformPlan();
            plan.setPlanId(platform);
            plan.setPlatformId(platform);
            plan.setBehaviors(plans.get(platform));
            planList.add(plan);
        }
        
        p.setPlatformPlans(planList);
        
        return p;
    }
}
