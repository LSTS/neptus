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
 * Jun 19, 2012
 */
package pt.lsts.neptus.mra.exporters;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * Class to extract data from a LogSource and generate Imagenex .837 file from data acquired from Delta T Multibeam 
 * @author jqcorreia
 * 
 */
@PluginDescription
public class ImcTo837 implements MRAExporter {
    DataOutputStream os;
    IMraLog pingLog;
    IMraLog esLog;
    int multiBeamEntityId;
    IMraLogGroup log;
    static int st;
    
    public ImcTo837(IMraLogGroup log) {
        this.log = log;
    }
    
    public String getName() {
        return "IMC to 837";
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        IMraLog log = source.getLog("SonarData");
        if (log == null)
            return false;
        
        IMCMessage first = log.firstLogEntry();
        IMCMessage msg = first;
        
        while(msg != null) {
            // Wait 2 seconds for the first valid multibeam SonarData message //FIXME
            if(msg.getTimestampMillis() > first.getTimestampMillis() + 2000)
                break;
            if(msg.getLong("type") == SonarData.TYPE.MULTIBEAM.value()) {
                return true;
            }
            msg = log.nextLogEntry();
        }
        return false;
    }

    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        try {
            File outFile = new File(log.getFile("Data.lsf").getParentFile() + "/multibeam.837");
            os = new DataOutputStream(new FileOutputStream(outFile));
            pingLog = log.getLog("SonarData");
            esLog = log.getLog("EstimatedState");
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return e.getClass().getSimpleName()+" while exporting to 837: "+e.getMessage();
        }

        IMCMessage pingMsg = pingLog.firstLogEntry();
        
        byte[] buffer;
        byte[] prevBuffer;
        byte[] zeroFill = new byte[236];
        prevBuffer = new byte[16000];
        short pitch;
        short roll;
        short heading;
        String lat = "";
        String lon = "";
        String sTime = "";
        String sMillis = "";
        double res[] = new double[2];
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        new LocationType();

        // Build zeroFill padding
        for(int i = 0; i < zeroFill.length; i++) {
            zeroFill[i] = 0;
        }
        
        try {
            while (pingMsg != null) {
                
                // Check for Sidescan message and multibeam entity 
                if(pingMsg.getInteger("type") == SonarData.TYPE.MULTIBEAM.value()) {
                        
                    IMCMessage esMsg = esLog.getEntryAtOrAfter(pingLog.currentTimeMillis());
                    if(esMsg == null) {
                        roll = 900;
                        pitch = 900;
                        heading = 900;
                    }
                    else {
                        roll = (short) (Math.toDegrees(esMsg.getDouble("phi")));
                        pitch = (short) (Math.toDegrees(esMsg.getDouble("theta")));
                        heading = (short) (Math.toDegrees(esMsg.getDouble("psi")));
                        res = CoordinateUtil.latLonAddNE2(Math.toDegrees(esMsg.getDouble("lat")), Math.toDegrees(esMsg.getDouble("lon")), esMsg.getDouble("x"), esMsg.getDouble("y"));
                        
                        int d = (int)res[0];
                        double m = ((res[0] - d) * 60);
                        lat = String.format(Locale.US, " %02d.%.5f",Math.abs(d),Math.abs(m)) + (d > 0 ? " N" : " S");
                        d = (int)res[1];
                        m = ((res[1] - d) * 60);
                        lon = String.format(Locale.US, "%03d.%.5f",Math.abs(d),Math.abs(m)) + (d > 0 ? " E" : " W");
//                        
                        NeptusLog.pub().info("<###> "+lat);
                        NeptusLog.pub().info("<###> "+lon);
                        
//                        if(heading < 0)
//                            heading = (short) (360 + heading);
                        
                    }
                    long timestamp = esLog.currentTimeMillis();
                    cal.setTimeInMillis(timestamp);
                    
                    String month="";
                    
                    switch(cal.get(Calendar.MONTH)) 
                    {
                        case 0: month = "JAN"; break;
                        case 1: month = "FEB"; break;
                        case 2: month = "MAR"; break;
                        case 3: month = "APR"; break;
                        case 4: month = "MAY"; break;
                        case 5: month = "JUN"; break;
                        case 6: month = "JUL"; break;
                        case 7: month = "AUG"; break;
                        case 8: month = "SEP"; break;
                        case 9: month = "OCT"; break;
                        case 10: month = "NOV"; break;
                        case 11: month = "DEC"; break;
                    }
                    sTime = String.format(Locale.US, "%02d-%s-%d\0%02d:%02d:%02d\0.00\0", 
                            cal.get(Calendar.DAY_OF_MONTH), month, cal.get(Calendar.YEAR), 
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
                    
                    sMillis = String.format(Locale.US, ".%03d\0", cal.get(Calendar.MILLISECOND));
                    
                    
                    buffer = new byte[pingMsg.getRawData("data").length];
                    // Header
                    os.write("837".getBytes());
                    os.writeByte((buffer.length == 8000 ? 10 : 11)); // 10 = 8000(IUX), 11 = 16000(IVX)
                    os.writeShort(buffer.length == 8000 ? 8192 : 16384); // Number of total bytes to read
                    os.writeShort(buffer.length + 13); // Number of bytes only for data points
                    os.write(sTime.getBytes()); // TIMESTAMP
                    os.writeInt(0);
                    os.writeByte(0x83); // 11000011 = 1 Reserved, 1 Xcdr Up, 000 Reserved and 011 Profile
                    os.writeByte(1); // Start gain
                    os.write(new byte[] { 0, 0 }); // Tilt Angle
                    os.write(new byte[] { 0, 0, 7 } ); // Reserved, Reserved, Pings Averaged
                    os.writeByte(18); // Pulse length in us/10
                    os.writeByte(0); // User defined byte
                    os.writeShort(0); // sound speed short ( 0 = 1500ms )
                    os.write(lat.getBytes()); // Lat and Lon NMEA style
                    os.write(lon.getBytes()); 
                    os.writeByte(0); // Speed
                    os.writeShort(0); // Course 
                    os.writeByte(0); // and a Reserved byte as 0
                    os.writeShort(260); // 260Hz operating frequency
                    os.writeShort((pitch*10+900)+32768); // Pitch = 0
                    os.writeShort((roll*10+900)+32768); // Roll
                    
                    os.writeShort(0x8000); // Heading = 0
                    os.writeShort(97); // Repetition rate in ms
                    os.writeByte(50);
                    os.writeShort(0); // 2 reserved bytes 0
                    os.write(sMillis.getBytes());
                    os.writeShort(0);
                    
                    // Sonar return header
                    os.write(buffer.length == 8000 ? "IUX".getBytes() : "IVX".getBytes());
                    os.write(new byte[] { 
                            16,     // Head ID
                            0,      // Serial status
                            7,      // Packet Number
                            36,     // Version
                            (byte)pingMsg.getInteger("max_range"),     // Range
                            0,      // reserved
                            0,      // reserved
                            //3,
                            //-24
                         });
                    os.writeShort(buffer.length); // data bytes

                    System.arraycopy(pingMsg.getRawData("data"), 0, buffer, 0, buffer.length);
                    
                    // Echo values
                    os.write(buffer);
                    os.writeByte(0xFC); // Trailing value always 0xFC
                    
                    // Exta bytes and zero-fill
                    os.writeFloat(0); // Offset X
                    os.writeFloat(0); // Offset Y
                    os.writeFloat(0); // Offset Z
                    os.writeByte(1); // Sensor type (?)
                    os.writeShort(pitch); // Pitch
                    os.writeShort(roll); // Roll   
                    os.writeShort(heading); // Heading
                    os.writeShort(0); // Timer Ticks
                    os.writeShort(0); // Azimuth Head Position
                    os.writeByte(1); // Azimuth Up/Down
                    os.writeFloat(0); // Heave
                    os.write(new byte[] { 0,0,0,0,0,0,0 }); // 7 reserved bytes 
                    if (buffer.length == 8000)
                        os.write(zeroFill, 0, 44);  // in case we have only 8000 bytes                  
                    else
                        os.write(zeroFill);
                    System.arraycopy(buffer, 0, prevBuffer, 0, buffer.length);
                } 
                pingMsg = pingLog.nextLogEntry();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getClass().getSimpleName()+" while exporting to 837: "+e.getMessage();
        }
        NeptusLog.pub().info("<###>end");
        return "Export to 837 completed successfully";
    }
    
//    public static void main(String args[]) throws Exception {
//        while(true) {
//            new ImcTo837(new LsfLogSource(
//                "/home/jqcorreia/lsts/logs/lauv-noptilus-1/20121220/160655_rows_btrack/Data.lsf",null));
//            try {
//                Thread.sleep(1000);
//                st+=10;
//                NeptusLog.pub().info("<###> "+st);
//                break;
//            }
//            catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }

}
