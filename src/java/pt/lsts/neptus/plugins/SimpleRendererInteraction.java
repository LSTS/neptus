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
 * Jun 8, 2012
 */
package pt.lsts.neptus.plugins;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public abstract class SimpleRendererInteraction extends ConsolePanel implements StateRendererInteraction, Renderer2DPainter {

    private static final long serialVersionUID = 1L;
    protected InteractionAdapter interactionAdapter;
    protected ToolbarSwitch associatedSwitch = null;
    protected boolean active = false;
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
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        interactionAdapter.paintInteraction(g, source);
    }
    
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        interactionAdapter.focusGained(event, source);
    }
    
    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        interactionAdapter.focusLost(event, source);
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        this.active = mode;
    }
    
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        interactionAdapter.mouseExited(event, source);
    }
}
