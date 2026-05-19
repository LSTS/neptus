/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.imgscalr.Scalr;

import pt.lsts.imc.Elevator;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PopUp;
import pt.lsts.imc.StationKeeping;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.NeptusProperty;
import java.awt.RenderingHints;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.coord.MapTileUtil;
import pt.lsts.neptus.util.sidescan.SideScanComposite;
import pt.lsts.neptus.util.sidescan.SlantRangeImageFilter;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanHistogramNormalizer;
import pt.lsts.neptus.mra.api.CorrectedPosition;

/**
 * @author jqcorreia
 * @author Manuel Ribeiro (handle all sidescan types)
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
    private LocationType topleftLT;
    private LocationType botrightLT;

    private IMraLogGroup source;
    private StateRenderer2D renderer;

    @NeptusProperty(category = "SideScan", name="Normalization factor")
    public double normalization = 0.05;

    @NeptusProperty(category = "SideScan", name="Time Variable Gain factor")
    public double tvgGain = 45;

    @NeptusProperty(name = "Color map to use", category = "SideScan")
    public ColorMap colorMap = ColorMapFactory.createBronzeColormap();

    @NeptusProperty(name = "Apply Slant Range Correction", category = "SideScan")
    public boolean slantRangeCorrection = true;

    @NeptusProperty(name = "EGN Normalization", category = "SideScan", description = "Perform empirical gain normalization")
    public boolean egnNormalization = true;

    @NeptusProperty(name = "Sub System to process", category = "SideScan", description = "Index of the subsystem to process")
    public int subSystem = 0;

    @NeptusProperty(category = "SideScan", name="Truncate Range (%)", description="Ignore data further than this range")
    public int truncRangePercent = 95;

    @NeptusProperty(category = "SideScan", name="Use Corrected positions", description="Use locations corrected by GPS")
    public boolean useCorrectedPositions = true;

    public enum BlendMode {
        Average,
        Maximum,
        Last
    }

    @NeptusProperty(category = "SideScan", name = "Blend Mode", description = "How pings are combined on the map")
    public BlendMode blendMode = BlendMode.Last;

    @NeptusProperty(category = "SideScan", name = "Translucent Mode", description = "Use age-based translucency")
    public boolean translucentMode = false;

    @NeptusProperty(category = "SideScan", name = "Swath Transparency", description = "Transparency of the sidescan swath (0.0 to 1.0)")
    public double swathTransparency = 0.0;

    public enum Transducer {
        Both,
        Port,
        Starboard
    }

    @NeptusProperty(category = "SideScan", name = "Transducer", description = "Which transducer to display")
    public Transducer transducer = Transducer.Both;

    @NeptusProperty(category = "SideScan", name = "Blur", description = "Apply blur to the rendered image")
    public boolean bgBlur = false;

    @NeptusProperty(category = "SideScan", name = "Black Background", description = "Force black background for comparison")
    public boolean blackBackground = false;

    @NeptusProperty(category = "SideScan", name = "Skip Station-Keeping / PopUp / Elevator", description = "Ignore sonar data collected during station-keeping, popup and elevator maneuvers")
    public boolean skipSK = true;

    public SidescanReplay() {
    }

    @Override
    public void cleanup() {
        image = null;
        dataSet.clear();
    }

    protected void generateImage(StateRenderer2D renderer) {
        final StateRenderer2D rend = renderer;

        if (dataSet.isEmpty())
            return;

        // Calculate the ground resolution (meters per pixel) for the current zoom level at this latitude.
        final double groundResolution = MapTileUtil.groundResolution(dataSet.get(0).loc.getLatitudeDegs(),
                renderer.getLevelOfDetail());
        final double invGR = 1 / groundResolution; // pixels per meter
        lod = renderer.getLevelOfDetail();

        // Determine the horizontal scale needed to fit the swath (range*2) into the ping image width.
        double samplesPerSwath = dataSet.get(0).raw.length;
        imageScaleX = (range * 2) * invGR / samplesPerSwath;

        // Compute canvas size from geographic extent in "geographic pixel coordinates".
        // This is rotation-independent (always north-up), ensuring the image remains valid 
        // even if the user rotates the map after generation.
        double[] geoExtent = topleftLT.getOffsetFrom(botrightLT);
        int w = (int) Math.abs(geoExtent[1] * invGR);  // East/West extent in pixels
        int h = (int) Math.abs(geoExtent[0] * invGR);  // North/South extent in pixels

        if (w <= 0 || h <= 0)
            return;

        // Memory safety: cap at 16000 to prevent OutOfMemoryError
        if (w > 16000 || h > 16000) {
            NeptusLog.pub().warn("SidescanReplay canvas too large (" + w + "x" + h + "), capping to 16000");
            w = Math.min(w, 16000);
            h = Math.min(h, 16000);
        }

        image = ImageUtils.createCompatibleImage(w, h, Transparency.TRANSLUCENT);

        Thread t = new Thread(SidescanReplay.class.getSimpleName() + " " + source.getDir().getParent()) {
            @Override
            public void run() {
                Graphics2D g = image.createGraphics();

                // Build a solid black "carpet" that follows the swath coverage
                if (blackBackground && !dataSet.isEmpty()) {
                    g.setColor(Color.BLACK);
                    g.setComposite(java.awt.AlphaComposite.SrcOver);

                    Path2D path = new Path2D.Double();
                    boolean first = true;

                    // First pass: Build port-side edge
                    for (SidescanData ssd : dataSet) {
                        double[] pos = ssd.loc.getOffsetFrom(topleftLT);
                        double px = pos[1] * invGR;
                        double py = -pos[0] * invGR;
                        int fullLen = ssd.raw.length;
                        double centerX = fullLen / 2.0;

                        AffineTransform at = new AffineTransform();
                        at.translate(px - centerX * imageScaleX, py);
                        at.rotate(ssd.heading, centerX * imageScaleX, 0);
                        at.scale(imageScaleX, invGR);

                        double xStart = (transducer == Transducer.Starboard) ? centerX : 0;
                        Point2D pEdge = at.transform(new Point2D.Double(xStart, 0), null);

                        if (first) {
                            path.moveTo(pEdge.getX(), pEdge.getY());
                            first = false;
                        } else {
                            path.lineTo(pEdge.getX(), pEdge.getY());
                        }
                    }

                    // Second pass: Build starboard-side edge (backwards)
                    for (int i = dataSet.size() - 1; i >= 0; i--) {
                        SidescanData ssd = dataSet.get(i);
                        double[] pos = ssd.loc.getOffsetFrom(topleftLT);
                        double px = pos[1] * invGR;
                        double py = -pos[0] * invGR;
                        int fullLen = ssd.raw.length;
                        double centerX = fullLen / 2.0;

                        AffineTransform at = new AffineTransform();
                        at.translate(px - centerX * imageScaleX, py);
                        at.rotate(ssd.heading, centerX * imageScaleX, 0);
                        at.scale(imageScaleX, invGR);

                        double xEnd = (transducer == Transducer.Port) ? centerX : fullLen;
                        Point2D pEdge = at.transform(new Point2D.Double(xEnd, 0), null);
                        path.lineTo(pEdge.getX(), pEdge.getY());
                    }
                    path.closePath();
                    g.fill(path);
                }

                if (translucentMode)
                    g.setComposite(new SideScanComposite(SideScanComposite.MODE.AGE));
                else if (blendMode == BlendMode.Average)
                    g.setComposite(new SideScanComposite(SideScanComposite.MODE.AVERAGE));
                else if (blendMode == BlendMode.Maximum)
                    g.setComposite(new SideScanComposite(SideScanComposite.MODE.MAX));
                else
                    g.setComposite(java.awt.AlphaComposite.SrcOver);

                ColorMap cp = colorMap;
                double lod = rend.getLevelOfDetail();
                int pcount = 0;

                for (SidescanData ssd : dataSet) {
                    if (lod != rend.getLevelOfDetail())
                        return;

                    int fullLen = ssd.raw.length;
                    BufferedImage pingImg = ImageUtils.createCompatibleImage(fullLen, 1, Transparency.TRANSLUCENT);
                    double baseAlpha = (1.0 - swathTransparency);

                    for (int i = 0; i < fullLen; i++) {
                        int rgb = cp.getColor((ssd.raw[i] & 0xFF) / 255.0).getRGB();
                        int alpha = (int) (baseAlpha * 255.0);
                        pingImg.setRGB(i, 0, (alpha << 24) | (rgb & 0x00FFFFFF));
                    }

                    if (slantRangeCorrection) {
                        pingImg = Scalr.apply(pingImg, new SlantRangeImageFilter(ssd.altitude, ssd.range, fullLen));
                    }

                    double centerX = fullLen / 2.0;

                    // Calculate the geographic pixel position relative to the top-left anchor.
                    // This uses a "north-up" coordinate system within the pre-rendered image.
                    double[] pos = ssd.loc.getOffsetFrom(topleftLT);
                    double px = pos[1] * invGR;    // East offset -> X coordinate in pixels
                    double py = -pos[0] * invGR;   // North offset -> Y coordinate (negated for screen Y-down)

                    // Apply local transformations for each ping:
                    // 1. Move to the ping's position (adjusting for center-of-swath).
                    // 2. Rotate to the vehicle's heading.
                    // 3. Scale the swath to match the map resolution.
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.translate(px - centerX * imageScaleX, py);
                    g2.rotate(ssd.heading, centerX * imageScaleX, 0);
                    g2.scale(imageScaleX, invGR);

                    if (transducer == Transducer.Port) {
                        g2.drawImage(pingImg, 0, 0, (int)centerX, 1, 0, 0, (int)centerX, 1, null);
                    } else if (transducer == Transducer.Starboard) {
                        g2.drawImage(pingImg, (int)centerX, 0, fullLen, 1, (int)centerX, 0, fullLen, 1, null);
                    } else {
                        g2.drawImage(pingImg, 0, 0, null);
                    }
                    g2.dispose();
                    pingImg = null;

                    if (++pcount % 250 == 0) {
                        rend.repaint();
                    }
                }

                if (bgBlur) {
                    // Optimized Real-time Background Blur:
                    // Create a downscaled version (1/4 size) of the image to dramatically speed up the expensive Gaussian calculation.
                    int smallW = Math.max(image.getWidth() / 4, 1);
                    int smallH = Math.max(image.getHeight() / 4, 1);

                    BufferedImage behind = ImageUtils.createCompatibleImage(smallW, smallH, Transparency.TRANSLUCENT);
                    Graphics2D bg = behind.createGraphics();
                    bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    // Draw bounded offset shifted by scaling factor
                    bg.drawImage(image, 1, 0, smallW, smallH, null);
                    bg.drawImage(image, -1, 0, smallW, smallH, null);
                    bg.drawImage(image, 0, 1, smallW, smallH, null);
                    bg.drawImage(image, 0, -1, smallW, smallH, null);
                    bg.dispose();

                    // Apply a softer blur radius relative to the smaller scale
                    behind = org.imgscalr.Scalr.apply(behind, new com.jhlabs.image.GaussianFilter(10));

                    Graphics2D gImg = image.createGraphics();
                    gImg.setComposite(java.awt.AlphaComposite.DstOver);
                    gImg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    gImg.drawImage(behind, 0, 0, image.getWidth(), image.getHeight(), null);
                    gImg.dispose();
                }
                rend.repaint();
            };
        };
        t.setDaemon(true);
        t.start();
    }

    private boolean firstPaint = true;

    @Override
    public void paint(Graphics2D g, final StateRenderer2D renderer) {

        if (firstPaint) {
            firstPaint = false;
            renderer.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    generateImage(renderer);
                }
            });

            renderer.addMouseListener(new MouseAdapter() {
                private void showMenu(MouseEvent e) {
                    if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                        SwingUtilities.invokeLater(() -> {
                            JPopupMenu menu = new JPopupMenu();
                            JMenuItem item = new JMenuItem(I18n.text("Configure Sidescan..."));
                            item.addActionListener(al -> {
                                // Snapshot parse-affecting parameters before editing
                                final double   snapNorm    = normalization;
                                final double   snapTvg     = tvgGain;
                                final boolean  snapEgn     = egnNormalization;
                                final int      snapSys     = subSystem;
                                final int      snapTrunc   = truncRangePercent;
                                final boolean  snapSkipSK  = skipSK;
                                final boolean  snapSlant   = slantRangeCorrection;
                                final boolean  snapCorrPos = useCorrectedPositions;

                                PluginUtils.editPluginProperties(SidescanReplay.this, true);

                                // Check if any parse-affecting parameter changed
                                boolean needsReparse =
                                        snapNorm   != normalization     ||
                                        snapTvg    != tvgGain           ||
                                        snapEgn    != egnNormalization  ||
                                        snapSys    != subSystem         ||
                                        snapTrunc  != truncRangePercent ||
                                        snapCorrPos != useCorrectedPositions ||
                                        snapSkipSK != skipSK;

                                if (needsReparse) {
                                    new Thread("SidescanReplay-reparse") {
                                        @Override
                                        public void run() {
                                            parse(source);
                                            SwingUtilities.invokeLater(() -> {
                                                generate = true;
                                                renderer.repaint();
                                            });
                                        }
                                    }.start();
                                } else {
                                    // Non-parsing property might have changed, just trigger a re-draw
                                    generate = true;
                                    renderer.repaint();
                                }
                            });
                            menu.add(item);
                            menu.show(e.getComponent(), e.getX(), e.getY());
                        });
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    showMenu(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    showMenu(e);
                }
            });
        }

        if (renderer.getLevelOfDetail() != lod)
            generate = true;
        if (generate) {
            generateImage(renderer);
            generate = false;
        }

        // Paint the pre-rendered image onto the map.
        // We use the fresh screen position of the anchor (topleftLT) to handle panning and zooming.
        // Because the image was rendered "north-up", we must un-rotate the Graphics context
        // around the anchor point to align it with the current map rotation.
        if (image != null && topleftLT != null) {
            Point2D corner = renderer.getScreenPosition(topleftLT);
            Graphics2D g2 = (Graphics2D) g.create();
            // 1. Translate to where the anchor (NW corner) currently is on the screen.
            g2.translate(corner.getX(), corner.getY());
            // 2. Counter-rotate the map's rotation to draw the image in geographic alignment.
            g2.rotate(-renderer.getRotation());
            g2.drawImage(image, 0, 0, null);
            g2.dispose();
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return SidescanParserFactory.existsSidescanParser(source);
    }

    @Override
    public String getName() {
        return I18n.text("Sidescan Replay");
    }

    @Override
    public void parse(IMraLogGroup source) {
        this.source = source;
        dataSet.clear();

        SidescanParser parser = SidescanParserFactory.build(source);
        if (parser == null) {
            NeptusLog.pub().error("No sidescan parser found for " + source.getDir().getName());
            return;
        }

        double minLat = 180;
        double maxLat = -180;
        double minLon = 180;
        double maxLon = -180;

        ArrayList<Integer> subsystems = parser.getSubsystemList();
        int sys = subsystems.isEmpty() ? 0 : (subSystem < subsystems.size() ? subsystems.get(subSystem) : subsystems.get(0));

        NeptusLog.pub().info("Parsing sidescan using " + parser.getClass().getSimpleName() + " (Subsystem: " + sys + ")");

        long t1 = parser.firstPingTimestamp();
        long t2 = parser.lastPingTimestamp();

        SidescanParameters configParams = new SidescanParameters(normalization, tvgGain);
        SidescanHistogramNormalizer histogram = null;
        if (egnNormalization) {
            histogram = SidescanHistogramNormalizer.create(source);
            configParams = SidescanHistogramNormalizer.HISTOGRAM_DEFAULT_PARAMATERS;
        }

        CorrectedPosition correctPositions = null;
        if (useCorrectedPositions) {
            correctPositions = new CorrectedPosition(source);
        }

        long batchSizeMillis = 1000; 
        long currentT = t1;

        range = 0;

        while (currentT < t2) {
            // Skip data collected during station-keeping, popup and elevator maneuvers
            if (skipSK) {
                IMCMessage planState = source.getLsfIndex().getMessageAt("PlanControlState", currentT / 1000.0);
                if (planState != null && planState.getAbbrev().equals("PlanControlState")) {
                    switch (planState.getInteger("man_type")) {
                        case StationKeeping.ID_STATIC:
                        case PopUp.ID_STATIC:
                        case Elevator.ID_STATIC:
                            currentT += batchSizeMillis;
                            continue;
                        default:
                            break;
                    }
                }
            }

            long endT = Math.min(currentT + batchSizeMillis, t2 + 1);

            ArrayList<SidescanLine> lines = parser.getLinesBetween(currentT, endT, sys, configParams);

            if (lines != null) {
                for (SidescanLine line : lines) {
                    range = (float) Math.max(range, line.getRange());

                    LocationType tempLoc = line.getState().getPosition();
                    if (useCorrectedPositions && correctPositions != null) {
                        SystemPositionAndAttitude p = correctPositions.getPosition(line.getTimestampMillis() / 1000.0);
                        if (p != null) {
                            tempLoc = p.getPosition();
                        }
                    }

                    // Resolve offsets into true absolute Lat/Lon so bounding box expands accurately
                    tempLoc = tempLoc.getNewAbsoluteLatLonDepth();

                    if (tempLoc.getLatitudeDegs() < minLat) minLat = tempLoc.getLatitudeDegs();
                    if (tempLoc.getLatitudeDegs() > maxLat) maxLat = tempLoc.getLatitudeDegs();
                    if (tempLoc.getLongitudeDegs() < minLon) minLon = tempLoc.getLongitudeDegs();
                    if (tempLoc.getLongitudeDegs() > maxLon) maxLon = tempLoc.getLongitudeDegs();

                    double[] data = line.getData();
                    if (histogram != null) {
                        data = histogram.normalize(data, sys);
                    }

                    byte[] raw = new byte[data.length];
                    for (int i = 0; i < data.length; i++) {
                        raw[i] = (byte) Math.min(data[i] * 255.0, 255.0);
                    }

                    double len = line.getState().getU() * 0.1; // Default along-track length
                    if (!dataSet.isEmpty()) {
                        len = tempLoc.getDistanceInMeters(dataSet.get(dataSet.size() - 1).loc);
                    }
                    dataSet.add(new SidescanData(raw, tempLoc, line.getState().getYaw(), line.getState().getAltitude(), line.getRange(), len));
                }
            }
            currentT = endT;
        }

        NeptusLog.pub().info("SidescanReplay parsed " + dataSet.size() + " lines.");

        if (minLat == 180) return; // No data

        topleftLT = new LocationType(maxLat, minLon);
        botrightLT = new LocationType(minLat, maxLon);

        // Expand the geographic bounding box by a generous margin (2x sonar range).
        // A 1x range buffer is the theoretical minimum to cover the swath width,
        // but an extra 1x range is added to account for rotated swath tips (corners) 
        // and to absorb any minor coordinate drift between parsing and background rendering.
        double pad = range * 2;
        topleftLT.setOffsetNorth(pad);
        topleftLT.setOffsetWest(pad);
        botrightLT.setOffsetSouth(pad);
        botrightLT.setOffsetEast(pad);

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
        public double altitude;
        public double range;
        public double heading;
        public double alongTrackLength;
        public byte[] raw;
        public LocationType loc;

        public SidescanData(byte[] raw, LocationType loc, double heading, double altitude, double range, double alongTdist) {
            this.raw = raw;
            this.loc = loc;
            this.heading = heading;
            this.altitude = altitude;
            this.range = range;
            this.alongTrackLength = alongTdist;
        }
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }
}