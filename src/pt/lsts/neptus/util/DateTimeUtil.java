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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 2008/04/16
 */
package pt.lsts.neptus.util;

import java.text.SimpleDateFormat;
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

    public static final long DAY = 1000 * 60 * 60 * 24;
    public static final long HOUR = 1000 * 60 * 60;
    public static final long MINUTE = 1000 * 60;
    public static final long SECOND = 1000;
    
    public static final int DAYS_SINCE_YEAR_0_TILL_1970 = 719530;
    
    public static final SimpleDateFormat dateFormaterXMLUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'.0Z'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}}; // This one should be UTC (Zulu)
//    public static final SimpleDateFormat dateFormaterXMLNoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");  // This one should be UTC (Zulu)
    public static final SimpleDateFormat dateFormaterXMLNoMillisUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;  // This one should be UTC (Zulu)
    public static final SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat dateFormaterUTC = new SimpleDateFormat("yyyy-MM-dd") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;
    public static final SimpleDateFormat dateFormaterNoSpaces = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm:ss.SSS");
    public static final SimpleDateFormat timeFormaterUTC = new SimpleDateFormat("HH:mm:ss.SSS") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;
    public static final SimpleDateFormat timeFormaterNoMillis = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat timeFormaterNoMillis2 = new SimpleDateFormat("HH'h'mm'm'ss's'");
    public static final SimpleDateFormat timeFormaterNoMillis2UTC = new SimpleDateFormat("HH'h'mm'm'ss's'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;
    public static final SimpleDateFormat timeFormaterNoSegs = new SimpleDateFormat("HH:mm");
    public static final SimpleDateFormat timeFormaterNoSegs2 = new SimpleDateFormat("HH'h'mm'm'");
    public static final SimpleDateFormat timeUTCFormaterNoSegs2 = new SimpleDateFormat("HH'h'mm'm'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;
    public static final SimpleDateFormat timeUTCFormaterNoSegs3 = new SimpleDateFormat("HH':'mm") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;
    public static final SimpleDateFormat dateTimeFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;
    public static final SimpleDateFormat dateTimeFormaterNoMillis = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat dateTimeFormaterNoSegs = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final SimpleDateFormat dateTimeFormater2UTC = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", new Locale("en")) {{setTimeZone(TimeZone.getTimeZone("UTC"));}};;

    public static final SimpleDateFormat dateTimeFileNameFormater = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss's'");
    public static final SimpleDateFormat dateTimeFileNameFormaterMillis = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss.SSS's'");

//    static {
//        dateFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//        dateFormaterXMLUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//        dateTimeFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//        dateTimeFormater2UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//        dateFormaterXMLNoMillisUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//        
//        timeUTCFormaterNoSegs2.setTimeZone(TimeZone.getTimeZone("UTC"));
//        timeUTCFormaterNoSegs3.setTimeZone(TimeZone.getTimeZone("UTC"));
//        timeFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//        timeFormaterNoMillis2UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//    }

    private static long initialTimeMillis;
    private static long initialTimeNanos;

    static {
        initialTimeMillis = System.currentTimeMillis();
        initialTimeNanos = System.nanoTime();
    }
    
    public static long formatedStringToMillis(String formatedTime) {
        String time = formatedTime.replaceAll(" ", "");
        String val = "";
        long total = 0;
        for (int i = 0; i < time.length(); i++) {
            char c = time.charAt(i);
            if (Character.isDigit(c)) {
                val += c;
            }
            else {
                switch (c) {
                    case 's':
                        total += Integer.parseInt(val) * 1000;
                        val = "";
                        break;
                    case 'm':
                        total += Integer.parseInt(val) * 1000 * 60;
                        val = "";
                        break;
                    case 'h':
                        total += Integer.parseInt(val) * 1000 * 60 * 60;
                        val = "";
                        break;
                    case 'd':
                        total += Integer.parseInt(val) * 1000 * 60 * 60 * 24;
                        val = "";
                        break;
                    default:
                        val = "";
                        break;
                }
            }
        }
        return total;        
    }
    
	public static final String milliSecondsToFormatedString(long timeMillis) {
		double time = timeMillis / 1000.0;
		//time = 3*60*60 + 2*60;
		String tt = "";
		if (time < 60)
			tt = new Double(time).doubleValue() + " s";
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


	public static double timeStampSeconds () {
		//long nanos = System.nanoTime();
		//long millis = System.currentTimeMillis();
		//double ret = (initialTimeMillis) * 1E-3 + (nanos - initialTimeNanos) * 1E-9;
		//initialTimeMillis = millis;
		//initialTimeNanos = nanos;
		//return ret;
		//return (initialTimeMillis) * 1E-3 +
		//	(System.nanoTime() - initialTimeNanos) * 1E-9;
		return System.currentTimeMillis() * 1E-3;
	}
	
	public static String getUID() {//0xF423F
		return ""+(initialTimeMillis * 1000000 + (initialTimeNanos % 1000000));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NeptusLog.pub().info("<###> "+getUID());
		NeptusLog.pub().info("<###> "+dateTimeFileNameFormater.format(new Date(System.currentTimeMillis())));
        
		NeptusLog.pub().info("<###> "+milliSecondsToFormatedString(HOUR*24));
        NeptusLog.pub().info("<###> "+milliSecondsToFormatedString(HOUR*36+MINUTE*30));
        NeptusLog.pub().info("<###> "+milliSecondsToFormatedString(DAY*78+HOUR*36+MINUTE*30));

		int i = 0;
		while (i++ < 10) {
			try { Thread.sleep(1000); } catch (InterruptedException e) {}
			//System.out.printf("%f  %f\n", System.currentTimeMillis() * 1E-3, timeStampSeconds());
			double m  = System.currentTimeMillis();
			double n  = timeStampSeconds();
			double nn = System.nanoTime();
			m *= 1E-3;
			System.out.printf("" + m + "  " + n + "  " + nn + "  " + (m - n) + "\n");
		}
		
		NeptusLog.pub().info("<###> "+dateTimeFormater2UTC.format(new Date()));
		NeptusLog.pub().info("<###> "+new Date());
		
		String clockStr = DateTimeUtil.timeUTCFormaterNoSegs3.format(new Date(System.currentTimeMillis()))
                + " " + I18n.text("UTC");
		System.out.println(clockStr);
	}
}
