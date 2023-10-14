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
 * Author: Sérgio Ferreira
 * Apr 24, 2014
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.DesiredPath;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IndicatedSpeed;
import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.TrueSpeed;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.util.ImageUtils;

/**
 * Neptus panel designed to show indicated speed and ground speed on the same frame. It allows for the setup of limits
 * for maximum and minimum acceptable velocity.
 *
 * @author canastaman
 * @author jfortuna
 * @version 3.1
 * @category UavPanel
 *
 */
@PluginDescription(name = "Speed Indicator Panel", icon = "pt/lsts/neptus/plugins/uavs/speed.png", author = "canasta", version = "3.1", category = CATEGORY.INTERFACE)
public class SpeedIndicatorPanel extends ConsolePanel implements MainVehicleChangeListener {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Minimum Speed", description = "Speed below which the vehicle enters VStall", userLevel = LEVEL.REGULAR)
    public SpeedType minSpeed = new SpeedType(12.0, Units.MPS);

    @NeptusProperty(name = "Maximum Speed", description = "Speed above which it's undesirable to fly", userLevel = LEVEL.REGULAR)
    public SpeedType maxSpeed = new SpeedType(25.0, Units.MPS);

    // Illustrative icons to differentiate speeds
    private ImageIcon ICON_TSPEED;
    private ImageIcon ICON_GSPEED;
    private ImageIcon ICON_ASPEED;

    // indicates if the UAV as changed plan and needs to update it's command speed value
    private boolean samePlan = false;

    private String currentPlan = null;
    private String currentManeuver = null;

    private PlanSpecification planSpec = null;

    // various speeds
    private SpeedType aSpeed = new SpeedType(0, Units.MPS);
    private SpeedType gSpeed = new SpeedType(0, Units.MPS);
    private SpeedType tSpeed = new SpeedType(0, Units.MPS);

    // sub panels used to better accommodate the information through the use of the layout manager
    private JPanel titlePanel = null;
    private JPanel speedPanel = null;
    private JPanel speedIconPanel = null;
    private JPanel speedGraphPanel = null;

    private JProgressBar aSpeedBar = null;
    private JProgressBar gSpeedBar = null;

    private JLabel aSpeedIcon = null;
    private JLabel gSpeedIcon = null;
    private JLabel tSpeedIcon = null;
    private JLabel tSpeedValue = null;

    public SpeedIndicatorPanel(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    // Listeners
    @Subscribe
    public void on(TrueSpeed msg) {
        if (msg.getSourceName().equals(getConsole().getMainSystem())) {
            gSpeed.setMPS(msg.getValue());

            // speeds updated
            speedLabelUpdate();
        }
    }

    @Subscribe
    public void on(IndicatedSpeed msg) {
        if (msg.getSourceName().equals(getConsole().getMainSystem())) {
            aSpeed.setMPS(msg.getValue());

            // speeds updated
            speedLabelUpdate();
        }
    }

    @Subscribe
    public void on(PlanControlState msg) {
        if (msg.getSourceName().equals(getConsole().getMainSystem())) {
            // if the vehicle is currently executing a plan we ask for that plan
            // and then identify what maneuver is being executed
            if (msg.getAsNumber("state").longValue() == STATE.EXECUTING.value()) {

                if (!msg.getAsString("plan_id").equals(currentPlan))
                    samePlan = false;

                currentPlan = msg.getAsString("plan_id");
                currentManeuver = msg.getAsString("man_id");

                if (planSpec != null && samePlan) {
                    for (PlanManeuver planMan : planSpec.getManeuvers()) {
                        if (planMan.getManeuverId().equals(currentManeuver)) {
                            Maneuver man = planMan.getData();
                            SpeedType st = SpeedType.parseImcSpeed(man, "speed", "speed_units");
                            if (st.getValue() >= 0) {
                                tSpeed.set(st);
                                // plan updated
                                speedLabelUpdate();
                            }
                        }
                    }
                }

                if (!samePlan) {
                    IMCMessage planControlMessage = IMCDefinition.getInstance().create("PlanControl");
                    planControlMessage.setValue("type", 0);
                    planControlMessage.setValue("op", "GET");
                    planControlMessage.setValue("request_id", IMCSendMessageUtils.getNextRequestId());

                    IMCSendMessageUtils.sendMessage(planControlMessage,
                            I18n.text("Error requesting plan specificaion"), true, getConsole().getMainSystem());
                }
            }
            else {
                samePlan = false;
            }
        }
    }

    @Subscribe
    public void on(DesiredPath msg) {
        if (msg.getSourceName().equals(getConsole().getMainSystem())) {
            tSpeed.set(SpeedType.parseImcSpeed(msg, "speed", "speed_units"));
            speedLabelUpdate();
        }
    }

    @Subscribe
    public void on(PlanControl msg) {
        if (msg.getSourceName().equals(getConsole().getMainSystem()) && !samePlan) {
            if (msg.getMessage("arg").getAbbrev().equals("PlanSpecification")) {
                planSpec = (PlanSpecification) msg.getMessage("arg");
                samePlan = true;
            }
        }
    }

    @Override
    public void initSubPanel() {
        ICON_TSPEED = ImageUtils.createScaleImageIcon("pt/lsts/neptus/plugins/uavs/icons/target.png",
                (int) (this.getHeight() * 0.2), (int) (this.getHeight() * 0.2));
        ICON_GSPEED = ImageUtils.createScaleImageIcon("pt/lsts/neptus/plugins/uavs/icons/ground.png",
                (int) (this.getHeight() * 0.2), (int) (this.getHeight() * 0.2));
        ICON_ASPEED = ImageUtils.createScaleImageIcon("pt/lsts/neptus/plugins/uavs/icons/air.png",
                (int) (this.getHeight() * 0.2), (int) (this.getHeight() * 0.2));

        titlePanelSetup();
        speedPanelSetup();

        // panel general layout setup
        this.setLayout(new MigLayout("gap 0 0, ins 0"));
        this.add(titlePanel, "w 100%, h 15%, wrap");
        this.add(speedPanel, "w 100%, h 85%, wrap");
    }

    private void titlePanelSetup() {
        titlePanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        JLabel titleLabel = new JLabel(I18n.text("Speed Indicator"), SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9));
        titlePanel.add(titleLabel, "w 100%, h 100%");
    }

    private void speedPanelSetup() {
        speedPanel = new JPanel(new MigLayout("gap 0 0, ins 0"));

        speedIconPanel = new JPanel(new MigLayout("ins 0"));
        tSpeedIcon = new JLabel(ICON_TSPEED);
        aSpeedIcon = new JLabel(ICON_ASPEED);
        gSpeedIcon = new JLabel(ICON_GSPEED);
        speedIconPanel.add(tSpeedIcon, "w 100%, h 34%, wrap");
        speedIconPanel.add(aSpeedIcon, "w 100%, h 33%, wrap");
        speedIconPanel.add(gSpeedIcon, "w 100%, h 33%");

        speedGraphPanel = new JPanel(new MigLayout("ins 0, rtl"));

        tSpeedValue = new JLabel(tSpeed.toString(), SwingConstants.CENTER);

        aSpeedBar = new JProgressBar(0, (int) ((maxSpeed.getMPS() + 10) * 10));
        aSpeedBar.setForeground(Color.cyan.darker());
        aSpeedBar.setBorderPainted(false);

        gSpeedBar = new JProgressBar(0, (int) ((maxSpeed.getMPS() + 10) * 10));
        gSpeedBar.setForeground(Color.green.darker());
        gSpeedBar.setBorderPainted(false);

        speedGraphPanel.add(tSpeedValue, "w 100%, h 34%, wrap");
        speedGraphPanel.add(aSpeedBar, "w 100%, h 33%, wrap");
        speedGraphPanel.add(gSpeedBar, "w 100%, h 33%");

        speedLabelUpdate();

        speedPanel.add(speedIconPanel, "w 20%, h 100%");
        speedPanel.add(speedGraphPanel, "w 80%, h 100%");
    }

    private void speedLabelUpdate() {
        aSpeedBar.setValue((int) (aSpeed.getMPS() * 10));
        if (aSpeed.getMPS() < minSpeed.getMPS()) {
            aSpeedBar.setForeground(Color.red.darker());
        }
        else {
            aSpeedBar.setForeground(Color.cyan.darker());
        }
        aSpeedBar.setString(aSpeed.toStringAsDefaultUnits());
        aSpeedBar.setStringPainted(true);
        gSpeedBar.setValue((int) (gSpeed.getMPS() * 10));
        gSpeedBar.setString(gSpeed.toStringAsDefaultUnits());
        gSpeedBar.setStringPainted(true);

        tSpeedValue.setText(tSpeed.toString());

        revalidate();
    }

    /*
     * (non-Javadoc)
     *
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }
}
