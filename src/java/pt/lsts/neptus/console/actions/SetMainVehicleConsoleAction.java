/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Hugo Dias
 * Oct 18, 2012
 */
package pt.lsts.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

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
                    console.addSystem(mv);
                    console.setMainSystem(mv);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
                setEnabled(true);
            }
        };
        worker.execute();
    }
}
