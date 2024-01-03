/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2009/09/12
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.ftp.FtpDownloader;
import pt.lsts.neptus.gui.NudgeGlassPane;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.concurrency.QueueWorkTickets;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * This is the log downloader worker panel. You can put it into an external frame using
 * the proper constructor. In this case you HAVE TO CALL the {@link #cleanup()}.
 * 
 * @author pdias
 * 
 */
public class LogsDownloaderWorker {

    protected static final Color CAM_CPU_ON_COLOR = Color.GREEN;
    private static final int ACTIVE_DOWNLOADS_QUEUE_SIZE = 1;
    static final String SERVER_MAIN = "main";
    static final String SERVER_CAM = "cam";
    // static final String SERVER_CAM2 = "cam2";

    static final int DEFAULT_PORT = 30021;

    private static final String DEFAULT_TITLE = I18n.text("Download Log Files");
    protected static final long DELTA_TIME_TO_CLEAR_DONE = 5000;
    protected static final long DELTA_TIME_TO_CLEAR_NOT_WORKING = 45000;

    protected static final String CAMERA_CPU_LABEL = "Slave CPU";

    private final ArrayList<String> serversList = new ArrayList<>(2);
    private final LinkedHashMap<String, FtpDownloader> ftpDownloaders = new LinkedHashMap<>(2);

    private String host = "127.0.0.1";
    private int port = DEFAULT_PORT;

    private String dirBaseToStoreFiles = "log/downloaded";

    private String logLabel = I18n.text("unknown"); // This should be a word with no spaces

    public final List<String> serverAvailabilityForListing = new ArrayList<>();

    private LogsDownloaderWorkerGUI gui = null;
    private LogsDownloaderWorkerActions actions = null;

    private ScheduledThreadPoolExecutor threadScheduledPool = null;
    private Runnable ttaskLocalDiskSpace = null;

    private MessageListener<MessageInfo, IMCMessage> messageListener;

    // Variable used in updateFilesListGUIForFolderSelected method
    private boolean isUpdatingFileList = false;
    private boolean exitRequest = false;
    private final Object lock = new Object();

    private QueueWorkTickets<DownloaderPanel> queueWorkTickets = new QueueWorkTickets<>(ACTIVE_DOWNLOADS_QUEUE_SIZE);

    /**
     * This will create a panel and a frame to control the logs downloading. Use {@link #setVisible(boolean)} to show
     * the frame.
     */
    public LogsDownloaderWorker() {
        this(null);
    }

    /**
     * If a parent frame is given, it only be used for parent dialogs and related, the created panel will not be added
     * to it (use in this case {@link #getContentPanel()} to get the content panel). <br>
     * The {@link #setVisible(boolean)} will work the same.
     */
    public LogsDownloaderWorker(JFrame parentFrame) {
        initialize(parentFrame);
    }

    private void initialize(JFrame parentFrame) {
        // Filling servers list
        serversList.add(SERVER_MAIN);
        serversList.add(SERVER_CAM);
        // serversList.add(SERVER_CAM2);

        // Init timer
        threadScheduledPool = LogsDownloaderWorkerUtil.createThreadPool(LogsDownloaderWorker.this);
        
        initializeGUI(parentFrame);

        // Register for EntityActivationState
        messageListener = LogsDownloaderWorkerUtil.createEntityStateMessageListener(LogsDownloaderWorker.this,
                gui.cameraButton);
        ImcMsgManager.getManager().addListener(messageListener); // all systems listener
    }

    private void initializeGUI(JFrame parentFrame) {
        gui = new LogsDownloaderWorkerGUI(this, parentFrame);
        actions = new LogsDownloaderWorkerActions(this, gui);
        
        if (gui.frame == null) {
            gui.frame.setTitle(DEFAULT_TITLE + " - " + logLabel);
            gui.frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    super.windowClosed(e);
                    cleanup();
                }
            });
        }

        gui.logFolderList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        updateFilesListGUIForFolderSelected();
                        return null;
                    }

                    @Override
                    public void finish() {
                        gui.logFilesList.setValueIsAdjusting(false);
                        gui.logFilesList.invalidate();
                        gui.logFilesList.validate();
                        gui.logFilesList.setEnabled(true);
                    }
                };
                AsyncWorker.getWorkerThread().postTask(task);
            }
        });
        gui.logFolderList.addMouseListener(
                LogsDownloaderWorkerUtil.createOpenLogInMRAMouseListener(LogsDownloaderWorker.this, gui.logFolderList));

        setEnableLogLabel(false);

        setEnableHost(true);

        if (!gui.frameIsExternalControlled)
            GuiUtils.centerOnScreen(gui.frame);

        ttaskLocalDiskSpace = LogsDownloaderWorkerGUIUtil.createTimerTaskLocalDiskSpace(this, gui, queueWorkTickets);
        threadScheduledPool.scheduleAtFixedRate(ttaskLocalDiskSpace, 500, 5000, TimeUnit.MILLISECONDS);
    }

    public static Icon getIcon() {
        return LogsDownloaderWorkerGUI.ICON_DOWNLOAD_FOLDERS;
    }

    public boolean validateConfiguration() {
        return gui.validateConfiguration();
    }

    /**
     * @return the content panel. Use it only if you give an external frame in the constructor.
     */
    public JXPanel getContentPanel() {
        return gui.frameCompHolder;
    }

    /**
     * This is used to clean and dispose safely of this component
     */
    public void cleanup() {
        disconnectFTPClientsForListing();

        if (threadScheduledPool != null) {
            threadScheduledPool.shutdownNow();
        }
        if (gui.frame != null) {
            if (!gui.frameIsExternalControlled) {
                gui.frame.dispose();
                gui.frame = null;
            }
            else {
                gui.frame = null;
            }
        }

        if (gui.downHelpDialog != null)
            gui.downHelpDialog.dispose();

        ImcMsgManager.getManager().removeListener(messageListener);

        queueWorkTickets.cancelAll();
    }

    private void disconnectFTPClientsForListing() {
        actions.stopLogListProcessing = true;

        for (FtpDownloader ftpDwnld : ftpDownloaders.values().toArray(new FtpDownloader[ftpDownloaders.size()])) {
            if (ftpDwnld != null && ftpDwnld.isConnected()) {
                try {
                    ftpDwnld.getClient().disconnect();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        cleanup();
    }

    ArrayList<String> getServersList() {
        return serversList;
    }
    
    LinkedHashMap<String, FtpDownloader> getFtpDownloaders() {
        return ftpDownloaders;
    }
    
    /**
     * Return the host for the serverKey
     * 
     * @param serverKey
     * @return
     */
    String getHostFor(String serverKey) {
        switch (serverKey) {
            case SERVER_MAIN:
                return getHost();
            case SERVER_CAM:
                return LogsDownloaderWorkerUtil.getCameraHost(getHost());
//            case SERVER_CAM2:
//                return LogsDownloaderWorkerUtil.getCameraHost2(getHost());
            default:
                break;
        }
        return null;
    }

    /**
     * Return the port for the serverKey
     * 
     * @param serverKey
     * @return
     */
    int getPortFor(String serverKey) {
        return getPort();
    }

    /**
     * Return if the server is available (may not be reachable).
     * Serves only the function to decide to try to contact or not.
     * 
     * @param serverKey
     * @return
     */
    boolean isServerAvailable(String serverKey) {
//        switch (serverKey) {
//            case SERVER_MAIN:
//            case SERVER_CAM:
//                return true;
//            case SERVER_CAM2:
//                return gui.cameraButton.getBackground() == LogsDownloaderWorker.CAM_CPU_ON_COLOR;
//            default:
//                return false;
//        }

        return serverAvailabilityForListing.contains(serverKey);
    }
    
    public String getHost() {
        return host;
    }

    /**
     * @param host
     */
    public void setHost(String host) {
        this.host = host;
        gui.hostField.setText(host);
    }

    /**
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
        gui.portField.setText("" + port);
    }

    public String getLogLabel() {
        return logLabel;
    }

    /**
     * @param logLabel
     */
    public void setLogLabel(String logLabel) {
        this.logLabel = logLabel;
        gui.logLabelField.setText(logLabel);
        if (!gui.frameIsExternalControlled)
            gui.frame.setTitle(DEFAULT_TITLE + " - " + logLabel);
    }

    /**
     * @param show
     */
    public void setVisible(boolean show) {
        gui.frame.setVisible(show);
        if (show)
            gui.frame.setState(Frame.NORMAL);
    }

    // FIXME Visibility
    void updateFilesListGUIForFolderSelected() {
        if (isUpdatingFileList)
            exitRequest = true;
        synchronized (lock) {
            isUpdatingFileList = true;
            exitRequest = false;

            gui.logFilesList.setValueIsAdjusting(true);

            final LinkedHashSet<LogFileInfo> validFiles = new LinkedHashSet<LogFileInfo>();
            for (Object comp : gui.logFolderList.getSelectedValues()) {
                try {
                    LogFolderInfo log = (LogFolderInfo) comp;
                    for (LogFileInfo lgfl : log.getLogFiles()) {
                        validFiles.add(lgfl);

                        if (exitRequest)
                            break;
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().debug(e.getMessage());
                }

                if (exitRequest)
                    break;
            }
            
            gui.logFilesList.setIgnoreRepaint(true);
            try {
                if (validFiles.isEmpty()) {
                    if (SwingUtilities.isEventDispatchThread()) {
                        if (exitRequest)
                            return;
                        gui.logFilesList.myModel.clear();
                    }
                    else {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                if (exitRequest)
                                    return;
                                gui.logFilesList.myModel.clear();
                            }
                        });
                    }
                }
                
                Object[] inListElmObjtAArray = gui.logFilesList.myModel.toArray();
                for (Object object : inListElmObjtAArray) {
                    final LogFileInfo fxS = (LogFileInfo) object;
                    if (!validFiles.contains(fxS)) {
                        if (SwingUtilities.isEventDispatchThread()) {
                            if (exitRequest)
                                break;
                            gui.logFilesList.removeFile(fxS);
                        }
                        else {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    if (exitRequest)
                                        return;
                                    gui.logFilesList.removeFile(fxS);
                                }
                            });
                        }
                        if (exitRequest)
                            break;
                    }
                }
                
                for (final LogFileInfo fxS : validFiles) {
                    if (SwingUtilities.isEventDispatchThread()) {
                        if (exitRequest)
                            break;
                        gui.logFilesList.addFile(fxS);
                    }
                    else {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                if (exitRequest)
                                    return;
                                gui.logFilesList.addFile(fxS);
                            }
                        });
                    }
                    if (exitRequest)
                        break;
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage());
            }
            finally {
                gui.logFilesList.setIgnoreRepaint(false);
                
                gui.logFilesList.setValueIsAdjusting(false);
                gui.logFilesList.invalidate();
                gui.logFilesList.validate();
                isUpdatingFileList = false;
            }
        }
    }

    /**
     * @param lfx
     * @param logFd
     */
    // FIXME Visibility
    void singleLogFileDownloadWorker(LogFileInfo lfx, LogFolderInfo logFd) {
        if (lfx.getState() == LogFolderInfo.State.SYNC // || lfx.getState() == LogFolderInfo.State.DOWNLOADING
                || lfx.getState() == LogFolderInfo.State.LOCAL) {
            return;
        }

        // Let us see if already exists in download list
        Component[] components = gui.downloadWorkersHolder.getComponents();
        for (Component cp : components) {
            try {
                DownloaderPanel dpp = (DownloaderPanel) cp;
                if (lfx.getName().equals(dpp.getUri())) {
                    if (dpp.getState() == DownloaderPanel.State.ERROR
                            || dpp.getState() == DownloaderPanel.State.IDLE
                            || dpp.getState() == DownloaderPanel.State.TIMEOUT
                            || dpp.getState() == DownloaderPanel.State.QUEUED
                            || dpp.getState() == DownloaderPanel.State.NOT_DONE) {
                        dpp.actionDownload();
                    }
                    return;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        DownloaderPanel workerD = null;

        try {
            String serverKey = lfx.getHost();
            String host = getHostFor(serverKey); // lfx.getHost();
            int port = getPortFor(serverKey); // this.port;
            FtpDownloader ftpDownloader = new FtpDownloader(host, port, false);

            if (lfx.isDirectory()) {
                HashMap<String, FTPFile> directoryContentsList = new LinkedHashMap<>();
                for (LogFileInfo lfi : lfx.getDirectoryContents()) {
                    directoryContentsList.put(lfi.getUriPartial(), lfi.getFile());
                }
                workerD = new DownloaderPanel(ftpDownloader, lfx.getFile(), lfx.getName(),
                        LogsDownloaderWorkerUtil.getFileTarget(lfx.getName(), getDirBaseToStoreFiles(), getLogLabel()), 
                        directoryContentsList, threadScheduledPool, queueWorkTickets);
            }
            else {
                workerD = new DownloaderPanel(ftpDownloader, lfx.getFile(), lfx.getName(),
                        LogsDownloaderWorkerUtil.getFileTarget(lfx.getName(), getDirBaseToStoreFiles(), getLogLabel()), 
                        threadScheduledPool, queueWorkTickets);
            }
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
        final LogFolderInfo lfdfinal = logFd;
        final LogFileInfo lfxfinal = lfx;
        workerD.addStateChangeListener(createDownloadStateListenerForDownloaderPanel(lfdfinal, 
                lfxfinal, workerD, gui.logFolderList, gui.logFilesList, gui.logFoldersListLabel,
                gui.downloadWorkersHolder, threadScheduledPool));
        gui.downloadWorkersHolder.add(workerD);
        gui.downloadWorkersHolder.revalidate();
        gui.downloadWorkersHolder.repaint();
        workerD.actionDownload();
    }

    /**
     * @param lfdfinal
     * @param lfxfinal
     * @param workerDFinal
     * @return
     */
    private static DownloadStateListener createDownloadStateListenerForDownloaderPanel(final LogFolderInfo lfdfinal,
            final LogFileInfo lfxfinal, final DownloaderPanel workerDFinal, LogFolderInfoList logFolderList,
            LogFileInfoList logFilesList, JXLabel logFoldersListLabel, JPanel downloadWorkersHolder, 
            ScheduledThreadPoolExecutor threadScheduledPool) {
        return new DownloadStateListener() {
            private LogFileInfo fxLog = lfxfinal;
            private Runnable task = null;

            @Override
            public void downloaderStateChange(DownloaderPanel.State newState, DownloaderPanel.State oldState) {
                // System.out.println("State state update for " + fxLog.getUriPartial() + " " + fxLog.getState() + "::" + oldState + "::" + newState);

                if (fxLog.getState() != LogFolderInfo.State.LOCAL) {
                    if (newState == DownloaderPanel.State.DONE)
                        fxLog.setState(LogFolderInfo.State.SYNC);
                    else if (newState == DownloaderPanel.State.ERROR)
                        fxLog.setState(LogFolderInfo.State.ERROR);
                    else if (newState == DownloaderPanel.State.WORKING || newState == DownloaderPanel.State.TIMEOUT
                            || newState == DownloaderPanel.State.QUEUED)
                        fxLog.setState(LogFolderInfo.State.DOWNLOADING);
                    else if (newState == DownloaderPanel.State.NOT_DONE)
                        fxLog.setState(LogFolderInfo.State.INCOMPLETE);
                    // else if (newState == DownloaderPanel.State.IDLE)
                    //     ;// fxLog.setState(LogFolderInfo.State.ERROR);

                    if (logFilesList.containsFile(fxLog)) {
                        logFilesList.revalidate();
                        logFilesList.repaint();
                    }

                    LogsDownloaderWorkerGUIUtil.updateLogFolderState(lfdfinal, logFolderList);
                    LogsDownloaderWorkerGUIUtil.updateLogStateIconForAllLogFolders(logFolderList, logFoldersListLabel);
                }

                if (newState == DownloaderPanel.State.WORKING || newState == DownloaderPanel.State.TIMEOUT
                        || newState == DownloaderPanel.State.QUEUED) {
                    cancelTasksIfSchedule();
                }
                else if (newState == DownloaderPanel.State.DONE) {
                    cancelTasksIfSchedule();
                    task = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (workerDFinal.getState() == DownloaderPanel.State.DONE) {
                                    workerDFinal.doStopAndInvalidate();
                                    downloadWorkersHolder.remove(workerDFinal);
                                    downloadWorkersHolder.revalidate();
                                    downloadWorkersHolder.repaint();
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    threadScheduledPool.schedule(task, DELTA_TIME_TO_CLEAR_DONE, TimeUnit.MILLISECONDS);
                }
                else {
                    cancelTasksIfSchedule();
                    task = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (workerDFinal.getState() != DownloaderPanel.State.WORKING
                                        && workerDFinal.getState() != DownloaderPanel.State.TIMEOUT
                                        && workerDFinal.getState() != DownloaderPanel.State.QUEUED) {
                                    workerDFinal.doStopAndInvalidate();
                                    // waitForStopOnAllLogFoldersDownloads(workerDFinal.getName());
                                    downloadWorkersHolder.remove(workerDFinal);
                                    downloadWorkersHolder.revalidate();
                                    downloadWorkersHolder.repaint();
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    threadScheduledPool.schedule(task, DELTA_TIME_TO_CLEAR_NOT_WORKING, TimeUnit.MILLISECONDS);
                }
            }

            private void cancelTasksIfSchedule() {
                if (task != null) {
                    threadScheduledPool.remove(task);
                    threadScheduledPool.purge();
                }
            }
        };
    }

    /**
     * @param logFd
     * @return
     */
    // FIXME Visibility
    boolean deleteLogFolderFromServer(LogFolderInfo logFd) {
        String path = logFd.getName();
        boolean ret = true;
        for (LogFileInfo fx : logFd.getLogFiles()) {
            if (fx.getState() != LogFolderInfo.State.LOCAL) {
                ret &= deleteLogFileFromServer(fx);
            }
        }
        boolean ret2 = false;
        for (String serverKey : serversList) {
            if (logFd.getState() != LogFolderInfo.State.LOCAL) {
                ret2 |= deleteLogFolderFromServerWorker(serverKey, path, true);
            }
        }
        return ret && ret2;
    }

    /**
     * @param logFx
     * @return
     */
    // FIXME Visibility
    boolean deleteLogFileFromServer(LogFileInfo logFx) {
        String path = logFx.getName();
        String hostFx = logFx.getHost();
        String host;
        for (String serverKey : serversList) {
            host = serverKey; // getHostFor(serverKey);
            // Not the best way but for now lets try like this
            if (hostFx.equals(host)) {
                boolean emptyFolder = true;
                if (logFx.isDirectory() && !logFx.getDirectoryContents().isEmpty()) {
                    List<Path> leftoverFolders = new ArrayList<>();
                    Path foldPath = Paths.get(path);
                    for (LogFileInfo fx : logFx.getDirectoryContents()) {
                        Path fxPath = Paths.get(fx.getName()).getParent();
                        Path fxPath1 = foldPath.relativize(fxPath);
                        leftoverFolders.add(fxPath1);
                        emptyFolder &= deleteLogFileFromServer(fx); // recursion
                    }

                    leftoverFolders = leftoverFolders.stream().distinct().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                    for (Path p : leftoverFolders) {
                        Path pt = foldPath.resolve(p);
                        emptyFolder &= deleteLogFolderFromServerWorker(serverKey, pt.toString(), true);
                    }
                }

                if (!emptyFolder) {
                    return false;
                } else if (logFx.getState() == LogFolderInfo.State.LOCAL) {
                    return true;
                } else {
                    // TODO do a better deletion of folder (presence test on servers)
                    if (logFx.isDirectory()) {
                        boolean ret2 = false;
                        if (logFx.getState() != LogFolderInfo.State.LOCAL) {
                            boolean ret3 = deleteLogFolderFromServerWorker(serverKey, path, true);
                            if (!ret3) {
                                // lets use brute force
                                try {
                                    FtpDownloader ftp = null;
                                    try {
                                        ftp = LogsDownloaderWorkerUtil.getOrRenewFtpDownloader(serverKey, ftpDownloaders, getHostFor(host), getPortFor(host));
                                    }
                                    catch (Exception e) {
                                        NeptusLog.pub().error("Error connecting to FTP deleting from '" + host + "@" + port +
                                                "' folder  '" + path + "' : " + e.getMessage());
                                    }
                                    ret3 |= ftp != null && removeDirectoryByPath(ftp.getClient(), path);
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error("Error FTP deleting from '" + host + "@" + port +
                                            "' folder '" + path + "' : " + e.getMessage());
                                }
                            }
                            ret2 |= ret3;
                        }
                        return ret2;
                    } else {
                        return deleteLogFolderFromServerWorker(serverKey, path, logFx.isDirectory());
                    }
                }
            }
        }
        return false;
    }

    private boolean deleteLogFolderFromServerWorker(String serverKey, String path, boolean isDirectory) {
        String host = getHostFor(serverKey);
        int port = getPortFor(serverKey);
        NeptusLog.pub().info("FTP deleting from '" + host + "@" + port + "'" +
                (isDirectory ? " folder " : " file ") + "'" + path + "'");
        boolean ret = false;
        try {
            FtpDownloader ftp = null;
            try {
                ftp = LogsDownloaderWorkerUtil.getOrRenewFtpDownloader(serverKey, ftpDownloaders, host, port);
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error connecting to FTP deleting from '" + host + "@" + port + "'" +
                        (isDirectory ? " folder " : " file ") + "'" + path + "' : " + e.getMessage());
            }
            ret = ftp != null && (isDirectory ? ftp.getClient().removeDirectory("/" + path) :
                    ftp.getClient().deleteFile("/" + path));
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error FTP deleting from '" + host + "@" + port + "'" +
                    (isDirectory ? " folder " : " file ") + "'" + path + "' : " + e.getMessage());
        }
        return ret;
    }

    private static boolean removeDirectoryByPath(FTPClient client, String path) throws IOException {
        String dirToRemove = path.substring(path.lastIndexOf('/')+1);
        String parentDir = path.substring(0, path.lastIndexOf('/'));
        return removeDirectoryByPath(client, parentDir, dirToRemove);
    }

    private static boolean removeDirectoryByPath(FTPClient ftpClient, String parentDir,
            String currentDir) throws IOException {
        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }

        FTPFile[] subFiles = ftpClient.listFiles(dirToList);

        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and the directory itself
                    continue;
                }
                String filePath = parentDir + "/" + currentDir + "/"
                        + currentFileName;
                if (currentDir.equals("")) {
                    filePath = parentDir + "/" + currentFileName;
                }

                if (aFile.isDirectory()) {
                    // remove the sub directory
                    removeDirectoryByPath(ftpClient, dirToList, currentFileName);
                } else {
                    // delete the file
                    boolean deleted = ftpClient.deleteFile(filePath);
                    if (deleted) {
                        NeptusLog.pub().info("DELETED the file: " + filePath);
                    } else {
                        NeptusLog.pub().error("CANNOT delete the file: " + filePath);
                    }
                }
            }

        }
        // finally, remove the directory itself
        boolean removed = ftpClient.removeDirectory(dirToList);
        if (removed) {
            NeptusLog.pub().info("REMOVED the directory: " + dirToList);
            return true;
        } else {
            NeptusLog.pub().error("CANNOT remove the directory: " + dirToList);
            return false;
        }
    }

    /**
     * @return the dirBaseToStoreFiles
     */
    String getDirBaseToStoreFiles() {
        return dirBaseToStoreFiles;
    }

    // --------------------------------------------------------------

    /**
     * @param visible
     */
    public void setVisibleHost(boolean visible) {
        gui.hostField.setVisible(visible);
        gui.hostLabel.setVisible(visible);
    }

    /**
     * @param visible
     */
    public void setVisiblePort(boolean visible) {
        gui.portField.setVisible(visible);
        gui.portLabel.setVisible(visible);
    }

    /**
     * @param visible
     */
    public void setVisibleLogLabel(boolean visible) {
        gui.logLabelField.setVisible(visible);
        gui.logLabelLabel.setVisible(visible);
    }

    /**
     * @param visible
     */
    public void setConfigPanelVisible(boolean visible) {
        gui.configCollapsiblePanel.setCollapsed(!visible);
    }

    /**
     * @param enable
     */
    public void setEnableHost(boolean enable) {
        gui.hostField.setEnabled(enable);
    }

    /**
     * @param enable
     */
    public void setEnablePort(boolean enable) {
        gui.portField.setEnabled(enable);
    }

    /**
     * @param enable
     */
    public void setEnableLogLabel(boolean enable) {
        gui.logLabelField.setEnabled(enable);
    }

    // --------------------------------------------------------------

    protected void warnMsg(String message) {
        NudgeGlassPane.nudge(gui.frameCompHolder.getRootPane(), (gui.frameIsExternalControlled ? getLogLabel() + " > " : "")
                + message, 2);
    }

    protected void warnLongMsg(String message) {
        NudgeGlassPane.nudge(gui.frameCompHolder.getRootPane(), (gui.frameIsExternalControlled ? getLogLabel() + " > " : "")
                + message, 6);
    }

    // --------------------------------------------------------------
    // Public interface methods

    public boolean doUpdateListFromServer() {
        gui.downloadListButton.doClick(100);
        return true;
    }

    /**
     * @param logList
     * @return
     */
    public boolean doDownloadLogFoldersFromServer(String... logList) {
        return doDownloadOrDeleteLogFoldersFromServer(true, logList);
    }

    /**
     * @param logList
     * @return
     */
    public boolean doDeleteLogFoldersFromServer(String... logList) {
        return doDownloadOrDeleteLogFoldersFromServer(false, logList);
    }

    /**
     * @param downloadOrDelete
     * @param logList
     * @return
     */
    private boolean doDownloadOrDeleteLogFoldersFromServer(final boolean downloadOrDelete, String... logList) {
        if (logList == null)
            return false;
        else if (logList.length == 0)
            return false;
        final LinkedList<LogFolderInfo> folders = new LinkedList<LogFolderInfo>();
        for (String str : logList) {
            if (gui.logFolderList.containsFolder(new LogFolderInfo(str)))
                folders.add(gui.logFolderList.getFolder(str));
        }
        if (folders.size() == 0)
            return false;
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                gui.logFolderList.setEnabled(false);
                gui.logFolderList.clearSelection();
                gui.logFolderList.setValueIsAdjusting(true);
                for (LogFolderInfo logFd : folders) {
                    // logFolderList.setSelectedValue(logFd, false);
                    int iS = gui.logFolderList.myModel.indexOf(logFd);
                    iS = gui.logFolderList.convertIndexToView(iS);
                    gui.logFolderList.addSelectionInterval(iS, iS);
                }
                gui.logFolderList.setValueIsAdjusting(false);
                if (downloadOrDelete)
                    gui.downloadSelectedLogDirsButton.doClick(100);
                else
                    gui.deleteSelectedLogFoldersButton.doClick(100);
                return null;
            }

            @Override
            protected void done() {
                super.done();
                try {
                    get();
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
                gui.logFolderList.setEnabled(true);
            }
        };
        worker.execute();
        return true;
    }

    /**
     * @param logList Use it if you want specific log folder state info., if not present gives all.
     * @return
     */
    public LinkedHashMap<String, LogFolderInfo.State> doGiveStateOfLogFolders(String... logList) {
        LinkedHashMap<String, LogFolderInfo.State> res = new LinkedHashMap<String, LogFolderInfo.State>();
        Vector<String> filter = null;
        if (logList != null) {
            if (logList.length > 0) {
                filter = new Vector<String>();
                for (String str : logList) {
                    filter.add(str);
                }
            }
        }
        for (Enumeration<?> iterator = gui.logFolderList.myModel.elements(); iterator.hasMoreElements();) {
            LogFolderInfo lfd = (LogFolderInfo) iterator.nextElement();
            if (filter == null)
                res.put(lfd.getName(), lfd.getState());
            else {
                if (filter.size() == 0)
                    break;
                if (filter.contains(lfd.getName())) {
                    res.put(lfd.getName(), lfd.getState());
                    filter.remove(lfd.getName());
                }
            }
        }
        return res;
    }

    /**
     * @return
     */
    public String[] doGiveListOfLogFolders() {
        LinkedList<String> list = new LinkedList<String>();
        for (Enumeration<?> iterator = gui.logFolderList.myModel.elements(); iterator.hasMoreElements();) {
            LogFolderInfo lfd = (LogFolderInfo) iterator.nextElement();
            list.add(lfd.getName());
        }
        return list.toArray(new String[0]);
    }

    /**
     * @param logFolder
     * @return
     */
    public String[] doGiveListOfLogFolderFiles(String logFolder) {
        LinkedList<String> list = new LinkedList<String>();
        for (Enumeration<?> iterator = gui.logFolderList.myModel.elements(); iterator.hasMoreElements();) {
            LogFolderInfo lfd = (LogFolderInfo) iterator.nextElement();
            if (lfd.getName().equalsIgnoreCase(logFolder)) {
                for (LogFileInfo lfx : lfd.getLogFiles()) {
                    list.add(lfx.getName());
                }
                break;
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * @param logFolder
     * @return
     */
    public LinkedHashMap<String, LogFolderInfo.State> doGiveStateOfLogFolderFiles(String logFolder) {
        LinkedHashMap<String, LogFolderInfo.State> res = new LinkedHashMap<String, LogFolderInfo.State>();
        for (Enumeration<?> iterator = gui.logFolderList.myModel.elements(); iterator.hasMoreElements();) {
            LogFolderInfo lfd = (LogFolderInfo) iterator.nextElement();
            if (lfd.getName().equalsIgnoreCase(logFolder)) {
                for (LogFileInfo lfx : lfd.getLogFiles()) {
                    res.put(lfx.getName(), lfx.getState());
                }
                break;
            }
        }
        return res;
    }

    private void doStopLogFoldersDownloads(boolean invalidateComponents, String... logList) {
        boolean stopAll = true;
        if (logList != null)
            if (logList.length > 0)
                stopAll = false;
        Component[] components = gui.downloadWorkersHolder.getComponents();
        for (Component cp : components) {
            try {
                DownloaderPanel workerD = (DownloaderPanel) cp;
                if (!stopAll) {
                    for (String prefix : logList) {
                        if (workerD.getName().startsWith(prefix)) {
                            if (!invalidateComponents)
                                workerD.actionStop();
                            else
                                workerD.actionStopAndInvalidate();
                            break;
                        }
                    }
                    continue;
                }
                workerD.actionStop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void waitForStopOnAllLogFoldersDownloads(String... logList) {
        if (!GeneralPreferences.logsDownloaderWaitForAllToStop)
            return;

        boolean waitStopAll = true;
        if (logList != null) {
            if (logList.length > 0)
                waitStopAll = false;
        }
        Component[] components = gui.downloadWorkersHolder.getComponents();
        for (Component cp : components) {
            try {
                DownloaderPanel workerD = (DownloaderPanel) cp;
                boolean wait = false;
                if (workerD.getState() == DownloaderPanel.State.WORKING
                        || workerD.getState() == DownloaderPanel.State.QUEUED) {
                    if (!waitStopAll) {
                        for (String prefix : logList) {
                            if (workerD.getName().startsWith(prefix)) {
                                wait = true;
                                break;
                            }
                        }
                        if (!wait)
                            continue;
                    }

                    while (workerD.getState() == DownloaderPanel.State.WORKING
                            || workerD.getState() == DownloaderPanel.State.QUEUED) {
                        try { Thread.sleep(100); } catch (Exception e) { }
                        NeptusLog.pub().warn("Waiting for '" + workerD.getUri() + "' to stop!");
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean doReset(final boolean justStopDownloads) {
        boolean isEventDispatchThread = SwingUtilities.isEventDispatchThread();

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (!justStopDownloads)
                    actions.resetting = true;

                boolean resetRes = true;
                if (!justStopDownloads) {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            warnLongMsg(I18n.text("Resetting... Wait please..."));
                        }
                    });
                }
                try {
                    disconnectFTPClientsForListing();
                }
                catch (final Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            warnLongMsg(I18n.textf("Error caught on resetting: %errormessage", e.getMessage()));
                        }
                    });
                    resetRes &= false;
                }
                try {
                    if (!justStopDownloads)
                        doStopLogFoldersDownloads(true);
                    else
                        doStopLogFoldersDownloads(false);
                    waitForStopOnAllLogFoldersDownloads();
                }
                catch (final Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            warnLongMsg(I18n.textf("Error caught on resetting: %errormessage", e.getMessage()));
                        }
                    });
                    resetRes &= false;
                }
                try {
                    if (!justStopDownloads) {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                gui.cleanInterface();
                            }
                        });
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    warnLongMsg(I18n.textf("Error caught on resetting: %errormessage", e.getMessage()));
                    resetRes &= false;
                }

                queueWorkTickets.cancelAll();

                if (!justStopDownloads) {
                    actions.resetting = false;
                }

                return resetRes;
            }

            @Override
            protected void done() {
                if (!justStopDownloads) {
                    gui.resetButton.setEnabled(true);
                }
                else {
                    gui.stopAllButton.setEnabled(true);
                    LogsDownloaderWorkerGUIUtil.updateLogStateIconForAllLogFolders(gui.logFolderList,
                            gui.logFoldersListLabel);
                }
            }
        };
        worker.execute();

        if (!isEventDispatchThread) {
            try {
                return worker.get();
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();

        final LogsDownloaderWorker logFetcher = new LogsDownloaderWorker();
        logFetcher.setEnableLogLabel(true);

        logFetcher.gui.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logFetcher.gui.frame.setVisible(true);

        // logFetcher.setHost("10.0.2.90");
        // logFetcher.setPort(8080);
    }
}
