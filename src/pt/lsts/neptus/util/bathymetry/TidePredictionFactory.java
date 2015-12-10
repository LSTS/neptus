/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Dec 4, 2013
 */
package pt.lsts.neptus.util.bathymetry;

import java.awt.Component;
import java.io.File;
import java.util.Date;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 * @author pdias
 */
public class TidePredictionFactory {

    public static String[] logFolderTidesFileOptions = { "Tides.tid", "tides.tid", "Tides.txt", "tides.txt" };
    
    public static String defaultTideFormat = "txt";
    
    public static TidePredictionFinder create(IMraLogGroup source) {
        File dir = source.getDir();
        dir = new File(dir, "mra");
        Date date = new Date((long)(source.getLsfIndex().getStartTime())* 1000);
        return createWorker(dir, date, logFolderTidesFileOptions);
    }
    
    public static TidePredictionFinder create(LsfIndex source) {
        File dir = source.getLsfFile().getParentFile();
        dir = new File(dir, "mra");
        Date date = new Date((long)(source.getStartTime())* 1000);
        return createWorker(dir, date, logFolderTidesFileOptions);
    }

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

    private static TidePredictionFinder createWorker(File fx, Date date) {
        TidePredictionFinder finder = null;
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
        
        if (finder == null) {
            CachedData data = new CachedData();
            if (data.contains(date))
                finder = data;
            else
                finder = null;
        }

        return finder;
    }
    
    public static String fetchData(Component parent) {
        Vector<String> harbors = new Vector<>();
        for (TideDataFetcher.Harbor h : TideDataFetcher.Harbor.values()) {
            harbors.add(h.toString());
        }

        String harbor = (String) JOptionPane.showInputDialog(parent,
                I18n.text("Please select harbor"), I18n.text("Fetch data"),
                JOptionPane.QUESTION_MESSAGE, null, harbors.toArray(new String[0]), I18n.text(harbors.get(0)));

        if (harbor == null)
            return null;
        
        Date start = null, end = null;
        ProgressMonitor progress = new ProgressMonitor(parent, I18n.text("Fetching tides"), "Starting", 0, 100);

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

//        int opt = JOptionPane.showOptionDialog(parent, I18n.text("Choose Format"), I18n.text("Tides"), 
//                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] { "txt", "tid" }, "txt");
//        int opt = 0;
        CachedData data;
        String path = ConfigFetch.getConfFolder() + "/tides/" + harbor;
//        switch (opt) {
//            case 1:
//                path += ".tid";
//                data = new TidCachedData(new File(path)); 
//                break;
//            default:
//                path += ".txt";
//                data = new CachedData(new File(path));
//                break;
//        }
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
            if (progress.isCanceled())
                return harbor;
            double done = current.getTime()-start.getTime();
            try {
                Date d = data.fetchData(harbor, current);
                if (d != null)
                    current = new Date(current.getTime() + 1000 * 3600 * 24 * 5);                
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(current);
            progress.setProgress((int)(done*100.0/delta));
            progress.setNote(current.toString());
        }
        try {
            progress.setNote("Storing data to disk");
            data.saveFile(harbor, data.getFileToSave(harbor));
            progress.setProgress(100);
            progress.close();                
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return harbor;
    }

    public static void main(String[] args) {
        for (int i = -10; i < 10; i++) {
            Date d = new Date(System.currentTimeMillis() + 1000 * 3600 * i);
            System.out.println(d+": "+TidePrediction.getTideLevel(d));
        }
    }
}
