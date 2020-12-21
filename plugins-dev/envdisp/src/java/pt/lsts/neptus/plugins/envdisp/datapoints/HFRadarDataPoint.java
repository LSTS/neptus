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
 * Jun 23, 2013
 */
package pt.lsts.neptus.plugins.envdisp.datapoints;

import java.util.ArrayList;
import java.util.Date;

import pt.lsts.neptus.NeptusLog;

public class HFRadarDataPoint extends BaseDataPoint<HFRadarDataPoint> {

    private double speedCmS;
    private double headingDegrees;
    private double resolutionKm = -1;
    private String info = "";
    
//    private ArrayList<HFRadarDataPoint> historicalData = new ArrayList<>();
    
    public HFRadarDataPoint(double lat, double lon) {
        super(lat, lon);
    }

    public HFRadarDataPoint getACopyWithoutHistory() {
        HFRadarDataPoint copy = new HFRadarDataPoint(getLat(), getLon());
        copy.setSpeedCmS(getSpeedCmS());
        copy.setHeadingDegrees(getHeadingDegrees());
        copy.setDateUTC(getDateUTC());
        copy.setResolutionKm(getResolutionKm());
        copy.setInfo(getInfo());
        return copy;
    }

    public HFRadarDataPoint copyToWithoutHistory(HFRadarDataPoint copy) {
        // no Lat/Lon copy
        
        copy.setDateUTC(getDateUTC());
        
        copy.setSpeedCmS(getSpeedCmS());
        copy.setHeadingDegrees(getHeadingDegrees());
        copy.setDateUTC(getDateUTC());
        copy.setResolutionKm(getResolutionKm());
        copy.setInfo(getInfo());
        return copy;
    }

    /**
     * @return the speedCmS
     */
    public double getSpeedCmS() {
        return speedCmS;
    }

    /**
     * @param speedCmS the speedCmS to set
     */
    public void setSpeedCmS(double speedCmS) {
        this.speedCmS = speedCmS;
    }

    /**
     * @return the headingDegrees
     */
    public double getHeadingDegrees() {
        return headingDegrees;
    }

    /**
     * @param headingDegrees the headingDegrees to set
     */
    public void setHeadingDegrees(double headingDegrees) {
        this.headingDegrees = headingDegrees;
    }

    /**
     * @return the resolutionKm
     */
    public double getResolutionKm() {
        return resolutionKm;
    }

    /**
     * @param resolutionKm the resolutionKm to set
     */
    public void setResolutionKm(double resolutionKm) {
        this.resolutionKm = resolutionKm;
    }

    /**
     * @return the info
     */
    public String getInfo() {
        return info;
    }

    /**
     * @param info the info to set
     */
    public void setInfo(String info) {
        this.info = info;
    }


    @Override
    public String toString() {
        return super.toString() +
        		"\tSpeed:\t" + speedCmS + 
        		"\tHeading:\t" + headingDegrees + 
        		"\tResolution:\t" + resolutionKm +
        		"\tInfo:\t" + info;
    }

    /**
     * @return the historicalData
     */
    public ArrayList<HFRadarDataPoint> getHistoricalData() {
        return historicalData;
    }
    
    public boolean calculateMean(Date currentDate) {
        if (historicalData.size() == 0)
            return false;
        Date mostRecentDate = null;
        double meanSpeed = 0;
        double meanHeading = 0;
        int size = historicalData.size();
        for (HFRadarDataPoint dp : historicalData) {
            if (currentDate.before(dp.dateUTC)) {
                size--;
                continue;
            }
            if (mostRecentDate == null || !mostRecentDate.after(dp.dateUTC))
                mostRecentDate = dp.dateUTC;
            meanSpeed += dp.speedCmS;
            meanHeading += dp.headingDegrees;
        }
        
        if (size < 1) {
            setSpeedCmS(Double.NaN);
            setHeadingDegrees(Double.NaN);
            setDateUTC(new Date(0));
            return false;
        }
        
        meanSpeed = meanSpeed / size;
        meanHeading = meanHeading / size;
        
        setSpeedCmS(meanSpeed);
        setHeadingDegrees(meanHeading);
        setDateUTC(mostRecentDate);
        
        return true;
    }

    public boolean useMostRecent(Date currentDate) {
        if (historicalData.size() == 0)
            return false;
        Date mostRecentDate = null;
        double mRecentSpeed = 0;
        double mRecentHeading = 0;
        int size = historicalData.size();
        for (HFRadarDataPoint dp : historicalData) {
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
            mRecentSpeed = dp.speedCmS;
            mRecentHeading = dp.headingDegrees;
        }
        
        if (size < 1) {
            setSpeedCmS(Double.NaN);
            setHeadingDegrees(Double.NaN);
            setDateUTC(new Date(0));
            return false;
        }
        
        setSpeedCmS(mRecentSpeed);
        setHeadingDegrees(mRecentHeading);
        setDateUTC(mostRecentDate);
        
        return true;
    }

    @Override
    public ArrayList<Object> getAllDataValues() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(speedCmS);
        ret.add(headingDegrees);
        ret.add(resolutionKm);
        ret.add(info);
        return ret;
    }
    
    @Override
    public boolean setAllDataValues(ArrayList<Object> newValues) {
        try {
            speedCmS = (double) newValues.get(0);
            headingDegrees = (double) newValues.get(1);
            resolutionKm = (double) newValues.get(2);
            info = (String) newValues.get(3);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
            return false;
        }
        return true;
    }
}