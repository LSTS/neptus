/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * 22/08/2016
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.coord.PolygonType.Vertex;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
public class PolygonPanel extends ParametersPanel implements StateRendererInteraction {

    private static final long serialVersionUID = -227163768464969072L;
    private StateRenderer2D renderer2d;
    private PolygonType polygon;
    private PolygonType.Vertex vertex = null;
    private InteractionAdapter adapter;
    private JButton btnOk, btnCancel;
    private JDialog dialog;
    private boolean userCancel = false;
    
    private PolygonPanel(PolygonType polygon, MissionType missionType) {
        super();
        
        if (polygon == null)
            polygon = new PolygonType();
        
        this.polygon = polygon;
        
        if (missionType == null) {
            missionType = new MissionType();
            if (!polygon.getVertices().isEmpty()) {
                HomeReference home = new HomeReference();
                home.setLocation(new LocationType(polygon.getVertices().get(0).getLocation()));
                missionType.setHomeRef(home);
            }
        }
        setMissionType(missionType);
        renderer2d = new StateRenderer2D(MapGroup.getMapGroupInstance(getMissionType()));
        adapter = new InteractionAdapter(null);
        initialize();
    }
    
    private PolygonPanel(PolygonType polygon, MissionType missionType, StateRenderer2D renderer) {
        if (polygon == null)
            polygon = new PolygonType();
        if (missionType == null) {
            missionType = new MissionType();
            if (!polygon.getVertices().isEmpty()) {
                HomeReference home = new HomeReference();
                home.setLocation(new LocationType(polygon.getVertices().get(0).getLocation()));
                missionType.setHomeRef(home);
            }
        }
        setMissionType(missionType);
        this.polygon = polygon;
        this.renderer2d = renderer;
        renderer2d.setActiveInteraction(this);
    }
    
    private  void initialize() {
        setLayout(new BorderLayout());
        add(renderer2d, BorderLayout.CENTER);
        btnOk = new JButton(I18n.text("Save"));
        btnCancel = new JButton(I18n.text("Cancel"));
        
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        bottom.add(btnOk);
        btnOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setUserCancel(false);
                if (dialog != null) {
                    dialog.dispose();
                    dialog.setVisible(false);
                }
            }
        });
        bottom.add(btnCancel);
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setUserCancel(true);
                if (dialog != null) {
                    dialog.dispose();
                    dialog.setVisible(false);
                }                
            }
        });
        add(bottom, BorderLayout.SOUTH);
        
        renderer2d.setActiveInteraction(this);
        polygon.recomputePath();        
    }
    
    @Override
    public String getErrors() {
        return null;
    }
    
    @Override
    public Image getIconImage() {
        return adapter.getIconImage();
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
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (!SwingUtilities.isRightMouseButton(event)) {
            adapter.mouseClicked(event, source);
            return;
        }
        
        Vertex v = intercepted(event, source);
        JPopupMenu popup = new JPopupMenu();
        if (v != null) {
            popup.add(I18n.text("Edit location")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LocationType l = new LocationType(v.getLocation());
                    LocationType newLoc = LocationPanel.showLocationDialog(renderer2d, I18n.text("Edit Vertex Location"), l, getMissionType(), true);
                    if (newLoc != null) {
                        newLoc.convertToAbsoluteLatLonDepth();
                        v.setLocation(newLoc);
                        polygon.recomputePath();
                    }                        
                    repaint();                    
                }
            });            
            popup.add(I18n.text("Remove vertex")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    polygon.removeVertex(v);
                    repaint();
                }
            });            
        }
        else
            popup.add(I18n.text("Add vertex")).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LocationType loc = source.getRealWorldLocation(event.getPoint());
                    polygon.addVertex(loc.getLatitudeDegs(), loc.getLongitudeDegs());
                    repaint();
                }
            });
        
        popup.show(source, event.getX(), event.getY());
    }
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        Vertex v = intercepted(event, source);
        if (v == null)
            adapter.mousePressed(event, source);
        else
            vertex = v;
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (vertex == null)
            adapter.mouseDragged(event, source);
        else {
            LocationType loc = source.getRealWorldLocation(event.getPoint());
            vertex.setLocation(loc);
            polygon.recomputePath();     
        }
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
        if (vertex != null)
            polygon.recomputePath();            
        vertex = null;
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
        adapter.setAssociatedSwitch(tswitch);
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
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }

    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);
    }
    
    public PolygonType.Vertex intercepted(MouseEvent evt, StateRenderer2D source) {
        for (PolygonType.Vertex v : polygon.getVertices()) {
            Point2D pt = source.getScreenPosition(new LocationType(v.getLocation()));
            if (pt.distance(evt.getPoint()) < 5) {
                return v;
            }
        }
        return null;
    }

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        g.setTransform(source.getIdentity());
        polygon.paint(g, source);        
        polygon.getVertices().forEach(v -> {
            Point2D pt = source.getScreenPosition(new LocationType(v.getLocation()));
            Ellipse2D ellis = new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10);
            Color c = Color.yellow;
            g.setColor(new Color(255-c.getRed(),255-c.getGreen(),255-c.getBlue(),200));
            g.fill(ellis);
            g.setColor(c);
            g.draw(ellis);
        });
    }
    
    public boolean isUserCancel() {
        return userCancel;
    }

    public void setUserCancel(boolean userCancel) {
        this.userCancel = userCancel;
    }
    
    private JDialog getDialog(Component parent, String title) {
        
        if (parent instanceof Window)
            dialog = new JDialog((Window)parent);
        else
            dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        
        dialog.setTitle(title);
        dialog.setSize(500, 500);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setAlwaysOnTop(false);
        GuiUtils.centerOnScreen(dialog);
        dialog.setResizable(false);
        
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                polygon = null;
                setUserCancel(true);
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        dialog.setVisible(true);
        return dialog;
    }
    
    public static PolygonType showPolygonDialog(Component parent, String title, PolygonType previous, MissionType mt, boolean editable) {
        PolygonType polygon = previous.clone();
        PolygonPanel panel = new PolygonPanel(polygon, mt);
        panel.setEditable(editable);
        if (parent == null)
            panel.getDialog(ConfigFetch.getSuperParentAsFrame(), title);
        else
            panel.getDialog(parent, title);
        
        if (panel.isUserCancel())
            return null;
        return panel.polygon;
    }
    
    public static void main(String[] args) {
        PolygonType pt = new PolygonType();
        pt.addVertex(41.180293, -8.701072);
        pt.addVertex(41.183136, -8.703647);
        pt.addVertex(41.181198, -8.706651);
        PolygonPanel.showPolygonDialog(null, "Edit polygon", pt, null, true);
    }
}
