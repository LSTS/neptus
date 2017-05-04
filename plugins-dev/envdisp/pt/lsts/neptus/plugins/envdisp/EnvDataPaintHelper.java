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
 * 23/04/2017
 */
package pt.lsts.neptus.plugins.envdisp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.UnitsUtil;

/**
 * @author pdias
 *
 */
class EnvDataPaintHelper {

    private static int filterUseLOD = 9;

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
     * Paint a color bar with captions sized w x h=70 x 110.
     * 
     * @param g
     * @param renderer
     * @param cmap
     * @param varName
     * @param units
     * @param minValue
     * @param maxValue
     */
    static void paintColorBar(Graphics2D g, StateRenderer2D renderer, ColorMap cmap, String varName, 
            String units, double minValue, double maxValue) {
//        boolean recreateImageColorBar = offScreenImageControlColorBar.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
//        if (recreateImageColorBar) {
            Graphics2D g2 = g; //offScreenImageControlColorBar.getImageGraphics();
            g2.translate(-5, -30);
            g2.setColor(new Color(250, 250, 250, 100));
            g2.fillRect(5, 30, 70, 110);

            ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, cmap);
            cb.setSize(15, 80);
            g2.setColor(Color.WHITE);
            Font prev = g2.getFont();
            g2.setFont(new Font("Helvetica", Font.BOLD, 18));
            g2.setFont(prev);
            g2.translate(15, 45);
            cb.paint(g2);
            g2.translate(-10, -15);
            
            g2.setColor(Color.WHITE);
            g2.drawString(varName, 2, 11);
            g2.setColor(Color.BLACK);
            g2.drawString(varName, 2, 12);

            try {
                double medValue = (maxValue - minValue) / 2. + minValue;
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue), 28, 20+5);
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue), 29, 21+5);
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(medValue), 28, 60);
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(medValue), 29, 61);
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(minValue), 28, 100-10);
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(minValue), 29, 101-10);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }

            g2.setColor(Color.WHITE);
            g2.drawString(units, 10, 105);
            g2.setColor(Color.BLACK);
            g2.drawString(units, 10, 106);

            g2.dispose();
//        }
//        offScreenImageControlColorBar.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);
    }


    /**
     * @param renderer
     * @param g2
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
     */
    static void paintHFRadarInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit,
            HashMap<String, HFRadarDataPoint> dataPointsCurrents, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            ColorMap colorMapCurrents, double minCurrentCmS, double maxCurrentCmS,
            boolean showCurrentsLegend, int showCurrentsLegendFromZoomLevel, Font font8Pt, boolean showDataDebugLegend) {
        try {
            List<HFRadarDataPoint> dest = new ArrayList<>(dataPointsCurrents.values());
            long stMillis = System.currentTimeMillis();
            DataCollector<HFRadarDataPoint> dataCollector = new DataCollector<HFRadarDataPoint>(ignoreDateLimitToLoad, dateLimit, renderer, 
                    offScreenBufferPixel, EnvDataShapesHelper.ARROW_RADIUS, (vals, ovals) -> {
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
                    });
            LongAccumulator visiblePts = dataCollector.visiblePts;
            LongAccumulator toDatePts = dataCollector.toDatePts;
            LongAccumulator fromDatePts = dataCollector.fromDatePts;
            ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> ptFilt = dest.parallelStream()
                    .collect(dataCollector);
            
            if (ptFilt.isEmpty()) {
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
            }
            double usePercent = (ptFilt.get(0).size() * 1. / visiblePts.longValue()) * 100;
            final int idx;
            if (renderer.getLevelOfDetail() > filterUseLOD) {
                if (usePercent <= 90)
                    idx = 0;
                else
                    idx = 1;
            }
            else {
                idx = 0;
            }
            debugOut(showDataDebugLegend, String.format("Currents stg 1 took %ss :: %d of %d from %d (%f%%) %d %d",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.get(0).size(), visiblePts.longValue(), dest.size(),
                    usePercent , ptFilt.get(1).size(), idx));
            stMillis = System.currentTimeMillis();
            
            ptFilt.get(idx).keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Pair<ArrayList<Object>, Date> pVal = ptFilt.get(idx).get(pt);
                    
                    double speedCmSV = (double) pVal.first().get(0);
                    double headingV = (double) pVal.first().get(1);
                    Date dateV = pVal.second();
                    
                    gt = (Graphics2D) g2.create();
                    gt.translate(pt.getX(), pt.getY());

                    Color color = Color.WHITE;
                    color = colorMapCurrents.getColor(speedCmSV / maxCurrentCmS);
                    if (dateV.before(dateColorLimit))
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    double rot = Math.toRadians(-headingV + 90) - renderer.getRotation();
                    gt.rotate(rot);
                    gt.fill(EnvDataShapesHelper.arrow);
                    gt.rotate(-rot);
                    
                    if (showCurrentsLegend && renderer.getLevelOfDetail() >= showCurrentsLegendFromZoomLevel) {
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
            });
            debugOut(showDataDebugLegend, String.format("Currents stg 2 took %ss",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1)));
            
            int offset = EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES
                    + EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES_DELTA * 0;
            String typeName = "Currents";
            EnvDataPaintHelper.paintDatesRange(g2, toDatePts.longValue(), fromDatePts.longValue(), offset, typeName,
                    showDataDebugLegend, font8Pt);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param renderer
     * @param g2
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
     */
    static void paintSSTInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit,
            HashMap<String, SSTDataPoint> dataPointsSST, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            ColorMap colorMapSST, double minSST, double maxSST,
            boolean showSSTLegend, int showSSTLegendFromZoomLevel, Font font8Pt, boolean showDataDebugLegend) {
        try {
            List<SSTDataPoint> dest = new ArrayList<>(dataPointsSST.values());
            long stMillis = System.currentTimeMillis();
            DataCollector<SSTDataPoint> dataCollector = new DataCollector<SSTDataPoint>(ignoreDateLimitToLoad, dateLimit, renderer, 
                    offScreenBufferPixel, EnvDataShapesHelper.CIRCLE_RADIUS, (vals, ovals) -> {
                        // sst
                        double v = (double) vals.get(0);
                        double o = (double) ovals.get(0);
                        double r = (v + o) / 2.;
                        vals.add(0, r);
                        return vals;
                    });
            LongAccumulator visiblePts = dataCollector.visiblePts;
            LongAccumulator toDatePts = dataCollector.toDatePts;
            LongAccumulator fromDatePts = dataCollector.fromDatePts;
            ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> ptFilt = dest.parallelStream()
                    .collect(dataCollector);
            
            if (ptFilt.isEmpty()) {
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
            }
            double usePercent = (ptFilt.get(0).size() * 1. / visiblePts.longValue()) * 100;
            final int idx;
            if (renderer.getLevelOfDetail() > filterUseLOD) {
                if (usePercent <= 90)
                    idx = 0;
                else
                    idx = 1;
            }
            else {
                idx = 0;
            }
            debugOut(showDataDebugLegend, String.format("SST stg 1 took %ss :: %d of %d from %d (%f%%) %d %d",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.get(0).size(), visiblePts.longValue(), dest.size(),
                    usePercent , ptFilt.get(1).size(), idx));
            stMillis = System.currentTimeMillis();

            ptFilt.get(idx).keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Pair<ArrayList<Object>, Date> pVal = ptFilt.get(idx).get(pt);
                    double sst = (double) pVal.first().get(0);
                    gt = (Graphics2D) g2.create();
                    gt.translate(pt.getX(), pt.getY());
                    Color color = Color.WHITE;
                    color = colorMapSST.getColor((sst - minSST) / (maxSST - minSST));
                    if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    gt.draw(EnvDataShapesHelper.circle);
                    gt.fill(EnvDataShapesHelper.circle);
                    
                    if (showSSTLegend && renderer.getLevelOfDetail() >= showSSTLegendFromZoomLevel) {
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
            });
            debugOut(showDataDebugLegend, String.format("SST stg 2 took %ss",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1)));
            
            int offset = EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES + EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES_DELTA * 1;
            String typeName = "SST";
            paintDatesRange(g2, toDatePts.longValue(), fromDatePts.longValue(), offset, typeName, showDataDebugLegend,
                    font8Pt);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param renderer
     * @param g2
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
     */
    static void paintWindInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit,
            HashMap<String, WindDataPoint> dataPointsWind, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            boolean useColorMapForWind, ColorMap colorMapWind, double minWind, double maxWind,
            Font font8Pt, boolean showDataDebugLegend) {
        try {
            List<WindDataPoint> dest = new ArrayList<>(dataPointsWind.values());
            long stMillis = System.currentTimeMillis();
            DataCollector<WindDataPoint> dataCollector = new DataCollector<WindDataPoint>(ignoreDateLimitToLoad, dateLimit, renderer, 
                    offScreenBufferPixel, EnvDataShapesHelper.WIND_BARB_RADIUS, (vals, ovals) -> {
                        // u, v
                        for (int i = 0; i < 2; i++) {
                            double v = (double) vals.get(i);
                            double o = (double) ovals.get(i);
                            double r = (v + o) / 2.;
                            vals.add(i, r);
                        }
                        return vals;
                    });
            LongAccumulator visiblePts = dataCollector.visiblePts;
            LongAccumulator toDatePts = dataCollector.toDatePts;
            LongAccumulator fromDatePts = dataCollector.fromDatePts;
            ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> ptFilt = dest.parallelStream()
                    .collect(dataCollector);
            
            if (ptFilt.isEmpty()) {
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
            }
            double usePercent = (ptFilt.get(0).size() * 1. / visiblePts.longValue()) * 100;
            final int idx;
            if (renderer.getLevelOfDetail() > filterUseLOD) {
                if (usePercent <= 90)
                    idx = 0;
                else
                    idx = 1;
            }
            else {
                idx = 0;
            }
            debugOut(showDataDebugLegend, String.format("Wind stg 1 took %ss :: %d of %d from %d (%f%%) %d %d",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.get(0).size(), visiblePts.longValue(), dest.size(),
                    usePercent , ptFilt.get(1).size(), idx));
            stMillis = System.currentTimeMillis();
            
            ptFilt.get(idx).keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    // u, v
                    Pair<ArrayList<Object>, Date> pVal = ptFilt.get(idx).get(pt);
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
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    
                    gt.rotate(Math.toRadians(headingV) - renderer.getRotation());
                    
                    double speedKnots = speedV * UnitsUtil.MS_TO_KNOT;
                    EnvDataShapesHelper.paintWindBarb(gt, speedKnots);
                }
                catch (Exception e) {
                    NeptusLog.pub().trace(e);
                }
                
                if (gt != null)
                    gt.dispose();
            });
            debugOut(showDataDebugLegend, String.format("Wind stg 2 took %ss",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1)));
            
            int offset = EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES + EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES_DELTA * 2;
            String typeName = "Wind";
            paintDatesRange(g2, toDatePts.longValue(), fromDatePts.longValue(), offset, typeName, showDataDebugLegend,
                    font8Pt);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param renderer
     * @param g2
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
     */
    static void paintWavesInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit,
            HashMap<String, WavesDataPoint> dataPointsWaves, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            ColorMap colorMapWaves, double minWaves, double maxWaves, boolean showWavesLegend,
            int showWavesLegendFromZoomLevel, Font font8Pt, boolean showDataDebugLegend) {
        try {
            List<WavesDataPoint> dest = new ArrayList<>(dataPointsWaves.values());
            long stMillis = System.currentTimeMillis();
            DataCollector<WavesDataPoint> dataCollector = new DataCollector<WavesDataPoint>(ignoreDateLimitToLoad, dateLimit, renderer, 
                    offScreenBufferPixel, EnvDataShapesHelper.ARROW_RADIUS, (vals, ovals) -> {
                        //significantHeight, peakPeriod, peakDirection
                        for (int i = 0; i < 3; i++) {
                            double v = (double) vals.get(i);
                            double o = (double) ovals.get(i);
                            double r = (v + o) / 2.;
                            vals.add(i, r);
                        }
                        return vals;
                    });
            LongAccumulator visiblePts = dataCollector.visiblePts;
            LongAccumulator toDatePts = dataCollector.toDatePts;
            LongAccumulator fromDatePts = dataCollector.fromDatePts;
            ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> ptFilt = dest.parallelStream()
                    .collect(dataCollector);
            
            if (ptFilt.isEmpty()) {
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
            }
            double usePercent = (ptFilt.get(0).size() * 1. / visiblePts.longValue()) * 100;
            final int idx;
            if (renderer.getLevelOfDetail() > filterUseLOD) {
                if (usePercent <= 90)
                    idx = 0;
                else
                    idx = 1;
            }
            else {
                idx = 0;
            }
            debugOut(showDataDebugLegend, String.format("Waves stg 1 took %ss :: %d of %d from %d (%f%%) %d %d",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.get(0).size(), visiblePts.longValue(), dest.size(),
                    usePercent , ptFilt.get(1).size(), idx));
            stMillis = System.currentTimeMillis();
            
            ptFilt.get(idx).keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    // significantHeight, peakPeriod, peakDirection
                    Pair<ArrayList<Object>, Date> pVal = ptFilt.get(idx).get(pt);
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
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    double rot = Math.toRadians(headingV) - renderer.getRotation();
                    gt.rotate(rot);
                    gt.fill(EnvDataShapesHelper.arrow);
                    gt.rotate(-rot);
                    
                    if (showWavesLegend && renderer.getLevelOfDetail() >= showWavesLegendFromZoomLevel) {
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
            });
            debugOut(showDataDebugLegend, String.format("Waves stg 2 took %ss",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1)));
            
            int offset = EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES + EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES_DELTA * 3;
            String typeName = "Waves";
            paintDatesRange(g2, toDatePts.longValue(), fromDatePts.longValue(), offset, typeName, showDataDebugLegend,
                    font8Pt);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void paintChlorophyllInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit,
            HashMap<String, ChlorophyllDataPoint> dataPointsChlorophyll, boolean ignoreDateLimitToLoad, int offScreenBufferPixel,
            ColorMap colorMapChlorophyll, double minChlorophyll, double maxChlorophyll,
            boolean showChlorophyllLegend, int showChlorophyllLegendFromZoomLevel, Font font8Pt, boolean showDataDebugLegend) {
        try {
            List<ChlorophyllDataPoint> dest = new ArrayList<>(dataPointsChlorophyll.values());
            long stMillis = System.currentTimeMillis();
            DataCollector<ChlorophyllDataPoint> dataCollector = new DataCollector<ChlorophyllDataPoint>(ignoreDateLimitToLoad, dateLimit, renderer, 
                    offScreenBufferPixel, EnvDataShapesHelper.CIRCLE_RADIUS, (vals, ovals) -> {
                        double v = (double) vals.get(0);
                        double o = (double) ovals.get(0);
                        double r = (v + o) / 2.;
                        vals.add(0, r);
                        return vals;
                    });
            LongAccumulator visiblePts = dataCollector.visiblePts;
            LongAccumulator toDatePts = dataCollector.toDatePts;
            LongAccumulator fromDatePts = dataCollector.fromDatePts;
            ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> ptFilt = dest.parallelStream()
                    .collect(dataCollector);
            
            if (ptFilt.isEmpty()) {
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                ptFilt.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
            }
            double usePercent = (ptFilt.get(0).size() * 1. / visiblePts.longValue()) * 100;
            final int idx;
            if (renderer.getLevelOfDetail() > filterUseLOD) {
                if (usePercent <= 90)
                    idx = 0;
                else
                    idx = 1;
            }
            else {
                idx = 0;
            }
            debugOut(showDataDebugLegend, String.format("Chlorophyll stg 1 took %ss :: %d of %d from %d (%f%%) %d %d",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.get(0).size(), visiblePts.longValue(), dest.size(),
                    usePercent , ptFilt.get(1).size(), idx));
            stMillis = System.currentTimeMillis();
            
            ptFilt.get(idx).keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Pair<ArrayList<Object>, Date> pVal = ptFilt.get(idx).get(pt);
                    
                    double val = ((double) pVal.first().get(0));
                    gt = (Graphics2D) g2.create();
                    gt.translate(pt.getX(), pt.getY());
                    Color color = Color.WHITE;
                    color = colorMapChlorophyll.getColor((val - minChlorophyll) / (maxChlorophyll - minChlorophyll));
                    if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    gt.draw(EnvDataShapesHelper.circle);
                    gt.fill(EnvDataShapesHelper.circle);
                    
                    if (showChlorophyllLegend && renderer.getLevelOfDetail() >= showChlorophyllLegendFromZoomLevel) {
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
            });
            debugOut(showDataDebugLegend, String.format("Chlorophyll stg 2 took %ss",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1)));
            
            int offset = EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES + EnvironmentalDataVisualization.OFFSET_REND_TXT_DATE_RANGES_DELTA * 4;
            String typeName = "Chlorophyll";
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

    public static class DataCollector<T extends BaseDataPoint<?>> implements
            java.util.stream.Collector<T, ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>, ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>> {
        
        public boolean ignoreDateLimitToLoad = false;
        public Date dateLimit;
        public StateRenderer2D renderer;
        public int offScreenBufferPixel;
        
        public int gridSpacing = 8;
        
        public BinaryOperator<ArrayList<Object>> merger;

        public LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
        public LongAccumulator toDatePts = new LongAccumulator((r, i) -> r = i > r ? i : r, 0);
        public LongAccumulator fromDatePts = new LongAccumulator((r, i) -> r = i < r ? i : r, Long.MAX_VALUE);

        public DataCollector(boolean ignoreDateLimitToLoad, Date dateLimit, StateRenderer2D renderer, 
                int offScreenBufferPixel, int gridSpacing, BinaryOperator<ArrayList<Object>> merger) {
            this.ignoreDateLimitToLoad = ignoreDateLimitToLoad;
            this.dateLimit = dateLimit;
            this.renderer = renderer; 
            this.offScreenBufferPixel = offScreenBufferPixel;
            this.gridSpacing = gridSpacing;
            this.merger = merger;
        }
        
        @Override
        public Supplier<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>> supplier() {
            return new Supplier<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>>() {
                @Override
                public ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> get() {
                    return new ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>();
                }
            };
        }

        @Override
        public BiConsumer<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>, T> accumulator() {
            return new BiConsumer<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>, T>() {
                @Override
                public void accept(ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> res, T dp) {
                    try {
                        if (res.isEmpty()) {
                            res.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                            res.add(new HashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                        }
                        
                        if (!ignoreDateLimitToLoad && dp.getDateUTC().before(dateLimit))
                            return;
                        
                        double latV = dp.getLat();
                        double lonV = dp.getLon();
                        ArrayList<Object> vals = dp.getAllDataValues();
                        
                        if (Double.isNaN(latV) || Double.isNaN(lonV))
                            return;
                        for (Object object : vals) {
                            if (object instanceof Number) {
                                double dv = ((Number) object).doubleValue();
                                if (Double.isNaN(dv) || !Double.isFinite(dv))
                                    return;
                            }
                        }
                        
                        Date dateV = new Date(dp.getDateUTC().getTime());

                        LocationType loc = new LocationType();
                        loc.setLatitudeDegs(latV);
                        loc.setLongitudeDegs(lonV);
                        
                        Point2D pt = renderer.getScreenPosition(loc);
                        
                        if (!isVisibleInRender(pt, renderer, offScreenBufferPixel))
                            return;
                        
                        visiblePts.accumulate(1);
                        
                        toDatePts.accumulate(dateV.getTime());
                        fromDatePts.accumulate(dateV.getTime());
                        
                        ArrayList<Point2D> pts = new ArrayList<>();
                        if (renderer.getLevelOfDetail() >= filterUseLOD)
                            pts.add((Point2D) pt.clone());

                        double x = pt.getX();
                        double y = pt.getY();
                        x = ((int) x) / gridSpacing * gridSpacing;
                        y = ((int) y) / gridSpacing * gridSpacing;
                        pt.setLocation(x, y);
                        pts.add(0, pt);

                        for (int idx = 0; idx < pts.size(); idx++) {
                            Point2D ptI = pts.get(idx);
                            if (!res.get(idx).containsKey(ptI)) {
                                res.get(idx).put(ptI, new Pair<>(vals, dateV));
                            }
                            else {
                                Pair<ArrayList<Object>, Date> pval = res.get(idx).get(ptI);
                                ArrayList<Object> pvals = pval.first();
                                vals = merger.apply(vals, pvals);
                                if (dateV.after(pval.second()))
                                    res.get(idx).put(ptI, new Pair<>(vals, dateV));
                                else
                                    res.get(idx).put(ptI, new Pair<>(vals, pval.second()));
                            }
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().debug(e);
                    }
                }
            };
        }

        @Override
        public BinaryOperator<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>> combiner() {
            return new BinaryOperator<ArrayList<Map<Point2D,Pair<ArrayList<Object>,Date>>>>() {
                @Override
                public ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> apply(
                        ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> res,
                        ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> resInt) {
                    for (int idxc = 0; idxc < 2; idxc++) {
                        final int idx = idxc;
                        resInt.get(idx).keySet().stream().forEach(k1 -> {
                            try {
                                Pair<ArrayList<Object>, Date> sI = resInt.get(idx).get(k1);
                                if (res.get(idx).containsKey(k1)) {
                                    Pair<ArrayList<Object>, Date> s = res.get(idx).get(k1);
                                    ArrayList<Object> vals = merger.apply(s.first(), sI.first());
                                    Date valDate = sI.second().after(s.second()) ? new Date(sI.second().getTime())
                                            : s.second();
                                    res.get(idx).put(k1, new Pair<ArrayList<Object>, Date>(vals, valDate));
                                }
                                else {
                                    res.get(idx).put(k1, sI);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e);
                            }
                        });
                    }
                    return res;
                }
            };
        }

        @Override
        public Function<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>, ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>> finisher() {
            return new Function<ArrayList<Map<Point2D,Pair<ArrayList<Object>,Date>>>, ArrayList<Map<Point2D,Pair<ArrayList<Object>,Date>>>>() {
                @Override
                public ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> apply(
                        ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> t) {
                    return t;
                }
            };
        }

        /* (non-Javadoc)
         * @see java.util.stream.Collector#characteristics()
         */
        @Override
        public Set<java.util.stream.Collector.Characteristics> characteristics() {
            return EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED);
        }
    }
}
