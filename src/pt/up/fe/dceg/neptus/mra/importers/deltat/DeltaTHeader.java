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
 * Author: jqcorreia
 * Apr 2, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.deltat;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.up.fe.dceg.neptus.NeptusLog;

/**
 * @author jqcorreia
 *
 */
public class DeltaTHeader {
    public short numBytes;
    public short numBeams;
    public short samplesPerBeam;
    public short sectorSize;
    public float startAngle;
    public short rangeResolution;
    
    public float angleIncrement;
    public short range;
    
    public double speed;
    
    public long timestamp;
    
    public boolean hasIntensity;     // Intensity Bytes included 0 = No, 1 = Yes
    
    public float soundVelocity;
    
    private static Calendar cal;
    private static Pattern pTimeStamp;
    {
        pTimeStamp = Pattern.compile("([0-9]{2})-([A-Z]{3})-([0-9]{4})\0([0-9]{2}):([0-9]{2}):([0-9]{2})");
        cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    Matcher m;
    
    public void parse(ByteBuffer b) {
        short month = 0;
        byte timestampBuf[] = new byte[25]; // 12 + 9 + 4 
        byte millisBuf[] = new byte[5];
        String timestampStr, millisStr;
        
        numBytes = b.getShort(4);
        numBeams = b.getShort(70);
        samplesPerBeam = b.getShort(72);
        sectorSize = b.getShort(74); 
        startAngle = b.getShort(76) / 100f - 180;
        angleIncrement = b.get(78) / 100f;
        range = b.getShort(79);
        
        byte vel83 = b.get(83);
        if (!isBitSet(vel83, 7))
            soundVelocity = 1500f;
        else {
            byte vel84 = b.get(84);
            soundVelocity = (float) ((((vel83 & 0x7F) << 8) | vel84)/10.0);       
        }      
        //NeptusLog.pub().info("Sound Vel: " + soundVelocity);
        
        rangeResolution = b.getShort(85);
        
        speed = (b.get(61) / 10.0) * 0.51444;
        
        byte hasInt = b.get(117);
        hasIntensity = (hasInt == 1) ? true : false;
        
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
        if (m.group(2).equalsIgnoreCase("FEB"))
            month = 1;
        if (m.group(2).equalsIgnoreCase("MAR"))
            month = 2;
        if (m.group(2).equalsIgnoreCase("APR"))
            month = 3;
        if (m.group(2).equalsIgnoreCase("MAY"))
            month = 4;
        if (m.group(2).equalsIgnoreCase("JUN"))
            month = 5;
        if (m.group(2).equalsIgnoreCase("JUL"))
            month = 6;
        if (m.group(2).equalsIgnoreCase("AUG"))
            month = 7;
        if (m.group(2).equalsIgnoreCase("SEP"))
            month = 8;
        if (m.group(2).equalsIgnoreCase("OCT"))
            month = 9;
        if (m.group(2).equalsIgnoreCase("NOV"))
            month = 10;
        if (m.group(2).equalsIgnoreCase("DEC"))
            month = 11;
        
        cal.set(Integer.valueOf(m.group(3)), month, Integer.valueOf(m.group(1)), Integer.valueOf(m.group(4)),
                Integer.valueOf(m.group(5)), Integer.valueOf(m.group(6)));
        cal.set(Calendar.MILLISECOND, Integer.valueOf(millisStr.substring(1, 4)));
        timestamp = cal.getTimeInMillis();
    }
    
    private static Boolean isBitSet (byte b, int bit) {
        return (b & (1 << bit)) != 0;
    }
}
