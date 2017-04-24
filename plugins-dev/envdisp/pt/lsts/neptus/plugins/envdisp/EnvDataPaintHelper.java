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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAccumulator;

import org.apache.commons.lang3.tuple.Triple;

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
            LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
            LongAccumulator toDatePts = new LongAccumulator((r, i) -> r = i > r ? i : r, 0);
            LongAccumulator fromDatePts = new LongAccumulator((r, i) -> r = i < r ? i : r, Long.MAX_VALUE);
            Map<Point2D, Pair<Pair<Double, Double>, Date>> ptFilt = dest.parallelStream()
                    .collect(HashMap<Point2D, Pair<Pair<Double, Double>, Date>>::new, (res, dp) -> {
                        try {
                            if (!ignoreDateLimitToLoad && dp.getDateUTC().before(dateLimit))
                                return;
                            
                            double latV = dp.getLat();
                            double lonV = dp.getLon();
                            double speedCmSV = dp.getSpeedCmS();
                            double headingV = AngleUtils.nomalizeAngleDegrees360(dp.getHeadingDegrees());
                            
                            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(speedCmSV)
                                    || Double.isNaN(headingV))
                                return;
                            
                            Date dateV = new Date(dp.getDateUTC().getTime());

                            LocationType loc = new LocationType();
                            loc.setLatitudeDegs(latV);
                            loc.setLongitudeDegs(lonV);
                            
                            Point2D pt = renderer.getScreenPosition(loc);
                            
                            if (!EnvDataPaintHelper.isVisibleInRender(pt, renderer, offScreenBufferPixel))
                                return;
                            
                            visiblePts.accumulate(1);
                            
                            toDatePts.accumulate(dateV.getTime());
                            fromDatePts.accumulate(dateV.getTime());
                            
                            double x = pt.getX();
                            double y = pt.getY();
                            x = x - x % EnvDataShapesHelper.ARROW_RADIUS;
                            y = y - y % EnvDataShapesHelper.ARROW_RADIUS;
                            pt.setLocation(x, y);
                            
                            if (!res.containsKey(pt)) {
                                res.put(pt, new Pair<Pair<Double, Double>, Date>(new Pair<Double, Double>(speedCmSV, headingV), dateV));
                            }
                            else {
                                Pair<Pair<Double, Double>, Date> pval = res.get(pt);
                                double val = pval.first().first();
                                val = (val + speedCmSV) / 2d;
                                double val1 = pval.first().second();
                                val1 = (val1 + headingV) / 2d;
                                if (dateV.after(pval.second()))
                                    res.put(pt, new Pair<Pair<Double, Double>, Date>(new Pair<Double, Double>(val, val1), dateV));
                                else
                                    res.put(pt, new Pair<Pair<Double, Double>, Date>(new Pair<Double, Double>(val, val1), pval.second()));
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().debug(e);
                        }
                    }, (res, resInt) -> {
                        resInt.keySet().stream().forEach(k1 -> {
                            try {
                                Pair<Pair<Double, Double>, Date> sI = resInt.get(k1);
                                if (res.containsKey(k1)) {
                                    Pair<Pair<Double, Double>, Date> s = res.get(k1);
                                    double val = (s.first().first() + sI.first().first()) / 2d;
                                    double val1 = (s.first().second() + sI.first().second()) / 2d;
                                    Date valDate = sI.second().after(s.second()) ? new Date(sI.second().getTime())
                                            : s.second();
                                    res.put(k1, new Pair<Pair<Double, Double>, Date>(new Pair<Double, Double>(val, val1), valDate));
                                }
                                else {
                                    res.put(k1, sI);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e);
                            }
                        });
                    });
            
            debugOut(showDataDebugLegend, String.format("Currents stg 1 took %ss :: %d of %d from %d (%f%%)",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.size(), visiblePts.longValue(), dest.size(),
                    (ptFilt.size() * 1. / visiblePts.longValue()) * 100));
            stMillis = System.currentTimeMillis();
            
            ptFilt.keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Pair<Pair<Double, Double>, Date> pVal = ptFilt.get(pt);
                    
                    double speedCmSV = pVal.first().first();
                    double headingV = pVal.first().second();
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
            LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
            LongAccumulator toDatePts = new LongAccumulator((r, i) -> r = i > r ? i : r, 0);
            LongAccumulator fromDatePts = new LongAccumulator((r, i) -> r = i < r ? i : r, Long.MAX_VALUE);
            Map<Point2D, Pair<Double, Date>> ptFilt = dest.parallelStream()
                    .collect(HashMap<Point2D, Pair<Double, Date>>::new, (res, dp) -> {
                        try {
                            if (!ignoreDateLimitToLoad && dp.getDateUTC().before(dateLimit))
                                return;
                            
                            double latV = dp.getLat();
                            double lonV = dp.getLon();
                            double sstV = dp.getSst();
                            
                            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(sstV))
                                return;
                            
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
                            
                            double x = pt.getX();
                            double y = pt.getY();
                            x = x - x % EnvDataShapesHelper.CIRCLE_RADIUS;
                            y = y - y % EnvDataShapesHelper.CIRCLE_RADIUS;
                            pt.setLocation(x, y);
                            
                            if (!res.containsKey(pt)) {
                                res.put(pt, new Pair<>(sstV, dateV));
                            }
                            else {
                                Pair<Double, Date> pval = res.get(pt);
                                double val = pval.first();
                                val = (val + sstV) / 2d;
                                if (dateV.after(pval.second()))
                                    res.put(pt, new Pair<>(val, dateV));
                                else
                                    res.put(pt, new Pair<>(val, pval.second()));
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().debug(e);
                        }
                    }, (res, resInt) -> {
                        resInt.keySet().stream().forEach(k1 -> {
                            try {
                                Pair<Double, Date> sI = resInt.get(k1);
                                if (res.containsKey(k1)) {
                                    Pair<Double, Date> s = res.get(k1);
                                    double val = (s.first() + sI.first()) / 2d;
                                    Date valDate = sI.second().after(s.second()) ? new Date(sI.second().getTime())
                                            : s.second();
                                    res.put(k1, new Pair<Double, Date>(val, valDate));
                                }
                                else {
                                    res.put(k1, sI);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e);
                            }
                        });
                    });
            
            debugOut(showDataDebugLegend, String.format("SST stg 1 took %ss :: %d of %d from %d (%f%%)",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.size(), visiblePts.longValue(), dest.size(),
                    (ptFilt.size() * 1. / visiblePts.longValue()) * 100));
            stMillis = System.currentTimeMillis();
            
            ptFilt.keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Pair<Double,Date> pVal = ptFilt.get(pt);
                    
                    gt = (Graphics2D) g2.create();
                    gt.translate(pt.getX(), pt.getY());
                    Color color = Color.WHITE;
                    color = colorMapSST.getColor((pVal.first() - minSST) / (maxSST - minSST));
                    if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    gt.draw(EnvDataShapesHelper.circle);
                    gt.fill(EnvDataShapesHelper.circle);
                    
                    if (showSSTLegend && renderer.getLevelOfDetail() >= showSSTLegendFromZoomLevel) {
                        gt.setFont(font8Pt);
                        gt.setColor(Color.WHITE);
                        gt.drawString(MathMiscUtils.round(pVal.first(), 1) + "\u00B0C", -15, 15);
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
            LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
            LongAccumulator toDatePts = new LongAccumulator((r, i) -> r = i > r ? i : r, 0);
            LongAccumulator fromDatePts = new LongAccumulator((r, i) -> r = i < r ? i : r, Long.MAX_VALUE);
            Map<Point2D, Triple<Double, Double, Date>> ptFilt = dest.parallelStream()
                    .collect(HashMap<Point2D, Triple<Double, Double, Date>>::new, (res, dp) -> {
                        try {
                            if (!ignoreDateLimitToLoad && dp.getDateUTC().before(dateLimit))
                                return;
                            
                            double latV = dp.getLat();
                            double lonV = dp.getLon();
                            double speedV = dp.getSpeed();
                            double headingV = AngleUtils.nomalizeAngleDegrees360(dp.getHeading());
                            
                            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(speedV)|| Double.isNaN(headingV))
                                return;
                            
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
                            
                            double x = pt.getX();
                            double y = pt.getY();
                            x = x - x % EnvDataShapesHelper.WIND_BARB_RADIUS;
                            y = y - y % EnvDataShapesHelper.WIND_BARB_RADIUS;
                            pt.setLocation(x, y);
                            
                            if (!res.containsKey(pt)) {
                                res.put(pt, Triple.of(speedV, headingV, dateV));
                            }
                            else {
                                Triple<Double, Double, Date> pval = res.get(pt);
                                double val = pval.getLeft();
                                val = (val + speedV) / 2d;
                                double val1 = pval.getMiddle();
                                val1 = (val1 + headingV) / 2d;
                                if (dateV.after(pval.getRight()))
                                    res.put(pt, Triple.of(val, val1, dateV));
                                else
                                    res.put(pt, Triple.of(val, val1, pval.getRight()));
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().debug(e);
                        }
                    }, (res, resInt) -> {
                        resInt.keySet().stream().forEach(k1 -> {
                            try {
                                Triple<Double, Double, Date> sI = resInt.get(k1);
                                if (res.containsKey(k1)) {
                                    Triple<Double, Double, Date> s = res.get(k1);
                                    double val = (s.getLeft() + sI.getLeft()) / 2d;
                                    double val1 = (s.getMiddle() + sI.getMiddle()) / 2d;
                                    Date valDate = sI.getRight().after(s.getRight()) ? new Date(sI.getRight().getTime())
                                            : s.getRight();
                                    res.put(k1, Triple.of(val, val1, valDate));
                                }
                                else {
                                    res.put(k1, sI);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e);
                            }
                        });
                    });
            
            debugOut(showDataDebugLegend, String.format("Wind stg 1 took %ss :: %d of %d from %d (%f%%)",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.size(), visiblePts.longValue(), dest.size(),
                    (ptFilt.size() * 1. / visiblePts.longValue()) * 100));
            stMillis = System.currentTimeMillis();
            
            ptFilt.keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Triple<Double, Double, Date> pVal = ptFilt.get(pt);
                    double speedV = pVal.getLeft();
                    double headingV = AngleUtils.nomalizeAngleDegrees360(pVal.getMiddle());
                    Date dateV = pVal.getRight();
                    
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
            LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
            LongAccumulator toDatePts = new LongAccumulator((r, i) -> r = i > r ? i : r, 0);
            LongAccumulator fromDatePts = new LongAccumulator((r, i) -> r = i < r ? i : r, Long.MAX_VALUE);
            Map<Point2D, Pair<Triple<Double, Double, Double>, Date>> ptFilt = dest.parallelStream()
                    .collect(HashMap<Point2D, Pair<Triple<Double, Double, Double>, Date>>::new, (res, dp) -> {
                        try {
                            if (!ignoreDateLimitToLoad && dp.getDateUTC().before(dateLimit))
                                return;
                            
                            double latV = dp.getLat();
                            double lonV = dp.getLon();
                            double sigHeightV = dp.getSignificantHeight();
                            double headingV = AngleUtils.nomalizeAngleDegrees360(dp.getPeakDirection());
                            double periodV = dp.getPeakPeriod();
                            
                            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(sigHeightV) || Double.isNaN(headingV)
                                    || Double.isNaN(periodV))
                                return;
                            
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
                            
                            double x = pt.getX();
                            double y = pt.getY();
                            x = x - x % EnvDataShapesHelper.ARROW_RADIUS;
                            y = y - y % EnvDataShapesHelper.ARROW_RADIUS;
                            pt.setLocation(x, y);
                            
                            if (!res.containsKey(pt)) {
                                res.put(pt, new Pair<Triple<Double, Double, Double>, Date>(Triple.of(sigHeightV, headingV, periodV), dateV));
                            }
                            else {
                                Pair<Triple<Double, Double, Double>, Date> pval = res.get(pt);
                                double val = pval.first().getLeft();
                                val = (val + sigHeightV) / 2d;
                                double val1 = pval.first().getMiddle();
                                val1 = (val1 + headingV) / 2d;
                                double val2 = pval.first().getRight();
                                val2 = (val2 + periodV) / 2d;
                                if (dateV.after(pval.second()))
                                    res.put(pt, new Pair<Triple<Double, Double, Double>, Date>(Triple.of(val, val1, val2), dateV));
                                else
                                    res.put(pt, new Pair<Triple<Double, Double, Double>, Date>(Triple.of(val, val1, val2), pval.second()));
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().debug(e);
                        }
                    }, (res, resInt) -> {
                        resInt.keySet().stream().forEach(k1 -> {
                            try {
                                Pair<Triple<Double,Double,Double>,Date> sI = resInt.get(k1);
                                if (res.containsKey(k1)) {
                                    Pair<Triple<Double, Double, Double>, Date> s = res.get(k1);
                                    double val = (s.first().getLeft() + sI.first().getLeft()) / 2d;
                                    double val1 = (s.first().getMiddle() + sI.first().getMiddle()) / 2d;
                                    double val2 = (s.first().getRight() + sI.first().getRight()) / 2d;
                                    Date valDate = sI.second().after(s.second()) ? new Date(sI.second().getTime())
                                            : s.second();
                                    res.put(k1, new Pair<Triple<Double, Double, Double>, Date>(Triple.of(val, val1, val2), valDate));
                                }
                                else {
                                    res.put(k1, sI);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e);
                            }
                        });
                    });
            
            debugOut(showDataDebugLegend, String.format("Waves stg 1 took %ss :: %d of %d from %d (%f%%)",
                    MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - stMillis) / 1E3, 1), ptFilt.size(), visiblePts.longValue(), dest.size(),
                    (ptFilt.size() * 1. / visiblePts.longValue()) * 100));
            stMillis = System.currentTimeMillis();
            
            ptFilt.keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Pair<Triple<Double, Double, Double>, Date> pVal = ptFilt.get(pt);
                    double sigHeightV = pVal.first().getLeft();
                    double headingV = AngleUtils.nomalizeAngleDegrees360(pVal.first().getMiddle());
                    @SuppressWarnings("unused")
                    double periodV = pVal.first().getRight();
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

}
