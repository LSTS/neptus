/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * 19/03/2017
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBInfo;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class SendPlanDialog extends JPanel {

    private static final long serialVersionUID = 1L;
    public String selectedVehicle;
    public String planId;
    public String startingManeuver;
    public boolean sendDefinition = false, isResume = false, ignoreErrors = false, skipCalibration = false;
    private ConsoleLayout console;

    private JComboBox<String> vehiclesCombo;
    private JComboBox<String> plansCombo;
    private JComboBox<String> maneuversCombo;
    private JCheckBox sendDefsCheck, ignoreErrorsCheck, skipCalibrationCheck;

    private ActionListener maneuversSelectedListener = this::maneuverSelected;

    public SendPlanDialog(ConsoleLayout console) {
        this.console = console;
        vehiclesCombo = new JComboBox<>(VehiclesHolder.getVehiclesArray());
        plansCombo = new JComboBox<>(console.getMission().getIndividualPlansList().keySet().toArray(new String[0]));
        Object planId = plansCombo.getSelectedItem();
        if(planId != null){
            this.planId = planId.toString();
        }
        maneuversCombo = new JComboBox<>(getPlanManeuvers(console));
        Object startMan = maneuversCombo.getSelectedItem();
        if(startMan != null){
            startingManeuver = startMan.toString();
        }
        sendDefsCheck = new JCheckBox(I18n.text("Send plan definition"));
        sendDefsCheck
                .setToolTipText(I18n.text("Include the current plan definition from this console (larger message)"));

        ignoreErrorsCheck = new JCheckBox(I18n.text("Ignore errors during execution"));
        ignoreErrorsCheck
                .setToolTipText(I18n.text("Continue plan execution even if errors are detected"));

        skipCalibrationCheck = new JCheckBox(I18n.text("Skip calibration"));
        skipCalibrationCheck
                .setToolTipText(I18n.text("Do not perform calibration before starting the plan"));


        ignoreErrorsCheck.addActionListener(this::ignoreErrorsSelected);
        skipCalibrationCheck.addActionListener(this::skipCalibrationSelected);
        sendDefsCheck.addActionListener(this::sendDefsSelected);
        vehiclesCombo.addActionListener(this::vehicleSelected);
        plansCombo.addActionListener(this::planSelected);
        maneuversCombo.addActionListener(maneuversSelectedListener);

        try {
            selectedVehicle = console.getMainSystem();
            vehiclesCombo.setSelectedItem(selectedVehicle);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (console.getPlan() != null) {
            plansCombo.setSelectedItem(console.getPlan().getId());
            this.planId = console.getPlan().getId();
        }

        initLayout();
    }

    private void initLayout() {
        setLayout(new GridLayout(0, 1));
        add(new JLabel(I18n.text("Target Vehicle")));
        add(vehiclesCombo);

        add(new JLabel(I18n.text("Plan to execute")));
        add(plansCombo);

        add(new JLabel(I18n.text("Plan to start at")));
        add(maneuversCombo);

        add(sendDefsCheck);
        add(ignoreErrorsCheck);
        add(skipCalibrationCheck);
    }

    private void vehicleSelected(ActionEvent evt) {
        selectedVehicle = vehiclesCombo.getSelectedItem().toString();
        vehicleHasPlanCheck();
    }

    private void planSelected(ActionEvent evt) {
        planId = plansCombo.getSelectedItem().toString();
        maneuversCombo.removeActionListener(maneuversSelectedListener);
        maneuversCombo.removeAllItems();
        for (String maneuver : getPlanManeuvers(console)) {
            maneuversCombo.addItem(maneuver);
        }
        maneuversCombo.addActionListener(maneuversSelectedListener);
        maneuversCombo.setSelectedIndex(0);
        vehicleHasPlanCheck();
    }

    private void maneuverSelected(ActionEvent actionEvent) {
        if (maneuversCombo.getSelectedItem() != null){
            startingManeuver = maneuversCombo.getSelectedItem().toString();
        }
        else {
            startingManeuver = null;
        }
        if(maneuversCombo.getSelectedIndex() != 0){
            sendDefsCheck.setEnabled(false);
            ignoreErrorsCheck.setEnabled(false);
            skipCalibrationCheck.setEnabled(false);
            sendDefinition = false;
            ignoreErrors = false;
            skipCalibration = false;
            isResume = true;
        }
        else {
            sendDefsCheck.setEnabled(true);
            ignoreErrorsCheck.setEnabled(true);
            skipCalibrationCheck.setEnabled(true);
            sendDefinition = sendDefsCheck.isSelected();
            ignoreErrors = ignoreErrorsCheck.isSelected();
            skipCalibration = skipCalibrationCheck.isSelected();
            isResume = false;
            vehicleHasPlanCheck();
        }
    }

    private void skipCalibrationSelected(ActionEvent evt) {
        skipCalibration = skipCalibrationCheck.isSelected();
    }

    private void ignoreErrorsSelected(ActionEvent evt) {
        ignoreErrors = ignoreErrorsCheck.isSelected();
    }

    private void sendDefsSelected(ActionEvent evt) {
        sendDefinition = sendDefsCheck.isSelected();
    }

    private void vehicleHasPlanCheck(){
        if(selectedVehicle != null && planId != null){
            try{
                Map<String, PlanDBInfo> plans = ImcSystemsHolder.lookupSystemByName(selectedVehicle).getPlanDBControl()
                        .getRemoteState().getStoredPlans();
                if(plans.containsKey(planId)){
                    sendDefsCheck.setEnabled(false);
                    sendDefinition = false;
                } else {
                    sendDefsCheck.setEnabled(true);
                    sendDefinition = sendDefsCheck.isSelected();
                }
            } catch (Exception e) {
                sendDefsCheck.setEnabled(true);
                sendDefinition = sendDefsCheck.isSelected();
                e.printStackTrace();
            }
        }
    }

    private String[] getPlanManeuvers(ConsoleLayout console) {
        String planName = (String)plansCombo.getSelectedItem();
        if(planName != null){
            PlanType planType = console.getMission().getIndividualPlansList().get(planName);
            if(planType != null){
                Maneuver[] maneuvers = planType.getGraph().getAllManeuvers();
                return Arrays.stream(maneuvers).map(Maneuver::getId).toArray(String[]::new);
            }
        }
        return new String[0];
    }

    public static SendPlanDialog sendPlan(ConsoleLayout console) {
        SendPlanDialog dialog = new SendPlanDialog(console);
        while(true){
            int op = JOptionPane.showConfirmDialog(console, dialog, I18n.text("Send plan acoustically"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (op == JOptionPane.OK_OPTION){
                if(dialog.ignoreErrors){
                    int confirm = JOptionPane.showConfirmDialog(
                            console,
                            I18n.text("Are you sure you want to proceed using the Ignore Errors option?"),
                            I18n.text("Ignore Errors"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION){
                        return dialog;
                    }
                }
                else {
                    return dialog;
                }
            }
            else{
                break;
            }
        }
        return null;
    }
    
    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        ConsoleLayout cl = ConsoleLayout.forge();
        cl.setMainSystem("lauv-xplore-1");
        cl.setMission(new MissionType());
        SendPlanDialog.sendPlan(cl);
    }
}
