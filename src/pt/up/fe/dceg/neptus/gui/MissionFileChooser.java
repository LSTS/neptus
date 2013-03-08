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
 * Jun 16, 2005
 * $Id:: MissionFileChooser.java 9736 2013-01-18 15:29:09Z pdias          $:
 */
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.ZipUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
        System.out.println(f);
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
