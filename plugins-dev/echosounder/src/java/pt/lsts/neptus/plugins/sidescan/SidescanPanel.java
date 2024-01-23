/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 30, 2012
 */
package pt.lsts.neptus.plugins.sidescan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import org.imgscalr.Scalr;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.propertiesproviders.SidescanConfig;
import pt.lsts.neptus.gui.TimelineChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.SidescanLogMarker;
import pt.lsts.neptus.mra.api.SidescanGuiUtils;
import pt.lsts.neptus.mra.api.SidescanHistogramNormalizer;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanPoint;
import pt.lsts.neptus.mra.api.SidescanUtil;
import pt.lsts.neptus.mra.replay.MraVehiclePosHud;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ColorUtils;
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

    private static final int BULLSEYE_HIDE_TIMEOUT_MILLIS = 5000;

    private static final int ZOOM_BOX_SIZE = 100;
    private static final int ZOOM_LAYER_BOX_SIZE = 300;

    //added
    private static final int MAX_RULER_SIZE = 15;
    private long topZoomTimestamp = 0;
    private long bottomZoomTimestamp = 0;
    private final List<SidescanLine> lines = Collections.synchronizedList(new ArrayList<>());
    private boolean isShowingZoomedImage = false;
    private long lastMouseMoveTS = 0;
    private final ScheduledExecutorService threadExecutor = Executors.newScheduledThreadPool(4);

    private final SidescanAnalyzer parent;
    SidescanConfig config = new SidescanConfig();
    private final SidescanToolbar toolbar = new SidescanToolbar(this);

    private final SidescanParameters sidescanParams = new SidescanParameters(0, 0); // Initialize it to zero for now

    enum InteractionMode {
        NONE,
        INFO,
        MARK,
        MEASURE,
        MEASURE_HEIGHT
    }

    private InteractionMode imode = InteractionMode.INFO;
    private final MraVehiclePosHud posHud;

    /**
     * Fix old marks related enum
     */
    private enum Operation {
        EXIT_CHANGE,
        EXIT_CANCEL,
        TEST_CHANGE,
        TEST_ORIG,
    }

    /**
     * Fix old marks related class
     */
    private static class SSCorrection {
        public SidescanLogMarker marker;

        public double latRadsSlant;
        public double latRadsHorizontal;

        public double lonRadsSlant;
        public double lonRadsHorizontal;

        public double distanceToNadirSlant = Double.NaN;
        public double distanceToNadirHorizontal = Double.NaN;

        public double wMetersSlant = Double.NaN;
        public double wMetersHorizontal = Double.NaN;
    }

    private final JPanel view = new JPanel() {
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

                    if (info) {
                        drawInfo(layer.getGraphics()); // update layer with location information
                    }

                    drawRuler(layer.getGraphics());

                    g.drawImage(layer, 0, 0, null);

                    if (zoom) {
                        Graphics2D gz = (Graphics2D) g.create();
                        gz.setColor(Color.WHITE);
                        drawZoom(gz); // Update layer with zoom information

                        Graphics2D zoomRuler = (Graphics2D) g.create();
                        zoomRuler.setColor(Color.WHITE);
                        drawZoomRuler(zoomRuler);  // Update layer with zoom ruler information
                    }

                    if (config.showPositionHud) {
                        posHud.setPathColor(config.pathColor);
                        int x = getWidth() / 2 - config.hudSize / 2; // 0;
                        int y = getHeight() - config.hudSize;
                        g.drawImage(posHud.getImage((firstPingTime + currentTime) / 1000.0), x, y, null);
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
    private final ArrayList<SidescanPoint> pointList = new ArrayList<>();

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

    private final BufferedImage mouseLocationImage = ImageUtils.createCompatibleImage(120, 98, Transparency.BITMASK);

    private final List<SidescanLine> lineList = Collections.synchronizedList(new ArrayList<>());
    //    private ArrayList<SidescanLine> drawList = new ArrayList<SidescanLine>();
    //    private ArrayList<SidescanLine> removeList = new ArrayList<SidescanLine>();

    private final NumberFormat altFormat = GuiUtils.getNeptusDecimalFormat(1);

    private final SidescanParser ssParser;

    private final String altStr = I18n.text("Altitude");
    private final String depthStr = I18n.text("Depth");
    private final String rollStr = I18n.text("Roll");
    private final String yawStr = I18n.text("Yaw");
    private final String sRangeStr = I18n.text("S Range");
    private final String hRangeStr = I18n.text("H Range");

    private int rangeForRulerStep;

    // private SlantRangeImageFilter filter;

    private final int subsystem;

    private VideoCreator creator;

    protected boolean record = false;

    private final Runnable updateLines = () -> {
        synchronized (lines) {
            lines.clear();
            for (SidescanLine line : lineList) {
                if (isBetweenTopAndBottom(line, bottomZoomTimestamp, topZoomTimestamp)) {
                    lines.add(line);
                }
            }
            view.repaint();
        }
    };

    private TimelineChangeListener timelineChangeListener;
    private final Object lockTimelineChangeListenerExecution = new Object();
    private ScheduledFuture<?> timelineChangeListenerExecution;

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

        // threadExecutor.execute(detectMouse);

        timelineChangeListener = (value) -> {
            synchronized (lockTimelineChangeListenerExecution) {
                if (timelineChangeListenerExecution != null) {
                    timelineChangeListenerExecution.cancel(false);
                }
                timelineChangeListenerExecution =
                        threadExecutor.schedule(() -> {
                            setSSLines(mouseY, null);
                            view.repaint();
                        }, 500, TimeUnit.MILLISECONDS);
            }
        };
        parent.getTimeline().addTimelineChangeListener(timelineChangeListener);
    }

    public void clean() {
        record(false);
        threadExecutor.shutdown();
        parent.getTimeline().removeTimelineChangeListener(timelineChangeListener);
    }

    /**
     * To record a *.mp4 video from sidescan panel
     *
     * @param r To start or stop recording of the waterfall video
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
            if (creator != null) {
                creator.closeStreams();
            }
        }
    }

    void updateImage(long currentTime, long lastUpdateTime) {
        int yref = 0;
        this.currentTime = currentTime;

        ArrayList<SidescanLine> removeList = new ArrayList<>();

        sidescanParams.setNormalization(config.normalization);
        sidescanParams.setTvgGain(config.tvgGain);
        sidescanParams.setMinValue(config.sliceMinValue);
        sidescanParams.setWindowValue(config.sliceWindowValue);

        boolean autoEGN = toolbar.btnAutoEgn.isSelected();
        boolean logarithmicDecompression = toolbar.btnLogarithmicDecompression.isSelected();

        ArrayList<SidescanLine> list = ssParser.getLinesBetween(firstPingTime + lastUpdateTime, firstPingTime
                + currentTime, subsystem, autoEGN ? SidescanHistogramNormalizer.HISTOGRAM_DEFAULT_PARAMATERS : sidescanParams);

        ArrayList<SidescanLine> drawList = new ArrayList<>(list);

        // Remove lines with repeated timestamps
        for(int i = 1; i < drawList.size(); i++) {
            SidescanLine prev = drawList.get(i - 1);
            SidescanLine curr = drawList.get(i);
            if(curr.getTimestampMillis() == prev.getTimestampMillis()) {
                drawList.remove(i - 1);
                i--;
            }
        }

        for (SidescanLine l : drawList) {
            // Update the rangeMax to the ruler
            if (l.getRange() != getRangeForRuler()) {
                setRangeForRuler(l.getRange());
            }

            // Deal with speed correction here, because this would be repeated code in the various parsers
            if (config.speedCorrection) {
                double horizontalScale = image.getWidth() / (l.getRange() * 2f);
                double verticalScale = horizontalScale;

                double secondsElapsed = (l.getTimestampMillis() - prevPingTime) / 1000f;
                double speed = l.getState().getU();

                // Finally the 'height' of the ping in pixels
                int size = (int) (secondsElapsed * speed * verticalScale);

                if (size <= 0 || secondsElapsed > 0.5) {
                    l.setYSize(1);
                }
                else {
                    l.setYSize(size);
                }
            }
            else {
                l.setYSize(1);
            }
            prevPingTime = l.getTimestampMillis();
            yref += l.getYSize();
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
            sidescanLine.setYPos(yref - d);
            d += sidescanLine.getYSize();
            if (sidescanLine.getData().length <= 0) {
                continue;
            }

            double[] data = sidescanLine.getData();
            if (autoEGN) {
                data = parent.getHistogram().normalize(data, subsystem);
            }
            else if (logarithmicDecompression) {
                data = parent.getHistogram().decompress(data, (double) toolbar.spinLogarithmicDecompression.getValue());
            }
            sidescanLine.setImage(new BufferedImage(data.length, 1, BufferedImage.TYPE_INT_RGB),
                    false);

            // Apply colormap to data
            for (int c = 0; c < data.length; c++) {
                sidescanLine.getImage().setRGB(c, 0, config.colorMap.getColor(data[c]).getRGB());
            }

            if (config.slantRangeCorrection) {
                sidescanLine.setImage(Scalr.apply(sidescanLine.getImage(),
                                new SlantRangeImageFilter(sidescanLine.getState().getAltitude(), sidescanLine.getRange(),
                                        sidescanLine.getImage().getWidth())),
                        true);
            }

            g2d.drawImage(ImageUtils.getScaledImage(sidescanLine.getImage(), image.getWidth(), sidescanLine.getYSize(), true), 0,
                    sidescanLine.getYPos(), null);
            // g2d.drawImage(sidescanLine.image, 0, sidescanLine.ypos, null);
        }
        synchronized (lineList) {
            SidescanLine sidescanLine;
            // Must be in synchronized block
            for (SidescanLine line : lineList) {
                sidescanLine = line;
                sidescanLine.setYPos(sidescanLine.getYPos() + yref);
                if (sidescanLine.getYPos() > image.getHeight()) {
                    removeList.add(sidescanLine);
                }
            }
            lineList.addAll(drawList);
            lineList.removeAll(removeList);
        }

        drawList.clear();
        removeList.clear();
    }

    private void drawZoom(Graphics g2) {
        if (mouseX == -1 && mouseY == -1) {
            isShowingZoomedImage = false;
            return;
        }

        Graphics g = g2.create();

        isShowingZoomedImage = true;
        int zX = (int) MathMiscUtils.clamp(mouseX, ZOOM_BOX_SIZE / 2, image.getWidth() - ZOOM_BOX_SIZE / 2);
        int zY = (int) MathMiscUtils.clamp(mouseY, ZOOM_BOX_SIZE / 2, image.getHeight() - ZOOM_BOX_SIZE / 2);

        // Understand what we are zooming in.
        Color origColor = g.getColor();
        g.setColor(Color.CYAN);
        g.drawRect(zX - ZOOM_BOX_SIZE / 2, zY - ZOOM_BOX_SIZE / 2, ZOOM_BOX_SIZE, ZOOM_BOX_SIZE);
        g.setColor(origColor);

        if (parent.getTimeline().isRunning()) {

            BufferedImage zoomImage = image.getSubimage(zX - ZOOM_BOX_SIZE / 2, zY - ZOOM_BOX_SIZE / 2, ZOOM_BOX_SIZE, ZOOM_BOX_SIZE);
            // BufferedImage zoomLayerImage = layer.getSubimage(zX - ZOOM_BOX_SIZE / 2, zY - ZOOM_BOX_SIZE / 2, ZOOM_BOX_SIZE, ZOOM_BOX_SIZE);

            // choose on which side to paint the zoom based on mouse position
            int xPosition = 1;
            if (mouseX <= (image.getWidth() / 2)) {
                xPosition = image.getWidth() - (ZOOM_LAYER_BOX_SIZE + 1);
            }

            // Draw zoomed image.
            g.drawImage(ImageUtils.getFasterScaledInstance(zoomImage, ZOOM_LAYER_BOX_SIZE, ZOOM_LAYER_BOX_SIZE),
                    xPosition, image.getHeight() - (ZOOM_LAYER_BOX_SIZE + 1), null);
            // g.drawImage(ImageUtils.getFasterScaledInstance(zoomLayerImage, ZOOM_LAYER_BOX_SIZE, ZOOM_LAYER_BOX_SIZE),
            //        xPosition, layer.getHeight() - (ZOOM_LAYER_BOX_SIZE + 1), null);
        }
        else {
            threadExecutor.execute(updateLines);
            int ypos = lines.size();
            if (ypos < 100) {
                isShowingZoomedImage = false;
                return;
            }
            synchronized (lines) {
                for (SidescanLine e : lines) {
                    e.setYSize(1);
                    int beginIndex = 0;
                    int endIndex = 0;
                    int leftMousePos = mouseX - ZOOM_BOX_SIZE / 2;
                    int rightMousePos = mouseX + ZOOM_BOX_SIZE / 2;

                    if (leftMousePos < 0) {
                        beginIndex = 0;
                        rightMousePos = ZOOM_BOX_SIZE;
                        endIndex = (rightMousePos * e.getData().length) / image.getWidth();
                        if (e.isImageWithSlantCorrection()) {
                            beginIndex = SidescanUtil.convertImagePointXToSidescanLinePointX(0, e, image);
                            endIndex = SidescanUtil.convertImagePointXToSidescanLinePointX(rightMousePos, e, image);
                        }
                    }
                    else if (rightMousePos > image.getWidth()) {
                        leftMousePos = image.getWidth() - ZOOM_BOX_SIZE;
                        beginIndex = (leftMousePos * e.getData().length) / image.getWidth();
                        endIndex = (image.getWidth() * e.getData().length) / image.getWidth();
                        if (e.isImageWithSlantCorrection()) {
                            beginIndex = SidescanUtil.convertImagePointXToSidescanLinePointX(leftMousePos, e, image);
                            endIndex = SidescanUtil.convertImagePointXToSidescanLinePointX(image.getWidth(), e, image);
                        }
                    }
                    else {
                        beginIndex = (leftMousePos * e.getData().length) / image.getWidth();
                        endIndex = (rightMousePos * e.getData().length) / image.getWidth();
                        if (e.isImageWithSlantCorrection()) {
                            beginIndex = SidescanUtil.convertImagePointXToSidescanLinePointX(leftMousePos, e, image);
                            endIndex = SidescanUtil.convertImagePointXToSidescanLinePointX(rightMousePos, e, image);
                        }
                    }

                    BufferedImage zoomedImg = new BufferedImage(endIndex - beginIndex, 1, BufferedImage.TYPE_INT_RGB);

                    // Apply colormap to data
                    for (int c = beginIndex; c < endIndex; c++) {
                        if (c >= e.getData().length || c < 0) {
                            continue;
                        }

                        zoomedImg.setRGB(c - beginIndex, 0, config.colorMap.getColor(e.getData()[c]).getRGB());
                    }

                    int vZoomScale = 3;
                    // choose on which side to paint the zoom based on mouse position
                    int xPosition = 1;
                    if (mouseX <= (layer.getWidth() / 2)) {
                        xPosition = layer.getWidth() - (ZOOM_LAYER_BOX_SIZE + 1);
                    }

                    Image full = ImageUtils.getScaledImage(zoomedImg, ZOOM_LAYER_BOX_SIZE, vZoomScale, true);
                    g.drawImage(full, xPosition, layer.getHeight() + (ZOOM_BOX_SIZE) - ypos, null);
                    ypos = ypos + vZoomScale;
                }
            }
        }

        // Mouse center indicator
        if (!isMouseAtRest(BULLSEYE_HIDE_TIMEOUT_MILLIS)) {
            g.setColor(ColorUtils.setTransparencyToColor(Color.CYAN, 180));
            int xBullseye = image.getWidth() - (ZOOM_LAYER_BOX_SIZE / 2 + 1) - 3;
            if (mouseX > (image.getWidth() / 2)) {
                xBullseye = (ZOOM_LAYER_BOX_SIZE / 2 + 1) - 3;
            }
            g.drawRect(xBullseye, image.getHeight() - (ZOOM_LAYER_BOX_SIZE / 2 + 1) - 3, 6, 6);
        }

        g.dispose();
    }

    private void drawInfo(Graphics g) {
        if (mouseSidescanLine != null) {
            LocationType hloc = SidescanUtil.convertImagePointXToLocation(mouseX, mouseSidescanLine, true, image);
            LocationType sloc = SidescanUtil.convertImagePointXToLocation(mouseX, mouseSidescanLine, false, image);

            double hdist = mouseSidescanLine.getState().getPosition().getNewAbsoluteLatLonDepth().getDistanceInMeters(hloc);
            double sdist = mouseSidescanLine.getState().getPosition().getNewAbsoluteLatLonDepth().getDistanceInMeters(sloc);

            Graphics2D location2d = (Graphics2D) mouseLocationImage.getGraphics();
            location2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            location2d.clearRect(0, 0, mouseLocationImage.getWidth(), mouseLocationImage.getHeight());
            location2d.drawString(
                    CoordinateUtil.dmToLatString(CoordinateUtil.decimalDegreesToDM(hloc.getLatitudeDegs())), 5, 15);
            location2d.drawString(
                    CoordinateUtil.dmToLonString(CoordinateUtil.decimalDegreesToDM(hloc.getLongitudeDegs())), 5, 26);
            location2d.drawString(depthStr + ": " + altFormat.format(mouseSidescanLine.getState().getDepth()) + " m", 5, 37);
            location2d.drawString(altStr + ": " + altFormat.format(mouseSidescanLine.getState().getAltitude()) + " m", 5, 48);
            location2d.drawString(rollStr + ": " + altFormat.format(Math.toDegrees(mouseSidescanLine.getState().getRoll()))
                    + "\u00B0", 5, 59);
            location2d.drawString(yawStr + ": " + altFormat.format(Math.toDegrees(mouseSidescanLine.getState().getYaw()))
                    + "\u00B0", 5, 70);
            location2d.drawString(hRangeStr + ": " + altFormat.format(hdist) + " m", 5, 81);
            location2d.drawString(sRangeStr + ": " + altFormat.format(sdist) + " m", 5, 92);

            g.drawImage(mouseLocationImage, 10, 20, null);
        }
    }

    private void drawMeasure(Graphics g) {
        int c = 0;
        SidescanPoint prevPoint = null;
        g.setColor(Color.GREEN);

        g.drawRect(3, 3, 6, 6);

        for (SidescanPoint point : pointList) {
            int pointX = SidescanUtil.convertSidescanLinePointXToImagePointX(point.x, point.line, image);

            if (c == 0) {
                g.drawRect(pointX - 3, point.y - 3, 6, 6);
            }
            else {
                int prevPointX = SidescanUtil.convertSidescanLinePointXToImagePointX(prevPoint.x, prevPoint.line, image);

                double distNoSlant = SidescanUtil.calcHorizontalDistanceFrom2XIndexesOf2SidescanLines(prevPoint.x,
                        prevPoint.line, point.x, point.line);
                distNoSlant = (int) (distNoSlant * 1000) / 1000.0;

                g.drawLine(prevPointX, prevPoint.y, pointX, point.y);
                g.drawRect(pointX - 3, point.y - 3, 6, 6);
                String lb = distNoSlant + "m";
                Rectangle2D lbBounds = g.getFontMetrics().getStringBounds(lb, g);
                g.setColor(ColorUtils.setTransparencyToColor(Color.BLACK, 160));
                g.fillRect((prevPointX + pointX) / 2 + 4 + (int) lbBounds.getX(),
                        (prevPoint.y + point.y) / 2 - 1 + (int) lbBounds.getY(),
                        (int) lbBounds.getWidth(), (int) lbBounds.getHeight());
                g.setColor(Color.BLACK);
                g.drawString(lb, (prevPointX + pointX) / 2 + 3, (prevPoint.y + point.y) / 2 - 1);
                g.setColor(Color.GREEN);
                g.drawString(lb, (prevPointX + pointX) / 2 + 4, (prevPoint.y + point.y) / 2);
            }
            prevPoint = point;
            c++;
        }
    }

    private void drawMeasureHeight(Graphics g2) {
        Graphics g = g2.create();
        g.setColor(Color.GREEN);

        if (measureHeightP != null) {
            int pointX = SidescanUtil.convertSidescanLinePointXToImagePointX(measureHeightP.x, measureHeightP.line, image);
            g.drawRect(pointX - 3, measureHeightP.y - 3, 6, 6);

            if (!Double.isNaN(measureHeightMouseX)) {
                int ssP = SidescanUtil.convertImagePointXToSidescanLinePointX((int) measureHeightMouseX,
                        measureHeightP.line, image);
                g.drawRect((int) (measureHeightMouseX - 3), measureHeightP.y - 3, 6, 6);

                double h = SidescanUtil.calcHeightFrom2XIndexesOfSidescanLine(measureHeightP.x, ssP, measureHeightP.line);
                h = (int) (h * 1000) / 1000.0;

                g.drawLine((int) measureHeightMouseX, measureHeightP.y, pointX, measureHeightP.y);
                g.drawRect(pointX - 3, measureHeightP.y - 3, 6, 6);
                String lb = h + "m";
                Rectangle2D lbBounds = g.getFontMetrics().getStringBounds(lb, g);
                g.setColor(ColorUtils.setTransparencyToColor(Color.BLACK, 160));
                g.fillRect((int) ((measureHeightMouseX + pointX) / 2 + 4 + lbBounds.getX()),
                        (int) (measureHeightP.y - 4 + lbBounds.getY()),
                        (int) lbBounds.getWidth(), (int) lbBounds.getHeight());
                g.setColor(Color.BLACK);
                g.drawString(lb, ((int) measureHeightMouseX + pointX) / 2 + 3, measureHeightP.y - 5);
                g.setColor(Color.GREEN);
                g.drawString(lb, ((int) measureHeightMouseX + pointX) / 2 + 4, measureHeightP.y - 4);
            }
        }
        g.dispose();
    }

    private void drawMarks(Graphics g0) {
        Graphics2D g1 = (Graphics2D) g0.create();
        if (marking) {
            int x = initialX - Math.abs(mouseX - initialX);
            int y = initialY - Math.abs(mouseY - initialY);
            int w = Math.abs(mouseX - initialX) * 2;
            int h = Math.abs(mouseY - initialY) * 2;
            Stroke oStroke = g1.getStroke();
            Stroke nStroke = new BasicStroke(3);
            g1.setStroke(nStroke);
            g1.setColor(Color.BLACK);
            g1.drawRect(x, y, w, h);
            g1.setStroke(oStroke);
            g1.setColor(Color.YELLOW);
            g1.drawRect(x, y, w, h);
        }
        g1.dispose();

        SidescanLine old = null;
        Graphics2D g = (Graphics2D) g0.create();
        for (LogMarker m : parent.getMarkerList()) {
            long timestamp = Double.valueOf(m.getTimestamp()).longValue();

            Color color = ColorUtils.setTransparencyToColor(Color.WHITE, 200);
            Color colorConstrast = ColorUtils.setTransparencyToColor(Color.BLACK, 200);

            SidescanLine line;

            synchronized (lineList) {
                for (SidescanLine sidescanLine : lineList) {
                    line = sidescanLine;
                    if (old != null) {
                        // In case of being a marker just with time information
                        if (timestamp >= old.getTimestampMillis() && timestamp <= line.getTimestampMillis()) {
                            if (m instanceof SidescanLogMarker) {
                                SidescanLogMarker slm = (SidescanLogMarker) m;

                                double distanceToNadir = slm.getX();
                                // This should be always slant corrected (old marks will be wrong, must be corrected)
                                int ssX = line.getIndexFromDistance(distanceToNadir, true);
                                int x = SidescanUtil.convertSidescanLinePointXToImagePointX(ssX, line, image);

                                int wBox = Math.max(slm.getW(), 3);
                                int hBox = Math.max(slm.getH(), 3);

                                if (slm.getW() > 0 && slm.getwMeters() > 0) {
                                    // We have a box mark
                                    double wMeters = slm.getwMeters();
                                    double distanceToNadirH = slm.getX();
                                    int dSSPort = line.getIndexFromDistance(distanceToNadirH - wMeters / 2, true);
                                    int dSSStarbord = line.getIndexFromDistance(distanceToNadirH + wMeters / 2, true);
                                    int dImgPort = SidescanUtil.convertSidescanLinePointXToImagePointX(dSSPort, line, image);
                                    int dImgStarbord = SidescanUtil.convertSidescanLinePointXToImagePointX(dSSStarbord, line, image);
                                    wBox = dImgStarbord - dImgPort;
                                }

                                g.setColor(color);
                                g.drawRect(x - (wBox / 2), line.getYPos() - (hBox / 2), wBox, hBox);
                                g.setColor(colorConstrast);
                                g.drawString(m.getLabel(), x - (wBox / 2) - 1, line.getYPos() - (hBox / 2) - 10 - 1);
                                g.setColor(color);
                                g.drawString(m.getLabel(), x - (wBox / 2), line.getYPos() - (hBox / 2) - 10);
                            }
                            else {
                                g.setColor(color);
                                g.fillRect(0, line.getYPos() - 1, 10, 2);
                                g.fillRect(line.getImage().getWidth(null) - 10, line.getYPos() - 1, 10, 2);
                                g.setColor(colorConstrast);
                                g.drawString(m.getLabel(), -1, line.getYPos() - 10 - 1);
                                g.setColor(color);
                                g.drawString(m.getLabel(), 0, line.getYPos() - 10);
                            }
                            break;
                        }
                    }
                    old = line;
                }
            }
        }
        g.dispose();
    }

    private void drawZoomRuler(Graphics g) {
        if (!isShowingZoomedImage) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        int fontSize = 11;

        // choose on which side to paint the zoom based on mouse position
        int xPosition = 0;
        int xLength = ZOOM_LAYER_BOX_SIZE;
        if (mouseX <= (image.getWidth() / 2)) {
            xPosition = layer.getWidth() - (ZOOM_LAYER_BOX_SIZE + 2);
            xLength = layer.getWidth();
        }

        int x = xPosition;
        int y = layer.getHeight() - (ZOOM_LAYER_BOX_SIZE);
        // Draw Horizontal Line
        g2d.setColor(Color.BLACK);
        g2d.drawLine(x, y, xLength, y);

        Rectangle drawRulerHere = new Rectangle(x, y - MAX_RULER_SIZE, ZOOM_LAYER_BOX_SIZE + 1, MAX_RULER_SIZE);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fill(drawRulerHere);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        g2d.setColor(Color.BLACK);

        // Draw top line
        g2d.drawLine(x, y - MAX_RULER_SIZE, xLength, y - MAX_RULER_SIZE);

        // Draw the zero
        g2d.drawLine(x, y, x, y - MAX_RULER_SIZE);
        // g2d.drawString("0", x+5, y-3);

        float zoomRange = (ZOOM_BOX_SIZE * (rangeForRuler * 2f)) / layer.getWidth();
        float zoomRangeStep = 1;
        if (zoomRange > 40) {
            zoomRangeStep = 10;
        }
        else if (zoomRange > 20) {
            zoomRangeStep = 5;
        }
        else if (zoomRange > 10) {
            zoomRangeStep = 2;
        }

        double step = ((zoomRangeStep * ZOOM_LAYER_BOX_SIZE) / zoomRange);
        int stepInt = Double.valueOf(step).intValue();
        double r = zoomRangeStep;

        int c = x + stepInt;

        for (; c <= xLength; c += stepInt, r += zoomRangeStep) {
            g2d.drawLine(c, y, c, y - MAX_RULER_SIZE);
            g2d.drawString("" + (int) r, c - 13, y - 3);
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
        int stepInt = Double.valueOf(step).intValue();
        double r = rangeForRulerStep;

        int c1 = (int) (layer.getWidth() / 2 - step);
        int c2 = (int) (layer.getWidth() / 2 + step);

        for (; c1 > 0; c1 -= stepInt, c2 += stepInt, r += rangeForRulerStep) {
            g2d.drawLine(c1, 0, c1, MAX_RULER_SIZE);
            g2d.drawLine(c2, 0, c2, MAX_RULER_SIZE);
            g2d.drawString("" + (int) r, c1 + 5, fontSize);
            g2d.drawString("" + (int) r, c2 - 20, fontSize);
        }

        g2d.dispose();
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
        rangeForRulerStep = SidescanGuiUtils.calcStepForRangeForRuler((int) rangeForRuler);
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
     *
     * @param zoomSelected true if zoom is selected.
     */
    public void setZoom(boolean zoomSelected) {
        this.zoom = zoomSelected;
    }

    boolean isBetweenTopAndBottom(SidescanLine line, long bottomTS, long topTS) {
        return bottomTS <= line.getTimestampMillis() && line.getTimestampMillis() <= topTS;
    }

    private void setLastMouseMoveTime() {
        lastMouseMoveTS = System.nanoTime();
        mouseMovementDetection();
    }

    private final Object lockUpdateLinesExecution = new Object();
    private ScheduledFuture<?> updateLinesExecution;
    private final Object lockRepaintExecution = new Object();
    private ScheduledFuture<?> repaintExecution;

    private void mouseMovementDetection() {
        synchronized (lockUpdateLinesExecution) {
            if (updateLinesExecution != null) {
                updateLinesExecution.cancel(false);
            }
            if (!parent.getTimeline().isRunning() && zoom) {
                updateLinesExecution = threadExecutor.schedule(() -> {
                    if (parent.getTimeline().isRunning() || !zoom) {
                        return;
                    }
                    setSSLines(mouseY, null);
                    threadExecutor.execute(updateLines);
                    view.repaint();
                }, 500, TimeUnit.MILLISECONDS);
            }
        }

        synchronized (lockRepaintExecution) {
            if (repaintExecution != null) {
                repaintExecution.cancel(false);
            }
            repaintExecution = threadExecutor.schedule(() -> {
                //if (isMouseAtRest(BULLSEYE_HIDE_TIMEOUT_MILLIS))
                view.repaint();
            }, BULLSEYE_HIDE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (image == null) {
            return;
        }
        mouseX = e.getX();
        mouseY = e.getY();
        setLastMouseMoveTime();
        setSSLines(mouseY, e);
    }

    /*
     * @param y , mouse Y coordinate to retrieve sidescanLine from
     * also sets top and bottom timestamp based on the mouseSidescanLine, for zoom function.
     */
    private void setSSLines(int y, MouseEvent e) {
        int Y = (int) MathMiscUtils.clamp(mouseY, ZOOM_BOX_SIZE / 2, image.getHeight() - ZOOM_BOX_SIZE / 2);
        synchronized (lineList) {
            // Must be in synchronized block
            for (SidescanLine line : lineList) {
                if (y >= line.getYPos() && y <= (line.getYPos() + line.getYSize())) {
                    mouseSidescanLine = line;
                    if (e != null) {
                        ((JPanel) e.getSource()).repaint();
                    }
                }

                // save bottom and top timestamps for zoom box according to mouse position
                if (mouseY < ZOOM_BOX_SIZE / 2) {
                    if (line.getYPos() == 1) {
                        topZoomTimestamp = line.getTimestampMillis();
                    }

                    if (line.getYPos() == ZOOM_BOX_SIZE) {
                        bottomZoomTimestamp = line.getTimestampMillis();
                    }
                }
                else {
                    if ((line.getYPos() + (ZOOM_BOX_SIZE / 2) <= Y) && Y <= (line.getYPos() + (ZOOM_BOX_SIZE / 2) + line.getYSize())) {
                        topZoomTimestamp = line.getTimestampMillis();
                    }
                }
                if ((line.getYPos() - (ZOOM_BOX_SIZE / 2) <= Y) && Y <= (line.getYPos() - (ZOOM_BOX_SIZE / 2) + line.getYSize())) {
                    bottomZoomTimestamp = line.getTimestampMillis();
                }
            }
        }
    }

    private boolean isMouseAtRest() {
        return isMouseAtRest(0);
    }

    private boolean isMouseAtRest(long timeoutMillis) {
        long now = System.nanoTime();
        return now - 1_000_000_000 > lastMouseMoveTS + timeoutMillis * 1000000;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        int y = e.getY();
        setLastMouseMoveTime();
        setSSLines(mouseY, e);

        for (SidescanLine line : lineList.toArray(new SidescanLine[0])) {
            if (y >= line.getYPos() && y <= (line.getYPos() + line.getYSize())) {
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
                if (mouseY >= line.getYPos() && mouseY <= (line.getYPos() + line.getYSize())) {
                    mouseSidescanLine = line;
                    ((JPanel) e.getSource()).repaint();
                    break;
                }
            }

            if (imode == InteractionMode.MARK && !parent.getTimeline().isRunning()) {
                if (LsfReportProperties.generatingReport) {
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
                int x = SidescanUtil.convertImagePointXToSidescanLinePointX(mouseX, mouseSidescanLine, image);
//                System.out.println("x " + x + "   from  mouseX " + mouseX);

                pointList.add(mouseSidescanLine.calcPointFromIndex(x, mouseSidescanLine.isImageWithSlantCorrection()));

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
                    int x = SidescanUtil.convertImagePointXToSidescanLinePointX(mouseX, mouseSidescanLine, image);
                    measureHeightP = mouseSidescanLine.calcPointFromIndex(x, mouseSidescanLine.isImageWithSlantCorrection());
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
                    if (initialY >= line.getYPos() && initialY <= (line.getYPos() + line.getYSize())) {
                        lInit = line;
                        break;
                    }
                }

                if (lInit != null) {
                    int xSS = SidescanUtil.convertImagePointXToSidescanLinePointX(initialX, lInit, image);
                    int y = initialY;
                    int wImg = Math.abs(mouseX - initialX) * 2;
                    int hImg = Math.abs(mouseY - initialY) * 2;

                    // Force slant correction
                    SidescanPoint point = lInit.calcPointFromIndex(xSS, true);

                    // Distance to line center point, negative values mean portboard
                    double distanceToNadir = lInit.getDistanceFromIndex(xSS, true);

                    int xPortImg = initialX - wImg / 2;
                    int xStarboardImg = initialX + wImg / 2;

                    int xPort = SidescanUtil.convertImagePointXToSidescanLinePointX(xPortImg, lInit, image);
                    int xStarboard = SidescanUtil.convertImagePointXToSidescanLinePointX(xStarboardImg, lInit, image);

                    // Force slant correction
                    double dHPort = lInit.getDistanceFromIndex(xPort, true);
                    double dHStarboard = lInit.getDistanceFromIndex(xStarboard, true);
                    double wMeters = dHStarboard - dHPort;

                    parent.mraPanel.addMarker(new SidescanLogMarker(res, lInit.getTimestampMillis(), point.location
                            .getLatitudeRads(), point.location.getLongitudeRads(), distanceToNadir, y, wImg,
                            hImg, wMeters, subsystem, config.colorMap));
                }
                else {
                    NeptusLog.pub().warn("Marking in sidescan where line was not found bellow mouse pointer.");
                }
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
        if (e.getButton() == MouseEvent.BUTTON3 && e.isControlDown()) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem(I18n.text("Fix old marks"));
            menuItem.addActionListener(fixSidescanMarkAction(popup));
            popup.add(menuItem);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Action for the popup to fix old marks.
     *
     * @param popup the {@link JPopupMenu} source for the action.
     * @return The {@link ActionListener} to fix sidescan mark
     */
    protected ActionListener fixSidescanMarkAction(JPopupMenu popup) {
        return e -> {
            ArrayList<LogMarker> allMarks = parent.getMarkerList();
            // Let us collect the possible marks (sidescan marks version < 1)
            ArrayList<SidescanLogMarker> ssMarks = new ArrayList<>();
            for (LogMarker m : allMarks) {
                if (m instanceof SidescanLogMarker) {
                    if (((SidescanLogMarker) m).getSidescanMarkVersion() < 1) {
                        ssMarks.add((SidescanLogMarker) m);
                    }
                }
            }

            if (ssMarks.isEmpty()) {
                GuiUtils.infoMessage(popup.getComponent(), I18n.text("Select mark"),
                        I18n.text("No marks to adjust"));
                return;
            }

            Object ret = JOptionPane.showInputDialog(popup.getComponent(), I18n.text("Select mark"),
                    I18n.text("Select mark"), JOptionPane.QUESTION_MESSAGE, null,
                    ssMarks.toArray(new SidescanLogMarker[ssMarks.size()]), null);

            if (ret == null) {
                return;
            }

            SidescanLogMarker ssMk = (SidescanLogMarker) ret;

            // Let us fix the marks
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                private Operation op = Operation.TEST_CHANGE;

                @Override
                protected Boolean doInBackground() {
                    ArrayList<SSCorrection> corrections = fixSidescanMark(ssMk);
                    op = Operation.TEST_CHANGE;
                    boolean changed = false;
                    boolean exit = false;
                    while (!exit) {
                        switch (op) {
                            case EXIT_CANCEL:
                                if (changed) {
                                    op = Operation.TEST_ORIG;
                                }
                                exit = true;
                                break;
                            case EXIT_CHANGE:
                                if (!changed) {
                                    op = Operation.TEST_CHANGE;
                                }
                                exit = true;
                            default:
                                break;
                        }
                        switch (op) {
                            case TEST_CHANGE:
                            case TEST_ORIG:
                                for (SSCorrection c : corrections) {
                                    SidescanLogMarker m = c.marker;
                                    if (op == Operation.TEST_ORIG) {
                                        m.fixLocation(c.latRadsSlant, c.lonRadsSlant);
                                        m.setX(c.distanceToNadirSlant);
                                        m.setwMeters(c.wMetersSlant);
                                        changed = false;
                                    }
                                    else if (op == Operation.TEST_CHANGE) {
                                        m.fixLocation(c.latRadsHorizontal, c.lonRadsHorizontal);
                                        m.setX(c.distanceToNadirHorizontal);
                                        m.setwMeters(c.wMetersHorizontal);
                                        changed = true;
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                        if (!exit) {
                            process(null);
                        }
                    }
                    if (changed) {
                        for (SSCorrection c : corrections) {
                            c.marker.resetSidescanMarkVersion();
                        }
                        parent.mraPanel.saveMarkers();
                    }

                    return changed;
                }

                @Override
                protected void process(List<Void> chunks) {
                    SidescanPanel.this.repaint(0);

                    String testStr = I18n.text("Test");
                    switch (op) {
                        case TEST_CHANGE:
                            testStr = I18n.text("Revert test");
                        case TEST_ORIG:

                            int retQ = JOptionPane.showOptionDialog(SidescanPanel.this,
                                    I18n.text("Change the marks?"), I18n.text("Fix old marks"),
                                    JOptionPane.YES_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null,
                                    new String[]{I18n.text("Change"), I18n.text("Cancel"), testStr}, testStr);
                            switch (retQ) {
                                case 0:
                                    op = Operation.EXIT_CHANGE;
                                    break;
                                case 2:
                                    if (I18n.text("Test").equalsIgnoreCase(testStr)) {
                                        op = Operation.TEST_CHANGE;
                                    }
                                    else if (I18n.text("Revert test").equalsIgnoreCase(testStr)) {
                                        op = Operation.TEST_ORIG;
                                    }
                                    break;
                                case 1:
                                default:
                                    op = Operation.EXIT_CANCEL;
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                protected void done() {
                    try {
                        boolean res = get();
                        if (res) {
                            GuiUtils.infoMessage(SidescanPanel.this, I18n.text("Fix old marks"),
                                    I18n.text("Marks fixed and saved"));
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            worker.execute();
        };
    }

    /**
     * Worker to fix old marks.
     */
    private ArrayList<SSCorrection> fixSidescanMark(SidescanLogMarker... ssMk) {
        ArrayList<SSCorrection> corrections = new ArrayList<>();

        for (SidescanLogMarker m : ssMk) {
            synchronized (lineList) {
                Iterator<SidescanLine> i = lineList.iterator();
                SidescanLine old = null;
                SidescanLine line;
                boolean found = false;
                while (i.hasNext()) {
                    line = i.next();
                    if (old != null) {
                        long timestamp = Double.valueOf(m.getTimestamp()).longValue();
                        if (timestamp >= old.getTimestampMillis() && timestamp <= line.getTimestampMillis()) {
                            double distanceToNadirSlant = m.getX();
                            int ssX = line.getIndexFromDistance(distanceToNadirSlant, false);
                            double distanceToNadirHoriz = line.getDistanceFromIndex(ssX, true);

                            int dPort = line.getIndexFromDistance(distanceToNadirSlant - m.getwMeters() / 2, false);
                            double distancePort = line.getDistanceFromIndex(dPort, true);
                            int dStarbord = line.getIndexFromDistance(distanceToNadirSlant + m.getwMeters() / 2, false);
                            double distanceStarbord = line.getDistanceFromIndex(dStarbord, true);
                            double wMetersHoriz = distanceStarbord - distancePort;

                            SidescanPoint point = line.calcPointFromIndex(ssX, true);

                            SSCorrection coor = new SSCorrection();
                            coor.marker = m;
                            coor.latRadsSlant = m.getLatRads();
                            coor.lonRadsSlant = m.getLonRads();
                            coor.latRadsHorizontal = point.location.getLatitudeRads();
                            coor.lonRadsHorizontal = point.location.getLongitudeRads();
                            coor.distanceToNadirSlant = distanceToNadirSlant;
                            coor.distanceToNadirHorizontal = distanceToNadirHoriz;
                            coor.wMetersSlant = m.getwMeters();
                            coor.wMetersHorizontal = wMetersHoriz;
                            corrections.add(coor);

                            found = true;
                            break;
                        }
                    }
                    old = line;
                }
                if (!found) {
                    SSCorrection coor = new SSCorrection();
                    coor.marker = m;
                    coor.latRadsSlant = m.getLatRads();
                    coor.lonRadsSlant = m.getLonRads();
                    coor.latRadsHorizontal = m.getLatRads();
                    coor.lonRadsHorizontal = m.getLonRads();
                    coor.distanceToNadirSlant = m.getX();
                    coor.distanceToNadirHorizontal = m.getX();
                    coor.wMetersSlant = m.getwMeters();
                    coor.wMetersHorizontal = m.getwMeters();
                    corrections.add(coor);
                }
            }
        }

        return corrections;
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
