/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * 11/08/2017
 */
package pt.lsts.neptus.plugins.atlas.elektronik;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.planning.PlanEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.atlas.elektronik.exporter.SeaCatMK1PlanExporter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "SeaCat-MK1 Plan Edition", icon = "images/planning/plan_editor.png", 
        author = "Paulo Dias", version = "1.0", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 100)
public class SeaCatMK1PlanEditor extends PlanEditor {

    @NeptusProperty(name = "Toolbar Location", userLevel = LEVEL.REGULAR)
    public ToolbarLocation toolbarLocation = ToolbarLocation.Right;

    @NeptusProperty(name = "Show Plan Simulation", userLevel = LEVEL.REGULAR)
    public boolean showSimulation;
    
    @NeptusProperty(name = "Show Depth Profile", userLevel = LEVEL.REGULAR)
    public boolean showDepth;

    @NeptusProperty(name = "Select Saved Plan on Console", userLevel = LEVEL.ADVANCED)
    public boolean selectSavedPlanOnConsole = false;;

    @NeptusProperty(name = "Close Editor on Save", userLevel = LEVEL.ADVANCED)
    public boolean closeEditorOnSave = true;
    
    private String lastExportFolder = ConfigFetch.getConfigFile();
    private JButton exportButton = null;
    private SeaCatMK1PlanExporter planExporter = new SeaCatMK1PlanExporter();
    
    /**
     * @param console
     */
    public SeaCatMK1PlanEditor(ConsoleLayout console) {
        super(console);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.plugins.planning.PlanEditor#setPlanChanged(boolean)
     */
    @Override
    protected void setPlanChanged(boolean planChanged) {
        super.setPlanChanged(planChanged);
        getExportButton().setEnabled(!planChanged);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.plugins.planning.PlanEditor#getSidePanel()
     */
    @Override
    protected JPanel getSidePanel() {
        if (sidePanel != null)
            return sidePanel;
        
        JPanel retPanel = super.getSidePanel();
        
        ArrayList<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < controls.getComponentCount(); i++) {
            Component cp = controls.getComponent(i);
            if (cp instanceof JButton) {
                JButton bt = ((JButton) cp);
                if (bt.getAction() == redoAction)
                    toRemove.add(0, i);
            }
        }
        toRemove.forEach(n -> controls.remove(n));
        
        exportButton = getExportButton();
        controls.add(exportButton);
        
        return retPanel;
    }
    
    protected JButton getExportButton() {
        if (exportButton == null) {
            exportButton = new JButton(getExportAction());
        }
        
        return exportButton;
    }
    
    protected AbstractAction getExportAction() {
        return new AbstractAction(I18n.text("Export"), ImageUtils.getScaledIcon("images/planning/fileexport.png", 16, 16)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (plan == null) {
                    GuiUtils.infoMessage(getConsole(), planExporter.getExporterName(), I18n.text("Nothing to export"));
                    return;
                }
//                PlanType plan = plan;
                
                JFileChooser chooser = GuiUtils.getFileChooser(lastExportFolder,
                        I18n.textf("%exporterName files", planExporter.getExporterName()),
                        planExporter.validExtensions());
                chooser.setSelectedFile(new File(plan.getDisplayName()));
                chooser.setDialogTitle(I18n.text("Select destination file"));
                JComponent accessory = planExporter.createFileChooserAccessory(chooser);
                if (accessory != null)
                    chooser.setAccessory(accessory);
                int op = chooser.showSaveDialog(getConsole());
                if (op != JFileChooser.APPROVE_OPTION)
                    return;
                try {
                    File dst = chooser.getSelectedFile();
                    String[] exts = planExporter.validExtensions();
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

                    ProgressMonitor pmonitor = new ProgressMonitor(getConsole(), planExporter.getExporterName(),
                            I18n.text("Exporting"), 0, 100);
                    planExporter.exportToFile(plan, dst, pmonitor);
                    lastExportFolder = dst.getAbsolutePath();
                    GuiUtils.infoMessage(
                            getConsole(),
                            planExporter.getExporterName(),
                            pmonitor.isCanceled() ? I18n.text("Export cancelled") : I18n
                                    .text("Export done"));
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }                        
            }
        };
    }
}
