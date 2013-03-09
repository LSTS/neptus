/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * May 14, 2010
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.planning.MultiVehicleTask.TASK_STATE;
import pt.up.fe.dceg.neptus.plugins.planning.MultiVehicleTask.TASK_TYPE;
import pt.up.fe.dceg.neptus.renderer2d.CustomInteractionSupport;
import pt.up.fe.dceg.neptus.renderer2d.ILayerPainter;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
@PluginDescription(name="MultiVehicle Planner", icon="pt/up/fe/dceg/neptus/plugins/planning/stars.png")
@LayerPriority(priority=90)
public class MultiVehiclePlanner extends SimpleSubPanel implements StateRendererInteraction, Renderer2DPainter {

    private static final long serialVersionUID = -1620554331443024866L;
    protected InteractionAdapter adapter ;
    protected Image stars = ImageUtils.getImage("pt/up/fe/dceg/neptus/plugins/planning/stars.png");
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
    public String getName() {
        return "Multi-vehicle planner";
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

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
