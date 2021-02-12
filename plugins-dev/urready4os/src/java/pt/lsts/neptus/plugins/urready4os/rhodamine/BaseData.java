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
 * Sep 24, 2014
 */
package pt.lsts.neptus.plugins.urready4os.rhodamine;


/**
 * @author pdias
 *
 */
public class BaseData  implements Comparable<BaseData> {
    protected String sourceSystem = "";
    
    protected double lat;
    protected double lon;
    protected long timeMillis;
    
    protected double depth;
    protected double depthLower = Double.NaN;

    protected double rhodamineDyePPB = Double.NaN;
    protected double crudeOilPPB = Double.NaN;
    protected double refineOilPPB = Double.NaN;
    protected double temperature = Double.NaN;
    
    public BaseData(double lat, double lon, double depth, long timeMillis) {
        this.lat = lat;
        this.lon = lon;
        this.depth = depth;
        this.timeMillis = timeMillis;
    }
    
    /**
     * @return the sourceSystem
     */
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    /**
     * @param sourceSystem the sourceSystem to set
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
    
    /**
     * @return the lat
     */
    public double getLat() {
        return lat;
    }
    
    /**
     * @return the lon
     */
    public double getLon() {
        return lon;
    }
    
    /**
     * @return the depth
     */
    public double getDepth() {
        return depth;
    }
    
    /**
     * @return the depthLower
     */
    public double getDepthLower() {
        return depthLower;
    }
    
    /**
     * @param depthLower the depthLower to set
     */
    public void setDepthLower(double depthLower) {
        this.depthLower = depthLower;
    }
    
    /**
     * @return the timeMillis
     */
    public long getTimeMillis() {
        return timeMillis;
    }
    
    /**
     * @return the rhodamineDyePPB
     */
    public double getRhodamineDyePPB() {
        return rhodamineDyePPB;
    }
    
    /**
     * @param rhodamine the rhodamineDyePPB to set
     */
    public BaseData setRhodamineDyePPB(double rhodamine) {
        this.rhodamineDyePPB = rhodamine;
        return this;
    }
    
    /**
     * @return the crudeOilPPB
     */
    public double getCrudeOilPPB() {
        return crudeOilPPB;
    }
    
    /**
     * @param crudeOilPPB the crudeOilPPB to set
     * @return 
     */
    public BaseData setCrudeOilPPB(double crudeOilPPB) {
        this.crudeOilPPB = crudeOilPPB;
        return this;
    }

    /**
     * @return the refineOilPPB
     */
    public double getRefineOilPPB() {
        return refineOilPPB;
    }
    
    /**
     * @param refineOilPPB the refineOilPPB to set
     * @return 
     */
    public BaseData setRefineOilPPB(double refineOilPPB) {
        this.refineOilPPB = refineOilPPB;
        return this;
    }

    /**
     * @return the temperature
     */
    public double getTemperature() {
        return temperature;
    }
    
    /**
     * @param temperature the temperature to set
     * @return 
     */
    public BaseData setTemperature(double temperature) {
        this.temperature = temperature;
        return this;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(BaseData o) {
        if (o != null && Double.compare(lat, o.lat) == 0 && Double.compare(lon, o.lon) == 0
                &&  Double.compare(timeMillis, o.timeMillis) == 0
                &&  Double.compare(depth, o.depth) == 0)
            return 0;
        else
            return 1; 
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (!(obj instanceof BaseData))
            return false;
        return compareTo((BaseData) obj) == 0 ? true : false;
    }
    
    public static String getId(BaseData hfrdp) {
        return hfrdp.lat + ":" + hfrdp.lon;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return timeMillis + ", "
                + lat + ", "
                + lon + ", "
                + depth + ", "
                + rhodamineDyePPB + ", "
                + crudeOilPPB + ", "
                + refineOilPPB + ", "
                + temperature;
    }
}
