/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: tsm
 * 29 Feb 2017
 */
package pt.lsts.neptus.plugins.mvplanner;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.plugins.mvplanner.algorithms.SpiralSTC;
import pt.lsts.neptus.plugins.mvplanner.jaxb.Payload;
import pt.lsts.neptus.plugins.mvplanner.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanner.tasks.SurveyTask;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;

import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author tsmarques
 * @date 1/29/17
 */
public class PlanGenerator {
    private ConsoleLayout console;


    public void setConsole(ConsoleLayout console) {
        this.console = console;
    }

    /**
     * Generate the plan corresponding to this task and its
     * profile
     * */
    public PlanType generate(PlanTask task) {
        if(task.getTaskType() == PlanTask.TaskTypeEnum.Survey) {
            PlanType newPlan = generateSurvey((SurveyTask) task);
            updatePlansList(newPlan);

            return newPlan;
        }

        return null;
    }

    /**
     * Generate a survey plan
     * */
    private PlanType generateSurvey(SurveyTask task) {
        List<ManeuverLocation> path = new SpiralSTC(task.getSurveyArea()).getPath();
        ManeuverLocation loc = path.get(0);
        Vector<double[]> offsets = new Vector<>();

        FollowPath fpath = (FollowPath) setupManeuver(task, path.get(0));

        for(ManeuverLocation point : path) {
            double[] newPoint = new double[4];
            double[] pOffsets = point.getOffsetFrom(loc);

            newPoint[0] = pOffsets[0];
            newPoint[1] = pOffsets[1];
            newPoint[2] = pOffsets[2];

            offsets.add(newPoint);
        }

        fpath.setOffsets(offsets);

        PlanType plan = new PlanType(console.getMission());
        plan.getGraph().addManeuver(fpath);
        plan.setId(task.getId());

        return plan;
    }

    /**
     * Setups the parameters for the "main" maneuver of the
     * given task
     * */
    private Maneuver setupManeuver(PlanTask task, LocationType maneuverLoc) {
        Maneuver newMan;
        String manType;

        if(task.getTaskType() == PlanTask.TaskTypeEnum.Survey) {
            newMan = new FollowPath();
            manType = "FollowPath";
        }
        else {
            newMan = new Goto();
            manType = "Goto";
        }

        /* Maneuver's Z */
        ManeuverLocation loc = new ManeuverLocation(maneuverLoc);
        loc.setZ(task.getProfile().getProfileZ());
        loc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        ((LocatedManeuver) newMan).setManeuverLocation(loc);

        /* Maneuver's speed */
        System.out.println("** " + task.getProfile().getProfileSpeed() + " " + task.getProfile().getSpeedUnits());
        DefaultProperty units = PropertiesEditor.getPropertyInstance("Speed units", String.class, task.getProfile().getSpeedUnits(), true);
        DefaultProperty propertySpeed = PropertiesEditor.getPropertyInstance("Speed", Double.class, task.getProfile().getProfileSpeed(), true);
        propertySpeed.setDisplayName(I18n.text("Speed"));
        units.setDisplayName(I18n.text("Speed units"));
        units.setShortDescription(I18n.text("The speed units"));

        /* start actions */
        Property startActionsProperty = setupPayloads(task.getProfile(), manType);

        Property[] props = new Property[] {units, propertySpeed, startActionsProperty};
        newMan.setProperties(props);

        return newMan;
    }

    /**
     * Fetch payloads needed for the profile and returns a start-actions
     * Property object to be added to the maneuver's properties
     *
     * @return {@link Property} object with the start-actions
     * */
    private Property setupPayloads(Profile planProfile, String manType) {
        List<Payload> payloadList = planProfile.getPayload();
        Vector<IMCMessage> setParamsMsg = new Vector<>();

        for(Payload payload : payloadList) {
            NeptusLog.pub().debug("[" + payload.getPayloadType() + "] ");
            SetEntityParameters paramsMsg = new SetEntityParameters();
            paramsMsg.setName(payload.getPayloadType());
            Vector<EntityParameter> parametersList = new Vector<>();

            /* create parameters according to profile */
            for(Map.Entry<String, String> entry : payload.getPayloadParameters().entrySet()) {
                parametersList.add(new EntityParameter(entry.getKey(), entry.getValue()));
                NeptusLog.pub().debug("[" + entry.getKey() + ", " + entry.getValue() + "]");
            }

            paramsMsg.setParams(parametersList);
            setParamsMsg.add(paramsMsg);
        }

        PlanActions startActions = new PlanActions();
        startActions.parseMessages(setParamsMsg);
        Property startActionsProperty = PropertiesEditor.getPropertyInstance("start-actions", manType + " start actions",
                PlanActions.class, startActions, false);

        System.out.println(startActionsProperty.getCategory().equalsIgnoreCase("%s start actions"));
        System.out.println(startActionsProperty.getCategory());

        return startActionsProperty;
    }


    private void updatePlansList(PlanType planType) {
        /* add plan to plan's tree */
        console.getMission().addPlan(planType);
        console.getMission().save(true);
    }
}
