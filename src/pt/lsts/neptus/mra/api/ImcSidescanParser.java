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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Correia
 * Feb 5, 2013
 */
package pt.lsts.neptus.mra.api;

import java.util.ArrayList;

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
    IMraLog pingParser;
    IMraLog stateParser;
    
    long firstTimestamp = -1;
    long lastTimestamp = -1;
    
    long lastTimestampRequested;
    
    public ImcSidescanParser(IMraLogGroup source) {
        pingParser = source.getLog("SonarData");
        stateParser = source.getLog("EstimatedState");
        
        calcFirstAndLastTimestamps();
    }

    
    private void calcFirstAndLastTimestamps() {
        IMCMessage msg;
        boolean firstFound = false;
        
        while((msg = getNextMessage(pingParser)) != null) {
            if(!firstFound) {
                firstFound = true;
                firstTimestamp = msg.getTimestampMillis();
            }
            lastTimestamp = msg.getTimestampMillis();
        }

        pingParser.firstLogEntry();
    }


    @Override
    public long firstPingTimestamp() {
        return firstTimestamp;
    };
    
    @Override
    public long lastPingTimestamp() {
        return lastTimestamp;
    }
    
    public ArrayList<Integer> getSubsystemList() {
        // For now just return a list with 1 item. In the future IMC will accomodate various SonarData subsystems
        ArrayList<Integer> l = new ArrayList<Integer>();
        l.add(1);
        return l;
    };

    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int subsystem, SidescanParameters params) {
        
        // Preparation
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        double[] fData = null;
        
        if(lastTimestampRequested > timestamp1) {
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
            ping = getNextMessage(pingParser); //FIXME
        }
        IMCMessage state = stateParser.getEntryAtOrAfter(ping.getTimestampMillis());

        // Null guards
        if (ping == null || state == null)
            return list;

        int range = ping.getInteger("range");
        if (range == 0)
            range = ping.getInteger("max_range");

        if (fData == null) {
        }

        while (ping == null || ping.getTimestampMillis() <= timestamp2) {
            // Null guards
            if (ping == null || state == null)
                break;

            fData = new double[ping.getRawData("data").length];
            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.setAltitude(state.getDouble("alt"));
            pose.getPosition().setLatitudeRads(state.getDouble("lat"));
            pose.getPosition().setLongitudeRads(state.getDouble("lon"));
            pose.getPosition().setOffsetNorth(state.getDouble("x"));
            pose.getPosition().setOffsetEast(state.getDouble("y"));
            pose.setRoll(state.getDouble("phi"));
            pose.setYaw(state.getDouble("psi"));
            pose.setP(state.getDouble("p"));
            pose.setQ(state.getDouble("q"));
            pose.setR(state.getDouble("r"));
            pose.setU(state.getDouble("u"));

            // Image building. Calculate and draw a line, scale and save it
            byte[] data = ping.getRawData("data");

            for (int c = 0; c < data.length; c++) {
                fData[c] = (data[c] & 0xFF) / 255.0;
            }
            
            list.add(new SidescanLine(ping.getTimestampMillis(), range, pose, ping.getFloat("frequency"), fData));

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
     * @param parser
     * @return
     */
    public IMCMessage getNextMessage(IMraLog parser) {
        IMCMessage msg;
        while((msg = parser.nextLogEntry()) != null) {
            if(msg.getInteger("type") == SonarData.TYPE.SIDESCAN.value()) {
                return msg;
            }
        }
        return null;
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.api.SidescanParser#cleanup()
     */
    @Override
    public void cleanup() {
        // TODO Auto-generated method stub
        
    }
}
