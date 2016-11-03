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

import pt.lsts.imc.BeamConfig;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.editor.ArrayListEditor;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.llf.LsfLogSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author tsm
 *
 */
public class MultibeamUtil {

    public static BathymetrySwath getMultibeamSwath(SonarData sonarData, SystemPositionAndAttitude pose) {
        NeptusLog.pub().debug("************** TO SWATH");
        byte[] dataBytes = sonarData.getData();
        double scaleFactor = sonarData.getScaleFactor();
        short bitsPerPoint = sonarData.getBitsPerPoint();

        ByteBuffer bytes = ByteBuffer.wrap(dataBytes);

        // fetch necessary data to compute BathymetrySwath's points
        double startAngle = ((double) bytes.getShort()) / 100.0;

        NeptusLog.pub().debug("** Start angle of: " + startAngle);

        // data
        Pair<double[], int[]> tmp = splitRangeAndIntensity(getData(bytes, scaleFactor, bitsPerPoint));
        double[] ranges = tmp.first();
        int[] intensities = tmp.second();


        if(ranges.length != intensities.length)
            return null;

        // compute swath's points
        double x;
        double angle;
        double yawAngle;
        float north;
        float east;
        float depth;
        double angleIncrement = sonarData.getBeamConfig().get(0).getBeamWidth();

        NeptusLog.pub().debug("** Angle increment: " + angleIncrement);

        BathymetryPoint[] points = new BathymetryPoint[ranges.length];
        int rangeZero = 0;
        for(int i = 0; i < ranges.length; i++) {
            if(ranges[i] == 0) {
                /*NeptusLog.pub().debug("** Range is 0");*/
                rangeZero++;
                continue;
            }

            angle = startAngle + angleIncrement * i;
            depth = (float) ((ranges[i] * Math.toRadians(angle)) + pose.getPosition().getDepth());
            x = ranges[i] * Math.sin(Math.toRadians(angle));
            yawAngle = -pose.getYaw();
            north = (float) (x * Math.sin(yawAngle));
            east = (float) (x * Math.cos(yawAngle));

            NeptusLog.pub().debug("** angle: " + angle);
            NeptusLog.pub().debug("** range: " + depth);
            NeptusLog.pub().debug("** x: " + x);
            NeptusLog.pub().debug("** yawAngle: " + yawAngle);
            NeptusLog.pub().debug("** north: " + north);
            NeptusLog.pub().debug("** east: " + east);
            NeptusLog.pub().debug("");

            points[i] = new BathymetryPoint(north, east, depth, intensities[i]);
        }

        NeptusLog.pub().debug("** Range zero: " + rangeZero + " of " + ranges.length);
        NeptusLog.pub().debug("");
        NeptusLog.pub().debug("");

        return new BathymetrySwath(sonarData.getTimestampMillis(), pose, points);
    }

    public static double[] getData(ByteBuffer bytes, double scaleFactor, short bitsPerPoint) {
        if (bitsPerPoint % 8 != 0)
            return null;

        byte[] data = new byte[bytes.remaining()];
         bytes.get(data, 0, bytes.remaining());

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

        // Only apply scale factor to ranges data
        for (int i = 0; i < fData.length/2; i++) {
            fData[i] *= scaleFactor;
            NeptusLog.pub().debug("** Converting " + fData[i]);
        }

        return fData;
    }


    /**
     * Split IMC SonarData's data into
     * ranges and intensity
     * */
    private static Pair<double[], int[]> splitRangeAndIntensity(double[] data) {
        double[] ranges = new double[data.length/2];
        int[] intensities = new int[data.length/2];

        for(int i = 0; i < data.length/2; i++) {
            ranges[i] = data[i];
            intensities[i] = (int) data[data.length/2 + i];
        }

        return new Pair<>(ranges, intensities);
    }

    public static SonarData swathToSonarData(BathymetrySwath swath) {
        SonarData sonarData = new SonarData();

        int bytesPerPoint = Short.BYTES;
        int headerBytes = Short.BYTES;

        BathymetryPoint[] points = swath.getData();
        ByteBuffer bytes = ByteBuffer.allocate(headerBytes + bytesPerPoint * (points.length * 2));
        List<BeamConfig> beamConfig = new ArrayList<>();

        float scaleFactor = 1.0f;

        // startAngles
        double startAngleDouble = -59.870003 * 100;
        short startAngle  = (short) startAngleDouble;
        bytes.putShort(startAngle);

        // put range and intensities
        for(int i = 0; i < points.length; i++) {
            double range = 0;
            double intensity = (double) Integer.MAX_VALUE;

            if(points[i] != null) {
                range = points[i].depth;
                intensity = points[i].intensity;
            }

            byte[] rangeBytes = valueToBytes(range, bytesPerPoint, scaleFactor);
            byte[] intensitiyBytes = valueToBytes(intensity, bytesPerPoint, scaleFactor);

            int intensityIndex = headerBytes + bytesPerPoint * (points.length + i);

            bytes.put(rangeBytes);
            putBytesAt(bytes, intensityIndex, intensitiyBytes);

            BeamConfig c = new BeamConfig();
            c.setBeamWidth(0.25);
            beamConfig.add(c);
        }

        sonarData.setType(SonarData.TYPE.MULTIBEAM);
        sonarData.setBitsPerPoint((short)Short.SIZE);
        sonarData.setScaleFactor(scaleFactor);
        sonarData.setTimestampMillis(swath.getTimestamp());
        sonarData.setData(bytes.array());
        sonarData.setBeamConfig(beamConfig);

        return sonarData;
    }

    /**
     * Converts a given value into a byte a representation of
     * `bytesPerPoint` bytes.
     * */
    public static byte[] valueToBytes(double value, int bytesPerPoint, float scaleFactor) {
        System.out.println("* Converting: " + value);
        byte[] bytes = new byte[bytesPerPoint];
        int valueScaled = (int) (value / scaleFactor);

        for(int i = 0; i < bytesPerPoint; i++) {
            System.out.println(((valueScaled >> 8 * i)));
            bytes[i] = (byte) (((valueScaled >> 8 * i)) & 0xFF);
        }
        System.out.println();
        return bytes;
    }

    /**
     * Puts the given bytes in the buffer starting at the
     * given position
     * */
    public static void putBytesAt(ByteBuffer buffer, int startPos, byte[] bytes) {
        for(int i = 0; i < bytes.length; i++)
            buffer.put(startPos + i, bytes[i]);
    }

    public static void main(String []args) {
        double data[] = {1.0,2.0,3.0,4.0,5.0,6.0,7.131,8.0,9.0,21.0,450.0,60.3242};
        int intensities[] = {1231,1243,436,867867,8978, 31, 312, 342, 654, 123, 68, 3123};

        if(data.length != intensities.length) {
            System.out.println("ERROR");
            return;
        }

        // Build and send Data
        System.out.println("* Writting");

        int bytesPerPoint = Short.BYTES;
        int headerBytes = Short.BYTES;

        short startAngle = 96;
        int nBytes = headerBytes + bytesPerPoint * data.length * 2;
        ByteBuffer buffer = ByteBuffer.allocate(nBytes);
        float scaleFactor = 1.0f;

        // put start angle
        buffer.putShort(startAngle);

        // put ranges and intensities
        for(int i = 0; i < data.length; i++) {
            byte[] rangeBytes = valueToBytes(data[i], bytesPerPoint, scaleFactor);
            byte[] intensitiyBytes = valueToBytes(intensities[i], bytesPerPoint, scaleFactor);

            int intensityIndex = headerBytes + bytesPerPoint * (data.length + i);

            buffer.put(rangeBytes);
            putBytesAt(buffer, intensityIndex, intensitiyBytes);
        }


        byte[] bytes = buffer.array().clone();
        System.out.println("** Wrote " + bytes.length + " bytes");
        System.out.println("** Validated: " + (bytes.length == nBytes));

        // read data
        System.out.println("\n\n* Reading");

        ByteBuffer newBuffer = ByteBuffer.wrap(bytes);
        int readNBytes = newBuffer.array().length;

        System.out.println("** Reading " + readNBytes + " bytes");
        System.out.println("** Validated " + (readNBytes == nBytes) + "\n\n");

        short readAngle = newBuffer.getShort();
        System.out.println("** Start angle: " + readAngle);

        byte[] newData = new byte[newBuffer.remaining()];
        newBuffer.get(newData, 0, newBuffer.remaining());

        double fData[] = new double[newData.length / bytesPerPoint];

        for (int i = 0; i < fData.length; i++) {
            double val = 0;
            for (int j = 0; j < bytesPerPoint; j++) {
                int index = i * bytesPerPoint + j; // progressing index of data
                int v = newData[index] & 0xFF;
                v = (v << 8 * j);
                val += v;
            }
            fData[i] = val;
            //System.out.println(val);
        }

        // Lets apply scaling
        for (int i = 0; i < fData.length; i++) {
            //fData[i] *= scaleFactor;
            System.out.println(fData[i]);
        }
    }
}
