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
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.util.ByteUtil;

/**
 * @author tsm
 *
 */
public class MultibeamUtil {
    private static final long hasIntensityMask = (1L << 0);
    private static final long hasAngleStepsMask = (1L << 1);

    public static BathymetrySwath getMultibeamSwath(SonarData sonarData, SystemPositionAndAttitude pose) {
        byte[] dataBytes = sonarData.getData();
        double dataScaleFactor = sonarData.getScaleFactor();
        short bitsPerPoint = sonarData.getBitsPerPoint();

        ByteBuffer bytes = ByteBuffer.wrap(dataBytes);
        bytes.order(ByteOrder.LITTLE_ENDIAN);

        short numberOfPoints = (short) (bytes.getShort() & 0xFFFF);
        double startAngleRads = bytes.getFloat();
        byte flags = bytes.get();
        boolean flagHasIntensities = (flags & hasIntensityMask) != 0;
        boolean flagHasAngleSteps = (flags & hasAngleStepsMask) != 0;
        double angleStepsScaleFactor = 1;
        double intensitiesScaleFactor = 1;
        double[] angleSteps;
        double ranges[];
        double[] intensities = null;

        // scale factors
        if (flagHasAngleSteps)
            angleStepsScaleFactor = bytes.getFloat();

        if (flagHasIntensities)
            intensitiesScaleFactor = bytes.getFloat();

        // read angle steps
        if (flagHasAngleSteps) {
            int nBytes = numberOfPoints * Short.BYTES;
            byte[] angleStepsBytes = new byte[nBytes];
            bytes.get(angleStepsBytes);

            angleSteps = SidescanUtil.getData(angleStepsBytes, angleStepsScaleFactor, (short) Short.SIZE);
        }
        else {
            angleSteps = new double[1];
            angleSteps[0] = sonarData.getBeamConfig().get(0).getBeamWidth();
        }

        // read ranges data
        int numberOfBytesEffData = numberOfPoints * (bitsPerPoint / 8);
        byte[] rangeByteData = new byte[numberOfBytesEffData];
        bytes.get(rangeByteData);
        ranges = SidescanUtil.getData(rangeByteData, dataScaleFactor, bitsPerPoint);

        if (flagHasIntensities) {
            // read intensities data
            byte[] intensityByteData = (numberOfBytesEffData <= bytes.remaining())
                    ? new byte[numberOfBytesEffData] : null;
            if (intensityByteData != null) {
                bytes.get(intensityByteData, 0, numberOfBytesEffData);
                intensities = SidescanUtil.getData(rangeByteData, intensitiesScaleFactor, bitsPerPoint);
            }
            else {
                intensities = new double[numberOfPoints];
                Arrays.fill(intensities, 0);
                NeptusLog.pub().warn("No intensities data available");
            }
        }
        else {
            intensities = new double[numberOfPoints];
            Arrays.fill(intensities, 0);
            NeptusLog.pub().warn("No intensities data available");
        }

        // build Bathymetry swath's points
        BathymetryPoint[] points = new BathymetryPoint[ranges.length];
        for(int i = 0; i < ranges.length; i++) {
            if(ranges[i] == 0)
                continue;

            double angleRads;
            if(!flagHasAngleSteps) {
                angleRads = startAngleRads + angleSteps[0] * i;
            }
            else {
                angleRads = startAngleRads;
                for (int j = 0; j <= i; j++)
                    angleRads +=  angleSteps[j];
            }

            float depth = (float) (ranges[i] * Math.cos(angleRads) + pose.getPosition().getDepth());

            double x = ranges[i] * Math.sin(angleRads);
            double yawAngle = -pose.getYaw();

            float north = (float) (x * Math.sin(yawAngle));
            float east = (float) (x * Math.cos(yawAngle));

            // FIXME: intensities data are being truncated (from long to int)
            points[i] = new BathymetryPoint(north, east, depth, (int) intensities[i]);
        }

        return new BathymetrySwath(sonarData.getTimestampMillis(), pose, points);
    }

    public static double[] getData(ByteBuffer bytes, double scaleFactor, short bitsPerPoint) {
        return SidescanUtil.getData(bytes.array(), scaleFactor, bitsPerPoint);
    }

    /**
     * Converts a given value into a byte a representation of
     * `bytesPerPoint` bytes.
     * */
    public static byte[] valueToBytes(double value, int bytesPerPoint, float scaleFactor) {
        byte[] bytes = new byte[bytesPerPoint];
        long valueScaled = (long) (value / scaleFactor);

        for (int i = 0; i < bytesPerPoint; i++)
            bytes[i] = (byte) (((valueScaled >> 8 * i)) & 0xFF);

        return bytes;
    }

    /**
     * Puts the given bytes in the buffer starting at the
     * given position
     * */
    public static void putBytesAt(ByteBuffer buffer, int startPos, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++)
            buffer.put(startPos + i, bytes[i]);
    }


    static int i = 0;
    /**
     * This is to be used for testing purposes. It assumes that the data, swath, comes from DeltaT MB.
     *
     * @param swath
     * @param pose
     * @return
     */
    public static SonarData swathToSonarDataOld(BathymetrySwath swath, SystemPositionAndAttitude pose) {
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

        // i = ++i % 3;
        sonarData.setFrequency(260000+i);
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
     * This is to be used for testing purposes. It assumes that the data, swath, comes from DeltaT MB.
     *
     * @param swath
     * @param pose
     * @return
     */
    public static SonarData swathToSonarData(BathymetrySwath swath, SystemPositionAndAttitude pose, 
            boolean useAngleStepsInData, boolean useIntensity, short bytesPerData) {
        SonarData sonarData = new SonarData();

        BathymetryPoint[] points = swath.getData();

        int originalBytesPerPoint = Short.BYTES; 
        int bytesPerPoint = originalBytesPerPoint;
        switch (bytesPerData) {
            case 1:
                bytesPerPoint = 1;
                break;
            case 2:
                bytesPerPoint = 2;
                break;
            case 4:
                bytesPerPoint = 4;
                break;
            case 8:
                bytesPerPoint = 8;
                break;
        }
        int headerBytes = Short.BYTES; // Number of data points
        int dataBytes = bytesPerPoint * points.length; // Ranges
        headerBytes += Float.BYTES; // StartAngle
        headerBytes += Byte.BYTES; // Flags
        if (useAngleStepsInData) {
            headerBytes += Float.BYTES; // Angle scale factor
            dataBytes += Short.BYTES * points.length; // Angle steps
        }
        if (useIntensity) {
            headerBytes += Float.BYTES; // Intensities scale factor
            dataBytes += bytesPerPoint * points.length; // Intensity
        }

        ByteBuffer bytes = ByteBuffer.allocate(headerBytes + dataBytes);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        List<BeamConfig> beamConfig = new ArrayList<>();

        float scaleFactor = 0.008f;
        float scaleFactorAngleDegs = 0.001f;
        float scaleFactorIntensity = 1;
        double angleIncrementDeg = 0.25;

        // number of beams
        short numberOfBeams = (short) points.length;
        bytes.putShort(numberOfBeams);
        
        // startAngles
        double startAngleDoubleDeg = -59.870003;
        bytes.putFloat((float) Math.toRadians(startAngleDoubleDeg));

        byte flags = 0;
        if (useIntensity)
            flags |= (1 << 0); // Intensities flag
        if (useAngleStepsInData)
            flags |= (1 << 1); // Angle steps flag
        bytes.put(flags);

        if (useAngleStepsInData)
            bytes.putFloat((float) Math.toRadians(scaleFactorAngleDegs));
        
        if (useIntensity)
            bytes.putFloat(scaleFactorIntensity);

        // put range and intensities
        for(int i = 0; i < points.length; i++) {
            double angleIncrementToStartDegs = angleIncrementDeg * i;
            double range = 0;
            double intensity = 0;
            double angleDegs = startAngleDoubleDeg + angleIncrementToStartDegs;
            double cosAngle = Math.cos(Math.toRadians(angleDegs)); 

            if(points[i] != null) {
                // remove transformation made by DeltaT parser
                range = (points[i].depth - pose.getPosition().getDepth()) / cosAngle;
                intensity = points[i].intensity;
            }

            byte[] angleBytes = valueToBytes(i == 0 ? 0 : Math.toRadians(angleIncrementDeg), Short.BYTES, (float) Math.toRadians(scaleFactorAngleDegs));
            byte[] rangeBytes = valueToBytes(range, bytesPerPoint, scaleFactor);
            byte[] intensitiyBytes = valueToBytes(intensity, bytesPerPoint, scaleFactorIntensity);

            // Test for 64bits
//            if (bytesPerPoint == 8) {
//                Arrays.fill(rangeBytes, (byte) 0xFF);
//                scaleFactor = 0.000000000000000001f;
//            }
            
//            angleBytes = valueToBytes(0x30, Short.BYTES, 1);
//            rangeBytes = valueToBytes(0x31, bytesPerPoint, 1);
//            intensitiyBytes = valueToBytes(0x32, bytesPerPoint, 1);

            // int angleIndex = headerBytes + Short.BYTES * (i - 1);
            int angleSize = useAngleStepsInData ? Short.BYTES * points.length : 0;
            int rangeIndex = headerBytes + angleSize + bytesPerPoint * i;
            int intensityIndex = headerBytes + angleSize + bytesPerPoint * (points.length + i);

            if (useAngleStepsInData)
                bytes.put(angleBytes);
            // putBytesAt(bytes, angleIndex, angleBytes);
            putBytesAt(bytes, rangeIndex, rangeBytes);
            if (useIntensity)
                putBytesAt(bytes, intensityIndex, intensitiyBytes);
        }

        BeamConfig c = new BeamConfig();
        c.setBeamWidth(Math.toRadians(angleIncrementDeg));
        c.setBeamHeight(Math.toRadians(angleIncrementDeg));
        beamConfig.add(c);

        // i = ++i % 3;
        sonarData.setFrequency(260000+i);
        sonarData.setMinRange(0); // 0.5m
        sonarData.setMaxRange(40);
        sonarData.setType(SonarData.TYPE.MULTIBEAM);
        sonarData.setBitsPerPoint((short) (bytesPerPoint * 8));
        sonarData.setScaleFactor((double) scaleFactor);
        sonarData.setTimestampMillis(swath.getTimestamp());
        sonarData.setData(bytes.array());
        sonarData.setBeamConfig(beamConfig);

        return sonarData;
    }
    
    public static void main(String[] args) {
        double valD = -10;
        long val = (long) valD;
        
        byte[] bt = valueToBytes(val, 8, 1);
        System.out.println(valD + "    " + val + "\n" + ByteUtil.dumpAsHexToString(bt));

        valD = Long.MAX_VALUE * 1.;
        val = (long) valD;
        bt = valueToBytes(val, 8, 1);
        System.out.println(valD + "    " + val + "\n" + ByteUtil.dumpAsHexToString(bt));

        valD = Long.MAX_VALUE * 1. * 2;
        val = (long) valD;
        bt = valueToBytes(val, 8, 1);
        System.out.println((((long)(valD - Long.MAX_VALUE))) + "   " + valD + "    " + val + "\n" + ByteUtil.dumpAsHexToString(bt));
    }
}
