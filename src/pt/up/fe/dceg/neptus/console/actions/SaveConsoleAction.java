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
 * $Id:: SaveConsoleAction.java 9660 2013-01-04 17:30:52Z pdias                 $:
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
public class SaveConsoleAction extends ConsoleAction {
    protected ConsoleLayout console;

    public SaveConsoleAction(ConsoleLayout console) {
        super(I18n.text("Save Console"), new ImageIcon(ImageUtils.getImage("images/menus/save.png")));
        this.console = console;
    }

    /**
     * Set disable is not allowed
     * @see javax.swing.AbstractAction#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean newValue) {
        if (newValue)
            super.setEnabled(newValue);
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
                console.saveFile();
                return null;
            }

            @Override
            protected void done() {
//                if(console.isConsoleChanged()){
//                   setEnabled(true); 
//                }
                setEnabled(true); 
            }
        };
        worker.execute();
    }

}
