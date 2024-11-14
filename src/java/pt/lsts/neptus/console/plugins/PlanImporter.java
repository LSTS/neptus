/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 7/10/2024
 */
package pt.lsts.neptus.console.plugins;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.IPlanFileExporter;
import pt.lsts.neptus.types.mission.plan.IPlanFileImporter;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pdias
 *
 */
@PluginDescription
public class PlanImporter extends ConsolePanel {
    private static final String TOOLS_MENU = I18n.text("Tools");
    private static final String IMPORT_PLAN_MENU = I18n.text("Import Plan");

    private String lastImportFolder = ConfigFetch.getConfigFile();

    /**
     * @param console
     */
    public PlanImporter(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void initSubPanel() {
        for (Class<? extends IPlanFileImporter> exporter : PluginsRepository.listExtensions(IPlanFileImporter.class).values()) {
            try {
                final IPlanFileImporter exp = exporter.getDeclaredConstructor().newInstance();
                addMenuItem(TOOLS_MENU + ">" + IMPORT_PLAN_MENU + ">" + exp.getImporterName(),
                        null, e -> {
                            JFileChooser chooser = GuiUtils.getFileChooser(lastImportFolder,
                                    I18n.textf("%importerName files", exp.getImporterName()),
                                    exp.validExtensions());
                            chooser.setDialogTitle(I18n.text("Select file"));
                            JComponent accessory = exp.createFileChooserAccessory(chooser);
                            if (accessory != null)
                                chooser.setAccessory(accessory);
                            int op = chooser.showSaveDialog(getConsole());
                            if (op != JFileChooser.APPROVE_OPTION)
                                return;

                            try {
                                File selectedFile = chooser.getSelectedFile();
                                if (selectedFile == null || !selectedFile.exists()) {
                                    JOptionPane.showMessageDialog(getConsole(),
                                            I18n.text("File does not exist!"),
                                            I18n.text("Import plan"), JOptionPane.ERROR_MESSAGE);
                                    return;
                                }

                                ProgressMonitor pmonitor = new ProgressMonitor(getConsole(), exp.getImporterName(),
                                        I18n.text("Importing"), 0, 100);
                                MissionType mission = getConsole().getMission();
                                List<PlanType> planList = exp.importFromFile(mission, selectedFile, pmonitor);

                                if (planList == null || planList.isEmpty()) {
                                    JOptionPane.showMessageDialog(getConsole(),
                                            I18n.text("No plan was imported!"),
                                            I18n.text("Import plan"), JOptionPane.ERROR_MESSAGE);
                                    return;
                                }

                                for (PlanType plan : planList) {
                                    getConsole().getMission().getIndividualPlansList().put(plan.getId(), plan);
                                    getConsole().getMission().save(true);
                                }
                                getConsole().post(Notification.success(I18n.text("Plan Import"),
                                        I18n.textf("Imported plan%s '%plan'", planList.size() > 1 ? "s" : "",
                                                planList.stream().map(PlanType::getId).collect(Collectors.joining(", ")))));
                                getConsole().updateMissionListeners();

                                lastImportFolder = selectedFile.getAbsolutePath();
                                GuiUtils.infoMessage(
                                        getConsole(),
                                        exp.getImporterName(),
                                        pmonitor.isCanceled() ? I18n.text("Import cancelled") : I18n
                                                .text("Import done"));
                            }
                            catch (Exception ex) {
                                GuiUtils.errorMessage(getConsole(), ex);
                            }
                        });
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }
}
