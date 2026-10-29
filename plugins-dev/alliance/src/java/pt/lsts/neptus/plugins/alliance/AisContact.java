/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Jul 24, 2014
 */
package pt.lsts.neptus.plugins.alliance;

import de.baderjene.aistoolkit.aisparser.message.Message01;
import de.baderjene.aistoolkit.aisparser.message.Message03;
import de.baderjene.aistoolkit.aisparser.message.Message05;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AISUtil;

/**
 * @author zp
 *
 */
public class AisContact {

    private int mmsi;
    private double sog = 0;
    private double cog = 0;
    private double hdg = 0;
    private double rateOfTurn = 0;
    private String label = null;
    private long lastUpdate = 0;
    private LocationType loc = new LocationType();
    private String navStatus = "underfined";
    /** Timestamp (epoch ms) of the most recent position report applied, 0 if none. Used for ordering. */
    private long lastReportMillis = 0;
    
    private Message05 additionalProperties = null;

    /** AIS "speed not available" sentinel (knots). */
    public static final double SOG_NOT_AVAILABLE = 102.3;

    /**
     * @return true if the position is a real position (in range, not the AIS 91/181 "not available"
     *         sentinels and not 0,0 which is used by feeds for missing values).
     */
    public static boolean isValidPosition(double latDegs, double lonDegs) {
        if (Double.isNaN(latDegs) || Double.isNaN(lonDegs))
            return false;
        if (latDegs < -90 || latDegs > 90 || lonDegs < -180 || lonDegs > 180)
            return false;
        return latDegs != 0 || lonDegs != 0;
    }

    /**
     * @return true if the angle is a valid heading/course. AIS uses 511 (heading) and 360 (course) as
     *         "not available", so only [0, 360) is accepted.
     */
    public static boolean isValidAngle(double degs) {
        return !Double.isNaN(degs) && degs >= 0 && degs < 360;
    }

    /**
     * @return true if the speed is valid. AIS uses 102.3 knots as "not available".
     */
    public static boolean isValidSpeed(double knots) {
        return !Double.isNaN(knots) && knots >= 0 && knots < SOG_NOT_AVAILABLE;
    }
    
    public LocationType getLocation() {
        return loc;
    }
    
    public AisContact(int mmsi) {
        // System.out.println("Created AIS contact for mmsi "+mmsi);
        this.mmsi = mmsi;
        lastUpdate = System.currentTimeMillis();
    }
    
    public void update(Message01 m) {
        applyPositionReport(m.getLatitude(), m.getLongitude(), m.getTrueHeading(), m.getCourseOverGround(),
                m.getSpeedOverGround(), m.getRateOfTurn(),
                AISUtil.translateNavigationalStatus(m.getNavigationStatus()), m.getSourceMmsi());
    }
    
    public void update(Message03 m) {
        applyPositionReport(m.getLatitude(), m.getLongitude(), m.getTrueHeading(), m.getCourseOverGround(),
                m.getSpeedOverGround(), m.getRateOfTurn(),
                AISUtil.translateNavigationalStatus(m.getNavigationStatus()), m.getSourceMmsi());
    }

    /**
     * Applies a Class A position report, ignoring AIS "not available" sentinel values so they don't
     * overwrite previously known good values.
     */
    private void applyPositionReport(double lat, double lon, double trueHeading, double courseOverGround,
            double speedOverGround, double rot, String nav, int sourceMmsi) {
        lastUpdate = System.currentTimeMillis();
        lastReportMillis = lastUpdate;
        if (isValidPosition(lat, lon)) {
            loc.setLatitudeDegs(lat);
            loc.setLongitudeDegs(lon);
        }
        if (isValidAngle(trueHeading))
            hdg = trueHeading;
        if (isValidAngle(courseOverGround))
            cog = courseOverGround;
        if (isValidSpeed(speedOverGround))
            sog = speedOverGround;
        rateOfTurn = rot;
        navStatus = nav;
        if (label == null)
            label = "" + sourceMmsi;
    }
    
    public void update(Message05 m) {
        lastUpdate = System.currentTimeMillis();
        if (m.getVesselName() != null && !m.getVesselName().trim().isEmpty()) {
            label = m.getVesselName().trim();
        }
        additionalProperties = m;
    }

    
    public long ageMillis() {
        return System.currentTimeMillis() - lastUpdate;
    }

    /**
     * @return the mmsi
     */
    public int getMmsi() {
        return mmsi;
    }

    /**
     * @return the sog
     */
    public double getSog() {
        return sog;
    }

    /**
     * @return the hdg
     */
    public double getHdg() {
        return hdg;
    }
    
    /**
     * @return the cog
     */
    public double getCog() {
        return cog;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label == null ? null : label.trim();
    }

    /**
     * @return the rateOfTurn
     */
    public double getRateOfTurn() {
        return rateOfTurn;
    }
    
    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @return epoch ms of the most recent position report applied to this contact, 0 if none yet
     */
    public long getLastReportMillis() {
        return lastReportMillis;
    }

    public void setLastReportMillis(long lastReportMillis) {
        this.lastReportMillis = lastReportMillis;
    }


    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @param sog the sog to set
     */
    public void setSog(double sog) {
        this.sog = sog;
    }

    /**
     * @param cog the cog to set
     */
    public void setCog(double cog) {
        this.cog = cog;
    }

    /**
     * @param hdg the hdg to set
     */
    public void setHdg(double hdg) {
        this.hdg = hdg;
    }
    
    public void setNavStatus(String navStatus) {
        this.navStatus = navStatus;
    }

    /**
     * @param rateOfTurn the rateOfTurn to set
     */
    public void setRateOfTurn(double rateOfTurn) {
        this.rateOfTurn = rateOfTurn;
    }
    
    /**
     * @param loc the loc to set
     */
    public void setLocation(LocationType loc) {
        this.loc = loc;
    }

    /**
     * @return the navStatus
     */
    public String getNavStatus() {
        return navStatus;
    }
    
    /**
     * @return the additionalProperties
     */
    public Message05 getAdditionalProperties() {
        return additionalProperties;
    }
}
