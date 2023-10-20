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
 * Author: Manuel R.
 * Oct 21, 2014
 */
package pt.lsts.neptus.mra.importers.sdf;

import java.io.File;
import java.util.ArrayList;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanUtil;

public class SdfSidescanParser implements SidescanParser {
    private SdfParser parser;

    public SdfSidescanParser(File file) {
        parser = new SdfParser(new File[]{file});
    }

    /**
     * @param files
     */
    public SdfSidescanParser(File[] files) {
        parser = new SdfParser(files);
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

        NeptusLog.pub().debug(">>>>>>>>>>>>>> getLinesBetween timestamp1=" + timestamp1 +
                ",  timestamp2=" + timestamp2 + ",  subsystem=" + subsystem);

        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        SdfData ping;
        try {
            ping = parser.getPingAt(timestamp1, subsystem);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            return list;
        }
        if (ping == null)
            return list;

        while (ping.getTimestamp() < timestamp2) {
            SdfData sboardPboard = ping; // one ping contains both Sboard and Portboard samples
            int nSamples = sboardPboard != null ? sboardPboard.getNumSamples() : 0;
            double fData[] = new double[nSamples * 2]; // x2 (portboard + sboard in the same ping)

            // Port side
            for (int i = 0; i < nSamples; i++) {
                fData[nSamples - i - 1] = sboardPboard.getPortData()[i];
            }

            // Starboard side
            for (int i = 0; i < nSamples; i++) {
                fData[i + nSamples] = sboardPboard.getStbdData()[i];
            }

            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.getPosition().setLatitudeDegs(Math.toDegrees(sboardPboard.getHeader().getShipLat())); // rads to
                                                                                                       // degrees
            pose.getPosition().setLongitudeDegs(Math.toDegrees(sboardPboard.getHeader().getShipLon()));// rads to
                                                                                                       // degrees

            pose.setRoll(Math.toRadians(sboardPboard.getHeader().getAuxRoll()));
            pose.setYaw(Math.toRadians(sboardPboard.getHeader().getShipHeading()));
            pose.setAltitude(sboardPboard.getHeader().getAuxAlt()); // altitude in meters
            pose.setU(sboardPboard.getHeader().getSpeedFish() / 100.0); // Convert cm/s to m/s
            pose.getPosition().setDepth(sboardPboard.getHeader().getAuxDepth());

            float frequency = ping.getHeader().getSonarFreq();
            float range = ping.getHeader().getRange();

            fData = SidescanUtil.applyNormalizationAndTVG(fData, range, config);
            
            list.add(new SidescanLine(ping.getTimestamp(), range, pose, frequency, fData));

            try {
                ping = parser.nextPing(subsystem); // no next ping available
            }
            catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
            if (ping == null)
                return list;
        }
        return list;
    }

    @Override
    public void cleanup() {
        parser.cleanup();
        parser = null;
    }
}
