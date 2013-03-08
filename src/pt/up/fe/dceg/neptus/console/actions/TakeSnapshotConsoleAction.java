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
 * $Id:: TakeSnapshotConsoleAction.java 9615 2012-12-30 23:08:28Z pdias         $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Hugo
 * 
 */
@SuppressWarnings("serial")
public class TakeSnapshotConsoleAction extends ConsoleAction {
    ConsoleLayout console;


    public TakeSnapshotConsoleAction(ConsoleLayout console) {
        super(I18n.text("Take Snapshot"), new ImageIcon(ImageUtils.getImage("images/menus/snapshot.png")));
        putValue(AbstractAction.ACCELERATOR_KEY, javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_PRINTSCREEN, java.awt.Event.CTRL_MASK, true));
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
                GuiUtils.takeSnapshot(console, "Console");
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
