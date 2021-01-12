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
 * 23/11/2017
 */
package pt.lsts.neptus.util.netcdf;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.UnitsUtil;

/**
 * @author pdias
 *
 */
public class NetCDFUnitsUtils {

    @SuppressWarnings("serial")
    private static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH':'mm':'ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};

    /**
     * Time Coordinate in the NetCDF Climate and Forecast (CF) Metadata Conventions v1.6
     * indicated the format as: <br/>
     *   
     *   <ul>
     *      <li>"seconds since 1992-10-8 15:15:42.5 -6:00"</li>
     *   </ul>
     * 
     * From http://coastwatch.pfeg.noaa.gov/erddap/convert/time.html:<br/><br/>
     * 
     * The first word can be (upper or lower case): <br/>
     *   <ul>
     *      <li>ms, msec, msecs, millis, millisecond, milliseconds,</li>
     *      <li>ms, msec, msecs, millis, millisecond, milliseconds,</li> 
     *      <li>s, sec, secs, second, seconds,</li>
     *      <li>m, min, mins, minute, minutes,</li>
     *      <li>h, hr, hrs, hour, hours,</li>
     *      <li>d, day, days,</li>
     *      <li>week, weeks, (not support)</li>
     *      <li>mon, mons, month, months, (not support)</li>
     *      <li>yr, yrs, year, or years (not support).</li>
     *   </ul>
     * 
     * "since" is required. <br/><br/>
     * 
     * The time can be any time in the format yyyy-MM-ddTHH:mm:ss.SSSZ, 
     * where Z is 'Z' or a ±hh or ±hh:mm offset from the Zulu/GMT time zone. 
     * If you omit Z and the offset, the Zulu/GMT time zone is used. 
     * Separately, if you omit .SSS, :ss.SSS, :mm:ss.SSS, or Thh:mm:ss.SSS, the 
     * missing fields are assumed to be 0.<br/><br/>
     * 
     * So another example is "hours since 0001-01-01".<br/><br/>
     * 
     * Technically, ERDDAP does NOT follow the UDUNITS standard when converting "years since" 
     * and "months since" time values to "seconds since". The UDUNITS standard defines a 
     * year as a fixed, single value: 3.15569259747e7 seconds. And UDUNITS defines a month 
     * as year/12. Unfortunately, most/all datasets that we have seen that use 
     * "years since" or "months since" clearly intend the values to be calendar years 
     * or calendar months. For example, "3 months since 1970-01-01" is usually intended 
     * to mean 1970-04-01. So, ERDDAP interprets "years since" and "months since" as 
     * calendar years and months, and does not strictly follow the UDUNITS standard.
     * 
     * @param timeStr
     * @return An array with 2 values, the multiplier and the offset
     */
    public static double[] getMultiplierAndMillisOffsetFromTimeUnits(String timeStr) {
        if ("days since 00-01-00 00:00:00".equalsIgnoreCase(timeStr) || "days since 00-01-00".equalsIgnoreCase(timeStr)) {
            // Reference time in year zero has special meaning
            return new double[] { DateTimeUtil.DAY, - DateTimeUtil.DAYS_SINCE_YEAR_0_TILL_1970 * DateTimeUtil.DAY};
        }
        else {
            String[] tk = timeStr.trim().split("[ ]");
            if (tk.length < 3) {
                return null;
            }
            else {
                double mult = 1;
                double off = 1;
                switch (tk[0].trim().toLowerCase().replace(".", "")) {
                    case "days":
                    case "day":
                    case "d":
                        mult = DateTimeUtil.DAY;
                        break;
                    case "hours":
                    case "hour":
                    case "hr":
                    case "h":
                        mult = DateTimeUtil.HOUR;
                        break;
                    case "minutes":
                    case "minute":
                    case "min":
                        mult = DateTimeUtil.MINUTE;
                        break;
                    case "seconds":
                    case "second":
                    case "sec":
                    case "s":
                        mult = DateTimeUtil.SECOND;
                        break;
                }
                
                String dateTkStr = tk[2];
                String timeTkStr = tk.length > 3 ? tk[3] : "0:0:0";
                String timeZoneTkStr = tk.length > 4 ? tk[4] : "";
                if (tk[2].contains("T")) { // Then is a ISO 8601, e.g. 1970-01-01T00:00:00Z 
                    String[] sp1 = tk[2].split("T");
                    dateTkStr = sp1[0];
                    if (sp1[1].contains("+")) {
                        String[] sp2 = sp1[1].split("\\+");
                        timeTkStr = sp2[0];
                        timeZoneTkStr = "+" + sp2[1];
                    }
                    else if (sp1[1].contains("\u2212") || sp1[1].contains("-")) {
                        String[] sp2 = sp1[1].split("[-\u2212]");
                        timeTkStr = sp2[0];
                        timeZoneTkStr = "-" + sp2[1];
                    }
                    else if (sp1[1].endsWith("Z")) {
                        timeTkStr = sp1[1].replaceAll("Z", "");
                        timeZoneTkStr = "";
                    }
                }
                
                try {
                    Date date = dateTimeFormaterUTC.parse(dateTkStr + " " + timeTkStr);
                    off = date.getTime();
                    
                    // Let us see if milliseconds are present
                    String[] mSplitArray = timeTkStr.split("\\.");
                    if (mSplitArray.length > 1) {
                        String millisStr = mSplitArray[1];
                        int millisSize = millisStr.length();
                        switch (millisSize) {
                            case 1:
                                off += Integer.parseInt(millisStr) * 100;
                                break;
                            case 2:
                                off += Integer.parseInt(millisStr) * 10;
                                break;
                            case 3:
                                off += Integer.parseInt(millisStr);
                                break;
                            default:
                                off += Integer.parseInt(millisStr.substring(0, 3));
                                break;
                        }
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }
                
                try {
                    if (!timeZoneTkStr.isEmpty()) { // So we have a time zone and so it's not UTC
                        // The time zone specification
                        // can also be written without a colon using one or two-digits
                        // (indicating hours) or three or four digits (indicating hours
                        // and minutes)
                        timeZoneTkStr = timeZoneTkStr.replace("\u2212",  "-"); //Replace unicode "-"
                        String[] tzStrs = timeZoneTkStr.split(":");
                        if (tzStrs.length > 1) { // Has colon
                            int hrTzNb = Integer.parseInt(tzStrs[0]); 
                            off -= hrTzNb * DateTimeUtil.HOUR;
                            off -= Integer.parseInt(tzStrs[1]) * Math.signum(hrTzNb) * DateTimeUtil.MINUTE;
                        }
                        else {
                            String tzSt = timeZoneTkStr.replace(":", "");
                            int tzNb = Integer.parseInt(tzSt);
                            if (Math.abs(tzNb) < 100) { // It's hours
                                off -= tzNb * DateTimeUtil.HOUR;
                            }
                            else { // It's hours plus minutes
                                int hrTzNb = tzNb / 100;
                                off -= hrTzNb * DateTimeUtil.HOUR;
                                int minTzNb = tzNb - hrTzNb * 100;
                                off -= minTzNb * DateTimeUtil.MINUTE;
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                return new double[] { mult, off };
            }
        }
    }

    // FIXME better control of unit for speed
    public static double getMultiplierForCmPerSecondsFromSpeedUnits(String speedUnits) {
        double mult = 1;
        if (speedUnits == null || speedUnits.isEmpty())
            return mult;
        switch (speedUnits.trim().toLowerCase()) {
            case "cm/s":
            case "cm s-1":
            case "cm s^-1":
            case "cm.s^-1":
                mult = 1;
                break;
            case "m/s":
            case "m s-1":
            case "m s^-1":
            case "m.s^-1":
                mult = 100;
                break;
            case "ft/s":
            case "ft s-1":
            case "ft s^-1":
            case "ft.s^-1":
                mult = 0.3048 * 100;
        }
        
        return mult;
    }

    /**
     * @param value
     * @param units
     * @return
     */
    public static double getValueForDegreesCelciusFromTempUnits(double value, String units) {
        if (units == null || units.isEmpty())
            return value;
        double ret = value;
        switch (units.toLowerCase().trim()) {
            case "K":
            case "kelvin":
                ret = value - UnitsUtil.CELSIUS_TO_KELVIN;
                break;
            case "\u00B0F":
            case "ºF":
            case "degree_F":
                ret = (value - 32) / 1.8;
                break;
        }
    
        return ret;
    }

    public static double getValueForMilliGPerM3FromTempUnits(double value, String units) {
        if (units == null || units.isEmpty())
            return value;
        double ret = value;
        switch (units.toLowerCase().trim()) {
            case "kg m-3":
            case "Kg m-3":
                ret = value * 1E3 * 1E3;
                break;
            case "g m-3":
                ret = value * 1E3;
                break;
            case "ug m-3":
            case "\u03BCg m-3":
                ret = value / 1E3;
                break;
        }
    
        return ret;
    }

    public static double getValueForMetterFromTempUnits(double value, String units) {
        if (units == null || units.isEmpty())
            return value;
        double ret = value;
        switch (units.toLowerCase().trim()) {
            case "km":
            case "Km":
                ret = value * 1E3;
                break;
            case "dm":
                ret = value / 1E1;
                break;
            case "cm":
                ret = value / 1E2;
                break;
            case "mm":
                ret = value / 1E3;
                break;
        }
    
        return ret;
    }

    public static void main(String[] args) throws Exception {
        try {
            double[] val = getMultiplierAndMillisOffsetFromTimeUnits("days since 00-01-00 00:00:00");
            System.out.println(val[0] + "    " + val[1]);
            val = getMultiplierAndMillisOffsetFromTimeUnits("seconds since 2013-07-04 00:00:00");
            System.out.println(val[0] + "    " + val[1]);
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
        
        try {
            Pattern timeStringPattern = Pattern.compile("^(\\w+?)\\ssince\\s(\\w+?)");
            String timeUnits = "days since 00-01-00 00:00:00";
            Matcher matcher = timeStringPattern.matcher(timeUnits);

            System.out.println(matcher.group(1));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            Date date = dateTimeFormaterUTC.parse("0001-01-01 00:00:00");
            System.out.println(date.getTime());
            
            Date ndate = new Date(date.getTime() + DateTimeUtil.DAYS_SINCE_YEAR_0_TILL_1970 * DateTimeUtil.DAY);
            System.out.println(ndate + "           " + ndate.getTime());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("------ Test time load -----");
        String dateT1Str = "2013-07-04 00:00:00";
        Date dateT1 = dateTimeFormaterUTC.parse(dateT1Str);
        System.out.printf("%22s  ==  %s \t%s\n", dateT1Str, dateT1, dateT1.getTime());

        dateT1Str = "2013-7-4 0:0:0";
        dateT1 = dateTimeFormaterUTC.parse(dateT1Str);
        System.out.printf("%22s  ==  %s \t%s\n", dateT1Str, dateT1, dateT1.getTime());

        dateT1Str = "2013-7-4 13:3:4.32";
        dateT1 = dateTimeFormaterUTC.parse(dateT1Str);
        System.out.printf("%22s  ==  %s \t%s\n", dateT1Str, dateT1, dateT1.getTime());
        
        dateT1Str = "2013-7-4 13:3:4";
        dateT1 = dateTimeFormaterUTC.parse(dateT1Str);
        System.out.printf("%22s  ==  %s \t%s\n", dateT1Str, dateT1, dateT1.getTime());
        
        String dateT2Str = "2013-7-4 13:3:4.32";
        System.out.println(Arrays.toString(dateT2Str.split("\\.")));
        dateT2Str = "2013-7-4 13:3:4";
        System.out.println(Arrays.toString(dateT2Str.split("\\.")));
        dateT2Str = "2013-7-4 13:3:4.";
        System.out.println(Arrays.toString(dateT2Str.split("\\.")));
        dateT2Str = "2013-7-4 13:3:4.262626";
        System.out.println(Arrays.toString(dateT2Str.split("\\.")));

        
        String dateT3Str = "seconds since 2013-7-4 13:3:4.32";
        double[] multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        Date dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        System.out.println("\nEvery resulting date should result in the same values!");
        dateT3Str = "seconds since 2013-7-4 13:3:4";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 +1:0";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 1";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 100";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 12:3:4 -100";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 12:3:4 \u2212100";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 11:33:4 -130";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 11:33:4 -1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:33:4 +130";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:33:04 +1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        System.out.println("\nOther tests!");

        dateT3Str = "seconds since 2013-7-4 14:33:04.67 +1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 1970-01-01T00:00:00Z";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2017-11-04T3:10:00.33+1:20";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4T14:33:04+1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);
    }
}
