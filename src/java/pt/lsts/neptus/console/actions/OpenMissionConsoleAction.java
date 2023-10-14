/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 17, 2012
 */
package pt.lsts.neptus.console.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.ImageUtils;

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
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK
                | InputEvent.ALT_DOWN_MASK, true));
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
