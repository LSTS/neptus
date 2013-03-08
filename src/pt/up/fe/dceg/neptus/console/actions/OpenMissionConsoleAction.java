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
 * $Id:: OpenMissionConsoleAction.java 9901 2013-02-11 14:37:41Z pdias          $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mystate.MyState;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Hugo
 * 
 */
@SuppressWarnings("serial")
public class OpenMissionConsoleAction extends ConsoleAction {
    protected ConsoleLayout console;

    public OpenMissionConsoleAction(ConsoleLayout console) {
        super(I18n.text("Open Mission"), new ImageIcon(ImageUtils.getImage("images/menus/open.png")));
        putValue(Action.SHORT_DESCRIPTION, null);
        putValue(Action.MNEMONIC_KEY, null);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Event.CTRL_MASK
                + java.awt.Event.ALT_MASK, true));
        this.console = console;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        console.setMissionFile(new String[] { "nmisz" });
        MissionType newMission = console.getMission();
        if (newMission != null) {
            HomeReference newHr = newMission.getHomeRef();
            double dist = newHr.getDistanceInMeters(MyState.getLocation());
            if (dist > 1E3)
                MyState.setLocation(newHr);
        }
    }
}
