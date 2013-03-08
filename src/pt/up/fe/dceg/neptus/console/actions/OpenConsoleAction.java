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
 * Oct 16, 2012
 * $Id:: OpenConsoleAction.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

import org.dom4j.DocumentException;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.ConsoleFileChooser;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Hugo
 * 
 */
@SuppressWarnings("serial")
public class OpenConsoleAction extends ConsoleAction {
    protected ConsoleLayout console;

    public OpenConsoleAction(ConsoleLayout console) {
        super(I18n.text("Open Console"), new ImageIcon(ImageUtils.getImage("images/menus/open.png")));
        this.console = console;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final File file = ConsoleFileChooser.showOpenConsoleDialog(console);
        if (file != null) {
            this.setEnabled(false);
            console.getContentPane().setVisible(false);
            console.reset();

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        ConsoleParse.parseFile(file.getAbsolutePath().toString(), console);
                    }
                    catch (DocumentException e) {
                        NeptusLog.pub().error(e);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    setEnabled(true);
                    console.getContentPane().setVisible(true);
                    console.setConsoleChanged(false);
                }
            };
            worker.execute();

        }

    }

}
