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
 * Author: Paulo Dias
 * 2009/09/12
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.EntityState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.ftp.FtpDownloader;
import pt.lsts.neptus.gui.MiniButton;
import pt.lsts.neptus.gui.NudgeGlassPane;
import pt.lsts.neptus.gui.swing.MessagePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;

/**
 * This is the log downloader worker panel. You can put it into an external frame using
 * the proper constructor. In this case you HAVE TO CALL the {@link #cleanup()}.
 * 
 * @author pdias
 * 
 */
public class LogsDownloaderWorker {

    private static final Color CAM_CPU_ON_COLOR = Color.GREEN;
    private static final int ACTIVE_DOWNLOADS_QUEUE_SIZE = 1;
    private static final String SERVER_MAIN = "main";
    private static final String SERVER_CAM = "cam";

    private static final int DEFAULT_PORT = 30021;

    private static final String DEFAULT_TITLE = I18n.text("Download Log Files");

    public static final ImageIcon ICON_DOWNLOAD_FOLDERS = ImageUtils.getScaledIcon(
            "images/downloader/folder_download.png", 32, 32);
    public static final ImageIcon ICON_DOWNLOAD_FILES = ImageUtils.getScaledIcon("images/downloader/file_down.png", 32,
            32);
    public static final ImageIcon ICON_DOWNLOAD_LIST = ImageUtils.getScaledIcon("images/downloader/sync-list.png", 32,
            32);
    public static final ImageIcon ICON_SETTINGS = ImageUtils.getScaledIcon("images/settings.png", 32, 32);
    public static final ImageIcon ICON_DELETE_FOLDERS = ImageUtils.getScaledIcon(
            "images/downloader/folder_delete1.png", 32, 32);
    public static final ImageIcon ICON_DELETE_FILES = ImageUtils.getScaledIcon("images/downloader/file_delete1.png",
            32, 32);
    public static final ImageIcon ICON_HELP = ImageUtils.getScaledIcon("images/downloader/help.png", 32, 32);
    public static final ImageIcon ICON_RESET = ImageUtils.getScaledIcon("images/buttons/redo.png", 32, 32);
    public static final ImageIcon ICON_STOP = ImageUtils.getScaledIcon("images/downloader/stop.png", 32, 32);
    public static final ImageIcon ICON_DOWNLOAD_PHOTO = ImageUtils
            .getScaledIcon("images/downloader/camera.png", 32, 32);

    private static final ColorMap diskFreeColorMap = ColorMapFactory
            .createInvertedColorMap((InterpolationColorMap) ColorMapFactory.createRedYellowGreenColorMap());

    protected static final long DELTA_TIME_TO_CLEAR_DONE = 5000;
    protected static final long DELTA_TIME_TO_CLEAR_NOT_WORKING = 45000;

    protected static final String CAMERA_CPU_LABEL = "Slave CPU";

    private FtpDownloader clientFtp = null;
    private FtpDownloader cameraFtp = null;

    private boolean stopLogListProcessing = false;
    private boolean resetting = false;

    private String host = "127.0.0.1";
    private int port = DEFAULT_PORT;

    private String dirBaseToStoreFiles = "log/downloaded";

    private String logLabel = I18n.text("unknown"); // This should be a word with no spaces

    private boolean frameIsExternalControlled = false;

    // Actions
    private AbstractAction downloadListAction = null;
    private AbstractAction downloadSelectedLogDirsAction = null;
    private AbstractAction downloadSelectedLogFilesAction = null;
    private AbstractAction deleteSelectedLogFoldersAction = null;
    private AbstractAction deleteSelectedLogFilesAction = null;
    private AbstractAction toggleConfPanelAction = null;
    private AbstractAction toggleExtraInfoPanelAction = null;
    private AbstractAction helpAction = null;
    private AbstractAction resetAction = null;
    private AbstractAction stopAllAction = null;
    private AbstractAction turnCameraOn = null;

    // UI
    private JFrame frame = null;
    private JXPanel frameCompHolder = null;
    private JTextField hostField = null;
    private JTextField portField = null;
    private JTextField logLabelField = null;
    private JLabel hostLabel = null;
    private JLabel portLabel = null;
    private JLabel logLabelLabel = null;
    private MessagePanel msgPanel = null;
    private JXLabel logFoldersListLabel = null;
    private JXLabel logFilesListLabel = null;
    private JPanel downloadWorkersHolder = null;
    private JScrollPane downloadWorkersScroll = null;
    private LogFolderInfoList logFolderList = null;
    private JScrollPane logFolderScroll = null;
    private LogFileInfoList logFilesList = null;
    private JScrollPane logFilesScroll = null;

    private JXLabel diskFreeLabel = null;

    private MiniButton downloadListButton = null;
    private MiniButton downloadSelectedLogDirsButton = null;
    private MiniButton downloadSelectedLogFilesButton = null;
    private MiniButton deleteSelectedLogFoldersButton = null;
    private MiniButton deleteSelectedLogFilesButton = null;

    private MiniButton toggleConfPanelButton = null;
    private MiniButton toggleExtraInfoPanelButton = null;

    private MiniButton helpButton = null;
    private MiniButton resetButton = null;
    private MiniButton stopAllButton = null;

    private JButton cameraButton = null;

    private DownloaderHelp downHelpDialog = null;

    private JXPanel configHolder = null;
    private JXCollapsiblePane configCollapsiblePanel = null;
    private JXCollapsiblePane extraInfoCollapsiblePanel = null;

    private JProgressBar listHandlingProgressBar = null;

    // Background Painter Stuff
    private RectanglePainter rectPainter;
    private CompoundPainter<JXPanel> compoundBackPainter;

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
        if (parentFrame != null) {
            frame = parentFrame;
            frameIsExternalControlled = true;
        }
        initializeComm();
        initialize();
    }

    private void initializeComm() {
        // Init timer
        threadScheduledPool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(4, new ThreadFactory() {
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
                t.setName(LogsDownloaderWorker.class.getSimpleName() + "::"
                        + Integer.toHexString(LogsDownloaderWorker.this.hashCode()) + "::" + count++);
                t.setDaemon(true);
                return t;
            }
        });

        // Register for EntityActivationState
        messageListener = new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                if (msg.getAbbrev().equals("EntityState")) {
                    // we need to check for the source match
                    int srcIdNumber = msg.getSrc();
                    ImcSystem sys = ImcSystemsHolder.lookupSystem(srcIdNumber);
                    if (sys != null && logLabel.equalsIgnoreCase(sys.getName())) {
                        EntityState est = (EntityState) msg;
                        String entityName = EntitiesResolver.resolveName(getLogLabel(), (int) msg.getSrcEnt());
                        if (entityName != null && CAMERA_CPU_LABEL.equalsIgnoreCase(entityName)) {
                            String descStateCode = est.getDescription();
                            // Testing for active state code (also for the translated string)
                            if (descStateCode != null
                                    && ("active".equalsIgnoreCase(descStateCode.trim()) || I18n.text("active")
                                            .equalsIgnoreCase(descStateCode.trim()))) {
                                cameraButton.setBackground(CAM_CPU_ON_COLOR);
                            }
                            else {
                                cameraButton.setBackground(null);
                            }
                        }
                    }
                }
            }
        };
        ImcMsgManager.getManager().addListener(messageListener); // all systems listener
    }

    private boolean isCamCpuOn() {
        return cameraButton.getBackground() == CAM_CPU_ON_COLOR;
    }

    private void initialize() {
        initializeActions();

        if (frame == null) {
            frame = new JFrame();
            frame.setSize(900, 560);
            frame.setIconImages(ConfigFetch.getIconImagesForFrames());
            frame.setTitle(DEFAULT_TITLE + " - " + logLabel);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    super.windowClosed(e);
                    cleanup();
                }
            });
        }

        hostLabel = new JLabel(I18n.text("Host: "));
        hostField = new JTextField(20);
        hostField.setText(host);
        portLabel = new JLabel(I18n.text("Port: "));
        portField = new JTextField(5);
        portField.setText("" + port);
        logLabelLabel = new JLabel(I18n.text("System Label: "));
        logLabelField = new JTextField(40);
        logLabelField.setText(logLabel);
        logLabelField.setToolTipText(I18n.text("This will dictate the directory where the logs will go."));

        msgPanel = new MessagePanel();
        msgPanel.showButtons(false);

        logFoldersListLabel = new JXLabel("<html><b>" + I18n.text("Log Folders"), JLabel.CENTER);
        logFilesListLabel = new JXLabel("<html><b>" + I18n.text("Log Files"), JLabel.CENTER);

        diskFreeLabel = new JXLabel("<html><b>?", JLabel.CENTER);
        diskFreeLabel.setBackgroundPainter(getCompoundBackPainter());

        resetButton = new MiniButton();
        resetButton.setToolTipText(I18n.text("Reset the interface"));
        resetButton.setIcon(ICON_RESET);
        resetButton.addActionListener(resetAction);

        stopAllButton = new MiniButton();
        stopAllButton.setToolTipText(I18n.text("Stop all log downloads"));
        stopAllButton.setIcon(ICON_STOP);
        stopAllButton.addActionListener(stopAllAction);

        cameraButton = new JButton();
        cameraButton.setToolTipText(I18n.text("Turn on/off camera CPU"));
        cameraButton.setIcon(ICON_DOWNLOAD_PHOTO);
        cameraButton.addActionListener(turnCameraOn);

        downloadWorkersHolder = new JPanel();
        downloadWorkersHolder.setLayout(new BoxLayout(downloadWorkersHolder, BoxLayout.Y_AXIS));
        downloadWorkersHolder.setBackground(Color.WHITE);

        downloadWorkersScroll = new JScrollPane();
        downloadWorkersScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        downloadWorkersScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        downloadWorkersScroll.setViewportView(downloadWorkersHolder);

        logFolderList = new LogFolderInfoList();
        logFolderList.setSortable(true);
        logFolderList.setAutoCreateRowSorter(true);
        logFolderList.setSortOrder(SortOrder.DESCENDING);
        logFolderList.addListSelectionListener(new ListSelectionListener() {
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
                        logFilesList.setValueIsAdjusting(false);
                        logFilesList.invalidate();
                        logFilesList.validate();
                        logFilesList.setEnabled(true);
                    }
                };
                AsyncWorker.getWorkerThread().postTask(task);
            }
        });

        logFolderList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    // Test if log can be opened in MRA, and open it

                    final String baseFxPath = dirBaseToStoreFiles + "/" + getLogLabel() + "/"
                            + logFolderList.getSelectedValue() + "/";
                    final File imc = new File(baseFxPath + "IMC.xml");
                    final File imcGz = new File(baseFxPath + "IMC.xml.gz");

                    final File log = new File(baseFxPath + "Data.lsf");
                    final File logGz = new File(baseFxPath + "Data.lsf.gz");

                    if ((imc.exists() || imcGz.exists()) && (logGz.exists() || log.exists())) {

                        JPopupMenu popup = new JPopupMenu();
                        popup.add(I18n.text("Open this log in MRA")).addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Thread t = new Thread(LogsDownloaderWorker.class.getSimpleName() + " :: MRA Openner") {
                                    public void run() {
                                        JFrame mra = new NeptusMRA();
                                        mra.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                                        File fx = null;
                                        if (logGz.exists())
                                            fx = logGz;
                                        if (log.exists())
                                            fx = log;

                                        ((NeptusMRA) mra).getMraFilesHandler().openLog(fx);
                                    };
                                };

                                t.setDaemon(true);
                                t.start();
                            }
                        });
                        
                        popup.show((Component)e.getSource(), e.getX(), e.getY());
                    }
                    else {
                        warnMsg(I18n.text("Basic log folder not synchronized. Can't open MRA"));
                        return;
                    }
                }
            }
        });

        logFolderScroll = new JScrollPane();
        logFolderScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logFolderScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logFolderScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        logFolderScroll.setViewportView(logFolderList);

        logFilesList = new LogFileInfoList();
        logFilesList.setSortable(true);
        logFilesList.setAutoCreateRowSorter(true);
        logFilesList.setSortOrder(SortOrder.DESCENDING);

        logFilesScroll = new JScrollPane();
        logFilesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logFilesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logFilesScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        logFilesScroll.setViewportView(logFilesList);

        downloadListButton = new MiniButton();
        downloadListButton.setToolTipText(I18n.text("Synchronize List of Log Folders"));
        downloadListButton.setIcon(ICON_DOWNLOAD_LIST);
        downloadListButton.addActionListener(downloadListAction);

        downloadSelectedLogDirsButton = new MiniButton();
        downloadSelectedLogDirsButton.setToolTipText(I18n.text("Synchronize Selected Log Folders"));
        downloadSelectedLogDirsButton.setIcon(ICON_DOWNLOAD_FOLDERS);
        downloadSelectedLogDirsButton.addActionListener(downloadSelectedLogDirsAction);

        downloadSelectedLogFilesButton = new MiniButton();
        downloadSelectedLogFilesButton.setToolTipText(I18n.text("Synchronize Selected Log Files"));
        downloadSelectedLogFilesButton.setIcon(ICON_DOWNLOAD_FILES);
        downloadSelectedLogFilesButton.addActionListener(downloadSelectedLogFilesAction);

        deleteSelectedLogFoldersButton = new MiniButton();
        deleteSelectedLogFoldersButton.setToolTipText(I18n.text("Delete Selected Log Folders"));
        deleteSelectedLogFoldersButton.setIcon(ICON_DELETE_FOLDERS);
        deleteSelectedLogFoldersButton.addActionListener(deleteSelectedLogFoldersAction);

        deleteSelectedLogFilesButton = new MiniButton();
        deleteSelectedLogFilesButton.setToolTipText(I18n.text("Delete Selected Log Files"));
        deleteSelectedLogFilesButton.setIcon(ICON_DELETE_FILES);
        deleteSelectedLogFilesButton.addActionListener(deleteSelectedLogFilesAction);

        // Config Panel Setup
        configCollapsiblePanel = new JXCollapsiblePane();
        configCollapsiblePanel.setLayout(new BorderLayout());
        configHolder = new JXPanel();
        configHolder.setBorder(new TitledBorder(I18n.text("Configuration")));
        configCollapsiblePanel.add(configHolder, BorderLayout.CENTER);
        GroupLayout layoutCfg = new GroupLayout(configHolder);
        configHolder.setLayout(layoutCfg);
        layoutCfg.setAutoCreateGaps(true);
        layoutCfg.setAutoCreateContainerGaps(false);
        layoutCfg.setHorizontalGroup(layoutCfg
                .createParallelGroup(GroupLayout.Alignment.CENTER)
                .addGroup(
                        layoutCfg.createSequentialGroup().addComponent(hostLabel).addComponent(hostField)
                        .addComponent(portLabel).addComponent(portField))
                        .addGroup(layoutCfg.createSequentialGroup().addComponent(logLabelLabel).addComponent(logLabelField)));
        layoutCfg.setVerticalGroup(layoutCfg
                .createSequentialGroup()
                .addGroup(
                        layoutCfg.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(hostLabel)
                        .addComponent(hostField).addComponent(portLabel).addComponent(portField))
                        .addGroup(
                                layoutCfg.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(logLabelLabel)
                                .addComponent(logLabelField)));
        layoutCfg.linkSize(SwingConstants.VERTICAL, hostLabel, hostField, portLabel, portField, logLabelLabel, logLabelField);
        layoutCfg.linkSize(SwingConstants.HORIZONTAL,  logLabelLabel, hostLabel);

        // This is called here (After the group layout configuration) because of an IllegalStateException during collapse redraw
        configCollapsiblePanel.setCollapsed(true);

        // Collapsible Panel Show/Hide buttons
        toggleConfPanelButton = new MiniButton();
        toggleConfPanelButton.setToolTipText(I18n.text("Show/Hide Configuration Panel"));
        toggleConfPanelButton.setIcon(ICON_SETTINGS);
        toggleConfPanelButton.addActionListener(toggleConfPanelAction);

        toggleExtraInfoPanelButton = new MiniButton();
        toggleExtraInfoPanelButton.setToolTipText(I18n.text("Show/Hide Download Panel"));
        toggleExtraInfoPanelButton.setIcon(ICON_SETTINGS);
        toggleExtraInfoPanelButton.addActionListener(toggleExtraInfoPanelAction);

        helpButton = new MiniButton();
        helpButton.setToolTipText(I18n.text("Show Help"));
        helpButton.setIcon(ICON_HELP);
        helpButton.addActionListener(helpAction);

        listHandlingProgressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        listHandlingProgressBar.setIndeterminate(false);
        listHandlingProgressBar.setStringPainted(true);
        listHandlingProgressBar.setString("");

        // Setup main content panel
        JPanel contentPanel = new JPanel();
        GroupLayout layout = new GroupLayout(contentPanel);
        contentPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout
                .createParallelGroup(GroupLayout.Alignment.CENTER)
                .addGroup(
                        layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(logFoldersListLabel)
                                .addGroup(
                                        layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(logFolderScroll)
                                        .addGroup(
                                                layout.createSequentialGroup()
                                                .addComponent(downloadListButton, 34, 34, 34)
                                                .addGap(10)
                                                .addComponent(downloadSelectedLogDirsButton, 34, 34, 34)
                                                .addComponent( downloadSelectedLogFilesButton, 34, 34, 34)
                                                .addGap(10)
                                                .addComponent(deleteSelectedLogFoldersButton, 34, 34, 34)
                                                .addComponent(deleteSelectedLogFilesButton, 34, 34, 34)
                                                .addGap(10)
                                                .addComponent(stopAllButton, 34, 34, 34)
                                                .addGap(10)
                                                .addComponent(toggleConfPanelButton,34, 34, 34)
                                                // .addComponent(toggleExtraInfoPanelButton, 25, 25, 25)
                                                .addGap(10)
                                                .addComponent(resetButton, 34, 34, 34)
                                                .addComponent(helpButton, 34, 34, 34)
                                                .addComponent(cameraButton, 34, 34, 34)
                                                .addComponent(diskFreeLabel, 60, 80,120))))
                                                .addGroup(
                                                        layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                        .addComponent(logFilesListLabel).addComponent(logFilesScroll)))
                                                        // .addComponent(msgPanel)
                                                        .addComponent(listHandlingProgressBar).addComponent(downloadWorkersScroll));
        layout.setVerticalGroup(layout
                .createSequentialGroup()
                .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(
                                layout.createSequentialGroup()
                                .addComponent(logFoldersListLabel)
                                .addGroup(
                                        layout.createSequentialGroup()
                                        .addComponent(logFolderScroll, 180, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                        .addGroup(
                                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                .addComponent(downloadListButton, 34, 34, 34)
                                                .addComponent(downloadSelectedLogDirsButton, 34, 34, 34)
                                                .addComponent(downloadSelectedLogFilesButton, 34, 34, 34)
                                                .addComponent(deleteSelectedLogFoldersButton, 34, 34, 34)
                                                .addComponent(deleteSelectedLogFilesButton, 34, 34, 34)
                                                .addComponent(stopAllButton, 34, 34, 34)
                                                .addComponent(toggleConfPanelButton, 34, 34, 34)
                                                .addComponent(resetButton, 34, 34, 34)
                                                .addComponent(helpButton, 34, 34, 34)
                                                .addComponent(cameraButton, 34, 34, 34)
                                                .addComponent(diskFreeLabel, 34, 34, 34))))
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                        .addComponent(logFilesListLabel)
                                                        .addComponent(logFilesScroll, 200, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                                        .addComponent(listHandlingProgressBar)
                                                        .addComponent(downloadWorkersScroll, 80, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        // Setup of the Frame Content
        frameCompHolder = new JXPanel();
        frameCompHolder.setLayout(new BorderLayout());
        frameCompHolder.add(configCollapsiblePanel, BorderLayout.NORTH);
        frameCompHolder.add(contentPanel, BorderLayout.CENTER);

        if (!frameIsExternalControlled) {
            frame.setLayout(new BorderLayout());
            frame.add(frameCompHolder, BorderLayout.CENTER);
        }

        downHelpDialog = new DownloaderHelp(frame);

        setEnableLogLabel(false);

        setEnableHost(true);

        if (!frameIsExternalControlled)
            GuiUtils.centerOnScreen(frame);

        ttaskLocalDiskSpace = getTimerTaskLocalDiskSpace();
        threadScheduledPool.scheduleAtFixedRate(ttaskLocalDiskSpace, 500, 5000, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("serial")
    private void initializeActions() {
        downloadListAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!validateAndSetUI()) {
                    popupErrorConfigurationDialog();
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
                                listHandlingProgressBar.setValue(0);
                                listHandlingProgressBar.setString(I18n.text("Starting..."));
                            }
                        });

                        downloadListButton.setEnabled(false);
                        // logFolderList.setEnabled(false);
                        logFolderList.setValueIsAdjusting(true);
                        // logFilesList.setEnabled(false);

                        // ->Getting txt list of logs from server
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                listHandlingProgressBar.setValue(10);
                                listHandlingProgressBar.setIndeterminate(true);
                                listHandlingProgressBar.setString(I18n
                                        .text("Connecting to remote system for log list update..."));
                            }
                        });

                        LinkedHashMap<FTPFile, String> retList = null;
                        LinkedHashMap<String, String> serversLogPresenceList = new LinkedHashMap<>(); 

                        long timeD1 = System.currentTimeMillis();
                        // Getting the file list from main CPU
                        try {
                            clientFtp = getOrRenewFtpDownloader(clientFtp, host, port);

                            retList = clientFtp.listLogs();

                            for (String partialUri : retList.values()) {
                                serversLogPresenceList.put(partialUri, SERVER_MAIN);
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error("Connecting with " + host + ":" + port + " with error: " + e.getMessage());
                        }
                        NeptusLog.pub().info(".......get list from main CPU server " + (System.currentTimeMillis() - timeD1) + "ms");                        

                        long timeD2 = System.currentTimeMillis();
                        //Getting the log list from Camera CPU
                        String cameraHost = getCameraHost(getHost());
                        if (cameraHost.length() > 0 && isCamCpuOn()) {
                            LinkedHashMap<FTPFile, String> retCamList = null;
                            try {
                                cameraFtp = getOrRenewFtpDownloader(cameraFtp, cameraHost, port);
                                retCamList = cameraFtp.listLogs();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error("Connecting with " + cameraHost + ":" + port + " with error: " + e.getMessage());
                            }
                            if (retCamList != null) {
                                if (retList == null) {
                                    retList = retCamList;

                                    for (String partialUri : retList.values()) {
                                        serversLogPresenceList.put(partialUri, SERVER_CAM);
                                    }
                                }
                                else {
                                    for (FTPFile camFTPFile : retCamList.keySet()) {
                                        String val = retCamList.get(camFTPFile);
                                        if (retList.containsValue(val)) {
                                            serversLogPresenceList.put(val, serversLogPresenceList.get(val) + " " + SERVER_CAM);
                                            continue;
                                        }
                                        else {
                                            retList.put(camFTPFile, val);
                                            serversLogPresenceList.put(val, SERVER_CAM);
                                        }
                                    }
                                }
                            }
                            NeptusLog.pub().info(".......get list from main CAM server " + (System.currentTimeMillis() - timeD2) + "ms");                        
                        }

                        NeptusLog.pub().info(".......get list from all servers " + (System.currentTimeMillis() - timeD1) + "ms");                        
                        if (retList == null) {
                            msgPanel.writeMessageTextln(I18n.text("Done"));
                            return null;
                        }

                        msgPanel.writeMessageTextln(I18n.textf("Log Folders: %numberoffolders", retList.size()));

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
                                    listHandlingProgressBar.setValue(100);
                                    listHandlingProgressBar.setIndeterminate(false);
                                    listHandlingProgressBar.setString(I18n.text("No logs..."));
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
                                    listHandlingProgressBar.setString(msg1);
                                }
                            });
                        }

                        // ->Removing from already existing LogFolders to LOCAL state
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                listHandlingProgressBar.setValue(20);
                                listHandlingProgressBar.setIndeterminate(false);
                                listHandlingProgressBar.setString(I18n.text("Filtering list..."));
                            }
                        });
                        long timeC1 = System.currentTimeMillis();
                        Object[] objArray = new Object[logFolderList.myModel.size()];
                        logFolderList.myModel.copyInto(objArray);
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
                        NeptusLog.pub().info(".......Removing from already existing LogFolders to LOCAL state "
                                + (System.currentTimeMillis() - timeC1) + "ms");

                        // ->Adding new LogFolders
                        LinkedList<LogFolderInfo> existenteLogFoldersFromServer = new LinkedList<LogFolderInfo>();
                        LinkedList<LogFolderInfo> newLogFoldersFromServer = new LinkedList<LogFolderInfo>();
                        for (String newLogName : retList.values()) {
                            if (stopLogListProcessing)
                                return null;

                            final LogFolderInfo newLogDir = new LogFolderInfo(newLogName);
                            if (logFolderList.containsFolder(newLogDir)) {
                                existenteLogFoldersFromServer.add(logFolderList.getFolder((newLogDir.getName())));
                            }
                            else {
                                newLogFoldersFromServer.add(newLogDir);
                                SwingUtilities.invokeAndWait(new Runnable() {
                                    @Override
                                    public void run() {
                                        logFolderList.addFolder(newLogDir);
                                    }
                                });
                            }
                        }
                        // msgPanel.writeMessageTextln("Logs Folders: " + logFolderList.myModel.size());

                        // ->Getting Log files list from server
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                listHandlingProgressBar.setValue(30);
                                listHandlingProgressBar.setIndeterminate(true);
                                listHandlingProgressBar.setString(I18n
                                        .text("Contacting remote system for complete log file list..."));

                                listHandlingProgressBar.setValue(40);
                                listHandlingProgressBar.setIndeterminate(false);
                                listHandlingProgressBar.setString(I18n.text("Processing log list..."));
                            }
                        });

                        objArray = new Object[logFolderList.myModel.size()];
                        logFolderList.myModel.copyInto(objArray);

                        long timeF0 = System.currentTimeMillis();
                        LinkedList<LogFolderInfo> tmpLogFolderList = getLogFileList(serversLogPresenceList);
                        NeptusLog.pub().info(".......Contacting remote system for complete log file list " +
                                (System.currentTimeMillis() - timeF0) + "ms");

                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                listHandlingProgressBar.setValue(70);
                                listHandlingProgressBar.setIndeterminate(false);
                                listHandlingProgressBar.setString(I18n.text("Updating logs info..."));
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
                                                    if (!getFileTarget(lfx.getName()).exists()) {
                                                        for (LogFileInfo lfi : lfx.getDirectoryContents()) {
                                                            if (!getFileTarget(lfi.getName()).exists()) {
                                                                if (lfx.getState() != LogFolderInfo.State.NEW && lfx.getState() != LogFolderInfo.State.DOWNLOADING)
                                                                    lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    else {
                                                        long sizeD = getDiskSizeFromLocal(lfx);
                                                        if (lfx.getSize() != sizeD && lfx.getState() == LogFolderInfo.State.SYNC)
                                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                    }
                                                }
                                                else {
                                                    if (!getFileTarget(lfx.getName()).exists()) {
                                                        if (lfx.getState() != LogFolderInfo.State.NEW && lfx.getState() != LogFolderInfo.State.DOWNLOADING) {
                                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                            // System.out.println("//////////// " + lfx.getName() + "  " + getFileTarget(lfx.getName()).exists());
                                                        }
                                                    }
                                                    else {
                                                        long sizeD = getDiskSizeFromLocal(lfx);
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
                                                if (!getFileTarget(lfx.getName()).exists()) {
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
                        NeptusLog.pub().info(".......Testing for log files from each log folder " +
                                (System.currentTimeMillis() - timeF1) + "ms");

                        long timeF2 = System.currentTimeMillis();
                        testNewReportedLogFoldersForLocalCorrespondent(newLogFoldersFromServer);
                        for (LogFolderInfo logFolder : existenteLogFoldersFromServer) {
                            updateLogFolderState(logFolder);
                        }
                        updateLogStateIconForAllLogFolders();
                        NeptusLog.pub().info(".......Updating LogFolders State " +
                                (System.currentTimeMillis() - timeF2) + "ms");

                        long timeF3 = System.currentTimeMillis();
                        // updateFilesListGUIForFolderSelected();
                        new Thread("updateFilesListGUIForFolderSelected") {
                            @Override
                            public void run() {
                                updateFilesListGUIForFolderSelected();
                            };
                        }.start();
                        NeptusLog.pub().info(".......updateFilesListGUIForFolderSelected " +
                                (System.currentTimeMillis() - timeF3) + "ms");

                        NeptusLog.pub().info("....process list from all servers " + (System.currentTimeMillis() - timeD3) + "ms");                        

                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                listHandlingProgressBar.setValue(90);
                                listHandlingProgressBar.setIndeterminate(false);
                                listHandlingProgressBar.setString(I18n.text("Updating GUI..."));
                            }
                        });
                        logFolderList.invalidate();
                        logFolderList.revalidate();
                        logFolderList.repaint();
                        logFolderList.setEnabled(true);
                        // logFilesList.invalidate();
                        // logFilesList.revalidate();
                        // logFilesList.repaint();
                        logFilesList.setEnabled(true);

                        NeptusLog.pub().info("....all downloadListAction " + (System.currentTimeMillis() - time) + "ms");
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                listHandlingProgressBar.setValue(100);
                                listHandlingProgressBar.setIndeterminate(false);
                                listHandlingProgressBar.setString(I18n.text("Done"));
                            }
                        });
                        return true;
                    }

                    @Override
                    public void finish() {
                        stopLogListProcessing = false;

                        logFolderList.setValueIsAdjusting(false);
                        logFolderList.invalidate();
                        logFolderList.revalidate();
                        logFolderList.repaint();
                        logFolderList.setEnabled(true);
                        // logFilesList.invalidate();
                        // logFilesList.revalidate();
                        // logFilesList.repaint();
                        listHandlingProgressBar.setValue(0);
                        listHandlingProgressBar.setIndeterminate(false);
                        listHandlingProgressBar.setString("");
                        logFilesList.setEnabled(true);
                        downloadListButton.setEnabled(true);
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
                if (!validateAndSetUI()) {
                    popupErrorConfigurationDialog();
                    return;
                }
                downloadSelectedLogDirsButton.setEnabled(false);
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        for (Object comp : logFolderList.getSelectedValues()) {
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
                        downloadSelectedLogDirsButton.setEnabled(true);
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
                if (!validateAndSetUI()) {
                    popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        downloadSelectedLogFilesButton.setEnabled(false);

                        for (Object comp : logFilesList.getSelectedValues()) {
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
                        downloadSelectedLogFilesButton.setEnabled(true);
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
                if (!validateAndSetUI()) {
                    popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        deleteSelectedLogFoldersButton.setEnabled(false);
                        // logFolderList.setEnabled(false);
                        // logFilesList.setEnabled(false);

                        Object[] objArray = logFolderList.getSelectedValues();
                        if (objArray.length == 0)
                            return null;

                        JOptionPane jop = new JOptionPane(I18n.text("Are you sure you want to delete "
                                + "selected log folders from remote system?"), JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.YES_NO_OPTION);
                        JDialog dialog = jop.createDialog(frameCompHolder, I18n.text("Remote Delete Confirmation"));
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
                        deleteSelectedLogFoldersButton.setEnabled(true);
                        logFilesList.revalidate();
                        logFilesList.repaint();
                        logFilesList.setEnabled(true);
                        logFolderList.revalidate();
                        logFolderList.repaint();
                        logFolderList.setEnabled(true);
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
                if (!validateAndSetUI()) {
                    popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        deleteSelectedLogFilesButton.setEnabled(false);

                        Object[] objArray = logFilesList.getSelectedValues();
                        if (objArray.length == 0)
                            return null;

                        JOptionPane jop = new JOptionPane(
                                I18n.text("Are you sure you want to delete selected log files from remote system?"),
                                JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
                        JDialog dialog = jop.createDialog(frameCompHolder, I18n.text("Remote Delete Confirmation"));
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
                        deleteSelectedLogFilesButton.setEnabled(true);
                        logFilesList.revalidate();
                        logFilesList.repaint();
                        logFilesList.setEnabled(true);
                        logFolderList.revalidate();
                        logFolderList.repaint();
                        logFolderList.setEnabled(true);
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
                configCollapsiblePanel.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION).actionPerformed(e);
            }
        };

        toggleExtraInfoPanelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                extraInfoCollapsiblePanel.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION).actionPerformed(e);
            }
        };

        helpAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GuiUtils.centerOnScreen(downHelpDialog.getDialog());
                downHelpDialog.getDialog().setIconImage(ICON_HELP.getImage());
                downHelpDialog.getDialog().setVisible(true);
            }
        };

        resetAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetButton.setEnabled(false);
                doReset(false);
            }
        };

        stopAllAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAllButton.setEnabled(false);
                doReset(true);
            }
        };

        turnCameraOn = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ArrayList<EntityParameter> propList = new ArrayList<>();
                    EntityParameter entParsm = new EntityParameter().setName("Active")
                            .setValue(cameraButton.getBackground() != CAM_CPU_ON_COLOR ? "true" : "false");
                    propList.add(entParsm);
                    SetEntityParameters setParams = new SetEntityParameters();
                    setParams.setName(CAMERA_CPU_LABEL);
                    setParams.setParams(propList);

                    ImcMsgManager.getManager().sendMessageToSystem(setParams, getLogLabel());
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
    }

    /**
     * @return the content panel. Use it only if you give an external frame in the constructor.
     */
    public JXPanel getContentPanel() {
        return frameCompHolder;
    }

    /**
     * @return
     */
    private Runnable getTimerTaskLocalDiskSpace() {
        if (ttaskLocalDiskSpace == null) {
            ttaskLocalDiskSpace = new Runnable() {
                @Override
                public void run() {
                    try {
                        File fxD = new File(dirBaseToStoreFiles);
                        long tspace = fxD.getTotalSpace();
                        long uspace = fxD.getUsableSpace();
                        if (tspace != 0 /* && uspace != 0 */) {
                            String tSpStr = MathMiscUtils.parseToEngineeringRadix2Notation(tspace, 2) + "B";
                            String uSpStr = MathMiscUtils.parseToEngineeringRadix2Notation(uspace, 2) + "B";
                            double pFree = 1.0 * (tspace - uspace) / tspace;
                            diskFreeLabel.setText("<html><b>" + uSpStr);
                            diskFreeLabel.setToolTipText(I18n.textf("Local free disk space %usedspace of %totalspace",
                                    uSpStr, tSpStr));
                            updateDiskFreeLabelBackColor(diskFreeColorMap.getColor(pFree));
                            return;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    diskFreeLabel.setText("<html><b>?");
                    diskFreeLabel.setToolTipText(I18n.text("Unknown local disk free space"));
                    updateDiskFreeLabelBackColor(Color.LIGHT_GRAY);

                    // Queue block test
                    ArrayList<DownloaderPanel> workingDonsloaders = queueWorkTickets.getAllWorkingClients();
                    Component[] components = downloadWorkersHolder.getComponents();
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
        }
        return ttaskLocalDiskSpace;
    }

    private FtpDownloader getOrRenewFtpDownloader(FtpDownloader clientFtp, String host, int port) throws Exception {
        if (clientFtp == null)
            clientFtp = new FtpDownloader(host, port);
        else
            clientFtp.setHostAndPort(host, port);

        if (!clientFtp.isConnected())
            clientFtp.renewClient();

        return clientFtp;
    }

    /**
     * This is used to clean and dispose safely of this component
     */
    public void cleanup() {
        disconnectFTPClientsForListing();

        if (threadScheduledPool != null) {
            threadScheduledPool.shutdownNow();
        }
        if (frame != null) {
            if (!frameIsExternalControlled) {
                frame.dispose();
                frame = null;
            }
            else {
                frame = null;
            }
        }

        if (downHelpDialog != null)
            downHelpDialog.dispose();

        ImcMsgManager.getManager().removeListener(messageListener);

        queueWorkTickets.cancelAll();
    }

    private void disconnectFTPClientsForListing() {
        stopLogListProcessing = true;
        if (clientFtp != null && clientFtp.isConnected()) {
            try {
                clientFtp.getClient().disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (cameraFtp != null && cameraFtp.isConnected()) {
            try {
                cameraFtp.getClient().disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
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

    /**
     * @return the rectPainter
     */
    private RectanglePainter getRectPainter() {
        if (rectPainter == null) {
            rectPainter = new RectanglePainter(0, 0, 0, 0, 10, 10);
            rectPainter.setFillPaint(Color.LIGHT_GRAY);
            rectPainter.setBorderPaint(Color.LIGHT_GRAY.darker().darker().darker());
            rectPainter.setStyle(RectanglePainter.Style.BOTH);
            rectPainter.setBorderWidth(2);
            rectPainter.setAntialiasing(true);
        }
        return rectPainter;
    }

    /**
     * @return the compoundBackPainter
     */
    private CompoundPainter<JXPanel> getCompoundBackPainter() {
        compoundBackPainter = new CompoundPainter<JXPanel>(getRectPainter(), new GlossPainter());
        return compoundBackPainter;
    }

    /**
     * @param color
     */
    private void updateDiskFreeLabelBackColor(Color color) {
        getRectPainter().setFillPaint(color);
        getRectPainter().setBorderPaint(color.darker());

        diskFreeLabel.setBackgroundPainter(getCompoundBackPainter());
    }

    private void popupErrorConfigurationDialog() {
        JOptionPane jop = new JOptionPane(I18n.text("Some of the configuration parameters are not correct!"),
                JOptionPane.ERROR_MESSAGE);
        JDialog dialog = jop.createDialog(frameCompHolder, I18n.text("Error on configuration"));
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setVisible(true);
    }

    /**
     * @return
     */
    public boolean validateConfiguration() {
        if ("".equalsIgnoreCase(hostField.getText())) {
            return false;
        }
        if ("".equalsIgnoreCase(portField.getText())) {
            return false;
        }
        else {
            try {
                Integer.parseInt(portField.getText());
            }
            catch (NumberFormatException e) {
                NeptusLog.pub().debug(e.getMessage());
                return false;
            }
        }
        if ("".equalsIgnoreCase(logLabelField.getText()))
            return false;
        return true;
    }

    /**
     * @return
     */
    private boolean validateAndSetUI() {
        int iPort = DEFAULT_PORT;
        if ("".equalsIgnoreCase(hostField.getText())) {
            return false;
        }
        if ("".equalsIgnoreCase(portField.getText())) {
            return false;
        }
        else {
            try {
                iPort = Integer.parseInt(portField.getText());
            }
            catch (NumberFormatException e) {
                NeptusLog.pub().debug(e.getMessage());
                return false;
            }
        }
        if ("".equalsIgnoreCase(logLabelField.getText()))
            return false;

        host = hostField.getText();
        port = iPort;
        logLabel = logLabelField.getText();
        if ("".equalsIgnoreCase(logLabel))
            logLabel = I18n.text("unknown");
        return true;
    }

    public String getHost() {
        return host;
    }

    /**
     * @param host
     */
    public void setHost(String host) {
        this.host = host;
        hostField.setText(host);
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
        portField.setText("" + port);
    }

    public String getLogLabel() {
        return logLabel;
    }

    /**
     * @param logLabel
     */
    public void setLogLabel(String logLabel) {
        this.logLabel = logLabel;
        logLabelField.setText(logLabel);
        if (!frameIsExternalControlled)
            frame.setTitle(DEFAULT_TITLE + " - " + logLabel);
    }

    /**
     * @param show
     */
    public void setVisible(boolean show) {
        frame.setVisible(show);
        if (show)
            frame.setState(Frame.NORMAL);
    }

    /**
     * @param lfx
     * @return
     */
    private LogFolderInfo findLogFolderInfoForFile(LogFileInfo lfx) {
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

    private void updateFilesListGUIForFolderSelected() {
        if (isUpdatingFileList)
            exitRequest = true;
        synchronized (lock) {
            isUpdatingFileList = true;
            exitRequest = false;

            logFilesList.setValueIsAdjusting(true);

            final LinkedHashSet<LogFileInfo> validFiles = new LinkedHashSet<LogFileInfo>();
            for (Object comp : logFolderList.getSelectedValues()) {
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

            logFilesList.setIgnoreRepaint(true);
            try {
                if (SwingUtilities.isEventDispatchThread()) {
                    if (exitRequest)
                        return;
                    logFilesList.myModel.clear();
                }
                else {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            if (exitRequest)
                                return;
                            logFilesList.myModel.clear();
                        }
                    });
                }
                for (final LogFileInfo fxS : validFiles) {
                    if (SwingUtilities.isEventDispatchThread()) {
                        if (exitRequest)
                            return;
                        logFilesList.addFile(fxS);
                    }
                    else {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                if (exitRequest)
                                    return;
                                logFilesList.addFile(fxS);
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
            logFilesList.setIgnoreRepaint(false);

            logFilesList.setValueIsAdjusting(false);
            logFilesList.invalidate();
            logFilesList.validate();
            isUpdatingFileList = false;
        }
    }

    /**
     * @param newLogFoldersFromServer
     */
    private void testNewReportedLogFoldersForLocalCorrespondent(LinkedList<LogFolderInfo> newLogFoldersFromServer) {
        for (LogFolderInfo lf : newLogFoldersFromServer) {
            File testFile = new File(getDirTarget(), lf.getName());
            if (testFile.exists()) {
                if (lf.getState() == LogFolderInfo.State.DOWNLOADING)
                    continue;

                lf.setState(LogFolderInfo.State.UNKNOWN);
                for (LogFileInfo lfx : lf.getLogFiles()) {
                    File testFx = new File(getDirTarget(), lfx.getName());
                    if (testFx.exists()) {
                        lfx.setState(LogFolderInfo.State.UNKNOWN);
                        long sizeD = getDiskSizeFromLocal(lfx);
                        if (lfx.getSize() == sizeD) {
                            lfx.setState(LogFolderInfo.State.SYNC);
                        }
                        else {
                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                            System.out.println("//////////// " + lfx + "  incomplete " + lfx.getSize());
                        }
                    }
                }
                updateLogFolderState(lf);
            }
            else {
                lf.setState(LogFolderInfo.State.NEW);
            }
        }

        updateLogStateIconForAllLogFolders();
    }

    /**
     * @param fx
     * @return Negative values for errors (HTTP like returns).
     */
    private long getDiskSizeFromLocal(LogFileInfo fx) {
        File fileTarget = getFileTarget(fx.getName());
        if (fileTarget == null)
            return -1;
        else if (fileTarget.exists()) {
            if (fileTarget.isFile()) {
                return fileTarget.length();
            }
            else if (fileTarget.isDirectory()) {
                long allSize = 0;
                for (LogFileInfo dirFileInfo : fx.getDirectoryContents()) {
                    long dfSize = getDiskSizeFromLocal(dirFileInfo);
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
     * @param lfx
     * @param logFd
     */
    private void singleLogFileDownloadWorker(LogFileInfo lfx, LogFolderInfo logFd) {
        if (lfx.getState() == LogFolderInfo.State.SYNC // || lfx.getState() == LogFolderInfo.State.DOWNLOADING
                || lfx.getState() == LogFolderInfo.State.LOCAL) {
            return;
        }

        // Let us see if already exists in download list
        Component[] components = downloadWorkersHolder.getComponents();
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
            FtpDownloader ftpDownloader = null;
            ftpDownloader = new FtpDownloader(lfx.getHost(), port);

            if (lfx.isDirectory()) {
                HashMap<String, FTPFile> directoryContentsList = new LinkedHashMap<>();
                for (LogFileInfo lfi : lfx.getDirectoryContents()) {
                    directoryContentsList.put(lfi.getUriPartial(), lfi.getFile());
                }
                workerD = new DownloaderPanel(ftpDownloader, lfx.getFile(), lfx.getName(),
                        getFileTarget(lfx.getName()), directoryContentsList, threadScheduledPool, queueWorkTickets);
            }
            else {
                workerD = new DownloaderPanel(ftpDownloader, lfx.getFile(), lfx.getName(),
                        getFileTarget(lfx.getName()), threadScheduledPool, queueWorkTickets);
            }
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
        final LogFolderInfo lfdfinal = logFd;
        final LogFileInfo lfxfinal = lfx;
        final DownloaderPanel workerDFinal = workerD;
        workerD.addStateChangeListener(new DownloadStateListener() {
            private LogFileInfo fxLog = lfxfinal;
            private Runnable task = null;

            @Override
            public void downloaderStateChange(DownloaderPanel.State newState, DownloaderPanel.State oldState) {
                //                System.out.println("State state update for " + fxLog.getUriPartial() + " " + fxLog.getState() + "::" + oldState + "::" + newState);

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
                    else if (newState == DownloaderPanel.State.IDLE)
                        ;// fxLog.setState(LogFolderInfo.State.ERROR);

                    if (logFilesList.containsFile(fxLog)) {
                        logFilesList.revalidate();
                        logFilesList.repaint();
                    }

                    updateLogFolderState(lfdfinal);
                    updateLogStateIconForAllLogFolders();
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
                    //                    timer.schedule(task, DELTA_TIME_TO_CLEAR_DONE);
                    threadScheduledPool.schedule(task, DELTA_TIME_TO_CLEAR_DONE, TimeUnit.MILLISECONDS);
                }
                else { //if (newState != DownloaderPanel.State.WORKING && newState != DownloaderPanel.State.TIMEOUT && newState != DownloaderPanel.State.QUEUED) { // FIXME VERIFICAR SE OK OU TIRAR
                    cancelTasksIfSchedule();
                    task = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (workerDFinal.getState() != DownloaderPanel.State.WORKING
                                        && workerDFinal.getState() != DownloaderPanel.State.TIMEOUT
                                        && workerDFinal.getState() != DownloaderPanel.State.QUEUED) {
                                    workerDFinal.doStopAndInvalidate();
                                    //                                    waitForStopOnAllLogFoldersDownloads(workerDFinal.getName());
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
        });
        downloadWorkersHolder.add(workerD);
        downloadWorkersHolder.revalidate();
        downloadWorkersHolder.repaint();
        workerD.actionDownload();
    }

    private void updateLogStateIconForAllLogFolders() {
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
     * @param logFolder
     */
    private void updateLogFolderState(LogFolderInfo logFolder) {
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
     * @param logFd
     * @return
     */
    private boolean deleteLogFolderFromServer(LogFolderInfo logFd) {
        String path = logFd.getName();
        boolean ret = deleteLogFolderFromServer(path);
        ret |= deleteLogFolderFromCameraServer(path);
        return ret;
    }

    /**
     * @param logFx
     * @return
     */
    private boolean deleteLogFileFromServer(LogFileInfo logFx) {
        String path = logFx.getName();
        String hostFx = logFx.getHost();
        // Not the best way but for now lets try like this
        if (hostFx.equals(host))
            return deleteLogFolderFromServer(path);
        else if (hostFx.equals(getCameraHost(host)))
            return deleteLogFolderFromCameraServer(path);
        else
            return false;
    }

    /**
     * @param path
     * @return
     */
    private boolean deleteLogFolderFromServer(String path) {
        try {
            System.out.println("Deleting folder");
            try {
                clientFtp = getOrRenewFtpDownloader(clientFtp, host, port);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return clientFtp.getClient().deleteFile("/" + path);
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteLogFolderFromCameraServer(String path) {
        try {
            if (cameraFtp != null) {
                try {
                    cameraFtp = getOrRenewFtpDownloader(cameraFtp, getCameraHost(host), port);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return cameraFtp.getClient().deleteFile("/" + path);
            }
            else
                return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param docList
     * @param serversLogPresenceList
     * @return
     */
    private LinkedList<LogFolderInfo> getLogFileList(LinkedHashMap<String, String> serversLogPresenceList) {

        if (serversLogPresenceList.size() == 0)
            return new LinkedList<LogFolderInfo>();

        LinkedList<LogFolderInfo> tmpLogFolders = new LinkedList<LogFolderInfo>();

        String cameraHost = getCameraHost(getHost());

        System.out.println(LogsDownloaderWorker.class.getSimpleName() + " :: " + cameraHost + " " + getLogLabel());

        try {
            for (String logDir : serversLogPresenceList.keySet()) {
                if (!serversLogPresenceList.get(logDir).contains(SERVER_MAIN))
                    continue;

                String isoStr = new String(logDir.getBytes(), "ISO-8859-1");
                //                boolean ret = clientFtp.getClient().changeWorkingDirectory("/" + isoStr + "/");
                //                if (!ret)
                //                    continue;
                LogFolderInfo lFolder = new LogFolderInfo(logDir);

                // Updating the LogFiles for each LogFolder
                FTPFile[] files = clientFtp.getClient().listFiles("/" + isoStr + "/");
                for (FTPFile file : files) {
                    String name = logDir + "/" + file.getName();
                    String uriPartial = logDir + "/" + file.getName();
                    LogFileInfo logFileTmp = new LogFileInfo(name);
                    logFileTmp.setUriPartial(uriPartial);
                    logFileTmp.setSize(file.getSize());
                    logFileTmp.setFile(file);
                    logFileTmp.setHost(getHost());
                    // Let us see if its a directory
                    if (file.isDirectory()) {
                        logFileTmp.setSize(-1); // Set size to -1 if directory
                        long allSize = 0;

                        LinkedHashMap<String, FTPFile> dirListing = clientFtp.listDirectory(logFileTmp.getName()); // Here there are no directories
                        ArrayList<LogFileInfo> directoryContents = new ArrayList<>();
                        for (String fName : dirListing.keySet()) {
                            FTPFile fFile = dirListing.get(fName);
                            String fURIPartial = fName;
                            LogFileInfo fLogFileTmp = new LogFileInfo(fName);
                            fLogFileTmp.setUriPartial(fURIPartial);
                            fLogFileTmp.setSize(fFile.getSize());
                            fLogFileTmp.setFile(fFile);
                            fLogFileTmp.setHost(getHost());

                            allSize += fLogFileTmp.getSize();
                            directoryContents.add(fLogFileTmp);
                        }
                        logFileTmp.setDirectoryContents(directoryContents);
                        logFileTmp.setSize(allSize);
                    }
                    lFolder.addFile(logFileTmp);
                    tmpLogFolders.add(lFolder);
                }
            }

            // REDO the same thing if cameraHost exists with the difference of a another client
            if (cameraHost != null && cameraFtp != null) {
                FtpDownloader ftpd = cameraFtp; // new FtpDownloader(cameraHost, port);
                for (String logDir : serversLogPresenceList.keySet()) {
                    if (!serversLogPresenceList.get(logDir).contains(SERVER_CAM))
                        continue;

                    String isoStr = new String(logDir.getBytes(), "ISO-8859-1");
                    //                    if (ftpd.getClient().changeWorkingDirectory("/" + isoStr + "/") == false) // Log doesnt exist in
                    //                        // DOAM
                    //                        continue;

                    LogFolderInfo lFolder = null;

                    for (LogFolderInfo lfi : tmpLogFolders) {
                        if (lfi.getName().equals(logDir))
                            lFolder = lfi;
                    }
                    if (lFolder == null) {
                        lFolder = new LogFolderInfo(logDir);
                    }

                    if (!ftpd.isConnected())
                        ftpd.renewClient();

                    try {
                        for (FTPFile file : ftpd.getClient().listFiles("/" + isoStr + "/")) {
                            String name = logDir + "/" + file.getName();
                            String uriPartial = logDir + "/" + file.getName();
                            LogFileInfo logFileTmp = new LogFileInfo(name);
                            logFileTmp.setUriPartial(uriPartial);
                            logFileTmp.setSize(file.getSize());
                            logFileTmp.setFile(file);
                            logFileTmp.setHost(cameraHost);
                            // Let us see if its a directory
                            if (file.isDirectory()) {
                                logFileTmp.setSize(-1); // Set size to -1 if directory
                                long allSize = 0;

                                LinkedHashMap<String, FTPFile> dirListing = ftpd.listDirectory(logFileTmp.getName()); // Here there are no directories
                                ArrayList<LogFileInfo> directoryContents = new ArrayList<>();
                                for (String fName : dirListing.keySet()) {
                                    FTPFile fFile = dirListing.get(fName);
                                    String fURIPartial = fName;
                                    LogFileInfo fLogFileTmp = new LogFileInfo(fName);
                                    fLogFileTmp.setUriPartial(fURIPartial);
                                    fLogFileTmp.setSize(fFile.getSize());
                                    fLogFileTmp.setFile(fFile);
                                    fLogFileTmp.setHost(cameraHost);

                                    allSize += fLogFileTmp.getSize();
                                    directoryContents.add(fLogFileTmp);
                                }
                                logFileTmp.setDirectoryContents(directoryContents);
                                logFileTmp.setSize(allSize);
                            }
                            lFolder.addFile(logFileTmp);
                            tmpLogFolders.add(lFolder);
                        }
                    }
                    catch (Exception e) {
                        System.err.println(isoStr);
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // NeptusLog.pub().info("<###>.......getLogListAsTemporaryStructureFromDOM " +
        // (System.currentTimeMillis()-time));
        return tmpLogFolders;
    }

    /**
     * @param name
     * @return
     */
    private File getFileTarget(String name) {
        File outFile = new File(getDirTarget(), name);
        // outFile.getParentFile().mkdirs(); Taking this out to not create empty folders
        return outFile;
    }

    /**
     * @return
     */
    private File getDirTarget() {
        File dirToStore = new File(dirBaseToStoreFiles);
        dirToStore.mkdirs();
        File dirTarget = new File(dirToStore, logLabel);
        // dirTarget.mkdirs(); Taking this out to not create empty folders
        return dirTarget;
    }

    // --------------------------------------------------------------

    /**
     * 
     */
    private void cleanInterface() {
        logFilesList.myModel.clear();
        logFolderList.myModel.clear();
        downloadWorkersHolder.removeAll();

        // Protected against disable problems
        downloadListButton.setEnabled(true);
        downloadSelectedLogDirsButton.setEnabled(true);
        downloadSelectedLogFilesButton.setEnabled(true);
        deleteSelectedLogFoldersButton.setEnabled(true);
        deleteSelectedLogFilesButton.setEnabled(true);

        logFoldersListLabel.setIcon(null);
    }

    // --------------------------------------------------------------

    /**
     * @param visible
     */
    public void setVisibleHost(boolean visible) {
        hostField.setVisible(visible);
        hostLabel.setVisible(visible);
    }

    /**
     * @param visible
     */
    public void setVisiblePort(boolean visible) {
        portField.setVisible(visible);
        portLabel.setVisible(visible);
    }

    /**
     * @param visible
     */
    public void setVisibleLogLabel(boolean visible) {
        logLabelField.setVisible(visible);
        logLabelLabel.setVisible(visible);
    }

    /**
     * @param visible
     */
    public void setConfigPanelVisible(boolean visible) {
        configCollapsiblePanel.setCollapsed(!visible);
    }

    /**
     * @param enable
     */
    public void setEnableHost(boolean enable) {
        hostField.setEnabled(enable);
    }

    /**
     * @param enable
     */
    public void setEnablePort(boolean enable) {
        portField.setEnabled(enable);
    }

    /**
     * @param enable
     */
    public void setEnableLogLabel(boolean enable) {
        logLabelField.setEnabled(enable);
    }

    // --------------------------------------------------------------

    private void warnMsg(String message) {
        NudgeGlassPane.nudge(frameCompHolder.getRootPane(), (frameIsExternalControlled ? getLogLabel() + " > " : "")
                + message, 2);
    }

    private void warnLongMsg(String message) {
        NudgeGlassPane.nudge(frameCompHolder.getRootPane(), (frameIsExternalControlled ? getLogLabel() + " > " : "")
                + message, 6);
    }

    // --------------------------------------------------------------
    // Public interface methods

    public boolean doUpdateListFromServer() {
        downloadListButton.doClick(100);
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
            if (logFolderList.containsFolder(new LogFolderInfo(str)))
                folders.add(logFolderList.getFolder(str));
        }
        if (folders.size() == 0)
            return false;
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                logFolderList.setEnabled(false);
                logFolderList.clearSelection();
                logFolderList.setValueIsAdjusting(true);
                for (LogFolderInfo logFd : folders) {
                    // logFolderList.setSelectedValue(logFd, false);
                    int iS = logFolderList.myModel.indexOf(logFd);
                    iS = logFolderList.convertIndexToView(iS);
                    logFolderList.addSelectionInterval(iS, iS);
                }
                logFolderList.setValueIsAdjusting(false);
                if (downloadOrDelete)
                    downloadSelectedLogDirsButton.doClick(100);
                else
                    deleteSelectedLogFoldersButton.doClick(100);
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
                logFolderList.setEnabled(true);
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
        for (Enumeration<?> iterator = logFolderList.myModel.elements(); iterator.hasMoreElements();) {
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
        for (Enumeration<?> iterator = logFolderList.myModel.elements(); iterator.hasMoreElements();) {
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
        for (Enumeration<?> iterator = logFolderList.myModel.elements(); iterator.hasMoreElements();) {
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
        for (Enumeration<?> iterator = logFolderList.myModel.elements(); iterator.hasMoreElements();) {
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
        Component[] components = downloadWorkersHolder.getComponents();
        for (Component cp : components) {
            try {
                DownloaderPanel workerD = (DownloaderPanel) cp;
                //                if (workerD.getState() == DownloaderPanel.State.WORKING) {
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
                //                }
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
        if (logList != null)
            if (logList.length > 0)
                waitStopAll = false;
        Component[] components = downloadWorkersHolder.getComponents();
        for (Component cp : components) {
            try {
                DownloaderPanel workerD = (DownloaderPanel) cp;
                boolean wait = false;
                if (workerD.getState() == DownloaderPanel.State.WORKING || workerD.getState() == DownloaderPanel.State.QUEUED) {
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

                    while (workerD.getState() == DownloaderPanel.State.WORKING || workerD.getState() == DownloaderPanel.State.QUEUED) {
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
                    resetting = true;

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
                            warnLongMsg(I18n.textf("Error couth on resetting: %errormessage", e.getMessage()));
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
                            warnLongMsg(I18n.textf("Error couth on resetting: %errormessage", e.getMessage()));
                        }
                    });
                    resetRes &= false;
                }
                try {
                    if (!justStopDownloads) {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                cleanInterface();
                            }
                        });
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    warnLongMsg(I18n.textf("Error couth on resetting: %errormessage", e.getMessage()));
                    resetRes &= false;
                }

                queueWorkTickets.cancelAll();

                if (!justStopDownloads) {
                    resetting = false;
                }

                return resetRes;
            }

            @Override
            protected void done() {
                if (!justStopDownloads) {
                    resetButton.setEnabled(true);
                }
                else {
                    stopAllButton.setEnabled(true);
                    updateLogStateIconForAllLogFolders();
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
     * Used to update the just deleted files from {@link #deleteSelectedLogFoldersAction} or
     * {@link #deleteSelectedLogFilesAction}.
     * 
     * @param logFiles
     * @return
     */
    private LinkedHashSet<LogFileInfo> updateLogFilesStateDeleted(LinkedHashSet<LogFileInfo> logFiles) {
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
                        if (dpp.getState() == DownloaderPanel.State.WORKING || dpp.getState() == DownloaderPanel.State.QUEUED) {
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
            if (!getFileTarget(lfx.getName()).exists()) {
                toDelFL.add(lfx);
                // logFd.getLogFiles().remove(lfx); //This cannot be done here
            }
            lfx.setState(LogFolderInfo.State.LOCAL);
        }
        return toDelFL;
    }

    public String getCameraHost(String mainHost) {
        String cameraHost = null;
        try {
            String[] parts = mainHost.split("\\.");
            parts[3] = "" + (Integer.parseInt(parts[3]) + 3);
            cameraHost = StringUtils.join(parts, ".");
        }
        catch (Exception oops) {
            NeptusLog.pub().error("Could not get camera host string: "+oops.getClass().getSimpleName(), oops);
            cameraHost = "";
        }
        catch (Error oops) {
            NeptusLog.pub().error("Could not get camera host string: "+oops.getClass().getSimpleName(), oops);
            cameraHost = "";
        }

        return cameraHost;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();

        final LogsDownloaderWorker logFetcher = new LogsDownloaderWorker();
        logFetcher.setEnableLogLabel(true);

        logFetcher.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logFetcher.frame.setVisible(true);

        // logFetcher.setHost("10.0.2.90");
        // logFetcher.setPort(8080);
    }
}
