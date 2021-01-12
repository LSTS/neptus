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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 15/01/2016
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.jdesktop.swingx.JXLabel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.concurrency.QueueWorkTickets;

/**
 * @author pdias
 *
 */
public class LogsDownloaderWorkerGUIUtil {

    private static final ColorMap diskFreeColorMap = ColorMapFactory
            .createInvertedColorMap((InterpolationColorMap) ColorMapFactory.createRedYellowGreenColorMap());

    /** To avoid instantiation */
    private LogsDownloaderWorkerGUIUtil() {
    }

    /**
     * Updates the {@link LogFolderInfo.State} of logFolder and updates the 
     * logFolderList if is in tis GUI.
     * 
     * @param logFolder
     * @param logFolderList
     */
    static void updateLogFolderState(LogFolderInfo logFolder, LogFolderInfoList logFolderList) {
        LogFolderInfo.State lfdState = logFolder.getState();
        LogFolderInfo.State lfdStateTmp = LogFolderInfo.State.UNKNOWN;
        long nTotal = 0, nDownloading = 0, nError = 0, nNew = 0, nIncomplete = 0, nLocal = 0, nSync = 0, nUnknown = 0;
        for (LogFileInfo tlfx : logFolder.getLogFiles()) {
            nTotal++;
            switch (tlfx.getState()) {
                case DOWNLOADING:
                    nDownloading++;
                    break;
                case ERROR:
                    nError++;
                    break;
                case NEW:
                    nNew++;
                    break;
                case SYNC:
                    nSync++;
                    break;
                case INCOMPLETE:
                    nIncomplete++;
                    break;
                case UNKNOWN:
                    nUnknown++;
                    break;
                case LOCAL:
                    nLocal++;
                    break;
            }
        }

        if (nDownloading > 0) {
            logFolder.setState(LogFolderInfo.State.DOWNLOADING);
        }
        else if (nError > 0) {
            logFolder.setState(LogFolderInfo.State.ERROR);
        }
        else if (nSync == nTotal) {
            logFolder.setState(LogFolderInfo.State.SYNC);
        }
        else if (nNew + nLocal == nTotal) {
            logFolder.setState(LogFolderInfo.State.NEW);
        }
        else if (nSync + nIncomplete + nUnknown + nNew + nLocal == nTotal) {
            logFolder.setState(LogFolderInfo.State.INCOMPLETE);
        }
        else if (nLocal == nTotal) {
            logFolder.setState(LogFolderInfo.State.LOCAL);
        }
        else if (nNew == nTotal) {
            logFolder.setState(LogFolderInfo.State.NEW);
        }
        else {
            logFolder.setState(LogFolderInfo.State.UNKNOWN);
        }
        lfdStateTmp = logFolder.getState();

        if (lfdState != lfdStateTmp) {
            if (logFolderList.containsFolder(logFolder)) {
                logFolderList.revalidate();
                logFolderList.repaint();
            }
        }
    }

    /**
     * Updates the logFolderList state icons as well as the summary logFoldersListLabel.
     * 
     * @param logFolderList
     * @param logFoldersListLabel
     */
    static void updateLogStateIconForAllLogFolders(LogFolderInfoList logFolderList,
            JXLabel logFoldersListLabel) {
        Object[] objArray = new Object[logFolderList.myModel.size()];
        logFolderList.myModel.copyInto(objArray);
        long nTotal = 0, nDownloading = 0, nError = 0, nNew = 0, nIncomplete = 0, nLocal = 0, nSync = 0, nUnknown = 0;
        for (Object comp : objArray) {
            LogFolderInfo log = (LogFolderInfo) comp;
            nTotal++;
            switch (log.getState()) {
                case DOWNLOADING:
                    nDownloading++;
                    break;
                case ERROR:
                    nError++;
                    break;
                case NEW:
                    nNew++;
                    break;
                case SYNC:
                    nSync++;
                    break;
                case INCOMPLETE:
                    nIncomplete++;
                    break;
                case UNKNOWN:
                    nUnknown++;
                    break;
                case LOCAL:
                    nLocal++;
                    break;
            }
        }

        if (objArray.length == 0) {
            logFoldersListLabel.setIcon(null);
        }
        else if (nDownloading > 0) {
            logFoldersListLabel.setIcon(LogFolderInfoList.ICON_DOWN);
        }
        else if (nError > 0) {
            logFoldersListLabel.setIcon(LogFolderInfoList.ICON_ERROR);
        }
        else if (nSync == nTotal) {
            logFoldersListLabel.setIcon(LogFolderInfoList.ICON_SYNC);
        }
        else if (nNew + nLocal == nTotal) {
            logFoldersListLabel.setIcon(LogFolderInfoList.ICON_NEW);
        }
        else if (nSync + nIncomplete + nUnknown + nNew + nLocal == nTotal) {
            logFoldersListLabel.setIcon(LogFolderInfoList.ICON_INCOMP);
        }
        else if (nLocal == nTotal) {
            logFoldersListLabel.setIcon(LogFolderInfoList.ICON_LOCAL);
        }
        else if (nNew == nTotal) {
            logFoldersListLabel.setIcon(LogFolderInfoList.ICON_NEW);
        }
        else {
            logFoldersListLabel.setIcon(LogFolderInfoList.ICON_UNKNOWN);
        }
    }

    /**
     * Used to update the just deleted files from {@link #deleteSelectedLogFoldersAction} or
     * {@link #deleteSelectedLogFilesAction}.
     * 
     * @param logFiles
     * @param downloadWorkersHolder
     * @param dirBaseToStoreFiles
     * @param logLabel
     * @return
     */
    static LinkedHashSet<LogFileInfo> updateLogFilesStateDeleted(LinkedHashSet<LogFileInfo> logFiles,
            JPanel downloadWorkersHolder, String dirBaseToStoreFiles, String logLabel) {
        LinkedHashSet<LogFileInfo> toDelFL = new LinkedHashSet<LogFileInfo>();
        for (LogFileInfo lfx : logFiles) {
            lfx.setState(LogFolderInfo.State.LOCAL);

            Component[] components = downloadWorkersHolder.getComponents();
            for (Component cp : components) {
                try {
                    DownloaderPanel dpp = (DownloaderPanel) cp;
                    // NeptusLog.pub().info("<###>........... "+dpp.getName());
                    if (lfx.getName().equals(dpp.getUri())) {
                        // NeptusLog.pub().info("<###>...........");
                        if (dpp.getState() == DownloaderPanel.State.WORKING
                                || dpp.getState() == DownloaderPanel.State.QUEUED) {
                            dpp.addStateChangeListener(null);
                            dpp.actionStop();
                            final DownloaderPanel workerDFinal = dpp;
                            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                                @Override
                                protected Void doInBackground() throws Exception {
                                    if (workerDFinal.getState() == DownloaderPanel.State.IDLE) {
                                        workerDFinal.doStopAndInvalidate();
                                        downloadWorkersHolder.remove(workerDFinal);
                                        downloadWorkersHolder.revalidate();
                                        downloadWorkersHolder.repaint();
                                    }
                                    return null;
                                }
                            };
                            worker.execute();
                        }
                        break;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!LogsDownloaderWorkerUtil.getFileTarget(lfx.getName(), dirBaseToStoreFiles, logLabel).exists()) {
                toDelFL.add(lfx);
                // logFd.getLogFiles().remove(lfx); //This cannot be done here
            }
            lfx.setState(LogFolderInfo.State.LOCAL);
        }
        return toDelFL;
    }

    /**
     * Creates the {@link Runnable} task to be schedule to update the local disk space info.
     * 
     * @param worker
     * @param gui
     * @param queueWorkTickets
     * @return
     */
    static Runnable createTimerTaskLocalDiskSpace(LogsDownloaderWorker worker,
            LogsDownloaderWorkerGUI gui, QueueWorkTickets<DownloaderPanel> queueWorkTickets) {
        Runnable ttaskLocalDiskSpace = new Runnable() {
            @Override
            public void run() {
                try {
                    File fxD = new File(worker.getDirBaseToStoreFiles());
                    long tspace = fxD.getTotalSpace();
                    long uspace = fxD.getUsableSpace();
                    if (tspace != 0 /* && uspace != 0 */) {
                        String tSpStr = MathMiscUtils.parseToEngineeringRadix2Notation(tspace, 2) + "B";
                        String uSpStr = MathMiscUtils.parseToEngineeringRadix2Notation(uspace, 2) + "B";
                        double pFree = 1.0 * (tspace - uspace) / tspace;
                        gui.diskFreeLabel.setText("<html><b>" + uSpStr);
                        gui.diskFreeLabel.setToolTipText(I18n.textf("Local free disk space %usedspace of %totalspace",
                                uSpStr, tSpStr));
                        gui.updateDiskFreeLabelBackColor(diskFreeColorMap.getColor(pFree));
                        return;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                gui.diskFreeLabel.setText("<html><b>?");
                gui.diskFreeLabel.setToolTipText(I18n.text("Unknown local disk free space"));
                gui.updateDiskFreeLabelBackColor(Color.LIGHT_GRAY);

                // Queue block test
                ArrayList<DownloaderPanel> workingDonsloaders = queueWorkTickets.getAllWorkingClients();
                Component[] components = gui.downloadWorkersHolder.getComponents();
                for (Component cp : components) {
                    if (!(cp instanceof DownloaderPanel))
                        continue;

                    DownloaderPanel workerD = (DownloaderPanel) cp;
                    if (workingDonsloaders.contains(workerD))
                        workingDonsloaders.remove(workerD);
                }
                for (DownloaderPanel cp : workingDonsloaders) {
                    queueWorkTickets.release(cp);
                    NeptusLog.pub().error(cp.getUri() + " should not be holding the lock (forcing release)! State: " + cp.getState());
                }
            }
        };
        return ttaskLocalDiskSpace;
    }
}
