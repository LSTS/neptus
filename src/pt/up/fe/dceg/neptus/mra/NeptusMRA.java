/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2007/09/25
 */
package pt.up.fe.dceg.neptus.mra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.BlockingGlassPane;
import pt.up.fe.dceg.neptus.gui.MissionFileChooser;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.gui.WaitPanel;
import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndexListener;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.importers.lsf.ConcatenateLsfLog;
import pt.up.fe.dceg.neptus.mra.replay.LogReplay;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginClassLoader;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;
import pt.up.fe.dceg.neptus.util.RecentlyOpenedFilesUtil;
import pt.up.fe.dceg.neptus.util.StreamUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.SystemImcMsgCommInfo;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;
import pt.up.fe.dceg.neptus.util.llf.LogUtils.LogValidity;
import pt.up.fe.dceg.neptus.util.llf.LsfLogSource;
import pt.up.fe.dceg.neptus.util.llf.LsfReport;
import pt.up.fe.dceg.neptus.util.logdownload.LogsDownloaderWorker;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;

/**
 * @author ZP
 * @author pdias (LSF)
 */
@SuppressWarnings("serial")
public class NeptusMRA extends JFrame implements PropertiesProvider {
    public final static String RECENTLY_OPENED_LOGS = "conf/mra_recent.xml";

    @NeptusProperty(name = "Show 3D replay")
    public static boolean show3D = true;

    @NeptusProperty(name = "Default time step (seconds)")
    public static double defaultTimestep = 1.0;
    
    @NeptusProperty(name = "Minimum depth for bathymetry", description="Filter all bathymetry data if vehicle's depth is less than this value (meters).")
    public static double minDepthForBathymetry = 1.0;
    
    
    private AbstractAction genReport, setMission, preferences, openLsf, httpDuneDownload, httpVehicleDownload;
    
    private File tmpFile = null;
    private InputStream activeInputStream = null;
    
    private LinkedHashMap<JMenuItem, File> miscFilesOpened = new LinkedHashMap<JMenuItem, File>();
    private JMenu recentlyOpenFilesMenu = null;
    private MRAPanel mraPanel = null;

    protected static final String MRA_TITLE = I18n.text("Neptus Mission Review And Analysis");
    protected BlockingGlassPane bgp = new BlockingGlassPane(400);

    protected JMenuBar menuBar;
    
    public NeptusMRA() {
        super(MRA_TITLE);
        try {
            PluginUtils.loadProperties("conf/mra.properties", this);
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Not possible to open")
                    + " \"conf/mra.properties\"");
        }
        setSize(1200, 700);

        setIconImage(Toolkit.getDefaultToolkit().getImage(ConfigFetch.class.getResource("/images/neptus-icon.png")));

        GuiUtils.centerOnScreen(this);
        setJMenuBar(createMenuBar());
        setVisible(true);

        JLabel lbl = new JLabel(MRA_TITLE, JLabel.CENTER);

        lbl.setBackground(Color.WHITE);
        lbl.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        getContentPane().add(lbl);
        lbl.setFont(new Font("Helvetica", Font.ITALIC, 32));
        lbl.setVerticalTextPosition(JLabel.BOTTOM);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        lbl.setForeground(new Color(80, 120, 175));
        lbl.revalidate();
       
        addWindowListener(new WindowAdapter() {
            boolean closed = false;

            @Override
            public void windowActivated(WindowEvent e) {
                super.windowActivated(e);
                ConfigFetch.setSuperParentFrameForced(NeptusMRA.this);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (closed)
                    return;
                closed = true;
                NeptusMRA.this.setVisible(false);
                
                if (mraPanel != null)
                    mraPanel.cleanup();
                mraPanel = null;

                abortPendingOpenLogActions();
                NeptusMRA.this.getContentPane().removeAll();
                NeptusMRA.this.dispose();

                ConfigFetch.setSuperParentFrameForced(null);                
            }
        });
        
        setGlassPane(bgp);
        repaint();
    }

    public static NeptusMRA showApplication() {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();

        return new NeptusMRA();
    }

    private void abortPendingOpenLogActions() {
        if (activeInputStream != null) {
            try {
                activeInputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            activeInputStream = null;
        }
        if (tmpFile != null) {
            if (tmpFile.exists()) {
                try {
                    tmpFile.delete();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void closeLogSource() {
        if (mraPanel != null) {
            mraPanel.cleanup();
            mraPanel = null;
            getContentPane().removeAll();
            NeptusLog.pub().info("<###>Log source was closed.");
        }
    }
    
    public void openLogSource(IMraLogGroup source) {
        abortPendingOpenLogActions();
        closeLogSource();
        getContentPane().removeAll();
        mraPanel = new MRAPanel(source,this);
        getContentPane().add(mraPanel);
        invalidate();
        validate();
        setMission.setEnabled(true);
        genReport.setEnabled(true);
    }

    // --- Extractors ---
    public File extractGzip(File f) {
        try {
            File res;
            bgp.setText(I18n.text("Decompressing LSF Data..."));
            GZIPInputStream ginstream = new GZIPInputStream(new FileInputStream(f));
            activeInputStream = ginstream;
            File outputFile = new File(f.getParent(), "Data.lsf");
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            FileOutputStream outstream = new FileOutputStream(outputFile, false);
            byte[] buf = new byte[2048];
            int len;
            while ((len = ginstream.read(buf)) > 0) {
                outstream.write(buf, 0, len);
            }
            ginstream.close();
            outstream.close();
            res = new File(f.getParent(), "Data.lsf");
            
            return res;
        }
        catch (IOException ioe) {
            System.err.println("Exception has been thrown" + ioe);
            bgp.setText(I18n.text("Decompressing LSF Data...") + "   "
                    + ioe.getMessage());
            return null;
        }
    }
    
    public File extractBzip2(File f) {
        bgp.setText(I18n.text("Decompressing BZip2 LSF Data..."));
        try {
            final FileInputStream fxInStream = new FileInputStream(f);
            activeInputStream = fxInStream;
            BZip2CompressorInputStream gzDataLog = new BZip2CompressorInputStream(fxInStream);
            File outFile = new File(f.getParent(), "Data.lsf");
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            FilterCopyDataMonitor fis = new FilterCopyDataMonitor(gzDataLog) {
                long target = 1 * 1024 * 1024;
                protected String decompressed = I18n.text("Decompressed");

                @Override
                public void updateValueInMessagePanel() {
                    if (downloadedSize > target) {
                        bgp.setText(decompressed + " "
                                + MathMiscUtils.parseToEngineeringRadix2Notation(downloadedSize, 2) + "B");
                        target += 1 * 1024 * 1024;
                    }
                }
            };
            
            StreamUtil.copyStreamToFile(fis, outFile);
            fxInStream.close();
            return outFile;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Does the necessary pre-processing of a log file based on it's extension
     * Currently supports gzip, bzip2 and no-compression formats
     * @param fx
     * @return True on success, False on failure
     */
    public boolean openLog(File fx) {
        bgp.block(true);
        File fileToOpen = null;
        
        if (fx.getName().toLowerCase().endsWith(FileUtil.FILE_TYPE_LSF_COMPRESSED)) {
            fileToOpen = extractGzip(fx);
        }
        else if (fx.getName().toLowerCase().endsWith(FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2)) {
            fileToOpen = extractBzip2(fx);
        }        
        else if (fx.getName().toLowerCase().endsWith(FileUtil.FILE_TYPE_LSF)) {
            fileToOpen = fx;
        }
              
        bgp.block(false);
        return openLSF(fileToOpen);
    }

    
    public boolean openLSF(File f) {
        bgp.block(true);
        bgp.setText(I18n.text("Loading LSF Data"));
        final File lsfDir = f.getParentFile();

        IMCDefinition.pathToDefaults = ConfigFetch.getDefaultIMCDefinitionsLocation();

        boolean alreadyConverted = false;
        if (lsfDir.isDirectory()) {
            if (new File(lsfDir, "mra/lsf.index").canRead())
                alreadyConverted = true;

        }
        else if (new File(lsfDir, "mra/lsf.index").canRead())
            alreadyConverted = true;

        if (alreadyConverted) {
            int option = JOptionPane.showConfirmDialog(NeptusMRA.this,
                    I18n.text("This log seems to have already been indexed. Index again?"));
        
            if (option == JOptionPane.YES_OPTION) {
                new File(lsfDir, "mra/lsf.index").delete(); 
            }
            
            if (option == JOptionPane.CANCEL_OPTION) {
                bgp.block(false);
                return false;
            }
        }
        
        bgp.setText(I18n.text("Loading LSF Data"));
        
        try {
            LsfLogSource source = new LsfLogSource(f, new LsfIndexListener() {
    
                @Override
                public void updateStatus(String messageToDisplay) {
                    bgp.setText(messageToDisplay);
                }
            });
            updateMissionFilesOpened(f);
            
            bgp.setText(I18n.text("Starting interface"));
            openLogSource(source);            
            bgp.setText(I18n.text("Done"));
            
            bgp.block(false);
            return true;
        }
        catch (Exception e) {
            bgp.block(false);
            e.printStackTrace();
            GuiUtils.errorMessage(NeptusMRA.this, I18n.text("Invalid LSF index"), I18n.text(e.getMessage()));
            return false;    
        }
    }
    
    public JMenuBar createMenuBar() {
        loadRecentlyOpenedFiles();

        menuBar = new JMenuBar();
        JMenu file = new JMenu(I18n.text("File"));

        file.add(getRecentlyOpenFilesMenu());      

        openLsf = new AbstractAction(I18n.text("Open LSF log"),
                ImageUtils.getIcon("images/menus/zipfolder.png")) {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser;

                File lastFile = null;
                try {
                    lastFile = miscFilesOpened.size() == 0 ? null : miscFilesOpened.values().iterator().next();
                    if (lastFile != null && !lastFile.isDirectory())
                        lastFile = lastFile.getParentFile();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                // String path = "./log/dune";
                if (lastFile != null && lastFile.isDirectory() && lastFile.canRead()) {
                    chooser = new JFileChooser(lastFile);
                }
                else if (!new File("./log/downloaded/").canRead())
                    chooser = new JFileChooser(ConfigFetch.getConfigFile());
                else
                    chooser = new JFileChooser(new File("./log/downloaded/"));

                chooser.setFileView(new NeptusFileView());
                chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("LSF log files"),
                        new String[] { "lsf", FileUtil.FILE_TYPE_LSF_COMPRESSED,
                    FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2 }));

                int res = chooser.showOpenDialog(NeptusMRA.this);

                if (res == JFileChooser.APPROVE_OPTION) {
                    final File f = chooser.getSelectedFile();
                    LogValidity validity = LogUtils.isValidLSFSource(f.getParentFile());
                    if (validity != LogUtils.LogValidity.VALID) {
                        String message = null;
                        if(validity == LogValidity.NO_DIRECTORY)
                            message = "No such directory / No read permissions";
                        if(validity == LogValidity.NO_VALID_LOG_FILE)
                            message = "No valid LSF log file present";
                        if(validity == LogValidity.NO_XML_DEFS)
                            message = "No valid XML definition present";
                        
                        GuiUtils.errorMessage(NeptusMRA.this, I18n.text("Open LSF log"),
                                I18n.text(message));
                        return;
                    }
                    
                    Thread t = new Thread("Open Log") {
                        public void run() {
//                            Component oldGlassPane = getGlassPane();
//                            setGlassPane(bgp);
                            bgp.block(true);
                            openLog(f);
                            bgp.block(false);
//                            setGlassPane(oldGlassPane);
                        };
                    };
                    t.start();
                }
                return;
            }
        };

        file.add(openLsf);
        
        AbstractAction exit = new AbstractAction(I18n.text("Exit"), ImageUtils.getIcon("images/menus/exit.png")) {
            public void actionPerformed(ActionEvent e) {
                NeptusMRA.this.setVisible(false);

                NeptusMRA.this.dispose();
                Container c = getContentPane();
                if (c instanceof MRAPanel) {
                    ((MRAPanel) c).cleanup();
                }
            }
        };
        file.addSeparator();
        file.add(exit);

        JMenu report = new JMenu(I18n.text("Report"));
        genReport = new AbstractAction(I18n.text("Save as PDF"), ImageUtils.getIcon("images/menus/changelog.png")) {
            public void actionPerformed(ActionEvent e) {
                final File f = new File(System.currentTimeMillis() + ".pdf");
                if (f.exists()) {
                    int resp = JOptionPane.showConfirmDialog(NeptusMRA.this,
                            I18n.text("Do you want to overwrite the existing file?"));
                    if (resp != JOptionPane.YES_OPTION)
                        return;
                }
                bgp.block(true);
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
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        try {
                            if (get()) {
                                GuiUtils.infoMessage(NeptusMRA.this, I18n.text("Generate PDF Report"),
                                        I18n.text("File saved to") +" "+ f.getAbsolutePath());
                                final String pdfF = f.getAbsolutePath();
                                new Thread() {
                                    public void run() {
                                        openPDFInExternalViewer(pdfF);
                                    };
                                }.start();
                            }
                        }
                        catch (Exception e) {
                            GuiUtils.errorMessage(NeptusMRA.this, "<html>"+I18n.text("PDF <b>was not</b> saved to file.")
                                    + "<br>"+I18n.text("Error")+": " + e.getMessage() + "</html>", I18n.text("PDF Creation Process"));
                            e.printStackTrace();
                        }
                        finally {
                            bgp.block(false);
                        }
                    }
                };
                worker.execute();

                // if (created) {
                // GuiUtils.infoMessage(NeptusMRA.this, "Generate PDF Report", "File saved to "+f.getAbsolutePath());
                // try {
                // if (ConfigFetch.getOS() == ConfigFetch.OS_WINDOWS)
                // Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + f.getCanonicalPath());
                // else {
                // String[] readers = { "acroread", "xpdf"};
                // String reader = null;
                //
                // for (int count = 0; count < readers.length && reader == null; count++)
                // if (Runtime.getRuntime().exec( new String[] {"which", readers[count]}).waitFor() == 0)
                // reader = readers[count];
                // if (reader == null)
                // System.err.println("No pdf reader was found");
                // else Runtime.getRuntime().exec(new String[] {
                // reader, f.getAbsolutePath()
                // }
                // );
                // }
                // }
                // catch (Exception ex) {
                // ex.printStackTrace();
                // }
                // //JOptionPane pane = new JOptionPane("File saved to "+f.getAbsolutePath(),
                // JOptionPane.DEFAULT_OPTION);
                // //pane.setOptions(new String[] {"Open", "OK"});
                // //pane.setVisible(true);
                //
                // //JOptionPane.showMessageDialog(NeptusMRA.this, "File saved to "+f.getAbsolutePath(),
                // "Generate PDF Report", JOptionPane.
                // }
            }
        };
        report.add(genReport);
        genReport.setEnabled(false);

        AbstractAction batchReport = new AbstractAction(I18n.text("Batch PDF report"),
                ImageUtils.getIcon("images/menus/changelog.png")) {
            public void actionPerformed(ActionEvent e) {

                JFileChooser chooser = new JFileChooser(new File("."));
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int res = chooser.showOpenDialog(NeptusMRA.this);

                if (res != JFileChooser.APPROVE_OPTION)
                    return;

                final File f = chooser.getSelectedFile();

                final WaitPanel panel = new WaitPanel();
                panel.start(NeptusMRA.this, ModalityType.DOCUMENT_MODAL);

                AsyncTask task = new AsyncTask() {
                    public void finish() {
                        panel.stop();
                        GuiUtils.infoMessage(NeptusMRA.this, I18n.text("Batch report ended successfully"), I18n.text("Files saved to")+" "
                                + (new File(".").getAbsolutePath()));
                    }

                    public Object run() throws Exception {
                        LsfReport.generateLogs(f, mraPanel);
                        return null;
                    }
                };

                AsyncWorker.post(task);

            }
        };
        report.add(batchReport);

        JMenu settings = new JMenu(I18n.text("Settings"));
        setMission = new AbstractAction(I18n.text("Set mission"), ImageUtils.getIcon("images/menus/mapeditor.png")) {
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
        settings.add(setMission);
        setMission.setEnabled(false);

        preferences = new AbstractAction(I18n.text("Preferences")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(NeptusMRA.this, true);
                try {
                    PluginUtils.saveProperties("conf/mra.properties", NeptusMRA.this);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        settings.add(preferences);

        JMenu tools = new JMenu(I18n.text("Tools"));

        try {

            httpVehicleDownload = new AbstractAction(I18n.text("Choose an active vehicle to download logs (HTTP)"),
                    ImageUtils.getScaledIcon("images/buttons/web.png", 16, 16)) {
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
                        final JDialog dialog = new JDialog(NeptusMRA.this);

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
                        dialog.setLocationRelativeTo(NeptusMRA.this);
                        dialog.setVisible(true);
                    }

                    if (selectedVehicle.size() == 1) {
                        ImcSystem sys = selectedVehicle.iterator().next();
                        LogsDownloaderWorker logFetcher = new LogsDownloaderWorker();
                        logFetcher.setHost(sys.getHostAddress());
                        // logFetcher.setPort(sys.getRemoteUDPPort());
                        logFetcher.setLogLabel(sys.getName().toLowerCase());
                        // new code 2010-10-27 pdias
                        Vector<URI> sUri = sys.getServiceProvided("http", "dune");
                        if (sUri.size() > 0) {
                            logFetcher.setHost(sUri.get(0).getHost());
                            logFetcher.setPort((sUri.get(0).getPort() <= 0) ? 80 : sUri.get(0).getPort());
                        }

                        logFetcher.setEnableHost(false);
                        logFetcher.setEnablePort(false);
                        logFetcher.setEnableLogLabel(false);
                        logFetcher.setVisible(true);
                    }
                }
            };
            httpVehicleDownload.setEnabled(true);

            tools.add(httpVehicleDownload);
        }
        catch (Error e) {
            e.printStackTrace();
        }

        httpDuneDownload = new AbstractAction(I18n.text("Download logs from location (HTTP)"), ImageUtils.getScaledIcon(
                "images/buttons/web.png", 16, 16)) {
            public void actionPerformed(ActionEvent e) {
                LogsDownloaderWorker logFetcher = new LogsDownloaderWorker();
                logFetcher.setConfigPanelVisible(true);
                logFetcher.setEnableLogLabel(true);
                logFetcher.setVisible(true);
            }
        };
        httpDuneDownload.setEnabled(true);
        tools.add(httpDuneDownload);

        tools.addSeparator();

        tools.add(I18n.text("Concatenate LSF logs")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                File[] folders = ConcatenateLsfLog.chooseFolders(NeptusMRA.this, new File(".").getAbsolutePath());

                if (folders != null) {
                    JFileChooser chooser = new JFileChooser(new File("."));
                    chooser.setDialogTitle(I18n.text("Select folder where to save concatenated log"));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int op = chooser.showOpenDialog(NeptusMRA.this);
                    if (op == JFileChooser.APPROVE_OPTION) {
                        try {
                            ConcatenateLsfLog.concatenateFolders(folders, chooser.getSelectedFile(), null);
                            openLog(new File(chooser.getSelectedFile(), "Data.lsf"));

                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(NeptusMRA.this, ex);
                        }
                    }
                }
            }
        });
        
        tools.add(I18n.text("Fuse LSF logs")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                
                File[] folders = ConcatenateLsfLog.chooseFolders(NeptusMRA.this, new File(".").getAbsolutePath());

                if (folders != null) {
                    JFileChooser chooser = new JFileChooser(new File("."));
                    chooser.setDialogTitle(I18n.text("Select folder where to save concatenated log"));
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int op = chooser.showOpenDialog(NeptusMRA.this);
                    if (op == JFileChooser.APPROVE_OPTION) {
                        try {
                            ConcatenateLsfLog.concatenateFolders(folders, chooser.getSelectedFile(), null);
                            openLog(new File(chooser.getSelectedFile(), "Data.lsf"));

                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(NeptusMRA.this, ex);
                        }
                    }
                }
            }
        });

        menuBar.add(file);
        menuBar.add(report);
        menuBar.add(settings);
        menuBar.add(tools);

        return menuBar;
    }
    
    public JMenuBar getMRAMenuBar() {
        return menuBar;
    }
    
    /**
     * @param pdf
     */
    protected void openPDFInExternalViewer(String pdf) {
        try {
            if (ConfigFetch.getOS() == ConfigFetch.OS_WINDOWS) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + pdf);
            }
            else {
                String[] readers = { "xpdf", "kpdf", "FoxitReader", "evince", "acroread" };
                String reader = null;

                for (int count = 0; count < readers.length && reader == null; count++) {
                    if (Runtime.getRuntime().exec(new String[] { "which", readers[count] }).waitFor() == 0)
                        reader = readers[count];
                }
                if (reader == null)
                    throw new Exception(I18n.text("Could not find PDF reader"));
                else
                    Runtime.getRuntime().exec(new String[] { reader, pdf });
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* RECENTLY OPENED LOG FILES */

    /**
     * This method initializes jMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getRecentlyOpenFilesMenu() {
        if (recentlyOpenFilesMenu == null) {
            recentlyOpenFilesMenu = new JMenu();
            recentlyOpenFilesMenu.setText(I18n.text("Recently opened"));
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, miscFilesOpened);
        }
        else {
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, miscFilesOpened);
        }
        return recentlyOpenFilesMenu;
    }

    private void loadRecentlyOpenedFiles() {
        String recentlyOpenedFiles = ConfigFetch.resolvePath(RECENTLY_OPENED_LOGS);
        Method methodUpdate = null;

        try {
            Class<?>[] params = { File.class };
            methodUpdate = this.getClass().getMethod("updateMissionFilesOpened", params);
        }
        catch (Exception e) {
            NeptusLog.pub().error(this + "loadRecentlyOpenedFiles", e);
            return;
        }

        if (recentlyOpenedFiles == null) {
            // JOptionPane.showInternalMessageDialog(this, "Cannot Load");
            return;
        }

        if (!new File(recentlyOpenedFiles).exists())
            return;

        RecentlyOpenedFilesUtil.loadRecentlyOpenedFiles(recentlyOpenedFiles, methodUpdate, this);
    }

    public boolean updateMissionFilesOpened(File fx) {
        RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, miscFilesOpened, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final File fx;
                Object key = e.getSource();
                File value = miscFilesOpened.get(key);
                if (value instanceof File) {
                    fx = (File) value;
//                    openLog(fx);
                    // if (fx.isDirectory())
                    // openDir(fx);
                    // else if ("zip".equalsIgnoreCase(FileUtil.getFileExtension(fx)))
                    // openZip(fx);
                    // else if (FileUtil.FILE_TYPE_LSF.equalsIgnoreCase(FileUtil.getFileExtension(fx)))
                    // openLSF(fx);
                    // else if (fx.getName().toLowerCase().endsWith(FileUtil.FILE_TYPE_LSF_COMPRESSED))
                    // openLSF(fx);
                    
                    Thread t = new Thread("Open Log") {
                        public void run() {
                            openLog(fx);
                        };
                    };
                    t.start();
                }
                else
                    return;
            }
        });
        getRecentlyOpenFilesMenu();
        storeRecentlyOpenedFiles();
        return true;
    }

    private void storeRecentlyOpenedFiles() {
        String recentlyOpenedFiles;
        LinkedHashMap<JMenuItem, File> hMap;
        String header;

        recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(RECENTLY_OPENED_LOGS);
        hMap = miscFilesOpened;
        header = I18n.text("Recently opened mission files")+".";

        RecentlyOpenedFilesUtil.storeRecentlyOpenedFiles(recentlyOpenedFiles, hMap, header);
    }

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return I18n.text("MRA Preferences");
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    /**
     * @author pdias
     * 
     */
    public abstract class FilterCopyDataMonitor extends FilterInputStream {

        public long downloadedSize = 0;

        public FilterCopyDataMonitor(InputStream in) {
            super(in);
            downloadedSize = 0;
        }

        @Override
        public int read() throws IOException {
            int tmp = super.read();
            downloadedSize += (tmp == -1) ? 0 : 1;
            if (tmp != -1)
                updateValueInMessagePanel();
            return tmp;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int tmp = super.read(b, off, len);
            downloadedSize += (tmp == -1) ? 0 : tmp;
            if (tmp != -1)
                updateValueInMessagePanel();
            return tmp;
        }

        public abstract void updateValueInMessagePanel();
    }

    public static void main(String[] args) {
        
        GuiUtils.setLookAndFeel();
        NeptusMRA mra = new NeptusMRA();
        mra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Thread t = new Thread("startup") {
            public void run() {
                PluginClassLoader.install();
                //ConfigFetch.initialize();
            };
        };
        t.start();
    }
}
