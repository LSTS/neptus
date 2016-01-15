/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 15/01/2016
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.net.ftp.FTPFile;
import org.jdesktop.swingx.JXCollapsiblePane;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
class LogsDownloaderWorkerActions {

    private LogsDownloaderWorker worker;
    private LogsDownloaderWorkerGUI gui;
    
    // Actions
    AbstractAction downloadListAction = null;
    AbstractAction downloadSelectedLogDirsAction = null;
    AbstractAction downloadSelectedLogFilesAction = null;
    AbstractAction deleteSelectedLogFoldersAction = null;
    AbstractAction deleteSelectedLogFilesAction = null;
    AbstractAction toggleConfPanelAction = null;
    AbstractAction toggleExtraInfoPanelAction = null;
    AbstractAction helpAction = null;
    AbstractAction resetAction = null;
    AbstractAction stopAllAction = null;
    AbstractAction turnCameraOn = null;

    public LogsDownloaderWorkerActions(LogsDownloaderWorker worker, LogsDownloaderWorkerGUI gui) {
        this.worker = worker;
        this.gui = gui;
        initializeActions();
    }

    @SuppressWarnings("serial")
    private void initializeActions() {
        downloadListAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        if (stopLogListProcessing)
                            stopLogListProcessing = false;

                        long time = System.currentTimeMillis();
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                gui.listHandlingProgressBar.setValue(0);
                                gui.listHandlingProgressBar.setString(I18n.text("Starting..."));
                            }
                        });

                        gui.downloadListButton.setEnabled(false);
                        // logFolderList.setEnabled(false);
                        gui.logFolderList.setValueIsAdjusting(true);
                        // logFilesList.setEnabled(false);

                        // ->Getting txt list of logs from server
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                gui.listHandlingProgressBar.setValue(10);
                                gui.listHandlingProgressBar.setIndeterminate(true);
                                gui.listHandlingProgressBar.setString(I18n
                                        .text("Connecting to remote system for log list update..."));
                            }
                        });

                        LinkedHashMap<FTPFile, String> retList = null;
                        LinkedHashMap<String, String> serversLogPresenceList = new LinkedHashMap<>(); 

                        long timeD1 = System.currentTimeMillis();
                        // Getting the file list from main CPU
                        try {
                            clientFtp = LogsDownloaderUtil.getOrRenewFtpDownloader(clientFtp, worker.getHost(), worker.getPort());

                            retList = clientFtp.listLogs();

                            for (String partialUri : retList.values()) {
                                serversLogPresenceList.put(partialUri, LogsDownloaderWorker.SERVER_MAIN);
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error("Connecting with " + worker.getHost() + ":" + worker.getPort() + " with error: " + e.getMessage());
                        }
                        NeptusLog.pub().warn(".......get list from main CPU server " + (System.currentTimeMillis() - timeD1) + "ms");                        

                        long timeD2 = System.currentTimeMillis();
                        //Getting the log list from Camera CPU
                        String cameraHost = LogsDownloaderUtil.getCameraHost(worker.getHost());
                        if (cameraHost.length() > 0 && isCamCpuOn()) {
                            LinkedHashMap<FTPFile, String> retCamList = null;
                            try {
                                cameraFtp = LogsDownloaderUtil.getOrRenewFtpDownloader(cameraFtp, cameraHost, worker.getPort());
                                retCamList = cameraFtp.listLogs();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error("Connecting with " + cameraHost + ":" + worker.getPort() + " with error: " + e.getMessage());
                            }
                            if (retCamList != null) {
                                if (retList == null) {
                                    retList = retCamList;

                                    for (String partialUri : retList.values()) {
                                        serversLogPresenceList.put(partialUri, LogsDownloaderWorker.SERVER_CAM);
                                    }
                                }
                                else {
                                    for (FTPFile camFTPFile : retCamList.keySet()) {
                                        String val = retCamList.get(camFTPFile);
                                        if (retList.containsValue(val)) {
                                            serversLogPresenceList.put(val, serversLogPresenceList.get(val) + " " + LogsDownloaderWorker.SERVER_CAM);
                                            continue;
                                        }
                                        else {
                                            retList.put(camFTPFile, val);
                                            serversLogPresenceList.put(val, LogsDownloaderWorker.SERVER_CAM);
                                        }
                                    }
                                }
                            }
                            NeptusLog.pub().warn(".......get list from main CAM server " + (System.currentTimeMillis() - timeD2) + "ms");                        
                        }

                        NeptusLog.pub().warn(".......get list from all servers " + (System.currentTimeMillis() - timeD1) + "ms");                        
                        if (retList == null) {
                            gui.msgPanel.writeMessageTextln(I18n.text("Done"));
                            return null;
                        }

                        gui.msgPanel.writeMessageTextln(I18n.textf("Log Folders: %numberoffolders", retList.size()));

                        long timeD3 = System.currentTimeMillis();
                        
                        // Added in order not to show the active log (the last one
                        if (retList.size() > 0) {
                            String[] ordList = retList.values().toArray(new String[retList.size()]);
                            Arrays.sort(ordList);
                            String activeLogName = ordList[ordList.length - 1];
                            for (FTPFile fFile : retList.keySet().toArray(new FTPFile[retList.size()])) {
                                if (retList.get(fFile).equals(activeLogName)) {
                                    retList.remove(fFile);
                                    break;
                                }
                            }
                        }

                        if (retList.size() == 0) {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    gui.listHandlingProgressBar.setValue(100);
                                    gui.listHandlingProgressBar.setIndeterminate(false);
                                    gui.listHandlingProgressBar.setString(I18n.text("No logs..."));
                                }
                            });
                            return null;
                        }
                        else {
                            final String msg1 = I18n.textf("Log Folders: %numberoffolders", retList.size());
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    // listHandlingProgressBar.setValue(10);
                                    // listHandlingProgressBar.setIndeterminate(true);
                                    gui.listHandlingProgressBar.setString(msg1);
                                }
                            });
                        }

                        // ->Removing from already existing LogFolders to LOCAL state
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                gui.listHandlingProgressBar.setValue(20);
                                gui.listHandlingProgressBar.setIndeterminate(false);
                                gui.listHandlingProgressBar.setString(I18n.text("Filtering list..."));
                            }
                        });
                        long timeC1 = System.currentTimeMillis();
                        Object[] objArray = new Object[gui.logFolderList.myModel.size()];
                        gui.logFolderList.myModel.copyInto(objArray);
                        for (Object comp : objArray) {
                            if (stopLogListProcessing)
                                return null;

                            try {
                                // NeptusLog.pub().info("<###>... upda
                                LogFolderInfo log = (LogFolderInfo) comp;
                                if (!retList.containsValue(log.getName())) {
                                    // retList.remove(log.getName());
                                    for (LogFileInfo lfx : log.getLogFiles()) {
                                        if (stopLogListProcessing)
                                            return null;
                                        lfx.setState(LogFolderInfo.State.LOCAL);
                                    }
                                    log.setState(LogFolderInfo.State.LOCAL);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }
                        }
                        NeptusLog.pub().warn(".......Removing from already existing LogFolders to LOCAL state "
                                + (System.currentTimeMillis() - timeC1) + "ms");

                        // ->Adding new LogFolders
                        LinkedList<LogFolderInfo> existenteLogFoldersFromServer = new LinkedList<LogFolderInfo>();
                        LinkedList<LogFolderInfo> newLogFoldersFromServer = new LinkedList<LogFolderInfo>();
                        for (String newLogName : retList.values()) {
                            if (stopLogListProcessing)
                                return null;

                            final LogFolderInfo newLogDir = new LogFolderInfo(newLogName);
                            if (gui.logFolderList.containsFolder(newLogDir)) {
                                existenteLogFoldersFromServer.add(gui.logFolderList.getFolder((newLogDir.getName())));
                            }
                            else {
                                newLogFoldersFromServer.add(newLogDir);
                                SwingUtilities.invokeAndWait(new Runnable() {
                                    @Override
                                    public void run() {
                                        gui.logFolderList.addFolder(newLogDir);
                                    }
                                });
                            }
                        }
                        // msgPanel.writeMessageTextln("Logs Folders: " + logFolderList.myModel.size());

                        // ->Getting Log files list from server
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                gui.listHandlingProgressBar.setValue(30);
                                gui.listHandlingProgressBar.setIndeterminate(true);
                                gui.listHandlingProgressBar.setString(I18n
                                        .text("Contacting remote system for complete log file list..."));

                                gui.listHandlingProgressBar.setValue(40);
                                gui.listHandlingProgressBar.setIndeterminate(false);
                                gui.listHandlingProgressBar.setString(I18n.text("Processing log list..."));
                            }
                        });

                        objArray = new Object[gui.logFolderList.myModel.size()];
                        gui.logFolderList.myModel.copyInto(objArray);

                        long timeF0 = System.currentTimeMillis();
                        LinkedList<LogFolderInfo> tmpLogFolderList = getLogFileList(serversLogPresenceList);
                        NeptusLog.pub().warn(".......Contacting remote system for complete log file list " +
                                (System.currentTimeMillis() - timeF0) + "ms");

                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                gui.listHandlingProgressBar.setValue(70);
                                gui.listHandlingProgressBar.setIndeterminate(false);
                                gui.listHandlingProgressBar.setString(I18n.text("Updating logs info..."));
                            }
                        });

                        long timeF1 = System.currentTimeMillis();
                        // Testing for log files from each log folder
                        for (Object comp : objArray) {
                            if (stopLogListProcessing)
                                return null;

                            try {
                                LogFolderInfo logFolder = (LogFolderInfo) comp;

                                int indexLFolder = tmpLogFolderList.indexOf(logFolder);
                                LinkedHashSet<LogFileInfo> logFilesTmp = (indexLFolder != -1) ? tmpLogFolderList.get(
                                        indexLFolder).getLogFiles() : new LinkedHashSet<LogFileInfo>();
                                        for (LogFileInfo logFx : logFilesTmp) {
                                            if (stopLogListProcessing)
                                                return null;

                                            if (!logFolder.getLogFiles().contains(logFx)) {
                                                // The file or directory is new
                                                logFolder.addFile(logFx);
                                            }
                                            else {
                                                // The file or directory is already known so let us update
                                                LogFileInfo lfx = logFolder.getLogFile(logFx.getName()/* fxStr */);
                                                if (lfx.getSize() == -1) {
                                                    lfx.setSize(logFx.getSize());
                                                }
                                                else if (lfx.getSize() != logFx.getSize()) {
                                                    System.out.println("//////////// " + lfx.getSize() + "  " + logFx.getSize());
                                                    if (lfx.getState() == LogFolderInfo.State.SYNC)
                                                        lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                    else if (lfx.getState() == LogFolderInfo.State.LOCAL)
                                                        lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                    lfx.setSize(logFx.getSize());
                                                    lfx.setFile(logFx.getFile());
                                                }
                                                else if (lfx.getSize() == logFx.getSize()) {
                                                    if (lfx.getState() == LogFolderInfo.State.LOCAL)
                                                        lfx.setState(LogFolderInfo.State.SYNC);
                                                }
                                                lfx.setHost(logFx.getHost());

                                                if (logFx.isDirectory()) {
                                                    ArrayList<LogFileInfo> notMatchElements = new ArrayList<>();
                                                    notMatchElements.addAll(lfx.getDirectoryContents());
                                                    for (LogFileInfo lfi : logFx.getDirectoryContents()) {
                                                        boolean alreadyExists = false;
                                                        for (LogFileInfo lfiLocal : lfx.getDirectoryContents()) {
                                                            if (lfi.equals(lfiLocal)) {
                                                                alreadyExists = true;
                                                                notMatchElements.remove(lfiLocal);
                                                                lfi.setSize(lfiLocal.getSize());
                                                                lfi.setFile(lfiLocal.getFile());
                                                                lfi.setHost(lfiLocal.getHost());
                                                            }
                                                        }
                                                        if (!alreadyExists) {
                                                            lfx.getDirectoryContents().add(lfi);
                                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                        }
                                                    }
                                                    for (LogFileInfo lfi : notMatchElements) {
                                                        lfx.getDirectoryContents().remove(lfi);
                                                    }
                                                }

                                                if (lfx.isDirectory()) {
                                                    if (!LogsDownloaderUtil.getFileTarget(lfx.getName(), worker.getDirBaseToStoreFiles(), worker.getLogLabel()).exists()) {
                                                        for (LogFileInfo lfi : lfx.getDirectoryContents()) {
                                                            if (!LogsDownloaderUtil.getFileTarget(lfi.getName(), worker.getDirBaseToStoreFiles(), worker.getLogLabel()).exists()) {
                                                                if (lfx.getState() != LogFolderInfo.State.NEW && lfx.getState() != LogFolderInfo.State.DOWNLOADING)
                                                                    lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    else {
                                                        long sizeD = LogsDownloaderUtil.getDiskSizeFromLocal(lfx, worker);
                                                        if (lfx.getSize() != sizeD && lfx.getState() == LogFolderInfo.State.SYNC)
                                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                    }
                                                }
                                                else {
                                                    if (!LogsDownloaderUtil.getFileTarget(lfx.getName(), worker.getDirBaseToStoreFiles(), worker.getLogLabel()).exists()) {
                                                        if (lfx.getState() != LogFolderInfo.State.NEW && lfx.getState() != LogFolderInfo.State.DOWNLOADING) {
                                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                            // System.out.println("//////////// " + lfx.getName() + "  " + LogsDownloaderUtil.getFileTarget(lfx.getName()).exists());
                                                        }
                                                    }
                                                    else {
                                                        long sizeD = LogsDownloaderUtil.getDiskSizeFromLocal(lfx, worker);
                                                        if (lfx.getSize() != sizeD && lfx.getState() == LogFolderInfo.State.SYNC)
                                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                    }
                                                }
                                            }
                                        }

                                        // Put LOCAL state on files not in server
                                        LinkedHashSet<LogFileInfo> toDelFL = new LinkedHashSet<LogFileInfo>();
                                        for (LogFileInfo lfx : logFolder.getLogFiles()) {
                                            if (!logFilesTmp.contains(lfx)
                                                    /* !res.keySet().contains(lfx.getName()) */) {
                                                lfx.setState(LogFolderInfo.State.LOCAL);
                                                if (!LogsDownloaderUtil.getFileTarget(lfx.getName(), 
                                                        worker.getDirBaseToStoreFiles(), worker.getLogLabel()).exists()) {
                                                    toDelFL.add(lfx);
                                                    // logFolder.getLogFiles().remove(lfx); //This cannot be done here
                                                }
                                            }
                                        }
                                        for (LogFileInfo lfx : toDelFL)
                                            logFolder.getLogFiles().remove(lfx);
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }
                        }
                        NeptusLog.pub().warn(".......Testing for log files from each log folder " +
                                (System.currentTimeMillis() - timeF1) + "ms");

                        long timeF2 = System.currentTimeMillis();
                        testNewReportedLogFoldersForLocalCorrespondent(newLogFoldersFromServer);
                        for (LogFolderInfo logFolder : existenteLogFoldersFromServer) {
                            updateLogFolderState(logFolder);
                        }
                        updateLogStateIconForAllLogFolders();
                        NeptusLog.pub().warn(".......Updating LogFolders State " +
                                (System.currentTimeMillis() - timeF2) + "ms");

                        long timeF3 = System.currentTimeMillis();
                        // updateFilesListGUIForFolderSelected();
                        new Thread("updateFilesListGUIForFolderSelected") {
                            @Override
                            public void run() {
                                updateFilesListGUIForFolderSelected();
                            };
                        }.start();
                        NeptusLog.pub().warn(".......updateFilesListGUIForFolderSelected " +
                                (System.currentTimeMillis() - timeF3) + "ms");

                        NeptusLog.pub().warn("....process list from all servers " + (System.currentTimeMillis() - timeD3) + "ms");                        

                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                gui.listHandlingProgressBar.setValue(90);
                                gui.listHandlingProgressBar.setIndeterminate(false);
                                gui.listHandlingProgressBar.setString(I18n.text("Updating GUI..."));
                            }
                        });
                        gui.logFolderList.invalidate();
                        gui.logFolderList.revalidate();
                        gui.logFolderList.repaint();
                        gui.logFolderList.setEnabled(true);
                        // logFilesList.invalidate();
                        // logFilesList.revalidate();
                        // logFilesList.repaint();
                        gui.logFilesList.setEnabled(true);

                        NeptusLog.pub().warn("....all downloadListAction " + (System.currentTimeMillis() - time) + "ms");
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                gui.listHandlingProgressBar.setValue(100);
                                gui.listHandlingProgressBar.setIndeterminate(false);
                                gui.listHandlingProgressBar.setString(I18n.text("Done"));
                            }
                        });
                        return true;
                    }

                    @Override
                    public void finish() {
                        stopLogListProcessing = false;

                        gui.logFolderList.setValueIsAdjusting(false);
                        gui.logFolderList.invalidate();
                        gui.logFolderList.revalidate();
                        gui.logFolderList.repaint();
                        gui.logFolderList.setEnabled(true);
                        // logFilesList.invalidate();
                        // logFilesList.revalidate();
                        // logFilesList.repaint();
                        gui.listHandlingProgressBar.setValue(0);
                        gui.listHandlingProgressBar.setIndeterminate(false);
                        gui.listHandlingProgressBar.setString("");
                        gui.logFilesList.setEnabled(true);
                        gui.downloadListButton.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                AsyncWorker.getWorkerThread().postTask(task);
            }
        };

        downloadSelectedLogDirsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                gui.downloadSelectedLogDirsButton.setEnabled(false);
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        for (Object comp : gui.logFolderList.getSelectedValues()) {
                            try {
                                // NeptusLog.pub().info("<###>... updateFilesForFolderSelected");
                                LogFolderInfo logFd = (LogFolderInfo) comp;
                                for (LogFileInfo lfx : logFd.getLogFiles()) {
                                    // if (downloadSelectedLogDirsButton.isEnabled())
                                    //      break; // If button enabled a reset was called, so let's interrupt all 
                                    if (resetting)
                                        break;

                                    singleLogFileDownloadWorker(lfx, logFd);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }

                            if (resetting)
                                break;
                        }
                        return true;
                    }

                    @Override
                    public void finish() {
                        gui.downloadSelectedLogDirsButton.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                AsyncWorker.getWorkerThread().postTask(task);
            }
        };

        downloadSelectedLogFilesAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        gui.downloadSelectedLogFilesButton.setEnabled(false);

                        for (Object comp : gui.logFilesList.getSelectedValues()) {
                            if (resetting)
                                break;

                            try {
                                LogFileInfo lfx = (LogFileInfo) comp;
                                singleLogFileDownloadWorker(lfx, findLogFolderInfoForFile(lfx));
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }
                        }
                        return true;
                    }

                    @Override
                    public void finish() {
                        gui.downloadSelectedLogFilesButton.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                AsyncWorker.getWorkerThread().postTask(task);
            }
        };

        deleteSelectedLogFoldersAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        gui.deleteSelectedLogFoldersButton.setEnabled(false);
                        // logFolderList.setEnabled(false);
                        // logFilesList.setEnabled(false);

                        Object[] objArray = gui.logFolderList.getSelectedValues();
                        if (objArray.length == 0)
                            return null;

                        JOptionPane jop = new JOptionPane(I18n.text("Are you sure you want to delete "
                                + "selected log folders from remote system?"), JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.YES_NO_OPTION);
                        JDialog dialog = jop.createDialog(gui.frameCompHolder, I18n.text("Remote Delete Confirmation"));
                        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                        dialog.setVisible(true);
                        Object userChoice = jop.getValue();
                        try {
                            if (((Integer) userChoice) != JOptionPane.YES_OPTION) {
                                return null;
                            }
                        }
                        catch (Exception e2) {
                            NeptusLog.pub().error(e2.getMessage());
                            return null;
                        }
                        gui.deleteSelectedLogFoldersButton.setEnabled(true);
                        for (Object comp : objArray) {
                            try {
                                LogFolderInfo logFd = (LogFolderInfo) comp;
                                boolean resDel = deleteLogFolderFromServer(logFd);
                                if (resDel) {
                                    logFd.setState(LogFolderInfo.State.LOCAL);
                                    LinkedHashSet<LogFileInfo> logFiles = logFd.getLogFiles();

                                    LinkedHashSet<LogFileInfo> toDelFL = updateLogFilesStateDeleted(logFiles);
                                    for (LogFileInfo lfx : toDelFL) {
                                        if (resetting)
                                            break;

                                        logFd.getLogFiles().remove(lfx);
                                    }
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }

                            if (resetting)
                                break;
                        }
                        updateFilesListGUIForFolderSelected();
                        return true;
                    }

                    @Override
                    public void finish() {
                        gui.deleteSelectedLogFoldersButton.setEnabled(true);
                        gui.logFilesList.revalidate();
                        gui.logFilesList.repaint();
                        gui.logFilesList.setEnabled(true);
                        gui.logFolderList.revalidate();
                        gui.logFolderList.repaint();
                        gui.logFolderList.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                AsyncWorker.getWorkerThread().postTask(task);
            }
        };

        deleteSelectedLogFilesAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        gui.deleteSelectedLogFilesButton.setEnabled(false);

                        Object[] objArray = gui.logFilesList.getSelectedValues();
                        if (objArray.length == 0)
                            return null;

                        JOptionPane jop = new JOptionPane(
                                I18n.text("Are you sure you want to delete selected log files from remote system?"),
                                JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
                        JDialog dialog = jop.createDialog(gui.frameCompHolder, I18n.text("Remote Delete Confirmation"));
                        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                        dialog.setVisible(true);
                        Object userChoice = jop.getValue();
                        try {
                            if (((Integer) userChoice) != JOptionPane.YES_OPTION) {
                                return null;
                            }
                        }
                        catch (Exception e2) {
                            NeptusLog.pub().error(e2.getMessage());
                            return null;
                        }
                        deleteSelectedLogFoldersButton.setEnabled(true);

                        LinkedHashSet<LogFileInfo> logFiles = new LinkedHashSet<LogFileInfo>();
                        for (Object comp : objArray) {
                            if (resetting)
                                break;

                            try {
                                LogFileInfo lfx = (LogFileInfo) comp;
                                if (deleteLogFileFromServer(lfx))
                                    logFiles.add(lfx);
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }
                        }
                        if (!resetting) {
                            updateLogFilesStateDeleted(logFiles);

                            updateFilesListGUIForFolderSelected();
                        }
                        return true;
                    }

                    @Override
                    public void finish() {
                        gui.deleteSelectedLogFilesButton.setEnabled(true);
                        gui.logFilesList.revalidate();
                        gui.logFilesList.repaint();
                        gui.logFilesList.setEnabled(true);
                        gui.logFolderList.revalidate();
                        gui.logFolderList.repaint();
                        gui.logFolderList.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                AsyncWorker.getWorkerThread().postTask(task);
            }
        };

        toggleConfPanelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.configCollapsiblePanel.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION).actionPerformed(e);
            }
        };

        toggleExtraInfoPanelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.extraInfoCollapsiblePanel.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION).actionPerformed(e);
            }
        };

        helpAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GuiUtils.centerOnScreen(gui.downHelpDialog.getDialog());
                gui.downHelpDialog.getDialog().setIconImage(LogsDownloaderWorkerGUI.ICON_HELP.getImage());
                gui.downHelpDialog.getDialog().setVisible(true);
            }
        };

        resetAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.resetButton.setEnabled(false);
                doReset(false);
            }
        };

        stopAllAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.stopAllButton.setEnabled(false);
                doReset(true);
            }
        };

        turnCameraOn = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ArrayList<EntityParameter> propList = new ArrayList<>();
                    EntityParameter entParsm = new EntityParameter().setName("Active")
                            .setValue(gui.cameraButton.getBackground() != LogsDownloaderWorker.CAM_CPU_ON_COLOR ? "true"
                                    : "false");
                    propList.add(entParsm);
                    SetEntityParameters setParams = new SetEntityParameters();
                    setParams.setName(LogsDownloaderWorker.CAMERA_CPU_LABEL);
                    setParams.setParams(propList);

                    ImcMsgManager.getManager().sendMessageToSystem(setParams, worker.getLogLabel());
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
    }

}
