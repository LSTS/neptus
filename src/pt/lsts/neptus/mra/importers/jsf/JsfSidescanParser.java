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
package pt.lsts.neptus.mra.importers.jsf;

import java.io.File;
import java.util.ArrayList;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;

/**
 * @author jqcorreia
 *
 */
public class JsfSidescanParser implements SidescanParser {

    private JsfParser parser;

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
        return parser.getIndex().subSystemsList;
    }

    @Override
    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int subsystem, SidescanParameters params) {
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        
        ArrayList<JsfSonarData> ping = parser.getPingAt(timestamp1, subsystem);
        
        if(ping.size() == 0) return list;

        while(ping.get(0).getTimestamp() < timestamp2) {
            JsfSonarData sboard = null;
            JsfSonarData pboard = null;

            if(ping.size() < 2) {
                ping = parser.nextPing(subsystem);
                continue;
            }
            
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
            double fData[] = new double[pboard.getNumberOfSamples() + sboard.getNumberOfSamples()];
            
            double avgSboard = 0, avgPboard = 0;
            
            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
                double r = pboard.getData()[i];
                avgPboard += r;
            }
            
            for (int i = 0; i < sboard.getNumberOfSamples(); i++) {
                double r = sboard.getData()[i];
                avgSboard += r;
            }
            
            avgPboard /= (double)pboard.getNumberOfSamples() * params.getNormalization();
            avgSboard /= (double)sboard.getNumberOfSamples() * params.getNormalization();
            
            // Calculate Portboard
            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
                double r =  i / (double)pboard.getNumberOfSamples();
                double gain;
                gain = Math.abs(30.0 * Math.log(r));
                
                double pb = pboard.getData()[i] * Math.pow(10, gain / params.getTvgGain());
                fData[i] = pb / avgPboard;
            }
            
            // Calculate Starboard
            for (int i = 0; i < sboard.getNumberOfSamples(); i++) {
                double r = 1 - (i / (double)sboard.getNumberOfSamples());
                double gain;
                
                gain = Math.abs(30.0 * Math.log(r));
                double sb = sboard.getData()[i] * Math.pow(10, gain / params.getTvgGain());
                fData[i + pboard.getNumberOfSamples()] = sb / avgSboard;
            }
            
            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.getPosition().setLatitudeDegs((pboard.getLat() / 10000.0) / 60.0);
            pose.getPosition().setLongitudeDegs((pboard.getLon() / 10000.0) / 60.0);
            pose.setRoll(Math.toRadians(pboard.getRoll() * (180 / 32768.0)));
            pose.setYaw(Math.toRadians(pboard.getHeading() / 100));
            pose.setAltitude(pboard.getAltMillis() / 1000.0);
            pose.setU(pboard.getSpeed() * 0.51444); // Convert knot-to-ms
            
            list.add(new SidescanLine(ping.get(0).getTimestamp(), ping.get(0).getRange(), pose, ping.get(0).getFrequency(), fData));

            ping = parser.nextPing(subsystem);
            if(ping.size() == 0) return list;
            
        }
        return list;
    }
}
