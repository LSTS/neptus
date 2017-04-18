/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 23 Mar 2017
 */
package pt.lsts.neptus.plugins.logs.utils;

import pt.lsts.imc.*;
import pt.lsts.imc.lsf.batch.LsfBatch;
import pt.lsts.neptus.data.GeoCollection;
import pt.lsts.neptus.plugins.logs.search.LogsDbHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author tsmarques
 * @date 3/9/17
 */
public class LogsIndexer {
    // File used to save new entries if database connection fails
    private static final String DB_ENTRIES_FILE = System.getProperty("user.home") + "/db_2.txt";

    private static String[] availableVehicles = {
            "lauv-xplore-1",
            "lauv-xplore-2",
            "lauv-noptilus-1",
            "lauv-noptilus-2",
            "lauv-noptilus-3",
            "lauv-xtreme",
            "lauv-xtreme-2",
            "lauv-arpao",
            "lauv-seacon-1",
            "lauv-seacon-2",
            "lauv-seacon-3",
            "lauv-seacon-4",
            "lauv-dolphin-1",
            "caravela",
            "nauv",
            "adamastor"
    };

    private static int fetchLogYear(String pathStr) {
        Pattern p = Pattern.compile("[\\d]{4}");
        Matcher m = p.matcher(pathStr);

        while (m.find()) {
            String s = m.group();

            return Integer.parseInt(s);
        }

        return -1;
    }

    private static String fetchLogName(String pathStr) {
        String[] pathParts = pathStr.split("/");
        String name = pathParts[pathParts.length - 2];

        return name;
    }


    private static long fetchLogDateMilliseconds(String pathStr) {
        //Pattern p = Pattern.compile("\\d+");
        // Find date pattern (e.g 2017-03-04)
        Pattern p = Pattern.compile("[\\d]{4}-[\\d]{2}-[\\d]{2}");
        Matcher m = p.matcher(pathStr);
        while (m.find()) {
            String s = m.group();
            String dateParts[] = s.split("-");

            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day = Integer.parseInt(dateParts[2]);

            Calendar c = Calendar.getInstance();
            // month is zero-based
            c.set(year, month - 1, day, 0, 0);
            return c.getTimeInMillis();
        }

        return -1;
    }

    private static String fetchVehicleId(String pathStr) {
        String vehicleId = null;
        try {
            vehicleId = Arrays.stream(availableVehicles)
                    .filter(v -> pathStr.contains(v))
                    .findFirst()
                    .get();
        }
        catch (NoSuchElementException e) {
            System.out.println("ERROR: Can't find vehicle");
        }

        return vehicleId;
    }

    private static String toString(Map.Entry<String, LogsDbHandler.DbEntry> e) {
        LogsDbHandler.DbEntry dbEntry = e.getValue();
        if(dbEntry.dateMillis == 0 || dbEntry.vehicleId == null) {
            System.out.println("ERROR: Coulnd't index log");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(e.getKey() + "$");
        sb.append(dbEntry.dateMillis + "$");
        sb.append(dbEntry.vehicleId + "$");

        List<String> dataList = new ArrayList<>(e.getValue().dataType);
        for(int i = 0; i < dataList.size(); i++) {
            String data = dataList.get(i);
            sb.append(data);

            if(i != dataList.size() - 1)
                sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * Save new data to a file
     * */
    private static void saveEntriesToFile(HashMap<String, LogsDbHandler.DbEntry> entries) {
        File f = new File(DB_ENTRIES_FILE);
        if(!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        Path outF = Paths.get(DB_ENTRIES_FILE);

        List<String> tmp = new ArrayList<>();
        for(Map.Entry<String, LogsDbHandler.DbEntry> e : entries.entrySet()) {
            HashSet<String> data = e.getValue().dataType;

            if(data.isEmpty())
                continue;
            tmp.add(toString(e));
        }

        System.out.println("Wrote output to " + outF.toAbsolutePath().toString());
        try {
            Files.write(outF, tmp, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate database with new data
     * */
    private static boolean updateDatabase(HashMap<String, LogsDbHandler.DbEntry> logsData) throws InterruptedException {
        // Update database
        LogsDbHandler db = new LogsDbHandler();
        db.connect();
        int nTries = 5;
        boolean success = false;

        System.out.println("Connecting to database...");
        while (nTries > 0 && !success) {
            success = db.isConnected();

            if(!success) {
                System.out.println("Failed, trying again in 5 seconds....");
                Thread.sleep(5000);
            }

            nTries--;
        }

        // couldn't connect to database
        if(!success) {
            System.out.println("Couldn't connect to database. Saved entries to file");
            saveEntriesToFile(logsData);
            return false;
        }

        System.out.println("Success...");
        System.out.println("Adding entries to database...");

        for(Map.Entry<String, LogsDbHandler.DbEntry> entry : logsData.entrySet())
            db.addEntry(entry.getValue());

        return true;
    }

    private static boolean isVehicleLog(String logPathStr) {
        return Arrays.stream(availableVehicles)
                .anyMatch(v -> logPathStr.contains(v));
    }

    public static void main(String[] args) throws Exception {
        LsfBatch batch = LsfBatch.selectFolders();
        HashMap<String, LogsDbHandler.DbEntry> logsData = new HashMap<>();

        // aux variables
        // used to calculate plan duration
        HashMap<String, Long> firstManeuverState = new HashMap<>();
        HashMap<String, Long> lastManeuverState = new HashMap<>();

        LsfBatch.LsfLog msg;
        PlanSpecification pSpec;
        System.out.println("Parsing logs...");
        while ((msg = batch.nextLog()) != null) {
            LogsDbHandler.DbEntry currentEntry = null;
            String logPath = msg.root + "/Data.lsf.gz";

            if(!isVehicleLog(msg.root))
                continue;

            if(logsData.containsKey(logPath))
                currentEntry = logsData.get(logPath);
            else {
                currentEntry = new LogsDbHandler.DbEntry();
                currentEntry.logPath = logPath;
                currentEntry.vehicleId = fetchVehicleId(msg.root);
                currentEntry.dateMillis = fetchLogDateMilliseconds(msg.root);
                currentEntry.dataType = new HashSet<>();
                currentEntry.year = fetchLogYear(logPath);
                currentEntry.logName = fetchLogName(logPath);

                logsData.put(logPath, currentEntry);
                firstManeuverState.put(logPath, Long.MAX_VALUE);
                lastManeuverState.put(logPath, Long.MIN_VALUE);
            }

            // check log
            switch (msg.curMessage.getMgId()) {
                // used to determine log payload
                case Salinity.ID_STATIC:
                    currentEntry.dataType.add("ctd");
                    break;

                // used to determine log payload
                case PH.ID_STATIC:
                    currentEntry.dataType.add("ph");
                    break;

                // used to determine log payload
                case Redox.ID_STATIC:
                    currentEntry.dataType.add("redox");
                    break;
                case Distance.ID_STATIC:
                    currentEntry.dataType.add("distance");
                    break;
                case Fluorescein.ID_STATIC:
                    currentEntry.dataType.add("fluorescein");
                    break;

                // used to determine log payload and location
                case PlanSpecification.ID_STATIC:
                    pSpec = (PlanSpecification) msg.curMessage.deserialize();
                    Vector<PlanManeuver> maneuvers = pSpec.getManeuvers();

                    // update entry's location
                    currentEntry.latRad = maneuvers.get(0).getData().get("lat", Double.class);
                    currentEntry.lonRad = maneuvers.get(0).getData().get("lon", Double.class);

                    // fetch SetEntityParameters messages
                    List<IMCMessage> params = maneuvers.stream()
                            .flatMap(man -> man.getStartActions().stream())
                            .filter(s -> (s instanceof SetEntityParameters))
                            .collect(Collectors.toList());

                    for(IMCMessage m : params) {
                        SetEntityParameters param = (SetEntityParameters) m;

                        String dataName = null;
                        switch (param.getName()) {
                            case "Multibeam":
                                dataName = "multibeam";
                                break;
                            case "Sidescan":
                                dataName = "sidescan";
                                break;
                            case "Camera":
                                dataName = "camera";
                                break;
                        }

                        if(dataName != null) {
                            boolean isActive = param.getParams()
                                    .stream()
                                    .anyMatch(p -> p.getName().equals("Active") && p.getValue().equals("true"));

                            if(isActive)
                                currentEntry.dataType.add(dataName);
                        }
                    }
                    break;

                // used to determine log/plan duration
                case VehicleState.ID_STATIC:
                    VehicleState vState = (VehicleState) msg.curMessage.deserialize();

                    if(vState.getOpMode() == VehicleState.OP_MODE.MANEUVER) {
                        long firstManState = firstManeuverState.get(currentEntry.logPath);
                        long lastManState = lastManeuverState.get(currentEntry.logPath);
                        long currTimeMillis = vState.getTimestampMillis();

                        // update plan's duration interval
                        if(currTimeMillis > lastManState)
                            lastManeuverState.put(currentEntry.logPath, currTimeMillis);

                        if(currTimeMillis < firstManState)
                            firstManeuverState.put(currentEntry.logPath, currTimeMillis);

                        // update duration if possible
                        if(firstManState != Double.MAX_VALUE && lastManState != Double.MIN_VALUE)
                            currentEntry.durationMillis = lastManState - firstManState;
                    }
                    break;
            }
        }
        System.out.println("Done...");
        // saveEntriesToFile(logsData);
        boolean success = updateDatabase(logsData);

        if (!success)
            System.exit(-1);

        System.exit(0);
    }
}
