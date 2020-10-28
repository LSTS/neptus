/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 21, 2014
 */
package pt.lsts.neptus.mra;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;

import pt.lsts.imc.lsf.LsfIndexListener;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.loader.FileHandler;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.platform.OsInfo;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.RecentlyOpenedFilesUtil;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LsfLogSource;
import pt.lsts.neptus.util.llf.LsfReport;
import pt.lsts.neptus.util.llf.LsfReportProperties;

/**
 * MRA Files Handler
 * - Extractors
 * - Open/Close log files
 * 
 * @author ZP
 * @author pdias (LSF)
 * @author jqcorreia
 * @author hfq
 */
public class MRAFilesHandler implements FileHandler {
    private static final String RECENTLY_OPENED_LOGS = "conf/mra_recent.xml";

    private LinkedHashMap<JMenuItem, File> miscFilesOpened = new LinkedHashMap<JMenuItem, File>();

    private NeptusMRA mra;

    private File tmpFile = null;
    private InputStream activeInputStream = null;

    /**
     * Constructor
     * 
     * @param mra
     */
    public MRAFilesHandler(NeptusMRA mra) {
        this.mra = mra;
    }

    /**
     * Does the necessary pre-processing of a log file based on it's extension
     * Currently supports gzip, bzip2 and no-compression formats.
     * @param fx
     * @return True on success, False on failure
     */
    public boolean openLog(File fx) {
        mra.getBgp().block(true);
        File fileToOpen = null;

        String errorMessage = "";

        if (fx.getName().toLowerCase().endsWith(FileUtil.FILE_TYPE_LSF_COMPRESSED)) {
            fileToOpen = extractGzip(fx);
        }
        else if (fx.getName().toLowerCase().endsWith(FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2)) {
            fileToOpen = extractBzip2(fx);
        }        
        else if (fx.getName().toLowerCase().endsWith(FileUtil.FILE_TYPE_LSF)) {
            fileToOpen = fx;
        }

        mra.getBgp().block(false);
        if (fileToOpen == null) {
            errorMessage = mra.getBgp().getText();
            GuiUtils.errorMessage(mra, I18n.text("Invalid LSF file"), I18n.text("LSF file does not exist!") + "\n"
                    + errorMessage);
            return false;
        }

        return openLSF(fileToOpen);
    }

    /**
     * Abort al actions pending while opening a log file.
     */
    protected void abortPendingOpenLogActions() {
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

    /**
     * Cleans up mraPanel and calls methos from MRAPanel to clean up visualizations and log source.
     */
    private void closeLogSource() {
        if (mra.getMraPanel() != null) {
            mra.getMraPanel().cleanup();
            mra.setMraPanel(null);
            mra.getContentPane().removeAll();
            NeptusLog.pub().info("Log source was closed.");
        }
    }

    /**
     * Open IMraLogGroup source
     * Enables SetMission and GenReport menu items.
     * @param source
     */
    private void openLogSource(IMraLogGroup source) {
        abortPendingOpenLogActions();
        closeLogSource();
        mra.getContentPane().removeAll();
        mra.setMraPanel(new MRAPanel(source, mra));
        mra.getContentPane().add(mra.getMraPanel());
        mra.invalidate();
        mra.validate();
        mra.getMRAMenuBar().getSetMissionMenuItem().setEnabled(true);
        mra.getMRAMenuBar().getGenReportMenuItem().setEnabled(true);
        mra.getMRAMenuBar().getGenReportCustomOptionsMenuItem().setEnabled(true);
    }

    /**
     * Open LSF Log file
     * @param f
     * @return
     */
    private boolean openLSF(File f) {
        mra.getBgp().block(true);
        mra.getBgp().setText(I18n.text("Loading LSF Data"));

        if (!f.exists()) {
            mra.getBgp().block(false);
            GuiUtils.errorMessage(mra, I18n.text("Invalid LSF file"), I18n.text("LSF file does not exist!"));
            return false; 
        }

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
            int option = GuiUtils.confirmDialogWithCancel(mra, I18n.text("Open Log"),
                    I18n.text("This log seems to have already been indexed. Index again?"));

            if (option == JOptionPane.YES_OPTION) {
                try {
                    FileUtils.deleteDirectory(new File(lsfDir, "mra"));
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error while trying to delete mra/ folder", e);
                }
            }

            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                mra.getBgp().block(false);
                return false;
            }
        }

        mra.getBgp().setText(I18n.text("Loading LSF Data"));

        try {
            LsfLogSource source = new LsfLogSource(f, new LsfIndexListener() {

                @Override
                public void updateStatus(String messageToDisplay) {
                    mra.getBgp().setText(messageToDisplay);
                }
            });

            updateMissionFilesOpened(f);

            mra.getBgp().setText(I18n.text("Starting interface"));
            openLogSource(source);            
            mra.getBgp().setText(I18n.text("Done"));

            mra.getBgp().block(false);
            return true;
        }
        catch (Exception e) {
            mra.getBgp().block(false);
            e.printStackTrace();
            GuiUtils.errorMessage(mra, I18n.text("Invalid LSF index"), I18n.text(e.getMessage()));
            return false;    
        }
    }

    // --- Extractors ---
    /**
     * Extract GNU zip files
     * @param f input file
     * @return decompressed file
     */
    private File extractGzip(File f) {
        GzipCompressorInputStream gzDataLog = null;
        try {
            mra.getBgp().setText(I18n.text("Decompressing LSF Data..."));
            gzDataLog = new GzipCompressorInputStream(new FileInputStream(f), true);
            activeInputStream = gzDataLog;
            File outputFile = new File(f.getParent(), "Data.lsf");
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            
            FilterCopyDataMonitor fis = createCopyMonitor(gzDataLog);
            StreamUtil.copyStreamToFile(fis, outputFile);

            File res = new File(f.getParent(), "Data.lsf");

            return res;
        }
        catch (Exception ioe) {
            System.err.println("Exception has been thrown: " + ioe);
            mra.getBgp().setText(I18n.text("Decompressing LSF Data...") + "   "
                    + ioe.getMessage());
            ioe.printStackTrace();
            return null;
        }
        finally {
            if (gzDataLog != null) {
                try {
                    gzDataLog.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Extract BZip files with BZip2 compressor.
     * @param f
     * @return decompressed file
     */
    private File extractBzip2(File f) {
        mra.getBgp().setText(I18n.text("Decompressing BZip2 LSF Data..."));
        BZip2CompressorInputStream bz2DataLog = null;
        try {
            FileInputStream fxInStream = new FileInputStream(f);
            bz2DataLog = new BZip2CompressorInputStream(fxInStream, true);
            activeInputStream = bz2DataLog;
            File outFile = new File(f.getParent(), "Data.lsf");
            if (!outFile.exists()) {
                outFile.createNewFile();
            }

            FilterCopyDataMonitor fis = createCopyMonitor(bz2DataLog);
            StreamUtil.copyStreamToFile(fis, outFile);
            
            return outFile;
        }
        catch (Exception e) {
            System.err.println("Exception has been thrown: " + e);
            mra.getBgp().setText(I18n.text("Decompressing LSF Data...") + "   "
                    + e.getMessage());
            e.printStackTrace();
            return null;
        }
        finally {
            if (bz2DataLog != null) {
                try {
                    bz2DataLog.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param stream
     * @return
     */
    private FilterCopyDataMonitor createCopyMonitor(InputStream stream) {
        FilterCopyDataMonitor fis = new FilterCopyDataMonitor(stream) {
            long targetStep = 1 * 1024 * 1024;
            long target = targetStep;
            protected String decompressed = I18n.text("Decompressed") + " ";

            @Override
            public void updateValueInMessagePanel() {
                if (downloadedSize > target) {
                    mra.getBgp().setText(decompressed
                            + MathMiscUtils.parseToEngineeringRadix2Notation(downloadedSize, 2) + "B");
                    target += targetStep;
                }
            }
        };
        return fis;
    }

    /**
     * Decompresses bytes read from a input stream of data 
     * Monitors input data stream size
     * 
     * @author pdias
     */
    private abstract class FilterCopyDataMonitor extends FilterInputStream {

        public long downloadedSize = 0;

        /**
         * @param in
         */
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

    // --- Generate PDF Reports --- 
    /**
     * Generates PDF report from log file.
     */
    public void generatePDFReport(final File f) {

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
//                GuiUtils.infoMessage(mra, I18n.text("Generating PDF Report..."), I18n.text("Generating PDF Report..."));
                mra.getMRAMenuBar().getReportMenuItem().setEnabled(false);
                LsfReportProperties.generatingReport = true;
                mra.getMraPanel().addStatusBarMsg("Generating Report...");
                return LsfReport.generateReport(mra.getMraPanel().getSource(), f, mra.getMraPanel());
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
                        GuiUtils.infoMessage(mra, I18n.text("Generate PDF Report"),
                                I18n.text("File saved to") +" "+ f.getAbsolutePath());
                        final String pdfF = f.getAbsolutePath();
                        int resp = GuiUtils.confirmDialog(mra, I18n.text("Open PDF Report"), I18n.text("Do you want to open PDF Report file?"));
                        if (resp == JOptionPane.YES_OPTION) {
                            new Thread() {
                                @Override
                                public void run() {
                                    openPDFInExternalViewer(pdfF);
                                };
                            }.start();
                        }
                    }
                }
                catch (Exception e) {
                    GuiUtils.errorMessage(mra, I18n.text("PDF Creation Process"), "<html>"+I18n.text("PDF <b>was not</b> saved to file.")
                            + "<br>"+I18n.text("Error")+": " + e.getMessage() + "</html>");
                    e.printStackTrace();
                }
                finally {
                    mra.getBgp().block(false);
                    mra.getMRAMenuBar().getReportMenuItem().setEnabled(true);
                    LsfReportProperties.generatingReport=false;
                    mra.getMraPanel().reDrawStatusBar();
                }
            }
        };
        worker.execute();
    }

    /**
     * Opens generated pdf report on default OS viewer.
     * 
     * @param pdf
     */
    private void openPDFInExternalViewer(String pdf) {
        try {
            if (OsInfo.getName() == OsInfo.Name.WINDOWS) {
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
       // GuiUtils.infoMessage(mra,  I18n.text("PDF Report Generated"),
       //         I18n.text("Opening file") +" "+ pdf);
    }

    // --- Recently opened files ---

    /**
     * Load Recently opened files from conf/mra_recent.xml
     */
    public void loadRecentlyOpenedFiles() {
        String recentlyOpenedFiles = ConfigFetch.resolvePath(RECENTLY_OPENED_LOGS);

        if (recentlyOpenedFiles == null || !new File(recentlyOpenedFiles).exists())
            return;

        Method methodUpdate = null;

        try {
            Class<?>[] params = { File.class };
            methodUpdate = this.getClass().getMethod("updateMissionFilesOpened", params);
            if(methodUpdate == null) {
                NeptusLog.pub().info("Method update = null");
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this + "loadRecentlyOpenedFiles", e);
            return;
        }

        //        if (recentlyOpenedFiles == null) {
        //            JOptionPane.showInternalMessageDialog(mra, "Cannot Load");
        //            return;
        //        }

        //        if (!new File(recentlyOpenedFiles).exists())
        //            return;

        RecentlyOpenedFilesUtil.loadRecentlyOpenedFiles(recentlyOpenedFiles, methodUpdate, this);
    }

    /**
     * Updates misstion files opened
     * @param fx
     * @return
     */
    public boolean updateMissionFilesOpened(File fx) {
        RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, getMiscFilesOpened(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final File fx;
                Object key = e.getSource();
                File value = getMiscFilesOpened().get(key);
                if (value instanceof File) {
                    fx = (File) value;
                    Thread t = new Thread("Open Log") {
                        @Override
                        public void run() {
                            mra.getMraFilesHandler().openLog(fx);
                        };
                    };
                    t.start();
                }
                else
                    return;
            }
        });

        mra.getMRAMenuBar().getRecentlyOpenFilesMenu();
        storeRecentlyOpenedFiles();
        return true;
    }

    /**
     * Updates /conf/mra_recent.xml
     */
    private void storeRecentlyOpenedFiles() {
        String recentlyOpenedFiles;
        LinkedHashMap<JMenuItem, File> hMap;
        String header;

        recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(RECENTLY_OPENED_LOGS);
        hMap = getMiscFilesOpened();
        header = I18n.text("Recently opened mission files")+".";

        RecentlyOpenedFilesUtil.storeRecentlyOpenedFiles(recentlyOpenedFiles, hMap, header);
    }

    /**
     * @return the miscFilesOpened
     */
    public LinkedHashMap<JMenuItem, File> getMiscFilesOpened() {
        return miscFilesOpened;
    }

    /**
     * @param miscFilesOpened the miscFilesOpened to set
     */
    public void setMiscFilesOpened(LinkedHashMap<JMenuItem, File> miscFilesOpened) {
        this.miscFilesOpened = miscFilesOpened;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.loader.FileHandler#handleFile(java.io.File)
     */
    @Override
    public void handleFile(File f) {
        openLog(f);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.loader.FileHandler#getName()
     */
    @Override
    public String getName() {
        return null;
    }
}
