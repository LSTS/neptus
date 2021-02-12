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
 * 6 de Jul de 2013
 */
package pt.lsts.neptus.plugins.envdisp.datapoints;

import java.util.ArrayList;
import java.util.Date;

import pt.lsts.neptus.NeptusLog;

/**
 * @author pdias
 *
 */
public class SSTDataPoint extends BaseDataPoint<SSTDataPoint> {

    private double sst = 0;
    
    /**
     * @param lat
     * @param lon
     */
    public SSTDataPoint(double lat, double lon) {
        super(lat, lon);
    }

    /**
     * @return the sst
     */
    public double getSst() {
        return sst;
    }
    
    /**
     * @param sst the sst to set
     */
    public void setSst(double sst) {
        this.sst = sst;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.envdisp.BaseDataPoint#getACopyWithoutHistory()
     */
    @Override
    public SSTDataPoint getACopyWithoutHistory() {
        SSTDataPoint copy = new SSTDataPoint(getLat(), getLon());
        copy.setSst(getSst());
        return copy;
    }
    
    public SSTDataPoint copyToWithoutHistory(SSTDataPoint copy) {
        // no Lat/Lon copy
        
        copy.setDateUTC(getDateUTC());
        
        copy.setSst(getSst());
        return copy;
    }

    @Override
    public String toString() {
        return super.toString() +
                "\tSST:\t" + sst;
    }

    public boolean useMostRecent(Date currentDate) {
        if (historicalData.size() == 0)
            return false;
        Date mostRecentDate = null;
        double mRecentSST = 0;
        int size = historicalData.size();
        for (SSTDataPoint dp : historicalData) {
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
            mRecentSST = dp.sst;
        }
        
        if (size < 1) {
            setSst(Double.NaN);
            setDateUTC(new Date(0));
            return false;
        }
        
        setSst(mRecentSST);
        setDateUTC(mostRecentDate);
        
        return true;
    }

    @Override
    public ArrayList<Object> getAllDataValues() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(sst);
        return ret;
    }
    
    @Override
    public boolean setAllDataValues(ArrayList<Object> newValues) {
        try {
            sst = (double) newValues.get(0);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
            return false;
        }
        return true;
    }
}
