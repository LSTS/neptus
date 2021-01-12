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
import pt.lsts.neptus.mra.api.SidescanUtil;

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
        ArrayList<JsfSonarData> ping;
        
        try {
            ping = parser.getPingAt(timestamp1, subsystem);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            return list;
        }
        
        if(ping.size() == 0) return list;

        while(ping.get(0).getTimestamp() < timestamp2) {
            JsfSonarData sboard = null;
            JsfSonarData pboard = null;

//            if(ping.size() < 2) {
//                ping = parser.nextPing(subsystem);
//                continue;
//            }
            
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
            
            int pboardNsamples = pboard != null ? pboard.getNumberOfSamples() : 0;
            int sboardNsamples = sboard != null ? sboard.getNumberOfSamples() : 0;
            
            if (pboard == null && sboard != null)
                pboardNsamples = sboardNsamples;
            else if (pboard != null && sboard == null)
                sboardNsamples = pboardNsamples;
            
            // From here portboard channel (pboard var) will be the reference
            double fData[] = new double[pboardNsamples + sboardNsamples];
            
            // Port side
            if (pboard != null) {
                for (int i = 0; i < pboardNsamples; i++) {
                    fData[i] = pboard.getData()[i];
                }
            }
            
            // Starboard side
            if (sboard != null) {
                for (int i = 0; i < sboardNsamples; i++) {
                    fData[i + pboardNsamples] = sboard.getData()[i];
                }
            }

            if (pboard != null || sboard != null) {
                if (pboard == null)
                    pboard = sboard;
                SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
                pose.getPosition().setLatitudeDegs((pboard.getLat() / 10000.0) / 60.0);
                pose.getPosition().setLongitudeDegs((pboard.getLon() / 10000.0) / 60.0);
                pose.getPosition().setDepth( pboard.getDepthMillis() / 1E3);
                pose.setRoll(Math.toRadians(pboard.getRoll() * (180 / 32768.0)));
                pose.setYaw(Math.toRadians(pboard.getHeading() / 100.0));
                pose.setAltitude(pboard.getAltMillis() / 1000.0);
                pose.setU(pboard.getSpeed() * 0.51444); // Convert knot-to-ms
                
                fData = SidescanUtil.applyNormalizationAndTVG(fData, pboard.getRange(), params);
                
                list.add(new SidescanLine(ping.get(0).getTimestamp(), ping.get(0).getRange(), pose, ping.get(0).getFrequency(), fData));
            }

            try {
                ping = parser.nextPing(subsystem); //no next ping available
            } 
            catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
            if(ping.size() == 0)
                return list;
        }
        return list;
    }
    @Override
    public void cleanup() {
        parser.cleanup();
        parser=null;  
    }
    
}
