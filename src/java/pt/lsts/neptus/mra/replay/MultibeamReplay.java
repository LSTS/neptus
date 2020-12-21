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
 * Author: José Correia
 * Jul 5, 2012
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 * @author hfq
 */
@PluginDescription(name = "Multibeam Replay", icon = "pt/lsts/neptus/mra/replay/echosounder.png")
public class MultibeamReplay implements LogReplayLayer {

    private BufferedImage img;

    private Point2D pos;

    private final ColorMap cm = ColorMapFactory.createJetColorMap();

    private BathymetryParser parser;

    private IMraLogGroup source;

    private final int baseLod = 18;
    private static final String MB_IMG_FILE_PATH = "mra/multibeam.png";
    private static final String FILE_83P_EXT = ".83P";

    private boolean isFirstPaint = true;

    @Override
    public void cleanup() {
        img = null;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (isFirstPaint) {
            isFirstPaint = false;
            if (source.getFile(MB_IMG_FILE_PATH) != null) {
                if (img == null) {
                    try {
                        NeptusLog.pub().info("Loading " + MB_IMG_FILE_PATH);
                        img = ImageIO.read(source.getFile(MB_IMG_FILE_PATH));
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (source.getFile(MB_IMG_FILE_PATH) == null) {
                final Graphics2D graph = g;
                final StateRenderer2D rend = renderer;

                Thread t = new Thread(MultibeamReplay.class.getSimpleName() + " " + source.getDir().getParent()) {

                    @Override
                    public void run() {
                        // NeptusLog.pub().info("Top left:" + parser.getBathymetryInfo().topLeft);
                        // NeptusLog.pub().info("bottom left: " + parser.getBathymetryInfo().bottomRight);
                        double res[] = parser.getBathymetryInfo().topLeft.getDistanceInPixelTo(
                                parser.getBathymetryInfo().bottomRight, baseLod);
                        // NeptusLog.pub().info("Resolution : " + res[0] + " " + res[1]);
                        // Create and paint image
                        img = new BufferedImage((int) res[0], (int) res[1], BufferedImage.TYPE_INT_ARGB);
                        parser.rewind();
                        BathymetrySwath swath;
                        while ((swath = parser.nextSwath(1)) != null) {
                            LocationType loc = swath.getPose().getPosition();

                            for (BathymetryPoint bp : swath.getData()) {
                                LocationType loc2 = new LocationType(loc);
                                if (bp == null)
                                    continue;

                                loc2.translatePosition(bp.north, bp.east, 0);

                                double dist[] = parser.getBathymetryInfo().topLeft.getDistanceInPixelTo(loc2, baseLod);

                                if (dist[0] > 0 && dist[1] > 0 && dist[0] < img.getWidth() && dist[1] < img.getHeight()) {
                                    img.setRGB((int) dist[0], (int) dist[1],
                                            cm.getColor(1 - (bp.depth / parser.getBathymetryInfo().maxDepth)).getRGB());
                                }
                            }
                            addImageToRender(graph, rend);
                            graph.dispose();
                            rend.repaint();
                        }


                        try {
                            NeptusLog.pub().info(
                                    "Recording " + source.getFile("Data.lsf").getParent() + "/" + MB_IMG_FILE_PATH);
                            ImageIO.write(img, "PNG", new File(source.getFile("Data.lsf").getParent() + "/"
                                    + MB_IMG_FILE_PATH));
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        addImageToRender(graph, rend);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
            addImageToRender(g, renderer);
        }
        else {
            addImageToRender(g, renderer);
        }
    }

    /**
     * @param g
     * @param renderer
     */
    private void addImageToRender(Graphics2D g, StateRenderer2D renderer) {
        pos = renderer.getScreenPosition(parser.getBathymetryInfo().topLeft);

        int difLod = renderer.getLevelOfDetail() - baseLod;
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.translate(pos.getX(), pos.getY());
        g2d.rotate(-renderer.getRotation());
        g2d.scale(Math.pow(2, difLod), Math.pow(2, difLod));
        g2d.drawImage(img, 0, 0, null);

        for (int i = 0; i < 200; i++) {
            double val = parser.getBathymetryInfo().maxDepth * i / 200.0;
            g.setColor(cm.getColor(1 - (val / parser.getBathymetryInfo().maxDepth)));
            g.drawRect(10, 100 + i, 10, 1);
            if (i % 50 == 0 || i == 0 || i == 200 - 1) {
                g.setColor(Color.black);
                g.drawString("" + Math.round(val) + "m", 30, 100 + i);
            }
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        if(source.getLog("EstimatedState") == null)
            return false;

        boolean canBeApplied = false;
        if (source.getDir().isDirectory()) {
            for (File temp : source.getDir().listFiles()) {
                if((temp.toString()).endsWith(FILE_83P_EXT)) {
                    canBeApplied = true;
                }
            }
        }
        return canBeApplied;
        // return source.getLog("EstimatedState") != null && BathymetryParserFactory.build(source) != null;
    }

    @Override
    public String getName() {
        return I18n.text("Multibeam Layer");
    }

    @Override
    public void parse(IMraLogGroup source) {

        parser = new DeltaTParser(source);
        this.source = source;
    }

    @Override
    public String[] getObservedMessages() {
        return null;
    }

    @Override
    public void onMessage(IMCMessage message) {
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }
}
