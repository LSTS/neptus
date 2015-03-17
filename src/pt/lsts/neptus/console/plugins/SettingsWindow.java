/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Margarida Faria
 * Jan 10, 2013
 */
package pt.lsts.neptus.console.plugins;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Window to encapsulate settings panel.<br>
 * Contains all the buttons.
 * 
 * @author Margarida Faria
 * 
 */
@Popup(icon = "images/menus/settings.png", name = "Console Settings", pos = POSITION.CENTER, accelerator = KeyEvent.VK_F3, height = 400, width = 600)
public class SettingsWindow extends ConsolePanel {

    private static final long serialVersionUID = 1L;
    private FunctionalitiesSettings settingsPanel;
    private final Vector<PropertiesProvider> subPanels = new Vector<>();
    private JCheckBox checkLvl;

    /**
     * Saves available plugins for later use in settings panel.
     * 
     * @param console
     * @param subPanels
     */
    public SettingsWindow(ConsoleLayout console) {
        super(console);

        this.removeAll();

        List<PropertiesProvider> consolePlugins = console.getAllPropertiesProviders();
        for (PropertiesProvider propertiesProvider : consolePlugins) {
            if (propertiesProvider == null)
                continue;
            if (propertiesProvider instanceof ContainerSubPanel) {
                List<ConsolePanel> containerPlugins = ((ContainerSubPanel) propertiesProvider).getSubPanels();
                for (ConsolePanel containerPlugin : containerPlugins) {
                    if (containerPlugin instanceof PropertiesProvider)
                        subPanels.add((PropertiesProvider) containerPlugin);
                }
            }
            else {
                subPanels.add(propertiesProvider);
            }
        }
    }

    private void addButtons() {
        checkLvl = new JCheckBox(I18n.text("Advanced"));
        checkLvl.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settingsPanel.updateForNewPermission();
            }
        });
        checkLvl.setSelected(false);
        // direct keyboard inputs to tree after this is pressed
        checkLvl.setFocusable(false);

        JButton save = new JButton(I18n.text("Save"));
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                settingsPanel.saveChanges();
                checkLvl.setSelected(false);
                settingsPanel.reset();
            }
        });

        JButton cancel = new JButton(I18n.text("Cancel"));
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                checkLvl.setSelected(false);
                settingsPanel.reset();
            }
        });

        GuiUtils.reactEnterKeyPress(save);
        GuiUtils.reactEscapeKeyPress(cancel);

        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new MigLayout("rtl"));
        buttonContainer.add(cancel, "sg buttonOC");
        buttonContainer.add(save, "sg buttonOC");
        buttonContainer.add(checkLvl);
        this.add(buttonContainer, "grow");
    }

    @Override
    public void initSubPanel() {
        this.setLayout(new MigLayout("insets 0"));
        this.removeAll();
        this.settingsPanel = new FunctionalitiesSettings(ConfigFetch.getDistributionType().equals(
                DistributionEnum.CLIENT), subPanels);
        this.add(settingsPanel, "w 100%!, h 100%, wrap");
        addButtons();
        // this is done after the level normal/advanced is set by creating the checkbox
        settingsPanel.reset();
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
    }

    @Override
    public void cleanSubPanel() {
        settingsPanel = null;
    }
}
