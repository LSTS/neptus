/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.util;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.JComponent;

/**
 * This class manages a JComponent (Subpanel) listeners for edit mode purposes 
 * @author rjpg
 * 
 */
public class ListenerManager {

    private Vector<MouseListener> mouselisteners = new Vector<MouseListener>();
    private Vector<MouseMotionListener> mousemotionlisteners = new Vector<MouseMotionListener>();
    private Vector<KeyListener> keyboardlisteners = new Vector<KeyListener>();
    private Vector<ListenerManager> childs = new Vector<ListenerManager>();

    private JComponent component;

    public ListenerManager(JComponent comp) {

        setComponent(comp);
    }

    public void setComponent(JComponent comp) {
        component = comp;

        for (MouseListener ml : component.getMouseListeners()) {
            mouselisteners.add(ml);
        }
        for (MouseMotionListener mml : component.getMouseMotionListeners()) {
            mousemotionlisteners.add(mml);
        }

        for (KeyListener kl : component.getKeyListeners()) {
            keyboardlisteners.add(kl);
        }

        for (Component child : component.getComponents()) {
            if (child instanceof JComponent) {
                childs.add(new ListenerManager((JComponent) child));
            }
        }
    }

    public void turnoff() {
        for (Object ml : mouselisteners.toArray()) {
            component.removeMouseListener((MouseListener) ml);

        }
        for (Object mml : mousemotionlisteners.toArray()) {
            component.removeMouseMotionListener((MouseMotionListener) mml);
        }

        for (Object kl : keyboardlisteners.toArray()) {
            component.removeKeyListener((KeyListener) kl);
        }

        // childs.toArray()
        for (Object child : childs.toArray()) {
            if (child instanceof ListenerManager) {
                ((ListenerManager) child).turnoff();
            }
        }
    }

    public void turnon() {
        for (Object ml : mouselisteners.toArray()) {
            component.addMouseListener((MouseListener) ml);

        }
        for (Object mml : mousemotionlisteners.toArray()) {
            component.addMouseMotionListener((MouseMotionListener) mml);
        }

        for (Object kl : keyboardlisteners.toArray()) {
            component.addKeyListener((KeyListener) kl);
        }
        for (Object child : childs.toArray()) {
            if (child instanceof ListenerManager) {
                ((ListenerManager) child).turnon();
            }
        }

    }

}
