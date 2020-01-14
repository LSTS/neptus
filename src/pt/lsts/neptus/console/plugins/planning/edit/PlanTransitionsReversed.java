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
 * Author: Paulo Dias
 * Apr 3, 2016
 */
package pt.lsts.neptus.console.plugins.planning.edit;

import java.util.Arrays;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.types.mission.TransitionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PlanTransitionsReversed extends AbstractUndoableEdit {
    protected PlanType plan;
    protected Maneuver startManeuver;
    protected Maneuver endManeuver;

    public PlanTransitionsReversed(PlanType plan, Maneuver startManeuver, Maneuver endManeuver) {
        this.plan = plan;
        this.startManeuver = startManeuver;
        this.endManeuver = endManeuver;
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
        return I18n.text("Reverse the plan transitions direction");
    }

    @Override
    public void undo() throws CannotUndoException {
        reverseTransitions();
        plan.getGraph().setInitialManeuver(startManeuver.getId());
//        startManeuver.setInitialManeuver(true);
//        endManeuver.setInitialManeuver(false);
    }

    @Override
    public void redo() throws CannotRedoException {
        reverseTransitions();
        plan.getGraph().setInitialManeuver(endManeuver.getId());
//        startManeuver.setInitialManeuver(false);
//        endManeuver.setInitialManeuver(true);
    }

    private void reverseTransitions() {
        List<TransitionType> tList = Arrays.asList(plan.getGraph().getAllEdges());
        for (TransitionType t : tList) {
            plan.getGraph().removeTransition(t);
            String sm = t.getSourceManeuver();
            String tm = t.getTargetManeuver();
            t.setSourceManeuver(tm);
            t.setTargetManeuver(sm);
            plan.getGraph().addTransition(t);
        }
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }
    
    /**
     * @return the startManeuver
     */
    public Maneuver getStartManeuver() {
        return startManeuver;
    }
    
    /**
     * @return the endManeuver
     */
    public Maneuver getEndManeuver() {
        return endManeuver;
    }
}
