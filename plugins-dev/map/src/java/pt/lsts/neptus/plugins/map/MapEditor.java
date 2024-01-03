/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 10, 2011
 */
package pt.lsts.neptus.plugins.map;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Collections;
import java.util.Timer;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.undo.UndoManager;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.map.edit.AddObjectEdit;
import pt.lsts.neptus.plugins.map.edit.ObjectPropertiesEdit;
import pt.lsts.neptus.plugins.map.edit.RemoveObjectEdit;
import pt.lsts.neptus.plugins.map.interactions.Box2DInteraction;
import pt.lsts.neptus.plugins.map.interactions.DrawPathInteraction;
import pt.lsts.neptus.plugins.map.interactions.LineInteraction;
import pt.lsts.neptus.plugins.map.interactions.MineDangerAreaInteraction;
import pt.lsts.neptus.plugins.map.interactions.PolygonInteraction;
import pt.lsts.neptus.plugins.map.interactions.QRouteInteraction;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.Model3DElement;
import pt.lsts.neptus.types.map.RotatableElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.coord.GdalDataSet;

/**
 * @author zp
 * @author pdias
 */
@PluginDescription(author = "José Pinto, Paulo Dias", name = "Map Editor", icon = "pt/lsts/neptus/plugins/map/map-edit.png", version = "1.5", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 90)
public class MapEditor extends ConsolePanel implements StateRendererInteraction, Renderer2DPainter,
        MissionChangeListener, ConfigurationListener {

    private static final long serialVersionUID = 1L;

    protected static ImageIcon handIcon = ImageUtils.createImageIcon("images/icons/hand.png");
    
    protected InteractionAdapter adapter;
    protected InteractionAdapter currentInteraction = null;
    protected UndoManager manager = createManager();
    protected Point mousePoint = null;
    protected Timer dragTesterTimer = null;
    protected Vector<AbstractElement> intersectedObjects = new Vector<AbstractElement>();
    protected MapGroup mg = null;
    protected StateRenderer2D renderer;
    protected MapType pivot = null;
    protected AbstractElement draggedObject = null;
    protected LocationType originalObjLocation = null;
    protected JToolBar toolbar = createToolbar();
    protected Point2D dragOffsets = null;
    protected boolean objectRotated = false;
    protected boolean objectMoved = false;
    protected double originalYaw;
    protected String orignalXML = null;
    protected ToolbarButton undo, redo;
    protected ToolbarSwitch associatedSwitch = null;

    public enum ControlsLocation {
        Left,
        Right,
        Top,
        Bottom
    };

    @NeptusProperty(name = "Toolbar location", userLevel = LEVEL.ADVANCED)
    public ControlsLocation toolbarLocation = ControlsLocation.Right;

    @NeptusProperty(name = "Ignore Addition of Transponders", category = "Ignore List", userLevel = LEVEL.ADVANCED)
    public boolean ignoreAdditionOfTransponders = false;

    @NeptusProperty(name = "Ignore Addition of Model3D", category = "Ignore List", userLevel = LEVEL.ADVANCED)
    public boolean ignoreAdditionOfModel3D = false;

    public MapEditor(ConsoleLayout console) {
        super(console);
        try {
            adapter = new InteractionAdapter(console);
        }
        catch (Exception e) { // for PluginsPotGenerator to work
            e.printStackTrace();
        }
        setVisibility(false);
    }

    @Override
    public void propertiesChanged() {
        Container parent = toolbar.getParent();
        if (parent != null) {
            parent.remove(toolbar);
            switch (toolbarLocation) {
                case Left:
                    parent.add(toolbar, BorderLayout.WEST);
                    toolbar.setOrientation(JToolBar.VERTICAL);
                    break;
                case Right:
                    parent.add(toolbar, BorderLayout.EAST);
                    toolbar.setOrientation(JToolBar.VERTICAL);
                    break;
                case Top:
                    parent.add(toolbar, BorderLayout.NORTH);
                    toolbar.setOrientation(JToolBar.HORIZONTAL);
                    break;
                case Bottom:
                    parent.add(toolbar, BorderLayout.SOUTH);
                    toolbar.setOrientation(JToolBar.HORIZONTAL);
                    break;
            }

            parent.invalidate();
            parent.validate();
        }
    }

    protected UndoManager createManager() {
        return new UndoManager() {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized void undo() throws javax.swing.undo.CannotUndoException {
                super.undo();
                updateUndoRedoActions();
            };

            @Override
            public synchronized void redo() throws javax.swing.undo.CannotRedoException {
                super.redo();
                updateUndoRedoActions();
            };

            @Override
            public synchronized boolean addEdit(javax.swing.undo.UndoableEdit anEdit) {
                boolean ret = super.addEdit(anEdit);
                if (ret)
                    updateUndoRedoActions();
                return ret;
            };
        };
    }

    protected void updateUndoRedoActions() {
        undo.setEnabled(manager.canUndo());
        undo.setToolTipText(manager.getUndoPresentationName());
        redo.setEnabled(manager.canRedo());
        redo.setToolTipText(manager.getRedoPresentationName());
    }

    protected void disableAllInteractionsBut(ToolbarSwitch source) {
        for (Component c : toolbar.getComponents()) {
            if (c instanceof ToolbarSwitch && ((ToolbarSwitch) c).isSelected()) {
                if (!c.equals(source))
                    ((ToolbarSwitch) c).doClick();
            }
        }
    }

    protected MapType getPivot() {
        return mg.getPivotMap();
    }

    protected JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);

        toolbar.setBackground(new Color(255, 200, 200));
        toolbar.setOpaque(true);
        final ToolbarSwitch freehand = new ToolbarSwitch(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/map/interactions/fh.png"),
                I18n.text("Add Free-hand drawing"), "fh");
        freehand.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    disableAllInteractionsBut(freehand);
                    currentInteraction = new DrawPathInteraction(getPivot(), manager, getConsole());
                    currentInteraction.setAssociatedSwitch(freehand);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        freehand.setToolTipText(I18n.text("Add freehand drawing"));
        toolbar.add(freehand);

        final ToolbarSwitch fp = new ToolbarSwitch(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/map/interactions/poly.png"),
                I18n.text("Add filled polygon"), "fp");

        fp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    disableAllInteractionsBut(fp);
                    currentInteraction = new PolygonInteraction(getPivot(), manager, true, getConsole());
                    currentInteraction.setAssociatedSwitch(fp);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        fp.setToolTipText(I18n.text("Add filled polygon"));
        toolbar.add(fp);

        final ToolbarSwitch up = new ToolbarSwitch(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/map/interactions/poly2.png"),
                I18n.text("Add unfilled polygon"), "fp");

        up.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    disableAllInteractionsBut(up);
                    currentInteraction = new PolygonInteraction(getPivot(), manager, false, getConsole());
                    currentInteraction.setAssociatedSwitch(up);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        up.setToolTipText(I18n.text("Add polygon (unfilled)"));
        toolbar.add(up);

        final ToolbarSwitch mda = new ToolbarSwitch(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/map/interactions/mda.png"),
                I18n.text("Add mine danger area"), "mda");

        mda.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    disableAllInteractionsBut(mda);
                    currentInteraction = new MineDangerAreaInteraction(getPivot(), manager, getConsole());
                    currentInteraction.setAssociatedSwitch(mda);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        mda.setToolTipText(I18n.text("Add mine danger area"));
        toolbar.add(mda);

        final ToolbarSwitch qr = new ToolbarSwitch(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/map/interactions/qr.png"), I18n.text("Add QRoute"),
                "qr");

        qr.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    disableAllInteractionsBut(qr);
                    currentInteraction = new QRouteInteraction(getPivot(), manager, getConsole());
                    currentInteraction.setAssociatedSwitch(qr);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        qr.setToolTipText(I18n.text("Add QRoute"));
        toolbar.add(qr);

        final ToolbarSwitch b2d = new ToolbarSwitch(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/map/interactions/b2d.png"), I18n.text("Add Box2D"),
                "b2d");

        b2d.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    disableAllInteractionsBut(b2d);
                    currentInteraction = new Box2DInteraction(getPivot(), manager, getConsole());
                    currentInteraction.setAssociatedSwitch(b2d);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        b2d.setToolTipText(I18n.text("Add Box2D"));
        toolbar.add(b2d);
        
        final ToolbarSwitch line = new ToolbarSwitch(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/map/interactions/draw-line.png"), I18n.text("Add Line Segment"),
                "line");

        line.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    disableAllInteractionsBut(line);
                    currentInteraction = new LineInteraction(getPivot(), manager, getConsole());
                    currentInteraction.setAssociatedSwitch(line);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        line.setToolTipText(I18n.text("Add Line Segment"));
        toolbar.add(line);

        undo = new ToolbarButton(ImageUtils.getIcon("pt/lsts/neptus/plugins/map/undo.png"), I18n.text("Undo"),
                "undo");

        undo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.undo();
            }
        });
        toolbar.add(undo);
        undo.setEnabled(manager.canUndo());

        redo = new ToolbarButton(ImageUtils.getIcon("pt/lsts/neptus/plugins/map/redo.png"), I18n.text("Redo"),
                "redo");

        redo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.redo();
            }
        });
        toolbar.add(redo);
        redo.setEnabled(manager.canRedo());

        return toolbar;
    }

    protected void testMouseIntersections() {
        intersectedObjects.clear();
        if (mousePoint == null || mg == null || renderer == null)
            return;

        LocationType loc = renderer.getRealWorldLocation(mousePoint);

        for (AbstractElement elem : mg.getAllObjects()) {
            if (elem.containsPoint(loc, renderer)) {
                intersectedObjects.add(elem);
            }
        }

        Collections.sort(intersectedObjects);
    }

    @Override
    public void missionUpdated(MissionType mission) {
        mg = MapGroup.getMapGroupInstance(mission);
        if (mg.getMaps().length == 0)
            mg.addMap(new MapType(mission.getHomeRef()));

        pivot = mg.getMaps()[0];
    }

    @Override
    public void missionReplaced(MissionType mission) {
        if (adapter.isActive())
            setActive(false, renderer);
        manager.discardAllEdits();
        pivot = null;
    }

    /**
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     *      pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        this.renderer = renderer;
    }

    /**
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getIconImage()
     */
    @Override
    public Image getIconImage() {
        return ImageUtils.getImage(PluginUtils.getPluginIcon(getClass()));
    }

    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    /**
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        return true;
    }

    protected void removeElement(String elemId) {
        if (mg.getMapObjectsByID(elemId).length == 0) {
            NeptusLog.pub().error("Trying to delete unexisting object: "+elemId);
            return;
        }
        
        AbstractElement elem = mg.getMapObjectsByID(elemId)[0];

        RemoveObjectEdit edit = new RemoveObjectEdit(elem);
        edit.redo();

        if (pivot == null)
            pivot = getPivot();
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED);
        mce.setSourceMap(pivot);
        mce.setChangedObject(draggedObject);
        pivot.warnChangeListeners(mce);

        manager.addEdit(edit);
    }

    protected void editElement(String elemId) {
        AbstractElement[] elements = mg.getMapObjectsByID(elemId);
        if (elements.length == 0)
            return;

        AbstractElement element = elements[0];
        String oldXml = element.asXML();
        element.showParametersDialog(getConsole(), getTransNames(), element.getParentMap(), true);
        if (!element.isUserCancel()) {
            mg.updateObjectIds();
            manager.addEdit(new ObjectPropertiesEdit(element, oldXml));
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            pivot = getPivot();
            mce.setSourceMap(pivot);
            mce.setChangedObject(draggedObject);
            pivot.warnChangeListeners(mce);            
        }
    }

    private String[] getTransNames() {
        String transNames[];
        try {
            Vector<TransponderElement> vector = mg.getAllObjectsOfType(TransponderElement.class);
            transNames = new String[vector.size()];
            int i = 0;
            for (TransponderElement transponderElement : vector) {
                transNames[i] = transponderElement.getIdentification();
                i++;
            }
        }
        catch (NullPointerException e) {
            // NeptusLog.pub().warn("I cannot find local trans for main vehicle");
            transNames = new String[0];
        }
        return transNames;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.mouseClicked(event, source);
            return;
        }
        // adapter.mouseClicked(event, source);

        mousePoint = event.getPoint();
        testMouseIntersections();
        final LocationType loc = source.getRealWorldLocation(mousePoint);

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            if (!intersectedObjects.isEmpty()) {
                for (final AbstractElement elem : intersectedObjects) {
                    final String elemId = elem.getId();
                    JMenu menu = new JMenu(elem.getId() + " [" + I18n.text(elem.getType()) + "]");

                    menu.add(I18n.text("Properties")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editElement(elemId);                            
                        }
                    });

                    if (renderer != null) {
                        menu.add(I18n.text("Center in")).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                renderer.focusLocation(elem.getCenterLocation());
                            }
                        });
                    }

                    menu.add(I18n.text("Remove")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeElement(elemId);
                        }
                    });
                    popup.add(menu);
                }
                popup.addSeparator();
            }

            JMenu add = new JMenu(I18n.text("Add..."));
            for (AbstractElement elem : MapType.getMapElements()) {
                if (ignoreAdditionOfTransponders && elem.getClass() == TransponderElement.class)
                    continue;
                else if (ignoreAdditionOfTransponders && elem.getClass() == Model3DElement.class)
                    continue;
                
                try {
                    final AbstractElement el = elem;
                    MapType m = null;
                    for (MapType mt : mg.getMaps())
                        if (mt.getHref() != null && mt.getHref().length() > 0)
                            m = mt;

                    final MapType pivot = m != null ? m : mg.getMaps()[0];
                    add.add(I18n.text(el.getType())).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                AbstractElement newElem = el.getClass().getConstructor(MapGroup.class, MapType.class)
                                        .newInstance(mg, pivot);
                                newElem.setCenterLocation(loc);
                                
                                Vector<String> objNames = new Vector<>();
                                for (AbstractElement el : mg.getAllObjects())
                                    objNames.add(el.getId());
                                newElem.showParametersDialog(MapEditor.this, objNames.toArray(new String[0]), pivot, true);
                                
                                if (!newElem.userCancel) {
                                    pivot.addObject(newElem);

                                    MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
                                    mce.setSourceMap(pivot);
                                    mce.setChangedObject(draggedObject);
                                    pivot.warnChangeListeners(mce);

                                    AddObjectEdit edit = new AddObjectEdit(newElem);
                                    manager.addEdit(edit);
                                }
                            }
                            catch (Exception ex) {
                                ex.printStackTrace();
                                NeptusLog.pub().error(ex);
                            }
                        }
                    });
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            add.addSeparator();

            JMenuItem addWorldFile = add.add(I18n.text("Image from World File"));
            addWorldFile.setToolTipText(I18n.text("Will position the image in currently visible UTM zone."));
            addWorldFile.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = GuiUtils.getFileChooser(ConfigFetch.getUserHomeFolder(), 
                            I18n.text("Images acompanied by world file"), "png", "jpg", "jpeg", "bmp", "tif", "tiff");
                    int op = chooser.showOpenDialog(getConsole());
                    if (op == JFileChooser.APPROVE_OPTION) {
                        File choice = chooser.getSelectedFile();
                        String extension = FileUtil.getFileExtension(choice);
                        try {
                            String filename = choice.getCanonicalPath();

                            // typical extension modification
                            File file = new File(filename.substring(0, filename.length() - extension.length())
                                    + extension.charAt(0) + extension.charAt(2) + 'w');

                            if (!file.canRead())
                                file = new File(filename + 'w');

                            if (file.canRead()) {
                                MapType m = null;
                                for (MapType mt : mg.getMaps())
                                    if (mt.getHref() != null && mt.getHref().length() > 0)
                                        m = mt;

                                final MapType pivot = m != null ? m : mg.getMaps()[0];

                                ImageElement el = new ImageElement(choice, file, renderer.getCenter());
                                el.setMapGroup(mg);
                                el.showParametersDialog(MapEditor.this, pivot.getObjectIds(), pivot, true);

                                if (!el.userCancel) {
                                    pivot.addObject(el);

                                    MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
                                    mce.setSourceMap(pivot);
                                    mce.setChangedObject(draggedObject);
                                    pivot.warnChangeListeners(mce);

                                    AddObjectEdit edit = new AddObjectEdit(el);
                                    manager.addEdit(edit);
                                }
                            }
                            else {
                                throw new Exception(I18n.text("Could not find world image file for given image"));
                            }
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(getConsole(), ex);
                            ex.printStackTrace();
                        }
                    }
                }
            });
            
            JMenuItem addGeotiff = add.add(I18n.text("GeoTIFF overlay"));
            addGeotiff.setToolTipText(I18n.text("Add GeoTIFF ground overlay."));
            addGeotiff.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = GuiUtils.getFileChooser(ConfigFetch.getUserHomeFolder(), 
                            I18n.text("GeoTIFF files"), "tiff", "tif");
                    int op = chooser.showOpenDialog(getConsole());
                    if (op == JFileChooser.APPROVE_OPTION) {
                        try {
                            MapType m = null;
                            for (MapType mt : mg.getMaps())
                                if (mt.getHref() != null && mt.getHref().length() > 0)
                                    m = mt;

                            final MapType pivot = m != null ? m : mg.getMaps()[0];
                            
                            GdalDataSet tiff = new GdalDataSet(chooser.getSelectedFile());

                            ProgressMonitor pm = new ProgressMonitor(getConsole(),
                                    I18n.text("GeoTIFF"), I18n.text("Processing GeoTIFF."), 0, 100);
                            pm.setMillisToDecideToPopup(0);
                            pm.setMillisToPopup(0);
                            pm.setProgress(25);
                            
                            // TIFF usually have very big files, so let us load in background
                            SwingWorker<ImageElement, Void> worker = new SwingWorker<ImageElement, Void>() {
                                @Override
                                protected ImageElement doInBackground() throws Exception {
                                    ImageElement el = tiff.asImageElement(new File(ConfigFetch.getNeptusTmpDir()));
                                    return el;
                                }
                                @Override
                                protected void done() {
                                    try {
                                        boolean isCanceled = pm.isCanceled();
                                        pm.close();
                                        if (isCanceled) {
                                            NeptusLog.pub().warn("Adding of GeoTIFF " + chooser.getSelectedFile()
                                                    + " was canceled by the user.");
                                            return;
                                        }
                                        
                                        ImageElement el = get();
                                        if (el.getCenterLocation().isLocationEqual(new LocationType())) {
                                            GuiUtils.errorMessage(MapEditor.this.getConsole(), I18n.text("Add GeoTIFF ground overlay."),
                                                    I18n.text("Unable to get geographic location for overlay."));
                                        }
                                        
                                        el.setMapGroup(mg);
                                        el.showParametersDialog(MapEditor.this.getConsole(), pivot.getObjectIds(), pivot, true);

                                        if (!el.userCancel) {
                                            pivot.addObject(el);

                                            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
                                            mce.setSourceMap(pivot);
                                            mce.setChangedObject(draggedObject);
                                            pivot.warnChangeListeners(mce);

                                            AddObjectEdit edit = new AddObjectEdit(el);
                                            manager.addEdit(edit);
                                        }
                                    }
                                    catch (Exception ex) {
                                        GuiUtils.errorMessage(MapEditor.this.getConsole(), I18n.text("Add GeoTIFF ground overlay."),
                                                I18n.text("Image format not understood: "+ex.getMessage()));
                                        ex.printStackTrace();                                
                                    }
                                }
                            };
                            worker.execute();
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().error(ex);
                            GuiUtils.errorMessage(getConsole(), ex);
                        }
                    }
                }
            });

            popup.add(add);

            JMenuItem item = new JMenuItem(I18n.text("Copy Location"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    CoordinateUtil.copyToClipboard(loc);
                }
            });
            item.setIcon(ImageUtils.getIcon("images/menus/editcopy.png"));
            popup.add(item);

            JMenu editElem = new JMenu(I18n.text("Properties..."));
            editElem.setEnabled(false);
            JMenu removeElem = new JMenu(I18n.text("Remove..."));
            removeElem.setEnabled(false);
            JMenu centerElem = new JMenu(I18n.text("Center in..."));
            centerElem.setEnabled(false);
            for (final AbstractElement elem : mg.getAllObjects()) {
                if (!intersectedObjects.contains(elem)) {
                    if (!editElem.isEnabled()) {
                        editElem.setEnabled(true);
                        removeElem.setEnabled(true);
                        if (renderer != null)
                            centerElem.setEnabled(true);
                    }

                    editElem.add(elem.getId() + " [" + I18n.text(elem.getType()) + "]").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editElement(elem.getId());
                        }
                    });
                    removeElem.add(elem.getId() + " [" + I18n.text(elem.getType()) + "]").addActionListener(
                            new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeElement(elem.getId());
                        }
                    });
                    if (renderer != null) {
                        centerElem.add(elem.getId() + " [" + I18n.text(elem.getType()) + "]").addActionListener(
                                new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        renderer.focusLocation(elem.getCenterLocation());
                                    }
                                });
                    }
                }
            }
            // renderer.focusLocation(location);
            MenuScroller.setScrollerFor(editElem, MapEditor.this, 150, 0, 0);
            MenuScroller.setScrollerFor(removeElem, MapEditor.this, 150, 0, 0);
            MenuScroller.setScrollerFor(centerElem, MapEditor.this, 150, 0, 0);
            popup.addSeparator();
            popup.add(editElem);
            popup.add(centerElem);
            popup.add(removeElem);
            popup.addSeparator();

            JMenuItem undo = new JMenuItem(manager.getUndoPresentationName());
            undo.setEnabled(manager.canUndo());
            if (manager.canUndo())
                undo.setIcon(ImageUtils.getIcon("pt/lsts/neptus/plugins/map/undo.png"));
            else
                undo.setIcon(ImageUtils.getIcon("pt/lsts/neptus/plugins/map/undo_disabled.png"));

            undo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    manager.undo();
                }
            });
            popup.add(undo);

            JMenuItem redo = new JMenuItem(manager.getRedoPresentationName());
            redo.setEnabled(manager.canRedo());
            if (manager.canRedo())
                redo.setIcon(ImageUtils.getIcon("pt/lsts/neptus/plugins/map/redo.png"));
            else
                redo.setIcon(ImageUtils.getIcon("pt/lsts/neptus/plugins/map/redo_disabled.png"));
            redo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    manager.redo();
                }
            });
            popup.add(redo);

            popup.show(renderer, event.getX(), event.getY());
        }
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.mousePressed(event, source);
            return;
        }

        if (event.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        testMouseIntersections();

        if (!intersectedObjects.isEmpty() && event.getClickCount() > 1) {
            draggedObject = intersectedObjects.lastElement();
            orignalXML = draggedObject.asXML();
            originalObjLocation = new LocationType(draggedObject.getCenterLocation());
            Point2D center = source.getScreenPosition(originalObjLocation);
            Point2D clicked = event.getPoint();

            dragOffsets = new Point2D.Double(clicked.getX() - center.getX(), clicked.getY() - center.getY());

            objectMoved = false;
        }
        else {
            adapter.mousePressed(event, source);
        }

        mousePoint = event.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.mouseDragged(event, source);
            return;
        }

        if (draggedObject != null) {
            if (event.isShiftDown() && draggedObject instanceof RotatableElement) {
                if (objectMoved)
                    mouseReleased(event, source);

                originalYaw = ((RotatableElement) draggedObject).getYaw();

                ((RotatableElement) draggedObject).rotateLeft((event.getPoint().getY() - mousePoint.getY()));
                objectRotated = true;
                mousePoint = event.getPoint();
                return;
            }
            LocationType oldLoc = new LocationType(draggedObject.getCenterLocation());

            if (objectRotated)
                mouseReleased(event, source);
            
            LocationType newLoc = source.getRealWorldLocation(event.getPoint());
            newLoc.translateInPixel(-dragOffsets.getX(), -dragOffsets.getY(), source.getLevelOfDetail());
            newLoc.setDepth(oldLoc.getDepth());
            draggedObject.setCenterLocation(newLoc);
            
            objectMoved = true;
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            mce.setSourceMap(draggedObject.getParentMap());
            mce.setChangedObject(draggedObject);
            mce.setMapGroup(draggedObject.getParentMap().getMapGroup());
            draggedObject.getParentMap().warnChangeListeners(mce);
        }
        else {
            adapter.mouseDragged(event, source);
        }

        mousePoint = event.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.mouseMoved(event, source);
            return;
        }

        setCursor(Cursor.getDefaultCursor());
        mousePoint = event.getPoint();
        adapter.mouseMoved(event, source);
    }
    
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.mouseReleased(event, source);
            return;
        }
        adapter.mouseReleased(event, source);

        if (event.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        if (draggedObject != null) {
            if (pivot == null)
                pivot = getPivot();
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            mce.setSourceMap(pivot);
            mce.setChangedObject(draggedObject);
            pivot.warnChangeListeners(mce);

            ObjectPropertiesEdit edit = new ObjectPropertiesEdit(draggedObject, orignalXML);
            manager.addEdit(edit);
        }

        objectMoved = false;
        objectRotated = false;
        originalYaw = 0;

        draggedObject = null;
        originalObjLocation = null;
        mousePoint = event.getPoint();
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.keyPressed(event, source);
            return;
        }
        
        if (event.getKeyCode() == KeyEvent.VK_Z && event.isControlDown()) {
            undo.doClick(20);
            event.consume();
        }
        else if (event.getKeyCode() == KeyEvent.VK_Y && event.isControlDown()) {
            redo.doClick(20);
            event.consume();
        }

        adapter.keyPressed(event, source);
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.keyReleased(event, source);
            return;
        }

        adapter.keyReleased(event, source);
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.keyTyped(event, source);
            return;
        }
        
        adapter.keyTyped(event, source);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);
        this.mg = MapGroup.getMapGroupInstance(getConsole().getMission());

        if (mode) {
            source.setBorder(new LineBorder(Color.orange.darker(), 3));
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                switch (toolbarLocation) {
                    case Left:
                        c.add(toolbar, BorderLayout.WEST);
                        toolbar.setOrientation(JToolBar.VERTICAL);
                        break;
                    case Right:
                        c.add(toolbar, BorderLayout.EAST);
                        toolbar.setOrientation(JToolBar.VERTICAL);
                        break;
                    case Top:
                        c.add(toolbar, BorderLayout.NORTH);
                        toolbar.setOrientation(JToolBar.HORIZONTAL);
                        break;
                    case Bottom:
                        c.add(toolbar, BorderLayout.SOUTH);
                        toolbar.setOrientation(JToolBar.HORIZONTAL);
                        break;
                }
                c.invalidate();
                c.validate();
            }

        }
        else {
            Container parent = toolbar.getParent();
            
            try {
                parent.remove(toolbar);
            }
            catch (Exception e) {
                NeptusLog.pub().error(
                        "Error removing toolbar of " + MapEditor.class.getSimpleName() + " from "
                                + MapPanel.class.getSimpleName(), e);
            }
            if (parent != null) {
                parent.invalidate();
                parent.validate();
            }
            
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    LocationType oldCenter = renderer.getCenter();
                    getConsole().getMission().save(true);
                    getConsole().warnMissionListeners();
                    renderer.setCenter(oldCenter);
                    return null;
                }
            };
            worker.execute();
            source.setBorder(new EmptyBorder(0, 0, 0, 0));
            if (toolbar.getParent() != null)
                toolbar.getParent().remove(toolbar);
        }
    }
        
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);        
    }

    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
        this.associatedSwitch = tswitch;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        adapter.paintInteraction(g, source);

        if (draggedObject != null && mousePoint != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(mousePoint.x - handIcon.getImage().getWidth(null) / 2, 
                    mousePoint.y - handIcon.getImage().getHeight(null) / 2);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.drawImage(handIcon.getImage(), 0, 0, handIcon.getImage().getWidth(null),
                    handIcon.getImage().getHeight(null), 0, 0, handIcon.getImage().getWidth(null),
                    handIcon.getImage().getHeight(null), null);
            g2.dispose();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }
}
