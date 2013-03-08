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
 * $Id:: LayoutEditConsoleAction.java 9615 2012-12-30 23:08:28Z pdias           $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;

/**
 * @author Hugo
 * 
 */
@SuppressWarnings("serial")
public class LayoutEditConsoleAction extends ConsoleAction {
    protected ConsoleLayout console;
    protected boolean enable = false;

    public LayoutEditConsoleAction(ConsoleLayout console) {
        super(I18n.text("Enable Layout Edit"), null, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
        this.console = console;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.enable = !enable;

        if (enable) {
            this.putValue(AbstractAction.NAME, I18n.text("Disable Layout Edit"));
        }
        else {
            this.putValue(AbstractAction.NAME, I18n.text("Enable Layout Edit"));
        }
        console.setModeEdit(enable);
    }
}
