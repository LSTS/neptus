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
 * Author: Hugo Dias
 * 16/12/2011
 */
package pt.lsts.neptus.plugins.s57;

import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JDialog;

import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.tiles.MapPainterProvider;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.coord.MapTileUtil;
import pt.lsts.s57.S57;
import pt.lsts.s57.S57Factory;
import pt.lsts.s57.S57Query;
import pt.lsts.s57.S57Utils;
import pt.lsts.s57.entities.S57Object;
import pt.lsts.s57.mc.MarinerControls;
import pt.lsts.s57.painters.NeptusS57Painter;
import pt.lsts.s57.ui.MarinerControlsOptionsPanel;
import pt.lsts.s57.ui.OptionsDialog;
import pt.lsts.s57.ui.S57OptionsPanel;

/**
 * @author Hugo Dias
 * @author Paulo Dias
 * 
 */
@MapTileProvider(name = "S57 Charts", usePropertiesOrCustomOptionsDialog = false, makeCustomOptionsDialogIndependent = true)
public class S57Chart implements MapPainterProvider {

    private final S57 s57;
//    private S63 s63;
    private final MarinerControls mc;

    private final Map<StateRenderer2D, NeptusS57Painter> painterList = new ConcurrentHashMap<StateRenderer2D, NeptusS57Painter>();

    public S57Chart() {
        File cacheFile = new File(System.getProperty("user.dir") + "/.cache/s57");
        cacheFile.mkdirs();
        this.s57 = S57Factory.build(cacheFile, new File("libJNI/gdal/" + S57Utils.getPlatformPath()));
//        try {
//            this.s63 = S63.forge(this.s57);
//        }
//        catch (NoClassDefFoundError e) {
//            this.s63 = null;
//        }
        this.mc = MarinerControls.forge();
        
        S57Utils.loadSession(s57);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        NeptusS57Painter painterToUse = painterList.get(renderer);
        if (painterToUse == null) {
            painterToUse = NeptusS57Painter.forge(s57, mc);
            s57.addPainter(painterToUse);
            painterList.put(renderer, painterToUse);
        }
        painterToUse.paint(g, renderer);
    }

    public JDialog getOptionsDialog(JDialog parent, final StateRenderer2D renderer) {
        @SuppressWarnings("serial")
        OptionsDialog dialog = new OptionsDialog() {
            @Override
            public void dispose() {
                super.dispose();
                NeptusS57Painter painterToRemove = painterList.get(renderer);
                if (painterToRemove != null) {
                    s57.removePainter(painterToRemove);
                    painterList.remove(renderer);
                }
            }
        };
        dialog.setIconImages(ConfigFetch.getIconImagesForFrames());
        dialog.addTab("S57", dialog.getIconMedium("icons/location.png"), new S57OptionsPanel(s57, dialog), "Settings for S57 Maps");
//        if(this.s63 != null) 
//            dialog.addTab("S63", dialog.getIconMedium("icons/location2.png"), new S63OptionsPanel(s57, s63, dialog), "Settings for S63 Maps");
        dialog.addTab("Mariner Controls", dialog.getIconMedium("icons/cog2.png"), new MarinerControlsOptionsPanel(mc,dialog), "Mariner Controls");
        dialog.setRenderer(renderer);
        // painter for this renderer
        NeptusS57Painter painterToUse = painterList.get(renderer);
        if(painterToUse == null){
            painterList.put(renderer, NeptusS57Painter.forge(s57, mc));
        }

        return dialog;
    }

    /**
     * Returns all the found depth soundings in the loaded S57 maps
     * @param latMinDegs Latitude minimum degrees (for bounding box)
     * @param latMaxDegs Latitude maximum degrees (for bounding box)
     * @param lonMinDegs Longitude minimum degrees (for bounding box)
     * @param lonMaxDegs Longitude maximum degrees (for bounding box)
     * @return
     */
    public Collection<LocationType> getDepthSoundings(double latMinDegs, double latMaxDegs, double lonMinDegs, double lonMaxDegs) {
        List<S57Object> objs = S57Query.forge(s57).findObjectsInside(latMaxDegs, lonMinDegs, latMinDegs,lonMaxDegs, new String[] { "SOUNDG"});
        ArrayList<LocationType> locs = new ArrayList<>();
        for (S57Object obj : objs) {
            LocationType loc = obj.getGeometry().getCenter();
            loc.setDepth(obj.getGeometry().getDepth());
            locs.add(loc);
        }
        
        return locs;    
    }
    
    public static int getMaxLevelOfDetail() {
        return MapTileUtil.LEVEL_MAX;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        for (NeptusS57Painter pt : painterList.values().toArray(new NeptusS57Painter[0])) {
            s57.removePainter(pt);
        }
    }
}
