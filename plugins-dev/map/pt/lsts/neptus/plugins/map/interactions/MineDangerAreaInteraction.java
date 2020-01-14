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
 * Nov 15, 2011
 */
package pt.lsts.neptus.plugins.map.interactions;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import javax.swing.undo.UndoManager;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.plugins.map.edit.AddObjectEdit;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MineDangerAreaElement;
import pt.lsts.neptus.util.GuiUtils;

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
        if (event.isControlDown())
            element.filled = false;
        
        pivot.addObject(element);
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

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        double r = element != null ? Math.abs(element.radius) : 0;
        g2.setColor(Color.red.darker());
        g2.setFont(new Font("Helvetica", Font.BOLD, 14));
        g2.drawString(I18n.textf("Radius: %radius m",GuiUtils.getNeptusDecimalFormat(1).format(r)), 55, 20);
        g2.dispose();
    }
}
