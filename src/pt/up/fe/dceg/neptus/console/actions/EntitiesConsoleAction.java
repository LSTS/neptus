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
 * Nov 27, 2012
 * $Id:: EntitiesConsoleAction.java 9615 2012-12-30 23:08:28Z pdias             $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.EntityStatePanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Hugo
 * 
 */
public class EntitiesConsoleAction extends ConsoleAction {
    private static final long serialVersionUID = 1L;
    private JDialog entitiesDialog = null;
    private ConsoleLayout console;

    public EntitiesConsoleAction(ConsoleLayout console) {
        super(I18n.text("Entities"), ImageUtils.getScaledIcon("images/buttons/events.png", 16, 16), KeyStroke
                .getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK, true));
        this.console = console;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (entitiesDialog == null) {
            entitiesDialog = new JDialog(console);
            entitiesDialog.setTitle(I18n.text("Entities"));
            entitiesDialog.setIconImage(ImageUtils.getImage("images/buttons/events.png"));
            entitiesDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            entitiesDialog.add(console.getSubPanelsOfClass(EntityStatePanel.class).get(0));
            entitiesDialog.setLocationRelativeTo(null);
            entitiesDialog.setSize(400, 400);
            entitiesDialog.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_E) {
                        entitiesDialog.setVisible(false);
                    }
                }
            });
        }
        entitiesDialog.setVisible(!entitiesDialog.isVisible());
        entitiesDialog.setFocusable(true);
        if (entitiesDialog.isVisible())
            entitiesDialog.requestFocus();
    }

}
