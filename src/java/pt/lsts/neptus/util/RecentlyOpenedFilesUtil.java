/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 17/Mai/2005
 */
package pt.lsts.neptus.util;

import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
                NeptusLog.pub().debug(e.getMessage());
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
