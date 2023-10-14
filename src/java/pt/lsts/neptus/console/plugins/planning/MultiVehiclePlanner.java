/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * May 14, 2010
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.planning.MultiVehicleTask.TASK_STATE;
import pt.lsts.neptus.console.plugins.planning.MultiVehicleTask.TASK_TYPE;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.CustomInteractionSupport;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
@PluginDescription(name="MultiVehicle Planner", icon="images/planning/stars.png")
@LayerPriority(priority=90)
public class MultiVehiclePlanner extends ConsolePanel implements StateRendererInteraction, Renderer2DPainter {

    private static final long serialVersionUID = -1620554331443024866L;
    protected InteractionAdapter adapter ;
    protected Image stars = ImageUtils.getImage("images/planning/stars.png");
    protected Vector<LocationType> lastClicks = new Vector<LocationType>();
    protected MultiVehicleTask editing = null;

    PriorityBlockingQueue<MultiVehicleTask> tasks = new PriorityBlockingQueue<MultiVehicleTask>();

    public MultiVehiclePlanner(ConsoleLayout console) {
        super(console);
        adapter = new InteractionAdapter(console);
        removeAll();
        add(new JLabel(new ImageIcon(stars)));
    }

    @Override
    public void initSubPanel() {
        Vector<CustomInteractionSupport> panels = getConsole().getSubPanelsOfInterface(CustomInteractionSupport.class);		
        for (CustomInteractionSupport cis: panels)
            cis.addInteraction(this);

                Vector<ILayerPainter> renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);		
                for (ILayerPainter p: renderers)
                    p.addPostRenderPainter(this, "Multi-Vehicle planner");
    }

    @Override
    public Image getIconImage() {
        return stars;
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
            lastClicks.clear();
            editing = null;
            return;
        }	

        if (event.getClickCount() == 2) {
            int size = lastClicks.size();
            MultiVehicleTask t = new MultiVehicleTask();
            t.state = TASK_STATE.ADDED;

            switch(size) {
                case 1:
                    t.type = TASK_TYPE.GOTO;
                    t.center = lastClicks.get(0);
                    t.depth = 2;
                    tasks.add(t);
                    break;
                case 2:
                    t.type = TASK_TYPE.LOITER;
                    t.center = lastClicks.get(0);
                    t.depth = 2;
                    t.width = lastClicks.get(1).getHorizontalDistanceInMeters(t.center);
                    tasks.add(t);
                    break;
                default:
                    t.type = TASK_TYPE.SCAN;
                    t.center = new LocationType(lastClicks.get(0));
                    t.length = lastClicks.get(1).getHorizontalDistanceInMeters(t.center);
                    double[] offsets = lastClicks.get(1).getOffsetFrom(lastClicks.get(0));
                    t.center.translatePosition(offsets[0]/2, offsets[1]/2, 0);

                    t.rotation = lastClicks.get(0).getXYAngle(lastClicks.get(1));
                    t.depth = 2;
                    LocationType l = source.getRealWorldLocation(event.getPoint());			
                    t.width = l.getHorizontalDistanceInMeters(lastClicks.get(1));
                    tasks.add(t);
                    break;
            }	
            lastClicks.clear();
            editing = null;
            source.repaint();
        }		
        else {
            lastClicks.add(source.getRealWorldLocation(event.getPoint()));
        }

    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        adapter.mouseDragged(event, source);
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);

        int size = lastClicks.size();
        if (size < 1)
            return;
        if (size == 1) {
            MultiVehicleTask t = new MultiVehicleTask();
            t.state = TASK_STATE.EDITING;
            t.type = TASK_TYPE.LOITER;
            t.center = lastClicks.get(0);
            t.depth = 2;
            LocationType l = source.getRealWorldLocation(event.getPoint());			
            t.width = l.getHorizontalDistanceInMeters(t.center);
            editing = t;
            source.repaint();
            return;
        }
        if (size == 2) {
            MultiVehicleTask t = new MultiVehicleTask();
            t.type = TASK_TYPE.SCAN;
            t.state = TASK_STATE.EDITING;
            t.center = new LocationType(lastClicks.get(0));
            t.length = lastClicks.get(1).getHorizontalDistanceInMeters(t.center);
            double[] offsets = lastClicks.get(1).getOffsetFrom(lastClicks.get(0));
            t.center.translatePosition(offsets[0]/2, offsets[1]/2, 0);

            t.rotation = lastClicks.get(0).getXYAngle(lastClicks.get(1));
            t.depth = 2;
            LocationType l = source.getRealWorldLocation(event.getPoint());			
            t.width = l.getHorizontalDistanceInMeters(lastClicks.get(1));
            editing = t;
            source.repaint();
            return;
        }

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
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        for (MultiVehicleTask t : tasks) {
            t.paint((Graphics2D)g.create(), renderer);
        }
        if (editing != null)
            editing.paint((Graphics2D)g.create(), renderer);
    }	

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {

    }
    
    public static void main(String[] args) {
        ConfigFetch.initialize();
        ConsoleParse.consoleLayoutLoader(new File("conf/consoles/multivehicleplanner.ncon").getAbsolutePath());
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
