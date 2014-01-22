/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * Jan 20, 2014
 */
package pt.lsts.neptus.mra;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.SystemImcMsgCommInfo;
import pt.lsts.neptus.gui.AboutPanel;
import pt.lsts.neptus.gui.MissionFileChooser;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.WaitPanel;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.lsf.ConcatenateLsfLog;
import pt.lsts.neptus.mra.replay.LogReplay;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.neptus.util.llf.LogUtils.LogValidity;
import pt.lsts.neptus.util.llf.LsfReport;
import pt.lsts.neptus.util.logdownload.LogsDownloaderWorker;
import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;

/**
 * MRA MenuBar
 * 
 * @author ZP
 * @author pdias (LSF)
 * @author jqcorreia
 * @author hfq
 */
@SuppressWarnings("serial")
public class MRAMenuBar {

    private JMenuBar menuBar;
    private JMenu fileMenu, reportMenu, settingsMenu, toolsMenu, helpMenu;

    // private JMenu recentlyOpenFilesMenu = null;

    private AbstractAction openLsf, exit;
    protected AbstractAction genReport;
    private AbstractAction batchReport;
    private AbstractAction preferences;
    private AbstractAction httpDuneDownload, httpVehicleDownload, concatenateLSFLogs, fuseLSFLogs;
    protected AbstractAction setMission;

    private LinkedHashMap<JMenuItem, File> miscFilesOpened;
    private NeptusMRA mra;
    private MRAPanel mraPanel; 

    /**
     * Constructor
     * 
     * @param miscFilesOpened Rencently opened log files
     */
    public MRAMenuBar(NeptusMRA mra) {
        this.mra = mra;
        this.miscFilesOpened = mra.getMiscFilesOpened();
        this.mraPanel = mra.getMraPanel();

        setMenuBar(createMRAMenuBar());
    }

    /**
     * @return the MenuBar
     */
    private JMenuBar createMRAMenuBar() {
        menuBar = new JMenuBar();

        setUpFileMenu();
        setUpReportMenu();
        setUpSettingsMenu();
        setUpToolsMenu();
        setUpHelpMenu();

        menuBar.add(fileMenu);
        menuBar.add(reportMenu);
        menuBar.add(settingsMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * Set up File Menu
     */
    private void setUpFileMenu() {
        fileMenu = new JMenu(I18n.text("File"));

        mra.loadRecentlyOpenedFiles();

        fileMenu.add(mra.getRecentlyOpenFilesMenu());

        openLsf = new AbstractAction(I18n.text("Open LSF log"), 
                ImageUtils.getIcon("images/menus/zipfolder.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser;

                File lastFile = null;

                try {
                    lastFile = miscFilesOpened.size() == 0 ? null : miscFilesOpened.values().iterator().next();
                    if (lastFile != null && !lastFile.isDirectory())
                        lastFile = lastFile.getParentFile();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if(lastFile != null && lastFile.isDirectory() && lastFile.canRead()) {
                    fileChooser = new JFileChooser(lastFile);
                }
                else if (!new File("./log/downloaded/").canRead())
                    fileChooser = new JFileChooser(ConfigFetch.getConfigFile());
                else
                    fileChooser = new JFileChooser(new File("./log/downloaded/"));

                fileChooser.setFileView(new NeptusFileView());
                fileChooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("LSF log files"),
                        new String[] { "lsf", FileUtil.FILE_TYPE_LSF_COMPRESSED, 
                    FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2 }));

                if (fileChooser.showOpenDialog(mra) == JFileChooser.APPROVE_OPTION) {
                    final File log = fileChooser.getSelectedFile();
                    LogValidity validity = LogUtils.isValidLSFSource(log.getParentFile());
                    if (validity != LogUtils.LogValidity.VALID) {
                        String message = null;
                        if(validity == LogValidity.NO_DIRECTORY)
                            message = "No such directory / No read permissions";
                        if(validity == LogValidity.NO_VALID_LOG_FILE)
                            message = "No valid LSF log file present";
                        if(validity == LogValidity.NO_XML_DEFS)
                            message = "No valid XML definition present";

                        GuiUtils.errorMessage(mra, I18n.text("Open LSF log"),
                                I18n.text(message));
                        return;
                    }

                    new Thread("Open Log") {
                        @Override
                        public void run() {
                            mra.openLog(log);
                        };
                    }.start();
                }
                return;
            }
        };
        openLsf.putValue(Action.SHORT_DESCRIPTION, I18n.text("Choose and Open a Lsf log") + ".");

        exit = new AbstractAction(I18n.text("Exit"), ImageUtils.getIcon("images/menus/exit.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (mraPanel != null) {
                    mraPanel.cleanup();
                }
                mra.setVisible(false);
                mra.dispose();
            }
        };
        exit.putValue(Action.SHORT_DESCRIPTION, I18n.text("Exit MRA") + ".");

        fileMenu.add(openLsf);
        fileMenu.addSeparator();
        fileMenu.add(exit);
    }

    /**
     * Set up Report Menu
     */
    private void setUpReportMenu() {
        reportMenu = new JMenu(I18n.text("Report"));
        genReport = new AbstractAction(I18n.text("Save as PDF"), ImageUtils.getIcon("images/menus/document-pdf.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                final File f = new File(System.currentTimeMillis() + ".pdf");
                if (f.exists()) {
                    int resp = JOptionPane.showConfirmDialog(mra,
                            I18n.text("Do you want to overwrite the existing file?"));
                    if (resp != JOptionPane.YES_OPTION)
                        return;
                }
                mra.getBgp().block(true);
                mra.generatePDFReport(f);
            }
        };
        genReport.putValue(Action.SHORT_DESCRIPTION, I18n.text("Generate a pdf file report from Log") + ".");
        reportMenu.add(genReport);
        genReport.setEnabled(false);

        batchReport = new AbstractAction(I18n.text("Batch PDF report"),
                ImageUtils.getIcon("images/menus/document-pdf.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser chooser = new JFileChooser(new File("."));
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int res = chooser.showOpenDialog(mra);

                if (res != JFileChooser.APPROVE_OPTION)
                    return;

                final File f = chooser.getSelectedFile();

                final WaitPanel panel = new WaitPanel();
                panel.start(mra, ModalityType.DOCUMENT_MODAL);

                AsyncTask task = new AsyncTask() {
                    @Override
                    public void finish() {
                        panel.stop();
                        GuiUtils.infoMessage(mra, I18n.text("Batch report ended successfully"), I18n.text("Files saved to")+" "
                                + (new File(".").getAbsolutePath()));
                    }

                    @Override
                    public Object run() throws Exception {
                        LsfReport.generateLogs(f, mraPanel);
                        return null;
                    }
                };
                AsyncWorker.post(task);
            }
        };
        batchReport.putValue(Action.SHORT_DESCRIPTION, I18n.text("Generate report from selected log files") + ".");
        reportMenu.add(batchReport);
    }

    /**
     * Set up Settings Menu
     */
    private void setUpSettingsMenu() {
        settingsMenu = new JMenu(I18n.text("Settings"));
        setMission = new AbstractAction(I18n.text("Set mission"), ImageUtils.getIcon("images/menus/mapeditor.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {

                File f = MissionFileChooser.showOpenMissionDialog(new String[] { "nmis", "nmisz" });
                if (f != null) {
                    MissionType mission = new MissionType(f.getAbsolutePath());
                    if (mraPanel != null) {
                        LogReplay replay = mraPanel.getMissionReplay();
                        if (replay != null) {
                            replay.setMission(mission);
                        }
                    }
                }
            }
        };
        setMission.putValue(Action.SHORT_DESCRIPTION, I18n.text("Load a mission file to add relevant marks to log analysis") + ".");
        setMission.setEnabled(false);

        preferences = new AbstractAction(I18n.text("Preferences"), ImageUtils.getScaledIcon("images/settings.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {

                PropertiesEditor.editProperties(mra.getMraProperties(), mra, true);
                try {
                    PluginUtils.saveProperties("conf/mra.properties", mra.getMraProperties());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        preferences.putValue(Action.SHORT_DESCRIPTION, I18n.text("Configure MRA preferences") + ".");

        settingsMenu.add(setMission);
        settingsMenu.addSeparator();
        settingsMenu.add(preferences);
    }

    /**
     * Set up Tools Menu
     */
    private void setUpToolsMenu() {
        toolsMenu = new JMenu(I18n.text("Tools"));

        try {
            httpVehicleDownload = new AbstractAction(I18n.text("Choose an active vehicle to download logs (FTP)"),
                    ImageUtils.getScaledIcon("images/buttons/web.png", 16, 16)) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    final LinkedHashSet<ImcSystem> selectedVehicle = new LinkedHashSet<ImcSystem>();
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    JList<ImcSystem> logsList = new JList<ImcSystem>();
                    DefaultListModel<ImcSystem> listModel = new DefaultListModel<ImcSystem>();

                    LinkedHashMap<ImcId16, SystemImcMsgCommInfo> sysList = ImcMsgManager.getManager().getCommInfo();
                    for (ImcId16 system : sysList.keySet()) {
                        ImcSystem sys3 = ImcSystemsHolder.lookupSystem(system);
                        try {
                            if (sys3.getType() == SystemTypeEnum.VEHICLE) {
                                listModel.addElement(sys3);
                            }
                        }
                        catch (Exception e1) {
                            NeptusLog.pub().info("<###> "+system + " "+I18n.text("not selectable"));
                        }
                    }

                    logsList = new JList<ImcSystem>(listModel);
                    final JList<ImcSystem> lis = logsList;
                    lis.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    JScrollPane listScrollPane = new JScrollPane(logsList);
                    panel.add(listScrollPane, BorderLayout.CENTER);

                    if (listModel.size() == 1) {
                        selectedVehicle.add((ImcSystem) listModel.get(0));
                    }
                    else {
                        final JDialog dialog = new JDialog(mra);

                        JButton but = new JButton(new AbstractAction(I18n.text("Select Vehicle")) {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                List<ImcSystem> l = lis.getSelectedValuesList();
                                for (ImcSystem f : l) {
                                    selectedVehicle.add(f);
                                }
                                dialog.setVisible(false);
                                dialog.dispose();
                            }
                        });
                        panel.add(but, BorderLayout.SOUTH);

                        dialog.add(panel);
                        dialog.setSize(300, 300);

                        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                        dialog.setLocationRelativeTo(mra);
                        dialog.setVisible(true);
                    }

                    if (selectedVehicle.size() == 1) {
                        ImcSystem sys = selectedVehicle.iterator().next();
                        LogsDownloaderWorker logFetcher = new LogsDownloaderWorker();
                        logFetcher.setHost(sys.getHostAddress());
                        // logFetcher.setPort(sys.getRemoteUDPPort());
                        logFetcher.setLogLabel(sys.getName().toLowerCase());
                        //Vector<URI> sUri = sys.getServiceProvided("http", "dune");
                        Vector<URI> sUri = sys.getServiceProvided("ftp", "");
                        if (sUri.size() > 0) {
                            logFetcher.setHost(sUri.get(0).getHost());
                            logFetcher.setPort((sUri.get(0).getPort() <= 0) ? 21 : sUri.get(0).getPort());
                        }

                        logFetcher.setEnableHost(false);
                        logFetcher.setEnablePort(false);
                        logFetcher.setEnableLogLabel(false);
                        logFetcher.setVisible(true);
                    }
                }
            };
            httpVehicleDownload.setEnabled(true);
        }
        catch (Error e) {
            e.printStackTrace();
        }
        httpVehicleDownload.putValue(Action.SHORT_DESCRIPTION, I18n.text("Choose an active vehicle to download logs (FTP)") + ".");


        httpDuneDownload = new AbstractAction(I18n.text("Download logs from location (FTP)"), ImageUtils.getScaledIcon(
                "images/buttons/web.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                LogsDownloaderWorker logFetcher = new LogsDownloaderWorker();
                logFetcher.setConfigPanelVisible(true);
                logFetcher.setEnableLogLabel(true);
                logFetcher.setVisible(true);
            }
        };
        httpDuneDownload.setEnabled(true);
        httpDuneDownload.putValue(Action.SHORT_DESCRIPTION, I18n.text("Download logs from location") + ".");

        concatenateLSFLogs = new AbstractAction(I18n.text("Concatenate LSF logs"), ImageUtils.getIcon("images/menus/cat.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                File[] folders = ConcatenateLsfLog.chooseFolders(mra, new File(".").getAbsolutePath());

                if (folders != null) {
                    JFileChooser chooser = new JFileChooser(new File("."));
                    chooser.setDialogTitle(I18n.text("Select folder where to save concatenated log"));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int op = chooser.showOpenDialog(mra);
                    if (op == JFileChooser.APPROVE_OPTION) {
                        try {
                            ConcatenateLsfLog.concatenateFolders(folders, chooser.getSelectedFile(), null);
                            mra.openLog(new File(chooser.getSelectedFile(), "Data.lsf"));

                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(mra, ex);
                        }
                    }
                }
            }
        };
        concatenateLSFLogs.putValue(Action.SHORT_DESCRIPTION, I18n.text("Concatenate LSF logs") + ".");

        fuseLSFLogs = new AbstractAction(I18n.text("Fuse LSF logs"), ImageUtils.getIcon("images/menus/merge.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {

                File[] folders = ConcatenateLsfLog.chooseFolders(mra, new File(".").getAbsolutePath());

                if (folders != null) {
                    JFileChooser chooser = new JFileChooser(new File("."));
                    chooser.setDialogTitle(I18n.text("Select folder where to save concatenated log"));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int op = chooser.showOpenDialog(mra);
                    if (op == JFileChooser.APPROVE_OPTION) {
                        try {
                            ConcatenateLsfLog.concatenateFolders(folders, chooser.getSelectedFile(), null);
                            mra.openLog(new File(chooser.getSelectedFile(), "Data.lsf"));

                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(mra, ex);
                        }
                    }
                }
            }
        };
        fuseLSFLogs.putValue(Action.SHORT_DESCRIPTION, I18n.text("Fuse LSF logs") + ".");

        toolsMenu.add(httpVehicleDownload);
        toolsMenu.add(httpDuneDownload);
        toolsMenu.addSeparator();
        toolsMenu.add(concatenateLSFLogs);
        toolsMenu.add(fuseLSFLogs);
    }

    /**
     * Set up Help Menu
     */
    private void setUpHelpMenu() {
        helpMenu = new JMenu(I18n.text("Help"));
        JMenuItem aboutMenuItem = new JMenuItem();
        aboutMenuItem.setText(I18n.text("About"));
        aboutMenuItem.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/menus/info.png")));
        aboutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutPanel ap = new AboutPanel();
                ap.setVisible(true);
            }
        });
        helpMenu.add(aboutMenuItem);
    }

    /**
     * @return the menuBar
     */
    public JMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * @param menuBar the menuBar to set
     */
    private void setMenuBar(JMenuBar menuBar) {
        this.menuBar = menuBar;
    }

    public AbstractAction getSetMissionMenuItem() {
        return this.setMission;
    }

    public AbstractAction getGenReportMenuItem() {
        return this.genReport;
    }

    //    public JMenu getRecentlyOpenFilesMenu() {
    //        if (recentlyOpenFilesMenu == null) {
    //            recentlyOpenFilesMenu = new JMenu();
    //            recentlyOpenFilesMenu.setText(I18n.text("Recently opened"));
    //            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, mra.getMiscFilesOpened());
    //        }
    //        else {
    //            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, mra.getMiscFilesOpened());
    //        }
    //        return recentlyOpenFilesMenu;
    //    }
    //
    //    private void setRecentlyOpenFilesMenu(JMenu recentlyOpenFilesMenu) {
    //        this.recentlyOpenFilesMenu = recentlyOpenFilesMenu;
    //    }
}
