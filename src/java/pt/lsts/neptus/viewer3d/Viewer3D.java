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
 * Author: Rui Gonçalves
 * 200?/??/??
 */
package pt.lsts.neptus.viewer3d;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.LinkedHashMap;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.TransformGroup;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import com.mnstarfire.loaders3d.Inspector3DS;
import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.utils.scenegraph.io.SceneGraphFileReader;
import com.sun.j3d.utils.scenegraph.io.SceneGraphFileWriter;
import com.sun.j3d.utils.scenegraph.io.UnsupportedUniverseException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.WaitPanel;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.renderer3d.Camera3D;
import pt.lsts.neptus.renderer3d.Obj3D;
import pt.lsts.neptus.renderer3d.Renderer3D;
import pt.lsts.neptus.renderer3d.Util3D;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.RecentlyOpenedFilesUtil;
import pt.lsts.neptus.util.X3dParse;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * 
 * @author RJPG
 * 
 */

public class Viewer3D extends JPanel implements ActionListener {

	private static final long serialVersionUID = 8461400298712596459L;

	private JMenuBar menu = new JMenuBar();

	private Renderer3D render;

	private LinkedHashMap<JMenuItem, File> miscFilesOpened = new LinkedHashMap<JMenuItem, File>();
	private JMenu recentlyOpenFilesMenu = null;
	public final static String RECENTLY_OPENED_3D = "conf/3d_recent.xml";

	private File openedFile = null;

	JFrame frame = null;

	Obj3D lobj = null;

	// private JPanel mainPanel=new JPanel();

	Thread t;

	public Viewer3D() {
		init();
		loadRecentlyOpenedFiles();
        setVisible(true);
		// SceneGraphFileWriter
		// menu.setVisible(true);
		// this.repaint();
	}

	public Viewer3D(TransformGroup obj) {
		super();
		init();

		Obj3D obj3d = new Obj3D();
		obj3d.setModel3D(obj);
		/*
		 * for(;t.isAlive();) try { Thread.sleep(500); } catch
		 * (InterruptedException e) {
		 * 
		 * //e.printStackTrace(); }
		 */
		render.addObj3D(obj3d);
		doLayout();
		repaint();
		setVisible(true);
	}

	private void init() {
		final WaitPanel wait = new WaitPanel();
		wait.start();

		GuiUtils.setLookAndFeel();
		t = new Thread() {
			public void run() {
				createMenuBar();
				createRender();
				Runnable r = new Runnable() {
					public void run() {
						setLayout(new BorderLayout());
						add(menu, BorderLayout.NORTH);
						add(render, BorderLayout.CENTER);
						wait.stop();
					}
				};
				SwingUtilities.invokeLater(r);
			}
		};
		// t.start();

		createMenuBar();
		createRender();
		setLayout(new BorderLayout());

		add(menu, BorderLayout.NORTH);
		add(render, BorderLayout.CENTER);
		wait.stop();

	}

	public boolean disableMenuBar() {
		this.remove(menu);
		return true;
	}

	public void setVisible(boolean visible) {
		prepareFrame();
		frame.getContentPane().add(this);
		frame.setVisible(visible);
	}

	protected void prepareFrame() {
		if (frame == null) {
			frame = new JFrame("Neptus 3D Viewer");
			frame.setSize(800, 600);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent arg0) {
					// this.
					// video = null;
				}
			});

			frame.setTitle("Neptus 3D Viewer");
			//frame.setIconImage(GuiUtils.getImage("images/neptus-icon.png"));
			frame.setIconImages(ConfigFetch.getIconImagesForFrames());
			// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		}
	}

	/**
	 * @return Returns the frame.
	 */
	public JFrame getFrame() {
		if (frame == null)
			prepareFrame();

		return frame;
	}

	public void createMenuBar() {

		JMenu file = new JMenu();
		file.getPopupMenu().setLightWeightPopupEnabled(false);
		file.setText("File");

		JMenuItem open = new JMenuItem("Open", new ImageIcon(ImageUtils
				.getImage("images/menus/open.png")));
		open.setText("Open");
		open.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				openFile();

			}
		});

		// JMenuItem save = new JMenuItem("Save", new ImageIcon(GuiUtils
		// .getImage("images/menus/save.png")));
		// save.setText("Save");

		JMenuItem saveas = new JMenuItem("Save as", new ImageIcon(ImageUtils
				.getImage("images/menus/saveas.png")));
		saveas.setText("Save as");
		saveas.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				saveAsFile();

			}
		});

		JMenuItem quit = new JMenuItem("Quit", new ImageIcon(ImageUtils
				.getImage("images/menus/exit.png")));
		quit.setText("Quit");
		quit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {

				setVisible(false);
				if (frame != null)
					frame.dispose();
			}
		});

		file.add(getRecentlyOpenFilesMenu());
		file.add(open);
		// file.add(save);
		file.add(saveas);
		file.addSeparator();
		file.add(quit);

		menu.add(file);

	}

	public void createRender() {
		Camera3D cam = new Camera3D(Camera3D.USER);
		cam.setRho(60.0);
		Camera3D[] cams = new Camera3D[1];
		cams[0] = cam;
		render = new Renderer3D(cams, (short) 1, (short) 1);
		render.setMode(Renderer3D.VIEWER_MODE);
		render.grid(true, true);
		render.infoAxis(true);
		render.infoCam(true);
		render.setViewMode(Renderer3D.ROTATION);
		render.showAxis(true);
		// Obj3D obj = new Obj3D(Util3D.makeAxis(true, 10.));
		// obj.setModel3D(Util3D.makeAxis(true, 10.));
		// render.addObj3D(obj);

	}

	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

	public File showOpenImageDialog() {
		JFileChooser jfc = new JFileChooser();
		File fx;
		if (openedFile != null && openedFile.exists()) {
			fx = openedFile;
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
		// jfc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
		jfc.setFileView(new NeptusFileView());
		// jfc.setAccessory(new MissionPreview(jfc));
		jfc.setFileFilter(new FileFilter() {

			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}

				String extension = FileUtil.getFileExtension(f).toLowerCase();
				if (extension != null) {
					if (extension.equals("j3d") || extension.equals("3ds")
							|| extension.equals("x3d")
							|| extension.equals("wrl")) {
						return true;
					} else {
						return false;
					}
				}

				return false;
			}

			public String getDescription() {
				return "3D model files ('j3d', '3ds', 'x3d', 'wrl')";
			}
		});

		int result = jfc.showDialog(Viewer3D.this, "Open 3D File");
		if (result == JFileChooser.CANCEL_OPTION)
			return null;
		return jfc.getSelectedFile();
	}

	public File showSaveImageDialog() {
		// FIXME This is not a good idea. ConfigFetch is static!!
		// ConfigFetch.initialize();

		JFileChooser jfc = new JFileChooser();
		File fx;
		if (openedFile != null && openedFile.exists()) {
			fx = openedFile;
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
		if (openedFile != null) {
			String fxS = FileUtil.replaceFileExtension(openedFile.getName(), "j3d");
			jfc.setSelectedFile(new File(openedFile.getParentFile(), fxS));
		}
		jfc.setCurrentDirectory(fx);
		jfc.setFileView(new NeptusFileView());
		// jfc.setAccessory(new MissionPreview(jfc));
		jfc.setFileFilter(new FileFilter() {

			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}

				String extension = FileUtil.getFileExtension(f);
				if (extension != null) {
					if (extension.equals("j3d"))
						return true;
					else
						return false;
				}
				return false;
			}

			public String getDescription() {
				return "Java 3D files ('j3d')";
			}
		});

		int result = jfc.showDialog(Viewer3D.this, "Save File");
		if (result == JFileChooser.CANCEL_OPTION)
			return null;
		return jfc.getSelectedFile();

	}

	public TransformGroup load(File xml) {
		NeptusLog.pub().info(this + " Load file: " + xml.getAbsoluteFile());

		X3dParse parse = new X3dParse();
		parse.setFileX3d(xml.getAbsolutePath().toString());
		// NeptusLog.pub().info("<###>File loading:"+xml.getAbsolutePath().toString());
		// parse.setFileSchema(this.getClass().getClassLoader().getResource("schema/x3d-3.0.xsd").getPath());
		// if(!parse.validateDtd())
		// {
		// fazer msgbox
		// NeptusLog.pub().info("<###>Erro na validação do schema");
		// return null;
		// }
		try {
			return parse.parse();
		}
		catch (Exception e) {
			NeptusLog.pub().error(" Viewer3D parse file error ["
					+ e.getStackTrace() + "]");
		}
		return null;
	}

	String saveFile = null;

	@SuppressWarnings("unchecked")
	public void saveAsFile() {
		TransformGroup themodel = lobj.getModel3D();
		File file = showSaveImageDialog();
		if (file != null) {
			BranchGroup scene = new BranchGroup();
			render.removeObj3D(lobj);
			Enumeration<Group> enume = themodel.getAllChildren();

			while (enume.hasMoreElements()) {
				Group next = enume.nextElement();
				themodel.removeChild(next);
				scene.addChild(next);
			}

			// String file=this.getModel3DHref();
			// file.replace(".3ds", ".j3d");

			//OutputStream outS;
			try {
				SceneGraphFileWriter filew = new SceneGraphFileWriter(file,
						null, false, "genereted by Neptus", null);
				filew.writeBranchGraph(scene);
				System.err.println("vehicle w:" + file.getPath() + "\n");
				filew.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedUniverseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			TransformGroup themodel2 = new TransformGroup();
			enume = scene.getAllChildren();
			while (enume.hasMoreElements()) {
				Group next = enume.nextElement();
				scene.removeChild(next);
				themodel2.addChild(next);
			}

			lobj = new Obj3D();
			lobj.setModel3D(themodel2);
			render.addObj3D(lobj);

		}
	}

	public void openFile() {
		openFile(null);
	}

	@SuppressWarnings("unchecked")
	public void openFile(File fx) {
		saveFile = null;
		File file;
		if (fx == null)
			file = showOpenImageDialog();
		else
			file = fx;
		if (file != null) {
			if ("3ds".equalsIgnoreCase(FileUtil.getFileExtension(file))) {
				if (lobj != null)
					render.removeObj3D(lobj);
				Inspector3DS loader = new Inspector3DS(file.getAbsolutePath()); // constructor
				loader.parseIt(); // process the file
				TransformGroup theModel1 = loader.getModel(); // get the
																// resulting 3D
				NeptusLog.waste().info("Point to view window "
						+ Util3D.getModelDim(theModel1));
				lobj = new Obj3D();
				lobj.setModel3D(theModel1);
				// lobj.setRoll(Math.PI/2);
				render.addObj3D(lobj);

				update3dFilesOpened(file);
			} else if ("wrl".equalsIgnoreCase(FileUtil.getFileExtension(file))) {

				/*
				 * try {
				 * 
				 * if (lobj != null) render.removeObj3D(lobj);
				 * 
				 * String filename = file.getAbsolutePath(); VrmlLoader loader =
				 * new VrmlLoader();
				 * 
				 * BufferedReader in = null; in = new BufferedReader(new
				 * InputStreamReader(new FileInputStream(filename), "UTF8"));
				 * 
				 * URL url = FileUtil.pathToURL(file.getParent()+"/");
				 * loader.setBaseUrl(url);
				 * 
				 * Scene scene = loader.load(in); BranchGroup branch =
				 * scene.getSceneGroup();
				 * 
				 * TransformGroup ret = new TransformGroup(); Enumeration<Group>
				 * enume = branch.getAllChildren(); while
				 * (enume.hasMoreElements()) { Group next = enume.nextElement();
				 * branch.removeChild(next); ret.addChild(next); }
				 * lobj.setModel3D(ret); //lobj.setRoll(Math.PI / 2);
				 * render.addObj3D(lobj);
				 * 
				 * 
				 * } catch (Throwable e) { e.printStackTrace();
				 * GuiUtils.errorMessage(this, "Load",
				 * "Error Loading WRL File");
				 * 
				 * }
				 */

				// --------------------------------------------------------
				if (lobj != null)
					render.removeObj3D(lobj);

				Loader myFileLoader = null; // holds the file loader
				Scene myVRMLScene = null; // holds the loaded scene
				BranchGroup myVRMLModel = null; // BG of the VRML scene
				try {
					// create an instance of the Loader
					myFileLoader = new org.web3d.j3d.loaders.VRML97Loader();

					myFileLoader.setBasePath(file.getParent());
					myFileLoader
							.setFlags(myFileLoader.getFlags()
									| org.web3d.j3d.loaders.VRML97Loader.LOAD_BEHAVIOR_NODES);
					// Load the scene from your VRML97 file
					myVRMLScene = myFileLoader.load(file.getAbsolutePath());

					// Obtain the root BranchGroup for the Scene
					myVRMLModel = myVRMLScene.getSceneGroup();
					lobj = new Obj3D();
					TransformGroup scene = new TransformGroup();
					Enumeration<Node> enume = myVRMLModel.getAllChildren();
					while (enume.hasMoreElements()) {
						Node next = enume.nextElement();
						myVRMLModel.removeChild(next);
						scene.addChild(next);
					}
					lobj.setModel3D(scene);
					// lobj.setRoll(Math.PI / 2);
					render.addObj3D(lobj);

					/*
					 * VrmlLoader f = new VrmlLoader(); Scene s = null;
					 * BranchGroup myVRMLModel = null; //BG of the VRML scene
					 * try { //f.setFlags(VrmlLoader.LOAD_ALL); s =
					 * f.load(file.getAbsolutePath()); // Obtain the root
					 * BranchGroup for the Scene myVRMLModel =
					 * s.getSceneGroup(); lobj = new Obj3D(); TransformGroup
					 * scene = new TransformGroup(); Enumeration<Group> enume =
					 * myVRMLModel.getAllChildren(); while
					 * (enume.hasMoreElements()) { Group next =
					 * enume.nextElement(); myVRMLModel.removeChild(next);
					 * scene.addChild(next); } lobj.setModel3D(scene);
					 * //lobj.setRoll(Math.PI / 2); render.addObj3D(lobj); }
					 * catch (FileNotFoundException e) { e.printStackTrace();
					 * GuiUtils.errorMessage(this, "Load",
					 * "Error Loading WRL File"); }
					 */

					update3dFilesOpened(file);

					System.err.println("fez o load----------");
				} catch (Exception e) {
					// in case there was a problem, print the stack out
					e.printStackTrace();
					// we still need a model, even if we can't load the right
					// one, I use a color cube just in case
					GuiUtils.errorMessage(this, "Load",
							"Error Loading WRL File");

				}

			} else if ("x3d".equalsIgnoreCase(FileUtil.getFileExtension(file))
					|| "x3dv".equalsIgnoreCase(FileUtil.getFileExtension(file))) {

				if (lobj != null)
					render.removeObj3D(lobj);

				Loader myFileLoader = null; // holds the file loader
				Scene myVRMLScene = null; // holds the loaded scene
				BranchGroup myVRMLModel = null; // BG of the VRML scene
				try {
					// create an instance of the Loader
					myFileLoader = new org.web3d.j3d.loaders.X3DLoader();
					myFileLoader.setBasePath(file.getParent());

					// myFileLoader.setFlags(org.web3d.j3d.loaders.X3DLoader.LOAD_ALL);
					// Load the scene from your VRML97 file
					myVRMLScene = myFileLoader.load(file.getAbsolutePath());

					// Obtain the root BranchGroup for the Scene
					myVRMLModel = myVRMLScene.getSceneGroup();
					lobj = new Obj3D();
					TransformGroup scene = new TransformGroup();
					Enumeration<Group> enume = myVRMLModel.getAllChildren();
					while (enume.hasMoreElements()) {
						Group next = enume.nextElement();
						myVRMLModel.removeChild(next);
						scene.addChild(next);
					}
					lobj.setModel3D(scene);
					// lobj.setRoll(Math.PI / 2);
					render.addObj3D(lobj);

					update3dFilesOpened(file);

					System.err.println("fez o load----------");
				} catch (Exception e) {
					// in case there was a problem, print the stack out
					e.printStackTrace();
					// we still need a model, even if we can't load the right
					// one, I use a color cube just in case
					GuiUtils.errorMessage(this, "Load",
							"Error Loading x3D File");

				}

			} else if ("j3d".equalsIgnoreCase(FileUtil.getFileExtension(file))) {

				if (lobj != null)
					render.removeObj3D(lobj);

				BranchGroup bg = null;

				try {
					SceneGraphFileReader filer = new SceneGraphFileReader(file);
					bg = (filer.readAllBranchGraphs())[0];
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (bg == null) {
					NeptusLog.pub().error("Error loading vehicle model\n"
							+ this);
				}
				TransformGroup scene = new TransformGroup();

				Enumeration<Group> enume = bg.getAllChildren();
				while (enume.hasMoreElements()) {
					Group next = enume.nextElement();
					bg.removeChild(next);
					scene.addChild(next);
				}
				lobj = new Obj3D();
				lobj.setModel3D(scene);
				// lobj.setRoll(Math.PI / 2);
				render.addObj3D(lobj);

				update3dFilesOpened(file);
			}

			else
				GuiUtils.errorMessage(this, "Load", "Invalid file type.");
		}
	}

	// Recently open ___________________________________

	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getRecentlyOpenFilesMenu() {
		if (recentlyOpenFilesMenu == null) {
			recentlyOpenFilesMenu = new JMenu();
			recentlyOpenFilesMenu.getPopupMenu().setLightWeightPopupEnabled(
					false);
			recentlyOpenFilesMenu.setText("Recently opened");
			RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(
					recentlyOpenFilesMenu, miscFilesOpened);
		} else {
			RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(
					recentlyOpenFilesMenu, miscFilesOpened);
		}
		return recentlyOpenFilesMenu;
	}

	/**
	 * @param type
	 */
	private void loadRecentlyOpenedFiles() {
		String recentlyOpenedFiles = ConfigFetch
				.resolvePath(RECENTLY_OPENED_3D);
		Method methodUpdate = null;

		try {
			Class<?>[] params = { File.class };
			methodUpdate = this.getClass().getMethod("update3dFilesOpened",
					params);
		} catch (Exception e) {
			NeptusLog.pub().error(this + "loadRecentlyOpenedFiles", e);
			return;
		}

		if (recentlyOpenedFiles == null) {
			// JOptionPane.showInternalMessageDialog(this, "Cannot Load");
			return;
		}

		if (!new File(recentlyOpenedFiles).exists())
			return;

		RecentlyOpenedFilesUtil.loadRecentlyOpenedFiles(recentlyOpenedFiles,
				methodUpdate, this);
	}

	public boolean update3dFilesOpened(File fx) {
		openedFile = fx;
		RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, miscFilesOpened,
				new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {
						File fx;
						Object key = e.getSource();
						File value = miscFilesOpened.get(key);
						if (value instanceof File) {
							fx = (File) value;
							openFile(fx);
						} else
							return;
					}
				});
		getRecentlyOpenFilesMenu();
		storeRecentlyOpenedFiles();
		return true;
	}

	private void storeRecentlyOpenedFiles() {
		String recentlyOpenedFiles;
		LinkedHashMap<JMenuItem, File> hMap;
		String header;

		recentlyOpenedFiles = ConfigFetch
				.resolvePathBasedOnConfigFile(RECENTLY_OPENED_3D);
		hMap = miscFilesOpened;
		header = "Recently opened 3D files.";

		RecentlyOpenedFilesUtil.storeRecentlyOpenedFiles(recentlyOpenedFiles,
				hMap, header);
	}

}
