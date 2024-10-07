/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 7/Ouc/2024
 */
package pt.lsts.neptus.plugins.fresnel;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.IPlanFileImporter;
import pt.lsts.neptus.types.mission.plan.PlanType;

import javax.swing.ProgressMonitor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@PluginDescription
public class PlanImporter implements IPlanFileImporter {
    @Override
    public String getImporterName() {
        return "Frenel Plan File";
    }

    @Override
    public String[] validExtensions() {
        return new String[] { "txt", "fresnel" };
    }

    @Override
    public List<PlanType> importFromFile(MissionType mission, File in, ProgressMonitor monitor) throws Exception {
        String timestamp = "";
        int auvNumber = 0;
        double speed = 1.0;

        int prog = 0;
        monitor.setProgress(prog);
        monitor.setNote("Importing plan...");

        // List of route segments, each containing waypoints (lat, lon, depth)
        List<List<String[]>> allRoutes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(in))) {
            List<String> routeDescription = new ArrayList<>();

            PlanType plan = new PlanType(mission);
            String line;
            List<String[]> currentRoute = null; // Stores the waypoints for each #length segment
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Extract TIMESTAMP, AUV_NUMBER, and SPEED
                if (line.startsWith("#TIMESTAMP:")) {
                    timestamp = line.substring(line.indexOf(":") + 1).trim(); // Get everything after 'TIMESTAMP:'
                    setProgress(++prog , "Timestamp read " + timestamp, monitor);
                } else if (line.startsWith("#AUV_NUMBER:")) {
                    auvNumber = Integer.parseInt(line.split(":")[1].trim());
                    setProgress(++prog , "AUVs read " + auvNumber, monitor);
                } else if (line.startsWith("#SPEED:")) {
                    speed = Double.parseDouble(line.split(":")[1].trim().split(" ")[0]);
                }

                // Start of a new route (#length), initialize the list for new waypoints
                if (line.startsWith("#length:") || line.startsWith("#length_2D:")) {
                    routeDescription.add(line.substring(line.indexOf(":") + 1).trim());
                    currentRoute = new ArrayList<>(); // Initialize a new route
                    allRoutes.add(currentRoute); // Add the previous route to the list

                    setProgress(prog = prog + (80 / auvNumber) , "AUVs route", monitor);
                }

                // Extract and store (lat, lon, depth) waypoints
                if (line.matches("^(\\s?-?\\d+(\\.\\d+)?\\s?,\\s?-?\\d+(\\.\\d+)?\\s?(,\\s?-?\\d+(\\.\\d+)?)?\\s?;).*$")) {
                //if (line.matches("^((-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?);\\s*).*$")) {
                //if (line.matches("^\\d+\\.\\d+,\\s?-\\d+\\.\\d+,\\s?\\d+\\.\\d+;.*$")) {
                    String[] waypoints = line.split(";");
                    for (String wp : waypoints) {
                        String[] coordinates = wp.split(",\\s*"); // Splitting by comma and optional spaces
                        if (currentRoute != null) {
                            currentRoute.add(coordinates);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("Error importing plan: " + e.getMessage(), e);
        }

        setProgress(prog = 90 , "Creating plans", monitor);

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS [z]");
            Date date = inputFormat.parse(timestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMddHHmm");
            String formattedDate = outputFormat.format(date);

            // Generate plans
            List<PlanType> planList = new ArrayList<>();

            for (int i = 0; i < allRoutes.size(); i++) {
                PlanType plan = new PlanType(mission);

                plan.setId("fresnel-" + formattedDate + "-" + i);
                GraphType planGraph = plan.getGraph();

                String firstManeuver = "";
                int wpIndex = 0;
                for (String[] wp : allRoutes.get(i)) {
                    double latDeg = Double.parseDouble(wp[0]);
                    double lonDeg = Double.parseDouble(wp[1]);
                    double depth = wp.length > 2 ? Double.parseDouble(wp[2]) : 0.0;

                    Goto gotoManeuver = new Goto();
                    gotoManeuver.setId("G" + (++wpIndex));
                    if (firstManeuver.isEmpty()) {
                        firstManeuver = gotoManeuver.getId();
                        gotoManeuver.setInitialManeuver(true);
                    }
                    ManeuverLocation loc = new ManeuverLocation();
                    loc.setLatitudeDegs(latDeg);
                    loc.setLongitudeDegs(lonDeg);
                    loc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
                    loc.setZ(depth);
                    gotoManeuver.setManeuverLocation(loc);
                    SpeedType speedType = new SpeedType(speed, SpeedType.Units.MPS);
                    gotoManeuver.setSpeed(speedType);

                    planGraph.addManeuver(gotoManeuver);
                }

                plan.getGraph().setInitialManeuver(firstManeuver);

                for (int j = 0; j < plan.getGraph().getAllManeuvers().length; j++) {
                    if (j == 0)
                        continue;
                    plan.getGraph().addTransition("G" + j, "G" + (j + 1), "ManeuverIsDone");
                }

                planList.add(plan);
            }

            setProgress(prog = 100 , "Import done", monitor);

            return planList;
        } catch (Exception e) {
            throw new Exception("Error importing plan: " + e.getMessage(), e);
        }
    }

    private void setProgress(int i, String msg, ProgressMonitor monitor) {
        monitor.setProgress(i);
        monitor.setNote(msg);

    }
}
