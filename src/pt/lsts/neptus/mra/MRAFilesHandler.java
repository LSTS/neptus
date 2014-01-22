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
 * Jan 21, 2014
 */
package pt.lsts.neptus.mra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import pt.lsts.imc.lsf.LsfIndexListener;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.loader.FileHandler;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.llf.LsfLogSource;

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

    private NeptusMRA mra;

    private File tmpFile = null;
    private InputStream activeInputStream = null;

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
            NeptusLog.pub().info("<###>Log source was closed.");
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
    }

    /**
     * Open LSF Log file
     * @param f
     * @return
     */
    private boolean openLSF(File f) {
        mra.getBgp().block(true);
        mra.getBgp().setText(I18n.text("Loading LSF Data"));
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
            int option = JOptionPane.showConfirmDialog(mra,
                    I18n.text("This log seems to have already been indexed. Index again?"));

            if (option == JOptionPane.YES_OPTION) {
                new File(lsfDir, "mra/lsf.index").delete(); 
            }

            if (option == JOptionPane.CANCEL_OPTION) {
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

            mra.updateMissionFilesOpened(f);

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
        try {
            File res;
            mra.getBgp().setText(I18n.text("Decompressing LSF Data..."));
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
                GuiUtils.errorMessage(mra, e);
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
            mra.getBgp().setText(I18n.text("Decompressing LSF Data...") + "   "
                    + ioe.getMessage());
            ioe.printStackTrace();
            return null;
        }
    }

    /**
     * Extract BZip files with BZip2 compressor.
     * @param f
     * @return decompressed file
     */
    private File extractBzip2(File f) {
        mra.getBgp().setText(I18n.text("Decompressing BZip2 LSF Data..."));
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
                        mra.getBgp().setText(decompressed + " "
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
     * Decompresses bytes read from a input stream of data 
     * Monitors input data stream size
     * 
     * @author pdias
     */
    public abstract class FilterCopyDataMonitor extends FilterInputStream {

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
