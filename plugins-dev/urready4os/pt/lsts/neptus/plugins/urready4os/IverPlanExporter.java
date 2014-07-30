/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 21, 2014
 */
package pt.lsts.neptus.plugins.urready4os;

import java.io.File;
import java.util.Collection;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.IPlanFileExporter;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
@PluginDescription
public class IverPlanExporter implements IPlanFileExporter  {

    private String iverWaypoint(int wptNum, double speedMps, double prevLength, double yoyoAmplitude, ManeuverLocation prev, ManeuverLocation dst) {
        //2; 37.554823; -1.063032; 5774.552; 270.02;  U32.8,-32.8,25.0 P0 PT25.0 VC1,0,0,1000,0,VC2,0,0,1000,0 S2; 0;-1
        
        StringBuilder sb = new StringBuilder();
        sb.append(wptNum+"; ");
        dst.convertToAbsoluteLatLonDepth();
        sb.append(dst.getLatitudeDegs()+"; ");
        sb.append(dst.getLongitudeDegs()+"; ");
        if (prev == null) {
            sb.append("0.0; ");
            sb.append("0.0; ");
        }
        else {
            sb.append(prev.getDistanceInMeters(dst)+"; ");
            sb.append(Math.toDegrees(prev.getXYAngle(dst))+"; ");
        }
        
        if (yoyoAmplitude == 0) {
        
            switch (dst.getZUnits()) {
                case DEPTH:
                    sb.append("D"+dst.getZ()+" ");
                    break;
                case ALTITUDE:
                    sb.append("H"+dst.getZ()+" ");
                    break;
                default:
                    sb.append("D0 ");
                    break;
            }       
        }
        else {
            sb.append("U"+(dst.getZ() - yoyoAmplitude)+","+(dst.getZ() + yoyoAmplitude)+",25.0 ");            
        }
        
        sb.append("P0 PT25 VC1,0,0,1000,0,VC2,0,0,1000,0 S2; 0;-1");
        
        return sb.toString();
    }

    @Override
    public String getExporterName() {
        return ".mis Mission File";
    }

    @Override
    public void exportToFile(PlanType plan, File out) throws Exception {
        Collection<ManeuverLocation> wpts = PlanUtil.getPlanWaypoints(plan);
        ManeuverLocation prev = null;
        int count = 1;
        
        for (ManeuverLocation l : wpts) {
            System.out.println(iverWaypoint(count++, 1.0, 0, 0, prev, l));
            prev = l;
        }
        
        //TODO
    }


    @Override
    public String[] validExtensions() {
        return new String[] {"mis"};
    }
}
