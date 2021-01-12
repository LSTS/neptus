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

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.checklist.ChecklistType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Hugo
 * 
 */
@SuppressWarnings("serial")
public class RunChecklistConsoleAction extends ConsoleAction {
    ConsoleLayout console;

    public RunChecklistConsoleAction(final ConsoleLayout console) {
        super(I18n.text("Run CheckList"), new ImageIcon(ImageUtils.getImage("images/buttons/checklist.png")));
        this.console = console;
        console.addMissionListener(new MissionChangeListener() {

            @Override
            public void missionUpdated(MissionType mission) {
                updateStatus(console);
            }

            private void updateStatus(final ConsoleLayout console) {
                if (console.getMission() != null && console.getMission().getChecklistsList().size() > 0) {
                    setEnabled(true);
                }
                else {
                    setEnabled(false);
                }
            }

            @Override
            public void missionReplaced(MissionType mission) {
                updateStatus(console);
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        this.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (console.getMission() == null) {
                    GuiUtils.errorMessage(null, I18n.text("Console error"),
                            I18n.text("Selecting Plan: no Mission Loaded in console"));
                    return null;
                }

                String checkl = (console.getMission().getChecklistsList().size() == 0) ? null
                        : (String) JOptionPane.showInputDialog(console,
                                I18n.text("Choose one of the available Checklists"),
                                I18n.text("Select Checklist"), JOptionPane.QUESTION_MESSAGE, new ImageIcon(),
                                console.getMission().getChecklistsList().keySet().toArray(), console
                                        .getMission().getChecklistsList().keySet().iterator().next());

                if (checkl != null) {
                    ChecklistType newClone = console.getMission().getChecklistsList().get(checkl)
                            .getChecklist().createCopy();
                    console.executeCheckList(newClone);
                }
                return null;
            }

            
        };
        worker.execute();
    }

}
