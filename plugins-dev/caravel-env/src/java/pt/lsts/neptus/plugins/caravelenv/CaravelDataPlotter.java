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
import org.apache.commons.lang3.tuple.Triple;
import pt.lsts.imc.AirSaturation;
import pt.lsts.imc.Chlorophyll;
import pt.lsts.imc.CurrentProfile;
import pt.lsts.imc.CurrentProfileCell;
import pt.lsts.imc.DissolvedOrganicMatter;
import pt.lsts.imc.DissolvedOxygen;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.Turbidity;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBarPainterUtil;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.messages.listener.Periodic;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.conf.PreferencesListener;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;
import scala.runtime.StringFormat;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.sun.tools.doclint.Entity.gt;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "Caravel Data Plotter", description = "Plotter for Caravel underway data.")
public class CaravelDataPlotter extends ConsoleLayer implements PreferencesListener, MainVehicleChangeListener {

    final static int ARROW_RADIUS = 12;
    final static Path2D.Double arrow = new Path2D.Double();
    static {
        arrow.moveTo(-5, 6);
        arrow.lineTo(0, -6);
        arrow.lineTo(5, 6);
        arrow.lineTo(0, 3);
        arrow.lineTo(-5, 6);
        arrow.closePath();
    }

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
    @NeptusProperty(name = "Show water current", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Water Current")
    public boolean showWaterCurrent = false;

    @NeptusProperty(name = "Valid temperature data sources", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Temperature")
    public String validTempDataSources = "CTD, Daemon";
    @NeptusProperty(name = "Valid salinity data sources", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Salinity")
    public String validSalDataSources = "CTD, Daemon";
    @NeptusProperty(name = "Valid turbidity data sources", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Turbidity")
    public String validTurbidityDataSources = "Fluorometers, Daemon";
    @NeptusProperty(name = "Valid chlorophyll data sources", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Chlorophyll")
    public String validChlorophyllDataSources = "Fluorometers, Daemon";
    @NeptusProperty(name = "Valid dissolved organic matter data sources", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Organic Matter")
    public String validDissolvedOrganicMatterDataSources = "Fluorometers, Daemon";
    @NeptusProperty(name = "Valid dissolved oxygen data sources", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Oxygen")
    public String validDissolvedOxygenDataSources = "Dissolved Oxygen, Daemon";
    @NeptusProperty(name = "Valid air saturation data sources", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Air Saturation")
    public String validAirSaturationDataSources = "Dissolved Oxygen, Daemon";
    @NeptusProperty(name = "Valid water current data sources", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Water Current")
    public String validWaterCurrentDataSources = "ADCP, Daemon";

    @NeptusProperty(name = "Min temperature", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Temperature")
    public double minTemp = 15;
    @NeptusProperty(name = "Max temperature", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Temperature")
    public double maxTemp = 35;

    @NeptusProperty(name = "Temperature color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Temperature")
    public ColorMap colormapTemp = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min salinity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Salinity")
    public double minSal = 33;
    @NeptusProperty(name = "Max salinity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Salinity")
    public double maxSal = 36;

    @NeptusProperty(name = "Salinity color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Salinity")
    public ColorMap colormapSal = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min turbidity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Turbidity")
    public double minTurbidity = 0;
    @NeptusProperty(name = "Max turbidity", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Turbidity")
    public double maxTurbidity = 30;

    @NeptusProperty(name = "Turbidity color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Turbidity")
    public ColorMap colormapTurbidity = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min chlorophyll", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Chlorophyll")
    public double minChlorophyll = 0;
    @NeptusProperty(name = "Max chlorophyll", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Chlorophyll")
    public double maxChlorophyll = 2;

    @NeptusProperty(name = "Chlorophyll color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Chlorophyll")
    public ColorMap colormapChlorophyll = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min dissolved organic matter", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Organic Matter")
    public double minDissolvedOrganicMatter = 0;
    @NeptusProperty(name = "Max dissolved organic matter", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Organic Matter")
    public double maxDissolvedOrganicMatter = 10;

    @NeptusProperty(name = "Dissolved Organic Matter color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Organic Matter")
    public ColorMap colormapDissolvedOrganicMatter = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min dissolved oxygen", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Oxygen")
    public double minDissolvedOxygen = 0;
    @NeptusProperty(name = "Max dissolved oxygen", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Oxygen")
    public double maxDissolvedOxygen = 300;

    @NeptusProperty(name = "Dissolved Oxygen color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Dissolved Oxygen")
    public ColorMap colormapDissolvedOxygen = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min air saturation", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Air Saturation")
    public double minAirSaturation = 0;
    @NeptusProperty(name = "Max air saturation", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Air Saturation")
    public double maxAirSaturation = 100;

    @NeptusProperty(name = "Air Saturation color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Air Saturation")
    public ColorMap colormapAirSaturation = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Min water current", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Water Current")
    public double minWaterCurrent = -2;
    @NeptusProperty(name = "Max water current", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Water Current")
    public double maxWaterCurrent = 2;

    @NeptusProperty(name = "Water Current color map", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Water Current")
    public ColorMap colormapWaterCurrent = ColorMapFactory.createJetColorMap();
    @NeptusProperty(name = "Water Current Depth", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Water Current",
            description = "The layer to show the water current data (m).")
    public double waterCurrentDepth = 2.0;
    @NeptusProperty(name = "Water Current Depth Window (m)", userLevel = NeptusProperty.LEVEL.REGULAR, category = "Water Current",
            description = "Window is +- this value from the waterCurrentDepth.")
    public short waterCurrentDepthWindow = 2;

    @NeptusProperty(name = "Max samples", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int maxSamples = 35000;

    @NeptusProperty(name = "Clamp to fit", userLevel = NeptusProperty.LEVEL.REGULAR)
    public boolean clampToFit = false;

    private OffScreenLayerImageControl offScreenSalinity = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenTemperature = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenTurbidity = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenChlorophyll = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenDissolvedOrganicMatter = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenDissolvedOxygen = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenAirSaturation = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenWaterCurrent = new OffScreenLayerImageControl();

    private Thread painterThread = null;
    private AtomicBoolean abortIndicator = null;

    private List<DataPoint> pointsTemp = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsSalinity = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsTurbidity = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsChlorophyll = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsDissolvedOrganicMatter = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsDissolvedOxygen = Collections.synchronizedList(new LinkedList<>());
    private List<DataPoint> pointsAirSaturation = Collections.synchronizedList(new LinkedList<>());
    private List<DataPointPolar> pointsWaterCurrent = Collections.synchronizedList(new LinkedList<>());

    private AtomicInteger trigger = new AtomicInteger(0);
    private int triggerCount = 0;

    @Override
    public void mainVehicleChange(String id) {
        pointsTemp.clear();
        pointsSalinity.clear();
        pointsTurbidity.clear();
        pointsChlorophyll.clear();
        pointsDissolvedOrganicMatter.clear();
        pointsDissolvedOxygen.clear();
        pointsAirSaturation.clear();
        pointsWaterCurrent.clear();
    }

    private static class DataPoint {
        LocationType location = null;
        double value = Double.NaN;
    }

    private static class DataPointPolar extends DataPoint {
        double directionRads = Double.NaN;
        double zDown = Double.NaN;
    }

    private static class DataPointXY {
        Point2D point = null;
        double value = Double.NaN;
    }

    private static class DataPointXYPolar extends DataPointXY {
        double directionRads = Double.NaN;
        double zDown = Double.NaN;
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

    private double getSystemLocationDepth() {
        // TODO FIXME
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(getConsole().getMainSystem());
        return sys == null ? Double.NaN : sys.getLocation().getDepth();
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
        if (!getConsole().getMainSystem().equalsIgnoreCase(msg.getSourceName())) {
            return;
        }
        String entity = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        if (entity != null && !validTempDataSources.contains(entity)) {
            return;
        }
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsTemp, msg);
        offScreenTemperature.triggerImageRebuild();
    }

    @Subscribe
    public void on(Salinity msg) {
        if (!getConsole().getMainSystem().equalsIgnoreCase(msg.getSourceName())) {
            return;
        }
        String entity = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        if (entity != null && !validSalDataSources.contains(entity)) {
            return;
        }
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsSalinity, msg);
        offScreenSalinity.triggerImageRebuild();
    }

    @Subscribe
    public void on(Turbidity msg) {
        if (!getConsole().getMainSystem().equalsIgnoreCase(msg.getSourceName())) {
            return;
        }
        String entity = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        if (entity != null && !validTurbidityDataSources.contains(entity)) {
            return;
        }
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsTurbidity, msg);
        offScreenTurbidity.triggerImageRebuild();
    }

    @Subscribe
    public void on(Chlorophyll msg) {
        if (!getConsole().getMainSystem().equalsIgnoreCase(msg.getSourceName())) {
            return;
        }
        String entity = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        if (entity != null && !validChlorophyllDataSources.contains(entity)) {
            return;
        }
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsChlorophyll, msg);
        offScreenChlorophyll.triggerImageRebuild();
    }

    @Subscribe
    public void on(DissolvedOrganicMatter msg) {
        if (!getConsole().getMainSystem().equalsIgnoreCase(msg.getSourceName())) {
            return;
        }
        String entity = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        if (entity != null && !validDissolvedOrganicMatterDataSources.contains(entity)) {
            return;
        }
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsDissolvedOrganicMatter, msg);
        offScreenDissolvedOrganicMatter.triggerImageRebuild();
    }

    @Subscribe
    public void on(DissolvedOxygen msg) {
        if (!getConsole().getMainSystem().equalsIgnoreCase(msg.getSourceName())) {
            return;
        }
        String entity = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        if (entity != null && !validDissolvedOxygenDataSources.contains(entity)) {
            return;
        }
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsDissolvedOxygen, msg);
        offScreenDissolvedOxygen.triggerImageRebuild();
    }

    @Subscribe
    public void on(AirSaturation msg) {
        if (!getConsole().getMainSystem().equalsIgnoreCase(msg.getSourceName())) {
            return;
        }
        String entity = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        if (entity != null && !validAirSaturationDataSources.contains(entity)) {
            return;
        }
        extractSensorMeasurementValueFromMessage(msg.getValue(), pointsAirSaturation, msg);
        offScreenAirSaturation.triggerImageRebuild();
    }

    @Subscribe
    public void on(CurrentProfile msg) {
        if (!getConsole().getMainSystem().equalsIgnoreCase(msg.getSourceName())) {
            return;
        }
        String entity = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        if (entity != null && !validWaterCurrentDataSources.contains(entity)) {
            return;
        }

        LocationType loc = getSystemLocation();
        double depth = getSystemLocationDepth();

        switch (msg.getCoordSys()) {
            case CurrentProfile.UTF_BEAMS:
                NeptusLog.pub().warn("BEAMS not supported yet.");
                return;
            case CurrentProfile.UTF_XYZ: // TODO FIXME, now it is the same as UTF_ENU
            case CurrentProfile.UTF_NED:
            // case CurrentProfile.UTF_ENU: // TODO FIXME
            default:
                break;
        }

        short nBeams = msg.getNbeams();
        short nCells = msg.getNcells();

        if (nBeams != 3 && nBeams != 4) {
            NeptusLog.pub().warn("Only 3 or 4 beams supported.");
            return;
        }

        if (msg.getProfile().size() != nCells) {
            NeptusLog.pub().warn("Number of cells does not match the profile size.");
            return;
        }

        Vector<CurrentProfileCell> profile = msg.getProfile();
        for (int i = 0; i < profile.size(); i++) {
            CurrentProfileCell cpcell = profile.get(i);
            List<DataPointPolar> points = pointsWaterCurrent;

            double measureDepth = cpcell.getCellPosition() + (depth < 0 ? 0 : depth);

            DataPointPolar pt = new DataPointPolar();
            pt.location = loc;
            Triple<Double, Double, Double> valueSDQ = calcWaterSpeedAndDirectionFromBeams(nBeams, cpcell);
            if (!Double.isNaN(valueSDQ.getLeft()) && !Double.isNaN(valueSDQ.getMiddle()) && !Double.isNaN(valueSDQ.getRight())) {
                pt.value = valueSDQ.getLeft();
                pt.directionRads = valueSDQ.getMiddle();
                pt.zDown = measureDepth;
                points.add(pt);
            }
        }

        if (pointsWaterCurrent.size() > maxSamples)
            pointsWaterCurrent.remove(0);
        trigger.incrementAndGet();
    }

    private Triple<Double, Double, Double> calcWaterSpeedAndDirectionFromBeams(short nBeams, CurrentProfileCell cpcell) {
        double vel = Double.NaN;
        double dirRads = Double.NaN;
        double z = Double.NaN;

        if (nBeams == 3 || nBeams == 4) {
            vel = Math.sqrt(Math.pow(cpcell.getBeams().get(0).getVel(), 2) + Math.pow(cpcell.getBeams().get(1).getVel(), 2));
            // East, North
            dirRads = AngleUtils.nomalizeAngleRads2Pi(AngleUtils.calcAngle(0, 0, cpcell.getBeams().get(0).getVel(), cpcell.getBeams().get(1).getVel()));
            // Up to Down
            z = -1 * cpcell.getBeams().get(2).getVel();
        }
        if (nBeams == 4) {
            z = -1 * (cpcell.getBeams().get(2).getVel() + cpcell.getBeams().get(3).getVel()) / 2.0;
        }
        return Triple.of(vel, dirRads, z);
    }

    @Periodic(value = 2_000)
    private void update() {
        triggerAllImagesRebuild();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        // Wee need all tests to run to recreate the image cache and not just the first one that is true
        boolean recreateImage = offScreenSalinity.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        recreateImage = offScreenTemperature.paintPhaseStartTestRecreateImageAndRecreate(g, renderer) || recreateImage;
        recreateImage = offScreenTurbidity.paintPhaseStartTestRecreateImageAndRecreate(g, renderer) || recreateImage;
        recreateImage = offScreenChlorophyll.paintPhaseStartTestRecreateImageAndRecreate(g, renderer) || recreateImage;
        recreateImage = offScreenDissolvedOrganicMatter.paintPhaseStartTestRecreateImageAndRecreate(g, renderer) || recreateImage;
        recreateImage = offScreenDissolvedOxygen.paintPhaseStartTestRecreateImageAndRecreate(g, renderer) || recreateImage;
        recreateImage = offScreenAirSaturation.paintPhaseStartTestRecreateImageAndRecreate(g, renderer) || recreateImage;
        recreateImage = offScreenWaterCurrent.paintPhaseStartTestRecreateImageAndRecreate(g, renderer) || recreateImage;

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
                    Graphics2D g2Temp = offScreenTemperature.getImageGraphics();
                    Graphics2D g2Salinity = offScreenSalinity.getImageGraphics();
                    Graphics2D g2Turbidity = offScreenTurbidity.getImageGraphics();
                    Graphics2D g2Chlorophyll = offScreenChlorophyll.getImageGraphics();
                    Graphics2D g2DissolvedOrganicMatter = offScreenDissolvedOrganicMatter.getImageGraphics();
                    Graphics2D g2DissolvedOxygen = offScreenDissolvedOxygen.getImageGraphics();
                    Graphics2D g2AirSaturation = offScreenAirSaturation.getImageGraphics();
                    Graphics2D g2WaterCurrent = offScreenWaterCurrent.getImageGraphics();

                    try {
                        List<DataPointXY> ptsTemp = new ArrayList<>();
                        List<DataPointXY> ptsSalinity = new ArrayList<>();
                        List<DataPointXY> ptsTurbidity = new ArrayList<>();
                        List<DataPointXY> ptsChlorophyll = new ArrayList<>();
                        List<DataPointXY> ptsDissolvedOrganicMatter = new ArrayList<>();
                        List<DataPointXY> ptsDissolvedOxygen = new ArrayList<>();
                        List<DataPointXY> ptsAirSaturation = new ArrayList<>();
                        List<DataPointXYPolar> ptsWaterCurrent = new ArrayList<>();

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
                        if (showWaterCurrent) {
                            ptsWaterCurrent = transformCurrentDataPointsToXYPolar(pointsWaterCurrent, waterCurrentDepth, waterCurrentDepthWindow, rendererCalculator);
                        }

                        if (showTemp && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsTemp, colormapTemp, minTemp, maxTemp, offScreenTemperature, g2Temp, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenTemperature.triggerImageRebuild();
                            }
                        }

                        if (showSal && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsSalinity, colormapSal, minSal, maxSal, offScreenSalinity, g2Salinity, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenSalinity.triggerImageRebuild();
                            }
                        }

                        if (showTurbidity && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsTurbidity, colormapTurbidity, minTurbidity, maxTurbidity, offScreenTurbidity, g2Turbidity, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenTurbidity.triggerImageRebuild();
                            }
                        }

                        if (showChlorophyll && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsChlorophyll, colormapChlorophyll, minChlorophyll, maxChlorophyll, offScreenChlorophyll, g2Chlorophyll, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenChlorophyll.triggerImageRebuild();
                            }
                        }

                        if (showDissolvedOrganicMatter && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsDissolvedOrganicMatter, colormapDissolvedOrganicMatter, minDissolvedOrganicMatter, maxDissolvedOrganicMatter, offScreenDissolvedOrganicMatter, g2DissolvedOrganicMatter, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenDissolvedOrganicMatter.triggerImageRebuild();
                            }
                        }

                        if (showDissolvedOxygen && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsDissolvedOxygen, colormapDissolvedOxygen, minDissolvedOxygen, maxDissolvedOxygen, offScreenDissolvedOxygen, g2DissolvedOxygen, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenDissolvedOxygen.triggerImageRebuild();
                            }
                        }

                        if (showAirSaturation && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(ptsAirSaturation, colormapAirSaturation, minAirSaturation, maxAirSaturation, offScreenAirSaturation, g2AirSaturation, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenAirSaturation.triggerImageRebuild();
                            }
                        }

                        if (showWaterCurrent && !abortIndicator.get()) {
                            try {
                                recreateSensorDataCacheImage(Collections.unmodifiableList(ptsWaterCurrent), colormapWaterCurrent, minWaterCurrent,
                                    maxWaterCurrent, offScreenWaterCurrent, g2WaterCurrent, rendererCalculator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreenWaterCurrent.triggerImageRebuild();
                            }
                        }

                        g2Temp.dispose();
                        g2Salinity.dispose();
                        g2Turbidity.dispose();
                        g2Chlorophyll.dispose();
                        g2DissolvedOrganicMatter.dispose();
                        g2DissolvedOxygen.dispose();
                        g2AirSaturation.dispose();
                        g2WaterCurrent.dispose();
                    }
                    catch (Exception | Error e) {
                        e.printStackTrace();
                        triggerAllImagesRebuild();
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
        offScreenWaterCurrent.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);

        paintColorbars(g, renderer);
    }

    private void triggerAllImagesRebuild() {
        offScreenTemperature.triggerImageRebuild();
        offScreenSalinity.triggerImageRebuild();
        offScreenTurbidity.triggerImageRebuild();
        offScreenChlorophyll.triggerImageRebuild();
        offScreenDissolvedOrganicMatter.triggerImageRebuild();
        offScreenDissolvedOxygen.triggerImageRebuild();
        offScreenAirSaturation.triggerImageRebuild();
        offScreenWaterCurrent.triggerImageRebuild();
    }

    private void recreateSensorDataCacheImage(List<DataPointXY> pts, ColorMap colormap, double minVal,
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
        boolean useArrows = false;
        if (!pts.isEmpty()) {
            DataPointXY pt0 = pts.get(0);
            if (pt0 instanceof DataPointXYPolar) {
                useArrows = true;
            }
        }
        pts.parallelStream().forEach(getDataPointXYSensorPainter(colormap, minVal, maxVal, offScreenImageControl,
                cacheImg, cacheImgScaleX, cacheImgScaleY, rendererCalculator, useArrows));

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

    private Consumer<DataPointXY> getDataPointXYSensorPainter(ColorMap colormap, double minVal, double maxVal,
                                                              OffScreenLayerImageControl offScreenImageControl,
                                                              BufferedImage cacheImg, double cacheImgScaleX, double cacheImgScaleY,
                                                              MapTileRendererCalculator rendererCalculator, boolean useArrows) {
        return pt -> {
            try {
                if (abortIndicator.get())
                    return;

                double v = (double) pt.value;

                if (clampToFit
                        && (Double.compare(v, minVal) < 0 || Double.compare(v, maxVal) > 0))
                    return;

                Color color = colormap.getColor(ColorMapUtils.getColorIndexZeroToOneLog10(v, minVal, maxVal));
                if (useArrows && false) {
                    try {
                        DataPointXYPolar ptPloar = (DataPointXYPolar) pt;
                        Graphics2D g2 = (Graphics2D) offScreenImageControl.getImageGraphics();
                        g2.setColor(color);
                        g2.translate((ptPloar.point.getX() + offScreenImageControl.getOffScreenBufferPixel()),
                                (ptPloar.point.getY() + offScreenImageControl.getOffScreenBufferPixel()));
                        double rot = ptPloar.directionRads + Math.PI / 2. - rendererCalculator.getRotation();
                        g2.rotate(rot);
                        g2.fill(arrow);
                        g2.rotate(-rot);
                    } catch (Exception e) {
                        //NeptusLog.pub().trace(e);
                    }
                } else {
                    cacheImg.setRGB((int) ((pt.point.getX() + offScreenImageControl.getOffScreenBufferPixel()) * cacheImgScaleX),
                            (int) ((pt.point.getY() + offScreenImageControl.getOffScreenBufferPixel()) * cacheImgScaleY), color.getRGB());
                }
            }
            catch (Exception e) {
                NeptusLog.pub().trace(e);
            }
        };
    }

    private List<DataPointXY> transformDataPointsToXY(List<DataPoint> points, MapTileRendererCalculator rendererCalculator) {
        return points.stream().collect(ArrayList<DataPointXY>::new,
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

    private List<DataPointXYPolar> transformCurrentDataPointsToXYPolar(List<DataPointPolar> points,
                                                                       double waterCurrentDepth, int waterCurrentDepthWindow,
                                                                       MapTileRendererCalculator rendererCalculator) {
        final double minDepth = waterCurrentDepth - waterCurrentDepthWindow;
        final double maxDepth = waterCurrentDepth + waterCurrentDepthWindow;
        return points.stream().filter(dp -> dp.zDown >= minDepth && dp.zDown <= maxDepth)
                .collect(ArrayList<DataPointXYPolar>::new,
                (r, p) -> {
                    if (abortIndicator.get()) {
                        return;
                    }
                    Point2D pxy = rendererCalculator.getScreenPosition(p.location);
                    DataPointXYPolar dpxy = new DataPointXYPolar();
                    dpxy.point = pxy;
                    dpxy.value = p.value;
                    dpxy.directionRads = p.directionRads;
                    dpxy.zDown = p.zDown;
                    r.add(dpxy);
                }, (r1, r2) -> {
                    for (DataPointXYPolar d2 : r2) {
                        if (abortIndicator.get()) {
                            break;
                        }
                        boolean found = false;
                        for (DataPointXYPolar d1 : r1) {
                            if (abortIndicator.get()) {
                                break;
                            }
                            if (d2.point.equals(d1.point)) {
                                d1.value = (d1.value + d2.value) / 2.;
                                d1.directionRads = (d1.directionRads + d2.directionRads) / 2.;
                                d1.zDown = (d1.zDown + d2.zDown) / 2.;
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

    private void paintColorbars(Graphics2D go, StateRenderer2D renderer) {
        int offsetHeight = 130 * 3;
        int offsetWidth = 5;
        int offsetDelta = 130;

        if (showTemp) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colormapTemp, "Temperature", "ºC", minTemp, maxTemp);
            gl.dispose();
            offsetHeight += offsetDelta;
        } else if (showSal) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colormapSal, "Salinity", "PSU", minSal, maxSal);
            gl.dispose();
            offsetHeight += offsetDelta;
        } else if (showTurbidity) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colormapTurbidity, "Turbidity", "NTU", minTurbidity, maxTurbidity);
            gl.dispose();
            offsetHeight += offsetDelta;
        } else if (showChlorophyll) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colormapChlorophyll, "Chlorophyll", "µg/L", minChlorophyll, maxChlorophyll);
            gl.dispose();
            offsetHeight += offsetDelta;
        } else if (showDissolvedOrganicMatter) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colormapDissolvedOrganicMatter, "Dissolved Organic Matter", "PPB", minDissolvedOrganicMatter, maxDissolvedOrganicMatter);
            gl.dispose();
            offsetHeight += offsetDelta;
        } else if (showDissolvedOxygen) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colormapDissolvedOxygen, "Dissolved Oxygen", "µM", minDissolvedOxygen, maxDissolvedOxygen);
            gl.dispose();
            offsetHeight += offsetDelta;
        } else if (showAirSaturation) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colormapAirSaturation, "Air Saturation", "%", minAirSaturation, maxAirSaturation);
            gl.dispose();
            offsetHeight += offsetDelta;
        } else if (showWaterCurrent) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            String txtName = String.format("Water Current [%.2f ±%d]", waterCurrentDepth, waterCurrentDepthWindow);
            ColorBarPainterUtil.paintColorBar(gl, colormapWaterCurrent, txtName, "m/s", minWaterCurrent, maxWaterCurrent);
            gl.dispose();
            offsetHeight += offsetDelta;
        }
    }

    /**
     * @param go
     * @param renderer
     */
    private void paintColorbars(Graphics2D go, StateRenderer2D renderer, ColorMap colormap, String name,
                                String unit, double minValue, double maxValue) {
        Graphics2D gl = (Graphics2D) go.create();
        ColorBarPainterUtil.paintColorBar(gl, colormap, name, unit, minValue, maxValue);
        gl.dispose();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.util.conf.PreferencesListener#preferencesUpdated()
     */
    @Override
    public void preferencesUpdated() {
        triggerAllImagesRebuild();
        getConsole().repaint();
    }
}