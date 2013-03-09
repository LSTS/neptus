/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Nov 10, 2011
 */
package pt.up.fe.dceg.neptus.plugins.map;

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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Collections;
import java.util.Timer;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.undo.UndoManager;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.gui.MenuScroller;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.map.edit.AddObjectEdit;
import pt.up.fe.dceg.neptus.plugins.map.edit.ObjectPropertiesEdit;
import pt.up.fe.dceg.neptus.plugins.map.edit.RemoveObjectEdit;
import pt.up.fe.dceg.neptus.plugins.map.interactions.Box2DInteraction;
import pt.up.fe.dceg.neptus.plugins.map.interactions.DrawPathInteraction;
import pt.up.fe.dceg.neptus.plugins.map.interactions.MineDangerAreaInteraction;
import pt.up.fe.dceg.neptus.plugins.map.interactions.PolygonInteraction;
import pt.up.fe.dceg.neptus.plugins.map.interactions.QRouteInteraction;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.CylinderElement;
import pt.up.fe.dceg.neptus.types.map.EllipsoidElement;
import pt.up.fe.dceg.neptus.types.map.ImageElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.MineDangerAreaElement;
import pt.up.fe.dceg.neptus.types.map.Model3DElement;
import pt.up.fe.dceg.neptus.types.map.ParallelepipedElement;
import pt.up.fe.dceg.neptus.types.map.QRouteElement;
import pt.up.fe.dceg.neptus.types.map.RotatableElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 * @author pdias
 */
@PluginDescription(author = "José Pinto, Paulo Dias", name = "Map Editor", icon = "pt/up/fe/dceg/neptus/plugins/map/map.png", version = "1.5", category = CATEGORY.INTERFACE)
@LayerPriority(priority=90)
public class MapEditor extends SimpleSubPanel implements StateRendererInteraction, Renderer2DPainter, MissionChangeListener, ConfigurationListener {

    private static final long serialVersionUID = 1L;
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

    public enum ControlsLocation {Left, Right, Top, Bottom};

    @NeptusProperty(name="Toolbar location")
    public ControlsLocation toolbarLocation = ControlsLocation.Right;

    protected Vector<AbstractElement> elements = null;
    
    public final Vector<AbstractElement> getElements() {
        if (elements == null) {
            elements = new Vector<AbstractElement>();
            elements.add(new MarkElement());
            elements.add(new TransponderElement());
            elements.add(new ParallelepipedElement());
            elements.add(new CylinderElement());
            elements.add(new EllipsoidElement());
            elements.add(new Model3DElement());
            elements.add(new ImageElement());
            elements.add(new MineDangerAreaElement(null, null));
            elements.add(new QRouteElement(null, null));
        }
        
        return elements;
    }
     
    
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
                updateUndoRedoActions();
                return super.addEdit(anEdit);                
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
            if (c instanceof ToolbarSwitch && ((ToolbarSwitch)c).isSelected()) {
                if (!c.equals(source) )
                    ((ToolbarSwitch)c).doClick();
            }
        }
    }

    protected MapType getPivot() {
        MapType m = null;
        for (MapType mt : mg.getMaps())
            if (mt.getHref() != null && mt.getHref().length() > 0)
                m = mt;
        return m != null ? m : mg.getMaps()[0];
    }

    protected JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);

        toolbar.setBackground(new Color(255, 200, 200));
        toolbar.setOpaque(true);
        final ToolbarSwitch freehand = new ToolbarSwitch(
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/interactions/fh.png"), 
                I18n.text("Add Free-hand drawing"), "fh");
        freehand.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch)e.getSource()).isSelected()) {
                    disableAllInteractionsBut(freehand);
                    currentInteraction = new DrawPathInteraction(getPivot(), manager, console);
                    currentInteraction.setAssociatedSwitch(freehand);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        freehand.setToolTipText( I18n.text("Add freehand drawing"));
        toolbar.add(freehand);

        final  ToolbarSwitch fp = new ToolbarSwitch(
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/interactions/poly.png"), 
                I18n.text("Add filled polygon"), "fp");

        fp.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch)e.getSource()).isSelected()) {
                    disableAllInteractionsBut(fp);
                    currentInteraction = new PolygonInteraction(getPivot(), manager, true, console);
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

        final  ToolbarSwitch up = new ToolbarSwitch(
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/interactions/poly2.png"), 
                I18n.text("Add unfilled polygon"), "fp");

        up.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch)e.getSource()).isSelected()) {
                    disableAllInteractionsBut(up);
                    currentInteraction = new PolygonInteraction(getPivot(), manager, false, console);
                    currentInteraction.setAssociatedSwitch(up);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        up.setToolTipText( I18n.text("Add polygon (unfilled)"));
        toolbar.add(up);


        final  ToolbarSwitch mda = new ToolbarSwitch(
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/interactions/mda.png"), 
                I18n.text("Add mine danger area"), "mda");

        mda.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch)e.getSource()).isSelected()) {
                    disableAllInteractionsBut(mda);
                    currentInteraction = new MineDangerAreaInteraction(getPivot(), manager, console);
                    currentInteraction.setAssociatedSwitch(mda);
                    currentInteraction.setActive(true, renderer);
                }
                else {
                    currentInteraction.setActive(false, renderer);
                    currentInteraction = null;
                }
            }
        });
        mda.setToolTipText( I18n.text("Add mine danger area"));
        toolbar.add(mda);

        final  ToolbarSwitch qr = new ToolbarSwitch(
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/interactions/qr.png"), 
                I18n.text("Add QRoute"), "qr");

        qr.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch)e.getSource()).isSelected()) {
                    disableAllInteractionsBut(qr);
                    currentInteraction = new QRouteInteraction(getPivot(), manager, console);
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

        final  ToolbarSwitch b2d = new ToolbarSwitch(
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/interactions/b2d.png"), 
                I18n.text("Add Box2D"), "b2d");

        b2d.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch)e.getSource()).isSelected()) {
                    disableAllInteractionsBut(b2d);
                    currentInteraction = new Box2DInteraction(getPivot(), manager, console);
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

        undo = new ToolbarButton(
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/undo.png"), 
                I18n.text("Undo"), "undo");

        undo.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.undo();
            }
        });
        toolbar.add(undo);
        undo.setEnabled(manager.canUndo());

        redo = new ToolbarButton(
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/redo.png"), 
                I18n.text("Redo"), "redo");

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
     * @see pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        this.renderer = renderer;
    }

    /**
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#getIconImage()
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
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        return true;
    }

    protected void removeElement(String elemId) {
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
        element.showParametersDialog(getConsole(), null, element.getParentMap(), true);
        if (!element.isUserCancel()) {
            manager.addEdit(new ObjectPropertiesEdit(element, oldXml));

            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            mce.setSourceMap(pivot);
            mce.setChangedObject(draggedObject);
            pivot.warnChangeListeners(mce);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (currentInteraction != null) {
            currentInteraction.mouseClicked(event, source);
            return;
        }
        //adapter.mouseClicked(event, source);

        mousePoint = event.getPoint();
        testMouseIntersections();
        final LocationType loc = source.getRealWorldLocation(mousePoint);

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            if (!intersectedObjects.isEmpty()) {
                for (final AbstractElement elem : intersectedObjects) {
                    final String elemId = elem.getId();
                    JMenu menu = new JMenu(elemId + " [" + I18n.text(elem.getType()) + "]");

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

                    menu.add("Remove").addActionListener(new ActionListener() {                    
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
            for (AbstractElement elem : getElements()) {
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
                                newElem.showParametersDialog(MapEditor.this, pivot.getObjectNames(), pivot, true);

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

            add.add(I18n.text("Image from World File")).addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("Images acompanied by world file"), new String[] {"png", "jpg", "bmp", "tif"}));
                    int op = chooser.showOpenDialog(getConsole());
                    if (op == JFileChooser.APPROVE_OPTION) {
                        File choice = chooser.getSelectedFile();
                        String extension = FileUtil.getFileExtension(choice);
                        try {
                            String filename = choice.getCanonicalPath();

                            // typical extension modification
                            File file = new File(filename.substring(0, filename.length()-extension.length())+extension.charAt(0)+extension.charAt(2)+'w');

                            if (!file.canRead())
                                file = new File(filename+'w');

                            if (file.canRead()) {
                                MapType m = null;
                                for (MapType mt : mg.getMaps())
                                    if (mt.getHref() != null && mt.getHref().length() > 0)
                                        m = mt;
                                
                                final MapType pivot = m != null ? m : mg.getMaps()[0];
                                
                                
                                ImageElement el = new ImageElement(choice, file);
                                el.setMapGroup(mg);
                                el.showParametersDialog(MapEditor.this, pivot.getObjectNames(), pivot, true);
                                
                                
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

            popup.add(add);

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

                    editElem.add(elem.getId() + " [" + elem.getType() + "]").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editElement(elem.getId());
                        }
                    });
                    removeElem.add(elem.getId() + " [" + elem.getType() + "]").addActionListener(new ActionListener() {                    
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeElement(elem.getId());
                        }
                    });
                    if (renderer != null) {
                        centerElem.add(elem.getId() + " [" + elem.getType() + "]").addActionListener(new ActionListener() {                    
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                renderer.focusLocation(elem.getCenterLocation());
                            }
                        });
                    }
                }
            }
            //renderer.focusLocation(location);
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
                undo.setIcon(ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/undo.png"));
            else
                undo.setIcon(ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/undo_disabled.png"));

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
                redo.setIcon(ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/redo.png"));
            else
                redo.setIcon(ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/map/redo_disabled.png"));
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

        adapter.mousePressed(event, source);

        if (event.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        testMouseIntersections();

        if (!intersectedObjects.isEmpty()) {
            draggedObject = intersectedObjects.lastElement();
            orignalXML = draggedObject.asXML();
            originalObjLocation = new LocationType(draggedObject.getCenterLocation());
            Point2D center = source.getScreenPosition(originalObjLocation);
            Point2D clicked = event.getPoint();

            dragOffsets = new Point2D.Double(clicked.getX()-center.getX(), clicked.getY()-center.getY());

            objectMoved = false;
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

                originalYaw = ((RotatableElement)draggedObject).getYaw();

                ((RotatableElement)draggedObject).rotateLeft((event.getPoint().getY()-mousePoint.getY()));
                objectRotated = true;
                mousePoint = event.getPoint();
                return;
            }

            if (draggedObject instanceof MarkElement || draggedObject instanceof TransponderElement) 
                draggedObject.setCenterLocation(source.getRealWorldLocation(event.getPoint()));            
            else {
                if (objectRotated)
                    mouseReleased(event, source);
                LocationType newLoc =  source.getRealWorldLocation(event.getPoint());
                newLoc.translateInPixel(-dragOffsets.getX(), -dragOffsets.getY(), source.getLevelOfDetail());
                draggedObject.setCenterLocation(newLoc);
            }

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
            System.out.println(edit);
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
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    getConsole().getMission().save(true);
                    getConsole().warnMissionListeners();
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
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
        this.associatedSwitch = tswitch;
    }


    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }


    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
