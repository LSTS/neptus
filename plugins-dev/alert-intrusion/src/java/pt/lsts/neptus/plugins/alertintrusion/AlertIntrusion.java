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
 * 14/5/2024
 */
package pt.lsts.neptus.plugins.alertintrusion;

import com.google.common.util.concurrent.AtomicDouble;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.alertintrusion.data.SystemSizeAndCourseData;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.util.WGS84Utilities;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@PluginDescription(name = "Alert Intrusion", icon = "pt/lsts/neptus/plugins/alertintrusion/colreg.png",
    description = "Alert Intrusion", category = PluginDescription.CATEGORY.INTERFACE, version = "0.1")
@LayerPriority(priority = 182)
public class AlertIntrusion extends ConsoleLayer implements MainVehicleChangeListener {

    public enum TimeUnit {
        MINUTES(60_000),
        HOURS(3_600_000);

        private final long microseconds;

        TimeUnit(long microseconds) {
            this.microseconds = microseconds;
        }

        public long getMicroseconds() {
            return microseconds;
        }
    }

    private static final GeneralPath shapeArrow = new GeneralPath();
    static {
        shapeArrow.moveTo(0, 0);
        shapeArrow.curveTo(7, 3, 7, 3, 14, 0);
        //shapeArrow.lineTo(14, 0);
        shapeArrow.lineTo(7, -14);
        shapeArrow.lineTo(0, 0);
    }

    private final Color shapeColor = new Color(0xFF, 0xD0, 0x46, 203);
    private final Color shapeHighColor = new Color(255, 70, 70, 203);
    private final Color blackTransparentColor = ColorUtils.setTransparencyToColor(Color.black, 100);

    @NeptusProperty(name = "Minimum distance allowed between vehicle and ships (meters)", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int collisionDistance = 100;
    @NeptusProperty(name = "Percentage of the critical distance to trigger the alert")
    public int collisionCriticalDistancePercentage = 20;
    @NeptusProperty(name = "Use course for calculation", userLevel = NeptusProperty.LEVEL.REGULAR)
    public boolean useCourseForCalculation = true;
    @NeptusProperty(name = "Minimum Speed To Be Stopped", description = "Configures the maximum speed (m/s) for the system to be considered stopped.",
            category = "Renderer", userLevel = NeptusProperty.LEVEL.REGULAR)
    public double minimumSpeedToBeStopped = 0.2;
    @NeptusProperty(name = "Minutes To Hide Systems Without Known Location", description = "Minutes after which systems disappear from render if inactive (0 to disable)",
            category = "Systems in Renderer", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int minutesToHideSystemsWithoutKnownLocation = 10;
    @NeptusProperty(name = "Projection Time Window Value", description = "Time to project the vehicles positions", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int projectionTimeWindowValue = 1;
    @NeptusProperty(name = "Projection Time Window Unit", description = "Time unit to project the vehicles positions", userLevel = NeptusProperty.LEVEL.REGULAR)
    public TimeUnit projectionTimeWindowUnit = TimeUnit.HOURS;

    private OffScreenLayerImageControl layerPainter;
    //private Map<String, VehicleRiskAnalysis> state = new ConcurrentHashMap<>();
    //private Map<String, VehicleRiskPanel> panels = new ConcurrentHashMap<>();

    private String lastMainVehicle;
    private final Map<String, Map<Date, Pair<String, Double>>> collisionsTree = new ConcurrentHashMap<>();

    private static final Image colregImage = ImageUtils.getScaledImage("pt/lsts/neptus/plugins/alertintrusion/colreg.png",
            50, 50);
    private static final Image colregImageSmall = ImageUtils.getScaledImage("pt/lsts/neptus/plugins/alertintrusion/colreg.png",
            20, 20);

    private final JLabel infoLabel = new JLabel("");

    public AlertIntrusion() {
        super();
    }

    @Override
    public boolean userControlsOpacity() {
        return true;
    }

    @Override
    public void initLayer() {
        layerPainter = new OffScreenLayerImageControl();
        // To allow to repaint even on pan and zoom
        layerPainter.setOffScreenBufferPixel(0);
        setOpacity(0.8f);
    }

    @Override
    public void cleanLayer() {
        layerPainter = null;
    }

    @Override
    public void mainVehicleChange(String id) {
        layerPainter.triggerImageRebuild();
    }

    @Periodic(millisBetweenUpdates = 10_000)
    void updateCollisions() {
        long start = System.currentTimeMillis();

        String mainSystemName = getConsole().getMainSystem();
        if (lastMainVehicle == null || !lastMainVehicle.equals(mainSystemName)) {
            collisionsTree.clear();
            lastMainVehicle = mainSystemName;
        }
        
        ImcSystem mainSystem = ImcSystemsHolder.getSystemWithName(mainSystemName);
        if (mainSystem == null) {
            collisionsTree.clear();
            layerPainter.triggerImageRebuild();
            return;
        }

        // (vehicle, ship) -> (distance, timestamp)
        final ConcurrentHashMap<Pair<String, String>, Pair<Double, Date>> collisions = new ConcurrentHashMap<>();

        final Date datetimeNow = new Date();
        long timeSpanMillis = projectionTimeWindowValue * projectionTimeWindowUnit.getMicroseconds();
        for (long timeOffset = 0; timeOffset < timeSpanMillis; timeOffset += 1_000L * collisionDistance / 4) {
            final long deltaTimeMillis = timeOffset;
            Arrays.stream(ImcSystemsHolder.lookupAllSystems())
                    .filter(system -> !system.getName().equalsIgnoreCase(mainSystemName))
                    .forEach(system -> {
                        Date dataAge = new Date(system.getLocationTimeMillis());
                        if (dataAge.before(datetimeNow) && datetimeNow.getTime() - dataAge.getTime()
                                > minutesToHideSystemsWithoutKnownLocation * 60_000L) {
                            return;
                        }
                        Date t = new Date(System.currentTimeMillis() + deltaTimeMillis);
                        LocationType locationSystem = system.getLocation().getNewAbsoluteLatLonDepth();
                        LocationType locationMain = mainSystem.getLocation().getNewAbsoluteLatLonDepth();
                        String systemName = system.getName();
                        SystemSizeAndCourseData sysData = SystemSizeAndCourseData.from(system);
                        calcDistanceAndAdd(mainSystemName, locationMain, systemName, locationSystem, deltaTimeMillis, t, sysData, collisions);
                    });

            Arrays.stream(ExternalSystemsHolder.lookupAllSystems())
                    .filter(system -> !system.getName().equalsIgnoreCase(mainSystemName))
                    .forEach(system -> {
                        Date dataAge = new Date(system.getLocationTimeMillis());
                        if (dataAge.before(datetimeNow) && datetimeNow.getTime() - dataAge.getTime()
                                > minutesToHideSystemsWithoutKnownLocation * 60_000L) {
                            return;
                        }
                        Date t = new Date(System.currentTimeMillis() + deltaTimeMillis);
                        LocationType locationSystem = system.getLocation().getNewAbsoluteLatLonDepth();
                        LocationType locationMain = mainSystem.getLocation().getNewAbsoluteLatLonDepth();
                        String systemName = system.getName();
                        SystemSizeAndCourseData sysData = SystemSizeAndCourseData.from(system);
                        calcDistanceAndAdd(mainSystemName, locationMain, systemName, locationSystem, deltaTimeMillis, t, sysData, collisions);
                    });
        }

        AtomicBoolean changed = new AtomicBoolean(false);
        if (!collisionsTree.isEmpty()) {
            changed.set(true);
        }
        collisionsTree.clear();

        collisions.forEach((systems, info) -> {
            String vehicle = systems.first();
            String ship = systems.second();
            Date when = info.second();
            double distance = info.first();
            collisionsTree.putIfAbsent(vehicle,
                    Collections.synchronizedSortedMap(new TreeMap<Date, Pair<String, Double>>()));
            collisionsTree.get(vehicle).put(when, new Pair<String, Double>(ship, distance));
            changed.set(true);
        });

        long diff = System.currentTimeMillis() - start;
        NeptusLog.pub().info("Risk detected {} collisions in {} milliseconds.", collisions.size(), diff);

        if (changed.get()) {
            layerPainter.triggerImageRebuild();
        }
    }

    private void calcDistanceAndAdd(String mainSystemName, LocationType locationMain, String systemName,
                                    LocationType locationSystem, long deltaTimeMillis, Date t, SystemSizeAndCourseData sysData,
                                    ConcurrentHashMap<Pair<String, String>, Pair<Double, Date>> collisions) {

        ImcSystem mainSys = ImcSystemsHolder.lookupSystemByName(mainSystemName);
        SystemSizeAndCourseData mainSysData = SystemSizeAndCourseData.from(mainSys);

        LocationType mainSysProjLoc = projectLocationWithCourseAndSpeed(locationMain, mainSysData, deltaTimeMillis);
        LocationType sysProjLoc = projectLocationWithCourseAndSpeed(locationSystem, sysData, deltaTimeMillis);

        //sysData.getTimestampMillis()
        double distance = WGS84Utilities.distance(
                mainSysProjLoc.getLatitudeDegs(), mainSysProjLoc.getLongitudeDegs(),
                sysProjLoc.getLatitudeDegs(), sysProjLoc.getLongitudeDegs());

        if (distance < collisionDistance)
            collisions.putIfAbsent(new Pair<>(mainSystemName, systemName), new Pair<>(distance, t));
    }

    private LocationType projectLocationWithCourseAndSpeed(LocationType locationSystem, SystemSizeAndCourseData sysData,
                                                           long deltaTimeMillis) {
        locationSystem = locationSystem.getNewAbsoluteLatLonDepth();
        // Using the haversine formula, which is a formula used to calculate the great-circle distance
        // between two points on a sphere given their longitudes and latitudes
        double R = 6371.0;
        double distance = sysData.getSpeedMps() * (double)(deltaTimeMillis / 1000L) / 1000.0;
        double bearingDegs = sysData.getCourseDegrees();
        double lat = locationSystem.getLatitudeDegs();
        double lon = locationSystem.getLongitudeDegs();
        double lat2 = Math.asin(Math.sin(0.017453292519943295 * lat) * Math.cos(distance / R)
                + Math.cos(0.017453292519943295 * lat) * Math.sin(distance / R)
                * Math.cos(0.017453292519943295 * bearingDegs));
        double lon2 = 0.017453292519943295 * lon + Math.atan2(Math.sin(0.017453292519943295 * bearingDegs)
                * Math.sin(distance / R) * Math.cos(0.017453292519943295 * lat), Math.cos(distance / R)
                - Math.sin(0.017453292519943295 * lat) * Math.sin(lat2));
        lat2 = Math.toRadians(57.29577951308232 * lat2);
        lon2 = Math.toRadians(57.29577951308232 * lon2);

        locationSystem.setLatitudeRads(lat2);
        locationSystem.setLongitudeRads(lon2);
        return locationSystem;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        AtomicBoolean repaintRequest = new AtomicBoolean(false);

        boolean recreateImage = layerPainter.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        if (recreateImage) {
            Graphics2D imageGraphics = layerPainter.getImageGraphics();
            // Paint what you want in the graphics
            if (!collisionsTree.isEmpty() && collisionsTree.get(lastMainVehicle) != null
                    && !collisionsTree.get(lastMainVehicle).isEmpty()) {
                Graphics2D graphicsContext = (Graphics2D) imageGraphics.create();
                graphicsContext.translate(20, 100);
                boolean res = !graphicsContext.drawImage(colregImage, null, null);
                if (res) {
                    repaintRequest.compareAndSet(false, true);
                }
                graphicsContext.dispose();

                createInfoLabel(imageGraphics);
            }

            AtomicReference<String> shipClosest = detectAndRenderClosestCollision(renderer, sdf, repaintRequest, imageGraphics);

            Point2D indicatorPoint = new Point2D.Double(20 + 25, 100 + 25);
            AtomicReference<Short> mainlookAngle = new AtomicReference<>((short) 0);
            boolean[] lookAngle = {false, false, false, false};
            collisionsTree.get(lastMainVehicle).forEach((time, pair) -> {
                String sysName = pair.first();
                LocationType loc = null;
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sysName);
                if (sys != null) {
                    loc = sys.getLocation().getNewAbsoluteLatLonDepth();
                } else {
                    ExternalSystem esys = ExternalSystemsHolder.lookupSystem(sysName);
                    if (esys != null) {
                        loc = esys.getLocation().getNewAbsoluteLatLonDepth();
                    }
                }
                if (loc != null) {
                    Point2D pointShipClosest = renderer.getScreenPosition(loc);
                    Pair<Double, Short> angleRadAndQuadrant = calculateAngleToRotate(indicatorPoint, pointShipClosest);
                    if (angleRadAndQuadrant.second() == 0) {
                        lookAngle[0] = true;
                    } else if (angleRadAndQuadrant.second() == 1) {
                        lookAngle[1] = true;
                    } else if (angleRadAndQuadrant.second() == 2) {
                        lookAngle[2] = true;
                    } else if (angleRadAndQuadrant.second() == 3) {
                        lookAngle[3] = true;
                    }

                    if (shipClosest.get() != null) {
                        String closestSysName = shipClosest.get();
                        if (closestSysName.equals(sysName)) {
                            mainlookAngle.set(angleRadAndQuadrant.second());
                        }
                    }
                }
            });
            IntStream.range(0, lookAngle.length).filter(i -> lookAngle[i]).forEach(i -> {
                Graphics2D graphicsContext = (Graphics2D) imageGraphics.create();
                graphicsContext.translate(20 + 25, 100 + 25);
                graphicsContext.rotate(Math.PI / 2 + Math.PI / 4 * (2 * i + 1));
                graphicsContext.translate(-7, -25 - 2);
                graphicsContext.setColor(mainlookAngle.get() == i ? shapeHighColor : shapeColor);
                graphicsContext.fill(shapeArrow);
                graphicsContext.setColor(Color.black);
                graphicsContext.draw(shapeArrow);
                graphicsContext.dispose();
            });
        }

        layerPainter.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);
        if (repaintRequest.get()) {
            layerPainter.triggerImageRebuild();
            renderer.invalidate();
            renderer.repaint(10);
        }
    }

    private void createInfoLabel(Graphics2D imageGraphics) {
        Graphics2D graphicsContext;
        int collisionSize = collisionsTree.get(lastMainVehicle).size();
        infoLabel.setText("# " + collisionSize);
        infoLabel.setHorizontalTextPosition(JLabel.CENTER);
        infoLabel.setHorizontalAlignment(JLabel.CENTER);
        infoLabel.setBackground(blackTransparentColor);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoLabel.setOpaque(true);
        graphicsContext = (Graphics2D) imageGraphics.create();
        graphicsContext.translate(20 + 50 + 5 + 2, 100 + 25);
        FontMetrics fontMetrics = graphicsContext.getFontMetrics();
        Rectangle2D rectBounds = fontMetrics.getStringBounds(infoLabel.getText(), graphicsContext);
        infoLabel.setBounds(0, 0, (int) rectBounds.getWidth() + 10, (int) rectBounds.getHeight() + 10);
        infoLabel.setForeground(Color.white);
        infoLabel.paint(graphicsContext);
        graphicsContext.dispose();
    }

    private AtomicReference<String> detectAndRenderClosestCollision(StateRenderer2D renderer, SimpleDateFormat sdf, AtomicBoolean askForLaterRepaint, Graphics2D imageGraphics) {
        AtomicReference<String> shipClosest = new AtomicReference<>(null);
        AtomicDouble distanceClosest = new AtomicDouble(Double.MAX_VALUE);
        AtomicReference<Date> timeClosest = new AtomicReference<>(null);
        collisionsTree.get(lastMainVehicle).forEach((time, pair) -> {
            String sysName = pair.first();
            double distance = pair.second();
            if (distance < distanceClosest.get()) {
                shipClosest.set(sysName);
                distanceClosest.set(distance);
                timeClosest.set(time);

                LocationType loc = null;
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sysName);
                if (sys != null) {
                    loc = sys.getLocation().getNewAbsoluteLatLonDepth();
                } else {
                    ExternalSystem esys = ExternalSystemsHolder.lookupSystem(sysName);
                    if (esys != null) {
                        loc = esys.getLocation().getNewAbsoluteLatLonDepth();
                    }
                }
                if (loc != null) {
                    Graphics2D graphicsContext = (Graphics2D) imageGraphics.create();
                    Point2D spos = renderer.getScreenPosition(loc);
                    graphicsContext.translate(spos.getX() - 20 - 8, spos.getY());
                    boolean res = !graphicsContext.drawImage(colregImageSmall, null, null);
                    if (res) {
                        askForLaterRepaint.compareAndSet(false, true);
                    }
                    graphicsContext.dispose();
                }

            }
            //Point2D pt = renderer.getScreenPosition(loc);
        });
        if (shipClosest.get() != null) {
            String line1 = shipClosest.get();
            String line2 = Math.round(distanceClosest.get()) + "m at " + sdf.format(timeClosest.get());
            infoLabel.setText("<html>" + line1 + "<br>" + line2 + "</html>");
            infoLabel.setForeground(Color.white);
            infoLabel.setHorizontalAlignment(JLabel.LEFT);
            Graphics2D graphicsContext = (Graphics2D) imageGraphics.create();
            graphicsContext.translate(20, 100 + 50 + 5 + 5);
            FontMetrics fontMetrics = graphicsContext.getFontMetrics();
            Rectangle2D rectBounds = fontMetrics.getStringBounds(line1, graphicsContext);
            Rectangle2D rectBounds2 = fontMetrics.getStringBounds(line2, graphicsContext);
            infoLabel.setBounds(0, 0, (int) Math.max(rectBounds.getWidth(), rectBounds2.getWidth()) + 10,
                    (int) (rectBounds.getHeight() + rectBounds2.getHeight() + 10));
            infoLabel.paint(graphicsContext);
            graphicsContext.dispose();
        }
        return shipClosest;
    }

    private Pair<Double, Short> calculateAngleToRotate(Point2D indicatorPoint, Point2D pointShipClosest) {
        double angleRad = AngleUtils.nomalizeAngleRadsPi(AngleUtils.calcAngle(
                indicatorPoint.getY(), indicatorPoint.getX(),
                pointShipClosest.getY(), pointShipClosest.getX()));
        double angleDeg = Math.toDegrees(angleRad);
        short quadrant = 0;
        if (angleRad >= 0 && angleRad <= Math.PI / 2) {
            angleRad = Math.PI / 4;
        } else if (angleRad >= Math.PI / 2 && angleRad <= Math.PI) {
            angleRad = 3 * Math.PI / 4;
            quadrant = 1;
        } else if (angleRad <= 0 && angleRad >= -Math.PI / 2) {
            angleRad = -Math.PI / 4;
            quadrant = 3;
        } else if (angleRad <= -Math.PI / 2 && angleRad >= -Math.PI) {
            angleRad = -3 * Math.PI / 4;
            quadrant = 2;
        }
        double angleDeg1 = Math.toDegrees(angleRad);
        System.out.println("angleDeg: " + angleDeg + "   :: angleDeg1: " + angleDeg1);
        return new Pair<>(angleRad, quadrant);
    }
}
