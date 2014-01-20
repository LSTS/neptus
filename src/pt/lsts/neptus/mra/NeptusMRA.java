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
package pt.lsts.neptus.mra;

import java.awt.Color;
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
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import pt.lsts.imc.lsf.LsfIndexListener;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.BlockingGlassPane;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.loader.FileHandler;
import pt.lsts.neptus.loader.NeptusMain;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.RecentlyOpenedFilesUtil;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * Neptus MRA main class
 * - sets up MRA frame
 * 
 * @author ZP
 * @author pdias (LSF)
 * @author jqcorreia
 * @author hfq
 */
@SuppressWarnings("serial")
public class NeptusMRA extends JFrame implements FileHandler {
    protected static final String MRA_TITLE = I18n.text("Neptus Mission Review And Analysis");
    protected static final String RECENTLY_OPENED_LOGS = "conf/mra_recent.xml";

    public static boolean vtkEnabled = true;

    private MRAProperties mraProperties = new MRAProperties();

    private File tmpFile = null;
    private InputStream activeInputStream = null;

    private LinkedHashMap<JMenuItem, File> miscFilesOpened = new LinkedHashMap<JMenuItem, File>();
    private JMenu recentlyOpenFilesMenu = null;
    private MRAPanel mraPanel = null;

    private BlockingGlassPane bgp = new BlockingGlassPane(400);

    protected MRAMenuBar mraMenuBar;

    /**
     * Constructor
     */
    public NeptusMRA() {
        super(MRA_TITLE);
        try {
            PluginUtils.loadProperties("conf/mra.properties", getMraProperties());
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Not possible to open")
                    + " \"conf/mra.properties\"");
        }
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        setSize(1200, 700);

        setIconImage(Toolkit.getDefaultToolkit().getImage(ConfigFetch.class.getResource("/images/neptus-icon.png")));

        GuiUtils.centerOnScreen(this);

        mraMenuBar = new MRAMenuBar(this);
        setJMenuBar(mraMenuBar.getMenuBar());

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

                if (getMraPanel() != null)
                    getMraPanel().cleanup();
                setMraPanel(null);

                abortPendingOpenLogActions();
                NeptusMRA.this.getContentPane().removeAll();
                NeptusMRA.this.dispose();

                ConfigFetch.setSuperParentFrameForced(null);                
            }
        });

        setGlassPane(getBgp());
        repaint();
    }

    /**
     * @return a new NeptusMRA instance
     */
    public static NeptusMRA showApplication() {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();

        return new NeptusMRA();
    }

    /**
     * 
     */
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
        if (getMraPanel() != null) {
            getMraPanel().cleanup();
            setMraPanel(null);
            getContentPane().removeAll();
            NeptusLog.pub().info("<###>Log source was closed.");
        }
    }

    public void openLogSource(IMraLogGroup source) {
        abortPendingOpenLogActions();
        closeLogSource();
        getContentPane().removeAll();
        setMraPanel(new MRAPanel(source,this));
        getContentPane().add(getMraPanel());
        invalidate();
        validate();
        mraMenuBar.setMission.setEnabled(true);
        mraMenuBar.genReport.setEnabled(true);
    }

    // --- Extractors ---
    public File extractGzip(File f) {
        try {
            File res;
            getBgp().setText(I18n.text("Decompressing LSF Data..."));
            GZIPInputStream ginstream = new GZIPInputStream(new FileInputStream(f));
            activeInputStream = ginstream;
            File outputFile = new File(f.getParent(), "Data.lsf");
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            FileOutputStream outstream = new FileOutputStream(outputFile, false);
            byte[] buf = new byte[2048];
            int len;
            try {
                while ((len = ginstream.read(buf)) > 0) {
                    outstream.write(buf, 0, len);
                }
            }
            catch (Exception e) {
                GuiUtils.errorMessage(NeptusMRA.this, e);
                NeptusLog.pub().error(e);
            }
            finally {
                ginstream.close();
                outstream.close();
            }
            res = new File(f.getParent(), "Data.lsf");

            return res;
        }
        catch (IOException ioe) {
            System.err.println("Exception has been thrown: " + ioe);
            getBgp().setText(I18n.text("Decompressing LSF Data...") + "   "
                    + ioe.getMessage());
            ioe.printStackTrace();
            return null;
        }
    }

    public File extractBzip2(File f) {
        getBgp().setText(I18n.text("Decompressing BZip2 LSF Data..."));
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
                        getBgp().setText(decompressed + " "
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
        getBgp().block(true);
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

        getBgp().block(false);
        return openLSF(fileToOpen);
    }


    public boolean openLSF(File f) {
        getBgp().block(true);
        getBgp().setText(I18n.text("Loading LSF Data"));
        final File lsfDir = f.getParentFile();

        //IMCDefinition.pathToDefaults = ConfigFetch.getDefaultIMCDefinitionsLocation();

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
                getBgp().block(false);
                return false;
            }
        }

        getBgp().setText(I18n.text("Loading LSF Data"));

        try {
            LsfLogSource source = new LsfLogSource(f, new LsfIndexListener() {

                @Override
                public void updateStatus(String messageToDisplay) {
                    getBgp().setText(messageToDisplay);
                }
            });

            updateMissionFilesOpened(f);

            getBgp().setText(I18n.text("Starting interface"));
            openLogSource(source);            
            getBgp().setText(I18n.text("Done"));

            getBgp().block(false);
            return true;
        }
        catch (Exception e) {
            getBgp().block(false);
            e.printStackTrace();
            GuiUtils.errorMessage(NeptusMRA.this, I18n.text("Invalid LSF index"), I18n.text(e.getMessage()));
            return false;    
        }
    }

    /**
     * @param pdf
     * FIXME better suited for utils?
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
    protected JMenu getRecentlyOpenFilesMenu() {
        if (recentlyOpenFilesMenu == null) {
            recentlyOpenFilesMenu = new JMenu();
            recentlyOpenFilesMenu.setText(I18n.text("Recently opened"));
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, getMiscFilesOpened());
        }
        else {
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, getMiscFilesOpened());
        }
        return recentlyOpenFilesMenu;
    }

    protected void loadRecentlyOpenedFiles() {
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
        RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, getMiscFilesOpened(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final File fx;
                Object key = e.getSource();
                File value = getMiscFilesOpened().get(key);
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
                        @Override
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

    protected void storeRecentlyOpenedFiles() {
        String recentlyOpenedFiles;
        LinkedHashMap<JMenuItem, File> hMap;
        String header;

        recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(RECENTLY_OPENED_LOGS);
        hMap = getMiscFilesOpened();
        header = I18n.text("Recently opened mission files")+".";

        RecentlyOpenedFilesUtil.storeRecentlyOpenedFiles(recentlyOpenedFiles, hMap, header);
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

    @Override
    public void handleFile(File f) {
        openLog(f);
    }

    /**
     * @return the mraMenuBar
     */
    public JMenuBar getMRAMenuBar() {
        return mraMenuBar;
    }

    /**
     * @return the miscFilesOpened
     */
    protected LinkedHashMap<JMenuItem, File> getMiscFilesOpened() {
        return miscFilesOpened;
    }

    /**
     * @return the MRAPanel
     */
    protected MRAPanel getMraPanel() {
        return mraPanel;
    }

    /**
     * @param mraPanel
     */
    private void setMraPanel(MRAPanel mraPanel) {
        this.mraPanel = mraPanel;
    }

    /**
     * @return the bgp
     */
    protected BlockingGlassPane getBgp() {
        return bgp;
    }

    protected MRAProperties getMraProperties() {
        return mraProperties;
    }

    protected void setMraProperties(MRAProperties mraProperties) {
        this.mraProperties = mraProperties;
    }

    public static void main(String[] args) {
        NeptusMain.main(new String[] {"mra"});
    }
}
