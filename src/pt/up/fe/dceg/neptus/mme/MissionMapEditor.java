/*
 *  Created on 13/Dez/2004
 */
package pt.up.fe.dceg.neptus.mme;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.IFrameOpener;
import pt.up.fe.dceg.neptus.gui.MapObjectInteraction;
import pt.up.fe.dceg.neptus.gui.MapTreeCellRenderer;
import pt.up.fe.dceg.neptus.gui.NeptusComboButton;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.loader.FileHandler;
import pt.up.fe.dceg.neptus.mme.wms.FetcherWMS;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.mp.MapChangeListener;
import pt.up.fe.dceg.neptus.mp.MissionChangeEvent;
import pt.up.fe.dceg.neptus.mp.MissionPlanner;
import pt.up.fe.dceg.neptus.renderer2d.MissionRenderer;
import pt.up.fe.dceg.neptus.renderer2d.Renderer;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer3d.Renderer3D;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.CylinderElement;
import pt.up.fe.dceg.neptus.types.map.EllipsoidElement;
import pt.up.fe.dceg.neptus.types.map.HomeReferenceElement;
import pt.up.fe.dceg.neptus.types.map.ImageElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.Model3DElement;
import pt.up.fe.dceg.neptus.types.map.ParallelepipedElement;
import pt.up.fe.dceg.neptus.types.map.PathElement;
import pt.up.fe.dceg.neptus.types.map.RotatableElement;
import pt.up.fe.dceg.neptus.types.map.ScalableElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.swing.JOutlookBar;

/**
 * @author Ze Carlos
 */
public class MissionMapEditor extends JPanel implements UndoableEditListener, PropertiesProvider, MapChangeListener,
        ActionListener, MouseListener, MouseMotionListener, TreeSelectionListener, MouseWheelListener, KeyListener,
        FileHandler {

    static final long serialVersionUID = 13;
    public static final int NOT_DRAWING = 1, FREE_HAND_DRAWING = 2, POINT_TO_POINT_DRAWING = 3;
    public static final String SAVEAS_PROPERTY = "file changed";

    private StateRenderer2D mapRenderer;
    private MapType map;
    private AbstractElement selectedObject = null;
    private JLabel statusText = new JLabel(" ");
    private JPanel statusBar = new JPanel();
    private int drawMode = NOT_DRAWING;
    private Float clickX, clickY;
    private PathElement curDrawing = null;
    private JTree ObjectTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode root;

    private String mapHref = "";
    private MapGroup mg;// , cleanMap;
    private boolean mapChanged = false;
    private boolean editable = true;
    private ToolbarSwitch gridSwitch, drawSwitch, zoomSwitch, translateSwitch, rotateSwitch, rulerSwitch, handSwitch,
            drawSwitchP2P, drawShapeSwitch;
    private Vector<ChangeListener> changeListeners = new Vector<ChangeListener>();
    private MissionPlanner parentMP = null;
    private IFrameOpener frameOpener = null;
    private FetcherWMS fetcher = new FetcherWMS();
    private boolean statusBarVisible = true;
    private MapObjectInteraction objInteraction = new MapObjectInteraction(this);
    private JOutlookBar outlook;
    private MissionType missionType;
    private Renderer3D r3d = null;
    boolean is2DVisible = true;
    private JTabbedPane tabPane;

    private int currentViewMode = Renderer.TRANSLATION;
    private boolean rotating = false;
    private boolean scaling = false;
    private Point2D lastPoint = null;
    private UndoManager undoManager = new UndoManager();
    private UndoableEditSupport undoSupport = new UndoableEditSupport();

    private LocationType draggedObjectInitialLocation = null;
    private double draggedObjectInitialAngle = 0;
    private double[] draggedObjectInitialDimmensions = new double[] { 1, 1, 1 };
    private boolean fillShape = false;

    private ToolbarButton undoButton = null, redoButton = null;

    public void switchTo2D() {
        if (tabPane != null) {
            tabPane.setSelectedIndex(0);
        }
    }

    public void switchTo3D() {
        if (tabPane != null && tabPane.getTabCount() > 1) {
            tabPane.setSelectedIndex(1);
        }
    }

    public void setViewMode(int mode) {
        this.currentViewMode = mode;
        mapRenderer.setViewMode(currentViewMode);
        if (r3d != null) {
            r3d.setViewMode(currentViewMode);
        }
    }

    private AbstractElement draggedObject = null;
    private LocationType lastCursorLocation = null;

    private void initListeners() {
        lastPoint = null;
        mapRenderer.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
                rotating = false;
                scaling = false;
                if (isEditable() && mapRenderer.getViewMode() == Renderer.GRAB && curDrawing == null) {
                    LocationType point = mapRenderer.getRealWorldLocation(e.getPoint());        
                    AbstractElement[] objs = mg.getObjectsFromMap(map.getId());
                    Arrays.sort(objs, new Comparator<AbstractElement>() {

                        public int compare(AbstractElement o1, AbstractElement o2) {
                            int diff = o1.getLayerPriority() - o2.getLayerPriority();
                            if (diff != 0)
                                return diff;
                            else
                                return (int) (o1.getCenterLocation().getAllZ() - o2.getCenterLocation()
                                        .getAllZ());
                        }
                    });
                    AbstractElement mo;
                    for (int i = objs.length - 1; i >= 0; i--) {
                        mo = objs[i];
                        
                        if (mo.containsPoint(point, mapRenderer)) {
                            System.out.println(mo.getId());
                            draggedObject = mo;
                            lastCursorLocation = mapRenderer.getRealWorldLocation(e.getPoint());
                            if (selectedObject != null)
                                selectedObject.setSelected(false);
                            selectedObject = draggedObject;
                            setTreeSelection(selectedObject);
                            mapRenderer.setCursor(StateRenderer2D.grab2Cursor);
                            if (e.isAltDown())
                                rotating = true;
                            if (e.isShiftDown())
                                scaling = true;

                            draggedObjectInitialLocation = new LocationType(draggedObject.getCenterLocation());

                            if (rotating && draggedObject instanceof RotatableElement) {
                                draggedObjectInitialAngle = ((RotatableElement) draggedObject).getYaw();
                            }

                            if (scaling && draggedObject instanceof ScalableElement) {
                                draggedObjectInitialDimmensions = ((ScalableElement) draggedObject).getDimension();
                            }
                            return;
                        }
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
                if (draggedObject != null) {
                    MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                    mce.setChangedObject(draggedObject);
                    mce.setChangeType(MapChangeEvent.OBJECT_MOVED);
                    mce.setSourceMap(draggedObject.getParentMap());
                    mce.setMapGroup(mg);
                    mg.warnListeners(mce);

                    if (!rotating && !scaling) {
                        if (draggedObjectInitialLocation.getDistanceInMeters(draggedObject.getCenterLocation()) != 0) {
                            MoveObjectEdit edit = new MoveObjectEdit(draggedObject, draggedObjectInitialLocation,
                                    draggedObject.getCenterLocation());
                            undoSupport.postEdit(edit);
                            warnChangeListeners();
                        }
                    }

                    if (rotating && draggedObject instanceof RotatableElement) {
                        RotateObjectEdit edit = new RotateObjectEdit(draggedObject, draggedObjectInitialAngle,
                                ((RotatableElement) draggedObject).getYaw());
                        undoSupport.postEdit(edit);
                        warnChangeListeners();
                    }

                    if (scaling && draggedObject instanceof ScalableElement) {
                        ScaleObjectEdit edit = new ScaleObjectEdit(draggedObject, draggedObjectInitialDimmensions,
                                ((ScalableElement) draggedObject).getDimension());
                        undoSupport.postEdit(edit);
                        warnChangeListeners();
                    }

                    mapRenderer.setCursor(StateRenderer2D.grabCursor);
                    draggedObject = null;
                    lastCursorLocation = null;
                    rotating = scaling = false;
                }
            }
        });

        mapRenderer.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {

                if (draggedObject == null)
                    return;

                if (lastPoint != null && rotating && draggedObject instanceof RotatableElement) {
                    RotatableElement rot = (RotatableElement) draggedObject;
                    rot.rotateRight(e.getY() - lastPoint.getY());
                    lastPoint = e.getPoint();
                    return;
                }

                if (lastPoint != null && scaling && draggedObject instanceof ScalableElement) {
                    ScalableElement smo = (ScalableElement) draggedObject;
                    double diff = lastPoint.getY() - e.getPoint().getY();

                    if (diff > 0)
                        smo.grow(diff);
                    else
                        smo.shrink(-diff);
                    lastPoint = e.getPoint();

                    return;
                }

                LocationType lt = mapRenderer.getRealWorldLocation(e.getPoint());
                double offsets[] = lt.getOffsetFrom(lastCursorLocation);

                draggedObject.getCenterLocation().translatePosition(offsets[0], offsets[1], 0);
                lastCursorLocation = lt;

                mapRenderer.forceRepaint();
            }
        });
    }

    public void addChangeListener(ChangeListener l) {
        changeListeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        changeListeners.remove(l);
    }

    private void warnChangeListeners() {
        for (int i = 0; i < changeListeners.size(); i++) {
            ((ChangeListener) changeListeners.get(i)).stateChanged(new ChangeEvent(this));
        }
    }

    public MissionMapEditor(MissionType missionType, MapType map, boolean editable) {
        super();
        this.map = map;
        mg = MapGroup.getNewInstance(missionType.getHomeRef());
        mapRenderer = new StateRenderer2D(mg);
        mapRenderer.setEditingMap(map.getId());

        setEditable(editable);

        createInterface();
        mapHref = map.getHref();
        setMapChanged(map.isChanged());
        setViewMode(StateRenderer2D.TRANSLATION);

        setMapGroup(mg);
        initListeners();
    }

    public MissionMapEditor(MapType map, CoordinateSystem homeReferential, boolean editable) {
        super();

        this.map = map;

        mg = MapGroup.getNewInstance(homeReferential);
        mapRenderer = new StateRenderer2D(mg);
        mapRenderer.setEditingMap(map.getId());

        setEditable(editable);

        createInterface();
        mapHref = map.getHref();

        setMapChanged(map.isChanged());
        setViewMode(StateRenderer2D.TRANSLATION);

        setMapGroup(mg);
        initListeners();
    }

    /**
     * Creates a MissionMapEditor with a default map loaded (no obstacles, 30x30x30)
     */
    public MissionMapEditor(String id, CoordinateSystem homeRef) {
        super();
        this.map = new MapType(new LocationType());
        map.setName(id);
        map.setId(id);
        map.setCenterLocation(homeRef);
        mg = MapGroup.getNewInstance(homeRef);
        mapRenderer = new StateRenderer2D(mg);
        mapRenderer.setEditingMap(map.getId());

        // mg.addChangeListener(mapRenderer);
        createInterface();

        setMapChanged(true);
        setViewMode(Renderer.TRANSLATION);

        setMapGroup(mg);
        initListeners();
    }

    public MissionMapEditor(ConsoleLayout console, MapType map) {
        super();
        this.map = map;
        this.mapHref = map.getHref();
        mg = MapGroup.getMapGroupInstance(console.getMission());
        mapRenderer = new StateRenderer2D(mg);
        setViewMode(Renderer.TRANSLATION);
        mg.addChangeListener(this);
        HomeReferenceElement homeRef = mg.getHomeRef();
        createInterface();
        clearTree();
        constructTree();
        addObjectToTree(homeRef);
        setMapChanged(false);
        initListeners();
    }

    public MissionMapEditor() {
        super();
    }

    /**
     * Creates the Editor interface
     * 
     */
    private void createInterface() {

        this.setLayout(new BorderLayout());

        root = new DefaultMutableTreeNode("Map Objects");
        treeModel = new DefaultTreeModel(root);
        ObjectTree = new JTree(treeModel);
        ObjectTree.setCellRenderer(new MapTreeCellRenderer());
        ObjectTree.addMouseListener(this);
        ObjectTree.addTreeSelectionListener(this);
        ObjectTree.addKeyListener(this);
        ObjectTree.setRootVisible(false);
        constructTree();
        undoSupport.addUndoableEditListener(this);
        outlook = new JOutlookBar();
        outlook.setTabPlacement(JTabbedPane.LEFT);

        JSplitPane split = new JSplitPane();
        JScrollPane scroll = new JScrollPane(ObjectTree);
        outlook.add(scroll, "Map Objects");
        outlook.add(objInteraction, "Object Interaction");
        outlook.setEnabledAt(1, false);

        split.add(outlook, JSplitPane.LEFT);
        split.setResizeWeight(0.1);

        split.add(mapRenderer, JSplitPane.RIGHT);

        this.add(split, BorderLayout.CENTER);
        mapRenderer.addMouseListener(this);
        mapRenderer.addMouseMotionListener(this);
        mapRenderer.addMouseWheelListener(this);
        mapRenderer.setMinDelay(0);

        statusBar.add(statusText);
        if (isStatusBarVisible())
            this.add(statusBar, BorderLayout.SOUTH);

        this.add(createToolbar(), BorderLayout.NORTH);
    }

    public JToolBar createToolbar() {

        ClassLoader cl = getClass().getClassLoader();
        JToolBar toolbar = new JToolBar();
        ToolbarButton tb;

        if (isEditable()) {

            tb = new ToolbarButton("images/buttons/undo.png", undoManager.getUndoPresentationName(), "undo");
            tb.addActionListener(this);
            tb.setEnabled(undoManager.canUndo());
            undoButton = tb;
            toolbar.add(tb);

            tb = new ToolbarButton("images/buttons/redo.png", undoManager.getRedoPresentationName(), "redo");
            tb.addActionListener(this);
            tb.setEnabled(undoManager.canRedo());
            redoButton = tb;
            toolbar.add(tb);

            toolbar.addSeparator();

            drawSwitch = new ToolbarSwitch("images/buttons/new_drawing.png",
                    "<html>Add free-hand <b>drawing</b> to the current map</html>", "draw", cl);
            drawSwitch.setState(false);
            drawSwitch.addActionListener(this);
            toolbar.add(drawSwitch);

            drawSwitchP2P = new ToolbarSwitch("images/buttons/drawing-point.png",
                    "<html>Add Point to Point <b>drawing</b> to the current map</html>", "drawp2p", cl);
            drawSwitchP2P.setState(false);
            drawSwitchP2P.addActionListener(this);
            toolbar.add(drawSwitchP2P);

            drawShapeSwitch = new ToolbarSwitch("images/buttons/shape.png",
                    "<html>Add a free-form <b>shape</b> to the current map</html>", "shape", cl);
            drawShapeSwitch.setState(false);
            drawShapeSwitch.addActionListener(this);
            toolbar.add(drawShapeSwitch);

            NeptusComboButton comboButton = new NeptusComboButton();
            comboButton.addAction("Add Mark", "<html>Add a new <b>waypoint</b> to the map</html>",
                    ImageUtils.getImage("images/buttons/addpoint.png"));
            comboButton.addAction("Add Parallelepiped", "<html>Add a new <b>paralellepiped</b> to the map</html>",
                    ImageUtils.getImage("images/buttons/new_rectangle.png"));
            comboButton.addAction("Add Ellipsoid", "<html>Add a new <b>ellipsoid</b> to the map</html>",
                    ImageUtils.getImage("images/buttons/new_ellipse.png"));
            comboButton.addAction("Add Cylinder", "<html>Add a new <b>cylinder</b> to the map</html>",
                    ImageUtils.getImage("images/buttons/cylinder.png"));
            comboButton.addAction("Add Image", "<html>Add a new <b>image</b> to the map</html>",
                    ImageUtils.getImage("images/buttons/new_image.png"));
            comboButton.addAction("Add 3D Model", "<html>Add a new <b>3D Model</b> to the map</html>",
                    ImageUtils.getImage("images/buttons/model3d.png"));
            comboButton.addAction("Add Transponder", "<html>Add a new acoustic <b>transponder</b> to the map</html>",
                    ImageUtils.getImage("images/transponder.png"));

            // if (MonSense == null) {
            // comboButton.addAction("Add Transponder", "<html>Add a new acoustic <b>transponder</b> to the map</html>",
            // GuiUtils.getImage("images/transponder.png"));
            // if (!ConfigFetch.isOnLockedMode())
            // comboButton.addAction("Add Sensor", "<html>Add a new <b>wireless sensor</b> to the map</html>",
            // GuiUtils.getImage("images/buttons/netdevice.png"));
            // }

            comboButton.addActionListener(this);
            toolbar.add(comboButton);

            tb = new ToolbarButton("images/buttons/delete_obj.png", "<html><b>Remove</b> the selected object</html>",
                    "remove");
            tb.addActionListener(this);
            toolbar.add(tb);

        }
        else {
            // null pointer exceptions...
            drawSwitch = new ToolbarSwitch("images/buttons/new_drawing.png", "dummy", "dummy", cl);
            drawSwitchP2P = drawSwitch;
        }

        toolbar.addSeparator();

        zoomSwitch = new ToolbarSwitch("images/buttons/zoom_btn.png", "<html><strong>Zoom</strong> in and out</html>",
                "zoom", cl);
        zoomSwitch.setState(false);
        zoomSwitch.addActionListener(this);
        toolbar.add(zoomSwitch);

        translateSwitch = new ToolbarSwitch("images/buttons/translate_btn.png",
                "<html>Move around the map (translate mode)</html>", "translate", cl);
        translateSwitch.setState(true);
        translateSwitch.addActionListener(this);
        toolbar.add(translateSwitch);

        rotateSwitch = new ToolbarSwitch("images/buttons/rotate_btn.png", "<html>Rotate the map (rotate mode)</html>",
                "rotate", cl);
        rotateSwitch.setState(false);
        rotateSwitch.setEnabled(false);
        rotateSwitch.addActionListener(this);
        toolbar.add(rotateSwitch);

        rulerSwitch = new ToolbarSwitch("images/buttons/ruler_btn.png",
                "<html>Use the <strong>Ruler</strong> to measure distances</html>", "ruler", cl);
        rulerSwitch.setState(false);
        rulerSwitch.addActionListener(this);
        toolbar.add(rulerSwitch);

        handSwitch = new ToolbarSwitch("images/buttons/hand_btn.png",
                "<html><strong>Move</strong> objects in the map</html>", "grab", cl);
        handSwitch.setState(false);
        handSwitch.addActionListener(this);
        toolbar.add(handSwitch);

        gridSwitch = new ToolbarSwitch("images/buttons/grid.png",
                "<html>Show a dimensional <strong>Grid</strong></html>", "grid", cl);
        gridSwitch.setState(false);
        gridSwitch.addActionListener(this);
        toolbar.add(gridSwitch);

        tb = new ToolbarButton("images/buttons/settings.png",
                "<html>View or alter the <b>settings</b> of the Map Editor.</html>", "settings");
        tb.addActionListener(this);
        toolbar.add(tb);

        return toolbar;
    }

    /**
     * This method (re)builds the left tree component where all the MapObjects are showed.
     * 
     */
    private void constructTree() {
        NeptusLog.pub().debug("[MME] Recreating object tree...");
        root.removeFromParent();
        root = new DefaultMutableTreeNode("Map Objects");
        treeModel.setRoot(root);

        if (map != null) {
            AbstractElement mo;
            Object[] mapObjects = map.getObjects().toArray();
            for (int i = 0; i < mapObjects.length; i++) {
                mo = (AbstractElement) mapObjects[i];
                addObjectToTree(mo);
            }
        }
        repaint();
    }

    /**
     * This method is called whenever the user selects a different node in the Objects Tree
     */
    public void valueChanged(TreeSelectionEvent e) {

        if (selectedObject != null) {
            selectedObject.setSelected(false);
            selectedObject = null;
            outlook.setEnabledAt(1, false);

        }

        if (e.isAddedPath()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) ObjectTree.getSelectionPath().getLastPathComponent();

            if (node.getUserObject() instanceof AbstractElement) {
                selectedObject = (AbstractElement) node.getUserObject();
                selectedObject.setSelected(true);
                if (!(selectedObject instanceof HomeReferenceElement) && isEditable()) {
                    selectedObject.setMapGroup(mg);
                    selectedObject.setParentMap(map);
                    objInteraction.setInteractingObject(selectedObject);

                    outlook.setEnabledAt(1, true);
                }
            }
        }

        mapRenderer.repaint();
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mg.warnListeners(mce);
        // map.warnChangeListeners(mce);
    }

    /**
     * This method returns the tree node corresponding to a given map object
     * 
     * @param obj A MapObject
     * @return The corresponding tree node
     */
    private DefaultMutableTreeNode getTreeNode(AbstractElement obj) {
        int rootChildren = treeModel.getChildCount(root);
        DefaultMutableTreeNode parent = null;
        for (int i = 0; i < rootChildren; i++) {
            DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) treeModel.getChild(root, i);
            if (tmp.getUserObject().equals(obj.getType() + "s")) {
                parent = tmp;
                break;
            }
        }
        if (parent == null)
            return null;

        int brotherhood = treeModel.getChildCount(parent);

        for (int i = 0; i < brotherhood; i++) {
            DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) treeModel.getChild(parent, i);
            if (tmp.getUserObject() == obj) {            // if (MonSense == null && console == null) {
                //
                // tb = new ToolbarButton("images/buttons/edit.png", "<html>View/Edit the <b>map info</b></html>",
                // "mapinfo");
                // tb.addActionListener(this);
                // toolbar.add(tb);
                //
                // toolbar.addSeparator();
                //
                // tb = new ToolbarButton("images/buttons/save.png", "<html><b>Save</b> this map to a file</html>", "save");
                // tb.addActionListener(this);
                // toolbar.add(tb);
                //
                // tb = new ToolbarButton("images/buttons/saveas.png", "<html><b>Save</b> this map to a new file...</html>",
                // "saveas");
                // tb.addActionListener(this);
                // toolbar.add(tb);
                //
                // tb = new ToolbarButton("images/buttons/revert.png",
                // "<html><b>Revert</b> the changes made to this map</html>", "revert");
                // tb.addActionListener(this);
                // toolbar.add(tb);
                //
                // tb = new ToolbarButton("images/buttons/open.png", "<html><b>Open</b> an existing map</html>", "open");
                // tb.addActionListener(this);
                // toolbar.add(tb);
                //
                // toolbar.addSeparator();
                //
                // }
                return tmp;
            }
        }
        return null;
    }

    /**
     * Alters the tree selection so that the given object is selected If the <B>obj</B> parameter is null then the root
     * node is selected
     * 
     * @param obj A MapObject to be selected
     */
    private void setTreeSelection(AbstractElement obj) {

        if (obj == null) {
            ObjectTree.setSelectionPath(new TreePath(root));
            return;
        }

        DefaultMutableTreeNode node = getTreeNode(obj);

        if (node == null) {
            ObjectTree.setSelectionPath(new TreePath(root));
            return;
        }

        TreePath selPath = new TreePath(treeModel.getPathToRoot(node));

        ObjectTree.setSelectionPath(selPath);
        ObjectTree.scrollPathToVisible(selPath);
    }

    /**
     * Removes an object from the tree
     * 
     * @param obj The MapObject to be removed from the tree
     */
    public void removeObjectFromTree(AbstractElement obj) {
        DefaultMutableTreeNode node = getTreeNode(obj);
        if (node != null) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            treeModel.removeNodeFromParent(node);
            if (parent.getChildCount() == 0)
                treeModel.removeNodeFromParent(parent);
        }
    }

    /**
     * Removes all objects from the tree
     * 
     */
    private void clearTree() {
        root.removeAllChildren();
        treeModel.reload();
    }

    /**
     * Adds a new object to the Object Tree. If the type of the node to add doesn't exist it will be added as its father
     * 
     * @param obj The MapObject to add to the tree
     */
    public void addObjectToTree(AbstractElement obj) {

        int numTypes = treeModel.getChildCount(root);
        DefaultMutableTreeNode parent = null;

        if ("Home Reference".equals(obj.getName())) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(obj);
            treeModel.insertNodeInto(child, root, 0);
            ObjectTree.scrollPathToVisible(new TreePath(child.getPath()));
            return;
        }
        else {
            for (int i = 0; i < numTypes; i++) {
                DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) treeModel.getChild(root, i);
                if (tmp.toString().equals(obj.getType() + "s")) {
                    parent = tmp;
                    break;
                }
            }
        }
        if (parent == null) {
            parent = new DefaultMutableTreeNode(obj.getType() + "s");
            treeModel.insertNodeInto(parent, root, root.getChildCount());
        }

        DefaultMutableTreeNode child = new DefaultMutableTreeNode(obj);
        treeModel.insertNodeInto(child, parent, parent.getChildCount());
        ObjectTree.scrollPathToVisible(new TreePath(child.getPath()));

    }

    /**
     * Called when an action occurs
     */
    public void actionPerformed(ActionEvent action) {

        if ("coords:ppiped".equals(action.getActionCommand())) {
            newObstacleDialog(this, "Add Parallelepiped", clickX, clickY);
        }

        if ("coords:drawing".equals(action.getActionCommand())) {
            newObstacleDialog(this, "drawing", clickX, clickY);
        }

        if ("coords:ellipsoid".equals(action.getActionCommand())) {
            newObstacleDialog(this, "Add Ellipsoid", clickX, clickY);
        }

        if ("Add Parallelepiped".equals(action.getActionCommand())) {
            newObstacleDialog(this, "ppiped", null, null);

        }

        if ("Add Ellipsoid".equals(action.getActionCommand())) {
            newObstacleDialog(this, "ellipsoid", null, null);
        }

        if ("Add Cylinder".equals(action.getActionCommand())) {
            newObstacleDialog(this, "cylinder", null, null);
        }

        if ("Add Mark".equals(action.getActionCommand())) {
            newObstacleDialog(this, "waypoint", null, null);
        }

        if ("Add Transponder".equals(action.getActionCommand())) {
            newObstacleDialog(this, "transponder", null, null);
        }

        if ("Add Image".equals(action.getActionCommand())) {
            newObstacleDialog(this, "image", null, null);
        }

        if ("Add 3D Model".equals(action.getActionCommand())) {
            newObstacleDialog(this, "3dmodel", null, null);
        }

        if ("remove".equals(action.getActionCommand())) {
            if (selectedObject != null) {
                removeObjectFromTree(selectedObject);
                map.remove(selectedObject.getId());
                selectedObject = null;
                repaint();
                setMapChanged(true);
            }
        }

        if ("open".equals(action.getActionCommand())) {
            openMap();
        }

        if ("save".equals(action.getActionCommand())) {

            boolean saved = false;

            if (mapHref.length() > 0) {
                saved = map.saveFile(mapHref);
            }
            else {
                saved = map.showSaveDialog();
            }

            if (saved) {
                mapHref = map.getHref();
                setMapChanged(false);
            }
        }

        if ("saveas".equals(action.getActionCommand())) {

            boolean saved = false;
            saved = map.showSaveDialog();

            if (saved) {
                mapHref = map.getHref();
                setMapChanged(false);
                // System.err.println("Saved the map "+map.getId()+ " to file "+map.getHref());
            }
        }

        if ("draw".equals(action.getActionCommand())) {
            switchTo2D();
            if (drawSwitch.isSelected()) {
                zoomSwitch.setState(false);
                translateSwitch.setState(false);
                rulerSwitch.setState(false);
                drawSwitchP2P.setState(false);
                rotateSwitch.setState(false);
                drawShapeSwitch.setState(false);
                setViewMode(Renderer.NONE);
                mapRenderer.setCursor(StateRenderer2D.drawCursor);
                // mapRenderer.setIgnoreRightClicks(true);
                this.drawMode = FREE_HAND_DRAWING;
                fillShape = false;
            }
            else {
                setViewMode(Renderer.TRANSLATION);
                translateSwitch.setState(true);
                this.drawMode = NOT_DRAWING;
                mapRenderer.setIgnoreRightClicks(false);
            }
        }

        if ("drawp2p".equals(action.getActionCommand())) {
            switchTo2D();
            if (drawSwitchP2P.isSelected()) {
                zoomSwitch.setState(false);
                translateSwitch.setState(false);
                rulerSwitch.setState(false);
                drawSwitch.setState(false);
                rotateSwitch.setState(false);
                handSwitch.setState(false);
                drawShapeSwitch.setState(false);
                setViewMode(Renderer.NONE);
                mapRenderer.setCursor(StateRenderer2D.drawCursor);
                mapRenderer.setIgnoreRightClicks(true);
                this.drawMode = POINT_TO_POINT_DRAWING;
                fillShape = false;
            }
            else {
                setViewMode(Renderer.TRANSLATION);
                translateSwitch.setState(true);
                this.drawMode = NOT_DRAWING;
                mapRenderer.setIgnoreRightClicks(false);
            }
        }

        if ("shape".equals(action.getActionCommand())) {
            switchTo2D();
            if (drawShapeSwitch.isSelected()) {
                zoomSwitch.setState(false);
                translateSwitch.setState(false);
                rulerSwitch.setState(false);
                drawSwitch.setState(false);
                rotateSwitch.setState(false);
                handSwitch.setState(false);
                drawSwitchP2P.setState(false);
                setViewMode(Renderer.NONE);
                mapRenderer.setCursor(StateRenderer2D.drawCursor);
                mapRenderer.setIgnoreRightClicks(true);
                this.drawMode = POINT_TO_POINT_DRAWING;
                fillShape = true;
            }
            else {
                setViewMode(Renderer.TRANSLATION);
                translateSwitch.setState(true);
                this.drawMode = NOT_DRAWING;
                mapRenderer.setIgnoreRightClicks(false);
            }
        }

        if ("zoom".equals(action.getActionCommand())) {
            if (zoomSwitch.isSelected()) {
                drawSwitch.setState(false);
                translateSwitch.setState(false);
                rulerSwitch.setState(false);
                drawSwitchP2P.setState(false);
                rotateSwitch.setState(false);
                handSwitch.setState(false);
                drawShapeSwitch.setState(false);
                setViewMode(Renderer.ZOOM);
                this.drawMode = NOT_DRAWING;
            }
            else {
                setViewMode(Renderer.TRANSLATION);
                translateSwitch.setState(true);
            }
        }

        if ("translate".equals(action.getActionCommand())) {
            if (translateSwitch.isSelected()) {
                zoomSwitch.setState(false);
                drawSwitch.setState(false);
                rulerSwitch.setState(false);
                drawSwitchP2P.setState(false);
                rotateSwitch.setState(false);
                handSwitch.setState(false);
                drawShapeSwitch.setState(false);
                setViewMode(Renderer.TRANSLATION);
                this.drawMode = NOT_DRAWING;
            }
            else {
                setViewMode(Renderer.TRANSLATION);
                translateSwitch.setState(true);
            }
        }

        if ("rotate".equals(action.getActionCommand())) {
            if (rotateSwitch.isSelected()) {
                zoomSwitch.setState(false);
                drawSwitch.setState(false);
                rulerSwitch.setState(false);
                translateSwitch.setState(false);
                drawSwitchP2P.setState(false);
                handSwitch.setState(false);
                drawShapeSwitch.setState(false);
                switchTo3D();
                setViewMode(Renderer.ROTATION);
                this.drawMode = NOT_DRAWING;
            }
            else {
                setViewMode(Renderer.TRANSLATION);
                translateSwitch.setState(true);
            }
        }

        if ("grab".equalsIgnoreCase(action.getActionCommand())) {
            if (handSwitch.isSelected()) {
                zoomSwitch.setState(false);
                drawSwitch.setState(false);
                rulerSwitch.setState(false);
                translateSwitch.setState(false);
                drawSwitchP2P.setState(false);
                rotateSwitch.setState(false);
                drawShapeSwitch.setState(false);
                switchTo2D();
                setViewMode(Renderer.GRAB);
                this.drawMode = NOT_DRAWING;
            }
            else {
                setViewMode(Renderer.TRANSLATION);
                translateSwitch.setState(true);
            }
        }

        if ("ruler".equals(action.getActionCommand())) {
            if (rulerSwitch.isSelected()) {
                zoomSwitch.setState(false);
                drawSwitch.setState(false);
                drawSwitchP2P.setState(false);
                translateSwitch.setState(false);
                rotateSwitch.setState(false);
                handSwitch.setState(false);
                drawShapeSwitch.setState(false);
                setViewMode(Renderer.RULER);
                this.drawMode = NOT_DRAWING;
            }
            else {
                setViewMode(Renderer.TRANSLATION);
                translateSwitch.setState(true);
            }

        }

        if ("mapinfo".equals(action.getActionCommand())) {
            MapParameters params = new MapParameters();
            params.setMapParameters(map);
            setMapChanged(true);
        }

        if ("3d".equals(action.getActionCommand())) {

            MapGroup tmp = mg;
            tmp.addMap(map);

            final MissionRenderer mr = new MissionRenderer(null, tmp, MissionRenderer.R3D_1CAM);

            if (getFrameOpener() != null) {
                JInternalFrame jif = getFrameOpener().createFrame(getMap().getName() + " 3D Preview", "Preview3D", mr);

                jif.addInternalFrameListener(new InternalFrameAdapter() {

                    public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent arg0) {
                        try {
                            ((JInternalFrame) arg0.getSource()).setIcon(true);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(this, e);
                        }
                    }

                    public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent arg0) {
                    }
                });
            }
            else {
                // System.err.println("Frame Opener is null!");

                Component rootInstance = SwingUtilities.getRoot(this);
                JDialog newFrame = null;
                if (rootInstance instanceof JDialog) {
                    newFrame = new JDialog((JDialog) rootInstance, map.getId() + " 3D View");
                }
                else {
                    newFrame = new JDialog((JFrame) rootInstance, map.getId() + " 3D View");
                }

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
        }

        if ("wms".equals(action.getActionCommand())) {

            Runnable wmsFetch = new Runnable() {
                public void run() {
                    LocationType topLeft = mapRenderer.getRealWorldLocation(new Point2D.Double(0, 0));
                    LocationType bottomRight = mapRenderer.getRealWorldLocation(new Point2D.Double(mapRenderer
                            .getWidth(), mapRenderer.getHeight()));

                    fetcher.setTopLeft(topLeft);
                    fetcher.setBottomRight(bottomRight);
                    fetcher.setImageHeight(mapRenderer.getHeight());
                    fetcher.setImageWidth(mapRenderer.getWidth());

                    Image img = fetcher.fetchImage();
                    ImageElement obj = new ImageElement(mg, map);
                    obj.setCenterLocation(mapRenderer.getCenter());
                    obj.setImageScale(1.0 / mapRenderer.getZoom());
                    obj.setImage(img);

                    MediaTracker mt = new MediaTracker(new JLabel());
                    mt.addImage(img, 0);
                    try {
                        mt.waitForAll();
                    }
                    catch (Exception e) {
                    }

                    mapRenderer.getMapGroup().removeMap("wms");
                    MapType m = new MapType(new LocationType());
                    m.addObject(obj);
                    mapRenderer.getMapGroup().addMap(m);
                    MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
                    mce.setChangedObject(obj);

                    mapRenderer.repaint();
                }
            };

            Thread t = new Thread(wmsFetch);
            t.start();

        }

        if ("settings".equals(action.getActionCommand())) {
            PropertiesEditor.editProperties(this, true);
        }

        if ("Add Sensor".equals(action.getActionCommand())) {
            // GuiUtils.errorMessage("Not done yet", "Falta fazer isto!");
            newObstacleDialog(this, "mote", null, null);
        }

        if ("grid".equals(action.getActionCommand())) {
            if (gridSwitch.isSelected()) {
                String sizeStr = JOptionPane.showInputDialog(this, "Please enter a grid cell size",
                        "" + mapRenderer.getGridSize());
                double cellSize;
                try {
                    cellSize = Double.parseDouble(sizeStr);
                }
                catch (Exception e) {
                    cellSize = 100.0;
                }
                mapRenderer.setGridSize(cellSize);
                mapRenderer.setGridShown(true);
            }
            else {
                mapRenderer.setGridShown(false);
            }
            mapRenderer.repaint();
        }

        if ("undo".equalsIgnoreCase(action.getActionCommand())) {
            undoManager.undo();
            refreshUndoRedo();
        }

        if ("redo".equalsIgnoreCase(action.getActionCommand())) {
            undoManager.redo();
            refreshUndoRedo();
        }

        if ("revert".equalsIgnoreCase(action.getActionCommand())) {
            getMap().loadFile(getMap().getHref());
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.MAP_RESET);
            constructTree();
            mce.setMapGroup(getMap().getMapGroup());
            mce.setSourceMap(getMap());
            getMap().warnChangeListeners(mce);
            setMapChanged(false);

        }

    }

    public boolean addExistingMapToMission(Component parent, File basedir) {
        int response;

        JFileChooser fc = new JFileChooser();
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
        fc.setCurrentDirectory(fx);
        // fc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
        fc.setFileView(new NeptusFileView());
        fc.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                try {
                    if (!f.exists() || !f.canRead())
                        return false;
                    if (f.getCanonicalPath().endsWith("xml") || f.getCanonicalPath().endsWith("nmap"))
                        return true;
                }
                catch (Exception e) {
                }
                return false;
            }

            public String getDescription() {
                return "Neptus Mission Map files (.nmap, .xml)";
            }
        });
        response = fc.showOpenDialog((parent != null) ? parent : this);
        if (response == JFileChooser.CANCEL_OPTION)
            return false;
        File file = fc.getSelectedFile();

        try {
            MapType mt = new MapType(file.getAbsolutePath());
            this.map = mt;
            mapRenderer.setMap(map);
            map.addChangeListener(mapRenderer);
            mapHref = mt.getHref();
            setMapChanged(false);
            constructTree();
            repaint();
            return true;
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "<html>Unable to read the selected file.<br>Try selecting another file...<br><font color=red>"
                            + e.getMessage() + "</font></html>", "Error reading file", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(System.out);
            return false;
        }
    }

    /**
     * Opens an existing map
     */
    public boolean openMap() {
        int response;
        if (getParentMP() != null) {
            response = JOptionPane.showConfirmDialog(this, "Do you wish to remove the current map from this mission?");

            if (response == JOptionPane.CANCEL_OPTION) {
                return false;
            }

            if (response == JOptionPane.YES_OPTION) {
                getParentMP().removeMap(getMap());
            }
        }

        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
        fc.setFileView(new NeptusFileView());
        response = fc.showOpenDialog(this);
        if (response == JFileChooser.CANCEL_OPTION)
            return false;
        File file = fc.getSelectedFile();
        fc.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                try {
                    if (!f.exists() || !f.canRead())
                        return false;
                    if (f.getCanonicalPath().endsWith("xml") || f.getCanonicalPath().endsWith("xml"))
                        return true;
                }
                catch (Exception e) {
                }
                return false;
            }

            public String getDescription() {
                return "Neptus Mission Map files";
            }
        });

        try {
            MapType mt = new MapType(file.getAbsolutePath());
            map = mt;
            mapRenderer.setMap(map);
            map.addChangeListener(mapRenderer);
            mapHref = mt.getHref();

            if (getParentMP() != null)
                getParentMP().addMap(getMap());

            setMapChanged(false);
            constructTree();
            repaint();
            return true;
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "<html>Unable to read the selected file.<br>Try selecting another file...<br><font color=red>"
                            + e.getMessage() + "</font></html>", "Error reading file", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(System.out);
            return false;
        }

    }

    /**
     * Creates a new dialog allowing the user to add a new obstacle to the current mission map
     * 
     * @param editor The MissionMapEditor Object that is calling this method
     * @param centerX The clicked map x coord or <b>null</b> if there wasn't a click
     * @param centerY The clicked map y coord or <b>null</b> if there wasn't a click
     */
    public void newObstacleDialog(MissionMapEditor editor, String type, Float centerX, Float centerY) {

        AbstractElement mo = null;
        if ("ppiped".equals(type))
            mo = new ParallelepipedElement(mg, map);
        if ("ellipsoid".equals(type))
            mo = new EllipsoidElement(mg, map);
        if ("cylinder".equals(type))
            mo = new CylinderElement(mg, map);
        if ("waypoint".equals(type))
            mo = new MarkElement(mg, map);

        if ("transponder".equals(type))
            mo = new TransponderElement(mg, map);

        if ("image".equals(type)) {
            mo = new ImageElement(mg, map);
        }

        // if ("mote".equals(type)) {
        // mo = new SensorElement(mg, map);
        // }

        if ("3dmodel".equals(type)) {
            mo = new Model3DElement(mg, map);
        }

        if (mo == null)
            return;

        mo.showParametersDialog(this, map.getObjectNames(), this.map, true);

        if (!mo.userCancel) {
            map.addObject(mo);
            setMapChanged(true);
            addObjectToTree(mo);
            setTreeSelection(mo);
            if (getParentMP() != null)
                getParentMP().warnMissionListeners(new MissionChangeEvent(MissionChangeEvent.TYPE_MAPOBJECT_ADDED, mo));

            AddObjectEdit edit = new AddObjectEdit(this, mo);
            undoSupport.postEdit(edit);
        }

        mapRenderer.repaint();
    }

    // Unitary test...
    public static void main(String args[]) {

        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();
        final MissionMapEditor mme = new MissionMapEditor("teste", new CoordinateSystem());
        JFrame frame = new JFrame("Map Editor");
        frame.getContentPane().add(mme);
        frame.setSize(500, 400);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GuiUtils.centerOnScreen(frame);
        frame.setVisible(true);
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == 127 && e.getSource() == ObjectTree && isEditable()) {
            if (ObjectTree.getSelectionPath() == null)
                return;

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) ObjectTree.getSelectionPath().getLastPathComponent();
            if (node.getUserObject() instanceof AbstractElement) {
                AbstractElement tmp = (AbstractElement) node.getUserObject();

                if (selectedObject == tmp)
                    selectedObject = null;

                map.remove(tmp.getName());
                setMapChanged(true);
                removeObjectFromTree(tmp);
                if (getParentMP() != null)
                    getParentMP().warnMissionListeners(
                            new MissionChangeEvent(MissionChangeEvent.TYPE_MAPOBJECT_REMOVED, tmp));

                RemoveObjectEdit edit = new RemoveObjectEdit(this, tmp);
                undoSupport.postEdit(edit);
                return;
            }
        }

        if (e.getKeyChar() == 10 && e.getSource() == ObjectTree) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) ObjectTree.getSelectionPath().getLastPathComponent();
            if (node.getUserObject() instanceof AbstractElement) {
                AbstractElement tmp = (AbstractElement) node.getUserObject();
                mapRenderer.focusLocation(tmp.getCenterLocation());
                mapRenderer.repaint();
                return;
            }
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent evt) {

        if (evt.getClickCount() >= 2 && evt.getSource() == ObjectTree) {

            String objName = "";
            if (ObjectTree.getSelectionCount() > 0) {
                objName = ObjectTree.getSelectionPath().getLastPathComponent().toString();
                AbstractElement mo;

                if ("Home Reference".equals(objName)) {
                    mo = mg.getHomeRef();
                    mo.showParametersDialog(this, null, this.map, false);
                }
                else {
                    mo = map.getObject(objName);

                    if (mo != null) {
                        mo.showParametersDialog(this, null, this.map, isEditable());

                        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                        changeEvent.setChangedObject(mo);
                        changeEvent.setSourceMap(map);
                        map.warnChangeListeners(changeEvent);

                        if (getParentMP() != null)
                            getParentMP().warnMissionListeners(
                                    new MissionChangeEvent(MissionChangeEvent.TYPE_MAPOBJECT_CHANGED, mo));

                        warnChangeListeners();
                    }
                }
            }
            return;
        }

        if (evt.getButton() == MouseEvent.BUTTON3 && evt.getSource() == ObjectTree) {
            TreePath path = ObjectTree.getPathForLocation(evt.getX(), evt.getY());

            if (path == null)
                return;

            ObjectTree.setSelectionPath(path);
            String objName = ObjectTree.getSelectionPath().getLastPathComponent().toString();
            AbstractElement tmp;

            if ("Home Reference".equals(objName))
                tmp = mg.getHomeRef();
            else
                tmp = map.getObject(objName);

            final AbstractElement mo = tmp;

            if (mo != null) {
                System.out.println(mo.getShapePoints());

                JPopupMenu popup = new JPopupMenu();
                popup.setLightWeightPopupEnabled(false);

                JMenuItem item = new JMenuItem("Focus object", new ImageIcon(
                        ImageUtils.getImage("images/buttons/zoom_btn.png")));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        mapRenderer.focusObject(mo);
                        if (r3d != null)
                            r3d.focusLocation(mo.getCenterLocation());
                    }
                });
                popup.add(item);

                item = new JMenuItem("View/Edit Properties",
                        new ImageIcon(ImageUtils.getImage("images/buttons/edit.png")));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        mo.showParametersDialog(MissionMapEditor.this, null, map, isEditable());

                        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                        changeEvent.setChangedObject(mo);
                        changeEvent.setSourceMap(map);
                        map.warnChangeListeners(changeEvent);
                        if (getParentMP() != null)
                            getParentMP().warnMissionListeners(
                                    new MissionChangeEvent(MissionChangeEvent.TYPE_MAPOBJECT_CHANGED, mo));
                        warnChangeListeners();
                    }
                });
                popup.add(item);

                if (mo.getTransparency() >= 100) {
                    item = new JMenuItem("Show object", new ImageIcon(ImageUtils.getImage("images/buttons/show.png")));
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            mo.setTransparency(0);
                            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                            changeEvent.setChangedObject(mo);
                            changeEvent.setSourceMap(map);
                            map.warnChangeListeners(changeEvent);
                            if (getParentMP() != null)
                                getParentMP().warnMissionListeners(
                                        new MissionChangeEvent(MissionChangeEvent.TYPE_MAPOBJECT_CHANGED, mo));
                            warnChangeListeners();
                        }
                    });
                    popup.add(item);
                }
                else {
                    item = new JMenuItem("Hide object", new ImageIcon(ImageUtils.getImage("images/buttons/hide.png")));
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            mo.setTransparency(100);
                            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                            changeEvent.setChangedObject(mo);
                            changeEvent.setSourceMap(map);
                            map.warnChangeListeners(changeEvent);
                            if (getParentMP() != null)
                                getParentMP().warnMissionListeners(
                                        new MissionChangeEvent(MissionChangeEvent.TYPE_MAPOBJECT_CHANGED, mo));
                            warnChangeListeners();
                        }
                    });
                    popup.add(item);
                }

                popup.addSeparator();

                item = new JMenuItem("Delete", new ImageIcon(ImageUtils.getImage("images/buttons/delete.png")));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        map.remove(mo.getName());
                        setMapChanged(true);
                        removeObjectFromTree(mo);

                        getParentMP().warnMissionListeners(
                                new MissionChangeEvent(MissionChangeEvent.TYPE_MAPOBJECT_REMOVED, mo));

                        RemoveObjectEdit edit = new RemoveObjectEdit(MissionMapEditor.this, mo);
                        undoSupport.postEdit(edit);

                        warnChangeListeners();
                        return;
                    }
                });
                if (!isEditable())
                    item.setEnabled(false);
                popup.add(item);

                popup.show(ObjectTree, evt.getX(), evt.getY());
            }

        }

    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        mouseMoved(e);
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    /**
     * When the user presses a mouse button (different from click!) this method is triggered.
     */
    public void mousePressed(MouseEvent evt) {

        if (evt.getSource() != mapRenderer)
            return;

        LocationType lt = mapRenderer.getRealWorldLocation(evt.getPoint());

        if (drawMode == FREE_HAND_DRAWING) {
            mapRenderer.setViewMode(Renderer.NONE);
            mapRenderer.setCursor(StateRenderer2D.drawCursor);

            curDrawing = new PathElement(mg, map, lt);
            map.addObject(curDrawing);
            setMapChanged(true);
            mapRenderer.setFastRendering(true);
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
            changeEvent.setChangedObject(curDrawing);
            changeEvent.setSourceMap(map);
            map.warnChangeListeners(changeEvent);
            warnChangeListeners();
            addObjectToTree(curDrawing);
            setTreeSelection(curDrawing);
        }
    }

    public void mouseReleased(MouseEvent arg0) {

        switch (drawMode) {
            case (FREE_HAND_DRAWING):
                if (curDrawing != null) {
                    curDrawing.setFinished(true);
                    mapRenderer.setFastRendering(false);
                    MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                    changeEvent.setChangedObject(curDrawing);
                    changeEvent.setSourceMap(map);
                    map.warnChangeListeners(changeEvent);

                    AddObjectEdit edit = new AddObjectEdit(this, curDrawing);
                    undoSupport.postEdit(edit);
                    curDrawing = null;
                }
                break;
            case (POINT_TO_POINT_DRAWING):
                double[] offsets = new double[] { 0, 0, 0 };
                if (curDrawing != null)
                    offsets = curDrawing.getCenterLocation().getOffsetFrom(
                            mapRenderer.getRealWorldLocation(arg0.getPoint()));
                if (arg0.getButton() == MouseEvent.BUTTON3) {

                    // drawX = mapRenderer.getRealXCoord((int) arg0.getPoint().getX());
                    // drawY = mapRenderer.getRealYCoord((int) arg0.getPoint().getY());

                    if (curDrawing != null) {
                        curDrawing.addPoint(-offsets[1], -offsets[0], 0, false);
                        curDrawing.setFinished(true);
                        mapRenderer.setFastRendering(false);
                        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                        changeEvent.setChangedObject(curDrawing);
                        changeEvent.setSourceMap(map);
                        map.warnChangeListeners(changeEvent);

                        AddObjectEdit edit = new AddObjectEdit(this, curDrawing);
                        undoSupport.postEdit(edit);
                        curDrawing = null;
                    }
                }
                else {
                    if (!(arg0.getSource() instanceof StateRenderer2D))
                        return;

                    // drawX = mapRenderer.getRealXCoord((int) arg0.getPoint().getX());
                    // drawY = mapRenderer.getRealYCoord((int) arg0.getPoint().getY());

                    if (curDrawing != null) {
                        curDrawing.addPoint(-offsets[1], -offsets[0], 0, false);

                        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                        changeEvent.setChangedObject(curDrawing);
                        changeEvent.setSourceMap(map);
                        map.warnChangeListeners(changeEvent);
                    }
                    else {
                        LocationType lt = mapRenderer.getRealWorldLocation(arg0.getPoint());
                        curDrawing = new PathElement(mg, map, lt);
                        curDrawing.setShape(fillShape);
                        offsets = curDrawing.getCenterLocation().getOffsetFrom(
                                mapRenderer.getRealWorldLocation(arg0.getPoint()));
                        curDrawing.addPoint(-offsets[1], -offsets[0], 0, false);
                        map.addObject(curDrawing);
                        setMapChanged(true);
                        mapRenderer.setFastRendering(true);
                        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
                        changeEvent.setChangedObject(curDrawing);
                        changeEvent.setSourceMap(map);
                        map.warnChangeListeners(changeEvent);
                        addObjectToTree(curDrawing);
                        setTreeSelection(curDrawing);
                    }
                    if (curDrawing != null)
                        curDrawing.getShapePoints();
                }
                break;
            default:
                break;
        }
    }

    /**
     * The mouse has been dragged by the user so if verifies the editor is currently in draw mode. If it is then changes
     * the current drawing.
     */
    public void mouseDragged(MouseEvent e) {

        if (e.getSource() != mapRenderer)
            return;

        if (drawMode == FREE_HAND_DRAWING) {

            // drawX = mapRenderer.getRealXCoord((int) e.getPoint().getX());
            // drawY = mapRenderer.getRealYCoord((int) e.getPoint().getY());

            double[] offsets = curDrawing.getCenterLocation().getOffsetFrom(
                    mapRenderer.getRealWorldLocation(e.getPoint()));
            curDrawing.addPoint(-offsets[1], -offsets[0], 0, false);

            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            changeEvent.setChangedObject(curDrawing);
            changeEvent.setSourceMap(map);

            map.warnChangeListeners(changeEvent);
        }
        mouseMoved(e);

    }

    /**
     * The mouse as been moved. Updates the current status label indicating the mouse position in real world
     * coordinates.
     */
    public void mouseMoved(MouseEvent e) {

        if (drawMode == POINT_TO_POINT_DRAWING && curDrawing != null) {
            double x = mapRenderer.getRealXCoord((int) e.getPoint().getX());
            double y = mapRenderer.getRealYCoord((int) e.getPoint().getY());

            curDrawing.setNextPoint(new Point2D.Double(x, y));
            mapRenderer.repaint();
        }
    }

    public void setMapGroup(MapGroup mg) {
        this.mg = mg;
        mg.addChangeListener(this);
        mg.addMap(map);
        HomeReferenceElement homeRef = mg.getHomeRef();
        mapRenderer.setMapGroup(mg);
        mapRenderer.getMapGroup().setHomeRef(homeRef);
        mapRenderer.focusObject(homeRef);
        clearTree();
        constructTree();
        addObjectToTree(homeRef);
    }

    public void mapChanged(MapChangeEvent mapChange) {
        if (mapChange.getChangedObject() != null && mapChange.getChangedObject() instanceof HomeReferenceElement) {
            HomeReferenceElement hro = (HomeReferenceElement) mapChange.getChangedObject();
            mapRenderer.getMapGroup().getHomeRef().setCoordinateSystem(hro.getCoordinateSystem());
        }
        mapRenderer.repaint();
    }

    /**
     * This method constructs an object of type Map with all the values in the current map. It is useful for translating
     * a map to XML.
     * 
     * @return
     */
    public MapType getMap() {
        map.setChanged(isMapChanged());
        return this.map;
    }

    public boolean isMapChanged() {
        return mapChanged;
    }

    public void setMapChanged(boolean mapChanged) {
        this.mapChanged = mapChanged;
        if (mapChanged)
            warnChangeListeners();
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * @param mapHref The mapHref to set.
     */
    public void setMapHref(String mapHref) {
        this.mapHref = mapHref;
    }

    public MissionPlanner getParentMP() {
        return parentMP;
    }

    public void setParentMP(MissionPlanner parent) {
        this.parentMP = parent;
        setFrameOpener(parent);
    }

    public DefaultProperty[] getProperties() {

        DefaultProperty wmsURL = PropertiesEditor.getPropertyInstance("Server URL", String.class,
                fetcher.getWmsServerURL(), true);
        wmsURL.setCategory("Web Mapping Service");
        wmsURL.setShortDescription("The server from where to fetch real world maps");

        String[] layers = fetcher.getRequestedLayers();
        System.err.println(layers.length);
        String currentLayers = "";
        if (layers.length > 0) {
            currentLayers = layers[0];
            for (int i = 1; i < layers.length; i++) {
                currentLayers = currentLayers + "," + layers[i];
            }
        }

        DefaultProperty visibleLayers = PropertiesEditor.getPropertyInstance("Visible Layers", String.class,
                currentLayers, true);
        visibleLayers.setCategory("Web Mapping Service");
        visibleLayers
                .setShortDescription("The layers that should be fetched from the WMS server (separated by commas)");

        DefaultProperty testProperty = PropertiesEditor.getPropertyInstance("Location", LocationType.class,
                new LocationType(), false);

        DefaultProperty renderingQuality = PropertiesEditor.getPropertyInstance("Rendering Quality", String.class,
                mapRenderer.getRenderingQuality(), true);
        renderingQuality.setCategory("Visualization Options");
        renderingQuality.setShortDescription("How the map should be drawn");
        AbstractPropertyEditor ape = PropertiesEditor.getComboBoxPropertyEditor(new String[] { "Fast", "Default",
                "Quality" }, mapRenderer.getRenderingQuality());
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(renderingQuality, ape);

        DefaultProperty[] properties = new DefaultProperty[] { wmsURL, visibleLayers, renderingQuality, testProperty };

        return properties;
    }

    public String getPropertiesDialogTitle() {
        return "Mission Map Editor preferences";
    }

    public void setProperties(Property[] properties) {

        for (int i = 0; i < properties.length; i++) {

            if ("Rendering Quality".equals(properties[i].getName())) {
                mapRenderer.setRenderingQuality((String) properties[i].getValue());
            }

            if ("Visible Layers".equals(properties[i].getName())) {
                StringTokenizer st = new StringTokenizer((String) properties[i].getValue(), ",");

                String[] layers = new String[st.countTokens()];
                int j = 0;
                while (st.hasMoreTokens())
                    layers[j++] = st.nextToken();

                fetcher.setRequestedLayers(layers);
            }

            if ("Server URL".equals(properties[i].getName())) {
                fetcher.setWmsServerURL((String) properties[i].getValue());
            }
            mapRenderer.repaint();
        }
    }

    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    public boolean isStatusBarVisible() {
        return statusBarVisible;
    }

    public void setStatusBarVisible(boolean value) {
        this.statusBarVisible = value;
        remove(statusBar);
        if (value)
            add(statusBar, BorderLayout.SOUTH);
    }

    public MissionType getMissionType() {
        return missionType;
    }

    public void setMissionType(MissionType missionType) {
        this.missionType = missionType;
    }

    public StateRenderer2D getMapRenderer() {
        return mapRenderer;
    }

    public IFrameOpener getFrameOpener() {
        return frameOpener;
    }

    public void setFrameOpener(IFrameOpener frameOpener) {
        this.frameOpener = frameOpener;
    }

    public void cleanup() {
        if (r3d != null)
            r3d.cleanup();
        mapRenderer.cleanup();
    }

    public void handleFile(File f) {

        MapType mt = new MapType(f.getAbsolutePath());
        CoordinateSystem center = new CoordinateSystem();
        LinkedList<AbstractElement> mel = mt.getAllElements();
        if (mel.size() > 0)
            center.setLocation(mel.get(0).getCenterLocation());

        final MissionMapEditor mme = new MissionMapEditor(mt, center, true);

        final JFrame frame = new JFrame("Neptus Mission Map Editor");
        frame.setContentPane(mme);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                int response = JOptionPane.showConfirmDialog(frame, "Do you want to save map?", "Closing...",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (response == JOptionPane.CANCEL_OPTION)
                    return;
                if (response == JOptionPane.YES_OPTION) {
                    mme.actionPerformed(new ActionEvent(this, 0, "save"));
                }
                frame.dispose();
            }

            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                System.exit(0);
            }
        });
        setEditable(false);
        frame.setSize(600, 400);
        frame.setIconImage(ImageUtils.getImage("images/neptus-icon.png"));
        GuiUtils.centerOnScreen(frame);
        frame.setVisible(true);
    }

    private void refreshUndoRedo() {
        undoButton.setEnabled(undoManager.canUndo());
        undoButton.setToolTipText(undoManager.getUndoPresentationName());

        redoButton.setEnabled(undoManager.canRedo());
        redoButton.setToolTipText(undoManager.getRedoPresentationName());
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        undoManager.addEdit(e.getEdit());
        refreshUndoRedo();
    }

    public UndoableEditSupport getUndoSupport() {
        return undoSupport;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }
}
