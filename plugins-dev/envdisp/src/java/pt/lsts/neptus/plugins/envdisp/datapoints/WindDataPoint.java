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
public class WindDataPoint extends BaseDataPoint<WindDataPoint> {

    private double u = 0;
    private double v = 0;
    
    /**
     * @param lat
     * @param lon
     */
    public WindDataPoint(double lat, double lon) {
        super(lat, lon);
    }

    public double getU() {
        return u;
    }
    
    public void setU(double u) {
        this.u = u;
    }

    public double getV() {
        return v;
    }
    
    public void setV(double v) {
        this.v = v;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.envdisp.BaseDataPoint#getACopyWithoutHistory()
     */
    @Override
    public WindDataPoint getACopyWithoutHistory() {
        WindDataPoint copy = new WindDataPoint(getLat(), getLon());
        copy.setU(getU());
        copy.setV(getV());
        return copy;
    }
    
    public WindDataPoint copyToWithoutHistory(WindDataPoint copy) {
        // no Lat/Lon copy
        
        copy.setDateUTC(getDateUTC());
        
        copy.setU(getU());
        copy.setV(getV());
        return copy;
    }

    @Override
    public String toString() {
        return super.toString() +
                "\tu:\t" + u +
                "\tv:\t" + v;
    }

    public double getSpeed() {
        double speed = Math.sqrt(u * u +  v * v);
        return speed;
    }

    public double getHeading() {
        double heading = Math.atan2(v, u);
        return Math.toDegrees(heading);
    }

    public boolean useMostRecent(Date currentDate) {
        if (historicalData.size() == 0)
            return false;
        Date mostRecentDate = null;
        double mRecentU = 0;
        double mRecentV = 0;
        int size = historicalData.size();
        for (WindDataPoint dp : historicalData) {
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
            mRecentU = dp.u;
            mRecentV = dp.v;
        }
        
        if (size < 1) {
            setU(Double.NaN);
            setV(Double.NaN);
            setDateUTC(new Date(0));
            return false;
        }
        
        setU(mRecentU);
        setV(mRecentV);
        setDateUTC(mostRecentDate);
        
        return true;
    }

    @Override
    public ArrayList<Object> getAllDataValues() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(u);
        ret.add(v);
        return ret;
    }
    
    @Override
    public boolean setAllDataValues(ArrayList<Object> newValues) {
        try {
            u = (double) newValues.get(0);
            v = (double) newValues.get(1);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
            return false;
        }
        return true;
    }
}
