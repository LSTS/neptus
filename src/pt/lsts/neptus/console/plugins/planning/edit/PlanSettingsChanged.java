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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.commons.lang.StringUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class PlanSettingsChanged extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, Vector<DefaultProperty>> previousSettings;
    protected Collection<DefaultProperty> newSettings;
    protected PlanType plan;
    
    public PlanSettingsChanged(PlanType plan, Collection<DefaultProperty> newSettings, LinkedHashMap<String, Vector<DefaultProperty>> previousSettings) {
        this.plan = plan;
        this.newSettings = newSettings;
        this.previousSettings = previousSettings;
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
        Vector<String> settings = new Vector<String>();
        for (DefaultProperty p : newSettings)
            settings.add(p.getDisplayName());
        
        return I18n.textf("Change %planSettings in the entire plan.", StringUtils.join(settings, ", "));
    }

    @Override
    public void undo() throws CannotUndoException {
        for (String key : previousSettings.keySet()) {
            try {
                plan.getGraph().getManeuver(key).setProperties(previousSettings.get(key).toArray(new DefaultProperty[0]));
            }
            catch (Exception e) {
                NeptusLog.pub().error(e, e);
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {      
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            try {
                m.setProperties(newSettings.toArray(new DefaultProperty[0]));
            }
            catch (Exception e) {
                NeptusLog.pub().error(e, e);
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
