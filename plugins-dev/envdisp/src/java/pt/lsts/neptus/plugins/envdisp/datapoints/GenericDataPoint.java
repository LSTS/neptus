/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 21, 2018
 */
package pt.lsts.neptus.plugins.envdisp.datapoints;

import java.util.ArrayList;
import java.util.Date;

import pt.lsts.neptus.NeptusLog;
import scala.collection.mutable.StringBuilder;

/**
 * @author pdias
 *
 */
public class GenericDataPoint extends BaseDataPoint<GenericDataPoint> {

    public enum Type {
        UNKNOWN,
        GEO_TRAJECTORY,
        GEO_2D
    }
    
    private double value = 0;
    private double depth = Double.NaN;

    private int[] indexesXY = null;
    private double gradientValue = Double.NaN;
    
    private Info info = new Info();
    
    /**
     * @param lat
     * @param lon
     */
    public GenericDataPoint(double lat, double lon) {
        super(lat, lon);
    }

    public static String getId(GenericDataPoint hfrdp) {
        return hfrdp.getId();
    }

    @Override
    public String getId() {
        return super.getId() + (Double.isFinite(depth) ?  ":" + depth : "");
    }

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }
    
    /**
     * @param value the value to set
     */
    public void setValue(double value) {
        this.value = value;
    }
    
    /**
     * @return the depth
     */
    public double getDepth() {
        return depth;
    }
    
    /**
     * @return the type
     */
    public Type getType() {
        return info.type;
    }
    
    /**
     * @return the indexesXY
     */
    public int[] getIndexesXY() {
        return indexesXY;
    }
    
    /**
     * @param indexesXY the indexesXY to set
     */
    public void setIndexesXY(int[] indexesXY) {
        this.indexesXY = indexesXY;
    }
    
    /**
     * @return the gradientValue
     */
    public double getGradientValue() {
        return gradientValue;
    }
    
    /**
     * @param gradientValue the gradientValue to set
     */
    public void setGradientValue(double gradientValue) {
        this.gradientValue = gradientValue;
    }
    
    /**
     * @param depth the depth to set
     */
    public void setDepth(double depth) {
        this.depth = depth;
    }
    
    /**
     * @return
     */
    public boolean hasDepth() {
        return Double.isFinite(depth);
    }
    
    /**
     * @return the info
     */
    public Info getInfo() {
        return info;
    }
    
    /**
     * @param info the info to set
     */
    public void setInfo(Info info) {
        this.info = info;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.envdisp.BaseDataPoint#getACopyWithoutHistory()
     */
    @Override
    public GenericDataPoint getACopyWithoutHistory() {
        GenericDataPoint copy = new GenericDataPoint(lat, lon);
        copy.setDateUTC(getDateUTC());
        copy.setValue(getValue());
        copy.setDepth(getDepth());
        copy.setInfo(getInfo());
        copy.setIndexesXY(getIndexesXY());
        copy.setGradientValue(getGradientValue());
        return copy;
    }
    
    public GenericDataPoint copyToWithoutHistory(GenericDataPoint copy) {
        // no Lat/Lon copy
        copy.setDateUTC(getDateUTC());
        copy.setValue(getValue());
        copy.setDepth(getDepth());
        copy.setInfo(getInfo());
        copy.setIndexesXY(getIndexesXY());
        copy.setGradientValue(getGradientValue());
        return copy;
    }

    @Override
    public String toString() {
        return super.toString()
                + "\tvalue:\t" + value
                + "\tdepth:\t" + (hasDepth() ? depth : "no_depth"
                + "\tinfo:\t" + info);
    }

    public boolean useMostRecent(Date currentDate) {
        if (historicalData.size() == 0)
            return false;
        Date mostRecentDate = null;
        double mRecentValue = 0;
        double mRecentGradient = 0;
        int size = historicalData.size();
        for (GenericDataPoint dp : historicalData) {
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
            mRecentValue = dp.value;
            mRecentGradient = dp.gradientValue;
        }
        
        if (size < 1) {
            setValue(Double.NaN);
            setDateUTC(new Date(0));
            setGradientValue(Double.NaN);
            return false;
        }
        
        setValue(mRecentValue);
        setDateUTC(mostRecentDate);
        setGradientValue(mRecentGradient);
        
        return true;
    }

    @Override
    public ArrayList<Object> getAllDataValues() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(value);
        return ret;
    }
    
    @Override
    public boolean setAllDataValues(ArrayList<Object> newValues) {
        try {
            value = (double) newValues.get(0);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
            return false;
        }
        return true;
    }
    
    public static class Info {
        public enum ScalarOrLogPreference {
            SCALAR,
            LOG10
        }
        
        public String name;
        public String fullName;
        public String fileName = "";
        public String unit;
        public String standardName = "";
        public String comment = "";
        
        public double minVal = Double.MIN_VALUE;
        public double maxVal = Double.MAX_VALUE;

        public Date minDate = new Date(0);
        public Date maxDate = new Date(0);
        
        public double minDepth = Double.NaN;
        public double maxDepth = Double.NaN;
        
        public boolean validGradientData = false;
        public double minGradient = Double.MIN_VALUE;
        public double maxGradient = Double.MAX_VALUE;
        
        public ScalarOrLogPreference scalarOrLogPreference = ScalarOrLogPreference.SCALAR;

        public Type type = Type.UNKNOWN;
        public int[] sizeXY = null;

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Info [");
            sb.append("name:'");
            sb.append(name);
            sb.append("'; ");
            sb.append("full_name:'");
            sb.append(fullName);
            sb.append("'; ");
            sb.append("standard_name:'");
            sb.append(standardName);
            sb.append("'; ");
            sb.append("unit:'");
            sb.append(unit);
            sb.append("'; ");
            sb.append("val_range:'");
            sb.append("[").append(minVal).append("; ").append(maxVal).append("]");
            sb.append("'; ");
            sb.append("date_range:'");
            sb.append("[").append(minDate).append("; ").append(maxDate).append("]");
            sb.append("'; ");
            sb.append("depth_range:'");
            sb.append("[").append(minDepth).append("; ").append(maxDepth).append("]");
            sb.append("'; ");
            sb.append("comment:'");
            sb.append(comment);
            sb.append("'; ");
            sb.append("]");
            return sb.toString();
        }
    }
}
