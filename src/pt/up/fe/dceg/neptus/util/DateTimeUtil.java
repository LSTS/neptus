/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2008/04/16
 */
package pt.up.fe.dceg.neptus.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author pdias
 *
 */
public class DateTimeUtil {

	public static final long DAY    = 1000*60*60*24;
    public static final long HOUR   = 1000*60*60;
    public static final long MINUTE = 1000*60;
    public static final long SECOND = 1000;
    
    public static final SimpleDateFormat dateFormaterXMLUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'.0Z'"); // This one should be UTC (Zulu)
//    public static final SimpleDateFormat dateFormaterXMLNoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");  // This one should be UTC (Zulu)
    public static final SimpleDateFormat dateFormaterXMLNoMillisUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");  // This one should be UTC (Zulu)
    public static final SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat dateFormaterUTC = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat dateFormaterNoSpaces = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm:ss.SSS");
    public static final SimpleDateFormat timeFormaterUTC = new SimpleDateFormat("HH:mm:ss.SSS");
    public static final SimpleDateFormat timeFormaterNoMillis = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat timeFormaterNoMillis2 = new SimpleDateFormat("HH'h'mm'm'ss's'");
    public static final SimpleDateFormat timeFormaterNoMillis2UTC = new SimpleDateFormat("HH'h'mm'm'ss's'");
    public static final SimpleDateFormat timeFormaterNoSegs = new SimpleDateFormat("HH:mm");
    public static final SimpleDateFormat timeFormaterNoSegs2 = new SimpleDateFormat("HH'h'mm'm'");
    public static final SimpleDateFormat timeUTCFormaterNoSegs2 = new SimpleDateFormat("HH'h'mm'm'");
    public static final SimpleDateFormat timeUTCFormaterNoSegs3 = new SimpleDateFormat("HH':'mm");
    public static final SimpleDateFormat dateTimeFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final SimpleDateFormat dateTimeFormaterNoMillis = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat dateTimeFormaterNoSegs = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final SimpleDateFormat dateTimeFormater2UTC = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", new Locale("en"));

    public static final SimpleDateFormat dateTimeFileNameFormater = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss's'");
    public static final SimpleDateFormat dateTimeFileNameFormaterMillis = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss.SSS's'");

    static {
        dateFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormaterXMLUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateTimeFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateTimeFormater2UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormaterXMLNoMillisUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        timeUTCFormaterNoSegs2.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormaterNoMillis2UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static long initialTimeMillis;
    private static long initialTimeNanos;

    static {
        initialTimeMillis = System.currentTimeMillis();
        initialTimeNanos = System.nanoTime();
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
		System.out.println(getUID());
		System.out.println(dateTimeFileNameFormater.format(new Date(System.currentTimeMillis())));
        
		System.out.println(milliSecondsToFormatedString(HOUR*24));
        System.out.println(milliSecondsToFormatedString(HOUR*36+MINUTE*30));
        System.out.println(milliSecondsToFormatedString(DAY*78+HOUR*36+MINUTE*30));

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
		
		System.out.println(dateTimeFormater2UTC.format(new Date()));
		System.out.println(new Date());
	}
}
