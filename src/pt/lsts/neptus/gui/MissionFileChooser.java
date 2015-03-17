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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * Jun 16, 2005
 */
package pt.lsts.neptus.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ZipUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 */
public class MissionFileChooser extends JFileChooser {

    private static final long serialVersionUID = -749492337802300793L;

    /**
     * Returns the file extension for the given file (null if no extension)
     * 
     * @param f The file from where to get the extension
     * @return The file extension of the given file
     */
    private static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * Shows
     */
    public static File showOpenMissionDialog(String[] possibleExtensions) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
        jfc.setAccessory(new MissionPreview(jfc));
        jfc.setFileView(new NeptusFileView());
        final String[] exts = possibleExtensions;

        jfc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }

                String extension = getExtension(f);
                if (extension != null) {

                    for (String ext : exts) {
                        if (extension.equalsIgnoreCase(ext))
                            return true;
                    }
                }

                return false;
            }

            public String getDescription() {
                if (exts.length == 0)
                    return I18n.text("Mission Files");

                String desc = I18n.text("Mission files") + " ('" + exts[0] + "'";

                for (int i = 1; i < exts.length; i++)
                    desc += ", '" + exts[i] + "'";

                desc += ")";

                return desc;
            }
        });

        int result = jfc.showDialog(ConfigFetch.getSuperParentFrame(), I18n.text("Open Mission"));
        if (result == JFileChooser.CANCEL_OPTION)
            return null;
        return jfc.getSelectedFile();

    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        File f = MissionFileChooser.showOpenMissionDialog(new String[] { "nmis", "xml" });
        NeptusLog.pub().info("<###> "+f);
    }
}

class MissionPreview extends JLabel implements PropertyChangeListener {
    private static final long serialVersionUID = 288244642142172020L;
    ImageIcon thumbnail = null;
    File file = null;

    public MissionPreview(JFileChooser fc) {
        // ConfigFetch.initialize();
        setPreferredSize(new Dimension(120, 50));
        setFont(new Font("Lucida Sans", Font.PLAIN, 10));
        fc.addPropertyChangeListener(this);
    }

    private LinkedHashMap<String, String> loadHeader(File f) {
        final LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();

        final SAXReader reader = new SAXReader();

        reader.addHandler("/mission-def/header/name", new ElementHandler() {
            public void onEnd(ElementPath arg0) {
                header.put("name", arg0.getCurrent().getText());
                reader.removeHandler("/mission-def/header/name");
            }

            public void onStart(ElementPath arg0) {
            }
        });

        reader.addHandler("/mission-def/header/description", new ElementHandler() {
            public void onEnd(ElementPath arg0) {
                header.put("description", arg0.getCurrent().getText());
                reader.removeHandler("/mission-def/header/description");
            }

            public void onStart(ElementPath arg0) {
            }
        });

        if (FileUtil.getFileExtension(f).equalsIgnoreCase("nmisz")
                || FileUtil.getFileExtension(f).equalsIgnoreCase("zip")) {
            try {
                InputStream is = ZipUtils.getMissionZipedAsInputSteam(f.getAbsolutePath());
                if (is != null)
                    reader.read(is);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        else {
            try {
                reader.read(f);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }

        return header;
    }

    public void loadFile() {
        if (file == null) {
            setText("");
            return;
        }
        try {
            LinkedHashMap<String, String> header = loadHeader(file);
            if (header.values().size() != 2) {
                setText("<html><font color='red'>" + I18n.text("Not a valid mission") + "</font></html>");
            }
            else {
                setText("<html><font color='blue'>Name: </font>" + header.get("name") + "<hr><font color='blue'>"
                        + I18n.text("Description") + ":</font><br>" + header.get("description") + "</html>");
            }
        }
        catch (Exception e) {
            setText("<html><font color='red'>" + I18n.text("Not a valid mission") + "</font></html>");
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            file = (File) e.getNewValue();
            if (isShowing()) {
                loadFile();
                repaint();
            }
        }
    }
}
