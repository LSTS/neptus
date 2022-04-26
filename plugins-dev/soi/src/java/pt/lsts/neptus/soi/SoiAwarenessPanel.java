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
 * May 10, 2018
 */
package pt.lsts.neptus.soi;

import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.SimpleMapPanel;
import pt.lsts.neptus.console.plugins.SystemsList;
import pt.lsts.neptus.endurance.SoiAwareness;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

/**
 * @author zp
 *
 */
@PluginDescription(name="SOI Awareness Panel")
@Popup(pos = POSITION.CENTER, width = 600, height = 600, accelerator = 'I')
public class SoiAwarenessPanel extends SimpleMapPanel implements Renderer2DPainter {

    private static final long serialVersionUID = 1L;
    private SoiAwareness interaction;
    
    public SoiAwarenessPanel(ConsoleLayout console) {
        super(console);
        interaction = new SoiAwareness();
        interaction.init(console);
    }

    @Override
    public void initSubPanel() {
        super.initSubPanel();
        renderer.addPostRenderPainter(this, "SoiAwarenessPanel");
        Vector<SystemsList> sl = getConsole().getSubPanelsOfClass(SystemsList.class);
        renderer.addPostRenderPainter(sl.firstElement(), "SystemsList");
        renderer.setActiveInteraction(interaction);  
        interaction.setActive(true, renderer);
        
    }
    
    @Override
    public DefaultProperty[] getProperties() {
        List<DefaultProperty> props = Arrays.asList(interaction.getProperties());
        props.addAll(Arrays.asList(super.getProperties()));
        return props.toArray(new DefaultProperty[0]);
    }
    
    @Override
    public void setProperties(Property[] properties) {
        interaction.setProperties(properties);
        super.setProperties(properties);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        SoiStateRenderer.paintStatic(g, renderer);
        
    }
}
