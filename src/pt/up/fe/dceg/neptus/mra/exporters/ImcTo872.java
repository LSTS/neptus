/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Jun 22, 2012
 */
package pt.up.fe.dceg.neptus.mra.exporters;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.util.llf.LsfLogSource;

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
        System.out.println("end");
    }
    
    public static void main(String args[]) throws Exception {
        new ImcTo872(new LsfLogSource(
                "/home/jqcorreia/lsts/dune/build/log/115016_Demo_1.6m_900rpm/Data.lsf", null));
    }
}
