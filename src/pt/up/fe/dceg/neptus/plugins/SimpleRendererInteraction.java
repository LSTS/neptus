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
 * Jun 8, 2012
 */
package pt.up.fe.dceg.neptus.plugins;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public abstract class SimpleRendererInteraction extends SimpleSubPanel implements StateRendererInteraction, Renderer2DPainter {

    private static final long serialVersionUID = 1L;
    protected InteractionAdapter interactionAdapter;
    protected ToolbarSwitch associatedSwitch = null;
    
    /**
     * @param console
     */
    public SimpleRendererInteraction(ConsoleLayout console) {
        super(console);
        interactionAdapter = new InteractionAdapter(console);
    }
    
    @Override
    public boolean getVisibility() {
        return false;
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
    }

    @Override
    public Image getIconImage() {
        return ImageUtils.getImage(PluginUtils.getPluginIcon(getClass()));
    }

    @Override
    public Cursor getMouseCursor() {
        return interactionAdapter.getMouseCursor();
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        interactionAdapter.mouseClicked(event, source);
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        interactionAdapter.mousePressed(event, source);
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        interactionAdapter.mouseDragged(event, source);
    }

     @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
         interactionAdapter.mouseMoved(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        interactionAdapter.mouseReleased(event, source);
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        interactionAdapter.wheelMoved(event, source);
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
        this.associatedSwitch = tswitch;
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        interactionAdapter.keyPressed(event, source);
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        interactionAdapter.keyReleased(event, source);
    }

      @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
          interactionAdapter.keyTyped(event, source);
    }

    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        
    }
}
