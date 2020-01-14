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
 * May 11, 2010
 */
package pt.lsts.neptus.renderer2d;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import pt.lsts.neptus.gui.ToolbarSwitch;

/**
 * @author zp
 *
 */
public interface StateRendererInteraction {

    public String getName();	
    public Image getIconImage();		
    public Cursor getMouseCursor();
    public boolean isExclusive();

    public void mouseClicked(MouseEvent event, StateRenderer2D source);
    public void mousePressed(MouseEvent event, StateRenderer2D source);
    public void mouseDragged(MouseEvent event, StateRenderer2D source);
    public void mouseMoved(MouseEvent event, StateRenderer2D source);
    public void mouseExited(MouseEvent event, StateRenderer2D source);
    public void mouseReleased(MouseEvent event, StateRenderer2D source);
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source);
    public void setAssociatedSwitch(ToolbarSwitch tswitch);
    public void keyPressed(KeyEvent event, StateRenderer2D source);
    public void keyReleased(KeyEvent event, StateRenderer2D source);
    public void keyTyped(KeyEvent event, StateRenderer2D source);
    public void focusLost(FocusEvent event, StateRenderer2D source);
    public void focusGained(FocusEvent event, StateRenderer2D source);
    public void setActive(boolean mode, StateRenderer2D source);
    public void paintInteraction(Graphics2D g, StateRenderer2D source);
}
