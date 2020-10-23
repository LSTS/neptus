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
 * May 14, 2018
 */
package pt.lsts.neptus.soi;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.wms.WmsFrameGrabber;

/**
 * @author zp
 *
 */
public class WmsLayer implements Renderer2DPainter {

    private BufferedImage img = null;
    private LocationType topLeft = null, bottomRight = null;
    private boolean loading = false;
    private String baseUrl;
    private Map<String, String> options = new LinkedHashMap<>();
    private float opacity = 0.8f;
    private Thread loader = null;

    
    public WmsLayer(String baseUrl, Map<String, String> options) {
        this.baseUrl = baseUrl;
        this.options.putAll(options);
    }

    private void refreshImage(StateRenderer2D renderer) {
        loading = true;
        img = null;
        final LocationType tl = renderer.getRealWorldLocation(new Point(0, 0)).convertToAbsoluteLatLonDepth();
        final LocationType br = renderer.getRealWorldLocation(new Point(renderer.getWidth(), renderer.getHeight()))
                .convertToAbsoluteLatLonDepth();
        String bbox = "" + tl.getLongitudeDegs() + "," + br.getLatitudeDegs() + ","
                + br.getLongitudeDegs() + "," + tl.getLatitudeDegs();
        if (loader != null)
            loader.interrupt();

        loader = new Thread("wms loader") {
            public void run() {
                img = WmsFrameGrabber.wmsFetch(baseUrl, options, "BBOX", bbox, "WIDTH",
                        "" + renderer.getWidth(), "HEIGHT", "" + renderer.getHeight());
                topLeft = tl;
                bottomRight = br;
                loading = false;
                loader = null;
            };
        };
        loader.start();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (!loading && (!renderer.getRealWorldLocation(new Point(0, 0)).equals(topLeft) || !renderer
                .getRealWorldLocation(new Point(renderer.getWidth(), renderer.getHeight())).equals(bottomRight))) {
            refreshImage(renderer);
        }

        if (img == null && !loading) {
            refreshImage(renderer);
        }

        if (loading) {
            g.setColor(Color.black);
            g.drawString("loading", 20, 30);
        }
        else {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g.drawImage(img, 0, 0, renderer.getWidth(), renderer.getHeight(), 0, 0, img.getWidth(), img.getHeight(),
                    null);
        }
    }

    public static void main(String[] args) {
        StateRenderer2D renderer2d = new StateRenderer2D();
        renderer2d.getRenderer().addPostRenderPainter(WmsLayerFactory.Wind(), "wind");
        GuiUtils.testFrame(renderer2d, "gg");
    }
}
