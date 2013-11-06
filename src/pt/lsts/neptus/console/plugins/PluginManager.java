/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Hugo
 * 14 de Mai de 2013
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.console.SubPanel;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent.SubPanelChangeAction;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.SimpleSubPanel;

import com.l2fprod.common.swing.StatusBar;
import com.sun.java.swing.plaf.windows.WindowsButtonUI;

/**
 * @author Hugo
 * 
 */
@Popup(name = "Plugin Manager", icon = "images/buttons/events.png",  pos = POSITION.CENTER, width = 500, height = 500, accelerator = 'P')
//@PluginDescription(name = "Plugin Manager", icon = "images/buttons/events.png")
public class PluginManager extends SimpleSubPanel {

    private static final long serialVersionUID = 1L;
    private JPanel content;
    @SuppressWarnings("unused")
    private JPanel statusPanel;
    private JTextArea description;
    private JList<String> activePluginsList;
    private JList<String> availablePluginsList;
    private Map<String, SubPanel> pluginsMap = new HashMap<String, SubPanel>();
    private String activeSelected = null;
    private String availableSelected = null;
    private ContainerSubPanel container;
    private JButton btnAdd;
    private JButton btnRemove;
    private JButton btnSettings;

    /**
     * Constructor
     * 
     * @param console
     */
    public PluginManager(ConsoleLayout console) {
        super(console);
        this.setLayout(new BorderLayout());
        // Content Panel
        this.add(content = new JPanel(new MigLayout("gap 5px 0, ins 5px")), BorderLayout.CENTER);
        // Status Bar
        StatusBar statusBar = new StatusBar();
        statusBar.setZoneBorder(BorderFactory.createLineBorder(Color.GRAY));
        statusBar.setZones(new String[] { "zone" }, new Component[] { statusPanel = new JPanel(), },
                new String[] { "*" });
        this.add(statusBar, BorderLayout.SOUTH);
        // Add components to the content panel
        this.createComponents();
        ///this.refreshActivePlugins();
        this.createActions();
        this.createListeners();
    }

    /**
     * Create components
     */
    private void createComponents() {
        JPanel activePluginsPanel = new JPanel(new MigLayout("ins 0"));
        JPanel availablePluginsPanel = new JPanel(new MigLayout("ins 0"));
        description = new JTextArea();
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        description.setBorder(BorderFactory.createTitledBorder("Plugin Description"));
        content.add(availablePluginsPanel, "h 100%, w 50%");
        content.add(activePluginsPanel, "h 100%, w 50%, wrap");
        content.add(description, "w 100%, h 150px, span");

        // availablePluginsPanel components
        // label
        JLabel availablePluginsLabel = new JLabel("Available");
        availablePluginsPanel.add(availablePluginsLabel, "h 7%");
        // buttons
        btnAdd = new JButton("Add");
        btnAdd.setUI(new WindowsButtonUI());
        availablePluginsPanel.add(btnAdd, "h 7%, wrap");

        // list
        JScrollPane listScroll = new JScrollPane();
        availablePluginsList = new JList<String>();
        availablePluginsList.setListData(this.getAvailablePlugins());

        listScroll.setViewportView(availablePluginsList);
        availablePluginsPanel.add(listScroll, "h 93%, w 100%, span");

        // activePluginsPanel components
        // label
        JLabel activePluginsLabel = new JLabel("Active");
        activePluginsPanel.add(activePluginsLabel, "h 7%");
        // buttons
        btnRemove = new JButton("Remove");
        btnRemove.setUI(new WindowsButtonUI());
        activePluginsPanel.add(btnRemove, "h 7%");

        btnSettings = new JButton("Settings");
        btnSettings.setUI(new WindowsButtonUI());
        activePluginsPanel.add(btnSettings, "h 7%, wrap");

        // list
        JScrollPane activePluginsScrollPane = new JScrollPane();
        activePluginsList = new JList<String>();
        activePluginsScrollPane.setViewportView(activePluginsList);
        activePluginsPanel.add(activePluginsScrollPane, "h 93%, w 100%, span");
    }

    private void createActions() {
        // Add button action
        btnAdd.setAction(new AbstractAction("Add") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (availableSelected == null)
                    return;
                SubPanel sp = PluginsRepository.getPanelPlugin(availableSelected, console);
                if (sp instanceof SimpleSubPanel) {
                    container.addSubPanel(sp);
                    sp.init();
                    console.informSubPanelListener(sp, SubPanelChangeAction.ADDED);
                    refreshActivePlugins();
                    NeptusLog.pub().warn(
                            "Added new plugin: " + sp.getName() + " Class name : " + sp.getClass().getCanonicalName());
                }
                else {
                    NeptusLog.pub().warn(
                            "Plugin: " + sp.getName() + " Class name : " + sp.getClass().getCanonicalName());
                }

            }
        });

        // Remove button action
        btnRemove.setAction(new AbstractAction("Remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (activeSelected == null)
                    return;
                SubPanel sp = pluginsMap.get(activeSelected);
                if (sp instanceof SimpleSubPanel) {
                    container.removeSubPanel(sp);
                    console.informSubPanelListener(sp, SubPanelChangeAction.REMOVED);
                    refreshActivePlugins();
                    NeptusLog.pub().warn(
                            "Removed plugin: " + sp.getName() + " Class name : " + sp.getClass().getCanonicalName());
                }
                else {
                    NeptusLog.pub().warn(
                            "Plugin: " + sp.getName() + " Class name : " + sp.getClass().getCanonicalName());
                }

            }
        });
        btnRemove.getAction().putValue(Action.SHORT_DESCRIPTION, "Remove selected plugin");

        btnSettings.setAction(new AbstractAction("Settings") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (activeSelected == null)
                    return;
                PropertiesEditor.editProperties(pluginsMap.get(activeSelected), true);
            }
        });
        btnSettings.getAction().putValue(Action.SHORT_DESCRIPTION, "Open settings dialog for the selected plugin");
    }

    private void createListeners() {
        activePluginsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = activePluginsList.locationToIndex(e.getPoint());
                    if (index > -1) {
                        String pluginName = (String) activePluginsList.getSelectedValue();
                        PropertiesEditor.editProperties(pluginsMap.get(pluginName), true);
                    }
                }
                if (e.getClickCount() == 1) {
                    int index = activePluginsList.locationToIndex(e.getPoint());
                    if (index > -1) {
                        String pluginName = (String) activePluginsList.getSelectedValue();
                        activeSelected = pluginName;
                        description.setText(pluginsMap.get(pluginName).getDescription());
                    }
                }
            }
        });
        availablePluginsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = availablePluginsList.locationToIndex(e.getPoint());
                    if (index > -1) {
                        String pluginName = (String) availablePluginsList.getSelectedValue();
                        availableSelected = pluginName;
                        Class<?> clazz = PluginsRepository.getPanelPlugins().get(pluginName);
                        description.setText(PluginUtils.getPluginDescription(clazz));
                    }
                }
            }
        });
    }

    private String[] getAvailablePlugins() {
        String[] plugins = PluginsRepository.getPanelPlugins().keySet().toArray(new String[0]);
        // Natural sort
        Arrays.sort(plugins, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Collator collator = Collator.getInstance(Locale.US);
                return collator.compare(PluginUtils.i18nTranslate(o1), PluginUtils.i18nTranslate(o2));
            }
        });
        return plugins;
    }

    private void refreshActivePlugins() {
        pluginsMap.clear();
        List<String> names = new ArrayList<>();
        for (SubPanel panel : console.getSubPanels()) {
            names.add(panel.getName());
            pluginsMap.put(panel.getName(), panel);
            if (panel instanceof ContainerSubPanel) {
                container = (ContainerSubPanel) panel;
                List<SubPanel> panels = ((ContainerSubPanel) panel).getSubPanels();
                
                Collections.sort(panels, new Comparator<SubPanel>() {
                    @Override
                    public int compare(SubPanel o1, SubPanel o2) {
                        final Collator collator = Collator.getInstance(Locale.US);
                        return collator.compare(PluginUtils.i18nTranslate(o1.getName()),
                                PluginUtils.i18nTranslate(o2.getName()));
                    }
                });
               
                for (SubPanel panel2 : panels) {
                    names.add(panel2.getName());
                    pluginsMap.put(panel2.getName(), panel2);
                }
            }
        }
       
        activePluginsList.setListData(names.toArray(new String[0]));
    }

    @Override
    public void initSubPanel() {
        this.refreshActivePlugins();

    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
