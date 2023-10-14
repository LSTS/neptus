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
 * Author: José Correia
 * May 18, 2012
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.coord.MapTileUtil;
import pt.lsts.neptus.util.sidescan.SideScanComposite;

/**
 * @author jqcorreia
 * 
 */
@PluginDescription(icon = "pt/lsts/neptus/mra/replay/echosounder.png")
public class SidescanReplay implements LogReplayLayer {

    private final List<SidescanData> dataSet = new ArrayList<SidescanData>();
    private float range = 0;

    private BufferedImage image;
    private double imageScaleX;

    private boolean generate = true;
    private int lod;
    private double top = 0, bot = 0, left = 0, right = 0;

    private LocationType topleftLT;
    private LocationType botrightLT;

    private LocationType lastCenter = new LocationType();

    private IMraLogGroup source;

    @Override
    public void cleanup() {
        image = null;
        dataSet.clear();
    }

    protected void generateImage(StateRenderer2D renderer) {
        final StateRenderer2D rend = renderer;

        if (dataSet.isEmpty())
            return;

        final double groundResolution = MapTileUtil.groundResolution(dataSet.get(0).loc.getLatitudeDegs(),
                renderer.getLevelOfDetail());
        final double invGR = 1 / groundResolution;
        lod = renderer.getLevelOfDetail();
        imageScaleX = range * 2 * (invGR) / 2000;

        Point2D p1 = renderer.getScreenPosition(topleftLT);
        Point2D p2 = renderer.getScreenPosition(botrightLT);

        top = p1.getY();
        left = p1.getX();
        right = p2.getX();
        bot = p2.getY();

        image = ImageUtils.createCompatibleImage((int) (right - left), (int) (bot - top), Transparency.BITMASK);
        lastCenter = renderer.getCenter();

        Thread t = new Thread(SidescanReplay.class.getSimpleName() + " " + source.getDir().getParent()) {
            @Override
            public void run() {
                Graphics2D g = ((Graphics2D) image.getGraphics());

                // g.setColor(Color.green);
                // g.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
                g.setColor(null);
                g.setComposite(new SideScanComposite());

                double lod = rend.getLevelOfDetail();

                for (SidescanData ssd : dataSet) {
                    if (lod != rend.getLevelOfDetail())
                        return;

                    Point2D p = rend.getScreenPosition(ssd.loc);
                    Graphics2D g2 = (Graphics2D) g.create();

                    g2.translate(-left, -top);
                    g2.translate(p.getX() - 1000 * imageScaleX, p.getY());
                    g2.rotate(ssd.heading, 1000 * imageScaleX, 0);
                    g2.scale(imageScaleX, 1);
                    // g2.drawImage(ssd.img, null, 0, 0);
                    // int ysize = (int)((ssd.alongTrackLength*invGR) > 1 ? (ssd.alongTrackLength*invGR) : 1);
                    // NeptusLog.pub().info("<###> "+ysize + " " + groundResolution + " " + ssd.alongTrackLength);
                    g2.drawImage(ssd.img, 0, 0, null);
                    g2.dispose();
                    rend.repaint();
                }
            };
        };
        t.setDaemon(true);
        t.start();
    }

    private boolean firstPaint = true;

    @Override
    public void paint(Graphics2D g, final StateRenderer2D renderer) {
        LocationType center = renderer.getCenter().getNewAbsoluteLatLonDepth();

        if (firstPaint) {
            firstPaint = false;
            renderer.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    generateImage(renderer);
                }
            });
        }

        if (renderer.getLevelOfDetail() != lod)
            generate = true;
        if (generate) {
            generateImage(renderer);
            generate = false;
        }
        double[] offset = center.getDistanceInPixelTo(lastCenter, renderer.getLevelOfDetail());

        left += offset[0];
        top += offset[1];
        Graphics2D g2 = (Graphics2D) g.create();
        g2.rotate(-renderer.getRotation(), renderer.getWidth() / 2, renderer.getHeight() / 2);
        g2.drawImage(image, null, (int) (left), (int) (top));
        g2.dispose();
        lastCenter = center;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLog("EstimatedState") != null && source.getLog("SonarData") != null;
    }

    @Override
    public String getName() {
        return I18n.text("Sidescan Replay");
    }

    @Override
    public void parse(IMraLogGroup source) {
        this.source = source;

        IMraLog ssParse = source.getLog("SonarData");
        IMraLog esParse = source.getLog("EstimatedState");

        IMCMessage msgSS = ssParse.firstLogEntry();
        IMCMessage msgES = esParse.getEntryAtOrAfter(msgSS.getTimestampMillis());
        IMCMessage prevMsgSS = null;

        LocationType loc = new LocationType();
        LocationType tempLoc;

        double minLat = 180;
        double maxLat = -180;
        double minLon = 360;
        double maxLon = -360;

        range = msgSS.getFloat("max_range");
        while (msgSS != null) {
            if (msgSS.getInteger("type") == SonarData.TYPE.SIDESCAN.value()) {
                msgES = esParse.getEntryAtOrAfter(msgSS.getTimestampMillis());
                if (msgES == null) {
                    msgSS = ssParse.nextLogEntry();
                    continue;
                }
                loc.setLatitudeDegs(Math.toDegrees(msgES.getDouble("lat")));
                loc.setLongitudeDegs(Math.toDegrees(msgES.getDouble("lon")));
                loc.setOffsetNorth(msgES.getDouble("x"));
                loc.setOffsetEast(msgES.getDouble("y"));
                tempLoc = loc.getNewAbsoluteLatLonDepth();

                if (tempLoc.getLatitudeDegs() < minLat)
                    minLat = tempLoc.getLatitudeDegs();
                if (tempLoc.getLatitudeDegs() > maxLat)
                    maxLat = tempLoc.getLatitudeDegs();
                if (tempLoc.getLongitudeDegs() < minLon)
                    minLon = tempLoc.getLongitudeDegs();
                if (tempLoc.getLongitudeDegs() > maxLon)
                    maxLon = tempLoc.getLongitudeDegs();

                if (prevMsgSS != null) {
                    byte[] currentRaw = msgSS.getRawData("data");
                    byte[] prevRaw = prevMsgSS.getRawData("data");
                    for (int i = 0; i < currentRaw.length; i++)
                        currentRaw[i] = (byte) ((prevRaw[i] + currentRaw[i]) / 2);
                    msgSS.setValue("data", currentRaw);
                    double len = msgES.getDouble("u") * 0.063;
                    dataSet.add(new SidescanData(currentRaw, loc.getNewAbsoluteLatLonDepth(), msgES.getDouble("psi"),
                            msgES.getDouble("alt"), len));
                }
                else {
                    double len = msgES.getDouble("u") * 0.2;
                    dataSet.add(new SidescanData(msgSS.getRawData("data"), loc.getNewAbsoluteLatLonDepth(), msgES
                            .getDouble("psi"), msgES.getDouble("alt"), len));
                }
            }
            msgSS = ssParse.nextLogEntry();
        }

        topleftLT = new LocationType(maxLat, minLon);
        botrightLT = new LocationType(minLat, maxLon);

        topleftLT.setOffsetNorth(range);
        topleftLT.setOffsetWest(range);
        botrightLT.setOffsetSouth(range);
        botrightLT.setOffsetEast(range);

        topleftLT = topleftLT.getNewAbsoluteLatLonDepth();
        botrightLT = botrightLT.getNewAbsoluteLatLonDepth();
    }

    @Override
    public String[] getObservedMessages() {
        // return new String[] { "EstimatedState", "SidescanPing" };
        return null;
    }

    @Override
    public void onMessage(IMCMessage message) {

    }

    class SidescanData {
        public double heading;
        public double alongTrackLength;
        public BufferedImage img;
        public LocationType loc;

        public SidescanData(byte[] raw, LocationType loc, double heading, double bottDistance, double alongTdist) {
            img = ImageUtils.createCompatibleImage(raw.length, 1, Transparency.BITMASK);
            double startAngle = 185.0;
            double angleStep = 170.0 / 2000.0;
            double angle;
            ColorMap cp = ColorMapFactory.createBronzeColormap();
            for (int i = 0; i < raw.length; i++) {
                angle = startAngle + (angleStep * i);
                double srange = (bottDistance * (2000f / 100f)) / Math.cos(Math.toRadians(angle));
                double d = Math.sqrt(Math.pow(srange, 2) - Math.pow(bottDistance, 2));
                int pos = (int) d * (i < 1000 ? -1 : 1);
                if (pos <= -1000 || pos >= 1000) {
                    continue;
                }
                img.setRGB(i, 0, cp.getColor((raw[i] & 0xFF) / 255.0).getRGB());
            }
            this.loc = loc;
            this.heading = heading;
            this.alongTrackLength = alongTdist;
        }
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }
}
