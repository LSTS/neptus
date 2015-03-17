/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Hugo Dias
 * Oct 17, 2012
 */
package pt.lsts.neptus.console.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.NameNormalizer;

/**
 * @author Hugo
 *
 */
@SuppressWarnings("serial")
public class SaveMissionAsConsoleAction extends ConsoleAction{
    protected ConsoleLayout console;

    public SaveMissionAsConsoleAction(ConsoleLayout console) {
        super(I18n.text("Save Mission As..."), new ImageIcon(ImageUtils.getImage("images/menus/saveas.png")));
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK
                + java.awt.Event.ALT_MASK, true));
        this.console = console;
        this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(console.getMission().getMissionFile());
        chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("Mission Files ") + "('nmisz')", new String[] { "nmisz" }));
        int resp = chooser.showDialog(console, I18n.text("Save"));
        if (resp == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().exists()) {
                resp = JOptionPane.showConfirmDialog(console,
                        I18n.text("Do you want to overwrite the existing file?"),
                        I18n.text("Save Mission As..."), JOptionPane.YES_NO_CANCEL_OPTION);
                if (resp != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            File dst = chooser.getSelectedFile();
            if (!dst.getAbsolutePath().endsWith(".nmisz")) {
                dst = new File(dst.getAbsolutePath() + ".nmisz");
            }
            console.getMission().setMissionFile(dst);
            console.getMission().setId(NameNormalizer.getRandomID());
            console.getMission().save(false);

            MapGroup.resetMissionInstance(console.getMission());
        }
    }
}
