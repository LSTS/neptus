/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * 23/11/2017
 */
package pt.lsts.neptus.plugins.envdisp.util;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
public class NetCDFUtils {

    public static final String NETCDF_ATT_STANDARD_NAME = "standard_name";
    public static final String NETCDF_ATT_FILL_VALUE = "_FillValue";
    public static final String NETCDF_ATT_MISSING_VALUE = "missing_value";
    public static final String NETCDF_ATT_VALID_RANGE = "valid_range";
    public static final String NETCDF_ATT_VALID_MIN = "valid_min";
    public static final String NETCDF_ATT_VALID_MAX = "valid_max";
    public static final String NETCDF_ATT_UNITS = "units";

    /**
     * Counts the evolution of a multi-for loop variables counters.
     * Counters go from most external loop to the most inner one.
     * 
     * @param shape The max values of the each loop counter
     * @param counter The state of the counters.
     * @return The next stage of the for loops or null if reach the end for all loops.
     */
    public static int[] advanceLoopCounter(final int[] shape, int[] counter) {
        for (int i : shape) {
            if (i < 1)
                return null;
        }
        for (int i = counter.length - 1; i >= 0; i--) {
            if (i >= counter.length - 1)
                counter[i]++;
            else {
                if (counter[i + 1] >= shape[i + 1]) {
                    counter[i]++;
                    counter[i + 1] = 0;
                }
            }
            if (i == 0 && counter[i] >= shape[i]) {
                return null;
            }
        }
        return counter;
    }

    /**
     * @param timeArray
     * @param timeIdx
     * @param timeMultiplier
     * @param timeOffset
     * @param fromDate
     * @param toDate
     * @param ignoreDateLimitToLoad
     * @param dateLimit
     * @return Returns the dates from the data array adjusting the fromDate and toDate restricted or not to the dateLimit
     */
    public static Date[] getTimeValues(Array timeArray, int timeIdx, double timeMultiplier, double timeOffset,
            Date fromDate, Date toDate, boolean ignoreDateLimitToLoad, Date dateLimit) {
        double timeVal = timeArray.getDouble(timeIdx); // get(timeIdx);
        Date dateValue = new Date((long) (timeVal * timeMultiplier + timeOffset));
        
        if (!ignoreDateLimitToLoad && dateValue.before(dateLimit))
            return null;
        
        if (fromDate == null) {
            fromDate = dateValue;
        }
        else {
            if (dateValue.before(fromDate))
                fromDate = dateValue;
        }
        if (toDate == null) {
            toDate = dateValue;
        }
        else {
            if (dateValue.after(toDate))
                toDate = dateValue;
        }
        
        return new Date[] {dateValue, fromDate, toDate};
    }

    /**
     * Tries to find the fill value for a variable.
     * 
     * @param var
     * @return The fill value using the {@link #NETCDF_ATT_FILL_VALUE} or {@link #NETCDF_ATT_MISSING_VALUE}
     * in this order
     * @throws NumberFormatException
     */
    public static double findFillValue(Variable var) throws NumberFormatException {
        Attribute fillValueAtt = null;
        if (var != null) {
            fillValueAtt = var.findAttribute(NETCDF_ATT_FILL_VALUE);
            if (fillValueAtt == null)
                fillValueAtt = var.findAttribute(NETCDF_ATT_MISSING_VALUE);
        }
        if (fillValueAtt != null) {
            try {
                return ((Number) fillValueAtt.getValue(0)).doubleValue();
            }
            catch (ClassCastException e) {
                return Double.parseDouble((String) fillValueAtt.getValue(0));
            }
        }
        return Double.NaN;
    }

    /**
     * Checks if a value is valid.
     * 
     * @param value
     * @param fillValue
     * @param validRange
     * @return
     */
    public static boolean isValueValid(double value, double fillValue,Pair<Double, Double> validRange) {
        if (!Double.isNaN(value) && value != fillValue) {
            if (validRange != null && !Double.isNaN(validRange.first()) && !Double.isNaN(validRange.second())) {
                if (value >= validRange.first() && value <= validRange.second())
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Check and return the {@link #NETCDF_ATT_VALID_RANGE} interval, or used the {@link #NETCDF_ATT_VALID_MIN}
     * and {@link #NETCDF_ATT_VALID_MAX}.
     * 
     * @param var
     * @return The interval or null if none.
     * @throws NumberFormatException
     */
    public static Pair<Double, Double> findValidRange(Variable var) throws NumberFormatException {
        Attribute validRangeValueAtt = var == null ? null : var.findAttribute(NETCDF_ATT_VALID_RANGE);
        Attribute validMinValueAtt = var == null ? null : var.findAttribute(NETCDF_ATT_VALID_MIN);
        Attribute validMaxValueAtt = var == null ? null : var.findAttribute(NETCDF_ATT_VALID_MAX);

        if (validRangeValueAtt != null || (validMinValueAtt != null || validMaxValueAtt != null)) {
            try {
                double minRange = 0;
                double maxRange = 0;
                if (validRangeValueAtt != null) {
                    minRange = ((Number) validRangeValueAtt.getValue(0)).doubleValue();
                    maxRange = ((Number) validRangeValueAtt.getValue(1)).doubleValue();
                }
                else if (validMinValueAtt != null && validMaxValueAtt != null) {
                    minRange = validMinValueAtt != null ? ((Number) validMinValueAtt.getValue(0)).doubleValue()
                            : Double.MIN_VALUE;
                    maxRange = validMaxValueAtt != null ? ((Number) validMaxValueAtt.getValue(0)).doubleValue()
                            : Double.MAX_VALUE;
                }
                else {
                    throw new NumberFormatException("One or both range values are not finite");
                }
                if (!Double.isFinite(minRange) || !Double.isFinite(maxRange)) {
                    throw new NumberFormatException(
                            String.format("One or both range values are not finite (%d | %d)", minRange, maxRange));
                }
                return new Pair<Double, Double>(minRange, maxRange);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @param dataFile
     * @param fileNameForErrorString
     * @param failIfNotFound
     * @param varStName
     * @return
     */
    public static Pair<String, Variable> findVariableForStandardNameOrName(NetcdfFile dataFile, String fileNameForErrorString,
            boolean failIfNotFound, String... varStName) {
        Pair<String, Variable> ret = findVariableForStandardName(dataFile, fileNameForErrorString, false, varStName);
        if (ret == null)
            ret = findVariableFor(dataFile, fileNameForErrorString, failIfNotFound, varStName);
        
        return ret;
    }

    /**
     * @param dataFile
     * @param fileNameForErrorString
     * @param failIfNotFound
     * @param varName
     * @return
     */
    public static Pair<String, Variable> findVariableFor(NetcdfFile dataFile, String fileNameForErrorString,
            boolean failIfNotFound, String... varName) {
        String name = "";
        Variable latVar = null;
        for (String st : varName) {
            latVar = dataFile.findVariable(st);
            if (latVar != null) {
                name = st;
                break;
            }
        }
        if (latVar == null) {
            String message = "Can't find variable '" + Arrays.toString(varName) + "' for netCDF file '"
                    + fileNameForErrorString + "'.";
            if (failIfNotFound) {
                new Exception("Aborting. " + message);
            }
            else {
                NeptusLog.pub().error(message);
                return null;
            }
        }
        return new Pair<String, Variable>(name, latVar);
    }

    public static Pair<String, Variable> findVariableForStandardName(NetcdfFile dataFile, String fileNameForErrorString,
            boolean failIfNotFound, String... varName) {
        String name = "";
        Variable latVar = null;
        for (String st : varName) {
            latVar = dataFile.findVariableByAttribute(null, NETCDF_ATT_STANDARD_NAME, st);
            if (latVar != null) {
                name = latVar.getShortName();
                break;
            }
        }
        if (latVar == null) {
            String message = "Can't find variable standard name '" + Arrays.toString(varName) + "' for netCDF file '"
                    + fileNameForErrorString + "'.";
            if (failIfNotFound) {
                new Exception("Aborting. " + message);
            }
            else {
                NeptusLog.pub().error(message);
                return null;
            }
        }
        return new Pair<String, Variable>(name, latVar);
    }

    /**
     * @param dimStr
     * @param name
     * @return
     */
    public static Map<String, Integer> getIndexesForVar(String dimStr, String... name) {
        HashMap<String, Integer> ret = new HashMap<>();
        if (dimStr == null || dimStr.length() == 0)
            return ret;
        
        Arrays.stream(name).filter(n -> n != null && !n.isEmpty()).forEach(n -> ret.put(n.trim(), -1));
        
        String[] tk = dimStr.split("[, \t]");
        for (int i = 0; i < tk.length; i++) {
            for (String n : ret.keySet()) {
                if (n.equalsIgnoreCase(tk[i].trim())) {
                    ret.put(n, i);
                    break;
                }
            }
        }
    
        return ret;
    }

    /**
     * Extracts from the time related variable the multiplier and offset to use to calculate the corrected time in
     * milliseconds.
     * 
     * Uses {@link NetCDFUnitsUtils#getMultiplierAndMillisOffsetFromTimeUnits(String)}.
     * 
     * @param timeVar This is the variable corresponding to time.
     * @param fileNameForErrorString Used to fill the data file source in the thrown exception.
     * @return An array with 2 values, the multiplier and the offset
     * @throws Exception
     */
    public static double[] getTimeMultiplierAndOffset(Variable timeVar, String fileNameForErrorString)
            throws Exception {
        String timeUnits = "days since 00-01-00 00:00:00"; // "seconds since 2013-07-04 00:00:00"
        Attribute timeUnitsAtt = timeVar.findAttribute(NETCDF_ATT_UNITS);
        if (timeUnitsAtt != null)
            timeUnits = (String) timeUnitsAtt.getValue(0);
        double[] multAndOffset = NetCDFUnitsUtils.getMultiplierAndMillisOffsetFromTimeUnits(timeUnits);
        if (multAndOffset == null) {
            throw new Exception("Aborting. Can't parse units for variable 'time' (was '" + timeUnits
                    + "') for netCDF file '" + fileNameForErrorString + "'.");
        }
        return multAndOffset;
    }

    public static void main(String[] args) throws Exception {
        { // Test #advanceLoopCounter
            int[] shape = {2, 1, 2, 3};
            int[] counter = new int[shape.length];
            Arrays.fill(counter, 0);
            for (int i = 0; i < Arrays.stream(shape).reduce(1, (x, y) -> (x+1) * y); i++) {
                System.out.println(Arrays.toString(counter));
                System.out.flush();
                counter = advanceLoopCounter(shape, counter);
                if (counter == null)
                    break;
            }
        }
    }
}
