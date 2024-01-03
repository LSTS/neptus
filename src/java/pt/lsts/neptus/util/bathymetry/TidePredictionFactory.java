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
 * Author: zp
 * Dec 4, 2013
 */
package pt.lsts.neptus.util.bathymetry;

import java.awt.Component;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;

import com.google.common.collect.Lists;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 * @author pdias
 */
public class TidePredictionFactory {

    public static final String MRA_TIDE_INDICATION_FILE = "tide.info";
    public static final String MRA_TIDE_INDICATION_FILE_PATH = TidePredictionFactory.MRA_TIDE_INDICATION_FILE;
    public static final String MRA_TIDE_INDICATION_FILE_PATH_OLD = "mra/" + TidePredictionFactory.MRA_TIDE_INDICATION_FILE;

    public static final String BASE_TIDE_FOLDER_PATH = ConfigFetch.getConfFolder() + "/tides";
    
    public static final String NO_TIDE_STR = "<" + I18n.text("No tides") + ">";
    public static final String OTHER_TIDE_STR = "<" + I18n.text("Other") + ">";

    public static String[] logFolderTidesFileOptions = { "Tides.tid", "tides.tid", "Tides.txt", "tides.txt" };
    public static String[] logFolderTidesFileExtensions = { "tid", "txt" };
    
    public static String defaultTideFormat = "tid";
    
    private static File tideFileInUse = GeneralPreferences.tidesFile;
    private static TidePredictionFinder cached = null;

    /** Avoid instantiation. */
    private TidePredictionFactory() {
    }
    
    /**
     * Return the current tide level for the loaded tide {@link #tideFileInUse}.
     * @return
     */
    public static double currentTideLevel() {
        return getTideLevel(System.currentTimeMillis());
    }
    
    /**
     * Return the tide level for the loaded tide {@link #tideFileInUse}
     * at time provided.
     * @return
     */
    public static double getTideLevel(long timestampMillis) {
        return getTideLevel(new Date(timestampMillis));
    }
    
    /**
     * Return the tide level for the loaded tide {@link #tideFileInUse}
     * at time provided.
     * Loads or reloads the tides according with {@link GeneralPreferences#tidesFile}.
     * @return
     */
    public static double getTideLevel(Date date) {
        if (tideFileInUse != GeneralPreferences.tidesFile || cached == null) {
            File fxToLoad = GeneralPreferences.tidesFile;
            cached = createWorker(fxToLoad, null);
            tideFileInUse = fxToLoad;
        }
        
        try {
            return cached.getTidePrediction(date, false);
        }
        catch (NullPointerException e) {
            if (cached == null) {
                NeptusLog.pub().debug("Nullpointer error getting tide data. Caller " + ReflectionUtil.getCallerStamp()
                    + ". " + e.getMessage() + " " + e);
            }
            else {
                NeptusLog.pub().debug("Nullpointer error getting tide for date " + date + ". Caller " + ReflectionUtil.getCallerStamp()
                    + ". " + e.getMessage() + " " + e);
            }
            return 0;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error getting tide for date " + date + ". Caller " + ReflectionUtil.getCallerStamp()
                    + ". " + e.getMessage() + " " + e);
            return 0;
        }
    }
    
    /**
     * Return the current tides source as string.
     * 
     * @return
     */
    public static String getTideSourceString() {
        if (tideFileInUse == null)
            return null;
        
        return tideFileInUse.getName();
    }

    /**
     * Return the current tides source file.
     * 
     * @return
     */
    public static File getTideSourceFile() {
        return tideFileInUse;
    }

    /**
     * Return the file from the source name. It is assumed the tides
     * folder ({@link #BASE_TIDE_FOLDER_PATH}).
     * 
     * @param source
     * @return
     */
    public static File getTidesSourceFileFrom(String source) {
        return new File(BASE_TIDE_FOLDER_PATH + "/" + source);
    }
    
    /**
     * Creates a tide finder with either data in log ({@link logFolderTidesFileOptions})
     * or from {@link GeneralPreferences#tidesFile}.
     * If none found return null.
     * @param source
     * @return
     */
    public static TidePredictionFinder create(IMraLogGroup source) {
        File dir = source.getDir();
        Date date = new Date((long)(source.getLsfIndex().getStartTime())* 1000);
        return createForLogWorker(dir, date);
    }
    
    /**
     * Creates a tide finder with either data in log ({@link logFolderTidesFileOptions})
     * or from {@link GeneralPreferences#tidesFile}.
     * If none found return null.
     * @param source
     * @return
     */
    public static TidePredictionFinder create(LsfIndex source) {
        File dir = source.getLsfFile().getParentFile();
        Date date = new Date((long)(source.getStartTime())* 1000);
        return createForLogWorker(dir, date);
    }

    private static TidePredictionFinder createForLogWorker(File dir, Date startDate) {
        File tideInfoFx = new File(dir, MRA_TIDE_INDICATION_FILE_PATH);
        if (tideInfoFx == null || !tideInfoFx.exists() || !tideInfoFx.canRead())
            return null;
        String hF = FileUtil.getFileAsString(tideInfoFx);
        if (hF == null || hF.isEmpty())
            return null;
        File fx = new File(BASE_TIDE_FOLDER_PATH, hF);
        if (fx == null || !fx.exists() || !fx.canRead())
            return null;
        
        return createWorker(fx, startDate);
    }

    /**
     * Worker to load tide file (extension indicates the format)
     * If none found loads the tides according with {@link GeneralPreferences#tidesFile}.
     * @param baseDir The base folder to look for the fileNames.
     * @param date The date we want to be in the data. Can be null.
     * @param fileNames Alternative file names for the tide data.
     * @return
     */
    private static TidePredictionFinder createWorker(File baseDir, Date date, String... fileNames) {
        TidePredictionFinder finder = null;
        for (String nm : fileNames) {
            File fx = new File(baseDir, nm);
            if (fx.exists())
                finder = createWorker(fx, date);
        }
        if (finder == null)
            finder = createWorker(null, date);
        
        return finder;
    }

    /**
     * Worker to load tide file (extension indicates the format)
     * If none found loads the tides according with {@link GeneralPreferences#tidesFile}.
     * @param fx The tide file to load.
     * @param date The date we want to be in the data. Can be null.
     * @return
     */
    private static TidePredictionFinder createWorker(File fx, Date date) {
        TidePredictionFinder finder = null;
        if (fx != null && fx.isDirectory()) {
            return createWorker(fx, null, logFolderTidesFileOptions);
        }
        else {
            if (fx != null && fx.canRead()) {
                switch (FileUtil.getFileExtension(fx)) {
                    case "txt":
                        finder = new CachedData(fx);
                        break;
                    case "tid":
                        finder = new TidCachedData(fx);
                        break;
                    default:
                        break;
                }
            }
            
            File defaultFx = GeneralPreferences.tidesFile;
            if (!defaultFx.exists()) {
                String newDefaultPath = FileUtil.replaceFileExtension(defaultFx, defaultTideFormat);
                defaultFx = new File(newDefaultPath);
                GeneralPreferences.tidesFile = defaultFx;
                GeneralPreferences.saveProperties();
            }
            if (finder == null && (fx != null && defaultFx != null && fx.compareTo(defaultFx) != 0)) {
                TidePredictionFinder data = createWorker(defaultFx, null);
                if (date == null || data.contains(date))
                    finder = data;
                else
                    finder = null;
            }
            
            return finder;
        }
    }
    
    /**
     * Lists the tide list in the {@link ConfigFetch#getConfFolder()}/tides folder.
     * @return
     */
    public static File[] getTidesFileList() {
        File[] ret = new File[0];
        File baseTideFolder = new File(BASE_TIDE_FOLDER_PATH);
        if (baseTideFolder.exists()) {
            ret = baseTideFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String ext = FileUtil.checkFileForExtensions(name, logFolderTidesFileExtensions);
                    return ext == null ? false : true;
                }
            });
        }
        
        return ret;
    }

    /**
     * Lists the tide list in the {@link ConfigFetch#getConfFolder()}/tides folder.
     * @return
     */
    public static String[] getTidesFileAsStringList() {
        File[] fileList = getTidesFileList();

        String[] ret = new String[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            ret[i] = fileList[i].getName();
        }
        return ret;
    }

    /**
     * See {@link #showTidesSourceChooserGuiPopup(Component, String, Date, Date)}
     * @param parent
     * @return
     */
    public static String showTidesSourceChooserGuiPopup(Component parent) {
        return showTidesSourceChooserGuiPopup(parent, null, null, null);
    }
    
    /**
     * See {@link #showTidesSourceChooserGuiPopup(Component, String, Date, Date)}
     * @param parent
     * @param currentSource
     * @return
     */
    public static String showTidesSourceChooserGuiPopup(Component parent, String currentSource) {
        return showTidesSourceChooserGuiPopup(parent, currentSource, null, null);
    }
    
    /**
     * This will popup a dialog for the user to choose the tides source. This will not change any defaults.
     * 
     * @param parent The parent component for the created windows.
     * @param currentSource The current source of tides (This should match the file name, no path, of a tide file in
     *            {@link TidePredictionFactory#BASE_TIDE_FOLDER_PATH}). This can be null.
     * @param startDate The start date for the tide. This can be null (also imposes null for the end date). This will be
     *            use to try to update the tides data.
     * @param endDate The end date for the tide. This can be null (also imposes null for the satrt date). This will be
     *            use to try to update the tides data.
     * @return The string info for the tides source file name (from {@link TidePredictionFactory#BASE_TIDE_FOLDER_PATH})).
     */
    public static String showTidesSourceChooserGuiPopup(Component parent, String currentSource, Date startDate,
            Date endDate) {
        
        if (startDate == null || endDate == null) {
            startDate = null;
            endDate = null;
        }
        
        // Choosing tide sources options
        String[] lstStringArray = TidePredictionFactory.getTidesFileAsStringList();
        Arrays.sort(lstStringArray);
        List<String> lst = Lists.asList(NO_TIDE_STR, OTHER_TIDE_STR, lstStringArray);
        String ret = (String) JOptionPane.showInputDialog(parent, I18n.text("Choose a tides source"), 
                I18n.text("Tides"), JOptionPane.QUESTION_MESSAGE, null, 
                lst.toArray(), currentSource);
        
        if (ret == null || NO_TIDE_STR.equals(ret))
            return null;

        // If other let us open Web options
        if (OTHER_TIDE_STR.equals(ret)) {
            String harbor = TidePredictionFactory.fetchData(parent, null, startDate, endDate, true);
            if (harbor == null || harbor.isEmpty())
                return null;
            else
                ret = harbor + "." + TidePredictionFactory.defaultTideFormat;
        }
        return ret;
    }
    
    /**
     * Visual helper to get tide data.
     * @param parent The parent for the {@link JProgressBar} and {@link JOptionPane} shown.
     * @return
     */
    public static String fetchData(Component parent) {
        return fetchData(parent, null, null, null, true);
    }
    
    /**
     * Visual helper to get tide data.
     * If start or end dates are null, they will be asked or.
     * If show progress is false no {@link JProgressBar} will be shown.
     * @param parent The parent for the {@link JProgressBar} and {@link JOptionPane} shown.
     * @param harbor The harbor to fetch or null.
     * @param start The start date or null.
     * @param end The end date or null.
     * @param showProgress If a {@link JProgressBar} is shown or not.
     * @return
     */
    public static String fetchData(Component parent, String harbor, Date start, Date end, boolean showProgress) {
        Vector<String> harbors = new Vector<>();
        for (TideDataFetcher.Harbor h : TideDataFetcher.Harbor.values()) {
            harbors.add(h.toString());
        }

        if (harbor == null) {
            harbor = (String) JOptionPane.showInputDialog(parent,
                I18n.text("Please select harbor"), I18n.text("Fetch data"),
                JOptionPane.QUESTION_MESSAGE, null, harbors.toArray(new String[0]), I18n.text(harbors.get(0)));
        }

        if (harbor == null)
            return null;
        
        ProgressMonitor progress = showProgress
                ? new ProgressMonitor(parent, I18n.textf("Fetching tides for %harbor", harbor), "Starting", 0, 100)
                : null;

        while (start == null) {
            String startStr = JOptionPane.showInputDialog(parent, I18n.text("Days to fetch in the past"), 30);
            try {
                if (startStr == null)
                    return null;
                long days = Integer.parseInt(startStr);
                if (days < 0)
                    continue;
                start = new Date(System.currentTimeMillis() - days * 24l * 3600l * 1000l);
                System.out.println(start);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        while (end == null) {
            String endStr = JOptionPane.showInputDialog(parent, I18n.text("Days to fetch in the future"), 30);
            try {
                if (endStr == null)
                    return null;
                long days = Integer.parseInt(endStr);
                if (days < 0)
                    continue;
                end = new Date(System.currentTimeMillis() + days * 24l * 3600l * 1000l);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return fetchData(harbor, start, end, progress);
    }

    /**
     * @param harbor The harbor to fetch or null.
     * @param start The start date or null.
     * @param end The end date or null.
     * @param progress If a {@link JProgressBar} provided will be used otherwise null for not to show.
     * @return
     */
    public static String fetchData(String harbor, Date start, Date end, ProgressMonitor progress) {
        CachedData data;
        String path = ConfigFetch.getConfFolder() + "/tides/" + harbor;

        if (progress != null) {
            progress.setMillisToDecideToPopup(0);
            progress.setMillisToPopup(0);
        }
        
        String format = defaultTideFormat;
        switch (format) {
            case "txt":
                path += ".txt";
                data = new CachedData(new File(path));
            case "tid":
            default:
                path += ".tid";
                data = new TidCachedData(new File(path)); 
                break;
        }

        Date current = new Date(start.getTime());
        double delta = end.getTime() - start.getTime();

        while (current.getTime() < end.getTime()) {
            if (progress != null && progress.isCanceled())
                return harbor;
            double done = current.getTime() - start.getTime();
            try {
                Date d = data.fetchData(harbor, current);
                if (d != null)
                    current = new Date(current.getTime() + 1000 * 3600 * 24 * 5);                
            }
            catch (Exception e) {
                e.printStackTrace();
                if (progress != null) {
                    progress.setNote(I18n.textf("Error: %error", e.getMessage()));
                    try { Thread.sleep(3000); } catch (InterruptedException e1) { }
                }
                break;
            }
            System.out.println(current);
            if (progress != null) {
                progress.setProgress((int) (done * 100.0 / delta));
                progress.setNote(current.toString());
            }
        }
        try {
            if (progress != null)
                progress.setNote(I18n.text("Storing data to disk"));
            data.saveFile(harbor, data.getFileToSave(harbor));
            if (progress != null) {
                progress.setProgress(100);
                progress.close();                
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return harbor;
    }

    public static void main(String[] args) {
        for (int i = -10; i < 10; i++) {
            Date d = new Date(System.currentTimeMillis() + 1000 * 3600 * i);
            System.out.println(d + ": " + TidePredictionFactory.getTideLevel(d));
        }
    }
}
