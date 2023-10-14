/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.plugins.envdisp.loader;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info.ScalarOrLogPreference;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Type;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.netcdf.NetCDFUnitsUtils;
import pt.lsts.neptus.util.netcdf.NetCDFUtils;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
public class NetCDFLoader {

    public static final String NETCDF_FILE_PATTERN = ".+\\.nc(\\.gz)?$";

    /**
     * @param dataFile
     * @param varName
     * @param dateLimit If null no filter for time is done
     * @param latDegMinMax If null no filter is applied
     * @param lonDegMinMax If null no filter is applied
     * @param depthMinMax If null no filter is applied
     * @return
     */
    public static final Map<String, GenericDataPoint> processFileForVariable1(NetcdfFile dataFile, String varName, Date dateLimit,
             Pair<Double, Double> latDegMinMax, Pair<Double, Double> lonDegMinMax, Pair<Double, Double> depthMinMax) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;

        String fileName = dataFile.getLocation();

        NeptusLog.pub().info("Starting processing " + varName + " file '" + dataFile.getLocation() + "'."
                + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

        Map<String, GenericDataPoint> dataDp = new LinkedHashMap<>();

        Date fromDate = null;
        Date toDate = null;

        try {
            // Get the Variable.
            Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, varName);
            String vName = searchPair == null ? null : searchPair.first();
            Variable vVar = searchPair == null ? null : searchPair.second();

            if (vName == null || vVar == null) {
                NeptusLog.pub().debug(String.format("Variable %s not found in data field %s", varName, fileName));
                return null;
            }

            String dimStr = vVar.getDimensionsString();
            List<Dimension> dimDim = vVar.getDimensions();

            @SuppressWarnings("unused")
            String latName = null;
            Variable latVar = null;
            @SuppressWarnings("unused")
            String lonName = null;
            Variable lonVar = null;
            @SuppressWarnings("unused")
            String timeName = null;
            Variable timeVar = null;
            @SuppressWarnings("unused")
            String depthName = null;
            Variable depthVar = null;

            // Find the vars for dims
            List<String> dimDimStrLst = Arrays.asList(dimDim.stream().flatMap(d -> Stream.of(d.getShortName())).toArray(String[]::new));
            Map<String, Variable> dimVars = new LinkedHashMap<>();
            for (Dimension d : dimDim) {
                Pair<String, Variable> sPair = NetCDFUtils.findVariableFor(dataFile, fileName, false, dimDimStrLst, d.getShortName());
                if (sPair == null || sPair.second() == null) {
                    dimVars.put(d.getShortName(), null);
                }
                else {
                    dimVars.put(d.getShortName(), sPair.second());
                    Variable v = sPair.second();
                    Attribute vSNAtt = v.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
                    if (vSNAtt != null) {
                        switch (vSNAtt.getStringValue().toLowerCase()) {
                            case "latitude":
                                latName = v.getShortName();
                                latVar = v;
                                continue;
                            case "longitude":
                                lonName = v.getShortName();
                                lonVar = v;
                                continue;
                            case "time":
                            case "ocean_time":
                                timeName = v.getShortName();
                                timeVar = v;
                                continue;
                            case "depth":
                                depthName = v.getShortName();
                                depthVar = v;
                                continue;
                            default:
                                break;
                        }
                    }
                    switch (v.getShortName().toLowerCase()) {
                        case "latitude":
                        case "lat":
                            latName = v.getShortName();
                            latVar = v;
                            continue;
                        case "longitude":
                        case "lon":
                            lonName = v.getShortName();
                            lonVar = v;
                            continue;
                        case "time":
                        case "ocean_time":
                            timeName = v.getShortName();
                            timeVar = v;
                            continue;
                        case "depth":
                            depthName = v.getShortName();
                            depthVar = v;
                            continue;
                        default:
                            break;
                    }
                }
            }

            // Get the latitude and longitude Variables.
            Group navDataGroup = dataFile.findGroup("navigation_data");
            if (latVar == null) {
                searchPair = null;
                if (navDataGroup != null) {
                    Variable varL = NetCDFUtils.findVariableForGroup(navDataGroup, null, "latitude", "lat");
                    if (varL == null)
                        varL = NetCDFUtils.findVariableWithAttributeForGroup(dataFile, navDataGroup, null,
                                NetCDFUtils.NETCDF_ATT_STANDARD_NAME, "latitude", "lat");
                    if (varL != null)
                        searchPair = new Pair<String, Variable>(varL.getShortName(), varL);
                }
                if (searchPair == null)
                    searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst,
                            "latitude", "lat");

                if (searchPair == null) {
                    // Last try
                    for (String name : dimVars.keySet()) {
                        Variable v = dimVars.get(name);
                        if (v == null)
                            continue;
                        String unitsStr = v.getUnitsString();
                        if (unitsStr != null && (NetCDFUtils.NETCDF_DEGREES_NORTH.equalsIgnoreCase(unitsStr)
                                || NetCDFUtils.NETCDF_DEGREE_NORTH.equalsIgnoreCase(unitsStr))) {
                            searchPair = new Pair<String, Variable>(v.getShortName(), v);
                            break;
                        }
                    }
                }

                latName = searchPair == null ? null : searchPair.first();
                latVar = searchPair == null ? null : searchPair.second();
            }

            if (lonVar == null) {
                searchPair = null;
                if (navDataGroup != null) {
                    Variable varL = NetCDFUtils.findVariableForGroup(navDataGroup, null, "longitude", "lon");
                    if (varL == null)
                        varL = NetCDFUtils.findVariableWithAttributeForGroup(dataFile, navDataGroup, null,
                                NetCDFUtils.NETCDF_ATT_STANDARD_NAME, "longitude", "lon");
                    if (varL != null)
                        searchPair = new Pair<String, Variable>(varL.getShortName(), varL);
                }
                if (searchPair == null)
                    searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst,
                            "longitude", "lon");

                if (searchPair == null) {
                    // Last try
                    for (String name : dimVars.keySet()) {
                        Variable v = dimVars.get(name);
                        if (v == null)
                            continue;
                        String unitsStr = v.getUnitsString();
                        if (unitsStr != null && (NetCDFUtils.NETCDF_DEGREES_EAST.equalsIgnoreCase(unitsStr))
                                || NetCDFUtils.NETCDF_DEGREE_EAST.equalsIgnoreCase(unitsStr)) {
                            searchPair = new Pair<String, Variable>(v.getShortName(), v);
                            break;
                        }
                    }
                }

                lonName = searchPair == null ? null : searchPair.first();
                lonVar = searchPair == null ? null : searchPair.second();
            }

            if (timeVar == null) {
                searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst, "time", "ocean_time");
                timeName = searchPair == null ? null : searchPair.first();
                timeVar = searchPair == null ? null : searchPair.second();
            }

            if (depthVar == null) {
                // If varName is already depth, no need to use it
                searchPair = "depth".equalsIgnoreCase(varName) ? null : NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst, "depth");
                depthName = searchPair == null ? null : searchPair.first();;
                depthVar = searchPair == null ? null : searchPair.second(); 
            }

            if (latVar == null || lonVar == null) {
                NeptusLog.pub().debug(String.format("Variable %s IS NOT georeference in data field %s", varName, fileName));
                return null;
            }

            // Get the lat/lon data from the file.
            Array latArray;  // ArrayFloat.D?
            Array lonArray;  // ArrayFloat.D?
            Array timeArray; //ArrayFloat.D?
            Array depthArray; //ArrayFloat.D?
            Array vArray;    // ArrayFloat.D?

            latArray = latVar.read();
            lonArray = lonVar.read();
            timeArray = timeVar != null ? timeVar.read() : null;
            depthArray = depthVar != null ? depthVar.read() : null;
            vArray = vVar.read();

            double[] multAndOffset = timeVar != null ? NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName) : null;
            double timeMultiplier = timeVar != null ? multAndOffset[0] : 1;
            double timeOffset = timeVar != null ? multAndOffset[1] : 0;

            Info info = createInfoBase(vVar);
            info.fileName = fileName;

            // Gradient calc
            boolean calculateGradient = true;
            if (info.type == Type.GEO_2D)
                calculateGradient = true;
            LongAccumulator xyGrad3DimCounter = new LongAccumulator((o, i) -> i, 0);
            ArrayList<GenericDataPoint> gradBuffer = new ArrayList<>();
            int[] gradShape = null;
            DoubleAccumulator minGradient = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                    Double.MAX_VALUE);
            DoubleAccumulator maxGradient = new DoubleAccumulator((o, n) -> Double.compare(n, o) > 0 ? n : o,
                    Double.MIN_VALUE);
            DoubleAccumulator minLonXDelta = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                    Double.MAX_VALUE);
            DoubleAccumulator minLatYDelta = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                    Double.MAX_VALUE);

            // Let us process
            Instant timeStart = Instant.now();
            NeptusLog.pub().warn(String.format("Start processing metadata for %s.", varName));
            try {
                double varFillValue = NetCDFUtils.findFillValue(vVar);
                Pair<Double, Double> varValidRange = NetCDFUtils.findValidRange(vVar);
                Pair<Double, Double> varScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(vVar);

                double latFillValue = NetCDFUtils.findFillValue(latVar);
                Pair<Double, Double> latValidRange = NetCDFUtils.findValidRange(latVar);
                Pair<Double, Double> latScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(latVar);

                double lonFillValue = NetCDFUtils.findFillValue(lonVar);
                Pair<Double, Double> lonValidRange = NetCDFUtils.findValidRange(lonVar);
                Pair<Double, Double> lonScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(lonVar);

                double depthFillValue = depthVar != null ? NetCDFUtils.findFillValue(depthVar) : 0;
                Pair<Double, Double> depthValidRange = depthVar != null ? NetCDFUtils.findValidRange(depthVar)
                        : new Pair<Double, Double>(0., 0.);
                Pair<Double, Double> depthScaleFactorAndAddOffset = depthVar != null
                        ? NetCDFUtils.findScaleFactorAnfAddOffset(depthVar)
                        : new Pair<Double, Double>(1., 0.);

                int[] shape = vVar.getShape();
                int[] counter = new int[shape.length];
                Arrays.fill(counter, 0);

                // Gradient calc
                switch (info.type) {
                    case GEO_TRAJECTORY:
                        info.sizeXY = Arrays.copyOf(shape, shape.length);
                        gradShape = Arrays.copyOfRange(shape, shape.length - 1, shape.length);
                        for (int j = 0; j < gradShape[gradShape.length - 1]; j++)
                            gradBuffer.add(null);
                        Arrays.fill(gradShape, -1);
                        break;
                    case GEO_2D:
                        info.sizeXY = Arrays.copyOfRange(shape, shape.length - 2, shape.length);
                        gradShape = Arrays.copyOfRange(shape, shape.length - 2, shape.length);
                        for (int j = 0; j < gradShape[gradShape.length - 1]; j++)
                            gradBuffer.add(null);
                        Arrays.fill(gradShape, -1);
                        break;
                    case UNKNOWN:
                    default:
                        break;
                }


                // The null values are ignored
                Map<String, Integer> timeCollumsIndexMap = timeVar == null ? new HashMap<>()
                        : NetCDFUtils.getIndexesForVar(dimStr, timeVar.getDimensionsString().split(" "));
                Map<String, Integer> latCollumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr,
                        latVar.getDimensionsString().split(" "));
                Map<String, Integer> lonCollumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr,
                        lonVar.getDimensionsString().split(" "));
                Map<String, Integer> depthCollumsIndexMap = depthVar == null ? new HashMap<>()
                        : NetCDFUtils.getIndexesForVar(dimStr, depthVar.getDimensionsString().split(" "));

                if (timeCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                    timeCollumsIndexMap.clear();
                if (latCollumsIndexMap.values().stream().anyMatch(i -> i < 0)) {
                    latCollumsIndexMap = NetCDFUtils.getMissingIndexesForVarTryMatchDimSize(latCollumsIndexMap,
                            vVar.getDimensions(), latVar.getDimensions());
                    if (latCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                        latCollumsIndexMap.clear();
                }
                if (lonCollumsIndexMap.values().stream().anyMatch(i -> i < 0)) {
                    lonCollumsIndexMap = NetCDFUtils.getMissingIndexesForVarTryMatchDimSize(lonCollumsIndexMap,
                            vVar.getDimensions(), lonVar.getDimensions());
                    if (lonCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                        lonCollumsIndexMap.clear();
                }
                if (depthCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                    depthCollumsIndexMap.clear();

//                Instant timeFinish = Instant.now();
//                long timeElapsed = Duration.between(timeStart, timeFinish).toMillis();
//                NeptusLog.pub().warn(String.format("End processing metadata for %s (took %s).", varName,
//                        DateTimeUtil.milliSecondsToFormatedString(timeElapsed)));
//                NeptusLog.pub().warn(String.format("Start processing values for %s.", varName));

                do {
//                    Instant timeEachLoopStart = Instant.now();

                    Date dateValue = null;
                    Date[] timeVals = !timeCollumsIndexMap.isEmpty()
                            ? NetCDFUtils.getTimeValues(timeArray, buildCounterFrom(counter, timeCollumsIndexMap),
                                    timeMultiplier, timeOffset, fromDate, toDate, ignoreDateLimitToLoad, dateLimit)
                            : null;

                    if (timeVals == null)
                        timeVals = NetCDFUtils.getTimeValuesByGlobalAttributes(dataFile, fromDate, toDate,
                                ignoreDateLimitToLoad, dateLimit);

                    if (timeVals == null)
                        timeVals = NetCDFUtils.getDatesAndDateLimits(new Date(0), fromDate, toDate);

                    dateValue = timeVals[0];
                    fromDate = timeVals[1];
                    toDate = timeVals[2];

                    double lat = latArray.getDouble(buildIndexFrom(latArray, counter, latCollumsIndexMap));
                    double lon = lonArray.getDouble(buildIndexFrom(lonArray, counter, lonCollumsIndexMap));

                    if (!NetCDFUtils.isValueValid(lat, latFillValue, latValidRange)
                            || !NetCDFUtils.isValueValid(lon, lonFillValue, lonValidRange)) {
                        NeptusLog.pub().debug(
                                String.format("While processing %s found invalid values for lat or lon!", varName));
                        fillGradient(calculateGradient, gradBuffer, gradShape, xyGrad3DimCounter, minGradient,
                                maxGradient, minLonXDelta, minLatYDelta, counter, null);

//                        Instant timeEachLoopFinish = Instant.now();
//                        long timeEachLoopElapsed = Duration.between(timeEachLoopStart, timeEachLoopFinish).toNanos();
//                        NeptusLog.pub().warn(String.format("Loop counter %s processing for %s (took %s ns).", Arrays.toString(counter), varName,
//                                timeEachLoopElapsed));

                        continue;
                    }

                    lat = lat * latScaleFactorAndAddOffset.first() + latScaleFactorAndAddOffset.second();
                    lon = lon * lonScaleFactorAndAddOffset.first() + lonScaleFactorAndAddOffset.second();
                    lat = AngleUtils.nomalizeAngleDegrees180(lat);
                    lon = AngleUtils.nomalizeAngleDegrees180(lon);

                    double depth = !depthCollumsIndexMap.isEmpty()
                            ? depthArray.getDouble(buildIndexFrom(depthArray, counter, depthCollumsIndexMap))
                            : Double.NaN;
                    if (!Double.isNaN(depth) && NetCDFUtils.isValueValid(depth, depthFillValue, depthValidRange)) {
                        depth = depth * depthScaleFactorAndAddOffset.first() + depthScaleFactorAndAddOffset.second();
                        depth = NetCDFUnitsUtils.getValueForMetterFromTempUnits(depth, depthVar.getUnitsString());
                    }

                    // Check limits passed
                    boolean checkLimitsLatOk = true;
                    boolean checkLimitsLonOk = true;
                    boolean checkLimitsDepthOk = true;
                    if (latDegMinMax != null
                            && (Double.isFinite(latDegMinMax.first()) && Double.compare(lat, latDegMinMax.first()) < 0
                                    || Double.isFinite(latDegMinMax.second())
                                            && Double.compare(lat, latDegMinMax.second()) > 0))
                        checkLimitsLatOk = false;
                    if (lonDegMinMax != null
                            && (Double.isFinite(lonDegMinMax.first()) && Double.compare(lon, lonDegMinMax.first()) < 0
                                    || Double.isFinite(lonDegMinMax.second())
                                            && Double.compare(lon, lonDegMinMax.second()) > 0))
                        checkLimitsLonOk = false;
                    if (Double.isFinite(depth) && depthMinMax != null
                            && (Double.isFinite(depthMinMax.first()) && Double.compare(depth, depthMinMax.first()) < 0
                                    || Double.isFinite(depthMinMax.second())
                                            && Double.compare(depth, depthMinMax.second()) > 0))
                        checkLimitsDepthOk = false;
                    if (!checkLimitsLatOk || !checkLimitsLonOk || !checkLimitsDepthOk) {
//                        NeptusLog.pub().debug(String.format(
//                                "While processing %s found a valid value outside passed limits (lat:%s, lon:%s, depth:%s)!",
//                                varName, checkLimitsLatOk ? "ok" : "rejected", checkLimitsLonOk ? "ok" : "rejected",
//                                checkLimitsDepthOk ? "ok" : "rejected"));
                        fillGradient(calculateGradient, gradBuffer, gradShape, xyGrad3DimCounter, minGradient,
                                maxGradient, minLonXDelta, minLatYDelta, counter, null);
                        continue;
                    }

//                    if (!checkLimitsDepthOk)
//                        depth = Double.NaN;

                    Index index = vArray.getIndex();
                    index.set(counter);

                    double v = vArray.getDouble(index);

                    if (NetCDFUtils.isValueValid(v, varFillValue, varValidRange)) {
                        GenericDataPoint dp = new GenericDataPoint(lat, lon);
                        dp.setInfo(info);

                        v = v * varScaleFactorAndAddOffset.first() + varScaleFactorAndAddOffset.second();
                        if (info.scalarOrLogPreference == ScalarOrLogPreference.LOG10)
                            v = Math.pow(10, v); // let us unlog

                        // Doing nothing with units, just using what it is
                        // v = NetCDFUnitsUtils.getValueForMetterFromTempUnits(v, vUnits);

                        dp.setValue(v);
                        if (dp.getInfo().minVal == Double.MIN_VALUE || v < dp.getInfo().minVal)
                            dp.getInfo().minVal = v;
                        if (dp.getInfo().maxVal == Double.MAX_VALUE || v > dp.getInfo().maxVal)
                            dp.getInfo().maxVal = v;

                        dp.setDateUTC(dateValue);
                        if (dp.getInfo().minDate.getTime() == 0 || dp.getInfo().minDate.after(dateValue))
                            dp.getInfo().minDate = dateValue;
                        if (dp.getInfo().maxDate.getTime() == 0 || dp.getInfo().maxDate.before(dateValue))
                            dp.getInfo().maxDate = dateValue;

                        dp.setDepth(depth); // See better this!!
                        if (Double.isFinite(depth)) {
                            if (!Double.isFinite(dp.getInfo().minDepth) || dp.getInfo().minDepth == Double.MIN_VALUE
                                    || depth < dp.getInfo().minDepth)
                                dp.getInfo().minDepth = depth;
                            if (!Double.isFinite(dp.getInfo().maxDepth) || dp.getInfo().maxDepth == Double.MAX_VALUE
                                    || depth > dp.getInfo().maxDepth)
                                dp.getInfo().maxDepth = depth;
                        }

                        GenericDataPoint dpo = dataDp.get(dp.getId());
                        if (dpo == null) {
                            switch (info.type) {
                                case GEO_TRAJECTORY:
                                    dp.setIndexesXY(Arrays.copyOf(counter, counter.length));
                                    break;
                                case GEO_2D:
                                    dp.setIndexesXY(Arrays.copyOfRange(counter, counter.length - 2, counter.length));
                                    break;
                                case UNKNOWN:
                                default:
                                    break;
                            }

                            dpo = dp.getACopyWithoutHistory();
                            dataDp.put(dpo.getId(), dpo);
                        }
                        else {
                            dp.setIndexesXY(dpo.getIndexesXY());
                        }

//                        Instant timeEachLoopFinish = Instant.now();
//                        long timeEachLoopElapsed = Duration.between(timeEachLoopStart, timeEachLoopFinish).toNanos();
//                        NeptusLog.pub().warn(String.format("Loop counter %s processing BEFORE for %s (took %s ns).", Arrays.toString(counter), varName,
//                                timeEachLoopElapsed));

                        ArrayList<GenericDataPoint> lst = dpo.getHistoricalData();
//                        boolean alreadyIn = false;
//                        for (GenericDataPoint tmpDp : lst) {
//                            // Check also depth and see if no time
//                            if (tmpDp.getDateUTC().equals(dp.getDateUTC()) && tmpDp.getDepth() == dp.getDepth()) {
//                                alreadyIn = true;
//                                break;
//                            }
//                        }
                        boolean alreadyIn = lst.parallelStream().anyMatch((tmpDp) -> {
                            // Check also depth and see if no time
                            return (tmpDp.getDateUTC().equals(dp.getDateUTC()) && tmpDp.getDepth() == dp.getDepth());
                        });

                        if (!alreadyIn)
                            dpo.getHistoricalData().add(dp);

                        fillGradient(calculateGradient, gradBuffer, gradShape, xyGrad3DimCounter, minGradient,
                                maxGradient, minLonXDelta, minLatYDelta, counter, dp);
                    }
                    else {
                        fillGradient(calculateGradient, gradBuffer, gradShape, xyGrad3DimCounter, minGradient,
                                maxGradient, minLonXDelta, minLatYDelta, counter, null);
                    }

//                    Instant timeEachLoopFinish = Instant.now();
//                    long timeEachLoopElapsed = Duration.between(timeEachLoopStart, timeEachLoopFinish).toNanos();
//                    NeptusLog.pub().warn(String.format("Loop counter %s processing for %s (took %s ns).", Arrays.toString(counter), varName,
//                            timeEachLoopElapsed));
                } while (NetCDFUtils.advanceLoopCounter(shape, counter) != null);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                Instant timeFinish = Instant.now();
                long timeElapsed = Duration.between(timeStart, timeFinish).toMillis();
                NeptusLog.pub().warn(String.format("End processing %s (took %s).", varName,
                        DateTimeUtil.milliSecondsToFormatedString(timeElapsed)));
            }

            // Gradient calculation
            if (minGradient.doubleValue() < Double.MAX_VALUE && maxGradient.doubleValue() > Double.MIN_VALUE) {
                info.minGradient = minGradient.doubleValue();
                info.maxGradient = maxGradient.doubleValue();
                info.validGradientData = true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
        finally {
            NeptusLog.pub().info("Ending processing " + varName + " netCDF file '" + fileName
                    + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
        }
        return dataDp;
    }

    /**
     * @param dataFile
     * @param varName
     * @param dateLimit If null no filter for time is done
     * @param latDegMinMax If null no filter is applied
     * @param lonDegMinMax If null no filter is applied
     * @param depthMinMax If null no filter is applied
     * @return
     */
    public static final Map<String, GenericDataPoint> processFileForVariable2(NetcdfFile dataFile, String varName, Date dateLimit,
            Pair<Double, Double> latDegMinMax, Pair<Double, Double> lonDegMinMax, Pair<Double, Double> depthMinMax) {
       final boolean ignoreDateLimitToLoad = dateLimit == null ? true : false;

       String fileName = dataFile.getLocation();

       NeptusLog.pub().info("Starting processing " + varName + " file '" + dataFile.getLocation() + "'."
               + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

       Map<String, GenericDataPoint> dataDp = new LinkedHashMap<>();

       AtomicReference<Date> fromDate = new AtomicReference<>(null);
       AtomicReference<Date> toDate = new AtomicReference<>(null);

       try {
           // Get the Variable.
           Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, varName);
           String vName = searchPair == null ? null : searchPair.first();
           Variable vVar = searchPair == null ? null : searchPair.second();

           if (vName == null || vVar == null) {
               NeptusLog.pub().debug(String.format("Variable %s not found in data field %s", varName, fileName));
               return null;
           }

           String dimStr = vVar.getDimensionsString();
           List<Dimension> dimDim = vVar.getDimensions();

           @SuppressWarnings("unused")
           String latName = null;
           Variable latVar = null;
           @SuppressWarnings("unused")
           String lonName = null;
           Variable lonVar = null;
           @SuppressWarnings("unused")
           String timeName = null;
           Variable timeVar = null;
           @SuppressWarnings("unused")
           String depthName = null;
           Variable depthVar = null;

           // Find the vars for dims
           List<String> dimDimStrLst = Arrays.asList(dimDim.stream().flatMap(d -> Stream.of(d.getShortName())).toArray(String[]::new));
           Map<String, Variable> dimVars = new LinkedHashMap<>();
           for (Dimension d : dimDim) {
               Pair<String, Variable> sPair = NetCDFUtils.findVariableFor(dataFile, fileName, false, dimDimStrLst, d.getShortName());
               if (sPair == null || sPair.second() == null) {
                   dimVars.put(d.getShortName(), null);
               }
               else {
                   dimVars.put(d.getShortName(), sPair.second());
                   Variable v = sPair.second();
                   Attribute vSNAtt = v.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
                   if (vSNAtt != null) {
                       switch (vSNAtt.getStringValue().toLowerCase()) {
                           case "latitude":
                               latName = v.getShortName();
                               latVar = v;
                               continue;
                           case "longitude":
                               lonName = v.getShortName();
                               lonVar = v;
                               continue;
                           case "time":
                           case "ocean_time":
                               timeName = v.getShortName();
                               timeVar = v;
                               continue;
                           case "depth":
                               depthName = v.getShortName();
                               depthVar = v;
                               continue;
                           default:
                               break;
                       }
                   }
                   switch (v.getShortName().toLowerCase()) {
                       case "latitude":
                       case "lat":
                           latName = v.getShortName();
                           latVar = v;
                           continue;
                       case "longitude":
                       case "lon":
                           lonName = v.getShortName();
                           lonVar = v;
                           continue;
                       case "time":
                       case "ocean_time":
                           timeName = v.getShortName();
                           timeVar = v;
                           continue;
                       case "depth":
                           depthName = v.getShortName();
                           depthVar = v;
                           continue;
                       default:
                           break;
                   }
               }
           }

           // Get the latitude and longitude Variables.
           Group navDataGroup = dataFile.findGroup("navigation_data");
           if (latVar == null) {
               searchPair = null;
               if (navDataGroup != null) {
                   Variable varL = NetCDFUtils.findVariableForGroup(navDataGroup, null, "latitude", "lat");
                   if (varL == null)
                       varL = NetCDFUtils.findVariableWithAttributeForGroup(dataFile, navDataGroup, null,
                               NetCDFUtils.NETCDF_ATT_STANDARD_NAME, "latitude", "lat");
                   if (varL != null)
                       searchPair = new Pair<String, Variable>(varL.getShortName(), varL);
               }
               if (searchPair == null)
                   searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst,
                           "latitude", "lat");

               if (searchPair == null) {
                   // Last try
                   for (String name : dimVars.keySet()) {
                       Variable v = dimVars.get(name);
                       if (v == null)
                           continue;
                       String unitsStr = v.getUnitsString();
                       if (unitsStr != null && (NetCDFUtils.NETCDF_DEGREES_NORTH.equalsIgnoreCase(unitsStr)
                               || NetCDFUtils.NETCDF_DEGREE_NORTH.equalsIgnoreCase(unitsStr))) {
                           searchPair = new Pair<String, Variable>(v.getShortName(), v);
                           break;
                       }
                   }
               }

               latName = searchPair == null ? null : searchPair.first();
               latVar = searchPair == null ? null : searchPair.second();
           }

           if (lonVar == null) {
               searchPair = null;
               if (navDataGroup != null) {
                   Variable varL = NetCDFUtils.findVariableForGroup(navDataGroup, null, "longitude", "lon");
                   if (varL == null)
                       varL = NetCDFUtils.findVariableWithAttributeForGroup(dataFile, navDataGroup, null,
                               NetCDFUtils.NETCDF_ATT_STANDARD_NAME, "longitude", "lon");
                   if (varL != null)
                       searchPair = new Pair<String, Variable>(varL.getShortName(), varL);
               }
               if (searchPair == null)
                   searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst,
                           "longitude", "lon");

               if (searchPair == null) {
                   // Last try
                   for (String name : dimVars.keySet()) {
                       Variable v = dimVars.get(name);
                       if (v == null)
                           continue;
                       String unitsStr = v.getUnitsString();
                       if (unitsStr != null && (NetCDFUtils.NETCDF_DEGREES_EAST.equalsIgnoreCase(unitsStr))
                               || NetCDFUtils.NETCDF_DEGREE_EAST.equalsIgnoreCase(unitsStr)) {
                           searchPair = new Pair<String, Variable>(v.getShortName(), v);
                           break;
                       }
                   }
               }

               lonName = searchPair == null ? null : searchPair.first();
               lonVar = searchPair == null ? null : searchPair.second();
           }

           if (timeVar == null) {
               searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst, "time", "ocean_time");
               timeName = searchPair == null ? null : searchPair.first();
               timeVar = searchPair == null ? null : searchPair.second();
           }

           if (depthVar == null) {
               // If varName is already depth, no need to use it
               searchPair = "depth".equalsIgnoreCase(varName) ? null : NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst, "depth");
               depthName = searchPair == null ? null : searchPair.first();
               depthVar = searchPair == null ? null : searchPair.second();
           }

           if (latVar == null || lonVar == null) {
               NeptusLog.pub().debug(String.format("Variable %s IS NOT georeference in data field %s", varName, fileName));
               return null;
           }

           // Get the lat/lon data from the file.
           Array latArray;  // ArrayFloat.D?
           Array lonArray;  // ArrayFloat.D?
           Array timeArray; //ArrayFloat.D?
           Array depthArray; //ArrayFloat.D?
           Array vArray;    // ArrayFloat.D?

           latArray = latVar.read();
           lonArray = lonVar.read();
           timeArray = timeVar != null ? timeVar.read() : null;
           depthArray = depthVar != null ? depthVar.read() : null;
           vArray = vVar.read();

           double[] multAndOffset = timeVar != null ? NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName) : null;
           double timeMultiplier = timeVar != null ? multAndOffset[0] : 1;
           double timeOffset = timeVar != null ? multAndOffset[1] : 0;

           Info info = createInfoBase(vVar);
           info.fileName = fileName;

           // Gradient calc
           final boolean calculateGradient = true;
//           if (info.type == Type.GEO_2D)
//               calculateGradient = true;
           LongAccumulator xyGrad3DimCounter = new LongAccumulator((o, i) -> i, 0);
           ArrayList<GenericDataPoint> gradBuffer = new ArrayList<>();
           int[] gradShape = null;
           DoubleAccumulator minGradient = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                   Double.MAX_VALUE);
           DoubleAccumulator maxGradient = new DoubleAccumulator((o, n) -> Double.compare(n, o) > 0 ? n : o,
                   Double.MIN_VALUE);
           DoubleAccumulator minLonXDelta = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                   Double.MAX_VALUE);
           DoubleAccumulator minLatYDelta = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                   Double.MAX_VALUE);

           LongAccumulator avgTime = new LongAccumulator((c, v) -> c == -1 ? v : (long)((c + v) / 2.0), -1);

           // Let us process
           Instant timeStart = Instant.now();
           NeptusLog.pub().warn(String.format("Start processing metadata for %s.", varName));
           int elemTotal = -1;
           final AtomicInteger elemCounter = new AtomicInteger(-1);
           final AtomicInteger elemCounterAccepted = new AtomicInteger(-1);
           try {
               double varFillValue = NetCDFUtils.findFillValue(vVar);
               Pair<Double, Double> varValidRange = NetCDFUtils.findValidRange(vVar);
               Pair<Double, Double> varScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(vVar);

               double latFillValue = NetCDFUtils.findFillValue(latVar);
               Pair<Double, Double> latValidRange = NetCDFUtils.findValidRange(latVar);
               Pair<Double, Double> latScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(latVar);

               double lonFillValue = NetCDFUtils.findFillValue(lonVar);
               Pair<Double, Double> lonValidRange = NetCDFUtils.findValidRange(lonVar);
               Pair<Double, Double> lonScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(lonVar);

               double depthFillValue = depthVar != null ? NetCDFUtils.findFillValue(depthVar) : 0;
               Pair<Double, Double> depthValidRange = depthVar != null ? NetCDFUtils.findValidRange(depthVar)
                       : new Pair<Double, Double>(0., 0.);
               Pair<Double, Double> depthScaleFactorAndAddOffset = depthVar != null
                       ? NetCDFUtils.findScaleFactorAnfAddOffset(depthVar)
                       : new Pair<Double, Double>(1., 0.);

               int[] shape = vVar.getShape();
               int[] counterIdx = new int[shape.length];
               Arrays.fill(counterIdx, 0);

               // Gradient calc
               switch (info.type) {
                   case GEO_TRAJECTORY:
                       info.sizeXY = Arrays.copyOf(shape, shape.length);
                       gradShape = Arrays.copyOfRange(shape, shape.length - 1, shape.length);
                       for (int j = 0; j < gradShape[gradShape.length - 1]; j++)
                           gradBuffer.add(null);
                       Arrays.fill(gradShape, -1);
                       break;
                   case GEO_2D:
                       info.sizeXY = Arrays.copyOfRange(shape, shape.length - 2, shape.length);
                       gradShape = Arrays.copyOfRange(shape, shape.length - 2, shape.length);
                       for (int j = 0; j < gradShape[gradShape.length - 1]; j++)
                           gradBuffer.add(null);
                       Arrays.fill(gradShape, -1);
                       break;
                   case UNKNOWN:
                   default:
                       break;
               }


               // The null values are ignored
               final Map<String, Integer> timeCollumsIndexMap = timeVar == null ? new HashMap<>()
                       : NetCDFUtils.getIndexesForVar(dimStr, timeVar.getDimensionsString().split(" "));
               final Map<String, Integer> latCollumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr,
                       latVar.getDimensionsString().split(" "));
               final Map<String, Integer> lonCollumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr,
                       lonVar.getDimensionsString().split(" "));
               final Map<String, Integer> depthCollumsIndexMap = depthVar == null ? new HashMap<>()
                       : NetCDFUtils.getIndexesForVar(dimStr, depthVar.getDimensionsString().split(" "));

               if (timeCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                   timeCollumsIndexMap.clear();
               if (latCollumsIndexMap.values().stream().anyMatch(i -> i < 0)) {
                   NetCDFUtils.getMissingIndexesForVarTryMatchDimSize(latCollumsIndexMap,
                           vVar.getDimensions(), latVar.getDimensions());
                   if (latCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                       latCollumsIndexMap.clear();
               }
               if (lonCollumsIndexMap.values().stream().anyMatch(i -> i < 0)) {
                   NetCDFUtils.getMissingIndexesForVarTryMatchDimSize(lonCollumsIndexMap,
                           vVar.getDimensions(), lonVar.getDimensions());
                   if (lonCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                       lonCollumsIndexMap.clear();
               }
               if (depthCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                   depthCollumsIndexMap.clear();

               Instant timeFinish = Instant.now();
               long timeElapsed = Duration.between(timeStart, timeFinish).toMillis();
               NeptusLog.pub().warn(String.format("End processing metadata for %s (took %s).", varName,
                       DateTimeUtil.milliSecondsToFormatedString(timeElapsed)));
               NeptusLog.pub().warn(String.format("Start processing values for %s.", varName));

               final int[] gradShapeFinal = gradShape;
               final Variable depthVarFinal = depthVar;
               elemTotal = Arrays.stream(shape).reduce(1, (x, y) -> x * y);
               elemCounter.set(0);
               elemCounterAccepted.set(0);
//               Stream.generate(() -> {
//                           synchronized (counterIdx) {
//                               int[] val = NetCDFUtils.advanceLoopCounter(shape, counterIdx);
//                               return val == null ? null : Arrays.copyOf(val, val.length);
//                           }
//                       })
//                       .limit(elemTotal).filter((v) -> v != null)
//                       .parallel()
//                       .forEach((counter) -> {
               AtomicReference<int[]> counterAtomicRef = new AtomicReference<>(counterIdx);
               IntStream.range(0, elemTotal)
                       .parallel()
                       .forEach((i) -> {
                   int[] counter = counterAtomicRef.getAndUpdate((c) -> NetCDFUtils.advanceLoopCounter(shape, Arrays.copyOf(c, c.length)));
                   
                   elemCounter.addAndGet(1);
                   Instant timeEachLoopStart = Instant.now();

                   Date dateValue = null;
                   Date[] timeVals = !timeCollumsIndexMap.isEmpty()
                           ? NetCDFUtils.getTimeValues(timeArray, buildCounterFrom(counter, timeCollumsIndexMap),
                                   timeMultiplier, timeOffset, fromDate.get(), toDate.get(), ignoreDateLimitToLoad, dateLimit)
                           : null;

                   if (timeVals == null)
                       timeVals = NetCDFUtils.getTimeValuesByGlobalAttributes(dataFile, fromDate.get(), toDate.get(),
                               ignoreDateLimitToLoad, dateLimit);

                   if (timeVals == null)
                       timeVals = NetCDFUtils.getDatesAndDateLimits(new Date(0), fromDate.get(), toDate.get());

                   dateValue = timeVals[0];
                   fromDate.accumulateAndGet(timeVals[1], (c, v) -> {
                       if (c == null)
                           return v;
                       else if (v.before(c))
                           return v;
                       return c;
                   });
                   toDate.accumulateAndGet(timeVals[2], (c, v) -> {
                       if (c == null)
                           return v;
                       else if (v.after(c))
                           return v;
                       return c;
                   });

                   double lat = latArray.getDouble(buildIndexFrom(latArray, counter, latCollumsIndexMap));
                   double lon = lonArray.getDouble(buildIndexFrom(lonArray, counter, lonCollumsIndexMap));

                   if (!NetCDFUtils.isValueValid(lat, latFillValue, latValidRange)
                           || !NetCDFUtils.isValueValid(lon, lonFillValue, lonValidRange)) {
                       NeptusLog.pub().debug(
                               String.format("While processing %s found invalid values for lat or lon!", varName));
                       fillGradient(calculateGradient, gradBuffer, gradShapeFinal, xyGrad3DimCounter, minGradient,
                               maxGradient, minLonXDelta, minLatYDelta, counter, null);

//                       Instant timeEachLoopFinish = Instant.now();
//                       long timeEachLoopElapsed = Duration.between(timeEachLoopStart, timeEachLoopFinish).toNanos();
//                       NeptusLog.pub().warn(String.format("Loop counter %s processing for %s (took %s ns).", Arrays.toString(counter), varName,
//                               timeEachLoopElapsed));

                       // avgTime.accumulate(Duration.between(timeEachLoopStart, Instant.now()).toNanos());
                       return;
                   }

                   lat = lat * latScaleFactorAndAddOffset.first() + latScaleFactorAndAddOffset.second();
                   lon = lon * lonScaleFactorAndAddOffset.first() + lonScaleFactorAndAddOffset.second();
                   lat = AngleUtils.nomalizeAngleDegrees180(lat);
                   lon = AngleUtils.nomalizeAngleDegrees180(lon);

                   double depth = !depthCollumsIndexMap.isEmpty()
                           ? depthArray.getDouble(buildIndexFrom(depthArray, counter, depthCollumsIndexMap))
                           : Double.NaN;
                   if (!Double.isNaN(depth) && NetCDFUtils.isValueValid(depth, depthFillValue, depthValidRange)) {
                       depth = depth * depthScaleFactorAndAddOffset.first() + depthScaleFactorAndAddOffset.second();
                       depth = NetCDFUnitsUtils.getValueForMetterFromTempUnits(depth, depthVarFinal.getUnitsString());
                   }

                   // Check limits passed
                   boolean checkLimitsLatOk = true;
                   boolean checkLimitsLonOk = true;
                   boolean checkLimitsDepthOk = true;
                   if (latDegMinMax != null
                           && (Double.isFinite(latDegMinMax.first()) && Double.compare(lat, latDegMinMax.first()) < 0
                                   || Double.isFinite(latDegMinMax.second())
                                           && Double.compare(lat, latDegMinMax.second()) > 0))
                       checkLimitsLatOk = false;
                   if (lonDegMinMax != null
                           && (Double.isFinite(lonDegMinMax.first()) && Double.compare(lon, lonDegMinMax.first()) < 0
                                   || Double.isFinite(lonDegMinMax.second())
                                           && Double.compare(lon, lonDegMinMax.second()) > 0))
                       checkLimitsLonOk = false;
                   if (Double.isFinite(depth) && depthMinMax != null
                           && (Double.isFinite(depthMinMax.first()) && Double.compare(depth, depthMinMax.first()) < 0
                                   || Double.isFinite(depthMinMax.second())
                                           && Double.compare(depth, depthMinMax.second()) > 0))
                       checkLimitsDepthOk = false;
                   if (!checkLimitsLatOk || !checkLimitsLonOk || !checkLimitsDepthOk) {
//                       NeptusLog.pub().debug(String.format(
//                               "While processing %s found a valid value outside passed limits (lat:%s, lon:%s, depth:%s)!",
//                               varName, checkLimitsLatOk ? "ok" : "rejected", checkLimitsLonOk ? "ok" : "rejected",
//                               checkLimitsDepthOk ? "ok" : "rejected"));
                       fillGradient(calculateGradient, gradBuffer, gradShapeFinal, xyGrad3DimCounter, minGradient,
                               maxGradient, minLonXDelta, minLatYDelta, counter, null);

                       avgTime.accumulate(Duration.between(timeEachLoopStart, Instant.now()).toNanos());
                       return;
                   }

//                   if (!checkLimitsDepthOk)
//                       depth = Double.NaN;

                   Index index = vArray.getIndex();
                   index.set(counter);

                   double v = vArray.getDouble(index);

                   if (NetCDFUtils.isValueValid(v, varFillValue, varValidRange)) {
                       GenericDataPoint dp = new GenericDataPoint(lat, lon);
                       dp.setInfo(info);

                       v = v * varScaleFactorAndAddOffset.first() + varScaleFactorAndAddOffset.second();
                       if (info.scalarOrLogPreference == ScalarOrLogPreference.LOG10)
                           v = Math.pow(10, v); // let us unlog

                       // Doing nothing with units, just using what it is
                       // v = NetCDFUnitsUtils.getValueForMetterFromTempUnits(v, vUnits);

                       dp.setValue(v);
                       if (dp.getInfo().minVal == Double.MIN_VALUE || v < dp.getInfo().minVal)
                           dp.getInfo().minVal = v;
                       if (dp.getInfo().maxVal == Double.MAX_VALUE || v > dp.getInfo().maxVal)
                           dp.getInfo().maxVal = v;

                       dp.setDateUTC(dateValue);
                       if (dp.getInfo().minDate.getTime() == 0 || dp.getInfo().minDate.after(dateValue))
                           dp.getInfo().minDate = dateValue;
                       if (dp.getInfo().maxDate.getTime() == 0 || dp.getInfo().maxDate.before(dateValue))
                           dp.getInfo().maxDate = dateValue;

                       dp.setDepth(depth); // See better this!!
                       if (Double.isFinite(depth)) {
                           if (!Double.isFinite(dp.getInfo().minDepth) || dp.getInfo().minDepth == Double.MIN_VALUE
                                   || depth < dp.getInfo().minDepth)
                               dp.getInfo().minDepth = depth;
                           if (!Double.isFinite(dp.getInfo().maxDepth) || dp.getInfo().maxDepth == Double.MAX_VALUE
                                   || depth > dp.getInfo().maxDepth)
                               dp.getInfo().maxDepth = depth;
                       }

                       synchronized (dataDp) {
                           GenericDataPoint dpo = dataDp.get(dp.getId());
                           if (dpo == null) {
                               switch (info.type) {
                                   case GEO_TRAJECTORY:
                                       dp.setIndexesXY(Arrays.copyOf(counter, counter.length));
                                       break;
                                   case GEO_2D:
                                       dp.setIndexesXY(Arrays.copyOfRange(counter, counter.length - 2, counter.length));
                                       break;
                                   case UNKNOWN:
                                   default:
                                       break;
                               }

                               dpo = dp.getACopyWithoutHistory();
                               dataDp.put(dpo.getId(), dpo);
                           }
                           else {
                               dp.setIndexesXY(dpo.getIndexesXY());
                           }

//                       Instant timeEachLoopFinish = Instant.now();
//                       long timeEachLoopElapsed = Duration.between(timeEachLoopStart, timeEachLoopFinish).toNanos();
//                       NeptusLog.pub().warn(String.format("Loop counter %s processing BEFORE for %s (took %s ns).", Arrays.toString(counter), varName,
//                               timeEachLoopElapsed));

                           ArrayList<GenericDataPoint> lst = dpo.getHistoricalData();
//                       boolean alreadyIn = false;
//                       for (GenericDataPoint tmpDp : lst) {
//                           // Check also depth and see if no time
//                           if (tmpDp.getDateUTC().equals(dp.getDateUTC()) && tmpDp.getDepth() == dp.getDepth()) {
//                               alreadyIn = true;
//                               break;
//                           }
//                       }
                           boolean alreadyIn = lst.parallelStream().anyMatch((tmpDp) -> {
                               // Check also depth and see if no time
                               return (tmpDp.getDateUTC().equals(dp.getDateUTC()) && tmpDp.getDepth() == dp.getDepth());
                           });

                           if (!alreadyIn)
                               dpo.getHistoricalData().add(dp);
                       }

                       fillGradient(calculateGradient, gradBuffer, gradShapeFinal, xyGrad3DimCounter, minGradient,
                               maxGradient, minLonXDelta, minLatYDelta, counter, dp);
                   }
                   else {
                       fillGradient(calculateGradient, gradBuffer, gradShapeFinal, xyGrad3DimCounter, minGradient,
                               maxGradient, minLonXDelta, minLatYDelta, counter, null);
                   }

//                   Instant timeEachLoopFinish = Instant.now();
//                   long timeEachLoopElapsed = Duration.between(timeEachLoopStart, timeEachLoopFinish).toNanos();
//                   NeptusLog.pub().warn(String.format("Loop counter %s processing for %s (took %s ns).", Arrays.toString(counter), varName,
//                           timeEachLoopElapsed));
                   avgTime.accumulate(Duration.between(timeEachLoopStart, Instant.now()).toNanos());
                   elemCounterAccepted.addAndGet(1);
               });
           }
           catch (Exception e) {
               e.printStackTrace();
           }
           finally {
               Instant timeFinish = Instant.now();
               long timeElapsed = Duration.between(timeStart, timeFinish).toMillis();
               NeptusLog.pub().warn(String.format("End processing %s (took %s) (mean per read %s us) [total: %s | proc: %s | accep: %s].",
                       varName,
                       DateTimeUtil.milliSecondsToFormatedString(timeElapsed), avgTime.get() / 1E3,
                       elemTotal, elemCounter.get(), elemCounterAccepted.get()));
           }

           // Gradient calculation
           if (minGradient.doubleValue() < Double.MAX_VALUE && maxGradient.doubleValue() > Double.MIN_VALUE) {
               info.minGradient = minGradient.doubleValue();
               info.maxGradient = maxGradient.doubleValue();
               info.validGradientData = true;
           }
       }
       catch (Exception e) {
           e.printStackTrace();
           return null;
       }
       finally {
           NeptusLog.pub().info("Ending processing " + varName + " netCDF file '" + fileName
                   + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
       }
       return dataDp;
   }

    /**
     * @param dataFile
     * @param varName
     * @param dateLimit If null no filter for time is done
     * @param latDegMinMax If null no filter is applied
     * @param lonDegMinMax If null no filter is applied
     * @param depthMinMax If null no filter is applied
     * @return
     */
    public static final Map<String, GenericDataPoint> processFileForVariable(NetcdfFile dataFile, String varName, Date dateLimit,
            Pair<Double, Double> latDegMinMax, Pair<Double, Double> lonDegMinMax, Pair<Double, Double> depthMinMax) {
       final boolean ignoreDateLimitToLoad = dateLimit == null ? true : false;

       String fileName = dataFile.getLocation();

       NeptusLog.pub().info("Starting processing " + varName + " file '" + dataFile.getLocation() + "'."
               + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

       Map<String, GenericDataPoint> dataDp = new LinkedHashMap<>();

       AtomicReference<Date> fromDate = new AtomicReference<>(null);
       AtomicReference<Date> toDate = new AtomicReference<>(null);

       try {
           // Get the Variable.
           Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, varName);
           String vName = searchPair == null ? null : searchPair.first();
           Variable vVar = searchPair == null ? null : searchPair.second();

           if (vName == null || vVar == null) {
               NeptusLog.pub().debug(String.format("Variable %s not found in data field %s", varName, fileName));
               return null;
           }

           String dimStr = vVar.getDimensionsString();
           List<Dimension> dimDim = vVar.getDimensions();

           @SuppressWarnings("unused")
           String latName = null;
           Variable latVar = null;
           @SuppressWarnings("unused")
           String lonName = null;
           Variable lonVar = null;
           @SuppressWarnings("unused")
           String timeName = null;
           Variable timeVar = null;
           @SuppressWarnings("unused")
           String depthName = null;
           Variable depthVar = null;

           // Find the vars for dims
           List<String> dimDimStrLst = Arrays.asList(dimDim.stream().flatMap(d -> Stream.of(d.getShortName())).toArray(String[]::new));
           Map<String, Variable> dimVars = new LinkedHashMap<>();
           for (Dimension d : dimDim) {
               Pair<String, Variable> sPair = NetCDFUtils.findVariableFor(dataFile, fileName, false, dimDimStrLst, d.getShortName());
               if (sPair == null || sPair.second() == null) {
                   dimVars.put(d.getShortName(), null);
               }
               else {
                   dimVars.put(d.getShortName(), sPair.second());
                   Variable v = sPair.second();
                   Attribute vSNAtt = v.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
                   if (vSNAtt != null) {
                       switch (vSNAtt.getStringValue().toLowerCase()) {
                           case "latitude":
                               latName = v.getShortName();
                               latVar = v;
                               continue;
                           case "longitude":
                               lonName = v.getShortName();
                               lonVar = v;
                               continue;
                           case "time":
                           case "ocean_time":
                               timeName = v.getShortName();
                               timeVar = v;
                               continue;
                           case "depth":
                               depthName = v.getShortName();
                               depthVar = v;
                               continue;
                           default:
                               break;
                       }
                   }
                   switch (v.getShortName().toLowerCase()) {
                       case "latitude":
                       case "lat":
                           latName = v.getShortName();
                           latVar = v;
                           continue;
                       case "longitude":
                       case "lon":
                           lonName = v.getShortName();
                           lonVar = v;
                           continue;
                       case "time":
                       case "ocean_time":
                           timeName = v.getShortName();
                           timeVar = v;
                           continue;
                       case "depth":
                           depthName = v.getShortName();
                           depthVar = v;
                           continue;
                       default:
                           break;
                   }
               }
           }

           // Get the latitude and longitude Variables.
           Group navDataGroup = dataFile.findGroup("navigation_data");
           if (latVar == null) {
               searchPair = null;
               if (navDataGroup != null) {
                   Variable varL = NetCDFUtils.findVariableForGroup(navDataGroup, null, "latitude", "lat");
                   if (varL == null)
                       varL = NetCDFUtils.findVariableWithAttributeForGroup(dataFile, navDataGroup, null,
                               NetCDFUtils.NETCDF_ATT_STANDARD_NAME, "latitude", "lat");
                   if (varL != null)
                       searchPair = new Pair<String, Variable>(varL.getShortName(), varL);
               }
               if (searchPair == null)
                   searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst,
                           "latitude", "lat");

               if (searchPair == null) {
                   // Last try
                   for (String name : dimVars.keySet()) {
                       Variable v = dimVars.get(name);
                       if (v == null)
                           continue;
                       String unitsStr = v.getUnitsString();
                       if (unitsStr != null && (NetCDFUtils.NETCDF_DEGREES_NORTH.equalsIgnoreCase(unitsStr)
                               || NetCDFUtils.NETCDF_DEGREE_NORTH.equalsIgnoreCase(unitsStr))) {
                           searchPair = new Pair<String, Variable>(v.getShortName(), v);
                           break;
                       }
                   }
               }

               latName = searchPair == null ? null : searchPair.first();
               latVar = searchPair == null ? null : searchPair.second();
           }

           if (lonVar == null) {
               searchPair = null;
               if (navDataGroup != null) {
                   Variable varL = NetCDFUtils.findVariableForGroup(navDataGroup, null, "longitude", "lon");
                   if (varL == null)
                       varL = NetCDFUtils.findVariableWithAttributeForGroup(dataFile, navDataGroup, null,
                               NetCDFUtils.NETCDF_ATT_STANDARD_NAME, "longitude", "lon");
                   if (varL != null)
                       searchPair = new Pair<String, Variable>(varL.getShortName(), varL);
               }
               if (searchPair == null)
                   searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst,
                           "longitude", "lon");

               if (searchPair == null) {
                   // Last try
                   for (String name : dimVars.keySet()) {
                       Variable v = dimVars.get(name);
                       if (v == null)
                           continue;
                       String unitsStr = v.getUnitsString();
                       if (unitsStr != null && (NetCDFUtils.NETCDF_DEGREES_EAST.equalsIgnoreCase(unitsStr))
                               || NetCDFUtils.NETCDF_DEGREE_EAST.equalsIgnoreCase(unitsStr)) {
                           searchPair = new Pair<String, Variable>(v.getShortName(), v);
                           break;
                       }
                   }
               }

               lonName = searchPair == null ? null : searchPair.first();
               lonVar = searchPair == null ? null : searchPair.second();
           }

           if (timeVar == null) {
               searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst, "time", "ocean_time");
               timeName = searchPair == null ? null : searchPair.first();
               timeVar = searchPair == null ? null : searchPair.second();
           }

           if (depthVar == null) {
               // If varName is already depth, no need to use it
               searchPair = "depth".equalsIgnoreCase(varName) ? null : NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst, "depth");
               depthName = searchPair == null ? null : searchPair.first();
               depthVar = searchPair == null ? null : searchPair.second();
           }

           if (latVar == null || lonVar == null) {
               NeptusLog.pub().debug(String.format("Variable %s IS NOT georeference in data field %s", varName, fileName));
               return null;
           }

           // Get the lat/lon data from the file.
           Array latArray;  // ArrayFloat.D?
           Array lonArray;  // ArrayFloat.D?
           Array timeArray; //ArrayFloat.D?
           Array depthArray; //ArrayFloat.D?
           Array vArray;    // ArrayFloat.D?

           latArray = latVar.read();
           lonArray = lonVar.read();
           timeArray = timeVar != null ? timeVar.read() : null;
           depthArray = depthVar != null ? depthVar.read() : null;
           vArray = vVar.read();

           double[] multAndOffset = timeVar != null ? NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName) : null;
           double timeMultiplier = timeVar != null ? multAndOffset[0] : 1;
           double timeOffset = timeVar != null ? multAndOffset[1] : 0;

           Info info = createInfoBase(vVar);
           info.fileName = fileName;

           // Gradient calc
           final boolean calculateGradient = false;
//           if (info.type == Type.GEO_2D)
//               calculateGradient = true;
           LongAccumulator xyGrad3DimCounter = new LongAccumulator((o, i) -> i, 0);
           ArrayList<GenericDataPoint> gradBuffer = new ArrayList<>();
           int[] gradShape = null;
           DoubleAccumulator minGradient = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                   Double.MAX_VALUE);
           DoubleAccumulator maxGradient = new DoubleAccumulator((o, n) -> Double.compare(n, o) > 0 ? n : o,
                   Double.MIN_VALUE);
           DoubleAccumulator minLonXDelta = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                   Double.MAX_VALUE);
           DoubleAccumulator minLatYDelta = new DoubleAccumulator((o, n) -> Double.compare(n, o) < 0 ? n : o,
                   Double.MAX_VALUE);

           LongAccumulator avgTime = new LongAccumulator((c, v) -> c == -1 ? v : (long)((c + v) / 2.0), -1);

           // Let us process
           Instant timeStart = Instant.now();
           NeptusLog.pub().warn(String.format("Start processing metadata for %s.", varName));
           int elemTotal = -1;
           final AtomicInteger elemCounter = new AtomicInteger(-1);
           final AtomicInteger elemCounterAccepted = new AtomicInteger(-1);
           try {
               double varFillValue = NetCDFUtils.findFillValue(vVar);
               Pair<Double, Double> varValidRange = NetCDFUtils.findValidRange(vVar);
               Pair<Double, Double> varScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(vVar);

               double latFillValue = NetCDFUtils.findFillValue(latVar);
               Pair<Double, Double> latValidRange = NetCDFUtils.findValidRange(latVar);
               Pair<Double, Double> latScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(latVar);

               double lonFillValue = NetCDFUtils.findFillValue(lonVar);
               Pair<Double, Double> lonValidRange = NetCDFUtils.findValidRange(lonVar);
               Pair<Double, Double> lonScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(lonVar);

               double depthFillValue = depthVar != null ? NetCDFUtils.findFillValue(depthVar) : 0;
               Pair<Double, Double> depthValidRange = depthVar != null ? NetCDFUtils.findValidRange(depthVar)
                       : new Pair<Double, Double>(0., 0.);
               Pair<Double, Double> depthScaleFactorAndAddOffset = depthVar != null
                       ? NetCDFUtils.findScaleFactorAnfAddOffset(depthVar)
                       : new Pair<Double, Double>(1., 0.);

               int[] shape = vVar.getShape();
               int[] counterIdx = new int[shape.length];
               Arrays.fill(counterIdx, 0);

               // Gradient calc
               switch (info.type) {
                   case GEO_TRAJECTORY:
                       info.sizeXY = Arrays.copyOf(shape, shape.length);
                       gradShape = Arrays.copyOfRange(shape, shape.length - 1, shape.length);
                       for (int j = 0; j < gradShape[gradShape.length - 1]; j++)
                           gradBuffer.add(null);
                       Arrays.fill(gradShape, -1);
                       break;
                   case GEO_2D:
                       info.sizeXY = Arrays.copyOfRange(shape, shape.length - 2, shape.length);
                       gradShape = Arrays.copyOfRange(shape, shape.length - 2, shape.length);
                       for (int j = 0; j < gradShape[gradShape.length - 1]; j++)
                           gradBuffer.add(null);
                       Arrays.fill(gradShape, -1);
                       break;
                   case UNKNOWN:
                   default:
                       break;
               }


               // The null values are ignored
               final Map<String, Integer> timeCollumsIndexMap = timeVar == null ? new HashMap<>()
                       : NetCDFUtils.getIndexesForVar(dimStr, timeVar.getDimensionsString().split(" "));
               final Map<String, Integer> latCollumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr,
                       latVar.getDimensionsString().split(" "));
               final Map<String, Integer> lonCollumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr,
                       lonVar.getDimensionsString().split(" "));
               final Map<String, Integer> depthCollumsIndexMap = depthVar == null ? new HashMap<>()
                       : NetCDFUtils.getIndexesForVar(dimStr, depthVar.getDimensionsString().split(" "));

               if (timeCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                   timeCollumsIndexMap.clear();
               if (latCollumsIndexMap.values().stream().anyMatch(i -> i < 0)) {
                   NetCDFUtils.getMissingIndexesForVarTryMatchDimSize(latCollumsIndexMap,
                           vVar.getDimensions(), latVar.getDimensions());
                   if (latCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                       latCollumsIndexMap.clear();
               }
               if (lonCollumsIndexMap.values().stream().anyMatch(i -> i < 0)) {
                   NetCDFUtils.getMissingIndexesForVarTryMatchDimSize(lonCollumsIndexMap,
                           vVar.getDimensions(), lonVar.getDimensions());
                   if (lonCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                       lonCollumsIndexMap.clear();
               }
               if (depthCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                   depthCollumsIndexMap.clear();

               Instant timeFinish = Instant.now();
               long timeElapsed = Duration.between(timeStart, timeFinish).toMillis();
               NeptusLog.pub().warn(String.format("End processing metadata for %s (took %s).", varName,
                       DateTimeUtil.milliSecondsToFormatedString(timeElapsed)));
               NeptusLog.pub().warn(String.format("Start processing values for %s.", varName));

               final int[] gradShapeFinal = gradShape;
               final Variable depthVarFinal = depthVar;
               elemTotal = Arrays.stream(shape).reduce(1, (x, y) -> x * y);
               elemCounter.set(0);
               elemCounterAccepted.set(0);

               AtomicReference<int[]> counterAtomicRef = new AtomicReference<>(counterIdx);
               
               dataDp = IntStream.range(0, elemTotal)
                   .parallel()
                   .mapToObj(
                       (i) -> {
                       int[] counter = counterAtomicRef.getAndUpdate((c) -> NetCDFUtils.advanceLoopCounter(shape, Arrays.copyOf(c, c.length)));
                       
                       elemCounter.addAndGet(1);
                       Instant timeEachLoopStart = Instant.now();

                       Date dateValue = null;
                       Date[] timeVals = !timeCollumsIndexMap.isEmpty()
                               ? NetCDFUtils.getTimeValues(timeArray, buildCounterFrom(counter, timeCollumsIndexMap),
                                       timeMultiplier, timeOffset, fromDate.get(), toDate.get(), ignoreDateLimitToLoad, dateLimit)
                               : null;

                       if (timeVals == null)
                           timeVals = NetCDFUtils.getTimeValuesByGlobalAttributes(dataFile, fromDate.get(), toDate.get(),
                                   ignoreDateLimitToLoad, dateLimit);

                       if (timeVals == null)
                           timeVals = NetCDFUtils.getDatesAndDateLimits(new Date(0), fromDate.get(), toDate.get());

                       dateValue = timeVals[0];
                       fromDate.accumulateAndGet(timeVals[1], (c, v) -> {
                           if (c == null)
                               return v;
                           else if (v.before(c))
                               return v;
                           return c;
                       });
                       toDate.accumulateAndGet(timeVals[2], (c, v) -> {
                           if (c == null)
                               return v;
                           else if (v.after(c))
                               return v;
                           return c;
                       });

                       double lat = latArray.getDouble(buildIndexFrom(latArray, counter, latCollumsIndexMap));
                       double lon = lonArray.getDouble(buildIndexFrom(lonArray, counter, lonCollumsIndexMap));

                       if (!NetCDFUtils.isValueValid(lat, latFillValue, latValidRange)
                               || !NetCDFUtils.isValueValid(lon, lonFillValue, lonValidRange)) {
//                           NeptusLog.pub().debug(
//                                   String.format("While processing %s found invalid values for lat or lon!", varName));
                           fillGradient(calculateGradient, gradBuffer, gradShapeFinal, xyGrad3DimCounter, minGradient,
                                   maxGradient, minLonXDelta, minLatYDelta, counter, null);

//                           Instant timeEachLoopFinish = Instant.now();
//                           long timeEachLoopElapsed = Duration.between(timeEachLoopStart, timeEachLoopFinish).toNanos();
//                           NeptusLog.pub().warn(String.format("Loop counter %s processing for %s (took %s ns).", Arrays.toString(counter), varName,
//                                   timeEachLoopElapsed));

                           // avgTime.accumulate(Duration.between(timeEachLoopStart, Instant.now()).toNanos());
                           return null;
                       }

                       lat = lat * latScaleFactorAndAddOffset.first() + latScaleFactorAndAddOffset.second();
                       lon = lon * lonScaleFactorAndAddOffset.first() + lonScaleFactorAndAddOffset.second();
                       lat = AngleUtils.nomalizeAngleDegrees180(lat);
                       lon = AngleUtils.nomalizeAngleDegrees180(lon);

                       double depth = !depthCollumsIndexMap.isEmpty()
                               ? depthArray.getDouble(buildIndexFrom(depthArray, counter, depthCollumsIndexMap))
                               : Double.NaN;
                       if (!Double.isNaN(depth) && NetCDFUtils.isValueValid(depth, depthFillValue, depthValidRange)) {
                           depth = depth * depthScaleFactorAndAddOffset.first() + depthScaleFactorAndAddOffset.second();
                           depth = NetCDFUnitsUtils.getValueForMetterFromTempUnits(depth, depthVarFinal.getUnitsString());
                       }

                       // Check limits passed
                       boolean checkLimitsLatOk = true;
                       boolean checkLimitsLonOk = true;
                       boolean checkLimitsDepthOk = true;
                       if (latDegMinMax != null
                               && (Double.isFinite(latDegMinMax.first()) && Double.compare(lat, latDegMinMax.first()) < 0
                                       || Double.isFinite(latDegMinMax.second())
                                               && Double.compare(lat, latDegMinMax.second()) > 0))
                           checkLimitsLatOk = false;
                       if (lonDegMinMax != null
                               && (Double.isFinite(lonDegMinMax.first()) && Double.compare(lon, lonDegMinMax.first()) < 0
                                       || Double.isFinite(lonDegMinMax.second())
                                               && Double.compare(lon, lonDegMinMax.second()) > 0))
                           checkLimitsLonOk = false;
                       if (Double.isFinite(depth) && depthMinMax != null
                               && (Double.isFinite(depthMinMax.first()) && Double.compare(depth, depthMinMax.first()) < 0
                                       || Double.isFinite(depthMinMax.second())
                                               && Double.compare(depth, depthMinMax.second()) > 0))
                           checkLimitsDepthOk = false;
                       if (!checkLimitsLatOk || !checkLimitsLonOk || !checkLimitsDepthOk) {
//                           NeptusLog.pub().debug(String.format(
//                                   "While processing %s found a valid value outside passed limits (lat:%s, lon:%s, depth:%s)!",
//                                   varName, checkLimitsLatOk ? "ok" : "rejected", checkLimitsLonOk ? "ok" : "rejected",
//                                   checkLimitsDepthOk ? "ok" : "rejected"));
                           fillGradient(calculateGradient, gradBuffer, gradShapeFinal, xyGrad3DimCounter, minGradient,
                                   maxGradient, minLonXDelta, minLatYDelta, counter, null);

                           avgTime.accumulate(Duration.between(timeEachLoopStart, Instant.now()).toNanos());
                           return null;
                       }

//                       if (!checkLimitsDepthOk)
//                           depth = Double.NaN;

                       Index index = vArray.getIndex();
                       index.set(counter);

                       double v = vArray.getDouble(index);

                       GenericDataPoint ret = null;
                       
                       if (NetCDFUtils.isValueValid(v, varFillValue, varValidRange)) {
                           GenericDataPoint dp = new GenericDataPoint(lat, lon);
                           dp.setInfo(info);

                           v = v * varScaleFactorAndAddOffset.first() + varScaleFactorAndAddOffset.second();
                           if (info.scalarOrLogPreference == ScalarOrLogPreference.LOG10)
                               v = Math.pow(10, v); // let us unlog

                           // Doing nothing with units, just using what it is
                           // v = NetCDFUnitsUtils.getValueForMetterFromTempUnits(v, vUnits);

                           dp.setValue(v);
                           if (dp.getInfo().minVal == Double.MIN_VALUE || v < dp.getInfo().minVal)
                               dp.getInfo().minVal = v;
                           if (dp.getInfo().maxVal == Double.MAX_VALUE || v > dp.getInfo().maxVal)
                               dp.getInfo().maxVal = v;

                           dp.setDateUTC(dateValue);
                           if (dp.getInfo().minDate.getTime() == 0 || dp.getInfo().minDate.after(dateValue))
                               dp.getInfo().minDate = dateValue;
                           if (dp.getInfo().maxDate.getTime() == 0 || dp.getInfo().maxDate.before(dateValue))
                               dp.getInfo().maxDate = dateValue;

                           dp.setDepth(depth); // See better this!!
                           if (Double.isFinite(depth)) {
                               if (!Double.isFinite(dp.getInfo().minDepth) || dp.getInfo().minDepth == Double.MIN_VALUE
                                       || depth < dp.getInfo().minDepth)
                                   dp.getInfo().minDepth = depth;
                               if (!Double.isFinite(dp.getInfo().maxDepth) || dp.getInfo().maxDepth == Double.MAX_VALUE
                                       || depth > dp.getInfo().maxDepth)
                                   dp.getInfo().maxDepth = depth;
                           }

                           switch (info.type) {
                               case GEO_TRAJECTORY:
                                   dp.setIndexesXY(Arrays.copyOf(counter, counter.length));
                                   break;
                               case GEO_2D:
                                   dp.setIndexesXY(Arrays.copyOfRange(counter, counter.length - 2, counter.length));
                                   break;
                               case UNKNOWN:
                               default:
                                   break;
                           }

                           fillGradient(calculateGradient, gradBuffer, gradShapeFinal, xyGrad3DimCounter, minGradient,
                                   maxGradient, minLonXDelta, minLatYDelta, counter, dp);
                           dp.getHistoricalData().add(dp.getACopyWithoutHistory());
                           ret = dp;
                       }
                       else {
                           fillGradient(calculateGradient, gradBuffer, gradShapeFinal, xyGrad3DimCounter, minGradient,
                                   maxGradient, minLonXDelta, minLatYDelta, counter, null);
                       }

//                       Instant timeEachLoopFinish = Instant.now();
//                       long timeEachLoopElapsed = Duration.between(timeEachLoopStart, timeEachLoopFinish).toNanos();
//                       NeptusLog.pub().warn(String.format("Loop counter %s processing for %s (took %s ns).", Arrays.toString(counter), varName,
//                               timeEachLoopElapsed));
                       avgTime.accumulate(Duration.between(timeEachLoopStart, Instant.now()).toNanos());
                       elemCounterAccepted.addAndGet(1);
                       
                       return ret;
                   })
                   .filter((dp) -> dp != null)
//                   .parallel()
                   .collect(
                       // suplier
                       () -> new HashMap<String, GenericDataPoint>(),
                       // accumulator
                       (r1, dp) -> {
                           if (dp == null)
                               return;
                           if (r1.isEmpty()) {
                               r1.put(dp.getId(), dp);
                               return;
                           }
                           
                           GenericDataPoint dpo = r1.get(dp.getId());
                           if (dpo == null) {
                               r1.put(dp.getId(), dp);
                               return;
                           }
                           
                           ArrayList<GenericDataPoint> lst = dpo.getHistoricalData();
                           boolean alreadyIn = lst.parallelStream().anyMatch((tmpDp) -> {
                               // Check also depth and see if no time
                               return (tmpDp.getDateUTC().equals(dp.getDateUTC()) && tmpDp.getDepth() == dp.getDepth());
                           });

                           if (!alreadyIn) {
                               if (Double.isNaN(dpo.getGradientValue()))
                                   dpo.setGradientValue(dp.getGradientValue());
                               dpo.getHistoricalData().add(dp.getACopyWithoutHistory());
                           }
                       }, 
                       //combiner
                       (r1, r2) -> {
                           r2.keySet().parallelStream().forEach((k) -> {
                               GenericDataPoint dp = r2.get(k);
                               GenericDataPoint dpo = r1.get(k);
                               if (dpo == null) {
                                   r1.put(dp.getId(), dp);
                                   return;
                               }
                               
                               for (GenericDataPoint dph : dp.getHistoricalData()) {
                                   ArrayList<GenericDataPoint> lst = dpo.getHistoricalData();
                                   boolean alreadyIn = lst.parallelStream().anyMatch((tmpDp) -> {
                                       // Check also depth and see if no time
                                       return (tmpDp.getDateUTC().equals(dph.getDateUTC()) && tmpDp.getDepth() == dph.getDepth());
                                   });

                                   if (!alreadyIn)
                                       dpo.getHistoricalData().add(dph);
                               }
                           });
                       });
               
           }
           catch (Exception e) {
               e.printStackTrace();
           }
           finally {
               Instant timeFinish = Instant.now();
               long timeElapsed = Duration.between(timeStart, timeFinish).toMillis();
               NeptusLog.pub().warn(String.format("End processing %s (took %s) (mean per read %s us) [total: %s | proc: %s | accep: %s].",
                       varName,
                       DateTimeUtil.milliSecondsToFormatedString(timeElapsed), avgTime.get() / 1E3,
                       elemTotal, elemCounter.get(), elemCounterAccepted.get()));
           }

           // Gradient calculation
           if (minGradient.doubleValue() < Double.MAX_VALUE && maxGradient.doubleValue() > Double.MIN_VALUE) {
               info.minGradient = minGradient.doubleValue();
               info.maxGradient = maxGradient.doubleValue();
               info.validGradientData = true;
           }
       }
       catch (Exception e) {
           e.printStackTrace();
           return null;
       }
       finally {
           NeptusLog.pub().info("Ending processing " + varName + " netCDF file '" + fileName
                   + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
       }
       return dataDp;
   }

    /**
     * @param calculateGradient
     * @param gradBuffer
     * @param gradShape
     * @param xyGrad3DimCounter
     * @param minGradient
     * @param maxGradient
     * @param minLonXDelta
     * @param minLatYDelta
     * @param counter
     * @param dpY
     */
    private static void fillGradient(boolean calculateGradient, ArrayList<GenericDataPoint> gradBuffer, int[] gradShape,
            LongAccumulator xyGrad3DimCounter, DoubleAccumulator minGradient, DoubleAccumulator maxGradient,
            DoubleAccumulator minLonXDelta, DoubleAccumulator minLatYDelta, int[] counter, GenericDataPoint dpY) {
        if (calculateGradient) {
            if (counter.length > 2) {
                int c = counter[counter.length - 3];
                if (c != xyGrad3DimCounter.intValue()) {
                    // Clear points buffer
                    Collections.fill(gradBuffer, null);
                }
                xyGrad3DimCounter.accumulate(c);
            }

            GenericDataPoint dpUpX = gradBuffer.get(counter[counter.length -1]);
            if (dpUpX != null) {
                GenericDataPoint dpUpXNext = null;
                if (counter[counter.length -1] + 1 < gradShape[gradShape.length -1])
                    dpUpXNext = gradBuffer.get(counter[counter.length -1] + 1);
                double vxNext = dpUpXNext != null ? dpUpXNext.getValue() : Double.NaN;
                double vyNext = dpY != null ? dpY.getValue() : Double.NaN;
                double dx = Double.isFinite(vxNext) ? vxNext - dpUpX.getValue() : 0;
                double dy = Double.isFinite(vyNext) ? vyNext - dpUpX.getValue() : 0;
                if (Double.isFinite(vxNext) || Double.isFinite(vyNext)) {
                    double gradient = Math.sqrt(dx * dx + dy * dy);
                    minGradient.accumulate(gradient);
                    maxGradient.accumulate(gradient);
                    dpUpX.setGradientValue(gradient);
                }

                if (dpUpXNext != null) {
                    double dist = Math.sqrt(Math.pow(dpUpX.getLat() - dpUpXNext.getLat(), 2)
                            + Math.pow(dpUpX.getLon() - dpUpX.getLon(), 2));
                    double angle = AngleUtils.calcAngle(dpUpX.getLon(), dpUpX.getLat(), dpUpXNext.getLon(),
                            dpUpXNext.getLat());
                    double distX = dist * Math.sin(angle);
                    minLonXDelta.accumulate(distX);
                    double distY = dist * Math.cos(angle);
                    minLatYDelta.accumulate(distY);
                }
            }
        }
        gradBuffer.set(counter[counter.length -1], dpY);
    }

    /**
     * @param varArray
     * @param counter
     * @param collumsIndexMap
     * @return
     */
    private static Index buildIndexFrom(Array varArray, int[] counter, Map<String, Integer> collumsIndexMap) {
        int[] idxCounter = buildCounterFrom(counter, collumsIndexMap);
        Index ret = varArray.getIndex();
        try {
            ret.set(idxCounter);
        }
        catch (Exception e) {
            System.err.println(">>  " + Arrays.toString(idxCounter) + "  " + Arrays.toString(varArray.getShape()));
            e.printStackTrace();
            throw e;
        }
        return ret;
    }

    /**
     * @param counter
     * @param collumsIndexMap
     * @return
     */
    private static int[] buildCounterFrom(int[] counter, Map<String, Integer> collumsIndexMap) {
        int[] ret = new int[collumsIndexMap.size()];
        Iterator<Integer> ci = collumsIndexMap.values().iterator();
        for (int i = 0; i < ret.length; i++)
            ret[i] = counter[ci.next()];
        return ret;
    }

    /**
     * @param vVar
     * @return
     */
    public static Info createInfoBase(Variable vVar) {
        Info info = new Info();
        info.name = vVar.getShortName();
        Attribute vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_LONG_NAME);
        info.fullName = vAtt == null ? vVar.getFullName() : vAtt.getStringValue();
        vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
        info.standardName = vAtt == null ? "" : vAtt.getStringValue();

        info.unit = vVar.getUnitsString();
        if (info.unit == null) {
            info.unit = "";
        }
        else {
            Pattern pattern = Pattern.compile("^ *?lo?g\\((.*)\\) *?$");
            Matcher matcher = pattern.matcher(info.unit);
            if (matcher.matches()) {
                info.unit = matcher.group(1);
                info.scalarOrLogPreference = ScalarOrLogPreference.LOG10;
            }
        }

        vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_COMMENT);
        info.comment = vAtt == null ? "" : vAtt.getStringValue();

        if (vVar.getDimensions().size() == 1)
            info.type = Type.GEO_TRAJECTORY;
        else if (vVar.getDimensions().size() >= 2)
            info.type = Type.GEO_2D;
        else
            info.type = Type.UNKNOWN;

        return info;
    }

    /**
     * Deletes the netCDF unzipped file if any. The file passed has to be of the form "*.nc.gz".
     * 
     * @param fx
     */
    public static void deleteNetCDFUnzippedFile(File fx) {
        // Deleting the unzipped file
        String absPath = fx.getAbsolutePath();
        if (absPath.matches(".+\\.nc\\.gz$")) {
            absPath = absPath.replaceAll("\\.nc\\.gz$", ".nc");
        }
        else if (absPath.matches(".+\\.nc\\.zip$")) {
            absPath = absPath.replaceAll("\\.nc\\.zip$", ".nc");
        }
        else {
            absPath = null;
        }

        if (absPath != null) {
            File unzipedFile = new File(absPath);
            if (unzipedFile.exists()) {
                try {
                    FileUtils.forceDelete(unzipedFile);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Shows a {@link JFileChooser} for the operator to choose a netCDF file.
     * 
     * @param parentWindow
     * @param recentFolder
     * @return The selected file or null if canceled.
     */
    public static <W extends Window> File showChooseANetCDFToOpen(W parentWindow, File recentFolder) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileView(new NeptusFileView());
        chooser.setCurrentDirectory(recentFolder);
        chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("netCDF files"), "nc", "nc.gz"));
        chooser.setApproveButtonText(I18n.text("Open file"));
        chooser.showOpenDialog(parentWindow);
        if (chooser.getSelectedFile() == null)
            return null;

        return chooser.getSelectedFile();
    }

    /**
     * Shows a {@link JFileChooser} for the operator to choose a netCDF file.
     * 
     * @param parentWindow
     * @param recentFolder
     * @return The selected files or null if canceled.
     */
    public static <W extends Window> File[] showChooseANetCDFMultipleToOpen(W parentWindow, File recentFolder) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileView(new NeptusFileView());
        chooser.setCurrentDirectory(recentFolder);
        chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("netCDF or XYZ files"), "nc", "nc.gz", "xyz", "xyz.gz"));
        chooser.setApproveButtonText(I18n.text("Open file"));
        chooser.setMultiSelectionEnabled(true);
        chooser.showOpenDialog(parentWindow);
        if (chooser.getSelectedFiles() == null || chooser.getSelectedFiles().length == 0)
            return null;

        return chooser.getSelectedFiles();
    }

    /**
     * Shows a GUI for the operator to choose a variable to use. That variable needs to use lat, lon.
     * 
     * @param fileName
     * @param dataFile
     * @param parentWindow
     * @return The variable chosen or null if cancelled.
     */
    public static <W extends Window> Variable showChooseVar(String fileName, NetcdfFile dataFile, W parentWindow) {
        List<Dimension> dimsRoot = dataFile.getDimensions();

        Map<String, Group> groupList = new HashMap<>();

        Map<String, Variable> varToConsider = NetCDFUtils.getVariables(dataFile, 1); // Choose variables with dim > 2

        for (String varStr : varToConsider.keySet().toArray(new String[varToConsider.size()])) {
            boolean isRemoved = false;

            // Remove vars that are root dimensions
            for (Dimension dimStr : dimsRoot) {
                if (varStr.equalsIgnoreCase(dimStr.getShortName())) {
                    varToConsider.remove(varStr);
                    isRemoved = true;
                    break;
                }
            }

            if (isRemoved)
                continue;

            // Collect groups
            Variable var = varToConsider.get(varStr);
            Group grp = var.getGroup();
            if (grp != null && !grp.getShortName().isEmpty() && !groupList.containsKey(grp.getShortName()))
                groupList.put(grp.getShortName(), grp);

            // Remove if an axis attribute
            if (var.findAttribute(NetCDFUtils.NETCDF_ATT_AXIS) != null) {
                varToConsider.remove(varStr);
                continue;
            }

            for (String sn : new String[] {"lat", "latitude", "lon", "longitude", "time", "lat_bnds", "lon_bnds"}) {
                if (sn.equalsIgnoreCase(var.getShortName())) {
                    varToConsider.remove(varStr);
                    isRemoved = true;
                    break;
                }
            }

            Attribute stdName = var.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
            if (stdName != null) {
                for (String sn : new String[] {"latitude", "longitude", "time"}) {
                    if (sn.equalsIgnoreCase(stdName.getStringValue())) {
                        varToConsider.remove(varStr);
                        isRemoved = true;
                        break;
                    }
                }
            }
        }

        boolean geoVarGroupExist = groupList.keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase(NetCDFUtils.NETCDF_GRP_GEOPHYSICAL_DATA));
        if (geoVarGroupExist) {
            // Remove all that not on the group
            for (String varStr : varToConsider.keySet().toArray(new String[varToConsider.size()])) {
                Variable v = varToConsider.get(varStr);
                if (!NetCDFUtils.NETCDF_GRP_GEOPHYSICAL_DATA.equalsIgnoreCase(v.getGroup().getShortName()))
                    varToConsider.remove(varStr);
            }
        }

        if (varToConsider.isEmpty()) {
            GuiUtils.infoMessage(parentWindow, I18n.text("Error loading"), I18n.text("Missing variables in data"));
            return null;
        }

        // Removing the ones that don't have location info
        // TODO

        ArrayList<JLabel> choicesVarsLbl = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>(varToConsider.keySet());
        Collections.sort(keys, (e1, e2) -> Collator.getInstance().compare(varToConsider.get(e1).getFullName(),
                varToConsider.get(e2).getFullName()));
        for (String vName : keys) {
            Variable var = varToConsider.get(vName);
            StringBuilder sb = new StringBuilder("<html><b>");
            Info info = NetCDFLoader.createInfoBase(var);
            info.fileName = fileName;
            sb.append(vName);
            sb.append(" :: ");
            sb.append(info.fullName);
            sb.append("</b><br/>");
            sb.append("std='");
            sb.append(info.standardName);
            sb.append("'");
            sb.append("<br/>");
            sb.append("unit='");
            sb.append(info.unit);
            sb.append("'");
            sb.append("<br/>");
            sb.append("comment='");
            String cmt = StringUtils.wrapEveryNChars(info.comment, (short) 60);
            cmt = cmt.replaceAll("\n", "<br/>");
            sb.append(cmt);
            sb.append("'");
            sb.append("<html>");

            @SuppressWarnings("serial")
            JLabel l = new JLabel(vName) {
                @Override
                public String toString() {
                    return sb.toString();
                }
            };
            choicesVarsLbl.add(l);
        }
        if (choicesVarsLbl.isEmpty()) {
            GuiUtils.infoMessage(parentWindow, I18n.text("Info"),
                    I18n.textf("No valid variables in data with dimentions (%s)", "<time>; lat; lon; <depth>"));
            return null;
        }
        Object choiceOpt = JOptionPane.showInputDialog(parentWindow, I18n.text("Choose one of the vars"),
                I18n.textf("Chooser for %f", fileName), JOptionPane.QUESTION_MESSAGE, null,
                choicesVarsLbl.toArray(new JLabel[choicesVarsLbl.size()]), 0);

        return choiceOpt == null ? null : varToConsider.get(((JLabel) choiceOpt).getText());
    }

    /**
     * This method will load a varName into a {@link GenericNetCDFDataPainter} and fill in the properties of it.
     * 
     * This runs on a separated thread returning a {@link Future}.
     *
     * @param filePath
     * @param dataFile
     * @param varName
     * @param plotUniqueId
     * @param dateLimit If null no filter is applied
     * @param latDegMinMax If null no filter is applied
     * @param lonDegMinMax If null no filter is applied
     * @param depthMinMax If null no filter is applied
     * @return
     */
    public static Future<GenericNetCDFDataPainter> loadNetCDFPainterFor(String filePath, NetcdfFile dataFile,
            String varName, long plotUniqueId, Date dateLimit, Pair<Double, Double> latDegMinMax,
            Pair<Double, Double> lonDegMinMax, Pair<Double, Double> depthMinMax) {
        FutureTask<GenericNetCDFDataPainter> fTask = new FutureTask<>(() -> {
            try {
                Map<String, GenericDataPoint> dataPoints = NetCDFLoader.processFileForVariable(dataFile, varName,
                        dateLimit, latDegMinMax, lonDegMinMax, depthMinMax);
                GenericNetCDFDataPainter gDataViz = new GenericNetCDFDataPainter(plotUniqueId, dataPoints);
                gDataViz.setNetCDFFile(filePath);
                return gDataViz;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage(), e);
                return null;
            }
        });
        Thread t = new Thread(() -> fTask.run(), NetCDFLoader.class.getSimpleName() + ":: loadNetCDFPainterFor " + varName);
        t.setDaemon(true);
        t.start();
        return fTask;
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {

        if (false) {
            NetcdfFile dataFile = null;

            String fileName = "../nrt_global_allsat_phy_l4_latest.nc.gz";
            dataFile = NetcdfFile.open(fileName, null);

            List<Variable> vars = dataFile.getVariables();
            for (Variable v : vars) {
                System.out.println(String.format("'%s'  '%s' '%s' '%d' '%s'  '%s'", v.getShortName(), v.getFullName(), v.getDescription(), v.getDimensions().size(),
                        v.getDimensionsString(), v.getRanges()));
            }
            System.out.println();

            Map<String, GenericDataPoint> data = processFileForVariable(dataFile, "sla", null, null, null, null);
            // data.keySet().stream().forEachOrdered(k -> System.out.println(data.get(k)));
            String[] keys = data.keySet().toArray(new String[0]);
            for (int i = 0; i < Math.min(10, data.size()); i++) {
                System.out.println(keys[i] + " -> " + data.get(keys[i]));
            }
        }

        if (true) {

            Pattern pattern = Pattern.compile("^ *?lo?g\\((.*)\\) *?$");
            Matcher matcher = pattern.matcher("log(re mg.m-3)");
            if (matcher.matches()) {
                System.out.println(matcher.group(1));
            }

            NetcdfFile dataFile = null;

            String baseFolder = "../NetCDF/";
//            baseFolder = "../../../netCDF/";

//            String fileName = baseFolder + "A2018116215500.L2_LAC.S3160_SOI.nc";
            String fileName = baseFolder + "nrt_global_allsat_phy_l4_latest.nc.gz";
            dataFile = NetcdfFile.open(fileName, null);

            List<Dimension> dims = dataFile.getDimensions();
            for (Dimension d : dims) {
                System.out.println("dim " + d.getShortName() + "   " + d.getLength());
            }

           Group rootGroup = dataFile.getRootGroup();
           System.out.println(String.format("root group: ", rootGroup.getShortName()));

           List<Attribute> globalAtt = dataFile.getGlobalAttributes();
           for (Attribute att : globalAtt) {
               System.out.println(String.format("Global Att: %s == %s", att.getShortName(), att.getValues()));
           }

           System.out.println(dataFile.getDetailInfo());

           List<Variable> varsL = dataFile.getVariables();
           for (Variable v : varsL) {
                System.out.println(String.format("Variable: %s :: %s >> Group='%s'::'%s' ParentGroup='%s'  Dim='%s'::'%s'", v.getShortName(), v.getFullName(),
                        v.getGroup().getShortName(), v.getGroup().getFullName(), v.getParentGroup().getShortName(),
                        v.getDimensions(), v.getDimensionsString()));
                for (Attribute att : v.getAttributes()) {
                    System.out.println(String.format("     Var Att: %s == %s", att.getShortName(), att.getValues()));
                }
           }


           fileName = baseFolder + "A2018116215500.L2_LAC.S3160_SOI.nc";
           Variable choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));

           fileName = baseFolder + "nrt_global_allsat_phy_l4_latest.nc.gz";
           dataFile = NetcdfFile.open(fileName, null);
           choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));

           fileName = baseFolder + "20180416184057-MAR-L2P_GHRSST-SSTskin-SLSTRA-20180416204859-v02.0-fv01.0.nc";
           dataFile = NetcdfFile.open(fileName, null);
           choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));

           fileName = baseFolder + "marmenor_his.nc";
           dataFile = NetcdfFile.open(fileName, null);
           choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));

           fileName = baseFolder + "temperature_salinity_STF_10042018.nc";
           dataFile = NetcdfFile.open(fileName, null);
           choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));

        }
    }
}
