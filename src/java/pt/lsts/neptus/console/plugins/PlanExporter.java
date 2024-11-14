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
 * Author: zp
 * Jul 25, 2014
 */
package pt.lsts.neptus.console.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.types.mission.plan.IPlanFileExporter;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
@PluginDescription
public class PlanExporter extends ConsolePanel {
    private static final String TOOLS_MENU = I18n.text("Tools");
    private static final String ExPORT_PLAN_MENU = I18n.text("Export Plan");


    private String lastExportFolder = ConfigFetch.getConfigFile();
    
    /**
     * @param console
     */
    public PlanExporter(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {
        for (Class<? extends IPlanFileExporter> exporter : PluginsRepository.listExtensions(IPlanFileExporter.class).values()) {
            try {
                final IPlanFileExporter exp = exporter.getDeclaredConstructor().newInstance();
                addMenuItem(TOOLS_MENU + ">" + ExPORT_PLAN_MENU + ">" + exp.getExporterName(),
                        null, e -> {
                            if (getConsole().getPlan() == null) {
                                GuiUtils.infoMessage(getConsole(), exp.getExporterName(), I18n.text("Nothing to export"));
                                return;
                            }
                            PlanType plan = getConsole().getPlan();

                            JFileChooser chooser = GuiUtils.getFileChooser(lastExportFolder,
                                    I18n.textf("%exporterName files", exp.getExporterName()),
                                    exp.validExtensions());
                            chooser.setSelectedFile(new File(plan.getDisplayName()));
                            chooser.setDialogTitle(I18n.text("Select destination file"));
                            JComponent accessory = exp.createFileChooserAccessory(chooser);
                            if (accessory != null)
                                chooser.setAccessory(accessory);
                            int op = chooser.showSaveDialog(getConsole());
                            if (op != JFileChooser.APPROVE_OPTION)
                                return;
                            try {
                                File dst = chooser.getSelectedFile();
                                String[] exts = exp.validExtensions();
                                if (exts.length > 0 && FileUtil.getFileExtension(dst).isEmpty()) {
                                    dst = new File(dst.getAbsolutePath() + "." + exts[0]);
                                }
                                if (dst.exists()) {
                                    int resp = JOptionPane.showConfirmDialog(getConsole(),
                                            I18n.text("Do you want to overwrite the existing file?"),
                                            I18n.text("Export plan"), JOptionPane.YES_NO_CANCEL_OPTION);
                                    if (resp != JOptionPane.YES_OPTION) {
                                        return;
                                    }
                                }

                                ProgressMonitor pmonitor = new ProgressMonitor(getConsole(), exp.getExporterName(),
                                        I18n.text("Exporting"), 0, 100);
                                exp.exportToFile(plan, dst, pmonitor);
                                lastExportFolder = dst.getAbsolutePath();
                                GuiUtils.infoMessage(
                                        getConsole(),
                                        exp.getExporterName(),
                                        pmonitor.isCanceled() ? I18n.text("Export cancelled") : I18n
                                                .text("Export done"));
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
