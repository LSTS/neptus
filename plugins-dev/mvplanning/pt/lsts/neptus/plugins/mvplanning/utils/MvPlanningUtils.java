/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 8 Jun 2016
 */

package pt.lsts.neptus.plugins.mvplanning.utils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Payload;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.position.painter.SystemInfoPainter;
import pt.lsts.neptus.types.coord.LocationType;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author tsmarques
 *
 */
public class MvPlanningUtils {

    /**
     * Builds a new maneuver for a task/plan of type taskType
     * according to the given profile
     * */
    public static Maneuver buildManeuver(Profile planProfile, LocationType manLoc, PlanTask.TASK_TYPE taskType) {
        Maneuver newMan;
        String manType;
        if(taskType == PlanTask.TASK_TYPE.COVERAGE_AREA) {
            newMan = new FollowPath();
            manType = "FollowPath";
        }
        else {
            newMan = new Goto();
            manType = "Goto";
        }

        /* Maneuver's Z */
        ManeuverLocation loc = new ManeuverLocation(manLoc);
        loc.setZ(planProfile.getProfileZ());
        loc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        ((LocatedManeuver) newMan).setManeuverLocation(loc);

        /* Maneuver's speed */
        System.out.println("** " + planProfile.getProfileSpeed() + " " + planProfile.getSpeedUnits());
        DefaultProperty units = PropertiesEditor.getPropertyInstance("Speed units", String.class, planProfile.getSpeedUnits(), true);
        DefaultProperty propertySpeed = PropertiesEditor.getPropertyInstance("Speed", Double.class, planProfile.getProfileSpeed(), true);
        propertySpeed.setDisplayName(I18n.text("Speed"));
        units.setDisplayName(I18n.text("Speed units"));
        units.setShortDescription(I18n.text("The speed units"));

        /* start actions */
        Property startActionsProperty = setupPayloads(planProfile, manType);

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
    public static Property setupPayloads(Profile planProfile, String manType) {
        List<Payload> payloadList = planProfile.getPayload();
        Vector<IMCMessage> setParamsMsg = new Vector<>();

        NeptusLog.pub().debug("\n");
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

    /**
     * Builds an Area object with the given characteristics
     * */
    public static Area buildArea(LocationType origin, LocationType center, double width, double height, double yaw) {
        double[] offsets = center.getOffsetFrom(origin);
        Rectangle2D.Double areaRec = new Rectangle2D.Double(-width/2, -height/2, width, height);

        AffineTransform transform = new AffineTransform();
        transform.translate(offsets[0], offsets[1]);
        transform.rotate(yaw);

        Area area = new Area(areaRec);
        area.transform(transform);

        return area;
    }
}
