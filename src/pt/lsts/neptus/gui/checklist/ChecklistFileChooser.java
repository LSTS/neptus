/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Created in 29/Sep/2005
 */
package pt.lsts.neptus.gui.checklist;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedHashMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 * 
 */
public class ChecklistFileChooser extends JFileChooser {

    private static final long serialVersionUID = -1779865653037996291L;

    /**
     * @return
     */
    public static File showOpenDialog(File basedir) {
        return showOpenDialog(null, I18n.text("Open Checklist"), basedir);
    }

    public static File showOpenDialog(Component parent, File basedir) {
        return showOpenDialog(parent, I18n.text("Open Checklist"), basedir);
    }

    public static File showOpenDialog() {
        return showOpenDialog(null, I18n.text("Open Checklist"), null);
    }

    /**
     * @return
     */
    public static File showSaveDialog(File basedir) {
        return showOpenDialog(null, I18n.text("Save Checklist"), basedir);
    }

    public static File showSaveDialog() {
        return showOpenDialog(null, I18n.text("Save Checklist"), null);
    }

    public static File showSaveDialog(Component parent, File basedir) {
        return showOpenDialog(parent, I18n.text("Save Checklist"), basedir);
    }

    /**
     * Shows
     */
    private static File showOpenDialog(Component parent, String title, File basedir) {
        File fx;
        if (basedir != null && basedir.exists()) {
            fx = basedir;
        }
        else {
            fx = new File(ConfigFetch.getConfigFile());
            fx = new File(fx.getParentFile(), "checklists");
            if (!fx.exists()) {
                fx = new File(ConfigFetch.resolvePath("."));
                if (!fx.exists()) {
                    fx = new File(".");
                }
            }
        }

        JFileChooser jfc = GuiUtils.getFileChooser(fx, I18n.text("Checklist files"), FileUtil.FILE_TYPE_CHECKLIST,
                FileUtil.FILE_TYPE_XML);
        jfc.setAccessory(new ChecklistPreview(jfc));

        int result = jfc.showDialog((parent == null) ? new JFrame() : parent, title);
        if (result == JFileChooser.CANCEL_OPTION)
            return null;
        return jfc.getSelectedFile();
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        File f = ChecklistFileChooser.showOpenDialog();
        NeptusLog.pub().info("<###> "+f);
    }
}

class ChecklistPreview extends JPanel implements PropertyChangeListener {
    private static final long serialVersionUID = -1591236595011124495L;

    private File file = null;
    private JLabel id = new JLabel(" ");

    public ChecklistPreview(JFileChooser fc) {
        setPreferredSize(new Dimension(120, 120));
        setLayout(new BorderLayout());
        add(id, BorderLayout.NORTH);
        fc.addPropertyChangeListener(this);
    }

    private LinkedHashMap<String, String> loadHeader(File f) {
        final LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();

        final SAXReader reader = new SAXReader();

        reader.addHandler("/checklist", new ElementHandler() {
            public void onEnd(ElementPath arg0) {
            }

            public void onStart(ElementPath arg0) {
                Element elem = arg0.getElement(0);
                String nameStr = elem.attributeValue("name");
                header.put("name", nameStr);
                reader.removeHandler("/checklist");
            }
        });

        reader.addHandler("/checklist/description", new ElementHandler() {
            public void onEnd(ElementPath arg0) {
                header.put("description", arg0.getCurrent().getText());
                reader.removeHandler("/checklist/description");
            }

            public void onStart(ElementPath arg0) {
            }
        });

        try {
            reader.read(f);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }

        return header;
    }

    public void loadFile() {
        if (file == null) {
            id.setText("");
            return;
        }
        try {
            if (file.exists()) {
                LinkedHashMap<String, String> header = loadHeader(file);
                if (header.values().size() != 2) {
                    id.setText("<html><font color='red'>" + I18n.text("Not a valid checklist") + "</font></html>");
                }
                else {
                    id.setText("<html><font color='blue'>" + I18n.text("Name") + ": </font>" + header.get("name")
                            + "<hr><font color='blue'>" + I18n.text("Description") + ":</font><br>"
                            + header.get("description") + "</html>");
                }

                /*
                 * ChecklistType cl = new ChecklistType(file.getAbsolutePath());
                 * // cl.load(file.getAbsolutePath()); if
                 * (!cl.getName().equalsIgnoreCase("")) {
                 * id.setText("<html> <b>Checklist name:</b><br>" + " <i>" +
                 * cl.getName() + "</i></html>"); } else { id.setText(" "); }
                 */
            }
            else {
                id.setText(" ");
            }
        }
        catch (Exception e) {
            id.setText("<html><font color='red'>" + I18n.text("Not a valid checklist") + "</font></html>");
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
