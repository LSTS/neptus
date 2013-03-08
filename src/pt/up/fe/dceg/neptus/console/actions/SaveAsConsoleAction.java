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
 * Oct 17, 2012
 * $Id:: SaveAsConsoleAction.java 9615 2012-12-30 23:08:28Z pdias               $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Hugo
 * 
 */
@SuppressWarnings("serial")
public class SaveAsConsoleAction extends ConsoleAction {
    protected ConsoleLayout console;

    public SaveAsConsoleAction(ConsoleLayout console) {
        super(I18n.text("Save Console As"), new ImageIcon(ImageUtils.getImage("images/menus/saveas.png")));
        this.console = console;
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
                console.saveasFile();
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