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
 * Author: jqcorreia
 * Apr 2, 2013
 */
package pt.lsts.neptus.mra.importers.deltat;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jqcorreia
 * @author pdias
 */
public class DeltaTHeader {
    
    public String fileType; // bytes 0-2 - ASCII '8' '3' 'P'
    public byte fileVersion; // byte 3 - 10 = v1.10
    
    public int numBytes; // 'N' - number of bytes that are written to the disk for this ping, N = 256 + (2*number_of_beams)
    public int numBeams;
    public int samplesPerBeam;
    public int sectorSize;
    public float startAngle;
    public int rangeResolution;
    
    public float angleIncrement;
    public int range;
    public int sonarFreqKHz;
    
    public long pingNumber;
    
    public double pulseLength; // Pulse Length (in microseconds)
    public double pulseRepetingRate;// Repeting Rate (in miliseconds) - time between pings
    
    public double speed;
    
    public long timestamp;
    
    public boolean hasIntensity;     // Intensity Bytes included 0 = No, 1 = Yes
    
    public float soundVelocity;
    
    public boolean sampleRateHigh; //Sample Rate ( 0 = Standard Resolution (1 in 500) - 1 = High Resolution (1 in 5000)
    
    public double rollAngleDegreesOrientModule; // equal to pitch
    public double pitchAngleDegreesOrientModule;//Pitch Angle (from Orientation Module) - (Pitch Angle*10)+900|P = bit 7 of byte 64|if 'P' = 0, Pitch Angle = 0 degrees|if 'P' = 1, Pitch Angle = [[((Byte 64 & 0x7F)<<8) | (byte 65)}-900]/10
    public double headingAngleDegreesOrientModule; // the same but no 900

    public Boolean dataIsCorrectedForRoll;
    public Boolean dataIsCorrectedForRayBending;
    public Boolean sonarIsOperatingInOverlappedMode;

    public int numberOfPingsAveraged;

    public String gnssShipPosLat = "";
    public String gnssShipPosLon = "";
    public double gnssShipCourse;
    
    public float sonarXOffset = Float.NaN;
    public float sonarYOffset = Float.NaN;
    public float sonarZOffset = Float.NaN;
    
    public float altitude = Float.NaN;
    
    private static Calendar cal;
    private static Pattern pTimeStamp;
    {
        pTimeStamp = Pattern.compile("([0-9]{2})-([A-Z]{3})-([0-9]{4})\0([0-9]{2}):([0-9]{2}):([0-9]{2})");
        cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    private Matcher m;
    
    public void parse(ByteBuffer b) {
        fileType = parseFileType(b);
        fileVersion = b.get(3);
        
        short month = 0;
        byte timestampBuf[] = new byte[25]; // 12 + 9 + 4 
        byte millisBuf[] = new byte[5];
        String timestampStr, millisStr;
        
        numBytes = b.getShort(4) & 0xFFFF;
        numBeams = b.getShort(70) & 0xFFFF;
        
        pingNumber = b.getInt(93) & 0xFFFFFFFF;
        
        samplesPerBeam = b.getShort(72) & 0xFFFF;
        sectorSize = b.getShort(74) & 0xFFFF; 
        startAngle = (b.getShort(76) & 0xFFFF) / 100f - 180;
        angleIncrement = (b.get(78) & 0xFF) / 100f;
        range = b.getShort(79) & 0xFFFF;
        
        sonarFreqKHz = b.getShort(81) & 0xFFFF;
        
        pulseLength = b.getShort(87) & 0xFFFF;
        pulseRepetingRate = b.getShort(91) & 0xFFFF;
        
        soundVelocity = parseSoundVelocity(b);
        
        rangeResolution = b.getShort(85) & 0xFFFF;
        
        speed = convertKnotsToMetersPerSecond((b.get(61) & 0xFF) / 10.0);
        
        gnssShipCourse = (b.getShort(62) & 0xFFFF) / 10.0;
        
        byte shipLatLonBuf[] = new byte[14];
        b.position(33);
        b.get(shipLatLonBuf, 0, 14);
        gnssShipPosLat = new String(shipLatLonBuf);
        b.position(47);
        b.get(shipLatLonBuf, 0, 14);
        gnssShipPosLon = new String(shipLatLonBuf);
        
        sonarXOffset = b.getFloat(100);
        sonarYOffset = b.getFloat(104);
        sonarZOffset = b.getFloat(108);
        
        byte hasInt = b.get(117);
        hasIntensity = (hasInt == 1) ? true : false;

        byte srate = b.get(122);
        sampleRateHigh = (srate == 0) ? false : true;

        // Timestamp processing
        b.position(8);
        b.get(timestampBuf, 0, 25);
        
        b.position(112);
        b.get(millisBuf, 0, 5);
          
        timestampStr = new String(timestampBuf);
        millisStr = new String(millisBuf);
        
        m = pTimeStamp.matcher(timestampStr);
        m.find();

        if (m.group(2).equalsIgnoreCase("JAN"))
            month = 0;
        else if (m.group(2).equalsIgnoreCase("FEB"))
            month = 1;
        else if (m.group(2).equalsIgnoreCase("MAR"))
            month = 2;
        else if (m.group(2).equalsIgnoreCase("APR"))
            month = 3;
        else if (m.group(2).equalsIgnoreCase("MAY"))
            month = 4;
        else if (m.group(2).equalsIgnoreCase("JUN"))
            month = 5;
        else if (m.group(2).equalsIgnoreCase("JUL"))
            month = 6;
        else if (m.group(2).equalsIgnoreCase("AUG"))
            month = 7;
        else if (m.group(2).equalsIgnoreCase("SEP"))
            month = 8;
        else if (m.group(2).equalsIgnoreCase("OCT"))
            month = 9;
        else if (m.group(2).equalsIgnoreCase("NOV"))
            month = 10;
        else if (m.group(2).equalsIgnoreCase("DEC"))
            month = 11;
        
        cal.set(Integer.valueOf(m.group(3)), month, Integer.valueOf(m.group(1)), Integer.valueOf(m.group(4)),
                Integer.valueOf(m.group(5)), Integer.valueOf(m.group(6)));
        cal.set(Calendar.MILLISECOND, Integer.valueOf(millisStr.substring(1, 4)));
        timestamp = cal.getTimeInMillis();
        
        
        byte vel66 = b.get(66);
        if (!isBitSet(vel66, 7)){
            rollAngleDegreesOrientModule = 0;
        }
        else {
            byte vel67 = b.get(67);
            rollAngleDegreesOrientModule = ((((vel66 & 0x7F) << 8) | (vel67 & 0xFF)) - 900) / 10.0;
        }    
        byte vel64 = b.get(64);
        if (!isBitSet(vel64, 7)){
            pitchAngleDegreesOrientModule = 0;
        }
        else {
            byte vel65 = b.get(65);
            pitchAngleDegreesOrientModule = ((((vel64 & 0x7F) << 8) | (vel65 & 0xFF)) - 900) / 10.0;
        }    
        byte vel68 = b.get(68);
        if (!isBitSet(vel68, 7)){
            headingAngleDegreesOrientModule = 0;
        }
        else {
            byte vel69 = b.get(69);
            headingAngleDegreesOrientModule = (((vel68 & 0x7F) << 8) | (vel69 & 0xFF)) / 10.0;
        }    

        byte vel123 = b.get(123);
        dataIsCorrectedForRoll = isBitSet(vel123, 0); // Bit 0 - 1 = data is corrected for roll
        dataIsCorrectedForRayBending = isBitSet(vel123, 1); // Bit 1 - 1 = data is corrected for ray bending
        sonarIsOperatingInOverlappedMode = isBitSet(vel123, 2); // Bit 2 - 1 = sonar is operating in overlapped mode
        
        numberOfPingsAveraged = b.get(125); // Number of Pings Averaged - 0 to 25
        altitude = Float.intBitsToFloat(Integer.reverseBytes(b.getInt(133)));
    }
    
    /**
     * @param b
     * @return
     */
    private float parseSoundVelocity(ByteBuffer b) {
        byte vel83 = b.get(83);
        if (!isBitSet(vel83, 7)){
            return 1500f;
        }
        else {
            byte vel84 = b.get(84);
            float sv = (float) ((((vel83 & 0x7F) << 8) | (vel84 & 0xFF)) / 10.0);
            return sv;
        }    
    }

    /**
     * @param speedKnots
     * @return
     */
    private double convertKnotsToMetersPerSecond(double speedKnots) {
        return speedKnots * 0.51444;
    }

    private static Boolean isBitSet (byte b, int bit) {
        return (b & (1 << bit)) != 0;
    }
    
    private String parseFileType(ByteBuffer b) {
        byte[] byteBuf = new byte[3];
        byteBuf[0] = b.get(0);
        byteBuf[1] = b.get(1);
        byteBuf[2] = b.get(2);
        String fileTypeStr = null;
        try {
            fileTypeStr = new String(byteBuf, 0, byteBuf.length, "ASCII");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return fileTypeStr;
    }
}
