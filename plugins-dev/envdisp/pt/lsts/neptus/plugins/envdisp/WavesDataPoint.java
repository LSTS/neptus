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
 * 6 de Jul de 2013
 */
package pt.lsts.neptus.plugins.envdisp;

import java.util.Date;

/**
 * @author pdias
 *
 */
public class WavesDataPoint extends BaseDataPoint<WavesDataPoint> {

    // hs
    private double significantHeight = 0;
    // tp
    private double peakPeriod = 0;
    // pdir
    private double peakDirection = 0;
    
    /**
     * @param lat
     * @param lon
     */
    public WavesDataPoint(double lat, double lon) {
        super(lat, lon);
    }

    public double getSignificantHeight() {
        return significantHeight;
    }
    
    public void setSignificantHeight(double significantHeight) {
        this.significantHeight = significantHeight;
    }

    /**
     * @return the peakPeriod
     */
    public double getPeakPeriod() {
        return peakPeriod;
    }
    
    /**
     * @param peakPeriod the peakPeriod to set
     */
    public void setPeakPeriod(double peakPeriod) {
        this.peakPeriod = peakPeriod;
    }
    
    /**
     * @return the peakDirection
     */
    public double getPeakDirection() {
        return peakDirection;
    }
    
    /**
     * @param peakDirection the peakDirection to set
     */
    public void setPeakDirection(double peakDirection) {
        this.peakDirection = peakDirection;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.envdisp.BaseDataPoint#getACopyWithoutHistory()
     */
    @Override
    public WavesDataPoint getACopyWithoutHistory() {
        WavesDataPoint copy = new WavesDataPoint(getLat(), getLon());
        copy.setSignificantHeight(significantHeight);
        copy.setPeakPeriod(peakPeriod);
        copy.setPeakDirection(peakDirection);
        return copy;
    }
    
    public WavesDataPoint copyToWithoutHistory(WavesDataPoint copy) {
        // no Lat/Lon copy
        
        copy.setDateUTC(getDateUTC());
        
        copy.setSignificantHeight(significantHeight);
        copy.setPeakPeriod(peakPeriod);
        copy.setPeakDirection(peakDirection);
        return copy;
    }

    @Override
    public String toString() {
        return super.toString() +
                "\tsignificantHeight:\t" + significantHeight +
                "\tpeakPeriod:\t" + peakPeriod +
                "\tpeakDirection:\t" + peakDirection;
    }

    public boolean useMostRecent(Date currentDate) {
        if (historicalData.size() == 0)
            return false;
        Date mostRecentDate = null;
        double mRecentSh = 0;
        double mRecentPp = 0;
        double mRecentPd = 0;
        int size = historicalData.size();
        for (WavesDataPoint dp : historicalData) {
            if (currentDate.before(dp.dateUTC)) {
                size--;
                continue;
            }
            if (!dp.dateUTC.after(currentDate) && (mostRecentDate == null || dp.dateUTC.after(mostRecentDate))) {
                mostRecentDate = dp.dateUTC;
            }
            else {
                size--;
                continue;
            }
            mRecentSh = dp.significantHeight;
            mRecentPp = dp.peakPeriod;
            mRecentPd = dp.peakDirection;
        }
        
        if (size < 1) {
            setSignificantHeight(Double.NaN);
            setPeakPeriod(Double.NaN);
            setPeakDirection(Double.NaN);
            setDateUTC(new Date(0));
            return false;
        }
        
        setSignificantHeight(mRecentSh);
        setPeakPeriod(mRecentPp);
        setPeakDirection(mRecentPd);
        setDateUTC(mostRecentDate);
        
        return true;
    }

}
