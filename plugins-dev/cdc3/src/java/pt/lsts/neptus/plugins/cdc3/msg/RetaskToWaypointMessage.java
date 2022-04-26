/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 21, 2019
 */
package pt.lsts.neptus.plugins.cdc3.msg;

/**
 * @author pdias
 *
 */
public class RetaskToWaypointMessage extends Cdc3Message {
    private float latitudeRads = 0;
    private float longitudeRads = 0;
    private float speedMps = 0;
    
    /**
     * @return the latitudeRads
     */
    public float getLatitudeRads() {
        return latitudeRads;
    }
    
    /**
     * @param latitudeRads the latitudeRads to set
     */
    public void setLatitudeRads(float latitudeRads) {
        this.latitudeRads = latitudeRads;
    }

    /**
     * @return the latitudeDegs
     */
    public float getLatitudeDegs() {
        return (float) Math.toDegrees(latitudeRads);
    }
    
    /**
     * @param latitudeRads the latitudeDeg to set
     */
    public void setLatitudeDegs(float latitudeDeg) {
        this.latitudeRads = (float) Math.toRadians(latitudeDeg);
    }

    /**
     * @return the longitudeRads
     */
    public float getLongitudeRads() {
        return longitudeRads;
    }
    
    /**
     * @param longitudeRads the longitudeRads to set
     */
    public void setLongitudeRads(float longitudeRads) {
        this.longitudeRads = longitudeRads;
    }

    /**
     * @return the longitudeDegs
     */
    public float getLongitudeDegs() {
        return (float) Math.toDegrees(longitudeRads);
    }
    
    /**
     * @param longitudeRads the longitudeDeg to set
     */
    public void setLongitudeDegs(float longitudeDeg) {
        this.longitudeRads = (float) Math.toRadians(longitudeDeg);
    }

    /**
     * @return the speedMps
     */
    public float getSpeedMps() {
        return speedMps;
    }

    /**
     * @param speedMps the speedMps to set
     */
    public void setSpeedMps(float speedMps) {
        this.speedMps = speedMps;
    }

    @Override
    public String toStringFields() {
        StringBuilder sb = new StringBuilder();
        sb.append("  \"latitudeDegs\" : ");
        sb.append(getLatitudeDegs());
        sb.append(",\n");
        sb.append("  \"latitude\" : ");
        sb.append(getLatitudeRads());
        sb.append(",\n");
        sb.append("  \"longitudeDegs\" : ");
        sb.append(getLongitudeDegs());
        sb.append(",\n");
        sb.append("  \"longitude\" : ");
        sb.append(getLongitudeRads());
        sb.append(",\n");
        sb.append("  \"speedMps\" : ");
        sb.append(getSpeedMps());
        sb.append(",\n");
        return sb.toString();
    }

}
