/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.console.actions;

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import pt.lsts.imc.gui.ImcStatePanel;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;

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
        ImcSystem[] active = ImcSystemsHolder.lookupAllActiveSystems();
        final HashSet<String> systems = new HashSet<String>();
        for (ImcSystem s : active) {
            systems.add(s.getName());
        }
        
        for (String s : console.getSystems().keySet()) {
            systems.add(s);
        }
        
        for (String s : systems) {
            combovt.addItem(s);
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
                for (String s : systems) {
                    //ConsoleSystem vehicle = entry.getValue();
                    if (s.equals((String) cbox.getSelectedItem())) {
                        if (imcStatePanel != null)
                            imcStatePanel.cleanup();
                        imcStatePanel = new ImcStatePanel(ImcMsgManager.getManager().getState(s));
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
