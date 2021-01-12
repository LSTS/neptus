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
 * Jun 16, 2005
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 */
public class MapFileChooser extends JFileChooser {
    
	private static final long serialVersionUID = -8791840446640115089L;

    public static File showOpenMapDialog(File basedir) {
        return showOpenDialog(null, I18n.text("Open Map"), basedir);
    }

    public static File showOpenMapDialog(Component parent, File basedir) {
        return showOpenDialog(parent , I18n.text("Open Map"), basedir);
    }

    public static File showOpenMapDialog(Component parent) {
        return showOpenDialog(parent , I18n.text("Open Map"), null);
    }

    public static File showOpenMapDialog() {
        return showOpenDialog(null, I18n.text("Open Map"), null);
    }

    /**
     * @return
     */
    public static File showSaveMapDialog(File basedir) {
        return showOpenDialog(null, I18n.text("Save Map"), basedir);
    }

    public static File showSaveMapDialog() {
        return showOpenDialog(null, I18n.text("Save Map"), null);
    }
 
    public static File showSaveMapDialog(Component parent, File basedir) {
        return showOpenDialog(parent, I18n.text("Save Map"), basedir);
    }

    public static File showSaveMapDialog(Component parent) {
        return showOpenDialog(parent, I18n.text("Save Map"), null);
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
			fx = new File(ConfigFetch.getMapsFolder());
			if (!fx.exists()) {
				fx = new File(ConfigFetch.resolvePath("."));
				if (!fx.exists()) {
					fx = new File(".");
				}
			}
		}

		JFileChooser jfc = GuiUtils.getFileChooser(fx, I18n.text("Map files"), FileUtil.FILE_TYPE_MAP);
		jfc.setAccessory(new MapPreview(jfc));
		
        int result = jfc.showDialog((parent == null) ? new JFrame() : parent, title);
		if(result == JFileChooser.CANCEL_OPTION)
			return null;
		return jfc.getSelectedFile();
	}
	
	public static void main(String[] args) {
        ConfigFetch.initialize();
		File f = MapFileChooser.showOpenMapDialog();
		System.out.println(f);
	}
}

class MapPreview extends JPanel implements PropertyChangeListener {
	private static final long serialVersionUID = 2591033311036253447L;
	ImageIcon thumbnail = null;
	File file = null;
	StateRenderer2D r2d = new StateRenderer2D();
	JLabel id = new JLabel(" ");
	public MapPreview(JFileChooser fc) {
		r2d.setLegendShown(false);
		r2d.setVisible(false);
		setPreferredSize(new Dimension(120, 120));
		setLayout(new BorderLayout());
		add(id, BorderLayout.NORTH);
		add(r2d, BorderLayout.CENTER);
		fc.addPropertyChangeListener(this);
	}
	
	public void loadFile() {
		if (file == null) {
			return;
		}
		try {
			MapMission mm = new MapMission();
			mm.loadMap(file.getAbsolutePath());
			if (mm != null) {
				r2d.setMap(mm.getMap());
				r2d.showAllMap();
				r2d.setVisible(true);
				id.setText(mm.getId());
			}			
		}
		catch (Exception e) {
			r2d.setVisible(false);
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
