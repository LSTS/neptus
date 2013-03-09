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
 * Oct 18, 2012
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Hugo
 * 
 */
@SuppressWarnings("serial")
public class SetMainVehicleConsoleAction extends ConsoleAction {
    ConsoleLayout console;

    public SetMainVehicleConsoleAction(ConsoleLayout console) {
        super(I18n.text("Set Main Vehicle..."), new ImageIcon(ImageUtils.getImage("images/menus/vehicle.png")));
        this.console = console;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (VehiclesHolder.getVehiclesArray().length == 0) {
                    GuiUtils.errorMessage(console, I18n.text("Console error"),
                            I18n.text("Selecting Main Vehicle: no Vehicles configured/loaded in Neptus"));
                }

                String mv = (String) JOptionPane.showInputDialog(console,
                        I18n.text("Choose one of the available Vehicles"), I18n.text("Select Vehicle"),
                        JOptionPane.QUESTION_MESSAGE, null, VehiclesHolder.getVehiclesArray(),
                        null);

                if (mv != null) {
                    console.setMainSystem(mv);
                }
                return null;
            }

            @Override
            protected void done() {
                setEnabled(true);
            }
        };
        worker.execute();
    }
}
