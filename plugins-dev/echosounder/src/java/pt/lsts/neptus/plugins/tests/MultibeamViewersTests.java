/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 20 Feb 2017
 */
package pt.lsts.neptus.plugins.tests;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.comm.transports.udp.UDPTransport;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.MultibeamUtil;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.util.llf.LsfLogSource;

import javax.swing.JFileChooser;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author tsm
 *
 */
public class MultibeamViewersTests {
    private static String chooseFilePath(String msg) {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(msg);

        if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            return null;

        return fc.getSelectedFile().getAbsolutePath();
    }

    public static SonarData reformatData(BathymetrySwath swath, SystemPositionAndAttitude pose, SonarDataInfo info) {
        SonarData oldData = MultibeamUtil.swathToSonarDataOld(swath, pose);
        BathymetryPoint[] points = swath.getData();
        byte[] dataBytes = oldData.getData();

        // read from old format
        ByteBuffer bytes = ByteBuffer.wrap(dataBytes);
        bytes.order(ByteOrder.LITTLE_ENDIAN);

        info.numberOfPoints = (short) (bytes.getShort() & 0xFFFF);
        info.startAngleRads = Math.toRadians(((double) bytes.getShort()) / 100.0);
        info.dataScaleFactor = oldData.getScaleFactor();
        info.angleIncrementRads = oldData.getBeamConfig().get(0).getBeamWidth();
        info.bitsPerPoint = oldData.getBitsPerPoint();

        int numberOfBytesEffData = info.numberOfPoints * (info.bitsPerPoint / 8);
        byte[] rangeByteData = new byte[numberOfBytesEffData];
        bytes.get(rangeByteData, 0, numberOfBytesEffData);
        byte[] intensityByteData = (numberOfBytesEffData <= bytes.remaining())
                ? new byte[numberOfBytesEffData] : null;
        if (intensityByteData != null)
            bytes.get(intensityByteData, 0, numberOfBytesEffData);

        // new data format

        if(info.flagHasAngleSteps) {
            info.angleStepsRads = new double[points.length];
            // generate angle steps
            // info.angleStepsScaleFactor = 0.001;
            for(int i = 0; i < info.numberOfPoints; i++)
                info.angleStepsRads[i] = info.angleIncrementRads; //* i;
        }

        // write data in new format
        int nBytes = info.computeSizeBytes();
        ByteBuffer buffer =  ByteBuffer.allocate(nBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort(info.numberOfPoints);
        buffer.putFloat((float) info.startAngleRads);

        info.flags = 0;
        if(info.flagHasIntensities)
            info.flags |= SonarDataInfo.hasIntensityMask;

        if(info.flagHasAngleSteps)
            info.flags |= SonarDataInfo.equiDistantMask;

        buffer.put(info.flags);

        if(info.flagHasAngleSteps)
            buffer.putFloat((float) info.angleStepsScaleFactor);

        if(info.flagHasIntensities)
            buffer.putFloat((float) info.intensitiesScaleFactor);

        if(info.flagHasAngleSteps)
            Arrays.stream(info.angleStepsRads)
                    .forEach(step -> buffer.put(MultibeamUtil.valueToBytes(step, Short.BYTES,
                            (float) info.angleStepsScaleFactor)));

        buffer.put(rangeByteData);
        if(info.flagHasIntensities)
            buffer.put(intensityByteData);

        oldData.setData(buffer.array());

        return oldData;
    }

    public static void main(String[] args) {
        boolean isEquidistant = true;
        boolean hasIntensities = true;

        String logsFile = chooseFilePath("Choose logs file");

        UDPTransport udp = new UDPTransport(6200, 1);

        try {
            LsfLogSource source = new LsfLogSource(logsFile, null);
            DeltaTParser mbParser = new DeltaTParser(source);
            mbParser.debugOn = false;

            Collection<Integer> vehSrcs = source.getVehicleSources();
            if (vehSrcs.size() < 1)
                return;

            int mainVehSrc = vehSrcs.iterator().next();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IMCOutputStream imcOs = new IMCOutputStream(baos);

            // collect swaths
            List<BathymetrySwath> swaths = new ArrayList<>();
            BathymetrySwath s = mbParser.nextSwath();
            while (s != null) {
                swaths.add(s);
                s = mbParser.nextSwath();
            }

            System.out.println("* Publishing data");
            int c = 0;
            for (BathymetrySwath swath : swaths) {
                SystemPositionAndAttitude pose = swath.getPose();
                EstimatedState currentEstimatedState = pose.toEstimatedState();

                SonarDataInfo info = new SonarDataInfo();
                info.flagHasAngleSteps = isEquidistant;
                info.flagHasIntensities = hasIntensities;

                SonarData sd = reformatData(swath, pose, info);

                currentEstimatedState.setSrc(mainVehSrc);
                sd.setSrc(mainVehSrc);

                sd.setSrcEnt(c++ % 2 + 1000);

                currentEstimatedState.setTimestamp(sd.getTimestamp());

                // publish estimated state
                baos.reset();
                currentEstimatedState.serialize(imcOs);
                udp.sendMessage("localhost", 6001, baos.toByteArray());

                // publish sonar data
                baos.reset();
                sd.serialize(imcOs);
                byte[] out = baos.toByteArray();
                udp.sendMessage("localhost", 6001, out);

                Thread.sleep(50);

                int av = System.in.available();
                if (av > 0) {
                    byte[] buffer = new byte[5000];
                    while (av > 0) {
                        System.in.read(buffer);
                        av = System.in.available();
                    }
                    av = System.in.available();
                    while (av < 1) {
                        Thread.sleep(50);
                        av = System.in.available();
                    }
                    while (av > 0) {
                        System.in.read(buffer);
                        av = System.in.available();
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class SonarDataInfo {
        public static final long hasIntensityMask = (1L << 0);
        public static final long equiDistantMask = (1L << 1);

        public short bitsPerPoint;
        public short numberOfPoints;
        public double startAngleRads;
        public byte flags;
        public boolean flagHasIntensities;
        public boolean flagHasAngleSteps;
        public double intensitiesScaleFactor;
        public double angleStepsRads[];
        public double angleStepsScaleFactor;
        public double angleIncrementRads;
        public long frequency;
        public long timestampMillis;
        public double dataScaleFactor;

        public SonarDataInfo() {
            this.bitsPerPoint = Short.SIZE;
        }

        public int computeSizeBytes() {
            int nBytes = Short.BYTES + Float.BYTES + 1;

            // scale factor and angle steps
            if(this.flagHasAngleSteps) {
                nBytes += Float.BYTES;
                nBytes += Short.BYTES * this.numberOfPoints;
            }

            // scale factor and intensities data
            if(this.flagHasIntensities) {
                nBytes += Float.BYTES;
                nBytes += (this.bitsPerPoint/8) * this.numberOfPoints;
            }

            // ranges data
            nBytes += (this.bitsPerPoint/8) * this.numberOfPoints;

            return nBytes;
        }
    }
}
