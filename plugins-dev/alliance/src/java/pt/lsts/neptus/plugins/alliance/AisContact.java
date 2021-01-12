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
    
    private Message05 additionalProperties = null;
    
    public LocationType getLocation() {
        return loc;
    }
    
    public AisContact(int mmsi) {
        System.out.println("Created AIS contact for mmsi "+mmsi);
        this.mmsi = mmsi;
        lastUpdate = System.currentTimeMillis();
    }
    
    public void update(Message01 m) {
        lastUpdate = System.currentTimeMillis();
        loc.setLatitudeDegs(m.getLatitude());
        loc.setLongitudeDegs(m.getLongitude());
        hdg = m.getTrueHeading();
        cog = m.getCourseOverGround();
        sog = m.getSpeedOverGround();
        rateOfTurn = m.getRateOfTurn();
        navStatus = AISUtil.translateNavigationalStatus(m.getNavigationStatus());
        if (label == null)
            label = ""+m.getSourceMmsi();
    }
    
    public void update(Message03 m) {
        lastUpdate = System.currentTimeMillis();
        loc.setLatitudeDegs(m.getLatitude());
        loc.setLongitudeDegs(m.getLongitude());
        hdg = m.getTrueHeading();
        cog = m.getCourseOverGround();
        sog = m.getSpeedOverGround();
        rateOfTurn = m.getRateOfTurn();
        navStatus = AISUtil.translateNavigationalStatus(m.getNavigationStatus());
        if (label == null)
            label = ""+m.getSourceMmsi();
    }
    
    public void update(Message05 m) {
        lastUpdate = System.currentTimeMillis();
        label = m.getVesselName().trim();
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
