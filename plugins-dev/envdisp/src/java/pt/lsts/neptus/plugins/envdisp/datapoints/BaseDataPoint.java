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
 * Jun 23, 2013
 */
package pt.lsts.neptus.plugins.envdisp.datapoints;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@SuppressWarnings("rawtypes")
public class BaseDataPoint<T extends BaseDataPoint> implements Comparable<BaseDataPoint> {
    //lat lon speed (cm/s)    degree  acquired (Date+Time)    resolution (km) origin
    protected double lat;
    protected double lon;
    protected Date dateUTC;

    protected ArrayList<T> historicalData = new ArrayList<>();
    
    public BaseDataPoint(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public BaseDataPoint getACopyWithoutHistory() {
        BaseDataPoint copy = new BaseDataPoint(getLat(), getLon());
        copy.setDateUTC(getDateUTC());
        return copy;
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
    public Date getDateUTC() {
        return dateUTC;
    }

    /**
     * @param dateUTC the dateUTC to set
     */
    public void setDateUTC(Date dateUTC) {
        this.dateUTC = dateUTC;
    }


    @Override
    public String toString() {
        return "Lat:\t" + lat +
        		"\tLon:\t" + lon +
        		"\tDate:\t" + dateUTC;
    }

    /**
     * @return the historicalData
     */
    public ArrayList<T> getHistoricalData() {
        return historicalData;
    }
    

    @SuppressWarnings("unchecked")
    public void purgeAllBefore(Date date) {
        if (date == null || historicalData.size() == 0)
            return;
        for (BaseDataPoint<T> dp : historicalData.toArray(new BaseDataPoint[0])) {
            if (dp.getDateUTC().before(date))
                historicalData.remove(dp);
        }
    }

    @Override
    public int compareTo(BaseDataPoint o) {
        if (Double.compare(lat, o.lat) == 0 && Double.compare(lon, o.lon) == 0)
            return 0;
        else
            return 1; 
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (!(obj instanceof BaseDataPoint))
            return false;
        return compareTo((BaseDataPoint) obj) == 0 ? true : false;
    }
    
    /**
     * @param hfrdp
     * @return
     */
    public static String getId(BaseDataPoint hfrdp) {
        return hfrdp.lat + ":" + hfrdp.lon;
    }

    /**
     * @return
     */
    public String getId() {
        return lat + ":" + lon;
    }

    /**
     * This is supposed to get all data as an {@link ArrayList} in order to bulk processing.
     * 
     * @return
     */
    public ArrayList<Object> getAllDataValues() {
        return new ArrayList<>();
    }
    
    /**
     * This is supposed to set all data as an {@link ArrayList} in order to bulk processing.
     * 
     * @param newValues
     */
    public boolean setAllDataValues(ArrayList<Object> newValues) {
        return true;
    }

    /**
     * To clean data points that are older than a date limit. Works directly on the dataPoints parameter.
     * 
     * @param dataPoints
     * @param dateLimit
     */
    public static <Bp extends BaseDataPoint<?>> void cleanDataPointsBeforeDate(HashMap<String, Bp> dataPoints,
            Date dateLimit) {
        if (dateLimit == null)
            return;
        
        for (String dpID : dataPoints.keySet().toArray(new String[0])) {
            Bp dp = dataPoints.get(dpID);
            if (dp == null)
                continue;
            
            if (dp.getDateUTC().before(dateLimit))
                dataPoints.remove(dpID);
            else {
                // Cleanup historicalData
                dp.purgeAllBefore(dateLimit);
            }
        }
    }
}