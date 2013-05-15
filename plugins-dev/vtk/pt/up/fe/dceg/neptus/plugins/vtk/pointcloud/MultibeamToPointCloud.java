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
 * Apr 26, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.pointcloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import pt.up.fe.dceg.neptus.mra.api.BathymetryInfo;
import pt.up.fe.dceg.neptus.mra.api.BathymetryPoint;
import pt.up.fe.dceg.neptus.mra.api.BathymetrySwath;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.importers.deltat.DeltaTParser;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.MultibeamDeltaTHeader;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.MultibeamDeltaTParser;

/**
 * @author hfq
 *
 */
public class MultibeamToPointCloud {
    
    public IMraLogGroup source;
    public IMraLog state;
 
    public double maxLat = 90;      // (N) 90º north
    public double minLat = -90;     // (S) 90º south

    public double maxLon = 180;     // (E) 180º east   
    public double minLon = -180;    // (W) 180º west
    
    public BathymetryPoint batPoint;           // float north, float east, float depth
    public BathymetrySwath batSwath;
    public BathymetryInfo batInfo;
    
    private File file;                          // *.83P file
    private FileInputStream fileInputStream;    // 83P file input stream
    private FileChannel channel;                // SeekableByteChanel connected to the file (83P)
    private ByteBuffer buf;
    
    private byte fileContent[];
    
    public MultibeamDeltaTParser multibeamDeltaTParser;
    
    public PointCloud<PointXYZ> pointCloud;
    
    
    public MultibeamToPointCloud(IMraLogGroup source, PointCloud<PointXYZ> pointCloud) {
        
        this.source = source;
        this.pointCloud = pointCloud;
        
        multibeamDeltaTParser = new MultibeamDeltaTParser(this.source, pointCloud);
        //getMyDeltaTHeader();
                
        //DeltaTParser deltaTParser = new DeltaTParser(source);
        //batInfo = deltaTParser.getBathymetryInfo();
        //long firstTimeStamp = deltaTParser.getFirstTimestamp();
    }
    
    /**
     * 
     */
    private void getMyDeltaTHeader() {
        file = source.getFile("multibeam.83P");  
        //System.out.println("print parent: " + file.toString());
        try {
            fileInputStream = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found: " + e);            
            e.printStackTrace();
        }
        catch (IOException ioe) {
            System.out.println("Exception while reading the file: " + ioe);
            ioe.printStackTrace();
        }
        
        
        channel = fileInputStream.getChannel();      
        long posOnFile = 0;
        long sizeOfRegionToMap = 256;   // 256 bytes currespondent to the header of each ping         
        try {
            buf = channel.map(MapMode.READ_ONLY, posOnFile, sizeOfRegionToMap);
        }
        catch (IOException e) {
            e.printStackTrace();
        } 
        
        MultibeamDeltaTHeader deltaTHeader = new MultibeamDeltaTHeader(buf);
        deltaTHeader.parseHeader();
        
    }

    /**
     * do not use
     */
    public void printDeltaTFileContent() {
        fileContent = new byte[(int)file.length()];
        
        String strFileContent = new String(fileContent);
        
        System.out.println("File content: ");
        System.out.println(strFileContent);
    }
}
