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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Manuel R.
 * Oct 21, 2014
 */
package pt.lsts.neptus.mra.importers.sdf;

import java.io.File;
import java.util.ArrayList;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;

public class SdfSidescanParser implements SidescanParser {

    private SdfParser parser;

    public SdfSidescanParser(File f) {
        parser = new SdfParser(f);
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
    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int subsystem,
            SidescanParameters config) {

        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();

        ArrayList<SdfData> ping = parser.getPingAt(timestamp1, subsystem);

        if(ping.size() == 0) return list;

        while(ping.get(0).getTimestamp() < timestamp2) {

            SdfData sboardPboard = ping.get(0); // one ping contains both Sboard and Portboard samples

            int nSamples = sboardPboard != null ? sboardPboard.getNumSamples() : 0;

            double fData[] = new double[nSamples*2]; // x2 (portboard + sboard in the same ping) 
            
            //TODO : fData[] ...

            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.getPosition().setLatitudeDegs(Math.toDegrees(sboardPboard.getHeader().getShipLat())); //rads to degrees
            pose.getPosition().setLongitudeDegs(Math.toDegrees(sboardPboard.getHeader().getShipLon()));//rads to degrees

            // or pose.setRoll(Math.toRadians(sboardPboard.getHeader().getRoll()));
            pose.setRoll(Math.toRadians(sboardPboard.getHeader().getAuxRoll()));

            pose.setYaw(Math.toRadians(sboardPboard.getHeader().getShipHeading()));           
            pose.setAltitude(sboardPboard.getHeader().getAltitude() ); // altitude in meters
            pose.setU(sboardPboard.getHeader().getSpeedFish() / 100.0); // Convert cm/s to m/s

            float frequency = ping.get(0).getHeader().getSonarFreq(); //TODO: ou .getSampleFreq()
            float range = ping.get(0).getHeader().getRange();

            list.add(new SidescanLine(ping.get(0).getTimestamp(), range, pose, frequency, fData));

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


    public static void main(String args[]) {
//        SdfSidescanParser p = new SdfSidescanParser(new File("C://Users//Manuel//workspace//neptus-dev//SDF-datasamples//test.sdf"));
//        long ts1 = 1349084855056L;
//        long ts2 = 1349084855067L; 
//        SidescanParameters config = new SidescanParameters(0.2, 75);
//        ArrayList<SidescanLine> l = p.getLinesBetween(ts1, ts2, 3502, config);
//        
//        System.out.println("freq: "+ l.get(0).frequency + ", range: "+ l.get(0).range + " m, TS: " + l.get(0).timestampMillis + ",  alt: "+ 
//                Math.round(l.get(0).state.getAltitude()) + "m, speed: " + l.get(0).state.getU() + " m/s, Heading: " + l.get(0).state.getYaw() +" rads ("+ Math.round(Math.toDegrees(l.get(0).state.getYaw()))+"deg)");
        
    }
}

