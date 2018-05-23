/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 23 May 2018
 */
package pt.lsts.neptus.plugins.telemetrycontrol;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ImageUtils;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.HashSet;


@Popup(width = 200, height = 150)
@PluginDescription(name = "Telemetry Control", author = "Tiago Sá Marques", description = "Telemetry control panel")
public class TelemetryControlPanel extends ConsolePanel implements PlanChangeListener {
    private final ImageIcon ICON_UP = ImageUtils.getIcon("images/planning/up.png");
    private final ImageIcon ICON_START = ImageUtils.getIcon("images/planning/start.png");
    private final ImageIcon ICON_STOP = ImageUtils.getIcon("images/planning/stop.png");

    private final Color COLOR_GREEN = new Color(0, 200, 125);
    private final Color COLOR_RED = Color.RED;

    private ToolbarButton sendPlan;
    private ToolbarButton startPlan;
    private ToolbarButton stopPlan;

    private final JToggleButton toggleTelemetry = new JToggleButton("OFF");
    private final JLabel mantasLabel = new JLabel("Manta");
    private final JLabel systemLabel = new JLabel("System");
    private final JComboBox mantasList = new JComboBox(new String[]{"1"});
    private final JComboBox systemsList = new JComboBox(new String[]{"1"});

    private AbstractAction sendPlanAction;
    private AbstractAction startPlanAction;
    private AbstractAction stopPlanAction;

    private final HashSet<String> mantas = new HashSet<>();
    private final HashSet<String> systems = new HashSet<>();

    private PlanType currSelectedPlan = null;

    public TelemetryControlPanel(ConsoleLayout console) {
        super(console);
        buildPlanel();
    }

    private void buildPlanel() {
        setLayout(new MigLayout());

        setupButtons();

        this.add(mantasLabel, "grow");
        this.add(systemLabel, "grow,wrap");
        this.add(mantasList, "grow");
        this.add(systemsList, "grow");
        this.add(toggleTelemetry, "grow,wrap");
        this.add(sendPlan);
        this.add(startPlan);
        this.add(stopPlan);
        this.setVisibility(true);

        toogleTelemetry();
        toogleControolButtons();
    }

    private void toogleControolButtons() {
        boolean enabled = false;
        if (currSelectedPlan != null)
            enabled = true;

        sendPlan.setEnabled(enabled);
        startPlan.setEnabled(enabled);
        stopPlan.setEnabled(enabled);
    }

    private void setupButtons() {
        sendPlanAction = new AbstractAction("Send Plan", ICON_UP) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        };

        startPlanAction = new AbstractAction("Start Plan", ICON_START) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        };

        stopPlanAction = new AbstractAction("Stop plan", ICON_STOP) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        };

        sendPlan = new ToolbarButton(sendPlanAction);
        startPlan = new ToolbarButton(startPlanAction);
        stopPlan = new ToolbarButton(stopPlanAction);

        // wtf java
        UIManager.put("ToggleButton.select", new ColorUIResource( COLOR_GREEN ));
        toggleTelemetry.setSelected(false);
        toggleTelemetry.addItemListener(itemEvent -> toogleTelemetry());
    }

    private void toogleTelemetry() {
        if (toggleTelemetry.isSelected())
            toggleTelemetry.setText("ON");
        else {
            toggleTelemetry.setBackground(COLOR_RED);
            toggleTelemetry.setText("OFF");
        }
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public void planChange(PlanType plan) {
        currSelectedPlan = plan;
        toogleControolButtons();
    }
}
