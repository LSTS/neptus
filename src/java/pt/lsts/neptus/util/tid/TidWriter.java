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
 * 
 * The format is the following:
 * <code><br>
 * # Tides Data<br>
 * # Time Zone: UTC<br>
 * # Harbor: Leixoes<br>
 * 2007/12/31 20:42:00.000 2.60<br>
 * 2008/01/01 02:40:00.000 1.38<br>
 * ...<br>
 * </code>
 * 
 * <p>
 * The first line is simply a comment.
 * The time zone line is optional but if not there {@link TimeZone} UTC is assumed.
 * The time zones ids to use should adhere to {@link TimeZone}.
 * </p>
 * <p>
 * The harbor line informs of the harbor location for the data. If not there "?" 
 * will appear, so use it.
 * </p>
 * <p>
 * The header lines should appear at the beginning of the file. If the time zone 
 * appear in the middle only the first will be used (but all previous dates will 
 * be loaded as UTC).
 * If additional columns are set, they will be loaded as doubles.
 * </p>
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

    /** Line ending to use */
    private static final String LINE_ENDING = "\r\n";

    /** The default decimal houses to use */
    private static final int DEFAULT_DECIMAL_HOUSES = 2;

    /** Harbor txt for location search */
    public static final String HARBOR_STR = "Harbor";
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
     * Writes a header.
     * It will write the time zone used (only once, safe for multiple calls) which is UTC.
     * @throws Exception
     */
    public void writeHeader(String title) throws Exception {
        writer.write("# " + (title != null ? title : "") + LINE_ENDING);
        if (!alreadyWroteTimeZoneWithHeader) {
            alreadyWroteTimeZoneWithHeader = true;
            writer.write("# " + TIMEZONE_STR + ": " + TimeZone.getTimeZone("UTC").getID() + LINE_ENDING);
        }
    }

    /**
     * Writes a header.
     * It will write the time zone used (only once, safe for multiple calls) which is UTC.
     * @throws Exception
     */
    public void writeHeader(String title, String placeIndicator) throws Exception {
        writeHeader(title);
        writeHeader(HARBOR_STR + ": " + placeIndicator);
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
        writer.write(LINE_ENDING);
    }
}
