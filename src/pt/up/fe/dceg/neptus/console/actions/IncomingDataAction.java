/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Nov 10, 2012
 * $Id:: IncomingDataAction.java 9615 2012-12-30 23:08:28Z pdias                $:
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
            imcStatePanel = new ImcStatePanel(ImcMsgManager.getManager().getState(vtl.getVehicle()));
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
                        imcStatePanel = new ImcStatePanel(ImcMsgManager.getManager().getState(vehicle.getVehicle()));
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
