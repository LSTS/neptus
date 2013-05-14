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
package pt.up.fe.dceg.neptus.console.plugins;

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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ContainerSubPanel;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelChangeEvent.SubPanelChangeAction;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.PluginsRepository;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;

import com.l2fprod.common.swing.StatusBar;
import com.sun.java.swing.plaf.windows.WindowsButtonUI;

/**
 * @author Hugo
 * 
 */
@Popup(pos = POSITION.TOP_LEFT, width = 500, height = 500, accelerator = 'P')
@PluginDescription(name = "Plugin Manager", icon = "images/buttons/events.png")
public class PluginManager extends SimpleSubPanel {

    private static final long serialVersionUID = 1L;
    private JPanel content;
    private JPanel statusPanel;
    private JTextArea description;
    private JList<String> activePluginsList;
    private JList<String> pluginsList;
    private Map<String, SubPanel> pluginsMap = new HashMap<String, SubPanel>();
    private String activeSelected = null;
    private String availableSelected = null;
    private ContainerSubPanel container;

    /**
     * @param console
     */
    public PluginManager(ConsoleLayout console) {
        super(console);
        this.setLayout(new BorderLayout());

        this.add(content = new JPanel(new MigLayout("gap 5px 0, ins 5px")), BorderLayout.CENTER);
        StatusBar statusBar = new StatusBar();
        statusBar.setZoneBorder(BorderFactory.createLineBorder(Color.GRAY));
        statusBar.setZones(new String[] { "zone" }, new Component[] { statusPanel = new JPanel(), },
                new String[] { "*" });
        JButton btn = new JButton("Reset");
        btn.setAction(new AbstractAction("Reset") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                content.removeAll();
                createComponents();
                content.revalidate();
                content.repaint();
            }
        });
        statusPanel.add(btn);
        this.add(statusBar, BorderLayout.SOUTH);
        this.createComponents();
    }

    private void createComponents() {
        JPanel activePluginsPanel = new JPanel(new MigLayout("ins 0"));
        JPanel availablePluginsPanel = new JPanel(new MigLayout("ins 0"));
        description =  new JTextArea();
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        
        description.setBorder(BorderFactory.createTitledBorder("Plugin Description"));
        content.add(availablePluginsPanel, "h 100%, w 50%");
        content.add(activePluginsPanel, "h 100%, w 50%, wrap");
        content.add(description, "w 100%, h 100px, span");
        
        // availablePluginsPanel components
        // label
        JLabel availablePluginsLabel = new JLabel("Available Plugins");
        availablePluginsPanel.add(availablePluginsLabel, "wrap");
        // buttons
        JButton btn = new JButton("Add");
        btn.setAction(new AbstractAction("Add") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if(availableSelected == null) return;
                SubPanel sp = PluginsRepository.getPanelPlugin(availableSelected, console);
                if(sp instanceof SimpleSubPanel){
                    container.addSubPanel(sp);
                    sp.init();
                    console.informSubPanelListener(sp, SubPanelChangeAction.ADDED);
                    refreshActivePlugins();
                    NeptusLog.pub().warn("Added new plugin: " + sp.getName() + " Class name : "+sp.getClass().getCanonicalName());
                }else{
                    NeptusLog.pub().warn("Plugin: " + sp.getName() + " Class name : "+sp.getClass().getCanonicalName());
                }
                
            }
        });
        btn.setUI(new WindowsButtonUI());
        availablePluginsPanel.add(btn, "wrap");

        // list
        JScrollPane listScroll = new JScrollPane();
        pluginsList = new JList<String>();
        String[] plugins = PluginsRepository.getPanelPlugins().keySet().toArray(new String[0]);
        Arrays.sort(plugins, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Collator collator = Collator.getInstance(Locale.US);
                return collator.compare(PluginUtils.i18nTranslate(o1), PluginUtils.i18nTranslate(o2));
            }
        });
        pluginsList.setListData(plugins);
        pluginsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = pluginsList.locationToIndex(e.getPoint());
                    if (index > -1) {
//                        String pluginName = (String) pluginsList.getSelectedValue();
//                        PropertiesEditor.editProperties(pluginsMap.get(pluginName), true);
                    }
                }
                if (e.getClickCount() == 1) {
                    int index = pluginsList.locationToIndex(e.getPoint());
                    if (index > -1) {
                        String pluginName = (String) pluginsList.getSelectedValue();
                        availableSelected = pluginName;
                        Class<?> clazz = PluginsRepository.getPanelPlugins().get(pluginName);
                        description.setText(PluginUtils.getPluginDescription(clazz));
                    }
                }
            }
        });
        listScroll.setViewportView(pluginsList);
        availablePluginsPanel.add(listScroll, "h 100%, w 100%");

        // activePluginsPanel components
        // label
        JLabel activePluginsLabel = new JLabel("Active Plugins");
        activePluginsPanel.add(activePluginsLabel, "wrap");
        // buttons
        JButton btn1 = new JButton("Remove");
        btn1.setAction(new AbstractAction("Remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if(activeSelected == null) return;
                SubPanel sp = pluginsMap.get(activeSelected);
                if(sp instanceof SimpleSubPanel){
                    container.removeSubPanel(sp);
                    console.informSubPanelListener(sp, SubPanelChangeAction.REMOVED);
                    refreshActivePlugins();
                    NeptusLog.pub().warn("Removed plugin: " + sp.getName() + " Class name : "+sp.getClass().getCanonicalName());
                }else{
                    NeptusLog.pub().warn("Plugin: " + sp.getName() + " Class name : "+sp.getClass().getCanonicalName());
                }
                
            }
        });
        btn1.setUI(new WindowsButtonUI());
        activePluginsPanel.add(btn1, "wrap");

        // list
        JScrollPane activePluginsScrollPane = new JScrollPane();
        activePluginsList = new JList<String>();
        this.refreshActivePlugins();
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
        activePluginsScrollPane.setViewportView(activePluginsList);
        activePluginsPanel.add(activePluginsScrollPane, "h 100%, w 100%");
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
                        Collator collator = Collator.getInstance(Locale.US);
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
        // TODO Auto-generated method stub

    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
