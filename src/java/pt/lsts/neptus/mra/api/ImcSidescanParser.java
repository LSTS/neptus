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
 * Author: José Correia
 * Feb 5, 2013
 */
package pt.lsts.neptus.mra.api;

import java.util.ArrayList;
import java.util.Arrays;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;

/**
 * @author jqcorreia
 *
 */
public class ImcSidescanParser implements SidescanParser {
    private IMraLog pingParser;
    private IMraLog stateParser;

    private long firstTimestamp = -1;
    private long lastTimestamp = -1;

    private ArrayList<Long> subSystemsList = new ArrayList<>();
    
    private long lastTimestampRequested;

    public ImcSidescanParser(IMraLogGroup source) {
        pingParser = source.getLog("SonarData");
        stateParser = source.getLog("EstimatedState");

        calcFirstAndLastTimestamps();
    }

    private void calcFirstAndLastTimestamps() {
        IMCMessage msg;
        boolean firstFound = false;

        while ((msg = getNextMessage(pingParser)) != null) {
            if (!firstFound) {
                firstFound = true;
                firstTimestamp = msg.getTimestampMillis();
            }
            lastTimestamp = msg.getTimestampMillis();
            
            Object subSysObj = msg.getValue("frequency");
            if (subSysObj instanceof Number) {
                try {
                    long freq = (long) Double.parseDouble(subSysObj.toString());
                    if (!subSystemsList.contains(freq))
                        subSystemsList.add(freq);
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        pingParser.firstLogEntry();
        subSystemsList.sort(null);
    }

    @Override
    public long firstPingTimestamp() {
        return firstTimestamp;
    };

    @Override
    public long lastPingTimestamp() {
        return lastTimestamp;
    }

    @Override
    public ArrayList<Integer> getSubsystemList() {
        // SonarData subsystems are accommodated by the frequency field.
        ArrayList<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < subSystemsList.size(); i++) {
            l.add(subSystemsList.get(i).intValue());
        }
        return l;
    };

    @Override
    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int subsystem,
            SidescanParameters params) {
        // Preparation
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
//        double[] fData = null;
        
        if (!subSystemsList.contains((long) subsystem))
            return list;

        if (subSystemsList.size() > 1 && lastTimestampRequested >= timestamp1
                || subSystemsList.size() <= 1 && lastTimestampRequested > timestamp1) {
            pingParser.firstLogEntry();
            stateParser.firstLogEntry();
        }

        lastTimestampRequested = timestamp1;
        IMCMessage ping;
        try {
            ping = pingParser.getEntryAtOrAfter(timestamp1);
        }
        catch (Exception e) {
            ping = null;
        }
        if (ping == null)
            return list;

        if (ping.getInteger("type") != SonarData.TYPE.SIDESCAN.value()) {
            ping = getNextMessage(pingParser); // FIXME
        }
        IMCMessage state = stateParser.getEntryAtOrAfter(ping.getTimestampMillis());

        // Null guards
        if (ping == null || state == null)
            return list;

        int range = ping.getInteger("range");
        if (range == 0)
            range = ping.getInteger("max_range");

        while (ping == null || ping.getTimestampMillis() <= timestamp2) {
            // Null guards
            if (ping == null || state == null)
                break;

            long pingFreq = ping.getLong("frequency");
            if (subsystem == pingFreq) {
                SystemPositionAndAttitude pose = new SystemPositionAndAttitude((EstimatedState) state);
                SidescanLine line = SidescanUtil.getSidescanLine(ping, pose, params);

//                double count = Arrays.stream(line.getData()).mapToInt(value -> Double.isFinite(value) ? 1 : 0).sum();
//                double mean = Arrays.stream(line.getData()).filter(v -> Double.isFinite(v)).sum() / count;
//                double variance = Arrays.stream(line.getData()).filter(v -> Double.isFinite(v)).map(v -> Math.pow(v - mean, 2)).sum() / count;
//                double min = Arrays.stream(line.getData()).min().orElse(0);
//                double max = Arrays.stream(line.getData()).reduce(0, (l, r) -> {
//                    if (Double.isInfinite(l)) return r;
//                    return Math.max(l, r);
//                });
//                System.out.println(String.format(">>>>> Line min: %s   max: %s   mean: %s   variance: %s    count: %s", min, max, mean, variance, count));
                list.add(line);
            }


            ping = getNextMessage(pingParser);
            if (ping != null)
                state = stateParser.getEntryAtOrAfter(ping.getTimestampMillis());
        }

        return list;
    }

    public long getCurrentTime() {
        return pingParser.currentTimeMillis();
    }

    /**
     * Method used to get the next SonarData message of Sidescan Type
     *
     * @param parser
     * @return
     */
    public IMCMessage getNextMessage(IMraLog parser) {
        IMCMessage msg;
        while ((msg = parser.nextLogEntry()) != null) {
            if (msg.getInteger("type") == SonarData.TYPE.SIDESCAN.value()) {
                return msg;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see pt.lsts.neptus.mra.api.SidescanParser#cleanup()
     */
    @Override
    public void cleanup() {
        // TODO Auto-generated method stub

    }
}
