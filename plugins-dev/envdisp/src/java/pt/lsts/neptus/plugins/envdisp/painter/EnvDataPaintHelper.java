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
 * 23/04/2017
 */
package pt.lsts.neptus.plugins.envdisp.painter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.envdisp.datapoints.BaseDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.ChlorophyllDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info;
import pt.lsts.neptus.plugins.envdisp.datapoints.HFRadarDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.SLADataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.SSTDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.WavesDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.WindDataPoint;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.MovingAverage;
import pt.lsts.neptus.util.UnitsUtil;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;

/**
 * @author pdias
 *
 */
public class EnvDataPaintHelper {

    static final int OFFSET_REND_TXT_DATE_RANGES = 52;
    static final int OFFSET_REND_TXT_DATE_RANGES_DELTA = 15;
    
    private static final double MIN_INTERPOLATION_POINT = 2;

    static int filterUseLOD = 9;

    public enum PointPaintEnum {
        POINT,
        ARROW,
        BARB,
        INTERPOLATE
    }
    
    private EnvDataPaintHelper() {
    }

    static void debugOut(boolean showAsSystemOutOrLogDebug, Object message) {
        if (showAsSystemOutOrLogDebug)
            System.out.println(message);
        else
            NeptusLog.pub().debug(message);
    }

    static void debugOut(boolean showAsSystemOutOrLogDebug, Object message, Throwable t) {
        if (showAsSystemOutOrLogDebug) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.out.println(message + "\n" + sw.toString());
        }
        else {
            NeptusLog.pub().debug(message, t);
        }
    }

    /**
     * @param sPos
     * @param renderer
     * @param offScreenBufferPixel The off screen buffer that might exist
     * @return
     */
    static boolean isVisibleInRender(Point2D sPos, StateRenderer2D renderer, int offScreenBufferPixel) {
        Dimension rendDim = renderer.getSize();
        return isVisibleInRender(sPos, rendDim, offScreenBufferPixel);
    }

    /**
     * @param sPos
     * @param rendererSize
     * @param offScreenBufferPixel The off screen buffer that might exist
     * @return
     */
    static boolean isVisibleInRender(Point2D sPos, Dimension rendererSize, int offScreenBufferPixel) {
        Dimension rendDim = rendererSize;
        if (sPos.getX() < 0 - offScreenBufferPixel
                || sPos.getY() < 0 - offScreenBufferPixel)
            return false;
        else if (sPos.getX() > rendDim.getWidth() + offScreenBufferPixel
                || sPos.getY() > rendDim.getHeight() + offScreenBufferPixel)
            return false;
    
        return true;
    }

    /**
     * @param g
     * @param toDate
     * @param fromDate
     * @param rendererOffset
     * @param typeName
     */
    static void paintDatesRange(Graphics2D g, long toDate, long fromDate, int rendererOffset, String typeName,
            boolean showDataDebugLegend, Font font8Pt) {
        if (showDataDebugLegend) {
            String fromDateStr = fromDate < Long.MAX_VALUE ? new Date(fromDate).toString() : "-";
            String toDateStr = toDate > 0 ? new Date(toDate).toString() : "-";
            String txtMsg = String.format("%s data from '%s' till '%s'", typeName, fromDateStr, toDateStr);
            Graphics2D gt = (Graphics2D) g.create();
            gt.setFont(font8Pt);
            gt.setColor(Color.BLACK);
            gt.drawString(txtMsg, 10 + 1, rendererOffset + 1);
            gt.setColor(Color.WHITE);
            gt.drawString(txtMsg, 10, rendererOffset);
            gt.dispose();
        }
    }
    
    /**
     * @param rendererCalculator
     * @param g2
     * @param transparency
     * @param dateColorLimit
     * @param dateLimit
     * @param dataPointsCurrents
     * @param ignoreDateLimitToLoad
     * @param offScreenBufferPixel
     * @param colorMapCurrents
     * @param minCurrentCmS
     * @param maxCurrentCmS
     * @param showCurrentsLegend
     * @param showCurrentsLegendFromZoomLevel
     * @param font8Pt
     * @param showDataDebugLegend
     * @param abortIndicator
     */
    public static void paintHFRadarInGraphics(MapTileRendererCalculator rendererCalculator, Graphics2D g2,
            int transparency, Date dateColorLimit, Date dateLimit,
            HashMap<String, HFRadarDataPoint> dataPointsCurrents, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            ColorMap colorMapCurrents, double minCurrentCmS, double maxCurrentCmS, boolean showCurrentsLegend, int showCurrentsLegendFromZoomLevel,
            Font font8Pt, boolean showDataDebugLegend, AtomicBoolean abortIndicator) {

        paintWorkerInGraphics("Currents", rendererCalculator, g2, 
                dataPointsCurrents, EnvDataShapesHelper.ARROW_RADIUS, 
                offScreenBufferPixel, colorMapCurrents, minCurrentCmS, maxCurrentCmS, showCurrentsLegend, 
                showCurrentsLegendFromZoomLevel, font8Pt, showDataDebugLegend, 0,
                dp -> ignoreDateLimitToLoad || !dp.getDateUTC().before(dateLimit),
                dp -> dp.getAllDataValues(), 
                (vals, ovals) -> {
                    // speedCmS, headingDegrees, resolutionKm, info(String)
                    for (int i = 0; i < 3; i++) {
                        double v = (double) vals.get(i);
                        double o = (double) ovals.get(i);
                        double r = (v + o) / 2.;
                        vals.add(i, r);
                    }
                    String str = (String) vals.get(3); //mergeStrings((String) vals.get(3), (String) ovals.get(3));
                    vals.add(3, str);
                    return vals;
                },
                (pt, dataMap) -> {
                    Graphics2D gt = null;
                    try {
                        Pair<ArrayList<Object>, Date> pVal = dataMap.get(pt);
                        
                        double speedCmSV = (double) pVal.first().get(0);
                        double headingV = (double) pVal.first().get(1);
                        Date dateV = pVal.second();
                        
                        gt = (Graphics2D) g2.create();
                        gt.translate(pt.getX(), pt.getY());

                        Color color = Color.WHITE;
                        color = colorMapCurrents.getColor(speedCmSV / maxCurrentCmS);
                        if (dateV.before(dateColorLimit))
                            color = ColorUtils.setTransparencyToColor(color, transparency / 2);
                        else
                            color = ColorUtils.setTransparencyToColor(color, transparency);
                        gt.setColor(color);
                        double rot = Math.toRadians(-headingV + 90) - rendererCalculator.getRotation();
                        gt.rotate(rot);
                        gt.fill(EnvDataShapesHelper.arrow);
                        gt.rotate(-rot);
                        
                        if (showCurrentsLegend && rendererCalculator.getLevelOfDetail() >= showCurrentsLegendFromZoomLevel) {
                            gt.setFont(font8Pt);
                            gt.setColor(Color.WHITE);
                            gt.drawString(MathMiscUtils.round(speedCmSV, 1) + "cm/s", 10, 2);
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                    
                    if (gt != null)
                        gt.dispose();
               }, abortIndicator);
    }

    /**
     * @param rendererCalculator
     * @param g2
     * @param transparency
     * @param dateColorLimit
     * @param dateLimit
     * @param dataPointsSST
     * @param ignoreDateLimitToLoad
     * @param offScreenBufferPixel
     * @param colorMapSST
     * @param minSST
     * @param maxSST
     * @param showSSTLegend
     * @param showSSTLegendFromZoomLevel
     * @param font8Pt
     * @param showDataDebugLegend
     * @param abortIndicator
     */
    public static void paintSSTInGraphics(MapTileRendererCalculator rendererCalculator, Graphics2D g2, 
            int transparency, Date dateColorLimit, Date dateLimit,
            HashMap<String, SSTDataPoint> dataPointsSST, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            ColorMap colorMapSST, double minSST, double maxSST,
            boolean showSSTLegend, int showSSTLegendFromZoomLevel, Font font8Pt, boolean showDataDebugLegend, AtomicBoolean abortIndicator) {

        paintWorkerInGraphics("SST", rendererCalculator, g2, 
                dataPointsSST, EnvDataShapesHelper.CIRCLE_RADIUS,
                offScreenBufferPixel, colorMapSST, minSST, maxSST, showSSTLegend, 
                showSSTLegendFromZoomLevel, font8Pt, showDataDebugLegend, 1, 
                dp -> ignoreDateLimitToLoad || !dp.getDateUTC().before(dateLimit),
                dp -> dp.getAllDataValues(),
                (vals, ovals) -> {
                    // sst
                    double v = (double) vals.get(0);
                    double o = (double) ovals.get(0);
                    double r = (v + o) / 2.;
                    vals.add(0, r);
                    return vals;
                },
                (pt, dataMap) -> {
                    Graphics2D gt = null;
                    try {
                        Pair<ArrayList<Object>, Date> pVal = dataMap.get(pt);
                        double sst = (double) pVal.first().get(0);
                        gt = (Graphics2D) g2.create();
                        gt.translate(pt.getX(), pt.getY());
                        Color color = Color.WHITE;
                        color = colorMapSST.getColor((sst - minSST) / (maxSST - minSST));
                        if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                            color = ColorUtils.setTransparencyToColor(color, transparency / 2);
                        else
                            color = ColorUtils.setTransparencyToColor(color, transparency);
                        gt.setColor(color);
                        //gt.draw(EnvDataShapesHelper.circle);
                        gt.fill(EnvDataShapesHelper.rectangle);
                        
                        if (showSSTLegend && rendererCalculator.getLevelOfDetail() >= showSSTLegendFromZoomLevel) {
                            gt.setFont(font8Pt);
                            gt.setColor(Color.WHITE);
                            gt.drawString(MathMiscUtils.round(sst, 1) + "\u00B0C", -15, 15);
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                    
                    if (gt != null)
                        gt.dispose();
               }, abortIndicator);
    }

    /**
     * @param rendererCalculator
     * @param g2
     * @param transparency
     * @param dateColorLimit
     * @param dateLimit
     * @param dataPointsWind
     * @param ignoreDateLimitToLoad
     * @param offScreenBufferPixel
     * @param useColorMapForWind
     * @param colorMapWind
     * @param minWind
     * @param maxWind
     * @param font8Pt
     * @param showDataDebugLegend
     * @param abortIndicator
     */
    public static void paintWindInGraphics(MapTileRendererCalculator rendererCalculator, Graphics2D g2,
            int transparency, Date dateColorLimit, Date dateLimit,
            HashMap<String, WindDataPoint> dataPointsWind, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            boolean useColorMapForWind, ColorMap colorMapWind, double minWind, double maxWind,
            Font font8Pt, boolean showDataDebugLegend, AtomicBoolean abortIndicator) {

        paintWorkerInGraphics("Wind", rendererCalculator, g2, 
                dataPointsWind, EnvDataShapesHelper.WIND_BARB_RADIUS, 
                offScreenBufferPixel, colorMapWind, minWind, maxWind, false, 
                0, font8Pt, showDataDebugLegend, 2,
                dp -> ignoreDateLimitToLoad || !dp.getDateUTC().before(dateLimit),
                dp -> dp.getAllDataValues(), 
                (vals, ovals) -> {
                    // u, v
                    for (int i = 0; i < 2; i++) {
                        double v = (double) vals.get(i);
                        double o = (double) ovals.get(i);
                        double r = (v + o) / 2.;
                        vals.add(i, r);
                    }
                    return vals;
                },
                (pt, dataMap) -> {
                    Graphics2D gt = null;
                    try {
                        // u, v
                        Pair<ArrayList<Object>, Date> pVal = dataMap.get(pt);
                        double u = (double) pVal.first().get(0);
                        double v = (double) pVal.first().get(1);
                        double speedV = Math.sqrt(u * u +  v * v);
                        double headingV = AngleUtils.nomalizeAngleDegrees360(Math.toDegrees(Math.atan2(v, u)));
                        Date dateV = pVal.second();
                        
                        gt = (Graphics2D) g2.create();
                        gt.translate(pt.getX(), pt.getY());
        
                        Color color = Color.BLACK;
                        if (useColorMapForWind)
                            color = colorMapWind.getColor(speedV / maxWind);
                        if (dateV.before(dateColorLimit))
                            color = ColorUtils.setTransparencyToColor(color, transparency / 2);
                        else
                            color = ColorUtils.setTransparencyToColor(color, transparency);
                        gt.setColor(color);
                        
                        gt.rotate(Math.toRadians(headingV) - rendererCalculator.getRotation());
                        
                        double speedKnots = speedV * UnitsUtil.MS_TO_KNOT;
                        EnvDataShapesHelper.paintWindBarb(gt, speedKnots);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                    
                    if (gt != null)
                        gt.dispose();
               }, abortIndicator);
    }
    
    /**
     * @param rendererCalculator
     * @param g2
     * @param trasparency
     * @param dateColorLimit
     * @param dateLimit
     * @param dataPointsWaves
     * @param ignoreDateLimitToLoad
     * @param offScreenBufferPixel
     * @param colorMapWaves
     * @param minWaves
     * @param maxWaves
     * @param showWavesLegend
     * @param showWavesLegendFromZoomLevel
     * @param font8Pt
     * @param showDataDebugLegend
     * @param abortIndicator
     */
    public static void paintWavesInGraphics(MapTileRendererCalculator rendererCalculator, Graphics2D g2, 
            int transparency, Date dateColorLimit, Date dateLimit,
            HashMap<String, WavesDataPoint> dataPointsWaves, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            ColorMap colorMapWaves, double minWaves, double maxWaves, boolean showWavesLegend,
            int showWavesLegendFromZoomLevel, Font font8Pt, boolean showDataDebugLegend, AtomicBoolean abortIndicator) {

        paintWorkerInGraphics("Waves", rendererCalculator, g2, 
                dataPointsWaves, EnvDataShapesHelper.ARROW_RADIUS, 
                offScreenBufferPixel, colorMapWaves, minWaves, maxWaves, showWavesLegend, 
                showWavesLegendFromZoomLevel, font8Pt, showDataDebugLegend, 3, 
                dp -> ignoreDateLimitToLoad || !dp.getDateUTC().before(dateLimit),
                dp -> dp.getAllDataValues(),
                (vals, ovals) -> {
                    // significantHeight, peakPeriod, peakDirection
                    for (int i = 0; i < 3; i++) {
                        double v = (double) vals.get(i);
                        double o = (double) ovals.get(i);
                        double r = (v + o) / 2.;
                        vals.add(i, r);
                    }
                    return vals;
                },
                (pt, dataMap) -> {
                    Graphics2D gt = null;
                    try {
                        // significantHeight, peakPeriod, peakDirection
                        Pair<ArrayList<Object>, Date> pVal = dataMap.get(pt);
                        double sigHeightV = (double) pVal.first().get(0);
                        double headingV = AngleUtils.nomalizeAngleDegrees360((double) pVal.first().get(2));
                        @SuppressWarnings("unused")
                        double periodV = (double) pVal.first().get(1);
                        Date dateV = pVal.second();
                        
                        gt = (Graphics2D) g2.create();
                        gt.translate(pt.getX(), pt.getY());
        
                        Color color = Color.WHITE;
                        color = colorMapWaves.getColor(sigHeightV / maxWaves);
                        if (dateV.before(dateColorLimit))
                            color = ColorUtils.setTransparencyToColor(color, transparency / 2);
                        else
                            color = ColorUtils.setTransparencyToColor(color, transparency);
                        gt.setColor(color);
                        double rot = Math.toRadians(headingV) - rendererCalculator.getRotation();
                        gt.rotate(rot);
                        gt.fill(EnvDataShapesHelper.arrow);
                        gt.rotate(-rot);
                        
                        if (showWavesLegend && rendererCalculator.getLevelOfDetail() >= showWavesLegendFromZoomLevel) {
                            gt.setFont(font8Pt);
                            gt.setColor(Color.WHITE);
                            gt.drawString(MathMiscUtils.round(sigHeightV, 1) + "m", 10, -8);
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                    
                    if (gt != null)
                        gt.dispose();
               }, abortIndicator);
    }

    /**
     * @param rendererCalculator
     * @param g2
     * @param transparency
     * @param dateColorLimit
     * @param dateLimit
     * @param dataPointsChlorophyll
     * @param ignoreDateLimitToLoad
     * @param offScreenBufferPixel
     * @param colorMapChlorophyll
     * @param minChlorophyll
     * @param maxChlorophyll
     * @param showChlorophyllLegend
     * @param showChlorophyllLegendFromZoomLevel
     * @param font8Pt
     * @param showDataDebugLegend
     * @param abortIndicator
     */
    public static void paintChlorophyllInGraphics(MapTileRendererCalculator rendererCalculator, Graphics2D g2, 
            int transparency, Date dateColorLimit, Date dateLimit,
            HashMap<String, ChlorophyllDataPoint> dataPointsChlorophyll, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            ColorMap colorMapChlorophyll, double minChlorophyll, double maxChlorophyll, boolean showChlorophyllLegend,
            int showChlorophyllLegendFromZoomLevel, Font font8Pt, boolean showDataDebugLegend, AtomicBoolean abortIndicator) {
        
        paintWorkerInGraphics("Chlorophyll", rendererCalculator, g2, 
                dataPointsChlorophyll, EnvDataShapesHelper.CIRCLE_RADIUS, 
                offScreenBufferPixel, colorMapChlorophyll, minChlorophyll, maxChlorophyll, showChlorophyllLegend, 
                showChlorophyllLegendFromZoomLevel, font8Pt, showDataDebugLegend, 4,
                dp -> ignoreDateLimitToLoad || !dp.getDateUTC().before(dateLimit),
                dp -> dp.getAllDataValues(), 
                (vals, ovals) -> {
                    double v = (double) vals.get(0);
                    double o = (double) ovals.get(0);
                    double r = (v + o) / 2.;
                    vals.add(0, r);
                    return vals;
                },
                (pt, dataMap) -> {
                    Graphics2D gt = null;
                    try {
                        Pair<ArrayList<Object>, Date> pVal = dataMap.get(pt);
                        
                        double val = ((double) pVal.first().get(0));
                        gt = (Graphics2D) g2.create();
                        gt.translate(pt.getX(), pt.getY());
                        Color color = Color.WHITE;
                        color = colorMapChlorophyll.getColor((val - minChlorophyll) / (maxChlorophyll - minChlorophyll));
                        if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                            color = ColorUtils.setTransparencyToColor(color, transparency / 2);
                        else
                            color = ColorUtils.setTransparencyToColor(color, transparency);
                        gt.setColor(color);
                        gt.draw(EnvDataShapesHelper.circle);
                        gt.fill(EnvDataShapesHelper.circle);
                        
                        if (showChlorophyllLegend && rendererCalculator.getLevelOfDetail() >= showChlorophyllLegendFromZoomLevel) {
                            gt.setFont(font8Pt);
                            gt.setColor(Color.WHITE);
                            gt.drawString(MathMiscUtils.round(val, 1) + "mg/m\u00B3", -15, 15);
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                    
                    if (gt != null)
                        gt.dispose();
               }, abortIndicator);
    }

    /**
     * @param rendererCalculator
     * @param g2
     * @param transparency
     * @param dateColorLimit
     * @param dateLimit
     * @param dataPointsSLA
     * @param ignoreDateLimitToLoad
     * @param offScreenBufferPixel
     * @param colorMapSLA
     * @param minSLA
     * @param maxSLA
     * @param showSLALegend
     * @param showSLALegendFromZoomLevel
     * @param font8Pt
     * @param showDataDebugLegend
     * @param abortIndicator
     */
    public static void paintSLAInGraphics(MapTileRendererCalculator rendererCalculator, Graphics2D g2,
            int transparency, Date dateColorLimit, Date dateLimit, HashMap<String, SLADataPoint> dataPointsSLA, 
            boolean ignoreDateLimitToLoad, int offScreenBufferPixel, ColorMap colorMapSLA,
            double minSLA, double maxSLA, boolean showSLALegend, int showSLALegendFromZoomLevel, 
            Font font8Pt, boolean showDataDebugLegend, AtomicBoolean abortIndicator) {
        
        paintWorkerInGraphics("SLA", rendererCalculator, g2,
                dataPointsSLA, EnvDataShapesHelper.CIRCLE_RADIUS, 
                offScreenBufferPixel, colorMapSLA, minSLA, maxSLA, showSLALegend, 
                showSLALegendFromZoomLevel, font8Pt, showDataDebugLegend, 1,
                dp -> ignoreDateLimitToLoad || !dp.getDateUTC().before(dateLimit),
                dp -> dp.getAllDataValues(), 
                (vals, ovals) -> {
                    // sla
                    double v = (double) vals.get(0);
                    double o = (double) ovals.get(0);
                    double r = (v + o) / 2.;
                    vals.add(0, r);
                    return vals;
                },
//                (pt, dataMap) -> {
//                    Graphics2D gt = null;
//                    try {
//                        Pair<ArrayList<Object>, Date> pVal = dataMap.get(pt);
//                        double sla = (double) pVal.first().get(0);
//                        gt = (Graphics2D) g2.create();
//                        gt.translate(pt.getX(), pt.getY());
//                        //System.out.println(pt);
//                        Color color = Color.WHITE;
//                        color = colorMapSLA.getColor((sla - minSLA) / (maxSLA - minSLA));
//                        if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
//                            color = ColorUtils.setTransparencyToColor(color, transparency / 2);
//                        else
//                            color = ColorUtils.setTransparencyToColor(color, transparency);
//                        gt.setColor(color);
//                        //gt.draw(EnvDataShapesHelper.rectangle);
//                        gt.fill(EnvDataShapesHelper.rectangle);
//                        
//                        if (showSLALegend && rendererCalculator.getLevelOfDetail() >= showSLALegendFromZoomLevel) {
//                            gt.setFont(font8Pt);
//                            gt.setColor(Color.WHITE);
//                            gt.drawString(MathMiscUtils.round(sla, 2) + "m", -15, 15);
//                        }
//                    }
//                    catch (Exception e) {
//                        NeptusLog.pub().trace(e);
//                    }
//                    
//                    if (gt != null)
//                        gt.dispose();
//                },
                (ptDataMap) -> {
                    Set<Point2D> points = ptDataMap.keySet();

                    double fullImgWidth = rendererCalculator.getSize().getWidth() + offScreenBufferPixel * 2.;
                    double fullImgHeight = rendererCalculator.getSize().getHeight() + offScreenBufferPixel * 2.;

                    double xMin = fullImgWidth;
                    double yMin = fullImgHeight;
                    Pair<Double, Double> minXY = calculateMinimumPointDistanceXY(points, xMin, yMin, abortIndicator);
                    xMin = minXY.first();
                    yMin = minXY.second();

                    System.out.println(String.format("xMin=%f   yMin=%f", xMin, yMin));

                    double cacheImgScaleX = 1. / xMin;
                    double cacheImgScaleY = 1. / yMin;

                    double cacheImgWidth = fullImgWidth;
                    double cacheImgHeight = fullImgHeight;
                    cacheImgWidth *= cacheImgScaleX;
                    cacheImgHeight *= cacheImgScaleY;
                    
                    BufferedImage cacheImg = createBufferedImage((int) cacheImgWidth, (int) cacheImgHeight, Transparency.TRANSLUCENT);
                    points.parallelStream().forEach(pt -> {
                        try {
                            Pair<ArrayList<Object>, Date> pVal = ptDataMap.get(pt);
                            double sla = (double) pVal.first().get(0);
                            Color color = colorMapSLA.getColor((sla - minSLA) / (maxSLA - minSLA));
                            if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                                color = ColorUtils.setTransparencyToColor(color, transparency);
                            else
                                color = ColorUtils.setTransparencyToColor(color, transparency / 2);
                            cacheImg.setRGB((int) ((pt.getX() + offScreenBufferPixel) * cacheImgScaleX),
                                    (int) ((pt.getY() + offScreenBufferPixel) * cacheImgScaleY), color.getRGB());
                        }
                        catch (Exception e) {
                            NeptusLog.pub().trace(e);
                        }
                    });

                    Graphics2D gt = (Graphics2D) g2.create();
                    try {
                        gt.translate(rendererCalculator.getWidth() / 2., rendererCalculator.getHeight() / 2.);
                        // gt.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 145));
                        gt.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        //gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//                        gt.drawImage(cacheImg, (int) (-rendererCalculator.getWidth() / 2. - offScreenBufferPixel), 
//                                (int) (-rendererCalculator.getHeight() / 2. - offScreenBufferPixel), 
//                                (int) (rendererCalculator.getWidth() + offScreenBufferPixel * 2), 
//                                (int) (rendererCalculator.getHeight() + offScreenBufferPixel * 2), null, null);
                        gt.drawImage(cacheImg, -(int) (cacheImg.getWidth() / cacheImgScaleX / 2.), 
                                -(int) (cacheImg.getHeight() / cacheImgScaleY / 2.), 
                                (int) (cacheImg.getWidth() / cacheImgScaleX), 
                                (int) (cacheImg.getHeight() / cacheImgScaleY), null, null);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                    if (gt != null)
                        gt.dispose();
                    
                    if (showSLALegend && rendererCalculator.getLevelOfDetail() >= showSLALegendFromZoomLevel) {
                        points.parallelStream().forEach(pt -> {
                            Graphics2D gt1 = null;
                            try {
                                Pair<ArrayList<Object>, Date> pVal = ptDataMap.get(pt);
                                double sla = (double) pVal.first().get(0);
                                gt1 = (Graphics2D) g2.create();
                                gt1.translate(pt.getX(), pt.getY());

                                gt1.setFont(font8Pt);
                                gt1.setColor(Color.WHITE);
                                gt1.drawString(MathMiscUtils.round(sla, 2) + "m", -15, 15);
                            }
                            catch (Exception e) {
                                NeptusLog.pub().trace(e);
                            }
                            if (gt1 != null)
                                gt1.dispose();
                        });
                    }
                },
                abortIndicator);
    }

    public static void paintGenericInGraphics(MapTileRendererCalculator rendererCalculator, Graphics2D g2,
            int transparency, Date dateColorLimit, Map<String, GenericDataPoint> dataPointsVar,
            int offScreenBufferPixel, ColorMap colorMapVar, double minVar, double maxVar, boolean showVarLegend,
            int showVarLegendFromZoomLevel, Font font8Pt, boolean showDataDebugLegend, AtomicBoolean abortIndicator,
            PointPaintEnum paintType, boolean isLogColorMap, boolean isClampToFit, boolean showGradient,
            Pair<Date, Date> dateLimits, Pair<Double, Double> depthLimits) {
        
        if (dataPointsVar == null || dataPointsVar.isEmpty())
            return;
        
        Info info = dataPointsVar.values().iterator().next().getInfo();
        
        final int pointSize;
        switch (paintType) {
            case ARROW:
                pointSize = EnvDataShapesHelper.ARROW_RADIUS;
                break;
            case BARB:
                pointSize = EnvDataShapesHelper.WIND_BARB_RADIUS;
                break;
            case INTERPOLATE:
            case POINT:
            default:
                pointSize = EnvDataShapesHelper.CIRCLE_RADIUS;
                break;
        }
        
        paintWorkerInGraphics(info.fullName, rendererCalculator, g2, dataPointsVar, 
                pointSize, offScreenBufferPixel, colorMapVar, minVar, maxVar, showVarLegend, 
                showVarLegendFromZoomLevel, font8Pt, showDataDebugLegend, 1,
                // Acceptor
                dp -> {
                    if (abortIndicator.get())
                        return false;

                    boolean depthOk = true;
                    if (!Double.isFinite(depthLimits.first()) && !Double.isFinite(depthLimits.second())
                            || !Double.isFinite(dp.getDepth())) {
                        depthOk = true;
                    }
                    else {
                        // All historical data has the same depth
                        double minDepth = depthLimits.first();
                        double maxDepth = depthLimits.second();
                        if (Double.compare(dp.getDepth(), maxDepth) <= 0
                                && Double.compare(dp.getDepth(), minDepth) >= 0)
                            depthOk = true;
                        else
                            depthOk = false;
                    }
                    
                    boolean dateOk = true;
                    if (depthOk) {
                        if (dateLimits.first().getTime() == 0 && dateLimits.second().getTime() == 0) {
                            dateOk = true;
                        }
                        else {
                            GenericDataPoint maxDateDp = (GenericDataPoint) dp.getHistoricalData().stream()
                                    .filter((d) -> !abortIndicator.get()).max((p1, p2) -> {
                                if (p1.getDateUTC() != null && p2.getDateUTC() != null)
                                    return p1.getDateUTC().compareTo(p2.getDateUTC());
                                else if (p2.getDateUTC() != null)
                                    return -1;
                                else
                                    return 1;
                            }).orElse(dp);
                            GenericDataPoint minDateDp = (GenericDataPoint) dp.getHistoricalData().stream()
                                    .filter((d) -> !abortIndicator.get()).min((p1, p2) -> {
                                if (p1.getDateUTC() != null && p2.getDateUTC() != null)
                                    return p1.getDateUTC().compareTo(p2.getDateUTC());
                                else if (p2.getDateUTC() != null)
                                    return -1;
                                else
                                    return 1;
                            }).orElse(dp);
                            
                            Date minDate = dateLimits.first();
                            Date maxDate = dateLimits.second();
                            if ((minDateDp.getDateUTC() == null || minDateDp.getDateUTC().compareTo(maxDate) <= 0)
                                    && (maxDateDp.getDateUTC() == null || maxDateDp.getDateUTC().compareTo(minDate) >= 0))
                                dateOk = true;
                            else
                                dateOk = false;
                        }
                    }
                    
                    return dateOk && depthOk;
                },
                // Extractor
                dp -> {
                    if (abortIndicator.get())
                        return new ArrayList<Object>();

                    Date minDate = dateLimits.first();
                    Date maxDate = dateLimits.second();
                    if (minDate.getTime() == 0 && maxDate.getTime() == 0)
                        return dp.getAllDataValues();
                    
                    GenericDataPoint ret = null;
                    for (GenericDataPoint elm : dp.getHistoricalData()) {
                        if (elm.getDateUTC().compareTo(maxDate) <= 0
                                && elm.getDateUTC().compareTo(minDate) >= 0) {
                            if (ret == null || !elm.getDateUTC().before(ret.getDateUTC()))
                                ret = elm;
                        }
                    }
                    
                    if (showGradient) {
                        ArrayList<Object> retGrad = new ArrayList<>();
                        if (Double.isFinite(ret.getGradientValue()))
                            retGrad.add(ret.getGradientValue());
                        return retGrad;
                    }
                    
                    return ret.getAllDataValues();
                },
                // Merger
                (vals, ovals) -> {
                    // sla
                    double v = (double) vals.get(0);
                    double o = (double) ovals.get(0);
                    double r = (v + o) / 2.;
                    vals.add(0, r);
                    return vals;
                },
                // Painter for points
                paintType == PointPaintEnum.INTERPOLATE ? null : (pt, dataMap) -> {
                    if (abortIndicator.get())
                        return;

                    Graphics2D gt = null;
                    try {
                        Pair<ArrayList<Object>, Date> pVal = dataMap.get(pt);
                        double v = (double) pVal.first().get(0);
                        
                        if (isClampToFit) {
                            if (Double.compare(v, minVar) < 0 || Double.compare(v, maxVar) > 0)
                                return;
                        }
                        
                        gt = (Graphics2D) g2.create();
                        gt.translate(pt.getX(), pt.getY());
                        //System.out.println(pt);
                        Color color = Color.WHITE;
                        if (!isLogColorMap)
                            color = colorMapVar.getColor(ColorMapUtils.getColorIndexZeroToOne(v, minVar, maxVar));
                        else
                            color = colorMapVar.getColor(ColorMapUtils.getColorIndexZeroToOneLog10(v, minVar, maxVar));
                        if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                            color = ColorUtils.setTransparencyToColor(color, transparency / 2);
                        else
                            color = ColorUtils.setTransparencyToColor(color, transparency);
                        gt.setColor(color);
                        switch (paintType) {
                            case ARROW:
                                gt.fill(EnvDataShapesHelper.arrow);
                                break;
                            case BARB:
                                double speedKnots = 5;
                                EnvDataShapesHelper.paintWindBarb(gt, speedKnots);
                                break;
                            case POINT:
                            case INTERPOLATE:
                            default:
                                gt.fill(EnvDataShapesHelper.rectangle);
                                break;
                        }
                        
                        if (showVarLegend && rendererCalculator.getLevelOfDetail() >= showVarLegendFromZoomLevel) {
                            gt.setFont(font8Pt);
                            gt.setColor(Color.WHITE);
                            gt.drawString(MathMiscUtils.round(v, 2) + info.unit, -15, 15);
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                    
                    if (gt != null)
                        gt.dispose();
                },
                // Painter for interpolation
                (ptDataMap) -> {
                    System.out.println("Start painter");
                    if (abortIndicator.get())
                        return;

                    Set<Point2D> points = ptDataMap.keySet();

                    double fullImgWidth = rendererCalculator.getSize().getWidth() + offScreenBufferPixel * 2.;
                    double fullImgHeight = rendererCalculator.getSize().getHeight() + offScreenBufferPixel * 2.;

                    double xMin = fullImgWidth;
                    double yMin = fullImgHeight;
                    System.out.println(" painter 1");
                    Pair<Double, Double> minXY = calculateMinimumPointDistanceXY(points, xMin, yMin, abortIndicator);
                    System.out.println(" painter 2");
                    xMin = minXY.first();
                    yMin = minXY.second();

                    System.out.println(String.format("xMin=%f   yMin=%f", xMin, yMin));
                    xMin = Math.max(MIN_INTERPOLATION_POINT, xMin);
                    yMin = Math.max(MIN_INTERPOLATION_POINT, yMin);
                    System.out.println(String.format("fixed xMin=%f   yMin=%f", xMin, yMin));

                    double cacheImgScaleX = 1. / xMin;
                    double cacheImgScaleY = 1. / yMin;

                    double cacheImgWidth = fullImgWidth;
                    double cacheImgHeight = fullImgHeight;
                    cacheImgWidth *= cacheImgScaleX;
                    cacheImgHeight *= cacheImgScaleY;

                    if (abortIndicator.get())
                        return;

                    BufferedImage cacheImg = createBufferedImage((int) cacheImgWidth, (int) cacheImgHeight, Transparency.TRANSLUCENT);
                    points.parallelStream().filter((d) -> !abortIndicator.get()).forEach(pt -> {
                        if (abortIndicator.get())
                            return;

                        try {
                            Pair<ArrayList<Object>, Date> pVal = ptDataMap.get(pt);
                            double v = (double) pVal.first().get(0);
                            
                            if (isClampToFit) {
                                if (Double.compare(v, minVar) < 0 || Double.compare(v, maxVar) > 0)
                                    return;
                            }

                            Color color = Color.WHITE;
                            if (!isLogColorMap)
                                color = colorMapVar.getColor(ColorMapUtils.getColorIndexZeroToOne(v, minVar, maxVar));
                            else
                                color = colorMapVar.getColor(ColorMapUtils.getColorIndexZeroToOneLog10(v, minVar, maxVar));
                            if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                                color = ColorUtils.setTransparencyToColor(color, transparency);
                            else
                                color = ColorUtils.setTransparencyToColor(color, transparency / 2);
                            cacheImg.setRGB((int) ((pt.getX() + offScreenBufferPixel) * cacheImgScaleX),
                                    (int) ((pt.getY() + offScreenBufferPixel) * cacheImgScaleY), color.getRGB());
                        }
                        catch (Exception e) {
                            NeptusLog.pub().trace(e);
                        }
                    });

                    if (abortIndicator.get())
                        return;

                    Graphics2D gt = (Graphics2D) g2.create();
                    try {
                        gt.translate(rendererCalculator.getWidth() / 2., rendererCalculator.getHeight() / 2.);
                        gt.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
                        gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_DEFAULT);
                        gt.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        gt.drawImage(cacheImg, -(int) (cacheImg.getWidth() / cacheImgScaleX / 2.), 
                                -(int) (cacheImg.getHeight() / cacheImgScaleY / 2.), 
                                (int) (cacheImg.getWidth() / cacheImgScaleX), 
                                (int) (cacheImg.getHeight() / cacheImgScaleY), null, null);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                    if (gt != null)
                        gt.dispose();

                    if (abortIndicator.get())
                        return;

                    if (showVarLegend && rendererCalculator.getLevelOfDetail() >= showVarLegendFromZoomLevel) {
                        points.parallelStream().filter((d) -> !abortIndicator.get()).forEach(pt -> {
                            if (abortIndicator.get())
                                return;

                            Graphics2D gt1 = null;
                            try {
                                Pair<ArrayList<Object>, Date> pVal = ptDataMap.get(pt);
                                double sla = (double) pVal.first().get(0);
                                gt1 = (Graphics2D) g2.create();
                                gt1.translate(pt.getX(), pt.getY());

                                gt1.setFont(font8Pt);
                                gt1.setColor(Color.WHITE);
                                gt1.drawString(MathMiscUtils.round(sla, 2) + info.unit, -15, 15);
                            }
                            catch (Exception e) {
                                NeptusLog.pub().trace(e);
                            }
                            if (gt1 != null)
                                gt1.dispose();
                        });
                    }
                },
                abortIndicator);
    }

    private static <Dp extends BaseDataPoint<?>> void paintWorkerInGraphics(String varName, 
            MapTileRendererCalculator rendererCalculator,  Graphics2D g2, 
            HashMap<String, Dp> dataPoints, int gridSpacing, 
            int offScreenBufferPixel, ColorMap colorMap, double minVal, double maxVal, 
            boolean showVarLegend, int showVarLegendFromZoomLevel, Font font8Pt, 
            boolean showDataDebugLegend, int debugPainterForDatesOffserIndex, Function<Dp, Boolean> acceptor,
            Function<Dp, ArrayList<Object>> extractor, BinaryOperator<ArrayList<Object>> merger,
            BiConsumer<Point2D, Map<Point2D, Pair<ArrayList<Object>, Date>>> eachPointerpainter,
            AtomicBoolean abortIndicator) {

        paintWorkerInGraphics(varName, rendererCalculator, g2, dataPoints, gridSpacing,
                offScreenBufferPixel, colorMap, minVal, maxVal, showVarLegend,
                showVarLegendFromZoomLevel, font8Pt, showDataDebugLegend, debugPainterForDatesOffserIndex,
                acceptor, extractor, merger, eachPointerpainter, null, abortIndicator);
    }

    private static <Dp extends BaseDataPoint<?>> void paintWorkerInGraphics(String varName, 
            MapTileRendererCalculator rendererCalculator, Graphics2D g2, 
            HashMap<String, Dp> dataPoints, int gridSpacing, 
            int offScreenBufferPixel, ColorMap colorMap, double minVal, double maxVal, 
            boolean showVarLegend, int showVarLegendFromZoomLevel, Font font8Pt, 
            boolean showDataDebugLegend, int debugPainterForDatesOffserIndex, Function<Dp, Boolean> acceptor,
            Function<Dp, ArrayList<Object>> extractor, BinaryOperator<ArrayList<Object>> merger,
            Consumer<Map<Point2D, Pair<ArrayList<Object>, Date>>> painter,
            AtomicBoolean abortIndicator) {

        paintWorkerInGraphics(varName, rendererCalculator, g2, dataPoints, gridSpacing,
                offScreenBufferPixel, colorMap, minVal, maxVal, showVarLegend,
                showVarLegendFromZoomLevel, font8Pt, showDataDebugLegend, debugPainterForDatesOffserIndex,
                acceptor, extractor, merger, null, painter, abortIndicator);
    }

    /**
     * @param varName
     * @param rendererCalculator
     * @param g2
     * @param dataPoints
     * @param gridSpacing
     * @param offScreenBufferPixel
     * @param colorMap
     * @param minVal
     * @param maxVal
     * @param showVarLegend
     * @param showVarLegendFromZoomLevel
     * @param font8Pt
     * @param showDataDebugLegend
     * @param debugPainterForDatesOffserIndex
     * @param acceptor See {@link DataCollector}
     * @param extractor See {@link DataCollector}
     * @param merger See {@link DataCollector}
     * @param eachPointPainter if null painter is used
     * @param painter
     * @param abortIndicator
     */
    private static <Dp extends BaseDataPoint<?>> void paintWorkerInGraphics(String varName, 
            MapTileRendererCalculator rendererCalculator, Graphics2D g2, 
            Map<String, Dp> dataPoints, int gridSpacing, 
            int offScreenBufferPixel, ColorMap colorMap, double minVal, double maxVal, 
            boolean showVarLegend, int showVarLegendFromZoomLevel, Font font8Pt, 
            boolean showDataDebugLegend, int debugPainterForDatesOffserIndex, Function<Dp, Boolean> acceptor, 
            Function<Dp, ArrayList<Object>> extractor, BinaryOperator<ArrayList<Object>> merger,
            BiConsumer<Point2D, Map<Point2D, Pair<ArrayList<Object>, Date>>> eachPointPainter,
            Consumer<Map<Point2D, Pair<ArrayList<Object>, Date>>> painter,
            AtomicBoolean abortIndicator) {
        
        try {
            List<Dp> dest = new ArrayList<>(dataPoints.values());
            long stMillis = System.currentTimeMillis();
            DataCollector<Dp> dataCollector = new DataCollector<Dp>(rendererCalculator, 
                    offScreenBufferPixel, gridSpacing, acceptor, extractor, merger, abortIndicator);
            LongAccumulator visiblePts = dataCollector.visiblePts;
            LongAccumulator toDatePts = dataCollector.toDatePts;
            LongAccumulator fromDatePts = dataCollector.fromDatePts;
            ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> ptFilt = dest.parallelStream()
                    .filter((d) -> !abortIndicator.get())
                    .collect(dataCollector);
            
            if (ptFilt.isEmpty()) {
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
            }
            double usePercent = (ptFilt.get(0) == null ? -1 : ptFilt.get(0).size() * 1. / visiblePts.longValue()) * 100;
            final int idx = 1; //getIndexForData(rendererCalculator.getLevelOfDetail(), usePercent);
            debugOut(showDataDebugLegend, String.format("%s stg 1 took %ss :: using %d of %d visible from original %d (%.1f%% of visible) | %d not gridded %sused | %s",
                    varName, MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), (ptFilt.get(0) == null ? -1 : ptFilt.get(0).size()), 
                    visiblePts.longValue(), dest.size(), usePercent, (ptFilt.get(1) == null ? -1 : ptFilt.get(1).size()), "", abortIndicator.get() ? "aborted" : ""));
            stMillis = System.currentTimeMillis();

            if (abortIndicator.get()) {
                debugOut(showDataDebugLegend, String.format("%s stg 2 took %ss %s", varName,
                        MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1),
                        abortIndicator.get() ? "aborted" : ""));
                return;
            }

            if (eachPointPainter != null) {
                ptFilt.get(idx).keySet().parallelStream().filter((d) -> !abortIndicator.get()).forEach(pt -> {
                    if (abortIndicator.get())
                        return;
                    
                    try {
                        eachPointPainter.accept(pt, ptFilt.get(idx));
                    }
                    catch (Exception e) {
                        NeptusLog.pub().trace(e);
                    }
                });                
            }
            else {
                try {
                    painter.accept(ptFilt.get(idx));
                }
                catch (Exception e) {
                    NeptusLog.pub().trace(e);
                }
            }
            debugOut(showDataDebugLegend, String.format("%s stg 2 took %ss %s", varName,
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1),
                    abortIndicator.get() ? "aborted" : ""));
            
            int offset = OFFSET_REND_TXT_DATE_RANGES + OFFSET_REND_TXT_DATE_RANGES_DELTA * debugPainterForDatesOffserIndex;
            String typeName = varName;
            paintDatesRange(g2, toDatePts.longValue(), fromDatePts.longValue(), offset, typeName, showDataDebugLegend,
                    font8Pt);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Merges the content of both strings splitting the content by ','.
     * 
     * @param strV
     * @param strO
     * @return
     */
    public static String mergeStrings(String strV, String strO) {
        if (strV.isEmpty())
            return strO;
        else if (strO.isEmpty())
            return strV;
        else if (strV.equalsIgnoreCase(strO))
            return strV;
        
        String[] strVTk1 = strV.split(",");
        String[] strVTk2 = strO.split(",");
        Stream.of(strVTk1).parallel().forEach(s -> s.trim());
        Stream.of(strVTk2).parallel().forEach(s -> s.trim());
        
        String[] both = ArrayUtils.addAll(strVTk1, strVTk2);
        List<String> distinct = Stream.of(both).distinct().collect(Collectors.toList());
        return distinct.stream().map(i -> i.toString()) .collect(Collectors.joining(", "));
    }
    
    /**
     * @param points
     * @param abortIndicator 
     * @param xMin
     * @param yMin
     * @return
     */
    private static Pair<Double, Double> calculateMinimumPointDistanceXY(Collection<Point2D> points,
            double xMinStartValue, double yMinStartValue, AtomicBoolean abortIndicator) {
        DoubleAccumulator xMin = new DoubleAccumulator((c, n) -> n < c ? n : c, xMinStartValue);
        DoubleAccumulator yMin = new DoubleAccumulator((c, n) -> n < c ? n : c, yMinStartValue);
        
        Instant ts = Instant.now();
        System.out.println(" min/max 1");

        MovingAverage maX = new MovingAverage((short) (points.size() * 0.1));
        MovingAverage maY = new MovingAverage((short) (points.size() * 0.1));
        
        ArrayList<Point2D> pointsXSorted = points.parallelStream().filter((p) -> !abortIndicator.get())
                .sorted((p, o) -> Double.compare(p.getX(), o.getX()))
                .collect(Collectors.toCollection(ArrayList<Point2D>::new));
                //.collect(Collectors.toCollection(() -> new ArrayList<Point2D>(points.size())));
        IntStream.range(0, pointsXSorted.size() - 1).parallel()
                .filter((p) -> !abortIndicator.get())
                .forEach((i) -> {
            if (i == 0)
                return;
            Point2D pot = pointsXSorted.get(i - 1);
            Point2D p = pointsXSorted.get(i);
            if (pot.getX() == p.getX())
                return;
            double d = Math.abs(pot.getX() - p.getX());
            synchronized (maX) {
                maX.update(d);
            }
            xMin.accumulate(d);
        });

        System.out.println(" min/max 5 " + (Duration.between(ts, Instant.now()).toMillis()) + "ms");
        ts = Instant.now();

        ArrayList<Point2D> pointsYSorted = points.parallelStream().filter((p) -> !abortIndicator.get())
                .sorted((p, o) -> Double.compare(p.getY(), o.getY()))
                .collect(Collectors.toCollection(ArrayList<Point2D>::new));
                //.collect(Collectors.toCollection(() -> new ArrayList<Point2D>(points.size())));
        IntStream.range(0, pointsYSorted.size() - 1).parallel()
                .filter((p) -> !abortIndicator.get())
                .forEach((i) -> {
            if (i == 0)
                return;
            Point2D pot = pointsYSorted.get(i - 1);
            Point2D p = pointsYSorted.get(i);
            if (pot.getY() == p.getY())
                return;
            double d = Math.abs(pot.getY() - p.getY());
            synchronized (maY) {
                maY.update(d);
            }
            yMin.accumulate(d);
        });

        System.out.println(" min/max 6 " + (Duration.between(ts, Instant.now()).toMillis()) + "ms");
        ts = Instant.now();

        System.out.println(String.format("MinMax %f x %f   with avg %f x %f  stddev %f x %f", xMin.doubleValue(), yMin.doubleValue(), maX.mean(), maY.mean(), maX.stdev(), maY.stdev()));
        
        // return new Pair<Double, Double>(xMin, yMin);
        return new Pair<Double, Double>(maX.mean() + maX.stdev(), maY.mean() + maY.stdev());
    }
    
    /**
     * @param cacheImgWidth
     * @param cacheImgHeight
     * @param translucent
     * @return
     */
    private static BufferedImage createBufferedImage(int cacheImgWidth, int cacheImgHeight, int translucent) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();
        return gc.createCompatibleImage((int) cacheImgWidth , (int) cacheImgHeight , Transparency.TRANSLUCENT);
    }
}
