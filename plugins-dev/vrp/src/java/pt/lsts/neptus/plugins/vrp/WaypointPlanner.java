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
 * Apr 22, 2010
 */
package pt.lsts.neptus.plugins.vrp;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.vecmath.Point2d;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.SystemsList;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.templates.PlanCreator;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.vrp.planning.VrpManager;
import pt.lsts.neptus.renderer2d.CustomInteractionSupport;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "zp", name = "TSP Planner Daemon", version = "0.1", icon = "pt/lsts/neptus/plugins/vrp/stars.png")
@LayerPriority(priority = 51)
public class WaypointPlanner extends ConsolePanel implements Renderer2DPainter, IEditorMenuExtension,
        StateRendererInteraction {

    @NeptusProperty(name = "Depth", description = "Depth of the first generated plan")
    public double planDepth = 1;

    @NeptusProperty(name = "Distance between generated plans", description = "The z offeset between plans, in case more than one plans are generated")
    public double planDistance = 0;

    private static final long serialVersionUID = 1L;
    protected Vector<LocationType> points = new Vector<LocationType>();
    protected Vector<Vector<Point2d>> solution = new Vector<Vector<Point2d>>();
    protected Vector<String> availableVehicles = new Vector<String>();
    protected AbstractAction openAction, saveAction, newAction, solveAction, createAction, clearAction;
    protected File workingFile = null;

    protected InteractionAdapter adapter;

    public WaypointPlanner(ConsoleLayout console) {

        super(console);
        adapter = new InteractionAdapter(console);
        setVisibility(false);

        // setVisibility(false);
        openAction = new AbstractAction("Load waypoint list") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                openList();
            }
        };

        saveAction = new AbstractAction("Save waypoint list") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                saveList();
            }
        };

        newAction = new AbstractAction("Create waypoint list",
                ImageUtils.getIcon("pt/lsts/neptus/plugins/planning/planning.png")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                newList();
            }
        };

        solveAction = new AbstractAction("Solve TSP",
                ImageUtils.getIcon("pt/lsts/neptus/plugins/planning/planning.png")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                solve();
            }
        };

        createAction = new AbstractAction("Generate Neptus Plans",
                ImageUtils.getIcon("pt/lsts/neptus/plugins/planning/planning.png")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {

                generatePlans();
            }
        };
    }

    private void openList() {
        JFileChooser chooser = new JFileChooser("conf/waypoints");
        int option = chooser.showOpenDialog(getConsole());
        chooser.setMultiSelectionEnabled(false);
        if (option == JFileChooser.APPROVE_OPTION) {
            NeptusLog.pub().info("<###>open " + chooser.getSelectedFile());
            workingFile = chooser.getSelectedFile();

            try {
                BufferedReader reader = new BufferedReader(new FileReader(workingFile));
                points.clear();
                solution = null;
                String line = reader.readLine();
                while (line != null) {
                    if (!line.startsWith("#")) {
                        String ll[] = line.split(" ");
                        double lat = CoordinateUtil.parseCoordString(ll[0]);
                        double lon = CoordinateUtil.parseCoordString(ll[1]);
                        LocationType l = new LocationType();
                        l.setLatitudeDegs(lat);
                        l.setLongitudeDegs(lon);
                        points.add(l);
                    }
                    line = reader.readLine();
                }
                reader.close();
            }
            catch (Exception e) {
                GuiUtils.errorMessage(getConsole(), e);
                NeptusLog.pub().error(e);
            }
            repaint();
        }
    }

    private void saveList() {
        JFileChooser chooser;
        if (workingFile != null)
            chooser = new JFileChooser(workingFile);
        else
            chooser = new JFileChooser("conf/waypoints");

        int option = chooser.showSaveDialog(getConsole());

        if (option == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                writer.write("# Waypoint list generated by Neptus on " + new Date() + "\n");
                for (LocationType lt : points) {
                    double[] lld = lt.getAbsoluteLatLonDepth();
                    writer.write(CoordinateUtil.latitudeAsPrettyString(lld[0], LatLonFormatEnum.DM) + " ");
                    writer.write(CoordinateUtil.longitudeAsPrettyString(lld[1], LatLonFormatEnum.DM) + "\n");
                }
                writer.close();
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }

    private void newList() {
        workingFile = null;
        points.clear();
        repaint();
    }

    private void solve() {

        availableVehicles.clear();

        MissionType mission = getConsole().getMission();

        if (mission == null) {
            GuiUtils.errorMessage(getConsole(), "TSP Plan generator", "No mission has been set.");
            return;
        }
        Vector<SystemsList> tmp = getConsole().getSubPanelsOfClass(SystemsList.class);

        if (!tmp.isEmpty()) {
            availableVehicles.addAll(tmp.get(0).getSelectedSystems(true));
        }

        if (availableVehicles.isEmpty() && getConsole().getMainSystem() != null) {
            availableVehicles.add(getConsole().getMainSystem());
        }

        Point2d zero = new Point2d(0,0);

        Vector<Point2d> pts = new Vector<Point2d>();
        for (LocationType l : points)
            pts.add(new Point2d(l.getOffsetFrom(mission.getHomeRef())));

        solution = VrpManager.computePathsSingleDepot(zero, pts, availableVehicles.size());
    }

    private void generatePlans() {
        // if (solution == null)
        solve();

        Object planid = JOptionPane.showInputDialog(getConsole(), "Enter desired plan prefix");
        if (planid == null)
            return;
        MissionType mission = getConsole().getMission();

        for (int i = 0; i < availableVehicles.size(); i++) {
            String vname = availableVehicles.get(i);
            Vector<Point2d> p = solution.get(i);
            PlanCreator creator = new PlanCreator(getConsole().getMission());

            LocationType loc = new LocationType(getConsole().getMission().getHomeRef());
            for (Point2d pt : p) {
                creator.setLocation(loc);
                creator.move(pt.x, pt.y);
                creator.addManeuver("Goto");
            }

            PlanType plan = creator.getPlan();
            if (planid.toString().length() > 0)
                plan.setId(planid.toString() + "_" + vname);
            plan.setVehicle(vname);

            mission.addPlan(plan);
        }
        getConsole().getMission().save(false);
        getConsole().updateMissionListeners();
    }

    protected boolean alreadyInited = false;

    @Override
    public void initSubPanel() {

        if (alreadyInited)
            return;
        alreadyInited = true;

        addMenuItem(I18n.text("Settings") + ">" + I18n.text("TSP Settings"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(WaypointPlanner.this, getConsole(), true);
            }
        });

        Vector<CustomInteractionSupport> panels = getConsole().getSubPanelsOfInterface(CustomInteractionSupport.class);
        for (CustomInteractionSupport cis : panels)
            cis.addInteraction(this);

        Vector<ILayerPainter> renders = getConsole().getSubPanelsOfInterface(ILayerPainter.class);

        for (ILayerPainter str2d : renders) {
            str2d.addPostRenderPainter(this, "TSP Painter");
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        for (LocationType l : points) {
            Point2D pt = renderer.getScreenPosition(l);
            if (pt != null) {
                g.setColor(Color.white);
                g.fill(new Ellipse2D.Double(pt.getX() - 3, pt.getY() - 3, 6, 6));
                g.setColor(Color.black);
                g.draw(new Ellipse2D.Double(pt.getX() - 3, pt.getY() - 3, 6, 6));
            }
        }
    }

    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {

        final LocationType l = new LocationType(loc);
        Vector<JMenuItem> menus = new Vector<JMenuItem>();

        JMenu tspSolver = new JMenu("TSP Solver");
        menus.add(tspSolver);

        AbstractAction add = new AbstractAction("Add waypoint") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                points.add(new LocationType(l));
            }
        };
        tspSolver.add(new JMenuItem(add));

        AbstractAction clear = new AbstractAction("Clear waypoints") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                points.clear();
            }
        };
        if (!points.isEmpty())
            tspSolver.add(new JMenuItem(clear));

        for (int i = 0; i < points.size(); i++) {
            LocationType lt = points.get(i);
            if (lt.getDistanceInMeters(loc) < 1.5) {
                final int index = i;
                AbstractAction remove = new AbstractAction("Remove waypoint") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        points.remove(index);
                    }
                };
                tspSolver.add(new JMenuItem(remove));
                break;
            }
        }

        tspSolver.addSeparator();
        tspSolver.add(new JMenuItem(openAction));
        tspSolver.add(new JMenuItem(saveAction));
        tspSolver.add(new JMenuItem(createAction));
        return menus;
    }

    @Override
    public Image getIconImage() {
        return ImageUtils.getImage(PluginUtils.getPluginIcon(this.getClass()));
    }

    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {

        if (event.getButton() == MouseEvent.BUTTON3) {
            // adapter.mouseClicked(event, source);
            JPopupMenu popup = new JPopupMenu();

            popup.add("Generate Plans").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    generatePlans();
                }
            });

            popup.add("Clear Waypoints").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    points.clear();
                }
            });

            popup.add("Load Waypoints").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    openList();
                }
            });

            popup.add("Save Waypoints").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    saveList();
                }
            });

            popup.add("Settings").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    PropertiesEditor.editProperties(WaypointPlanner.this, getConsole(), true);
                }
            });

            popup.show(source, event.getX(), event.getY());
        }
        else {
            LocationType clicked = source.getRealWorldLocation(event.getPoint());

            for (int i = 0; i < points.size(); i++) {
                LocationType lt = points.get(i);
                if (lt.getDistanceInMeters(clicked) < 1.5) {
                    points.remove(i);
                    return;
                }
            }

            points.add(clicked);
        }

    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        adapter.mouseDragged(event, source);
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);
    }
    
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
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

    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
