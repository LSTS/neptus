/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
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
import java.util.Vector;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.mp.preview.PlanSimulationOverlay;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanCompability;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ImageUtils;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="Plan Simulation",icon="images/planning/robot.png")
public class PlanSimulationLayer extends ConsoleLayer {

    private PlanSimulationOverlay simOverlay = null;
    private PlanType mainPlan = null;
    private Image errorImage = ImageUtils.getImage("pt/lsts/neptus/console/plugins/planning/error.png");
    private Image warnImage = ImageUtils.getImage("pt/lsts/neptus/console/plugins/planning/warning.png");
    private Image fineImage = ImageUtils.getImage("pt/lsts/neptus/console/plugins/planning/fine.png");
    private enum PlanCheck {
        Fine, Warning, Error
    }
    
    private Vector<Pair<PlanCheck, String>> checks = new Vector<>();
    
    @Subscribe
    public void on(ConsoleEventPlanChange evt) {
        this.mainPlan = evt.getCurrent();
        refreshOverlay();
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
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        if (simOverlay != null) {
            simOverlay.paint((Graphics2D)g.create(), renderer);
        }
        g.setColor(Color.white);
        int pos = 20;
        for (Pair<PlanCheck, String> check : checks) {
            switch (check.first()) {
                case Error:
                    g.drawImage(errorImage, 50, pos-12, null);
                    break;
                case Warning:
                    g.drawImage(warnImage, 50, pos-12, null);
                    break;
                case Fine:
                    g.drawImage(fineImage, 50, pos-12, null);
                    break;
                default:
                    break;
            }
            g.drawString(check.second(), 70, pos);
            pos += 20;
        }
    }

    @Override
    public void initLayer() {
        mainPlan = getConsole().getPlan();
        refreshOverlay();
    }
    
    private void refreshOverlay() {
        if (mainPlan != null)
            simOverlay = new PlanSimulationOverlay(mainPlan, 0, 4, null);
        else
            simOverlay = null;
        validatePlan();
    }
    
    private synchronized void validatePlan() {
        checks.clear();
        if (mainPlan == null)
            return;
        
        try {
            mainPlan.validatePlan();
            checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Fine, "Plan definition is valid"));
        }
        catch (Exception e) {
            checks.add(new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Error, e.getMessage()));
        }
        
        checks.add(validateVehicle());
        checks.add(validatePayload());
    }
    
    private Pair<PlanCheck, String> validateVehicle() {
        if (mainPlan.getVehicle().equals(getConsole().getMainSystem())) {
            return new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Fine, "Vehicle matches the plan");            
        }
        else {
            return new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Warning, "Console and plan vehicles differ");
        }
    }
    
    
    private Pair<PlanCheck, String> validatePayload() {
        try {
            PlanCompability.testCompatibility(VehiclesHolder.getVehicleById(getConsole().getMainSystem()), mainPlan);
            return new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Fine, "Payload configuration is valid");
        }
        catch (Exception e) {
            return new Pair<PlanSimulationLayer.PlanCheck, String>(PlanCheck.Warning, e.getMessage());
        }
        
        
    }

    @Override
    public void cleanLayer() {

    }

}
