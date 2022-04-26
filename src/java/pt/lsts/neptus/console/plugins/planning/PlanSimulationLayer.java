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
 * Author: zp
 * Jan 23, 2014
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.preview.PlanSimulationListener;
import pt.lsts.neptus.mp.preview.PlanSimulationOverlay;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.plan.PlanCompatibility;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Plan Simulation", icon = "images/planning/robot.png")
public class PlanSimulationLayer extends ConsoleLayer implements PlanSimulationListener {

    private PlanSimulationOverlay simOverlay = null;
    private PlanType mainPlan = null;
    private Image errorImage = ImageUtils.getImage("pt/lsts/neptus/console/plugins/planning/error.png");
    private Image warnImage = ImageUtils.getImage("pt/lsts/neptus/console/plugins/planning/warning.png");
    private Image fineImage = ImageUtils.getImage("pt/lsts/neptus/console/plugins/planning/fine.png");

    @NeptusProperty(name = "Max AUV distance", description = "Warn user if AUV distance exceeds this value", userLevel = LEVEL.REGULAR)
    private double maxAUVDistance = 1000;

    @NeptusProperty(name = "Max AUV end distance", description = "Warn user if last planned AUV position is further than this distance away from base", userLevel = LEVEL.REGULAR)
    private double maxAUVDistAtEnd = 300;

    @NeptusProperty(name = "Max UAV distance", description = "Warn user if UAV distance exceeds this value", userLevel = LEVEL.REGULAR)
    private double maxUAVDistance = 10000;

    @NeptusProperty(name = "Max UAV end distance", description = "Warn user if last planned UAV position is further than this distance away from base", userLevel = LEVEL.REGULAR)
    private double maxUAVDistAtEnd = 500;

    @NeptusProperty(name = "Use My State (true) or Home Ref (false)", userLevel = LEVEL.ADVANCED)
    private boolean useMystate = true;
    
    private enum PlanCheck {
        Fine,
        Warning,
        Error
    }

    private Vector<Pair<PlanCheck, String>> checks = new Vector<>();

    @Subscribe
    public void on(ConsoleEventPlanChange evt) {
        try {
        this.mainPlan = evt.getCurrent();
        refreshOverlay();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void on(ConsoleEventMainSystemChange evt) {
        refreshOverlay();
    }

    @Override
    public boolean userControlsOpacity() {
        return true;
    }

    @Override
    public void simulationFinished(PlanSimulationOverlay source) {
        source.removeListener(this);
        validatePlan();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        if (simOverlay != null) {
            simOverlay.paint((Graphics2D) g.create(), renderer);
        }
        g.setColor(Color.white);
        int pos = 20;
        for (Pair<PlanCheck, String> check : checks) {
            switch (check.first()) {
                case Error:
                    g.drawImage(errorImage, 5, pos - 12, null);
                    break;
                case Warning:
                    g.drawImage(warnImage, 5, pos - 12, null);
                    break;
                case Fine:
                    g.drawImage(fineImage, 5, pos - 12, null);
                    break;
                default:
                    break;
            }
            g.drawString(check.second(), 24, pos);
            pos += 20;
        }
    }

    @Override
    public void initLayer() {
        mainPlan = getConsole().getPlan();
        refreshOverlay();
    }

    private void refreshOverlay() {
        if (mainPlan != null) {
            synchronized (PlanSimulationLayer.this) {
                VehicleType vt = mainPlan.getVehicleType();
                simOverlay = new PlanSimulationOverlay(mainPlan, 0, vt == null ? VehicleType.MAX_DURATION_H : vt.getMaxDurationHours(), null);
                simOverlay.addListener(this);
            }
        }
        else
            simOverlay = null;
    }

    private synchronized void validatePlan() {
        checks.clear();
        if (mainPlan == null)
            return;

        try {
            mainPlan.validatePlan();            
        }
        catch (Exception e) {
            checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Error, e.getMessage()));
        }

        checks.addAll(validatePlanCompatibility());
        checks.addAll(validateDistances());
        checks.addAll(validateCollisions());

        if (checks.isEmpty()) {
            checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Fine, I18n.textf(
                    "Plan takes approximately %timeAmount",
                    DateTimeUtil.milliSecondsToFormatedString((long) (simOverlay.getTotalTime() * 1000)))));
        }
    }

    private List<Pair<PlanCheck, String>> validateDistances() {
        VehicleType v = VehiclesHolder.getVehicleById(getConsole().getMainSystem());
        LocationType base = useMystate ? MyState.getLocation() : getConsole().getMission().getHomeRef();
        double maxDistToBase = 0;
        double distAtEnd = 0;

        ArrayList<Pair<PlanCheck, String>> checks = new ArrayList<>();

        synchronized (PlanSimulationLayer.this) {
            for (SystemPositionAndAttitude s : simOverlay.getStates().toArray(new SystemPositionAndAttitude[0])) {
                distAtEnd = s.getPosition().getDistanceInMeters(base);
                maxDistToBase = Math.max(distAtEnd, maxDistToBase);
            }
        }

        if (v != null) {
            if ("auv".equalsIgnoreCase(v.getType()) || "uuv".equalsIgnoreCase(v.getType())) {
                if (maxDistToBase > maxAUVDistance) {
                    checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Warning, I18n.textf(
                            "%vehicle will be %maxDistToBase meters away from here", v.getId(), (int) maxDistToBase)));
                }
                if (distAtEnd > maxAUVDistAtEnd) {
                    checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Warning, I18n.textf(
                            "%vehicle will finish %distance  meters away from base", v.getId(), (int) distAtEnd)));
                }
            }
            else if ("uav".equalsIgnoreCase(v.getType())) {
                if (maxDistToBase > maxUAVDistance) {
                    checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Warning, I18n.textf(
                            "%vehicle will be %maxDistToBase meters away from here", v.getId(), (int) maxDistToBase)));
                }
                if (distAtEnd > maxUAVDistAtEnd) {
                    checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Warning, I18n.textf(
                            "%vehicle will finish %distance  meters away from base", v.getId(), (int) distAtEnd)));
                }
            }
        }

        return checks;
    }

    private List<Pair<PlanCheck, String>> validateCollisions() {
        ArrayList<Pair<PlanCheck, String>> checks = new ArrayList<>();
        Vector<AbstractElement> obstacles = MapGroup.getMapGroupInstance(getConsole().getMission()).getObstacles();

        synchronized (PlanSimulationLayer.this) {
            for (SystemPositionAndAttitude s : simOverlay.getStates().toArray(new SystemPositionAndAttitude[0])) {
                for (AbstractElement a : obstacles) {
                    if (a.containsPoint(s.getPosition(), null)) {
                        checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Warning,
                                I18n.textf("Vehicle may collide with %obstacle", a.getId())));
                        return checks;
                    }
                }
            }
        }

        return checks;
    }

    private List<Pair<PlanCheck, String>> validatePlanCompatibility() {
        ArrayList<Pair<PlanCheck, String>> checks = new ArrayList<>();
        try {
            PlanCompatibility.testCompatibility(VehiclesHolder.getVehicleById(getConsole().getMainSystem()), mainPlan);
        }
        catch (Exception e) {
            checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Warning, e.getMessage()));
        }

        return checks;
    }

    @Override
    public void cleanLayer() {

    }

}
