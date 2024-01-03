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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.dinopolis.gpstool.gpsinput.nmea.NMEA0183Sentence;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author ZP
 * @author pdias
 */
public class NMEAUtils {
	
	private static LinkedHashMap<String, Integer> gps_uids = new LinkedHashMap<String, Integer>();
	private static IMCMessage gpsFix = IMCDefinition.getInstance().create("GpsFix");	
	private static IMCMessage customMessage = null;
	
    private static boolean isExtGpsFix = true;

    {
        if (gpsFix != null) {
            if (gpsFix.getMessageType().getFieldType("validity") != null)
                isExtGpsFix = true;
            else
                isExtGpsFix = false;
        }
    }

	
	public static IMCMessage getCustomMessage() {
		if (customMessage == null) {
			IMCMessageType type = new IMCMessageType();
			type.addField("id", "plaintext", null, null, null);
			type.addField("home_lat", "fp64_t", "rad", null, null);
			type.addField("home_lon", "fp64_t", "rad", null, null);
			type.addField("x", "fp32_t", "meter", null, null);
			type.addField("y", "fp32_t", "meter", null, null);
			type.addField("z", "fp32_t", "meter", null, null);
			customMessage = new IMCMessage(type);
		}
		return customMessage;
	}
	
	public static void logGGASentenceAsGpsFix(String source, String ggaSentence) {
		
		try {
			NMEA0183Sentence nmea = new NMEA0183Sentence(ggaSentence);
		
			if (!nmea.isValid())
				return;
			
			
			List<?> data_fields = nmea.getDataFields();
			
			int time = (int)Float.parseFloat((String) data_fields.get(0));
			String latitude = (String) data_fields.get(1);
			String north_south = (String) data_fields.get(2);
			String longitude = (String) data_fields.get(3);
			String east_west = (String) data_fields.get(4);
			int valid_fix = Integer.parseInt((String) data_fields.get(5));
			int num_satellites = Integer.parseInt((String) data_fields.get(6));
			float hor_dilution = Float.parseFloat((String) data_fields.get(7));
			float altitude = Float.parseFloat((String) data_fields.get(8));
			
			double wgs84_lat = nmeaLatOrLongToWGS84(latitude);
			double wgs84_long = nmeaLatOrLongToWGS84(longitude);
	
			if (north_south.equalsIgnoreCase("S"))
				wgs84_lat = -wgs84_lat;
	
			if (east_west.equalsIgnoreCase("W"))
				wgs84_long = -wgs84_long;
			
			int uid = 0;
			if (gps_uids.containsKey(source))
				uid = gps_uids.get(source);
			else {
				uid = gps_uids.size();
				gps_uids.put(source, uid);
			}

			gpsFix.setValue("id", uid);
			gpsFix.setValue("time", (int)time);
			gpsFix.setValue("lat", Math.toRadians(Double.valueOf(wgs84_lat)));
			gpsFix.setValue("lon", Math.toRadians(Double.valueOf(wgs84_long)));           
			if (isExtGpsFix) {
                int valitity = (valid_fix > 0 ? 0x4/* VALID_POS */: 0x0) | 0x80/* VALID_HDOP */;
			    gpsFix.setValue("validity", valitity);
			    gpsFix.setValue("height", altitude);
			    gpsFix.setValue("hdop", hor_dilution);
			}
			else {
			    gpsFix.setValue("fix_quality", (valid_fix > 0)? 1 : 0);
			    gpsFix.setValue("altitude", altitude);
	            gpsFix.setValue("hdilution", hor_dilution);
			}
			gpsFix.setValue("satellites", num_satellites);
			
//			NeptusMessageLogger.getLogger().logMessage(source, "unknown", gpsFix);
			gpsFix.setSrc(ImcId16.NULL_ID.intValue());
            LsfMessageLogger.log(gpsFix);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void convertGGALogToLLF(File source, File dest) throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(source));
		
		String line = reader.readLine();
		while (line != null) {
			logGGASentenceAsGpsFix(source.getName(), line);
		}		
		reader.close();
	}
	
	public static String nmeaType(String sentence) {
	    return sentence.trim().split(",")[0];
	}
	
	public static double nmeaLatOrLongToWGS84(String nmea_pos) throws NumberFormatException {
		int comma_pos = nmea_pos.indexOf('.');
		if ((comma_pos != 4) && (comma_pos != 5))
			throw new NumberFormatException("unknown NMEA position format: '" + nmea_pos + "'");

		String wgs84_deg = nmea_pos.substring(0, comma_pos - 2);
		String wgs84_min = nmea_pos.substring(comma_pos - 2);
		double wgs84_pos = Double.parseDouble(wgs84_deg) + Double.parseDouble(wgs84_min) / 60.0;
		return (wgs84_pos);
	}

    private static Date processTimeFromSentence(String sentence, int fieldCount) {        
        try {
            NMEA0183Sentence nmea = new NMEA0183Sentence(sentence);
        
            if (!nmea.isValid())
                return null;
            
            List<?> data_fields = nmea.getDataFields();
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
            
            String timeUTC = (String) data_fields.get(0); // hhmmss.ss UTC
            if (timeUTC == null || timeUTC.length() == 0) {
                return null;
            }

            LocalTime ltNow = LocalTime.now(ZoneId.of("UTC"));
            LocalTime tTime = LocalTime.MIDNIGHT.plusHours(Long.parseLong(timeUTC.substring(0, 2)))
                    .plusMinutes(Long.parseLong(timeUTC.substring(2, 4)))
                    .plus(Double.valueOf(Double.parseDouble(timeUTC.substring(4)) * 1E6).longValue(), ChronoUnit.MICROS);
            Duration tSpan = Duration.between(ltNow, tTime);
            if (tSpan.abs().toHours() > 12) {
                if (tSpan.isNegative())
                    now = now.plusDays(1);
                else
                    now = now.minusDays(1);
            }
            
            now = now.plusHours(Long.parseLong(timeUTC.substring(0, 2)));
            now = now.plusMinutes(Long.parseLong(timeUTC.substring(2, 4)));
            now = now.plus(Double.valueOf(Double.parseDouble(timeUTC.substring(4)) * 1E6).longValue(), ChronoUnit.MICROS);
            Date date = Date.from(now.toInstant());
            return date;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Date processGGATimeFromSentence(String sentence) {
        return processTimeFromSentence(sentence, 0);
    }

    public static Date processGLLTimeFromSentence(String sentence) {
        return processTimeFromSentence(sentence, 4);
    }

	public static LocationType processGGASentence(String sentence) {		
		try {
			NMEA0183Sentence nmea = new NMEA0183Sentence(sentence);
		
			if (!nmea.isValid())
				return null;
			
			List<?> data_fields = nmea.getDataFields();
			
			String latitude = (String) data_fields.get(1);
			String north_south = (String) data_fields.get(2);
			String longitude = (String) data_fields.get(3);
			String east_west = (String) data_fields.get(4);
			
			int valid_fix = Integer.parseInt((String) data_fields.get(5));
	
			if (valid_fix == 0)
				return null;
	
			// check for empty messages:
			if (latitude.length() == 0)
				return null;
	
			double wgs84_lat = nmeaLatOrLongToWGS84(latitude);
			double wgs84_long = nmeaLatOrLongToWGS84(longitude);
	
			if (north_south.equalsIgnoreCase("S"))
				wgs84_lat = -wgs84_lat;
	
			if (east_west.equalsIgnoreCase("W"))
				wgs84_long = -wgs84_long;
	
			LocationType loc = new LocationType();
			loc.setLatitudeDegs(wgs84_lat);
			loc.setLongitudeDegs(wgs84_long);
			loc.setDepth(-Float.valueOf((String)data_fields.get(8)));
	
			return loc;
		}
		catch (Exception ex) {
		    NeptusLog.pub().warn("Corrupted GGA String: " + ex.getMessage());
			return null;
		}
	}
	
    public static double processGPHDTSentence(String sentence) {        
        try {
            NMEA0183Sentence nmea = new NMEA0183Sentence(sentence);
        
            if (!nmea.isValid())
                return Double.NaN;
            
            List<?> data_fields = nmea.getDataFields();
            
            String headingDegsStr = (String) data_fields.get(0);

            // check for empty messages:
            if (headingDegsStr.length() == 0)
                return Double.NaN;
    
            try {
                double headDegs = Double.parseDouble(headingDegsStr);
                return headDegs;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
    
            return Double.NaN;
        }
        catch (Exception ex) {
            NeptusLog.pub().warn("Corrupted GPHDT String: " + ex.getMessage());
            return Double.NaN;
        }
    }

    public static LocationType processRMCSentence(String sentence) {
		try {
			NMEA0183Sentence nmea = new NMEA0183Sentence(sentence);
			if (!nmea.isValid())
				return null;
			List<?> data_fields = nmea.getDataFields();

			String latitude = (String) data_fields.get(2);
			String north_south = (String) data_fields.get(3);
			String longitude = (String) data_fields.get(4);
			String east_west = (String) data_fields.get(5);

			// check for empty messages:
			if (latitude.length() == 0)
				return null;

			double wgs84_lat = nmeaLatOrLongToWGS84(latitude);
			double wgs84_long = nmeaLatOrLongToWGS84(longitude);

			if (north_south.equalsIgnoreCase("S"))
				wgs84_lat = -wgs84_lat;

			if (east_west.equalsIgnoreCase("W"))
				wgs84_long = -wgs84_long;

			LocationType loc = new LocationType();
			loc.setLatitudeDegs(wgs84_lat);
			loc.setLongitudeDegs(wgs84_long);

			return loc;
		}
		catch (Exception ex) {
		    NeptusLog.pub().warn("Corrupted RMC String: " + ex.getMessage());
			return null;
		}
	}

    public static LocationType processGLLSentence(String sentence) {
        try {
            NMEA0183Sentence nmea = new NMEA0183Sentence(sentence);
            if (!nmea.isValid())
                return null;
            List<?> data_fields = nmea.getDataFields();

            String latitude = (String) data_fields.get(0);
            String north_south = (String) data_fields.get(1);
            String longitude = (String) data_fields.get(2);
            String east_west = (String) data_fields.get(3);

            // check for empty messages:
            if (latitude.length() == 0)
                return null;

            double wgs84_lat = nmeaLatOrLongToWGS84(latitude);
            double wgs84_long = nmeaLatOrLongToWGS84(longitude);

            if (north_south.equalsIgnoreCase("S"))
                wgs84_lat = -wgs84_lat;

            if (east_west.equalsIgnoreCase("W"))
                wgs84_long = -wgs84_long;

            LocationType loc = new LocationType();
            loc.setLatitudeDegs(wgs84_lat);
            loc.setLongitudeDegs(wgs84_long);

            return loc;
        }
        catch (Exception ex) {
            NeptusLog.pub().warn("Corrupted GLL String: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Searches an external system with mmsi or name and return an external system or
     * a new one created (the name is prevalent, if name is not known make it the same
     * as MMSI).
     * 
     * @param mmsi
     * @param name
     * @return
     */
    public static synchronized ExternalSystem getAndRegisterExternalSystem(int mmsi, String name) {
        ExternalSystem sys = null;
        if (name.equals("" + mmsi)) {
            sys = ExternalSystemsHolder.lookupSystem(name);
            if (sys == null) {
                sys = new ExternalSystem(name);
                ExternalSystemsHolder.registerSystem(sys);
            }
        }
        else {
            sys = ExternalSystemsHolder.lookupSystem(name);
            ExternalSystem sysMMSI = ExternalSystemsHolder.lookupSystem("" + mmsi);
            if (sys == null && sysMMSI == null) {
                sys = new ExternalSystem(name);
                ExternalSystemsHolder.registerSystem(sys);
            }
            else if (sys == null && sysMMSI != null) {
                sys = new ExternalSystem(name);
                ExternalSystemsHolder.purgeSystem("" + mmsi);
                ExternalSystemsHolder.registerSystem(sys);
            }
            else {
                // sys exists
                if (sysMMSI != null)
                    ExternalSystemsHolder.purgeSystem("" + mmsi);
            }
        }
        return sys;
    }
    
    public static void mainTimeTest(String[] args) {
        String sentence = "$GPGGA,000843.8794,2953.44042676,N,13210.21690050,W,2,12,0.9,19.10,M,-37.38,M,0.4,0444*4B";
        sentence = "$GPGGA,235958.4569,2957.18648288,N,13145.90976826,W,2,12,1.1,20.04,M,-37.87,M,1.0,0444*4C";
        LocationType myLoc = NMEAUtils.processGGASentence(sentence);
        Date dateTime = NMEAUtils.processGGATimeFromSentence(sentence);
        
        System.out.println(myLoc);
        System.out.println(dateTime);
        
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
        System.out.println(now);
        
        NMEA0183Sentence nmea = new NMEA0183Sentence(sentence);
        List<?> data_fields = nmea.getDataFields();
        String timeUTC = (String) data_fields.get(0); // hhmmss.ss UTC
        
        now = now.plusHours(Long.parseLong(timeUTC.substring(0, 2)));
        now = now.plusMinutes(Long.parseLong(timeUTC.substring(2, 4)));
        now = now.plus(Double.valueOf(Double.parseDouble(timeUTC.substring(4)) * 1E6).longValue(), ChronoUnit.MICROS);
        System.out.println(now);
        
        Date date = Date.from(now.toInstant());
        System.out.println(date);
        
        System.out.println("#############################");
        LocalTime ltNow = LocalTime.now(ZoneId.of("UTC"));
        ltNow = LocalTime.MIDNIGHT.plusHours(23).plusMinutes(59).plus(59059, ChronoUnit.MILLIS); // 23:59:59.059
        System.out.println("local time: " + ltNow);
        LocalTime tTime = LocalTime.MIDNIGHT.plusHours(Long.parseLong(timeUTC.substring(0, 2)))
                .plusMinutes(Long.parseLong(timeUTC.substring(2, 4)))
                .plus(Double.valueOf(Double.parseDouble(timeUTC.substring(4)) * 1E6).longValue(), ChronoUnit.MICROS);
        System.out.println("gps time: " + tTime);
        Duration tSpan = Duration.between(ltNow, tTime);
        System.out.println(tSpan);
        System.out.println(tSpan.abs().toHours() + "  " + (tSpan.isNegative() ? "-" : "+"));
        
        System.out.println("-----");
        ltNow = LocalTime.MIDNIGHT.plusHours(Long.parseLong(timeUTC.substring(0, 2)))
                .plusMinutes(Long.parseLong(timeUTC.substring(2, 4)))
                .plus(Double.valueOf(Double.parseDouble(timeUTC.substring(4)) * 1E6).longValue(), ChronoUnit.MICROS);
        System.out.println("local time: " + ltNow);
        tTime = LocalTime.MIDNIGHT.plusHours(00).plusMinutes(00).plus(0, ChronoUnit.MILLIS); // 00:00:00.000
        System.out.println("gps time: " + tTime);
        tSpan = Duration.between(ltNow, tTime);
        System.out.println(tSpan);
        System.out.println(tSpan.abs().toHours() + "  " + (tSpan.isNegative() ? "-" : "+"));

        System.out.println("-----");
        ltNow = LocalTime.MIDNIGHT.plusHours(Long.parseLong(timeUTC.substring(0, 2)))
                .plusMinutes(Long.parseLong(timeUTC.substring(2, 4)))
                .plus(Double.valueOf(Double.parseDouble(timeUTC.substring(4)) * 1E6).longValue(), ChronoUnit.MICROS);
        System.out.println("local time: " + ltNow);
        tTime = LocalTime.MIDNIGHT.plusHours(23).plusMinutes(59).plus(59059, ChronoUnit.MILLIS); // 23:59:59.059
        System.out.println("gps time: " + tTime);
        tSpan = Duration.between(ltNow, tTime);
        System.out.println(tSpan);
        System.out.println(tSpan.abs().toHours() + "  " + (tSpan.isNegative() ? "-" : "+"));
        
        System.out.println("-----");
        ltNow = LocalTime.MIDNIGHT.plusHours(00).plusMinutes(00).plus(0, ChronoUnit.MILLIS); // 00:00:00.000
        System.out.println("local time: " + ltNow);
        tTime = LocalTime.MIDNIGHT.plusHours(Long.parseLong(timeUTC.substring(0, 2)))
                .plusMinutes(Long.parseLong(timeUTC.substring(2, 4)))
                .plus(Double.valueOf(Double.parseDouble(timeUTC.substring(4)) * 1E6).longValue(), ChronoUnit.MICROS);
        System.out.println("gps time: " + tTime);
        tSpan = Duration.between(ltNow, tTime);
        System.out.println(tSpan);
        System.out.println(tSpan.abs().toHours() + "  " + (tSpan.isNegative() ? "-" : "+"));
        
        System.out.println(processTimeFromSentence("$GPGGA,000843.8794,2953.44042676,N,13210.21690050,W,2,12,0.9,19.10,M,-37.38,M,0.4,0444*4B", 0)
                .toInstant().atZone(ZoneId.of("UTC")));
        System.out.println(processTimeFromSentence("$GPGGA,235958.4569,2957.18648288,N,13145.90976826,W,2,12,1.1,20.04,M,-37.87,M,1.0,0444*4C", 0)
                .toInstant().atZone(ZoneId.of("UTC")));

    }

	public static void main(String[] args) {
		String nmea = "$GPGGA,120602.476,4112.4827,N,00832.0861,W,1,03,3.4,-51.3,M,51.3,M,,0000*5C";
		LocationType lt = NMEAUtils.processGGASentence(nmea);
		NeptusLog.pub().info("<###> "+lt.getDebugString());
		if (lt != null) {
			IMCMessage msg = IMCDefinition.getInstance().create("RemoteSensorInfo");
			msg.setValue("id", "914785889");
			msg.setValue("lat", lt.getLatitudeDegs());
			msg.setValue("lon", lt.getLongitudeDegs());
			msg.setValue("alt", -lt.getDepth());
			msg.setValue("heading", 0);
			msg.setValue("data", "");
			msg.setValue("sensor_class", "auv");
			
			msg.dump(System.out);
		}
		
		logGGASentenceAsGpsFix("+351914785889", nmea);
		logGGASentenceAsGpsFix("+351914785889", nmea);
		logGGASentenceAsGpsFix("+351914785887", nmea);
		logGGASentenceAsGpsFix("+351914785889", nmea);
		
		mainTimeTest(args);
	}
}
