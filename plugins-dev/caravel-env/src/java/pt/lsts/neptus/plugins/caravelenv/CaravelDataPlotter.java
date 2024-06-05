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
 * Author: Paulo Dias
 * 5 June 2024
 */
package pt.lsts.neptus.plugins.caravelenv;

import com.google.common.eventbus.Subscribe;
import pt.lsts.imc.AirSaturation;
import pt.lsts.imc.Chlorophyll;
import pt.lsts.imc.DissolvedOrganicMatter;
import pt.lsts.imc.DissolvedOxygen;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.Turbidity;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.messages.listener.Periodic;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.conf.PreferencesListener;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "Caravel Data Plotter", description = "Plotter for Caravel underway data.")
public class CaravelDataPlotter extends ConsoleLayer implements PreferencesListener {

    @NeptusProperty(name = "Show temperature", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Temperature")
    public boolean showTemp = false;
    @NeptusProperty(name = "Show salinity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Salinity")
    public boolean showSal = true;
    @NeptusProperty(name = "Show turbidity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Turbidity")
    public boolean showTurbidity = false;
    @NeptusProperty(name = "Show chlorophyll", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Chlorophyll")
    public boolean showChlorophyll = false;
    @NeptusProperty(name = "Show dissolved organic matter", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Organic Matter")
    public boolean showDissolvedOrganicMatter = false;
    @NeptusProperty(name = "Show dissolved oxygen", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Oxygen")
    public boolean showDissolvedOxygen = false;
    @NeptusProperty(name = "Show air saturation", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Air Saturation")
    public boolean showAirSaturation = false;

    @NeptusProperty(name = "Min temperature", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Temperature")
    private double minTemp = 15;
    @NeptusProperty(name = "Max temperature", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Temperature")
    private double maxTemp = 35;

    @NeptusProperty(name = "Temperature color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Temperature")
    private ColorMap colormapTemp = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min salinity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Salinity")
    private double minSal = 33;
    @NeptusProperty(name = "Max salinity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Salinity")
    private double maxSal = 36;

    @NeptusProperty(name = "Salinity color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Salinity")
    private ColorMap colormapSal = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min turbidity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Turbidity")
    private double minTurbidity = 0;
    @NeptusProperty(name = "Max turbidity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Turbidity")
    private double maxTurbidity = 30;

    @NeptusProperty(name = "Turbidity color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Turbidity")
    private ColorMap colormapTurbidity = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min chlorophyll", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Chlorophyll")
    private double minChlorophyll = 0;
    @NeptusProperty(name = "Max chlorophyll", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Chlorophyll")
    private double maxChlorophyll = 2;

    @NeptusProperty(name = "Chlorophyll color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Chlorophyll")
    private ColorMap colormapChlorophyll = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min dissolved organic matter", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Organic Matter")
    private double minDissolvedOrganicMatter = 0;
    @NeptusProperty(name = "Max dissolved organic matter", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Organic Matter")
    private double maxDissolvedOrganicMatter = 10;

    @NeptusProperty(name = "Dissolved Organic Matter color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Organic Matter")
    private ColorMap colormapDissolvedOrganicMatter = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min dissolved oxygen", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Oxygen")
    private double minDissolvedOxygen = 0;
    @NeptusProperty(name = "Max dissolved oxygen", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Oxygen")
    private double maxDissolvedOxygen = 300;

    @NeptusProperty(name = "Dissolved Oxygen color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Oxygen")
    private ColorMap colormapDissolvedOxygen = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min air saturation", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Air Saturation")
    private double minAirSaturation = 0;
    @NeptusProperty(name = "Max air saturation", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Air Saturation")
    private double maxAirSaturation = 100;

    @NeptusProperty(name = "Air Saturation color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Air Saturation")
    private ColorMap colormapAirSaturation = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Max samples", userLevel = NeptusProperty.LEVEL.REGULAR)
    private int maxSamples = 35000;

    @NeptusProperty(name = "Clamp to fit", userLevel = NeptusProperty.LEVEL.REGULAR)
    private boolean clampToFit = false;

    private OffScreenLayerImageControl offScreenSalinity = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenTemperature = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenTurbidity = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenChlorophyll = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenDissolvedOrganicMatter = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenDissolvedOxygen = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenAirSaturation = new OffScreenLayerImageControl();

    private Thread painterThread = null;
    private AtomicBoolean abortIndicator = null;

    private List<DataPoint> pointsTemp = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsSalinity = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsTurbidity = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsChlorophyll = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsDissolvedOrganicMatter = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsDissolvedOxygen = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsAirSaturation = Collections.synchronizedList(new LinkedList<>());

    private AtomicInteger trigger = new AtomicInteger(0);
    private int triggerCount = 0;

    private class DataPoint {
        LocationType location = null;
        double value = Double.NaN;
    }

    private class DataPointXY {
        Point2D point = null;
        double value = Double.NaN;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        pointsTemp.clear();

    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

//        /* (non-Javadoc)
//         * @see pt.lsts.neptus.util.nmea.NmeaListener#nmeaSentence(java.lang.String)
//         */
//        @Override
//        public void nmeaSentence(String sentence) {
//            if (sentence.startsWith("t1")) {
//                LocationType loc = MyState.getLocation().getNewAbsoluteLatLonDepth();
//
//                DataPoint pt = new DataPoint();
//                pt.location = loc;
//
//                String[] tks = sentence.split(",");
//                for (String tk : tks) {
//                    try {
//                        String[] tts = tk.trim().split("=");
//                        if (tts.length < 2)
//                            continue;
//                        switch (tts[0].trim()) {
//                            case "t1":
//                                pt.temperature = Double.parseDouble(tts[1]);
//                                break;
//                            case "s":
//                                pt.salinity= Double.parseDouble(tts[1]);
//                                break;
//                            default:
//                                break;
//                        }
//                    }
//                    catch (NumberFormatException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                try {
//                    Salinity msgSal = new Salinity(Double.valueOf(pt.salinity).floatValue());
//                    msgSal.setSrc(GeneralPreferences.imcCcuId.intValue());
//                    msgSal.setTimestampMillis(System.currentTimeMillis());
//                    LsfMessageLogger.log(msgSal);
//                    Temperature msgTemp = new Temperature(Double.valueOf(pt.temperature).floatValue());
//                    msgTemp.setSrc(GeneralPreferences.imcCcuId.intValue());
//                    msgTemp.setTimestampMillis(System.currentTimeMillis());
//                    LsfMessageLogger.log(msgTemp);
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//                points.add(pt);
//                if (points.size() > maxSamples)
//                    points.remove(0);
//                trigger.incrementAndGet();
//            }
//        }

    private LocationType getSystemLocation() {
        // TODO FIXME
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(getConsole().getMainSystem());
        return sys == null ? null : sys.getLocation();
    }

    private void extractSensorMeasurementValueFromMessage(double value, List<DataPoint> points, IMCMessage msg) {
        LocationType loc = getSystemLocation();
        DataPoint pt = new DataPoint();
        pt.location = loc;
        pt.value = value;

        points.add(pt);
        if (points.size() > maxSamples)
            points.remove(0);
        trigger.incrementAndGet();
    }

    @Subscribe
    public void on(Temperature msg) {
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsTemp, msg);
    }

    @Subscribe
    public void on(Salinity msg) {
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsSalinity, msg);
    }

    @Subscribe
    public void on(Turbidity msg) {
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsTurbidity, msg);
    }

    @Subscribe
    public void on(Chlorophyll msg) {
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsChlorophyll, msg);
    }

    @Subscribe
    public void on(DissolvedOrganicMatter msg) {
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsDissolvedOrganicMatter, msg);
    }

    @Subscribe
    public void on(DissolvedOxygen msg) {
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsDissolvedOxygen, msg);
    }

    @Subscribe
    public void on(AirSaturation msg) {
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsAirSaturation, msg);
    }

    @Periodic(value = 2_000)
    private void update() {
        offScreenTemperature.triggerImageRebuild();
        offScreenSalinity.triggerImageRebuild();
        offScreenTurbidity.triggerImageRebuild();
        offScreenChlorophyll.triggerImageRebuild();
        offScreenDissolvedOrganicMatter.triggerImageRebuild();
        offScreenDissolvedOxygen.triggerImageRebuild();
        offScreenAirSaturation.triggerImageRebuild();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        boolean recreateImage = offScreenSalinity.paintPhaseStartTestRecreateImageAndRecreate(g, renderer)
                || offScreenTemperature.paintPhaseStartTestRecreateImageAndRecreate(g, renderer)
                || offScreenTurbidity.paintPhaseStartTestRecreateImageAndRecreate(g, renderer)
                || offScreenChlorophyll.paintPhaseStartTestRecreateImageAndRecreate(g, renderer)
                || offScreenDissolvedOrganicMatter.paintPhaseStartTestRecreateImageAndRecreate(g, renderer)
                || offScreenDissolvedOxygen.paintPhaseStartTestRecreateImageAndRecreate(g, renderer)
                || offScreenAirSaturation.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);

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
                    Graphics2D g2 = offScreenSalinity.getImageGraphics();
                    try {

                        ArrayList<DataPointXY> ptsTemp = new ArrayList<>();
                        ArrayList<DataPointXY> ptsSalinity = new ArrayList<>();
                        ArrayList<DataPointXY> ptsTurbidity = new ArrayList<>();
                        ArrayList<DataPointXY> ptsChlorophyll = new ArrayList<>();
                        ArrayList<DataPointXY> ptsDissolvedOrganicMatter = new ArrayList<>();
                        ArrayList<DataPointXY> ptsDissolvedOxygen = new ArrayList<>();
                        ArrayList<DataPointXY> ptsAirSaturation = new ArrayList<>();

                        if (showTemp) {
                            ptsTemp = transformDataPointsToXY(pointsTemp, rendererCalculator);
                        }
                        if (showSal) {
                            ptsSalinity = transformDataPointsToXY(pointsSalinity, rendererCalculator);
                        }
                        if (showTurbidity) {
                            ptsTurbidity = transformDataPointsToXY(pointsTurbidity, rendererCalculator);
                        }
                        if (showChlorophyll) {
                            ptsChlorophyll = transformDataPointsToXY(pointsChlorophyll, rendererCalculator);
                        }
                        if (showDissolvedOrganicMatter) {
                            ptsDissolvedOrganicMatter = transformDataPointsToXY(pointsDissolvedOrganicMatter, rendererCalculator);
                        }
                        if (showDissolvedOxygen) {
                            ptsDissolvedOxygen = transformDataPointsToXY(pointsDissolvedOxygen, rendererCalculator);
                        }
                        if (showAirSaturation) {
                            ptsAirSaturation = transformDataPointsToXY(pointsAirSaturation, rendererCalculator);
                        }

                        if (showTemp && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsTemp, colormapTemp, minTemp, maxTemp, offScreenTemperature, g2, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenTemperature.triggerImageRebuild();
                            }
                        }

                        if (showSal && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsSalinity, colormapSal, minSal, maxSal, offScreenSalinity, g2, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenSalinity.triggerImageRebuild();
                            }
                        }

                        if (showTurbidity && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsTurbidity, colormapTurbidity, minTurbidity, maxTurbidity, offScreenTurbidity, g2, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenTurbidity.triggerImageRebuild();
                            }
                        }

                        if (showChlorophyll && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsChlorophyll, colormapChlorophyll, minChlorophyll, maxChlorophyll, offScreenChlorophyll, g2, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenChlorophyll.triggerImageRebuild();
                            }
                        }

                        if (showDissolvedOrganicMatter && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsDissolvedOrganicMatter, colormapDissolvedOrganicMatter, minDissolvedOrganicMatter, maxDissolvedOrganicMatter, offScreenDissolvedOrganicMatter, g2, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenDissolvedOrganicMatter.triggerImageRebuild();
                            }
                        }

                        if (showDissolvedOxygen && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsDissolvedOxygen, colormapDissolvedOxygen, minDissolvedOxygen, maxDissolvedOxygen, offScreenDissolvedOxygen, g2, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenDissolvedOxygen.triggerImageRebuild();
                            }
                        }

                        if (showAirSaturation && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsAirSaturation, colormapAirSaturation, minAirSaturation, maxAirSaturation, offScreenAirSaturation, g2, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenAirSaturation.triggerImageRebuild();
                            }
                        }

                        g2.dispose();
                    }
                    catch (Exception | Error e) {
                        e.printStackTrace();
                        offScreenTemperature.triggerImageRebuild();
                        offScreenSalinity.triggerImageRebuild();
                        offScreenTurbidity.triggerImageRebuild();
                        offScreenChlorophyll.triggerImageRebuild();
                        offScreenDissolvedOrganicMatter.triggerImageRebuild();
                        offScreenDissolvedOxygen.triggerImageRebuild();
                        offScreenAirSaturation.triggerImageRebuild();
                    }

                    renderer.invalidate();
                    renderer.repaint(200);
                }
            }, CaravelDataPlotter.class.getSimpleName() + ":: Painter");
            painterThread.setDaemon(true);
            painterThread.start();
        }
        offScreenTemperature.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
        offScreenSalinity.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
        offScreenTurbidity.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
        offScreenChlorophyll.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
        offScreenDissolvedOrganicMatter.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
        offScreenDissolvedOxygen.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
        offScreenAirSaturation.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);

        paintColorbars(g, renderer);
    }

    private void recreateSensorDataCacheImage(ArrayList<DataPointXY> pts, ColorMap colormap, double minVal,
                                              double maxVal, OffScreenLayerImageControl offScreenImageControl, Graphics2D g2,
                                              MapTileRendererCalculator rendererCalculator) {
        double fullImgWidth = rendererCalculator.getSize().getWidth() + offScreenImageControl.getOffScreenBufferPixel() * 2.;
        double fullImgHeight = rendererCalculator.getSize().getHeight() + offScreenImageControl.getOffScreenBufferPixel() * 2.;

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

                double v = (double) pt.value;

                if (clampToFit
                        && (Double.compare(v, minVal) < 0 || Double.compare(v, maxVal) > 0))
                    return;

                Color color = colormap.getColor(ColorMapUtils.getColorIndexZeroToOneLog10(v, minVal, maxVal));
                cacheImg.setRGB((int) ((pt.point.getX() + offScreenImageControl.getOffScreenBufferPixel()) * cacheImgScaleX),
                        (int) ((pt.point.getY() + offScreenImageControl.getOffScreenBufferPixel()) * cacheImgScaleY), color.getRGB());
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

    private ArrayList<DataPointXY> transformDataPointsToXY(List<DataPoint> points, MapTileRendererCalculator rendererCalculator) {
        return pointsTemp.stream().collect(ArrayList<DataPointXY>::new,
                (r, p) -> {
                    if (abortIndicator.get()) {
                        return;
                    }
                    Point2D pxy = rendererCalculator.getScreenPosition(p.location);
                    DataPointXY dpxy = new DataPointXY();
                    dpxy.point = pxy;
                    dpxy.value = p.value;
                    r.add(dpxy);
                }, (r1, r2) -> {
                    for (DataPointXY d2 : r2) {
                        if (abortIndicator.get()) {
                            break;
                        }
                        boolean found = false;
                        for (DataPointXY d1 : r1) {
                            if (abortIndicator.get()) {
                                break;
                            }
                            if (d2.point.equals(d1.point)) {
                                d1.value = (d1.value + d2.value) / 2.;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            r1.add(d2);
                        }
                    }
                });
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