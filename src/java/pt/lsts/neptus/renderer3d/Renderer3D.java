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
 * Author: 
 * Mar 16, 2005
 */
package pt.lsts.neptus.renderer3d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.Locale;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.media.j3d.VirtualUniverse;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;
import com.sun.j3d.utils.universe.Viewer;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.JVideoPanelConsole;
import pt.lsts.neptus.gui.Grid3D;
import pt.lsts.neptus.gui.Properties3D;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.mme.wms.FetcherWMS;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.MapChangeListener;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.renderer2d.Renderer;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.CylinderElement;
import pt.lsts.neptus.types.map.DynamicElement;
import pt.lsts.neptus.types.map.EllipsoidElement;
import pt.lsts.neptus.types.map.HomeReferenceElement;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.map.ScatterPointsElement;
import pt.lsts.neptus.types.map.VehicleTailElement;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;

//import com.sun.j3d.utils.behaviors.picking.PickObject;

/***
 * 
 * @author RJPG Generic Class to display 3D graphics with multiple views
 * 
 */
public class Renderer3D extends JPanel implements MapChangeListener, Renderer,
		ActionListener, MouseListener, MouseMotionListener, KeyListener,
		MouseWheelListener, PreferencesListener, ComponentListener {

	protected int priority3D = Thread.MIN_PRIORITY;

	private static final long serialVersionUID = 1L;

	// ------------------------------------------------cleanup
	public boolean clean = false; // já foi corrido o clean up ?

	// roll-phi-X
	// pitch-theta-Y
	// yaw-psi-Z
	/*-------------------- desta classe---------------------------*/
	public static final int NEPTUS_MODE = 0, VIEWER_MODE = 1;

	public int mode = NEPTUS_MODE;

	protected int NVIEWS = 4; // N vistas: 4 neste momento

	/*------------------variáveis de JavaSwing--------------------*/
	private JPanel[] views; // panel[] vistas

	private JPanel viewspanel = new JPanel(); // para juntar todas

	JToolBar toolbar = new JToolBar("Controls"); // vai sair

	/*------------------variáveis de Java3D-----------------------*/
	private VirtualUniverse universe; // Universo

	private Locale locale; // node Locale

	private BoundingSphere bounds; // limites do universo

	private BranchGroup contentBranch; // BranchGroup para os objectos

	public TransformGroup contentsTransGr; // nó de transformação de toda
											// a sena

	private BranchGroup contentNoPickBranch; // BranchGroup para os objectos não
												// "clicaveis"

	public TransformGroup contentsNoPickTransGr;

	private BranchGroup viewBranch; // BranchGroup para a parte das vistas

	private TransformGroup viewTransGr; // nó de transformação de toda a
										// sena

	private BranchGroup bgBranch; // BranchGroup para background

	private Background bg; // backgorund leaf

	/*------------------variáveis da aplicação--------------------*/
	public Hashtable<AbstractElement, Obj3D> objects = new Hashtable<AbstractElement, Obj3D>(); // Hash
																								// para
																								// o
																								// mapa

	protected Hashtable<VehicleType, Obj3D> vehicles = new Hashtable<VehicleType, Obj3D>(); // Hash
																									// para
																									// veiculos

	public Camera3D[] cams;

	protected LocationType location = new LocationType();

	private MapGroup myMapGroup;

	public String lockedVehicle = null;

	private Vector<Obj3D> objs3D = new Vector<Obj3D>();

	/*------------------------executar codigo atomicamente -------------*/
	ReentrantLock lock = new ReentrantLock();

	/*----------------vars auxs de controle de menus--------------*/
	public short panel_op = 0;

	private short lines = 2, coluns = 2;

	private short panel_max = -1;

	private VehicleType veicle_op;

	private short viewing = 1;

//	private boolean sea = false;

	private boolean seaflat = false;

	private boolean grid = false;

	private boolean axis = false;

	private boolean transp = false;

	private boolean mapa = false;

	// private VehicleType vehicle_lock_interface;

	private short rule = 0;

	private short background = 1;

	// ----------------------grid
	public boolean gNE = true;

	public boolean gUE = false;

	public boolean gUN = false;

	public float gdimension = 1000.0f;

	public float gspacing = 10.0f;

	public boolean gtext = false;

	public Color gcolor = new Color(0.8f, 0.8f, 0.8f);

	public Point3d gcenter = new Point3d(0.0f, 0.0f, 0.0f);

	// ----------------------mouse...
	private boolean mouse1 = false;// direito

	// private boolean mouse2=false;//meio
	private boolean mouse3 = false;// esquerdo

	private int mousex, mousey;

	private Cursor rotateCursor, translateCursor, zoomCursor, crosshairCursor;

	// ----------------------modelos
	BranchGroup landscape;

	Obj3D seaobj; // mar ondas

	Obj3D seaflatobj; // mar flat

	Obj3D lochomeobj; // origem

	Obj3D gridobj; // grelha

	Obj3D plan; // plano se hoyver...

	// --------------------------variaveis de projecção --------------

	public ConsoleLayout console = null; // por defeito não há consola associada

	private PickTool pickTool;

	public Vector<ProjectionObj> projections = new Vector<ProjectionObj>();
	
	public Vector<SensorObj> sensors = new Vector<SensorObj>();

	public boolean stop_vehicles;

	/*------------------------Tail-----------------------------*/
	// ----------Novo---------
	private Vector<VehicleTailObj3D> VehiclesTails = new Vector<VehicleTailObj3D>();

	// --------- Antigo-------
	//private boolean isAllTailOn = false;
	private Hashtable<VehicleType, VehicleTailElement> vehicleTails = new Hashtable<VehicleType, VehicleTailElement>();
	

	/*------------------------métodos-----------------------------*/

	/**
	 * Default constructor with four views in 2*2 Layout
	 * 
	 */
	public Renderer3D() {
		Camera3D cs[] = new Camera3D[4];
		try {

			cs[0] = new Camera3D(Camera3D.TOP); // top
			cs[1] = new Camera3D(Camera3D.RIGHT); // left
			cs[2] = new Camera3D(Camera3D.BACK); // front
			cs[3] = new Camera3D(Camera3D.USER); // user*/
		} catch (Exception e) {
			NeptusLog.pub().error(e);
			GuiUtils.errorMessage(this, "Erro Render-3D",
					"erro iniciando Vistas");
			return;
		}
		cams = cs;
		NVIEWS = 4;
		lines = 2;
		coluns = 2;
		createLayout(lines, coluns);
		init3D();
		setCoordinateSystem();
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setViewMode(Renderer.TRANSLATION);

	}

	/**
	 * @param cs
	 *            array of Camera3D
	 * @param rows
	 *            of Layout
	 * @param cols
	 *            of Layout
	 */
	public Renderer3D(Camera3D cs[], short rows, short cols) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		NVIEWS = cs.length;
		cams = cs;
		lines = rows;
		coluns = cols;
		createLayout(rows, cols);
		init3D();
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setViewMode(Renderer.TRANSLATION);
		grid(true, true);
		infoAxis(true);
		infoCam(true);
		setObjsIcons(true);
		setVehiclesIcons(true);
		/*
		 * Thread t = new Thread(new Runnable() { public void run() { double
		 * offset=0; for(;;) { try {//nada
		 * //NeptusLog.pub().info("<###>----------Novo state ----------"); LocationType
		 * loc1 = new LocationType(); loc1.setDepth(-10); offset+=0.1;
		 * loc1.setLatitude(location.getLatitude());
		 * loc1.setLongitude(location.getLongitude());
		 * loc1.setOffsetNorth(offset); VehicleState sv1 = new
		 * VehicleState(loc1, 0., -Math.PI/3, 0.0); VehicleType
		 * v=VehiclesHolder.getVehicleById("rov-sim");
		 * Renderer3D.this.vehicleStateChanged(v,sv1);
		 * //NeptusLog.pub().info("<###>esperou"); Thread.sleep(2000); } catch
		 * (Exception e){ NeptusLog.pub().info("<###>excepcao"); } } } });
		 * 
		 * t.start();
		 */
	}

	private void loadCursors() {
		rotateCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				ImageUtils.getImage("images/cursors/rotate_cursor.png"),
				new java.awt.Point(15, 15), "Rotate");
		zoomCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				ImageUtils.getImage("images/cursors/zoom_cursor.png"),
				new java.awt.Point(15, 15), "Zoom");
		translateCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				ImageUtils.getImage("images/cursors/translate_cursor.png"),
				new java.awt.Point(15, 15), "Translate");
		crosshairCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				ImageUtils.getImage("images/cursors/crosshair_cursor.png"),
				new Point(12, 12), "Crosshair");
	}

	/**
	 * @param c
	 *            Camera3D to add (attach) on scene
	 */
	void addCamera3D(Camera3D c) {
		viewTransGr.addChild(c.getCamera3D());
		c.associatedrender = this;
	}

	private void createLayout(int rows, int coluns) {
		viewspanel.setLayout(new GridLayout(rows, coluns));
		views = new JPanel[NVIEWS];
		for (int i = 0; i < NVIEWS; i++) {
			views[i] = new JPanel();
			views[i].setBorder(BorderFactory
					.createBevelBorder(BevelBorder.LOWERED));
			viewspanel.add(views[i]);
			views[i].setVisible(true);
		}

		this.setLayout(new BorderLayout());
		this.add(viewspanel, BorderLayout.CENTER);
	}

	/**
	 * init 3d environment
	 */
	private void init3D() {
		// Limites (generico)
		bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0),
				Double.MAX_VALUE);
		GeneralPreferences.addPreferencesListener(this);
		setUniverse();// Raiz da arvore(inicio do Grafo)
		setContent(); // conteudo da "cena"
		setViewing(); // posicionar cameras
	}

	/**
	 * init the contetns of the scene (lights bacground capabilities)
	 */
	private void setContent() {

		contentsNoPickTransGr = new TransformGroup();
		contentsNoPickTransGr
				.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		contentsNoPickTransGr
				.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		contentsNoPickTransGr.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		contentsNoPickTransGr
				.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		contentsNoPickTransGr
				.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

		contentsTransGr = new TransformGroup();
		contentsTransGr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		contentsTransGr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		contentsTransGr.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		contentsTransGr.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		contentsTransGr.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

		// Transform3D scaleUniverse=new Transform3D();
		// scaleUniverse.setIdentity();
		// scaleUniverse.setScale(0.01);
		// contentsTransGr.setTransform(scaleUniverse);
		contentNoPickBranch = new BranchGroup();
		contentNoPickBranch.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		contentBranch = new BranchGroup();
		contentBranch.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		setBackgroundType(background);
		// contentBranch.addChild(bg);

		setLighting();

		makeGrid(); // GRID

		contentBranch.addChild(contentsTransGr);
		contentNoPickBranch.addChild(contentsNoPickTransGr);

		locale.addBranchGraph(contentBranch);
		locale.addBranchGraph(contentNoPickBranch);

		pickTool = new PickTool(contentBranch);
		pickTool.setMode(PickTool.GEOMETRY);

		// contentBranch.compile();
	}

	public void setBackgroundColor(Color3f c) {
		if (bgBranch != null) {
			contentsTransGr.removeChild(bgBranch);
			bgBranch.detach();
		}

		bgBranch = new BranchGroup();
		bgBranch.setCapability(BranchGroup.ALLOW_DETACH);
		bg = new Background(c);
		bg.setApplicationBounds(bounds);
		bgBranch.addChild(bg);
		contentsTransGr.addChild(bgBranch);
	}

	public short getBackgroundType() {
		return background;
	}

	public void setBackgroundType(short t) {
		if (t == 1)
			setBackgroundColor(new Color3f(0.007843137254901961f,
					0.4431372549019608f, 0.6705882352941176f));
		if (t == 2) {
			if (bgBranch != null) {
				contentsTransGr.removeChild(bgBranch);
				bgBranch.detach();
			}
			Background bg = new Background();
			bg.setApplicationBounds(bounds);
			BranchGroup backGeoBranch = new BranchGroup();
			Sphere sphereObj = new Sphere(1.0f, Sphere.GENERATE_NORMALS
					| Sphere.GENERATE_NORMALS_INWARD
					| Sphere.GENERATE_TEXTURE_COORDS, 90);
			Appearance backgroundApp = sphereObj.getAppearance();
			TransformGroup rotx = new TransformGroup();
			Transform3D rot = new Transform3D();
			rot.rotX(Math.PI / 2);
			rotx.setTransform(rot);
			rotx.addChild(sphereObj);
			backGeoBranch.addChild(rotx);
			bg.setGeometry(backGeoBranch);
			Image texture = ImageUtils.getImage("images/backsky.png");
			TextureLoader tex = new TextureLoader(texture,"RGB",
					this);
			if (tex != null)
				backgroundApp.setTexture(tex.getTexture());

			bgBranch = new BranchGroup();

			bgBranch.setCapability(BranchGroup.ALLOW_DETACH);

			bgBranch.addChild(bg);

			contentsTransGr.addChild(bgBranch);
			// System.err.println("entrou--------------");
		}
		if (t == 3) {
			if (bgBranch != null) {
				contentsTransGr.removeChild(bgBranch);
				bgBranch.detach();
			}
			Background bg = new Background();
			bg.setApplicationBounds(bounds);
			BranchGroup backGeoBranch = new BranchGroup();
			Sphere sphereObj = new Sphere(1.0f, Sphere.GENERATE_NORMALS
					| Sphere.GENERATE_NORMALS_INWARD
					| Sphere.GENERATE_TEXTURE_COORDS, 90);
			Appearance backgroundApp = sphereObj.getAppearance();
			TransformGroup rotx = new TransformGroup();
			Transform3D rot = new Transform3D();
			rot.rotX(Math.PI / 2);
			rotx.setTransform(rot);
			rotx.addChild(sphereObj);
			backGeoBranch.addChild(rotx);
			bg.setGeometry(backGeoBranch);
			Image texture = ImageUtils.getImage("images/landscape.png");
			TextureLoader tex = new TextureLoader(texture, "RGB",
					this);
			if (tex != null)
				backgroundApp.setTexture(tex.getTexture());

			bgBranch = new BranchGroup();

			bgBranch.setCapability(BranchGroup.ALLOW_DETACH);

			bgBranch.addChild(bg);
			contentsTransGr.addChild(bgBranch);

		}

		background = t;

	}

	// private void deactivateMap() {
	//
	// }

	public void setPlanObj(Obj3D obj) {
	    //System.err.println("##################### set plan Foi CHAMADO ####################");
		if (plan != null)
			removeObj3D(plan);

		// NeptusLog.pub().info("<###>setPlanObj("+obj+")");
		plan = obj;
		if (plan != null)
			addObj3D(plan);

	}

	public void addObj3D(Obj3D obj) {
		objs3D.add(obj);
		obj.setParent3D(contentsTransGr);
	}

	public void removeObj3D(Obj3D obj) {
		objs3D.remove(obj);
		contentsTransGr.removeChild(obj.getFullObj3D());
	}

	private void activateMap() {
		LocationType home = new LocationType();
		for (Enumeration<AbstractElement> enuma = objects.keys(); enuma
				.hasMoreElements();) {
			AbstractElement objs = enuma.nextElement();
			if (!(objs instanceof HomeReferenceElement)) {
				home = objs.getCenterLocation();
			}
		}

		FetcherWMS land = new FetcherWMS();
		LocationType topleft = new LocationType(home);
		topleft.translatePosition(200, -200, 0);
		LocationType bottomright = new LocationType(home);
		bottomright.translatePosition(-200, 200, 0);
		land.setTopLeft(topleft);
		land.setBottomRight(bottomright);
		Texture3D terrain = new Texture3D(land.fetchImage(), ImageUtils
				.getImage("images/altura.png"), 400, 400);
		terrain.maxvalue = 10;
		terrain.minvalue = 8;
		terrain.transparency = 0.0f;
		TransformGroup shape = terrain.getModel3D();

		TransformGroup center = new TransformGroup();

		// Transform3D m = new Transform3D();

		center.addChild(shape);
		landscape = new BranchGroup();
		landscape.addChild(center);
		// ------para o mapa
		landscape.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		landscape.setCapability(BranchGroup.ALLOW_DETACH);

		landscape.compile();

		// contentsTransGr.addChild(landscape);
	}

	private void makeGrid() {
		TransformGroup theModel = new TransformGroup();
		theModel.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		theModel.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		theModel.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		theModel.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		theModel.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);

		RenderingAttributes renderingAttributes = new RenderingAttributes(true, // boolean
				// depthBufferEnable,
				true, // boolean depthBufferWriteEnable,
				1.0f, // float alphaTestValue,
				RenderingAttributes.ALWAYS, // int alphaTestFunction,
				true, // boolean visible,
				true, // boolean ignoreVertexColors,
				false, // boolean rasterOpEnable,
				RenderingAttributes.ROP_COPY // int rasterOp
		);
		Color3f c = new Color3f(gcolor);
		ColoringAttributes coloringAttributes = new ColoringAttributes(c,
				ColoringAttributes.SHADE_GOURAUD);
		Appearance appearance = new Appearance();
		appearance.setRenderingAttributes(renderingAttributes);
		appearance.setColoringAttributes(coloringAttributes);

		if (gNE) {
			int i = (int) (gdimension / gspacing);
			int x = 0;
			float step = 0.f;
			int index = 0;
			Point3d listp[] = new Point3d[(i + 1) * 2];
			while (x < i + 1) {
				listp[index] = new Point3d(gdimension / 2, (-gdimension / 2)
						+ step, 0.0);
				index++;
				listp[index] = new Point3d(-gdimension / 2, (-gdimension / 2)
						+ step, 0.0);
				index++;
				x++;
				step += gspacing;
			}

			LineArray myLines = new LineArray(listp.length,
					LineArray.COORDINATES);
			myLines.setCoordinates(0, listp);
			Shape3D shape3D = new Shape3D(myLines, appearance);

			i = (int) (gdimension / gspacing);
			x = 0;
			step = 0.f;
			index = 0;
			Point3d listp2[] = new Point3d[(i + 1) * 2];
			while (x < i + 1) {
				listp2[index] = new Point3d((-gdimension / 2) + step,
						+gdimension / 2, 0.0);
				index++;
				listp2[index] = new Point3d((-gdimension / 2) + step,
						-gdimension / 2, 0.0);
				index++;
				x++;
				step += gspacing;
			}

			LineArray myLines2 = new LineArray(listp2.length,
					LineArray.COORDINATES);
			myLines2.setCoordinates(0, listp2);
			Shape3D shape3D2 = new Shape3D(myLines2, appearance);
			theModel.addChild(shape3D);
			theModel.addChild(shape3D2);
		}

		if (gUE) {
			int i = (int) (gdimension / gspacing);
			int x = 0;
			float step = 0.f;
			int index = 0;
			Point3d listp[] = new Point3d[(i + 1) * 2];
			while (x < i + 1) {
				listp[index] = new Point3d(0.0f, gdimension / 2,
						(-gdimension / 2) + step);
				index++;
				listp[index] = new Point3d(0.0f, -gdimension / 2,
						(-gdimension / 2) + step);
				index++;
				x++;
				step += gspacing;
			}

			LineArray myLines = new LineArray(listp.length,
					LineArray.COORDINATES);
			myLines.setCoordinates(0, listp);
			Shape3D shape3D = new Shape3D(myLines, appearance);

			i = (int) (gdimension / gspacing);
			x = 0;
			step = 0.f;
			index = 0;
			Point3d listp2[] = new Point3d[(i + 1) * 2];
			while (x < i + 1) {
				listp2[index] = new Point3d(0.0f, (-gdimension / 2) + step,
						+gdimension / 2);
				index++;
				listp2[index] = new Point3d(0.0f, (-gdimension / 2) + step,
						-gdimension / 2);
				index++;
				x++;
				step += gspacing;
			}

			LineArray myLines2 = new LineArray(listp2.length,
					LineArray.COORDINATES);
			myLines2.setCoordinates(0, listp2);
			Shape3D shape3D2 = new Shape3D(myLines2, appearance);
			theModel.addChild(shape3D);
			theModel.addChild(shape3D2);
		}

		if (gUN) {
			int i = (int) (gdimension / gspacing);
			int x = 0;
			float step = 0.f;
			int index = 0;
			Point3d listp[] = new Point3d[(i + 1) * 2];
			while (x < i + 1) {
				listp[index] = new Point3d(gdimension / 2, 0.0f,
						(-gdimension / 2) + step);
				index++;
				listp[index] = new Point3d(-gdimension / 2, 0.0f,
						(-gdimension / 2) + step);
				index++;
				x++;
				step += gspacing;
			}

			LineArray myLines = new LineArray(listp.length,
					LineArray.COORDINATES);
			myLines.setCoordinates(0, listp);
			Shape3D shape3D = new Shape3D(myLines, appearance);

			i = (int) (gdimension / gspacing);
			x = 0;
			step = 0.f;
			index = 0;
			Point3d listp2[] = new Point3d[(i + 1) * 2];
			while (x < i + 1) {
				listp2[index] = new Point3d((-gdimension / 2) + step, 0.0f,
						+gdimension / 2);
				index++;
				listp2[index] = new Point3d((-gdimension / 2) + step, 0.0f,
						-gdimension / 2);
				index++;
				x++;
				step += gspacing;
			}

			LineArray myLines2 = new LineArray(listp2.length,
					LineArray.COORDINATES);
			myLines2.setCoordinates(0, listp2);
			Shape3D shape3D2 = new Shape3D(myLines2, appearance);
			theModel.addChild(shape3D);
			theModel.addChild(shape3D2);
		}
		gridobj = new Obj3D(theModel);
	}

	public void grid(boolean g, boolean i) {
		gtext = i;
		if (!g) {
			contentsTransGr.removeChild(gridobj.getFullObj3D());
			grid = false;
		} else {
			gridobj.setPos(gcenter);
			contentsTransGr.removeChild(gridobj.getFullObj3D());
			gridobj.setParent3D(contentsTransGr);
			grid = true;
		}
	}

	public void menuOKgrid() {

		if (grid) {
			contentsTransGr.removeChild(gridobj.getFullObj3D());
			this.makeGrid();
			gridobj.setPos(gcenter);
			gridobj.setParent3D(contentsTransGr);

		} else {
			this.makeGrid();
			gridobj.setPos(gcenter);
		}

	}

	public void infoAxis(boolean flag) {
		for (int i = 0; i < NVIEWS; i++)
			cams[i].canvas.axisinfo = flag;
	}

	public void infoCam(boolean flag) {
		for (int i = 0; i < NVIEWS; i++)
			cams[i].canvas.caminfo = flag;
	}

	private void setLighting() {
		AmbientLight ambientLight = new AmbientLight();
		ambientLight.setEnable(true);
		ambientLight.setColor(new Color3f(0.5f, 0.5f, 0.5f));
		ambientLight.setCapability(AmbientLight.ALLOW_STATE_READ);
		ambientLight.setCapability(AmbientLight.ALLOW_STATE_WRITE);
		ambientLight.setInfluencingBounds(bounds);
		contentsTransGr.addChild(ambientLight);

		DirectionalLight dirLight = new DirectionalLight();
		dirLight.setEnable(true);
		dirLight.setColor(new Color3f(0.8f, 0.8f, 0.8f));
		dirLight.setDirection(new Vector3f(1f, 1f, 1f));
		dirLight.setCapability(AmbientLight.ALLOW_STATE_WRITE);
		dirLight.setInfluencingBounds(bounds);
		contentsTransGr.addChild(dirLight);

		DirectionalLight dirLight2 = new DirectionalLight();
		dirLight2.setEnable(true);
		dirLight2.setColor(new Color3f(0.7f, 0.7f, 0.7f));
		dirLight2.setDirection(new Vector3f(-1f, -1f, -1f));
		dirLight2.setCapability(AmbientLight.ALLOW_STATE_WRITE);
		dirLight2.setInfluencingBounds(bounds);
		contentsTransGr.addChild(dirLight2);
	}

	/**
	 * init the viewBranch
	 */
	private void setViewing() {
		viewBranch = new BranchGroup();
		viewTransGr = new TransformGroup();
		viewTransGr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		viewTransGr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		viewTransGr.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		viewTransGr.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		viewTransGr.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

		for (int i = 0; i < NVIEWS; i++) {
			views[i].setLayout(new BorderLayout());
			views[i].add(cams[i].canvas, BorderLayout.CENTER);
			addCamera3D(cams[i]);
			cams[i].associatedrender = this;
			cams[i].canvas.startRenderer();
			cams[i].canvas.addMouseListener(this);
			cams[i].canvas.addMouseMotionListener(this);
			cams[i].canvas.addKeyListener(this);
			cams[i].canvas.addMouseWheelListener(this);
			// AUX();

		}
		viewBranch.addChild(viewTransGr);
		locale.addBranchGraph(viewBranch);

	}

	public void unFreeze() {
		for (int i = 0; i < NVIEWS; i++) {
			cams[i].canvas.addMouseListener(this);
			cams[i].canvas.addMouseMotionListener(this);
			cams[i].canvas.addKeyListener(this);
			cams[i].canvas.addMouseWheelListener(this);
			// AUX();

		}
	}

	public void freeze() {
		for (int i = 0; i < NVIEWS; i++) {
			cams[i].canvas.removeMouseListener(this);
			cams[i].canvas.removeMouseMotionListener(this);
			cams[i].canvas.removeKeyListener(this);
			cams[i].canvas.removeMouseWheelListener(this);
		}
	}

	public void noBorder() {
		for (int i = 0; i < NVIEWS; i++) {
			views[i].setBorder(BorderFactory.createEmptyBorder());
		}
	}

	public void border() {
		for (int i = 0; i < NVIEWS; i++) {
			views[i].setBorder(BorderFactory
					.createBevelBorder(BevelBorder.LOWERED));

		}
	}

	private void setUniverse() {
		// Creating the VirtualUniverse and the Locale nodes
		universe = new VirtualUniverse();
		updatePreferencies();

		locale = new Locale(universe);
	}

	/**
	 * inverse z axis(Cameras inverse as well...)
	 */
	public void setCoordinateSystem() // NED ou NWU...
	{
		Transform3D t = new Transform3D();
		t.rotX(Math.PI);// só isto parajá
		t.rotY(0.0);
		t.rotZ(0.0);
		contentsTransGr.setTransform(t); // ajustes nos objs
		viewTransGr.setTransform(t); // ajustes nas cams
	}

	public TransformGroup getFlatSea() {

		TransformGroup ret = new TransformGroup();
		QuadArray GFront = new QuadArray(4, QuadArray.COORDINATES);

		Point3d bottomLeft = new Point3d(-1000, 0, -1000);
		Point3d bottomRight = new Point3d(-1000, 0, 1000);
		Point3d topLeft = new Point3d(1000, 0, -1000);
		Point3d topRight = new Point3d(1000, 0, 1000);

		GFront.setCoordinate(3, topLeft);
		GFront.setCoordinate(2, topRight);
		GFront.setCoordinate(1, bottomRight);
		GFront.setCoordinate(0, bottomLeft);

		Appearance appGFront = new Appearance();
		TransparencyAttributes trans = new TransparencyAttributes();
		trans.setTransparency(0.5f);
		trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
		appGFront.setTransparencyAttributes(trans);

		GeometryInfo gi = new GeometryInfo(GFront);
		Material mat = new Material();
		Color3f c = new Color3f();
		c.set(Color.BLUE);

		mat.setDiffuseColor(c);
		mat.setSpecularColor(c);
		mat.setShininess(0.1f);
		appGFront.setMaterial(mat);

		NormalGenerator ng = new NormalGenerator();
		// gi.convertToIndexedTriangles();
		ng.generateNormals(gi);
		gi.recomputeIndices();
		gi.unindexify();
		gi.compact();

		PolygonAttributes p = new PolygonAttributes(
				PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
				0.0f);
		appGFront.setPolygonAttributes(p);

		GeometryArray geom = gi.getGeometryArray();
		geom.setCapability(GeometryArray.ALLOW_NORMAL_READ);
		Shape3D shape = new Shape3D(geom, appGFront);

		ret.addChild(shape);
		return ret;

	}

	/*--Metodos "abstractos"("virtuais" em C++)--*/
	/*
	 * public void mapChanged(MapGroup mapGroup, MapObject mo) { if
	 * (objects.containsKey(mo)) { //novo if (mo instanceof Path2D && ((Path2D)
	 * mo).isFinished()) { Obj3D obj = new Obj3D();
	 * obj.setModel3D(mo.getModel3D()); obj.setPos(mo.getNEDPosition());//
	 * contentsTransGr.addChild(obj.getFullObj3D()); objects.put(mo, obj); } if
	 * (!(mo instanceof Path2D)) { Obj3D obj = new Obj3D();
	 * obj.setModel3D(mo.getModel3D()); obj.setPos(mo.getNEDPosition());//
	 * obj.setRoll(mo.getRollRad()); obj.setPitch(mo.getPitchRad());
	 * obj.setYaw(mo.getYawRad()); contentsTransGr.addChild(obj.getFullObj3D());
	 * objects.put(mo, obj); } } else { // já existe Obj3D obj; //falta testar
	 * se é caminho e já está feito... obj = (Obj3D) objects.get(mo);
	 * obj.setPos(mo.getNEDPosition()); obj.setRoll(mo.getRollRad());
	 * obj.setPitch(mo.getPitchRad()); obj.setYaw(mo.getYawRad()); }
	 * 
	 * if (transp) // por defeito o obj vem transparente { settrans(); } else {
	 * setnotrans(); } }
	 */

	public MapGroup getMapGroup() {
		return myMapGroup;
	}

	public void setMapGroup(MapGroup mapGroup) {

		reset();

		if (myMapGroup != null)
			myMapGroup.removeChangeListener(this);

		myMapGroup = mapGroup;

		focusLocation(mapGroup.getHomeRef().getCenterLocation());

		AbstractElement[] objs = mapGroup.getAllObjects();
		mapGroup.addChangeListener(this);

		for (int i = 0; i < objs.length; i++) {
			// System.err.println("Adicionado o objecto "+objs[i]+", ("+objs[i].hashCode()+")");
			if (!(objects.containsKey(objs[i]))) { // novo
				// NeptusLog.pub().info("<###> "+((Path2D) objs[i]).isFinished());
				if (objs[i] instanceof PathElement
						&& ((PathElement) objs[i]).isFinished()) { // corrigir a
																	// flag na
																	// class
					Obj3D obj = new Obj3D();
					obj.setModel3D(Object3DCreationHelper.getModel3DForRender(objs[i])); // objs[i].getModel3D());
					obj.setPos(objs[i].getCenterLocation().getOffsetFrom(
							location));
					// obj.setPos(location.getOffsetFrom(objs[i].getCenterLocation()));//
					obj.setRoll(objs[i].getRollRad());
					obj.setPitch(objs[i].getPitchRad());
					obj.setYaw(objs[i].getYawRad());
					// obj.setPickble();
					contentsTransGr.addChild(obj.getFullObj3D());
					objects.put(objs[i], obj);
				}
				if (!(objs[i] instanceof PathElement)) {
					if (objs[i] instanceof DynamicElement) {
						Obj3D obj = new Obj3D();
						TransformGroup m3d = Object3DCreationHelper.getModel3DForRender(objs[i]);
						if (m3d == null)
							return;
						obj.setModel3D(m3d); //objs[i].getModel3D());
						obj.setPos(objs[i].getCenterLocation().getOffsetFrom(
								location));
						obj.setRoll(objs[i].getRollRad());
						obj.setPitch(objs[i].getPitchRad());
						obj.setYaw(objs[i].getYawRad());

						// System.err.println(contentsTransGr);
						// obj.setPickble();
						contentsTransGr.addChild(obj.getFullObj3D());
						objects.put(objs[i], obj);
						// System.err.println("---------------");
						obj.drawinfo = true;

					} else {
						Obj3D obj = new Obj3D();
						// TransformGroup model3d = objs[i].getModel3D();

						obj.setModel3D(Object3DCreationHelper.getModel3DForRender(objs[i])); //objs[i].getModel3D());
						obj.setPos(objs[i].getCenterLocation().getOffsetFrom(
								location));
						// obj.setPos(location.getOffsetFrom(objs[i].getCenterLocation()));//
						obj.setRoll(objs[i].getRollRad());
						obj.setPitch(objs[i].getPitchRad());
						obj.setYaw(objs[i].getYawRad());
						// obj.setPickble();
						contentsTransGr.addChild(obj.getFullObj3D());
						// String ObjectType = objs[i].getTypeName();
						// String ObjectId = objs[i].getId();
						// TODO criar o branchgroup do object...
						// Object branch = new Object();
						objects.put(objs[i], obj);
					}
				}
			} else { // já existe
				Obj3D obj; // falta testar se é caminho e já está feito...
				obj = objects.get(objs[i]);

				obj.setPos(objs[i].getCenterLocation().getOffsetFrom(location));
				// obj.setPos(location.getOffsetFrom(objs[i].getCenterLocation()));
				obj.setRoll(objs[i].getRollRad());
				obj.setPitch(objs[i].getPitchRad());
				obj.setYaw(objs[i].getYawRad());
				setTrans(objs[i], objs[i].getTransparency() / 100.f);
			}
		}
		// if (transp) // por defeito o obj vem transparente
		// {
		// setTrans();
		// } else {
		// setNoTrans();
		// }

	}

	public void vehicleStateChanged(String sys, SystemPositionAndAttitude state) {

		/*---
		 boolean newTailAdded = false;
		 if (!vehicleTails.containsKey(vehicle)) {
		 VehicleTailElement vehicleTail = new VehicleTailElement(getMapGroup());
		 vehicleTail.setCenterLocation(getMapGroup().getHomeRef().getCenterLocation());
		 vehicleTail.setNumberOfPoints(numberOfShownPoints);
		 vehicleTails.put(vehicle, vehicleTail);
		 newTailAdded = true;
		 }
		 double[] distFromRef = state.getPosition().getOffsetFrom(vehicleTails.get(vehicle).getCenterLocation());
		 vehicleTails.get(vehicle).addPoint(distFromRef[0], distFromRef[1], distFromRef[2]);

		 if (newTailAdded)
		 {
		 VehicleTailElement vehicleTail = vehicleTails.get(vehicle);
		 if (isAllTailOn || (!isAllTailOn && vehiclesTailOn.contains(vehicle)))
		 {
		 MapChangeEvent mce = new MapChangeEvent(
		 MapChangeEvent.OBJECT_ADDED);
		 mce.setMapGroup(getMapGroup());
		 mce.setChangedObject(vehicleTail);
		 this.mapChanged(mce);
		 }
		 }
		 ---*/

		if (stop_vehicles)
			return;

		VehicleType vehicle = VehiclesHolder.getVehicleById(sys);
		if (vehicle == null)
		    return;
		
		if (!(vehicles.containsKey(vehicle))) {
			lock.lock();
			Obj3D v3D = new Obj3D(Object3DCreationHelper.getVehicleModel3D(vehicle.getModel3DHref())); //vehicle.getModel3D());
			vehicles.put(vehicle, v3D);

			// v3D.setPos(state.getNEDPosition());
			// v3D.setPos(location.getOffsetFrom(state.getPosition()));
			v3D.setPos(state.getPosition().getOffsetFrom(location));
			// location.getOffsetFrom(objs[i].getCenterLocation())
			v3D.setRoll(state.getRoll());
			v3D.setPitch(state.getPitch());
			v3D.setYaw(state.getYaw());

			contentsNoPickTransGr.addChild(v3D.getFullObj3D());
			lock.unlock();
			// viewTransGr.removeChild(cams[0].getCamera3D());
			// v3D.move.addChild(cams[0].getCamera3D());
			// v3D.addCamera3D(cams[0]);

		} else {
			Obj3D v3D;
			v3D = vehicles.get(vehicle);
			// cams[0].canvas.postSwap();
			// cams[0].canvas.startRenderer();
			// cams[0].canvas.startRenderer();
			// cams[0].canvas.stopRenderer();
			// NeptusLog.pub().info("<###>já existe");
			// cams[0].canvas.preRender();

			// double d[]=state.getNEDPosition();
			// cams[0].setPivot(new Vector3d(d[0],d[1],d[2]));

			// cams[0].canvas.render();
			// cams[0].canvas.postSwap();
			// cams[0].canvas.setEnabled(false);
			// cams[0].view.stopView();
			// cams[0].view.stopBehaviorScheduler();

			// v3D.setPos(state.getNEDPosition());
			// state.getPosition().getOffsetFrom(location)
			// v3D.setPos(location.getOffsetFrom(state.getPosition()));
			Point3d pos = new Point3d(state.getPosition().getOffsetFrom(
					location));
			v3D.setPos(pos);
			v3D.setRoll(state.getRoll());
			v3D.setPitch(state.getPitch());
			v3D.setYaw(state.getYaw());
			lock.lock();
			for (VehicleTailObj3D vtailo : VehiclesTails) {
				if (vtailo.getVehicle() == vehicle) {
					// System.err.println("Encontrou : nova posição");
					vtailo.addNewVehicleState(state);
				}

			}

			for (ProjectionObj p : projections) {
				if (p.getVehicle().getId().equals(vehicle.getId())) {
					// p.refreshVideoMap();
					p.refreshObj(pos, state.getRoll(), state.getPitch(), state
							.getYaw());
				}
			}
			lock.unlock();
			// cams[0].canvas.renderField(Canvas3D.FIELD_ALL);
			// cams[0].canvas.postRender();
			// cams[0].view.startBehaviorScheduler();
			// cams[0].view.startView();
			// cams[0].canvas.postSwap();
			// cams[0].canvas.swap();
			// cams[0].canvas.swap();
			// cams[0].canvas.startRenderer();

			// cams[0].canvas.setEnabled(true);
			// cams[].setPsi(state.getYaw()+Math.PI/2);

		}
		/*--
		VehicleTailElement vte = vehicleTails.get(vehicle);
		if (vte != null) {
			MapChangeEvent mce = new MapChangeEvent(
					MapChangeEvent.OBJECT_CHANGED);
			mce.setChangedObject(vte);
			this.mapChanged(mce);
		}
		--*/
	}

	/*--------------------------main para testar------------------*/
	public static void main(String args[]) {

		// NeptusLog.pub().info("<###>d"+Math.tan();
		NeptusLog.pub().info("<###>angulo" + 2 * Math.toDegrees(Math.atan(1 / 3.45)));

		ConfigFetch.initialize();
		VehiclesHolder.loadVehicles();
		VehicleType vehicle = VehiclesHolder.getVehicleById("lusitania");
		// VehicleType vehicle2 = new VehicleType(
		// "vehicles-defs/rov-ies-vehicle-lsts.xml");
		// VehicleType vehicle3=new
		// VehicleType("vehicles-defs/rov-kos-vehicle-lsts.xml");
		// Renderer3D R=new Renderer3D();
		// String s = new String();
		// s=vehicle.getModel3DHref();

		Camera3D cs[] = new Camera3D[1]; // escolher uma cam de lado
		cs[0] = new Camera3D(Camera3D.USER); // top
		cs[0].setScale(1f);
		cs[0].setPivot(new Vector3d(0, 0, 0));

		cs[0].setPsi(0);
		cs[0].setRho(100);
		cs[0].setTheta(0);

		// Util3D.setCameraPositionBox(cs[0], pa, pb);
		/*
		 * cs[1]=new Camera3D(Camera3D.RIGHT); //left cs[2]=new
		 * Camera3D(Camera3D.BACK); //front cs[3]=new Camera3D(Camera3D.USER);
		 * //user
		 */

		Renderer3D r = new Renderer3D(cs, (short) 1, (short) 1); // 1 por 1
		// r.setViewMode(-1); //escolher o modo de visão nada (para o cursor)
		// r.freeze(); // congelar a janela para o utilizador

		LocationType loc = new LocationType();

		SystemPositionAndAttitude sv = new SystemPositionAndAttitude(loc, 0., 0., 0.);
		// LocationType loc1 = new LocationType();
		// loc1.setDepth(0.25);
		// loc1.setOffsetWest(1);
		// VehicleState sv1 = new VehicleState(loc1, 0., 0., 0.0);
		// LocationType loc2 = new LocationType();
		// loc2.setDepth(1.);
		// loc2.setOffsetWest(-1);
		// VehicleState sv2 = new VehicleState(loc2, 0., 0., 0.);
		// r.vehicleStateChanged(vehicle,sv);
		// r.vehicleStateChanged(vehicle2, sv1);
		// r.followVehicle(vehicle2);
		// r.vehicleStateChanged(vehicle3,sv2);
		// loc.setOffsetNorth(1.); //1 metro em x
		sv = new SystemPositionAndAttitude(loc, 0., 0., 0.);

		// r.vehicleStateChanged(vehicle,sv);
		// vehicle.setModel3dHref("sef");
		JFrame a = new JFrame("Render");
		a.setSize(640, 480);
		a.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		a.getContentPane().setLayout(new BorderLayout());
		a.getContentPane().add(r, BorderLayout.CENTER);
		a.setVisible(true);

		while (1 != 0) {
			// Delayed(1000);
			sv.setPitch(0.0);
			r.vehicleStateChanged(vehicle.getId(), sv);

			// GuiUtils.takeSnapshot(r,"");
		}

		// NeptusLog.pub().info("<###> "+s);
	}

	public void setMap(MapType map) {
		// Não perder tempo a implementar isto
		// System.err.println("set map chamado");
	}

	// public void vehicleStateChanged(Vehicle vehicle, VehicleState state) {
	// // TODO Auto-generated method stub
	//
	// }

	/*-------------------------------------------------------*/
	public void lockView(int view, Obj3D obje) {
		if (obje == null) {
			if (cams[view].lockobj != null) {
				cams[view].lockobj.removeCamera3D(cams[view]);
				viewTransGr.addChild(cams[view].getCamera3D());
			}

			cams[view].lock = null;
			cams[view].lockmapobj = null;
			cams[view].lockobj = null;
			return;
		}

		if (cams[view].lockobj == null)// If cam3D is not locked
		{
			viewTransGr.removeChild(cams[view].getCamera3D());
			obje.addCamera3D(cams[view]);
			cams[view].lockobj = obje;
			cams[view].lock = null;
			cams[view].lockmapobj = null;

			Enumeration<AbstractElement> e = objects.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				Obj3D obj = objects.get(key);
				if (obj == obje) {
					cams[view].lockmapobj = (AbstractElement) key;
				}
			}

			Enumeration<VehicleType> e1 = vehicles.keys();
			while (e1.hasMoreElements()) {
				Object key = e1.nextElement();
				Obj3D obj = vehicles.get(key);
				if (obj == obje) {
					cams[view].lock = (VehicleType) key;
				}
			}

			// NeptusLog.pub().info("<###>está lock");
		} else {
			cams[view].lockobj.removeCamera3D(cams[view]);
			obje.addCamera3D(cams[view]);
			cams[view].lockobj = obje;
			cams[view].lock = null;
			cams[view].lockmapobj = null;

			Enumeration<AbstractElement> e = objects.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				Obj3D obj = objects.get(key);
				if (obj == obje) {
					cams[view].lockmapobj = (AbstractElement) key;
				}
			}

			Enumeration<VehicleType> e1 = vehicles.keys();
			while (e1.hasMoreElements()) {
				Object key = e1.nextElement();
				Obj3D obj = objects.get(key);
				if (obj == obje) {
					cams[view].lock = (VehicleType) key;
				}
			}

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if ("maximize".equals(e.getActionCommand())) {
			for (int i = 0; i < NVIEWS; i++) {
				viewspanel.remove(views[i]);
				// cams[i].canvas.stopRenderer();
			}
			for (int i = 0; i < NVIEWS; i++) // para forçar a lteração do
												// tamanho de todas as canvas
			{ // para o desenho2d não se passar...
				if (i != panel_op) // panal_op já vai alterar o seu tam 
									// frente
				{
					viewspanel.setLayout(new BorderLayout());
					cams[i].canvas.startRenderer();
					viewspanel.add(views[i], BorderLayout.CENTER);
					viewspanel.doLayout();
					views[i].doLayout();
					panel_max = panel_op;
				}
			}
			for (int i = 0; i < NVIEWS; i++) {
				viewspanel.remove(views[i]);
				// cams[i].canvas.stopRenderer();
			}
			viewspanel.setLayout(new BorderLayout());
			cams[panel_op].canvas.startRenderer();
			viewspanel.add(views[panel_op], BorderLayout.CENTER);
			viewspanel.doLayout();
			views[panel_op].doLayout();
			panel_max = panel_op;

			return;
		}
		if ("restore".equals(e.getActionCommand())) {
			// boolean auxc=cams[panel_max].canvas.caminfo; //info cam *
			viewspanel.remove(views[panel_op]);

			// cams[panel_op].canvas.stopRenderer();
			viewspanel.setLayout(new GridLayout(lines, coluns));
			for (int i = 0; i < NVIEWS; i++) {
				cams[i].canvas.truePostRender();
				cams[i].canvas.startRenderer();

				cams[i].canvas.validate();
				viewspanel.doLayout();
				views[i].doLayout();
				viewspanel.add(views[i]);
				cams[i].canvas.addNotify();
				// cams[i].canvas.caminfo=false;

			}
			viewspanel.doLayout();
			Dimension d2 = new Dimension();
			// d= viewspanel.getSize();
			d2.setSize(1, 1);
			for (int i = 0; i < NVIEWS; i++) {
				views[i].doLayout();

				// cams[i].canvas.paint(cams[i].canvas.g2);

				// cams[i].canvas.doLayout();
				// cams[i].canvas.repaint();

			}
			// cams[panel_max].canvas.caminfo=auxc; //info cam
			panel_max = -1;
			// viewspanel.setSize(d2);
			// d= viewspanel.getSize();
			// d.setSize(d.width,d.height);
			// Dimension d2=new Dimension();
			// d2.setSize(1,1);
			// viewspanel.setSize(d2);
			// d.setSize(d.height,d.width);
			// viewspanel.setSize(d);
			// cams[0].setPhi(cams[0].phi);

			return;
		}
		if ("lock".equals(e.getActionCommand())) {

			if (cams[panel_op].lock != veicle_op)
				lockView(panel_op, (Obj3D) vehicles.get(veicle_op));
			else
				lockView(panel_op, null);

			/*
			 * if(cams[panel_op].lockobj==null)//se cam não está presa {
			 * viewTransGr.removeChild(cams[panel_op].getCamera3D()); Obj3D v3D;
			 * v3D=(Obj3D)vehicles.get(veicle_op);
			 * v3D.addCamera3D(cams[panel_op]); cams[panel_op].lock=veicle_op;
			 * cams[panel_op].lockobj=v3D; cams[panel_op].lockmapobj=null;
			 * //NeptusLog.pub().info("<###>está lock"); } else{
			 * if(cams[panel_op].lock==veicle_op) //se está presa e é o
			 * proprio { Obj3D v3D;
			 * v3D=(Obj3D)vehicles.get(cams[panel_op].lock);
			 * v3D.removeCamera3D(cams[panel_op]); cams[panel_op].lock=null;
			 * cams[panel_op].lockobj=null; cams[panel_op].lockmapobj=null; //só
			 * para comfirmar...
			 * viewTransGr.addChild(cams[panel_op].getCamera3D()); } else // se
			 * passa a prender a outro {
			 * 
			 * Obj3D v3D; if(cams[panel_op].lock!=null) {
			 * v3D=(Obj3D)vehicles.get(cams[panel_op].lock);
			 * v3D.removeCamera3D(cams[panel_op]); } else {
			 * v3D=(Obj3D)objects.get(cams[panel_op].lock);
			 * v3D.removeCamera3D(cams[panel_op]); }
			 * v3D=(Obj3D)vehicles.get(veicle_op);
			 * viewTransGr.removeChild(cams[panel_op].getCamera3D());
			 * v3D.addCamera3D(cams[panel_op]); cams[panel_op].lock=veicle_op;
			 * cams[panel_op].lockobj=v3D; } }
			 */

			return;
		}
		if ("move".equals(e.getActionCommand())) {
			if (viewing == 1)
				setViewMode(Renderer.NONE);
			else {
				setViewMode(Renderer.TRANSLATION);
				// viewing=1;
			}
			sendChangeEvent();

			return;
		}
		if ("zoom".equals(e.getActionCommand())) {
			if (viewing == 2)
				setViewMode(Renderer.NONE);
			else {
				setViewMode(Renderer.ZOOM);
				// viewing=2;
			}
			sendChangeEvent();

			return;
		}
		if ("rotate".equals(e.getActionCommand())) {
			if (viewing == 3)
				setViewMode(Renderer.NONE);
			else {
				setViewMode(Renderer.ROTATION);
				// viewing=3;
			}
			sendChangeEvent();

			return;
		}
		if ("ruler".equals(e.getActionCommand())) {
			if (viewing == Renderer.RULER)
				setViewMode(Renderer.NONE);
			else {
				setViewMode(Renderer.RULER);
				// viewing=3;
			}
			sendChangeEvent();

			return;
		}

		if ("resetmenu".equals(e.getActionCommand())) {

			return;
		}
		if ("resettop".equals(e.getActionCommand())) {
			cams[panel_op].resetTop();

			return;
		}
		if ("resetright".equals(e.getActionCommand())) {
			cams[panel_op].resetRight();

			return;
		}
		if ("resetback".equals(e.getActionCommand())) {
			cams[panel_op].resetBack();

			return;
		}
		if ("resetuser".equals(e.getActionCommand())) {
			cams[panel_op].resetUser();

			return;
		}
		if ("resetview".equals(e.getActionCommand())) {
			cams[panel_op].setType(cams[panel_op].type);

			return;
		}
		if ("sea flat".equals(e.getActionCommand())) {
			if (seaflatobj == null) {
				seaflatobj = new Obj3D(getFlatSea());
				seaflatobj.setRoll(-Math.PI / 2);
			}
			if (seaflat) {
				contentsTransGr.removeChild(seaflatobj.getFullObj3D());
				seaflat = false;
			} else {
				Point3d centro = new Point3d(0., 0., 0.);
				seaflatobj.setPos(centro);
				seaflatobj.setParent3D(contentsTransGr);
				seaflat = true;
			}

			return;
		}

		if ("map".equals(e.getActionCommand())) {

			if (mapa) {
				// contentsTransGr.addChild(landscape);
				contentsTransGr.removeChild(landscape);
				mapa = false;
			} else {
				activateMap();
				contentsTransGr.addChild(landscape);
				mapa = true;
			}

			return;
		}
		if ("transparency".equals(e.getActionCommand())) {

			if (transp) {
				setNoTrans();
				transp = false;
			} else {
				setTrans();
				transp = true;
			}

			return;
		}

		if ("backsolid".equals(e.getActionCommand())) {
			setBackgroundType((short) 1);
			return;
		}

		if ("backsky".equals(e.getActionCommand())) {
			setBackgroundType((short) 2);
			return;
		}

		if ("backskyground".equals(e.getActionCommand())) {
			setBackgroundType((short) 3);
			return;
		}

		if ("grid".equals(e.getActionCommand())) {
			if (grid) {
				contentsTransGr.removeChild(gridobj.getFullObj3D());
				grid = false;
			} else {
				gridobj.setPos(gcenter);
				gridobj.setParent3D(contentsTransGr);
				grid = true;
			}

			return;
		}
		if ("menugrid".equals(e.getActionCommand())) {
			// Grid3D grid3Ddialog=new Grid3D();

			Grid3D.showGridDialog(this);

			return;
		}

		if ("Axis".equals(e.getActionCommand())) {

			if (axis) {

				axis = false;
				showAxis(axis);
			} else {
				axis = true;
				showAxis(axis);
			}

			return;
		}

		if ("infocamera".equals(e.getActionCommand())) {
			if (cams[panel_op].canvas.caminfo) {

				cams[panel_op].canvas.caminfo = false;
			} else {
				cams[panel_op].canvas.caminfo = true;
			}

			return;
		}
		if ("infoaxis".equals(e.getActionCommand())) {
			if (cams[panel_op].canvas.axisinfo) {

				cams[panel_op].canvas.axisinfo = false;
			} else {
				cams[panel_op].canvas.axisinfo = true;
			}

			return;
		}
		if ("infoobj".equals(e.getActionCommand())) {
			if (cams[panel_op].canvas.objinfo) {

				cams[panel_op].canvas.objinfo = false;
			} else {
				cams[panel_op].canvas.objinfo = true;
			}

			return;
		}
		if ("infoveicle".equals(e.getActionCommand())) {
			if (cams[panel_op].canvas.veicleinfo) {

				cams[panel_op].canvas.veicleinfo = false;
			} else {
				cams[panel_op].canvas.veicleinfo = true;
			}

			return;
		}

		if ("iconsobj".equals(e.getActionCommand())) {
			if (cams[panel_op].canvas.objsicons) {
				cams[panel_op].canvas.objsicons = false;
			} else {
				cams[panel_op].canvas.objsicons = true;
			}

			return;
		}

		if ("iconsvehicle".equals(e.getActionCommand())) {
			if (cams[panel_op].canvas.vehicleicons) {
				cams[panel_op].canvas.vehicleicons = false;
			} else {
				cams[panel_op].canvas.vehicleicons = true;
			}
			return;
		}

		if ("propreties".equals(e.getActionCommand().substring(0, 10))) {
			// NeptusLog.pub().info("<###>carregou em properties");
			// NeptusLog.pub().info("<###> "+e.getActionCommand().substring(10));

			Enumeration<AbstractElement> enuma = objects.keys();
			while (enuma.hasMoreElements()) {
				AbstractElement key = (AbstractElement) enuma.nextElement();
				// Obj3D obj = (Obj3D) objects.get(key);
				if (key.getId().equals(e.getActionCommand().substring(10))) {
					// settrans(key);
					Properties3D.showPropreties3DDialog(this, key);

				}
			}

			return;
		}

		if ("Enable Pick".equals(e.getActionCommand())) {
			// System.err.println("Enable Pick...");

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */

	public void mouseClicked(MouseEvent e) {
		for (short i = 0; i < NVIEWS; i++) {
			cams[i].canvas.selected = false;
			// cams[i].canvas.repaint();
			// cams[i].canvas.renderField(Canvas3D.FIELD_ALL);
			// cams[i].setPhi(cams[i].phi);
			if (e.getSource() == cams[i].canvas) {
				/*
				 * try {
				 * 
				 * 
				 * UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName
				 * ()); } catch(Exception ex){ }
				 */

				panel_op = i;
				// NeptusLog.pub().info("<###> "+panel_op);
				// cams[i].canvas.renderField(MyCanvas3D.FIELD_ALL);
				// cams[panel_op].canvas.repaint();

				cams[panel_op].canvas.selected = true;
				cams[panel_op].setPhi(cams[i].phi);

			}
		}

		if (e.getButton() == MouseEvent.BUTTON1) {
			if (viewing == Renderer.RULER) {
				if (rule == 1) // arrastou e clicou
				{
					rule = 0;
					cams[panel_op].canvas.reguainfo = false;
				} else// (rule==0) //clica
				{
					rule = 1;
					cams[panel_op].canvas.reguainfo = true;
					cams[panel_op].canvas.p1.x = e.getX();
					cams[panel_op].canvas.p1.y = e.getY();
					cams[panel_op].canvas.p2.x = e.getX();
					cams[panel_op].canvas.p2.y = e.getY();
				}

			}
		}

		if (e.getButton() == MouseEvent.BUTTON3) {

			JPopupMenu popup = new JPopupMenu();
			popup.setLightWeightPopupEnabled(false);
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);

			// popup.set
			JMenuItem item;
			if (NVIEWS != 1) // só uma vista está sempre max...
			{
				item = new JMenuItem("Maximize");
				item.setActionCommand("maximize");
				item.addActionListener(this);
				if (panel_max != -1)
					item.setEnabled(false);
				popup.add(item);

				item = new JMenuItem("Restore");
				item.setActionCommand("restore");
				if (panel_max == -1)
					item.setEnabled(false);
				item.addActionListener(this);
				popup.add(item);
			}

			item = new JMenuItem("Reset View");
			item.setIcon(ImageUtils.getIcon("images/menus/restart.png"));
			item.setActionCommand("resetview");
			item.addActionListener(this);
			popup.add(item);

			item = new JMenu("Viewing");
			item.setIcon(ImageUtils.getIcon("images/menus/position.png"));
			popup.add(item);
			JCheckBoxMenuItem chekm = new JCheckBoxMenuItem("Move");
			chekm.setActionCommand("move");
			chekm.addActionListener(this);
			if (viewing == 1)
				chekm.setSelected(true);
			item.add(chekm);
			JCheckBoxMenuItem chekz = new JCheckBoxMenuItem("Zoom");
			chekz.setActionCommand("zoom");
			if (viewing == 2)
				chekz.setSelected(true);
			chekz.addActionListener(this);
			item.add(chekz);

			JCheckBoxMenuItem chekr = new JCheckBoxMenuItem("Rotate");
			chekr.setActionCommand("rotate");
			if (viewing == 3)
				chekr.setSelected(true);
			chekr.addActionListener(this);
			item.add(chekr);

			JCheckBoxMenuItem chekruler = new JCheckBoxMenuItem("Ruler");
			chekruler.setActionCommand("ruler");
			if (viewing == Renderer.RULER)
				chekruler.setSelected(true);
			chekruler.addActionListener(this);
			item.add(chekruler);

			JMenu reset = new JMenu("Select View");
			reset.setIcon(ImageUtils.getIcon("images/menus/view.png"));
			popup.add(reset);
			JMenuItem resetitem = new JMenuItem("Top");
			resetitem.setActionCommand("resettop");
			resetitem.addActionListener(this);
			reset.add(resetitem);
			resetitem = new JMenuItem("Right");
			resetitem.setActionCommand("resetright");
			resetitem.addActionListener(this);
			reset.add(resetitem);
			resetitem = new JMenuItem("Back");
			resetitem.setActionCommand("resetback");
			resetitem.addActionListener(this);
			reset.add(resetitem);
			resetitem = new JMenuItem("User");
			resetitem.setActionCommand("resetuser");
			resetitem.addActionListener(this);
			reset.add(resetitem);
			resetitem = new JMenuItem("Custom View...");
			resetitem.setActionCommand("custommenu");
			resetitem.addActionListener(this);
			reset.add(resetitem);

			/*
			 * item = new JMenu("Editing..."); popup.add(item);
			 */
			popup.add(reset);

			JMenu edit = new JMenu("Settings");
			edit.setIcon(ImageUtils.getIcon("images/menus/configure.png"));
			
			JCheckBoxMenuItem seaflatonoff = new JCheckBoxMenuItem("Sea Flat");
			seaflatonoff.setActionCommand("sea flat");
			seaflatonoff.setSelected(seaflat);
			seaflatonoff.addActionListener(this);
			edit.add(seaflatonoff);

			if (mode == NEPTUS_MODE) {
				edit.addSeparator();
				JCheckBoxMenuItem transponoff = new JCheckBoxMenuItem(
						"Transparency");
				transponoff.setActionCommand("transparency");
				transponoff.setSelected(transp);
				transponoff.addActionListener(this);
				edit.add(transponoff);
				JCheckBoxMenuItem map = new JCheckBoxMenuItem("Map");
				map.setActionCommand("map");
				map.setSelected(mapa);
				map.addActionListener(this);
				edit.add(map);
			}

			edit.addSeparator();

			JMenu back = new JMenu("Background");

			edit.add(back);
			chekm = new JCheckBoxMenuItem("Solid Color");
			chekm.setActionCommand("backsolid");
			chekm.addActionListener(this);
			if (background == 1)
				chekm.setSelected(true);
			back.add(chekm);

			chekm = new JCheckBoxMenuItem("Sky texture");
			chekm.setActionCommand("backsky");
			chekm.addActionListener(this);
			if (background == 2)
				chekm.setSelected(true);
			back.add(chekm);

			chekm = new JCheckBoxMenuItem("Sky/Ground texture");
			chekm.setActionCommand("backskyground");
			chekm.addActionListener(this);
			if (background == 3)
				chekm.setSelected(true);
			back.add(chekm);

			popup.add(edit);

			JMenu grid = new JMenu("Grid");
			JCheckBoxMenuItem gridonoff = new JCheckBoxMenuItem("grid");
			gridonoff.setActionCommand("grid");
			gridonoff.setSelected(this.grid);
			gridonoff.addActionListener(this);
			grid.add(gridonoff);

			if (mode == VIEWER_MODE) {
				JCheckBoxMenuItem showAxis = new JCheckBoxMenuItem("Axis");
				showAxis.setActionCommand("Axis");
				showAxis.setSelected(this.axis);
				showAxis.addActionListener(this);
				grid.add(showAxis);
			}

			JMenuItem menugrid = new JMenuItem("Menu Grid");
			menugrid.setActionCommand("menugrid");
			menugrid.addActionListener(this);
			grid.add(menugrid);
			grid.setIcon(ImageUtils.getIcon("images/menus/grid.png"));

			popup.add(grid);

			if (mode == NEPTUS_MODE) {
				item = new JMenu("Lock");
				item.setIcon(ImageUtils.getIcon("images/menus/lock.png"));
				for (Enumeration<VehicleType> enuma = vehicles.keys(); enuma
						.hasMoreElements();) {
					VehicleType vt = (VehicleType) enuma.nextElement();

					final JCheckBoxMenuItem chek = new JCheckBoxMenuItem(vt
							.getName());
					chek.setName(vt.getId());
					chek.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							// System.err.println("A fixar : "+chek.getName());
                            if (VehiclesHolder.getVehicleById(chek.getName()) == null
                                    || (lockedVehicle != null && lockedVehicle.equals(VehiclesHolder.getVehicleById(
                                            chek.getName()).getId()))) {
								followVehicle(null);
                            }
							else {
								// followVehicle(null);
                                followVehicle(VehiclesHolder.getVehicleById(chek.getName()).getId());
							}
						}
					});

					item.add(chek);
					if (cams[panel_op].lock == vt)
						chek.setSelected(true);
					veicle_op = vt;

					// chek.setActionCommand("lock");
					// chek.addActionListener(this);

				}

				popup.add(item);

				item = new JMenu("Infos");
				item.setIcon(ImageUtils.getIcon("images/menus/comment.png"));
				popup.add(item);
				chekm = new JCheckBoxMenuItem("Camera");
				chekm.setActionCommand("infocamera");
				chekm.addActionListener(this);
				if (cams[panel_op].canvas.caminfo)
					chekm.setSelected(true);
				item.add(chekm);
				chekz = new JCheckBoxMenuItem("Axis");
				chekz.setActionCommand("infoaxis");
				if (cams[panel_op].canvas.axisinfo)
					chekz.setSelected(true);
				chekz.addActionListener(this);
				// chekz.setEnabled(false);
				item.add(chekz);

				chekz = new JCheckBoxMenuItem("Objects");
				chekz.setActionCommand("infoobj");
				if (cams[panel_op].canvas.objinfo)
					chekz.setSelected(true);
				chekz.addActionListener(this);
				// chekz.setEnabled(false);
				item.add(chekz);

				chekr = new JCheckBoxMenuItem("Vehicle");
				chekr.setActionCommand("infoveicle");
				if (cams[panel_op].canvas.veicleinfo)
					chekr.setSelected(true);
				chekr.addActionListener(this);
				// chekr.setEnabled(false);
				item.add(chekr);

				JCheckBoxMenuItem chekiconso = new JCheckBoxMenuItem(
						"Objects Icons");
				chekiconso.setActionCommand("iconsobj");
				if (cams[panel_op].canvas.objsicons)
					chekiconso.setSelected(true);
				chekiconso.addActionListener(this);
				// chekr.setEnabled(false);
				item.add(chekiconso);

				JCheckBoxMenuItem chekiconsv = new JCheckBoxMenuItem(
						"Vehicle Icons");
				chekiconsv.setActionCommand("iconsvehicle");
				if (cams[panel_op].canvas.vehicleicons)
					chekiconsv.setSelected(true);
				chekiconsv.addActionListener(this);
				// chekr.setEnabled(false);
				item.add(chekiconsv);

				item = new JMenu("Properties");
				item.setIcon(ImageUtils.getIcon("images/menus/list.png"));
				for (Enumeration<AbstractElement> enuma = objects.keys(); enuma
						.hasMoreElements();) {
					AbstractElement vt = (AbstractElement) enuma.nextElement();
					JMenuItem iten = new JMenuItem(vt.getId());
					item.add(iten);
					// if(cams[panel_op].lock==vt)
					// chek.setSelected(true);
					iten.setActionCommand("propreties" + vt.getId());
					iten.addActionListener(this);
				}
			}

			popup.add(item);
			if (console != null) {
				item = new JMenu("Projection");
				item.setIcon(ImageUtils.getIcon("images/menus/camera.png"));
				// popup.add(item);

				for (Enumeration<VehicleType> enuma = vehicles.keys(); enuma
						.hasMoreElements();) {
					VehicleType vt = (VehicleType) enuma.nextElement();

					final JMenuItem chek = new JMenuItem(vt.getName());
					chek.setName(vt.getId());
					chek.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							if (getConsole().getSubPanelsOfClass(
									JVideoPanelConsole.class).isEmpty()) {
								GuiUtils
										.errorMessage(
												null,
												"Console error (render projection)",
												"No JMF Video Panels activated in console");
							} else {
								VehicleType vsProj = VehiclesHolder
										.getVehicleById(chek.getName());
								ProjectionObj projObj = null;
								for (ProjectionObj po : projections) {
									if (po.getVehicle().getId().equals(
											vsProj.getId()))
										projObj = po;
								}
								if (projObj == null) {
									projObj = new ProjectionObj(
											Renderer3D.this, vsProj);
									projObj.setGridResolutionH(2);
									projObj.setGridResolutionV(2);
									projections.add(projObj);
								}
								PropertiesEditor.editProperties(
										new ProjectionPropertiesProvider(
												projObj), true);
								// if(projObj.getVideoSource()!=null)
								// {
								// NeptusLog.pub().info("<###>grabFrame:"+projObj.getVideoSource().grabFrameImage());
								// }
							}

						}
					});

					item.add(chek);
				}
				/*
				 * final JMenuItem chek = new JMenuItem("test ray");
				 * chek.setName("test ray"); chek.addActionListener(new
				 * java.awt.event.ActionListener() { public void
				 * actionPerformed(java.awt.event.ActionEvent e) {
				 * pickTool.setShapeRay(new Point3d(0,0,-10),new
				 * Vector3d(10,10,10)); //pickTool. PickResult result =
				 * pickTool.pickClosest(); if (result == null) {
				 * 
				 * NeptusLog.pub().info("<###>---Nothing picked---");
				 * 
				 * } else { NeptusLog.pub().info("<###>--------------- picked---");
				 * //result.setFirstIntersectOnly(true);
				 * //NeptusLog.pub().info("<###> "+result);
				 * 
				 * 
				 * //NeptusLog.pub().info("<###>Coordinates:"+result.getClosestIntersection
				 * (new Point3d(-10,0.1,0)).getPointCoordinates());
				 * System.out.println
				 * ("Coordinates to world:"+result.getClosestIntersection(new
				 * Point3d(0,0,-10)).getPointCoordinatesVW());
				 * 
				 * 
				 * Primitive p =
				 * (Primitive)result.getNode(PickResult.PRIMITIVE); Sphere
				 * sph=new Sphere(1.0f, Sphere.GENERATE_NORMALS|
				 * Sphere.GENERATE_TEXTURE_COORDS,null); TransformGroup ts=new
				 * TransformGroup(); Transform3D trs=new Transform3D(); Point3d
				 * pt=result.getClosestIntersection(new
				 * Point3d(0,0,-10)).getPointCoordinatesVW();
				 * trs.setTranslation(new Vector3d(pt.x,pt.y,pt.z));
				 * 
				 * BranchGroup ss=new BranchGroup(); ss.addChild(ts);
				 * ts.setTransform(trs); ts.addChild(sph);
				 * Renderer3D.this.contentsNoPickTransGr.addChild(ss);
				 * 
				 * Shape3D s = (Shape3D)result.getNode(PickResult.SHAPE3D);
				 * 
				 * if (p != null) {
				 * 
				 * NeptusLog.pub().info("<###> "+p.getClass().getName());
				 * 
				 * } else if (s != null) {
				 * 
				 * NeptusLog.pub().info("<###> "+s.getClass().getName());
				 * 
				 * } else{
				 * 
				 * NeptusLog.pub().info("<###>null");
				 * 
				 * }
				 * 
				 * } NeptusLog.pub().info("<###>--------------- end  picked---");
				 * //Sphere s=new Sphere(1.0f, Sphere.GENERATE_NORMALS| //
				 * Sphere.GENERATE_TEXTURE_COORDS,null); //TransformGroup ts=new
				 * TransformGroup(); //Transform3D trs=new Transform3D();
				 * //trs.setTranslation(new Vector3d(0,0,-10)); //BranchGroup
				 * ss=new BranchGroup(); //ss.addChild(ts);
				 * //ts.setTransform(trs); //ts.addChild(s);
				 * //Renderer3D.this.contentsTransGr.addChild(ss); } });
				 * item.add(chek);
				 */

				popup.add(item);

			}

			if (console != null) {
				item = new JMenu("Sonar display");
				item.setIcon(ImageUtils.getIcon("images/menus/sensor.png"));
				// popup.add(item);
				for (Enumeration<VehicleType> enuma = vehicles.keys(); enuma
						.hasMoreElements();) {
					VehicleType vt = (VehicleType) enuma.nextElement();
					
					final JMenuItem chek = new JMenuItem(vt.getName());
					chek.setName(vt.getId());

					chek.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							 
								VehicleType vsProj = VehiclesHolder
										.getVehicleById(chek.getName());
								SensorObj sensorObj = null;
								for (SensorObj po : sensors) {
									if (po.getVehicle().getId().equals(
											vsProj.getId()))
										sensorObj = po;
								}
								if (sensorObj == null) {
									sensorObj = new SensorObj(Renderer3D.this, vsProj);
									sensors.add(sensorObj);
								}
								PropertiesEditor.editProperties(
										new SensorPropertiesProvider(
												sensorObj), true);
								// if(projObj.getVideoSource()!=null)
								// {
								// NeptusLog.pub().info("<###>grabFrame:"+projObj.getVideoSource().grabFrameImage());
								// }
							

						}
					});
					
					
					
					item.add(chek);
				}

				popup.add(item);
			}

			item = new JMenuItem("R3D Shortcuts");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent arg0) {
					GuiUtils
							.htmlMessage(
									ConfigFetch.getSuperParentFrame(),
									"3D Renderer Shortcuts",
									"(Keys pressed while the Renderer component is focused)",
									"<html><h1>3D Renderer Shortcuts </h1><table border='1' align='center'>"
											+ "<tr><td><div align='center'><strong>Key Combination</strong></div></td>"
											+ "<td><div align='center'><strong>Action</strong></div></td></tr>"
											+ "<tr><td>F1</td><td>Reset to Top View(x,y)</td></tr>"
											+ "<tr><td>F2</td><td>Reset to Right View(z,x)</td></tr>"
											+ "<tr><td>F3</td><td>Reset to Front View(z,y)</td></tr>"
											+ "<tr><td>F4</td><td>Reset to Perspective View</td></tr>"
											+ "<tr><td>Ctrl+R</td><td>Rotation Control Mode </td></tr>"
											+ "<tr><td>Ctrl+M or Ctrl+T</td><td>Translation Control Mode</td></tr>"
											+ "<tr><td>Ctrl+Z</td><td>Zoom Control Mode</td></tr>"
											+ "<tr><td>Shift+Mouse Wheel </td><td>Move Back and Front clipping area</td></tr></table></html>");
				}
			});
			item.setIcon(ImageUtils.getIcon("images/menus/info.png"));
			popup.add(item);

			popup.show(views[panel_op], e.getX(), e.getY());

			/*
			 * PlasticLookAndFeel.setMyCurrentTheme(new
			 * com.jgoodies.looks.plastic.theme.SkyBlue()); try {
			 * UIManager.put("ClassLoader",
			 * LookUtils.class.getClass().getClassLoader());
			 * UIManager.setLookAndFeel(new PlasticXPLookAndFeel()); }
			 * catch(Exception ex){ }
			 */

		}
	}

	public Point3d fireRay(Point3d p, Vector3d dir) {
		pickTool.setShapeRay(p, dir);
		PickResult result = pickTool.pickClosest();
		if (result == null) {
			return null;
		} else {
			return result.getClosestIntersection(p).getPointCoordinatesVW();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

		// NeptusLog.pub().info("<###>arastou");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {

		if (e.getButton() == MouseEvent.BUTTON1) {
			// NeptusLog.pub().info("<###>press1");
			mouse1 = true;
			mousex = e.getX();
			mousey = e.getY();
		}
		if (e.getButton() == MouseEvent.BUTTON3) {
			// NeptusLog.pub().info("<###>press1");
			mouse3 = true;
			mousex = e.getX();
			mousey = e.getY();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
		// // TODO Auto-generated method stub
		// NeptusLog.pub().info("<###>rel");
		if (e.getButton() == MouseEvent.BUTTON1) {
			// NeptusLog.pub().info("<###>rel1");
			mouse1 = false;
		}
		if (e.getButton() == MouseEvent.BUTTON3) {
			// NeptusLog.pub().info("<###>rel1");
			mouse3 = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent
	 * )
	 */
	/* private void move */
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		// if (e.isAltDown()) {
		if (mouse1) {
			if (viewing == 3)
				for (short i = 0; i < NVIEWS; i++)
					if (e.getSource() == cams[i].canvas) {
						int mx = e.getX(), my = e.getY();

						if (e.isShiftDown()
								|| cams[i].projection == View.PERSPECTIVE_PROJECTION) {
							cams[i].setPsi(cams[i].psi + ((mousex - mx) / 50.));
							cams[i].setTheta(cams[i].theta
									+ ((mousey - my) / 50.));
						} else {
							cams[i].setPhi(cams[i].phi + ((mousex - mx) / 50.));
						}
						mousex = mx;
						mousey = my;
					}
			if (viewing == 1) //
				for (short i = 0; i < NVIEWS; i++)
					if (e.getSource() == cams[i].canvas) {
						int auxx = views[i].getWidth();
						int auxy = views[i].getHeight();
						int mx = e.getX(), my = e.getY();
						int difx = mousex - mx;
						int dify = mousey - my;
						switch (cams[i].type) {
						case Camera3D.TOP: {
							if (!e.isShiftDown()) {
								Vector3d vec = cams[i].pivot;
								Vector3d vaux = new Vector3d();
								vaux.y = 1;
								vaux.x = 1;

								vaux.y = difx / ((cams[i].scale / 2)) / (auxx);
								vaux.x = -dify / ((cams[i].scale / 1.51))
										/ (auxy);

								Vector3d vaux2 = new Vector3d();
								vaux2.x = (vaux.x * Math.cos(-cams[i].phi))
										- (vaux.y * Math.sin(-cams[i].phi));
								vaux2.y = (vaux.x * Math.sin(-cams[i].phi))
										+ (vaux.y * Math.cos(-cams[i].phi));

								// NeptusLog.pub().info("<###> "+cams[i].phi);
								vec.y += vaux2.y;
								vec.x += vaux2.x;
								// vec.y += difx / ((cams[i].scale/2))/(auxx);
								// vec.x += -dify /
								// ((cams[i].scale/1.51))/(auxy);
								cams[i].setPivot(vec);

							} else {
								Vector3d vec = cams[i].pivot;
								vec.z += dify / (cams[i].scale * 150.);
								cams[i].setPivot(vec);
							}
							break;
						}
						case Camera3D.RIGHT: {
							if (!e.isShiftDown()) {

								Vector3d vec = cams[i].pivot;

								Vector3d vaux = new Vector3d();
								vaux.x = 1;
								vaux.z = 1;

								vaux.x = -difx / ((cams[i].scale / 2)) / (auxx);
								vaux.z = -dify / ((cams[i].scale / 1.51))
										/ (auxy);

								Vector3d vaux2 = new Vector3d();
								vaux2.x = (vaux.x * Math.cos(-cams[i].phi))
										- (vaux.z * Math.sin(-cams[i].phi));
								vaux2.z = (vaux.x * Math.sin(-cams[i].phi))
										+ (vaux.z * Math.cos(-cams[i].phi));

								vec.x += vaux2.x;
								vec.z += vaux2.z;
								// vec.x += difx / ((cams[i].scale/2))/(auxx);
								// vec.z += dify /
								// ((cams[i].scale/1.51))/(auxy);

								cams[i].setPivot(vec);

							} else {
								Vector3d vec = cams[i].pivot;
								vec.y += dify / (cams[i].scale * 150.);
								cams[i].setPivot(vec);
							}
							break;
						}
						case Camera3D.BACK: {
							if (!e.isShiftDown()) {
								Vector3d vec = cams[i].pivot;

								Vector3d vaux = new Vector3d();
								vaux.y = 1;
								vaux.z = 1;

								vaux.y = -difx / ((cams[i].scale / 2)) / (auxx);
								vaux.z = -dify / ((cams[i].scale / 1.51))
										/ (auxy);

								Vector3d vaux2 = new Vector3d();
								vaux2.y = (vaux.y * Math.cos(-cams[i].phi))
										- (vaux.z * Math.sin(-cams[i].phi));
								vaux2.z = (vaux.y * Math.sin(-cams[i].phi))
										+ (vaux.z * Math.cos(-cams[i].phi));

								vec.y += vaux2.y;
								vec.z += vaux2.z;
								// vec.y += difx / ((cams[i].scale/2))/(auxx);
								// vec.z += dify /
								// ((cams[i].scale/1.51))/(auxy);

								cams[i].setPivot(vec);

							} else {
								Vector3d vec = cams[i].pivot;
								vec.x += dify / (cams[i].scale * 150.);
								cams[i].setPivot(vec);
							}
							break;
						}
						case Camera3D.USER: {
							if (!e.isShiftDown()) {
								Vector3d vec = cams[i].pivot;
								Vector3d vaux = new Vector3d();
								vaux.y = 1;
								vaux.x = 1;

								vaux.y = -difx / (150 / (cams[i].rho));
								vaux.x = -dify / (150 / (cams[i].rho));

								Vector3d vaux2 = new Vector3d();
								vaux2.x = (vaux.y * Math.cos(cams[i].psi))
										- (vaux.x * Math.sin(cams[i].psi));
								vaux2.y = (vaux.y * Math.sin(cams[i].psi))
										+ (vaux.x * Math.cos(cams[i].psi));

								// NeptusLog.pub().info("<###> "+cams[i].phi);
								vec.y += vaux2.y;
								vec.x += vaux2.x;
								// vec.y += difx / (150/(cams[i].rho));
								// vec.x += -dify / (150/(cams[i].rho));

								cams[i].setPivot(vec);

							} else {
								Vector3d vec = cams[i].pivot;
								vec.z += dify / (150 / (cams[i].rho));
								cams[i].setPivot(vec);
							}
							break;
						}

						default: {
							if (!e.isShiftDown()) {
								Vector3d vec = cams[i].pivot;
								vec.x += difx / (cams[i].scale * 150.);
								vec.y += -dify / (cams[i].scale * 150.);

								cams[i].setPivot(vec);

							} else {
								Vector3d vec = cams[i].pivot;
								vec.z += dify / (cams[i].scale * 150.);
								cams[i].setPivot(vec);
							}

						}

						}
						mousex = mx;
						mousey = my;
					}

			if (viewing == 2)
				for (short i = 0; i < NVIEWS; i++)
					if (e.getSource() == cams[i].canvas) {
						int mx = e.getX(), my = e.getY();

						if (cams[i].projection == View.PERSPECTIVE_PROJECTION) {
							if (!e.isShiftDown())
								cams[i].setRho(cams[i].rho
										+ ((my - mousey) / 30.));
							else
								cams[i].setScale(cams[i].scale
										+ ((my - mousey) / 150.));
						} else {
							if (!e.isShiftDown())
								cams[i].setScale(cams[i].scale
										+ ((mousey - my) / 150.));
							else
								cams[i].setRho(cams[i].rho
										+ ((mousey - my) / 30.));

						}
						mousex = mx;
						mousey = my;
					}
			// NeptusLog.pub().info("<###>drag");
		}

		if (mouse3) {
			if (viewing == 3)
				for (short i = 0; i < NVIEWS; i++)
					if (e.getSource() == cams[i].canvas) {
						int mx = e.getX(), my = e.getY();
						cams[i].setPhi(cams[i].phi + ((mousex - mx) / 50.));

						mousex = mx;
						mousey = my;
					}

			if (viewing == 1) //
				for (short i = 0; i < NVIEWS; i++)
					if (e.getSource() == cams[i].canvas) {
						int mx = e.getX(), my = e.getY();
						@SuppressWarnings("unused")
						int difx = mousex - mx;
						int dify = mousey - my;
						switch (cams[i].type) {
						case Camera3D.TOP: {

							Vector3d vec = cams[i].pivot;
							vec.z += dify / (cams[i].scale * 150.);
							cams[i].setPivot(vec);

							break;
						}
						case Camera3D.RIGHT: {
							Vector3d vec = cams[i].pivot;
							vec.y += dify / (cams[i].scale * 150.);
							cams[i].setPivot(vec);
							break;
						}
						case Camera3D.BACK: {
							Vector3d vec = cams[i].pivot;
							vec.x += dify / (cams[i].scale * 150.);
							cams[i].setPivot(vec);

							break;
						}
						case Camera3D.USER: {
							Vector3d vec = cams[i].pivot;
							vec.z += dify / (150 / (cams[i].rho));
							cams[i].setPivot(vec);

							break;
						}

						default: {
							Vector3d vec = cams[i].pivot;
							vec.z += dify / (cams[i].scale * 150.);
							cams[i].setPivot(vec);

						}

						}
						mousex = mx;
						mousey = my;
					}

			if (viewing == 2)
				for (short i = 0; i < NVIEWS; i++)
					if (e.getSource() == cams[i].canvas) {
						int mx = e.getX(), my = e.getY();

						if (cams[i].projection == View.PERSPECTIVE_PROJECTION) {

							cams[i].setScale(cams[i].scale
									+ ((my - mousey) / 150.));
						} else {
							cams[i].setRho(cams[i].rho + ((mousey - my) / 30.));

						}
						mousex = mx;
						mousey = my;
					}
			// NeptusLog.pub().info("<###>drag");
		}
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		// NeptusLog.pub().info("<###>move");

		if (viewing == Renderer.RULER && e.getSource() == cams[panel_op].canvas)
			if (rule == 1) // arrastou e clicou
			{

				cams[panel_op].canvas.p2.x = e.getX();
				cams[panel_op].canvas.p2.y = e.getY();
				cams[panel_op].setPhi(cams[panel_op].phi);
			}
	}

	public void componentResized(ComponentEvent e) {
		// TODO Auto-generated method stub
		// for(int i=0;i<NVIEWS;i++)
		// cams[i].canvas.repaint();

	}

	/*
	 * private Shape3D pick(Canvas3D canvas, BranchGroup rootBranchGroup, int x,
	 * int y, Point3d pickedPoint) { PickObject po = new PickObject(canvas,
	 * rootBranchGroup); SceneGraphPath sgp = po.pickClosest(x, y,
	 * PickObject.USE_GEOMETRY); if(sgp!=null) { PickRay ray =
	 * (PickRay)po.generatePickRay(x,y); double distance[] = new double[1];
	 * Vector3d rayvect = new Vector3d(); for( int i=sgp.nodeCount()-1 ; i>=0 ;
	 * i-- ) { if(sgp.getNode(i) instanceof Shape3D) { Shape3D pickedShape =
	 * (Shape3D)sgp.getNode(i); if(pickedShape.intersect(sgp, ray, distance)) {
	 * ray.get(pickedPoint, rayvect); rayvect.scale(distance[0]);
	 * pickedPoint.add(rayvect); return pickedShape; } } } } return null; }
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.lsts.neptus.renderer2d.Renderer#focusLocation(pt.lsts.
	 * neptus.types.coord.AbstractLocationPoint)
	 */
	public void focusLocation(LocationType location) {
		// TODO Auto-generated method stub
		// location.getOffsetFrom();
		double d[] = new double[3];
		d = this.location.getOffsetFrom(location);

		for (Obj3D obj : objs3D) {
			double newpos2[] = obj.pos;
			newpos2[0] += d[0];
			newpos2[1] += d[1];
			newpos2[2] += d[2];
			obj.setPos(newpos2);
		}

		if (seaobj != null) {
			double newpos2[] = seaobj.pos;
			newpos2[0] += d[0];
			newpos2[1] += d[1];
			newpos2[2] += d[2];
			seaobj.setPos(newpos2);
		}

		if (seaflatobj != null) {
			double newpos2[] = seaflatobj.pos;
			newpos2[0] += d[0];
			newpos2[1] += d[1];
			newpos2[2] += d[2];
			seaflatobj.setPos(newpos2);
		}

		if (gridobj != null) {
			double newpos2[] = gridobj.pos;
			newpos2[0] += d[0];
			newpos2[1] += d[1];
			newpos2[2] += d[2];
			gridobj.setPos(newpos2);
		}

		for (Enumeration<VehicleType> enuma = vehicles.keys(); enuma
				.hasMoreElements();) {
			VehicleType vt = (VehicleType) enuma.nextElement();
			Obj3D v3D = (Obj3D) vehicles.get(vt);
			double newpos[] = v3D.pos;
			newpos[0] += d[0];
			newpos[1] += d[1];
			newpos[2] += d[2];
			v3D.setPos(newpos);
			// v3D.setPos(location.getOffsetFrom());

		}

		/*
		 * for (Enumeration enuma = objects.keys(); enuma.hasMoreElements();) {
		 * MapObject vt = (MapObject) enuma.nextElement(); Obj3D
		 * v3D=(Obj3D)vehicles.get(vt); double newpos[]=v3D.pos;
		 * newpos[0]-=d[0]; newpos[1]-=d[1]; newpos[2]-=d[2];
		 * v3D.setPos(newpos); //v3D.setPos(location.getOffsetFrom());
		 * 
		 * }
		 */

		// Obj3D[] objarray = new Obj3D[0];
		Iterator<Obj3D> it = objects.values().iterator();
		while (it.hasNext()) {
			Obj3D obj = (Obj3D) it.next();
			double newpos[] = obj.pos;
			newpos[0] += d[0];
			newpos[1] += d[1];
			newpos[2] += d[2];
			obj.setPos(newpos);
		}

		for (VehicleTailObj3D tailobj : VehiclesTails) {
			// Obj3D obj = (Obj3D) it.next();
			double newpos[] = tailobj.pos;
			newpos[0] += d[0];
			newpos[1] += d[1];
			newpos[2] += d[2];
			// tailobj.location.setLocation(location);
			tailobj.setPos(newpos);
		}

		this.location.setLocation(location);
		for (int i = 0; i < NVIEWS; i++) 
			cams[i].setType(cams[i].getType());
		
		/*
		 * Collection col=objects.values(); if (!col.isEmpty()) for (int i
		 * =0;i<objects.values().toArray().length;i++) objarray[i]= (Obj3D)
		 * objects.values().toArray()[i]; else NeptusLog.pub().info("<###>erro");
		 * for(int i=0;i<objarray.length;i++) { double newpos[]=objarray[i].pos;
		 * newpos[0]+=d[0]; newpos[1]+=d[1]; newpos[2]+=d[2];
		 * objarray[i].setPos(newpos); }
		 */
		// vehicles;
		/*
		 * it = vehicles.values().iterator(); while (it.hasNext()) { Obj3D obj =
		 * (Obj3D) it.next(); double newpos[]=obj.pos; newpos[0]+=d[0];
		 * newpos[1]+=d[1]; newpos[2]+=d[2]; obj.setPos(newpos); } /
		 * objarray=(Obj3D[]) vehicles.values().toArray(); for(int
		 * i=0;i<objarray.length;i++) { double newpos[]=objarray[i].pos;
		 * newpos[0]+=d[0]; newpos[1]+=d[1]; newpos[2]+=d[2];
		 * objarray[i].setPos(newpos); }
		 */

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.lsts.neptus.renderer2d.Renderer#focusObject(pt.lsts.neptus
	 * .mme.objects.MapObject)
	 */
	public void focusObject(AbstractElement mo) {
		// TODO Auto-generated method stub
		this.focusLocation(mo.getCenterLocation());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.lsts.neptus.mp.MapChangeListener#mapChanged(pt.lsts.neptus
	 * .mp.MapChangeEvent)
	 */

	private Cylinder foundCylinder(TransformGroup model) {
		Enumeration<?> childs = model.getAllChildren();
		
		while (childs.hasMoreElements()) {
			Object child = childs.nextElement();
			if (child instanceof Cylinder) {
				// NeptusLog.pub().info("<###>-------------FOUND-------------");
				return (Cylinder) child;
			} else if (child instanceof TransformGroup) {
				return foundCylinder((TransformGroup) child);
			}

		}
		return null;
	}

	public void setNoTrans() {

		Enumeration<AbstractElement> e = objects.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Obj3D obj = (Obj3D) objects.get(key);
			if (key instanceof EllipsoidElement) {
				TransformGroup model = obj.getModel3D();
				// Cylinder shape=foundCylinder(model);
				Sphere shape = Util3D.foundSphere(model);
				// NeptusLog.pub().info("<###> "+shape);
				Appearance appearance3 = shape.getAppearance();
				TransparencyAttributes trans = new TransparencyAttributes();
				// trans.setTransparency(0.3f);
				trans.setTransparencyMode(TransparencyAttributes.NONE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
				appearance3.setTransparencyAttributes(trans);
				// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
				shape.setAppearance(appearance3);
			}
			if (key instanceof CylinderElement) {
				TransformGroup model = obj.getModel3D();
				Cylinder shape = foundCylinder(model);
				// NeptusLog.pub().info("<###> "+shape);
				Appearance appearance3 = shape.getAppearance();
				TransparencyAttributes trans = new TransparencyAttributes();
				// trans.setTransparency(0.3f);
				trans.setTransparencyMode(TransparencyAttributes.NONE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
				appearance3.setTransparencyAttributes(trans);
				// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
				shape.setAppearance(appearance3);
			}
			if (key instanceof ParallelepipedElement) {
				TransformGroup model = obj.getModel3D();
				Box shape = Util3D.foundBox(model);
				// NeptusLog.pub().info("<###> "+shape);
				Appearance appearance3 = shape.getAppearance();
				TransparencyAttributes trans = new TransparencyAttributes();
				// trans.setTransparency(0.3f);
				trans.setTransparencyMode(TransparencyAttributes.NONE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
				appearance3.setTransparencyAttributes(trans);
				// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
				shape.setAppearance(appearance3);
			}
		}
	}

	public void setTrans() {
		Enumeration<AbstractElement> e = objects.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Obj3D obj = (Obj3D) objects.get(key);
			if (key instanceof EllipsoidElement) {
				TransformGroup model = obj.getModel3D();
				// Cylinder shape=foundCylinder(model);
				Sphere shape = Util3D.foundSphere(model);
				// NeptusLog.pub().info("<###> "+shape);
				Appearance appearance3 = shape.getAppearance();
				TransparencyAttributes trans = new TransparencyAttributes();
				trans.setTransparency(0.3f);
				trans.setTransparencyMode(TransparencyAttributes.NICEST);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
				appearance3.setTransparencyAttributes(trans);
				// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
				shape.setAppearance(appearance3);
			}
			if (key instanceof CylinderElement) {
				TransformGroup model = obj.getModel3D();
				Cylinder shape = foundCylinder(model);
				// NeptusLog.pub().info("<###> "+shape);
				Appearance appearance3 = shape.getAppearance();

				TransparencyAttributes trans = new TransparencyAttributes();
				trans.setTransparency(0.3f);
				trans.setTransparencyMode(TransparencyAttributes.NICEST);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
				appearance3.setTransparencyAttributes(trans);
				// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
				shape.setAppearance(appearance3);
			}
			if (key instanceof ParallelepipedElement) {
				TransformGroup model = obj.getModel3D();
				Box shape = Util3D.foundBox(model);
				// NeptusLog.pub().info("<###> "+shape);
				Appearance appearance3 = shape.getAppearance();
				TransparencyAttributes trans = new TransparencyAttributes();
				trans.setTransparency(0.3f);
				trans.setTransparencyMode(TransparencyAttributes.NICEST);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
				trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
				appearance3.setTransparencyAttributes(trans);
				// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
				shape.setAppearance(appearance3);
			}

		}

	}

	public void setTrans(AbstractElement obje) {
		setTrans(obje, 0.3f);
	}

	public void setTrans(AbstractElement obje, float transp) {
		if (transp >= 0.95f) {
			hide(obje);
			return;
		}
		if (transp <= 0.5f) {
			setNoTrans(obje);

		}
		unHide(obje);
		Enumeration<AbstractElement> e = objects.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Obj3D obj = (Obj3D) objects.get(key);
			if (key == obje) {
				if (key instanceof EllipsoidElement) {
					TransformGroup model = obj.getModel3D();
					// Cylinder shape=foundCylinder(model);
					Sphere shape = Util3D.foundSphere(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = new TransparencyAttributes();
					trans.setTransparency(transp);
					trans.setTransparencyMode(TransparencyAttributes.NICEST);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					appearance3.setTransparencyAttributes(trans);
					// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
					shape.setAppearance(appearance3);
				}
				if (key instanceof CylinderElement) {
					TransformGroup model = obj.getModel3D();
					Cylinder shape = foundCylinder(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();

					TransparencyAttributes trans = new TransparencyAttributes();
					trans.setTransparency(transp);
					trans.setTransparencyMode(TransparencyAttributes.NICEST);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					appearance3.setTransparencyAttributes(trans);
					// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
					shape.setAppearance(appearance3);
				}
				if (key instanceof ParallelepipedElement) {
					TransformGroup model = obj.getModel3D();
					Box shape = Util3D.foundBox(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = new TransparencyAttributes();
					trans.setTransparency(transp);
					trans.setTransparencyMode(TransparencyAttributes.NICEST);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					appearance3.setTransparencyAttributes(trans);
					// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
					shape.setAppearance(appearance3);
				}

				if (key instanceof ImageElement) {
					TransformGroup model = obj.getModel3D();
					Shape3D shape = Util3D.foundImageShape(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = appearance3
							.getTransparencyAttributes();
					trans.setTransparency(transp);
					trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
				}
			}

		}

	}

	public void setNoTrans(AbstractElement obje) {

		Enumeration<AbstractElement> e = objects.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Obj3D obj = (Obj3D) objects.get(key);
			if (key == obje) {

				if (key instanceof ImageElement) {
					/*
					 * TransformGroup model=obj.getModel3D(); //Cylinder
					 * shape=foundCylinder(model); Sphere
					 * shape=Util3D.foundImage(model);
					 * //NeptusLog.pub().info("<###> "+shape); Appearance
					 * appearance3=shape.getAppearance(); TransparencyAttributes
					 * trans=new TransparencyAttributes();
					 * //trans.setTransparency(0.3f);
					 * trans.setTransparencyMode(TransparencyAttributes.NONE);
					 * trans
					 * .setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
					 * trans
					 * .setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					 * appearance3.setTransparencyAttributes(trans);
					 * //System.out
					 * .println(shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE
					 * )); shape.setAppearance(appearance3);
					 */
				}
				if (key instanceof EllipsoidElement) {
					TransformGroup model = obj.getModel3D();
					// Cylinder shape=foundCylinder(model);
					Sphere shape = Util3D.foundSphere(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = new TransparencyAttributes();
					// trans.setTransparency(0.3f);
					trans.setTransparencyMode(TransparencyAttributes.NONE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					appearance3.setTransparencyAttributes(trans);
					// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
					shape.setAppearance(appearance3);
				}
				if (key instanceof CylinderElement) {
					TransformGroup model = obj.getModel3D();
					Cylinder shape = foundCylinder(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = new TransparencyAttributes();
					// trans.setTransparency(0.3f);
					trans.setTransparencyMode(TransparencyAttributes.NONE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					appearance3.setTransparencyAttributes(trans);
					// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
					shape.setAppearance(appearance3);
				}
				if (key instanceof ParallelepipedElement) {
					TransformGroup model = obj.getModel3D();
					Box shape = Util3D.foundBox(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = new TransparencyAttributes();
					// trans.setTransparency(0.3f);
					trans.setTransparencyMode(TransparencyAttributes.NONE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					appearance3.setTransparencyAttributes(trans);
					// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
					shape.setAppearance(appearance3);
				}

				if (key instanceof ImageElement) {
					TransformGroup model = obj.getModel3D();
					Shape3D shape = Util3D.foundImageShape(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = appearance3
							.getTransparencyAttributes();
					trans.setTransparencyMode(TransparencyAttributes.NONE);
				}
			}
		}
	}

	public boolean isTrans(AbstractElement obje) {
		Enumeration<AbstractElement> e = objects.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Obj3D obj = (Obj3D) objects.get(key);
			if (key == obje) {
				if (key instanceof EllipsoidElement) {
					TransformGroup model = obj.getModel3D();
					// Cylinder shape=foundCylinder(model);
					Sphere shape = Util3D.foundSphere(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = new TransparencyAttributes();

					trans = appearance3.getTransparencyAttributes();
					// trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);

					// NeptusLog.pub().info("<###> "+trans.getCapability(TransparencyAttributes.ALLOW_VALUE_READ));
					if (trans.getTransparency() > 0.)
						return true;
					else
						return false;

					// NeptusLog.pub().info("<###> "+shape.getCapability(Shape3D.ALLOW_APPEARANCE_WRITE));
					// shape.setAppearance(appearance3);
				}
				if (key instanceof CylinderElement) {
					TransformGroup model = obj.getModel3D();
					Cylinder shape = foundCylinder(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();

					TransparencyAttributes trans = new TransparencyAttributes();
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					trans = appearance3.getTransparencyAttributes();
					if (trans.getTransparency() > 0.)
						return true;
					else
						return false;

				}
				if (key instanceof ParallelepipedElement) {
					TransformGroup model = obj.getModel3D();
					Box shape = Util3D.foundBox(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = new TransparencyAttributes();
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					trans = appearance3.getTransparencyAttributes();
					if (trans.getTransparency() > 0.)
						return true;
					else
						return false;

				}

				if (key instanceof ImageElement) {
					TransformGroup model = obj.getModel3D();
					Shape3D shape = Util3D.foundImageShape(model);
					// NeptusLog.pub().info("<###> "+shape);
					Appearance appearance3 = shape.getAppearance();
					TransparencyAttributes trans = new TransparencyAttributes();
					trans
							.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					trans = appearance3.getTransparencyAttributes();
					if (trans.getTransparencyMode() == TransparencyAttributes.NONE)
						return false;
					else
						return true;

				}
			}

		}
		return false;
	}

	public void hide(AbstractElement obje) {
		Enumeration<AbstractElement> e = objects.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Obj3D obj = (Obj3D) objects.get(key);
			if (key == obje) {
				if (!(obj.hide)) {
					obj.hide = true;
					contentsTransGr.removeChild(obj.getFullObj3D());
				}

			}

		}

	}

	public void unHide(AbstractElement obje) {
		Enumeration<AbstractElement> e = objects.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Obj3D obj = (Obj3D) objects.get(key);
			if (key == obje) {
				if (obj.hide) {
					obj.hide = false;
					contentsTransGr.addChild(obj.getFullObj3D());
				}
			}
		}
	}

	public void reset() {
		for (int i = 0; i < NVIEWS; i++) {
			if (cams[i].lockmapobj != null) // se está presa e é o proprio
				lockView(i, null);
			// lockView(i,(Obj3D)vehicles.get(vehicle));

		}

		Enumeration<AbstractElement> e = objects.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Obj3D obj = (Obj3D) objects.get(key);
			contentsTransGr.removeChild(obj.getFullObj3D());
			// NeptusLog.pub().info("<###>removing object "+key);
		}
		objects.clear();

		// setMapGroup(mapChange.getMapGroup());
		// if (transp) // por defeito o obj vem transparente
		// {
		// settrans();
		// } else {
		// setNoTrans();
		// }

		for (short i = 0; i < NVIEWS; i++)
			cams[i].setPivot(new Vector3d(0, 0, 0));

	}

	public void mapChanged(MapChangeEvent mapChange) {

		// NeptusLog.pub().info("<###>MAp Changed!");

		// System.err
		// .println("||||||||||||||Mapa alterado 3D |||||||||||||||||||");
		// System.err.println("Mapa alterado 3D ");
		// System.err.flush();

		// rever bem isto

		// System.err.println("?????????????Numero de objectos: "+objects.size());
		// System.err.println("MapChangeEvent: mapChange"+mapChange.getEventType()+" no objecto "+mapChange.getChangedObject()+", ("+mapChange.getChangedObject().hashCode()+")");

		if (mapChange.getEventType() == MapChangeEvent.MAP_RESET) {
			// System.err.println("map reset, new map: "+mapChange.getMapGroup());
			if (mapChange.getMapGroup() == null)
				return;

			// libertar as cams que estão presas em objs do mapa
			for (int i = 0; i < NVIEWS; i++) {
				if (cams[i].lockmapobj != null) // se está presa e é o
												// proprio
					lockView(i, null);
				// lockView(i,(Obj3D)vehicles.get(vehicle));
			}

			Enumeration<AbstractElement> e = objects.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				Obj3D obj = (Obj3D) objects.get(key);
				contentsTransGr.removeChild(obj.getFullObj3D());
				// NeptusLog.pub().info("<###>removing object "+key);
			}
			objects.clear();

			setMapGroup(mapChange.getMapGroup());
			// if (transp) // por defeito o obj vem transparente
			// {
			// setTrans();
			// } else {
			// setNoTrans();
			// }

			// System.err.println("está aqui o -1 entrou no reset");
			return;
		}

		/*
		 * if (mapChange.getChangedObject() == null) return;
		 */
		if (mapChange.getEventType() == MapChangeEvent.OBJECT_CHANGED) {
			// NeptusLog.pub().info("<###>Object Changed!");
			lock.lock();
			AbstractElement objs = mapChange.getChangedObject();
			if (objs == null) {
				lock.unlock();
				return;
			}

			if (objs instanceof ScatterPointsElement) // -> Rui, isto não
														// funcionava no meu PC
														// por isso comentei
														// (ZP)
			// if (false) //-> Nos meus pcs dá bem
			{
				ScatterPointsElement objscatter = (ScatterPointsElement) objs;
				// System.err.println("O obj foi :\n Parcialmente aumentado ou reduzido \n ou normalmente alterado");

				if (objscatter.getLastAdded() == null
						&& objscatter.getLastRemoved() == null) // alteração
																// normal de
																// todos...
				{

					// System.err.println(" scatter Normal-------------------------------------");

					Obj3D obj; // falta testar se é caminho e já está
								// feito...
					obj = (Obj3D) objects.get(objs);
					// System.err.println("MapChangeEvent: "+mapChange.getEventType()+" no objecto "+mapChange.getChangedObject()+", ("+mapChange.getChangedObject().hashCode()+")");
					if (obj == null) {
						lock.unlock();
						return;
					}

					Obj3D aux = obj;// saber qual é o obj antigo para voltar a
									// prender as cams

					boolean transpaux = isTrans(objs);
					contentsTransGr.removeChild(obj.getFullObj3D());
					obj = objscatter.getFullObj3D(location);
					contentsTransGr.addChild(obj.getFullObj3D());
					objects.put(objs, obj);
					if (transpaux) // por defeito o obj vem transparente
					{
						setTrans(objs);
					} else {
						setNoTrans(objs);
					}

					// -----------------------camaras que estavam presas no obj
					// antigo prendem no novo
					for (int i = 0; i < NVIEWS; i++) {
						if (cams[i].lockobj == aux) // se está presa e é o
													// proprio
							lockView(i, obj);
						// lockView(i,(Obj3D)vehicles.get(vehicle));
					}
					// -----------------------

					lock.unlock();
					return;
				}
				// Se foi parcialmente
				/*
				 * if (objscatter.getLastremoved() != null) {
				 * //System.err.println(
				 * "scatter point removed------------------------------------------"
				 * );
				 * 
				 * Obj3D obj; //falta testar se é caminho e já está
				 * feito... obj = (Obj3D) objects.get(objs);
				 * obj.removeObj3DByRelativeLocation(objscatter
				 * .getLastRemoved()); }
				 */

				if (objscatter.getLastAdded() != null) {
					// System.err.println("scatter point added added------------------------------------------");
					// Obj3D obj; //falta testar se é caminho e já está
					// feito...

					// obj = (Obj3D) objects.get(objs);

					Obj3D obj = (Obj3D) objects.get(objs);

					if (obj != null) {
						Obj3D[] objarray;
						if (objscatter != null) {
							if (obj.getObj3DChildsLength() > objscatter
									.getNumberOfPoints() * 2) {
								// System.err.println("obj.getObj3DChildsLength():"+obj.getObj3DChildsLength());
								// System.err.println("objscatter.getNumberOfPoints()"+objscatter.getNumberOfPoints());
								obj.removeLastNObj3D(obj.getObj3DChildsLength()
										- objscatter.getNumberOfPoints() * 2);
							}

							objarray = objscatter.getLastAddedModel3D();
							if (objarray != null)
								if (objarray[0] != null)
									obj.addObj3D(objarray[0]);
						}
					}
					/*
					 * for(int i=1;i<objarray.length;i++) {
					 * //System.err.println(
					 * "<><<<><><><><>><><<entrou no ciclo------------------------------------------"
					 * );
					 * //obj.removeObj3DByRelativeLocation(objscatter.getLastadded
					 * (20)); //obj.addObj3D(objarray[i]); }
					 */
				}

				lock.unlock();
				return;
			}

			if (objs instanceof PathElement
					&& ((PathElement) objs).isFinished()) { // se é caminho e
															// está pronto
				Obj3D obj;
				obj = (Obj3D) objects.get(objs);
				if (obj == null) // foi alterado ao desenhar mas é novo
				{
					obj = new Obj3D();
					obj.setModel3D(Object3DCreationHelper.getModel3DForRender(objs)); //objs.getModel3D());
					obj
							.setPos(objs.getCenterLocation().getOffsetFrom(
									location));
					obj.setRoll(objs.getRollRad());
					obj.setPitch(objs.getPitchRad());
					obj.setYaw(objs.getYawRad());
					contentsTransGr.addChild(obj.getFullObj3D());
					objects.put(objs, obj);
					lock.unlock();
					return;
				} else {

					Obj3D aux = obj; // saber qual o obj antigo para por as cams
										// desse no novo

					contentsTransGr.removeChild(obj.getFullObj3D());
					obj = new Obj3D();
					obj.setModel3D(Object3DCreationHelper.getModel3DForRender(objs)); //objs.getModel3D());
					System.err.println("entro:" + mapChange.getChangeType());

					obj
							.setPos(objs.getCenterLocation().getOffsetFrom(
									location));
					obj.setRoll(objs.getRollRad());
					obj.setPitch(objs.getPitchRad());
					obj.setYaw(objs.getYawRad());
					// obj.setPickble();

					contentsTransGr.addChild(obj.getFullObj3D());
					objects.put(objs, obj);

					// -----------------------camaras que estavam presas no obj
					// antigo prendem no novo
					for (int i = 0; i < NVIEWS; i++) {
						if (cams[i].lockobj == aux) // se está presa e é o
													// proprio
							lockView(i, obj);
						// lockView(i,(Obj3D)vehicles.get(vehicle));
					}
					// -------------------------

					lock.unlock();
					return;
				}
			}

			if (!(objs instanceof PathElement)) {

				Obj3D obj; // falta testar se é caminho e já está feito...
				obj = (Obj3D) objects.get(objs);
				// System.err.println("MapChangeEvent: "+mapChange.getEventType()+" no objecto "+mapChange.getChangedObject()+", ("+mapChange.getChangedObject().hashCode()+")");
				if (obj == null) {
					lock.unlock();
					return;
				}

				Obj3D aux = obj;// saber qual é o obj antigo para voltar a
								// prender as cams
				if (mapChange.getChangeType().equals(MapChangeEvent.OBJECT_SCALED)
						|| mapChange.getChangeType().equals(MapChangeEvent.UNKNOWN_CHANGE)) {
					contentsTransGr.removeChild(obj.getFullObj3D());
					obj = new Obj3D();
					obj.setModel3D(Object3DCreationHelper.getModel3DForRender(objs)); //objs.getModel3D());
				}
				obj.setPos(objs.getCenterLocation().getOffsetFrom(location));
				obj.setRoll(objs.getRollRad());
				obj.setPitch(objs.getPitchRad());
				obj.setYaw(objs.getYawRad());
				if (mapChange.getChangeType().equals(MapChangeEvent.OBJECT_SCALED)
						|| mapChange.getChangeType().equals(MapChangeEvent.UNKNOWN_CHANGE)) {
					contentsTransGr.addChild(obj.getFullObj3D());
					objects.put(objs, obj);
				}

				setTrans(objs, objs.getTransparency() / 100.f);
				// if (transpaux) // por defeito o obj vem transparente
				// {
				// setTrans(objs);
				// } else {
				// setNoTrans(objs);
				// }

				// -----------------------camaras que estavam presas no obj
				// antigo prendem no novo
				for (int i = 0; i < NVIEWS; i++) {
					if (cams[i].lockobj == aux) // se está presa e é o
												// proprio
						lockView(i, obj);
					// lockView(i,(Obj3D)vehicles.get(vehicle));
				}
				// -----------------------

				lock.unlock();
				return;
			}
			lock.unlock();
			return;

		}
		if (mapChange.getEventType() == MapChangeEvent.OBJECT_ADDED) {
			// System.err.println("||||||||||||||Objecto adicionadono 3D |||||||||||||||||||");
			AbstractElement objs = mapChange.getChangedObject();

			if (objs == null)
				return;

			if (objects.containsKey(objs)) {
				// System.err.println("Tentativa de adicionar um objecto que já existente...");
				return;
			}

			if (objs instanceof ScatterPointsElement) {
				ScatterPointsElement objscatter = (ScatterPointsElement) objs;

				// boolean transpaux = isTrans(objs);

				Obj3D obj;
				obj = objscatter.getFullObj3D(location);
				contentsTransGr.addChild(obj.getFullObj3D());
				objects.put(objs, obj);

				return;
			}
			if (objs instanceof DynamicElement) {
				Obj3D obj = new Obj3D();
				TransformGroup m3d = Object3DCreationHelper.getModel3DForRender(objs);
				if (m3d == null)
					return;
				obj.setModel3D(m3d); //objs.getModel3D());
				obj.setPos(objs.getCenterLocation().getOffsetFrom(location));
				obj.setRoll(objs.getRollRad());
				obj.setPitch(objs.getPitchRad());
				obj.setYaw(objs.getYawRad());

				// System.err.println(contentsTransGr);
				// obj.setPickble();
				contentsTransGr.addChild(obj.getFullObj3D());
				objects.put(objs, obj);
				System.err.println("---------------");
				obj.drawinfo = true;
				return;
			}
			if (objs instanceof PathElement
					&& ((PathElement) objs).isFinished()) { // corrigir a flag
															// na class
				Obj3D obj = new Obj3D();
				obj.setModel3D(Object3DCreationHelper.getModel3DForRender(objs)); //objs.getModel3D());
				obj.setPos(objs.getCenterLocation().getOffsetFrom(location));
				obj.setRoll(objs.getRollRad());
				obj.setPitch(objs.getPitchRad());
				obj.setYaw(objs.getYawRad());
				contentsTransGr.addChild(obj.getFullObj3D());
				objects.put(objs, obj);
				return;
			}
			if (!(objs instanceof PathElement)) {
				Obj3D obj = new Obj3D();
				//if (objs.getModel3D() == null)
				TransformGroup m3d = Object3DCreationHelper.getModel3DForRender(objs);
				if (m3d == null)
					return;
				obj.setModel3D(m3d); //objs.getModel3D());
				obj.setPos(objs.getCenterLocation().getOffsetFrom(location));
				obj.setRoll(objs.getRollRad());
				obj.setPitch(objs.getPitchRad());
				obj.setYaw(objs.getYawRad());
				// System.err.println(contentsTransGr);
				// obj.setPickble();
				contentsTransGr.addChild(obj.getFullObj3D());
				// System.err.println("---------------");
				objects.put(objs, obj);
				// obj.drawinfo=true;
				// if (transp) // por defeito o obj vem transparente
				// {
				// setTrans();
				// } else {
				// setNoTrans();
				// }
				return;
			}
			return;
		}

		if (mapChange.getEventType() == MapChangeEvent.OBJECT_REMOVED) {

			// System.err.println("Remover o objecto ");
			AbstractElement objs = mapChange.getChangedObject();

			if (objs == null) {
				NeptusLog.pub()
						.warn("Map changed : Changed object is null! - " + this);
				return;
			}

			Obj3D obj;
			obj = (Obj3D) objects.get(objs);
			// System.err.println("MapChangeEvent: "+mapChange.getEventType()+" no objecto "+mapChange.getChangedObject()+", ("+mapChange.getChangedObject().hashCode()+")");
			// NeptusLog.pub().info("<###>Object="+obj);
			if (obj == null) {
				// System.err.println("TEntativa de remover um objecto não existente ("+objs.hashCode()+")");
				return;
			}
			// ------------------------------camara deixa de estár presa
			for (int i = 0; i < NVIEWS; i++) {
				if (cams[i] != null && cams[i].lockobj == obj) // se está
																// presa e é o
																// proprio
					lockView(i, null);
				// lockView(i,(Obj3D)vehicles.get(vehicle));
			}
			// --------------------------------
			if (contentsTransGr != null)
				contentsTransGr.removeChild(obj.getFullObj3D()); // tira do
																	// Java3D

			objects.remove(objs); // tira da hash
		}
	}

	/*--------------por acabar se é que vai ser preciso-------*/
	// private void newHomeRef(AbstractElement homeref) {
	//
	// for (Enumeration<AbstractElement> enuma = objects.keys();
	// enuma.hasMoreElements();) {
	// AbstractElement objs = (AbstractElement) enuma.nextElement();
	//
	// if (!(objs instanceof HomeReferenceElement)) {
	//
	// Obj3D obj2 = (Obj3D) objects.get(objs);
	// //................
	//
	// }
	//
	// }
	// }
	public void keyTyped(KeyEvent e) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		for (short i = 0; i < NVIEWS; i++)
			if (e.getSource() == cams[i].canvas) {
				if (KeyEvent.VK_F1 == (e.getKeyCode())) {
					cams[i].resetTop();
				}
				if (KeyEvent.VK_F2 == (e.getKeyCode())) {
					cams[i].resetRight();
				}
				if (KeyEvent.VK_F3 == (e.getKeyCode())) {
					cams[i].resetBack();
				}
				if (KeyEvent.VK_F4 == (e.getKeyCode())) {
					cams[i].resetUser();
				}
				if (e.isControlDown()) {
					if (KeyEvent.VK_R == (e.getKeyCode())) {
						viewing = Renderer.ROTATION;
						sendChangeEvent();
					}
					if (KeyEvent.VK_M == (e.getKeyCode())) {
						viewing = Renderer.TRANSLATION;
						sendChangeEvent();
					}
					if (KeyEvent.VK_V == (e.getKeyCode())) {
						viewing = Renderer.TRANSLATION;
						sendChangeEvent();
					}
					if (KeyEvent.VK_Z == (e.getKeyCode())) {
						viewing = Renderer.ZOOM;
						sendChangeEvent();
					}
					if (KeyEvent.VK_T == (e.getKeyCode())) {
						viewing = Renderer.TRANSLATION;
						sendChangeEvent();
					}
				}
				if (e.isAltDown()) {
					if (KeyEvent.VK_C == (e.getKeyCode())) {
						if (cams[panel_op].canvas.caminfo) {

							cams[panel_op].canvas.caminfo = false;
						} else {
							cams[panel_op].canvas.caminfo = true;
						}
					}
					if (KeyEvent.VK_M == (e.getKeyCode())) {
						viewing = 1;
						sendChangeEvent();
					}
					if (KeyEvent.VK_V == (e.getKeyCode())) {
						viewing = 1;
						sendChangeEvent();
					}
					if (KeyEvent.VK_Z == (e.getKeyCode())) {
						viewing = 2;
						sendChangeEvent();
					}
				}

				if (KeyEvent.VK_F5 == (e.getKeyCode())) {
					// System.err.println("leaving proj");
					for (ProjectionObj p : projections) {
						stop_vehicles = true;
						p.leaveProjection();
						stop_vehicles = false;
					}

				}
			}

	}

	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void setViewMode(int mode) {
		// final int TRANSLATION = 1, ZOOM = 2, ROTATION = 3, NONE = -1;
		// TODO Do this...
		viewing = (short) mode;
		if (rotateCursor == null) {
			loadCursors();
		}
		switch (mode) {
		case (Renderer.ROTATION):
			for (int i = 0; i < NVIEWS; i++)
				views[i].setCursor(rotateCursor);
			break;
		case (Renderer.TRANSLATION):
			for (int i = 0; i < NVIEWS; i++)
				views[i].setCursor(translateCursor);
			break;
		case (Renderer.ZOOM):
			for (int i = 0; i < NVIEWS; i++)
				views[i].setCursor(zoomCursor);
			break;
		case (Renderer.RULER):
			for (int i = 0; i < NVIEWS; i++)
				views[i].setCursor(crosshairCursor);
			break;
		default: {
			for (int i = 0; i < NVIEWS; i++)
				views[i].setCursor(Cursor
						.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		}

	}

	public String getLockedVehicle() {
		return lockedVehicle;
	}

	public void removeAllVehicles() {

		contentsNoPickTransGr.removeAllChildren();
		vehicles.clear();
		// NeptusLog.pub().info("<###>removeu Todos");
	}

	public void removeVehicle(VehicleType vehicle) {
		// removeObj3D((Obj3D) vehicles.get(vehicle));
		if ((Obj3D) vehicles.get(vehicle) != null)
			contentsNoPickTransGr.removeChild(((Obj3D) vehicles.get(vehicle))
					.getFullObj3D());
		vehicles.remove(vehicle);
		// NeptusLog.pub().info("<###>removeu");
	}

	public void followVehicle(String system) {

		if (lockedVehicle != null && lockedVehicle.equals(system))
			return;

		if (lockedVehicle != null && !lockedVehicle.equals(system))
			for (int i = 0; i < NVIEWS; i++)
				if (cams[i].lock != null)
					lockView(i, null);

		lockedVehicle = system;
		// Seguir o veículo dado.
		if (system == null) {
			for (int i = 0; i < NVIEWS; i++) {
				if (cams[i].lock != null)
					lockView(i, null);
			}
		} else {
			for (int i = 0; i < NVIEWS; i++) {
			    VehicleType vehicle = VehiclesHolder.getVehicleById(system);
			    if (vehicle != null) {
			        lockView(i, (Obj3D) vehicles.get(vehicle));
			        cams[i].setType(cams[i].getType());
			    }
			}
		}

		// vehicle_lock_interface = vehicle;

	}

	private Vector<ChangeListener> changeListeners = new Vector<ChangeListener>();

	public void addChangeListener(ChangeListener cl) {
		changeListeners.add(cl);
	}

	public void removeChangeListener(ChangeListener cl) {
		changeListeners.remove(cl);
	}

	public int getShowMode() {
		return (int) viewing;
	}

	private void sendChangeEvent() {
		for (int i = 0; i < changeListeners.size(); i++) {
			ChangeEvent ce = new ChangeEvent(this);
			((ChangeListener) changeListeners.get(i)).stateChanged(ce);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seejava.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.
	 * MouseWheelEvent)
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {

		for (short i = 0; i < NVIEWS; i++)
			if (e.getSource() == cams[i].canvas) {
				int mw = e.getWheelRotation();

				if (cams[i].projection == View.PERSPECTIVE_PROJECTION) {
					if (!e.isShiftDown())
						cams[i].setRho(cams[i].rho
								+ ((mw * (cams[i].rho / 1.1)) / 5.));
					else
						cams[i].setScale(cams[i].scale + ((mw * 10) / 150.));
				} 
				else {
					if (!e.isShiftDown())
						cams[i].setScale(cams[i].scale
								+ ((-mw * 10) * (cams[i].scale / 100.)));
					else
						cams[i].setRho(cams[i].rho
								+ ((-mw * (cams[i].rho / 1.1)) / 30.));

				}
			}
	}

	private JDialog dialog;

	public void getDialog(String title) {
		dialog = new JDialog(new JFrame(), title);
		dialog.getContentPane().add(this);
		dialog.setSize(getWidth() + 5, getHeight() + 80);
		dialog.setModal(true);
		dialog.setAlwaysOnTop(true);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// userCancel = true;
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		GuiUtils.centerOnScreen(dialog);
		// this.add(getControlsPanel(), java.awt.BorderLayout.SOUTH);
		JPanel controlsPanel = new JPanel();
		this.add(controlsPanel);
		dialog.setVisible(true);
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	Obj3D axisobj = new Obj3D(Util3D.makeAxis(true, 10.));

	public void showAxis(boolean flag) {
		if (flag)
			this.addObj3D(axisobj);
		else
			this.removeObj3D(axisobj);
		axis = flag;

	}

	public void setObjsIcons(boolean b) {
		for (short i = 0; i < NVIEWS; i++) {

			cams[i].canvas.objsicons = b;

		}
	}

	public void setVehiclesIcons(boolean b) {
		for (short i = 0; i < NVIEWS; i++) {

			cams[i].canvas.vehicleicons = b;

		}
	}

	protected void updatePreferencies() {
		priority3D = GeneralPreferences.renderer3DPriority;

		int np = 500;
		np = GeneralPreferences.numberOfShownPoints;
		for (VehicleTailElement vte : vehicleTails.values())
			vte.setNumberOfPoints(np);
	}

	public void preferencesUpdated() {

		updatePreferencies();

	}

	public void setVehicleIcons(boolean flag) {
		for (int i = 0; i < NVIEWS; i++) {
			cams[i].canvas.vehicleicons = flag;

		}

	}

	private boolean stopped = false;

	@Override
	public void setVisible(boolean aFlag) {
		super.setVisible(aFlag);
		if (!isVisible() || getWidth() <= 0 && !stopped) {
			stop();
			stopped = true;
		} else {
			start();
			stopped = false;
		}
	}

	boolean renderingStop = false;

	public void stop() {
		if (renderingStop)
			return;
		for (int i = 0; i < NVIEWS; i++)
			cams[i].canvas.stopRenderer();
		renderingStop = true;
	}

	public void start() {
		if (!renderingStop)
			return;
		for (int i = 0; i < NVIEWS; i++)
			cams[i].canvas.startRenderer();
		renderingStop = false;
	}

	public ConsoleLayout getConsole() {
		return console;
	}

	public void setConsole(ConsoleLayout console) {
		this.console = console;
	}

	public void cleanup() {

		if (clean)
			return;

		if (cams[0] == null)
			return;

		freeze();
		for (short i = 0; i < NVIEWS; i++) {

			viewTransGr.removeChild(cams[i].getCamera3D());
			cams[i].cleanup();
			cams[i] = null;
			// cams[i].
		}

		// locale.removeBranchGraph(contentBranch);
		// locale.removeBranchGraph(viewBranch);

		// contentBranch.removeAllChildren();
		// contentBranch.detach();
		contentBranch = null;
		// contentsTransGr.removeAllChildren();

		contentsTransGr = null;
		// viewBranch.removeAllChildren();
		// viewBranch.detach();
		viewBranch = null;
		// viewTransGr.removeAllChildren();
		viewTransGr = null;
		VirtualUniverse.setJ3DThreadPriority(0);

		universe.removeAllLocales();

		// locale.getVirtualUniverse().
		// universe=null;

		System.gc();
		if (myMapGroup != null)
			myMapGroup.removeChangeListener(this);

		clean = true;
		Viewer.clearViewerMap();
		Primitive.clearGeometryCache(); // java3D 1.4.1
		GeneralPreferences.removePreferencesListener(this);

		// System.err.println("destroy");
	}

	public void destroy() {
		// ((SimpleUniverse)universe).cleanup();
		this.cleanup();
	}

	public BoundingSphere getBoundsSphere() {
		return bounds;
	}

	public void setBoundsSphere(BoundingSphere bounds) {
		this.bounds = bounds;
	}

	public void componentHidden(ComponentEvent e) {
		stop();
	}

	public void componentMoved(ComponentEvent e) {

	}

	public void componentShown(ComponentEvent e) {
		start();
	}

	public void clearVehicleTail(String[] vehiclesArray) {
		lock.lock();
		if (vehiclesArray == null) {
			for (Enumeration<VehicleType> enuma = vehicles.keys(); enuma
					.hasMoreElements();) {
				VehicleType vtype = (VehicleType) enuma.nextElement();
				for (VehicleTailObj3D vtailo : VehiclesTails) {
					if (vtailo.getVehicle() == vtype)
						vtailo.clearTail();
				}
			}
		} else {

			for (String sys : vehiclesArray) {
			    VehicleType vtype = VehiclesHolder.getVehicleById(sys);
				for (VehicleTailObj3D vtailo : VehiclesTails) {
					if (vtailo.getVehicle() == vtype)
						vtailo.clearTail();
				}
			}
		}
		lock.unlock();

		/*
		 * 
		 * if (vehicles == null) { for (VehicleTailElement vte :
		 * vehicleTails.values()) { vte.clearPoints(); MapChangeEvent mce = new
		 * MapChangeEvent( MapChangeEvent.OBJECT_CHANGED);
		 * mce.setMapGroup(getMapGroup()); mce.setChangedObject(vte);
		 * this.mapChanged(mce); } return; } for (VehicleType v : vehicles) {
		 * VehicleTailElement vte = vehicleTails.get(v); if (vte != null) {
		 * vte.clearPoints(); MapChangeEvent mce = new MapChangeEvent(
		 * MapChangeEvent.OBJECT_CHANGED); mce.setMapGroup(getMapGroup());
		 * mce.setChangedObject(vte); this.mapChanged(mce); } }
		 */
	}

	public void setVehicleTailOff(String[] vehiclesArray) {
		lock.lock();
		if (vehiclesArray == null) {
			for (Enumeration<VehicleType> enuma = vehicles.keys(); enuma
					.hasMoreElements();) {
				VehicleType vtype = (VehicleType) enuma.nextElement();
				VehicleTailObj3D confirm = null;
				for (VehicleTailObj3D vtailo : VehiclesTails) {
					if (vtailo.getVehicle() == vtype)
						confirm = vtailo;
				}
				if (confirm != null) {
					contentsNoPickTransGr.removeChild(confirm.getFullObj3D());
					VehiclesTails.remove(confirm);
					confirm.clean();
				}

			}
		} else {
			for (String sys : vehiclesArray) {
			    VehicleType vtype = VehiclesHolder.getVehicleById(sys);
				VehicleTailObj3D confirm = null;
				for (VehicleTailObj3D vtailo : VehiclesTails) {
					if (vtailo.getVehicle() == vtype)
						confirm = vtailo;
				}
				if (confirm != null) {
					contentsNoPickTransGr.removeChild(confirm.getFullObj3D());
					VehiclesTails.remove(confirm);
					confirm.clean();
				}
			}
		}
		lock.unlock();
		/*
		 * if (vehicles == null) { isAllTailOn = false; vehiclesTailOn.clear();
		 * for (VehicleTailElement vte : vehicleTails.values()) { MapChangeEvent
		 * mce = new MapChangeEvent( MapChangeEvent.OBJECT_REMOVED);
		 * mce.setMapGroup(getMapGroup()); mce.setChangedObject(vte);
		 * this.mapChanged(mce); } } else { if (isAllTailOn) { boolean oneOK =
		 * false; for (VehicleType v : vehicles) { if (vehiclesTailOn.remove(v))
		 * { oneOK = true;
		 * 
		 * vehiclesTailOn.add(v); VehicleTailElement vte = vehicleTails.get(v);
		 * if (vte != null) { MapChangeEvent mce = new MapChangeEvent(
		 * MapChangeEvent.OBJECT_REMOVED); mce.setMapGroup(getMapGroup());
		 * mce.setChangedObject(vte); this.mapChanged(mce); } } } if (oneOK)
		 * isAllTailOn = false; } }
		 */
	}

	public void setVehicleTailOn(String[] vehiclesArray) {
		lock.lock();
		if (vehiclesArray == null) {
			for (Enumeration<VehicleType> enuma = vehicles.keys(); enuma
					.hasMoreElements();) {
				VehicleType vtype = (VehicleType) enuma.nextElement();
				boolean confirm = false;
				for (VehicleTailObj3D vtailo : VehiclesTails) {
					if (vtailo.getVehicle() == vtype)
						confirm = true;
				}
				if (!confirm) {
					// System.err.println("entrou e vai criar");
					VehicleTailObj3D novoVTail = new VehicleTailObj3D(location);
					novoVTail.setVehicle(vtype);
					VehiclesTails.add(novoVTail);
					contentsNoPickTransGr.addChild(novoVTail.getFullObj3D());
				}

			}
		} else {
			for (String sys : vehiclesArray) {
			    VehicleType vtype = VehiclesHolder.getVehicleById(sys);
				boolean confirm = false;
				for (VehicleTailObj3D vtailo : VehiclesTails) {
					if (vtailo.getVehicle() == vtype)
						confirm = true;
				}
				if (!confirm) {
					VehicleTailObj3D novoVTail = new VehicleTailObj3D(location);
					novoVTail.setVehicle(vtype);
					VehiclesTails.add(novoVTail);
					contentsNoPickTransGr.addChild(novoVTail.getFullObj3D());
				}
			}
		}
		lock.unlock();
		/*
		 * if (true) return; //FIXME
		 * 
		 * if (vehicles == null) { isAllTailOn = true; vehiclesTailOn.clear();
		 * for (VehicleTailElement vte : vehicleTails.values()) { MapChangeEvent
		 * mce = new MapChangeEvent( MapChangeEvent.OBJECT_ADDED);
		 * mce.setMapGroup(getMapGroup()); mce.setChangedObject(vte);
		 * this.mapChanged(mce); } } else { if (!isAllTailOn) { for (VehicleType
		 * v : vehicles) { vehiclesTailOn.add(v); VehicleTailElement vte =
		 * vehicleTails.get(v); if (vte != null) { MapChangeEvent mce = new
		 * MapChangeEvent( MapChangeEvent.OBJECT_ADDED);
		 * mce.setMapGroup(getMapGroup()); mce.setChangedObject(vte);
		 * this.mapChanged(mce); } } } }
		 */
	}

}
