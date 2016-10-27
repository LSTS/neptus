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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsm
 * 14 Oct 2016
 */
package pt.lsts.neptus.mra.api;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.util.llf.LsfLogSource;

import java.nio.ByteBuffer;

/**
 * @author tsm
 *
 */
public class MultibeamUtil {

    public static BathymetrySwath getMultibeamSwath(SonarData sonarData, SystemPositionAndAttitude pose) {
        byte[] dataBytes = sonarData.getData();
        double scaleFactor = sonarData.getScaleFactor();
        short bitsPerPoint = sonarData.getBitsPerPoint();

        ByteBuffer bytes = ByteBuffer.wrap(dataBytes);

        // fetch necessary data to compute BathymetrySwath's points
        int nPoints = sonarData.getBeamConfig().size();
        short startAngle = bytes.getShort(0);
        float angleIncrement = ((float) bytes.get(1)) / 10;
        double[] ranges = getData(bytes, nPoints, scaleFactor, bitsPerPoint);
        int[] intensities = getIntensityInfo(bytes, nPoints);


        if(ranges.length != intensities.length)
            return null;

        // compute swath's points
        double x;
        double angle;
        double yawAngle;
        float north;
        float east;
        float depth;

        BathymetryPoint[] points = new BathymetryPoint[ranges.length];
        for(int i = 0; i < ranges.length; i++) {
            if(ranges[i] == 0)
                continue;

            angle = startAngle + angleIncrement * i;
            depth = (float) ((ranges[i] * Math.toRadians(angle)) + pose.getPosition().getDepth());
            x = ranges[i] * Math.sin(Math.toRadians(angle));
            yawAngle = -pose.getYaw();
            north = (float) (x * Math.sin(yawAngle));;
            east = (float) (x * Math.cos(yawAngle));

            points[i] = new BathymetryPoint(north, east, depth, intensities[i]);
        }

        return new BathymetrySwath(sonarData.getTimestampMillis(), pose, points);
    }

    public static double[] getData(ByteBuffer bytes, int nPoints, double scaleFactor, short bitsPerPoint) {
        if (bitsPerPoint % 8 != 0)
            return null;

        // fetch data relative to ranges
        byte[] data = new byte[nPoints];
        bytes.get(data, bytes.position(), nPoints);

        int bytesPerPoint = bitsPerPoint < 8 ? 1 : (bitsPerPoint / 8);
        double[] fData = new double[data.length / bytesPerPoint];

        int k = 0;
        for (int i = 0; i < data.length; i++) {
            double val = 0;
            for (int j = 0; j < bytesPerPoint; j++) {
                i = i + j; // progressing index of data
                int v = data[i] & 0xFF;
                v = (v << 8 * j);
                val += v;
            }
            fData[k++] = val;
        }

        // Lets apply scaling
        for (int i = 0; i < fData.length; i++) {
            fData[i] *= scaleFactor;
        }

        return fData;
    }
    
    public static int[] getIntensityInfo(ByteBuffer bytes, int nPoints) {
        int[] intensities = new int[nPoints];
        for(int i = 0; i < nPoints; i++) {
            intensities[i] = bytes.getInt();
        }

        return intensities;
    }
}
