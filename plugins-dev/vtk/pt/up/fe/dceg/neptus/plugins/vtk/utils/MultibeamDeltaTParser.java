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
 * Author: hfq
 * May 1, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import com.jogamp.opengl.util.texture.spi.TGAImage.Header;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mra.api.BathymetryInfo;
import pt.up.fe.dceg.neptus.mra.api.BathymetryParser;
import pt.up.fe.dceg.neptus.mra.api.BathymetryPoint;
import pt.up.fe.dceg.neptus.mra.api.BathymetrySwath;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.importers.deltat.DeltaTHeader;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author hfq
 *
 */
public class MultibeamDeltaTParser implements BathymetryParser{
    private IMraLogGroup logGroup;
    private IMraLog stateParserLogMra;
    private IMCMessage stateIMCMsg;
    
    private File file;
    private FileInputStream fis;
    private FileChannel channel;
    private ByteBuffer buf;
    private long currPos = 0;
    

    
    public BathymetryInfo info;
    
    
    private double maxLatitude = (Math.PI)/2;    // 90º North (+)
    private double minLatitude = -(Math.PI)/2;   // 90º South (-)
    private double maxLongitude = Math.PI;        // 180º East (+)
    private double minLongitude = -Math.PI;       // 180º West (-)
    
    private double maxLat = (Math.PI)/2;
    private double minLat = -(Math.PI)/2;
    private double maxLon = Math.PI;
    private double minLon = -Math.PI;
    
    public MultibeamDeltaTParser(IMraLogGroup source) {
        this.logGroup = source;
        file = source.getFile("multibeam.83P");
        
        System.out.println("MultiBeamDeltaTParser");
        
        try {
            fis = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found: " + e);            
            e.printStackTrace();
        }
        catch (IOException ioe) {
            System.out.println("Exception while reading the file: " + ioe);
            ioe.printStackTrace();
        }
        
        channel = fis.getChannel();
        
        stateParserLogMra = logGroup.getLog("EstimatedState");
        
        initialize();
    }
    
    
    /**
     * 
     */
    private void initialize() {
        info = new BathymetryInfo();
        
        System.out.println("foi ao initialize");
        
        SystemPositionAndAttitude pose = null;
        BathymetryPoint[] data = null;
        long timestamp = 0;
        BathymetrySwath bs;
        //= new BathymetrySwath(timestamp, pose, data);
        
        int i = 0;
        
        
        
        //while ((bs = nextSwath()) != null) {
        while (((bs = nextSwath()) != null) || (i <= 1)) {
            System.out.println("entrou no while");            
            if (i <= 1) {
                // get vehicle pose lat and lon
                double lat = bs.getPose().getPosition().getLatitudeAsDoubleValueRads();
                double lon = bs.getPose().getPosition().getLongitudeAsDoubleValueRads();
            
                System.out.println("Vehicle pos lat: " + lat);
                System.out.println("Vehicle pos lon: " + lon);
                
                System.out.println("maxLat: " + maxLat);
                System.out.println("minLat: " + minLat);
                System.out.println("maxLon: " + maxLon);
                System.out.println("minLon: " + minLon);
                
                maxLat = Math.max(lat, maxLat);
                minLat = Math.min(lat, minLat);
                maxLon = Math.max(lon, maxLon);
                minLon = Math.min(lon, minLon);
                         
                for(int c = 0; c < bs.numBeams; c++) {
                    BathymetryPoint p = bs.getData()[c];
                    
                    info.minDepth = Math.min(info.minDepth, p.depth);
                    info.maxDepth = Math.max(info.maxDepth, p.depth);
                }
            }
            else {
                break;
            }
            i++;
        }
        System.out.println("saiu while i = " + i);
    }


    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mra.api.BathymetryParser#getFirstTimestamp()
     */
    @Override
    public long getFirstTimestamp() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mra.api.BathymetryParser#getLastTimestamp()
     */
    @Override
    public long getLastTimestamp() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mra.api.BathymetryParser#getBathymetryInfo()
     */
    @Override
    public BathymetryInfo getBathymetryInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mra.api.BathymetryParser#getSwathAt(long)
     */
    @Override
    public BathymetrySwath getSwathAt(long timestamp) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mra.api.BathymetryParser#nextSwath()
     */
    @Override
    public BathymetrySwath nextSwath() {
        try {
            if(currPos >= channel.size()) // got to the end of file
                return null;
            
            BathymetryPoint data[];
                
                // read ping header
            System.out.println("read header");
            buf = channel.map(MapMode.READ_ONLY, currPos, 256);
            DeltaTHeader header = new DeltaTHeader();
            header.parse(buf);
            printHeaderArgs(header);         
            //MultibeamDeltaTHeader header = new MultibeamDeltaTHeader(buf);
    
                // Parse and process data
            buf = channel.map(MapMode.READ_ONLY, currPos + 256, header.numBeams * 2); // numberBeam * 2 -> number of bytes
            System.out.println("Current file position 2: "  + buf.position());
            data = new BathymetryPoint[header.numBeams];
            
                // get vehicle pos at the timestamp
            stateIMCMsg = stateParserLogMra.getEntryAtOrAfter(header.timestamp);
            
            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.getPosition().setLatitudeRads(stateIMCMsg.getDouble("lat"));
            pose.getPosition().setLongitudeRads(stateIMCMsg.getDouble("lon"));
            pose.getPosition().setOffsetNorth(stateIMCMsg.getDouble("x"));
            pose.getPosition().setOffsetEast(stateIMCMsg.getDouble("y"));
            pose.getPosition().setDepth(stateIMCMsg.getDouble("Depth"));
            pose.setRoll(stateIMCMsg.getDouble("phi"));
            pose.setPitch(stateIMCMsg.getDouble("theta"));
            pose.setYaw(stateIMCMsg.getDouble("psi"));
            
            printPose(pose);
            
            currPos += header.numBytes;     // advance to the next ping (position file pointer);
            
            BathymetrySwath swath = new BathymetrySwath(header.timestamp, new SystemPositionAndAttitude(), data);
            return swath;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * @param pose 
     * 
     */
    private void printPose(SystemPositionAndAttitude pose) {
        System.out.println("latitude: " + pose.getPosition().getLatitudeAsDoubleValue());
        System.out.println("longitude: " + pose.getPosition().getLongitudeAsDoubleValue());
        System.out.println("offSetNorth: " + pose.getPosition().getOffsetNorth());
        System.out.println("offSetEast: " + pose.getPosition().getOffsetEast());
        System.out.println("Yaw: " + pose.getYaw());
        System.out.println("Roll: " + pose.getRoll());
        System.out.println("Pitch: " + pose.getPitch());
        System.out.println("Depth: " + pose.getPosition().getDepth());
    }


    private void printHeaderArgs(DeltaTHeader header) {
        System.out.println("timestamp: " + header.timestamp);
        System.out.println("startAngle: " + header.startAngle);
        System.out.println("numberByte: " + header.numBytes);
        System.out.println("angleIncrement: " + header.angleIncrement);
        System.out.println("range: " + header.range);
        System.out.println("range Resolution: " + header.rangeResolution);
        System.out.println("samples per beam: " + header.samplesPerBeam);
        System.out.println("Sector size: " + header.sectorSize);   
        
        System.out.println("Current file position 1: " + buf.position());
    }
}
