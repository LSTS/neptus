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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.util.WGS84Utilities;

import java.awt.Graphics2D;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@PluginDescription(name = "Alert Intrusion", icon = "pt/lsts/neptus/plugins/alertintrusion/alert-intrusion.png",
    description = "Alert Intrusion", category = PluginDescription.CATEGORY.INTERFACE, version = "0.1")
@LayerPriority(priority = 121)
public class AlertIntrusion extends ConsoleLayer implements MainVehicleChangeListener {

    @NeptusProperty(name = "Minimum distance allowed between AUVs and Ships (meters)")
    public int collisionDistance = 100;
    @NeptusProperty(name = "Percentage of the critical distance to trigger the alert")
    public int collisionCriticalDistancePercentage = 20;

    private OffScreenLayerImageControl layerPainter;
    //private Map<String, VehicleRiskAnalysis> state = new ConcurrentHashMap<>();
    //private Map<String, VehicleRiskPanel> panels = new ConcurrentHashMap<>();

    private String lastMainVehicle;
    private Map<String, Map<Date, Pair<String, Double>>> collisionsTree = new ConcurrentHashMap<>();

    public AlertIntrusion() {
        super();
    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
        layerPainter = new OffScreenLayerImageControl();
    }

    @Override
    public void cleanLayer() {
        layerPainter = null;
    }

    @Override
    public void mainVehicleChange(String id) {

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

        for (long timeOffset = 0; timeOffset < 3_600 * 3_000; timeOffset += 1_000 * collisionDistance / 4) {
            final long time = timeOffset;
            Arrays.stream(ImcSystemsHolder.lookupAllSystems())
                    .filter(system -> !system.getName().equalsIgnoreCase(mainSystemName))
                    .forEach(system -> {
                        Date t = new Date(System.currentTimeMillis() + time);
                        LocationType locationSystem = system.getLocation().getNewAbsoluteLatLonDepth();
                        LocationType locationMain = mainSystem.getLocation().getNewAbsoluteLatLonDepth();
                        String systemName = system.getName();
                        calcDistanceAndAdd(mainSystemName, locationMain, systemName, locationSystem, t, collisions);
                    });

            Arrays.stream(ExternalSystemsHolder.lookupAllSystems())
                    .filter(system -> !system.getName().equalsIgnoreCase(mainSystemName))
                    .forEach(system -> {
                        Date t = new Date(System.currentTimeMillis() + time);
                        LocationType locationSystem = system.getLocation().getNewAbsoluteLatLonDepth();
                        LocationType locationMain = mainSystem.getLocation().getNewAbsoluteLatLonDepth();
                        String systemName = system.getName();
                        calcDistanceAndAdd(mainSystemName, locationMain, systemName, locationSystem, t, collisions);
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
        NeptusLog.pub().info("RiskAnalysis detected {} collisions in {} milliseconds.", collisions.size(), diff);

        if (changed.get()) {
            layerPainter.triggerImageRebuild();
        }
    }

    private void calcDistanceAndAdd(String mainSystemName, LocationType locationMain, String systemName,
                                    LocationType locationSystem, Date t, ConcurrentHashMap<Pair<String, String>,
                                    Pair<Double, Date>> collisions) {
        double distance = WGS84Utilities.distance(
                locationMain.getLatitudeDegs(), locationMain.getLongitudeDegs(),
                locationSystem.getLatitudeDegs(), locationSystem.getLongitudeDegs());
        if (distance < collisionDistance)
            collisions.putIfAbsent(new Pair<>(mainSystemName, systemName), new Pair<>(distance, t));
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        boolean recreateImage = layerPainter.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        if (recreateImage) {
            Graphics2D g2 = layerPainter.getImageGraphics();
            // Paint what you want in the graphics
            collisionsTree.get(lastMainVehicle).forEach((time, pair) -> {
                String ship = pair.first();
                double distance = pair.second();
                g2.drawString("Collision with " + ship + " at " + sdf.format(time) + " (" + distance + "m)", 10, 10);
            });

        }
        layerPainter.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);
    }
}
