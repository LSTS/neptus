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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Nov 29, 2011
 */
package pt.lsts.neptus.console.plugins.planning.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.AngleCalc;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * @author zp
 *
 */
public class PlanRotated extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected PlanType plan;
    protected LocatedManeuver pivot;
    protected double angleRads;

    public PlanRotated(PlanType plan, LocatedManeuver pivot, double angleRads) {
        this.plan = plan;
        this.pivot = pivot;
        this.angleRads = angleRads;
    }

    @Override
    public boolean canUndo() {
        return true;
    }

    @Override
    public boolean canRedo() {
        return true;
    }

    @Override
    public String getPresentationName() {
        return I18n.text("Rotate the plan");
    }

    @Override
    public void undo() throws CannotUndoException {
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (m != pivot && m instanceof LocatedManeuver) {
                LocatedManeuver satellite = (LocatedManeuver) m;
                double[] top = pivot.getManeuverLocation().getDistanceInPixelTo(satellite.getManeuverLocation(),
                        MapTileUtil.LEVEL_OFFSET);
                double[] topR = AngleCalc.rotate(2 * -angleRads, top[0], top[1], false);
                double deltaX = topR[0];
                double deltaY = topR[1];
                LocationType lt = new LocationType(pivot.getManeuverLocation());
                lt.translateInPixel(deltaX, deltaY, MapTileUtil.LEVEL_OFFSET);
                lt.setAbsoluteDepth(satellite.getManeuverLocation().getAllZ());
                ManeuverLocation l = satellite.getManeuverLocation();
                l.setLocation(lt);
                satellite.setManeuverLocation(l);
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (m != pivot && m instanceof LocatedManeuver) {
                LocatedManeuver satellite = (LocatedManeuver) m;
                double[] top = pivot.getManeuverLocation().getDistanceInPixelTo(satellite.getManeuverLocation(),
                        MapTileUtil.LEVEL_OFFSET);
                double[] topR = AngleCalc.rotate(2 * angleRads, top[0], top[1], false);
                double deltaX = topR[0];
                double deltaY = topR[1];
                LocationType lt = new LocationType(pivot.getManeuverLocation());
                lt.translateInPixel(deltaX, deltaY, MapTileUtil.LEVEL_OFFSET);
                lt.setAbsoluteDepth(satellite.getManeuverLocation().getAllZ());
                ManeuverLocation l = satellite.getManeuverLocation();
                l.setLocation(lt);
                satellite.setManeuverLocation(l);
            }
        }
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }

    /**
     * @return the pivot
     */
    public LocatedManeuver getPivot() {
        return pivot;
    }

    /**
     * @return the angle
     */
    public double getAngle() {
        return angleRads;
    }
}
