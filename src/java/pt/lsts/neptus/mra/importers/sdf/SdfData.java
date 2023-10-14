/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Manuel R.
 * Oct 21, 2014
 */
package pt.lsts.neptus.mra.importers.sdf;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.util.MathMiscUtils;

public class SdfData {
    private SdfHeader header;
    private long[] portData;
    private long[] stbdData;
    private long timestamp;
    private long fixTimestamp;
    private int numSamples;

    void parseData(ByteBuffer buf) {
        numSamples = header.getNumSamples();
        //index 4 of buf ( integer ) has number of samples (that must be equal to header numSamples)
        if (numSamples!=buf.getInt(4)) {
            NeptusLog.pub().info("<###> "+SdfParser.class.getSimpleName() + " :: Sample size mismatch");
            return;
        }

        portData = new long[numSamples];
        stbdData = new long[numSamples];

        // index = first 4bytes (marker) + next 4bytes (indicate num of samples)
        int index = 8; 
        // index2 = numSamples * int (size 4bytes) + 12bytes ([4] marker + [4] num samples first array + [4] num samples 2nd array)
        int index2 = (numSamples*4)+12; 
        
        for (int i=0;i<numSamples; i++){
            long portValue = buf.getInt(index) & 0xffffffffL;  //signed int to unsigned long
            long stbdValue = buf.getInt(index2) & 0xffffffffL; //signed int to unsigned long
            portData[i] = portValue;
            stbdData[i] = stbdValue;
           
            index+=4;
            index2+=4;
        }
    }


    /**
     * @return the numSamples
     */
    public int getNumSamples() {
        return numSamples;
    }


    /**
     * @param numSamples the numSamples to set
     */
    public void setNumSamples(int numSamples) {
        this.numSamples = numSamples;
    }


    /**
     * @return the header
     */
    public SdfHeader getHeader() {
        return header;
    }


    /**
     * @param header the header to set
     */
    public void setHeader(SdfHeader header) {
        this.header = header;
    }


    public void calculateTimeStamp() {
        int year = header.getYear();
        int month = header.getMonth();
        int day = header.getDay();
        int hour = header.getHour();
        int minute = header.getMinute();
        int seconds = header.getSecond();
        double fSeconds = header.getfSecond();
        int milis = (int) (fSeconds * 1000) ;
        
        Calendar cal =  Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(year, month - 1, day, hour, minute, seconds);
        cal.set(Calendar.MILLISECOND, milis);
        setTimestamp(cal.getTimeInMillis());
    }

    public void calculateFixTimeStamp() {
        int year = header.getFixTimeYear();
        int month = header.getFixTimeMonth();
        int day = header.getFixTimeDay();
        int hour = header.getFixTimeHour();
        int minute = header.getFixTimeMinute();
        float secondsWithMillis = header.getFixTimeSecond();
        Pair<Long, Float> splitVal = MathMiscUtils.splitDecimalPart(secondsWithMillis);
        int seconds = Math.toIntExact(splitVal.first());
        int millis = (int) (splitVal.second() * 1000) ;

        Calendar cal =  Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(year, month - 1, day, hour, minute, seconds);
        cal.set(Calendar.MILLISECOND, millis);
        setFixTimestamp(cal.getTimeInMillis());
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the fixTimestamp
     */
    public long getFixTimestamp() {
        return fixTimestamp;
    }

    /**
     * @param fixTimestamp the fixTimestamp to set
     */
    public void setFixTimestamp(long fixTimestamp) {
        this.fixTimestamp = fixTimestamp;
    }

    /**
     * @return the portData
     */
    public long[] getPortData() {
        return portData;
    }

    /**
     * @param portData the portData to set
     */
    public void setPortData(long[] portData) {
        this.portData = portData;
    }


    /**
     * @return the stbdData
     */
    public long[] getStbdData() {
        return stbdData;
    }


    /**
     * @param stbdData the stbdData to set
     */
    public void setStbdData(long[] stbdData) {
        this.stbdData = stbdData;
    }
    
}
