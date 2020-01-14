/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 5, 2013
 */
package pt.lsts.neptus.mra.importers.jsf;

import java.nio.ByteBuffer;

import pt.lsts.neptus.NeptusLog;

/**
 * @author jqcorreia
 *
 */
public class JsfSonarData {
    enum Units {
        XY_MILI(1),
        LATLON(2),
        XY_DEC(3);
        
        int code;
        
        Units(int code) {
            this.code = code;
        }
        
        int getCode() { return  code; }
    }

    private JsfHeader header;
    
    private long timestamp;
    private int pingNumber;
    
    /*
    Validity flags bitmap:
        Bit 0: Lat Lon or XY valid
        Bit 1: Course valid
        Bit 2: Speed valid
        Bit 3: Heading valid
        Bit 4: Pressure valid
        Bit 5: Pitch roll valid
        Bit 6: Altitude valid
        Bit 7: Reserved
        Bit 8: Water temperature valid
        Bit 9: Depth valid
        Bit 10: Annotation valid
        Bit 11: Cable counter valid
        Bit 12: KP valid
        Bit 13: Position interpolated
    */
    private short validityBitmap;
    
    private int x;
    private int y;
    private int lat;
    private int lon;
    private Units units;

    private short msb; 

    private int numberOfSamples;
    private int depthMillis;
    private int altMillis;
    
    private short heading; // 1/100 of a degree
    private short pitch; // 1/100 of a degree
    private short roll; // 1/100 of a degree
    private float speed; // speed m2 * 10
    
    private short factor;
    private short packetNumber;
    private float frequency;
    private float range;

    private short dataFormat;
    private double[] data;
    
    /**
     * @return the header
     */
    public JsfHeader getHeader() {
        return header;
    }

    /**
     * @param header the header to set
     */
    public void setHeader(JsfHeader header) {
        this.header = header;
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
     * @return the pingNumber
     */
    public int getPingNumber() {
        return pingNumber;
    }

    /**
     * @param pingNumber the pingNumber to set
     */
    public void setPingNumber(int pingNumber) {
        this.pingNumber = pingNumber;
    }

    /**
     * @return the validityBitmap
     */
    public short getValidityBitmap() {
        return validityBitmap;
    }

    /**
     * @param validityBitmap the validityBitmap to set
     */
    public void setValidityBitmap(short validityBitmap) {
        this.validityBitmap = validityBitmap;
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * @return the lat
     */
    public int getLat() {
        return lat;
    }

    /**
     * @param lat the lat to set
     */
    public void setLat(int lat) {
        this.lat = lat;
    }

    /**
     * @return the lon
     */
    public int getLon() {
        return lon;
    }

    /**
     * @param lon the lon to set
     */
    public void setLon(int lon) {
        this.lon = lon;
    }

    /**
     * @return the units
     */
    public Units getUnits() {
        return units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(Units units) {
        this.units = units;
    }
    
    /**
     * @return the speed
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * @param speed the speed to set
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * @return the frequency
     */
    public float getFrequency() {
        return frequency;
    }

    /**
     * @param frequency the frequency to set
     */
    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    /**
     * @return the numberOfSamples
     */
    public int getNumberOfSamples() {
        return numberOfSamples;
    }

    /**
     * @param numberOfSamples the numberOfSamples to set
     */
    public void setNumberOfSamples(int numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    /**
     * @return the depthMillis
     */
    public int getDepthMillis() {
        return depthMillis;
    }

    /**
     * @param depthMillis the depthMillis to set
     */
    public void setDepthMillis(int depthMillis) {
        this.depthMillis = depthMillis;
    }

    /**
     * @return the altMillis
     */
    public int getAltMillis() {
        return altMillis;
    }

    /**
     * @param altMillis the altMillis to set
     */
    public void setAltMillis(int altMillis) {
        this.altMillis = altMillis;
    }

    /**
     * @return the factor
     */
    public short getFactor() {
        return factor;
    }

    /**
     * @param factor the factor to set
     */
    public void setFactor(short factor) {
        this.factor = factor;
    }

    /**
     * @return the heading
     */
    public short getHeading() {
        return heading;
    }

    /**
     * @param heading the heading to set
     */
    public void setHeading(short heading) {
        this.heading = heading;
    }

    /**
     * @return the pitch
     */
    public short getPitch() {
        return pitch;
    }

    /**
     * @param pitch the pitch to set
     */
    public void setPitch(short pitch) {
        this.pitch = pitch;
    }

    /**
     * @return the roll
     */
    public short getRoll() {
        return roll;
    }

    /**
     * @param roll the roll to set
     */
    public void setRoll(short roll) {
        this.roll = roll;
    }

    /**
     * @return the packetNumber
     */
    public short getPacketNumber() {
        return packetNumber;
    }

    /**
     * @param packetNumber the packetNumber to set
     */
    public void setPacketNumber(short packetNumber) {
        this.packetNumber = packetNumber;
    }
    
    
    /**
     * @return the range
     */
    public float getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(float range) {
        this.range = range;
    }

    /**
     * @return the data
     */
    public double[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(double[] data) {
        this.data = data;
    }

    void parseHeader(ByteBuffer buf) {
        // Calculate time stamp
        int pingTime = buf.getInt(0);

        short hours = buf.getShort(160);
        short minutes = buf.getShort(162);
        short seconds = buf.getShort(164);
        
        msb = buf.getShort(16);
        
        pingTime = pingTime - ((hours * 3600) + (minutes * 60) + seconds); // Seconds from 1 Jan 1970 to log data at midnight
        timestamp = pingTime * 1000l + buf.getInt(200);
        pingNumber = buf.getInt(8);
        
        if(buf.getShort(88) == Units.LATLON.getCode()) {
            lon = buf.getInt(80);
            lat = buf.getInt(84);
        }
        else {
            x = buf.getInt(80);
            y = buf.getInt(84);
        }
    
        // 0 = 1 short per sample - Envelope Data 
        // 1 = 2 shorts per sample - Analytic Signal Data, (Real, Imaginary)
        dataFormat = (short) (buf.getShort(34) & 0xFFFF);  
        
        int msbNumberOfSamples = (msb & 0x0F00) << 8; // First 4 bits of msb shifted so only adding is needed to numberOfSamples
        numberOfSamples = buf.getShort(114) & 0xFFFF;
        numberOfSamples = msbNumberOfSamples + numberOfSamples;
        
        range = ((buf.getInt(116) / new Float(Math.pow(10, 9))) * numberOfSamples * 1500) / 2.0f;
        range = Math.round(range);

        int msbStartFreq = (msb & 0x000F) << 16; // First 4 bits of msb shifted so only adding is needed to start Frequency
        frequency = ((buf.getShort(126) & 0xFFFF) + msbStartFreq) / 100.0f;
    
        depthMillis = buf.getInt(136);
        altMillis = buf.getInt(144);
        
        factor = buf.getShort(168);
        
        heading = buf.getShort(172);
        pitch = buf.getShort(174);
        roll = buf.getShort(176);
        
        speed = (buf.getShort(194) / 10f);

        if(numberOfSamples < 0) {
            NeptusLog.pub().info("<###>asdd " + this);
        }
        data = new double[numberOfSamples];
    }

    void parseData(ByteBuffer buf) {
        data = new double[numberOfSamples];
        
        if (dataFormat != 0)
            return;
        
        double w = Math.pow(2, -factor); // Calc the weighting factor outside the loop
        
        for (int i = 0; i < numberOfSamples * 2; i += 2) {
            int s = ((buf.get(i + 1) & 0xFF) << 8) + (buf.get(i) & 0xFF); 
            double d = s * w;

            if (header.getChannel() == 0) {
                data[numberOfSamples - (i / 2) - 1] = d;
            }
            else {
                data[i / 2] = d;
            }
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Freq: " + frequency + " Range: " + range + " pingNumber: " + pingNumber + 
               " channel: " + header.getChannel() + " numberOfSamples: " + numberOfSamples + 
               " factor: " + factor + " speed: " + speed;
    }
}
