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
 * Apr 29, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 * @author hfq
 * bytes 0 through 255 contain the following File Header info
 * 
 * bytes            description
 * 0-2      -   ASCII '8' '3' 'P'
 * 3        -   File Version 10=v1.10
 * 4-5      -   Total bytes 'N' - number of bytes that are written to the disk for this ping
 *              N = 256 + (2*number_of_beams)
 * 6        -   reserved - always 0;
 * 7        -   reserved - always 0;
 * 8-19     -   Date - Sonar Ping Interrogation Timestamp - system date string (12 bytes) "DD-MMM-YYYY"
 * 20-28    -   Time - Sonar Ping Interrogation Timestamp - system time, null terminated string (9 bytes)
 * 29-32    -   Hundreths of Seconds - Hundredths of Seconds - system time, null terminated string (4 bytes) ".hh"
 * 33-46    -   GNSS Ships Positon Latitude (14 bytes) "_dd.mm.xxxxx_N" dd = degrees, mm = minutes, xxxxx = decimal Minutes, _ = Space, N = North or S = South
 * 47-60    -   GNSS Ships Postion Longitude (14 byes) "ddd.mm.xxxxx_E" ddd= degrees, mm = minutes, xxxxx = decimal minutes, E = East or W = West
 * 61       -   GNSS Ships Speed - Speed = (Byte 61)/10 in knots
 * 62-63    -   GNSS Ships Heading / Ships Course - Heading * 10 (in degrees)
 * 64-65    -   Pitch Angle (from Orientation Module) - (Pitch Angle*10)+900
 *              P = bit 7 of byte 64
 *              if 'P' = 0, Pitch Angle = 0 degrees
 *              if 'P' = 1, Pitch Angle = [[((Byte 64 & 0x7F)<<8) | (byte 65)}-900]/10
 * 66-67    -   Roll Angle (from Orientation Module) - (Roll angle*10)+900
 *              R = bit 7 of byte 66
 *              if 'R' = 0, Roll Angle = 0 degrees
 *              if 'R' = 1, Roll Angle = [[((Byte 66 & 0x7F)<<8) | (Byte 67)]-900]/10
 * 68-69    -   Heading angle (from Orientation Module)
 *              H = but 7 of byte 68
 *              if 'H' = 0, Heading Angle = 0 degrees
 *              if 'H' = 1, Heagin Angle = [((byte 68 & 0x7F)<<8) | (Byte 69)]/10
 * 70-71    -   Number of Beams
 * 72-73    -   Number of Samples Per Beam
 * 74-75    -   Sector Size (in degrees)
 * 76-77    -   Start Angle (Beam 0 angle) - [Start Angle (in degrees) + 180]*100
 * 78       -   Angle Increment - Angle spacing per beam = (Byte 78)/100 in degrees
 * 79-80    -   Acoustic Range in meters
 * 81-82    -   Acoustic Frequency (in KHz)
 * 83-84    -   Sound Velocity (in meters/second)*10
 *              if 'V' = 0, Sound Velocity = 1500.0 m/s
 *              if 'V' = 1, Sound Velocity = [((Byte 83 & 0x7F)<<8 |(Byte 84)]/10.0
 * 85-86    -   Range Resolution (in milimeters)
 * 87-88    -   Pulse Length (in microseconds)
 * 89-90    -   Profile Tilt angle (mounting offset) (in degrees) + 180
 * 91-92    -   Repeting Rate (in miliseconds) - time between pings
 * 93-96    -   Ping Number - increment for every ping (4 bytes)
 * 97-99    -   reserved - always 0
 * 100-103  -   Sonar X-Offset - 4-byte single precision floating point number (meters)
 * 104-107  -   Sonar Y-Offset - 4-byte single ... (meters)
 * 108-111  -   Sonar Z-Offset - 4-byte single ... (meters)
 * 112-116  -   Miliseconds - Sonar Ping Interrogation Timestamp (5 bytes) ".mmm"
 * 117      -   Intensity Bytes Included 0 = N0, 1 = Yes
 * 118-119  -   Ping Latency - Time from sonar ping interrogation to actual ping (in units of 100 microseconds)
 * 120-121  -   Data Latency - time from sonar ping interrogation to 83P UDP datagram (in units of 100 microseconds)
 * 122      -   Sample Rate ( 0 = Standard Resolution (1 in 500) - 1 = High Resolution (1 in 5000)
 * 123      -   Options Flags 
 *              Bit 0 - 1 = data is corrected for roll
 *              Bit 1 - 1 = data is corrected for ray bending
 *              Bit 2 - 1 = sonar is operating in overlapped mode
 *              Bit 3 - 0 .... all others are 0
 * 124      -   Reserved - 0;
 * 125      -   Number of Pings Averaged - 0 to 25
 * 126-127  -   Center Ping time Offset - The Center Pint time offset is the time difference between the center ping interrogation 
 *              and the current ping interrogation.
 *              Current Ping time = Sonar Ping Interrogation Timespamp - Center Ping Time Offset + Ping Latency;
 * 128-131  -   Heave (from External Sensor) - 4 byte single precision floating point number
 * 132      -   User Defined Byte (copy of byte 45)
 * 133-136  -   Altitude - 4-byte single precision floating point number
 * 137      -   External Sensor Flags
 * 138-141  -   Pitch Angle (from External Sensor)
 * 142-145  -   Roll Angle  (from External Sensor)
 * 146-149  -   Heading Angle (From External Sensor)
 * 150      -   Transmit Scan Flag
 * 151-154  -   Transmit Scan Angle
 * 155-255  -   Reserved - always 0      
 */
public class MultibeamDeltaTHeader {
    ByteBuffer buf;

    String fileType; // bytes 0-2 - ASCII '8' '3' 'P'
    byte fileVersion; // byte 3 - 0 = v1.xx
    short numberBytesPing; // 'N' - number of bytes that are written to the disk for this ping
                        // N = 256 + (2*number_of_beams)
    byte intensity;     // Intensity Bytes included 0 = No, 1 = Yes
    
    String systemDate;
    String systemTime;
    String systemTimeHundredsSeconds;
    long timeStamp;
    Calendar cal;
    
    String gnssShipPosLat;
    String gnssShipPosLong;    
    //byte gnssShipSpeed;
    float gnssShipSpeedKnots;
    float gnssShipSpeedMS;
    float gnssShipHeading;
    
    float pitchAngle;
    float rollAngle;
    float headingAngle;
    
    short numberBeams;
    short numberSamplesPerBeam;
    short sectorSize;           // whole swath angle in degrees, from beam 0 to n
    float startAngle;           // start -60 degrees on beam 0, end at +60 on beam n
    
    //byte angleIncrement;        // angle spacing per beam = (BYTE 78)/100 in degrees  <- tem de ser float
    float angleIncrement;
    short acousticRange;        // in meters
    short acousticFrequency;    // in kHz
    //short soundVelocity;        // (m/s)
    float soundVelocity;
    short rangeResolution;      // milimeters
    short pulseLength;          // microseconds
    //int profileTiltangle;     // mounting offset - Profile tilt Angle (in degrees) + 180
    float profileTiltAngle;
    short repetitionRate;       // in miliseconds - time between pings
    int pingNumber;             // increment for every ping (4 bytes)
    
    float sonarXOffset;
    float sonarYOffset;
    float sonarZOffset;
    
    String miliseconds;
    
    short pingLatency;
    short dataLatency;
    byte sampleRate;
    byte optionFlags;
    byte numberOfPingsAveraged;
    short centerPingtimeOffSet;
    float heave;
    byte userDefinedByte;
    float altitude;
    byte externalSensorFlags;
    float externalPitchAngle;
    float externalRollAngle;
    float externalHeadingAngle;
    byte transmitScanFlag;
    float transmitScanAngle;
    
    public MultibeamDeltaTHeader(ByteBuffer buf) {
        this.buf = buf;
    }

    /**
     * @param buf
     */
    public void parseHeader() {
        fileType = parseFileType();
        fileVersion = buf.get(3);
        numberBytesPing = buf.getShort(4);
        intensity = buf.get(117);
        buf.position(8);
        systemDate = parseSystemDate();
        systemTime = parseSystemTime();
        systemTimeHundredsSeconds = parseTimeHundredsSeconds();
        
        gnssShipPosLat = parsePositionLatitude();
        gnssShipPosLong = parsePositionLongitude();

        gnssShipSpeedKnots = (buf.get(61) / 10f);
        gnssShipSpeedMS = convertKnotsToMetersPerSecond(gnssShipSpeedKnots);
        
        gnssShipHeading = buf.getShort(62) * 10f;
        
        pitchAngle = parsePitchAngle();
        rollAngle = parseRollAngle();
        headingAngle = parseHeadingAngle();

        numberBeams = buf.getShort(70);
        numberSamplesPerBeam = buf.getShort(72);
        sectorSize = buf.getShort(74);
        startAngle = buf.getShort(76) / 100f - 180f;
        angleIncrement = buf.get(78)/100f;
        acousticRange = buf.getShort(79);
        acousticFrequency = buf.getShort(81);
        
        soundVelocity = parseSoundVelocity();

        rangeResolution = buf.getShort(85);
        pulseLength = buf.getShort(87);
        profileTiltAngle = buf.getShort(89) + 180f;
        
        repetitionRate = buf.getShort(91);
        pingNumber = buf.getInt(93);
        
        sonarXOffset = buf.getFloat(100);
        sonarYOffset = buf.getFloat(104);
        sonarZOffset = buf.getFloat(108);
        
        miliseconds = parseMiliseconds();
        
        pingLatency = buf.getShort(118);
        dataLatency = buf.getShort(120);
        sampleRate = buf.get(122);
        optionFlags = buf.get(123);
        numberOfPingsAveraged = buf.get(125);
        centerPingtimeOffSet = buf.getShort(126);
        heave = buf.getFloat(128);
        userDefinedByte = buf.get(132);
        altitude = buf.getFloat(133);
        
        //printDeltaTHeaderInfo();
    }

    /**
     * @return
     */
    private String parseMiliseconds() {
        buf.position(112);
        byte milisbuf[] = new byte[5];
        buf.get(milisbuf, 0, 5);
        return (new String(milisbuf));
    }

    /**
     * @param gnssShipSpeedKnots2
     * @return
     */
    private float convertKnotsToMetersPerSecond(float gnssShipSpeedKnots2) {
        return (gnssShipSpeedKnots2 * 0.51444f);
    }

    /**
     * @return
     */
    private float parseSoundVelocity() {
        float vel;
        byte vel83 = buf.get(83);
        byte vel84 = buf.get(84);
        
        if (!isBitSet(vel83, 7))
            return (float) (1500.0);
        else {
            vel = (float) ((((vel83 & 0x7F) << 8) | vel84)/10.0);
            return vel;
        }
    }

    /**
     * @return
     * FIX ME
     */
    private float parseStartAngle() {
        //byte start76 = buf.get(76);
        //byte start77 = buf.get(77);
        float start = (buf.getShort(76) / 100f - 180f);        
    
        return (float) start;
    }

    /**
     * @return
     */
    private float parseHeadingAngle() {
        byte heading68 = buf.get(68);
        if (!isBitSet(heading68, 7)) {      // H = 0
            return 0;
        }
        else {
            byte heading69 = buf.get(69);   // H = 1
            float heading = (((heading68 & 0x7F) << 8) | heading69)/10f;
            return heading;
        }      
    }

    /**
     * @return
     * FIX ME!!!
     */
    private float parseRollAngle() {
        byte roll66 = buf.get(66);
        if (!isBitSet(roll66, 7)) {
            return 0;
        }
        else {
            byte roll67 = buf.get(67);
            float roll = (((roll66 & 0x7F << 8) | roll67) - 900f)/10f;
            return roll;
        }
    }

    /**
     * @return
     *  // FIXME - não retorna o valor certo
     */
    private float parsePitchAngle() {
        byte pitch64 = buf.get(64);
        if (!isBitSet(pitch64, 7))
            return 0;
        else {
            byte pitch65 = buf.get(65);
            float pitchf = ((((pitch64 & 0x7F) << 8) | pitch65)/10.0f);
            return pitchf;
        }       
    }

    /**
     * @return
     */
    private String parsePositionLongitude() {
        String posLong;
        byte[] posLongBuf = new byte[14];
        buf.get(posLongBuf, 0, 14);
        posLong = new String(posLongBuf);
        return posLong;
    }

    /**
     * @return
     */
    private String parsePositionLatitude() {
        String posLat;
        byte[] posLatBuf = new byte[14];
        buf.get(posLatBuf, 0, 14);
        posLat = new String(posLatBuf);
        return posLat;
    }

    /**
     * @return
     */
    private String parseTimeHundredsSeconds() {
        String systemTimeHundredsSeconds;
        byte[] systemTimeBuf = new byte[4];
        buf.get(systemTimeBuf, 0, 4);
        systemTimeHundredsSeconds = new String(systemTimeBuf);
        return systemTimeHundredsSeconds;
    }

    /**
     * @return
     */
    private String parseSystemTime() {
        String systemTime;
        byte[] systemTimeBuf = new byte[9];
        buf.get(systemTimeBuf, 0, 9);
        systemTime = new String(systemTimeBuf);
        return systemTime;
    }

    /**
     * @return
     */
    private String parseSystemDate() {
        String systemDate;
        byte[] systemDateBuf = new byte[12];
        buf.get(systemDateBuf, 0, 12);
        systemDate = new String(systemDateBuf);
        return systemDate;
    }

    /**
     * 
     */
    private String parseFileType() {
        byte[] byteBuf = new byte[3];
        byteBuf[0] = buf.get(0);
        byteBuf[1] = buf.get(1);
        byteBuf[2] = buf.get(2);
        String fileTypeStr = null;
        try {
            fileTypeStr = new String(byteBuf, 0, byteBuf.length, "ASCII");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        };
        return fileTypeStr;       
    }
    
    private static Boolean isBitSet (short sh, int bit) {
        return (sh & (1 << bit)) != 0;
    }
    
    private static Boolean isBitSet (byte b, int bit) {
        return (b & (1 << bit)) != 0;
    }
    
    /**
     * FIXME - change all system out to Neptus Log
     */
    private void printDeltaTHeaderInfo() {
        System.out.println("File Type: " + fileType);
        System.out.println("File Version: " + fileVersion);
        System.out.println("Number of bytes for this ping N: " + numberBytesPing);
        System.out.println("Intensity 0 = No, 1 = Yes: " + intensity);
        System.out.println("System Date: " + systemDate);
        System.out.println("System Time: " + systemTime);
        System.out.println("System Time Hundreds seconds: " + systemTimeHundredsSeconds);
        System.out.println("GNSS AUV Position Latitude: " + gnssShipPosLat);    
        System.out.println("GNSS AUV Position Longitude: " + gnssShipPosLong);
        System.out.println("GNSS AUV Speed: " + gnssShipSpeedKnots + " knots");
        System.out.println("GNSS AUV Speed: " + gnssShipSpeedMS + " m/s");    
        System.out.println("GNSS AUV Course/Heading: " + gnssShipHeading);    
        System.out.println("Pitch Angle: " + pitchAngle);
        System.out.println("Roll Angle: " + rollAngle);
        System.out.println("Heading Angle: " + headingAngle);
        System.out.println("Number of beams: " + numberBeams);
        System.out.println("Number Samples Per Beam: " + numberSamplesPerBeam);  
        System.out.println("Sector Size: " + sectorSize);
        System.out.println("Start angle: " + startAngle);
        System.out.println("Angle Increment: " + angleIncrement);
        System.out.println("Acoustic Range: " + acousticRange);
        System.out.println("Acoustic Frequency: " + acousticFrequency + " kHz");
        System.out.println("Sound Velocity: " + soundVelocity + " m/s");
        System.out.println("Range Resolution: " + rangeResolution + " (mm)");
        System.out.println("Pulse Length: " + pulseLength + " microseconds");
        System.out.println("Profile tilt angle: " + profileTiltAngle);
        System.out.println("Repetition rate: " + repetitionRate + " miliseconds");
        System.out.println("Ping Number: " + pingNumber);
        System.out.println("Sonar X-Offset: " + sonarXOffset + " m");
        System.out.println("Sonar Y-Offset: " + sonarYOffset + " m");
        System.out.println("Sonar Z-Offset: " + sonarZOffset + " m");
        System.out.println("Miliseconds: " + miliseconds);
        System.out.println("Ping Latency (in units of 100 microseconds): " + pingLatency);
        System.out.println("Data Latency (in units of 100 microseconds): " + dataLatency);
        System.out.println("Sample Rate: " + sampleRate);
        System.out.println("Option flags: " + optionFlags);
        System.out.println("Number of Pings Averaged: " + numberOfPingsAveraged);
        System.out.println("Center Ping Time Offset (in units of 100 microseconds): " + centerPingtimeOffSet);
        System.out.println("Heave: " + heave + " meters");
        System.out.println("User defined Byte: " + userDefinedByte);
        System.out.println("Altitude: " + altitude);
    }
}
