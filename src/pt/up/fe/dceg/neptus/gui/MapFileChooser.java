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
import javax.swing.filechooser.FileFilter;

import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.renderer2d.Renderer;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
		JFileChooser jfc = new JFileChooser();
		File fx;
		if (basedir != null && basedir.exists()) {
			fx = basedir;
		}
		else {
			fx = new File(ConfigFetch.getConfigFile());
			if (!fx.exists()) {
				fx = new File(ConfigFetch.resolvePath("."));
				if (!fx.exists()) {
					fx = new File(".");
				}
			}
		}
		jfc.setCurrentDirectory(fx);
		//jfc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
		jfc.setAccessory(new MapPreview(jfc));
		jfc.setFileView(new NeptusFileView());
		jfc.setFileFilter(new FileFilter() {
			
			public boolean accept(File f) {
				 if (f.isDirectory()) {
		            return true;
				 }

		        String extension = FileUtil.getFileExtension(f);
		        if (extension != null) {
		            if (extension.equals(FileUtil.FILE_TYPE_MAP) ||
		                extension.equals(FileUtil.FILE_TYPE_XML)) {
		                    return true;
		            }
		            else {
		                return false;
		            }
		    	}

		        return false;
			}
			
			public String getDescription() {
                return I18n.text("Map files") + " ('" + FileUtil.FILE_TYPE_MAP + "', '" + FileUtil.FILE_TYPE_XML + "')";
            }
		});
		
		int result = jfc.showDialog((parent == null)?new JFrame():parent, title);
		//int result = jfc.showDialog(new JFrame(), "Open Map");
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
		r2d.setViewMode(Renderer.NONE);
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
