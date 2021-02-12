/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 04/12/2015
 */
package pt.lsts.neptus.util.tid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;

import org.apache.commons.lang3.ArrayUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.FileUtil;

/**
 * This is a TID file reader.
 * See {@link TidWriter} for format information.
 * @author Paulo Dias
 *
 */
public class TidReader {

    /* Format:
    --------
    2014/08/27 07:53:32.817 0.007601 
    2014/08/27 07:53:32.861 0.007705 
    2014/08/27 07:53:32.957 0.007487 
    2014/08/27 07:53:33.057 0.006612 
    */

    /** Data formatter */
    @SuppressWarnings("serial")
    private final SimpleDateFormat dateTimeFormatterUTC = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;
    /** Time formatter */
    @SuppressWarnings("serial")
    private final SimpleDateFormat dateTimeFormatterUTC2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;

    /** The writer to use */
    private BufferedReader reader;
    
    /** The name of the harbor read */
    private String harbor = "?";
    /** Indicates if the harbor data has already been found */
    private boolean asReadTheHarborInfo = false;
    /** Indicates if the time zone data has already been found */
    private boolean asReadTheTimeZoneInfo = false;
    
    /**
     * @param reader The reader to be used.
     */
    public TidReader(BufferedReader reader) {
        this.reader = reader;
    }

    /**
     * This should be called at the end of the reading.
     * @return the harbor
     */
    public String getHarbor() {
        return harbor;
    }
    
    /**
     * Indicates if the harbor info has been found already.
     * @return
     */
    public boolean asReadTheHarborInfo() {
        return asReadTheHarborInfo;
    }
    
    /**
     * Parses the comment lines for harbor info and time zone.
     * @param line
     */
    private void processComment(String line) {
        if (asReadTheHarborInfo && asReadTheTimeZoneInfo)
            return;
        if (isCommentLine(line)) {
            if (!asReadTheHarborInfo) {
                String val = extractValueWithHeader(line, TidWriter.HARBOR_STR);
                if (val != null && !val.isEmpty()) {
                    harbor = val;
                    asReadTheHarborInfo = true;
                    return;
                }
            }
            if (!asReadTheTimeZoneInfo) {
                String val = extractValueWithHeader(line, TidWriter.TIMEZONE_STR);
                if (val != null && !val.isEmpty()) {
                    TimeZone tz = TimeZone.getTimeZone(val);
                    if (tz.getID().equalsIgnoreCase(val)) {
                        dateTimeFormatterUTC.setTimeZone(tz);
                        dateTimeFormatterUTC2.setTimeZone(tz);
                    }
                    else {
                        NeptusLog.pub().error("Error processing time zone, using UTC.");
                    }
                    asReadTheTimeZoneInfo = true;
                    return;
                }
            }
        }
    }

    /**
     * Extract value from a line in the form: "header: value" or "header= value". 
     * @param line
     * @param header
     * @return
     */
    private String extractValueWithHeader(String line, String header) {
        if (line == null || line.isEmpty() || header == null || header.isEmpty())
            return null;
        
        int idx = line.indexOf(header);
        if (idx >= 0) {
            String ss = line.substring(idx + header.length());
            ss = ss.trim();
            if (ss.startsWith(":"))
                ss = ss.replaceFirst(":", "");
            else if (ss.startsWith("="))
                ss = ss.replaceFirst(":", "");
            ss = ss.trim();
            return ss;
        }
        
        return null;
    }
    
    /**
     * Writes the date, time and each value passed into a line entry.
     * @param timeMillis
     * @param vals
     * @throws Exception
     */
    public Data readData() {
        try {
            while (true) {
                String line = reader.readLine();
                if (isCommentLine(line)) {
                    processComment(line);
                    continue;
                }
                
                try {
                    line = line.trim();
                    String[] tokens = line.split(" ");
                    if (tokens.length < 3)
                        continue;
                    
                    Date date;
                    try {
                        date = dateTimeFormatterUTC.parse(tokens[0] + " " + tokens[1]);
                    }
                    catch (Exception e) {
                        try {
                            date = dateTimeFormatterUTC2.parse(tokens[0] + " " + tokens[1]);
                        }
                        catch (Exception e1) {
                            e1.printStackTrace();
                            continue;
                        }
                    }
                    
                    double height = Double.parseDouble(tokens[2]);
                    
                    ArrayList<Double> others = new ArrayList<>();
                    if (tokens.length > 3) {
                        for (int i = 3; i < tokens.length; i++) {
                            double val = Double.parseDouble(tokens[i]);
                            others.add(val);
                        }
                    }
                    
                    Data data = new Data();
                    data.timeMillis = date.getTime();
                    data.height = height;
                    Double[] othersArrD = others.toArray(new Double[others.size()]);
                    data.other = ArrayUtils.toPrimitive(othersArrD);
                    
                    return data;
                }
                catch (Exception e) {
                    continue;
                }
            }
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * @param line
     * @return
     */
    private boolean isCommentLine(String line) {
        return line.trim().startsWith("#") || line.trim().startsWith("--");
    }
    
    /**
     * This is the return data from a tide read.
     * @author pdias
     *
     */
    public class Data {
        public long timeMillis;
        public double height;
        public double[] other;
    }
    
    public static void main(String[] args) throws Exception {
        String tmpFolder = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator", "/");
        File tidFx = new File(tmpFolder + "tmp.tid");
        tidFx.delete();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(tidFx));
        TidWriter tidWriter = new TidWriter(writer, 2);
        tidWriter.writeHeader("Title", "harbor");
        Random rand = new Random(28091893);
        Date date = new GregorianCalendar(1993, 9, 28).getTime();
        for (int i = 0; i < 10; i++) {
            tidWriter.writeData(new Double(date.getTime() + i * 6.45 * 1E3 * 60 * 60).longValue(), 2.0 * rand.nextDouble());
        }
        tidFx.deleteOnExit();
        writer.close();
        
        System.out.println("TID Created");
        System.out.println("===========");
        System.out.println(FileUtil.getFileAsString(tidFx));
        System.out.println();
        System.out.println();
        
        System.out.println("TID Read");
        System.out.println("========");

        BufferedReader reader = new BufferedReader(new FileReader(tidFx));
        TidReader tidReader = new TidReader(reader);
        while (true) {
            Data data = tidReader.readData();
            if (data == null)
                break;
            System.out.println("Data: " + new Date(data.timeMillis) 
                    + " depth: " + data.height + " other: " 
                    + Arrays.toString(data.other));
        }
        System.out.println("Harbor read: " + tidReader.getHarbor());
    }
}
