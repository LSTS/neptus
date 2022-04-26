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
 * Author: zp
 * Sep 22, 2020
 */
package pt.lsts.neptus.plugins.mjpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.importers.deltat.DeltaTHeader;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class Extract83PNav {

    private final FileChannel channel;
    private final IMCOutputStream out;
    private final int src = 28;
    
    public Extract83PNav(File f83p) throws IOException {
        
        if (new File(f83p.getParent(), "Data.lsf").exists()) {
            throw new IOException("File Data.lsf already exists. Exiting.");
        }
        out = new IMCOutputStream(new FileOutputStream(new File(f83p.getParent(), "Data.lsf")));

        channel = FileChannel.open(f83p.toPath());
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    channel.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    public SystemPositionAndAttitude getPose(DeltaTHeader header) {
        SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
        LocationType loc = new LocationType();
        loc.setLatitudeDegs(CoordinateUtil.latFrom83PFormatWorker(header.gnssShipPosLat));
        loc.setLongitudeDegs(CoordinateUtil.lonFrom83PFormatWorker(header.gnssShipPosLon));
        loc.setAbsoluteDepth(-1);
        pose.setPosition(loc);
        
        pose.setTime(header.timestamp);
        pose.setAltitude(header.altitude);
        pose.setDepth(header.sonarZOffset);
        pose.setRoll(Math.toRadians(header.rollAngleDegreesOrientModule));
        pose.setPitch(Math.toRadians(header.pitchAngleDegreesOrientModule));
        pose.setYaw(Math.toRadians(header.gnssShipCourse));
        pose.setAltitude(header.altitude);
        pose.setU(header.speed);        
        return pose;
    }
    
    public void process() throws Exception {
        DeltaTHeader header = new DeltaTHeader();
        ByteBuffer buf;
        long curPos = 0;
        
        
        while (curPos < channel.size()) {
            buf = channel.map(MapMode.READ_ONLY, curPos, 256);
            header.parse(buf);
            curPos += header.numBytes;
            SystemPositionAndAttitude pos = getPose(header);
            EstimatedState result = pos.toEstimatedState();
            result.setTimestampMillis(pos.getTime());
            result.setSrc(src);
            out.writeMessage(result);
        }
        out.close();
    }
    
    public static void main(String[] args) throws Exception {
        new Extract83PNav(new File("/media/zp/5e169b60-ba8d-47db-b25d-9048fe40eed1/OMARE/Raw/lauv-noptilus-3/20200910/092416_mb2/Data.83P")).process();
    }
    
}
