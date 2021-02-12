/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 11, 2015
 */
package pt.lsts.neptus.plugins.followref;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowReference;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.Reference;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name = "LowLevelControl")
@Popup(name = "Low Level Controller", width = 300, height = 110, pos = POSITION.TOP_LEFT)
public class LowLevelControl extends ConsolePanel {

    @NeptusProperty(userLevel = LEVEL.REGULAR, description = "Distance, in meters between vehicle's and generated desired waypoint")
    public int carrotDistance = 500;

    @NeptusProperty(userLevel = LEVEL.REGULAR, description = "Offset, in degrees to add to all desired headings")
    public int headingOffset = 0;

    private static final long serialVersionUID = 3247920529375102294L;
    private static final String PLAN_ID = "LowLevelController";

    private JFormattedTextField txtDesiredSpeed, txtDesiredHeading, txtDesiredDepth;
    private JLabel lblDepthLabel;
    private JButton startBtn, stopBtn;

    private double desiredHeading, desiredSpeed, desiredDepth;
    private boolean controlling = false;
    private Reference referenceToSend = new Reference();
    private LocationType vehicleLocation = null;
    private final Color lightRed = new Color(255, 230, 230);
    private final Color lightGreen = new Color(230, 255, 230);
    private VehicleType vehicle = null;

    /**
     * @param console
     */
    public LowLevelControl(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {
        TableLayout layout = new TableLayout(new double[] { 0.5, 0.5 }, new double[] { 25, 25, 25, 30 });
        layout.setHGap(2);
        layout.setVGap(2);
        setLayout(layout);

        txtDesiredHeading = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
        txtDesiredSpeed = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(2));
        txtDesiredDepth = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(2));
        startBtn = new JButton(I18n.text("Start"));
        stopBtn = new JButton(I18n.text("Stop"));
        txtDesiredHeading.setValue(0);
        txtDesiredHeading.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                desiredHeading = Double.parseDouble(txtDesiredHeading.getText());
            }
        });

        txtDesiredSpeed.setValue(0);
        txtDesiredSpeed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                desiredSpeed = Double.parseDouble(txtDesiredSpeed.getText());
                System.out.println(desiredSpeed);
            }
        });
        txtDesiredDepth.setValue(0);
        txtDesiredDepth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                desiredDepth = Double.parseDouble(txtDesiredDepth.getText());
                System.out.println(desiredDepth);
            }
        });

        lblDepthLabel = new JLabel(I18n.text("Depth (meters):"));
        add(new JLabel(I18n.text("Heading (degrees):")), "0,0");
        add(new JLabel(I18n.text("Speed (m/s):")), "0,1");
        add(lblDepthLabel, "0,2");

        add(txtDesiredHeading, "1,0");
        add(txtDesiredSpeed, "1,1");
        add(txtDesiredDepth, "1,2");

        add(startBtn, "0,3");
        add(stopBtn, "1,3");

        startBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                FollowReference man = new FollowReference();
                man.setControlSrc(ImcMsgManager.getManager().getLocalId().intValue());
                man.setControlEnt((short) 255);
                man.setLoiterRadius(0);

                PlanControl pc = new PlanControl();
                pc.setPlanId(PLAN_ID).setOp(PlanControl.OP.START).setType(PlanControl.TYPE.REQUEST).setArg(man);

                send(pc);
            }
        });

        stopBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PlanControl pc = new PlanControl();
                pc.setPlanId(PLAN_ID).setOp(PlanControl.OP.STOP).setType(PlanControl.TYPE.REQUEST);

                send(pc);
            }
        });
        try {
            vehicle = VehiclesHolder.getVehicleById(getConsole().getMainSystem());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            vehicle = null;
        }
    }

    @Periodic
    public void sendReference() {
        try {
            if (Double.parseDouble(txtDesiredDepth.getText()) != desiredDepth)
                txtDesiredDepth.setBackground(lightRed);
            else
                txtDesiredDepth.setBackground(lightGreen);

            if (Double.parseDouble(txtDesiredHeading.getText()) != desiredHeading)
                txtDesiredHeading.setBackground(lightRed);
            else
                txtDesiredHeading.setBackground(lightGreen);

            if (Double.parseDouble(txtDesiredSpeed.getText()) != desiredSpeed)
                txtDesiredSpeed.setBackground(lightRed);
            else
                txtDesiredSpeed.setBackground(lightGreen);
        }
        catch (Exception e) {

        }

        referenceToSend.setSpeed(new DesiredSpeed(desiredSpeed, SpeedUnits.METERS_PS));

        if (vehicleLocation == null)
            return;

        LocationType loc = new LocationType(vehicleLocation);

        if (desiredSpeed != 0) {
            loc.setAzimuth(desiredHeading);
            loc.setOffsetDistance(carrotDistance);
        }
        loc.convertToAbsoluteLatLonDepth();

        referenceToSend.setLat(loc.getLatitudeRads());
        referenceToSend.setLon(loc.getLongitudeRads());

        if (vehicle != null) {
            if (vehicle.getType().equals("AUV")) {
                referenceToSend.setZ(new DesiredZ((float) desiredDepth, ZUnits.DEPTH));
            }
            else if (vehicle.getType().equals("AUV")) {
                referenceToSend.setZ(new DesiredZ((float) desiredDepth, ZUnits.ALTITUDE));
            }
            else {
                referenceToSend.setZ(null);
            }
        }
        if (controlling)
            send(referenceToSend);
    }

    @Subscribe
    public void on(ConsoleEventMainSystemChange change) {
        try {
            vehicle = VehiclesHolder.getVehicleById(change.getCurrent());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            vehicle = null;
        }

        if (vehicle != null) {
            if (vehicle.getType().equals("AUV")) {
                lblDepthLabel.setText(I18n.text("Depth (meters):"));
                txtDesiredDepth.setVisible(true);
            }
            else if (vehicle.getType().equals("UAV")) {
                lblDepthLabel.setText(I18n.text("Altitude (meters):"));
                txtDesiredDepth.setVisible(true);
            }
            else {
                lblDepthLabel.setText("");
                txtDesiredDepth.setVisible(false);
            }
        }
    }

    @Subscribe
    public void on(EstimatedState state) {
        if (state.getSourceName().equals(getMainVehicleId()))
            vehicleLocation = IMCUtils.parseLocation(state);
    }

    @Subscribe
    public void on(PlanControlState state) {
        if (!state.getSourceName().equals(getMainVehicleId()))
            return;

        if (state.getPlanId().equals(PLAN_ID) && state.getState() == PlanControlState.STATE.EXECUTING)
            controlling = true;
        else
            controlling = false;

        if (controlling) {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            txtDesiredDepth.setEnabled(true);
            txtDesiredSpeed.setEnabled(true);
            txtDesiredHeading.setEnabled(true);
        }
        else {
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            txtDesiredDepth.setValue(0);
            txtDesiredSpeed.setValue(0);
            txtDesiredHeading.setValue(0);
            txtDesiredDepth.setEnabled(false);
            txtDesiredSpeed.setEnabled(false);
            txtDesiredHeading.setEnabled(false);
        }
    }
}
