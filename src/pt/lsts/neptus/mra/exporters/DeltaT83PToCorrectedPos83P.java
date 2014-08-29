/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 25/08/2014
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTHeader;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleCalc;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.MathMiscUtils;
import ucar.jpeg.jj2000.j2k.util.StringFormatException;

/**
 * Apply corrected position to 83P and re-exported.  
 * @author pdias
 * 
 */
@PluginDescription
public class DeltaT83PToCorrectedPos83P implements MRAExporter {
    private IMraLogGroup log = null;
    private CorrectedPosition correctedPosition = null;

    private DeltaTParser deltaParser = null;

    private FileInputStream fis;
    private FileChannel channel;
    private ByteBuffer buf;
    private long curPos = 0;

    public DeltaT83PToCorrectedPos83P(IMraLogGroup log) {
        this.log = log;
    }
    
    public String getName() {
        return "83P to 83P with Corrected Position";
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return DeltaTParser.canBeApplied(source);
    }

    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        if (!canBeApplied(source))
            return "No data to process!";
        
        File fileOrig = DeltaTParser.findDataSource(source);
        String destName = FileUtil.getFileNameWithoutExtension(fileOrig) + "Corrected" + FileUtil.getFileExtension(fileOrig);
        File fileDest =  new File(fileOrig.getParent(), destName);
        boolean ret = FileUtil.copyFile(fileOrig.getPath(), fileDest.getPath());
        if (!ret) {
            return "Unable to copy data!";
        }
        
        deltaParser = new DeltaTParser(source);
        correctedPosition = deltaParser.getCorrectedPosition();
        
        BathymetrySwath nextSwath = null; 
        DeltaTHeader nextHeader = null;
        ByteBuffer nextHeaderBuf = null;
        
        do {
            // 33-46    -   GNSS Ships Positon Latitude (14 bytes) "_dd.mm.xxxxx_N" dd = degrees, mm = minutes, xxxxx = decimal Minutes, _ = Space, N = North or S = South
            // 47-60    -   GNSS Ships Postion Longitude (14 byes) "ddd.mm.xxxxx_E" ddd= degrees, mm = minutes, xxxxx = decimal minutes, E = East or W = West
            
            nextSwath = deltaParser.nextSwath(); 
            nextHeader = getNextHeader();
            nextHeaderBuf = getNextHeaderBuffer();

            long nextSwathTimeStamp = nextSwath.getTimestamp();
            SystemPositionAndAttitude pos = correctedPosition.getPosition(nextSwathTimeStamp);
            LocationType posLoc = pos.getPosition();
            posLoc = posLoc.getNewAbsoluteLatLonDepth();
            
            double[] latDM = CoordinateUtil.decimalDegreesToDM(AngleCalc.nomalizeAngleDegrees180(posLoc.getLatitudeDegs()));
            double[] lonDM = CoordinateUtil.decimalDegreesToDM(AngleCalc.nomalizeAngleDegrees180(posLoc.getLongitudeDegs()));
            
            latDM[1] = MathMiscUtils.round(latDM[1], 4);
            lonDM[1] = MathMiscUtils.round(lonDM[1], 4);
            
        } while (nextSwath != null);
        
        return "Export to 83P completed successfully";
    }

    /**
     * @return
     */
    private ByteBuffer getNextHeaderBuffer() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return
     */
    private DeltaTHeader getNextHeader() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public static void main(String[] args) {
        double[] latDM = CoordinateUtil.decimalDegreesToDM(AngleCalc.nomalizeAngleDegrees180(38.276276276766));
        double[] lonDM = CoordinateUtil.decimalDegreesToDM(AngleCalc.nomalizeAngleDegrees180(13.453));
        
//        latDM[1] = MathMiscUtils.round(latDM[1], 4);
//        lonDM[1] = MathMiscUtils.round(lonDM[1], 4);
        
        String latStr = CoordinateUtil.dmToLatString(latDM[0], latDM[1], 4);
        String lonStr = CoordinateUtil.dmToLonString(lonDM[0], lonDM[1], 4);
        
        latStr = latStr.replaceAll("[NSEW]", ".");
        lonStr = lonStr.replaceAll("[NSEW]", ".");
        
        System.out.println(latStr);
        System.out.println(lonStr);
    }
}
