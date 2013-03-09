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
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.NameNormalizer;

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
        this.setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(console.getMission().getMissionFile());
        chooser.setFileFilter(GuiUtils.getCustomFileFilter("Mission Files ('nmisz')", new String[] { "nmisz" }));
        int resp = chooser.showDialog(console, "Save");
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
