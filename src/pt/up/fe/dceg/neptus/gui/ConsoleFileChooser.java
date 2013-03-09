/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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

        JFileChooser jfc = new JFileChooser();

        File fx;
        if (basedir != null && basedir.exists()) {
            fx = basedir;
        }
        else {
            fx = new File(ConfigFetch.getConfigFile());
            fx = new File(fx.getParentFile(), "conf/consoles");
            if (!fx.exists()) {
                fx = new File(ConfigFetch.resolvePath("."));
                if (!fx.exists()) {
                    fx = new File(".");
                }
            }
        }
        jfc.setCurrentDirectory(fx);
        // jfc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));

        jfc.setFileView(new NeptusFileView());
        jfc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }

                String extension = FileUtil.getFileExtension(f).toLowerCase();
                if (extension != null) {
                    if (extension.equals("xml") || extension.equals("ncn") || extension.equals("ncon")) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }

                return false;
            }

            public String getDescription() {
                return I18n.textf("Console files (%extensions)", "'ncon', 'xml'");
            }
        });

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
        System.out.println(f);
    }
}
