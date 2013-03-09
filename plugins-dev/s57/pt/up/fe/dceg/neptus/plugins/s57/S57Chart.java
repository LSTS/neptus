/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo Dias
 * 16/12/2011
 */
package pt.up.fe.dceg.neptus.plugins.s57;

import java.awt.Graphics2D;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JDialog;

import pt.up.fe.dceg.neptus.plugins.MapTileProvider;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.tiles.MapPainterProvider;
import pt.up.fe.dceg.neptus.s57.ApplicationFactory;
import pt.up.fe.dceg.neptus.s57.S57;
import pt.up.fe.dceg.neptus.s57.S57Painter;
import pt.up.fe.dceg.neptus.s57.mc.MarinerControls;
import pt.up.fe.dceg.neptus.s57.ui.OptionsDialog;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;

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
        this.s57 = ApplicationFactory.build(true, true);
        this.mc = this.s57.newMarinerControls();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     * pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
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
