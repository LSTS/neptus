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
 * Author: José Pinto
 * Dec 13, 2012
 */
package pt.lsts.neptus.plugins.sidescan;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.util.Vector;

import org.imgscalr.Scalr;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarData;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.LogReplayLayer;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.sidescan.SlantRangeImageFilter;

/**
 * @author zp
 * 
 */
public class SidescanOverlay implements LogReplayLayer {

    File dir = null;

    public void cleanup() {

    };

    public String getName() {
        return I18n.text("Sidescan Layer");
    };

    public String[] getObservedMessages() {
        return null;
    };

    public boolean getVisibleByDefault() {
        return false;
    };

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLog("SonarData") != null;
    }

    @Override
    public void onMessage(IMCMessage message) {
    }

    boolean producing = false;
    Vector<SidescanTile> tiles = null;

    protected void loadTiles() {
        NeptusLog.pub().info("loading tiles...");
        tiles = new Vector<>();
        for (File f : dir.listFiles()) {
            if (!f.getName().endsWith(".tile"))
                continue;
            try {
                tiles.add(new SidescanTile(f));
            }
            catch (Exception e) {
                e.printStackTrace();
            }                            
        }        
        NeptusLog.pub().info("loaded "+tiles.size()+" tiles");
    }
    
    protected void generateTiles(final IMraLogGroup source) {
        NeptusLog.pub().info("generating tiles...");
        createTiles(source);
        try {
            FileWriter fw = new FileWriter(new File (source.getFile("mra"), "/sss/tiles.generated"));
            fw.write("done!");
            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void parse(final IMraLogGroup source) {
        if (source.getFile("mra/sss") == null) {
            File f = new File(source.getFile("mra"), "sss");
            f.mkdirs();
        }

        dir = source.getFile("mra/sss");

        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                producing = true;
                if (source.getFile("mra/sss/tiles.generated") == null)
                    generateTiles(source);
                loadTiles();                    
                producing = false;
            }
        });
        t.setName("Sidescan Overlay Tile loader");
        t.setDaemon(true);
        t.start();

    }

    protected boolean createTiles(IMraLogGroup source) {
        File outputDir = source.getFile("mra/sss");
        LsfIndex index = source.getLsfIndex();
        int tileHeight = 20;
        BufferedImage curImage = null;
        ColorMap bronze = ColorMapFactory.createBronzeColormap();
        int tileNumber = 1;
        int tileH = 0;
        IMCMessage start = null, end = null;
        double curSpeedSum = 0;
        double curAltSum = 0;
        for (int i = index.getFirstMessageOfType("SonarData"); i != -1; i = index.getNextMessageOfType("SonarData", i)) {
            try {
                SonarData ping = index.getMessage(i, SonarData.class);
                int stateIndex = index.getMsgIndexAt("EstimatedState", index.timeOf(i));
                IMCMessage state = null;
                if (stateIndex != -1) {
                    state = index.getMessage(stateIndex);
                    curSpeedSum += state.getDouble("u");
                    curAltSum += state.getDouble("alt");
                }
                if (ping.getType() != SonarData.TYPE.SIDESCAN)
                    continue;
                byte[] data = ping.getData();
                if (curImage == null || tileH >= tileHeight) {
                    if (tileH == tileHeight) {
                        end = state;

                        double length = curSpeedSum / (tileH-2);
                        double alt = curAltSum / tileH;     
                        curSpeedSum = curAltSum = 0;
                        
                        //slant range correction
                        curImage = Scalr.apply(curImage, new SlantRangeImageFilter(alt, ping.getMaxRange(), curImage.getWidth()));
                        //length *= (end.getTimestamp() - start.getTimestamp());
                        
                        LocationType startLoc = IMCUtils.getLocation(start);
                        LocationType endLoc = IMCUtils.getLocation(end);
                        if (length > 0 && Math.abs(endLoc.getDistanceInMeters(startLoc) - length) < 5) {
                            SidescanTile tile = new SidescanTile(curImage, startLoc, ping.getMaxRange()*2,
                                    start.getDouble("psi"), endLoc.getDistanceInMeters(startLoc));
                            tile.saveToFile(new File(outputDir, "tile" + tileNumber + ".tile"));                        
                            tileNumber++;
                        }
                    }
                    start = state;

                    tileH = 0;
                    if (ping.getBitsPerPoint() == 16)
                        curImage = new BufferedImage(data.length / 2, tileHeight, BufferedImage.TYPE_INT_ARGB);
                    else if (ping.getBitsPerPoint() == 8)
                        curImage = new BufferedImage(data.length / 2, tileHeight, BufferedImage.TYPE_INT_ARGB);
                    else {
                        System.err.println("the number of bits per point of this sidescan is not supported.");
                        return false;
                    }
                }

                if (ping.getBitsPerPoint() == 8) {
                    for (int x = 0; x < data.length; x += 2)
                        curImage.setRGB(x / 2, tileHeight - tileH - 1,
                                bronze.getColor(((data[x] & 0xFF) + (data[x + 1] & 0xFF)) / 510.0).getRGB());
                }
                
                tileH++;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (producing) {
            g.setColor(Color.red.darker());
            g.drawString("Sidescan layer is processing...", 60, 20);
        }      
        else {
            for (SidescanTile t : tiles) {
                Point2D p = renderer.getScreenPosition(t.getBottomCenter());
                AffineTransform tr = new AffineTransform();
                tr.translate(p.getX(), p.getY());
                tr.rotate(t.rotation-renderer.getRotation());
                
                tr.scale(renderer.getZoom()*(t.width/300), renderer.getZoom()*(t.width/300));
                tr.translate(-150, 0);
                g.drawImage(t.getTile(), tr, null);
            }
        }
    }
}
