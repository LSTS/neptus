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
 * Mar 26, 2020
 */
package pt.lsts.neptus.plugins.envdisp.loader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.LongAccumulator;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Type;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.netcdf.NetCDFUtils;

/**
 * @author pdias
 *
 */
public class XyzLoader {
    public static final String XYZ_FILE_PATTERN = ".+\\.xyz((\\.gz)|(\\.bz2))?$";

    public static Info createInfoBase() {
        Info info = new Info();
        info.name = "z";
        info.fullName = "Z";
        info.standardName = info.fullName;
        info.unit = "m";
        info.comment = "";
        info.type = Type.GEO_TRAJECTORY;
        return info;
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
    public static final Map<String, GenericDataPoint> processFile(String filePath, InputStream dataFile,
            String varName, boolean lonLatZOrder, Date dateLimit,
             Pair<Double, Double> latDegMinMax, Pair<Double, Double> lonDegMinMax, Pair<Double, Double> depthMinMax) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        String fileName = filePath;
        
        NeptusLog.pub().info("Starting processing " + varName + " file '" + fileName + "'."
                + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

        Map<String, GenericDataPoint> dataDp = new LinkedHashMap<>();

        Date fromDate = null;
        Date toDate = null;
        
        try {
            Info info = createInfoBase();
            info.fileName = fileName;
            
            // Gradient calc
            boolean calculateGradient = false;
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
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(dataFile))) {
                int[] counter = new int[1];
                Arrays.fill(counter, 0);

                // Gradient calc
                switch (info.type) {
                    case GEO_TRAJECTORY:
//                        info.sizeXY = Arrays.copyOf(shape, shape.length);
//                        gradShape = Arrays.copyOfRange(shape, shape.length - 1, shape.length);
//                        for (int j = 0; j < gradShape[gradShape.length - 1]; j++)
//                            gradBuffer.add(null);
//                        Arrays.fill(gradShape, -1);
//                        break;
                    case GEO_2D:
//                        info.sizeXY = Arrays.copyOfRange(shape, shape.length - 2, shape.length);
//                        gradShape = Arrays.copyOfRange(shape, shape.length - 2, shape.length);
//                        for (int j = 0; j < gradShape[gradShape.length - 1]; j++)
//                            gradBuffer.add(null);
//                        Arrays.fill(gradShape, -1);
//                        break;
                    case UNKNOWN:
                    default:
                        break;
                }

                while (true) {
                    String line = reader.readLine();
                    if (line == null)
                        break;
                    if (line.trim().matches("[#%;/*].*"))
                        continue;
                    String[] tk = line.trim().split("[\t;, ]", 4);
                    if (tk.length < 3)
                        continue;

                    Date[] timeVals = NetCDFUtils.getDatesAndDateLimits(new Date(0), fromDate, toDate);
                    Date dateValue = timeVals[0];
                    fromDate = timeVals[1];
                    toDate = timeVals[2];

                    try {
                        double lat = Double.parseDouble(tk[lonLatZOrder ? 1 : 0]);
                        double lon = Double.parseDouble(tk[lonLatZOrder ? 0 : 1]);
                        double v = Double.parseDouble(tk[2]);

                        // Check limits passed
                        boolean checkLimitsLatOk = true;
                        boolean checkLimitsLonOk = true;
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

                        if (!checkLimitsLatOk || !checkLimitsLonOk) {
                            NeptusLog.pub().debug(String.format(
                                    "While processing %s found a valid value outside passed limits (lat:%s, lon:%s)!",
                                    varName, checkLimitsLatOk ? "ok" : "rejected", checkLimitsLonOk ? "ok" : "rejected"));
                            continue;
                        }

                        GenericDataPoint dp = new GenericDataPoint(lat, lon);
                        dp.setInfo(info);

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

                        ArrayList<GenericDataPoint> lst = dpo.getHistoricalData();
                        boolean alreadyIn = lst.parallelStream().anyMatch((tmpDp) -> {
                            // Check also depth and see if no time
                            return (tmpDp.getDateUTC().equals(dp.getDateUTC()) && tmpDp.getDepth() == dp.getDepth());
                        });

                        if (!alreadyIn)
                            dpo.getHistoricalData().add(dp);

                        counter[0]++;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                };
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
            NeptusLog.pub().info("Ending processing " + varName + " XYZ file '" + fileName
                    + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
        }
        return dataDp;
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
    public static Future<GenericNetCDFDataPainter> loadXyzPainterFor(String filePath, InputStream dataFile,
            String varName, boolean lonLatZOrder, long plotUniqueId, Date dateLimit, Pair<Double, Double> latDegMinMax,
            Pair<Double, Double> lonDegMinMax, Pair<Double, Double> depthMinMax) {
        FutureTask<GenericNetCDFDataPainter> fTask = new FutureTask<>(() -> {
            try {
                Map<String, GenericDataPoint> dataPoints = processFile(filePath, dataFile,
                        varName, lonLatZOrder,
                        dateLimit, latDegMinMax, lonDegMinMax, depthMinMax);
                GenericNetCDFDataPainter gDataViz = new GenericNetCDFDataPainter(plotUniqueId, dataPoints);
                gDataViz.getAdditionalParams().put("lonLatZOrder", Boolean.valueOf(lonLatZOrder).toString());
                gDataViz.setNetCDFFile(filePath);
                return gDataViz;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage(), e);
                return null;
            }
            finally {
                dataFile.close();
            }
        });
        Thread t = new Thread(() -> fTask.run(), NetCDFLoader.class.getSimpleName() + ":: loadXyzPainterFor " + varName);
        t.setDaemon(true);
        t.start();
        return fTask;
    }
}
