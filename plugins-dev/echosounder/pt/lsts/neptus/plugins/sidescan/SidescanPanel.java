/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 30, 2012
 */
package pt.lsts.neptus.plugins.sidescan;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.imgscalr.Scalr;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.PropertiesProviders.SidescanConfig;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.SidescanLogMarker;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanPoint;
import pt.lsts.neptus.mra.replay.MraVehiclePosHud;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.VideoCreator;
import pt.lsts.neptus.util.llf.LsfReportProperties;
import pt.lsts.neptus.util.sidescan.SlantRangeImageFilter;

/**
 * MRA sidescan panel
 * 
 * @author jqcorreia
 * @author Manuel Ribeiro (new zoom)
 * @author pdias
 */
public class SidescanPanel extends JPanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;

    private static final int ZOOM_BOX_SIZE = 100;
    private static final int ZOOM_LAYER_BOX_SIZE = 300;

    //added 
    private static final int MAX_RULER_SIZE = 15;
    private long topZoomTimestamp = 0;
    private long bottomZoomTimestamp = 0;
    private List<SidescanLine> lines = Collections.synchronizedList(new ArrayList<SidescanLine>());
    private boolean isShowingZoomedImage = false;
    private long lastMouseMoveTS = 0;
    private ExecutorService threadExecutor = Executors.newCachedThreadPool();

    private SidescanAnalyzer parent;
    SidescanConfig config = new SidescanConfig();
    private SidescanToolbar toolbar = new SidescanToolbar(this);

    private SidescanParameters sidescanParams = new SidescanParameters(0, 0); // Initialize it to zero for now
    enum InteractionMode {
        NONE,
        INFO,
        MARK,
        MEASURE,
        MEASURE_HEIGHT;
    }

    private InteractionMode imode = InteractionMode.INFO;
    private MraVehiclePosHud posHud;

    private JPanel view = new JPanel() {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            try {
                super.paintComponent(g);

                if (image != null && layer != null) {
                    g.drawImage(image, 0, 0, null); // Draw sidescan image

                    Graphics2D lg2d = (Graphics2D) layer.getGraphics();
                    lg2d.setBackground(new Color(255, 255, 255, 0));
                    lg2d.clearRect(0, 0, layer.getWidth(), layer.getHeight()); // Clear layer image

                    drawMarks(layer.getGraphics());

                    if (measure && !parent.getTimeline().isRunning()) {
                        drawMeasure(layer.getGraphics());
                    }
                    else if (parent.getTimeline().isRunning()) { // clear points list if sidescan is running
                        pointList.clear();
                    }

                    if (measureHeight && !parent.getTimeline().isRunning()) {
                        drawMeasureHeight(layer.getGraphics());
                    }
                    else if (parent.getTimeline().isRunning()) { // clear points list if sidescan is running
                        measureHeightP = null;
                        measureHeightMouseX = Double.NaN;
                    }

                    if (zoom) {
                        Graphics2D gz = (Graphics2D) g.create();
                        gz.setColor(Color.WHITE);
                        drawZoom(gz); // Update layer with zoom information

                        Graphics2D zoomRuler = (Graphics2D) g.create();
                        zoomRuler.setColor(Color.WHITE);
                        drawZoomRuler(zoomRuler);  // Update layer with zoom ruler information
                    }

                    if (info)
                        drawInfo(layer.getGraphics()); // update layer with location information

                    drawRuler(layer.getGraphics());

                    g.drawImage(layer, 0, 0, null);

                    if (config.showPositionHud) {
                        posHud.setPathColor(config.pathColor);
                        g.drawImage(posHud.getImage((firstPingTime + currentTime) / 1000.0), 0, getHeight()
                                - config.hudSize, null);
                    }

                    if (record) {
                        creator.addFrame(image, firstPingTime + currentTime);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private BufferedImage image;
    private BufferedImage layer;
    private Graphics2D g2d;

    private long firstPingTime;
    // private long lastPingTime;

    private long currentTime;

    private long prevPingTime;

    private float rangeForRuler = 0;

    // Misc
    private BufferedImage bufferedCache;
    // private byte[] prevData = null;
    // private int[] iData = null;

    // Zoom
    private boolean zoom = false;

    // Measure
    private boolean measure = false;
    private ArrayList<SidescanPoint> pointList = new ArrayList<SidescanPoint>();

    // Measure Height
    private boolean measureHeight = false;
    private SidescanPoint measureHeightP = null;
    private double measureHeightMouseX = Double.NaN;

    // Info
    private boolean info = false;

    // Marking
    private boolean marking = false;
    private int initialX;
    private int initialY;

    // // Mouse related fields
    // private boolean mousePressed = false;
    // private boolean mouseEntered = false;

    private int mouseX, mouseY;
    // private SidescanPoint mouseSidescanPoint; // Mouse position geographical location
    private SidescanLine mouseSidescanLine;

    private BufferedImage mouseLocationImage = ImageUtils.createCompatibleImage(120, 71, Transparency.BITMASK);

    private List<SidescanLine> lineList = Collections.synchronizedList(new ArrayList<SidescanLine>());
    //    private ArrayList<SidescanLine> drawList = new ArrayList<SidescanLine>();
    //    private ArrayList<SidescanLine> removeList = new ArrayList<SidescanLine>();

    private NumberFormat altFormat = GuiUtils.getNeptusDecimalFormat(1);

    private SidescanParser ssParser;

    private String altStr = I18n.text("Altitude");
    // private String depthStr = I18n.text("Depth");
    private String rollStr = I18n.text("Roll");
    private String sRangeStr = I18n.text("S Range");
    private String hRangeStr = I18n.text("H Range");

    private int rangeForRulerStep;

    // private SlantRangeImageFilter filter;

    private int subsystem;

    private VideoCreator creator;

    protected boolean record = false;

    private Runnable updateLines = new Runnable() {
        @Override
        public void run() {
            synchronized (lines) {
                lines.clear();
                for (SidescanLine line : lineList) {
                    if (isBetweenTopAndBottom(line,bottomZoomTimestamp, topZoomTimestamp)) {
                        lines.add(line);
                    }
                }
            }
        }
    };

    private Runnable detectMouse = new Runnable() {
        @Override
        public void run() {
            boolean updated = false;
            while (true) {
                if (zoom) {
                    if (parent.getTimeline().isRunning()) 
                        updated = false;

                    while (!updated) {
                        try {
                            if (isMouseAtRest() && !parent.getTimeline().isRunning()) {
                                setSSLines(mouseY, null);
                                threadExecutor.execute(updateLines);
                                view.repaint();
                                updated=true;
                            }
                            Thread.sleep(500);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public SidescanPanel(SidescanAnalyzer analyzer, SidescanParser parser, int subsystem) {
        this.parent = analyzer;
        ssParser = parser;
        initialize();
        this.subsystem = subsystem;

        posHud = new MraVehiclePosHud(analyzer.mraPanel.getSource(), config.hudSize, config.hudSize);
    }

    private void initialize() {
        firstPingTime = ssParser.firstPingTimestamp();
        prevPingTime = firstPingTime;

        // Deal with panel resize by recreating the image buffers
        view.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                image = ImageUtils.createCompatibleImage(view.getWidth(), view.getHeight(), Transparency.OPAQUE);
                bufferedCache = ImageUtils.createCompatibleImage(view.getWidth(), view.getHeight(), Transparency.OPAQUE);
                g2d = (Graphics2D) image.getGraphics();
                layer = ImageUtils.createCompatibleImage(view.getWidth(), view.getHeight(), Transparency.TRANSLUCENT);
                clearLines();
                lines.clear();
            }
        });

        // Create SlantRangeFilter
        // filter = new SlantRangeImageFilter(0, pingParser.firstLogEntry().getDouble("max_range"),
        // pingParser.firstLogEntry().getRawData("data").length);
        view.addMouseListener(this);
        view.addMouseMotionListener(this);

        setLayout(new MigLayout("ins 0, gap 5"));
        add(toolbar, "w 100%, wrap");
        add(view, "w 100%, h 100%");

        threadExecutor.execute(detectMouse);
    }

    /**
     * To record a *.mp4 video from sidescan panel
     * 
     * @param r
     */
    void record(boolean r) {
        record = r;
        if (r) {
            try {
                creator = new VideoCreator(new File(parent.mraPanel.getSource().getFile("Data.lsf").getParent()
                        + "/mra/Sidescan_" + subsystem + ".mp4"), 800, 600);
                NeptusLog.pub().info("RECORDING TO Sidescan.mp4");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            if (creator != null)
                creator.closeStreams();
        }
    }

    void updateImage(long currentTime, long lastUpdateTime) {
        int yref = 0;
        this.currentTime = currentTime;

        ArrayList<SidescanLine> drawList = new ArrayList<SidescanLine>();
        ArrayList<SidescanLine> removeList = new ArrayList<SidescanLine>();

        sidescanParams.setNormalization(config.normalization);
        sidescanParams.setTvgGain(config.tvgGain);

        ArrayList<SidescanLine> list = ssParser.getLinesBetween(firstPingTime + lastUpdateTime, firstPingTime
                + currentTime, subsystem, sidescanParams);

        drawList.addAll(list);

        for (SidescanLine l : drawList) {
            // Deal with speed correction here, because this would be repeated code in the various parsers
            if (config.speedCorrection) {
                double horizontalScale = image.getWidth() / (l.range * 2f);
                double verticalScale = horizontalScale;

                double secondsElapsed = (l.timestampMillis - prevPingTime) / 1000f;
                double speed = l.state.getU();

                // Finally the 'height' of the ping in pixels
                int size = (int) (secondsElapsed * speed * verticalScale);

                if (size <= 0 || secondsElapsed > 0.5) {
                    l.ysize = 1;
                }
                else {
                    l.ysize = size;
                }
            }
            else {
                l.ysize = 1;
            }
            prevPingTime = l.timestampMillis;
            yref += l.ysize;
        }

        // This check is to prevent negative array indexes (from dragging too much)
        if (yref <= image.getHeight()) {
            ImageUtils.copySrcIntoDst(image, bufferedCache, 0, 0, image.getWidth(), image.getHeight() - yref, 0, 0,
                    image.getWidth(), image.getHeight());
            g2d.drawImage(bufferedCache, 0, yref, null);
        }
        else {
            yref = image.getHeight() - 1;
        }

        int d = 0;

        for (SidescanLine sidescanLine : drawList) {
            sidescanLine.ypos = yref - d;
            d += sidescanLine.ysize;
            sidescanLine.image = new BufferedImage(sidescanLine.data.length, 1, BufferedImage.TYPE_INT_RGB);

            // Apply colormap to data
            for (int c = 0; c < sidescanLine.data.length; c++) {
                sidescanLine.image.setRGB(c, 0, config.colorMap.getColor(sidescanLine.data[c]).getRGB());
            }

            if (config.slantRangeCorrection) {
                sidescanLine.image = Scalr.apply(sidescanLine.image, new SlantRangeImageFilter(sidescanLine.state.getAltitude(), sidescanLine.range, sidescanLine.image.getWidth()));
                sidescanLine.imageWithSlantRangeCorrection = true;
            }

            g2d.drawImage(ImageUtils.getScaledImage(sidescanLine.image, image.getWidth(), sidescanLine.ysize, true), 0,
                    sidescanLine.ypos, null);
            // g2d.drawImage(sidescanLine.image, 0, sidescanLine.ypos, null);

            // Update the rangeMax to the ruler
            if (!sidescanLine.imageWithSlantRangeCorrection && (sidescanLine.range != getRangeForRuler())) {
                setRangeForRuler(sidescanLine.range);
            }
            else if (sidescanLine.imageWithSlantRangeCorrection
                    && ((float) sidescanLine.getRangeSlantedCorrected() != getRangeForRuler())) {
                setRangeForRuler((float) sidescanLine.getRangeSlantedCorrected());
            }
        }
        synchronized (lineList) {
            SidescanLine sidescanLine;
            Iterator<SidescanLine> i = lineList.iterator(); // Must be in synchronized block
            while (i.hasNext()) {
                sidescanLine = i.next();
                sidescanLine.ypos += yref;
                if (sidescanLine.ypos > image.getHeight())
                    removeList.add(sidescanLine);
            }
            lineList.addAll(drawList);
            lineList.removeAll(removeList);
        }

        drawList.clear();
        removeList.clear();
    }

    private void drawZoom(Graphics g2) {
        if (mouseX == -1 && mouseY == -1)  {
            isShowingZoomedImage = false;
            return;
        }

        Graphics g = g2.create();
        
        isShowingZoomedImage = true;
        int zX = (int) MathMiscUtils.clamp(mouseX, ZOOM_BOX_SIZE / 2, image.getWidth() - ZOOM_BOX_SIZE / 2);
        int zY = (int) MathMiscUtils.clamp(mouseY, ZOOM_BOX_SIZE / 2, image.getHeight() - ZOOM_BOX_SIZE / 2);

        // Understand what we are zooming in.
        g.drawRect(zX - ZOOM_BOX_SIZE / 2, zY - ZOOM_BOX_SIZE / 2, ZOOM_BOX_SIZE, ZOOM_BOX_SIZE);

        if (parent.getTimeline().isRunning()) {

            BufferedImage zoomImage = image.getSubimage(zX - ZOOM_BOX_SIZE / 2, zY - ZOOM_BOX_SIZE / 2, ZOOM_BOX_SIZE, ZOOM_BOX_SIZE);
            BufferedImage zoomLayerImage = layer.getSubimage(zX - ZOOM_BOX_SIZE / 2, zY - ZOOM_BOX_SIZE / 2, ZOOM_BOX_SIZE, ZOOM_BOX_SIZE);

            // Draw zoomed image.
            g.drawImage(ImageUtils.getFasterScaledInstance(zoomImage, ZOOM_LAYER_BOX_SIZE, ZOOM_LAYER_BOX_SIZE),
                    image.getWidth() - (ZOOM_LAYER_BOX_SIZE + 1), image.getHeight() - (ZOOM_LAYER_BOX_SIZE + 1), null);
            g.drawImage(ImageUtils.getFasterScaledInstance(zoomLayerImage, ZOOM_LAYER_BOX_SIZE, ZOOM_LAYER_BOX_SIZE),
                    layer.getWidth() - (ZOOM_LAYER_BOX_SIZE + 1), layer.getHeight() - (ZOOM_LAYER_BOX_SIZE + 1), null);
        }
        else {
            threadExecutor.execute(updateLines);
            int ypos = lines.size();
            if (ypos < 100)  {
                isShowingZoomedImage = false;
                return;
            }
            synchronized (lines) {
                for (SidescanLine e : lines ) { 
                    e.ysize = 1;
                    int beginIndex = 0;
                    int endIndex = 0;
                    int leftMousePos = mouseX - ZOOM_BOX_SIZE / 2;
                    int rightMousePos = mouseX + ZOOM_BOX_SIZE / 2;

                    if (leftMousePos < 0) {
                        beginIndex = 0;
                        rightMousePos = ZOOM_BOX_SIZE;
                        endIndex = (rightMousePos * e.data.length ) / image.getWidth() ;
                    }
                    else if (rightMousePos > image.getWidth()) {
                        leftMousePos = image.getWidth() - ZOOM_BOX_SIZE;
                        beginIndex = (leftMousePos * e.data.length ) / image.getWidth() ;
                        endIndex = (image.getWidth() * e.data.length ) / image.getWidth() ;
                    }
                    else {
                        beginIndex = (leftMousePos * e.data.length ) / image.getWidth() ;
                        endIndex = (rightMousePos * e.data.length ) / image.getWidth() ;
                    }

                    e.image = new BufferedImage(endIndex-beginIndex, 1, BufferedImage.TYPE_INT_RGB);

                    // Apply colormap to data
                    for (int c = beginIndex; c < endIndex; c++) {
                        if (c >= e.data.length || c < 0)
                            break;

                        e.image.setRGB(c - beginIndex , 0, config.colorMap.getColor(e.data[c]).getRGB());
                    }

                    int vZoomScale = 3;
                    Image full = ImageUtils.getScaledImage(e.image, ZOOM_LAYER_BOX_SIZE, vZoomScale, true);
                    g.drawImage(full, layer.getWidth() - (ZOOM_LAYER_BOX_SIZE + 1), layer.getHeight() + (ZOOM_BOX_SIZE) - ypos, null);
                    ypos = ypos + vZoomScale;
                }
            }
        }

        // Mouse center indicator
        g.setColor(Color.CYAN);
        g.drawRect(image.getWidth() - (ZOOM_LAYER_BOX_SIZE / 2 + 1) - 3, image.getHeight() - (ZOOM_LAYER_BOX_SIZE / 2 + 1) - 3, 6, 6);
        
        g.dispose();
    }

    private void drawInfo(Graphics g) {
        if (mouseSidescanLine != null) {
            LocationType loc = convertImagePointXToLocation(mouseX, mouseSidescanLine, mouseSidescanLine.imageWithSlantRangeCorrection);
            double dist = mouseSidescanLine.state.getPosition().getNewAbsoluteLatLonDepth().getDistanceInMeters(loc);
            String distStr = mouseSidescanLine.imageWithSlantRangeCorrection ? hRangeStr : sRangeStr;
            
            Graphics2D location2d = (Graphics2D) mouseLocationImage.getGraphics();
            location2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            location2d.clearRect(0, 0, mouseLocationImage.getWidth(), mouseLocationImage.getHeight());
            location2d.drawString(
                    CoordinateUtil.dmToLatString(CoordinateUtil.decimalDegreesToDM(loc.getLatitudeDegs())), 5, 15);
            location2d.drawString(
                    CoordinateUtil.dmToLonString(CoordinateUtil.decimalDegreesToDM(loc.getLongitudeDegs())), 5, 26);
            location2d.drawString(altStr + ": " + altFormat.format(mouseSidescanLine.state.getAltitude()), 5, 37);
            location2d.drawString(rollStr + ": " + altFormat.format(Math.toDegrees(mouseSidescanLine.state.getRoll())),
                    5, 48);
            location2d.drawString(distStr+": " + altFormat.format(dist) + " m", 5, 59);

            g.drawImage(mouseLocationImage, 10, 20, null);
        }
    }

    private void drawMeasure(Graphics g) {
        int c = 0;
        double distance;
        SidescanPoint prevPoint = null;
        g.setColor(Color.GREEN);

        g.drawRect(3, 3, 6, 6);

        for (SidescanPoint point : pointList) {
            int pointX = convertSidescanLinePointXToImagePointX(point.x, point.line);
//            System.out.println(" " + point.x);
            
            SidescanPoint ptSlant = point.line.calcPointForCoord(point.x, false);
            SidescanPoint ptNoSlant = point.line.calcPointForCoord(point.x, true);
//            System.out.printf("Distances >> %f vs %f\n",
//                    point.line.state.getPosition().getNewAbsoluteLatLonDepth().getDistanceInMeters(ptSlant.location),
//                    point.line.state.getPosition().getNewAbsoluteLatLonDepth().getDistanceInMeters(ptNoSlant.location));
            
            if (c == 0) {
                g.drawRect(pointX - 3, point.y - 3, 6, 6);
            }
            else {
                int prevPointX = convertSidescanLinePointXToImagePointX(prevPoint.x, prevPoint.line);

                SidescanPoint prevPtSlant = prevPoint.line.calcPointForCoord(prevPoint.x, false);
                SidescanPoint prevPtNoSlant = prevPoint.line.calcPointForCoord(prevPoint.x, true);
//                System.out.printf("Distances prev >> %f vs %f\n",
//                        point.line.state.getPosition().getNewAbsoluteLatLonDepth().getDistanceInMeters(prevPtSlant.location),
//                        point.line.state.getPosition().getNewAbsoluteLatLonDepth().getDistanceInMeters(prevPtNoSlant.location));

                double distSlant = prevPtSlant.location.getDistanceInMeters(ptSlant.location);
                double distNoSlant = prevPtNoSlant.location.getDistanceInMeters(ptNoSlant.location);
                distSlant = (int) (distSlant * 1000) / 1000.0;
                distNoSlant = (int) (distNoSlant * 1000) / 1000.0;
                
//                distance = prevPoint.location.getDistanceInMeters(point.location);
//                distance = (int) (distance * 1000) / 1000.0;

                g.drawLine(prevPointX, prevPoint.y, pointX, point.y);
                g.drawRect(pointX - 3, point.y - 3, 6, 6);
                g.setColor(Color.BLACK);
                g.drawString(distNoSlant + "m", (prevPointX + pointX) / 2 + 3, (prevPoint.y + point.y) / 2 - 1);
                g.setColor(Color.GREEN);
                g.drawString(distNoSlant + "m", (prevPointX + pointX) / 2 + 4, (prevPoint.y + point.y) / 2);
            }
            prevPoint = point;
            c++;
        }
    }

    private void drawMeasureHeight(Graphics g2) {
        Graphics g = g2.create();
        g.setColor(Color.GREEN);
        
        if (measureHeightP != null) {
            int pointX = convertSidescanLinePointXToImagePointX(measureHeightP.x, measureHeightP.line);
            g.drawRect(pointX - 3, measureHeightP.y - 3, 6, 6);
            
            if (!Double.isNaN(measureHeightMouseX)) {
                int ssP = convertImagePointXToSidescanLinePointX((int) measureHeightMouseX, measureHeightP.line);
                g.drawRect((int) (measureHeightMouseX - 3), measureHeightP.y - 3, 6, 6);
                
                double p1 = measureHeightP.line.getDistanceForCoord(measureHeightP.x, false);
                double p2 = measureHeightP.line.getDistanceForCoord(ssP, false);
                
                double l = Math.abs(p2 - p1);
                double a = measureHeightP.line.state.getAltitude();
                double r = Math.abs(Math.max(p1, p2));
                double h = l * a / r;
                h = (int) (h * 1000) / 1000.0;

                g.drawLine((int) measureHeightMouseX, measureHeightP.y, pointX, measureHeightP.y);
                g.drawRect(pointX - 3, measureHeightP.y - 3, 6, 6);
                g.setColor(Color.BLACK);
                g.drawString(h + "m", ((int) measureHeightMouseX + pointX) / 2 + 3, measureHeightP.y - 5);
                g.setColor(Color.GREEN);
                g.drawString(h + "m", ((int) measureHeightMouseX + pointX) / 2 + 4, measureHeightP.y - 4);
            }
        }
        g.dispose();
    }

    private void drawMarks(Graphics g2) {
        if (marking) {
            int x = Math.min(initialX, mouseX);
            int y = Math.min(initialY, mouseY);
            int w = Math.max(initialX, mouseX) - Math.min(initialX, mouseX);
            int h = Math.max(initialY, mouseY) - Math.min(initialY, mouseY);
            g2.drawRect(x, y, w, h);
        }

        SidescanLine old = null;
        Graphics2D g = (Graphics2D) g2.create();
        for (LogMarker m : parent.getMarkerList()) {
            long timestamp = new Double(m.getTimestamp()).longValue();

            Color color = Color.WHITE;
            Color colorConstrast = Color.BLACK;

            SidescanLine line;

            synchronized (lineList) {
                Iterator<SidescanLine> i = lineList.iterator();
                while (i.hasNext()) {
                    line = i.next();
                    if (old != null) {
                        // In case of being a marker just with time information
                        if (timestamp >= old.timestampMillis && timestamp <= line.timestampMillis) {
                            if (m instanceof SidescanLogMarker) {
                                SidescanLogMarker slm = (SidescanLogMarker) m;
                                double scale = (image.getWidth() / 2) / line.range;

                                int x = (int) ((image.getWidth() / 2) + (slm.x * scale));
                                g.setColor(color);
                                g.drawRect(x - (slm.w / 2), line.ypos - (slm.h / 2), slm.w, slm.h);
                                g.setColor(colorConstrast);
                                g.drawString(m.getLabel(), x - (slm.w / 2) - 1, line.ypos - (slm.h / 2) - 10 - 1);
                                g.setColor(color);
                                g.drawString(m.getLabel(), x - (slm.w / 2), line.ypos - (slm.h / 2) - 10);
                            }
                            else {
                                g.setColor(color);
                                g.fillRect(0, line.ypos - 1, 10, 2);
                                g.fillRect(line.image.getWidth(null) - 10, line.ypos - 1, 10, 2);
                                g.setColor(colorConstrast);
                                g.drawString(m.getLabel(), 0 - 1, line.ypos - 10 - 1);
                                g.setColor(color);
                                g.drawString(m.getLabel(), 0, line.ypos - 10);
                            }
                            break; // ??
                        }
                    }
                    old = line;
                }
            }
        }
        g.dispose();
    }

    /**
     * @param zoomRuler
     */
    private void drawZoomRuler(Graphics g) {
        if (!isShowingZoomedImage)
            return;

        Graphics2D g2d = (Graphics2D) g.create();
        int fontSize = 11;
        int x = layer.getWidth() - (ZOOM_LAYER_BOX_SIZE + 1);
        int y = layer.getHeight() - (ZOOM_LAYER_BOX_SIZE);
        // Draw Horizontal Line
        g2d.setColor(Color.BLACK);
        g2d.drawLine(x, y, layer.getWidth(), y);

        Rectangle drawRulerHere = new Rectangle(x, y - MAX_RULER_SIZE, ZOOM_LAYER_BOX_SIZE + 1, MAX_RULER_SIZE);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fill(drawRulerHere);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        g2d.setColor(Color.BLACK);

        // Draw top line
        g2d.drawLine(x, y - MAX_RULER_SIZE, layer.getWidth(), y - MAX_RULER_SIZE);

        // Draw the zero
        g2d.drawLine(x, y, x , y-MAX_RULER_SIZE);
        // g2d.drawString("0", x+5, y-3);

        float zoomRange  = (ZOOM_BOX_SIZE * (rangeForRuler*2f)) / layer.getWidth();
        float zoomRangeStep = 1;

        double step = ((zoomRangeStep * ZOOM_LAYER_BOX_SIZE) / zoomRange);
        double r = zoomRangeStep;

        int c = x + (int) step;

        for (; c<=layer.getWidth(); c += step , r += zoomRangeStep) {
            g2d.drawLine(c, y, c, y - MAX_RULER_SIZE);
            g2d.drawString("" + (int) r, c - 13, y-3);
        }
        
        g2d.dispose();
    }

    private void drawRuler(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        int fontSize = 11;

        // Draw Horizontal Line
        g2d.drawLine(0, 0, layer.getWidth(), 0);

        Rectangle drawRulerHere = new Rectangle(0, 0, layer.getWidth(), MAX_RULER_SIZE);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fill(drawRulerHere);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        g2d.setColor(Color.BLACK);

        // Draw top line
        g2d.drawLine(0, 0, layer.getWidth(), 0);

        // Draw the zero
        g2d.drawLine(layer.getWidth() / 2, 0, layer.getWidth() / 2, MAX_RULER_SIZE);
        g2d.drawString("0", layer.getWidth() / 2 - 10, fontSize);

        // Draw the axes
        g2d.drawLine(0, 0, 0, 15);
        g2d.drawString("" + (int) rangeForRuler, 2, 11);

        g2d.drawLine(layer.getWidth() - 1, 0, layer.getWidth() - 1, MAX_RULER_SIZE);
        g2d.drawString("" + (int) rangeForRuler, layer.getWidth() - 20, fontSize);

        double step = (layer.getWidth() / ((rangeForRuler * 2) / rangeForRulerStep));
        double r = rangeForRulerStep;

        int c1 = (int) (layer.getWidth() / 2 - step);
        int c2 = (int) (layer.getWidth() / 2 + step);

        for (; c1 > 0; c1 -= step, c2 += step, r += rangeForRulerStep) {
            g2d.drawLine(c1, 0, c1, MAX_RULER_SIZE);
            g2d.drawLine(c2, 0, c2, MAX_RULER_SIZE);
            g2d.drawString("" + (int) r, c1 + 5, fontSize);
            g2d.drawString("" + (int) r, c2 - 20, fontSize);
        }
        
        g2d.dispose();
    }

    private int convertSidescanLinePointXToImagePointX(int sidescanLineX, SidescanLine sidescanLine) {
//        return convertSidescanLinePointXToImagePointX(sidescanLineX, sidescanLine.xsize,
//                slantRangeCorrection);
        int sidescanLineXSize = sidescanLine.xsize;
        // sidescanLineX = 212;
        if (!sidescanLine.imageWithSlantRangeCorrection) {
            return (int) (sidescanLineX / (sidescanLineXSize / (float) image.getWidth()));
        }
        else {
            int imgWidth = image.getWidth();
            int sspoints = sidescanLine.data.length;
            double ximg = sidescanLineX * imgWidth / sspoints;
            double hInImg = sidescanLine.state.getAltitude() * (imgWidth / (sidescanLine.range * 2));
            double dInImg = imgWidth / 2 - ximg;
            double d = imgWidth / 2 - Math.signum(dInImg) * Math.sqrt(dInImg * dInImg - hInImg * hInImg);
//            System.out.println("imgW " + imgWidth + "  ssW " + sspoints + "  range " + sidescanLine.range);
//            System.out.println("ss2img " + sidescanLineX + ">>" + ximg + ">>" + d + " vs " + (int) (sidescanLineX / (sidescanLineXSize / (float) image.getWidth())));
            return (int) d;
        }
    }

//    private int convertSidescanLinePointXToImagePointX(int sidescanLineX, int sidescanLineXSize,
//            boolean slantRangeCorrection) {
//        return (int) (sidescanLineX / (sidescanLineXSize / (float) image.getWidth()));
//    }

    private int convertImagePointXToSidescanLinePointX(int imageMouseX, SidescanLine sidescanLine) {
//        return convertImagePointXToSidescanLinePointX(imageMouseX, sidescanLine.xsize, slantRangeCorrection);
        int sidescanLineXSize = sidescanLine.xsize;
        if (!sidescanLine.imageWithSlantRangeCorrection) {
            return (int) (imageMouseX * (sidescanLineXSize / (float) image.getWidth()));
        }
        else {
            // imageMouseX = 111;
            int imgWidth = image.getWidth();
            int sspoints = sidescanLine.data.length;
            double hInImg = sidescanLine.state.getAltitude() * (imgWidth / (sidescanLine.range * 2));
            double d1 =  Math.signum(imageMouseX - imgWidth / 2) * Math.sqrt(Math.pow(imageMouseX - imgWidth / 2, 2) + hInImg * hInImg);
            double x1 = d1 + imgWidth / 2;
            double valCalcSSpx = x1 * sspoints / imgWidth;
//            System.out.println("imgW " + imgWidth + "  ssW " + sspoints + "  range " + sidescanLine.range);
//            System.out.println("img2ss " + imageMouseX + ">>" + x1 + ">>" + valCalcSSpx+ " vs " + (int) (imageMouseX * (sidescanLineXSize / (float) image.getWidth())));
            return (int) valCalcSSpx;
        }
    }

//    private int convertImagePointXToSidescanLinePointX(int imageMouseX, int sidescanLineXSize,
//            boolean slantRangeCorrection) {
//        if (!slantRangeCorrection) {
//            return (int) (imageMouseX * (sidescanLineXSize / (float) image.getWidth()));
//        }
//        else {
//        }
//    }

    /**
     * Method to convert from mouse click x point in the image to sidescan x point.
     * 
     * @param imageMouseX
     * @param sidescanLine
     * @return
     */
    private SidescanPoint convertImagePointXToSidescanPoint(int imageMouseX, SidescanLine sidescanLine,
            boolean slantRangeCorrection) {
        return sidescanLine.calcPointForCoord(
                convertImagePointXToSidescanLinePointX(imageMouseX, sidescanLine),
                slantRangeCorrection);
    }

    private LocationType convertImagePointXToLocation(int imageMouseX, SidescanLine sidescanLine,
            boolean slantRangeCorrection) {
        return convertImagePointXToSidescanPoint(imageMouseX, sidescanLine, slantRangeCorrection).location;
    }

    /**
     * @return the rangeForRuler
     */
    private float getRangeForRuler() {
        return rangeForRuler;
    }

    /**
     * @param rangeForRuler the rangeForRuler to set
     */
    private void setRangeForRuler(float rangeForRuler) {
        this.rangeForRuler = rangeForRuler;
        rangeForRulerStep = this.rangeForRuler < 10 ? 1 : 10;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void clearLines() {
        lineList.clear();
        image.getGraphics().clearRect(0, 0, image.getWidth(), image.getHeight());
    }

    /**
     * Set this panel Interaction Mode
     * 
     * @param imode mode to set (see InteractionMode enum)
     */
    public void setInteractionMode(InteractionMode imode) {
        // For now clear Measure Interaction Mode structures here //FIXME
        measure = false;
        pointList.clear();
        measureHeight = false;
        measureHeightP = null;
        measureHeightMouseX = Double.NaN;
        
        this.imode = imode;
    }

    /**
     * Define if zoom layer is active.
     * @param zoomSelected true if zoom is selected.
     */
    public void setZoom(boolean zoomSelected) {
        this.zoom = zoomSelected;
    }

    boolean isBetweenTopAndBottom(SidescanLine line, long bottomTS, long topTS) {
        if (bottomTS <= line.timestampMillis && line.timestampMillis <= topTS )
            return true;

        return false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (image == null )
            return;
        mouseX = e.getX();
        mouseY = e.getY();
        lastMouseMoveTS = System.nanoTime();
        setSSLines(mouseY, e);
    }

    /*
     * @param y , mouse Y coordinate to retrieve sidescanLine from
     * also sets top and bottom timestamp based on the mouseSidescanLine, for zoom function.
     */
    private void setSSLines(int y, MouseEvent e) {
        int Y = (int) MathMiscUtils.clamp(mouseY, ZOOM_BOX_SIZE / 2, image.getHeight() - ZOOM_BOX_SIZE / 2);
        synchronized (lineList) {
            Iterator<SidescanLine> i = lineList.iterator(); // Must be in synchronized block
            while (i.hasNext()) {
                SidescanLine line = i.next();

                if (y >= line.ypos && y <= (line.ypos + line.ysize)) {
                    mouseSidescanLine = line;
                    if (e!=null) ((JPanel) e.getSource()).repaint();
                }

                // save bottom and top timestamps for zoom box according to mouse position
                if (mouseY < ZOOM_BOX_SIZE / 2) { 
                    if (line.ypos == 1) 
                        topZoomTimestamp = line.timestampMillis;                        

                    if (line.ypos == ZOOM_BOX_SIZE) 
                        bottomZoomTimestamp = line.timestampMillis;
                } 
                else {
                    if ((line.ypos + (ZOOM_BOX_SIZE/2 ) <= Y) && Y <= (line.ypos + (ZOOM_BOX_SIZE/2 ) + line.ysize))
                        topZoomTimestamp = line.timestampMillis;
                }
                if ((line.ypos - (ZOOM_BOX_SIZE/2 ) <= Y) && Y <= (line.ypos - (ZOOM_BOX_SIZE/2 ) + line.ysize))
                    bottomZoomTimestamp = line.timestampMillis;
            }
        }
    };

    private boolean isMouseAtRest() {
        long now = System.nanoTime();
        if (now - 1000000000 > lastMouseMoveTS)
            return true;

        return false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        int y = e.getY();
        for (SidescanLine line : lineList.toArray(new SidescanLine[0])) {
            if (y >= line.ypos && y <= (line.ypos + line.ysize)) {
                mouseSidescanLine = line;
                ((JPanel) e.getSource()).repaint();
            }
        }
        ((JPanel) e.getSource()).repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        if (e.getButton() == MouseEvent.BUTTON1) {
            for (SidescanLine line : lineList) {
                if (mouseY >= line.ypos && mouseY <= (line.ypos + line.ysize)) {
                    mouseSidescanLine = line;
                    ((JPanel) e.getSource()).repaint();
                    break;
                }
            }

            if (imode == InteractionMode.MARK && !parent.getTimeline().isRunning()) {
                if (LsfReportProperties.generatingReport==true){
                    GuiUtils.infoMessage(getRootPane(), I18n.text("Can not add Marks"), I18n.text("Can not add Marks - Generating Report."));
                    return;
                }

                marking = true;
                initialX = mouseX;
                initialY = mouseY;
            }
            else if (imode == InteractionMode.MEASURE && !parent.getTimeline().isRunning()) {
                measure = true;
                // int x = (int) (mouseX * (mouseSidescanLine.xsize / (float)image.getWidth()));
                int x = convertImagePointXToSidescanLinePointX(mouseX, mouseSidescanLine);
//                System.out.println("x " + x + "   from  mouseX " + mouseX);
                
                pointList.add(mouseSidescanLine.calcPointForCoord(x, mouseSidescanLine.imageWithSlantRangeCorrection));

                if (pointList.size() > 2) {
                    pointList.clear();
                }
            }
            else if (imode == InteractionMode.MEASURE_HEIGHT && !parent.getTimeline().isRunning()) {
                measureHeight = true;
                if (measureHeightP != null && !Double.isNaN(measureHeightMouseX)) {
                    measureHeightP = null;
                    measureHeightMouseX = Double.NaN;
                }
                if (measureHeightP == null) {
                    int x = convertImagePointXToSidescanLinePointX(mouseX, mouseSidescanLine);
                    measureHeightP = mouseSidescanLine.calcPointForCoord(x, mouseSidescanLine.imageWithSlantRangeCorrection);
                }
                else {
                    measureHeightMouseX = mouseX;
                }
            }
            else if (imode == InteractionMode.INFO) {
                info = true;
            }
            ((JPanel) e.getSource()).repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        info = false;

        if (marking) {
            String res = JOptionPane.showInputDialog("Insert marker name");

            // Check for a valid response
            if (res != null) {
                // Find the corresponding SidescanLine object
                SidescanLine lInit = null;
                for (SidescanLine line : lineList) {
                    if (initialY >= line.ypos && initialY <= (line.ypos + line.ysize)) {
                        lInit = line;
                        break;
                    }
                }
                int initialPointXSidescan = convertImagePointXToSidescanLinePointX(initialX, lInit);
                int mousePointXSidescan = convertImagePointXToSidescanLinePointX(mouseX, mouseSidescanLine);
                int x = (mousePointXSidescan + initialPointXSidescan) / 2;
                int y = (mouseY + initialY) / 2;

                // Find the corresponding SidescanLine object
                SidescanLine l = null;
                for (SidescanLine line : lineList) {
                    if (y >= line.ypos && y <= (line.ypos + line.ysize)) {
                        l = line;
                        break;
                    }
                }

                SidescanPoint point = l.calcPointForCoord(x, l.imageWithSlantRangeCorrection);

                // Distance to line center point, negative values mean portboard
                double distanceToNadir = l.state.getPosition().getHorizontalDistanceInMeters(point.location);
                distanceToNadir *= (x > l.xsize / 2 ? 1 : -1);

                int x1 = Math.min(mousePointXSidescan, initialPointXSidescan);
                int x2 = Math.max(mousePointXSidescan, initialPointXSidescan);

                SidescanPoint p1 = l.calcPointForCoord(x1, l.imageWithSlantRangeCorrection);
                SidescanPoint p2 = l.calcPointForCoord(x2, l.imageWithSlantRangeCorrection);

                double d1 = l.state.getPosition().getHorizontalDistanceInMeters(p1.location);
                d1 *= (x1 > l.xsize / 2 ? 1 : -1);

                double d2 = l.state.getPosition().getHorizontalDistanceInMeters(p2.location);
                d2 *= (x2 > l.xsize / 2 ? 1 : -1);

                d1 += l.range;
                d2 += l.range;
                double wMeters = d2 - d1;

                parent.mraPanel.addMarker(new SidescanLogMarker(res, l.timestampMillis, point.location
                        .getLatitudeRads(), point.location.getLongitudeRads(), distanceToNadir, y, Math.abs(mouseX
                                - initialX), Math.abs(mouseY - initialY), wMeters, subsystem, config.colorMap));
            }
            marking = false;
        }

        if (imode != InteractionMode.MEASURE) {
            measure = false;
            pointList.clear();
        }
        if (imode != InteractionMode.MEASURE_HEIGHT) {
            measureHeight = false;
            measureHeightP = null;
            measureHeightMouseX = Double.NaN;
        }

        ((JPanel) e.getSource()).repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {  
        mouseX = mouseY = -1;
        repaint();
    }
}
