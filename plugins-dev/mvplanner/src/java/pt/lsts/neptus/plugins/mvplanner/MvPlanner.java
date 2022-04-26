package pt.lsts.neptus.plugins.mvplanner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.mvplanner.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanner.jaxb.ProfileMarshaler;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridArea;
import pt.lsts.neptus.plugins.mvplanner.tasks.SurveyTask;
import pt.lsts.neptus.plugins.mvplanner.ui.MapObject;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Allows an operator to add tasks and obstacles as polygonal objects.
 * Adapted from: https://github.com/zepinto/neptus/tree/develop/plugins-dev/auto-planner
 * @author tsm
 */
@PluginDescription(author = "José Pinto, Tiago Marques", name = "MvPlanner: Multi-Vehicle Planner",
        icon = "pt/lsts/neptus/plugins/map/map-edit.png",
        version = "0.1", category = PluginDescription.CATEGORY.INTERFACE)
@LayerPriority(priority = 90)
public class MvPlanner extends ConsoleInteraction implements Renderer2DPainter {

    @NeptusProperty(name = "Operational area's cell size", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int opAreaCellSize = 10;

    @NeptusProperty(name = "Display operational area's grid", userLevel = NeptusProperty.LEVEL.REGULAR)
    public boolean displayOpAreaGrid = false;

    @NeptusProperty(name = "Display finished tasks", userLevel = NeptusProperty.LEVEL.REGULAR)
    public boolean displayFinishedtasks = false;

    private final ProfileMarshaler profileMarshaler = new ProfileMarshaler();
    private final Map<String, Profile> availableProfiles = profileMarshaler.getAllProfiles();
    private final PlanGenerator planGenerator = new PlanGenerator();
    private final VehicleAwareness vawareness = new VehicleAwareness();

    /** Mapping of current tasks and their IDs **/
    private final ConcurrentHashMap<String, PlanTask> tasks = new ConcurrentHashMap<>();

    protected PolygonType currentPolygon = new PolygonType();
    protected PolygonType.Vertex vertex = null;
    protected Vector<MapPanel> maps = new Vector<>();

    /**
     * The preview of the area to be generated for the
     * current polygon, it it has more than 2 vertices
     * */
    private GridArea areaPreview = null;

    /** Area used to compute safe paths **/
    private GridArea operationalArea = null;

    /** If currently adding an object to the map **/
    private boolean addingObject = false;


    private void onNewObjectAdded(MapObject obj) {
        if(obj.isObstacle()) {
            // add obstacle
            return;
        }
        else if(obj.isOpArea())
            operationalArea = new GridArea(obj.getPolygon(), opAreaCellSize);

        onNewTaskAdded(obj.getPolygon(), obj.getSelectedProfile());
    }

    /**
     * Called when a new task object is added, creates
     * its corresponding PlanTask and adds it to the
     * planning problem
     * */
    private void onNewTaskAdded(PolygonType taskPolygon, Profile taskProfile) {
        int nPoints = taskPolygon.getVertices().size();

        // visit point
        if(nPoints == 1) {

        }
        // survey
        else if(nPoints > 2) {
            PlanTask surveyTask = new SurveyTask(taskPolygon, taskProfile);
            surveyTask.associatePlan(planGenerator.generate(surveyTask));
            tasks.put(surveyTask.getId(), surveyTask);
        }
    }

    @Subscribe
    public void consume(ConsoleEventVehicleStateChanged event) {
        vawareness.onVehicleStateChange(event);
    }

    @Subscribe
    public void consume(PlanControlState event) {
        vawareness.onPlanControlState(event);

        PlanTask task = tasks.getOrDefault(event.getPlanId(), null);

        if (task != null)
            synchronized (task) {
                task.setCompletion(event.getPlanProgress());
            }
    }

    /**
     * Adds a layer to all map panels that paints the current currentPolygon
     */
    @Override
    public void initInteraction() {
        planGenerator.setConsole(getConsole());
        vawareness.setConsole(getConsole());
        maps = getConsole().getSubPanelsOfClass(MapPanel.class);
        for (MapPanel p : maps)
            p.addPreRenderPainter(this);
    }

    /**
     * Removes layer from all map panels
     */
    @Override
    public void cleanInteraction() {
        for (MapPanel p : maps)
            p.removePostRenderPainter(this);
    }

    /**
     * @see ConsoleInteraction
     */
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        vertex = null;
        super.setActive(mode, source);
    }

    /**
     * Given a point in the map, checks if there is some vertex intercepted.
     */
    public PolygonType.Vertex intercepted(MouseEvent evt, StateRenderer2D source) {
        for (PolygonType.Vertex v : currentPolygon.getVertices()) {
            Point2D pt = source.getScreenPosition(new LocationType(v.getLocation()));
            if (pt.distance(evt.getPoint()) < 5) {
                return v;
            }
        }
        return null;
    }

    /**
     * @see ConsoleInteraction
     */
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (!SwingUtilities.isRightMouseButton(event)) {
            if (event.getClickCount() == 2) {
                LocationType loc = source.getRealWorldLocation(event.getPoint());
                addNewVertex(loc);
                source.repaint();
                return;
            }
            else {
                super.mouseClicked(event, source);
                return;
            }
        }

        PolygonType.Vertex v = intercepted(event, source);
        JPopupMenu popup = new JPopupMenu();
        if (v != null) {
            popup.add(I18n.text("Edit location")).addActionListener(e -> {
                LocationType l = new LocationType(v.getLocation());
                LocationType newLoc = LocationPanel.showLocationDialog(source, I18n.text("Edit Vertex Location"), l,
                        getConsole().getMission(), true);
                if (newLoc != null) {
                    v.setLocation(new LocationType(newLoc));
                    currentPolygon.recomputePath();

                    if(areaPreview != null)
                        areaPreview.recomputeDimensions(currentPolygon);
                }
                source.repaint();
            });
            popup.add(I18n.text("Remove vertex")).addActionListener(e -> {
                currentPolygon.removeVertex(v);
                source.repaint();

                int nVertices = currentPolygon.getVertices().size();
                if(nVertices == 0)
                    addingObject = false;
                else if(nVertices < 3)
                    areaPreview = null;
            });
        }
        else {
            popup.add(I18n.text("Add vertex")).addActionListener(e -> {
                LocationType loc = source.getRealWorldLocation(event.getPoint());
                addNewVertex(loc);
                source.repaint();
            });

            int nVertex = currentPolygon.getVertices().size();
            if(currentPolygon != null && nVertex > 1) {
                popup.add(I18n.text("Close Object")).addActionListener(e -> {
                    /* If object was closed */
                    MapObject obj = closeObject();
                    if (obj != null) {
                        currentPolygon.setColor(Color.YELLOW.darker());
                        currentPolygon = new PolygonType();
                        source.repaint();

                        onNewObjectAdded(obj);
                        addingObject = false;
                        areaPreview = null;
                    }
                });
                popup.add(I18n.text("Cancel")).addActionListener(e -> {
                    addingObject = false;
                    currentPolygon = new PolygonType();
                    areaPreview = null;
                });
            }

            if(!addingObject)
                popup.add(I18n.text("Allocate tasks")).addActionListener(e -> {
                    // call allocator
                });
        }

        popup.show(source, event.getX(), event.getY());
    }

    private void addNewVertex(LocationType loc) {
        currentPolygon.addVertex(loc.getLatitudeDegs(), loc.getLongitudeDegs());

        addingObject = true;

        if(areaPreview != null)
            areaPreview.recomputeDimensions(currentPolygon);
        else if(currentPolygon.getVertices().size() >= 3)
            areaPreview = new GridArea(currentPolygon);
    }

    private MapObject closeObject() {
        ParametersWindow window = new ParametersWindow(currentPolygon, availableProfiles);
        window.show();
        return window.getOutcome();
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        PolygonType.Vertex v = intercepted(event, source);
        if (v == null)
            super.mousePressed(event, source);
        else
            vertex = v;
    }

    /**
     * @see ConsoleInteraction
     */
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (vertex == null)
            super.mouseDragged(event, source);
        else {
            vertex.setLocation(source.getRealWorldLocation(event.getPoint()));
            currentPolygon.recomputePath();
        }
    }

    /**
     * @see ConsoleInteraction
     */
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        super.mouseReleased(event, source);
        if (vertex != null)
            currentPolygon.recomputePath();

        if(areaPreview != null)
            areaPreview.recomputeDimensions(currentPolygon);

        vertex = null;
    }

    /**
     * Paints both the currentPolygon and the vertices of the currentPolygon
     * @see ConsoleInteraction
     */
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        g.setTransform(source.getIdentity());
        paint(g, source);
        currentPolygon.getVertices().forEach(v -> {
            Point2D pt = source.getScreenPosition(new LocationType(v.getLocation()));
            Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10);
            Color c = Color.yellow;
            g.setColor(new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue(), 200));
            g.fill(ellis);
            g.setColor(c);
            g.draw(ellis);
        });
    }

    /**
     * Paints the currentPolygon with color selected by the user
     * @see Renderer2DPainter
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        currentPolygon.paint(g, renderer);

        for(PlanTask task : tasks.values()) {
            // operator doesn't want to see finished tasks
            if(task.getState() == PlanTask.TaskStateEnum.Completed && !displayFinishedtasks)
                continue;

            task.paintTask(g, renderer);
        }

        if(operationalArea != null)
            operationalArea.paint(g, renderer, Color.BLACK, displayOpAreaGrid, false);

        if(areaPreview != null)
            areaPreview.paint(g, renderer, Color.CYAN.darker(), false, false);
    }

    public class ParametersWindow {
        private MapObject outcome = null;
        private PolygonType polygon;
        private Map<String, Profile> profiles;

        public ParametersWindow(PolygonType currentObject, Map<String, Profile> availableProfiles) {
            polygon = currentObject;
            profiles = availableProfiles;
        }

        public MapObject getOutcome() {
            return outcome;
        }

        public void show() {
            JDialog dialog = new JDialog((Frame) ConfigFetch.getSuperParentFrame());

        /* Main panel */
            JPanel idPanel = new JPanel();
            FlowLayout flow = new FlowLayout();
            flow.setAlignment(FlowLayout.LEFT);
            idPanel.setLayout(flow);

            JComboBox<String> profilesBox = loadProfiles();
            JComboBox<String> objectType = new JComboBox<>();

        /* Parameters */
            JTextField objName = new JTextField(8);
            objName.setEditable(true);
            objName.setText(NameNormalizer.getRandomID());

            JCheckBox obstacleCheck = new JCheckBox(I18n.text("Obstacle"));
            obstacleCheck.setSelected(false);

            JCheckBox opAreaCheck = new JCheckBox(I18n.text("Operational area"));
            opAreaCheck.setSelected(false);

            if(polygon.getVertices().size() <= 2)
                opAreaCheck.setEnabled(false);

            opAreaCheck.addItemListener(e -> {
                boolean enabledValue = false;
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    enabledValue = true;
                    obstacleCheck.setSelected(false);
                }

                profilesBox.setEnabled(enabledValue);
                objectType.setEnabled(!enabledValue);
                obstacleCheck.setEnabled(!enabledValue);

            });

            obstacleCheck.addItemListener(e -> {
                boolean enabledValue = false;
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    enabledValue = true;
                    opAreaCheck.setSelected(false);
                }

                profilesBox.setEnabled(!enabledValue);
                objectType.setEnabled(!enabledValue);

                if(polygon.getVertices().size() <= 2) {
                    opAreaCheck.setSelected(!enabledValue);
                    opAreaCheck.setEnabled(!enabledValue);
                }

            });

            JPanel checksPanel = new JPanel();
            checksPanel.setLayout(new BoxLayout(checksPanel, BoxLayout.PAGE_AXIS));
            checksPanel.add(opAreaCheck);
            checksPanel.add(obstacleCheck);

            idPanel.add(new JLabel(I18n.text("Object Name:")));
            idPanel.add(objName);
            idPanel.add(profilesBox);
            idPanel.add(checksPanel);


        /* buttons */
            JPanel buttonsPanel = new JPanel();
            FlowLayout layout = new FlowLayout();
            layout.setAlignment(FlowLayout.LEFT);
            buttonsPanel.setLayout(layout);

            JButton add = new JButton(I18n.text("OK"));
            add.setActionCommand("add");
            add.addActionListener(e -> {
                polygon.setId(objName.getText());
                outcome = new MapObject(polygon);

                if(opAreaCheck.isSelected())
                    outcome.setAsOpArea();
                else if(obstacleCheck.isSelected())
                    outcome.setAsObstacle();

                if(profilesBox.isEnabled())
                    outcome.associateProfile(availableProfiles.get(profilesBox.getSelectedItem()));

                dialog.setVisible(false);
                dialog.dispose();
            });
            add.setPreferredSize(new Dimension(100,25));
            buttonsPanel.add(add);

            JButton cancel = new JButton(I18n.text("Cancel"));
            cancel.setActionCommand("cancel");
            cancel.addActionListener(e -> {
                dialog.setVisible(false);
                dialog.dispose();
            });

            cancel.setPreferredSize(new Dimension(100,25));
            buttonsPanel.add(cancel);

            GuiUtils.reactEnterKeyPress(add);
            GuiUtils.reactEscapeKeyPress(cancel);

            dialog.setLayout(new BorderLayout());
            //dialog.setModal(true);
            dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);

            dialog.getContentPane().add(idPanel, BorderLayout.NORTH);
            dialog.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
            //dialog.setSize(paramsPanel.getPreferredSize());
            Dimension pSize = new Dimension(100, 100);
            dialog.setSize(Math.max(pSize.width, 480), pSize.height + 12*2);
            GuiUtils.centerOnScreen(dialog);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });
            dialog.setVisible(true);
        }

        private JComboBox<String> loadProfiles() {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
            JComboBox<String> profilesBox = new JComboBox<>(model);
            profiles.keySet().forEach(model::addElement);

            return profilesBox;
        }
    }
}
