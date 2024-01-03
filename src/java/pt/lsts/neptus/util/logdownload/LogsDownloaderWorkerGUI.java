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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;

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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.MiniButton;
import pt.lsts.neptus.gui.swing.MessagePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
class LogsDownloaderWorkerGUI {

    public static final ImageIcon ICON_DOWNLOAD_FOLDERS = ImageUtils.getScaledIcon(
            "images/downloader/folder_download.png", 32, 32);
    public static final ImageIcon ICON_DOWNLOAD_FILES = ImageUtils.getScaledIcon("images/downloader/file_down.png", 32,
            32);
    public static final ImageIcon ICON_DOWNLOAD_LIST = ImageUtils.getScaledIcon("images/downloader/sync-list.png", 32,
            32);
    public static final ImageIcon ICON_DOWNLOAD_LIST_STOP = ImageUtils.getScaledIcon("images/downloader/sync-list-stop.png", 32,
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

    private LogsDownloaderWorker worker;
    
    // UI
    JFrame frame = null;
    JXPanel frameCompHolder = null;
    JTextField hostField = null;
    JTextField portField = null;
    JTextField logLabelField = null;
    JLabel hostLabel = null;
    JLabel portLabel = null;
    JLabel logLabelLabel = null;
    MessagePanel msgPanel = null;
    JXLabel logFoldersListLabel = null;
    JXLabel logFilesListLabel = null;
    JPanel downloadWorkersHolder = null;
    JScrollPane downloadWorkersScroll = null;
    LogFolderInfoList logFolderList = null;
    JScrollPane logFolderScroll = null;
    LogFileInfoList logFilesList = null;
    JScrollPane logFilesScroll = null;

    JXLabel diskFreeLabel = null;

    MiniButton downloadListButton = null;
    MiniButton downloadSelectedLogDirsButton = null;
    MiniButton downloadSelectedLogFilesButton = null;
    MiniButton deleteSelectedLogFoldersButton = null;
    MiniButton deleteSelectedLogFilesButton = null;

    MiniButton toggleConfPanelButton = null;
    MiniButton toggleExtraInfoPanelButton = null;

    MiniButton helpButton = null;
    MiniButton resetButton = null;
    MiniButton stopAllButton = null;

    JButton cameraButton = null;

    DownloaderHelp downHelpDialog = null;

    JXPanel configHolder = null;
    JXCollapsiblePane configCollapsiblePanel = null;
    JXCollapsiblePane extraInfoCollapsiblePanel = null;

    JProgressBar listHandlingProgressBar = null;

    // Background Painter Stuff
    private RectanglePainter rectPainter;
    private CompoundPainter<JXPanel> compoundBackPainter;
    
    boolean frameIsExternalControlled = false;

    public LogsDownloaderWorkerGUI(LogsDownloaderWorker worker) {
        this(worker, null);
    }

    public LogsDownloaderWorkerGUI(LogsDownloaderWorker worker, JFrame parentFrame) {
        this.worker = worker;
        
        if (parentFrame != null) {
            frame = parentFrame;
            frameIsExternalControlled = true;
        }
        
        initialize();
    }

    private void initialize() {
        if (frame == null) {
            frame = new JFrame();
            frame.setSize(900, 560);
            frame.setIconImages(ConfigFetch.getIconImagesForFrames());
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }
        
        initializeCollapsibleConfigPanel();

        msgPanel = new MessagePanel();
        msgPanel.showButtons(false);

        logFoldersListLabel = new JXLabel("<html><b>" + I18n.text("Log Folders"), JLabel.CENTER);
        logFilesListLabel = new JXLabel("<html><b>" + I18n.text("Log Files"), JLabel.CENTER);

        diskFreeLabel = new JXLabel("<html><b>?", JLabel.CENTER);
        diskFreeLabel.setBackgroundPainter(getCompoundBackPainter());

        initializeButtons();

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
//        logFolderList.addListSelectionListener(new ListSelectionListener() {
//            @Override
//            public void valueChanged(ListSelectionEvent e) {
//                if (e.getValueIsAdjusting())
//                    return;
//                AsyncTask task = new AsyncTask() {
//                    @Override
//                    public Object run() throws Exception {
//                        updateFilesListGUIForFolderSelected();
//                        return null;
//                    }
//
//                    @Override
//                    public void finish() {
//                        logFilesList.setValueIsAdjusting(false);
//                        logFilesList.invalidate();
//                        logFilesList.validate();
//                        logFilesList.setEnabled(true);
//                    }
//                };
//                AsyncWorker.getWorkerThread().postTask(task);
//            }
//        });
        // logFolderList.addMouseListener(LogsDownloaderUtil.createOpenLogInMRAMouseListener(LogsDownloaderWorker.this, logFolderList));

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


        listHandlingProgressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        listHandlingProgressBar.setIndeterminate(false);
        listHandlingProgressBar.setStringPainted(true);
        listHandlingProgressBar.setString("");

        // Setup main content panel
        initializeMainPanel();

        downHelpDialog = new DownloaderHelp(frame);
    }

    private void initializeButtons() {
        resetButton = new MiniButton();
        resetButton.setToolTipText(I18n.text("Reset the interface"));
        resetButton.setIcon(ICON_RESET);
//        resetButton.addActionListener(resetAction);

        stopAllButton = new MiniButton();
        stopAllButton.setToolTipText(I18n.text("Stop all log downloads"));
        stopAllButton.setIcon(ICON_STOP);
//        stopAllButton.addActionListener(stopAllAction);

        cameraButton = new JButton();
        cameraButton.setToolTipText(I18n.text("Turn on/off camera CPU"));
        cameraButton.setIcon(ICON_DOWNLOAD_PHOTO);
//        cameraButton.addActionListener(turnCameraOn);

        downloadListButton = new MiniButton() {
            private static final long serialVersionUID = 1487342520662303342L;

            @Override
            public void setState(boolean state) {
                super.setState(state);
                this.setIcon(state ? LogsDownloaderWorkerGUI.ICON_DOWNLOAD_LIST_STOP
                        : LogsDownloaderWorkerGUI.ICON_DOWNLOAD_LIST);
            }
        };
        downloadListButton.setToggle(true);
        downloadListButton.setToolTipText(I18n.text("Synchronize List of Log Folders"));
        downloadListButton.setIcon(ICON_DOWNLOAD_LIST);
        // downloadListButton.addActionListener(downloadListAction);

        downloadSelectedLogDirsButton = new MiniButton();
        downloadSelectedLogDirsButton.setToolTipText(I18n.text("Synchronize Selected Log Folders"));
        downloadSelectedLogDirsButton.setIcon(ICON_DOWNLOAD_FOLDERS);
        // downloadSelectedLogDirsButton.addActionListener(downloadSelectedLogDirsAction);

        downloadSelectedLogFilesButton = new MiniButton();
        downloadSelectedLogFilesButton.setToolTipText(I18n.text("Synchronize Selected Log Files"));
        downloadSelectedLogFilesButton.setIcon(ICON_DOWNLOAD_FILES);
        // downloadSelectedLogFilesButton.addActionListener(downloadSelectedLogFilesAction);

        deleteSelectedLogFoldersButton = new MiniButton();
        deleteSelectedLogFoldersButton.setToolTipText(I18n.text("Delete Selected Log Folders"));
        deleteSelectedLogFoldersButton.setIcon(ICON_DELETE_FOLDERS);
        // deleteSelectedLogFoldersButton.addActionListener(deleteSelectedLogFoldersAction);

        deleteSelectedLogFilesButton = new MiniButton();
        deleteSelectedLogFilesButton.setToolTipText(I18n.text("Delete Selected Log Files"));
        deleteSelectedLogFilesButton.setIcon(ICON_DELETE_FILES);
        // deleteSelectedLogFilesButton.addActionListener(deleteSelectedLogFilesAction);

        // Collapsible Panel Show/Hide buttons
        toggleConfPanelButton = new MiniButton();
        toggleConfPanelButton.setToolTipText(I18n.text("Show/Hide Configuration Panel"));
        toggleConfPanelButton.setIcon(ICON_SETTINGS);
//        toggleConfPanelButton.addActionListener(toggleConfPanelAction);

        toggleExtraInfoPanelButton = new MiniButton();
        toggleExtraInfoPanelButton.setToolTipText(I18n.text("Show/Hide Download Panel"));
        toggleExtraInfoPanelButton.setIcon(ICON_SETTINGS);
//        toggleExtraInfoPanelButton.addActionListener(toggleExtraInfoPanelAction);

        helpButton = new MiniButton();
        helpButton.setToolTipText(I18n.text("Show Help"));
        helpButton.setIcon(ICON_HELP);
//        helpButton.addActionListener(helpAction);

    }

    private void initializeCollapsibleConfigPanel() {
        hostLabel = new JLabel(I18n.text("Host: "));
        hostField = new JTextField(20);
        hostField.setText(worker.getHost());
        portLabel = new JLabel(I18n.text("Port: "));
        portField = new JTextField(5);
        portField.setText("" + worker.getPort());
        logLabelLabel = new JLabel(I18n.text("System Label: "));
        logLabelField = new JTextField(40);
        logLabelField.setText(worker.getLogLabel());
        logLabelField.setToolTipText(I18n.text("This will dictate the directory where the logs will go."));

        // Config Panel Setup
        configCollapsiblePanel = new JXCollapsiblePane();
        configCollapsiblePanel.setLayout(new BorderLayout());
        configHolder = new JXPanel();
        configHolder.setBorder(new TitledBorder(I18n.text("Configuration")));
        configCollapsiblePanel.add(configHolder, BorderLayout.CENTER);
        
        setupCollapsibleConfigPanelLayout();

        // This is called here (After the group layout configuration) because of an IllegalStateException during collapse redraw
        configCollapsiblePanel.setCollapsed(true);
    }

    private void setupCollapsibleConfigPanelLayout() {
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
    }
    
    private void initializeMainPanel() {
        // Setup main content panel
        JPanel contentPanel = new JPanel();
        setupMainPanelLayout(contentPanel);

        // Setup of the Frame Content
        frameCompHolder = new JXPanel();
        frameCompHolder.setLayout(new BorderLayout());
        frameCompHolder.add(configCollapsiblePanel, BorderLayout.NORTH);
        frameCompHolder.add(contentPanel, BorderLayout.CENTER);

        if (!frameIsExternalControlled) {
            frame.setLayout(new BorderLayout());
            frame.add(frameCompHolder, BorderLayout.CENTER);
        }
    }

    private void setupMainPanelLayout(JPanel contentPanel) {
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
    CompoundPainter<JXPanel> getCompoundBackPainter() {
        compoundBackPainter = new CompoundPainter<JXPanel>(getRectPainter(), new GlossPainter());
        return compoundBackPainter;
    }

    /**
     * @param color
     */
    void updateDiskFreeLabelBackColor(Color color) {
        getRectPainter().setFillPaint(color);
        getRectPainter().setBorderPaint(color.darker());

        diskFreeLabel.setBackgroundPainter(getCompoundBackPainter());
    }

    boolean validateAndSetUI() {
        int iPort = LogsDownloaderWorker.DEFAULT_PORT;
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

        //host = hostField.getText();
        worker.setHost(hostField.getText());
        // port = iPort;
        worker.setPort(iPort);
        // logLabel = logLabelField.getText();
        worker.setLogLabel(logLabelField.getText());
        if ("".equalsIgnoreCase(worker.getLogLabel()))
            worker.setLogLabel(I18n.text("unknown")); //logLabel = I18n.text("unknown");
        return true;
    }

    boolean validateConfiguration() {
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

    void popupErrorConfigurationDialog() {
        JOptionPane jop = new JOptionPane(I18n.text("Some of the configuration parameters are not correct!"),
                JOptionPane.ERROR_MESSAGE);
        JDialog dialog = jop.createDialog(frameCompHolder, I18n.text("Error on configuration"));
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setVisible(true);
    }

    void cleanInterface() {
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
}
