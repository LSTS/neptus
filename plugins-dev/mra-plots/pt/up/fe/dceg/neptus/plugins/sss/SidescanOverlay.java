/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 13, 2012
 */
package pt.up.fe.dceg.neptus.plugins.sss;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.util.Vector;

import org.imgscalr.Scalr;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.replay.LogReplayLayer;
import pt.up.fe.dceg.neptus.plugins.sidescan.SlantRangeImageFilter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

/**
 * @author zp
 * 
 */
public class SidescanOverlay implements LogReplayLayer {

    File dir = null;

    public void cleanup() {

    };

    public String getName() {
        return "Sidescan Layer";
    };

    public String[] getObservedMessages() {
        return null;
    };

    public boolean getVisibleByDefault() {
        return false;
    };

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("SonarData") != null;
    }

    @Override
    public void onMessage(IMCMessage message) {
    }

    boolean producing = false;
    Vector<SidescanTile> tiles = null;

    @Override
    public void parse(final IMraLogGroup source) {
        if (source.getFile("sss") == null) {
            File f = new File(source.getFile("."), "sss");
            f.mkdir();            
        }

        dir = new File(source.getFile("."), "sss");

        if (source.getFile("sss/tiles.generated") == null) {

            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    producing = true;
                    createTiles(source);
                    try {
                        FileWriter fw = new FileWriter(new File(source.getFile("sss"), "tiles.generated"));
                        fw.write("done!");
                        fw.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    producing = false;
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    protected boolean createTiles(IMraLogGroup source) {
        File outputDir = source.getFile("sss");
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

        else if (tiles == null) {
            System.out.println("loading tiles...");
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

            System.out.println("loaded "+tiles.size()+" tiles");
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
