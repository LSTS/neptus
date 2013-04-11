/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 7, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.jsf;

import java.io.File;
import java.util.ArrayList;

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mra.api.SidescanLine;
import pt.up.fe.dceg.neptus.mra.api.SidescanParser;
import pt.up.fe.dceg.neptus.plugins.sidescan.SidescanConfig;

/**
 * @author jqcorreia
 *
 */
public class JsfSidescanParser implements SidescanParser {

    JsfParser parser;

    public JsfSidescanParser(File f) {
        parser = new JsfParser(f);
    }
    
    @Override
    public long firstPingTimestamp() {
        return parser.getFirstTimeStamp();
    }

    @Override
    public long lastPingTimestamp() {
        return parser.getLastTimeStamp();
    }
    
    @Override
    public ArrayList<Integer> getSubsystemList() {
        return parser.index.subSystemsList;
    }

    @Override
    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int subsystem, SidescanConfig config) {
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        
        ArrayList<JsfSonarData> ping = parser.getPingAt(timestamp1, subsystem);
        ArrayList<JsfSonarData> nextPing;
        
        if(ping.size() == 0) return list;
        while(ping.get(0).getTimestamp() < timestamp2) {
            int size = 1;
            JsfSonarData sboard = null;
            JsfSonarData pboard = null;

            for (JsfSonarData temp : ping) {
                if(temp != null) {
                    if (temp.getHeader().getChannel() == 0) {
                        pboard = temp;
                    }
                    if (temp.getHeader().getChannel() == 1) {
                        sboard = temp;
                    }
                }
            }
            // From here portboard channel (pboard var) will be the reference
//            BufferedImage line = new BufferedImage(pboard.getNumberOfSamples() + sboard.getNumberOfSamples(), 1, BufferedImage.TYPE_INT_RGB);
            double fData[] = new double[pboard.getNumberOfSamples() + sboard.getNumberOfSamples()];
            
            double avgSboard = 0, avgPboard = 0;
            
//            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
//                double r = pboard.getData()[i];
//                min = Math.min(r, min);
//                max = Math.max(r, max);
//            }
            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
                double r = pboard.getData()[i];
                avgPboard += r;
            }
            
            for (int i = 0; i < sboard.getNumberOfSamples(); i++) {
                double r = sboard.getData()[i];
                avgSboard += r;
            }
            
            avgPboard /= (double)pboard.getNumberOfSamples() * config.normalization;
            avgSboard /= (double)sboard.getNumberOfSamples() * config.normalization;
            
            float horizontalScale = (float)fData.length / (pboard.getRange() * 2f);
            float verticalScale = horizontalScale;
        
            nextPing = parser.nextPing(subsystem);
            
            float secondsUntilNextPing = (nextPing.get(0).getTimestamp() - ping.get(0).getTimestamp()) / 1000f;
            float speed = ping.get(0).getSpeed();
            
            size = (int) (secondsUntilNextPing * speed * verticalScale);
            if (size <= 0) {
                size = 1;
            }
            size = 1;
            
            // Draw Portboard
            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
                double r =  i / (double)pboard.getNumberOfSamples();
                double gain;
                gain = Math.abs(30.0 * Math.log(r));
                
                double pb = pboard.getData()[i] * Math.pow(10, gain / config.tvgGain);
                fData[i] = pb / avgPboard;
            }
            
            // Draw Starboard
            for (int i = 0; i < sboard.getNumberOfSamples(); i++) {
                double r = 1 - (i / (double)sboard.getNumberOfSamples());
                double gain;
                
                gain = Math.abs(30.0 * Math.log(r));
                double sb = sboard.getData()[i] * Math.pow(10, gain / config.tvgGain);
                fData[i + pboard.getNumberOfSamples()] = sb / avgSboard;
            }
            
            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.getPosition().setLatitude((pboard.getLat() / 10000.0) / 60.0);
            pose.getPosition().setLongitude((pboard.getLon() / 10000.0) / 60.0);
            pose.setRoll(Math.toRadians(pboard.getRoll() * (180 / 32768.0)));
            pose.setYaw(Math.toRadians(pboard.getHeading() / 100));
            pose.setAltitude(pboard.getAltMillis() / 1000);
            
            list.add(new SidescanLine(ping.get(0).getTimestamp(), ping.get(0).getRange(), pose, fData));

            ping = nextPing;
        }
        return list;
    }
}
