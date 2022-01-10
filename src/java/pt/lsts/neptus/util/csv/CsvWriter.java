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
 * Author: pdias
 * 03/05/2016
 */
package pt.lsts.neptus.util.csv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import pt.lsts.neptus.util.FileUtil;

/**
 * @author pdias
 *
 */
public class CsvWriter {

    /** The comment prefix to use */
    private static final String COMMENT_PREFIX = "# ";
    
    /** Line ending to use */
    private static final String LINE_ENDING = "\r\n";

    /** The default decimal houses to use */
    private static final int DEFAULT_DECIMAL_HOUSES = -1;

    /** Time zone txt for search */
    public static final String TIMEZONE_STR = "Time Zone";
    
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
    
    /** Time Zone indicator control */
    private boolean alreadyWroteTimeZoneWithHeader = false;
    
    /**
     * @param writer The writer to be used.
     */
    public CsvWriter(BufferedWriter writer) {
        this(writer, DEFAULT_DECIMAL_HOUSES);
    }

    /**
     * @param writer The writer to be used.
     * @param decimalHouses The decimal houses to use for the output.
     */
    public CsvWriter(BufferedWriter writer, int decimalHouses) {
        this.decimalHouses = decimalHouses;
        this.writer = writer;
    }

    /**
     * Writes a header.
     * @throws Exception
     */
    public void writeHeader(String title, String... headerLines) throws Exception {
        writer.write(COMMENT_PREFIX + (title != null ? title : "") + LINE_ENDING);
        if (!alreadyWroteTimeZoneWithHeader) {
            alreadyWroteTimeZoneWithHeader = true;
            writer.write(COMMENT_PREFIX + TIMEZONE_STR + ": " + TimeZone.getTimeZone("UTC").getID() + LINE_ENDING);
        }
        for (String str : headerLines) {
            if (str == null)
                continue;
            
            writer.write(COMMENT_PREFIX + str + LINE_ENDING);
        }
    }

    /**
     * Writes the date, time and each value passed into a line entry.
     * @param timeMillis
     * @param vals
     * @throws Exception
     */
    public void writeData(long timeMillis, Object... vals) throws Exception {
        Date date = new Date(timeMillis);
        String dateStr = dateFormatterUTC.format(date);
        String timeStr = timeFormatterUTC.format(date);
        
        writer.write(dateStr + " " + timeStr);
        for (Object d : vals) {
            String fmt = "%s";
            if (d instanceof Number) {
                try {
                    Long.parseLong(d.toString());
                    fmt = "%d";
                    writer.write(String.format(Locale.US, ", " + fmt, ((Number) d).longValue()));
                }
                catch (NumberFormatException e) {
                    fmt = "%" + (decimalHouses >= 0 ? "." + decimalHouses : "") + "f";
                    writer.write(String.format(Locale.US, ", " + fmt, ((Number) d).doubleValue()));
                }
            }
            else {
                writer.write(String.format(Locale.US, ", " + fmt, d));
            }
        }
        writer.write(LINE_ENDING);
    }
    
    public static void main(String[] args) throws Exception {
        File fx = new File("test.csv");
        Writer outputWriter = new FileWriter(fx);
        BufferedWriter outFile = new BufferedWriter(outputWriter);
        CsvWriter csv = new CsvWriter(outFile);
        csv.writeHeader("data", "time, A, B, C");
        csv.writeData(System.currentTimeMillis(), 1, 4.343434, 1., 0.1, "olá");
        outFile.close();
        System.out.println(FileUtil.getFileAsString(fx));
    }
}
