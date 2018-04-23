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
 * Author: zp
 * 19/03/2017
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.mission.MissionType;
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
    public boolean sendDefinition = false, ignoreErrors = false, skipCalibration = true;

    private JComboBox<String> vehiclesCombo;
    private JComboBox<String> plansCombo;
    private JCheckBox sendDefsCheck, ignoreErrorsCheck, skipCalibrationCheck;

    public SendPlanDialog(ConsoleLayout console) {
        vehiclesCombo = new JComboBox<>(VehiclesHolder.getVehiclesArray());
        plansCombo = new JComboBox<>(console.getMission().getIndividualPlansList().keySet().toArray(new String[0]));
        sendDefsCheck = new JCheckBox(I18n.text("Send plan definition"));
        sendDefsCheck
                .setToolTipText(I18n.text("Include the current plan definition from this console (larger message)"));

        ignoreErrorsCheck = new JCheckBox(I18n.text("Ignore errors during execution"));
        ignoreErrorsCheck
                .setToolTipText(I18n.text("Continue plan execution even if errors are detected"));

        skipCalibrationCheck = new JCheckBox(I18n.text("Skip calibration"));
        skipCalibrationCheck
                .setToolTipText(I18n.text("Do not perform calibration before starting the plan"));
        
        

        try {
            selectedVehicle = console.getMainSystem();
            vehiclesCombo.setSelectedItem(selectedVehicle);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (console.getPlan() != null) {
            plansCombo.setSelectedItem(console.getPlan().getId());
            planId = console.getPlan().getId();
        }

        sendDefsCheck.addActionListener(this::sendDefsSelected);
        vehiclesCombo.addActionListener(this::vehicleSelected);
        plansCombo.addActionListener(this::planSelected);

        initLayout();
    }

    private void initLayout() {
        setLayout(new GridLayout(0, 1));
        add(new JLabel(I18n.text("Target Vehicle")));
        add(vehiclesCombo);

        add(new JLabel(I18n.text("Plan to execute")));
        add(plansCombo);
        add(sendDefsCheck);
        add(ignoreErrorsCheck);
        add(skipCalibrationCheck);
    }

    private void vehicleSelected(ActionEvent evt) {
        selectedVehicle = vehiclesCombo.getSelectedItem().toString();
    }

    private void planSelected(ActionEvent evt) {
        planId = plansCombo.getSelectedItem().toString();
    }

    private void sendDefsSelected(ActionEvent evt) {
        sendDefinition = sendDefsCheck.isSelected();
    }

    public static SendPlanDialog sendPlan(ConsoleLayout console) {
        SendPlanDialog dialog = new SendPlanDialog(console);
        int op = JOptionPane.showConfirmDialog(console, dialog, I18n.text("Send plan acoustically"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op == JOptionPane.OK_OPTION)
            return dialog;
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
