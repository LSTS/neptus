/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

public class ConsoleFileChooser extends JFileChooser {

    private static final long serialVersionUID = 1L;

    // /**
    // * Returns the file extension for the given file (null if no extension)
    // * @param f The file from where to get the extension
    // * @return The file extension of the given file
    // */
    // private static String getExtension(File f) {
    // String ext = null;
    // String s = f.getName();
    // int i = s.lastIndexOf('.');
    //
    // if (i > 0 && i < s.length() - 1) {
    // ext = s.substring(i+1).toLowerCase();
    // }
    // return ext;
    // }

    public static File showOpenDialog(File basedir) {
        return showOpenDialog(null, I18n.text("Open Console"), basedir);
    }

    public static File showOpenDialog(Component parent, File basedir) {
        return showOpenDialog(parent, I18n.text("Open Console"), basedir);
    }

    public static File showOpenDialog() {
        return showOpenDialog(null, I18n.text("Open Console"), null);
    }

    /**
     * Shows
     */
    public static File showOpenConsoleDialog(Component parent) {
        return showOpenDialog(parent, I18n.text("Open Console"), null);
    }

    public static File showSaveConsoleDialog(Component parent) {
        return showOpenDialog(parent, I18n.text("Save Console"), null);
    }

    /**
     * @return
     */
    public static File showSaveDialog(File basedir) {
        return showOpenDialog(null, I18n.text("Save Console"), basedir);
    }

    public static File showSaveDialog(Component parent, File basedir) {
        return showOpenDialog(parent, I18n.text("Save Console"), basedir);
    }

    private static File showOpenDialog(Component parent, String title, File basedir) {

        File fx;
        if (basedir != null && basedir.exists()) {
            fx = basedir;
        }
        else {
            fx = new File(ConfigFetch.getConsolesFolder());
            if (!fx.exists()) {
                fx = new File(ConfigFetch.resolvePath("."));
                if (!fx.exists()) {
                    fx = new File(".");
                }
            }
        }

        JFileChooser jfc = GuiUtils.getFileChooser(fx, I18n.text("Console files"), "ncon");

        int result = jfc.showDialog(parent, title);
        if (result == JFileChooser.CANCEL_OPTION)
            return null;
        return jfc.getSelectedFile();
    }

    public static File showOpenConsoleDialog() {
        return showOpenConsoleDialog(new JFrame());
    }

    public static File showSaveConsoleDialog() {
        return showOpenConsoleDialog(new JFrame());
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        File f = ConsoleFileChooser.showOpenConsoleDialog();
        NeptusLog.pub().info("<###> "+f);
    }
}
