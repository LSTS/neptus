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
 * Jul 21, 2014
 */
package pt.lsts.neptus.plugins.urready4os;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.swing.ProgressMonitor;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.ManeuversUtil;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.mp.maneuvers.YoYo;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.mission.plan.IPlanFileExporter;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.UnitsUtil;

/**
 * @author zp
 *
 */
@PluginDescription
public class IverPlanExporter implements IPlanFileExporter {

    private static final int INFINITE_PARKING_MINUTES = 3600;

    private String iverWaypoint(int wptNum, double speedMps, double yoyoAmplitude, double pitchDegs,
            int parkingTimeMinutes, ManeuverLocation prev, ManeuverLocation dst) {

        StringBuilder sb = new StringBuilder();
        sb.append(wptNum + "; ");
        dst.convertToAbsoluteLatLonDepth();
        sb.append(String.format(Locale.US, "%.6f; ", dst.getLatitudeDegs()));
        sb.append(String.format(Locale.US, "%.6f; ", dst.getLongitudeDegs()));
        if (prev == null) {
            sb.append("0.0; ");
            sb.append("0.0; ");
        }
        else {
            sb.append(String.format(Locale.US, "%.3f; ", /*prevLength +*/ prev.getDistanceInMeters(dst)));
            sb.append(String.format(Locale.US, "%.2f; ", Math.toDegrees(prev.getXYAngle(dst))));
        }

        if (yoyoAmplitude == 0) {

            switch (dst.getZUnits()) {
                case DEPTH:
                    sb.append(String.format(Locale.US, "D%.1f ", metersToFeet(dst.getZ())));
                    break;
                case ALTITUDE:
                    sb.append(String.format(Locale.US, "H%.1f ", metersToFeet(dst.getZ())));
                    break;
                default:
                    sb.append("D0.00 ");
                    break;
            }
        }
        else {
            sb.append(String.format(Locale.US, "U%.1f,%.1f,%.1f ", metersToFeet((dst.getZ() - yoyoAmplitude)),
                    metersToFeet((dst.getZ() + yoyoAmplitude)), pitchDegs));
        }

        int parkMinutes = parkingTimeMinutes < 0 ? 0 : parkingTimeMinutes;
        
        sb.append(String.format(Locale.US, "P%d VC1,0,0,1000,0,VC2,0,0,1000,0 S%.1f; 0;-1\r\n", parkMinutes,
                mpsToKnots(speedMps)));

        return sb.toString();
    }

    @Override
    public String getExporterName() {
        return "Iver .mis Mission File";
    }

    public double metersToFeet(double meters) {
        return meters * UnitsUtil.METER_TO_FEET;
    }

    public double mpsToKnots(double mps) {
        return mps * UnitsUtil.MS_TO_KNOT;
    }

    @Override
    public void exportToFile(PlanType plan, File out, ProgressMonitor monitor) throws Exception {
        double distanceSum = 0;
        double timeSum = 0;
        double minDepth = Double.MAX_VALUE, minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;
        double maxDepth = -Double.MAX_VALUE, maxLat = -Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        int count = 1;
        ManeuverLocation previousLoc = null;

        StringBuilder wpts = new StringBuilder();
        StringBuilder wpt_times = new StringBuilder();
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            double speed = ManeuversUtil.getSpeedMps(m);
            if (Double.isNaN(speed))
                continue;
            if (m instanceof YoYo) {
                ManeuverLocation loc = ((YoYo) m).getManeuverLocation();
                loc.convertToAbsoluteLatLonDepth();

                double amp = ((YoYo) m).getAmplitude();
                double depth = loc.getDepth();
                double distance = 0;
                if (previousLoc != null)
                    distance = previousLoc.getDistanceInMeters(loc);
                double time = distance / speed;// * Math.cos(((YoYo)m).getPitchAngle()));
                maxDepth = Math.max(depth + amp, maxDepth);
                minDepth = Math.min(depth - amp, minDepth);
                minLat = Math.min(minLat, loc.getLatitudeDegs());
                maxLat = Math.max(maxLat, loc.getLatitudeDegs());
                minLon = Math.min(minLon, loc.getLongitudeDegs());
                maxLon = Math.max(maxLon, loc.getLongitudeDegs());

                wpts.append(iverWaypoint(count, speed, ((YoYo) m).getAmplitude(),
                        Math.toDegrees(((YoYo) m).getPitchAngle()), 0, previousLoc, ((YoYo) m).getManeuverLocation()));
                timeSum += time;
                distanceSum += distance;
                if (distanceSum == 0)
                    wpt_times.append(String.format(Locale.US, "WP%d;Time=0;Dist=0\r\n", count++));
                else
                    wpt_times.append(String.format(Locale.US, "WP%d;Time=%.11f;Dist=%.11f\r\n", count++, timeSum, distanceSum));
                previousLoc = loc;
            }
            else if (m instanceof LocatedManeuver) {
                for (ManeuverLocation wpt : ((LocatedManeuver) m).getWaypoints()) {
                    int parkTimeMinutes = 0;
                    if (m instanceof StationKeeping) {
                        if (((StationKeeping) m).getDuration() < 0) {
                            parkTimeMinutes = 0;
                        }
                        else if (((StationKeeping) m).getDuration() == 0) {
                            parkTimeMinutes = INFINITE_PARKING_MINUTES;
                        }
                        else {
                            parkTimeMinutes = ((StationKeeping) m).getDuration(); // seconds
                            parkTimeMinutes = Math.max(0, parkTimeMinutes); // seconds
                            parkTimeMinutes = (int) Math.round(parkTimeMinutes / 60.); // minutes
                        }
                        timeSum += parkTimeMinutes * 60;
                    }
                    wpt.convertToAbsoluteLatLonDepth();
                    double depth = wpt.getDepth();
                    double distance = 0;
                    if (previousLoc != null)
                        distance = previousLoc.getDistanceInMeters(wpt);
                    double time = distance / speed;
                    maxDepth = Math.max(depth, maxDepth);
                    minDepth = Math.min(depth, minDepth);
                    minLat = Math.min(minLat, wpt.getLatitudeDegs());
                    maxLat = Math.max(maxLat, wpt.getLatitudeDegs());
                    minLon = Math.min(minLon, wpt.getLongitudeDegs());
                    maxLon = Math.max(maxLon, wpt.getLongitudeDegs());

                    wpts.append(iverWaypoint(count, speed, 0, 0, parkTimeMinutes, previousLoc, wpt));
                    timeSum += time;
                    distanceSum += distance;
                    if (distanceSum == 0)
                        wpt_times.append(String.format(Locale.US, "WP%d;Time=%.11f;Dist=0\r\n", count++, parkTimeMinutes * 60.));
                    else
                        wpt_times.append(String.format(Locale.US, "WP%d;Time=%.11f;Dist=%.11f\r\n", count++, timeSum, distanceSum));
                    previousLoc = wpt;
                }
            }
            else {
                NeptusLog.pub().warn("Maneuvers of type "+m.getType()+" are not supported and thus will be skipped.");
            }
        }
        int secs = (int) timeSum;
        wpt_times.append(String.format(Locale.US, "Total Time = %02d:%02d:%02d;Total Distance = %.2f\r\n", secs / 3600,
                (secs % 3600) / 60, secs % 60, distanceSum));

        String template = PluginUtils.getResourceAsString("pt/lsts/neptus/plugins/urready4os/template.mis");

        template = template.replaceAll("\\$\\{wpts\\}", wpts.toString().trim());
        template = template.replaceAll("\\$\\{wpt_times\\}", wpt_times.toString().trim());
        template = template.replaceAll("\\$\\{mission_name\\}", out.getName());
        template = template.replaceAll("\\$\\{minLat\\}", String.format(Locale.US, "%.6f", minLat));
        template = template.replaceAll("\\$\\{maxLat\\}", String.format(Locale.US, "%.6f", maxLat));
        template = template.replaceAll("\\$\\{minLon\\}", String.format(Locale.US, "%.6f", minLon));
        template = template.replaceAll("\\$\\{maxLon\\}", String.format(Locale.US, "%.6f", maxLon));
        template = template.replaceAll("\\$\\{centerLat\\}", String.format(Locale.US, "%.6f", (minLat + maxLat) / 2));
        template = template.replaceAll("\\$\\{centerLon\\}", String.format(Locale.US, "%.6f", (minLon + maxLon) / 2));

        FileUtils.write(out, template, (Charset) null);
    }

    @Override
    public String[] validExtensions() {
        return new String[] { "mis", "srp" };
    }
}
