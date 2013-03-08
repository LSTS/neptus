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
 * Oct 18, 2012
 * $Id:: AutoSnapshotConsoleAction.java 9615 2012-12-30 23:08:28Z pdias         $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.PeriodicSnapshotWorker;



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
