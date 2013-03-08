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
 * Nov 15, 2011
 * $Id:: MineDangerAreaInteraction.java 9615 2012-12-30 23:08:28Z pdias         $:
 */
package pt.up.fe.dceg.neptus.plugins.map.interactions;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import javax.swing.undo.UndoManager;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.plugins.map.edit.AddObjectEdit;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MineDangerAreaElement;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
@LayerPriority(priority = 60)
public class MineDangerAreaInteraction extends InteractionAdapter implements Renderer2DPainter {

    private static final long serialVersionUID = -3574806724182214294L;

    protected MapType pivot;
    protected MineDangerAreaElement element = null;
    protected UndoManager undoManager = null;

    public MineDangerAreaInteraction(MapType pivot, UndoManager undoManager, ConsoleLayout console) {
        super(console);
        this.pivot = pivot;
        this.undoManager = undoManager;
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {

        if (event.getButton() != MouseEvent.BUTTON1) {
            super.mousePressed(event, source);
            return;
        }

        LocationType lt = source.getRealWorldLocation(event.getPoint());

        element = new MineDangerAreaElement(pivot.getMapGroup(), pivot);
        element.setCenterLocation(lt);
        element.radius = 0;

        pivot.addObject(element);
        source.setFastRendering(true);
        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
        changeEvent.setChangedObject(element);
        changeEvent.setSourceMap(pivot);
        pivot.warnChangeListeners(changeEvent);

    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        super.mouseReleased(event, source);
        if (element == null) {
            return;
        }

        source.setFastRendering(false);
        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        changeEvent.setChangedObject(element);
        changeEvent.setSourceMap(pivot);
        AddObjectEdit edit = new AddObjectEdit(element);
        undoManager.addEdit(edit);
        pivot.warnChangeListeners(changeEvent);

        element = null;

        if (associatedSwitch != null)
            associatedSwitch.doClick();

    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {

        if (element == null) {
            super.mouseDragged(event, source);
            return;
        }

        double radius = source.getRealWorldLocation(event.getPoint()).getDistanceInMeters(element.getCenterLocation());
        element.radius = radius;

        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        changeEvent.setChangedObject(element);
        changeEvent.setSourceMap(pivot);

        pivot.warnChangeListeners(changeEvent);

        source.repaint();
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        if (!mode && element != null) {
            source.setFastRendering(false);
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            changeEvent.setChangedObject(element);
            changeEvent.setSourceMap(pivot);
            AddObjectEdit edit = new AddObjectEdit(element);
            undoManager.addEdit(edit);
            pivot.warnChangeListeners(changeEvent);
        }

        if (mode)
            source.addPostRenderPainter(this, I18n.text("Mine Danger Area"));
        else
            source.removePostRenderPainter(this);
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     * pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Graphics2D g2 = (Graphics2D) g.create();
        // super.paint(g2);

        double r = Math.abs(element.radius);
        g2.setColor(Color.red.darker());
        g2.setFont(new Font("Helvetica", Font.BOLD, 14));
        g2.drawString(I18n.textf("Radius: %radius m",GuiUtils.getNeptusDecimalFormat(1).format(r)), 55, 20);
        g2.dispose();
    }
}
