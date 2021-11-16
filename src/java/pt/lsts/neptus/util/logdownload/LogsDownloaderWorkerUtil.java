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
 * 14/01/2016
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.lang.StringUtils;

import pt.lsts.imc.EntityState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.ftp.FtpDownloader;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.loader.NeptusMain;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.neptus.util.llf.LogUtils.LogValidity;

/**
 * @author pdias
 *
 */
class LogsDownloaderWorkerUtil {

    /** To avoid instantiation */
    private LogsDownloaderWorkerUtil() {
    }

    /**
     * Gets the IP for CAM CPU from the main CPU IP + 3 in the last byte.
     * 
     * @param mainHost This is and IPv4 of the main CPU.
     * @return The CAM CPU IP or empty on error.
     */
    static String getCameraHost(String mainHost) {
        return getCameraHostWorker(mainHost, 3);
    }

    static String getCameraHost2(String mainHost) {
        return getCameraHostWorker(mainHost, 2);
    }

    private static String getCameraHostWorker(String mainHost, int offset) {
        String cameraHost = null;
        try {
            String[] parts = mainHost.split("\\.");
            parts[3] = "" + (Integer.parseInt(parts[3]) + offset);
            cameraHost = StringUtils.join(parts, ".");
        }
        catch (Exception oops) {
            NeptusLog.pub().error("Could not get camera host string: " + oops.getClass().getSimpleName(), oops);
            cameraHost = "";
        }
        catch (Error oops) {
            NeptusLog.pub().error("Could not get camera host string: " + oops.getClass().getSimpleName(), oops);
            cameraHost = "";
        }

        return cameraHost;
    }

    /**
     * Creates a {@link ScheduledThreadPoolExecutor} for use on {@link LogsDownloaderWorker}.
     * 
     * @param caller
     * @return
     */
    static ScheduledThreadPoolExecutor createThreadPool(LogsDownloaderWorker caller) {
        ScheduledThreadPoolExecutor ret = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(4, new ThreadFactory() {
            private ThreadGroup group;
            private long count = 0;
            {
                SecurityManager s = System.getSecurityManager();
                group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            }
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r);
                t.setName(caller.getClass().getSimpleName() + "::"
                        + Integer.toHexString(caller.hashCode()) + "::" + count++);
                t.setDaemon(true);
                return t;
            }
        });
        
        return ret;
    }

    /**
     * Creates a message listener for {@link EntityState} for {@link LogsDownloaderWorker}.
     * 
     * @param worker
     * @param cameraButton
     * @return
     */
    static MessageListener<MessageInfo, IMCMessage> createEntityStateMessageListener(LogsDownloaderWorker worker,
            JButton cameraButton) {
        return new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                if (msg.getAbbrev().equals("EntityState")) {
                    // we need to check for the source match
                    int srcIdNumber = msg.getSrc();
                    ImcSystem sys = ImcSystemsHolder.lookupSystem(srcIdNumber);
                    if (sys != null && worker.getLogLabel().equalsIgnoreCase(sys.getName())) {
                        EntityState est = (EntityState) msg;
                        String entityName = EntitiesResolver.resolveName(worker.getLogLabel(), (int) msg.getSrcEnt());
                        if (entityName != null && LogsDownloaderWorker.CAMERA_CPU_LABEL.equalsIgnoreCase(entityName)) {
                            String descStateCode = est.getDescription();
                            // Testing for active state code (also for the translated string)
                            if (descStateCode != null
                                    && ("active".equalsIgnoreCase(descStateCode.trim()) || I18n.text("active")
                                            .equalsIgnoreCase(descStateCode.trim()))) {
                                cameraButton.setBackground(LogsDownloaderWorker.CAM_CPU_ON_COLOR);
                            }
                            else {
                                cameraButton.setBackground(null);
                            }
                        }
                    }
                }
            }
        };
    }

    /**
     * Creates a {@link MouseListener} to open the selected log folder in MRA.
     * 
     * @param worker
     * @param logFolderList
     * @return
     */
    static MouseAdapter createOpenLogInMRAMouseListener(LogsDownloaderWorker worker, LogFolderInfoList logFolderList) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    // Test if log can be opened in MRA, and open it

                    final String baseFxPath = worker.getDirBaseToStoreFiles() + "/" + worker.getLogLabel() + "/"
                            + logFolderList.getSelectedValue() + "/";
                    File logFolder = new File(baseFxPath);
                    LogValidity isLogOkForOpening = LogUtils.isValidLSFSource(logFolder);

                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem jm = popup.add(I18n.text("Open this log in MRA"));
                    if (isLogOkForOpening == LogValidity.VALID) {
                        File log = LogUtils.getValidLogFileFromLogFolder(logFolder);
                        jm.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Thread t = new Thread(LogsDownloaderWorker.class.getSimpleName() + " :: MRA Openner") {
                                    public void run() {
                                        JFrame mra = new NeptusMRA();
                                        mra.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                        NeptusMain.wrapMainApplicationWindowWithCloseActionWindowAdapter(mra);
                                        ((NeptusMRA) mra).getMraFilesHandler().openLog(log);
                                    };
                                };
                                t.setDaemon(true);
                                t.start();
                            }
                        });
                    }
                    else {
                        jm.setEnabled(false);
                    }
                    
                    popup.show((Component)e.getSource(), e.getX(), e.getY());
                }
            }
        };
    }

    /**
     * Gets the clientFtp with the connection renewed ({@link FtpDownloader#renewClient()})
     * or create a new one connected.
     * 
     * @param clientFtp
     * @param host
     * @param port
     * @return
     * @throws Exception
     */
    private static FtpDownloader getOrRenewFtpDownloader(FtpDownloader clientFtp, String host, int port)
            throws Exception {
        if (clientFtp == null)
            clientFtp = new FtpDownloader(host, port, false);
        else
            clientFtp.setHostAndPort(host, port);

        if (!clientFtp.isConnected())
            clientFtp.renewClient();

        return clientFtp;
    }

    /**
     * Gets the clientFtp with the connection renewed ({@link FtpDownloader#renewClient()})
     * or create a new one connected. This is based on the key (serverKey) and at the end stores
     * in the ftpDownloaders.
     * 
     * @param serverKey
     * @param ftpDownloaders
     * @param host
     * @param port
     * @return
     * @throws Exception
     */
    static FtpDownloader getOrRenewFtpDownloader(String serverKey, LinkedHashMap<String, FtpDownloader> ftpDownloaders,
            String host, int port) throws Exception {
        FtpDownloader ftpDwnldr = ftpDownloaders.get(serverKey);
        ftpDwnldr = getOrRenewFtpDownloader(ftpDwnldr, host, port);
        ftpDownloaders.put(serverKey, ftpDwnldr); // Even if null is added
        return ftpDwnldr;
    }

    /**
     * Return the file for the file name in the dirBaseToStoreFiles for logs and the logLabel
     * of the log selected.
     * 
     * Does not create the parent folders (except the dirBaseToStoreFiles ones).
     * 
     * @param name
     * @param dirBaseToStoreFiles
     * @param logLabel
     * @return
     */
    static File getFileTarget(String name, String dirBaseToStoreFiles, String logLabel) {
        File outFile = new File(getDirTarget(dirBaseToStoreFiles, logLabel), name);
        // outFile.getParentFile().mkdirs(); Taking this out to not create empty folders
        return outFile;
    }

    /**
     * Return the folderfor the dirBaseToStoreFiles for logs and the logLabel
     * of the log selected.
     * 
     * Does not create the parent folders (except the dirBaseToStoreFiles ones).
     * 
     * @param dirBaseToStoreFiles
     * @param logLabel
     * @return
     */
    static File getDirTarget(String dirBaseToStoreFiles, String logLabel) {
        File dirToStore = new File(dirBaseToStoreFiles);
        dirToStore.mkdirs();
        File dirTarget = new File(dirToStore, logLabel);
        // dirTarget.mkdirs(); Taking this out to not create empty folders
        return dirTarget;
    }

    /**
     * Return the file size (or in case is a folder, its content size).
     * 
     * @param fx
     * @param worker
     * @return Negative values for errors (HTTP like returns).
     */
    static long getDiskSizeFromLocal(LogFileInfo fx, LogsDownloaderWorker worker) {
        File fileTarget = LogsDownloaderWorkerUtil.getFileTarget(fx.getName(), 
                worker.getDirBaseToStoreFiles(), worker.getLogLabel());
        if (fileTarget == null)
            return -1;
        else if (fileTarget.exists()) {
            if (fileTarget.isFile()) {
                return fileTarget.length();
            }
            else if (fileTarget.isDirectory()) {
                long allSize = 0;
                for (LogFileInfo dirFileInfo : fx.getDirectoryContents()) {
                    long dfSize = getDiskSizeFromLocal(dirFileInfo, worker);
                    if (dfSize >= 0)
                        allSize += dfSize;
                }
                return allSize;
            }
            else
                return -500;
        }
        else if (!fileTarget.exists()) {
            return -400;
        }
        return -500;
    }

    /**
     * Finds the LogFolderInfo for the lfx in the logFolderList.
     * 
     * @param lfx
     * @param logFolderList
     * @return
     */
    static LogFolderInfo findLogFolderInfoForFile(LogFileInfo lfx, LogFolderInfoList logFolderList) {
        for (Object comp : logFolderList.getSelectedValues()) {
            try {
                LogFolderInfo logFd = (LogFolderInfo) comp;
                if (logFd.getLogFiles().contains(lfx))
                    return logFd;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
