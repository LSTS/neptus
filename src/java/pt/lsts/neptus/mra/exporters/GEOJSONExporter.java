/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Mauro Brandão
 * Oct 15, 2015
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.util.WGS84Utilities;

/**
 * @author Mauro Brandão
 */
@PluginDescription(name="Export to GeoJson")
public class GEOJSONExporter implements MRAExporter {

    public double minHeight = 1000;
    public double maxHeight = -1;

    private IMraLogGroup source;

    public int secondsGapInEstimatedStateForPathBreak = 30;
    
    public GEOJSONExporter(IMraLogGroup source) {
        this.source = source;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    public String GeoJsonHeader() {
        String ret = "{ \"type\": \"FeatureCollection\",\"features\": [";

        return ret;
    }

    public String path(ArrayList<ManeuverLocation> coords, String name, String style) {
        String retAll = "";
        int idx = 0;
        while (idx < coords.size()) {
            String ret = "{\"type\": \"Feature\",\"geometry\": {\"type\": \"LineString\",\"coordinates\": [\n";
            
            LocationType l;
            
            for (l = coords.get(idx); idx < coords.size(); l = coords.get(idx), idx++) {
                if (l == null)
                    break;
                l.convertToAbsoluteLatLonDepth();
                ret += "["+l.getLongitudeDegs() + "," + l.getLatitudeDegs() + ","+(-l.getDepth())+"]";
                if(idx < coords.size()-1)  ret += ",\n";
            }
            ret += "]},\n";
            ret +="\"properties\": {";
            ret +="\"name\": \""+ name +"\",";
            Date d = new Date((long) (1000 * source.getLsfIndex().getStartTime()));
            ret +="\"description\": \"Plan executed on "+d+"\"";
            ret +="}";
            ret +="}";
            retAll += ret;
        }
        NeptusLog.pub().info("name: "+name);
        return retAll;
    }

    public String GeoJsonFooter() {
        return "]}";
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        try {
            pmonitor.setNote("Generating output dirs");
            File out = new File(source.getFile("mra"), "geojson");
            out.mkdirs();

            out = new File(out, "map.geojson");
            BufferedWriter bw = new BufferedWriter(new FileWriter(out));
            bw.write(GeoJsonHeader());

            // To account for multiple systems paths
            Hashtable<String, ArrayList<ManeuverLocation>> pathsForSystems = new Hashtable<>();
            Hashtable<String, IMCMessage> lastEstimatedStateForSystems = new Hashtable<>();
            
            LocationType bottomRight = null, topLeft = null;
            pmonitor.setProgress(50);
            // Path
            Iterable<IMCMessage> it = source.getLsfIndex().getIterator("EstimatedState", 0, 3000);
            pmonitor.setProgress(1);
            pmonitor.setNote("Generating path");
            double start = source.getLsfIndex().getStartTime();
            double end = source.getLsfIndex().getEndTime();
            for (IMCMessage s : it) {
                double progress = ((s.getTimestamp() - start) / (end - start)) * 30 + 1;
                pmonitor.setProgress((int)progress);
                ManeuverLocation loc = new ManeuverLocation(IMCUtils.parseLocation(s));
                loc.convertToAbsoluteLatLonDepth();

                int srcSys = s.getSrc();
                String systemName = source.getSystemName(srcSys);
                if (systemName == null || systemName.isEmpty()) {
                    continue;
                }
                ArrayList<ManeuverLocation> statesSys = pathsForSystems.get(systemName);
                if (statesSys == null) {
                    statesSys = new ArrayList<>();
                    pathsForSystems.put(systemName, statesSys);
                }
                IMCMessage lastEsSys = lastEstimatedStateForSystems.get(systemName);
                if (lastEsSys != null &&s.getTimestampMillis() -lastEsSys.getTimestampMillis() > secondsGapInEstimatedStateForPathBreak * 1E3) {
                    statesSys.add(null);
                }
                lastEstimatedStateForSystems.put(systemName, s);
                statesSys.add(loc);
                
                if (bottomRight == null) {
                    bottomRight = new LocationType(loc);
                    topLeft = new LocationType(loc);
                }

                if (loc.getLatitudeDegs() < bottomRight.getLatitudeDegs())
                    bottomRight.setLatitudeDegs(loc.getLatitudeDegs());
                else if (loc.getLatitudeDegs() > topLeft.getLatitudeDegs())
                    topLeft.setLatitudeDegs(loc.getLatitudeDegs());
                if (loc.getLongitudeDegs() < topLeft.getLongitudeDegs())
                    topLeft.setLongitudeDegs(loc.getLongitudeDegs());
                else if (loc.getLongitudeDegs() > bottomRight.getLongitudeDegs())
                    bottomRight.setLongitudeDegs(loc.getLongitudeDegs());
            }

            pmonitor.setProgress(60);
            pmonitor.setNote("Writing path to file");
            for (String sys : pathsForSystems.keySet()) {
                ArrayList<ManeuverLocation> st = pathsForSystems.get(sys);
                bw.write(path(st,sys, "estate"));
            }
            pmonitor.setProgress(70);
            PlanType plan = null;
            try {
                MissionType mt = LogUtils.generateMission(source);
                if (mt != null)
                    plan = LogUtils.generatePlan(mt, source);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
            pmonitor.setProgress(80);
            if (plan != null) {
                pmonitor.setNote("Writing plan");
                ArrayList<ManeuverLocation> planPath = PlanUtil.getPlanWaypoints(plan);
                if(planPath.isEmpty()){bw.write(",");}
                bw.write(path(planPath, "Planned waypoints", "plan"));
                pmonitor.setProgress(90);
            }
            
            topLeft.translatePosition(50, -50, 0);
            bottomRight.translatePosition(-50, 50, 0);
            topLeft.convertToAbsoluteLatLonDepth();
            bottomRight.convertToAbsoluteLatLonDepth();
            
            pmonitor.setProgress(90);
            bw.write(GeoJsonFooter());
            bw.close();
            pmonitor.setProgress(100);
            
            if (pmonitor.isCanceled()){
                return "Cancelled by the user";
            }
            
            return "Log exported to " + out.getAbsolutePath();
        }
        catch (Exception e) {
            GuiUtils.errorMessage("Error while exporting to GeoJson", "Exception of type " + e.getClass().getSimpleName()
                    + " occurred: " + e.getMessage());
            e.printStackTrace();
            pmonitor.close();
            return null;
        }
    }

    public static void main(String[] args) {
        LocationType loc1 = new LocationType(41.08, -8.2343);
        LocationType loc2 = new LocationType(41.12, -8.2324);
        System.out.println(loc1.getDistanceInMeters(loc2));
        System.out.println(loc2.getDistanceInMeters(loc1));
        double[] res1 = loc2.getOffsetFrom(loc1);
        double[] res2 = WGS84Utilities.WGS84displacement(loc1.getLatitudeDegs(), loc1.getLongitudeDegs(), 0, loc2.getLatitudeDegs(), loc2.getLongitudeDegs(), 0);
        System.out.println(Math.sqrt(res2[0] * res2[0] + res2[1] * res2[1]));
        System.out.println(Math.sqrt(res1[0] * res1[0] + res1[1] * res1[1]));
    }

}
