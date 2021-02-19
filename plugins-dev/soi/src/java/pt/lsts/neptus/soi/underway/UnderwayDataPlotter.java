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
 * May 29, 2018
 */
package pt.lsts.neptus.soi.underway;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.IConsoleInteraction;
import pt.lsts.neptus.console.IConsoleLayer;
import pt.lsts.neptus.messages.listener.Periodic;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;
import pt.lsts.neptus.util.nmea.NmeaListener;
import pt.lsts.neptus.util.nmea.NmeaProvider;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "Underway Data Plotter", description = "Plotter for Falkor underway data.")
public class UnderwayDataPlotter extends ConsoleLayer implements NmeaListener, PreferencesListener {

    @NeptusProperty(name = "Show temperature", userLevel = LEVEL.REGULAR, category = "Temperature")
    private boolean showTemp = false;
    @NeptusProperty(name = "Show salinity", userLevel = LEVEL.REGULAR, category = "Salinity")
    private boolean showSal = true;

    @NeptusProperty(name = "Min salinity", userLevel = LEVEL.REGULAR, category = "Salinity")
    private double minSal = 33;
    @NeptusProperty(name = "Max salinity", userLevel = LEVEL.REGULAR, category = "Salinity")
    private double maxSal = 36;

    @NeptusProperty(name = "Salinity color map", userLevel = LEVEL.REGULAR, category = "Salinity")
    private ColorMap colormapSal = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min temperature", userLevel = LEVEL.REGULAR, category = "Temperature")
    private double minTemp = 15;
    @NeptusProperty(name = "Max temperature", userLevel = LEVEL.REGULAR, category = "Temperature")
    private double maxTemp = 35;

    @NeptusProperty(name = "Temperature color map", userLevel = LEVEL.REGULAR, category = "Temperature")
    private ColorMap colormapTemp = ColorMapFactory.createJetColorMap();
    
    @NeptusProperty(name = "Max samples", userLevel = LEVEL.REGULAR)
    private int maxSamples = 35000;

    @NeptusProperty(name = "Clamp to fit", userLevel = LEVEL.REGULAR)
    private boolean clampToFit = false;

    private OffScreenLayerImageControl offScreenSalinity = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenTemperature = new OffScreenLayerImageControl();
    
    private Thread painterThread = null;
    private AtomicBoolean abortIndicator = null;

    private List<DataPoint> points = Collections.synchronizedList(new LinkedList<>());
    
    private AtomicInteger trigger = new AtomicInteger(0);
    private int triggerCount = 0;
    
    private class DataPoint {
        LocationType location = null;
        double temperature = Double.NaN;
        double salinity = Double.NaN;
    }

    private class DataPointXY {
        Point2D point = null;
        double temperature = Double.NaN;
        double salinity = Double.NaN;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
        Vector<NmeaProvider> lst = getConsole().getSubPanelsOfInterface(NmeaProvider.class);
        lst.stream().forEach(p -> p.addListener(UnderwayDataPlotter.this));
        List<IConsoleInteraction> lstInt = getConsole().getInteractions().stream().filter(i -> i instanceof NmeaProvider).collect(Collectors.toList());
        lstInt.stream().forEach(p -> ((NmeaProvider) p).addListener(UnderwayDataPlotter.this));
        List<IConsoleLayer> lstCL = getConsole().getLayers().stream().filter(i -> i instanceof NmeaProvider).collect(Collectors.toList());
        lstCL.stream().forEach(p -> ((NmeaProvider) p).addListener(UnderwayDataPlotter.this));
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        Vector<NmeaProvider> lst = getConsole().getSubPanelsOfInterface(NmeaProvider.class);
        lst.stream().forEach(p -> p.removeListener(UnderwayDataPlotter.this));
        List<IConsoleInteraction> lstInt = getConsole().getInteractions().stream().filter(i -> i instanceof NmeaProvider).collect(Collectors.toList());
        lstInt.stream().forEach(p -> ((NmeaProvider) p).removeListener(UnderwayDataPlotter.this));
        List<IConsoleLayer> lstCL = getConsole().getLayers().stream().filter(i -> i instanceof NmeaProvider).collect(Collectors.toList());
        lstCL.stream().forEach(p -> ((NmeaProvider) p).removeListener(UnderwayDataPlotter.this));
        
        points.clear();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.util.nmea.NmeaListener#nmeaSentence(java.lang.String)
     */
    @Override
    public void nmeaSentence(String sentence) {
        if (sentence.startsWith("t1")) {
            LocationType loc = MyState.getLocation().getNewAbsoluteLatLonDepth();
            
            DataPoint pt = new DataPoint();
            pt.location = loc;
            
            String[] tks = sentence.split(",");
            for (String tk : tks) {
                try {
                    String[] tts = tk.trim().split("=");
                    if (tts.length < 2)
                        continue;
                    switch (tts[0].trim()) {
                        case "t1":
                            pt.temperature = Double.parseDouble(tts[1]);
                            break;
                        case "s":
                            pt.salinity= Double.parseDouble(tts[1]);
                            break;
                        default:
                            break;
                    }
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            
            try {
                Salinity msgSal = new Salinity(Double.valueOf(pt.salinity).floatValue());
                msgSal.setSrc(GeneralPreferences.imcCcuId.intValue());
                msgSal.setTimestampMillis(System.currentTimeMillis());
                LsfMessageLogger.log(msgSal);
                Temperature msgTemp = new Temperature(Double.valueOf(pt.temperature).floatValue());
                msgTemp.setSrc(GeneralPreferences.imcCcuId.intValue());
                msgTemp.setTimestampMillis(System.currentTimeMillis());
                LsfMessageLogger.log(msgTemp);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            points.add(pt);
            if (points.size() > maxSamples)
                points.remove(0);
            trigger.incrementAndGet();
        }
    }

    @Periodic(value = 2000)
    private void update() {
        offScreenSalinity.triggerImageRebuild();
        offScreenTemperature.triggerImageRebuild();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        
        boolean recreateImage = offScreenSalinity.paintPhaseStartTestRecreateImageAndRecreate(g, renderer)
                || offScreenTemperature.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        
        int c = trigger.get();
        if (c != triggerCount) {
            recreateImage = true;
            triggerCount = c;
        }
        
        if (recreateImage) {
            if (painterThread != null) {
                try {
                    abortIndicator.set(true);
                    painterThread.interrupt();
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
                        Graphics2D g2 = offScreenSalinity.getImageGraphics();

                        ArrayList<DataPointXY> pts = new ArrayList<>();
                        
                        if (showSal || showTemp) {
                            pts = points.stream().collect(ArrayList<DataPointXY>::new,
                            (r, p) -> {
                                if (abortIndicator.get())
                                    return;
                                Point2D pxy = rendererCalculator.getScreenPosition(p.location);
                                DataPointXY dpxy = new DataPointXY();
                                dpxy.point = pxy;
                                dpxy.salinity = p.salinity;
                                dpxy.temperature = p.temperature;
                                r.add(dpxy);
                            }, (r1, r2) -> {
                                for (DataPointXY d2 : r2) {
                                    if (abortIndicator.get())
                                        break;
                                    boolean found = false;
                                    for (DataPointXY d1 : r1) {
                                        if (abortIndicator.get())
                                            break;
                                        if (d2.point.equals(d1.point)) {
                                            d1.salinity = (d1.salinity + d2.salinity) / 2.;
                                            d1.temperature = (d1.temperature + d2.temperature) / 2.;
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found)
                                        r1.add(d2);
                                }
                            });
                        }
                        
                        if (showTemp && !abortIndicator.get()) {
                            try {
                                double fullImgWidth = rendererCalculator.getSize().getWidth() + offScreenTemperature.getOffScreenBufferPixel() * 2.;
                                double fullImgHeight = rendererCalculator.getSize().getHeight() + offScreenTemperature.getOffScreenBufferPixel() * 2.;

                                double xMin = fullImgWidth;
                                double yMin = fullImgHeight;
                                xMin = 8;
                                yMin = 8;

                                double cacheImgScaleX = 1. / xMin;
                                double cacheImgScaleY = 1. / yMin;

                                double cacheImgWidth = fullImgWidth;
                                double cacheImgHeight = fullImgHeight;
                                cacheImgWidth *= cacheImgScaleX;
                                cacheImgHeight *= cacheImgScaleY;
                                
                                BufferedImage cacheImg = createBufferedImage((int) cacheImgWidth, (int) cacheImgHeight, Transparency.TRANSLUCENT);
                                pts.parallelStream().forEach(pt -> {
                                    try {
                                        if (abortIndicator.get())
                                            return;

                                        double v = (double) pt.temperature;
                                        
                                        if (clampToFit
                                                && (Double.compare(v, minTemp) < 0 || Double.compare(v, maxTemp) > 0))
                                            return;

                                        Color color = colormapTemp.getColor(ColorMapUtils.getColorIndexZeroToOneLog10(v, minTemp, maxTemp));
                                        cacheImg.setRGB((int) ((pt.point.getX() + offScreenTemperature.getOffScreenBufferPixel()) * cacheImgScaleX),
                                                (int) ((pt.point.getY() + offScreenTemperature.getOffScreenBufferPixel()) * cacheImgScaleY), color.getRGB());
                                    }
                                    catch (Exception e) {
                                        NeptusLog.pub().trace(e);
                                    }
                                });

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
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenTemperature.triggerImageRebuild();
                            }
                        }
                        
                        if (showSal && !abortIndicator.get()) {
                            try {
                                double fullImgWidth = rendererCalculator.getSize().getWidth() + offScreenSalinity.getOffScreenBufferPixel() * 2.;
                                double fullImgHeight = rendererCalculator.getSize().getHeight() + offScreenSalinity.getOffScreenBufferPixel() * 2.;

                                double xMin = fullImgWidth;
                                double yMin = fullImgHeight;
                                xMin = 8;
                                yMin = 8;

                                double cacheImgScaleX = 1. / xMin;
                                double cacheImgScaleY = 1. / yMin;

                                double cacheImgWidth = fullImgWidth;
                                double cacheImgHeight = fullImgHeight;
                                cacheImgWidth *= cacheImgScaleX;
                                cacheImgHeight *= cacheImgScaleY;
                                
                                BufferedImage cacheImg = createBufferedImage((int) cacheImgWidth, (int) cacheImgHeight, Transparency.TRANSLUCENT);
                                pts.parallelStream().forEach(pt -> {
                                    try {
                                        if (abortIndicator.get())
                                            return;

                                        double v = (double) pt.salinity;
                                        
                                        if (clampToFit
                                                && (Double.compare(v, minSal) < 0 || Double.compare(v, maxSal) > 0))
                                            return;

                                        Color color = colormapSal.getColor(ColorMapUtils.getColorIndexZeroToOneLog10(v, minSal, maxSal));
                                        cacheImg.setRGB((int) ((pt.point.getX() + offScreenSalinity.getOffScreenBufferPixel()) * cacheImgScaleX),
                                                (int) ((pt.point.getY() + offScreenSalinity.getOffScreenBufferPixel()) * cacheImgScaleY), color.getRGB());
                                    }
                                    catch (Exception e) {
                                        NeptusLog.pub().trace(e);
                                    }
                                });

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
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenSalinity.triggerImageRebuild();
                            }
                        }

                        g2.dispose();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        offScreenSalinity.triggerImageRebuild();
                    }
                    catch (Error e) {
                        e.printStackTrace();
                        offScreenSalinity.triggerImageRebuild();
                    }
                    
                    renderer.invalidate();
                    renderer.repaint(200);
                }
            }, "UnderwayData:: Painter");
            painterThread.setDaemon(true);
            painterThread.start();
        }            
        offScreenTemperature.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
        offScreenSalinity.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
        
        paintColorbars(g, renderer);
    }
    
    private static BufferedImage createBufferedImage(int cacheImgWidth, int cacheImgHeight, int translucent) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();
        return gc.createCompatibleImage((int) cacheImgWidth , (int) cacheImgHeight , Transparency.TRANSLUCENT);
    }

    /**
     * @param go
     * @param renderer
     */
    private void paintColorbars(Graphics2D go, StateRenderer2D renderer) {
//        int offsetHeight = 130;
//        int offsetWidth = 5;
//        int offsetDelta = 130;
//        if (showSal) {
//            Graphics2D gl = (Graphics2D) go.create();
//            gl.translate(offsetWidth, offsetHeight);
//            ColorBarPainterUtil.paintColorBar(gl, colorMapVar, I18n.text(info.name), info.unit, minValue, maxValue);
//            gl.dispose();
//            offsetHeight += offsetDelta;
//        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.util.conf.PreferencesListener#preferencesUpdated()
     */
    @Override
    public void preferencesUpdated() {
        getConsole().repaint();
    }
}
