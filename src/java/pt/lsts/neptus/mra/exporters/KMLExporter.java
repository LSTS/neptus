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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ProgressMonitor;

import org.imgscalr.Scalr;

import pt.lsts.imc.Elevator;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PopUp;
import pt.lsts.imc.StationKeeping;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColormapOverlay;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.WorldImage;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryParserFactory;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.api.LsfTreeSet;
import pt.lsts.neptus.mra.api.SidescanHistogramNormalizer;
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
import pt.lsts.neptus.types.map.PlanUtil;
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

/**
 * @author zp
 */
@PluginDescription(name="Export to KML")
public class KMLExporter implements MRAExporter {
    public double minHeight = 1000;
    public double maxHeight = -1;
    private IMraLogGroup source;
    private ProgressMonitor pmonitor;

    @NeptusProperty(category = "SideScan", name="Time Variable Gain")
    public double timeVariableGain = 45;

    @NeptusProperty(category = "SideScan", name="Normalization")
    public double normalization = 0.05;
    
    @NeptusProperty(category = "SideScan", name="Gain Normalization", description = "Perform an empirical analysis of the data to achieve optimal gain normalization.")
    public boolean egnNormalization = true;
    
    
    @NeptusProperty(category = "SideScan", name="Swath transparency")
    public double swathTransparency = 0.0;
    
    @NeptusProperty(category = "SideScan", name="Background Blur")
    public boolean bgBlur = true;
    

    @NeptusProperty(category="Output", name="Generated layers transparency")
    public double layerTransparency = 0;

    @NeptusProperty(category = "SideScan", name="Separate transducers")
    public boolean separateTransducers = false;

    @NeptusProperty(category = "SideScan", name="Separate line segments")
    public boolean separateLineSegments = false;

    @NeptusProperty(category = "SideScan", name="Slant Range Correction")
    public boolean slantRangeCorrection = true;
    
    @NeptusProperty(category = "SideScan", name="Pixel blending mode", description="How to blend multiple measurements on same location")
    public SideScanComposite.MODE blendMode = SideScanComposite.MODE.NONE;
    
    @NeptusProperty(category = "Output", name="Compress Output")
    public boolean compressOutput = true;

    public double maximumSidescanRange = 0;

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
    public boolean exportBathymetry = false;

    @NeptusProperty(category = "Export", name="Export Sidescan")
    public boolean exportSidescan = true;
    
    @NeptusProperty(category = "SideScan", name="Skip PopUps and SKs", description="Skip popup and station keeping maneuvers")
    public boolean skipSK = true;
    
    @NeptusProperty(category = "SideScan", name="Single Vehicle Survey", description="Do not use data from other vehicles")
    public boolean singleVehicle = true;
    
    @NeptusProperty(category = "SideScan", name="Margin in meters", description="Margins to be used to generate mosaic (from vehicle path)")
    public int margin = 150;
    
    @NeptusProperty(category = "SideScan", name="Resolution (px/meter)", description="Resolution of generated mosaic")
    public double sssResolution = 3;
        
    @NeptusProperty(category = "SideScan", name="Truncate Range (%)", description="Ignore data further than this range")
    public int truncRangePercent = 95;
    
    @NeptusProperty(category = "SideScan", name="Use Corrected positions", description="Use locations corrected by GPS")
    public boolean correctedPositions = true;
    
    @NeptusProperty(category = "SideScan", name="Sub System to process", description="In case of multiple frequencies / ranges")
    public int subSystem = 0;
    
    @NeptusProperty(category = "Export", name="Export Marks")
    public boolean exportMarks = true;
    
    
    
    public KMLExporter(IMraLogGroup source) {
        this.source = source;
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
        
        ret += "\t\t<Style id=\"s_ylw-pushpin\">";
        ret += "\t\t\t<IconStyle>";
        ret += "\t\t\t\t<color>80ffffff</color>";
        ret += "\t\t\t\t<scale>1.1</scale>";
        ret += "\t\t\t\t<Icon>";
        ret += "\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/ltblu-circle.png</href>";
        ret += "\t\t\t\t</Icon>";
        ret += "\t\t\t\t<hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>";
        ret += "\t\t\t</IconStyle>";
        ret += "\t\t\t<LabelStyle>";
        ret += "\t\t\t\t<color>80ffffff</color>";
        ret += "\t\t\t</LabelStyle>";
        ret += "\t\t\t<ListStyle>";
        ret += "\t\t\t\t<ItemIcon>";
        ret += "\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/paddle/ltblu-circle-lv.png</href>";
        ret += "\t\t\t\t</ItemIcon>";
        ret += "\t\t\t</ListStyle>";
        ret += "\t\t</Style>";
        ret += "\t\t<StyleMap id=\"mark\">";
        ret += "\t\t\t<Pair>";
        ret += "\t\t\t\t<key>normal</key>";
        ret += "\t\t\t\t<styleUrl>#s_ylw-pushpin</styleUrl>";
        ret += "\t\t\t</Pair>";
        ret += "\t\t\t<Pair>";
        ret += "\t\t\t\t<key>highlight</key>";
        ret += "\t\t\t\t<styleUrl>#s_ylw-pushpin_hl</styleUrl>";
        ret += "\t\t\t</Pair>";
        ret += "\t\t</StyleMap>";

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

    public String path(ArrayList<ManeuverLocation> coords, String name, String style) {
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

    public String folderOpen(String name) {
        String ret = "\t\t<Folder>\n";
        ret += "\t\t\t<name>" + name + "</name>\n";
        ret += "\t\t\t<open>0</open>\n";
        return ret;
    }

    public String folderClose() {
        String ret = "\t\t</Folder>\n";
        return ret;
    }

    public String markPlacemark(LocationType coord, String name, String style) {
        String ret = "\t\t\t<Placemark>\n";
        ret += "\t\t\t\t<name>" + name + "</name>\n";
        ret += "\t\t\t\t<styleUrl>#" + style + "</styleUrl>\n";
        ret += "\t\t\t\t<Point>\n";
        ret += "\t\t\t\t\t<gx:altitudeMode>clampToSeaFloor</gx:altitudeMode>\n";
        ret += "\t\t\t\t\t<coordinates> ";

        LocationType l = coord;
        l.convertToAbsoluteLatLonDepth();
        ret += l.getLongitudeDegs() + "," + l.getLatitudeDegs() + ","+(-l.getDepth())+"\n";// -" + l.getDepth()+"\n";
        
        ret += "\t\t\t\t\t</coordinates>\n";
        ret += "\t\t\t\t</Point>\n";
        ret += "\t\t\t</Placemark>\n";
        return ret;
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
            
            if (skipSK) {
                IMCMessage msg = source.getLsfIndex().getMessageAt("PlanControlState", state.getTimestamp());
                if (msg != null && msg.getAbbrev().equals("PlanControlState"))
                    switch(msg.getInteger("man_type")) {
                        case StationKeeping.ID_STATIC:
                        case PopUp.ID_STATIC:
                            System.out.println("skipping maneuver.");
                            continue;
                        default:
                            break;
                    }
            }

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
        CorrectedPosition positions = new CorrectedPosition(source);
        SidescanHistogramNormalizer histogram = null;
        if (egnNormalization) {
            histogram = SidescanHistogramNormalizer.create(source);
        }
        
        double totalProg = 100;
        double startProg = 100;

        // FIXME temporary fix
        boolean makeAbs = (ssParser instanceof JsfSidescanParser);

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
        
        
        int sys = ssParser.getSubsystemList().get(subSystem);
        SidescanParameters params = new SidescanParameters(normalization, timeVariableGain);
        if (egnNormalization)
            params = SidescanHistogramNormalizer.HISTOGRAM_DEFAULT_PARAMATERS;
        
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

            // check maneuver type and advance data from StationKeeping, Popup and Elevator maneuvers
            if (skipSK) {
                IMCMessage msg = source.getLsfIndex().getMessageAt("PlanControlState", time / 1000.0);
                if (msg != null && msg.getAbbrev().equals("PlanControlState"))
                    switch(msg.getInteger("man_type")) {
                        case StationKeeping.ID_STATIC:
                        case PopUp.ID_STATIC:
                        case Elevator.ID_STATIC:
                            continue;
                        default:
                            break;
                    }
            }
            
            ArrayList<SidescanLine> lines;
            try {
                lines = ssParser.getLinesBetween(time, time + 1000, sys, params);
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Integer margin = null;
            
            for (SidescanLine sl : lines) {
                double[] data = sl.getData();
                if (histogram != null)
                    data = histogram.normalize(data, sys);
                
                int truncRange = (int) (.01*truncRangePercent * sl.getRange());
                
                if (margin == null) {
                    margin = 0;
                    if (truncRange > 0 && truncRange < sl.getRange()) {
                        margin = (int) (((sl.getRange()-truncRange) / sl.getRange()) * (sl.getData().length / 2)); 
                    }
                }
                
                if (filter != null && !filter.isDataValid(new Date(sl.getTimestampMillis())))
                    continue;
                
                int widthPixels = (int) (sl.getRange() * resolution * 2);
                
                if (swath == null || swath.getWidth() != widthPixels)
                    swath = new BufferedImage(widthPixels, 1, BufferedImage.TYPE_INT_ARGB);

                int samplesPerPixel = (int) Math.round(1.0 * sl.getData().length / widthPixels);
                if (samplesPerPixel == 0)
                    continue;
                double sum = 0;
                int count = 0;

                int startPixel, endPixel;

               
                
                switch (ducer) {
                    case board:
                        startPixel = margin;
                        endPixel = data.length / 2;
                        filename = fname+"_board";
                        break;
                    case starboard:
                        startPixel = data.length / 2;
                        endPixel = data.length - margin;
                        filename = fname+"_starboard";
                        break;
                    default:
                        startPixel = margin;
                        endPixel = data.length - margin;
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
                        else {
                            double dist = Math.abs(i - data.length/2) / (double)data.length;
                            alpha = (int) (350*dist);
                        }
                        
                        if (pixelInImgToWrite >= 0 && pixelInImgToWrite < widthPixels) {
                            int color = cmap.getColor(val).getRGB() ^ ((alpha & 0xFF) << 24);
                            swath.setRGB(pixelInImgToWrite, 0, color);               
                        }
                        sum = 0;
                        count = 0;
                    }
                    if (!Double.isNaN(data[i]) && !Double.isInfinite(data[i])) { 
                        count++;
                        sum += data[i];
                    }
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                if (blendMode != SideScanComposite.MODE.NONE)
                    g2.setComposite(new SideScanComposite(blendMode));
                
                double[] pos;
                if (correctedPositions)
                    pos = positions.getPosition(sl.getTimestampMillis()/1000.0).getPosition().getOffsetFrom(topLeft);
                else
                    pos = sl.getState().getPosition().getOffsetFrom(topLeft);
                     
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
                lbl.repaint();
            }
        }

        frm.setVisible(false);
        frm.dispose();

        
        
        try {
            BufferedImage behind = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            
            // Fill in the empty pixels in the background
            behind.getGraphics().drawImage(img, 1, 0, null);
            behind.getGraphics().drawImage(img, -1, 0, null);
            behind.getGraphics().drawImage(img, 0, 1, null);
            behind.getGraphics().drawImage(img, 0, -1, null);            
            behind = Scalr.apply(behind, new com.jhlabs.image.GaussianFilter(25));  
            behind.getGraphics().drawImage(img, 0, 0, null);
            
            ImageIO.write(behind, "PNG", new File(dir, filename + ".png"));
            
            ImageLayer il = new ImageLayer("Sidescan mosaic from " + source.name(), behind, topLeft, bottomRight);
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
            il.saveAsPng(new File(dir.getParentFile(), filename + ".png"), true);
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

                if (Math.random() < 0.2)
                    continue;
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
        if (!MRAProperties.batchMode && PluginUtils.editPluginProperties(this, true))
            return I18n.text("Cancelled by the user.");

        try {
            pmonitor.setNote(I18n.text("Generating output dirs"));
            File out = new File(source.getFile("mra"), "kml");
            out.mkdirs();

            out = new File(out, "Data.kml");
            BufferedWriter bw = new BufferedWriter(new FileWriter(out));
            File f = source.getFile(".");
            String name = f.getCanonicalFile().getName();
            bw.write(kmlHeader(name));

            // To account for multiple systems paths
            Hashtable<String, ArrayList<ManeuverLocation>> pathsForSystems = new Hashtable<>();
            Hashtable<String, IMCMessage> lastEstimatedStateForSystems = new Hashtable<>();
           
            LocationType bottomRight = null, topLeft = null;

            int vehicle = -1;
            
            // main vehicle is the source of first message
            if (singleVehicle)
                vehicle = source.getLsfIndex().getMessage(0).getSrc();                
            
            // Path
            Iterable<IMCMessage> it = source.getLsfIndex().getIterator("EstimatedState", 0, 3000);
            pmonitor.setProgress(1);
            pmonitor.setNote(I18n.text("Generating path"));
            double start = source.getLsfIndex().getStartTime();
            double end = source.getLsfIndex().getEndTime();
            for (IMCMessage s : it) {
                if (vehicle > -1 && s.getSrc() != vehicle) // only consider main vehicle
                    continue;
                double progress = ((s.getTimestamp() - start) / (end - start)) * 30 + 1;
                pmonitor.setProgress((int)progress);
                LocationType loc = IMCUtils.parseLocation(s);
                loc.convertToAbsoluteLatLonDepth();

                int srcSys = s.getSrc();
                String systemName = source.getSystemName(srcSys);
                if (systemName == null || systemName.isEmpty()) {
                    continue;
                }
                ArrayList<ManeuverLocation> statesSys = pathsForSystems.get(systemName);
                if (statesSys == null) {
                    statesSys = new ArrayList<>();
                    pathsForSystems.put(systemName, statesSys);
                }
                IMCMessage lastEsSys = lastEstimatedStateForSystems.get(systemName);
                if (lastEsSys != null &&s.getTimestampMillis() -lastEsSys.getTimestampMillis() > secondsGapInEstimatedStateForPathBreak * 1E3) {
                    statesSys.add(null);
                }
                lastEstimatedStateForSystems.put(systemName, s);
                statesSys.add(new ManeuverLocation(loc));
                
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
            }

            if (topLeft == null) {
                bw.close();
                throw new Exception("This log doesn't have required data (EstimatedState)");
            }
            pmonitor.setNote(I18n.text("Writing paths to file"));
            for (String sys : pathsForSystems.keySet()) {
                ArrayList<ManeuverLocation> st = pathsForSystems.get(sys);
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
                pmonitor.setNote(I18n.text("Writing plan"));
                ArrayList<ManeuverLocation> locs = PlanUtil.getPlanWaypoints(plan);
                bw.write(path(locs, "Planned waypoints", "plan"));
                pmonitor.setProgress(90);
            }

            topLeft.translatePosition(margin, -margin, 0);
            bottomRight.translatePosition(-margin, margin, 0);
            topLeft.convertToAbsoluteLatLonDepth();
            bottomRight.convertToAbsoluteLatLonDepth();

            if (exportSidescan) {
                pmonitor.setNote(I18n.text("Generating sidescan overlay"));
                
                if (separateLineSegments) {
                    double lastTime = 0;
                    int count = 1;
                    for (Double seg : LogUtils.lineSegments(source)) {
                        bw.write(sidescanOverlay(out.getParentFile(), sssResolution, topLeft, bottomRight, "sss" + count,
                                (long) (lastTime * 1000), (long) (seg * 1000), Ducer.both));
                        lastTime = seg;
                        count++;
                    }
                }
                else {
                    bw.write(sidescanOverlay(out.getParentFile(), sssResolution, topLeft, bottomRight, Ducer.both));
                    if (separateTransducers) {
                        bw.write(sidescanOverlay(out.getParentFile(), sssResolution, topLeft, bottomRight, Ducer.board));
                        bw.write(sidescanOverlay(out.getParentFile(), sssResolution, topLeft, bottomRight, Ducer.starboard));
                    }
                }
            }

            if (exportBathymetry) {
                pmonitor.setNote(I18n.text("Generating bathymetry overlay"));
                String mb = multibeamOverlay(out.getParentFile());
                if (!mb.isEmpty())
                    bw.write(mb);
                else {
                    WorldImage imgDvl = new WorldImage(1, ColorMapFactory.createJetColorMap());
                    imgDvl.setMaxVal(MRAProperties.maxBathymDepth);
                    imgDvl.setMinVal(3d);
                    it = source.getLsfIndex().getIterator("EstimatedState", 0, 100);
                    for (IMCMessage s : it) {
                        LocationType loc = IMCUtils.parseState(s).getPosition();
                        double alt = s.getDouble("alt");
                        double depth = s.getDouble("depth");
                        if (alt == -1 || depth < MRAProperties.minDepthForBathymetry)
                            continue;
                        else
                            imgDvl.addPoint(loc, s.getDouble("alt") + s.getDouble("depth"));
                    }
                    if (imgDvl.getAmountDataPoints() > 0) {
                        ImageIO.write(imgDvl.processData(), "PNG", new File(out.getParent(), "dvl_bath.png"));
                        bw.write(overlay(new File(out.getParent(), "dvl_bath.png"), "DVL Bathymetry", imgDvl.getSouthWest(),
                                imgDvl.getNorthEast(), visibilityForBathymetry));
                    }
                }

                if (exportMarks) {
                    pmonitor.setNote(I18n.text("Generating marks placemarks"));
                    Collection<LogMarker> markersLst = LogMarker.load(source);
                    if (!markersLst.isEmpty()) {
                        bw.write(folderOpen("Marks"));
                        markersLst.forEach(m -> {
                            try {
                                bw.write(markPlacemark(m.getLocation(), m.getLabel(), "mark"));
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        bw.write(folderClose());
                    }
                }
            }

            bw.write(kmlFooter());

            bw.close();

            if (pmonitor.isCanceled())
                return I18n.text("Cancelled by the user");
            if (compressOutput) {
                pmonitor.setNote(I18n.text("Compressing output"));
                
                System.out.println(new File(source.getFile("mra"), "Data.kmz"));
                System.out.println(new File(source.getFile("mra"), "kml"));
                ZipUtils.zipDir(new File(source.getFile("mra"), "Data.kmz").getAbsolutePath(), new File(source.getFile("mra"), "kml").getAbsolutePath());            
                
                pmonitor.setNote("Deleting old directory");
                FileUtil.deltree(out.getAbsolutePath());
                pmonitor.close();
                return I18n.textf("Log exported to %path", new File(source.getFile("mra"), "Data.kmz").getAbsolutePath());
            }
            else
                return  I18n.textf("Log exported to %path", out.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
            GuiUtils.errorMessage(I18n.text("Error while exporting to KML"), I18n.textf(
                    "Exception of type %exception occurred: %message", e.getClass().getSimpleName(), e.getMessage()));
            e.printStackTrace();
            pmonitor.close();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        MRAProperties.batchMode = true;
        GuiUtils.setLookAndFeelNimbus();
        if (args.length == 0)
            BatchMraExporter.apply(KMLExporter.class);
        else {
            File[] roots = new File[args.length];
            for (int i = 0; i < roots.length; i++)
                roots[i] = new File(args[i]);

            LsfTreeSet set = new LsfTreeSet(roots);
            BatchMraExporter.apply(set, KMLExporter.class);
        }
    }
}
