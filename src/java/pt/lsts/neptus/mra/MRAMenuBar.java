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
 * Author: hfq
 * Jan 20, 2014
 */
package pt.lsts.neptus.mra;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.text.Collator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
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
import javax.swing.ProgressMonitor;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.SystemImcMsgCommInfo;
import pt.lsts.neptus.gui.AboutPanel;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.gui.MissionFileChooser;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.WaitPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.lsf.ConcatenateLsfLog;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.RecentlyOpenedFilesUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.neptus.util.llf.LogUtils.LogValidity;
import pt.lsts.neptus.util.llf.LsfReport;
import pt.lsts.neptus.util.logdownload.LogsDownloaderWorker;

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
    private JMenu recentlyOpenFilesMenu = null;
    private JMenu exporters;
    private JMenu tideMenu;
    
    private boolean isExportersAdded = false;
    private boolean isTidesAdded = false;

    private AbstractAction openLsf, exit;
    protected AbstractAction genReport;
    protected AbstractAction genReportCustomOptions;
    protected AbstractAction reportOptions;
    private AbstractAction batchReport;
    private AbstractAction preferences;
    private AbstractAction httpDuneDownload, httpVehicleDownload, concatenateLSFLogs, fuseLSFLogs;
    protected AbstractAction setMission;

    private LinkedHashMap<JMenuItem, File> miscFilesOpened;
    private NeptusMRA mra;
    //private MRAPanel mraPanel; 

    
    /**
     * Constructor
     * 
     * @param miscFilesOpened Rencently opened log files
     */
    public MRAMenuBar(NeptusMRA mra) {
        this.mra = mra;
        //this.mraPanel = mra.getMraPanel();
    }

    /**
     * @return the MenuBar
     */
    public JMenuBar createMRAMenuBar() {
        this.miscFilesOpened = mra.getMraFilesHandler().getMiscFilesOpened();
        setMenuBar(new JMenuBar());

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

        getRecentlyOpenFilesMenu();
        mra.getMraFilesHandler().loadRecentlyOpenedFiles();

        // fileMenu.add(mra.getRecentlyOpenFilesMenu());
        fileMenu.add(getRecentlyOpenFilesMenu());

        openLsf = new AbstractAction(I18n.text("Open LSF log"), 
                ImageUtils.getIcon("images/menus/zipfolder.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                File lastFile = null;
                try {
                    lastFile = miscFilesOpened.size() == 0 ? null : miscFilesOpened.values().iterator().next();
                    if (lastFile != null && !lastFile.isDirectory())
                        lastFile = lastFile.getParentFile();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                File currentDirectory;
                if(lastFile != null && lastFile.isDirectory() && lastFile.canRead()) {
                    currentDirectory = lastFile;
                }
                else if (!new File(ConfigFetch.getLogsDownloadedFolder()).canRead())
                    currentDirectory = new File(ConfigFetch.getConfigFile());
                else
                    currentDirectory = new File(ConfigFetch.getLogsDownloadedFolder());

                JFileChooser fileChooser = GuiUtils.getFileChooser(currentDirectory, I18n.text("LSF log files"), 
                        FileUtil.FILE_TYPE_LSF, FileUtil.FILE_TYPE_LSF_COMPRESSED, FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2);

                if (fileChooser.showOpenDialog(mra) == JFileChooser.APPROVE_OPTION) {
                    final File log = fileChooser.getSelectedFile();
                    boolean proceed = true;
                    
                    LogValidity validity = LogUtils.isValidLSFSource(log.getParentFile());
                    if (validity != LogUtils.LogValidity.VALID) {
                        if(validity == LogValidity.NO_XML_DEFS) {
                            int op = GuiUtils.confirmDialog(mra, I18n.text("Open LSF log"),
                                    I18n.text("The log folder does not include IMC (xml) definitions. Use defaults?"));
                            if (op != JOptionPane.YES_OPTION) {
                                proceed = false;
                            }
                        }
                        else if(validity == LogValidity.NO_DIRECTORY) {
                            GuiUtils.errorMessage(mra, I18n.text("Open LSF log"),
                                    I18n.text("No such directory / No read permissions"));
                            proceed = false;
                        }
                        else if(validity == LogValidity.NO_VALID_LOG_FILE) {
                            GuiUtils.errorMessage(mra, I18n.text("Open LSF log"),
                                    I18n.text("No valid LSF log file present"));
                            proceed = false;
                        }
                        if (!proceed)
                            return;
                    }

                    new Thread("Open Log") {
                        @Override
                        public void run() {
                            mra.getMraFilesHandler().openLog(log);
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
                if (mra.getMraPanel() != null) {
                    mra.getMraPanel().cleanup();
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
     * This method initializes jMenu
     * 
     * @return javax.swing.JMenu
     */
    public JMenu getRecentlyOpenFilesMenu() {
        if (recentlyOpenFilesMenu == null) {
            recentlyOpenFilesMenu = new JMenu();
            recentlyOpenFilesMenu.setText(I18n.text("Recently opened"));
            recentlyOpenFilesMenu.setToolTipText("Most recently opened log files.");
            recentlyOpenFilesMenu.setIcon(ImageUtils.getIcon("images/menus/open.png"));
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, miscFilesOpened);
        }
        else {
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, miscFilesOpened);
        }
        return recentlyOpenFilesMenu;
    }

    /**
     * Set up Report Menu
     */
    private void setUpReportMenu() {
        reportMenu = new JMenu(I18n.text("Report"));
        reportOptions = new AbstractAction(I18n.text("PDF Report Options"), ImageUtils.getIcon("images/menus/settings.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {

                PropertiesEditor.editProperties(mra.getReportProperties(), mra, true);
                try {
                    PluginUtils.saveProperties("conf/report.properties", mra.getReportProperties());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        genReport = new AbstractAction(I18n.text("Save as PDF"), ImageUtils.getIcon("images/menus/document-pdf.png")) {
        
            @Override
            public void actionPerformed(ActionEvent e) {

                File f = new File(mra.getMraPanel().getSource().getDir().toString() + "/" + mra.getMraPanel().getSource().name() + " - " + System.currentTimeMillis() + ".pdf");
                if (f.exists()) {
                    int resp = JOptionPane.showConfirmDialog(mra,
                            I18n.text("Do you want to overwrite the existing file?"));
                    if (resp != JOptionPane.YES_OPTION)
                        return;
                }
                //mra.getBgp().block(true);
                //mra.getBgp().setText(I18n.text("Generating PDF Report"));
                mra.getMraFilesHandler().generatePDFReport(f);
                //mra.getBgp().setText(I18n.text("Done"));
            }
        };
        genReport.putValue(Action.SHORT_DESCRIPTION, I18n.text("Generate a pdf file report from Log") + ".");
        reportMenu.add(genReport);
        genReport.setEnabled(false);

        batchReport = new AbstractAction(I18n.text("Batch PDF report"),
                ImageUtils.getIcon("images/menus/document-pdf.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser chooser = GuiUtils.getFileChooser(ConfigFetch.getLogsDownloadedFolder());
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
                        LsfReport.generateLogs(f, mra.getMraPanel());
                        return null;
                    }
                };
                AsyncWorker.post(task);
            }
        };
        batchReport.putValue(Action.SHORT_DESCRIPTION, I18n.text("Generate report from selected log files") + ".");
        reportMenu.add(batchReport);

        reportMenu.addSeparator();
        reportMenu.add(reportOptions);

        genReportCustomOptions = new AbstractAction(I18n.text("Save as PDF with custom settings"), ImageUtils.getIcon("images/menus/document-pdf.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {

                PropertiesEditor.editProperties(mra.getReportProperties(), mra, true);
                try {
                    PluginUtils.saveProperties("conf/report.properties", mra.getReportProperties());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }

                File f = new File(mra.getMraPanel().getSource().getDir().toString() + "/" + mra.getMraPanel().getSource().name() + " - " + System.currentTimeMillis() + ".pdf");
                if (f.exists()) {
                    int resp = JOptionPane.showConfirmDialog(mra,
                            I18n.text("Do you want to overwrite the existing file?"));
                    if (resp != JOptionPane.YES_OPTION)
                        return;
                }
                //mra.getBgp().block(true);
                //mra.getBgp().setText(I18n.text("Generating PDF Report"));
                mra.getMraFilesHandler().generatePDFReport(f);
                //mra.getBgp().setText(I18n.text("Done"));
            }
        };
        genReportCustomOptions.putValue(Action.SHORT_DESCRIPTION, I18n.text("Generate a pdf file report from Log with custom options") + ".");
        reportMenu.add(genReportCustomOptions);
        genReportCustomOptions.setEnabled(false);

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
                    if (mra.getMraPanel() != null)
                        mra.getMraPanel().warnChangeListeners(mission);                    
                }
            }
        };
        setMission.putValue(Action.SHORT_DESCRIPTION, I18n.text("Load a mission file to add relevant marks to log analysis") + ".");
        setMission.setEnabled(false);

        preferences = new AbstractAction(I18n.text("Preferences"), ImageUtils.getScaledIcon("images/settings.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (!PropertiesEditor.editProperties(mra.getMraProperties(), mra, true)) {
                    try {
                        NeptusLog.pub().info("Saving MRA configuration to disk");
                        mra.getMraProperties().save();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
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
                    JFileChooser chooser = GuiUtils.getFileChooser(ConfigFetch.getConfigFile());
                    chooser.setDialogTitle(I18n.text("Select folder where to save concatenated log"));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int op = chooser.showOpenDialog(mra);
                    if (op == JFileChooser.APPROVE_OPTION) {
                        try {
                            ConcatenateLsfLog.concatenateFolders(folders, chooser.getSelectedFile(), null);
                            mra.getMraFilesHandler().openLog(new File(chooser.getSelectedFile(), "Data.lsf"));

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
                            mra.getMraFilesHandler().openLog(new File(chooser.getSelectedFile(), "Data.lsf"));

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
        aboutMenuItem.setIcon(ImageUtils.getIcon("images/menus/info.png"));
        aboutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutPanel ap = new AboutPanel(mra);
                ap.setVisible(true);
            }
        });
        helpMenu.add(aboutMenuItem);
    }

    /**
     * This Menu is only added to tools menu after a Log file is added
     * @param source
     */
    public void setUpExportersMenu(final IMraLogGroup source) {
        LinkedHashMap<String, Class<? extends MRAExporter>> exporterMap = PluginsRepository
                .listExtensions(MRAExporter.class);

        LinkedHashMap<String, MRAExporter> exporters = new LinkedHashMap<>();
        
        //Vector<MRAExporter> exporterList = new Vector<>();

        for (Class<? extends MRAExporter> clazz : exporterMap.values()) {
            Constructor<?>[] constructors = clazz.getConstructors();
            
            boolean added = false;
            
            for (Constructor<?> c : constructors) {
                if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0].equals(IMraLogGroup.class)) {
                    try {
                        exporters.put(PluginUtils.getPluginI18nName(clazz), clazz.getConstructor(IMraLogGroup.class).newInstance(new Object[] { source }));
                    }
                    catch (Exception e) {                        
                        e.printStackTrace();
                    }
                    added = true;
                }
            }
            
            if (!added) {
                try {
                    exporters.put(PluginUtils.getPluginI18nName(clazz), clazz.getDeclaredConstructor().newInstance());
                    added = true;
                }
                catch (Exception e) { }                
            }
            
            if (!added) {
                NeptusLog.pub().error("Error Exporter of type "+clazz.getName()+": No valid constructor.");
            }
        }

        // Check for existence of Exporters menu and remove on existence (in case of opening a new log)
        if(getExportersMenu() != null)
            toolsMenu.remove(getExportersMenu());

        setExportersMenu(new JMenu(I18n.text("Exporters")));
        JMenu experimental = new JMenu(I18n.text("Experimental"));
        getExportersMenu().setIcon(ImageUtils.getIcon("images/menus/export.png"));
        getExportersMenu().setToolTipText(I18n.text("Export data to") + "...");
        
        
        Vector<String> names = new Vector<>();
        names.addAll(exporters.keySet());
        Collections.sort(names, Collator.getInstance());
        
        for (String name : names) {
            final MRAExporter exp = exporters.get(name);
            try {
                if (exp.canBeApplied(source)) {
                    JMenuItem item = new JMenuItem(new AbstractAction(name) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Thread t = new Thread(name + " processing") {
                                @Override
                                public void run() {
                                    ProgressMonitor monitor = new ProgressMonitor(mra.getMraPanel(), name, "", 0, 100);
                                    monitor.setProgress(0);
                                    String res = exp.process(source, monitor);
                                    if (res != null)
                                        GuiUtils.infoMessage(mra.getMraPanel(), name, res);
                                    monitor.close();
                                };
                            };
                            t.setDaemon(true);
                            t.start();
                        }
                    });
                    item.setIcon(ImageUtils.getIcon("images/menus/export.png"));
                    
                    if (PluginUtils.isPluginExperimental(exp.getClass()))
                        experimental.add(item);
                    else
                        getExportersMenu().add(item);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (getExportersMenu().getItemCount() > 0) {
            if(!isExportersAdded) {
                toolsMenu.addSeparator();
                isExportersAdded = true;
            }
            if (experimental.getItemCount() > 0)
                getExportersMenu().add(experimental);
            toolsMenu.add(getExportersMenu());
        }
    }
    
    public void setUpTidesMenu(final IMraLogGroup source) {
        if(getTidesMenu() != null)
            settingsMenu.remove(getTidesMenu());

        String strTitle = I18n.text("Change tides source");
        JMenuItem changeTideMenu = new JMenuItem(strTitle);
        changeTideMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int res = GuiUtils.confirmDialog(mra, strTitle,
                        I18n.text("This will require a log reopening. Want to proceed?"));
                switch (res) {
                    case JOptionPane.YES_OPTION:
                        InfiniteProgressPanel busyPanel = InfiniteProgressPanel.createInfinitePanelBeans(I18n.text("Processing"), 100);
                        JDialog dialog = new JDialog(mra, I18n.text("Tides"), ModalityType.DOCUMENT_MODAL);
                        dialog.add(busyPanel);
                        dialog.pack();
                        dialog.setSize(300, 200);
                        dialog.setLocationRelativeTo(mra);
                        Thread wt = new Thread("Open Log") {
                            @Override
                            public void run() {
                                TidesMraLoader.chooseTideSource(source, mra);
                                dialog.setVisible(false);
                                busyPanel.setBusy(false);
                                dialog.dispose();
                                mra.getMraFilesHandler().openLog(source.getFile("Data.lsf"));
                            };
                        };
                        busyPanel.setBusy(true);
                        wt.setDaemon(true);
                        wt.start();
                        dialog.setVisible(true);
                        break;
                    default:
                        break;
                }
            }
        });

        JMenu tMenu = new JMenu(I18n.text("Tides"));
        tMenu.add(changeTideMenu);

        setTideMenu(tMenu);

        if (getTidesMenu()!= null) {
            if(!isTidesAdded) {
                settingsMenu.addSeparator();
                isTidesAdded = true;
            }
            settingsMenu.add(getTidesMenu());
        }
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

    /**
     * Gets setMission MenuItem
     * @return setMission
     */
    public AbstractAction getSetMissionMenuItem() {
        return this.setMission;
    }

    /**
     * Gets genReport MenuItem
     * @return genReport
     */
    public AbstractAction getGenReportMenuItem() {
        return this.genReport;
    }

    /**
     * Gets report MenuItem
     * @return genReport
     */
    public JMenu getReportMenuItem() {
        return this.reportMenu;
    }

    /**
     * Gets genReportCustomOptions MenuItem
     * @return genReport
     */
    public AbstractAction getGenReportCustomOptionsMenuItem() {
        return this.genReportCustomOptions;
    }

    /**
     * Gets genReportCustomOptions MenuItem
     * @return genReport
     */
    public AbstractAction getReportOptionsMenuItem() {
        return this.reportOptions;
    }

    /**
     * @return the exporters
     */
    private JMenu getExportersMenu() {
        return exporters;
    }

    /**
     * @param exporters the exporters to set
     */
    private void setExportersMenu(JMenu exportersMenu) {
        this.exporters = exportersMenu;
    }
    
    /**
     * @return the tideMenu
     */
    private JMenu getTidesMenu() {
        return tideMenu;
    }

    /**
     * @param tideMenu the tideMenu to set
     */
    private void setTideMenu(JMenu tideMenu) {
        this.tideMenu = tideMenu;
    }
}
