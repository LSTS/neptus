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
 * Author: José Pinto
 * 2010/01/18
 */
package pt.lsts.neptus.mp.templates;

import java.util.LinkedHashMap;

import com.l2fprod.common.propertysheet.DefaultProperty;

import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zepinto
 *
 */
@PluginDescription(name="YoYo Survey", author="ZP", description="Survey the water column around a moving point")
public class YoYoTemplate extends AbstractPlanTemplate {

    @NeptusProperty(name="Center location", description="The center of the survey (point of interest)")
    public LocationType loc = new LocationType();

    @NeptusProperty(name="Size", description="Length, in meters of the side length of the survey")
    public double size = 100;

    @NeptusProperty(name="Maximum Depth", description="Maximum depth of the yoyo behavior")
    public double maxdepth = 20;

    @NeptusProperty(name="Minimum Depth", description="Maximum depth of the yoyo behavior")
    public double mindepth = 2;

    @NeptusProperty(name="Speed", description="Travelling speed")
    public SpeedType speed = new SpeedType(1.1, Units.MPS);
    
    @NeptusProperty(name="Rotation", description="Rotation of the survey square, in degrees")
    public double rot = 0;
    
    @NeptusProperty(name="Movement towards north", description="Velocity of the point of interest")
    public double vn = 0;
    
    @NeptusProperty(name="Movement towards east", description="Velocity of the point of interest")
    public double ve = 0;

    @NeptusProperty(name="Popup duration", description="Duration of the popups at corners")
    public int popup = 60;
    
    @NeptusProperty(name="Pitch angle", description="Pitch angle to be used in yoyo maneuvers (degrees)")
    public double pitch = 15;
    
    @Override
    public DefaultProperty[] getProperties() {
        if (loc.getDistanceInMeters(LocationType.ABSOLUTE_ZERO) == 0 && mission != null)
            loc.setLocation(IMCUtils.lookForStartPosition(mission));			
        return super.getProperties();
    }

    protected static final int DIR_YUP = 0, DIR_X1 = 1, DIR_YDOWN = 2, DIR_X2 = 3;

    public PlanType generatePlan() throws Exception {
        
        PlanCreator planCreator = new PlanCreator(mission);
        planCreator.setSpeed(speed);
        
        LocationType center = new LocationType(loc);
        double radius = Math.sqrt((size * size)/2);
        double ang = Math.toRadians(45 + rot);
        double time = 0;
        planCreator.setLocation(center);
        planCreator.move(Math.sin(ang) * radius + vn * time, Math.cos(ang) * radius + ve * time);
        planCreator.setDepth(mindepth);
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("timeout", 45);
        planCreator.addGoto(map);
        if (popup > 0) {
            planCreator.setDepth(0);
            planCreator.addManeuver("PopUp", "duration", popup, "radius", 20);
        }
        double amplitude = (maxdepth - mindepth) / 2;
        double depth = (maxdepth + mindepth) / 2;
        
        for (int i = 0; i < 4; i++) {
            time += size * speed.getMPS();
            ang = Math.toRadians(i * 90 + 135 + rot);
            planCreator.setLocation(center);
            planCreator.move(Math.sin(ang) * radius + time * vn, Math.cos(ang) * radius + time * ve);
            planCreator.setDepth(depth);
            planCreator.addManeuver("YoYo", "amplitude", amplitude, "pitchAngle", Math.toRadians(pitch));
            if (popup > 0) {
                planCreator.setDepth(0);
                planCreator.addManeuver("PopUp", "duration", popup, "radius", 20);
            }
        }
        
        return planCreator.getPlan();
    }
}
