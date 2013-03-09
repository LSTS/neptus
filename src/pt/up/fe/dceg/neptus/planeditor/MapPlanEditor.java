/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZePinto
 * 200?/??/??
 */
package pt.up.fe.dceg.neptus.planeditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.MenuScroller;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.VehicleSelectionDialog;
import pt.up.fe.dceg.neptus.gui.ZValueSelector;
import pt.up.fe.dceg.neptus.gui.tablelayout.TableLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverFactory;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.renderer2d.CustomInteractionSupport;
import pt.up.fe.dceg.neptus.renderer2d.Renderer;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.PlanElement;
import pt.up.fe.dceg.neptus.types.map.PlanUtil;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.TransitionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.propertysheet.DefaultProperty;

/**
 * 
 * @author ZP
 */
@SuppressWarnings("serial")
public class MapPlanEditor extends JPanel implements MouseListener, MouseMotionListener, Renderer, IMapPopup,
CustomInteractionSupport {

    public static final boolean DEBUG = false;

    private String defaultCondition = "ManeuverIsDone";
    // private String defaultAction = "";

    private MissionType mission = null;
    private PlanType plan = null;
    private StateRenderer2D renderer = null;
    private PlanElement planElem;
    private ManeuverFactory mf = null;
    private MapGroup mapGroup = null;
    private Maneuver selectedManeuver = null;
    private Point2D lastDragPoint = null;
    private IndividualPlanEditor planEditor = null;
    private int lastViewMode = Renderer.TRANSLATION;
    private Vector<IEditorMenuExtension> menuExtensions = new Vector<IEditorMenuExtension>();

    boolean editable = true;

    private Vector<String> takenNames = new Vector<String>();

    /**
     * Class constructor. Creates a new editor for the given plan.
     * 
     * @see #isVehicleSupported(String)
     * @param plan The plan to be edited
     */
    public MapPlanEditor(PlanType plan) {

        this.plan = plan;

        if (plan != null) {
            this.mission = plan.getMissionType();
            parsePlan();
        }

        initialize();
    }

    public void setIndividualPlanEditor(IndividualPlanEditor editor) {
        this.planEditor = editor;
    }

    /**
     * Verifies if a given vehicle supports this kind of editor
     * 
     * @param vehicleID The vehicle's ID
     * @return <b>true</b> if the vehicle is supported or <b>false</b> otherwise
     */
    public static boolean isVehicleSupported(String vehicleID) {
        VehicleType vehicle = VehiclesHolder.getVehicleById(vehicleID);

        if (vehicle == null)
            return false;

        LinkedHashMap<String, String> maneuvers = vehicle.getFeasibleManeuvers();

        for (String maneuver : maneuvers.keySet()) {
            Maneuver man = ManeuverFactory.createManeuver(maneuver, maneuvers.get(maneuver));
            if (man != null) {
                return true;
            }
        }

        return false;
    }

    private void initialize() {
        ConfigFetch.mark("MapPlanEditor START");
        setLayout(new TableLayout(new double[] { 0.0, TableLayout.FILL }, new double[] { 0.0, TableLayout.FILL }));
        mapGroup = MapGroup.getMapGroupInstance(mission);
        ConfigFetch.mark("R2D START");
        renderer = new StateRenderer2D(mapGroup);
        ConfigFetch.benchmark("R2D START");
        renderer.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        planElem = new PlanElement(mapGroup, new MapType());
        planElem.setRenderer(renderer);
        planElem.setPlan(plan);
        planElem.setTransp2d(1.0);

        renderer.addPostRenderPainter(planElem, "Plan Painter");

        renderer.addMouseListener(this);
        renderer.addMouseMotionListener(this);

        this.add(renderer, "1,1");
        ConfigFetch.benchmark("MapPlanEditor START");
    }

    public void setMission(MissionType mission) {
        if (mission == null)
            return;

        mapGroup = MapGroup.getMapGroupInstance(mission);
        renderer.setMapGroup(mapGroup);
        this.mission = mission;
    }

    public void setPlan(PlanType plan) {
        this.plan = plan;
        if (plan == null) {
            renderer.removePostRenderPainter(planElem);
            return;
        }
        if (plan.getMissionType() != getMission())
            setMission(plan.getMissionType());

        renderer.removePostRenderPainter(planElem);

        parsePlan();
        planElem = new PlanElement(mapGroup, new MapType());
        planElem.setTransp2d(1.0);
        renderer.addPostRenderPainter(planElem, "Plan Layer");
        planElem.setRenderer(renderer);
        planElem.setPlan(plan);

        renderer.repaint();
    }

    private void parsePlan() {

        this.mf = plan.getVehicleType().getManeuverFactory();

        for (Maneuver man : plan.getGraph().getAllManeuvers()) {
            takenNames.add(man.getId());
        }
    }

    public void mouseClicked(MouseEvent e) {

        if (renderer.getActiveInteraction() != null)
            return;

        final Point2D mousePoint = e.getPoint();
        final LocationType loc = renderer.getRealWorldLocation(mousePoint);

        if (DEBUG) {
            Maneuver man = planElem.iterateManeuverUnder(e.getPoint());
            if (man != null)
                System.err.println(man.getId());
        }

        if (e.getClickCount() == 2) {
            planElem.iterateManeuverBack(e.getPoint());
            Maneuver man = planElem.iterateManeuverBack(e.getPoint());
            if (man != null) {
                boolean wasInitialManeuver = man.isInitialManeuver();

                PropertiesEditor.editProperties(man, SwingUtilities.getWindowAncestor(this), isEditable());

                if (man.isInitialManeuver())
                    plan.getGraph().setInitialManeuver(man.getId());
                else {
                    if (wasInitialManeuver) {
                        man.setInitialManeuver(true);
                        GuiUtils.infoMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Set Properties"),
                                I18n.text("To change the initial maneuver, please set another maneuver as the initial"));
                    }
                }
                planElem.recalculateManeuverPositions(renderer);
                renderer.repaint();
                return;
            }
        }

        if (e.isControlDown() && e.getButton() == MouseEvent.BUTTON1) {
            Maneuver m = plan.getGraph().getLastManeuver();
            addManeuverAtEnd(e.getPoint(), m.getType());
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            AbstractAction copy = new AbstractAction(I18n.text("Copy location")) {
                public void actionPerformed(ActionEvent e) {
                    CoordinateUtil.copyToClipboard(renderer.getRealWorldLocation(mousePoint));
                }
            };
            copy.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/editcopy.png")));
            popup.add(copy);

            JMenu viewMode = new JMenu(I18n.text("View Mode"));

            AbstractAction transMode = new AbstractAction(I18n.text("Translation/Move")) {
                public void actionPerformed(ActionEvent e) {
                    lastViewMode = Renderer.TRANSLATION;
                    renderer.setViewMode(Renderer.TRANSLATION);
                }
            };
            transMode.putValue(AbstractAction.SMALL_ICON,
                    new ImageIcon(ImageUtils.getScaledImage("images/buttons/translate_btn.png", 16, 16)));
            transMode.putValue(AbstractAction.SELECTED_KEY, lastViewMode == Renderer.TRANSLATION);
            viewMode.add(new JCheckBoxMenuItem(transMode));

            AbstractAction rotateMode = new AbstractAction(I18n.text("Rotate")) {
                public void actionPerformed(ActionEvent e) {
                    lastViewMode = Renderer.ROTATION;
                    renderer.setViewMode(Renderer.ROTATION);
                    renderer.setCursor(StateRenderer2D.rotateCursor);
                }
            };
            rotateMode.putValue(AbstractAction.SMALL_ICON,
                    new ImageIcon(ImageUtils.getScaledImage("images/buttons/rotate_btn.png", 16, 16)));
            rotateMode.putValue(AbstractAction.SELECTED_KEY, lastViewMode == Renderer.ROTATION);
            viewMode.add(new JCheckBoxMenuItem(rotateMode));

            AbstractAction rulerMode = new AbstractAction(I18n.text("Measure/Ruler")) {
                public void actionPerformed(ActionEvent e) {
                    lastViewMode = Renderer.RULER;
                    renderer.setViewMode(Renderer.RULER);
                    renderer.setCursor(StateRenderer2D.crosshairCursor);
                }
            };
            rulerMode.putValue(AbstractAction.SMALL_ICON,
                    new ImageIcon(ImageUtils.getScaledImage("images/buttons/ruler_btn.png", 16, 16)));
            rulerMode.putValue(AbstractAction.SELECTED_KEY, lastViewMode == Renderer.RULER);
            viewMode.add(new JCheckBoxMenuItem(rulerMode));

            popup.add(viewMode);

            JMenuItem item = new JMenuItem(I18n.text("Plan editor help"));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent arg0) {
                    GuiUtils.htmlMessage(
                            ConfigFetch.getSuperParentFrame(),
                            I18n.text("Neptus Plan Editor"),
                            I18n.text("How to create mission plans the easy way"),

                            I18n.text("<html><h1>Neptus Plan Editor</h1><br>"
                                    + "<h2>Adding maneuvers</h2><blockquote>You can add a new maneuver by right-clicking the map and choosing <em>'Add &lt;maneuver&gt;'</em>. The maneuver will automatically be positioned at the clicked location and a transition between the last maneuver in the plan and the new maneuver will be created.<br>Also note that all settings except the location in the new maneuver will be set equal to the last maneuver in the plan. If no maneuver currently exists in the plan, the default settings will be used.</blockquote>"
                                    + "<h2>Relocating maneuvers</h2><blockquote>The added maneuvers can be relocated by dragging them or by setting their location property.<br></blockquote>"
                                    + "<h2>Setting maneuver properties</h2><blockquote>Right click the maneuver to be edited and select <em>'&lt;maneuver&gt; properties'</em></blockquote>"
                                    + "<h2>Removing maneuvers</h2><blockquote>Right click the maneuver and select <em>'remove &lt;maneuver&gt;'</em></blockquote>"
                                    + "<h2>Initial maneuver</h2><blockquote>In any plan there is exactly one special maneuver that is the <b>Initial Maneuver</b>, by default, the first added maneuver is automatically set as initial but you can change it by setting another maneuver as initial (by setting its <em>Initial Maneuver</em> property)</blockquote>"
                                    + "<h2>Adding a maneuver between two existing maneuvers</h2><blockquote>You can intercalate two existing maneuvers by right-clicking one maneuver and selecting <em>'Add &lt;new maneuver&gt; after &lt;clicked maneuver&gt;. The added maneuver will be located in the medium point between the two maneuvers.</blockquote>"
                                    + "<h2>Dragging the entire plan</h2><blockquote>If you wish, you can move all the maneuvers in the plan by keeping pressed the <b>Control</b> key while dragging one maneuver.</blockquote>"
                                    + "<h2>Rotating the plan</h2><blockquote>The plan can be rotated by keeping the <b>Shift</b> key pressed while dragging a maneuver. The dragged maneuver will remain static but the rest of the plan will orbit around it.</blockquote>")

                            );

                }
            });
            item.setIcon(ImageUtils.getIcon("images/menus/info.png"));
            popup.add(item);

            final Maneuver[] mans = planElem.getAllInterceptedManeuvers(e.getPoint());
            if (mans.length == 1) {
                popup.addSeparator();
                final Maneuver man = mans[0];
                AbstractAction props = new AbstractAction(I18n.textf("%maneuver properties",man.getId())) {
                    public void actionPerformed(ActionEvent e) {
                        boolean wasInitialManeuver = man.isInitialManeuver();

                        PropertiesEditor.editProperties(man, SwingUtilities.getWindowAncestor(MapPlanEditor.this),
                                isEditable());

                        if (man.isInitialManeuver())
                            plan.getGraph().setInitialManeuver(man.getId());
                        else {
                            if (wasInitialManeuver) {
                                man.setInitialManeuver(true);
                                GuiUtils.infoMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Set Properties"),
                                        I18n.text("To change the initial maneuver, please set another maneuver as the initial"));
                            }
                        }

                        planElem.recalculateManeuverPositions(renderer);
                        renderer.repaint();
                    }
                };
                props.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/edit.png")));
                popup.add(props);

                if (man instanceof StateRendererInteraction) {
                    AbstractAction editMan = new AbstractAction(I18n.textf("Edit %man in the map", man.getId())) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            renderer.setActiveInteraction((StateRendererInteraction) man);
                        }
                    };
                    popup.add(editMan);
                }

                if (isEditable()) {
                    AbstractAction remove = new AbstractAction(I18n.textf("Remove $maneuver", man.getId())) {
                        public void actionPerformed(ActionEvent e) {
                            removeManeuver(man.getId());
                        }
                    };
                    remove.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getImage("images/menus/editdelete.png")));
                    popup.add(remove);

                    popup.addSeparator();

                    for (String manName : mf.getAvailableManeuversIDs()) {
                        final String name = manName;
                        AbstractAction act = new AbstractAction(I18n.textf("Add %manName after %manId", manName, man.getId()),
                                mf.getManeuverIcon(manName)) {
                            public void actionPerformed(ActionEvent evt) {
                                addManeuverAfter(name, man.getId());
                                planElem.recalculateManeuverPositions(renderer);
                                renderer.repaint();
                            }
                        };
                        // act.putValue(AbstractAction.SMALL_ICON, new
                        // ImageIcon(GuiUtils.getImage("images/menus/new.png")));
                        popup.add(act);
                    }

                    if (man.isInitialManeuver()) {
                        popup.addSeparator();

                        for (String manName : mf.getAvailableManeuversIDs()) {
                            final String manType = manName;
                            AbstractAction act = new AbstractAction(I18n.textf("Add %manName before %manId",manName, man.getId()),
                                    mf.getManeuverIcon(manName)) {
                                public void actionPerformed(ActionEvent evt) {
                                    addManeuverAtBeginning(manType, (Point) mousePoint);
                                    planElem.recalculateManeuverPositions(renderer);
                                    renderer.repaint();
                                }
                            };
                            // act.putValue(AbstractAction.SMALL_ICON, new
                            // ImageIcon(GuiUtils.getImage("images/menus/new.png")));
                            popup.add(act);
                        }
                    }
                }
            }
            else if (mans.length > 1) {
                popup.addSeparator();
                for (Maneuver m : mans) {
                    final Maneuver man = m;
                    JMenu subMenu = new JMenu(man.getId());
                    if (man instanceof LocatedManeuver) {
                        subMenu.setText(man.getId()
                                + " ("
                                + (GuiUtils.getNeptusDecimalFormat(1).format(((LocatedManeuver) man).getManeuverLocation()
                                        .getAllZ())) + " m)");
                    }
                    subMenu.setIcon(new ImageIcon(ImageUtils.getImage("images/menus/plan.png")));

                    AbstractAction props = new AbstractAction(I18n.textf("%maneuver properties", man.getId())) {
                        public void actionPerformed(ActionEvent e) {

                            PropertiesEditor.editProperties(man, SwingUtilities.getWindowAncestor(MapPlanEditor.this),
                                    isEditable());

                            if (man.isInitialManeuver())
                                plan.getGraph().setInitialManeuver(man.getId());

                            planElem.recalculateManeuverPositions(renderer);
                            renderer.repaint();
                        }
                    };
                    props.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getImage("images/menus/edit.png")));
                    subMenu.add(props);

                    if (isEditable()) {
                        AbstractAction remove = new AbstractAction(I18n.textf("Remove %maneuver", man.getId())) {
                            public void actionPerformed(ActionEvent e) {
                                removeManeuver(man.getId());
                            }
                        };
                        remove.putValue(AbstractAction.SMALL_ICON,
                                new ImageIcon(ImageUtils.getImage("images/menus/editdelete.png")));
                        subMenu.add(remove);

                        subMenu.addSeparator();

                        for (String manName : mf.getAvailableManeuversIDs()) {
                            final String name = manName;
                            AbstractAction act = new AbstractAction(I18n.textf("Add %manName after %manId", manName, man.getId()),
                                    mf.getManeuverIcon(manName)) {
                                public void actionPerformed(ActionEvent evt) {
                                    addManeuverAfter(name, man.getId());
                                    planElem.recalculateManeuverPositions(renderer);
                                    renderer.repaint();
                                }
                            };
                            // act.putValue(AbstractAction.SMALL_ICON, new
                            // ImageIcon(GuiUtils.getImage("images/menus/new.png")));
                            subMenu.add(act);
                        }
                    }
                    popup.add(subMenu);
                }
            }
            else {
                if (isEditable()) {
                    popup.addSeparator();
                    JMenu planSettings = new JMenu(I18n.text("Plan Settings"));

                    AbstractAction pDepth = new AbstractAction(I18n.text("Plan depth / altitude...")) {
                        public void actionPerformed(ActionEvent e) {

                            ManeuverLocation loc = new ManeuverLocation();
                            loc.setDepth(0);
                            loc.setZUnits(Z_UNITS.NONE);

                            boolean pressedOk = ZValueSelector.showHeightDepthDialog(MapPlanEditor.this ,loc, I18n.text("Plan depth / altitude"));
                            if (pressedOk)
                                planElem.setPlanZ(loc.getZ(), loc.getZUnits());
                        }
                    };
                    pDepth.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));

                    planSettings.add(pDepth);

                    AbstractAction pVel = new AbstractAction(I18n.text("Plan speed...")) {
                        public void actionPerformed(ActionEvent e) {

                            String[] optionsNew = { "m/s", "RPM", "%" };
                            int choNew = JOptionPane.showOptionDialog(ConfigFetch.getSuperParentFrame(),
                                    I18n.text("Choose one to continue"), I18n.text("Choose"), JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, optionsNew, optionsNew[0]);

                            if (choNew == JOptionPane.CLOSED_OPTION) {
                                return;
                            }
                            String velUnit = optionsNew[choNew];

                            double velocity = 0;
                            boolean validVel = false;
                            while (!validVel) {
                                String res = JOptionPane.showInputDialog(ConfigFetch.getSuperParentFrame(),
                                        I18n.text("Enter new speed")+" (" + velUnit + ")");
                                if (res == null)
                                    return;
                                try {
                                    velocity = Double.parseDouble(res);
                                    validVel = true;
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Set plan speed"),
                                            I18n.text("Speed must be a numeric value"));
                                }
                            }

                            DefaultProperty propVel = new DefaultProperty();
                            propVel.setName("Speed");
                            propVel.setValue(velocity);
                            propVel.setType(Double.class);
                            propVel.setDisplayName(I18n.text("Speed"));
                            planElem.setPlanProperty(propVel);

                            DefaultProperty propVelUnits = new DefaultProperty();
                            propVelUnits.setName("Speed units");
                            propVelUnits.setValue(velUnit);
                            propVelUnits.setType(String.class);
                            propVelUnits.setDisplayName(I18n.text("Speed units"));
                            planElem.setPlanProperty(propVelUnits);

                        }
                    };
                    pVel.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    planSettings.add(pVel);

                    AbstractAction pRadius = new AbstractAction(I18n.text("Radius tolerance...")) {
                        public void actionPerformed(ActionEvent e) {
                            double radius = 0;
                            boolean validRadius = false;
                            while (!validRadius) {
                                String res = JOptionPane.showInputDialog(ConfigFetch.getSuperParentFrame(),
                                        I18n.text("Enter new radius tolerance (meters)"));
                                if (res == null)
                                    return;
                                try {
                                    radius = Double.parseDouble(res);
                                    validRadius = true;
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(),
                                            I18n.text("Set plan radius tolerance"), 
                                            I18n.text("Radius tolerance must be a numeric value"));
                                }
                            }

                            DefaultProperty propVel = new DefaultProperty();
                            propVel.setName("Radius tolerance");
                            propVel.setValue(radius);
                            propVel.setType(Double.class);
                            propVel.setDisplayName(I18n.text("Radius tolerance"));
                            planElem.setPlanProperty(propVel);
                        }
                    };
                    pRadius.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    planSettings.add(pRadius);

                    AbstractAction pMaxTime = new AbstractAction(I18n.text("Maximum time...")) {
                        public void actionPerformed(ActionEvent e) {
                            int maxTime = 0;
                            boolean validTime = false;
                            while (!validTime) {
                                String res = JOptionPane.showInputDialog(ConfigFetch.getSuperParentFrame(),
                                        I18n.text("Enter new maximum time (per maneuver, in seconds)"));
                                if (res == null)
                                    return;
                                try {
                                    maxTime = Integer.parseInt(res);
                                    validTime = true;
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Set maximum time"),
                                            I18n.text("Maximum time must be numeric"));
                                }
                            }

                            DefaultProperty propMaxTime = new DefaultProperty();
                            propMaxTime.setName("Maximum time");
                            propMaxTime.setValue(maxTime);
                            propMaxTime.setType(Integer.class);
                            propMaxTime.setDisplayName(I18n.text("Maximum time"));
                            planElem.setPlanProperty(propMaxTime);
                        }
                    };
                    pMaxTime.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    planSettings.add(pMaxTime);

                    AbstractAction pVehicle = new AbstractAction(I18n.text("Set plan vehicles...")) {
                        public void actionPerformed(ActionEvent e) {
                            String[] vehicles = VehicleSelectionDialog.showSelectionDialog(
                                    SwingUtilities.getWindowAncestor(MapPlanEditor.this),
                                    plan.getVehicles().toArray(new VehicleType[0]));
                            Vector<VehicleType> vts = new Vector<VehicleType>();
                            for (String v : vehicles) {
                                vts.add(VehiclesHolder.getVehicleById(v));
                            }
                            plan.setVehicles(vts);
                        }
                    };
                    pVehicle.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    planSettings.add(pVehicle);

                    planSettings.setIcon(new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    popup.add(planSettings);

                    popup.addSeparator();
                    AbstractAction pTransitions = new AbstractAction(I18n.text("Plan Transitions")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JDialog transitions = new JDialog(SwingUtilities.getWindowAncestor(MapPlanEditor.this),
                                    I18n.textf("Edit '%plan' plan transitions", plan.getId()));
                            transitions.setModalityType(ModalityType.DOCUMENT_MODAL);
                            transitions.getContentPane().add(new PlanTransitionsSimpleEditor(plan));
                            // transitions.pack();
                            transitions.setSize(800, 500);
                            // GuiUtils.centerOnScreen(statistics);
                            GuiUtils.centerParent(transitions, transitions.getOwner());
                            transitions.setVisible(true);
                            parsePlan();
                            warnChanged();
                            renderer.repaint();
                        }
                    };
                    pTransitions.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    popup.add(pTransitions);

                    AbstractAction paTransitions = new AbstractAction(I18n.text("Plan Actions")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PropertiesEditor.editProperties(plan, SwingUtilities.getWindowAncestor(MapPlanEditor.this),
                                    isEditable());
                            parsePlan();
                            warnChanged();
                            renderer.repaint();
                        }
                    };
                    paTransitions.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    popup.add(paTransitions);

                    popup.addSeparator();

                    // AbstractAction pStatistics = new AbstractAction("Edited Plan Statistics") {
                    // public void actionPerformed(ActionEvent e) {
                    // JDialog statistics = new JDialog(SwingUtilities.getWindowAncestor(MapPlanEditor.this));
                    // statistics.getContentPane().add(new JLabel(plan.planStatistics()));
                    // statistics.pack();
                    // //GuiUtils.centerOnScreen(statistics);
                    // statistics.setVisible(true);
                    // }
                    // };
                    // pStatistics.putValue(AbstractAction.SMALL_ICON, new
                    // ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    // popup.add(pStatistics);
                    JMenu pStatistics = PlanUtil.getPlanStatisticsAsJMenu(plan, "Edited Plan Statistics");
                    pStatistics.setIcon(new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    popup.add(pStatistics);

                    popup.addSeparator();

                    for (String manName : mf.getAvailableManeuversIDs()) {
                        AbstractAction act = new AbstractAction(I18n.textf("Add %manName", manName), mf.getManeuverIcon(manName)) {
                            private Point2D mousePos = mousePoint;

                            public void actionPerformed(ActionEvent evt) {
                                String maneuverName = this.getValue(AbstractAction.NAME).toString().substring(4);
                                addManeuverAtEnd((Point) mousePos, maneuverName);
                                planElem.recalculateManeuverPositions(renderer);
                                renderer.repaint();
                            }
                        };
                        // act.putValue(AbstractAction.SMALL_ICON, new
                        // ImageIcon(GuiUtils.getImage("images/menus/new.png")));
                        popup.add(act);
                    }
                }
            }

            for (IEditorMenuExtension extension : menuExtensions) {
                Collection<JMenuItem> items = null;

                try {
                    items = extension.getApplicableItems(loc, this);
                }
                catch (Exception ex) {
                    NeptusLog.pub().error(ex);
                }

                if (items != null && !items.isEmpty()) {
                    popup.addSeparator();
                    for (JMenuItem it : items) {
                        if (it instanceof JMenu)
                            MenuScroller.setScrollerFor((JMenu) it, MapPlanEditor.this, 150, 0, 0);
                        popup.add(it);
                    }
                }
            }

            popup.show(this, (int) mousePoint.getX(), (int) mousePoint.getY());
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (renderer.getActiveInteraction() != null)
            return;

        if (!isEditable())
            return;

        if (lastDragPoint == null)
            selectedManeuver = planElem.iterateManeuverBack(e.getPoint());

        if (selectedManeuver != null && selectedManeuver instanceof LocatedManeuver) {

            if (lastDragPoint != null) {

                double diffX = e.getPoint().getX() - lastDragPoint.getX();
                double diffY = e.getPoint().getY() - lastDragPoint.getY();

                if (e.isControlDown()) {
                    LocationType oldPt = renderer.getRealWorldLocation(lastDragPoint);
                    LocationType newPt = renderer.getRealWorldLocation(e.getPoint());

                    double[] offsets = newPt.getOffsetFrom(oldPt);

                    planElem.translatePlan(offsets[0], offsets[1], 0);
                    lastDragPoint = e.getPoint();
                }
                else {
                    if (e.isShiftDown()) {
                        double ammount = Math.toRadians(lastDragPoint.getY() - e.getPoint().getY());
                        planElem.rotatePlan((LocatedManeuver) selectedManeuver, ammount);
                        lastDragPoint = e.getPoint();
                    }
                    else {
                        Point2D newManPos = planElem.translateManeuverPosition(selectedManeuver.getId(), diffX, diffY);
                        ManeuverLocation loc = ((LocatedManeuver) selectedManeuver).getManeuverLocation();

                        ManeuverLocation center = new ManeuverLocation();
                        center.setLocation(renderer.getRealWorldLocation(newManPos));
                        center.setDepth(loc.getDepth());
                        center.setZUnits(loc.getZUnits());
                        center.setOffsetDown(loc.getOffsetDown());
                        ((LocatedManeuver) selectedManeuver).setManeuverLocation(center);
                        lastDragPoint = newManPos;
                    }
                }
                renderer.setFastRendering(true);
                renderer.repaint();
            }
            else {
                lastDragPoint = e.getPoint();
                renderer.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mouseMoved(MouseEvent e) {

        if (renderer.getActiveInteraction() != null)
            return;

        if (plan == null || selectedManeuver != null)
            return;

        String[] mans = planElem.getManeuversUnder(e.getPoint());
        if (mans.length > 0 && renderer.getViewMode() != Renderer.RULER) {
            renderer.setCursor(Cursor.getDefaultCursor());

            String text = plan.getGraph().getManeuver(mans[0]).getTooltipText();

            for (int i = 1; i < mans.length && i < 3; i++) {
                String tooltip = plan.getGraph().getManeuver(mans[i]).getTooltipText();
                if (tooltip.startsWith("<html>"))
                    tooltip = tooltip.substring(6);
                text = text + "<br><br>" + tooltip;
            }

            if (mans.length >= 4)
                text = text + "<br><b>(...)</b>";
            renderer.setToolTipText(text);
        }
        else {
            switch (renderer.getViewMode()) {
                case Renderer.RULER:
                    renderer.setCursor(StateRenderer2D.crosshairCursor);
                    break;
                case Renderer.TRANSLATION:
                    renderer.setCursor(StateRenderer2D.translateCursor);
                    break;
                case Renderer.ROTATION:
                    renderer.setCursor(StateRenderer2D.rotateCursor);
                    break;
                case Renderer.ZOOM:
                    renderer.setCursor(StateRenderer2D.zoomCursor);
                    break;
                default:
                    renderer.setCursor(Cursor.getDefaultCursor());
                    break;
            }
            renderer.setToolTipText(null);
        }

    }

    public void mousePressed(MouseEvent e) {

        if (renderer.getActiveInteraction() != null)
            return;

        if (e.getButton() == MouseEvent.BUTTON1) {
            selectedManeuver = planElem.iterateManeuverUnder(e.getPoint());
            if (selectedManeuver != null)
                renderer.setViewMode(Renderer.NONE);
        }
    }

    public void mouseReleased(MouseEvent e) {

        if (renderer.getActiveInteraction() != null)
            return;

        lastDragPoint = null;
        if (selectedManeuver != null) {
            planElem.recalculateManeuverPositions(renderer);
            this.selectedManeuver = null;
            warnChanged();
        }
        if(getViewMode() != Renderer.RULER) {
            setViewMode(lastViewMode);
            renderer.setFastRendering(false);
            mouseMoved(e);
        }
    }

    private void removeManeuver(String manID) {
        Maneuver man = plan.getGraph().getManeuver(manID);
        Maneuver next = plan.getGraph().getFollowingManeuver(manID);

        if (next != null)
            plan.getGraph().removeTransition(manID, next.getId());

        for (Maneuver previous : plan.getGraph().getPreviousManeuvers(manID)) {
            plan.getGraph().removeTransition(previous.getId(), manID);

            if (next != null && previous != null)
                plan.getGraph().addTransition(previous.getId(), next.getId(), defaultCondition);
        }
        plan.getGraph().removeManeuver(man);

        planElem.recalculateManeuverPositions(renderer);
        renderer.repaint();

        if (man.isInitialManeuver() && next != null) {
            plan.getGraph().setInitialManeuver(next.getId());
        }
        warnChanged();
    }

    private Maneuver addManeuverAfter(String manType, String manID) {
        Maneuver nextMan = plan.getGraph().getFollowingManeuver(manID);
        Maneuver previousMan = plan.getGraph().getManeuver(manID);
        Maneuver newMan = mf.getManeuver(manType);

        if (newMan == null) {
            GuiUtils.errorMessage(this, I18n.text("Error adding maneuver"), I18n.textf("The maneuver %manType can't be added", manType));
            return null;
        }

        newMan.setId(getNewManeuverName(manType));

        if (newMan instanceof LocatedManeuver)
            ((LocatedManeuver) newMan).getManeuverLocation().setLocation(renderer.getCenter());

        if (newMan instanceof LocatedManeuver && previousMan instanceof LocatedManeuver
                && nextMan instanceof LocatedManeuver) {
            LocationType loc1 = new LocationType(((LocatedManeuver) previousMan).getManeuverLocation());
            LocationType loc2 = new LocationType(((LocatedManeuver) nextMan).getManeuverLocation());

            double offsets[] = loc2.getOffsetFrom(loc1);

            loc1.translatePosition(offsets[0] / 2, offsets[1] / 2, 0);
            loc1.setDepth(loc1.getDepth());

            ((LocatedManeuver) newMan).getManeuverLocation().setLocation(loc1);
        }
        else {
            if (newMan instanceof LocatedManeuver && previousMan instanceof LocatedManeuver) {
                LocationType loc1 = new LocationType(((LocatedManeuver) previousMan).getManeuverLocation());
                loc1.translatePosition(0, 30, 0);
                ((LocatedManeuver) newMan).getManeuverLocation().setLocation(loc1);
            }
            if (newMan instanceof LocatedManeuver && nextMan instanceof LocatedManeuver) {
                LocationType loc1 = new LocationType(((LocatedManeuver) nextMan).getManeuverLocation());
                loc1.translatePosition(0, -30, 0);
                ((LocatedManeuver) newMan).getManeuverLocation().setLocation(loc1);
            }
        }

        if (plan.getGraph().getExitingTransitions(previousMan).size() != 0) {
            for (TransitionType exitingTrans : plan.getGraph().getExitingTransitions(previousMan)) {
                plan.getGraph().removeTransition(exitingTrans.getSourceManeuver(), exitingTrans.getTargetManeuver());
            }
        }

        plan.getGraph().addManeuver(newMan);
        plan.getGraph().addTransition(previousMan.getId(), newMan.getId(), defaultCondition);
        if (nextMan != null) {
            plan.getGraph().removeTransition(previousMan.getId(), nextMan.getId());
            plan.getGraph().addTransition(newMan.getId(), nextMan.getId(), defaultCondition);
        }

        warnChanged();

        return newMan;
    }

    private Maneuver addManeuverAtBeginning(String manType, Point loc) {
        Maneuver man = addManeuverAtEnd(loc, manType);

        for (TransitionType t : plan.getGraph().getIncomingTransitions(man)) {
            plan.getGraph().removeTransition(t.getSourceManeuver(), t.getTargetManeuver());
        }

        String initial = plan.getGraph().getInitialManeuverId();
        plan.getGraph().addTransition(man.getId(), initial, defaultCondition);
        plan.getGraph().setInitialManeuver(man.getId());

        return man;

    }

    private Maneuver addManeuverAtEnd(Point loc, String manType) {

        Maneuver man = mf.getManeuver(manType);

        if (man == null) {
            GuiUtils.errorMessage(this, I18n.text("Error adding maneuver"), I18n.textf("The maneuver %manType can't be added", manType));
            return null;
        }

        man.setId(getNewManeuverName(manType));

        Maneuver lastMan = plan.getGraph().getLastManeuver();

        if (lastMan != null && lastMan != man) {

            if (plan.getGraph().getExitingTransitions(lastMan).size() != 0) {
                for (TransitionType exitingTrans : plan.getGraph().getExitingTransitions(lastMan)) {
                    // System.out.println("removing "+exitingTrans.getSourceManeuver()+" -> "+exitingTrans.getTargetManeuver());

                    plan.getGraph()
                    .removeTransition(exitingTrans.getSourceManeuver(), exitingTrans.getTargetManeuver());
                }
            }

            if (man.getType().equals(lastMan.getType())) {
                String id = man.getId();
                man = (Maneuver) lastMan.clone();
                man.getStartActions().getPayloadConfigs().clear();
                man.getStartActions().getActionMsgs().clear();
                man.getEndActions().getPayloadConfigs().clear();
                man.getEndActions().getActionMsgs().clear();
                man.setId(id);
            }

            if (man instanceof LocatedManeuver && lastMan instanceof LocatedManeuver) {
                LocationType lt = new LocationType(((LocatedManeuver) man).getManeuverLocation());
                lt.setDepth(((LocatedManeuver) lastMan).getManeuverLocation().getAllZ());
                lt.setOffsetDown(0);
                ((LocatedManeuver) man).getManeuverLocation().setLocation(lt);
            }

            // System.out.println("adding "+lastMan.getId()+" -> "+man.getId());

            plan.getGraph().addTransition(lastMan.getId(), man.getId(), defaultCondition);

        }

        if (man instanceof LocatedManeuver) {
            double oldDepth = ((LocatedManeuver) man).getManeuverLocation().getAllZ();
            LocationType l = renderer.getRealWorldLocation(loc);
            l.setDepth(oldDepth);
            l.setOffsetDown(0);
            ((LocatedManeuver) man).getManeuverLocation().setLocation(l);
        }

        plan.getGraph().addManeuver(man);

        parsePlan();
        warnChanged();

        return man;
    }

    private String getNewManeuverName(String manType) {

        int i = 1;
        while (plan.getGraph().getManeuver(manType + i) != null)
            i++;

        return manType + i;
    }

    /*
     * public Maneuver getInterceptedManeuver(Point pt, boolean iterateManeuvers) { if (selectedManeuver != null) {
     * Point2D p = renderer.getScreenPosition((currentlySelectedManeuver).getDestination()); if (p.distance(pt) < 5 &&
     * !iterateManeuvers) { return currentlySelectedManeuver; } } Vector<Maneuver> interceptedManeuvers = new
     * Vector<Maneuver>();
     * 
     * Maneuver[] maneuvers = plan.getGraph().getAllManeuvers(); for (Maneuver man : maneuvers) { if (man instanceof
     * Goto) { Goto g = (Goto) man; Point2D p = renderer.getScreenPosition(g.getDestination()); if (p.distance(pt) < 5)
     * { interceptedManeuvers.add(man); } } }
     * 
     * if (interceptedManeuvers.size() == 1) { currentlySelectedManeuver = interceptedManeuvers.firstElement(); return
     * interceptedManeuvers.firstElement(); } if (interceptedManeuvers.size() > 1) {
     * 
     * if (currentlySelectedManeuver != null && interceptedManeuvers.contains(currentlySelectedManeuver)) { Maneuver ret
     * =
     * interceptedManeuvers.get((interceptedManeuvers.indexOf(currentlySelectedManeuver)+1)%interceptedManeuvers.size()
     * ); currentlySelectedManeuver = ret;
     * 
     * return ret; } else { currentlySelectedManeuver = interceptedManeuvers.firstElement(); return
     * interceptedManeuvers.firstElement(); } } currentlySelectedManeuver = null; return null; }
     */

    private void warnChanged() {
        if (planEditor != null && planEditor.getParentMP() != null) {
            planEditor.getParentMP().setMissionChanged(true);
        }
    }

    public void reset() {
        planElem.recalculateManeuverPositions(renderer);
        repaint();
    }

    public IndividualPlanEditor getPlanEditor() {
        return planEditor;
    }

    public void setPlanEditor(IndividualPlanEditor planEditor) {
        this.planEditor = planEditor;
    }

    public PlanElement getPlanElem() {
        return planElem;
    }

    public StateRenderer2D getRenderer() {
        return renderer;
    }

    public MissionType getMission() {
        return mission;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        if (editable)
            setBorder(BorderFactory.createLineBorder(Color.red, 3));
        else
            setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        renderer.addChangeListener(cl);
    }

    @Override
    public void cleanup() {
        renderer.cleanup();
    }

    @Override
    public void clearVehicleTail(String[] vehicles) {
        renderer.clearVehicleTail(vehicles);
    }

    @Override
    public void focusLocation(LocationType location) {
        renderer.focusLocation(location);
    }

    @Override
    public void focusObject(AbstractElement mo) {
        renderer.focusObject(mo);
    }

    @Override
    public void followVehicle(String vehicle) {
        renderer.followVehicle(vehicle);
    }

    @Override
    public int getShowMode() {
        return renderer.getShowMode();
    }

    @Override
    public String getLockedVehicle() {
        return renderer.getLockedVehicle();
    }

    @Override
    public MapGroup getMapGroup() {
        return renderer.getMapGroup();
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        renderer.removeChangeListener(cl);
    }

    @Override
    public void setMapGroup(MapGroup mapGroup) {
        renderer.setMapGroup(mapGroup);
    }

    @Override
    public void setVehicleTailOff(String[] vehicles) {
        renderer.setVehicleTailOff(vehicles);
    }

    @Override
    public void setVehicleTailOn(String[] vehicles) {
        renderer.setVehicleTailOn(vehicles);
    }

    public int getViewMode() {
        return renderer.getViewMode();
    }

    @Override
    public void setViewMode(int mode) {
        lastViewMode = mode;
        renderer.setViewMode(mode);
    }

    @Override
    public void vehicleStateChanged(String vehicle, SystemPositionAndAttitude state) {
        renderer.vehicleStateChanged(vehicle, state);
    }

    public void vehicleStateChanged(String vehicle, SystemPositionAndAttitude state, boolean repaint) {
        renderer.vehicleStateChanged(vehicle, state, repaint);
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
    public void addInteraction(StateRendererInteraction interaction) {
        renderer.addInteraction(interaction);
    }

    @Override
    public Collection<StateRendererInteraction> getInteractionModes() {
        return renderer.getInteractionModes();
    }

    @Override
    public void removeInteraction(StateRendererInteraction interaction) {
        renderer.removeInteraction(interaction);

    }

    @Override
    public void setActiveInteraction(StateRendererInteraction interaction) {
        renderer.setActiveInteraction(interaction);
    }

    @Override
    public StateRendererInteraction getActiveInteraction() {
        return renderer.getActiveInteraction();
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        MissionType mission = new MissionType("missions/OTA/OTA_Air_Base.nmisz");
        PlanType plan = mission.getIndividualPlansList().get("apv");

        MapPlanEditor editor = new MapPlanEditor(plan);

        GuiUtils.testFrame(editor, "Map Plan Editor");
        editor.setEditable(false);
    }
}
