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
 * Apr 26, 2010
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BasicStroke;
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
import java.util.Collection;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

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
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
@LayerPriority(priority = 50)
@SuppressWarnings("serial")
@PluginDescription(name = "Polygon Coverage Planner", icon = "images/planning/polyline.png")
public class AreaCoveragePlanner extends ConsolePanel implements StateRendererInteraction, IEditorMenuExtension,
        Renderer2DPainter {

    @NeptusProperty(name = "Depth")
    public double depth = 1;

    @NeptusProperty(name = "Grid width")
    public double grid = 20;

    protected InteractionAdapter adapter;

    private boolean initCalled = false;

    public AreaCoveragePlanner(ConsoleLayout console) {
        super(console);
        adapter = new InteractionAdapter(console);
        setVisibility(false);
    }

    @Override
    public void initSubPanel() {

        if (initCalled)
            return;
        initCalled = true;

        addMenuItem(I18n.text("Settings")+">" + I18n.text("Coverage Planner Settings"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(AreaCoveragePlanner.this, getConsole(), true);
            }
        });

    };

    PathElement pe = null;
    int vertexCount = 0;

    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        if (pe != null) {
            Graphics2D g = (Graphics2D) g2.create();
            pe.paint(g, renderer, -renderer.getRotation());
            g.dispose();
        }
    }

    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {

        JMenu menu = new JMenu("Polygon coverage");
        final LocationType l = loc;
        menu.add(new AbstractAction("Add polygon vertex") {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                if (pe == null) {
                    pe = new PathElement(MapGroup.getMapGroupInstance(getConsole().getMission()), new MapType(), l);
                    pe.setMyColor(Color.green.brighter());
                    pe.setShape(true);
                    pe.setFinished(true);
                    pe.setStroke(new BasicStroke(2.0f));
                    pe.addPoint(0, 0, 0, false);
                }
                else {
                    double[] offsets = l.getOffsetFrom(pe.getCenterLocation());
                    pe.addPoint(offsets[1], -offsets[0], 0, false);
                }
                vertexCount++;
            }
        });

        if (vertexCount > 0) {
            menu.add(new AbstractAction("Clear") {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    pe = null;
                    vertexCount = 0;
                }
            });
        }

        if (vertexCount > 2) {
            menu.add(new AbstractAction("Generate plan(s)") {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    solve();
                }
            });
        }

        Vector<JMenuItem> items = new Vector<JMenuItem>();
        items.add(menu);
        return items;
    }

    private void solve() {

        if (pe == null) {
            GuiUtils.errorMessage(getConsole(), "Coverage Plan Solver", "The polygon is not valid");
            return;
        }

        double north, east, south, west;
        double[] bounds = pe.getBounds3d();

        south = bounds[PathElement.SOUTH_COORD];
        west = bounds[PathElement.WEST_COORD];
        north = bounds[PathElement.NORTH_COORD];
        east = bounds[PathElement.EAST_COORD];

        CoverageCell[][] cells = new CoverageCell[(int) ((north - south) / grid) + 1][(int) ((east - west) / grid) + 1];

        for (int i = 0; i < cells.length; i++)
            for (int j = 0; j < cells[i].length; j++) {
                cells[i][j] = new CoverageCell();
                cells[i][j].i = i;
                cells[i][j].j = j;
            }

        int i = 0, j = 0;
        int desiredCells = 0;
        for (double n = south + grid / 2; n < north; n += grid) {
            j = 0;
            for (double e = west + grid / 2; e < east; e += grid) {
                LocationType lt = new LocationType(pe.getCenterLocation());
                lt.translatePosition(n, e, 0);
                CoverageCell cell = cells[i][j];
                cell.realWorldLoc = lt.getNewAbsoluteLatLonDepth();
                if (pe.containsPoint(lt, null)) {
                    cell.desired = true;
                    desiredCells++;
                }
                cells[i][j] = cell;
                j++;
            }
            i++;
        }

        CoverageCell initialCell = null;
        i = 0;
        for (j = 0; j < cells[0].length - 1 && initialCell == null; j++)
            for (i = 0; i < cells.length && initialCell == null; i++)
                if (cells[i][j].desired)
                    initialCell = cells[i][j];

        if (initialCell == null) {
            GuiUtils.errorMessage("Polygon coverage", "Polygon area is invalid");
            return;
        }

        CoverageCell current = initialCell;
        desiredCells--;

        int dir = -1;

        while (desiredCells > 0) {
            current.visited = true;
            current.active = false;
            if (dir == 1) {
                if (current.i < cells.length - 1 && cells[current.i + 1][current.j].desired == true
                        && cells[current.i + 1][current.j].visited == false) {
                    current.next = cells[current.i + 1][current.j];
                    cells[current.i + 1][current.j].previous = current;
                    current = current.next;
                    current.active = true;
                }
                else {
                    dir = -1;
                    if (current.j == cells[0].length - 1)
                        break;

                    while (!cells[current.i][current.j + 1].desired && i > 0 && current.previous != null) {
                        current.active = false;
                        current = current.previous;
                    }

                    if (i == 0)
                        break;

                    current.next = cells[current.i][current.j + 1];
                    cells[current.i][current.j + 1].previous = current;
                    current = current.next;
                    current.active = true;
                }
            }
            else {
                if (current.i > 0 && cells[current.i - 1][current.j].desired == true
                        && cells[current.i - 1][current.j].visited == false) {
                    current.next = cells[current.i - 1][current.j];
                    cells[current.i - 1][current.j].previous = current;
                    current = current.next;
                    current.active = true;
                }
                else {
                    dir = 1;
                    if (current.j == cells[0].length - 1)
                        break;

                    while (current.previous != null && !cells[current.i][current.j + 1].desired && i < cells.length) {
                        current.active = false;
                        current = current.previous;
                    }

                    if (i == cells.length)
                        break;

                    current.next = cells[current.i][current.j + 1];
                    cells[current.i][current.j + 1].previous = current;
                    current = current.next;
                    current.active = true;
                }
            }
            desiredCells--;
        }
        generatePlans(cells, initialCell);
    }

    void generatePlans(CoverageCell[][] mat, CoverageCell first) {

        Vector<String> selectedVehicles = new Vector<String>();
        Vector<SystemsList> tmp = getConsole().getSubPanelsOfClass(SystemsList.class);

        selectedVehicles.addAll(tmp.get(0).getSelectedSystems(true));
        Object planid;

        if (selectedVehicles.size() > 1)
            planid = JOptionPane.showInputDialog(getConsole(), "Enter desired plan prefix");
        else
            planid = JOptionPane.showInputDialog(getConsole(), "Enter desired plan name");

        MissionType mission = getConsole().getMission();

        if (mission == null) {
            GuiUtils.errorMessage(getConsole(), "Coverage Plan Solver", "No mission has been set");
            return;
        }

        if (selectedVehicles.size() <= 1) {
            CoverageCell current = first, next = current.next;
            PlanCreator creator = new PlanCreator(mission);
            creator.setLocation(first.realWorldLoc);
            // creator.addManeuver("Goto");
            while (next != null) {
                if (next.j != current.j) {
                    CoverageCell pivot = current;
                    while (pivot.previous != null && pivot.previous.i == current.i)
                        pivot = pivot.previous;
                    creator.setLocation(pivot.realWorldLoc);
                    creator.addManeuver("Goto");
                    creator.setLocation(next.realWorldLoc);
                    creator.addManeuver("Goto");
                }
                current = next;
                next = current.next;
            }

            PlanType plan = creator.getPlan();
            plan.setId(planid.toString());
            plan.setVehicle(getConsole().getMainSystem());
            mission.addPlan(plan);
            mission.save(false);
            getConsole().updateMissionListeners();
        }
        else {
            double distance = 0;
            CoverageCell current = first, next = current.next;
            distance += current.realWorldLoc.getDistanceInMeters(next.realWorldLoc);
            while (next != null) {
                if (next.j != current.j) {
                    CoverageCell pivot = current;
                    while (pivot.previous != null && pivot.previous.i == current.i)
                        pivot = pivot.previous;
                }
                distance += current.realWorldLoc.getDistanceInMeters(next.realWorldLoc);
                current = next;
                next = current.next;
            }

            double distEach = distance / selectedVehicles.size();

            current = first;
            next = current.next;
            PlanCreator creator = new PlanCreator(mission);
            creator.setLocation(current.realWorldLoc);
            distance = 0;
            int curIndex = 0;
            while (next != null) {

                if (next.j != current.j) {
                    CoverageCell pivot = current;
                    while (pivot.previous != null && pivot.previous.i == current.i)
                        pivot = pivot.previous;
                    creator.setLocation(pivot.realWorldLoc);
                    creator.addManeuver("Goto");

                    distance += current.realWorldLoc.getDistanceInMeters(next.realWorldLoc);

                    if (distance < distEach) {
                        creator.setLocation(next.realWorldLoc);
                        creator.addManeuver("Goto");
                    }
                }
                else
                    distance += current.realWorldLoc.getDistanceInMeters(next.realWorldLoc);

                if (distance > distEach) {
                    creator.setLocation(current.realWorldLoc);
                    creator.addManeuver("Goto");
                    PlanType plan = creator.getPlan();
                    plan.setVehicle(selectedVehicles.get(curIndex));
                    plan.setId(planid + "_" + selectedVehicles.get(curIndex++));

                    mission.addPlan(plan);
                    creator = new PlanCreator(mission);
                    creator.setLocation(current.realWorldLoc);
                    creator.addManeuver("Goto");
                    distance = 0;
                }
                current = next;
                next = current.next;
            }
            PlanType plan = creator.getPlan();
            plan.setVehicle(selectedVehicles.get(curIndex));
            plan.setId(planid + "_" + selectedVehicles.get(curIndex++));

            mission.addPlan(plan);

            mission.save(false);
            getConsole().updateMissionListeners();

        }
    }

    void printMatrix(CoverageCell[][] mat) {
        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < mat[0].length; j++) {
                if (mat[i][j] != null)
                    System.out.print(mat[i][j].rep());
            }
            System.out.println();
        }
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
            JPopupMenu popup = new JPopupMenu();
            popup.add("Generate plans locally").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    solve();
                }
            });

            popup.add("Clear polygon").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    pe = null;
                    vertexCount = 0;
                }
            });

            popup.add("Settings").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    PropertiesEditor.editProperties(AreaCoveragePlanner.this, getConsole(), true);
                }
            });

            popup.show(source, event.getX(), event.getY());
        }
        else if (pe == null) {
            LocationType l = source.getRealWorldLocation(event.getPoint());
            pe = new PathElement(MapGroup.getMapGroupInstance(getConsole().getMission()), new MapType(), l);
            pe.setMyColor(Color.green.brighter());
            pe.setShape(true);
            pe.setFinished(true);
            pe.setStroke(new BasicStroke(2.0f));
            pe.addPoint(0, 0, 0, false);
            vertexCount = 1;
        }
        else {
            LocationType l = source.getRealWorldLocation(event.getPoint());
            double[] offsets = l.getOffsetFrom(pe.getCenterLocation());
            pe.addPoint(offsets[1], offsets[0], 0, false);
            vertexCount++;
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
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
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

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
