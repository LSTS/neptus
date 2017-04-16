/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 15/04/2017
 */
package pt.lsts.neptus.mp.interactions;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.function.Predicate;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.RendezvousPoints;
import pt.lsts.neptus.mp.RendezvousPoints.Point;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 *
 */
public class EmergencyRendezvousPointInteraction implements StateRendererInteraction {

    private RendezvousPoints originalPoints = null;
    private RendezvousPoints copyPoints = null;
    
    protected InteractionAdapter adapter = new InteractionAdapter(null);
    private RendezvousPoints.Point selPoint = null;
    
    public EmergencyRendezvousPointInteraction(RendezvousPoints points) {
        this.originalPoints = points;
        this.copyPoints = RendezvousPoints.loadXml(this.originalPoints.asXml());
        this.copyPoints.setEditing(true);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getName()
     */
    @Override
    public String getName() {
        return "Emergency Rendezvous Point";
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getIconImage()
     */
    @Override
    public Image getIconImage() {
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#getMouseCursor()
     */
    @Override
    public Cursor getMouseCursor() {
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        return true;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseClicked(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        Point2D mousePoint = event.getPoint();
        if (SwingUtilities.isRightMouseButton(event)) {
            JPopupMenu popupMenu = createPopUpMenu(event, source);
            popupMenu.show(source, (int) mousePoint.getX(), (int) mousePoint.getY());
        }
        else {
            adapter.mouseClicked(event, source);
        }
    }

    private JPopupMenu createPopUpMenu(MouseEvent event, StateRenderer2D source) {
        final StateRenderer2D renderer = source;
        final Point2D mousePoint = event.getPoint();
        
        JPopupMenu popup = new JPopupMenu();
        Point selPoint = getClickedPoint(source, event);
        if (selPoint  == null) {
            @SuppressWarnings("serial")
            AbstractAction addMenu = new AbstractAction(I18n.text("Add point")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LocationType locClick = renderer.getRealWorldLocation(mousePoint);
                    copyPoints.addPoint(locClick);
                }
            };
            popup.add(addMenu);
        }
        else {
            @SuppressWarnings("serial")
            AbstractAction remMenu = new AbstractAction(I18n.text("Remove point")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    copyPoints.removePoint(selPoint);
                }
            };
            popup.add(remMenu);
            @SuppressWarnings("serial")
            AbstractAction copyMenu = new AbstractAction(I18n.text("Copy point")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CoordinateUtil.copyToClipboard(selPoint.asLocation());
                }
            };
            copyMenu.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/editcopy.png")));
            popup.add(copyMenu);
        }
        return popup;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mousePressed(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        selPoint = getClickedPoint(source, event);

        adapter.mousePressed(event, source);
    }

    /**
     * @param source
     * @param event
     * @return 
     */
    private Point getClickedPoint(StateRenderer2D source, MouseEvent event) {
        Predicate<Point> predicate = new Predicate<Point>() {
            @Override
            public boolean test(Point point) {
                Point2D sPt = source.getScreenPosition(point.asLocation());
                return event.getPoint().distance(sPt) < 15;
            };
        };
        return copyPoints.getPoints().stream().filter(predicate).findFirst().orElse(null);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseDragged(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (selPoint != null) {
            LocationType locClick = source.getRealWorldLocation(event.getPoint());
            selPoint.setLatDeg(locClick.getLatitudeDegs());
            selPoint.setLonDeg(locClick.getLongitudeDegs());
            copyPoints.triggerChange();
        }
        else {
            adapter.mouseDragged(event, source);
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseMoved(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseExited(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#mouseReleased(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        selPoint = null;
        adapter.mouseReleased(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#wheelMoved(java.awt.event.MouseWheelEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#setAssociatedSwitch(pt.lsts.neptus.gui.ToolbarSwitch)
     */
    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyPressed(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyReleased(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#keyTyped(java.awt.event.KeyEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#focusLost(java.awt.event.FocusEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#focusGained(java.awt.event.FocusEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#setActive(boolean, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#paintInteraction(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        copyPoints.paint(g, source);
    }
}
