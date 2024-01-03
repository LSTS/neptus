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
 * Author: rasm
 * Apr 12, 2011
 */

package pt.lsts.neptus.plugins.gps.device;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;

/**
 * NMEA Parser.
 * 
 * @author Ricardo Martins
 */
public class NMEA {
    /** States of the finite state machine. */
    private enum State {
        /** Waiting for the dollar sign. */
        DOLLAR,
        /** Processing sentence body. */
        BODY,
        /** Waiting for the first checksum character. */
        CSUM1,
        /** ! Waiting for the second checksum character. */
        CSUM2,
        /** ! Waiting for carriage-return. */
        CR,
        /** Waiting for line-feed. */
        LF
    };

    /** Chunk buffer. */
    private final ByteBuffer chunk = ByteBuffer.allocate(1024);
    /** NMEA sentence parts. */
    private final Vector<String> chunks = new Vector<String>();
    /** Data of latest GPS fix. */
    private final Fix fix = new Fix();
    /** Current state machine state. */
    private State state;
    /** Chunk index. */
    private int chunkIdx;
    /** Received checksum. */
    private byte rcsum;
    /** Computed checksum. */
    private byte ccsum;
    /** True if sentence has checksum. */
    private boolean hasCsum;
    /** Listener. */
    private FixListener listener;

    /**
     * Default constructor.
     * 
     * @param aListener
     *            fix listener.
     */
    public NMEA(FixListener aListener) {
        listener = aListener;
        clear();
    }

    /**
     * Parse one byte.
     * 
     * @throws InvalidChecksumException
     *             if an invalid checksum is found.
     */
    public void parse(byte b) throws InvalidChecksumException {
        switch (state) {
            case DOLLAR:
                if (b == '$')
                    state = State.BODY;
                break;

            case BODY:
                if (b == ',') {
                    ccsum ^= b;
                    appendChunk();
                }
                else if (b == '*') {
                    state = State.CSUM1;
                    hasCsum = true;
                    appendChunk();
                }
                else if (b == '\r') {
                    state = State.LF;
                }
                else {
                    ccsum ^= b;
                    chunk.put(b);
                    ++chunkIdx;
                }
                break;

            case CSUM1:
                rcsum = parseNibble(b, 4);
                state = State.CSUM2;
                break;

            case CSUM2:
                rcsum |= parseNibble(b, 0);
                state = State.CR;
                break;

            case CR:
                if (b == '\r')
                    state = State.LF;
                break;

            case LF:
                if (b == '\n') {
                    if (hasCsum && (rcsum != ccsum))
                        throw new InvalidChecksumException(ccsum, rcsum);
                    interpret();
                }
                clear();
                break;
        }
    }

    /**
     * Clear/reset parser.
     */
    private void clear() {
        state = State.DOLLAR;
        rcsum = 0;
        ccsum = 0;
        hasCsum = false;
        chunk.clear();
        chunkIdx = 0;
        chunks.clear();
    }

    /**
     * Interpret the parsed sentence.
     */
    private void interpret() {
        if ("GPGGA".equals(chunks.get(0)))
            interpretGPGGA();
        else if ("GPVTG".equals(chunks.get(0)))
            interpretGPVTG();
    }

    /**
     * Interpret a GPGGA sentence.
     */
    private void interpretGPGGA() {
        // Validity.
        int quality = Integer.parseInt(chunks.get(6));
        if (quality == 1) {
            fix.setType(Fix.Type.STANDALONE);
            fix.setValid(true);
        }
        else if (quality == 2) {
            fix.setType(Fix.Type.DIFFERENTIAL);
            fix.setValid(true);
        }
        else {
            fix.setType(Fix.Type.STANDALONE);
            fix.setValid(false);
        }

        // Position.
        fix.setLatitude(parseLatitude(chunks.get(2), chunks.get(3)));
        fix.setLongitude(parseLongitude(chunks.get(4), chunks.get(5)));
        fix.setHeight(parseReal(chunks.get(9)) + parseReal(chunks.get(11)));

        // Satellites.
        fix.setSatellites(parseInteger(chunks.get(7)));

        // Dilution.
        fix.setHorizontalDilution(parseReal(chunks.get(8)));

        // Time.
        fix.setTime(parseTime(chunks.get(1)));

        listener.onFix(fix);
    }

    /**
     * Interpret a GPVTG sentence.
     */
    private void interpretGPVTG() {
        if (chunks.get(1).length() > 0)
            fix.setCog(parseReal(chunks.get(1)));

        double value = parseReal(chunks.get(7));
        fix.setSog(value * 1000.0 / 3600.0);
    }

    /**
     * Parse time.
     * 
     * @param str
     *            string representing a latitude coordinate in NMEA format.
     * @return the parsed time or 0 on all fields if parsing is not possible.
     */
    private Time parseTime(String str) {
        Time time = new Time();

        try {
            time.setHour(Integer.parseInt(str.substring(0, 2), 10));
            time.setMinutes(Integer.parseInt(str.substring(2, 4), 10));
            time.setSeconds(Double.parseDouble(str.substring(4)));
            return time;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return time;
    }

    /**
     * Parse an real number.
     * 
     * @param str
     *            string representing a real number.
     * @return the parsed number or 0 if parsing is not possible.
     */
    private double parseReal(String str) {
        try {
            return Double.parseDouble(str);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Parse an integer in base 10.
     * 
     * @param str
     *            string representing an integer.
     * @return the parsed integer or 0 if parsing is not possible.
     */
    private int parseInteger(String str) {
        try {
            return Integer.parseInt(str, 10);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Parse latitude.
     * 
     * @param cord
     *            string representing a latitude coordinate in NMEA format.
     * @param hemisphere
     *            string representing the hemisphere (N or S).
     * @return the parsed latitude in degrees or 0 if parsing is not possible.
     */
    private double parseLatitude(String cord, String hemisphere) {
        try {
            int degrees = Integer.parseInt(cord.substring(0, 2));
            double minutes = Double.parseDouble(cord.substring(2));

            double value = degrees + (minutes / 60.0);

            if (hemisphere.equals("S"))
                value = -value;

            return value;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Parse longitude.
     * 
     * @param cord
     *            string representing a longitude coordinate in NMEA format.
     * @param hemisphere
     *            string representing the hemisphere (W or E).
     * @return the parsed longitude in degrees or 0 if parsing is not possible.
     */
    private double parseLongitude(String cord, String hemisphere) {
        try {
            int degrees = Integer.parseInt(cord.substring(0, 3));
            double minutes = Double.parseDouble(cord.substring(3));

            double value = degrees + (minutes / 60.0);

            if (hemisphere.equals("W"))
                value = -value;

            return value;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Convert a hexadecimal character into the corresponding nibble value.
     * 
     * @param b
     *            ASCII character to convert.
     * @param shift
     *            shift left the value by this amount of bits.
     * @return parsed nibble.
     */
    private byte parseNibble(byte b, int shift) {
        byte rv = 0;
        if (b >= '0' && b <= '9')
            rv |= (b - '0') << shift;
        else if (b >= 'a' && b <= 'f')
            rv |= (b - 'a' + 10) << shift;
        else if (b >= 'A' && b <= 'F')
            rv |= (b - 'A' + 10) << shift;
        return rv;
    }

    /**
     * Append a sentence part (something between commas) to the list of current
     * sentence parts.
     */
    private void appendChunk() {
        try {
            chunks.add(new String(chunk.array(), 0, chunkIdx, "US-ASCII"));
        }
        catch (UnsupportedEncodingException e) {
            NeptusLog.pub().info("<###> "+e);
        }

        chunk.clear();
        chunkIdx = 0;
    }
}
