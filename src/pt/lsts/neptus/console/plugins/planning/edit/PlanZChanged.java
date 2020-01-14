/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Nov 29, 2011
 */
package pt.lsts.neptus.console.plugins.planning.edit;

import java.util.LinkedHashMap;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class PlanZChanged extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, Z_UNITS> previousUnits;
    protected LinkedHashMap<String, Double> previousValues;
    protected double newZ;
    protected Z_UNITS newUnits;
    protected PlanType plan;

    public PlanZChanged(PlanType plan, double newZ, Z_UNITS newUnits, LinkedHashMap<String, Z_UNITS> previousUnits,
            LinkedHashMap<String, Double> previousValues) {
        this.plan = plan;
        this.newZ = newZ;
        this.newUnits = newUnits;
        this.previousUnits = previousUnits;
        this.previousValues = previousValues;
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
        return I18n.textf("Set %zunits of %newValue in the entire plan.", newUnits.name(),
                newZ);
    }

    @Override
    public void undo() throws CannotUndoException {
        for (String key : previousUnits.keySet()) {
            LocatedManeuver man = (LocatedManeuver)plan.getGraph().getManeuver(key); 
            ManeuverLocation loc = man.getManeuverLocation();
            loc.setZ(previousValues.get(key));
            loc.setZUnits(previousUnits.get(key));
            man.setManeuverLocation(loc);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (m instanceof LocatedManeuver) {
                LocatedManeuver man = (LocatedManeuver)m; 
                ManeuverLocation loc = man.getManeuverLocation();
                loc.setZ(newZ);
                loc.setZUnits(newUnits);
                man.setManeuverLocation(loc);
            }                
        }
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }

}
