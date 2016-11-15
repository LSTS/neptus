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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.lsts.imc.BeamConfig;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;

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
        bytes.order(ByteOrder.LITTLE_ENDIAN);

        // fetch necessary data to compute BathymetrySwath's points
        int numberOfBeams = bytes.getShort() & 0xFFFF;
        double startAngleDeg = ((double) bytes.getShort()) / 100.0;

        int numberOfBytesEffData = numberOfBeams * (bitsPerPoint / 8);
        byte[] rangeByteData = new byte[numberOfBytesEffData];
        bytes.get(rangeByteData, 0, numberOfBytesEffData);
        byte[] intensityByteData = (numberOfBytesEffData <= bytes.remaining())
                ? new byte[numberOfBytesEffData] : null;        
        if (intensityByteData != null)
            bytes.get(intensityByteData, 0, numberOfBytesEffData);
            
        // data
        // Pair<double[], int[]> tmp = splitRangeAndIntensity(getData(bytes, scaleFactor, bitsPerPoint));
        double[] ranges = SidescanUtil.getData(rangeByteData, scaleFactor, bitsPerPoint);
        long[] intensities;
        if (intensityByteData != null) {
            intensities = SidescanUtil.transformData(intensityByteData, bitsPerPoint);
        }
        else {
            intensities = new long[numberOfBeams];
            Arrays.fill(intensities, 0);   
        }

        if(ranges.length != intensities.length)
            return null;

        double angleIncrementRad = sonarData.getBeamConfig().get(0).getBeamWidth();

        BathymetryPoint[] points = new BathymetryPoint[ranges.length];
        for(int i = 0; i < ranges.length; i++) {
            if(ranges[i] == 0)
                continue;

            double angleDeg = startAngleDeg + Math.toDegrees(angleIncrementRad) * i;
            float depth = (float) (ranges[i] * Math.cos(Math.toRadians(angleDeg)) + pose.getPosition().getDepth());

            double x = ranges[i] * Math.sin(Math.toRadians(angleDeg));
            double yawAngle = -pose.getYaw();

            float north = (float) (x * Math.sin(yawAngle));
            float east = (float) (x * Math.cos(yawAngle));

            points[i] = new BathymetryPoint(north, east, depth, (int) intensities[i]);
        }

        return new BathymetrySwath(sonarData.getTimestampMillis(), pose, points);
    }

    public static double[] getData(ByteBuffer bytes, double scaleFactor, short bitsPerPoint) {
        return SidescanUtil.getData(bytes.array(), scaleFactor, bitsPerPoint);
    }

    public static SonarData swathToSonarData(BathymetrySwath swath, SystemPositionAndAttitude pose) {
        SonarData sonarData = new SonarData();

        int bytesPerPoint = Short.BYTES;
        int headerBytes = Short.BYTES * 2;

        BathymetryPoint[] points = swath.getData();
        ByteBuffer bytes = ByteBuffer.allocate(headerBytes + bytesPerPoint * (points.length * 2));
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        List<BeamConfig> beamConfig = new ArrayList<>();

        float scaleFactor = 0.008f;
        double angleIncrementDeg = 0.25;

        // number of beams
        short numberOfBeams = (short) points.length;
        bytes.putShort(numberOfBeams);
        
        // startAngles
        double startAngleDoubleDeg = -59.870003 * 100;
        short startAngleCentiDeg  = (short) startAngleDoubleDeg;
        bytes.putShort(startAngleCentiDeg);

        // put range and intensities
        for(int i = 0; i < points.length; i++) {
            double range = 0;
            double intensity = 0;
            double angle = Math.cos(Math.toRadians(startAngleDoubleDeg / 100 + angleIncrementDeg * i));

            if(points[i] != null) {
                // remove transformation made by DeltaT parser
                range = (points[i].depth - pose.getPosition().getDepth()) / angle;
                intensity = points[i].intensity;
            }

            byte[] rangeBytes = valueToBytes(range, bytesPerPoint, scaleFactor);
            byte[] intensitiyBytes = valueToBytes(intensity, bytesPerPoint, 1);

            int intensityIndex = headerBytes + bytesPerPoint * (points.length + i);

            bytes.put(rangeBytes);
            putBytesAt(bytes, intensityIndex, intensitiyBytes);
        }

        BeamConfig c = new BeamConfig();
        c.setBeamWidth(Math.toRadians(angleIncrementDeg));
        c.setBeamHeight(Math.toRadians(angleIncrementDeg));
        beamConfig.add(c);

        sonarData.setFrequency(260000);
        sonarData.setMinRange(0); // 0.5m
        sonarData.setMaxRange(40);
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
        byte[] bytes = new byte[bytesPerPoint];
        int valueScaled = (int) (value / scaleFactor);

        for(int i = 0; i < bytesPerPoint; i++)
            bytes[i] = (byte) (((valueScaled >> 8 * i)) & 0xFF);

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
        double data[] = {4.0543,2.037375,3.053357,4.335350,5.03535,6.02243,7.131,8.5433540,9.054354,21.054345,450.054354,60.32424354};
        int intensities[] = {1231,1243,436,867867,8978, 31, 312, 342, 654, 123, 68, 3123};

        if(data.length != intensities.length) {
            System.out.println("ERROR");
            return;
        }

        // Build and send Data
        int bytesPerPoint = Short.BYTES;
        int headerBytes = Short.BYTES;

        short startAngle = 96;
        int nBytes = headerBytes + bytesPerPoint * data.length * 2;
        ByteBuffer buffer = ByteBuffer.allocate(nBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        float scaleFactor = 0.0001f;

        // put start angle
        buffer.putShort(startAngle);

        // put ranges and intensities
        for(int i = 0; i < data.length; i++) {
            byte[] rangeBytes = valueToBytes(data[i], bytesPerPoint, scaleFactor);
            byte[] intensitiyBytes = valueToBytes(intensities[i], bytesPerPoint, 1);

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
        newBuffer.order(ByteOrder.LITTLE_ENDIAN);
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
        }

        // Lets apply scaling
        for (int i = 0; i < fData.length; i++) {
            if(i < fData.length / 2)
                fData[i] *= scaleFactor;
            System.out.println(fData[i]);
        }
    }
}
