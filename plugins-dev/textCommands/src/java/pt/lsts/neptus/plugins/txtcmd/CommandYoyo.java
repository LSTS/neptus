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
 * Author: zp
 * Feb 24, 2014
 */
package pt.lsts.neptus.plugins.txtcmd;

import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.templates.PlanCreator;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class CommandYoyo extends AbstractTextCommand {

    @NeptusProperty(description="Survey center (the vehicle will move around this point)")
    LocationType dest = new LocationType();
    
    @NeptusProperty(description="Side length of the survey (in meters)")
    double size=50;
    
    @NeptusProperty(description="Vehicle speed during the survey")
    SpeedType speed = new SpeedType(1.1, Units.MPS);
    
    @NeptusProperty(description="Speed of the point of interest towards North")
    double vn = 0;
    
    @NeptusProperty(description="Speed of the point of interest towards East")
    double ve = 0;    
    
    @NeptusProperty(description="Maximum depth for the survey, in meters")
    double maxdepth = 4;
    
    @NeptusProperty(description="Minimum depth for the survey, in meters")
    double minDepth = 1;
    
    @NeptusProperty(description="Pitch angle in degress to use for the survey")
    double pitch = 15;
    
    @NeptusProperty(description="Rotation of the survey, in degrees")
    double rot = 0;
    
    @NeptusProperty(description="Ammount of time to wait at surface on popups (0 means no popups)")
    double popup = 0;    
    
    @Override
    public String getCommand() {
        return "yoyo";
    }
    
    @Override
    public PlanType resultingPlan(MissionType mt) {
        PlanCreator planCreator = new PlanCreator(mt);
        planCreator.setSpeed(speed);
        
        LocationType center = new LocationType(dest);
        double radius = Math.sqrt((size * size)/2);
        double ang = Math.toRadians(45 + rot);
        double time = 0;
        planCreator.setLocation(center);
        planCreator.move(Math.sin(ang) * radius + vn * time, Math.cos(ang) * radius + ve * time);
        planCreator.setDepth(minDepth);
        planCreator.addGoto(null);
        if (popup > 0) {
            planCreator.setDepth(0);
            planCreator.addManeuver("PopUp", "duration", popup, "radius", 20);
        }
        double amplitude = (maxdepth - minDepth) / 2;
        double depth = (maxdepth + minDepth) / 2;
        
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
        
        PlanType pt = planCreator.getPlan();
        pt.setId("yoyo");
        return pt;
    }

    @Override
    public void setCenter(LocationType loc) {
        dest = new LocationType(loc);
    }

    public static void main(String[] args) {
        CommandYoyo gt = new CommandYoyo();
        PluginUtils.editPluginProperties(gt, true);
        System.out.println(gt.buildCommand());
    }
}
