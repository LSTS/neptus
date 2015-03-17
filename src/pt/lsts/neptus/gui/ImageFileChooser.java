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
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 */
@SuppressWarnings("serial")
public class ImageFileChooser extends JFileChooser {
    
	/**
	 * Returns the file extension for the given file (null if no extension)
	 * @param f The file from where to get the extension
	 * @return The file extension of the given file
	 */
	private static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
	
	/**
	 * Shows 
	 */
	public static File showOpenImageDialog() {
		JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
		jfc.setAccessory(new ImagePreview(jfc));
		jfc.setFileView(new NeptusFileView());
		jfc.setFileFilter(new FileFilter() {
			
			public boolean accept(File f) {
				 if (f.isDirectory()) {
		            return true;
				 }

		        String extension = getExtension(f);
		        if (extension != null) {
		            if (extension.equals("png") ||
		                extension.equals("jpg") ||
		                extension.equals("jpeg") ||
		                extension.equals("gif")) {
		                    return true;
		            } 
		            else {
		                return false;
		            }
		    	}

		        return false;
			}
			
			public String getDescription() {
				return I18n.text("Image files");
			}
		});
		
		jfc.showDialog(ConfigFetch.getSuperParentFrame(), I18n.text("Open Image"));
		return jfc.getSelectedFile();
	}
	
	public static void main(String[] args) {
		File f = ImageFileChooser.showOpenImageDialog();
		NeptusLog.pub().info("<###> "+f);
	}
}

// copy-paste de http://www.ictp.trieste.it/~manuals/programming/Java/tutorial/uiswing/components/filechooser.html
@SuppressWarnings("serial")
class ImagePreview extends JComponent implements PropertyChangeListener {
	private ImageIcon thumbnail = null;
	private File file = null;
	
	public ImagePreview(JFileChooser fc) {
		setPreferredSize(new Dimension(100, 50));
		fc.addPropertyChangeListener(this);
	}
	
	public void loadImage() {
		if (file == null) {
			return;
		}
		
		if (file.canRead()) {
			ImageIcon tmpIcon = new ImageIcon(file.getPath());
			
			if (tmpIcon.getIconWidth() > 90) {
				thumbnail = new ImageIcon(tmpIcon.getImage().
						getScaledInstance(90, -1, Image.SCALE_DEFAULT));
			} else {
				thumbnail = tmpIcon;
			}
		}
	}
	
	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
			file = (File) e.getNewValue();
			if (isShowing()) {
				loadImage();
				repaint();
			}
		}
	}
	
	public void paintComponent(Graphics g) {
		if (thumbnail == null) {
			loadImage();
		}
		if (thumbnail != null) {
			int x = getWidth()/2 - thumbnail.getIconWidth()/2;
			int y = getHeight()/2 - thumbnail.getIconHeight()/2;
			
			if (y < 0) {
				y = 0;
			}
			
			if (x < 5) {
				x = 5;
			}
			thumbnail.paintIcon(this, g, x, y);
		}
	}
}
