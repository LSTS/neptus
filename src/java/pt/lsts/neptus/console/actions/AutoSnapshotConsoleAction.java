/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 18, 2012
 */
package pt.lsts.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.PeriodicSnapshotWorker;



public class AutoSnapshotConsoleAction extends ConsoleAction {
    private static final long serialVersionUID = 5741528409297995492L;
    
    protected PeriodicSnapshotWorker snapshotHelper = null;
    private static ImageIcon ICON_SNAPSHOT = ImageUtils.createImageIcon("images/menus/snapshot.png");
    private static ImageIcon ICON_SNAPSHOT_NO = ICON_SNAPSHOT;
    private static String nameTurnOn = I18n.text("Start Auto Snapshot");
    private static String nameTurnOff = I18n.text("Stop Auto Snapshot");

    private boolean running;

    public AutoSnapshotConsoleAction(ConsoleLayout console) {
        super(nameTurnOn, ICON_SNAPSHOT);
        running = false;
        snapshotHelper = new PeriodicSnapshotWorker();
        snapshotHelper.setPrefix("Console");
        snapshotHelper.setComponentToSnapshot(console);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            stopAutoSnapshot();
        }
        else {
            startAutoSnapshot();
        }

    }

    public void cleanClose() {
        if (snapshotHelper != null)
            snapshotHelper.stopWorking();
    }

    private void stopAutoSnapshot() {
        // autoSnapshotAction.putValue(AbstractAction.NAME, I18n.text("Start auto snapshot"));
        putValue(AbstractAction.NAME, nameTurnOn);
        putValue(AbstractAction.SMALL_ICON, ICON_SNAPSHOT);
        running = false;
        snapshotHelper.stopWorking();
    }

    private void startAutoSnapshot() {
        // autoSnapshotAction.putValue(AbstractAction.NAME, I18n.text("Stop auto snapshot"));
        putValue(AbstractAction.NAME, nameTurnOff);
        putValue(AbstractAction.SMALL_ICON, ICON_SNAPSHOT_NO);

        running = true;
        snapshotHelper.startWorking();
    }
}
