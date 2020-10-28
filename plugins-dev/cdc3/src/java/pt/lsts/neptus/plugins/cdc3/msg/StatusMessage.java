/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
public class StatusMessage extends Cdc3Message {
    private float latitudeRads = 0;
    private float longitudeRads = 0;
    private int depth = 0;
    private float altitude = 0;
    private float yawRad = 0;
    private int progress = 0;
    private int fuelLevel = 0;
    private int fuelConfidence = 0;
    
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
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }
    
    /**
     * @param depth the depth to set
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    /**
     * @return the altitude
     */
    public float getAltitude() {
        return altitude;
    }
    
    /**
     * @param altitude the altitude to set
     */
    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }
    
    /**
     * @return the yawRad
     */
    public float getYawRad() {
        return yawRad;
    }
    
    /**
     * @param yawRad the yawRad to set
     */
    public void setYawRad(float yawRad) {
        this.yawRad = yawRad;
    }

    /**
     * @return the yawDeg
     */
    public float getYawDeg() {
        return (float) Math.toDegrees(yawRad);
    }
    
    /**
     * @param yawDeg the yawDeg to set
     */
    public void setYawDeg(float yawDeg) {
        this.yawRad = (float) Math.toRadians(yawDeg);
    }

    /**
     * @return the progress
     */
    public int getProgress() {
        return progress;
    }
    
    /**
     * @param progress the progress to set
     */
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    /**
     * @return the fuelLevel
     */
    public int getFuelLevel() {
        return fuelLevel;
    }
    
    /**
     * @param fuelLevel the fuelLevel to set
     */
    public void setFuelLevel(int fuelLevel) {
        this.fuelLevel = fuelLevel;
    }
    
    /**
     * @return the fuelConfidence
     */
    public int getFuelConfidence() {
        return fuelConfidence;
    }
    
    /**
     * @param fuelConfidence the fuelConfidence to set
     */
    public void setFuelConfidence(int fuelConfidence) {
        this.fuelConfidence = fuelConfidence;
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
        sb.append("  \"depth\" : ");
        sb.append(depth);
        sb.append(",\n");
        sb.append("  \"yawDegs\" : ");
        sb.append(getYawDeg());
        sb.append(",\n");
        sb.append("  \"yaw\" : ");
        sb.append(getYawRad());
        sb.append(",\n");
        sb.append("  \"altitude\" : ");
        sb.append(altitude);
        sb.append(",\n");
        sb.append("  \"progress\" : ");
        sb.append(progress);
        sb.append(",\n");
        sb.append("  \"fuelLevel\" : ");
        sb.append(fuelLevel);
        sb.append(",\n");
        sb.append("  \"fuelConfidence\" : ");
        sb.append(fuelConfidence);
        sb.append(",\n");   
        return sb.toString();
    }
}
