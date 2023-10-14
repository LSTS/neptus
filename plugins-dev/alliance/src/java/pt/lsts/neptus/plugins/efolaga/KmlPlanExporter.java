/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 25, 2014
 */
package pt.lsts.neptus.plugins.efolaga;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Locale;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.IPlanFileExporter;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 */
@PluginDescription
public class KmlPlanExporter implements IPlanFileExporter {

    @Override
    public String getExporterName() {
        return "KML (eFolaga)";
    }
    
    private String placemark(String id, ManeuverLocation loc) {
        loc.convertToAbsoluteLatLonDepth();
        StringBuilder sb = new StringBuilder();
        sb.append("  <Placemark>\n");
        sb.append("    <name>"+id+"</name>\n");
        sb.append("    <description>going-to.string=FIXED_CONSTANT_DEPTH_UPDATES=depth="+Math.abs(loc.getZ())+"</description>\n");
        sb.append("    <Point>\n");
        sb.append(String.format(Locale.US, "      <coordinates>%.5f,%.5f,0.</coordinates>\n", loc.getLongitudeDegs(), loc.getLatitudeDegs()));
        sb.append("    </Point>\n");
        sb.append("  </Placemark>\n");
        return sb.toString();
    }

    @Override
    public void exportToFile(PlanType plan, File out, ProgressMonitor monitor) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        writer.write(header(plan));
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            switch (m.getType()) {
                case "Goto":
                    writer.write(placemark(m.getId(), ((Goto)m).getEndLocation()));
                    break;
                default:
                    break;
            }
        }
        
        writer.write(footer(plan));
        writer.close();
    }
    
    private String header(PlanType plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append("<kml xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\">\n");
        sb.append("<Document>\n");
        sb.append("  <name>Neptus plan '"+plan.getId()+"'</name>\n");
        return sb.toString();
    }
    
    private String footer(PlanType plan) {
        return "</Document>\n</kml>\n";
    }
    
    @Override
    public String[] validExtensions() {
        return new String[] {"kml"};
    }
    
    public static void main(String[] args) throws Exception {
        KmlPlanExporter exporter = new KmlPlanExporter();
        MissionType mt = new MissionType("/home/zp/workspace/neptus/missions/APDL/missao-apdl.nmisz");
        PlanType pt = mt.getIndividualPlansList().get("folaga_plan");
        
        exporter.exportToFile(pt, new File("/home/zp/Desktop/out.kml"), null);
    }
}
