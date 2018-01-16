/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 27/04/2017
 */
package pt.lsts.neptus.plugins.atlas.elektronik.exporter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.lsts.neptus.i18n.I18n;

/**
 * @author pdias
 *
 */
public class SeaCatMK1FilePreview extends JPanel implements PropertyChangeListener {

    private static final long serialVersionUID = -1458418071853907575L;

    private static final String ATLAS_AUV_MISSION_PLAN = "ATLAS AUV Mission Plan";
    
    private File file = null;
    private JLabel id = new JLabel(" ");

    public SeaCatMK1FilePreview(JFileChooser fc) {
        setPreferredSize(new Dimension(120, 120));
        setLayout(new BorderLayout());
        add(id, BorderLayout.NORTH);
        fc.addPropertyChangeListener(this);
    }

    public void loadFile() {
        if (file == null) {
            id.setText("");
            return;
        }
        try {
            if (file.exists()) {
                LinkedHashMap<String, String> header = loadHeader(file);
                if (header.values().size() < 3) {
                    id.setText("<html><font color='red'>" + I18n.text("Not a valid file") + "</font></html>");
                }
                else if (header.get("Type") == ATLAS_AUV_MISSION_PLAN){
                    StringBuilder sb = new StringBuilder();
                    sb.append("<html>");
                    header.keySet().forEach(k -> {
                        sb.append("<b>");
                        sb.append(k);
                        sb.append(": </b>");
                        sb.append(header.get(k));
                        sb.append("<br>");
                    });
                    sb.append("</html>");
                    id.setText(sb.toString());
                    return;
                }
            }
            
            id.setText(" ");
        }
        catch (Exception e) {
            id.setText("<html><font color='red'>" + I18n.text("Not a valid file") + "</font></html>");
        }
    }

    /**
     * @param file2
     * @return
     */
    private LinkedHashMap<String, String> loadHeader(File file) {
        LinkedHashMap<String, String> props = new LinkedHashMap<>();
        FileChannel channel = null;
        try (RandomAccessFile raFile = new RandomAccessFile(file, "r")) {
            channel = raFile.getChannel();
            MappedByteBuffer buf = channel.map(MapMode.READ_ONLY, 0, 300);
            String data = Charset.forName("UTF-8").decode(buf).toString();
            String[] tk1 = data.replaceAll("%", "").trim().split("\n");
            for (String st1 : tk1) {
                st1 = st1.trim();
                if (st1.isEmpty())
                    continue;
                if (st1.contains(ATLAS_AUV_MISSION_PLAN))
                    props.put("Type", ATLAS_AUV_MISSION_PLAN);
                else {
                    String[] tk2 = st1.split(":(?!\\d)");
                    if (tk2.length != 2)
                        continue;
                    
                    props.put(tk2[0].trim(), tk2[1].trim());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (channel != null) {
                try {
                    channel.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return props;
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
