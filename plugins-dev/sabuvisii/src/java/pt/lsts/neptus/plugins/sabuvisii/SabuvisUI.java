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

import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.mp.maneuvers.VehicleFormation;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
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
import java.util.ArrayList;
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
    private VehicleFormation vForm = null;

    private AbstractAction sendFormationAbs = null;
    
    @Override
    public void initLayer() {

       setupInteraction();
       getConsole().addInteraction(interaction);

    }

    private static String[] getSystemNames() {
        ImcSystem[] systems = ImcSystemsHolder.lookupAllActiveSystems();

        return Arrays.stream(systems)
                .map(ImcSystem::getName) // Map each system to its name
                .toArray(String[]::new);
    }

    private class SystemSelectionWindow {
        private JComboBox<String> leaderComboBox;
        private JPanel checkBoxPanel = new JPanel();
        private List<String> systems; // Original list of systems
        private String selectedLeader = null; // Current leader

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

            // Label and ComboBox for leader selection
            JLabel leaderLabel = new JLabel("Select Leader:");

            systems = Arrays.asList(getSystemNames());
            ArrayList<String> lList = new ArrayList<>();
            lList.add("<Choose Leader>");
            lList.addAll(systems);
            leaderComboBox = new JComboBox<>(lList.toArray(new String[0]));

            leaderComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedLeader = (String) leaderComboBox.getSelectedItem();
                    updateCheckBoxPanel(); // Update the checkboxes when the leader changes
                }
            });

            mainPanel.add(leaderLabel);
            mainPanel.add(leaderComboBox);

            // Separator
            mainPanel.add(Box.createVerticalStrut(10));

            // Label and Checkboxes for selecting systems
            JLabel systemsLabel = new JLabel("Select Systems:");
            mainPanel.add(systemsLabel);

            checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
            updateCheckBoxPanel();

            JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
            scrollPane.setPreferredSize(new Dimension(350, 150));

            mainPanel.add(scrollPane, BorderLayout.CENTER);

            JComboBox<String> planCbox = new JComboBox<>(getConsole().getMission().getIndividualPlansList().keySet().toArray(new String[0]));


            JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Get selected leader
                    String leader = (String) leaderComboBox.getSelectedItem();

                    if (leader != null) {
                        if (leader.equals("<Choose Leader>")) {
                            GuiUtils.errorMessage(getConsole(), I18n.text("Error"), I18n.text("Invalid leader."));
                            return;
                        }
                        Vector<VehicleType> list = new Vector<>();
                        vForm = new VehicleFormation();
                        VehicleType vLeader = VehiclesHolder.getVehicleById(leader);
                        list.add(vLeader);

                        StringBuilder a = new StringBuilder();
                        for (Component comp : checkBoxPanel.getComponents()) {
                            if (comp instanceof JCheckBox) {
                                JCheckBox checkBox = (JCheckBox) comp;
                                if (checkBox.isSelected()) {
                                    list.add(VehiclesHolder.getVehicleById(checkBox.getText()));
                                    a.append(" ").append(checkBox.getText());
                                }
                            }
                        }

                        list.add(VehiclesHolder.getVehicleById("lauv-xplore-1"));
                        if (list.size() < 2) {
                            GuiUtils.errorMessage(getConsole(), I18n.text("Error"), I18n.text("No participants selected."));
                            return;
                        }

                        vForm.setParticipants(list);
                        Vector<double[]> trajPoints = new Vector<>();


                        if (getConsole().getMission().getIndividualPlansList().isEmpty()) {
                            GuiUtils.errorMessage(getConsole(), I18n.text("Error"), I18n.text("No plan available.\nCreate a new plan."));
                            return;
                        }

                        PlanType sel = null;
                        for (Map.Entry<String, PlanType> plan : getConsole().getMission().getIndividualPlansList().entrySet()) {
                            if (plan.getKey().equals((String) planCbox.getSelectedItem())) {
                                sel = plan.getValue();
                                break;
                            }
                        }
                        Vector<LocatedManeuver> mans = PlanUtil.getLocationsAsSequence(sel);

                        if (mans.isEmpty()) {
                            GuiUtils.errorMessage(getConsole(), I18n.text("Error"), I18n.text("Invalid plan selected."));
                            return;
                        }

                        // Set initial location
                        vForm.setManeuverLocation(mans.get(0).getManeuverLocation());

                        // iterate through all maneuvers and add them as offsets
                        for (LocatedManeuver man : mans) {
                            if (man instanceof StationKeeping) {
                                StationKeeping lt = (StationKeeping) man;
                                double[] ret = lt.getManeuverLocation().getOffsetFrom(vForm.getManeuverLocation());
                                double[] point = new double[]{ret[0], ret[1], ret[2], lt.getDuration()};
                                trajPoints.add(point);
                            } else if (man instanceof RowsManeuver) {
                                List<LocationType> locations = ((RowsManeuver) man).getPathLocations();
                                RowsManeuver rm = (RowsManeuver) man;
                                for (LocationType lt : locations) {
                                    double[] ret = lt.getOffsetFrom(vForm.getManeuverLocation());
                                    double[] point = new double[]{ret[0], ret[1], ret[2], -1};
                                    trajPoints.add(point);
                                }
                            } else {
                                double[] ret = man.getManeuverLocation().getOffsetFrom(vForm.getManeuverLocation());
                                double[] point = new double[]{ret[0], ret[1], ret[2], -1};
                                trajPoints.add(point);
                            }
                        }
                        if (trajPoints.isEmpty()) {
                            GuiUtils.errorMessage(getConsole(), I18n.text("Error"), I18n.text("Trajectory is invalid."));
                            return;
                        }


                        vForm.setOffsets(trajPoints);

                        // Display the selections
                        GuiUtils.infoMessage(getConsole(), I18n.text("Participants"), "Leader: " + leader + "\n" + a.toString());
                        sendFormationAbs.setEnabled(true);
                        frame.dispose();
                    }
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
                if (!system.equals(selectedLeader)) {
                    JCheckBox checkBox = new JCheckBox(system);
                    checkBoxPanel.add(checkBox);
                }
            }

            // Refresh the panel
            checkBoxPanel.revalidate();
            checkBoxPanel.repaint();
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
                    popup.add(new AbstractAction("Set Parameters") {

                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            SwingUtilities.invokeLater(SystemSelectionWindow::new);
                        }
                    });

                    popup.addSeparator();

                    sendFormationAbs = new AbstractAction("Send formation") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            if (vForm != null) {
                                Map<String, Boolean> ret = new HashMap<>();
                                ExecutorService executorService = Executors.newSingleThreadExecutor();
                                executorService.submit(() -> {
                                    for (VehicleType veh : vForm.getParticipants()) {
                                        boolean v = ImcMsgManager.getManager().sendMessageToSystem(vForm.serializeToIMC(), veh.getId());
                                        ret.put(veh.getId(), v);
                                    }
                                });

                                // Shutdown the executor when tasks are complete
                                executorService.shutdown();
                                if (!ret.isEmpty()) {
                                    GuiUtils.errorMessage(getConsole(), I18n.text("Error sending"), I18n.text("Unable to send formation to: "+ String.join(", ", ret.keySet())));
                                }
                            }
                            else
                                GuiUtils.errorMessage(getConsole(), I18n.text("Error"), I18n.text("Invalid Formation."));

                        }
                    };

                    popup.add(sendFormationAbs);
                    popup.add(new AbstractAction("Clear formation") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            vForm = null;
                            sendFormationAbs.setEnabled(false);
                        }
                    });

                    popup.show(source, event.getX(), event.getY());
                }
            }
        };
    }


//    private void sendMessage(Message msg) {
//        Runnable send = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    if (sendCommandsReliably)
//                        sendMessageReliably(msg);
//                    else
//                        sendMessageUnreliable(msg);
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                    NeptusLog.pub().error(e);
//                }
//            }
//        };
//
//        Thread t = new Thread(send, "NECSAVE send");
//        t.setDaemon(true);
//        t.start();
//    }

    @Override
    public void cleanLayer() {

    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }
}
