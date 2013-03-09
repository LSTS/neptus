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
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.KeyStroke;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.AboutPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings("serial")
public class AboutAction extends ConsoleAction {

    protected ConsoleLayout console;
    protected AboutPanel aboutPanel;

    public AboutAction(ConsoleLayout console) {
        super(I18n.text("About"), ImageUtils.createImageIcon("images/menus/info.png"), I18n
                .text("About"), KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK
                + InputEvent.ALT_DOWN_MASK, true));
        this.console = console;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (aboutPanel == null) {
            final AboutPanel ap = new AboutPanel();
            aboutPanel = ap;
            ap.setVisible(true);
            console.addWindowToOppenedList(ap);
            ap.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    aboutPanel = null;
                    console.removeWindowToOppenedList(ap);
                }
            });
        }
        else {
            aboutPanel.setVisible(true);
            aboutPanel.requestFocusInWindow();
        }
    }
}
