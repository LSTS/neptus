/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 2008/05/17
 */
package pt.lsts.neptus.gui.swing;

import java.awt.Image;
import java.io.File;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.FileChooserUI;

import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
        // return Boolean.valueOf(true);
        // }
        return null;
    }

    @Override
    public Icon getIcon(File f) {
        if (isNeptusFile(f)) {
            String fex = FileUtil.getFileExtension(f);
            if (fex != null)
                fex = fex.toLowerCase();
            
            if ("gz".equalsIgnoreCase(fex)) {
                fex = FileUtil.getFileExtension(f.getName().substring(0, f.getName().length() - 3)) + "." + fex;
            }
            if ("bz2".equalsIgnoreCase(fex)) {
                fex = FileUtil.getFileExtension(f.getName().substring(0, f.getName().length() - 4)) + "." + fex;
            }
            
            Image ic = iconSet.get(fex);
            if (ic != null)
                return new ImageIcon(ic);
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
