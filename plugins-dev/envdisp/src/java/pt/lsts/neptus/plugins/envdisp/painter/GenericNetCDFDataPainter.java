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
 * Apr 22, 2018
 */
package pt.lsts.neptus.plugins.envdisp.painter;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.lsts.neptus.colormap.ColorBarPainterUtil;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info;
import pt.lsts.neptus.plugins.envdisp.painter.EnvDataPaintHelper.PointPaintEnum;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;

/**
 * @author pdias
 *
 */
public class GenericNetCDFDataPainter {
    private OffScreenLayerImageControl offScreen = new OffScreenLayerImageControl();
    private Font font = new Font("Helvetica", Font.PLAIN, 9);
    
    private Info info = null;
    // ID is lat/lon/<depth>
    private final Map<String, GenericDataPoint> dataPointsVar;
    
    private Thread painterThread = null;
    private AtomicBoolean abortIndicator = null;

    private long plotUniqueId = 0;
    
    @NeptusProperty
    private String plotName = "";

    @NeptusProperty(editable = false)
    private String netCDFFile = null;;
    @NeptusProperty(editable = false)
    private String varName = "";

    @NeptusProperty
    private boolean showVar = true;
    @NeptusProperty
    private boolean showVarLegend = false;
    @NeptusProperty
    private int showVarLegendFromZoomLevel = 13;
    @NeptusProperty
    private boolean showVarColorbar = true;

    @NeptusProperty
    private ColorMap colorMapVar = ColorMapFactory.createJetColorMap();
    @NeptusProperty
    private boolean isLogColorMap = false;
    
    @NeptusProperty
    private boolean interpolate = true;
    @NeptusProperty
    private boolean isClampToFit = false;
    @NeptusProperty
    private boolean showGradient = false;

    @NeptusProperty(description = "A value between 10 and 255 (the higher the more opaque)")
    private int transparency = 128;

    @NeptusProperty
    private double minValue = Double.MIN_VALUE;
    @NeptusProperty
    private double maxValue = Double.MAX_VALUE;

    @NeptusProperty
    private Date minDate = new Date(0);
    @NeptusProperty
    private Date maxDate = new Date(Long.MAX_VALUE);

    @NeptusProperty
    private double minDepth = Double.MIN_VALUE;
    @NeptusProperty
    private double maxDepth = Double.MAX_VALUE;
    
    @NeptusProperty(editable = false)
    private Map<String, String> additionalParams = new HashMap<>();    

    public GenericNetCDFDataPainter(long plotUniqueId, Map<String, GenericDataPoint> dataPointsVar) throws Exception {
        if (dataPointsVar == null || dataPointsVar.isEmpty())
            throw new Exception("Empty data set found!");
        
        this.dataPointsVar = dataPointsVar;
        this.info = this.dataPointsVar.values().iterator().next().getInfo();
        
        this.varName = this.info.name;
        this.plotName = this.varName + " :: " + this.info.fullName;
        
        this.minValue = this.info.minVal;
        this.maxValue = this.info.maxVal;

        this.minDepth = Double.isFinite(this.info.minDepth) ? this.info.minDepth : this.minDepth;
        this.maxDepth = Double.isFinite(this.info.maxDepth) ? this.info.maxDepth : this.maxDepth;

        this.minDate = this.info.minDate.getTime() != 0 ? this.info.minDate : this.minDate;
        this.maxDate = this.info.maxDate.getTime() != 0 ? this.info.maxDate : this.maxDate;

        switch (this.info.scalarOrLogPreference) {
            case LOG10:
                this.isLogColorMap = true;
                break;
            case SCALAR:
            default:
                break;
        }
    }
    
    /**
     * @return the dataPointsVar
     */
    public Map<String, GenericDataPoint> getDataPointsVar() {
        return dataPointsVar;
    }
    
    /**
     * @return the offScreen
     */
    public OffScreenLayerImageControl getOffScreenLayer() {
        return offScreen;
    }
    
    /**
     * @return the plotUniqueId
     */
    public long getPlotUniqueId() {
        return plotUniqueId;
    }
    
    /**
     * @return the font
     */
    public Font getFont() {
        return font;
    }
    
    /**
     * @param font the font to set
     */
    public void setFont(Font font) {
        this.font = font;
    }
    
    /**
     * @return the plotName
     */
    public String getPlotName() {
        return plotName;
    }
    
    /**
     * @param plotName the plotName to set
     */
    public void setPlotName(String plotName) {
        this.plotName = plotName;
    }
    
    /**
     * @return the netCDFFile
     */
    public String getNetCDFFile() {
        return netCDFFile;
    }

    /**
     * @param netCDFFile the netCDFFile to set
     */
    public void setNetCDFFile(String netCDFFile) {
        this.netCDFFile = netCDFFile;
    }

    /**
     * @return the info
     */
    public Info getInfo() {
        return info;
    }
    
    /**
     * @return the varName
     */
    public String getVarName() {
        return varName;
    }

    /**
     * @param varName the varName to set
     */
    public void setVarName(String varName) {
        this.varName = varName;
    }

    /**
     * @return the showVar
     */
    public boolean isShowVar() {
        return showVar;
    }

    /**
     * @param showVar the showVar to set
     */
    public void setShowVar(boolean showVar) {
        this.showVar = showVar;
    }

    /**
     * @return the showVarLegend
     */
    public boolean isShowVarLegend() {
        return showVarLegend;
    }

    /**
     * @param showVarLegend the showVarLegend to set
     */
    public void setShowVarLegend(boolean showVarLegend) {
        this.showVarLegend = showVarLegend;
    }

    /**
     * @return the showVarLegendFromZoomLevel
     */
    public int getShowVarLegendFromZoomLevel() {
        return showVarLegendFromZoomLevel;
    }

    /**
     * @param showVarLegendFromZoomLevel the showVarLegendFromZoomLevel to set
     */
    public void setShowVarLegendFromZoomLevel(int showVarLegendFromZoomLevel) {
        this.showVarLegendFromZoomLevel = showVarLegendFromZoomLevel;
    }

    /**
     * @return the showVarColorbar
     */
    public boolean isShowVarColorbar() {
        return showVarColorbar;
    }

    /**
     * @param showVarColorbar the showVarColorbar to set
     */
    public void setShowVarColorbar(boolean showVarColorbar) {
        this.showVarColorbar = showVarColorbar;
    }

    /**
     * @return the colorMapVar
     */
    public ColorMap getColorMapVar() {
        return colorMapVar;
    }

    /**
     * @param colorMapVar the colorMapVar to set
     */
    public void setColorMapVar(ColorMap colorMapVar) {
        this.colorMapVar = colorMapVar;
    }

    /**
     * @return the isLogColorMap
     */
    public boolean isLogColorMap() {
        return isLogColorMap;
    }
    
    /**
     * @param isLogColorMap the isLogColorMap to set
     */
    public void setLogColorMap(boolean isLogColorMap) {
        this.isLogColorMap = isLogColorMap;
    }
    
    /**
     * @return the interpolate
     */
    public boolean isInterpolate() {
        return interpolate;
    }
    
    /**
     * @param interpolate the interpolate to set
     */
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }
    
    /**
     * @return the clampToFit
     */
    public boolean isClampToFit() {
        return isClampToFit;
    }
    
    /**
     * @param isClampToFit the isClampToFit to set
     */
    public void setClampToFit(boolean isClampToFit) {
        this.isClampToFit = isClampToFit;
    }
    
    /**
     * @return the minValue
     */
    public double getMinValue() {
        return minValue;
    }

    /**
     * @param minValue the minValue to set
     */
    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    /**
     * @return the maxValue
     */
    public double getMaxValue() {
        return maxValue;
    }

    /**
     * @param maxValue the maxValue to set
     */
    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * @return the minDate
     */
    public Date getMinDate() {
        return minDate;
    }

    /**
     * @param minDate the minDate to set
     */
    public void setMinDate(Date minDate) {
        this.minDate = minDate;
    }

    /**
     * @return the maxDate
     */
    public Date getMaxDate() {
        return maxDate;
    }

    /**
     * @param maxDate the maxDate to set
     */
    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    /**
     * @return the minDepth
     */
    public double getMinDepth() {
        return minDepth;
    }

    /**
     * @param minDepth the minDepth to set
     */
    public void setMinDepth(double minDepth) {
        this.minDepth = minDepth;
    }

    /**
     * @return the maxDepth
     */
    public double getMaxDepth() {
        return maxDepth;
    }

    /**
     * @param maxDepth the maxDepth to set
     */
    public void setMaxDepth(double maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * @return the showGradient
     */
    public boolean isShowGradient() {
        return showGradient;
    }
    
    /**
     * @param showGradient the showGradient to set
     */
    public void setShowGradient(boolean showGradient) {
        this.showGradient = showGradient;
    }

    /**
     * @return the transparency
     */
    public int getTransparency() {
        return transparency;
    }
    
    /**
     * @param transparency the transparency to set
     */
    public void setTransparency(int transparency) {
        this.transparency = transparency;
    }
    
    /**
     * @return the additionalParams
     */
    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    /**
     * @param additionalParams the additionalParams to set
     */
    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }

    public void paint(Graphics2D go, StateRenderer2D renderer, boolean showDataDebugLegend) {
        boolean recreateImage = offScreen.paintPhaseStartTestRecreateImageAndRecreate(go, renderer);
        if (recreateImage) {
            if (painterThread != null) {
                try {
                    abortIndicator.set(true);
                    painterThread.interrupt();
                    //System.out.println("WWWWWWWWWW " + abortIndicator + "  " +  painterThread.isInterrupted() + " " + painterThread.isAlive());
                    painterThread = null;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            final MapTileRendererCalculator rendererCalculator = new MapTileRendererCalculator(renderer);
            abortIndicator = new AtomicBoolean();
            painterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Graphics2D g2 = offScreen.getImageGraphics();

                        Date dateColorLimit = new Date(System.currentTimeMillis() - 3 * DateTimeUtil.HOUR);
                        
                        if (showVar) {
                            try {
                                PointPaintEnum paintType = EnvDataPaintHelper.PointPaintEnum.INTERPOLATE;
                                if (!interpolate) {
                                    paintType = EnvDataPaintHelper.PointPaintEnum.POINT;
                                }
                                
                                EnvDataPaintHelper.paintGenericInGraphics(rendererCalculator, g2,
                                        (int) MathMiscUtils.clamp(transparency, 10, 255), dateColorLimit, dataPointsVar,
                                        offScreen.getOffScreenBufferPixel(), colorMapVar, minValue, maxValue,
                                        showVarLegend, showVarLegendFromZoomLevel, font, showDataDebugLegend,
                                        abortIndicator, paintType, isLogColorMap, isClampToFit, showGradient,
                                        new Pair<Date, Date>(minDate, maxDate),
                                        new Pair<Double, Double>(minDepth, maxDepth));
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreen.triggerImageRebuild();
                            }
                        }
                        
                        g2.dispose();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        offScreen.triggerImageRebuild();
                    }
                    catch (Error e) {
                        e.printStackTrace();
                        offScreen.triggerImageRebuild();
                    }
                }
            }, "EnvDisp::" + varName + "::Painter");
            painterThread.setDaemon(true);
            painterThread.start();
        }            
        offScreen.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(go, renderer);
        
        paintColorbars(go, renderer);
    }

    /**
     * @param go
     * @param renderer
     */
    private void paintColorbars(Graphics2D go, StateRenderer2D renderer) {
        int offsetHeight = 130;
        int offsetWidth = 5;
        int offsetDelta = 130;
        if (showVar && showVarColorbar) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colorMapVar, I18n.text(info.name), info.unit, minValue, maxValue);
            gl.dispose();
            offsetHeight += offsetDelta;
        }
    }
    
    public static void main(String[] args) {
        
    }
}
