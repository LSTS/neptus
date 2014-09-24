/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Sep 24, 2014
 */
package pt.lsts.neptus.plugins.urready4os.rhodamine;


/**
 * @author pdias
 *
 */
public class BaseData  implements Comparable<BaseData> {
    protected double lat;
    protected double lon;
    protected long dateUTC;
    
    protected double depth;

    protected float rhodamineDyePPB = Float.NaN;
    protected float crudeOilPPB = Float.NaN;
    protected float refineOilPPB = Float.NaN;
    
    public BaseData(double lat, double lon, double depth, long dateUTC) {
        this.lat = lat;
        this.lon = lon;
        this.depth = depth;
        this.dateUTC = dateUTC;
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
     * @return the dateUTC
     */
    public long getDateUTC() {
        return dateUTC;
    }
    
    /**
     * @return the rhodamineDyePPB
     */
    public float getRhodamineDyePPB() {
        return rhodamineDyePPB;
    }
    
    /**
     * @param rhodamineDyePPB the rhodamineDyePPB to set
     */
    public BaseData setRhodamineDyePPB(float rhodamineDyePPB) {
        this.rhodamineDyePPB = rhodamineDyePPB;
        return this;
    }
    
    /**
     * @return the crudeOilPPB
     */
    public float getCrudeOilPPB() {
        return crudeOilPPB;
    }
    
    /**
     * @param crudeOilPPB the crudeOilPPB to set
     * @return 
     */
    public BaseData setCrudeOilPPB(float crudeOilPPB) {
        this.crudeOilPPB = crudeOilPPB;
        return this;
    }
    

    /**
     * @return the refineOilPPB
     */
    public float getRefineOilPPB() {
        return refineOilPPB;
    }
    
    /**
     * @param refineOilPPB the refineOilPPB to set
     * @return 
     */
    public BaseData setRefineOilPPB(float refineOilPPB) {
        this.refineOilPPB = refineOilPPB;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(BaseData o) {
        if (Double.compare(lat, o.lat) == 0 && Double.compare(lon, o.lon) == 0)
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

}

