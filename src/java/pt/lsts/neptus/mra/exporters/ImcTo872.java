/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Correia
 * Jun 22, 2012
 */
package pt.lsts.neptus.mra.exporters;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * Class to extract data from a LogSource and generate Imagenex .872 file from data acquired from YellowFin Sidescan Sonar
 * @author jqcorreia
 *
 */
public class ImcTo872 {
    DataOutputStream os;
    IMraLog pingLog;
    int multiBeamEntityId;
    LsfLogSource log;
    
    public ImcTo872(LsfLogSource log) {
        try {
            File outFile = new File(log.getFile("Data.lsf").getParentFile() + "/sidescan.872");
            os = new DataOutputStream(new FileOutputStream(outFile));
            this.log = log;
            pingLog = log.getLog("SidescanPing");
            convert();
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void convert() {
        IMCMessage pingMsg = pingLog.firstLogEntry();
        int c = 0;
        byte[] buffer;
        byte[] zeroFill = new byte[4093-3000+1];
        
        // Build zeroFill padding
        for(int i = 0; i < zeroFill.length; i++) {
            zeroFill[i] = 0;
        }
        
        try {
            while (pingMsg != null) {
                // Check for Sidescan message and multibeam entity 
//                if(log.getEntityName(pingMsg.getInteger("src"), pingMsg.getInteger("src_ent")).equals("Sidescan")) {
                    buffer = new byte[pingMsg.getRawData("data").length];
                    System.arraycopy(pingMsg.getRawData("data"), 0, buffer, 0, buffer.length);
                    
                    // Header
                    os.write("872".getBytes());
                    os.writeByte(0); // File version 0 = 1000 points per channel, 8 bit data
                    os.writeInt(++c);
                    os.writeShort(4096); // Number of total bytes to read per ping
                    os.writeShort(1000); // Data points per channel
                    os.writeByte(1); // Byte per point
                    os.writeByte(8); // Data point bit depth
                    os.writeByte(0); // Type of GPS messages, number of gps messages
                    os.writeShort(3000); // GPS String offset
                    os.writeShort(0); // Event annotation counter = 0 
                    os.write("01-JAN-2001\0".getBytes());
                    os.write("00:00:00\0".getBytes());
                    os.write(".000\0".getBytes());
                    os.writeByte(1); // Medium Frequency
                    os.writeByte(7); // 7 = 30m
                    os.writeByte(0); // Data gain
                    os.writeByte(30); // 30 = Balanced channels
                    os.writeShort(63); // Repetition rate in ms
                    os.writeShort(15000); // Sound velocity ms * 10
                    os.write("IGXB".getBytes());
                    os.write(new byte[] {0,0,0,0,0,0,0,0,0}); // Sonar return data header
                    
                    os.writeInt(0); // Reserved always 0
                    os.writeByte(1); // Yellow fin AUV
                    os.writeByte(0); // Real range (?)
                    byte fill[] = new byte[999-72+1];
                    for(int z = 0; z < fill.length; z++)
                        fill[z] = 0;
                    
                    
                    // Writing stream data
                    os.write(fill); // First zero fill
                    os.write(buffer); // 2000 data points regarding Port and starboard channels
                    os.write(zeroFill); // Second zero fill
                    os.writeShort(8192); // Bytes to previous ping
//                } 
                pingMsg = pingLog.nextLogEntry();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        NeptusLog.pub().info("<###>end");
    }
    
    public static void main(String args[]) throws Exception {
        new ImcTo872(new LsfLogSource(
                "/home/jqcorreia/lsts/dune/build/log/115016_Demo_1.6m_900rpm/Data.lsf", null));
    }
}
