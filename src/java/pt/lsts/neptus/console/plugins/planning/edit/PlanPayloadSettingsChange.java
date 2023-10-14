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
 * Author: pdias
 * 19/05/2017
 */
package pt.lsts.neptus.console.plugins.planning.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.dom4j.DocumentHelper;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PlanPayloadSettingsChange extends AbstractUndoableEdit {

    protected PlanType plan;
    protected String beforeStartActions;
    protected String beforeEndActions;
    protected String afterStartActions;
    protected String afterEndActions;

    public PlanPayloadSettingsChange(PlanType plan, String beforeStartActions, String beforeEndActions) {
        this.plan = plan;
        this.beforeStartActions = beforeStartActions;
        this.beforeEndActions = beforeEndActions;
        this.afterStartActions = plan.getStartActions().asDocument("startActions").asXML();
        this.afterEndActions = plan.getEndActions().asDocument("endActions").asXML();
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
        return I18n.text("Change plan actions.");
    }

    @Override
    public void undo() throws CannotUndoException {
        try {
            plan.getStartActions().load(DocumentHelper.parseText(beforeStartActions).getRootElement());
            plan.getEndActions().load(DocumentHelper.parseText(beforeEndActions).getRootElement());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.toString());
            throw new CannotUndoException();
        }
    }

    @Override
    public void redo() throws CannotRedoException {      
        try {
            plan.getStartActions().load(DocumentHelper.parseText(afterStartActions).getRootElement());
            plan.getEndActions().load(DocumentHelper.parseText(afterEndActions).getRootElement());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            throw new CannotRedoException();
        }
    }
}
