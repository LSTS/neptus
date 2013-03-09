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
 */
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
		System.out.println(f);
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
