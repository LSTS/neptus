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
 * Author: zp
 * Jan 22, 2014
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.VehicleChooser;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.templates.AbstractPlanTemplate;
import pt.lsts.neptus.mp.templates.ScriptedPlanTemplate;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * @author zp
 * 
 */
public class PlanTemplatesDialog {

    private ConsoleLayout console;
    private ImageIcon templateIcon = ImageUtils.getIcon("images/planning/template.png");
    private final String scriptsDir = "conf/planscripts/";
    
    public PlanTemplatesDialog(ConsoleLayout console) {
        this.console = console;
    }

    public void showDialog() {
        VehicleType choice = null;
        if (console.getMainSystem() != null)
            choice = VehicleChooser.showVehicleDialog(null, VehiclesHolder.getVehicleById(console.getMainSystem()), console);
        else
            choice = VehicleChooser.showVehicleDialog(null, null, console);

        if (choice == null)
            return;

        Class<?>[] classes = ReflectionUtil.listPlanTemplates();
        File[] scripts = new File(scriptsDir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("js");
            }
        });

        String[] names = new String[classes.length + scripts.length];
        for (int i = 0; i < classes.length; i++) {
            names[i] = PluginUtils.getPluginName(classes[i]);
        }
        for (int i = 0; i < scripts.length; i++) {
            names[classes.length + i] = scripts[i].getName();
        }

        JOptionPane jop = new JOptionPane(I18n.text("Choose the plan template"), JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, templateIcon);

        jop.setSelectionValues(names);
        jop.setInitialSelectionValue(names[0]);

        JDialog dialog = jop.createDialog(console, I18n.text("Add plan template"));
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setVisible(true);

        Object option = jop.getInputValue();
        if (option == null)
            return;

        PlanType plan = null;

        if (option.toString().endsWith("js")) {
            ScriptedPlanTemplate planTemplate = new ScriptedPlanTemplate();
            String source = FileUtil.getFileAsString(scriptsDir + option);
            // NeptusLog.pub().info("<###> "+scriptsDir+option+":\n"+source);
            planTemplate.setSource(source);

            planTemplate.setMission(console.getMission());
            PropertiesEditor.editProperties(planTemplate, true);
            try {
                plan = planTemplate.generatePlan();
                // NeptusLog.pub().info("<###> "+plan.asXML());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            Class<?> c = classes[0];
            for (int i = 0; i < classes.length; i++) {
                if (PluginUtils.getPluginName(classes[i]).equals(option)) {
                    c = classes[i];
                    break;
                }
                else {
                    NeptusLog.pub().info("<###> " + option + " != " + PluginUtils.getPluginName(classes[i]));
                }
            }

            plan = AbstractPlanTemplate.addTemplateToMission(console, console.getMission(), c);
            if (plan == null)
                return;

            // This test is being done just to verify if some error is occurring at the plan generation
            try {
                plan.validatePlan();
            }
            catch (Exception e) {
                GuiUtils.errorMessage(console, e);
            }
        }
        if (plan == null)
            return;

        plan.setVehicle(choice);
        plan.setMissionType(console.getMission());
        final PlanType p = plan;

        new Thread() {
            @Override
            public void run() {
                console.getMission().addPlan(p);
                console.getMission().save(true);
                console.updateMissionListeners();
                GuiUtils.infoMessage(console, I18n.text("Plan template"),
                        I18n.textf("The plan %planid was added to this mission", p.getId()));
            };
        }.run();

    }
}
