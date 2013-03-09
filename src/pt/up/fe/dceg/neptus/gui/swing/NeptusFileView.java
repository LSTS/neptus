/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2008/05/17
 */
package pt.up.fe.dceg.neptus.gui.swing;

import java.awt.Image;
import java.io.File;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.FileChooserUI;

import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 * 
 */
public class NeptusFileView extends FileView {

    public static Hashtable<String, Image> iconSet = new Hashtable<String, Image>();

    static {
        JFileChooser chooser = new JFileChooser();
        FileChooserUI fcui = (FileChooserUI) UIManager.getUI(chooser);
        fcui.installUI(chooser);
        FileView def = fcui.getFileView(chooser);

        // get the standard icon for a folder
        File tmp = new File(".");
        Icon folder = def.getIcon(tmp);
        int w = folder.getIconWidth();
        int h = folder.getIconHeight();

        if (w < 20 || h < 20) {
            iconSet.put(FileUtil.FILE_TYPE_MISSION, ImageUtils.getScaledImage("images/files-icons/nmis16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_MISSION_COMPRESSED,
                    ImageUtils.getScaledImage("images/files-icons/nmisz16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_MAP, ImageUtils.getScaledImage("images/files-icons/nmap16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_CONSOLE, ImageUtils.getScaledImage("images/files-icons/ncon16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_CONFIG, ImageUtils.getScaledImage("images/files-icons/ncfg16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_VEHICLE, ImageUtils.getScaledImage("images/files-icons/nvcl16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_CHECKLIST, ImageUtils.getScaledImage("images/files-icons/nchk16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_WSN, ImageUtils.getScaledImage("images/files-icons/nwsn16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_INI, ImageUtils.getScaledImage("images/files-icons/ini16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_RMF, ImageUtils.getScaledImage("images/files-icons/rmf16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_LSF, ImageUtils.getScaledImage("images/files-icons/lsf16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_LSF_COMPRESSED,
                    ImageUtils.getScaledImage("images/files-icons/lsfgz16.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2,
                    ImageUtils.getScaledImage("images/files-icons/lsfgz16.png", w, h));
        }
        else {
            iconSet.put(FileUtil.FILE_TYPE_MISSION, ImageUtils.getScaledImage("images/files-icons/nmis.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_MISSION_COMPRESSED,
                    ImageUtils.getScaledImage("images/files-icons/nmisz.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_MAP, ImageUtils.getScaledImage("images/files-icons/nmap.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_CONSOLE, ImageUtils.getScaledImage("images/files-icons/ncon.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_CONFIG, ImageUtils.getScaledImage("images/files-icons/ncfg.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_VEHICLE, ImageUtils.getScaledImage("images/files-icons/nvcl.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_CHECKLIST, ImageUtils.getScaledImage("images/files-icons/nchk.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_WSN, ImageUtils.getScaledImage("images/files-icons/nwsn.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_INI, ImageUtils.getScaledImage("images/files-icons/ini.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_RMF, ImageUtils.getScaledImage("images/files-icons/rmf.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_LSF, ImageUtils.getScaledImage("images/files-icons/lsf.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_LSF_COMPRESSED,
                    ImageUtils.getScaledImage("images/files-icons/lsfgz.png", w, h));
            iconSet.put(FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2,
                    ImageUtils.getScaledImage("images/files-icons/lsfgz.png", w, h));
        }
    }

    /**
	 * 
	 */
    public NeptusFileView() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param f
     * @return
     */
    protected boolean isNeptusFile(File f) {
        String fext = FileUtil.getFileExtension(f);
        if ("gz".equalsIgnoreCase(fext)) {
            fext = FileUtil.getFileExtension(f.getName().substring(0, f.getName().length() - 3)) + "." + fext;
        }
        if ("bz2".equalsIgnoreCase(fext)) {
            fext = FileUtil.getFileExtension(f.getName().substring(0, f.getName().length() - 4)) + "." + fext;
        }
        for (String ext : iconSet.keySet()) {
            if (fext.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean isTraversable(File f) {
        // if (isDirLink(f)) {
        // return new Boolean(true);
        // }
        return null;
    }

    @Override
    public Icon getIcon(File f) {
        if (isNeptusFile(f)) {
            String fex = FileUtil.getFileExtension(f);
            if ("gz".equalsIgnoreCase(fex)) {
                fex = FileUtil.getFileExtension(f.getName().substring(0, f.getName().length() - 3)) + "." + fex;
            }
            if ("bz2".equalsIgnoreCase(fex)) {
                fex = FileUtil.getFileExtension(f.getName().substring(0, f.getName().length() - 4)) + "." + fex;
            }
            return new ImageIcon(iconSet.get(fex));
        }
        return super.getIcon(f);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GuiUtils.setSystemLookAndFeel();
        JFileChooser chooser = new JFileChooser(new File(ConfigFetch.getConfigFile()));
        chooser.setFileView(new NeptusFileView());
        chooser.showOpenDialog(null);
    }
}
