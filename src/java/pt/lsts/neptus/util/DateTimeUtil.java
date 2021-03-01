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
 * Author: Paulo Dias
 * 2008/04/16
 */
package pt.lsts.neptus.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;


/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class DateTimeUtil {

    public static final Date REF_DATE;
    static {
        Calendar cal = Calendar.getInstance();
        cal.set(2004, Calendar.SEPTEMBER, 1, 0, 0, 0);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        REF_DATE = cal.getTime();
    }
    
    public static final long DAY = 1000 * 60 * 60 * 24;
    public static final long HOUR = 1000 * 60 * 60;
    public static final long MINUTE = 1000 * 60;
    public static final long SECOND = 1000;
    
    public static final int DAYS_SINCE_YEAR_0_TILL_1970 = 719530;
    public static final SimpleDateFormat dateFormatterXMLUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'.0Z'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}}; // This one should be UTC (Zulu)
    public static final SimpleDateFormat dateFormatterXMLNoMillisUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};  // This one should be UTC (Zulu)
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat dateFormatterUTC = new SimpleDateFormat("yyyy-MM-dd") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    public static final SimpleDateFormat dateFormatterNoSpaces = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
    public static final SimpleDateFormat timeFormatterUTC = new SimpleDateFormat("HH:mm:ss.SSS") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    public static final SimpleDateFormat timeFormatterNoMillis = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat timeFormatterNoMillis2 = new SimpleDateFormat("HH'h'mm'm'ss's'");
    public static final SimpleDateFormat timeFormatterNoMillis2UTC = new SimpleDateFormat("HH'h'mm'm'ss's'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    public static final SimpleDateFormat timeFormatterNoSegs = new SimpleDateFormat("HH:mm");
    public static final SimpleDateFormat timeFormatterNoSegs2 = new SimpleDateFormat("HH'h'mm'm'");
    public static final SimpleDateFormat timeFormatterNoSegs3 = new SimpleDateFormat("HH':'mm");
    public static final SimpleDateFormat timeUTCFormatterNoSegs2 = new SimpleDateFormat("HH'h'mm'm'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    public static final SimpleDateFormat timeUTCFormatterNoSegs3 = new SimpleDateFormat("HH':'mm") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    public static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final SimpleDateFormat dateTimeFormatterUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    public static final SimpleDateFormat dateTimeFormatterNoMillis = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat dateTimeFormatterNoSegs = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final SimpleDateFormat dateTimeFormatter2UTC = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", new Locale("en")) {{setTimeZone(TimeZone.getTimeZone("UTC"));}};

    public static final SimpleDateFormat dateTimeFileNameFormatter = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss's'");
    public static final SimpleDateFormat dateTimeFileNameFormatterMillis = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss.SSS's'");

    // yyyy-mm-dd hh:mm:ss
    // Omitted time-zone shall be interpreted as UTC. This deviates from the iso-8601 specification which specifies no time zone information to be interpreted as local time.
    public static final SimpleDateFormat dateTimeFormatterISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", new Locale("en")) {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    public static final SimpleDateFormat dateTimeFormatterISO8601_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", new Locale("en")) {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    public static final SimpleDateFormat dateTimeFormatterISO8601_2 = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS", new Locale("en")) {{setTimeZone(TimeZone.getTimeZone("UTC"));}};

    /** Default time format. */
    private static final DateTimeFormatter defaultTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    /** Currently configured timezone. */
    private static final ZoneId currentZoneId = ZoneId.of("UTC");

    private static long initialTimeMillis;
    private static long initialTimeNanos;

    static {
        initialTimeMillis = System.currentTimeMillis();
        initialTimeNanos = System.nanoTime();
    }
    
    /**
     * Formats a timestamp in a format suitable to be presented to the user using the current timezone.
     *
     * @param milliSeconds milliseconds since the Unix Epoch.
     * @return formatted time.
     */
    public static String formatTime(long milliSeconds) {
        return formatTime(milliSeconds, currentZoneId);
    }

    /**
     * Formats a timestamp in a format suitable to be presented to the user using a given timezone.
     *
     * @param milliSeconds milliseconds since the Unix Epoch.
     * @param zoneId timezone identifier.
     * @return formatted time.
     */
    public static String formatTime(long milliSeconds, ZoneId zoneId) {
        return Instant.ofEpochMilli(milliSeconds).atZone(zoneId).format(defaultTimeFormat);
    }

	public static final String milliSecondsToFormatedString(long timeMillis) {
	    return timeInFormatedString(timeMillis, false);
	}

	public static final String milliSecondsToFormatedString(long timeMillis, boolean alwaysRoundSeconds) {
	    return timeInFormatedString(timeMillis, alwaysRoundSeconds);
	}

	public static double timeStampSeconds () {
		return System.currentTimeMillis() * 1E-3;
	}

	public static String getUID() {//0xF423F
	    return ""+(initialTimeMillis * 1000000 + (initialTimeNanos % 1000000));
	}

	private static final String timeInFormatedString(long timeMillis, boolean alwaysRoundSeconds) {
		double time = timeMillis / 1000.0;
		//time = 3*60*60 + 2*60;
		String tt = "";
		if (time < 60) {
		    if (!alwaysRoundSeconds)
		        tt = Double.valueOf(time).doubleValue() + "s";
		    else
		        tt = Math.round(Double.valueOf(time)) + "s";
		}
		else if ((time/60.0) < 60) {
			long mi  = (long) (time/60.0);
			long sec = (long) (time%60.0);
			tt = mi + "m " + ((sec < 10)?"0":"") + sec + "s";
		}
		else if ((time/60.0/60.0) < 24) {
			long hr = (long) (time/60.0/60.0);
			long mi = (long) ((time/60.0)%60.0);
			tt = hr + "h " + ((mi < 10)?"0":"") + mi + "m";
		}
        else {
            long dy = (long) (time/60.0/60.0/24.0);
            long hr = (long) ((time/60.0/60.0)%24);
            long mi = (long) ((time/60.0)%60.0);
            tt = dy + "d " + ((hr < 10)?"0":"") + hr + "h " + ((mi < 10)?"0":"") + mi + "m";
        }
		return tt;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NeptusLog.pub().info("<###> "+getUID());
		NeptusLog.pub().info("<###> "+dateTimeFileNameFormatter.format(new Date(System.currentTimeMillis())));
        
		NeptusLog.pub().info("<###> "+milliSecondsToFormatedString(HOUR*24));
        NeptusLog.pub().info("<###> "+milliSecondsToFormatedString(HOUR*36+MINUTE*30));
        NeptusLog.pub().info("<###> "+milliSecondsToFormatedString(DAY*78+HOUR*36+MINUTE*30));

		int i = 0;
		while (i++ < 10) {
			try { Thread.sleep(1000); } catch (InterruptedException e) {}
			double m  = System.currentTimeMillis();
			double n  = timeStampSeconds();
			double nn = System.nanoTime();
			m *= 1E-3;
			System.out.printf("" + m + "  " + n + "  " + nn + "  " + (m - n) + "\n");
		}
		
		NeptusLog.pub().info("<###> "+dateTimeFormatter2UTC.format(new Date()));
		NeptusLog.pub().info("<###> "+new Date());
		
		String clockStr = DateTimeUtil.timeUTCFormatterNoSegs3.format(new Date(System.currentTimeMillis()))
                + " " + I18n.text("UTC");
		System.out.println(clockStr);
		
		System.out.println();
		System.out.println(timeInFormatedString(3533, true));
        System.out.println(timeInFormatedString(3533, false));
        System.out.println(timeInFormatedString(103533, true));
        System.out.println(timeInFormatedString(103533, false));

        System.out.println(milliSecondsToFormatedString(3533));
        System.out.println(milliSecondsToFormatedString(3533, true));
	}
}
