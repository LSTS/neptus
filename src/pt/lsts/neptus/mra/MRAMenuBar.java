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
import javax.swing.SwingWorker;

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
public class MRAMenuBar extends JMenuBar {

    private static final long serialVersionUID = 1362828137699399670L;

    private JMenuBar menuBar;
    private JMenu fileMenu, reportMenu, settingsMenu, toolsMenu, helpMenu;

    // private JMenu recentlyOpenFilesMenu = null;

    private AbstractAction openLsf, exit;
    private AbstractAction preferences, httpDuneDownload, httpVehicleDownload;
    protected AbstractAction setMission, genReport;

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
    @SuppressWarnings("serial")
    private JMenuBar createMRAMenuBar() {
        mra.loadRecentlyOpenedFiles();

        menuBar = new JMenuBar();

        fileMenu = new JMenu(I18n.text("File"));

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
                            // mra.bgp.block(true);
                            mra.openLog(log);
                            // mra.bgp.block(false);
                        };
                    }.start();
                }
                return;
            }
        };

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

        fileMenu.add(openLsf);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        reportMenu = new JMenu(I18n.text("Report"));

        genReport = new AbstractAction(I18n.text("Save as PDF"), ImageUtils.getIcon("images/menus/changelog.png")) {

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

                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return LsfReport.generateReport(mraPanel.getSource(), f, mraPanel);
                    }

                    @Override
                    protected void done() {
                        super.done();
                        try {
                            get();
                        } catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        try {
                            if (get()) {
                                GuiUtils.infoMessage(mra,  I18n.text("Generate PDF Report"),
                                        I18n.text("File saved to") +" "+ f.getAbsolutePath());
                                final String pdfPath = f.getAbsolutePath();
                                new Thread() {
                                    @Override
                                    public void run() {
                                        mra.openPDFInExternalViewer(pdfPath);
                                    }                              
                                }.start();
                            }
                        } catch (Exception e) {
                            GuiUtils.errorMessage(mra, "<html>"+I18n.text("PDF <b>was not</b> saved to file.")
                                    + "<br>"+I18n.text("Error")+": " + e.getMessage() + "</html>", I18n.text("PDF Creation Process"));
                            e.printStackTrace();
                        } finally {
                            mra.getBgp().block(false);
                        }
                    }
                };
                worker.execute();

                /*                if (created) {
                    GuiUtils.infoMessage(NeptusMRA.this, "Generate PDF Report", "File saved to "+f.getAbsolutePath());
                    try {
                        if (ConfigFetch.getOS() == ConfigFetch.OS_WINDOWS)
                            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + f.getCanonicalPath());
                        else {
                            String[] readers = { "acroread", "xpdf"};
                            String reader = null;

                            for (int count = 0; count < readers.length && reader == null; count++)
                                if (Runtime.getRuntime().exec( new String[] {"which", readers[count]}).waitFor() == 0)
                                    reader = readers[count];
                            if (reader == null)
                                System.err.println("No pdf reader was found");
                            else Runtime.getRuntime().exec(new String[] {
                                    reader, f.getAbsolutePath()
                            }
                                    );
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    //JOptionPane pane = new JOptionPane("File saved to "+f.getAbsolutePath(),
                    JOptionPane.DEFAULT_OPTION);
                    //pane.setOptions(new String[] {"Open", "OK"});
                    //pane.setVisible(true);

                    //JOptionPane.showMessageDialog(NeptusMRA.this, "File saved to "+f.getAbsolutePath(),
                    "Generate PDF Report", JOptionPane.
                }*/
            }
        };

        // FIXME - hfq revision stoped

        reportMenu.add(genReport);
        genReport.setEnabled(false);

        AbstractAction batchReport = new AbstractAction(I18n.text("Batch PDF report"),
                ImageUtils.getIcon("images/menus/changelog.png")) {

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
        reportMenu.add(batchReport);

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
        settingsMenu.add(setMission);
        setMission.setEnabled(false);

        preferences = new AbstractAction(I18n.text("Preferences")) {

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
        settingsMenu.add(preferences);

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

            toolsMenu.add(httpVehicleDownload);
        }
        catch (Error e) {
            e.printStackTrace();
        }

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
        toolsMenu.add(httpDuneDownload);

        toolsMenu.addSeparator();

        toolsMenu.add(I18n.text("Concatenate LSF logs")).addActionListener(new ActionListener() {

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
        });

        toolsMenu.add(I18n.text("Fuse LSF logs")).addActionListener(new ActionListener() {

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
        });

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

        menuBar.add(fileMenu);
        menuBar.add(reportMenu);
        menuBar.add(settingsMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        return menuBar;
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
}
