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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Hugo Dias
 * Nov 10, 2012
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ConsoleSystem;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.gui.ImcStatePanel;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * @author Hugo
 * 
 */
public class IncomingDataAction extends ConsoleAction {

    private static final long serialVersionUID = 1L;
    protected ConsoleLayout console;
    protected ImcStatePanel imcStatePanel;

    public IncomingDataAction(ConsoleLayout console) {
        super(I18n.text("Incoming Data"), ImageUtils.createImageIcon("images/menus/view_tree.png"), I18n
                .text("Incoming IMC Data"), KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK, true));
        this.console = console;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        JDialog dialog = new JDialog(console);
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setIconImage(ImageUtils.getImage("images/menus/view_tree.png"));
        dialog.setName(I18n.text("Incoming Data"));
        dialog.setTitle(I18n.text("Incoming Data"));
        dialog.setSize(600, 400);
        dialog.setLayout(new BorderLayout());
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (imcStatePanel != null)
                    imcStatePanel.cleanup();
            }
        });

        final JScrollPane statePanel = new JScrollPane();
        dialog.add(statePanel, BorderLayout.CENTER);
        JComboBox<String> combovt = new JComboBox<String>();
        dialog.add(combovt, BorderLayout.NORTH);

        final Map<String, ConsoleSystem> vehicles = console.getConsoleSystems();
        for (Entry<String, ConsoleSystem> entry : vehicles.entrySet()) {
            combovt.addItem(entry.getValue().getVehicleId());
        }
        String mainVehicle = console.getMainSystem();
        if (mainVehicle == null) {
            NeptusLog.pub().warn(this + "Main vehicle N/A");
        }
        else {
            ConsoleSystem vtl = console.getSystem(mainVehicle);
            if (imcStatePanel != null)
                imcStatePanel.cleanup();
            imcStatePanel = new ImcStatePanel(ImcMsgManager.getManager().getState(vtl.getVehicle().getId()));
            statePanel.setViewportView(imcStatePanel);
        }
        
        combovt.setSelectedItem(mainVehicle);
        combovt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JComboBox<?> cbox = (JComboBox<?>) evt.getSource();
                for (Entry<String, ConsoleSystem> entry : vehicles.entrySet()) {
                    ConsoleSystem vehicle = entry.getValue();
                    if (vehicle.getVehicleId().equals((String) cbox.getSelectedItem())) {
                        if (imcStatePanel != null)
                            imcStatePanel.cleanup();
                        imcStatePanel = new ImcStatePanel(ImcMsgManager.getManager().getState(vehicle.getVehicle().getId()));
                        statePanel.setViewportView(imcStatePanel);
                        statePanel.revalidate();
                        statePanel.repaint();
                        break;
                    }
                }
            }
        });

        dialog.setVisible(true);
    }
}
