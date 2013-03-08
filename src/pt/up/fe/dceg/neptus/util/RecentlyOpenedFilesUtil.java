/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 17/Mai/2005
 * $Id:: RecentlyOpenedFilesUtil.java 9630 2013-01-02 15:53:51Z zepinto   $:
 */
package pt.up.fe.dceg.neptus.util;

import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 * 
 */
public class RecentlyOpenedFilesUtil {

    /**
     * To use to construct JMenuItens for recently opened files.
     * 
     * @param recentlyOpenFilesMenuItem
     * @param filesOpenedLHM<JMenuItem,File>
     */
    public static void constructRecentlyFilesMenuItems(JMenu recentlyOpenFilesMenuItem,
            LinkedHashMap<?, ?> filesOpenedLHM) {
        recentlyOpenFilesMenuItem.removeAll();

        if (filesOpenedLHM.size() == 0) {
            recentlyOpenFilesMenuItem.setEnabled(false);
        }
        else {
            recentlyOpenFilesMenuItem.setEnabled(true);
            Iterator<?> it = filesOpenedLHM.keySet().iterator();
           // int i = 1;
            while (it.hasNext()) {
                JMenuItem nMenuItem = (JMenuItem) it.next();
                // TEST
                File fx = (File) filesOpenedLHM.get(nMenuItem);
                //String ni = Integer.toString(i).length() == 1 ? " " + i : "" + i;
                nMenuItem.setText(fx.getParentFile().getName() + System.getProperty("file.separator")
                        + fx.getName());
                recentlyOpenFilesMenuItem.add(nMenuItem);
                //i++;
            }

        }
    }

    /**
     * To use to update JMenuItens for recently opened files
     * 
     * @param fx
     * @param filesOpened
     * @param actionListener If none set to null.
     * @return
     */
    public static boolean updateFilesOpenedMenuItems(File fx, LinkedHashMap<JMenuItem, File> filesOpened,
            ActionListener actionListener) {
        if (filesOpened.containsValue(fx)) {
            for (JMenuItem key : filesOpened.keySet()) {
                if (filesOpened.get(key).equals(fx)) {
                    filesOpened.remove(key);
                    break;
                }
            }
        }

        if (!filesOpened.containsValue(fx)) {
            JMenuItem nMenuItem = new JMenuItem() {
                private static final long serialVersionUID = 1L;

                /*
                 * (non-Javadoc)
                 * 
                 * @see javax.swing.AbstractButton#getText()
                 */
                public String getText() {
                    return super.getText();
                }
            };
            // nMenuItem.setText(fx.getAbsolutePath());
            nMenuItem.setText(fx.getName());
            nMenuItem.setToolTipText(fx.getAbsolutePath());
            if (actionListener != null)
                nMenuItem.addActionListener(actionListener);

            LinkedHashMap<JMenuItem, File> oldList = new LinkedHashMap<JMenuItem, File>();
            oldList.putAll(filesOpened);
            filesOpened.clear();

            filesOpened.put(nMenuItem, fx);
            filesOpened.putAll(oldList);
        }
        else {

        }
        while (filesOpened.size() > 10) {
            Object[] keys = filesOpened.keySet().toArray();
            filesOpened.remove(keys[filesOpened.size() - 1]);
        }
        return true;
    }

    public static void loadRecentlyOpenedFiles(String filePath, Method updateMethod, Object objectOfUpdateMethod) {
        String recentlyOpenedFiles = ConfigFetch.resolvePath(filePath);
        if (recentlyOpenedFiles == null) {
            // JOptionPane.showInternalMessageDialog(this, "Cannot Load")
            return;
        }

        if (!new File(recentlyOpenedFiles).exists())
            return;
        PropertiesLoader pLoader = new PropertiesLoader(recentlyOpenedFiles, PropertiesLoader.XML_PROPERTIES);
        String nFiles = pLoader.getProperty("n-files");
        int nf = 0;
        if (nFiles == null)
            nf = 0;
        else {
            try {
                nf = Integer.parseInt(nFiles);
                for (int i = nf - 1; i >= 0; i--) {
                    String file = pLoader.getProperty("file" + i);
                    File fx = new File(file);
                    if (fx.exists()) {
                        Object[] args = { fx };
                        try {
                            updateMethod.invoke(objectOfUpdateMethod, args);
                        }
                        catch (Exception e1) {
                            NeptusLog.pub().error("loadRecentlyOpenedFiles", e1);
                        }
                    }
                }
            }
            catch (NumberFormatException e) {
                nf = 0;
            }
        }
    }

    /**
     * @param filePath
     * @param hMap
     * @param header
     */
    public static void storeRecentlyOpenedFiles(String filePath, LinkedHashMap<?, ?> hMap, String header) {
        String recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(filePath);
        if (recentlyOpenedFiles == null) {
            JOptionPane.showInternalMessageDialog(null, "Cannot Load");
            return;
        }
        PropertiesLoader pLoader = new PropertiesLoader(recentlyOpenedFiles, PropertiesLoader.XML_PROPERTIES);
        pLoader.clear();
        int nf = hMap.size();
        pLoader.setProperty("n-files", Integer.toString(nf));
        Iterator<?> it = hMap.values().iterator();
        for (int i = 0; i < nf; i++) {
            File fx = (File) it.next();
            pLoader.setProperty("file" + i, fx.getAbsolutePath());
        }
        try {
            pLoader.storeToXML(header);
        }
        catch (Exception e) {
            NeptusLog.pub().error("storeRecentlyOpenedFiles", e);
        }
    }

}
