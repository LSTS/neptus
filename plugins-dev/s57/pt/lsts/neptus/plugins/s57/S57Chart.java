/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Hugo Dias
 * 16/12/2011
 */
package pt.lsts.neptus.plugins.s57;

import java.awt.Graphics2D;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JDialog;

import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.tiles.MapPainterProvider;
import pt.lsts.neptus.s57.S57;
import pt.lsts.neptus.s57.S57Factory;
import pt.lsts.neptus.s57.S57Painter;
import pt.lsts.neptus.s57.mc.MarinerControls;
import pt.lsts.neptus.s57.ui.OptionsDialog;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * @author Hugo Dias
 * @author Paulo Dias
 * 
 */
@MapTileProvider(name = "S57 Charts", usePropertiesOrCustomOptionsDialog = false, makeCustomOptionsDialogIndependent = true)
public class S57Chart implements MapPainterProvider {

    private final S57 s57;
    private final MarinerControls mc;
    
    private final Map<StateRenderer2D, S57Painter> painterList = new ConcurrentHashMap<StateRenderer2D, S57Painter>();
    
    public S57Chart() {
        this.s57 = S57Factory.build();
        this.mc = MarinerControls.forge();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     * pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        S57Painter painterToUse = painterList.get(renderer);
        if (painterToUse == null){
           painterToUse = s57.newPainter(mc);
           painterList.put(renderer, painterToUse);
        }
        painterToUse.paint(g, renderer);
    }

    public JDialog getOptionsDialog(JDialog parent, final StateRenderer2D renderer) {
        @SuppressWarnings("serial")
        OptionsDialog dialog = new OptionsDialog(s57, mc) {
            @Override
            public void dispose() {
                super.dispose();
                S57Painter painterToRemove = painterList.get(renderer);
                if (painterToRemove != null)
                    s57.removePainter(painterToRemove);
            }
        };
        dialog.setIconImages(ConfigFetch.getIconImagesForFrames());
        if (renderer != null) {
            dialog.setSrend(renderer);
            painterList.put(renderer, s57.newPainter(mc));
        }
        return dialog;
    }

    public static int getMaxLevelOfDetail() {
        return MapTileUtil.LEVEL_MAX;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        for (S57Painter pt : painterList.values().toArray(new S57Painter[0])) {
            s57.removePainter(pt);
        }
    }
}
