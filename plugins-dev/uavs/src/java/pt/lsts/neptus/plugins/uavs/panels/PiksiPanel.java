/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jfortuna
 * 12/2/2015
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.RemoteActions;
import pt.lsts.imc.GpsFixRtk;
import pt.lsts.imc.GpsFixRtk.TYPE;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;

import com.google.common.eventbus.Subscribe;

/**
 * Shows buttons to do various calibrations for Piksi.
 *
 * @author krisklau
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Piksi Panel", author = "krisklau", version = "0.1", category = CATEGORY.INTERFACE)
public class PiksiPanel extends ConsolePanel implements MainVehicleChangeListener {
    
    public enum DisplayType {
       DISTANCE, NED
    }
    
    @NeptusProperty(name = "Display NED coordinates", description = "Set true to display NED xyz coordinates. Set to false to give distance.", userLevel = LEVEL.REGULAR)
    public DisplayType displayType = DisplayType.DISTANCE;

    // GUI
    private JPanel titlePanel = null;
    private JPanel buttonPanel = null;
    private JPanel statusPanel = null;
    private JScrollPane scrollStatusPane = null;

    // Piksi Status
    private String fixStatus = "";
    private double[] nedPos = new double[3];

    // Container class for received updates
    class GpsFixRtkContainer {
        public String vehicleName = "";
        public String displayName = "";
        GpsFixRtk lastMessage;
        JLabel text = null;
    }

    // Table containing ID of IMC systems and an GpsFixRtk container
    private LinkedHashMap<Integer, GpsFixRtkContainer> vehicleFixes;

    /**
     * @param console
     */
    public PiksiPanel(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    @Override
    public void initSubPanel() {
        titlePanelSetup();
        buttonPanelSetup();
        statusPanelSetup();

        // panel general layout setup
        this.setLayout(new MigLayout("gap 0 0, ins 0"));
        this.add(titlePanel, "w 100%, h 20%, wrap");
        this.add(buttonPanel, "w 100%, h 20%, wrap");
        this.add(scrollStatusPane, "w 100%, h 60%, wrap");

        this.vehicleFixes = new LinkedHashMap<Integer, GpsFixRtkContainer>();
    }

    /*
     * (non-Javadoc)
     *
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }

    // Listener
    @Subscribe
    public void on(GpsFixRtk msg) {
        // Check if it is a piksi
        String entName = EntitiesResolver.resolveName(msg.getSourceName(), (int) msg.getSrcEnt());
        
        // Check if the entity contains the "Piksi" label
        if (entName != null && entName.toLowerCase().indexOf("piksi") == -1) {
            // Nope. 
            return;
        }

        // If new, add to table
        if (!vehicleFixes.containsKey(msg.getSrc())) {
            GpsFixRtkContainer container = new GpsFixRtkContainer();
            container.lastMessage = msg;

            container.text = new JLabel();
            container.text.setHorizontalAlignment(SwingConstants.LEFT);

            container.vehicleName = msg.getSourceName();

            container.vehicleName = container.vehicleName.replace("ntnu-",  "");
            container.vehicleName = container.vehicleName.replace("testbed", "test");

            vehicleFixes.put(msg.getSrc(), container);

            // Remove all, re-sort.
            statusPanel.removeAll();

            // Loop through to sort and stop last wrap
            SortedSet<Integer> keys = new TreeSet<Integer>(vehicleFixes.keySet());
            for (Integer key : keys) {
                GpsFixRtkContainer c = vehicleFixes.get(key);

                if (key != keys.last())
                    statusPanel.add(c.text, "w 100%, h 20px, wrap");
                else
                    statusPanel.add(c.text, "w 100%, h 20px");
            }

            statusPanel.revalidate();
        }

        // Update text
        GpsFixRtkContainer container = vehicleFixes.get(msg.getSrc());

        // Sanity check, should not happen
        if (container == null) {
            System.out.println("Warning: Key not found. Should not happen. ");
            return;
        }

        container.lastMessage = msg;

        fixStatus = msg.getTypeStr();
        nedPos[0] = msg.getN();
        nedPos[1] = msg.getE();
        nedPos[2] = msg.getD();

        String statusText;
        if (msg.getType() == TYPE.FIXED) {
            if (displayType == DisplayType.NED) {
                statusText = container.vehicleName + ": "
                        + String.format("FIX: %.2f, %.2f, %.2f", nedPos[0], nedPos[1], nedPos[2]);
            }
            else {
                statusText = container.vehicleName + ": " + String.format("FIX: %.3fm",
                        Math.sqrt(Math.pow(nedPos[0], 2.0) + Math.pow(nedPos[1], 2.0) + Math.pow(nedPos[2], 2.0)));
            }

            container.text.setBackground(new Color(81, 179, 54));

            // If IAR number is higher than 1, change the color to Orange
            if (msg.getIarHyp() > 1) {
                container.text.setBackground(new Color(205, 179, 49));
            }

            container.text.setOpaque(true);
        }
        else if(msg.getType() == TYPE.FLOAT) {
            statusText = container.vehicleName + ": " + "Float" + ", IAR: " + String.format("%d", msg.getIarHyp());
            container.text.setBackground(new Color(205, 179, 49));
            container.text.setOpaque(true);
        }
        else if(msg.getType() == TYPE.NONE) {
            statusText = container.vehicleName + ": " + fixStatus;
            container.text.setBackground(new Color(161, 79, 23));
            container.text.setOpaque(true);
        }
        else {
            statusText = container.vehicleName + ": " + fixStatus;
            container.text.setOpaque(false);
        }

        container.text.setText(statusText);
    }

    private void statusPanelSetup() {
        statusPanel = new JPanel(new MigLayout("gap 0 0, ins 0"));

        scrollStatusPane = new JScrollPane(statusPanel);
        scrollStatusPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollStatusPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    private void titlePanelSetup() {
        titlePanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        JLabel titleLabel = new JLabel(I18n.text("Piksi Interface"), SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9));
        titlePanel.add(titleLabel, "w 100%, h 100%");
    }

    private void buttonPanelSetup() {
        buttonPanel = new JPanel(new MigLayout("gap 0 0, ins 0"));

        // Calibrate
        JButton initBaselineButton = new JButton(I18n.text("Init Baseline"));
        initBaselineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteActions action = new RemoteActions();
                action.setActions("piksiInitZeroBaseline=1");
                send(action);
            }
        });
        buttonPanel.add(initBaselineButton, "w 34%, h 100%");

        // Arm
        JButton resetFilterButton = new JButton(I18n.text("Reset Filter"));
        resetFilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteActions action = new RemoteActions();
                action.setActions("piksiResetFilters=1");
                send(action);
            }
        });
        buttonPanel.add(resetFilterButton, "w 33%, h 100%");

        // Disarm
        JButton resetIARButton = new JButton(I18n.text("Reset IAR"));
        resetIARButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteActions action = new RemoteActions();
                action.setActions("piksiResetIARs=1");
                send(action);
            }
        });
        buttonPanel.add(resetIARButton, "w 33%, h 100%");
    }
}
