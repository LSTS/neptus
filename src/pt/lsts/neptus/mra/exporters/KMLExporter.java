/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * Jan 8, 2013
 */
package pt.lsts.neptus.mra.exporters;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ProgressMonitor;

import org.imgscalr.Scalr;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColormapOverlay;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.WorldImage;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryParserFactory;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.mra.importers.jsf.JsfSidescanParser;
import pt.lsts.neptus.mra.importers.lsf.DVLBathymetryParser;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.ImageLayer;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ZipUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.neptus.util.sidescan.AcousticCommsFilter;
import pt.lsts.neptus.util.sidescan.SideScanComposite;
import pt.lsts.neptus.util.sidescan.SlantRangeImageFilter;
import pt.lsts.util.WGS84Utilities;

/**
 * @author zp
 */
@PluginDescription
public class KMLExporter implements MRAExporter {
    public double minHeight = 1000;
    public double maxHeight = -1;
    private IMraLogGroup source;
    private ProgressMonitor pmonitor;

    @NeptusProperty(category = "SideScan", name="Time Variable Gain")
    public double timeVariableGain = 300;

    @NeptusProperty(category = "SideScan", name="Normalization")
    public double normalization = 0.1;
    
    @NeptusProperty(category = "SideScan", name="Swath transparency")
    public double swathTransparency = 0.25;

    @NeptusProperty(category="Output", name="Generated layers transparency")
    public double layerTransparency = 0.5;

    @NeptusProperty(category = "SideScan", name="Separate transducers")
    public boolean separateTransducers = false;

    @NeptusProperty(category = "SideScan", name="Separate line segments")
    public boolean separateLineSegments = false;

    @NeptusProperty(category = "SideScan", name="Slant Range Correction")
    public boolean slantRangeCorrection = true;
    
    @NeptusProperty(category = "SideScan", name="Pixel blending mode", description="How to blend multiple measurements on same location")
    public SideScanComposite.MODE blendMode = SideScanComposite.MODE.MAX;
    
    @NeptusProperty(category = "Output", name="Compress Output")
    public boolean compressOutput = true;

    public double maximumSidescanRange = 50;

    @NeptusProperty(name = "Interval (seconds) for path separation", description = "If two vehicle states have a further time separation they will originate two separate paths")
    public int secondsGapInEstimatedStateForPathBreak = 30;

    @NeptusProperty(category = "Default Visibility", name="Show Bathymetry")
    public boolean visibilityForBathymetry = true;

    @NeptusProperty(category = "Default Visibility", name="Show Sidescan")
    public boolean visibilityForSideScan = true;
    
    @NeptusProperty(category = "Default Visibility", name="Show Legend")
    public boolean visibilityForLegends = true;
    
    @NeptusProperty(category = "SideScan", name="Acoustic Communications Filter")
    public boolean filterMicromodem = false;

    @NeptusProperty(category = "Export", name="Export Bathymetry")
    public boolean exportBathymetry = true;

    @NeptusProperty(category = "Export", name="Export Sidescan")
    public boolean exportSidescan = true;

    public KMLExporter(IMraLogGroup source) {
        this.source = source;
    }

    @Override
    public String getName() {
        return I18n.text("Export to KML");
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    public String kmlHeader(String title) {
        String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://earth.google.com/kml/2.1\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n";
        ret += "\t<Document>\n";
        ret += "\t\t<name>" + title + "</name>\n";

        Date d = new Date((long) (1000 * source.getLsfIndex().getStartTime()));
        ret += "\t\t<description>Plan executed on " + d + "</description>";

        ret += "\t\t<Style id=\"estate\">\n";
        ret += "\t\t\t<LineStyle>\n";
        ret += "\t\t\t<color>99ff0000</color>\n";
        ret += "\t\t\t<width>4</width>\n";
        ret += "\t\t\t</LineStyle>\n";
        ret += "\t\t</Style>\n";

        ret += "\t\t<Style id=\"plan\">\n";
        ret += "\t\t\t<LineStyle>\n";
        ret += "\t\t\t<color>990000ff</color>\n";
        ret += "\t\t\t<width>4</width>\n";
        ret += "\t\t\t</LineStyle>\n";
        ret += "\t\t</Style>\n";

        return ret;
    }

    public String overlay(File imageFile, String title, LocationType sw, LocationType ne, boolean visibility) {
        sw.convertToAbsoluteLatLonDepth();
        ne.convertToAbsoluteLatLonDepth();
        String ret = "\t\t<GroundOverlay>\n";
        try {
            ret += "\t\t\t<name>" + title + "</name>\n";
            ret += "\t\t\t<visibility>" + (visibility ? 1 : 0) + "</visibility>\n";
            ret += "\t\t\t<description></description>\n";
            ret += "\t\t\t<Icon>\n";
            ret += "\t\t\t\t<href>" + imageFile.getName() + "</href>\n";
            ret += "\t\t\t</Icon>\n";
            ret += "\t\t\t<gx:altitudeMode>clampToSeaFloor</gx:altitudeMode>\n";
            ret += "\t\t\t<LatLonBox>\n";
            ret += "\t\t\t\t<north>" + ne.getLatitudeDegs() + "</north>\n";
            ret += "\t\t\t\t<south>" + sw.getLatitudeDegs() + "</south>\n";
            ret += "\t\t\t\t<east>" + ne.getLongitudeDegs() + "</east>\n";
            ret += "\t\t\t\t<west>" + sw.getLongitudeDegs() + "</west>\n";
            ret += "\t\t\t\t<rotation>0</rotation>\n";
            ret += "\t\t\t</LatLonBox>\n";
            ret += "\t\t</GroundOverlay>\n";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";

        }
        return ret;
    }

    public String path(Vector<LocationType> coords, String name, String style) {
        String retAll = "";
        int idx = 0;
        int pathNumber = 0;
        while (idx < coords.size()) {
            String ret = "\t\t<Placemark>\n";
            ret += "\t\t\t<name>" + name + " " + pathNumber++ + "</name>\n";
            ret += "\t\t\t<styleUrl>#" + style + "</styleUrl>\n";
            ret += "\t\t\t<LineString>\n";
            ret += "\t\t\t\t<gx:altitudeMode>clampToSeaFloor</gx:altitudeMode>\n";
            ret += "\t\t\t\t<coordinates> ";

            LocationType l;
            
            for (l = coords.get(idx); idx < coords.size(); l = coords.get(idx), idx++) {
                if (l == null)
                    break;
                l.convertToAbsoluteLatLonDepth();
                ret += l.getLongitudeDegs() + "," + l.getLatitudeDegs() + ","+(-l.getDepth())+"\n";// -" + l.getDepth()+"\n";
            }
            ret += "\t\t\t\t</coordinates>\n";
            ret += "\t\t\t</LineString>\n";
            ret += "\t\t</Placemark>\n";
            retAll += ret;
        }
        return retAll;
    }

    public String kmlFooter() {
        return "\t</Document>\n</kml>\n";
    }

    public String dvlOverlay(File dir, int resolution) {
        ColormapOverlay overlay = new ColormapOverlay("dvlBathymetry", 1, false, 0);
        TidePredictionFinder finder = TidePredictionFactory.create(source);

        for (EstimatedState state : source.getLsfIndex().getIterator(EstimatedState.class, 100)) {
            if (state.getAlt() < 0 || state.getDepth() < MRAProperties.minDepthForBathymetry
                    || Math.abs(state.getTheta()) > Math.toDegrees(10))
                continue;

            LocationType loc = new LocationType(Math.toDegrees(state.getLat()), Math.toDegrees(state.getLon()));
            loc.translatePosition(state.getX(), state.getY(), 0);

            if (finder == null)
                overlay.addSample(loc, Math.max(0, state.getAlt()) + state.getDepth());
            else {
                try {
                    overlay.addSample(loc,
                            Math.max(0, state.getAlt()) + state.getDepth() - finder.getTidePrediction(state.getDate(), false));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        ImageLayer il = overlay.getImageLayer();
        try {
            ImageIO.write(il.getImage(), "PNG", new File(dir, "dvl.png"));

            il.setTransparency(layerTransparency);
            il.saveToFile(new File(dir.getParentFile(), "dvl.layer"));
            LocationType sw = new LocationType();
            LocationType ne = new LocationType();
            sw.setLatitudeStr(il.getBottomRight().getLatitudeStr());
            sw.setLongitudeStr(il.getTopLeft().getLongitudeStr());

            ne.setLatitudeStr(il.getTopLeft().getLatitudeStr());
            ne.setLongitudeStr(il.getBottomRight().getLongitudeStr());

            return overlay(new File(dir, "dvl.png"), "DVL Bathymetry mosaic", sw, ne, false);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    enum Ducer {
        starboard,
        board,
        both
    }
    
    public String sidescanOverlay(File dir, double resolution, LocationType topLeft, LocationType bottomRight,
            String fname, long startTime, long endTime, Ducer ducer) {
        SidescanParser ssParser = SidescanParserFactory.build(source);

        double totalProg = 100;
        double startProg = 100;
        // FIXME temporary fix
        boolean makeAbs = (ssParser instanceof JsfSidescanParser);

        // System.out.println("makeAbs: "+makeAbs);

        if (ssParser == null || ssParser.getSubsystemList().isEmpty())
            return "";

        double[] offsets = topLeft.getOffsetFrom(bottomRight);
        int width = (int) Math.abs(offsets[1] * resolution);
        int height = (int) Math.abs(offsets[0] * resolution);

        if (width <= 0 || height <= 0)
            return "";

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        JLabel lbl = new JLabel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(java.awt.Graphics g) {
                g.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
            };
        };

        JFrame frm = GuiUtils.testFrame(lbl, "Creating sidescan mosaic...");
        frm.getContentPane().setBackground(Color.white);
        frm.setSize(800, 600);
        GuiUtils.centerOnScreen(frm);
        Graphics2D g = (Graphics2D) img.getGraphics();
        long start = Math.max(ssParser.firstPingTimestamp(), startTime);
        long end = Math.min(ssParser.lastPingTimestamp(), endTime);
        
        
        int sys = ssParser.getSubsystemList().get(0);
        SidescanParameters params = new SidescanParameters(normalization, timeVariableGain);
        String filename = fname;

        BufferedImage swath = null;
        
        AcousticCommsFilter filter = null;
        if (filterMicromodem)
            filter = new AcousticCommsFilter(source);
        
        ColorMap cmap = ColorMapFactory.createBronzeColormap();
        for (long time = start; time < end - 1000; time += 1000) {
            if (pmonitor.isCanceled()) {
                frm.setVisible(false);
                frm.dispose();
                return "";
            }
            double progress = ((double)(time - start) / (end - start)) * totalProg + startProg;
            pmonitor.setProgress((int)progress);


            ArrayList<SidescanLine> lines;
            try {
                lines = ssParser.getLinesBetween(time, time + 1000, sys, params);
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            BufferedImage previous = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
            for (SidescanLine sl : lines) {

                if (filter != null && !filter.isDataValid(new Date(sl.getTimestampMillis())))
                    continue;
                
                int widthPixels = (int) (sl.getRange() * resolution * 2);

                if (swath == null || swath.getWidth() != widthPixels)
                    swath = new BufferedImage(widthPixels, 3, BufferedImage.TYPE_INT_ARGB);

                if (previous != null)
                    swath.getGraphics().drawImage(previous, 0, 0, swath.getWidth(), 1, 1, 0, 2, previous.getWidth(),
                            null);

                int samplesPerPixel = (int) Math.round(1.0 * sl.getData().length / widthPixels);
                if (samplesPerPixel == 0)
                    continue;
                double sum = 0;
                int count = 0;

                int startPixel, endPixel;

                switch (ducer) {
                    case board:
                        startPixel = 0;
                        endPixel = sl.getData().length / 2;
                        filename = fname+"_board";
                        break;
                    case starboard:
                        startPixel = sl.getData().length / 2;
                        endPixel = sl.getData().length;
                        filename = fname+"_starboard";
                        break;
                    default:
                        startPixel = 0;
                        endPixel = sl.getData().length;
                        filename = fname;
                        break;
                }

                int pixelOffset = (int) Math.round((widthPixels - 1.0 * sl.getData().length / samplesPerPixel) / 2.0);
                for (int i = startPixel; i < endPixel; i++) {
                    if (i != 0 && i % samplesPerPixel == 0) {
                        int alpha = (int) (swathTransparency * 255);
                        double val = sum / count;

                        int pixelInImgToWrite = (i / samplesPerPixel - 1) + pixelOffset;

                        if (Double.isNaN(val) || Double.isInfinite(val))
                            alpha = 255;
                        if (pixelInImgToWrite >= 0 && pixelInImgToWrite < widthPixels)
                            swath.setRGB(pixelInImgToWrite, 0, cmap.getColor(val).getRGB()
                                    ^ ((alpha & 0xFF) << 24));
                        sum = 0;
                        count = 0;
                    }
                    if (!Double.isNaN(sl.getData()[i]) && !Double.isInfinite(sl.getData()[i])) { 
                        count++;
                        sum += sl.getData()[i];
                    }
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                if (blendMode != SideScanComposite.MODE.NONE)
                    g2.setComposite(new SideScanComposite(blendMode));
                double[] pos = sl.getState().getPosition().getOffsetFrom(topLeft);
                g2.translate(pos[1] * resolution, -pos[0] * resolution);
                if (makeAbs && sl.getState().getYaw() < 0)
                    g2.rotate(Math.toRadians(300) + sl.getState().getYaw());
                else
                    g2.rotate(sl.getState().getYaw());
                g2.setColor(Color.black);
                g2.scale(1, resolution);

                if (slantRangeCorrection)
                    swath = Scalr.apply(swath, new SlantRangeImageFilter(sl.getState().getAltitude(), sl.getRange(), swath.getWidth()));

                g2.drawImage(swath, -swath.getWidth() / 2, 0, null);
                g2.dispose();
                previous = swath;
                lbl.repaint();
            }
        }

        frm.setVisible(false);
        frm.dispose();

        try {
            ImageIO.write(img, "PNG", new File(dir, filename + ".png"));
            ImageLayer il = new ImageLayer("Sidescan mosaic from " + source.name(), img, topLeft, bottomRight);
            il.setTransparency(layerTransparency);
            String sufix = "";
            switch (ducer) {
                case board:
                    sufix = " port";
                    break;
                case starboard:
                    sufix = " starboard";
                    break;
                default:
                    break;
            }
            il.saveToFile(new File(dir.getParentFile(), filename + ".layer"));
            return overlay(new File(dir, filename + ".png"), "Sidescan mosaic" + sufix, 
                    new LocationType(bottomRight.getLatitudeDegs(), topLeft.getLongitudeDegs()),
                    new LocationType(topLeft.getLatitudeDegs(), bottomRight.getLongitudeDegs()), ducer == Ducer.both ? visibilityForSideScan : false) ;
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public String sidescanOverlay(File dir, double resolution, LocationType topLeft, LocationType bottomRight,
            Ducer ducer) {
       return sidescanOverlay(dir, resolution, topLeft, bottomRight, "sidescan", 0, System.currentTimeMillis(), ducer);
    }

    public String multibeamLegend(File dir) {
        BufferedImage img = new BufferedImage(100, 170, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(new Color(255, 255, 255, 100));
        g.fillRect(5, 30, 70, 110);

        ColorMap cmap = ColorMapFactory.createJetColorMap();
        ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, cmap);
        cb.setSize(15, 80);
        g.setColor(Color.black);
        Font prev = g.getFont();
        g.setFont(new Font("Helvetica", Font.BOLD, 18));
        g.setFont(prev);
        g.translate(15, 45);
        cb.paint(g);
        g.translate(-10, -15);

        try {
            g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(0), 28, 20);
            g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(MRAProperties.maxBathymDepth / 2), 28, 60);
            g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(MRAProperties.maxBathymDepth), 28, 100);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            e.printStackTrace();
        }

        try {
            ImageIO.write(img, "PNG", new File(dir, "mb_legend.png"));
            String ret = "\t\t<ScreenOverlay>\n";
            ret += "\t\t\t<name>Multibeam layer legend</name>\n";
            ret += "\t\t\t<visibility>" + visibilityForLegends + "</visibility>\n";
            ret += "\t\t\t<Icon>\n";
            // ret += "\t\t\t\t<href>" + new File(dir, "mb_legend.png").toURI().toURL() + "</href>\n";
            ret += "\t\t\t\t<href>" + new File(dir, "mb_legend.png").getName() + "</href>\n";
            ret += "\t\t\t</Icon>\n";
            ret += "\t\t\t<overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>\n";
            ret += "\t\t\t<screenXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>\n";
            ret += "\t\t\t<rotationXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>\n";
            ret += "\t\t\t<size x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>\n";
            ret += "\t\t</ScreenOverlay>\n";
            return ret;
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String multibeamOverlay(File dir) {
        if (pmonitor.isCanceled())
            return "";
        BathymetryParser parser = BathymetryParserFactory.build(source);
        double pixelWidth = 1.0;

        double startProg = 200;

        if (parser instanceof DVLBathymetryParser)
            pixelWidth = 2.5;

        if (parser == null) {
            NeptusLog.pub().info(I18n.text("no multibeam data has been found."));
            return "";
        }

        String legend = multibeamLegend(dir);

        parser.rewind();

        LocationType topLeft = new LocationType(parser.getBathymetryInfo().topLeft);
        LocationType bottomRight = new LocationType(parser.getBathymetryInfo().bottomRight);

        topLeft.translatePosition(maximumSidescanRange, -maximumSidescanRange, 0);
        bottomRight.translatePosition(-maximumSidescanRange, maximumSidescanRange, 0);
        topLeft.convertToAbsoluteLatLonDepth();
        bottomRight.convertToAbsoluteLatLonDepth();

        double[] offsets = topLeft.getOffsetFrom(bottomRight);
        double mult = 1.25;
        int width = (int) Math.abs(offsets[1] * mult);
        int height = (int) Math.abs(offsets[0] * mult);

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BathymetrySwath swath;
        long first = (long) (1000 * source.getLsfIndex().getStartTime());
        long time = (long) (1000 * source.getLsfIndex().getEndTime()) - first;
        long lastPercent = -1;

        JLabel lbl = new JLabel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(java.awt.Graphics g) {
                g.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
            };
        };

        JFrame frm = GuiUtils.testFrame(lbl, "Creating multibeam mosaic...");
        frm.getContentPane().setBackground(Color.black);
        frm.setSize(800, 600);
        GuiUtils.centerOnScreen(frm);

        ColorMap cmap = ColorMapFactory.createJetColorMap();


        while ((swath = parser.nextSwath(1)) != null) {
            if (pmonitor.isCanceled()) {
                frm.setVisible(false);
                frm.dispose();
                return "";
            }
            LocationType loc = swath.getPose().getPosition();

            for (BathymetryPoint bp : swath.getData()) {

                // if (Math.random() < 0.2)
                // continue;
                LocationType loc2 = new LocationType(loc);
                if (bp == null)
                    continue;
                loc2.translatePosition(bp.north, bp.east, 0);

                double[] pos = loc2.getOffsetFrom(topLeft);
                Color c = cmap.getColor(1 - (bp.depth / MRAProperties.maxBathymDepth));

                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 64));
                g.fill(new Ellipse2D.Double(pos[1] * mult - pixelWidth / 2, -pos[0] * mult - pixelWidth / 2,
                        pixelWidth, pixelWidth));
            }
            long percent = ((swath.getTimestamp() - first) * 100) / time;
            if (percent != lastPercent)
                pmonitor.setProgress((int)(startProg + percent));
            //NeptusLog.pub().info("MULTIBEAM: " + percent + "% done...");
            lastPercent = percent;

            lbl.repaint();
            lbl.setBackground(Color.black);
            try {
                Thread.yield();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        frm.setVisible(false);
        frm.dispose();

        try {
            ImageIO.write(img, "PNG", new File(dir, "mb_bath2.png"));
            ImageLayer il = new ImageLayer("Bathymetry from " + source.name(), img, topLeft, bottomRight);
            il.setTransparency(layerTransparency);
            il.saveToFile(new File(dir.getParentFile(), "multibeam.layer"));

            return legend+overlay(new File(dir, "mb_bath2.png"), "Multibeam Bathymetry", 
                    new LocationType(bottomRight.getLatitudeDegs(), topLeft.getLongitudeDegs()),
                    new LocationType(topLeft.getLatitudeDegs(), bottomRight.getLongitudeDegs()), visibilityForBathymetry);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    @Deprecated
    public String multibeamOverlay_old(File dir) {
        if (source.getFile("multibeam.83P") == null) {
            NeptusLog.pub().info("no multibeam data hasn been found.");
            return "";
        }

        WorldImage imgMb = new WorldImage(3, ColorMapFactory.createJetColorMap());
        DeltaTParser parser = new DeltaTParser(source);
        parser.rewind();
        BathymetrySwath swath;
        long first = (long) (1000 * source.getLsfIndex().getStartTime());
        long time = (long) (1000 * source.getLsfIndex().getEndTime()) - first;
        long lastPercent = -1;

        while ((swath = parser.nextSwath(0.1)) != null) {
            // System.out.println("processing swath...");
            LocationType loc = swath.getPose().getPosition();
            for (BathymetryPoint bp : swath.getData()) {

                if (Math.random() < 0.05)
                    continue;
                LocationType loc2 = new LocationType(loc);
                if (bp == null)
                    continue;
                loc2.translatePosition(-bp.north, -bp.east, 0);

                imgMb.addPoint(loc2, 1 - (bp.depth / parser.getBathymetryInfo().maxDepth));
                long percent = ((swath.getTimestamp() - first) * 100) / time;
                if (percent != lastPercent)
                    NeptusLog.pub().info("MULTIBEAM: " + percent + "% done...");
                lastPercent = percent;
            }
        }

        try {
            ImageIO.write(imgMb.processData(), "PNG", new File(dir, "mb_bath.png"));
            return overlay(new File(dir, "mb_bath.png"), "Multibeam Bathymetry", imgMb.getSouthWest(),
                    imgMb.getNorthEast(), visibilityForBathymetry);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        this.pmonitor = pmonitor;
        pmonitor.setMinimum(0);
        pmonitor.setMaximum(320);

        PluginUtils.editPluginProperties(this, true);

        try {
            pmonitor.setNote("Generating output dirs");
            File out = new File(source.getFile("mra"), "kml");
            out.mkdirs();

            out = new File(out, "Data.kml");
            BufferedWriter bw = new BufferedWriter(new FileWriter(out));
            File f = source.getFile(".");
            String name = f.getCanonicalFile().getName();
            bw.write(kmlHeader(name));

            // To account for multiple systems paths
            Hashtable<String, Vector<LocationType>> pathsForSystems = new Hashtable<>();
            Hashtable<String, IMCMessage> lastEstimatedStateForSystems = new Hashtable<>();
            // Vector<LocationType> states = new Vector<>();

            LocationType bottomRight = null, topLeft = null;

            // Path
            Iterable<IMCMessage> it = source.getLsfIndex().getIterator("EstimatedState", 0, 3000);
            pmonitor.setProgress(1);
            pmonitor.setNote("Generating path");
            double start = source.getLsfIndex().getStartTime();
            double end = source.getLsfIndex().getEndTime();
            for (IMCMessage s : it) {
                double progress = ((s.getTimestamp() - start) / (end - start)) * 30 + 1;
                pmonitor.setProgress((int)progress);
                LocationType loc = IMCUtils.parseLocation(s);
                loc.convertToAbsoluteLatLonDepth();

                int srcSys = s.getSrc();
                String systemName = source.getSystemName(srcSys);
                if (systemName == null || systemName.isEmpty()) {
                    continue;
                }
                Vector<LocationType> statesSys = pathsForSystems.get(systemName);
                if (statesSys == null) {
                    statesSys = new Vector<>();
                    pathsForSystems.put(systemName, statesSys);
                }
                IMCMessage lastEsSys = lastEstimatedStateForSystems.get(systemName);
                if (lastEsSys != null &&s.getTimestampMillis() -lastEsSys.getTimestampMillis() > secondsGapInEstimatedStateForPathBreak * 1E3) {
                    statesSys.add(null);
                }
                lastEstimatedStateForSystems.put(systemName, s);
                statesSys.add(loc);
                
                if (bottomRight == null) {
                    bottomRight = new LocationType(loc);
                    topLeft = new LocationType(loc);
                }

                if (loc.getLatitudeDegs() < bottomRight.getLatitudeDegs())
                    bottomRight.setLatitudeDegs(loc.getLatitudeDegs());
                else if (loc.getLatitudeDegs() > topLeft.getLatitudeDegs())
                    topLeft.setLatitudeDegs(loc.getLatitudeDegs());
                if (loc.getLongitudeDegs() < topLeft.getLongitudeDegs())
                    topLeft.setLongitudeDegs(loc.getLongitudeDegs());
                else if (loc.getLongitudeDegs() > bottomRight.getLongitudeDegs())
                    bottomRight.setLongitudeDegs(loc.getLongitudeDegs());

                // states.add(loc);
            }

            if (topLeft == null) {
                bw.close();
                throw new Exception("This log doesn't have required data (EstimatedState)");
            }
            pmonitor.setNote("Writing path to file");
            // bw.write(path(states, "Estimated State", "estate"));
            for (String sys : pathsForSystems.keySet()) {
                Vector<LocationType> st = pathsForSystems.get(sys);
                bw.write(path(st, "Estimated State " + sys, "estate"));
            }
            pmonitor.setProgress(70);
            PlanType plan = null;
            try {
                MissionType mt = LogUtils.generateMission(source);
                if (mt != null)
                    plan = LogUtils.generatePlan(mt, source);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
            if (plan != null) {
                pmonitor.setNote("Writing plan");
                bw.write(path(plan.planPath(), "Planned waypoints", "plan"));
                pmonitor.setProgress(90);
            }

            topLeft.translatePosition(50, -50, 0);
            bottomRight.translatePosition(-50, 50, 0);
            topLeft.convertToAbsoluteLatLonDepth();
            bottomRight.convertToAbsoluteLatLonDepth();

            if (exportSidescan) {
                pmonitor.setNote("Generating sidescan overlay");
                
                if (separateLineSegments) {
                    double lastTime = 0;
                    int count = 1;
                    for (Double seg : LogUtils.lineSegments(source)) {
                        bw.write(sidescanOverlay(out.getParentFile(), 6, topLeft, bottomRight, "sss" + count,
                                (long) (lastTime * 1000), (long) (seg * 1000), Ducer.both));
                        lastTime = seg;
                        count++;
                    }
                }
                else {
                    bw.write(sidescanOverlay(out.getParentFile(), 6, topLeft, bottomRight, Ducer.both));
                    if (separateTransducers) {
                        bw.write(sidescanOverlay(out.getParentFile(), 6, topLeft, bottomRight, Ducer.board));
                        bw.write(sidescanOverlay(out.getParentFile(), 6, topLeft, bottomRight, Ducer.starboard));
                    }
                }
            }

            if (exportBathymetry) {
                pmonitor.setNote("Generating bathymetry overlay");
                String mb = multibeamOverlay(out.getParentFile());
                if (!mb.isEmpty())
                    bw.write(mb);
                else {
                    WorldImage imgDvl = new WorldImage(1, ColorMapFactory.createJetColorMap());
                    imgDvl.setMaxVal(20d);
                    imgDvl.setMinVal(3d);
                    it = source.getLsfIndex().getIterator("EstimatedState", 0, 100);
                    for (IMCMessage s : it) {
                        LocationType loc = IMCUtils.parseState(s).getPosition();
                        double alt = s.getDouble("alt");
                        double depth = s.getDouble("depth");
                        if (alt == -1 || depth < MRAProperties.minDepthForBathymetry)
                            continue;
                        else
                            imgDvl.addPoint(loc, s.getDouble("alt"));
                    }
                    if (imgDvl.getAmountDataPoints() > 0) {
                        ImageIO.write(imgDvl.processData(), "PNG", new File(out.getParent(), "dvl_bath.png"));
                        bw.write(overlay(new File(out.getParent(), "dvl_bath.png"), "DVL Bathymetry", imgDvl.getSouthWest(),
                                imgDvl.getNorthEast(), visibilityForBathymetry));
                    }
                }
            }

            bw.write(kmlFooter());

            bw.close();

            if (pmonitor.isCanceled())
                return "Cancelled by the user";
            if (compressOutput) {
                pmonitor.setNote("Compressing output");
                
                System.out.println(new File(source.getFile("mra"), "Data.kmz"));
                System.out.println(new File(source.getFile("mra"), "kml"));
                ZipUtils.zipDir(new File(source.getFile("mra"), "Data.kmz").getAbsolutePath(), new File(source.getFile("mra"), "kml").getAbsolutePath());            
                
                pmonitor.setNote("Deleting old directory");
                FileUtil.deltree(out.getAbsolutePath());
                pmonitor.close();
                return "Log exported to " + new File(source.getFile("mra"), "Data.kmz").getAbsolutePath();
            }
            else
                return "Log exported to " + out.getAbsolutePath();
        }
        catch (Exception e) {
            GuiUtils.errorMessage("Error while exporting to KML", "Exception of type " + e.getClass().getSimpleName()
                    + " occurred: " + e.getMessage());
            e.printStackTrace();
            pmonitor.close();
            return null;
        }
    }

    public static void main(String[] args) {
        LocationType loc1 = new LocationType(41.08, -8.2343);
        LocationType loc2 = new LocationType(41.12, -8.2324);
        System.out.println(loc1.getDistanceInMeters(loc2));
        System.out.println(loc2.getDistanceInMeters(loc1));
        double[] res1 = loc2.getOffsetFrom(loc1);
        double[] res2 = WGS84Utilities.WGS84displacement(loc1.getLatitudeDegs(), loc1.getLongitudeDegs(), 0, loc2.getLatitudeDegs(), loc2.getLongitudeDegs(), 0);
        System.out.println(Math.sqrt(res2[0] * res2[0] + res2[1] * res2[1]));
        System.out.println(Math.sqrt(res1[0] * res1[0] + res1[1] * res1[1]));
    }

}
