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
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.mp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.AboutPanel;
import pt.up.fe.dceg.neptus.gui.ChangeLog;
import pt.up.fe.dceg.neptus.gui.CoordinateSystemPanel;
import pt.up.fe.dceg.neptus.gui.IFrameOpener;
import pt.up.fe.dceg.neptus.gui.LatLonConv;
import pt.up.fe.dceg.neptus.gui.MissionBrowser;
import pt.up.fe.dceg.neptus.gui.MissionFileChooser;
import pt.up.fe.dceg.neptus.gui.MissionInfoPanel;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.VehicleChooser;
import pt.up.fe.dceg.neptus.gui.VehicleInfo;
import pt.up.fe.dceg.neptus.gui.checklist.ChecklistFileChooser;
import pt.up.fe.dceg.neptus.gui.checklist.ChecklistPanel;
import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.loader.FileHandler;
import pt.up.fe.dceg.neptus.mme.MissionMapEditor;
import pt.up.fe.dceg.neptus.planeditor.IndividualPlanEditor;
import pt.up.fe.dceg.neptus.renderer2d.MissionRenderer;
import pt.up.fe.dceg.neptus.types.checklist.ChecklistType;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.mission.ChecklistMission;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.VehicleMission;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.NameNormalizer;
import pt.up.fe.dceg.neptus.util.RecentlyOpenedFilesUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;
import pt.up.fe.dceg.neptus.util.output.OutputMonitor;
import pt.up.fe.dceg.neptus.util.output.OutputPanel;
import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;

/**
 * This is the most important class of the <b>Neptus MissionPlanner</b> project. 
 * This application will allow a user to specify a neptus mission and then deploy it as a
 * XML file or send it directly to the vehicle for launch.
 * @author Ze Carlos
 * @author pdias (small adds)
 */
public class MissionPlanner extends JFrame implements IFrameOpener, ChangeListener, ActionListener,
        WindowListener, InternalFrameListener, FileHandler {

	public static final long serialVersionUID = 1;
	public static final int CANCEL = 0, OK = 1, ERROR = 2;
	private int numFrames = 0;
	private JDesktopPane desktop;
	private Hashtable<String ,JComponent> menus = new Hashtable<String, JComponent>();
	private MapGroup myMapGroup;
	private MissionType myMissionType = null;
	private MissionBrowser mBrowser = null;
	
	private Vector<ChangeListener> changeListeners = new Vector<ChangeListener>();
	private LinkedHashMap<String, JInternalFrame> planFrames = new LinkedHashMap<String, JInternalFrame>(),
		mapFrames = new LinkedHashMap<String, JInternalFrame>(),
		vehicleFrames = new LinkedHashMap<String, JInternalFrame>(),
		checkListFrames = new LinkedHashMap<String, JInternalFrame>();
	
	private LinkedHashMap<String, PlanType> planTypes = new LinkedHashMap<String, PlanType>();
	private LinkedHashMap<String, IndividualPlanEditor> planEditors = new LinkedHashMap<String, IndividualPlanEditor>();
	 
	private LinkedHashMap<String, MapType> mapTypes = new LinkedHashMap<String, MapType>();
	private LinkedHashMap<String, MissionMapEditor> mapEditors = new LinkedHashMap<String, MissionMapEditor>();
	private LinkedHashMap<String, VehicleMission> vehicles = new LinkedHashMap<String, VehicleMission> ();
	
	private LinkedHashMap<String, ChecklistType> checkLists = new LinkedHashMap<String, ChecklistType>();
	private LinkedHashMap<String, ChecklistPanel> checkListEditors = new LinkedHashMap<String, ChecklistPanel>();
	
	private Vector<MissionChangeListener> missionListeners = new Vector<MissionChangeListener>();
	
	boolean missionChanged = false;
	String workingFile = null;
	
	private LinkedHashMap<JMenuItem, File> miscFilesOpened = new LinkedHashMap<JMenuItem, File>();
	private JMenu recentlyOpenFilesMenu = null;
	public final static String RECENTLY_OPENED_MISSIONS = "conf/mp_recent.xml";
	
	/**
	 * Constructor - builds the interface and shows it to the user
	 */
	public MissionPlanner() {
		super("Neptus Mission Planner");

		GuiUtils.setLookAndFeel();		
		createInterface();	
		
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Rectangle bounds = GuiUtils.getDefaultScreenBounds();
		this.setSize(bounds.width - 150, bounds.height - 150);
		//this.setIconImage(GuiUtils.getImage("images/neptus-icon.png"));
		this.setIconImages(ConfigFetch.getIconImagesForFrames());
		this.addWindowListener(this);
		GuiUtils.centerOnScreen(this);
		refreshMenus();
	}
	
	
	/**
	 * Constructor - builds the interface and shows it to the user
	 */
	public MissionPlanner(MissionType mt) {
		this();
		setMission(mt);
		refreshMenus();
	}
	
	
	/**
	 * Removes a possibly existing map with an ID equal to the Map given
	 * @param mt Will remove a Map that has the same ID as this.
	 */
	public void removeMap(MapType mt) {
		mapTypes.remove(mt.getId());
		getMapGroup().removeMap(mt.getId());
		mBrowser.removeItem(mt);
		setMissionChanged(true);
	}
	
	/**
	 * This method adds a new map to the current mission, generally,
	 * it will be called from children map editors when a map is opened
	 * @param newMap
	 */
	public void addMap(MapType newMap) {
		System.out.println("add map to mapTypes: "+newMap.getId());
		mapTypes.put(newMap.getId(), newMap);
		getMapGroup().addMap(newMap);
		mBrowser.addMap(newMap);
		
		setMissionChanged(true);
	}
	
	
	
	/**
	 * This method is called when the main application window is closed.
	 * Verifies if the user has unsaved changes and asks him what to do if there are
	 * any unsaved changes.
	 */
	public void windowClosing(WindowEvent arg0) {
		ActionEvent evt = new ActionEvent(this, ActionEvent.RESERVED_ID_MAX + 1, "quit");
		this.actionPerformed(evt);
	}

    /**
	 * The implementation of this function is required by the
	 * <b>WindowListener</b>. This kind of event is ignored.
	 */
	public void windowActivated(WindowEvent arg0) {
        //System.out.println("windowActivated()");
        //(PDias) Para poder ser esta a base das frames quando se
        //  abre o MC e MP
        ConfigFetch.setSuperParentFrameForced(MissionPlanner.this);
    }

	/**
	 * The implementation of this function is required by the
	 * <b>WindowListener</b>. This kind of event is ignored.
	 */
	public void windowClosed(WindowEvent arg0) {}
	
	/**
	 * The implementation of this function is required by the
	 * <b>WindowListener</b>. This kind of event is ignored.
	 */
	public void windowDeactivated(WindowEvent arg0) {}
	
	/**
	 * The implementation of this function is required by the
	 * <b>WindowListener</b>. This kind of event is ignored.
	 */
	public void windowDeiconified(WindowEvent arg0) {}
	
	/**
	 * The implementation of this function is required by the
	 * <b>WindowListener</b>. This kind of event is ignored.
	 */
	public void windowIconified(WindowEvent arg0) {}
	
	/**
	 * The implementation of this function is required by the
	 * <b>WindowListener</b>. This kind of event is ignored.
	 */
    public void windowOpened(WindowEvent arg0) {
    }	
	
	//public void showMissionElement()

	/**
	 * Creates an internal frame with the given component and title
	 * @param title The title to appear in the frame
	 * @param newComponent The component to be shown in the new frame
	 */
	public JInternalFrame createFrame(String title, String name, JComponent newComponent) {
		JInternalFrame jif = new JInternalFrame(title, true, true, true, true);
		jif.setName(name);
		
		if (newComponent instanceof IndividualPlanEditor) {
			jif.setFrameIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
	                getClass().getResource("/images/menus/plan.png"))));	
		}
		else {
		
			if (newComponent instanceof MissionMapEditor) {
				final JComponent mmeComp = newComponent;
				
				jif.setFrameIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
		                getClass().getResource("/images/menus/mapeditor.png"))));
				jif.addInternalFrameListener(new InternalFrameAdapter()  {        			
        			public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent arg0) {
        				try {
        					
        					((MissionMapEditor)mmeComp).switchTo2D();
        					//((JInternalFrame) arg0.getSource()).setIcon(true);
        				}
        				catch (Exception e) {
        					NeptusLog.pub().error(this, e);
        				}
        			};        			        			
        		});
			}
			else {
		
				if (newComponent instanceof VehicleInfo) {
					Image originalImage = ImageUtils.getImage(((VehicleInfo)newComponent).getVehicleMission().getVehicle().getSideImageHref());
					Image scaledImage = ImageUtils.getScaledImage(originalImage, 22,22,false);
					jif.setFrameIcon(new ImageIcon(scaledImage));	
				}
				else {
					Image neptusIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(
			                getClass().getResource("/images/neptus-icon.png"))).getImage();
					jif.setFrameIcon(new ImageIcon(ImageUtils.getScaledImage(neptusIcon, 16,16,false)));
				}
			}
		}
		
		
		jif.addInternalFrameListener(this);
		jif.getContentPane().add(newComponent);
		//jif.setIconifiable(false);
		desktop.add(jif);
		jif.setSize(600,450);
		
        if (newComponent instanceof LatLonConv)
        {
        	//LatLonConv llc = (LatLonConv) newComponent;
        	//jif.setSize(llc.getWidth(), llc.getHeight());
        	jif.setSize(419,224);
        }

		jif.setVisible(true);
		jif.setLocation(numFrames*20, numFrames*20);
		numFrames++;
		
		return jif;
	}
	
	
	/**
	 * Creates all the interface elements
	 */
	public void createInterface() {
				
		this.getContentPane().setLayout(new BorderLayout());
		JToolBar toolbar = createToolbar();
		this.setJMenuBar(createMenuBar());
						
		desktop = new JDesktopPane();
		desktop.setBackground(new Color(24,58,83));
		
		final ImageIcon icon = new ImageIcon(ImageUtils.getImage("images/lsts.png"));
		final JLabel lbl = new JLabel(icon);

		lbl.setBounds(desktop.getWidth() - icon.getIconWidth(), 
				desktop.getHeight() - icon.getIconHeight(),
				icon.getIconWidth(), icon.getIconHeight());
		
		desktop.addComponentListener(new ComponentAdapter() {
			public void componentResized(java.awt.event.ComponentEvent e) {
				lbl.setBounds(e.getComponent().getWidth() - icon.getIconWidth(), 
						e.getComponent().getHeight() - icon.getIconHeight(),
						icon.getIconWidth(), icon.getIconHeight());
			};
		});
		
		desktop.add(lbl, Integer.MIN_VALUE);

		getContentPane().add(toolbar, BorderLayout.NORTH);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setOneTouchExpandable(true);
		mBrowser = new MissionBrowser(null);
		mBrowser.addChangeListener(this);
		mBrowser.setMinimumSize(new java.awt.Dimension(160,100));
		mBrowser.addMouseListener(new MouseAdapter() {		    
		    public void mouseClicked(MouseEvent e) {
		        if (e.getClickCount() >= 2 && e.getButton() == MouseEvent.BUTTON1) {
                    editSelectedMissionElement();
		        }
		        if (e.getButton() == MouseEvent.BUTTON3) {
		        	//mBrowser.get
		        	TreePath path = mBrowser.getElementTree().getPathForLocation(e.getX(), e.getY());
		        	if (path != null) {
		        		mBrowser.getElementTree().setSelectionPath(path);
		        		Object element = mBrowser.getSelectedItem();
		        		showElementPopup(e, element);
		        	}
		        }
            }
		});
		
		split.add(mBrowser);
		split.add(desktop);
		//getContentPane().add(new MissionBrowser(null), BorderLayout.WEST);
		getContentPane().add(split, BorderLayout.CENTER);
	}

	private void showElementPopup(MouseEvent e, Object element) {
		JPopupMenu popup = null;
		JMenuItem item;
		if (element instanceof MapType || element instanceof PlanType) {
			String append = " map";
			if (element instanceof PlanType)
				append = " plan";
			
			popup = new JPopupMenu();
			item = new JMenuItem("Edit"+append);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					editSelectedMissionElement();
				}
			});
			popup.add(item);
			
			popup.addSeparator();
			
			item = new JMenuItem("Remove"+append);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					removeSelectedMissionElement(mBrowser.getSelectedItem());
				}
			});
			popup.add(item);
			
			popup.addSeparator();
			
			if (element instanceof PlanType) {
				final PlanType plan = (PlanType) element;
				item = new JMenuItem("Duplicate"+append);
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						duplicatePlan(plan);
					}
				});				
				popup.add(item);
				
				try {
					if (plan.getVehicleType().getConsoles().size() > 0) {
						item = new JMenuItem("Show operator Console");					
						item.addActionListener(new ActionListener() {						
							public void actionPerformed(ActionEvent e) {
								showConsole(plan);
							};
						});
					}
					popup.add(item);
				} catch (Exception e1) {
					NeptusLog.pub().error("Show operator Console", e1);
				}
			}
		}
		
		if (element.toString().equals("Mission Information")) {
			popup = new JPopupMenu();
			item = new JMenuItem("Edit Mission Info");
			item.setActionCommand("info");
			item.addActionListener(this);
			popup.add(item);
		}
		
		if (element.toString().equals("Home Reference")) {
			popup = new JPopupMenu();
			item = new JMenuItem("Edit Home Reference");
			
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					editSelectedMissionElement();
				}
			});
			popup.add(item);
		}
		
		if (element instanceof ChecklistType) {
			popup = new JPopupMenu();
			item = new JMenuItem("View/Edit this check list");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					editSelectedMissionElement();
				}				
			});
			
			popup.add(item);
			
			item = new JMenuItem("Remove this check list");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					removeSelectedMissionElement(mBrowser.getSelectedItem());	
				}				
			});

			popup.add(item);
		}
		
		if (popup != null)
			popup.show((JComponent) e.getSource(), e.getX(), e.getY());
	}
	
	/**
	 * @param plan
	 */
	private void duplicatePlan(PlanType plan) {
		String newID = JOptionPane.showInputDialog(this, "Please enter new plan id", "copy_of_"+plan.getId());		 
		PlanType newPlan = plan.copy(newID);
		mBrowser.addPlan(newPlan);
		//FIXME ver se isto resolve os planos duplicados passarem a ser guardados
		planTypes.put(newPlan.getId(), newPlan);
        warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_PLAN_ADDED));
        setMissionChanged(true);
	}

	private void showConsole(PlanType plan) {
    	VehiclesHolder.showConsole(plan, getUpdatedMission(), this);
	}


	private void removeSelectedMissionElement(Object selectedItem) {
		mBrowser.removeItem(selectedItem);
		
		String id = selectedItem.toString();
		
		if (selectedItem instanceof MapType) {
			try {
				if (mapFrames.containsKey(id)) {
					JInternalFrame frame = (JInternalFrame)mapFrames.get(id);
					frame.setVisible(false);
					frame.dispose();
				}
				mapTypes.remove(id);
				mapEditors.remove(id);
				getMapGroup().removeMap(id);
				warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_MAP_REMOVED));
			}
			catch (NullPointerException e) {}
		}
		
		if (selectedItem instanceof PlanType) {
			id = ((PlanType)selectedItem).getId();
			try {
				if (planFrames.containsKey(id)) {
					JInternalFrame frame = (JInternalFrame)planFrames.get(id);
					frame.setVisible(false);
					frame.dispose();
				}
				planTypes.remove(id);
				planEditors.remove(id);				
				warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_PLAN_REMOVED));
			}
			catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
		
		if (selectedItem instanceof ChecklistType) {
			try {
				if (checkListFrames.containsKey(id)) {
					JInternalFrame frame = (JInternalFrame)checkListFrames.get(id);
					frame.setVisible(false);
					frame.dispose();
				}
				checkLists.remove(id);
				checkListEditors.remove(id);
				warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_CHECKLIST_REMOVED));
			}
			catch (NullPointerException e) {}
		}		
	}
	
	
	

	protected JToolBar createToolbar() {
		JToolBar toolbar = new JToolBar();

		
		ToolbarButton tb = new ToolbarButton("images/buttons/new.png", "Create a new mission", "newMission");
		tb.addActionListener(this);
		toolbar.add(tb);

		tb = new ToolbarButton("images/buttons/open.png", "Open an existing mission", "openMission");
		tb.addActionListener(this);
		toolbar.add(tb);
		
		
		tb = new ToolbarButton("images/buttons/save.png", "Saves the current mission", "saveMission");
		tb.addActionListener(this);
		toolbar.add(tb);
		
		menus.put("saveMission_btn", tb);
		
		tb = new ToolbarButton("images/buttons/edit.png", "View and edit the mission information", "info");
		tb.addActionListener(this);
		toolbar.add(tb);

		menus.put("editInfo_btn", tb);
		
		tb = new ToolbarButton("images/buttons/mapeditor.png", "Add a map to this mission", "newMap");
		tb.addActionListener(this);
		toolbar.add(tb);
		
		menus.put("newMap_btn", tb);


		tb = new ToolbarButton("images/buttons/plan.png", "Add a plan to this mission", "newPlan");
		tb.addActionListener(this);
		toolbar.add(tb);
		
		menus.put("newPlan_btn", tb);
		
		tb = new ToolbarButton("images/buttons/checklist.png", "Create a new check list", "newCheckList");
		tb.addActionListener(this);
		toolbar.add(tb);
		
		menus.put("newCheck_btn", tb);

		tb = new ToolbarButton("images/buttons/vehicle.png", "Add a vehicle", "newVehicle");
		tb.addActionListener(this);
		toolbar.add(tb);
		
		menus.put("newVehicle_btn", tb);
		
		return toolbar;
	}
	
	
	/**
	 * Creates the window menu
	 * @return A JMenubar with all items
	 */
    protected JMenuBar createMenuBar() {
    	ClassLoader cl = getClass().getClassLoader();
        JMenuBar menuBar = new JMenuBar();

        // The mission menu (new mission, save, load, exit, ...)
        JMenu menu = new JMenu("Mission");
        menuBar.add(menu);

        JMenu edit = new JMenu("Edit");
        menuBar.add(edit);
        
        JMenu view = new JMenu("View");
        menuBar.add(view);
        
        JMenu help = new JMenu("Help");
        menuBar.add(help);
        
        // Show ChangeLog item
        JMenuItem menuItem = new JMenuItem("Change Log", new ImageIcon(cl.getResource("images/menus/changelog.png")));
        menuItem.setActionCommand("changelog");
        menuItem.addActionListener(this);
        view.add(menuItem);
        
        //Show Output console
        menuItem = new JMenuItem();
        menuItem.setText("Show Console");
        menuItem.setIcon(new ImageIcon(this.getClass().getClassLoader()
				.getResource("images/menus/display.png")));
        menuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				OutputPanel.showWindow();
			}
		});
        view.add(menuItem);
        
        //Show Lat/Lon Conv.
        menuItem = new JMenuItem();
        menuItem.setIcon(new ImageIcon(ImageUtils
                .getImage("images/menus/displaylatlon.png")));
        menuItem.setText("Lat/Lon Con.");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				LatLonConv latLonConv = new LatLonConv();
				createFrame("Lat/Lon Conv.", "Lat/Lon Conv.", latLonConv);
			}
		});
        view.add(menuItem);
        
        //Show About item
        menuItem = new JMenuItem("About", new ImageIcon(cl.getResource("images/menus/info.png")));
        menuItem.setActionCommand("about");
        menuItem.addActionListener(this);
        help.add(menuItem);
        
        
        menuItem = new JMenuItem("Create Mission", new ImageIcon(cl.getResource("images/menus/new.png")));
        menuItem.setActionCommand("newMission");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        //menus.put("newMission", menuItem);
        
        menuItem = new JMenuItem("Open Mission", new ImageIcon(cl.getResource("images/menus/open.png")));
        menuItem.setActionCommand("openMission");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        loadRecentlyOpenedFiles();
        JMenu recent = getRecentlyOpenFilesMenu();
        menu.add(recent);

        menu.addSeparator();
        
        JMenu addSubMenu = new JMenu("Add...");
        addSubMenu.setIcon(new ImageIcon(cl.getResource("images/menus/new.png")));
        menu.add(addSubMenu);
        
        //menus.put("addMenu", addSubMenu);
        
        menuItem = new JMenuItem("A new Plan", new ImageIcon(cl.getResource("images/menus/plan.png")));
        menuItem.setActionCommand("newPlan");
        menuItem.addActionListener(this);
        addSubMenu.add(menuItem);
        
        menus.put("newPlan", menuItem);
        /*
        menuItem = new JMenuItem("A vehicle", new ImageIcon(cl.getResource("images/menus/vehicle.png")));
        menuItem.setActionCommand("newVehicle");
        menuItem.addActionListener(this);
        addSubMenu.add(menuItem);
        
        menus.put("newVehicle", menuItem);
        */
        
        menuItem = new JMenuItem("A new Map", new ImageIcon(cl.getResource("images/menus/mapeditor.png")));
        menuItem.setActionCommand("newMap");
        menuItem.addActionListener(this);
        addSubMenu.add(menuItem);
        
        menus.put("newMap", menuItem);
        
        
        menuItem = new JMenuItem("An existing Map", new ImageIcon(cl.getResource("images/menus/open_map.png")));
        menuItem.setActionCommand("openMap");
        menuItem.addActionListener(this);
        addSubMenu.add(menuItem);
        
        menus.put("openMap", menuItem);
        
        menuItem = new JMenuItem("A new CheckList", new ImageIcon(cl.getResource("images/buttons/checklist.png")));
        menuItem.setActionCommand("newCheckList");
        menuItem.addActionListener(this);
        addSubMenu.add(menuItem);
        
        menus.put("newCheckList", menuItem);
        
        menuItem = new JMenuItem("An existing CheckList", new ImageIcon(cl.getResource("images/buttons/checklist.png")));
        menuItem.setActionCommand("openCheckList");
        menuItem.addActionListener(this);
        addSubMenu.add(menuItem);
        
        menus.put("openCheckList", menuItem);
        
        menu.addSeparator();  
        
        menu.addSeparator();
        
        // save item
        menuItem = new JMenuItem("Save", new ImageIcon(cl.getResource("images/menus/save.png")));
        menuItem.setActionCommand("saveMission");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        menus.put("saveMission", menuItem);
        
        // save as item
        menuItem = new JMenuItem("Save As...", new ImageIcon(cl.getResource("images/menus/saveas.png")));
        menuItem.setActionCommand("saveMissionAs");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        menus.put("saveMissionAs", menuItem);
        
//      save as item
        menuItem = new JMenuItem("Save as zip file", new ImageIcon(cl.getResource("images/menus/saveas.png")));
        menuItem.setActionCommand("saveAsZip");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        menus.put("saveAsZip", menuItem);        

        menu.addSeparator();
        
        // quit item
        menuItem = new JMenuItem("Quit", new ImageIcon(cl.getResource("images/menus/exit.png")));
        menuItem.setActionCommand("quit");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        
        // Edit Mission information item
        menuItem = new JMenuItem("Mission Information", new ImageIcon(cl.getResource("images/menus/settings.png")));
        menuItem.setActionCommand("info");
        menuItem.addActionListener(this);
        edit.add(menuItem);
        
        // Edit General Preferences
        menuItem = new JMenuItem("Neptus Properties", new ImageIcon(cl.getResource("images/menus/settings.png")));
        menuItem.setActionCommand("preferences");
        menuItem.addActionListener(this);
        edit.add(menuItem);
        

        // Enable / Disable the menus accordingly
        refreshMenus();
        
        // after inserting all the muenu items, returns the menubar...
        return menuBar;
        
    }
    
    /**
     * This procedure is called whenever an action occurs
     * If you wish to add another actions of your own, create new actions
     * and change is actionCommand like this: 
     * <code>myAction.setActionCommand("myCommand");</code>
     */
	public void actionPerformed(ActionEvent action) {
		
		// This action is sent when the quit menu item is clicked
		if ("quit".equals(action.getActionCommand())) {
			if (closeCurrentMission()) {
				for (int i = 0; i < changeListeners.size(); i++) {
					ChangeEvent ce = new ChangeEvent(this);
					((ChangeListener) changeListeners.get(i)).stateChanged(ce);
				}
				this.dispose();
			}
			else {
				return;
			}
		}
		
		if ("openMission".equals(action.getActionCommand())) {
			
			File userFile = MissionFileChooser.showOpenMissionDialog(new String[] {"nmis", "xml", "nmisz"});
			
			if (userFile == null)
				return;
			
			openMission(userFile);
			/*
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				final String file = userFile.getAbsolutePath();
				AsyncWorker.post(new AsyncTask() {
					@Override
					public Object run() throws Exception {
						MissionType mt = new MissionType(file);
						
						if (mt == null)							
							throw new Exception("Incompatible file type");						
						
						setMission(mt);
						setMapGroup(MapGroup.getMapGroupInstance(mt));
						setMissionChanged(false);
						setWorkingFile(file);
						refreshMenus();
						updateMissionFilesOpened(new File(file));
						return null;
					}
					
					@Override
					public void finish() {					
						setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						try {
							getResultOrThrow();
						}
						catch (Exception e) {
							GuiUtils.errorMessage(MissionPlanner.this, "Incompatible file type", "The selected file is not supported");
						}
						
					}
				});				
			}
			catch (Exception e) {
				GuiUtils.errorMessage(this, "Error loading mission", "<html>An "+e.getClass().getSimpleName()+ " occured while loading mission:<br>"+e.getMessage()+"</html>");
				NeptusLog.pub().error(this, e);
				this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
			return;
			*/
		}
		
		if ("saveMission".equals(action.getActionCommand())) {
		    
			saveMission(false);
		}
		
		if ("saveMissionAs".equals(action.getActionCommand())) {
			saveMission(true);
		}

		// ChangeLog menu item
		if ("changelog".equals(action.getActionCommand())) {
			ChangeLog cl = new ChangeLog();
			cl.setVisible(true);
			return;
		}

		// ChangeLog menu item
		if ("about".equals(action.getActionCommand())) {
			AboutPanel ap = new AboutPanel();
			ap.setVisible(true);
			return;
		}
		
		if ("newPlan".equals(action.getActionCommand())) {
		    if (getMission() != null) {
		      
		    	IndividualPlanEditor planEditor;
		    	
		    	//MissionGraph curGraph;
		        
		        String id = GuiUtils.idSelector(planTypes.keySet().toArray(), getNewPlanId());		        
		        if (id == null)
		            return;		        
		        PlanType plan = new PlanType(getMission());
		        plan.setId(id);
		        
		        VehicleType vehicle = VehicleChooser.showVehicleDialog();
		        if (vehicle == null)
		        	return;
		        plan.setVehicle(vehicle.getId());
		        planEditor = new IndividualPlanEditor(this, plan);		        
		        /*
		        curGraph = new MissionGraph(plan, true);		        
		        curGraph.addActionListener(this);
		        curGraph.addChangeListener(this);
		        curGraph.setParentMP(this);
		        */
		        PlanType ipt = planEditor.getPlan();
		        JInternalFrame graphFrame = createFrame("[PlanEditor] '"+ipt.getId()+"'", ipt.getId(), planEditor);
		        mBrowser.addPlan(ipt);
		        planFrames.put(ipt.getId(), graphFrame);
		        planTypes.put(ipt.getId(), ipt);
		        planEditors.put(ipt.getId(), planEditor);
		        warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_PLAN_ADDED));
		        setMissionChanged(true);
		        
		        VehicleType vt = ipt.getVehicleType();
		        if (!vehicles.containsKey(vt.getId())) {
		            VehicleMission vm = new VehicleMission();
		            vm.setId(vt.getId());
		            vm.setName(vt.getName());
		            vm.setVehicle(vt);
		            vm.setCoordinateSystem(getMission().getHomeRef());
		            vehicles.put(vm.getId(), vm);
		            
		            mBrowser.addVehicle(vm);
		            
		        }
		        else {
		        	NeptusLog.pub().debug("vechile "+ipt.getVehicleType().getId()+" is already in the mission");
		        }
		        
		        return;
		    }
		    else {
		        JOptionPane.showMessageDialog(this, "You have to create a mission first", "Create a mission first", JOptionPane.WARNING_MESSAGE);
		    }
		}
		
		// New menu item
		if ("newMission".equals(action.getActionCommand())) {
		    int answer;
			if (getMission() != null && missionChanged) {
				
				if (getMission().getOriginalFilePath() != null)
					answer = JOptionPane.showConfirmDialog(this, "<html>Save changes to the current <strong>mission</strong>?</html>", "Save changes?", JOptionPane.YES_NO_CANCEL_OPTION);	
				else
					answer = JOptionPane.showConfirmDialog(this, "<html>Do you want to save the current <strong>mission</strong> first?</html>", "Save mission?", JOptionPane.YES_NO_CANCEL_OPTION);
				
				if (answer == JOptionPane.CANCEL_OPTION) {
					return;
				}
				
				if (answer == JOptionPane.NO_OPTION){
					myMissionType = null;
					warnMissionListeners(new MissionChangeEvent(
							MissionChangeEvent.TYPE_MISSION_CLOSED));
				}
				else {
					int result = saveMission(true);
					if (result != OK) 
						return;
					warnMissionListeners(new MissionChangeEvent(
							MissionChangeEvent.TYPE_MISSION_CLOSED));
				}
			}
			
			Object[] frames = desktop.getAllFrames();
			for (int i = 0; i < frames.length; i++) {
				((JInternalFrame) frames[i]).dispose();
			}
			
			CoordinateSystem cs = new CoordinateSystem();					
			
			setMission(new MissionType());
			HomeReference hr = new HomeReference();
			hr.setCoordinateSystem(cs);
			getMission().setHomeRef(hr);
			setMissionChanged(true);			
			setMapGroup(MapGroup.getMapGroupInstance(getMission()));
			//MapGroup.getMissionInstance().setCoordinateSystem(hr);
			//allMaps = new MapGroup(hr);
			refreshMenus();
			
			mBrowser.setMission(getMission());
		}
		
		if ("newMap".equals(action.getActionCommand())) {
			if (myMissionType == null)
			    GuiUtils.errorMessage(this, "Create a mission first", "In order to use this feature, \nyou have to create a mission first");
			else {
			    String id = GuiUtils.idSelector(mapTypes.keySet().toArray(), getNewMapId());
			    if (id == null)
			        return;
			    else {
			        MissionMapEditor mme = new MissionMapEditor(id, getMission().getHomeRef());
			       
			        mme.setParentMP(this);
			        
			        mme.setMapGroup(getMapGroup());
			        
			        MapType map = mme.getMap();
			        
			        mBrowser.addMap(map);
			        mapFrames.put(map.getId(), createFrame("[MapEditor] '"+map.getId()+"'", map.getId(), mme));
			        mapEditors.put(map.getId(), mme);
			        System.out.println("Adding map "+map.getId()+" to mapTypes");
			        mapTypes.put(map.getId(), map);
			        mme.addChangeListener(this);
			        setMissionChanged(true);
			        warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_MAP_ADDED));
			    }
			}
		}
		
		if ("openMap".equals(action.getActionCommand())) {
			if (myMissionType == null)
			    GuiUtils.errorMessage(this, "Create a mission first", "In order to use this feature, \nyou have to create a mission first");
			else {
				MissionMapEditor mme = new MissionMapEditor("unnamed map", getMission().getHomeRef());
				mme.setParentMP(this);
				
				File fxBase;
				if (getMission().getCompressedFilePath() != null && !"".equalsIgnoreCase(getMission().getCompressedFilePath()))
					fxBase = new File(getMission().getCompressedFilePath());
				else if (getMission().getOriginalFilePath() != null && !"".equalsIgnoreCase(getMission().getOriginalFilePath()))
					fxBase = new File(getMission().getOriginalFilePath());
				else
					fxBase = new File(ConfigFetch.resolvePath("."));
				
				if (!mme.addExistingMapToMission(this, fxBase))
				    return;
				else {
				    MapType map = mme.getMap();
					if (mapTypes.containsKey(map.getId())) {
						MapType existingMap = (MapType) mapTypes.get(map.getId());
						if (existingMap.getHref().equals(map.getHref())) {
							GuiUtils.errorMessage(this, "Map already in mission","The chosen is already in the current mission.");
							return;
						}
					    GuiUtils.errorMessage(this, "Invalid identifier","The map identifier is already in use");
					    String id = GuiUtils.idSelector(mapTypes.values().toArray(), NameNormalizer.getRandomID());
					    if (id == null)
					        return;
					    else {
					        map.setId(id);
					        mme = new MissionMapEditor(getMission(), map, true);//,  getMission().getHomeRef(), true);
					       
					        mme.setParentMP(this);
					    }
					}
				    mBrowser.addMap(map);
					mapFrames.put(map.getId(), createFrame("[MapEditor] '"+ map.getId()+"'",map.getId(), mme));
					mapEditors.put(map.getId(), mme);
					mapTypes.put(map.getId(), map);
					
					getMapGroup().addMap(map);
					mme.setMapGroup(getMapGroup());
					mme.addChangeListener(this);
					setMissionChanged(true);
					warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_MAP_ADDED));
				}
			}
		}
		
		if ("info".equals(action.getActionCommand())) {
		    this.myMissionType = MissionInfoPanel.changeMissionParameters(getMission());
		    setMissionChanged(true);
		}
		
		if ("preferences".equals(action.getActionCommand())) {
			 PropertiesEditor.editProperties(new GeneralPreferences(), this, true);
		}
		
		if ("newCheckList".equals(action.getActionCommand())) {
			String inputValue = JOptionPane.showInputDialog(this, "Please enter a name for the new checklist", getNewCheckListId());
            if (inputValue != null && !inputValue.trim().equalsIgnoreCase(""))
            {
            	if (checkLists.get(inputValue.trim()) != null) {
            		GuiUtils.errorMessage(this, "Checklist already created", "The given check list name already exists");
            		return;
            	}
            	
                ChecklistType checklist = new ChecklistType();
                checklist.setName(inputValue.trim());
                mBrowser.addCheckList(checklist);
                //myMissionType.
                //checkLists.put(checklist.get(), checklist);
                openCheckListFrame(checklist);
                setMissionChanged(true);     
                warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_CHECKLIST_ADDED));
            }
		}

		if ("openCheckList".equals(action.getActionCommand())) {
			File fxBase;
			if (getMission().getCompressedFilePath() != null && !"".equalsIgnoreCase(getMission().getCompressedFilePath()))
				fxBase = new File(getMission().getCompressedFilePath());
			else if (getMission().getOriginalFilePath() != null && !"".equalsIgnoreCase(getMission().getOriginalFilePath()))
				fxBase = new File(getMission().getOriginalFilePath());
			else
				fxBase = new File(ConfigFetch.resolvePath("."));
			File fx = ChecklistFileChooser.showOpenDialog(this, fxBase);
			ChecklistType clo = new ChecklistType(fx.getAbsolutePath());
			if (!clo.isLoadOk()) {
				GuiUtils.errorMessage(this, "Add an check list", "Could not load the check list file.");
        		return;
			}
        	if (checkLists.get(clo.getName()) != null) {
        		GuiUtils.errorMessage(this, "Checklist already created", "The given check list name already exists");
        		return;
        	}
        	
            mBrowser.addCheckList(clo);
            openCheckListFrame(clo);
            setMissionChanged(true);     
            warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_CHECKLIST_ADDED));
		}

		
		if ("saveAsZip".equals(action.getActionCommand())) {
			if (isMissionChanged()) {
                if (GuiUtils.confirmDialog(this, "Save as Zip file", "The mission has to be saved first. Continue?") != JOptionPane.YES_OPTION)
					return;				
			}
			
			saveMission(false);			
			String filename = GuiUtils.getLogFileName("mission_state", "nmisz");
			if (getUpdatedMission().asZipFile(filename, false))
				JOptionPane.showMessageDialog(this, "Mission state was saved on file '"+filename+"'");
				
		}
		
		if ("newVehicle".equals(action.getActionCommand())) {
			Vector<VehicleType> usedVehicles = new Vector<VehicleType>();
			for (VehicleMission vm : vehicles.values()) {
				usedVehicles.add(vm.getVehicle());
			}
			VehicleType vt = VehicleChooser.showVehicleDialog(usedVehicles);
			
			if (vt != null && !vehicles.containsKey(vt.getId())) {
				VehicleMission vm = new VehicleMission();
	            vm.setId(vt.getId());
	            vm.setName(vt.getName());
	            vm.setVehicle(vt);
	            vm.setCoordinateSystem(getMission().getHomeRef());
	            vehicles.put(vm.getId(), vm);
	            
	            mBrowser.addVehicle(vm);
			}
		}
		
		return;
	}
	
	public JInternalFrame openCheckListFrame(ChecklistType checklist) {
		
        ChecklistPanel clPanel = new ChecklistPanel(checklist);
        
        clPanel.addChangeListener(this);
        
        JInternalFrame ifrm = createFrame(checklist.getName() + " - Checklist",
                checklist.getName(),
                clPanel);
        clPanel.setJInternalFrame(ifrm);
        
        
        
        ifrm.setSize(new Dimension(clPanel.getWidth()+100, clPanel.getHeight()+150));
        //ifrm.setSize(new Dimension(mp.getWidth(), mp.getHeight()+120));
        ifrm.setResizable(true);
        //ifrm.setMaximizable(false);
        checkListFrames.put(checklist.getName(), ifrm);
        checkLists.put(checklist.getName(), checklist);
        checkListEditors.put(checklist.getName(), clPanel);
        return ifrm;
	}
	
	public void showPlanPreview(PlanType plan, int mode) {
	    
		final MissionRenderer mr = new MissionRenderer(plan, getMapGroup(), mode);
	    
	    JFrame newFrame = new JFrame(plan.getId()+" preview");
	    newFrame.setLayout(new BorderLayout());
	    newFrame.add(mr, BorderLayout.CENTER);
	    newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    newFrame.addWindowListener(new WindowAdapter() {
	        public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                mr.dispose();
            }
	    });
	    newFrame.setSize(500, 400);
	    newFrame.setVisible(true);
	}
	

	/**
	 * This method refreshes some component states (enabled / disabled) 
	 * according to the program state
	 */
	private void refreshMenus() {
	    Object[] comps = menus.values().toArray();
	    if (myMissionType != null) {
	        for (int i = 0; i < comps.length; i++)
	            ((JComponent) comps[i]).setEnabled(true);
	        
	    }
	    else {
	        for (int i = 0; i < comps.length; i++)
	            ((JComponent) comps[i]).setEnabled(false);
	    }
	    
	    ToolbarButton btn = (ToolbarButton) menus.get("saveMission_btn");
	    btn.setEnabled(myMissionType != null && isMissionChanged());
	    JMenuItem itm = (JMenuItem) menus.get("saveMission");
	    itm.setEnabled(myMissionType != null && isMissionChanged());
	}
	
	
	public void internalFrameClosing(InternalFrameEvent arg0) {
		JInternalFrame closedFrame = (JInternalFrame)arg0.getSource();
		
		if (mapFrames.containsKey(closedFrame.getName())) {
		    MissionMapEditor mme = (MissionMapEditor) mapEditors.get(closedFrame.getName());
		    MapType map = mme.getMap();
		    mapTypes.put(map.getId(), map);
		    mapFrames.remove(closedFrame.getName());
		    mme.cleanup();
		}
		
		if (checkListFrames.containsKey(closedFrame.getName())) {			
			ChecklistPanel clPanel = (ChecklistPanel)checkListEditors.get(closedFrame.getName());
			checkLists.put(closedFrame.getName(), clPanel.getChecklistType());
			checkListFrames.remove(closedFrame.getName());
		}
		
		if (planFrames.containsKey(closedFrame.getName())) {
		    IndividualPlanEditor mg = (IndividualPlanEditor) planEditors.get(closedFrame.getName());
		    planTypes.put(closedFrame.getName(), mg.getPlan());
		    planFrames.remove(closedFrame.getName());
		    //planEditors.remove(closedFrame.getName());
		}
		
		if (vehicleFrames.containsKey(closedFrame.getName())) {
			vehicleFrames.remove(closedFrame.getName());
		}
		
		mBrowser.reload();
		
	}

	/**
	 * Does absolutely nothing... but must be implemented in order to fulfill the
	 * <b>InternalFrameListener</b> interface.
	 */
	public void internalFrameActivated(InternalFrameEvent arg0) {}
	
	/**
	 * Does absolutely nothing... but must be implemented in order to fulfill the
	 * <b>InternalFrameListener</b> interface.
	 */
	public void internalFrameClosed(InternalFrameEvent arg0) {
	    
	    
	}
	
	/**
	 * Does absolutely nothing... but must be implemented in order to fulfill the
	 * <b>InternalFrameListener</b> interface.
	 */
	public void internalFrameDeactivated(InternalFrameEvent arg0) {
		//((JInternalFrame)arg0.getSource()).
	}
	
	/**
	 * Does absolutely nothing... but must be implemented in order to fulfill the
	 * <b>InternalFrameListener</b> interface.
	 */
	public void internalFrameDeiconified(InternalFrameEvent arg0) {}
	
	/**
	 * Does absolutely nothing... but must be implemented in order to fulfill the
	 * <b>InternalFrameListener</b> interface.
	 */
	public void internalFrameIconified(InternalFrameEvent arg0) {
	    
	    //arg0.getInternalFrame().dispose();
	}
	
	/**
	 * Does absolutely nothing... but must be implemented in order to fulfill the
	 * <b>InternalFrameListener</b> interface.
	 */
	public void internalFrameOpened(InternalFrameEvent arg0) {}
	
//  @jve:decl-index=0:visual-constraint="640, 480"
    public MissionType getMission() {
        return myMissionType;
    }
    public void setMission(MissionType mission) {
        this.myMissionType = mission;

        JInternalFrame[] frames = desktop.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
        	frames[i].dispose();
        	frames[i].setVisible(false);
        }

        mapEditors = new LinkedHashMap<String, MissionMapEditor>();
        planEditors = new LinkedHashMap<String, IndividualPlanEditor>();
        mapFrames = new LinkedHashMap<String, JInternalFrame>();
        mapTypes = new LinkedHashMap<String, MapType>();
        planFrames = new LinkedHashMap<String, JInternalFrame>();
        planTypes = new LinkedHashMap<String, PlanType>();
		vehicles = new LinkedHashMap<String, VehicleMission>();
		checkListEditors = new LinkedHashMap<String, ChecklistPanel>();
		checkLists = new LinkedHashMap<String, ChecklistType>();
		checkListFrames = new LinkedHashMap<String, JInternalFrame>();
		
        loadMission();
        
        setMissionChanged(false);
    }
    
	private void loadMission() {
		
	    mBrowser.setMission(getMission());
        setTitle("Neptus Mission Planner - "+getMission().getName());
        if (myMissionType == null)
            return;
        
        setMapGroup(MapGroup.getMapGroupInstance(myMissionType));

        Object [] maps =  getMission().getMapsList().values().toArray();
        
        for (int i = 0; i < maps.length; i++) {
            MapMission mm = (MapMission) maps[i];
            MapType mt = mm.getMap();
            mt.setHref(mm.getHref());
            mt.setChanged(false);
            mapTypes.put(mm.getId(), mt);
        }
        
        Object [] plans =  getMission().getIndividualPlansList().values().toArray();
        for (int i = 0; i < plans.length; i++) {
            PlanType ipt = (PlanType) plans[i];
            planTypes.put(ipt.getId(), ipt);
        }
        
        Object [] clists =  getMission().getChecklistsList().values().toArray();
        for (int i = 0; i < clists.length; i++) {
            ChecklistMission clm = (ChecklistMission) clists[i];
            ChecklistType clt = clm.getChecklist();
            checkLists.put(clt.getName(), clt);
        }
        
		
		Object [] vs = getMission().getVehiclesList().values().toArray();
		for (int i = 0; i < vs.length; i++) {
		    VehicleMission vm = (VehicleMission) vs[i];
		    vehicles.put(vm.getId(), vm);
		}
    }

    private void editSelectedMissionElement() {
	    
	    if (mBrowser.getSelectedItem() == null)
	        return;
	    
	    if ("Home Reference".equals(mBrowser.getSelectedItem().toString())) {
	    		    
	    	CoordinateSystem oldCS = new CoordinateSystem();
	    	oldCS.setCoordinateSystem(getMission().getHomeRef());
	    	
            CoordinateSystem cs2 = CoordinateSystemPanel.showHomeRefDialog("Home Reference",
                    myMissionType.getHomeRef(), mBrowser);

	    	if (cs2 == null)
	    		return;
	    	
	    	if (!oldCS.equals(cs2)) {
		    	myMissionType.getHomeRef().setCoordinateSystem(cs2);
		    	MapGroup.getMapGroupInstance(myMissionType).setCoordinateSystem(cs2);
		    	setMissionChanged(true);
		    	mBrowser.reload();
	    	}
	    	
	        return;
	    }
	    
	    if ("Mission Information".equals(mBrowser.getSelectedItem().toString())) {
	        
	    	String oldName = getMission().getName();
	        String oldId = getMission().getId();
	        String oldDesc = getMission().getDescription();
	        String oldType = getMission().getType();	    	
	    	
	    	this.myMissionType = MissionInfoPanel.changeMissionParameters(getMission());
	        
	    	String newName = getMission().getName();
	        String newId = getMission().getId();
	        String newDesc = getMission().getDescription();
	        String newType = getMission().getType();	    	
	    	
	        if (oldType.equals(newType) && oldId.equals(newId) && oldDesc.equals(newDesc) && oldName.equals(newName))
	        	return;
	        
	    	setMissionChanged(true);
	        setTitle("Neptus Mission Planner - "+getMission().getName());
	        mBrowser.reload();
	        return;
	    }
	    
	    if (mBrowser.getSelectedItem() instanceof ChecklistType) {
	        
	        ChecklistType clType = (ChecklistType)mBrowser.getSelectedItem();
	        clType = checkLists.get(clType.getName());
	        
	        if (checkListFrames.containsKey(clType.getName())) {
	        	JInternalFrame tmp = (JInternalFrame) checkListFrames.get(clType.getName());
	            try {
	            	tmp.setSelected(true);
	            	tmp.setIcon(false);
		        	tmp.toFront();		        	
	            }
	            
	            catch (Exception e) {
	            	NeptusLog.pub().error(this, e);
	            }	         
	        }
	        else {	        
	        	openCheckListFrame(clType);
	        }
	    }
	    
	    if (mBrowser.getSelectedItem() instanceof MapType) {
	        
	        MapType Map = (MapType)mBrowser.getSelectedItem();
	        if (mapFrames.containsKey(Map.getId())) {
	        	JInternalFrame tmp = (JInternalFrame) mapFrames.get(Map.getId());
	            try {
	            	tmp.setSelected(true);
	            	tmp.setIcon(false);
		        	tmp.toFront();
	            }
	            
	            catch (Exception e) {
	            	NeptusLog.pub().error(this, e);
	            }

	            return;
	        }
	        MissionMapEditor mme;
	        
	        if (mapTypes.containsKey(Map.getId()))
	            Map = (MapType) mapTypes.get(Map.getId());
	        
	        mme = new MissionMapEditor(getMission(), Map, true);//Map, getMission().getHomeRef(), true);
	        mme.setParentMP(this);
	        mme.setMapGroup(getMapGroup());
			mapTypes.put(Map.getId(), Map);
	        mapFrames.put(Map.getId(), createFrame("[MapEditor] '"+Map.getId()+"'", Map.getId(), mme));
	        mapEditors.put(Map.getId(), mme);
	        mme.addChangeListener(this);	        
	    }
	    
	    if (mBrowser.getSelectedItem() instanceof PlanType) {
	        
	        PlanType plan = (PlanType) mBrowser.getSelectedItem();
	        
	        if (planFrames.containsKey(plan.getId())) {
	        	JInternalFrame tmp = (JInternalFrame) planFrames.get(plan.getId());
	            try {
	            	tmp.setSelected(true);
	            	tmp.setIcon(false);
		        	tmp.toFront();
	            }
	            catch (Exception e) {
	            	NeptusLog.pub().error(this, e);
	            }
	            return;
	        }
	        
	        IndividualPlanEditor planEditor;
	        
	        if (planTypes.containsKey(plan.getId())) {
	        	System.out.println("case 1");
	            planEditor = new IndividualPlanEditor(this, (PlanType) planTypes.get(plan.getId()));	            
	        }
	        else {	  
	        	System.out.println("case 2");
	        	planEditor = new IndividualPlanEditor(this, plan);
	        }
	        
	        /*
	        
	        MissionGraph planEditor;
	        
	        if (planTypes.containsKey(plan.getId())) {
	            planEditor = new MissionGraph((IndividualPlanType) planTypes.get(plan.getId()), true);	            
	        }
	        else {
	            planEditor = new MissionGraph(plan, true);
	        }
	        
	        */
            planTypes.put(plan.getId(), plan);
	        planFrames.put(plan.getId(), createFrame("[PlanEditor] '"+plan.getId() +"'", plan.getId(), planEditor));
	        planEditors.put(plan.getId(), planEditor);
	        //planEditor.addChangeListener(this);
	        planEditor.setParentMP(this);
	        setMissionChanged(true);

	        return;
	    }

    
	    if (mBrowser.getSelectedItem() instanceof VehicleMission) {
	    	VehicleMission vm = (VehicleMission)mBrowser.getSelectedItem();
	    	if (vehicleFrames.containsKey(vm.getId())) {
	    		
	    		JInternalFrame tmp = (JInternalFrame) vehicleFrames.get(vm.getId());
	            try {
	            	tmp.setSelected(true);
	            	tmp.setIcon(false);
		        	tmp.toFront();
	            }
	            catch (Exception e) {
	            	NeptusLog.pub().error(this, e);
	            }
	            mBrowser.reload();
	            return;
	        }
	    	
	       
	        //TODO verificar se já existe uma janela aberta com este veículo!!
	        final VehicleMission veType = (VehicleMission)mBrowser.getSelectedItem();
	        
	        //FIXME Talvez nao seja a melhor forma de fazer isto se o CS do vehicle
	        //       for o HomeRef.
	        CoordinateSystem cs = new CoordinateSystem();
	        cs.setId("home_" + veType.getId());
	        cs.setName("home_" + veType.getName());
	        cs.setCoordinateSystem(veType.getCoordinateSystem());
	        veType.setCoordinateSystem(cs);
	        setMissionChanged(true);
	        final VehicleInfo vi = new VehicleInfo(veType.getVehicle(), veType.getCoordinateSystem(), false, true);
	        
	        JInternalFrame jif = createFrame("[VehicleInfo] '"+veType.getId()+"'", veType.getId(), vi);
	        
	        jif.setMaximizable(false);
	        java.awt.Dimension d = vi.getPreferredSize();
	        d.width += 5;
	        d.height += 65 + 45;
	        jif.setSize(d);
	        vehicleFrames.put(veType.getId(), jif);
	        jif.addInternalFrameListener(new InternalFrameAdapter() {
	        	/* (non-Javadoc)
				 * @see javax.swing.event.InternalFrameAdapter#internalFrameClosing(javax.swing.event.InternalFrameEvent)
				 */
				public void internalFrameClosing(InternalFrameEvent arg0) {
					super.internalFrameClosing(arg0);
					VehicleMission vm = vi.getVehicleMission();
					veType.setCoordinateSystem(vm.getCoordinateSystem());
				}
	        });
	    }
    }
    

    
    
	public int saveMission(boolean selectNewFile) {
		File userFile = null;
		LinkedHashMap<String, MapMission> maps = new LinkedHashMap<String, MapMission>();
		LinkedHashMap<String, PlanType> plans = new LinkedHashMap<String, PlanType>();
		LinkedHashMap<String, VehicleMission> vehicles = new LinkedHashMap<String, VehicleMission>();		
		
	    if (mapEditors.size() > 0) {
	    	Object[] editors = mapEditors.values().toArray();
	    	for (int i = 0; i < editors.length; i++) {
	    		MissionMapEditor mme = (MissionMapEditor) editors[i];
	    		MapType mt = mme.getMap();
	    		mapTypes.put(mt.getId(), mt);
	    	}
	    }

	    
	    // Verifica se algum dos mapas não foi ainda guardado em disco
	    Object[] tmp = mapTypes.values().toArray();
	    for (int i = 0; i < tmp.length; i++) {

	    	MapType map = (MapType) tmp[i];
	    	if (map.getHref().length() < 1) {
	    		int answer = JOptionPane.showConfirmDialog(this, "<html>The <strong>map '"+map.getId()+"'</strong> was not saved yet. Do you want to <strong>save</strong> it now?</html>");
	    		if (answer == JOptionPane.OK_OPTION) {
	    			if (!map.showSaveDialog()) {
	    				refreshMenus();
	    				return CANCEL;
	    			}
	    			else {
	    				if (mapEditors.containsKey(map.getId())) {
	    					MissionMapEditor mme = (MissionMapEditor) mapEditors.get(map.getId());
	    					mme.setMissionType(getMission());
	    					mme.setMapHref(map.getHref());
	    				}
	    			}
	    		}
	    		else{
	    			refreshMenus();
	    			return CANCEL;
	    		}
	    	}
	    	if (map.isChanged()) {
	    		int answer = JOptionPane.showConfirmDialog(this, "<html><strong>Save</strong> changes to the <strong>map '"+map.getId()+"'</strong> ?</html>");
	    		if (answer == JOptionPane.OK_OPTION) {
	    			if (!map.saveFile(map.getHref())) {
	    				refreshMenus();
	    				return ERROR;
	    			}
	    		}
	    		else{
	    			refreshMenus();
	    			return CANCEL;
	    		}
	    	}
	    	
	    	MapMission mm = new MapMission();
	    	mm.setId(map.getId());
	    	mm.setName(map.getName());
	    	mm.setHref(map.getHref());
	    	mm.setMap(map);
	    	maps.put(mm.getId(), mm);	    		    
	    }
	    
	    // Verifica se existe algum editor de planos aberto
	    if (planEditors.size() > 0) {
	    	Object[] editors = planEditors.values().toArray();
	    	for (int i = 0; i < editors.length; i++) {
	    		IndividualPlanEditor planEditor = (IndividualPlanEditor) editors[i];
	    		PlanType ipt = planEditor.getPlan();
	    		planTypes.put(ipt.getId(), ipt);
	    	}
	    }
	    
	    // Cria a lista de planos da missao
	    Object[] planKeys = planTypes.keySet().toArray();
	    for (int i = 0; i< planKeys.length; i++) {
	    	Object key = planKeys[i];
	    	PlanType ipt = (PlanType) planTypes.get(key);
	    	if (ipt.isEmpty())
	    		continue;
	    	if (!ipt.hasInitialManeuver()) {
	    		GuiUtils.errorMessage(this, "Mission NOT saved", "<html>The plan <strong>"+ipt.getId()+"</strong> has no associated initial maneuver.</html>");
	    		return ERROR;
	    	}	    	
	    	plans.put((String)key, planTypes.get(key));
	    }
	    
	    // Verifica se existem editores de checklists abertos
	    if (checkListEditors.size() > 0) {
	    	Object[] editors= checkListEditors.values().toArray();
	    	for (int i = 0; i< editors.length; i++) {
	    		ChecklistPanel clPanel = (ChecklistPanel) editors[i];
	    		if (clPanel.isDirty()) {
	    			if (!clPanel.save())
	    				return CANCEL;
	    		}
                ChecklistType clType = clPanel.getChecklistType();
                checkLists.put(clType.getName(), clType);
	    	}
	    }
	    
	    if (vehicleFrames.size() > 0) {
	    	Object[] vFrames = vehicleFrames.values().toArray();
	    	for (int i = 0; i < vFrames.length; i++) {
	    		JInternalFrame jif = (JInternalFrame) vFrames[i];
	    		jif.doDefaultCloseAction();
	    	}
	    }
	    
	    // Cria a lista de veículos da missao
	    Object[] vehicleKeys = this.vehicles.keySet().toArray();
	    for (int i = 0; i< vehicleKeys.length; i++) {
	    	Object key = vehicleKeys[i];
	    	vehicles.put((String)key, this.vehicles.get(key));
	    }

		// Se a missao tiver sido carregada (ou já foi guardada) de um ficheiro,
	    if (getMission().getCompressedFilePath() != null && getMission().getCompressedFilePath().length() > 0 && !selectNewFile) {  	
    	    userFile = new File(getMission().getCompressedFilePath());
    	}
	    else if (getMission().getOriginalFilePath() != null && getMission().getOriginalFilePath().length() > 0 && !selectNewFile) {  	
    	    userFile = new File(getMission().getOriginalFilePath());
    	}
	    else {
	    	//System.out.println("getWorkingFile="+getWorkingFile()+", selectNewFile="+selectNewFile);
	    	// Se a missão ainda não foi guardada, pergunta o nome do ficheiro onde guardar a missão.
	    	JFileChooser fc = new JFileChooser();
	    	fc.setDialogTitle("Choose the Mission file...");
	    	fc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
	    	fc.setFileView(new NeptusFileView());
	    	fc.setFileFilter(new FileFilter() {
	    		public boolean accept(File f) {
			    	try {
						if (!f.exists() || !f.canRead())
							return false;
						if (f.getCanonicalPath().toLowerCase().endsWith(
                                    "nmisz")
                                    || f.getCanonicalPath().toLowerCase()
                                            .endsWith("nmis"))
							return true;
						
						//if (f.getCanonicalPath().endsWith("xml") || f.getCanonicalPath().endsWith("XML"))
						//	return true;
						
						return f.isDirectory();
			    	}
			    	catch (Exception e) {}
			    	return false;
				}
			
				public String getDescription() {
					return "Neptus Mission Files (*.nmisz, *.nmis)";
				}
			
	    	});
		    int state = fc.showSaveDialog(this);
		    if (state == JFileChooser.CANCEL_OPTION) {
		    	refreshMenus();
		        return CANCEL;
		    }
		    else {
	        	String filename = fc.getSelectedFile().getAbsolutePath();
	        	
	        	if (!filename.toLowerCase().endsWith(".xml")
                        && !filename.toLowerCase().endsWith(".nmis")
                        && !filename.toLowerCase().endsWith(".nmisz"))
	        		userFile = new File(filename + ".nmisz");
		    	else
		    		userFile = fc.getSelectedFile();
		    }
	    }
	    
	    
	    // Cria o objecto missao
		if (FileUtil.FILE_TYPE_MISSION_COMPRESSED.equalsIgnoreCase(FileUtil
				.getFileExtension(userFile))) {
			getMission().setCompressedFilePath(userFile.getAbsolutePath());
		}
		else {
			getMission().setOriginalFilePath(userFile.getAbsolutePath());
			getMission().setCompressedFilePath(null);
		}
	    getMission().setMapsList(maps);
	    
	    getMission().setPlanList(plans);
	    getMission().setVehiclesList(vehicles);
	    getMission().getChecklistsList().clear();
	    
	    for (ChecklistType clType : checkLists.values()) {  	
	    	ChecklistMission clMission = new ChecklistMission();
	    	clMission.setChecklist(clType);
	    	getMission().getChecklistsList().put(clMission.getName(), clMission);
	    }
	    
	   // getMission().setChecklistsList(checkLists);
	    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    boolean ret = getMission().save(false);//FileUtil.saveToFile(userFile.getAbsolutePath(), FileUtil
                //.getAsPrettyPrintFormatedXMLString(getMission().asDocument()));
	    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        if (ret)
        {
            setMissionChanged(false);
 //           JOptionPane.showMessageDialog(this, "Mission saved with success",
 //                   "Success", JOptionPane.INFORMATION_MESSAGE);            
        } 
        else
        {
            refreshMenus();
            return ERROR;
        }

	    refreshMenus();
	    return OK;
    }
	/**
	 * @return Returns the missionChanged.
	 */
	public boolean isMissionChanged() {
		return missionChanged;
	}
	
	
	/**
	 * @param missionChanged The missionChanged to set.
	 */
	public void setMissionChanged(boolean missionChanged) {
		//System.out.println("Mission Changed = "+missionChanged);
		this.missionChanged = missionChanged;
		refreshMenus();
		if (missionChanged) {
			for (int i = 0; i < changeListeners.size(); i++) {
				ChangeEvent ce = new ChangeEvent(this);
				((ChangeListener) changeListeners.get(i)).stateChanged(ce);
			}
		}
	}
	
	/**
	 * This method is called when changes have ocurred
	 */
	public void stateChanged(ChangeEvent e) {		
		
		setMissionChanged(true);
		
		for (int i = 0; i < changeListeners.size(); i++) {
			ChangeListener cl = (ChangeListener) changeListeners.get(i);			
			cl.stateChanged(e);
		}
	}
	
	/**
	 * Adds a new change listener to the current list of listeners<br>
	 * ChangeListeners will receive events whenever the mission has changes
	 * @param cl The new ChangeListener
	 */
	public void addChangeListener(ChangeListener cl) {
		if (!changeListeners.contains(cl))
			changeListeners.add(cl);
	}
	
	/**
	 * Removes the given change listener from the current list of listeners
	 * @see ChangeListener
	 * @param cl The ChangeListener to be removed
	 */
	public void removeChangeListener(ChangeListener cl) {
		changeListeners.remove(cl);
	}
	
	
	/**
	 * This method creates a MissionType object with the current defined mission
	 * @return The updated MissionType
	 */
	public MissionType getUpdatedMission() {
		LinkedHashMap<String, MapMission> maps = new LinkedHashMap<String, MapMission>();
		LinkedHashMap<String, PlanType> plans = new LinkedHashMap<String, PlanType>();
		LinkedHashMap<String, VehicleMission> vehicles = new LinkedHashMap<String, VehicleMission>();
		LinkedHashMap<String, ChecklistMission> clists = new LinkedHashMap<String, ChecklistMission>();

		if (checkListEditors.size() > 0) {
			for (ChecklistPanel clp : checkListEditors.values()) {
				ChecklistType clt = clp.getChecklistType();
				checkLists.put(clt.getName(), clt);
			}
		}
		
		for (ChecklistType clist : checkLists.values()) {
			clists.put(clist.getName(), clist.getCheckListMission());
		}
	    	    
	    // Verifica se existem editores de mapas abertos (se houverem, actualiza os Maps)
	    if (mapEditors.size() > 0) {
	    	Object[] editors = mapEditors.values().toArray();
	    	for (int i = 0; i < editors.length; i++) {
	    		MissionMapEditor mme = (MissionMapEditor) editors[i];
	    		mme.setMissionType(getMission());
	    		MapType mt = mme.getMap();
	    		
	    		mapTypes.put(mt.getId(), mt);
	    	}
	    }
	    
		// Transforma todos os Maps em MapMissions
	    Object[] tmp = mapTypes.values().toArray();
	    for (int i = 0; i < tmp.length; i++) {
	    	MapType map = (MapType) tmp[i];
	    	MapMission mm = new MapMission();
	    	mm.setId(map.getId());
	    	mm.setName(map.getName());
	    	mm.setHref(map.getHref());
	    	mm.setMap(map);
	    	maps.put(mm.getId(), mm);	    
	    }
	    
	    
//	  Verifica se existe algum editor de planos aberto
	    if (planEditors.size() > 0) {
	    	Object[] editors = planEditors.values().toArray();
	    	for (int i = 0; i < editors.length; i++) {
	    		IndividualPlanEditor planEditor = (IndividualPlanEditor) editors[i];
	    		
	    		PlanType ipt = planEditor.getPlan();
	    		planTypes.put(ipt.getId(), ipt);
	    	}
	    }	    
	    
	    // Cria a lista de planos da missao
	    Object[] planKeys = planTypes.keySet().toArray();
	    for (int i = 0; i< planKeys.length; i++) {
	    	Object key = planKeys[i];
	    	PlanType ipt = (PlanType) planTypes.get(key);
	    	plans.put((String)key, ipt);
	    }
	    
	    // Cria a lista de veiculos da missao
	    Object[] vehicleKeys = this.vehicles.keySet().toArray();
	    for (int i = 0; i< vehicleKeys.length; i++) {
	    	Object key = vehicleKeys[i];
	    	vehicles.put((String)key, this.vehicles.get(key));
	    }
	    
	    MissionType mt = getMission();
	    //mt.setDescription(getMission().getDescription());
	    //mt.setHomeRef(getMission().getHomeRef());
	    //mt.setId(getMission().getId());
	    //mt.setName(getMission().getName());
	    //mt.setType(getMission().getType());
	    mt.setMapsList(maps);
	    System.out.println("No of maps: "+maps.size());
	    mt.setPlanList(plans);
	    mt.setVehiclesList(vehicles);
	    mt.setChecklistsList(clists);
	    //mt.setOriginalFilePath(getMission().getOriginalFilePath());
	    
	    return mt;
	}
	
	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */    
	private JMenu getRecentlyOpenFilesMenu() 
	{
		if (recentlyOpenFilesMenu == null) 
		{
			recentlyOpenFilesMenu = new JMenu();
			recentlyOpenFilesMenu.setText("Recently opened");
			RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(
			        recentlyOpenFilesMenu, miscFilesOpened);
		}
		else
		{
			RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(
			        recentlyOpenFilesMenu, miscFilesOpened);		    
		}
		return recentlyOpenFilesMenu;
	}
	
	
	 /**
     * @param type
     */
    private void loadRecentlyOpenedFiles()
    {
        String recentlyOpenedFiles = ConfigFetch.resolvePath(RECENTLY_OPENED_MISSIONS);
        Method methodUpdate = null;
        
        try
        {
            Class<?>[] params = {File.class};
            methodUpdate = this.getClass().getMethod("updateMissionFilesOpened", params);
        } catch (Exception e)
        {
            NeptusLog.pub().error(this + "loadRecentlyOpenedFiles", e);
            return;
        }
        
        if (recentlyOpenedFiles == null)
        {
            //JOptionPane.showInternalMessageDialog(this, "Cannot Load");
            return;
        }
        
        if (!new File(recentlyOpenedFiles).exists())
            return;
 
        RecentlyOpenedFilesUtil.loadRecentlyOpenedFiles(recentlyOpenedFiles,
                methodUpdate, this);
    }
    
    public boolean updateMissionFilesOpened(File fx)
    {
        RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, miscFilesOpened,
                new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        File fx;
                        Object key = e.getSource();
                        File value = miscFilesOpened.get(key);
                        if (value instanceof File)
                        {
                            fx = (File) value;
                            openMission(fx);
                        } else
                            return;
                    }
                });
        getRecentlyOpenFilesMenu();
        storeRecentlyOpenedFiles();
        return true;
    }
    
    private void storeRecentlyOpenedFiles()
    {
        String recentlyOpenedFiles;
        LinkedHashMap<JMenuItem, File> hMap;
        String header;

        recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(RECENTLY_OPENED_MISSIONS);
        hMap = miscFilesOpened;
        header = "Recently opened mission files.";
        
        RecentlyOpenedFilesUtil.storeRecentlyOpenedFiles(
            recentlyOpenedFiles, hMap, header);
    }
	
    public void openMission(File userFile) {
		if (!closeCurrentMission())
			return;
		
		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			final String file = userFile.getAbsolutePath();
			AsyncWorker.post(new AsyncTask() {
				@Override
				public Object run() throws Exception {
					MissionType mt = new MissionType(file);
					
					setMission(mt);
					setMapGroup(MapGroup.getMapGroupInstance(mt));
					setMissionChanged(false);
					
					refreshMenus();
					updateMissionFilesOpened(new File(file));
					return null;
				}
				
				@Override
				public void finish() {					
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					try {
						getResultOrThrow();
					}
					catch (Exception e) {
						GuiUtils.errorMessage(MissionPlanner.this, "Incompatible file type", "The selected file is not supported");
					}
					
				}
			});				
		}
		catch (Exception e) {
			GuiUtils.errorMessage(this, "Error loading mission", "<html>An "+e.getClass().getSimpleName()+ " occured while loading mission:<br>"+e.getMessage()+"</html>");
			NeptusLog.pub().error(this, e);
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
    }
    
	public MapGroup getMapGroup() {
		if (myMapGroup == null) {
			myMapGroup = MapGroup.getMapGroupInstance(myMissionType);
		}
		return myMapGroup;
	}
	
	public static final int IN_MAPS = 1, IN_PLANS = 2, ANYWHERE = 0; 
	public boolean isNameTaken(int where, String name) {
		if (where == 1 || where == 0) {
			if (mapTypes.containsKey(name))
				return true;
		}
		if (where == 2 || where == 0) {
			if (planTypes.containsKey(name))
				return true;
		}
		return false;
	}
	
	public void setMapGroup(MapGroup mapGroup) {
		// Nothing to do..
		this.myMapGroup = mapGroup;
		
		//this.mapGroup = MapGroup.setMissionInstance(getMission().getId(), mapGroup);
	}
		
	public boolean closeCurrentMission() {
		if (myMissionType != null && isMissionChanged()) {	
			if (getMission().getOriginalFilePath() != null) {
				int answer = JOptionPane.showConfirmDialog(this, "<html>Do you want to save the changes to the current <strong>mission</strong>?</html>", "Save changes?", JOptionPane.YES_NO_CANCEL_OPTION);
				if (answer == JOptionPane.NO_OPTION) {
					//myMissionType = null;
				}
				if (answer == JOptionPane.YES_OPTION) {
					int result = saveMission(false);	
					if (result != OK)
						return false;
					//myMissionType = null;
				}
				if (answer == JOptionPane.CANCEL_OPTION) {
					return false;
				}
			}
			else {
				int answer = JOptionPane.showConfirmDialog(this, "<html>Do you want to save the current <strong>mission</strong>?</html>", "Save mission?", JOptionPane.YES_NO_CANCEL_OPTION);
				if (answer == JOptionPane.NO_OPTION) {
					//myMissionType = null;
				}
				if (answer == JOptionPane.YES_OPTION) {
					int result = saveMission(false);
					if (result != OK)
						return false;					
				}			
				if (answer == JOptionPane.CANCEL_OPTION) {
					return false;
				}
			}
		}
		warnMissionListeners(new MissionChangeEvent(
				MissionChangeEvent.TYPE_MISSION_CLOSED));
		return true;
	}
	
	public void addMissionChangeListener(MissionChangeListener listener) {
		if (!missionListeners.contains(listener))
			missionListeners.add(listener);
	}
	
	public void removeMissionChangeListener(MissionChangeListener listener) {
		if (missionListeners.contains(listener))
			missionListeners.remove(listener);
	}
	
	public void warnMissionListeners(MissionChangeEvent event) {
		for (MissionChangeListener mcl : missionListeners) {
			mcl.missionChanged(event);
		}
	}
	
	private String getNewPlanId() {
		int i = 1;
		while (planTypes.containsKey("plan"+i))
			i++;
		return "plan"+i;
	}
	
	private String getNewMapId() {
		int i = 1;
		while (mapTypes.containsKey("map"+i))
			i++;
		return "map"+i;
	}
	
	private String getNewCheckListId() {
		int i = 1;
		while (checkLists.containsKey("checklist"+i))
			i++;
		return "checklist"+i;
	}
	
	public void handleFile(File f) {
		ConfigFetch.setSuperParentFrame(this);
		setMission(new MissionType(f.getAbsolutePath()));
        GuiUtils.centerOnScreen(this);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				super.windowClosed(e);
				OutputMonitor.end();
				System.exit(0);
			}
		});
        setVisible(true);
	}
}
