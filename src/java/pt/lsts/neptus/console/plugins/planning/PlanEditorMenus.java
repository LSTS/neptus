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
 * 17/04/2017
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.undo.UndoManager;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.planning.edit.PlanElementAdded;
import pt.lsts.neptus.console.plugins.planning.edit.PlanElementRemoved;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.element.IPlanElement;
import pt.lsts.neptus.mp.element.PlanElements;
import pt.lsts.neptus.mp.element.PlanElementsFactory;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
class PlanEditorMenus {

    private PlanEditorMenus() {
    }

    static void addPlanElementsMenuItems(PlanEditor planEditor, PlanType plan, MouseEvent event,
            StateRenderer2D source, JPopupMenu popup, UndoManager undoManager) {
        
        String vehicleName = plan.getVehicle();
        VehicleType vehicle = VehiclesHolder.getVehicleById(vehicleName);
        if (vehicle == null)
            return;
        
        PlanElementsFactory pef = vehicle.getPlanElementsFactory();
        if (pef == null || pef.getPlanElementsSize() == 0)
            return;
        
        ArrayList<Action> menuActions = new ArrayList<>();
        
        List<Class<IPlanElement<?>>> peiClasses = pef.getPlanElementsClasses();
        peiClasses.stream().forEach(peClass -> {
            final boolean isEditOrNew;
            PlanElements pElems = plan.getPlanElements();
            IPlanElement<?> pe = pElems.getPlanElements().stream().filter(t -> t.getClass() == peClass)
                    .findFirst().orElse(null);
            if (pe == null)
                isEditOrNew = false;
            else
                isEditOrNew = true;
            
            final IPlanElement<?> rpel = pe;
            String name = rpel == null ? I18n.text(pef.getNameFor(peClass)) : I18n.text(rpel.getName());
            @SuppressWarnings("serial")
            AbstractAction addOrChange = new AbstractAction(
                    isEditOrNew ? I18n.textf("Edit %name", name) : I18n.textf("Add %name", name)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    IPlanElement<?> el = rpel;
                    if (!isEditOrNew) {
                        el = pef.getPlanInstance(peClass);
                        if (el == null) {
                            NeptusLog.pub().error("Not possible to create a new plan element " + peClass.getSimpleName());
                            GuiUtils.errorMessage(planEditor.getConsole(), I18n.text("Adding new plan element"),
                                    I18n.text("Not possible to create a new plan element"));
                            return;
                        }
                        
                        pElems.getPlanElements().add(el);
                        PlanElementAdded peaEvt = new PlanElementAdded(el, plan);
                        undoManager.addEdit(peaEvt);
                    }
                    else {
                        pef.configureInstance(el);
                    }
                    
                    planEditor.updateDelegate(el, source);
                }
            };
            menuActions.add(addOrChange);
            
            if (isEditOrNew) {
                @SuppressWarnings("serial")
                AbstractAction remove = new AbstractAction(I18n.textf("Remove %name", name)) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        pElems.getPlanElements().remove(rpel);
                        PlanElementRemoved peaEvt = new PlanElementRemoved(rpel, plan);
                        undoManager.addEdit(peaEvt);

                        planEditor.updateDelegate((StateRendererInteraction) null, source);
                    }
                };
                menuActions.add(remove);
            }
        });
        
        if (!menuActions.isEmpty()) {
            popup.addSeparator();
            menuActions.stream().forEach(m -> popup.add(m));
        }
    }
}
