/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Apr 2, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.deltat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mra.api.BathymetryParser;
import pt.up.fe.dceg.neptus.mra.api.BathymetryPoint;
import pt.up.fe.dceg.neptus.mra.api.BathymetrySwath;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.util.llf.LsfLogSource;

/**
 * @author jqcorreia
 *
 */
public class DeltaTParser implements BathymetryParser {

    File file;
    IMraLogGroup source;
    FileInputStream fis;
    FileChannel channel;
    ByteBuffer buf;
    long curPos = 0;
    
    IMCMessage state;
    IMraLog stateParser;
    
    public DeltaTParser(IMraLogGroup source) {
        this.source = source;
        file = source.getFile("multibeam.83P");
        try {
            fis = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        channel = fis.getChannel();
        stateParser = source.getLog("EstimatedState");
    }
    
    @Override
    public long getFirstTimestamp() {
        return 0;
    }

    @Override
    public long getLastTimestamp() {
        return 0;
    }

    @Override
    public BathymetrySwath getSwathAt(long timestamp) {
        return null;
    }

    @Override
    public BathymetrySwath nextSwath() {
        try {
            if(curPos >= channel.size())
                return null;
            
            BathymetryPoint data[];
            
            buf = channel.map(MapMode.READ_ONLY, curPos, 256);
            DeltaTHeader header = new DeltaTHeader();
            header.parse(buf);
            
            // Parse and process data ( no need to create another structure for this )
            buf = channel.map(MapMode.READ_ONLY, curPos + 256, header.numBeams * 2);
            data = new BathymetryPoint[header.numBeams];
            state = stateParser.getEntryAtOrAfter(header.timestamp);
            
            double lat = Math.toDegrees(state.getDouble("lat"));
            double lon = Math.toDegrees(state.getDouble("lon"));
            double offNorth = state.getDouble("x");
            double offEast = state.getDouble("y");
            double depth = state.getDouble("depth");
            double heading = state.getDouble("psi");
            
            for(int c = 0; c < header.numBeams; c++) { 
                double range = buf.getShort(c*2) * (header.rangeResolution / 1000.0);
                double angle = header.startAngle + header.angleIncrement * c;
                
                double height = range * Math.cos(Math.toRadians(angle)) + depth;

                double x = range * Math.sin(Math.toRadians(angle));
                double theta = -heading;
                double ox = x * Math.cos(Math.toRadians(theta));
                double oy = x * Math.sin(Math.toRadians(theta));
                
                data[c] = new BathymetryPoint(lat, lon, offNorth + ox, offEast + oy, height);
            }
            curPos += header.numBytes; // Advance current position
            
            return new BathymetrySwath(header.timestamp,  new SystemPositionAndAttitude(), data);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            LsfLogSource source = new LsfLogSource(new File("/home/jqcorreia/lsts/logs/lauv-noptilus-1/20130208/124645_bathym_plan/Data.lsf"), null);
            DeltaTParser p = new DeltaTParser(source);
            BathymetrySwath bs;
            
            int c = 0;
            while((bs = p.nextSwath()) != null) {
                for(BathymetryPoint bp : bs.getData()) {
                    double r[] = CoordinateUtil.latLonAddNE2(bp.lat, bp.lon, bp.north, bp.east);
                    float f[] = new float[2];
                    
                    f[0] = (float) (r[0] * 1000000f);
                    f[1] = new Double(r[1]).floatValue();
                    
                    System.out.println(r[0]);
                    System.out.println(" " + f[0]);
                }
                c++;
            }
            System.out.println(c);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
