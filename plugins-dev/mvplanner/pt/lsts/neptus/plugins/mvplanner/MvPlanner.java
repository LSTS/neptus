package pt.lsts.neptus.plugins.mvplanner;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.mvplanner.ui.MapObject;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.util.conf.ConfigFetch;

import javax.swing.*;


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
    protected List<PolygonType> addedPolygons = new ArrayList<>();

    protected PolygonType currentPolygon = new PolygonType();
    protected PolygonType.Vertex vertex = null;
    protected Vector<MapPanel> maps = new Vector<>();

    /** If currently adding an object to the map **/
    private boolean addingObject = false;


    private void onNewObjectAdded(MapObject obj) {
        if(obj.isObstacle()) {
            // add obstacle
            return;
        }
        else if(obj.isOpArea()) {
            // add operational area
            return;
        }

        onNewTaskAdded(obj.getPolygon());
    }

    /**
     * Called when a new task object is added, creates
     * its corresponding PlanTask and adds it to the
     * planning problem
     * */
    private void onNewTaskAdded(PolygonType taskPolygon) {
        int nPoints = taskPolygon.getVertices().size();

        // visit point
        if(nPoints == 1) {

        }
        // survey
        else if(nPoints > 2){

        }
    }

    /**
     * Adds a layer to all map panels that paints the current currentPolygon
     */
    @Override
    public void initInteraction() {
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
            Point2D pt = source.getScreenPosition(new LocationType(v.lat, v.lon));
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
                currentPolygon.addVertex(loc.getLatitudeDegs(), loc.getLongitudeDegs());
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
                LocationType l = new LocationType(v.lat, v.lon);
                LocationType newLoc = LocationPanel.showLocationDialog(source, I18n.text("Edit Vertex Location"), l,
                        getConsole().getMission(), true);
                if (newLoc != null) {
                    newLoc.convertToAbsoluteLatLonDepth();
                    v.lat = newLoc.getLatitudeDegs();
                    v.lon = newLoc.getLongitudeDegs();
                    currentPolygon.recomputePath();
                }
                source.repaint();
            });
            popup.add(I18n.text("Remove vertex")).addActionListener(e -> {
                currentPolygon.removeVertex(v);
                source.repaint();

                if(currentPolygon.getVertices().size() == 0)
                    addingObject = false;
            });
        }
        else {
            popup.add(I18n.text("Add vertex")).addActionListener(e -> {
                LocationType loc = source.getRealWorldLocation(event.getPoint());
                currentPolygon.addVertex(loc.getLatitudeDegs(), loc.getLongitudeDegs());
                source.repaint();

                addingObject = true;
            });

            int nVertex = currentPolygon.getVertices().size();
            if(currentPolygon != null && nVertex > 1) {
                popup.add(I18n.text("Close Object")).addActionListener(e -> {
                    /* If object was closed */
                    MapObject obj = closeObject();
                    if (obj != null) {
                        currentPolygon.setColor(Color.YELLOW.darker());
                        addedPolygons.add(currentPolygon);
                        currentPolygon = new PolygonType();
                        source.repaint();

                        onNewObjectAdded(obj);
                        addingObject = false;
                    }
                });
                popup.add(I18n.text("Cancel")).addActionListener(e -> {
                    addingObject = false;
                    currentPolygon = new PolygonType();
                });
            }

            if(!addingObject)
                popup.add(I18n.text("Allocate tasks")).addActionListener(e -> {
                    // call allocator
                });
        }

        popup.show(source, event.getX(), event.getY());
    }

    private MapObject closeObject() {
        ParametersWindow window = new ParametersWindow(currentPolygon);
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
            LocationType loc = source.getRealWorldLocation(event.getPoint());
            vertex.lat = loc.getLatitudeDegs();
            vertex.lon = loc.getLongitudeDegs();
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
            Point2D pt = source.getScreenPosition(new LocationType(v.lat, v.lon));
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

        for(PolygonType p : addedPolygons)
            p.paint(g, renderer);
    }

    public class ParametersWindow {
        private MapObject outcome = null;
        private PolygonType polygon;

        public ParametersWindow(PolygonType currentObject) {
            polygon = currentObject;
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

            JComboBox<String> availableProfiles = new JComboBox<>();
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

                availableProfiles.setEnabled(enabledValue);
                objectType.setEnabled(!enabledValue);
                obstacleCheck.setEnabled(!enabledValue);

            });

            obstacleCheck.addItemListener(e -> {
                boolean enabledValue = false;
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    enabledValue = true;
                    opAreaCheck.setSelected(false);
                }

                availableProfiles.setEnabled(!enabledValue);
                objectType.setEnabled(!enabledValue);

                if(polygon.getVertices().size() <= 2) {
                    opAreaCheck.setSelected(!enabledValue);
                    opAreaCheck.setEnabled(!enabledValue);
                }

            });

            idPanel.add(new JLabel(I18n.text("Object Name:")));
            idPanel.add(objName);
            idPanel.add(obstacleCheck);
            idPanel.add(opAreaCheck);
            idPanel.add(availableProfiles);


        /* buttons */
            JPanel buttonsPanel = new JPanel();
            FlowLayout layout = new FlowLayout();
            layout.setAlignment(FlowLayout.RIGHT);
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

                dialog.setVisible(false);
                dialog.dispose();
            });
            add.setPreferredSize(new Dimension(100,25));
            buttonsPanel.add(add);

            JButton cancel = new JButton(I18n.text("Cancel"));
            cancel.setActionCommand("cancel");
            cancel.addActionListener(e -> {
                currentPolygon = new PolygonType();
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
    }
}
