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
 * Nov 27, 2012
 */
package pt.lsts.neptus.console.actions;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.EntityStatePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;

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
