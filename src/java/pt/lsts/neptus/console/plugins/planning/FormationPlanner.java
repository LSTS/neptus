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
 * 28 de Mai de 2011
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.mp.maneuvers.VehicleFormation;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.CustomInteractionSupport;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;

/**
 * This Neptus plugin provides an interface for Vehicle Formation (cooperative) plans<br/>
 * It is heavily related to the <b>VehicleFormation</b> IMC message
 * 
 * @author ZP
 */
@PluginDescription(author = "zp", name = "Formation Planner", description = "This Neptus plugin provides an interface for Vehicle Formation (cooperative) plans")
@LayerPriority(priority = 50)
public class FormationPlanner extends ConsolePanel implements Renderer2DPainter, StateRendererInteraction {
    private static final long serialVersionUID = 1L;
    protected InteractionAdapter adapter = new InteractionAdapter(null);
    
    /**
     * Object holding all the parameters for vehicle formation
     */
    protected VehicleFormation model = new VehicleFormation();

    /**
     * @param console
     */
    public FormationPlanner(ConsoleLayout console) {
        super(console);
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

        if (event.getButton() == MouseEvent.BUTTON3) {
            NeptusLog.pub().info("<###>show popup...");
            JPopupMenu popup = new JPopupMenu();
            popup.add(new AbstractAction("Set participants") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    // TODO Auto-generated method stub

                }
            });

            popup.add(new AbstractAction("Edit trajectory") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    // TODO Auto-generated method stub

                }
            });

            popup.addSeparator();

            popup.add(new AbstractAction("Start formation") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent arg0) {

                }
            });

            popup.show(source, event.getX(), event.getY());

        }
        else if (event.getClickCount() == 2) {
            NeptusLog.pub().info("<###>add point...");
            // TODO
        }
        else
            adapter.mouseClicked(event, source);
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        adapter.mouseDragged(event, source);
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        NeptusLog.pub().info("<###>mouse moved");
        adapter.mouseMoved(event, source);
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
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);

        if (mode)
            source.addPostRenderPainter(this, "Formation Planner");
        else
            source.removePostRenderPainter(this);

    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        AffineTransform plain = new AffineTransform(g.getTransform());
        // TODO view and edit trajectory points

        g.setTransform(plain);
        g.setColor(Color.white);
        g.drawString(model.getParticipants().size() + " participants", 8, 16);
        g.setColor(Color.black);
        g.drawString(model.getParticipants().size() + " participants", 7, 15);
    }

    protected boolean inited = false;

    @Override
    public void initSubPanel() {
        if (!inited) {
            Vector<CustomInteractionSupport> panels = getConsole().getSubPanelsOfInterface(
                    CustomInteractionSupport.class);
            for (CustomInteractionSupport cis : panels)
                cis.addInteraction(this);

            // Vector<ILayerPainter> renders = getConsole().getSubPanelImplementations(new ILayerPainter[0]);

            // for (ILayerPainter str2d : renders) {
            // str2d.addPostRenderPainter(this, "Formation Planner");
            // }
            inited = true;
        }
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
