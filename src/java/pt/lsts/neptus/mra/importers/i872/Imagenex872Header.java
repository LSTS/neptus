/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Imagenex872Header {
    
    private static int[] rangeMap = {0,0,0,0,0,10,20,30,40,50,60,80,100,125,150,200};
    /**
     * Each ping header has 1000 bytes
     */
    private final int HEADER_SIZE = 1000;
    /**
     * Number of the ping. First ping may not start in 0.
     */
    private int pingNumber;
    /**
     * 0 -> $GPGLL; 1 -> $GPGGA; 2 -> $GPRMC
     */
    private int gpsType;
    private String date, time, milliseconds;
    private int operatingFrequency;
    private int rangeIndex;
    private int numberGPSStrings;

    public Imagenex872Header(ByteBuffer headerInfo, Boolean onlyTimestamp) {
        try {
            parseHeader(headerInfo, onlyTimestamp);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract all the important information of the header.
     * @param pingBuffer The ping buffer.
     * @throws Exception In case of invalid header
     */
    private void parseHeader(ByteBuffer pingBuffer, Boolean onlyTimestamp) throws Exception {
        byte[] headerBytes = new byte[HEADER_SIZE];
        pingBuffer.get(headerBytes, 0, HEADER_SIZE);
        if (headerBytes[0] != '8' || headerBytes[1] != '7' || headerBytes[2] != '2') {
            throw new Exception("Invalid header");
        }
        parseDate(headerBytes);
        if (onlyTimestamp) {
            return;
        }
        pingNumber = ((headerBytes[4] & 0xff) << 24) | ((headerBytes[5] & 0xff) << 16) |
                ((headerBytes[6] & 0xff) << 8) | (headerBytes[7] & 0xff);
        gpsType = headerBytes[14] >> 4;
        numberGPSStrings = headerBytes[14] % 16;
        operatingFrequency = headerBytes[45];
        rangeIndex = headerBytes[46];
    }

    /**
     * Gets the date, time and milliseconds of the header
     */
    private void parseDate(byte[] headerBytes) {
        date = byteArrayToString(headerBytes, 19, 11);
        time = byteArrayToString(headerBytes, 31, 8);
        milliseconds = byteArrayToString(headerBytes, 40, 4);
    }

    /**
     * Converts a byte array to a string
     * @param originalArray The array which contains the string
     * @param startOffset Initial offset of the string
     * @param length Length of the string
     * @return A string created from the originalArray
     */
    public String byteArrayToString(byte[] originalArray, int startOffset, int length) {
        byte[] dateBytes = new byte[length];
        int maxIndex = startOffset + length;
        for (int i = startOffset; i < maxIndex; i++) {
            dateBytes[i-startOffset] = originalArray[i];
        }
        return new String(dateBytes);
    }
    
    /**
     * Gets the timestamp in which the header was created.
     * @return The timestamp of this header
     */
    public long getTimestamp() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss.SSS");
            String fullDate = date + " " + time + milliseconds;
            Date parsedDate = dateFormat.parse(fullDate);
            return parsedDate.getTime();
        }
        catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public int getFrequency() {
        return operatingFrequency;
    }
    
    public int getNumberGPSStrings() {
        return numberGPSStrings;
    }
    
    public int getGPSType() {
        return gpsType;
    }
    
    public int getRange() {
        return rangeMap[rangeIndex];
    }
    
    public int getPingNumber() {
        return pingNumber;
    }
}
