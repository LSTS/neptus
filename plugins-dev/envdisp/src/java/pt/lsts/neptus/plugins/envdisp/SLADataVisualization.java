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
 * Mar 3, 2018
 */
package pt.lsts.neptus.plugins.envdisp;

import java.awt.Font;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBarPainterUtil;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.comm.ssh.SSHConnectionDialog;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.ftp.FtpDownloader;
import pt.lsts.neptus.gui.editor.FolderPropertyEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.envdisp.datapoints.BaseDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.SLADataPoint;
import pt.lsts.neptus.plugins.envdisp.painter.EnvDataPaintHelper;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;

/**
 * @author pdias
 *
 */
@PluginDescription(author = "Paulo Dias", name = "SLA Data Visualization", version = "1.0", 
icon = "pt/lsts/neptus/plugins/envdisp/hf-radar.png", description = "Sea level anomaly data visualization..")
@LayerPriority(priority = -301)
public class SLADataVisualization extends ConsoleLayer implements IPeriodicUpdates, ConfigurationListener {

    private static final String CATEGORY_TEST = "Test";
    private static final String CATEGORY_DATA_UPDATE = "Data Update";
    private static final String CATEGORY_VISIBILITY_SLA = "Visibility SLA";

    @NeptusProperty(name = "Show SLA", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SLA)
    public boolean showSLA = true;
    @NeptusProperty(name = "Show SLA legend", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SLA)
    public boolean showSLALegend = true;
    @NeptusProperty(name = "Show SLA legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SLA)
    public int showSLALegendFromZoomLevel = 13;
    @NeptusProperty(name = "Show SLA colorbar", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SLA,
            description = "Show the color scale bar. Only one will show.")
    public boolean showSLAColorbar = false;
    @NeptusProperty(name = "Colormap for SLA", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SLA)
    private ColorMap colorMapSLA = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Minutes between updates", category = CATEGORY_DATA_UPDATE)
    public int updateFileDataMinutes = 12 * 60;
    @NeptusProperty(name = "Data limit validity (hours)", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public int dateLimitHours = 30;
    @NeptusProperty(name = "Use data x hour in the future (hours)", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public int dateHoursToUseForData = 1;
    @NeptusProperty(name = "Ignore data limit validity to load data", userLevel=LEVEL.ADVANCED, category = CATEGORY_DATA_UPDATE)
    public boolean ignoreDateLimitToLoad = false;

    @NeptusProperty(name = "Base Folder For SLA netCDF Files", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE, 
            description = "The folder to look for SLA data. Admissible files '*.nc or *.nc.gz'. NetCDF variables used: lat, lon, time, sla.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForSLANetCDFFiles = new File("IHData/SLA");
    @NeptusProperty(name = "Request AVISO SLA_data from Copernicus.eu", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE,
            description = "You need an account from http://marine.copernicus.eu/services-portfolio/access-to-products/")
    public boolean requestSLAFromFTP = true;
    
    @NeptusProperty(name = "Show visible data date-time interval", userLevel = LEVEL.ADVANCED, category = CATEGORY_TEST, 
            description = "Draws the string with visible curents data date-time interval.")
    public boolean showDataDebugLegend = false;

    @NeptusProperty(name = "SLA min m", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SLA)
    private double minSLA = -1.0;
    @NeptusProperty(name = "SLA max m", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SLA)
    private double maxSLA = 1.0;

    @NeptusProperty(name = "CMEMS Copernicus.eu host", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE, editable = false)
    private String cmemsHost = "ftp.sltac.cls.fr";
    @NeptusProperty(name = "CMEMS Copernicus.eu AVISO SLA file", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE, editable = false)
    private String cmemsFile = "/Core/SEALEVEL_GLO_PHY_L4_NRT_OBSERVATIONS_008_046/dataset-duacs-nrt-global-merged-allsat-phy-l4-v3/nrt_global_allsat_phy_l4_latest.nc.gz";
    private String cmemsFileName = "nrt_global_allsat_phy_l4_latest.nc.gz";
    
    @NeptusProperty
    private static String cmemsUsername = "user";
    private static String cmemsPassword = null;
    private boolean forceCMEMSPassShow = true;
    
    private static final String netCDFFilePattern = ".\\.nc(\\.gz)?$";
    private static final String slaFilePattern = netCDFFilePattern;

    private final Font font8Pt = new Font("Helvetica", Font.PLAIN, 9);

    @SuppressWarnings("serial")
    static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH':'mm':'ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    @SuppressWarnings("serial")
    static final SimpleDateFormat dateTimeFormaterSpacesUTC = new SimpleDateFormat("yyyy MM dd  HH mm ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};

    private int updateSeconds = 30;
    private long lastMillisFileDataUpdated = System.currentTimeMillis() + 60000; // To defer the first run on start
    
    private OffScreenLayerImageControl offScreen = new OffScreenLayerImageControl();
    
    private FtpDownloader ftpDownloader = null;
    private FTPFile cmemsFTPFile = null;
    private InputStream stream;
    private File outFile = null;
    private File outTmpFile = null;
    private long fullSize = 0;
    private long downloadedSize = 0;
    
    // ID is lat/lon
    private final HashMap<String, SLADataPoint> dataPointsSLA = new HashMap<>();
    private Thread painterThread = null;
    private AtomicBoolean abortIndicator = null;

    public SLADataVisualization() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
        createNewCMEMSConection();
    }

    private void createNewCMEMSConection() {
        try {
            ftpDownloader = new FtpDownloader(cmemsHost, 21);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        if (ftpDownloader != null) {
            try {
                ftpDownloader.close();
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e.getMessage());
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        paintWorker(g, renderer);
    }
    
    public void paintWorker(Graphics2D go, StateRenderer2D renderer) {
        boolean recreateImage = offScreen.paintPhaseStartTestRecreateImageAndRecreate(go, renderer);
        if (recreateImage) {
            if (painterThread != null) {
                try {
                    abortIndicator.set(true);
                    painterThread.interrupt();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            final MapTileRendererCalculator rendererCalculator = new MapTileRendererCalculator(renderer);
            abortIndicator = new AtomicBoolean();
            painterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Graphics2D g2 = offScreen.getImageGraphics();

                        Date dateColorLimit = new Date(System.currentTimeMillis() - 3 * DateTimeUtil.HOUR);
                        Date dateLimit = new Date(System.currentTimeMillis() - dateLimitHours * DateTimeUtil.HOUR);
                        
                        if (showSLA) {
                            try {
                                EnvDataPaintHelper.paintSLAInGraphics(rendererCalculator, g2, 128, dateColorLimit, dateLimit, dataPointsSLA,
                                        ignoreDateLimitToLoad, offScreen.getOffScreenBufferPixel(), colorMapSLA, minSLA, maxSLA, showSLALegend,
                                        showSLALegendFromZoomLevel, font8Pt, showDataDebugLegend, abortIndicator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreen.triggerImageRebuild();
                            }
                        }
                        
                        g2.dispose();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        offScreen.triggerImageRebuild();
                    }
                    catch (Error e) {
                        e.printStackTrace();
                        offScreen.triggerImageRebuild();
                    }
                }
            }, "SLA::Painter");
            painterThread.setDaemon(true);
            painterThread.start();
        }            
        offScreen.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(go, renderer);
        
        paintColorbars(go, renderer);
    }

    /**
     * @param go
     * @param renderer
     */
    private void paintColorbars(Graphics2D go, StateRenderer2D renderer) {
        int offsetHeight = 130;
        int offsetWidth = 5;
        int offsetDelta = 130;
        if (showSLA && showSLAColorbar) {
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colorMapSLA, I18n.text("SLA"), "m", minSLA, maxSLA);
            gl.dispose();
            offsetHeight += offsetDelta;
        }
    }

    public String validateUpdateFileDataMinutes(int value) {
        if (value < 1 && value > 10)
            return "Keep it between 1 and 10";
        return null;
    }

    public String validateDateLimitHours(int value) {
        if (value < 3 && value > 24 * 40)
            return "Keep it between 3 and 24*40=960";
        return null;
    }

    public String validateDateHoursToUseForData(int value) {
        if (value < 0)
            return "Keep it above 0";
        return null;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return updateSeconds * 1000;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public synchronized boolean update() {
        if (lastMillisFileDataUpdated <= 0
                || System.currentTimeMillis() - lastMillisFileDataUpdated >= updateFileDataMinutes * 60 * 1000) {
            
            NeptusLog.pub().info("Update Data");

            lastMillisFileDataUpdated = System.currentTimeMillis();

            outFile = new File(baseFolderForSLANetCDFFiles, cmemsFileName);
            outTmpFile = new File(baseFolderForSLANetCDFFiles, cmemsFileName + ".dpart");
            try {
                if (requestSLAFromFTP) {
                    downloadSLADataFromCMEMSFTP();
                }

                loadSLAFromFiles();
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
            }

            try {
                cleanUpData();
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
            }
        }

        return true;
    }

    /**
     * 
     */
    private void downloadSLADataFromCMEMSFTP() {
        if (ftpDownloader == null) {
            createNewCMEMSConection();
        }
        
        if (ftpDownloader != null) {
            try {
                if (!ftpDownloader.isConnected()) {
                    if (forceCMEMSPassShow || cmemsPassword == null) {
                        String[] ret = SSHConnectionDialog.showConnectionDialog(cmemsHost, cmemsUsername, cmemsPassword == null ? ""
                                : cmemsPassword, 21, "SLA Data", SwingUtilities.windowForComponent(getConsole()), false, false, true, true);
                        if (ret.length == 0)
                            forceCMEMSPassShow = true;
                        else
                            forceCMEMSPassShow = false;

                        cmemsUsername = ret[1];
                        cmemsPassword = ret[2];
                    }
                    else {
                        forceCMEMSPassShow = false;
                    }

                    ftpDownloader.renewClient(cmemsUsername, cmemsPassword);
                    
                    int rpc = ftpDownloader.getClient().getReplyCode();
                    if(!FTPReply.isPositiveCompletion(rpc)) {
                        NeptusLog.pub().warn(ftpDownloader.getClient().getReplyString());
                        forceCMEMSPassShow = true;
                    }                                
                }

                String fileName = new String(cmemsFile.getBytes(), "ISO-8859-1");
                FTPFile[] filesLsit = ftpDownloader.getClient().listFiles(fileName);
                cmemsFTPFile = filesLsit[0];
                
                // Check size and date-time match
                if (outFile.exists()) {
                    if (outFile.lastModified() == cmemsFTPFile.getTimestamp().getTimeInMillis() &&
                            FileUtils.sizeOf(outFile) == cmemsFTPFile.getSize()) {
                        NeptusLog.pub().warn("Skip download of " + outFile.getName());
                        return; // skipDownload 
                    }
                }
                
                stream = ftpDownloader.getClient().retrieveFileStream(fileName);

                fullSize = cmemsFTPFile.getSize();
                downloadedSize = 0;

                outTmpFile.getParentFile().mkdirs();

                try {
                    outTmpFile.createNewFile();
                } 
                catch (IOException e) {
                    NeptusLog.pub().warn(e.getMessage());
                }

                FilterInputStream ioS = new FilterInputStream(stream) {
                    @Override
                    public int read() throws IOException {
                        int tmp = super.read();
                        downloadedSize += (tmp == -1) ? 0 : 1;
                        return tmp;
                    }

                    /*This one just calls "read(byte[] b, int off, int len)",
                     * so no need to implemented
                     */
                    //public int read(byte[] b) throws IOException {

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        int tmp = super.read(b, off, len);
                        downloadedSize += (tmp == -1) ? 0 : tmp;
                        return tmp;
                    }
                };
                boolean streamRes = true; //StreamUtil.copyStreamToFile(ioS, outTmpFile, false);
                try {
                    FileUtils.copyInputStreamToFile(ioS, outTmpFile);
                }
                catch (Exception e) {
                    NeptusLog.pub().warn(e.getMessage());
                    streamRes = false;
                }
                outTmpFile.setLastModified(Math.max(cmemsFTPFile.getTimestamp().getTimeInMillis(), 0));

                NeptusLog.pub().info("To receive / received: " + fullSize + "/" + downloadedSize);

                if (streamRes && fullSize == downloadedSize) {
                    if (outFile.exists())
                        outFile.delete();
                    FileUtils.moveFile(outTmpFile, outFile);
                }
                else {
                    try {
                        if (outTmpFile.exists())
                            outTmpFile.delete();
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(e.getMessage());
                    }
                }
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e.getMessage());
                if (outFile.exists())
                    outTmpFile.delete();
            }
            finally {
                if (ftpDownloader != null && ftpDownloader.isConnected()) {
                    try {
                        ftpDownloader.close();
                    }
                    catch (IOException e) {
                        NeptusLog.pub().warn(e.getMessage());
                    }
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (updateFileDataMinutes < 1)
            updateFileDataMinutes = 1;
        if (updateFileDataMinutes > 10)
            updateFileDataMinutes = 10;
        
        if (dateLimitHours < 3)
            dateLimitHours = 3;
        if (dateLimitHours > 24 * 5)
            dateLimitHours = 24 * 5;

        if (dateHoursToUseForData < 0)
            dateHoursToUseForData = 0;

        if (minSLA >= maxSLA)
            minSLA = maxSLA - 10;

        lastMillisFileDataUpdated = -1;
        cleanUpData();
    }

    private void cleanUpData() {
        cleanDataPointsBeforeDate();
        updateValues();
        offScreen.triggerImageRebuild();
    }

    private Date createDateToMostRecent() {
        Date nowDate = new Date(System.currentTimeMillis() + dateHoursToUseForData * DateTimeUtil.HOUR);
        return nowDate;
    }
    
    private Date createDateLimitToRemove() {
        Date dateLimit = new Date(System.currentTimeMillis() - dateLimitHours * DateTimeUtil.HOUR);
        return dateLimit;
    }
    
    private void updateValues() {
        Date nowDate = createDateToMostRecent();

        for (String dpID : dataPointsSLA.keySet().toArray(new String[0])) {
            SLADataPoint dp = dataPointsSLA.get(dpID);
            if (dp == null)
                continue;
            dp.useMostRecent(nowDate);
        }
    }

    private void cleanDataPointsBeforeDate() {
        Date dateLimit = ignoreDateLimitToLoad ? null : createDateLimitToRemove();
        
        BaseDataPoint.cleanDataPointsBeforeDate(dataPointsSLA, dateLimit);
    }

    private void loadSLAFromFiles() {
        File[] fileList = FileUtil.getFilesFromDisk(baseFolderForSLANetCDFFiles, slaFilePattern);
        if (fileList == null)
            return;
        
        for (File fx : fileList) {
            try {
                HashMap<String, SLADataPoint> sladp = processSLAFile(fx.getAbsolutePath());
                if (sladp != null && sladp.size() > 0)
                    EnvironmentalDataVisualization.mergeDataToInternalDataList(dataPointsSLA, sladp);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e.getMessage());
            }
            
            if ("gz".equalsIgnoreCase(FileUtil.getFileExtension(fx))) {
                String absPath = fx.getAbsolutePath();
                absPath = absPath.replaceAll("\\.gz$", "");
                File unzipedFile = new File(absPath);
                if (unzipedFile.exists()) {
                    try {
                        FileUtils.forceDelete(unzipedFile);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    private HashMap<String, SLADataPoint> processSLAFile(String fileName) {
        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
        if (fxName == null)
            fxName = fileName;
        if (!new File(fxName).exists())
            return new HashMap<>();
        return LoaderHelper.processSLAFile(fxName, ignoreDateLimitToLoad ? null : createDateLimitToRemove());
    }

    @SuppressWarnings("unused")
    private void debugOut(Object message) {
        if (showDataDebugLegend)
            System.out.println(message);
        else
            NeptusLog.pub().debug(message);
    }

    @SuppressWarnings("unused")
    private void debugOut(Object message, Throwable t) {
        if (showDataDebugLegend) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.out.println(message + "\n" + sw.toString());
        }
        else {
            NeptusLog.pub().debug(message, t);
        }
    }
}
