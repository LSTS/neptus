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
 * 02/06/2015
 */
package pt.lsts.nasa.mts.data;

import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Date;

import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.util.AngleCalc;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.MathMiscUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * IWG1 Packet Definitions
   The IWG1 file is a text file with comma separated values recorded at once per second. The following is listing of the data parameters in order:
    
    IWG1
    TimeStamp (UTC)
    Latitude (deg)
    Longitude (deg)
    GPS_Alt_MSL (m)
    GPS_Altitude (m)
    Pressure_Altitude (ft)
    RADAR_Altitude (ft)
    Ground_Speed (m/s)
    True_Air_Speed (m/s)
    Indicated_Air_Speed (knots)
    Mach_Number (Mach)
    Vertical_Speed (m/s)
    True_Heading (deg)
    Track_Angle (deg)
    Drift_Angle (deg)
    Pitch_Angle (deg)
    Roll_Angle (deg)
    Slip_Angle (deg)
    Attack_Angle (deg)
    Static_Air_Temp (C)
    Dew_Point (C)
    Total_Air_Temp (C)
    Static_Pressure (mbar)
    Dynamic_Pressure (mbar)
    Cabin_Pressure (mbar)
    Wind_Speed (m/s)
    Wind_Direction (deg)
    Vert_Wind_Speed (m/s)
    Solar_Zenith_Angle (deg)
    Aircraft_Sun_Elevation (deg)
    Sun_Azimuth (deg)
    Aircraft_Sun_Azimuth (deg)
 *   
 *   Short Name Units   Range   Description
Date/Time   UTC     ISO-8601 formatted date and time
Lat degree_N (dec)  -90 to 90   Platform Latitude
Lon degree_E (dec)  -180 to 179.9999    Platform Longitude
GPS_MSL_Alt m       GPS Altitude, Mean Sea Level (MSL)
WGS_84_Alt  m       WGS 84 Geoid Altitude
Press_Alt   feet        Pressure Altitude
Radar_Alt   feet    Zero or greater Radar Altimeter Altitude
Grnd_Spd    m/s     Ground Speed
True_Airspeed   m/s     True Airspeed
Indicated_Airspeed  knots       Indicated Airspeed
Mach_Number         Aircraft Mach Number
Vert_Velocity   m/s [3] Aircraft Vertical Velocity
True_Hdg    degrees_true    0 to 359.9999   True Heading
Track   degrees_true    0 to 359.9999   Track Angle
Drift   degrees     Drift Angle
Pitch   degrees -90 to 90 [1]   Pitch
Roll    degrees -90 to 90 [2]   Roll
Side_slip   degrees     Side Slip Angle
Angle_of_Attack degrees -90 to 90 [1]   Angle of Attack
Ambient_Temp    degrees_C       Ambient Temperature
Dew_Point   degrees_C       Dew Point
Total_Temp  degrees_C       Total Temperature
Static_Press    mbar        Static Pressure
Dynamic_Press   mbar        Dynamic Pressure (total minus static)
Cabin_Pressure  mbar        Cabin Pressure / Altitude
Wind_Speed  m/s Zero or greater Wind Speed
Wind_Dir    degrees_true    0 to 359.9999   Wind Direction
Vert_Wind_Spd   m/s [3] Vertical Wind Speed
Solar_Zenith    degrees     Solar Zenith Angle
Sun_Elev_AC degrees     Sun Elevation from Aircraft
Sun_Az_Grd  degrees_true    0 to 359.9999   Sun Azimuth from Ground
Sun_Az_AC   degrees_true    0 to 359.9999   Sun Azimuth from Aircraft

[1] Negative is nose down, positive is nose up.
[2] Negative is left wing down, positive is right wing down.
[3] Negative is downward, positive is upward.

 * @author pdias
 *
 */
public class IWG1Data {

    private static final int NUMBER_OF_MANDATORY_ELEMENTS = 33;
    
    @Expose
    private long timeStampMillis = -1;
    @Expose
    private double latitudeDegs = Double.NaN;
    @Expose
    private double longitudeDegs = Double.NaN;
    @Expose
    private double gpsAltMSL = Double.NaN;
    @Expose
    private double gpsAltitude = Double.NaN;
    @Expose
    private double pressureAltitude = Double.NaN;
    @Expose
    private double radarAltitude = Double.NaN;
    @Expose
    private double groundSpeed = Double.NaN;
    @Expose
    private double trueAirSpeed = Double.NaN;
    @Expose
    private double indicatedAirSpeed = Double.NaN;
    @Expose
    private double machNumber = Double.NaN;
    @Expose
    private double verticalSpeed = Double.NaN;
    @Expose
    private double trueHeadingDegs = Double.NaN;
    @Expose
    private double trackAngleDegs = Double.NaN;
    @Expose
    private double driftAngleDegs = Double.NaN;
    @Expose
    private double pitchAngleDegs = Double.NaN;
    @Expose
    private double rollAngleDegs = Double.NaN;
    @Expose
    private double slipAngleDegs = Double.NaN;
    @Expose
    private double attackAngleDegs = Double.NaN;
    @Expose
    private double staticAirTemp = Double.NaN;
    @Expose
    private double dewPoint = Double.NaN;
    @Expose
    private double totalAirTemp = Double.NaN;
    @Expose
    private double staticPressure = Double.NaN;
    @Expose
    private double dynamicPressure = Double.NaN;
    @Expose
    private double cabinPressure = Double.NaN;
    @Expose
    private double windSpeed = Double.NaN;
    @Expose
    private double windDirection = Double.NaN;
    @Expose
    private double vertWindSpeed = Double.NaN;
    @Expose
    private double solarZenithAngleDegs = Double.NaN;
    @Expose
    private double aircraftSunElevationDegs = Double.NaN;
    @Expose
    private double sunAzimuthDegs = Double.NaN;
    @Expose
    private double aircraftSunAzimuthDegs = Double.NaN;

    // Private fields
    @Expose
    private long sourceId = ImcId16.NULL_ID.longValue();
    @Expose
    private double bathymetry = Double.NaN;
    @Expose
    private double temperature = Double.NaN;
    @Expose
    private double salinity = Double.NaN;
    @Expose
    private double condutivity = Double.NaN;
    
    
    private static Gson gson = null;
    
    
    public IWG1Data() {
    }

    /**
     * @return the timeStampMillis
     */
    public long getTimeStampMillis() {
        return timeStampMillis;
    }

    /**
     * @param timeStampMillis the timeStampMillis to set
     */
    public void setTimeStampMillis(long timeStampMillis) {
        this.timeStampMillis = timeStampMillis;
    }

    /**
     * @return the latitudeDegs
     */
    public double getLatitudeDegs() {
        return latitudeDegs;
    }

    /**
     * @param latitudeDegs the latitudeDegs to set
     */
    public void setLatitudeDegs(double latitudeDegs) {
        this.latitudeDegs = latitudeDegs;
    }

    /**
     * @return the longitudeDegs
     */
    public double getLongitudeDegs() {
        return longitudeDegs;
    }

    /**
     * @param longitudeDegs the longitudeDegs to set
     */
    public void setLongitudeDegs(double longitudeDegs) {
        this.longitudeDegs = longitudeDegs;
    }

    /**
     * @return the gpsAltMSL
     */
    public double getGpsAltMSL() {
        return gpsAltMSL;
    }

    /**
     * @param gpsAltMSL the gpsAltMSL to set
     */
    public void setGpsAltMSL(double gpsAltMSL) {
        this.gpsAltMSL = gpsAltMSL;
    }

    /**
     * @return the gpsAltitude
     */
    public double getGpsAltitude() {
        return gpsAltitude;
    }

    /**
     * @param gpsAltitude the gpsAltitude to set
     */
    public void setGpsAltitude(double gpsAltitude) {
        this.gpsAltitude = gpsAltitude;
    }

    /**
     * @return the pressureAltitude
     */
    public double getPressureAltitude() {
        return pressureAltitude;
    }

    /**
     * @param pressureAltitude the pressureAltitude to set
     */
    public void setPressureAltitude(double pressureAltitude) {
        this.pressureAltitude = pressureAltitude;
    }

    /**
     * @return the radarAltitude
     */
    public double getRadarAltitude() {
        return radarAltitude;
    }

    /**
     * @param radarAltitude the radarAltitude to set
     */
    public void setRadarAltitude(double radarAltitude) {
        this.radarAltitude = radarAltitude;
    }

    /**
     * @return the groundSpeed
     */
    public double getGroundSpeed() {
        return groundSpeed;
    }

    /**
     * @param groundSpeed the groundSpeed to set
     */
    public void setGroundSpeed(double groundSpeed) {
        this.groundSpeed = groundSpeed;
    }

    /**
     * @return the trueAirSpeed
     */
    public double getTrueAirSpeed() {
        return trueAirSpeed;
    }

    /**
     * @param trueAirSpeed the trueAirSpeed to set
     */
    public void setTrueAirSpeed(double trueAirSpeed) {
        this.trueAirSpeed = trueAirSpeed;
    }

    /**
     * @return the indicatedAirSpeed
     */
    public double getIndicatedAirSpeed() {
        return indicatedAirSpeed;
    }

    /**
     * @param indicatedAirSpeed the indicatedAirSpeed to set
     */
    public void setIndicatedAirSpeed(double indicatedAirSpeed) {
        this.indicatedAirSpeed = indicatedAirSpeed;
    }

    /**
     * @return the machNumber
     */
    public double getMachNumber() {
        return machNumber;
    }

    /**
     * @param machNumber the machNumber to set
     */
    public void setMachNumber(double machNumber) {
        this.machNumber = machNumber;
    }

    /**
     * @return the verticalSpeed
     */
    public double getVerticalSpeed() {
        return verticalSpeed;
    }

    /**
     * @param verticalSpeed the verticalSpeed to set
     */
    public void setVerticalSpeed(double verticalSpeed) {
        this.verticalSpeed = verticalSpeed;
    }

    /**
     * @return the trueHeadingDegs
     */
    public double getTrueHeadingDegs() {
        return trueHeadingDegs;
    }

    /**
     * @param trueHeadingDegs the trueHeadingDegs to set
     */
    public void setTrueHeadingDegs(double trueHeadingDegs) {
        this.trueHeadingDegs = trueHeadingDegs;
    }

    /**
     * @return the trackAngleDegs
     */
    public double getTrackAngleDegs() {
        return trackAngleDegs;
    }

    /**
     * @param trackAngleDegs the trackAngleDegs to set
     */
    public void setTrackAngleDegs(double trackAngleDegs) {
        this.trackAngleDegs = trackAngleDegs;
    }

    /**
     * @return the driftAngleDegs
     */
    public double getDriftAngleDegs() {
        return driftAngleDegs;
    }

    /**
     * @param driftAngleDegs the driftAngleDegs to set
     */
    public void setDriftAngleDegs(double driftAngleDegs) {
        this.driftAngleDegs = driftAngleDegs;
    }

    /**
     * @return the pitchAngleDegs
     */
    public double getPitchAngleDegs() {
        return pitchAngleDegs;
    }

    /**
     * @param pitchAngleDegs the pitchAngleDegs to set
     */
    public void setPitchAngleDegs(double pitchAngleDegs) {
        this.pitchAngleDegs = pitchAngleDegs;
    }

    /**
     * @return the rollAngleDegs
     */
    public double getRollAngleDegs() {
        return rollAngleDegs;
    }

    /**
     * @param rollAngleDegs the rollAngleDegs to set
     */
    public void setRollAngleDegs(double rollAngleDegs) {
        this.rollAngleDegs = rollAngleDegs;
    }

    /**
     * @return the slipAngleDegs
     */
    public double getSlipAngleDegs() {
        return slipAngleDegs;
    }

    /**
     * @param slipAngleDegs the slipAngleDegs to set
     */
    public void setSlipAngleDegs(double slipAngleDegs) {
        this.slipAngleDegs = slipAngleDegs;
    }

    /**
     * @return the attackAngleDegs
     */
    public double getAttackAngleDegs() {
        return attackAngleDegs;
    }

    /**
     * @param attackAngleDegs the attackAngleDegs to set
     */
    public void setAttackAngleDegs(double attackAngleDegs) {
        this.attackAngleDegs = attackAngleDegs;
    }

    /**
     * @return the staticAirTemp
     */
    public double getStaticAirTemp() {
        return staticAirTemp;
    }

    /**
     * @param staticAirTemp the staticAirTemp to set
     */
    public void setStaticAirTemp(double staticAirTemp) {
        this.staticAirTemp = staticAirTemp;
    }

    /**
     * @return the dewPoint
     */
    public double getDewPoint() {
        return dewPoint;
    }
    
    /**
     * @param dewPoint the dewPoint to set
     */
    public void setDewPoint(double dewPoint) {
        this.dewPoint = dewPoint;
    }
    
    /**
     * @return the totalAirTemp
     */
    public double getTotalAirTemp() {
        return totalAirTemp;
    }
    
    /**
     * @param totalAirTemp the totalAirTemp to set
     */
    public void setTotalAirTemp(double totalAirTemp) {
        this.totalAirTemp = totalAirTemp;
    }
    
    /**
     * @return the staticPressure
     */
    public double getStaticPressure() {
        return staticPressure;
    }

    /**
     * @param staticPressure the staticPressure to set
     */
    public void setStaticPressure(double staticPressure) {
        this.staticPressure = staticPressure;
    }

    /**
     * @return the dynamicPressure
     */
    public double getDynamicPressure() {
        return dynamicPressure;
    }

    /**
     * @param dynamicPressure the dynamicPressure to set
     */
    public void setDynamicPressure(double dynamicPressure) {
        this.dynamicPressure = dynamicPressure;
    }

    /**
     * @return the cabinPressure
     */
    public double getCabinPressure() {
        return cabinPressure;
    }

    /**
     * @param cabinPressure the cabinPressure to set
     */
    public void setCabinPressure(double cabinPressure) {
        this.cabinPressure = cabinPressure;
    }

    /**
     * @return the windSpeed
     */
    public double getWindSpeed() {
        return windSpeed;
    }

    /**
     * @param windSpeed the windSpeed to set
     */
    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    /**
     * @return the windDirection
     */
    public double getWindDirection() {
        return windDirection;
    }

    /**
     * @param windDirection the windDirection to set
     */
    public void setWindDirection(double windDirection) {
        this.windDirection = windDirection;
    }

    /**
     * @return the vertWindSpeed
     */
    public double getVertWindSpeed() {
        return vertWindSpeed;
    }

    /**
     * @param vertWindSpeed the vertWindSpeed to set
     */
    public void setVertWindSpeed(double vertWindSpeed) {
        this.vertWindSpeed = vertWindSpeed;
    }

    /**
     * @return the solarZenithAngleDegs
     */
    public double getSolarZenithAngleDegs() {
        return solarZenithAngleDegs;
    }

    /**
     * @param solarZenithAngleDegs the solarZenithAngleDegs to set
     */
    public void setSolarZenithAngleDegs(double solarZenithAngleDegs) {
        this.solarZenithAngleDegs = solarZenithAngleDegs;
    }

    /**
     * @return the aircraftSunElevationDegs
     */
    public double getAircraftSunElevationDegs() {
        return aircraftSunElevationDegs;
    }

    /**
     * @param aircraftSunElevationDegs the aircraftSunElevationDegs to set
     */
    public void setAircraftSunElevationDegs(double aircraftSunElevationDegs) {
        this.aircraftSunElevationDegs = aircraftSunElevationDegs;
    }

    /**
     * @return the sunAzimuthDegs
     */
    public double getSunAzimuthDegs() {
        return sunAzimuthDegs;
    }

    /**
     * @param sunAzimuthDegs the sunAzimuthDegs to set
     */
    public void setSunAzimuthDegs(double sunAzimuthDegs) {
        this.sunAzimuthDegs = sunAzimuthDegs;
    }

    /**
     * @return the aircraftSunAzimuthDegs
     */
    public double getAircraftSunAzimuthDegs() {
        return aircraftSunAzimuthDegs;
    }

    /**
     * @param aircraftSunAzimuthDegs the aircraftSunAzimuthDegs to set
     */
    public void setAircraftSunAzimuthDegs(double aircraftSunAzimuthDegs) {
        this.aircraftSunAzimuthDegs = aircraftSunAzimuthDegs;
    }

    /**
     * @return the sourceId
     */
    public long getSourceId() {
        return sourceId;
    }
    
    /**
     * @param sourceId the sourceId to set
     */
    public void setSourceId(long sourceId) {
        this.sourceId = sourceId;
    }
    
    /**
     * @return the bathymetry
     */
    public double getBathymetry() {
        return bathymetry;
    }

    /**
     * @param bathymetry the bathymetry to set
     */
    public void setBathymetry(double bathymetry) {
        this.bathymetry = bathymetry;
    }

    /**
     * @return the temperature
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * @param temperature the temperature to set
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /**
     * @return the salinity
     */
    public double getSalinity() {
        return salinity;
    }

    /**
     * @param salinity the salinity to set
     */
    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    /**
     * @return the condutivity
     */
    public double getCondutivity() {
        return condutivity;
    }

    /**
     * @param condutivity the condutivity to set
     */
    public void setCondutivity(double condutivity) {
        this.condutivity = condutivity;
    }

    
//    /**
//     * It is assumed that the reader starts at the IWG1 prefix
//     * @param iwg1Reader
//     * @return
//     */
//    public static IWG1Data parseIWG1(Reader iwg1Reader) {
//        synchronized (IWG1Data.class) {
//            StringBuilder sb = new StringBuilder();
//            
//            char[] cbuf = new char[4];
//            iwg1Reader.read(cbuf, 0, 4);
//        }
//
//        return null;
//    }
    
    public static IWG1Data parseIWG1(String iwg1) {
        if (iwg1 == null || iwg1.isEmpty())
            return null;
        
        IWG1Data ret = new IWG1Data();
        
        String[] tokens = iwg1.split(",");
        if (tokens.length == 0 || tokens.length < NUMBER_OF_MANDATORY_ELEMENTS)
            return null;
        
        for (int i = 0; i < tokens.length; i++) {
            String tk = tokens[i];
            if (tk == null)
                return null;
            switch (i) {
                case 0:
                    if (tk.length() < 4 || !"IWG1".equalsIgnoreCase(tk))
                        return null;
                    break;
                case 1:
                    parseTimeStamp(ret, tk);
                    break;
                case 2:
                    ret.latitudeDegs = parseDouble(tk);
                    break;
                case 3:
                    ret.longitudeDegs = parseDouble(tk);
                    break;
                case 4:
                    ret.gpsAltMSL = parseDouble(tk);
                    break;
                case 5:
                    ret.gpsAltitude = parseDouble(tk);
                    break;
                case 6:
                    ret.pressureAltitude = parseDouble(tk);
                    break;
                case 7:
                    ret.radarAltitude = parseDouble(tk);
                    break;
                case 8:
                    ret.groundSpeed = parseDouble(tk);
                    break;
                case 9:
                    ret.trueAirSpeed = parseDouble(tk);
                    break;
                case 10:
                    ret.indicatedAirSpeed = parseDouble(tk);
                    break;
                case 11:
                    ret.machNumber = parseDouble(tk);
                    break;
                case 12:
                    ret.verticalSpeed = parseDouble(tk);
                    break;
                case 13:
                    ret.trueHeadingDegs = parseDouble(tk);
                    break;
                case 14:
                    ret.trackAngleDegs = parseDouble(tk);
                    break;
                case 15:
                    ret.driftAngleDegs = parseDouble(tk);
                    break;
                case 16:
                    ret.pitchAngleDegs = parseDouble(tk);
                    break;
                case 17:
                    ret.rollAngleDegs = parseDouble(tk);
                    break;
                case 18:
                    ret.slipAngleDegs = parseDouble(tk);
                    break;
                case 19:
                    ret.attackAngleDegs = parseDouble(tk);
                    break;
                case 20:
                    ret.staticAirTemp = parseDouble(tk);
                    break;
                case 21:
                    ret.dewPoint = parseDouble(tk);
                    break;
                case 22:
                    ret.totalAirTemp = parseDouble(tk);
                    break;
                case 23:
                    ret.staticPressure = parseDouble(tk);
                    break;
                case 24:
                    ret.dynamicPressure = parseDouble(tk);
                    break;
                case 25:
                    ret.cabinPressure = parseDouble(tk);
                    break;
                case 26:
                    ret.windSpeed = parseDouble(tk);
                    break;
                case 27:
                    ret.windDirection = parseDouble(tk);
                    break;
                case 28:
                    ret.vertWindSpeed = parseDouble(tk);
                    break;
                case 29:
                    ret.solarZenithAngleDegs = parseDouble(tk);
                    break;
                case 30:
                    ret.aircraftSunElevationDegs = parseDouble(tk);
                    break;
                case 31:
                    ret.sunAzimuthDegs = parseDouble(tk);
                    break;
                case 32:
                    ret.aircraftSunAzimuthDegs = parseDouble(tk);
                    break;
                // Custom parameters
                case 33:
                    double id = parseDouble(tk);
                    if (!Double.isNaN(id)) {
                        ret.sourceId = new Double(id).longValue();
                    }
                    break;
                case 34:
                    ret.bathymetry = parseDouble(tk);
                    break;
                case 35:
                    ret.temperature = parseDouble(tk);
                    break;
                case 36:
                    ret.salinity = parseDouble(tk);
                    break;
                case 37:
                    ret.condutivity = parseDouble(tk);
                    break;
                default:
                    break;
            }
        }
        
        return ret;
    }

    /**
     * @param ret
     * @param tk
     */
    private static void parseTimeStamp(IWG1Data ret, String tk) {
        Date dateTime = null;
        try {
            dateTime = DateTimeUtil.dateTimeFormaterISO8601.parse(tk);
        }
        catch (ParseException e) {
            try {
                dateTime = DateTimeUtil.dateTimeFormaterISO8601_1.parse(tk);
            }
            catch (ParseException e1) {
                try {
                    dateTime = DateTimeUtil.dateTimeFormaterISO8601_2.parse(tk);
                }
                catch (ParseException e2) {
                    e2.printStackTrace();
                }
            }
        }
        if (dateTime != null)
            ret.setTimeStampMillis(dateTime.getTime());
    }

    /**
     * @param ret
     * @param tk
     */
    private static double parseDouble(String tk) {
        try {
            if ("NaN".equalsIgnoreCase(tk))
                return Double.NaN;
            if ("inf".equalsIgnoreCase(tk))
                return Double.NaN;
            return Double.parseDouble(tk);
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public String toIWG1() {
        StringBuilder sb = new StringBuilder("IWG1");
        synchronized (IWG1Data.class) {
            
            sb.append(",");
            if (timeStampMillis >= 0) {
                sb.append(DateTimeUtil.dateTimeFormaterISO8601.format(new Date(new Double(timeStampMillis).longValue())));
            }
            sb.append(",");
            if (!Double.isNaN(latitudeDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees180(latitudeDegs), 4));
            }
            sb.append(",");
            if (!Double.isNaN(longitudeDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees180(longitudeDegs), 4));
            }
            sb.append(",");
            if (!Double.isNaN(gpsAltMSL)) {
                sb.append(MathMiscUtils.round(gpsAltMSL, 4));
            }
            sb.append(",");
            if (!Double.isNaN(gpsAltitude)) {
                sb.append(MathMiscUtils.round(gpsAltitude, 4));
            }
            sb.append(",");
            if (!Double.isNaN(pressureAltitude)) {
                sb.append(MathMiscUtils.round(pressureAltitude, 4));
            }
            sb.append(",");
            if (!Double.isNaN(radarAltitude)) {
                sb.append(MathMiscUtils.round(radarAltitude, 4));
            }
            sb.append(",");
            if (!Double.isNaN(groundSpeed)) {
                sb.append(MathMiscUtils.round(groundSpeed, 4));
            }
            sb.append(",");
            if (!Double.isNaN(trueAirSpeed)) {
                sb.append(MathMiscUtils.round(trueAirSpeed, 4));
            }
            sb.append(",");
            if (!Double.isNaN(indicatedAirSpeed)) {
                sb.append(MathMiscUtils.round(indicatedAirSpeed, 4));
            }
            sb.append(",");
            if (!Double.isNaN(machNumber)) {
                sb.append(MathMiscUtils.round(machNumber, 4));
            }
            sb.append(",");
            if (!Double.isNaN(verticalSpeed)) {
                sb.append(MathMiscUtils.round(verticalSpeed, 4));
            }
            sb.append(",");
            if (!Double.isNaN(trueHeadingDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees360(trueHeadingDegs), 4));
            }
            sb.append(",");
            if (!Double.isNaN(trackAngleDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees360(trackAngleDegs), 4));
            }
            sb.append(",");
            if (!Double.isNaN(driftAngleDegs)) {
                sb.append(MathMiscUtils.round(driftAngleDegs, 4));
            }
            sb.append(",");
            if (!Double.isNaN(pitchAngleDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees180(pitchAngleDegs), 4));
            }
            sb.append(",");
            if (!Double.isNaN(rollAngleDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees180(rollAngleDegs), 4));
            }
            sb.append(",");
            if (!Double.isNaN(slipAngleDegs)) {
                sb.append(MathMiscUtils.round(slipAngleDegs, 4));
            }
            sb.append(",");
            if (!Double.isNaN(attackAngleDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees180(attackAngleDegs), 4));
            }
            sb.append(",");
            if (!Double.isNaN(staticAirTemp)) {
                sb.append(MathMiscUtils.round(staticAirTemp, 4));
            }
            sb.append(",");
            if (!Double.isNaN(dewPoint)) {
                sb.append(MathMiscUtils.round(dewPoint, 4));
            }
            sb.append(",");
            if (!Double.isNaN(totalAirTemp)) {
                sb.append(MathMiscUtils.round(totalAirTemp, 4));
            }
            sb.append(",");
            if (!Double.isNaN(staticPressure)) {
                sb.append(MathMiscUtils.round(staticPressure, 4));
            }
            sb.append(",");
            if (!Double.isNaN(dynamicPressure)) {
                sb.append(MathMiscUtils.round(dynamicPressure, 4));
            }
            sb.append(",");
            if (!Double.isNaN(cabinPressure)) {
                sb.append(MathMiscUtils.round(cabinPressure, 4));
            }
            sb.append(",");
            if (!Double.isNaN(windSpeed)) {
                sb.append(MathMiscUtils.round(windSpeed, 4));
            }
            sb.append(",");
            if (!Double.isNaN(windDirection)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees360(windDirection), 4));
            }
            sb.append(",");
            if (!Double.isNaN(vertWindSpeed)) {
                sb.append(MathMiscUtils.round(vertWindSpeed, 4));
            }
            sb.append(",");
            if (!Double.isNaN(solarZenithAngleDegs)) {
                sb.append(MathMiscUtils.round(solarZenithAngleDegs, 4));
            }
            sb.append(",");
            if (!Double.isNaN(aircraftSunElevationDegs)) {
                sb.append(MathMiscUtils.round(aircraftSunElevationDegs, 4));
            }
            sb.append(",");
            if (!Double.isNaN(sunAzimuthDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees360(sunAzimuthDegs), 4));
            }
            sb.append(",");
            if (!Double.isNaN(aircraftSunAzimuthDegs)) {
                sb.append(MathMiscUtils.round(AngleCalc.nomalizeAngleDegrees360(aircraftSunAzimuthDegs), 4));
            }

            // Private fields
            sb.append(",");
            if (sourceId != ImcId16.NULL_ID.longValue()) {
                sb.append(sourceId);
            }
            sb.append(",");
            if (!Double.isNaN(bathymetry)) {
                sb.append(MathMiscUtils.round(bathymetry, 4));
            }
            sb.append(",");
            if (!Double.isNaN(temperature)) {
                sb.append(MathMiscUtils.round(temperature, 4));
            }
            sb.append(",");
            if (!Double.isNaN(salinity)) {
                sb.append(MathMiscUtils.round(salinity, 4));
            }
            sb.append(",");
            if (!Double.isNaN(condutivity)) {
                sb.append(MathMiscUtils.round(condutivity, 4));
            }

        }
        sb.append("\r\n");
        return sb.toString();
    }

    public static IWG1Data parseJSON(Reader jsonReader) {
        synchronized (IWG1Data.class) {
            if (gson == null)
                gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    //.registerTypeAdapter(Id.class, new IdTypeAdapter())
                    .enableComplexMapKeySerialization()
                    .serializeSpecialFloatingPointValues()
                    //.setDateFormat(DateFormat.LONG)
                    //.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                    .setPrettyPrinting()
                    .setVersion(1.0)
                    .create();;
        }
        return gson.fromJson(jsonReader, IWG1Data.class);
    }
    
    public static IWG1Data parseJSON(String json) {
        return parseJSON(new StringReader(json));
    }

    public String toJSON() {
        synchronized (IWG1Data.class) {
            if (gson == null)
                gson = new Gson();
        }
        return gson.toJson(this);
    }
    
    public static void main(String[] args) throws Exception {
//        IWG1Data o = IWG1Data.parseJSON(new FileReader(new File(".", "plugins-dev/cloud/com/inovaworks/example.json")));
        IWG1Data i = new IWG1Data();
        System.out.println(i.toIWG1());
    }
}
