/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 02/12/2015
 */
package pt.lsts.neptus.util.tid;

import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This is a TID file writer
 * @author Paulo Dias
 *
 */
public class TidWriter {

    /* Format:
    --------
    2014/08/27 07:53:32.817 0.007601 
    2014/08/27 07:53:32.861 0.007705 
    2014/08/27 07:53:32.957 0.007487 
    2014/08/27 07:53:33.057 0.006612 
    */

    /** The default decimal houses to use */
    private static final int DEFAULT_DECIMAL_HOUSES = 6;
    
    /** Data formatter */
    @SuppressWarnings("serial")
    private final SimpleDateFormat dateFormatterUTC = new SimpleDateFormat("yyyy/MM/dd") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;
    /** Time formatter */
    @SuppressWarnings("serial")
    private final SimpleDateFormat timeFormatterUTC = new SimpleDateFormat("HH:mm:ss.SSS") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;

    /** The writer to use */
    private BufferedWriter writer;
    /** The decimal houses to use in the output */
    private int decimalHouses;
    
    /**
     * @param writer The writer to be used.
     */
    public TidWriter(BufferedWriter writer) {
        this(writer, DEFAULT_DECIMAL_HOUSES);
    }

    /**
     * @param writer The writer to be used.
     * @param decimalHouses The decimal houses to use for the output.
     */
    public TidWriter(BufferedWriter writer, int decimalHouses) {
        this.decimalHouses = decimalHouses;
        this.writer = writer;
    }

    /**
     * Writes a header. (Empty for now.)
     * @throws Exception
     */
    public void writeHeader() throws Exception {
        writer.write("--------\r\n");
    }

    /**
     * Writes a header.
     * @throws Exception
     */
    public void writeHeader(String title) throws Exception {
        writer.write("# " + (title != null ? title : "") + "\r\n");
    }

    /**
     * Writes the date, time and each value passed into a line entry.
     * @param timeMillis
     * @param vals
     * @throws Exception
     */
    public void writeData(long timeMillis, double... vals) throws Exception {
        Date date = new Date(timeMillis);
        String dateStr = dateFormatterUTC.format(date);
        String timeStr = timeFormatterUTC.format(date);
        
        writer.write(dateStr + " " + timeStr);
        for (double d : vals)
            writer.write(String.format(Locale.US, " %." + decimalHouses + "f", d));
        writer.write("\r\n");
    }
}
