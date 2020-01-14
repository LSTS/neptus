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
 * Author: José Pinto
 * 2005/02/15
 */
package pt.lsts.neptus.renderer2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.planning.MapShortcutsLayer;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.MapChangeListener;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.renderer2d.IMapRendererChangeEvent.RendererChangeEvent;
import pt.lsts.neptus.renderer2d.tiles.TileMercatorSVG;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.ScatterPointsElement;
import pt.lsts.neptus.types.map.VehicleTailElement;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * This class provides a 2D visualization of the world, including maps, vehicle poses and other layers
 * 
 * @author Ze Carlos
 * @author pdias
 */
public class StateRenderer2D extends JPanel implements PropertiesProvider, Renderer, MapChangeListener,
        MouseWheelListener, MouseMotionListener, MouseListener, KeyListener, PreferencesListener, ILayerPainter,
        CustomInteractionSupport, IMapPopup, FocusListener {

    static final long serialVersionUID = 15;
    public static final int MAP_MOVES = 0, VEHICLE_MOVES = 1;
    public static final float DEFAULT_ZOOM = 2.5f / 2.0f;
    private final int DEFAULT_LOD = 18;
    private final int MIN_LOD = MapTileUtil.LEVEL_MIN;
    private final int MAX_LOD = MapTileUtil.LEVEL_MAX;
    private boolean worldMapShowScreenControls = false;

    public static Cursor rotateCursor, translateCursor, zoomCursor, grabCursor, grab2Cursor, crosshairCursor,
            drawCursor;

    protected AffineTransform identity = new AffineTransform();

    /**
     * Never update this directly. Use {@link #worldPixelXY} instead.
     */
    private final LocationType center = new LocationType();

    @SuppressWarnings("serial")
    public final Point2D worldPixelXY = new Point2D.Double() {

        @Override
        public void setLocation(double x, double y) {
            super.setLocation(x, y);
            propagateChange();
        }

        @Override
        public void setLocation(Point2D p) {
            super.setLocation(p);
            propagateChange();
        };

        private void propagateChange() {
            // updateCenter();
            double[] latLon = MapTileUtil.xyToDegrees(worldPixelXY.getX(), worldPixelXY.getY(), getLevelOfDetail());
            center.setLatitudeDegs(latLon[0]);
            center.setLongitudeDegs(latLon[1]);
            Point2D nXY = MapTileUtil.degreesToXY(latLon[0], latLon[1], levelOfDetail);
            super.setLocation(nXY.getX(), nXY.getY());  //This is the one that has to be called, otherwise a endless cycle will emerge

            setLevelOfDetail(levelOfDetail);
            repaint();
        }
    };
    {
        double ms2 = MapTileUtil.mapSize(DEFAULT_LOD) / 2.0;
        worldPixelXY.setLocation(ms2, ms2);
    }

    private float zoom = DEFAULT_ZOOM; // zoomMult = 1.0f;
    private int levelOfDetail = DEFAULT_LOD;
    {
        setLevelOfDetail(getLevelOfDetail());
    }

    private double setLevelOfDetailLastLat = Double.NaN; // #setLevelOfDetail(..) helper variable
    private double mapScale = Double.NaN; // Depends on the levelOfDetail and center and screen DPI, used to cache the value
    private double mapScaleLastLat = Double.NaN; // #getMapScale() helper variable
    private int mapScaleLastLevelOfDetail = -1; // #getMapScale() helper variable
    private int mapScaleLastScreenResolution = -1; // #getMapScale() helper variable

    protected double rotationRads = 0;
    protected double gridSize = 100.0;

    protected double fixedVehicleWidth = 25;

    protected int show_mode = VEHICLE_MOVES;
    protected int lastClickedButton;
    protected LinkedList<MapClickListener> clickListeners = new LinkedList<MapClickListener>();
    protected Hashtable<String, SystemPositionAndAttitude> vehicleStates = new Hashtable<String, SystemPositionAndAttitude>();
    protected String[] vehicles = new String[0];
    protected Hashtable<String, Image> vehicleImages = new Hashtable<String, Image>();
    protected Hashtable<String, VehicleTailElement> vehicleTails = new Hashtable<String, VehicleTailElement>();
    protected HashSet<String> vehiclesTailOn = new HashSet<String>();

    protected boolean worldMapShown = true;
    protected boolean worldBondariesShown = false;
    protected String worldMapStyle = TileMercatorSVG.getTileStyleID();

    protected boolean gridShown = false;
    protected boolean showDots = false;
    protected boolean legendShown = false;
    protected boolean vehicleImageShown = false;
    protected boolean mapCenterShown = true;
    protected boolean mapDragEnable = true;
    protected boolean isAllTailOn = false;

    protected int numberOfShownPoints = 0;
    protected Point2D lastDragPoint = null;
    protected Point2D rulerFirstPoint = null;
    protected Point2D rulerLastPoint = null;
    protected MarkElement homeRef;
    protected MapLegend legend = new MapLegend();
    protected CursorLocationPainter cursorPainter = new CursorLocationPainter();
    protected final NumberFormat df = GuiUtils.getNeptusDecimalFormat(2);
    protected MapChangeEvent lastMapChangeEvent = null;
    protected Color gridColor = new Color(0, 0, 0);
    protected MapGroup mapGroup = null;
    protected int countCache = 0;
    protected static GeneralPath arrow = new GeneralPath();
    // arrow path initialization
    {
        arrow.moveTo(-2, -10);
        arrow.lineTo(2, -10);
        arrow.lineTo(2, 0);
        arrow.lineTo(5, 0);
        arrow.lineTo(0, 10);
        arrow.lineTo(-5, 0);
        arrow.lineTo(-2, 0);
        arrow.closePath();
    }

    protected WorldRenderPainter worldMapPainter = null;

    protected String lockedVehicle = null;
//    protected TransponderSecurityArea securityArea = new TransponderSecurityArea();
    protected PaintersBag painters = new PaintersBag(this);
    protected boolean ignoreRightClicks = false;
    protected boolean showProperties = true;
    protected int minDelay = 10;
    protected String editingMap = null;
    protected Vector<RightMouseClickListener> rightClickListeners = new Vector<RightMouseClickListener>();
    protected GeneralPath triangle = null;
    protected Vector<ChangeListener> changeListeners = new Vector<ChangeListener>();
    protected boolean smoothResizing = false;
    protected boolean antialiasing = true;
    protected Vector<StateRendererInteraction> interactions = new Vector<StateRendererInteraction>();
    private final StateRendererInteraction defaultInteraction = new InteractionAdapter(null);
    protected StateRendererInteraction activeInteraction = defaultInteraction;
    protected long lastPaintTime = 0;
    protected BufferedImage cache = null;
    protected boolean forceRepaint = false;
    protected double lastAngle = 0;
    protected boolean shuttingDown = false;

    private BufferedImage stage;

    private Vector<IEditorMenuExtension> menuExtensions = new Vector<IEditorMenuExtension>();

    private IMapRendererChangeEvent rendererChangelistener = null;
    private boolean respondToRendererChangeEvents = false;
    private boolean processingRendererChangeEvents = false;

    /**
     * Empty class constructor - creates a new renderer panel with empty map.
     */
    public StateRenderer2D() {
        setMapGroup(MapGroup.getMapGroupInstance(null));

        LocationType portugal = new LocationType();
        portugal.setLatitudeDegs(38.711233);
        portugal.setLongitudeDegs(-9.18457);
        setCenter(portugal);
        init();
    }

    /**
     * Similar to the empty constructor but creates a map centered at the given position.
     * 
     * @param center The location to be the center of this renderer
     */
    public StateRenderer2D(LocationType center) {
        CoordinateSystem cs = new CoordinateSystem();
        cs.setLocation(center);
        setMapGroup(MapGroup.getNewInstance(cs));
        setCenter(center);
        init();
    }

    /**
     * Creates a new instance with the given map loaded
     * 
     * @param mapGroup The map to be loaded in this Renderer
     */
    public StateRenderer2D(MapGroup mapGroup) {
        setMapGroup(mapGroup);
        init();
    }

    /**
     * Default initializations and adding default layers
     */
    protected void init() {
        loadCursors();
        addMouseListener(this);
        addMouseMotionListener(this);
        addFocusListener(this);
        setBackground(new Color(2, 113, 171));
        setFocusable(true);
        // addPostRenderPainter(new TransponderSecurityArea(), "Transponder Security Area");
        addKeyListener(this);

        preferencesUpdated();
        GeneralPreferences.addPreferencesListener(this);

        try {
            worldMapPainter = new WorldRenderPainter(StateRenderer2D.this, isWorldBondariesShown(), isWorldMapShown(),
                    getWorldMapStyle());
            worldMapPainter.setShowOnScreenControls(worldMapShowScreenControls);
            addPreRenderPainter(worldMapPainter);
            addPostRenderPainter(worldMapPainter.getPostRenderPainter(), "World Map Painter Control");
            //addMouseListener(worldMapPainter);
            //addMouseMotionListener(worldMapPainter);
        }
        catch (NoClassDefFoundError e) {
            NeptusLog.pub().warn("Probably running inside a reduced api jar!!", e);
            e.printStackTrace();
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                stage = null; 
            }
        });
    }

    public boolean isShowWorldMapOnScreen() {
        return worldMapPainter.isDrawWorldMap();
    }

    public void setShowWorldMapOnScreen(boolean showOnScreen) {
        worldMapPainter.setDrawWorldMap(showOnScreen);
    }

    /**
     * @return the showOnScreenControls
     */
    public boolean isShowWorldMapOnScreenControls() {
        return worldMapShowScreenControls;
    }

    /**
     * @param showOnScreenControls the showOnScreenControls to set
     */
    public void setShowWorldMapOnScreenControls(boolean showOnScreenControls) {
        worldMapShowScreenControls = showOnScreenControls;
        if (worldMapPainter != null)
            worldMapPainter.setShowOnScreenControls(showOnScreenControls);
    }

    /**
     * Creates a retrieves a Thread that updates the Renderer periodically. Use {@link #shuttingDown} to true to stop
     * the thread.
     * 
     * @param millisBetweenUpdates Time, in milliseconds between updates (repaint)
     * @return The created Thread, unstarted
     */
    protected Thread getRenderer2dUpdaterThread(int millisBetweenUpdates) {
        final long millis = millisBetweenUpdates;
        Thread t = new Thread("R2D updater") {
            @Override
            public void run() {
                while (!shuttingDown) {
                    try {
                        Thread.sleep(millis);
                        repaint();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        return t;
    }

    /**
     * Creates a memory cache with all mouse cursors that can be used by the Renderer
     */
    private void loadCursors() {
        rotateCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                ImageUtils.getImage("images/cursors/rotate_cursor.png"), new Point(12, 12), "Rotate");
        zoomCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                ImageUtils.getImage("images/cursors/zoom_cursor.png"), new Point(6, 6), "Zoom");
        translateCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                ImageUtils.getImage("images/cursors/translate_cursor.png"), new Point(12, 12), "Translate");
        grabCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                ImageUtils.getImage("images/cursors/grab_cursor.png"), new Point(11, 11), "Grab");
        grab2Cursor = Toolkit.getDefaultToolkit().createCustomCursor(
                ImageUtils.getImage("images/cursors/grab2_cursor.png"), new Point(11, 11), "Grab2");
        crosshairCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                ImageUtils.getImage("images/cursors/crosshair_cursor.png"), new Point(12, 12), "Crosshair");
        drawCursor = Toolkit.getDefaultToolkit().createCustomCursor(ImageUtils.getImage("images/cursors/pencil.png"),
                new Point(0, 20), "Draw");
    }

    /**
     * Retrieve real-world location that matches the top-left pixel of this Renderer
     * 
     * @return The LocationType that matches the top-left pixel of this Renderer
     */
    public LocationType getTopLeftLocationType() {
        return getRealWorldLocation(new Point2D.Double(0, 0));
    }

    /**
     * Retrieve real-world location that matches the bottom-right pixel of this Renderer
     * 
     * @return The LocationType that matches the bottom-right pixel of this Renderer
     */
    public LocationType getBottomRightLocationType() {
        return getRealWorldLocation(new Point2D.Double(getWidth(), getHeight()));
    }

    /**
     * Retrieve a random location that is visible in this Renderer
     * 
     * @return A LocationType that is inside the visible area of this Renderer
     */
    public LocationType getRandomVisibleLocationType() {
        LocationType center = new LocationType(getCenter());
        Random rnd = new Random(System.currentTimeMillis());

        double offsetEast = getWidth() / (2 * zoom);
        double offsetNorth = getHeight() / (2 * zoom);

        if (rnd.nextBoolean()) {
            offsetEast *= -1;
        }

        if (rnd.nextBoolean()) {
            offsetNorth *= -1;
        }

        offsetEast *= rnd.nextFloat();
        offsetNorth *= rnd.nextFloat();

        center.translatePosition(offsetNorth, offsetEast, 0);
        setLevelOfDetail(levelOfDetail);
        return center;
    }

    /**
     * Similar to {@link #getLocationOnScreen(LocationType)} but always returns a value, even if it is not currently
     * visible
     * 
     * @param lt The location to be transformed into renderer coordinates
     * @return The renderer coordinates for the given location
     */
    public Point2D getScreenPosition(LocationType lt) {
        return MapTileRendererCalculator.getScreenPositionHelper(lt, worldPixelXY, getSize(), levelOfDetail,
                rotationRads);
    }

    /**
     * @param fixedVehicleWidth the fixedVehicleWidth to set
     */
    public void setFixedVehicleWidth(double fixedVehicleWidth) {
        this.fixedVehicleWidth = fixedVehicleWidth;
    }

    /**
     * Implementation of {@link MapChangeListener} interface
     */
    @Override
    public void mapChanged(MapChangeEvent mapChange) {

        // If for some reason this listener is being called multiple times...
        if (mapChange.equals(lastMapChangeEvent))
            return;
        lastMapChangeEvent = mapChange;

        // If map was replaced, reset the view
        if (mapChange.getEventType() == MapChangeEvent.MAP_RESET) {

            LocationType oldCenter = new LocationType(getCenter());
            setMapGroup(MapGroup.getMapGroupInstance(getMapGroup().getMission()));
            // center.setLocation(oldCenter);
            setCenter(oldCenter);
            repaint();
            return;
        }

        // Repaint the map
        forceRepaint();
    }

    /**
     * Set the map to be displayed in this renderer
     * 
     * @param mapGroup The map to be rendered
     */
    @Override
    public void setMapGroup(MapGroup mapGroup) {
        if (this.mapGroup != null) {
            this.mapGroup.removeChangeListener(this);
        }

        this.mapGroup = mapGroup;
        // this.center.setLocation(mapGroup.getCoordinateSystem());
        setCenter((LocationType) mapGroup.getCoordinateSystem().getNewAbsoluteLatLonDepth());
        mapGroup.addChangeListener(this);
        // legend.setCenterLocation(center);
        legend.setCenterLocation(getCenter());
        setLegendShown(true);
        this.addMouseWheelListener(this);
    }

    /**
     * Resizes the current view in order to show all objects in the map
     */
    public void showAllMap() {
        AbstractElement[] objs = mapGroup.getAllObjects();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (int i = 0; i < objs.length; i++) {
            double ned[] = objs[i].getNEDPosition();
            minX = Math.min(minX, ned[1]);
            minY = Math.min(minY, ned[0]);
            maxX = Math.max(maxX, ned[1]);
            maxY = Math.max(maxY, ned[0]);
        }
        double realWidth = maxX - minX;
        double realHeight = maxY - minY;

        LocationType newCenter = new LocationType();
        newCenter.setOffsetNorth(realHeight / 2);
        newCenter.setOffsetEast(realWidth / 2);

        setZoom(getWidth() / (float) realWidth);

        focusLocation(newCenter);
    }

    /**
     * @return The {@link MapGroup} currently being rendered
     */
    @Override
    public MapGroup getMapGroup() {
        return this.mapGroup;
    }

    /**
     * Similar to {@link #vehicleStateChanged(VehicleType, SystemPositionAndAttitude)} but the caller has a choice
     * of whether to repaint the renderer
     * 
     * @param systemId The vehicle that has an updated state
     * @param state The new state for the the vehicle
     * @param repaint Whether to repaint the renderer
     */
    public void vehicleStateChanged(String systemId, SystemPositionAndAttitude state, boolean repaint) {
        if (state == null) {
            vehicleStates.remove(systemId);
            vehicleTails.remove(systemId);
            vehicles = vehicleStates.keySet().toArray(new String[0]);
            if (repaint)
                repaint();
        }
        else {
            VehicleType vehicleType = VehiclesHolder.getVehicleById(systemId);
            if (!vehicleImages.containsKey(systemId)) {
                if (vehicleType != null) {
                    Image vehicleImage = ImageUtils.getImage(vehicleType.getTopImageHref());
                    // vehicleImage = GuiUtils.applyTransparency(vehicleImage, 0.5f);
                    vehicleImages.put(systemId, vehicleImage);
                }
            }
            if (!vehicleTails.containsKey(systemId)) {
                if (vehicleType != null) {
                    VehicleTailElement vehicleTail = new VehicleTailElement(getMapGroup(), new MapType(),
                            vehicleType.getIconColor());
                    vehicleTail.setNumberOfPoints(numberOfShownPoints);
                    vehicleTails.put(systemId, vehicleTail);
                }
                else {
                    VehicleTailElement vehicleTail = new VehicleTailElement(getMapGroup(), new MapType(),
                            Color.black);
                    vehicleTail.setNumberOfPoints(numberOfShownPoints);
                    vehicleTails.put(systemId, vehicleTail);
                }
            }

            if (!vehicleStates.containsKey(systemId))
                vehicles = vehicleStates.keySet().toArray(new String[0]);
            vehicleStates.put(systemId, state);

            //double[] distFromRef = state.getPosition().getOffsetFrom(vehicleTails.get(systemId).getCenterLocation());
            vehicleTails.get(systemId).addPoint(state.getPosition());

            if (!repaint || System.currentTimeMillis() - lastPaintTime < minDelay) {
                return;
            }

            repaint();
        }
    }

    /**
     * Implementation of {@link VehicleStateListener} that will repaint the renderer when vehicles are updated
     */
    @Override
    public void vehicleStateChanged(String systemId, SystemPositionAndAttitude state) {
        vehicleStateChanged(systemId, state, true);
    }

    /**
     * Removes the given vehicle from this Renderer's vehicle list
     * 
     * @param vehicle The vehicle to be removed from this Renderer
     */
    public void removeVehicle(VehicleType vehicle) {
        vehicleStates.remove(vehicle.getName());
        vehicleImages.remove(vehicle.getName());
        vehicleTails.remove(vehicle.getName());
    }

    /**
     * This method is called when the mouse wheel rotates, changing the current zoom
     * 
     * @param arg0 The {@link MouseWheelEvent} generated by Swing
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent arg0) {
        activeInteraction.wheelMoved(arg0, this);
        this.repaint();
    }

    /**
     * @return the levelOfDetail
     */
    public int getLevelOfDetail() {
        return levelOfDetail;
    }

    /**
     * @param levelOfDetail the levelOfDetail to set
     */
    public int setLevelOfDetail(int levelOfDetail) {
        int oldLevelOfDetail = this.levelOfDetail;
        if (this.levelOfDetail != levelOfDetail) {
            levelOfDetail = Math.min(Math.max(levelOfDetail, MIN_LOD), MAX_LOD);

            int minLod = 1;
            for (int lod = MIN_LOD; lod <= MAX_LOD; lod++) {
                int mst = MapTileUtil.mapSize(lod);
                int wt = getWidth();
                int ht = getHeight();
                minLod = lod;
                if (mst >= wt && mst >= ht)
                    break;
            }
            levelOfDetail = Math.max(minLod, levelOfDetail);

            double tz = 1.0 / MapTileUtil.groundResolution(getCenter().getLatitudeDegs(), levelOfDetail);
            if (tz < 0)
                return this.levelOfDetail;
            // int oldLevelOfDetail = this.levelOfDetail;
            this.levelOfDetail = levelOfDetail;
            zoom = (float) tz;
            setLevelOfDetailLastLat = getCenter().getLatitudeDegs();
        }

        if (Double.isNaN(setLevelOfDetailLastLat) || setLevelOfDetailLastLat != getCenter().getLatitudeDegs()) {
            setLevelOfDetailLastLat = getCenter().getLatitudeDegs();
            double tz = 1.0 / MapTileUtil.groundResolution(setLevelOfDetailLastLat, this.levelOfDetail);
            zoom = (float) tz;
        }

        if (this.levelOfDetail != oldLevelOfDetail) {
            // Update World Center Pixel
            double wc = Math.pow(2, this.levelOfDetail - oldLevelOfDetail);
            double nwx = worldPixelXY.getX() == 0 ? 0 : worldPixelXY.getX() * wc;
            double nwy = worldPixelXY.getY() == 0 ? 0 : worldPixelXY.getY() * wc;
            worldPixelXY.setLocation(nwx, nwy);
        }

        getMapScale(); // To cache mapScale (this should be here after updated levelOfDetail and center)

        this.repaint();
        // System.err.println("levelOfDetail=" + this.levelOfDetail + "  |  zoom=" + this.zoom + "  |  worldPixelXY=" +
        // worldPixelXY
        // + "  |  groundResolution="
        // + MapTileUtil.groundResolution(getCenter().getLatitudeAsDoubleValue(), levelOfDetail)
        // + "  |  mapSize=" + MapTileUtil.mapSize(levelOfDetail)
        // + "  |  wxh=" + getWidth() + "x" + getHeight());
        
        warnRendererChangeEvent();
        
        return this.levelOfDetail;
    }

    public void zoomIn() {
        zoomInOut(true, getWidth() / 2, getHeight() / 2);
    }

    public void zoomOut() {
        zoomInOut(false, getWidth() / 2, getHeight() / 2);
    }

    public void zoomInOut(boolean inOrOut, double localRenderX, double localRenderY) {
        if (!inOrOut) { // zoom out
            double nwx = -(localRenderX - getWidth() / 2);
            double nwy = -(localRenderY - getHeight() / 2);
            if (rotationRads != 0) {
                double[] np = AngleUtils.rotate(rotationRads, nwy, nwx, true);
                nwx = np[1];
                nwy = np[0];
            }
            worldPixelXY.setLocation(worldPixelXY.getX() + nwx, worldPixelXY.getY() + nwy);
            setLevelOfDetail(getLevelOfDetail() - 1);
        }
        else { // zoom in
            setLevelOfDetail(getLevelOfDetail() + 1);
            double nwx = (localRenderX - getWidth() / 2);
            double nwy = (localRenderY - getHeight() / 2);
             if (rotationRads != 0) {
                double[] np = AngleUtils.rotate(rotationRads, nwy, nwx, true);
                nwx = np[1];
                nwy = np[0];
            }
            worldPixelXY.setLocation(worldPixelXY.getX() + nwx, worldPixelXY.getY() + nwy);
        }
    }

    public void resetView() {
        focusLocation(getMapGroup().getHomeRef().getCenterLocation());
        setRotation(0);
        setZoom(DEFAULT_ZOOM);
        setLevelOfDetail(DEFAULT_LOD);
        repaint();
    }

    /**
     * Retrieve current zoom
     * 
     * @return the current zoom value for this Renderer
     */
    public float getZoom() {
        return zoom;
    }

    /**
     * Changes the current world zoom
     */
    public float setZoom(float newZoom) {
        double grdResol = 1.0 / newZoom;
        for (int lod = MIN_LOD; lod <= MAX_LOD; lod++) {
            double lodG1 = MapTileUtil.groundResolution(getCenter().getLatitudeDegs(), lod);
            double lodG2 = MapTileUtil.groundResolution(getCenter().getLatitudeDegs(), lod + 1);
            if (grdResol > lodG1) {
                setLevelOfDetail(lod);
                break;
            }
            else if ((grdResol > lodG2)) {
                double t1 = Math.abs(lodG1 - grdResol);
                double t2 = Math.abs(grdResol - lodG2);
                if (t1 <= t2) {
                    setLevelOfDetail(lod);
                    break;
                }
                else {
                    setLevelOfDetail(lod + 1);
                    break;
                }
            }
        }
        return zoom;
    }

    /**
     * Gets Map Scale at screen resolution of screen DPIs using {@link Toolkit#getDefaultToolkit()
     * #getScreenResolution()}
     * 
     * @return
     */
    public double getMapScale() {
        boolean recalc = false;
        if (Double.isNaN(mapScale) || Double.isNaN(mapScaleLastLat) || mapScaleLastLevelOfDetail == -1
                || mapScaleLastScreenResolution == -1) {
            recalc = true;
        }
        else if (mapScaleLastLat != getCenter().getLatitudeDegs()
                || mapScaleLastLevelOfDetail != levelOfDetail
                || mapScaleLastScreenResolution != Toolkit.getDefaultToolkit().getScreenResolution()) {
            recalc = true;
        }

        if (recalc) {
            mapScaleLastLat = getCenter().getLatitudeDegs();
            mapScaleLastLevelOfDetail = levelOfDetail;
            mapScaleLastScreenResolution = Toolkit.getDefaultToolkit().getScreenResolution();
            mapScale = MapTileUtil.mapScale(mapScaleLastLat, mapScaleLastLevelOfDetail, mapScaleLastScreenResolution);
        }

        return mapScale;
    }

    /**
     * Sets the world map
     */
    public void setMap(MapType map) {
        this.mapGroup = MapGroup.getNewInstance(null);
        mapGroup.addMap(map);
        forceRepaint();
    }

    public void forceRepaint() {
        forceRepaint = true;
        repaint();
    }

    private Long startTime = null;

    @Override
    protected void paintComponent(Graphics g) {
        zoom = Math.min(50, zoom);
        if (startTime == null)
            startTime = System.currentTimeMillis();

        if (!isVisible() || getWidth() <= 0) {
            return;
        }
        if(stage == null) {
            stage = ImageUtils.createCompatibleImage(getWidth(), getHeight(), Transparency.OPAQUE);
        }
        Graphics2D g2d = (Graphics2D) stage.getGraphics();

        update(g2d, forceRepaint);
        g.drawImage(stage,0,0,getWidth(), getHeight(),null);
        forceRepaint = false;
        lastPaintTime = System.currentTimeMillis();
        //        NeptusLog.pub().info("<###> "+(System.nanoTime() - nt) / Math.pow(10,6));
    }

    /**
     * Adds a new MapClickListener to the current list of listeners MapClickListeners will receive all the map click
     * events
     * 
     * @param newListener A class implementing the MapClickListener interface
     */
    public void addMapClickListener(MapClickListener newListener) {
        NeptusLog.pub().info("Added a map click listener: " + newListener.getClass().getSimpleName());
        clickListeners.addLast(newListener);
    }

    /**
     * Removes the given MapClickListener object from the current active list
     * 
     * @param listener The MapClickListener to be removed
     */
    public void removeMapClickListener(MapClickListener listener) {
        clickListeners.remove(listener);
    }

    /**
     * @return the vehicleImageShown
     */
    public boolean isVehicleImageShown() {
        return vehicleImageShown;
    }

    /**
     * @param vehicleImageShown the vehicleImageShown to set
     */
    public void setVehicleImageShown(boolean vehicleImageShown) {
        this.vehicleImageShown = vehicleImageShown;
    }

    /**
     * @return the mapCenterShown
     */
    public boolean isMapCenterShown() {
        return mapCenterShown;
    }

    /**
     * @param mapCenterShown the mapCenterShown to set
     */
    public void setMapCenterShow(boolean mapCenterShown) {
        this.mapCenterShown = mapCenterShown;
    }

    /**
     * @return the mapDragEnable
     */
    public boolean isMapDragEnable() {
        return mapDragEnable;
    }

    /**
     * @param mapDragEnable the mapDragEnable to set
     */
    public void setMapDragEnable(boolean mapDragEnable) {
        this.mapDragEnable = mapDragEnable;
    }

    /**
     * Whenever the component is painted, its graphics are updated to the current vehicle state.
     */
    public void update(Graphics2D g2d, boolean force) {
        if (antialiasing)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        else
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Graphics2D original = g2d;
        if (lockedVehicle != null && vehicleStates.get(lockedVehicle) != null) {
            SystemPositionAndAttitude state = vehicleStates.get(lockedVehicle);
            // NeptusLog.pub().info("<###> "+state);
            LocationType loc = new LocationType(state.getPosition());
            loc.convertToAbsoluteLatLonDepth();
            setRotation(state.getYaw());
            setCenter(loc);
        }

        // g2d = (Graphics2D) g2d.create();
        identity = g2d.getTransform();

        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        boolean systemsPainterActive = false;

        synchronized (painters) {
            for (Renderer2DPainter painter : painters.getPreRenderPainters()) {
                if (painter instanceof SystemPainterProvider) {
                    SystemPainterProvider spp = (SystemPainterProvider) painter;
                    systemsPainterActive = systemsPainterActive || spp.isSystemPainterEnabled();
                }
                Graphics2D g = (Graphics2D) original.create();
                try {
                    painter.paint(g, this);
                }
                catch (Exception e) {
                    NeptusLog.pub().warn(
                            painter.getClass().getSimpleName() + " pre-render painter: " + e.getMessage(), e);
                }
                g.dispose();
            }
        }

        AbstractElement[] objs = mapGroup.getAllObjects();

        for (int i = 0; i < objs.length; i++) {
            AbstractElement tmp = objs[i];
            if (tmp.getTransparency() >= 100)
                continue;
            try {
                tmp.paint((Graphics2D) original.create(), this, (float) -rotationRads);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(tmp.getClass().getSimpleName() + " map element paint: " + e.getMessage(), e);
            }
        }

        g2d = (Graphics2D) original.create();
        synchronized (painters) {
            for (Renderer2DPainter painter : painters.getPostRenderPainters()) {
                if (painter instanceof SystemPainterProvider) {
                    SystemPainterProvider spp = (SystemPainterProvider) painter;
                    systemsPainterActive = systemsPainterActive || spp.isSystemPainterEnabled();
                }
                Graphics2D g = (Graphics2D) original.create();
                try {
                    painter.paint(g, this);
                }
                catch (Exception e) {
                    NeptusLog.pub().warn(
                            painter.getClass().getSimpleName() + " post-render painter: " + e.getMessage(), e);
                }
                g.dispose();
            }
        }

        g2d.setTransform(identity);
        // Tail drawing
        for (String key : vehicleTails.keySet()) {
            VehicleTailElement vte = vehicleTails.get(key);
            if (isAllTailOn || (!isAllTailOn && vehiclesTailOn.contains(key)))
                if (vte != null) {
                    try {
                        vte.paint(g2d, this, (float) -rotationRads);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(
                                vte.getClass().getSimpleName() + " vehicle breadcrums painter: " + e.getMessage(), e);
                    }
                }
        }

        // Normalizes the graphics transformation
        g2d.setTransform(identity);
        g2d.translate(this.getWidth() / 2, this.getHeight() / 2);
        g2d.scale(1, -1);
        g2d.setColor(Color.BLACK);

        // And paints all the vehicles on screen
        for (Enumeration<String> e = vehicleStates.keys(); e.hasMoreElements();) {
            String system = (String) e.nextElement();
            VehicleType vehicle = VehiclesHolder.getVehicleById(system);
            Image vehicleImage = (Image) vehicleImages.get(system);
            SystemPositionAndAttitude vehicleState = (SystemPositionAndAttitude) vehicleStates.get(system);
            double wSize = vehicle != null ? vehicle.getYSize() : 1;
            double hSize = vehicle != null ? vehicle.getXSize() : 1;
            double vehicleWidth = wSize * zoom;
            double vehicleHeight = hSize * zoom;

            if (fixedVehicleWidth != 0) {
                if (hSize > wSize) {
                    vehicleHeight = fixedVehicleWidth;
                    vehicleWidth = fixedVehicleWidth * (wSize / hSize);
                }
                else {
                    vehicleWidth = fixedVehicleWidth;
                    vehicleHeight = (hSize / wSize) * fixedVehicleWidth;
                }
            }
            Point2D tt = getScreenPosition(vehicleState.getPosition());
            Graphics2D copy = (Graphics2D) original.create();
            copy.setStroke(new BasicStroke(1f));

            if (lockedVehicle != null && system != null && lockedVehicle.equals(system))
                copy.setColor(Color.green.brighter());
            else
                copy.setColor(Color.red.darker());

            copy.translate(tt.getX(), tt.getY());
            if (!systemsPainterActive /* isVehicleSymbolShown() */)
                copy.drawString(system/* .getId() */, 12, 0);

            copy.rotate(vehicleState.getYaw() - rotationRads); // Needs to be rotated anyway for the image drawing
            // bellow
            if (!systemsPainterActive /* isVehicleSymbolShown() */) {
                Color iconColor = vehicle != null ? vehicle.getIconColor() : Color.WHITE;
                copy.draw(new Ellipse2D.Double(-10, -10, 20, 20));
                copy.scale(1, -1);
                copy.setColor(iconColor);
                copy.fill(arrow);
                copy.setColor(iconColor.darker());
                copy.draw(arrow);
                copy.scale(1, -1);
            }
            if (isVehicleImageShown()) {
                Graphics2D otherCopy = (Graphics2D) copy.create();
                otherCopy.scale(vehicleWidth / vehicleImage.getWidth(null),
                        -vehicleHeight / vehicleImage.getHeight(null));
                otherCopy.drawImage(vehicleImage, (int) ((-vehicleImage.getWidth(null) / 2)),
                        (int) (-vehicleImage.getHeight(null) / 2), null);
                // otherCopy.drawImage(vehicleImage, (int) ((vehicleWidth * zoom)), (int) (vehicleHeight * zoom), null);
                otherCopy.dispose();
            }
        }

        if (activeInteraction != null)
            activeInteraction.paintInteraction(g2d, this);

        if (isGridShown())
            drawGrid(g2d, getGridSize());
    }

    /**
     * {@link MouseMotionListener} implementation. Used to translate / rotate / zoom the map
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        requestFocusInWindow();
        activeInteraction.mouseDragged(e, this);
        repaint();
    }

    /**
     * {@link MouseMotionListener} implementation. Used to update the ruler position or mouse location.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        requestFocusInWindow();
        activeInteraction.mouseMoved(e, this);
        repaint();
    }

    /**
     * Add a listener to the set of objects that will be warned whenever there is a right click in the renderer
     * 
     * @param listener The listener to be added
     */
    public void addRightMouseClickListener(RightMouseClickListener listener) {
        if (!rightClickListeners.contains(listener))
            rightClickListeners.add(listener);
    }

    /**
     * Removes the lister. See {@link #addRightMouseClickListener(RightMouseClickListener)}
     * 
     * @param listener The listener to be removed
     */
    public void removeRightMouseClickListener(RightMouseClickListener listener) {
        rightClickListeners.remove(listener);
    }

    /**
     * {@link MouseListener} implementation. Used to display right-click popup menu
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        requestFocusInWindow();

        activeInteraction.mouseClicked(e, this);
        if (activeInteraction != defaultInteraction && activeInteraction.isExclusive())
            return;

        final Point2D mousePoint = e.getPoint();
        final LocationType loc = getRealWorldLocation(mousePoint);

        // Right click
        if (e.getButton() == MouseEvent.BUTTON3 && !ignoreRightClicks) {

            JPopupMenu popup = new JPopupMenu();

            if (rightClickListeners.size() > 0) {
                final MouseEvent evt = e;

                for (RightMouseClickListener l : rightClickListeners) {
                    final RightMouseClickListener r = l;
                    @SuppressWarnings("serial")
                    AbstractAction act = new AbstractAction(r.getPresentationName()) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            r.itemSelected(StateRenderer2D.this, evt.getPoint(), getRealWorldLocation(evt.getPoint()));
                        }
                    };
                    popup.add(act);
                }

                popup.addSeparator();

            }

            final LocationType lt = getRealWorldLocation(e.getPoint());

            JMenuItem item = new JMenuItem(I18n.text("Choose Visible World Map"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    worldMapPainter.showChooseMapStyleDialog(StateRenderer2D.this);
                }
            });
            item.setIcon(new ImageIcon(worldMapPainter.ICON_WORLD_SETTINGS));
            popup.add(item);

            item = new JMenuItem(I18n.text("Choose Visible Layers"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    painterSelection();
                }
            });
            popup.add(item);

            item = new JMenuItem(I18n.text("Copy Location"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    CoordinateUtil.copyToClipboard(lt);
                }
            });
            item.setIcon(ImageUtils.getIcon("images/menus/editcopy.png"));
            popup.add(item);

            item = new JMenuItem(I18n.text("Center"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    setCenter(lt);
                    repaint();
                }
            });

            item = new JMenuItem(I18n.text("R2D Shortcuts"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    GuiUtils.htmlMessage(
                            ConfigFetch.getSuperParentFrame() == null ? StateRenderer2D.this
                                    : ConfigFetch.getSuperParentFrame(),
                            I18n.text("2D Renderer Shortcuts"),
                            I18n.text("(Keys pressed while the Renderer component is focused)"),
                            MapShortcutsLayer.getShortcutsHtml());
                }
            });
            item.setIcon(ImageUtils.getIcon("images/menus/info.png"));
            popup.add(item);

            item = new JMenuItem(I18n.text("Settings..."));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    PropertiesEditor.editProperties(StateRenderer2D.this, true);
                }
            });
            item.setIcon(ImageUtils.getIcon("images/menus/settings.png"));
            // popup.add(item);

            for (IEditorMenuExtension extension : menuExtensions) {
                Collection<JMenuItem> items = null;

                try {
                    items = extension.getApplicableItems(loc, this);
                }
                catch (Exception ex) {
                    NeptusLog.pub().error(ex, ex);
                }

                if (items != null && !items.isEmpty()) {
                    popup.addSeparator();
                    for (JMenuItem it : items) {
                        if (it instanceof JMenu)
                            MenuScroller.setScrollerFor((JMenu) it, this, 150, 0, 0);
                        popup.add(it);
                    }
                }
            }

            popup.show(this, e.getX(), e.getY());

            return;
        }
    }

    /**
     * {@link MouseListener} implementation. Currently empty.
     */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@link MouseListener} implementation. Currently calls activeInteraction.mouseExited(e, this).
     */
    @Override
    public void mouseExited(MouseEvent e) {
        activeInteraction.mouseExited(e, this);
    }

    /**
     * {@link MouseListener} implementation. Used for interacting with renderer, according to current mode
     */
    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        activeInteraction.mousePressed(e, this);
        repaint();
    }

    /**
     * {@link MouseListener} implementation. Used for interacting with renderer, according to current mode
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        requestFocusInWindow();
        activeInteraction.mouseReleased(e, this);
        repaint();
    }
    
    @Override
    public void focusGained(FocusEvent e) {
        activeInteraction.focusGained(e, this);
    }
    
    @Override
    public void focusLost(FocusEvent e) {
        activeInteraction.focusLost(e, this);
    }

    /**
     * Translates a screen coordinate into ned from Guinea (0,0)
     * 
     * @param screenX The x-axis coordinate of a screen pixel
     */
    public double getRealXCoord(int screenX) {
        double[] mapcenter = getCenter().getOffsetFrom(new LocationType());
        double x = screenX;
        x -= getWidth() / 2;
        return (mapcenter[1] + x / zoom);
    }

    /**
     * Translates a screen coordinate into ned from Guinea (0,0)
     * 
     * @param screenY The y-axis coordinate of a screen pixel
     */
    public double getRealYCoord(int screenY) {
        double[] mapcenter = getCenter().getOffsetFrom(new LocationType());
        double y = screenY;
        y -= getHeight() / 2;
        return (double) (mapcenter[0] - y / zoom);
    }

    /**
     * Given a point on the screen, returns its real world coordinates
     * 
     * @param screenCoordinates A point on screen
     * @return The real world location of the given point (absolute)
     */
    public LocationType getRealWorldLocation(Point2D screenCoordinates) {
        return MapTileRendererCalculator.getRealWorldLocationHelper(screenCoordinates, worldPixelXY, getSize(),
                levelOfDetail, rotationRads);
    }

    /**
     * Recenters the map, presenting the given location at the exact center of the Renderer
     * 
     * @param location the location to focus
     */
    @Override
    public void focusLocation(LocationType location) {
        // center = new LocationType();
        // center.setLocation(location);
        setCenter((LocationType) location.getNewAbsoluteLatLonDepth());
        repaint();
    }

    /**
     * Recenters the map to the central point of the given MapObject
     * 
     * @param mo The map object that will appear in the center of the Renderer
     */
    @Override
    public void focusObject(AbstractElement mo) {
        focusLocation(mo.getCenterLocation());
    }

    /**
     * Returns the current view mode
     * 
     * @return The current view mode
     */
    public int getViewMode() {
        return TRANSLATION;
    }

    /**
     * @return the minDelay
     */
    public int getMinDelay() {
        return minDelay;
    }

    /**
     * @param minDelay the minDelay to set
     */
    public void setMinDelay(int minDelay) {
        this.minDelay = minDelay;
    }

    /**
     * Sets the currently locked vehicle
     */
    @Override
    public void followVehicle(String systemId) {
        if (systemId != null)
            show_mode = MAP_MOVES;
        else
            show_mode = VEHICLE_MOVES;

        this.lockedVehicle = systemId;

        repaint();
    }

    /**
     * Add a listener to this renderer. See {@link Renderer}.
     */
    @Override
    public void addChangeListener(ChangeListener cl) {
        changeListeners.add(cl);
    }

    /**
     * Remove a listener from this renderer. See {@link Renderer}
     */
    @Override
    public void removeChangeListener(ChangeListener cl) {
        changeListeners.remove(cl);
    }

    /**
     * Retrieve the currently active show mode (vehicle lock).
     * 
     * @return 0 if the view is not locked in the vehicle or 1 if the vehicle is locked
     */
    @Override
    public int getShowMode() {
        return show_mode;
    }

    /**
     * Verifies if the legent is currently being shown
     */
    public boolean isLegendShown() {
        return legendShown;
    }

    /**
     * Set the visibility for the legent overlay
     */
    public void setLegendShown(boolean legendShown) {
        if (this.legendShown == false && legendShown == true) {
            addPostRenderPainter(legend, "Legend");
            addPostRenderPainter(cursorPainter, "Cursor Painter");
        }

        if (this.legendShown == true && legendShown == false) {
            removePostRenderPainter(legend);
            removePostRenderPainter(cursorPainter);
        }

        this.legendShown = legendShown;
    }

    /**
     * @return the worldMapShown
     */
    public boolean isWorldMapShown() {
        return worldMapShown;
    }

    /**
     * @param worldMapShown the worldMapShown to set
     */
    public void setWorldMapShown(boolean worldMapShown) {
        if (worldMapPainter == null)
            return;
        worldMapPainter.setDrawWorldMap(worldMapShown);
        this.worldMapShown = worldMapShown;
    }

    /**
     * @return the worldBondariesShown
     */
    public boolean isWorldBondariesShown() {
        return worldBondariesShown;
    }

    /**
     * @param worldBondariesShown the worldBondariesShown to set
     */
    public void setWorldBondariesShown(boolean worldBondariesShown) {
        if (worldMapPainter == null)
            return;
        worldMapPainter.setDrawWorldBoundaries(worldBondariesShown);
        this.worldBondariesShown = worldBondariesShown;
    }

    /**
     * @return the worldMapStyle
     */
    public String getWorldMapStyle() {
        return worldMapStyle;
    }

    /**
     * @param worldMapStyle the worldMapStyle to set
     */
    public void setWorldMapStyle(String worldMapStyle) {
        if (worldMapPainter == null)
            return;
        worldMapPainter.setMapStyle(worldMapStyle);
        this.worldMapStyle = worldMapStyle;
    }

    /**
     * Retrieve a real-world location that is the center of the currently visible view
     * 
     * @return The center of the currently visible view
     */
    public LocationType getCenter() {
        return center.getNewAbsoluteLatLonDepth();
    }

    /**
     * Translates the current view in order to have the given location at its center <br>
     * <b>NOTE:</b> Don't use this method when you are translating the map (for this use the {@link #worldPixelXY}).
     * 
     * @param center The new center
     */
    public void setCenter(LocationType center) {
        Point2D nc = center.getNewAbsoluteLatLonDepth().getPointInPixel(getLevelOfDetail());
        worldPixelXY.setLocation(nc);
        setLevelOfDetail(getLevelOfDetail());
    }

    /**
     * 
     * @param g Draws a grid overlay
     * @param cellSize The width of each grid cell, in meters
     */
    public void drawGrid(Graphics2D g, double cellSize) {
        double panelCellSize = (cellSize * zoom);

        if (panelCellSize < 3)
            return;

        g.setTransform(identity);
        g.setColor(new Color(getGridColor().getRed(), getGridColor().getGreen(), getGridColor().getBlue(), 100));

        if (getRotation() == 0) {

            double offsets[] = getRealWorldLocation(new Point2D.Double(0, 0)).getOffsetFrom(
                    getMapGroup().getCoordinateSystem());

            for (double i = -(offsets[1] % gridSize) * getZoom(); i < getWidth(); i += panelCellSize) {
                g.draw(new Line2D.Double(i, 0, i, getHeight()));
            }
            for (double i = (offsets[0] % gridSize) * getZoom(); i < getHeight(); i += panelCellSize) {
                g.draw(new Line2D.Double(0, i, getWidth(), i));
            }
        }
    }

    /**
     * Verifies if the grid is currently being drawn on screen
     * 
     * @return <b>true</b> if the grid is being shown or <b>false</b> otherwise
     */
    public boolean isGridShown() {
        return gridShown;
    }

    /**
     * Sets the grid visibility
     */
    public void setGridShown(boolean gridShown) {
        this.gridShown = gridShown;
    }

    /**
     * Retrieve the current grid cell width
     */
    public double getGridSize() {
        return gridSize;
    }

    /**
     * Change the grid cell width in meters
     */
    public void setGridSize(double gridSize) {
        this.gridSize = gridSize;
    }

    /**
     * Retrieve the color of the grid
     */
    public Color getGridColor() {
        return gridColor;
    }

    /**
     * Set the color of the grid
     */
    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
    }

    /**
     * Retrieve the currently locked vehicle
     * 
     * @return The locked vehicle or <b>null</b> if no vehicle is locked
     */
    @Override
    public String getLockedVehicle() {
        return lockedVehicle;
    }

    /**
     * Change the currently locked vehicle
     * 
     * @param lockedVehicle The vehicle to be locked
     */
    public void setLockedVehicle(String lockedVehicle) {
        this.lockedVehicle = lockedVehicle;
    }

    /**
     * Perform cleanup tasks
     */
    @Override
    public void cleanup() {
        if (getMapGroup() != null)
            getMapGroup().removeChangeListener(this);

        GeneralPreferences.removePreferencesListener(this);
        shuttingDown = true;
    }

    /**
     * @return the identity
     */
    public AffineTransform getIdentity() {
        return identity;
    }

    /**
     * {@link KeyListener} implementation. Monitors key presses for changing the current view
     */
    @Override
    public void keyPressed(KeyEvent keyEvt) {
        activeInteraction.keyPressed(keyEvt, this);
        repaint();
        return;
    }

    /**
     * {@link KeyListener} implementation. Currently empty.
     */
    @Override
    public void keyReleased(KeyEvent keyEvt) {
        activeInteraction.keyReleased(keyEvt, this);
        repaint();
    }

    /**
     * {@link KeyListener} implementation. Currently empty.
     */
    @Override
    public void keyTyped(KeyEvent keyEvt) {
        activeInteraction.keyTyped(keyEvt, this);
        repaint();
    }

    /**
     * {@link PropertiesProvider} implementation.
     */
    @Override
    public DefaultProperty[] getProperties() {
        DefaultProperty p1 = PropertiesEditor.getPropertyInstance("World Bondaries Shown", Boolean.class, new Boolean(
                isWorldBondariesShown()), true);
        DefaultProperty p2 = PropertiesEditor.getPropertyInstance("WorldMap Shown", Boolean.class, new Boolean(
                isWorldMapShown()), true);
        return new DefaultProperty[] {  p1, p2 };
    }

    /**
     * {@link PropertiesProvider} implementation.
     */
    @Override
    public String getPropertiesDialogTitle() {
        return "Renderer2D properties";
    }

    /**
     * {@link PropertiesProvider} implementation.
     */
    @Override
    public void setProperties(Property[] properties) {
        for (Property p : properties) {
            if (p.getName().equals("Show Grid")) {
                setGridShown((Boolean) p.getValue());
            }
            if (p.getName().equals("Grid Size")) {
                setGridSize((Double) p.getValue());
            }
            if (p.getName().equals("Grid Color")) {
                setGridColor((Color) p.getValue());
            }
            if (p.getName().equals("World Bondaries Shown")) {
                setWorldBondariesShown((Boolean) p.getValue());
            }
            if (p.getName().equals("WorldMap Shown")) {
                setWorldMapShown((Boolean) p.getValue());
            }
            if (p.getName().equals("WorldMap Style")) {
                setWorldMapStyle((String) p.getValue());
            }
        }
    }

    /**
     * {@link PropertiesProvider} implementation.
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    /**
     * Return the preferred size for this component
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 200);
    }

    /**
     * Add a new overlay to this renderer
     * 
     * @return <b>true</b> if the overlay was added or <b>false</b> if the overlay was already added.
     */
    @Override
    public boolean addPostRenderPainter(Renderer2DPainter painter, String name) {
        synchronized (painters) {
            LayerPriority lp = painter.getClass().getAnnotation(LayerPriority.class);
            if (lp != null)
                painters.addPainter(I18n.text(name), painter, lp.priority(), 0);
            else
                painters.addPainter(I18n.text(name), painter, 1, 0);
        }
        return true;
    }

    /**
     * Remove an overlay from this renderer
     * 
     * @return <b>true</b> if the given overlay was removed or <b>false</b> if it didn't exist
     */
    @Override
    public boolean removePostRenderPainter(Renderer2DPainter painter) {
        synchronized (painters) {
            painters.remove(painter);
        }
        return true;
    }

    /**
     * Add an overlay that will be painted <b>before</b> the map
     */
    @Override
    public void addPreRenderPainter(Renderer2DPainter painter) {
        synchronized (painters) {
            LayerPriority lp = painter.getClass().getAnnotation(LayerPriority.class);
            if (lp != null)
                painters.addPainter(painter.getClass().getSimpleName(), painter, lp.priority(), 0);
            else
                painters.addPainter(painter.getClass().getSimpleName(), painter, -1, 0);
        }
    }

    /**
     * Remove the given overlay
     */
    @Override
    public void removePreRenderPainter(Renderer2DPainter painter) {
        synchronized (painters) {
            painters.remove(painter);
        }
    }

    /**
     * Remove any overlay that is an instance of the given class
     * 
     * @param clazz A class
     */
    public void removePaintersOfType(Class<?> clazz) {
        synchronized (painters) {
            painters.removePaintersOfType(clazz);
        }
    }

    public void setPainterActive(String painterName, boolean active) {
        painters.setPainterActive(painterName, active);
    }

    /**
     * Adds the given mouse listener as listener for this component
     */
    @Override
    public synchronized void addMouseListener(MouseListener l) {
        super.addMouseListener(l);
    }

    /**
     * If set to true, right-click menus won't be shown
     */
    public void setIgnoreRightClicks(boolean ignoreNextClick) {
        this.ignoreRightClicks = ignoreNextClick;
    }

    /**
     * @return The current map rotation in radians.
     */
    public double getRotation() {
        while (rotationRads > 2 * Math.PI)
            rotationRads -= 2 * Math.PI;

        while (rotationRads < -2 * Math.PI)
            rotationRads += 2 * Math.PI;

        return rotationRads;
    }

    /**
     * @param rotationAngle the rotationAngle to be set on the renderer, in radians
     */
    public void setRotation(double rotationAngle) {
        this.rotationRads = rotationAngle;
        warnRendererChangeEvent();
    }

    /**
     * This method verifies if any object is intercepted by the given xy coordinates It is very useful for verifying if
     * a click on the map was over an object.
     * 
     * @param screenPoint The point in the screen to test for possible interceptions
     * @return The intercepted <b>MapObject</b> or <b>null</b> if no object was intercepted
     */
    public AbstractElement getFirstInterceptedObject(Point2D screenPoint) {
        LocationType lt = getRealWorldLocation(screenPoint);
        Object[] objArray;

        if (editingMap == null) {
            objArray = mapGroup.getAllObjects();
            // NeptusLog.pub().info("<###>editing map is null!");
        }
        else {
            // NeptusLog.pub().info("<###>editing map is not null!");
            objArray = mapGroup.getObjectsFromMap(editingMap);
        }

        for (int i = 0; i < objArray.length; i++) {
            AbstractElement obj = (AbstractElement) objArray[i];
            if (obj.containsPoint(lt, this)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * @return The id of the map that is currently being edited (or null if no map is being edited)
     */
    public String getEditingMap() {
        return editingMap;
    }

    /**
     * Set the if of the map that is being edited
     * 
     * @param editingMap The id of the map that is being edited
     */
    public void setEditingMap(String editingMap) {
        this.editingMap = editingMap;
    }

    /**
     * Clear the tail of the given vehicles
     * 
     * @param An array of vehicles whose tails should be cleared
     */
    @Override
    public void clearVehicleTail(String[] vehicles) {
        if (vehicles == null) {
            for (VehicleTailElement vte : vehicleTails.values())
                vte.clearPoints();
            return;
        }
        for (String v : vehicles) {
            VehicleTailElement vte = vehicleTails.get(v);
            if (vte != null)
                vte.clearPoints();
        }
        repaint();
    }

    /**
     * Stop showing the tails of the given vehicles
     * 
     * @param An array of vehicles whose tails should not be displayed
     */
    @Override
    public void setVehicleTailOff(String[] vehicles) {
        if (vehicles == null) {
            isAllTailOn = false;
            vehiclesTailOn.clear();
        }
        else {
            if (isAllTailOn) {
                boolean oneOK = false;
                for (String v : vehicles) {
                    if (vehiclesTailOn.remove(v))
                        oneOK = true;
                }
                if (oneOK)
                    isAllTailOn = false;
            }
        }
    }

    /**
     * Start showing the tails of the given vehicles
     * 
     * @param An array of vehicles whose tails should be displayed
     */
    @Override
    public void setVehicleTailOn(String[] vehicles) {
        if (vehicles == null) {
            isAllTailOn = true;
            vehiclesTailOn.clear();
        }
        else {
            if (!isAllTailOn) {
                for (String v : vehicles)
                    vehiclesTailOn.add(v);
            }
        }
    }

    /**
     * @see pt.lsts.neptus.util.conf.PreferencesListener#preferencesUpdated()
     */
    @Override
    public void preferencesUpdated() {
        int np = GeneralPreferences.numberOfShownPoints;
        for (VehicleTailElement vte : vehicleTails.values())
            vte.setNumberOfPoints(np);
        if (np < 0)
            numberOfShownPoints = ScatterPointsElement.INFINITE_NUMBER_OF_POINTS;
        else
            numberOfShownPoints = np;
    }

    /**
     * @return the smoothResizing
     */
    public boolean isSmoothResizing() {
        return smoothResizing;
    }

    /**
     * @param smoothResizing the smoothResizing to set
     */
    public void setSmoothResizing(boolean smoothResizing) {
        this.smoothResizing = smoothResizing;
    }

    /**
     * @return the antialiasing
     */
    public boolean isAntialiasing() {
        return antialiasing;
    }

    /**
     * @param antialiasing the antialiasing to set
     */
    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
    }

    /**
     * @return the vehicleLocation
     */
    public LocationType getVehicleLocation(String vehicle) {
        try {
            return vehicleStates.get(vehicle).getPosition();
        }
        catch (Exception e) {
            NeptusLog.pub().debug("getVehicleLocation(" + vehicle + ")");
            return null;
        }
    }

    /**
     * @return the vehicleStates
     */
    public SystemPositionAndAttitude getVehicleState(String vehicle) {
        try {
            return vehicleStates.get(vehicle);
        }
        catch (Exception e) {
            NeptusLog.pub().debug("getVehicleState(" + vehicle + ")");
            return null;
        }
    }

    /**
     * @return An array of vehicles currently being displayed in the renderer
     */
    public String[] getVehiclesInRender() {
        return vehicles;
    }

    /**
     * Add a new form of interaction with the renderer
     */
    @Override
    public void addInteraction(StateRendererInteraction interaction) {
        if (!interactions.contains(interaction))
            interactions.add(interaction);
    }

    /**
     * Remove the given interaction from this renderer
     */
    @Override
    public void removeInteraction(StateRendererInteraction interaction) {
        if (activeInteraction == interaction) {
            setActiveInteraction(defaultInteraction);
        }
        interactions.remove(interaction);
    }

    @Deprecated
    @Override
    public void setViewMode(int mode) {
    }

    /**
     * Change the currently active interaction
     */
    @Override
    public void setActiveInteraction(StateRendererInteraction interaction) {
        requestFocusInWindow();
        if (interaction == null)
            this.activeInteraction = defaultInteraction;
        else
            this.activeInteraction = interaction;
    }

    /**
     * Retrieve the currently active interaction for this renderer
     */
    @Override
    public StateRendererInteraction getActiveInteraction() {
        return activeInteraction;
    }

    /**
     * Retrieve a list of all available interactions
     */
    @Override
    public final Collection<StateRendererInteraction> getInteractionModes() {
        return interactions;
    }

    // New for MiniMap UAV Panel
    /**
     * @return the lastDragPoint
     */
    public Point2D getLastDragPoint() {
        return lastDragPoint;
    }

    /**
     * Show a dialog where the user can select which layers to show
     */
    public void painterSelection() {
        painters.showSelectionDialog(SwingUtilities.getWindowAncestor(this));
    }

    @Override
    public boolean addMenuExtension(IEditorMenuExtension extension) {
        if (!menuExtensions.contains(extension))
            return menuExtensions.add(extension);
        return false;
    }

    @Override
    public final Collection<IEditorMenuExtension> getMenuExtensions() {
        return menuExtensions;
    }

    @Override
    public boolean removeMenuExtension(IEditorMenuExtension extension) {
        return menuExtensions.remove(extension);
    }

    @Override
    public StateRenderer2D getRenderer() {
        return this;
    }

    /**
     * @return the worldMapPainter
     */
    public WorldRenderPainter getWorldMapPainter() {
        return worldMapPainter;
    }
    
    /**
     * This will create a new renderer change event and dispatch it to the listener, if set.
     */
    private void warnRendererChangeEvent() {
        if (processingRendererChangeEvents || rendererChangelistener == null)
            return;
        
        RendererChangeEvent event = new RendererChangeEvent(this, getCenter(), getRotation(), getLevelOfDetail());
        NeptusLog.pub().debug("Sending   " + event);
        // new Exception().printStackTrace();
        rendererChangelistener.mapRendererChangeEvent(event);
    }
    
    /**
     * To add a {@link IMapRendererChangeEvent} listener.
     * 
     * @param listener
     */
    public void addRendererChangeEvent(IMapRendererChangeEvent listener) {
        this.rendererChangelistener = listener;
    }

    /**
     * To remove a {@link IMapRendererChangeEvent} listener.
     * 
     * @param listener
     */
    public void removeRendererChangeEvent(IMapRendererChangeEvent listener) {
        if (rendererChangelistener != null && rendererChangelistener == listener)
            this.rendererChangelistener = null;
    }

    /**
     * To enable or disable the external renderer change events.
     * 
     * @param respondToRendererChangeEvents the respondToRendererChangeEvents to set
     */
    public void setRespondToRendererChangeEvents(boolean respondToRendererChangeEvents) {
        this.respondToRendererChangeEvents = respondToRendererChangeEvents;
    }
    
    /**
     * This is an external change event to be processed.
     * 
     * @param event
     * @return
     */
    public boolean newRendererChangeEvent(IMapRendererChangeEvent.RendererChangeEvent event) {
        if (!respondToRendererChangeEvents || this == event.getSource())
            return false;
        
        NeptusLog.pub().debug("Receiving " + event);
        processingRendererChangeEvents = true;
        try {
            setCenter(event.getCenterLoc());
            setRotation(event.getRotationRads());
            setLevelOfDetail(event.getLevelOfDetail());
        }
        catch (Exception e) {
            NeptusLog.pub().debug(e.getMessage());
        }
        finally {
            processingRendererChangeEvents = false;
        }

        return true;
    }
}
