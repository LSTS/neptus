/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Renato Campos
 * 14 Nov 2018
 */
package pt.lsts.neptus.mra.importers.i872;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.NMEAUtils;

public class I872Ping {

    public static final int PING_SIZE = 4096; // bytes
    public static final int DATA_SIZE = 1000; // bytes
    
    private static final int GPSOffset = 3000; // bytes
    private static final int GPSStringSize = 100; // bytes
    
    private int[] portChannelData;
    private int[] starboardChannelData;
    private I872Header header;
    private ArrayList<String> gpsData;
    private LocationType locationType;
    
    public I872Ping(ByteBuffer pingInfo) {
        portChannelData = new int[DATA_SIZE];
        starboardChannelData = new int[DATA_SIZE];
        parsePing(pingInfo);
    }

    /**
     * @param pingInfo
     */
    private void parsePing(ByteBuffer buffer) {
        parseHeader(buffer);
        parseData(buffer);
        parseGPSStrings(buffer);     
    }
    
    /**
     * Parses and stores header info.
     * Each ping has an header of 1000 bytes, from 0-999
     */
    private void parseHeader(ByteBuffer buffer) {
        header = new I872Header(buffer, false);
    }
    
    /**
     * Parses and stores data for a ping.
     * 
     */
    private void parseData(ByteBuffer buffer) {
        
        for (int i = 0; i < 1000; i++) {
            portChannelData[i] = (buffer.get() & 0xff);
        }
        for (int i = 0; i < 1000; i++) {
            starboardChannelData[i] = (buffer.get() & 0xff);
        }
    }

    /**
     * 
     */
    private void parseGPSStrings(ByteBuffer buffer) {
        int numberGPSStrings = header.getNumberGPSStrings();
        ArrayList<String> gpsStrings = new ArrayList<String>();
        byte[] gpsBuffer = new byte[GPSStringSize * numberGPSStrings];
        buffer.get(gpsBuffer, 0, gpsBuffer.length);
        for (int i = 0; i < numberGPSStrings; i++) {
            String temp = header.byteArrayToString(gpsBuffer, i*GPSStringSize, GPSStringSize);
            if (temp.trim().length() > 0) {
                gpsStrings.add(temp);
            }
            
        }
        if (gpsStrings.size() != numberGPSStrings) {
            System.err.println("Invalid gps strings at ping: " + header.getPingNumber());
            return;
        }
        Collections.sort(gpsStrings, String.CASE_INSENSITIVE_ORDER);
        String selectedGPSString = gpsStrings.get(header.getGPSType());
        switch(header.getGPSType()) {
            case 0:
                locationType = NMEAUtils.processGLLSentence(selectedGPSString);
                break;
            case 1:
                locationType = NMEAUtils.processGGASentence(selectedGPSString);
                break;
            case 2:
                locationType = NMEAUtils.processRMCSentence(selectedGPSString);
                break;
        }
        /*
        if (locationType == null) {
            System.out.println("Selected gps type: " + header.getGPSType());
            System.out.println("Number of gps strings: " + header.getNumberGPSStrings());
            System.out.println("Failing String: " + selectedGPSString);
            gpsStrings.forEach(s -> System.out.println(s));
        }
        */
    }
    
    /**
     * Get timestamp in which this ping was created
     * @return
     */
    public long getTimestamp() {
        return header.getTimestamp();
    }
    
    /**
     * Get port data
     * @return
     */
    public int[] getPortData() {
        return portChannelData;
    }
    
    /**
     * Get starboard data
     * @return
     */
    public int[] getStarboardData() {
        return starboardChannelData;
    }
    
    /**
     * Get latitude in radians
     * @return
     */
    public double getLatitude() {
        if (locationType != null) {
            return locationType.getLatitudeRads();
        }
        return 0;
    }
    
    /**
     * Get longitude in radians
     * @return
     */
    public double getLongitude() {
        if (locationType != null) {
            return locationType.getLongitudeRads();
        }
        return 0;
    }
    
    public int getRange() {
        return header.getRange();
    }
    
    public int getFrequency() {
        return header.getFrequency();
    }

    



}
