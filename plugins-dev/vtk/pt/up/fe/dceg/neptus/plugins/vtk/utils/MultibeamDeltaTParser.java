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
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
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
    
    private int realNumberOfBeams = 0;
    private int totalNumberPoints = 0;
    
    public BathymetryInfo info;
    
    //private double maxLatitude = (Math.PI)/2;    // 90º North (+)
    //private double minLatitude = -(Math.PI)/2;   // 90º South (-)
    //private double maxLongitude = Math.PI;        // 180º East (+)
    //private double minLongitude = -Math.PI;       // 180º West (-)
    
    private double maxLat = (Math.PI)/2;
    private double minLat = -(Math.PI)/2;
    private double maxLon = Math.PI;
    private double minLon = -Math.PI;
    
    //private double minX = 1000;
    //private double maxX = -1000;
    //private double minY = 1000;
    //private double maxY = -1000;
    
    public PointCloud<PointXYZ> pointCloud;
    
    public MultibeamDeltaTParser(IMraLogGroup source, PointCloud<PointXYZ> pointCloud) {
        this.logGroup = source;
        this.pointCloud = pointCloud;
        
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

        BathymetrySwath bs;
        
        int i = 0;
        
        while ((bs = nextSwath()) != null) {
            double lat = bs.getPose().getPosition().getLatitudeAsDoubleValueRads();
            double lon = bs.getPose().getPosition().getLongitudeAsDoubleValueRads();
                
            maxLat = Math.max(lat, maxLat);
            minLat = Math.min(lat, minLat);
            maxLon = Math.max(lon, maxLon);
            minLon = Math.min(lon, minLon);
                         
                //for(int c = 0; c < bs.numBeams; c++) {
            for(int c = 0; c < realNumberOfBeams; ++c) {
                BathymetryPoint p = bs.getData()[c];
                    
                info.minDepth = Math.min(info.minDepth, p.depth);
                info.maxDepth = Math.max(info.maxDepth, p.depth);               
                    //minX = Math.min(minX, p.north);
                    //maxX = Math.max(maxX, p.north);
                    //minY = Math.min(minY, p.east);
                    //maxX = Math.max(maxY, p.east);
                    
                    //pointCloud.getVerts().InsertNextCell(1);
                        //pointCloud.getVerts().InsertCellPoint(pointCloud.getPoints().InsertNextPoint(
                        //            data[i].east, data[i].north, data[i].depth));
                    //pointCloud.getVerts().InsertCellPoint(pointCloud.getPoints().InsertNextPoint(
                    //        p.north, p.east, p.depth));
                //pointCloud.getVerts().InsertNextCell(1);
                //pointCloud.getVerts().InsertCellPoint(pointCloud.getPoints().InsertNextPoint(
                //        (pose.getPosition().getOffsetNorth() + ox),
                //        (pose.getPosition().getOffsetEast() + oy),
                //        height));
                //pointCloud.getVerts().InsertCellPoint(pointCloud.getPoints().InsertNextPoint(p.north, p.east, p.depth));
                //System.out.println("north: " + p.north + " east: " + p.east + " depth: " + p.depth);
            }
            totalNumberPoints = totalNumberPoints + bs.numBeams;
        }
        //System.out.println("Min X (north): " + minX);
        //System.out.println("Max X (north: " + maxX);
        //System.out.println("Min Y (east): " + minY);
        //System.out.println("Max Y (east): " + maxY);
        System.out.println("Min Depth: " + info.minDepth);
        System.out.println("Max Depth: " + info.maxDepth);
        
        pointCloud.setNumberOfPoints(totalNumberPoints);
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
    public BathymetryInfo getBathymetryInfo() {
        return null;
    }

    @Override
    public BathymetrySwath getSwathAt(long timestamp) {
        return null;
    }

    @Override
    public BathymetrySwath nextSwath() {
        try {

            if(currPos >= channel.size()) // got to the end of file
                return null;
            

            BathymetryPoint data[];
                
                // read ping header
            buf = channel.map(MapMode.READ_ONLY, currPos, 256);
            DeltaTHeader header = new DeltaTHeader();
            header.parse(buf);
            //printHeaderArgs(header);         
            //MultibeamDeltaTHeader header = new MultibeamDeltaTHeader(buf);
    
                // Parse and process data
            buf = channel.map(MapMode.READ_ONLY, currPos + 256, header.numBeams * 2); // numberBeam * 2 -> number of bytes
            data = new BathymetryPoint[header.numBeams];
            
                // get vehicle pos at the timestamp
            stateIMCMsg = stateParserLogMra.getEntryAtOrAfter(header.timestamp);
            
            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.getPosition().setLatitudeRads(stateIMCMsg.getDouble("lat"));
            maxLat = Math.max(maxLat, pose.getPosition().getLatitudeAsDoubleValueRads());         
            minLat = Math.min(minLat, pose.getPosition().getLatitudeAsDoubleValueRads());
            pose.getPosition().setLongitudeRads(stateIMCMsg.getDouble("lon"));
            maxLon = Math.max(maxLat, pose.getPosition().getLongitudeAsDoubleValueRads());
            minLon = Math.min(minLon, pose.getPosition().getLongitudeAsDoubleValueRads());
            
            pose.getPosition().setOffsetNorth(stateIMCMsg.getDouble("x"));
            pose.getPosition().setOffsetEast(stateIMCMsg.getDouble("y"));
            pose.getPosition().setDepth(stateIMCMsg.getDouble("depth"));
            //pose.setRoll(stateIMCMsg.getDouble("phi"));
            //pose.setPitch(stateIMCMsg.getDouble("theta"));
            pose.setYaw(stateIMCMsg.getDouble("psi"));         
            //printPose(pose);
            
            for(int i = 0; i < header.numBeams; ++i) {
                double range = buf.getShort(i*2) * (header.rangeResolution / 1000.0f);  // range resolution in mm -> 1000, range in meters -> short
            
                    // something wrong with the 83P data, when buf.getShort(i*2) -> range on 83P, is = 0, data is discarded
                if(range == 0.0) {
                    continue;
                }
                else {
                    realNumberOfBeams = realNumberOfBeams + 1; 
                }
                
                double angle = header.startAngle + header.angleIncrement * i;
                double height = range * Math.cos(Math.toRadians(angle)) + pose.getPosition().getDepth();                
                double xBeamOffset = range * Math.sin(Math.toRadians(angle));                
                    // heading
                double psi = -pose.getYaw();               
                double ox = xBeamOffset * Math.sin(psi);               
                double oy = xBeamOffset * Math.cos(psi);
                
                data[i] = new BathymetryPoint((float) (pose.getPosition().getOffsetNorth() + ox),
                        (float) (pose.getPosition().getOffsetEast() + oy), (float) height);  
                
                pointCloud.getVerts().InsertNextCell(1);
                pointCloud.getVerts().InsertCellPoint(pointCloud.getPoints().InsertNextPoint(
                        (pose.getPosition().getOffsetNorth() + ox),
                        (pose.getPosition().getOffsetEast() + oy),
                        height));
            }
                    
            currPos += header.numBytes;     // advance to the next ping (position file pointer);
            
            BathymetrySwath swath = new BathymetrySwath(header.timestamp, new SystemPositionAndAttitude(), data);
            //swath.numBeams = header.numBeams; <- can't be this because of erros on range data available on the 83P file
            swath.numBeams = realNumberOfBeams;
            realNumberOfBeams = 0;
            
            return swath;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * @param data
     */
    private void printPoint(BathymetryPoint[] data, int i) {
        System.out.println("Point in Swath: ");
        System.out.println("North (x): " + data[i].north);
        System.out.println("East (y): " + data[i].east);
        System.out.println("Height (z): " + data[i].depth);     
    }


    /**
     * @param pose 
     */
    private void printPose(SystemPositionAndAttitude pose) {
        System.out.println("latitude: " + pose.getPosition().getLatitudeAsDoubleValue());
        System.out.println("longitude: " + pose.getPosition().getLongitudeAsDoubleValue());
        System.out.println("offSetNorth: " + pose.getPosition().getOffsetNorth());
        System.out.println("offSetEast: " + pose.getPosition().getOffsetEast());
        System.out.println("Yaw: " + pose.getYaw());
        //System.out.println("Roll: " + pose.getRoll());
        //System.out.println("Pitch: " + pose.getPitch());
        System.out.println("Depth: " + pose.getPosition().getDepth());
    }

    /**
     * @param header
     */
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
