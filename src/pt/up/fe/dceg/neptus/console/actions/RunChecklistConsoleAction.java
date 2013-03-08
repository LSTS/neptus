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
 * $Id:: RunChecklistConsoleAction.java 9615 2012-12-30 23:08:28Z pdias         $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.checklist.ChecklistType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

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
