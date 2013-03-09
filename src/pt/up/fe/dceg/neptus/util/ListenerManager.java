/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.util;

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
