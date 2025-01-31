/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: maribeiro
 * Jan 23, 2025
 */
package pt.lsts.neptus.plugins.sabuvisii;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanDB;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@PluginDescription(name = "SABUVIS UI", icon= "pt/lsts/neptus/plugins/sabuvisii/grid.png")
public class SabuvisUI extends ConsoleLayer {
    private ImcSystemsHolder holder;
    private ConsoleInteraction interaction;

    private AbstractAction sendFormationAbs = null;
    
    @Override
    public void initLayer() {

       setupInteraction();
       getConsole().addInteraction(interaction);

    }

    private static String[] getActiveSystemNames() {
        ImcSystem[] systems = ImcSystemsHolder.lookupAllActiveSystems();

        return Arrays.stream(systems)
                .map(ImcSystem::getName) // Map each system to its name
                .toArray(String[]::new);
    }

    private class SystemSelectionWindow {
        private JPanel checkBoxPanel = new JPanel();
        private List<String> systems; // Original list of systems

        public SystemSelectionWindow() {
            JDialog frame = new JDialog(new JFrame(), "Formation Participants");

            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    frame.dispose();
                }
            });

            frame.setSize(180, 300);

            // Main panel
            JPanel mainPanel = new JPanel();
            checkBoxPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            frame.add(mainPanel);

            // Separator
            mainPanel.add(Box.createVerticalStrut(10));

            // Label and Checkboxes for selecting systems
            JLabel systemsLabel = new JLabel("Select Systems:");
            mainPanel.add(systemsLabel);

            checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

            //get all available systems
            systems = Arrays.asList(getActiveSystemNames());
            updateCheckBoxPanel();

            JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
            scrollPane.setPreferredSize(new Dimension(350, 150));

            mainPanel.add(scrollPane, BorderLayout.CENTER);

            JComboBox<String> planCbox = new JComboBox<>(getConsole().getMission().getIndividualPlansList().keySet().toArray(new String[0]));


            JButton submitButton = new JButton("Send");
            submitButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Vector<VehicleType> list = new Vector<>();

                    StringBuilder a = new StringBuilder();
                    for (Component comp : checkBoxPanel.getComponents()) {
                        if (comp instanceof JCheckBox) {
                            JCheckBox checkBox = (JCheckBox) comp;
                            if (checkBox.isSelected()) {
                                list.add(VehiclesHolder.getVehicleById(checkBox.getText()));
                                a.append("[ ").append(checkBox.getText()).append("] ");
                            }
                        }
                    }

                    if (list.size() < 2) {
                        GuiUtils.errorMessage(getConsole(), I18n.text("Error"), I18n.text("At least 2 participants need to be selected."));
                        return;
                    }

                    String selPlanId = (String) planCbox.getSelectedItem();
                    PlanType planSelected = getConsole().getMission().getIndividualPlansList().get(selPlanId);
                    if (planSelected == null) {
                        GuiUtils.errorMessage(getConsole(), I18n.text("Error"), I18n.text("Invalid plan selected."));
                        return;
                    }

                    GuiUtils.infoMessage(getConsole(), I18n.text("Sending to..."), a.toString());
                    Map<String, Boolean> sendResMap = new HashMap<>();
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.submit(() -> {
                        //send plan to each vehicle
                        for (VehicleType veh : list) {
                            IMCMessage planSpecificationMessage = IMCUtils.generatePlanSpecification(planSelected);
                            int reqId = IMCSendMessageUtils.getNextRequestId();
                            PlanDB pdb = new PlanDB();
                            pdb.setType(PlanDB.TYPE.REQUEST);
                            pdb.setOp(PlanDB.OP.SET);
                            pdb.setRequestId(reqId);
                            pdb.setPlanId(planSelected.getId());
                            pdb.setArg(planSpecificationMessage);
                            pdb.setInfo("Plan sent by SABUVISUI");

                            boolean sent = ImcMsgManager.getManager().sendMessageToSystem(pdb, veh.getId());
                            if (!sent)
                                sendResMap.put(veh.getId(), false);
                        }
                        if (sendResMap.values().stream().anyMatch(value -> !value)) {
                            GuiUtils.errorMessage(getConsole(), I18n.text("Error sending"), I18n.text("Unable to send formation to: "+ String.join(", ", sendResMap.keySet())));
                        }

                    });

                    // Shutdown the executor when tasks are complete
                    executorService.shutdown();

                    for (VehicleType veh : list) {
                        sendStart(planSelected.getId(), veh.getId());
                    }

                    frame.dispose();
                }
            });
            mainPanel.add(new JLabel(I18n.text("Select Plan:")));
            mainPanel.add(planCbox);

            mainPanel.add(Box.createVerticalStrut(10)); // Add spacing
            mainPanel.add(submitButton);

            // Display the frame
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setVisible(true);
        }

        private void updateCheckBoxPanel() {
            // Clear the current checkboxes
            checkBoxPanel.removeAll();

            // Populate checkboxes with systems excluding the selected leader
            for (String system : systems) {
                    JCheckBox checkBox = new JCheckBox(system);
                    checkBoxPanel.add(checkBox);
            }

            // Refresh the panel
            checkBoxPanel.revalidate();
            checkBoxPanel.repaint();
        }

        private void sendStart(String planId, String systemId) {
            Runnable send = new Runnable() {
                @Override
                public void run() {
                    try {
                        int reqId = IMCSendMessageUtils.getNextRequestId();
                        PlanControl pc = new PlanControl();
                        pc.setType(PlanControl.TYPE.REQUEST);
                        pc.setRequestId(reqId);
                        pc.setPlanId(planId);
                        pc.setOp(PlanControl.OP.START);

                        ImcMsgManager.getManager().sendMessageToSystem(pc, systemId);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
            };

            Thread t = new Thread(send, "Sabuvis PC send");
            t.setDaemon(true);
            t.start();
        }
    }

//    @Override
//    public void paint(Graphics2D g, StateRenderer2D renderer) {
//        paintPlan(g, renderer);
//    }
//
//    private void paintPlan(Graphics2D g, StateRenderer2D source) {
//        if (vForm == null)
//            return;
//
//        Vector<Color> colors = new Vector<>(Arrays.asList(
//                        ColorUtils.generateVisuallyDistinctColors(
//                                vForm.getPathLocations().size(),
//                        0.5f,
//                        0.5f)));
//        System.out.println("Vector: " + vForm.getPathLocations().size());
//
//        vForm.paintInteraction(g, source);
//    }

    private void setupInteraction() {
        interaction = new ConsoleInteraction() {

            @Override
            public void initInteraction() {

            }

            @Override
            public void cleanInteraction() {

            }

            @Override
            public void mouseClicked(MouseEvent event, StateRenderer2D source) {
                if (event.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(new AbstractAction("Set Participants") {

                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            SwingUtilities.invokeLater(SystemSelectionWindow::new);
                        }
                    });

                    popup.addSeparator();

                    popup.show(source, event.getX(), event.getY());
                }
            }
        };
    }

    @Override
    public void cleanLayer() {

    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }
}
