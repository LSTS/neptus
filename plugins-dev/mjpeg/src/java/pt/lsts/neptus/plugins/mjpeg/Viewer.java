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
 */
package pt.lsts.neptus.plugins.mjpeg;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.VerticalLayout;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.Timeline;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogMarkerListener;
import pt.lsts.neptus.mra.replay.MraVehiclePosHud;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.LsfReportProperties;

/**
 * Video viewer.
 *
 * @author Ricardo Martins
 * @author José Pinto
 */
@PluginDescription(name = "Video", icon = "images/downloader/camera.png")
public class Viewer extends JComponent implements MRAVisualization, LogMarkerListener {
    private static final long serialVersionUID = 1L;
    /** Parent MRA panel. */
    private final MRAPanel panel;
    /** Log folder. */
    private File logFolder;
    /** LSF index. */
    private LsfIndex lsfIndex;
    /** Toolbar. */
    private Toolbar toolbar = null;
    /** True to show caption overlay. */
    private boolean showCaption = true;
    /** Caption overlay. */
    private final Caption caption = new Caption();
    /** True to show track overlay. */
    private boolean showTrack = true;
    /** Track overlay. */
    private MraVehiclePosHud track;
    /** Current zoom point. */
    private Point2D zoomPoint = null;
    /** List of markers. */
    private final Vector<LogMarker> markers = new Vector<>();
    /** Frame decoder. */
    private FrameDecoder decoder = null;
    /** Current video frame. */
    private VideoFrame videoFrame = null;
    /** Timeline bar. */
    private Timeline timeline;

    public Viewer(MRAPanel panel) {
        this.panel = panel;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3 && videoFrame != null) {
                    zoomPoint = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (e.getButton() != MouseEvent.BUTTON3) {
                    zoomPoint = null;
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                if (e.getButton() != MouseEvent.BUTTON3 && videoFrame != null) {
                    zoomPoint = e.getPoint();
                    repaint();
                }
            }
        });
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timeStep) {
        this.logFolder = source.getFile(".");
        this.lsfIndex = source.getLsfIndex();
        this.track = new MraVehiclePosHud(source, 150, 150);
        this.decoder = FrameDecoderFactory.createDecoder(logFolder);

        int frameRate = this.decoder.getFrameRate();
        NeptusLog.pub().info("average frame rate is " + Integer.toString(frameRate));

        initializeTimeline(decoder.getFrameCount(), frameRate);
        initializeToolbar();

        JPanel panel = new JPanel(new MigLayout());
        panel.add(this, "w 100%, h 100%, wrap");
        panel.add(timeline, "split, span, grow");
        panel.add(toolbar, "wrap");

        synchronized (markers) {
            markers.clear();
            markers.addAll(this.panel.getMarkers());
            Collections.sort(markers);
        }

        return panel;
    }

    private void initializeTimeline(int frameCount, int frameRate) {
        timeline = new Timeline(0, frameCount - 1, frameRate, frameRate, false);
        timeline.addTimelineChangeListener(this::setFrame);
        timeline.getSlider().setValue(0);
        timeline.getSlider().setUI(new BasicSliderUI(timeline.getSlider()) {
            @Override
            public void paintTicks(Graphics g) {
                super.paintTicks(g);
                for (LogMarker m : markers) {
                    long markTime = Double.valueOf(m.getTimestamp()).longValue();
                    int timelineValue = decoder.getFrameNumberByTime(markTime);
                    int timelinePosition = xPositionForValue(timelineValue);
                    g.drawLine(timelinePosition, 0, timelinePosition, timeline.getSlider().getHeight() / 2);
                }
            }
        });
    }

    private void initializeToolbar() {
        toolbar = new Toolbar();
        toolbar.getSaveButton().addActionListener((e) ->
                saveFrame(videoFrame));
        toolbar.getMarkButton().addActionListener((e) ->
                addMark(videoFrame));
        toolbar.getShowCaptionButton().addActionListener((e) ->
                setCaptionVisibility(toolbar.getShowCaptionButton().isSelected()));
        toolbar.getShowTrackButton().addActionListener((e) ->
                setTrackVisibility(toolbar.getShowTrackButton().isSelected()));
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return FrameDecoderFactory.isDecodable(source.getFile("."));
    }

    @Override
    public void onCleanup() {
    }

    @Override
    public void onHide() {
    }

    @Override
    public void onShow() {
    }

    @Override
    public Double getDefaultTimeStep() {
        return 0.25;
    }

    @Override
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(getClass()), 16, 16);
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (videoFrame == null)
            return;

        Image image = videoFrame.getImage();
        if (image == null)
            return;

        long timeStamp = videoFrame.getTimeStamp();

        Graphics2D g2d = (Graphics2D) g;
        g.drawImage(image, 0, 0, getWidth(), getHeight(), 0, 0, image.getWidth(this), image.getHeight(this), this);

        if (showCaption)
            drawCaption(g2d, timeStamp);

        if (showTrack)
            drawTrack(g2d, getHeight(), timeStamp);

        if (zoomPoint != null)
            drawZoom(g2d, image);
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        repaint();

        synchronized (markers) {
            markers.clear();
            markers.addAll(panel.getMarkers());
            Collections.sort(markers);
        }
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        repaint();

        synchronized (markers) {
            markers.clear();
            markers.addAll(panel.getMarkers());
            Collections.sort(markers);
        }
    }

    @Override
    public void goToMarker(LogMarker marker) {
        if (decoder == null)
            return;

        int sliderValue = decoder.getFrameNumberByTime((long) marker.getTimestamp());
        timeline.getSlider().setValue(sliderValue);

        //System.err.format("gotoMarker(): %d\n", sliderValue);
    }

    private void addMark(VideoFrame frame) {
        if (frame == null)
            return;

        if (LsfReportProperties.generatingReport) {
            GuiUtils.infoMessage(panel.getRootPane(),
                    I18n.text("Can not add Marks"),
                    I18n.text("Can not add Marks - Generating Report."));
            return;
        }

        String markerName = JOptionPane.showInputDialog(panel,
                "Enter marker name",
                "Add marker",
                JOptionPane.QUESTION_MESSAGE);

        if (StringUtils.isBlank(markerName))
            return;

        SystemPositionAndAttitude state = lookupPosition(videoFrame.getTimeStamp());
        if (state == null)
            return;

        LocationType position = state.getPosition();
        if (position == null)
            return;

        LogMarker marker = new LogMarker(markerName, videoFrame.getTimeStamp(),
                position.getLatitudeRads(),
                position.getLongitudeRads());

        panel.addMarker(marker);
    }

    private void saveFrame(VideoFrame frame) {
        if (frame == null)
            return;

        JFileChooser fileChooser = new JFileChooser(logFolder);
        JPanel accessory = new JPanel();
        accessory.setBorder(new EmptyBorder(0, 5, 5, 0));
        accessory.setLayout(new VerticalLayout());
        JCheckBox addCaption = new JCheckBox(I18n.text("Include Caption"), true);
        JCheckBox addTrack = new JCheckBox(I18n.text("Include Track"), true);
        accessory.add(addCaption);
        accessory.add(addTrack);
        fileChooser.setAccessory(accessory);

        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File outputFile = enforceFileExtension(fileChooser.getSelectedFile(), "jpg");

        try {
            saveFrameToFile(frame, outputFile, addCaption.isSelected(), addTrack.isSelected());
            GuiUtils.infoMessage(panel, I18n.text("Image saved"),
                    I18n.textf("Image saved in %filename", outputFile.getAbsolutePath()));
        } catch (Exception e) {
            GuiUtils.errorMessage(Viewer.this, e);
        }
    }

    /**
     * Guarantees that a given file has the given extension.
     *
     * @param file file object.
     * @param extension desired extension.
     * @return file object with the desired extension.
     */
    private static File enforceFileExtension(File file, String extension) {
        if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase(extension))
            return file;

        return new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + "." + extension);
    }

    /**
     * Save a given video frame to a file.
     *
     * @param frame video frame object.
     * @param outputFile output file.
     * @param addCaption true to add a caption overlay, false otherwise.
     * @param addTrack true to add a track overlay, false otherwise.
     * @throws IOException if save operation failed.
     */
    private void saveFrameToFile(VideoFrame frame, File outputFile, boolean addCaption, boolean addTrack) throws IOException {
        Image image = frame.getImage();
        long timeStamp = frame.getTimeStamp();
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        BufferedImage imageCopy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imageCopy.createGraphics();
        g.drawImage(image, 0, 0, null);

        if (addCaption)
            drawCaption(g, timeStamp);

        if (addTrack)
            drawTrack(g, height, timeStamp);

        ImageIO.write(imageCopy, "JPG", outputFile);
    }

    /**
     * Set the visibility of the caption overlay.
     *
     * @param visibility true to display the caption overlay, false otherwise.
     */
    private void setCaptionVisibility(boolean visibility) {
        showCaption = visibility;
        repaint();
    }

    /**
     * Set the visibility of the track overlay.
     *
     * @param visibility true to display the track overlay, false otherwise.
     */
    private void setTrackVisibility(boolean visibility) {
        showTrack = visibility;
        repaint();
    }

    /**
     * Retrieve the position and attitude of an image given its timestamp.
     *
     * @param timeStamp image timestamp.
     * @return position and attitude, or null if the information is not available.
     */
    private SystemPositionAndAttitude lookupPosition(long timeStamp) {
        int msgIndex = lsfIndex.getMsgIndexAt("EstimatedState", timeStamp / 1000.0);
        if (msgIndex == -1) {
            System.err.format("failed to find position for timestamp: %d\n", timeStamp);
            return null;
        }

        IMCMessage m = lsfIndex.getMessage(msgIndex);

        LocationType loc = new LocationType(Math.toDegrees(m.getDouble("lat")), Math.toDegrees(m.getDouble("lon")));
        loc.setDepth(m.getDouble("depth"));
        loc.translatePosition(m.getDouble("x"), m.getDouble("y"), 0);
        loc.convertToAbsoluteLatLonDepth();

        SystemPositionAndAttitude state = new SystemPositionAndAttitude(loc,
                m.getDouble("phi"), m.getDouble("theta"), m.getDouble("psi"));
        state.setTime(timeStamp);
        state.setU(m.getDouble("u"));
        state.setAltitude(m.getTypeOf("alt") != null ? m.getDouble("alt") : Double.NaN);

        return state;
    }

    /**
     * Draw caption overlay.
     *
     * @param g         2D graphics object.
     * @param timeStamp time of the image.
     */
    private void drawCaption(Graphics2D g, long timeStamp) {
        SystemPositionAndAttitude state = lookupPosition(timeStamp);
        if (state != null)
            caption.draw(state, g);
    }

    /**
     * Draw track overlay.
     *
     * @param g         2D graphics object.
     * @param height    height of the image.
     * @param timeStamp time of the image.
     */
    private void drawTrack(Graphics2D g, int height, long timeStamp) {
        g.drawImage(track.getImage(timeStamp / 1000.0), 10, height - 160, null);
    }

    /**
     * Draw zoomed area overlay.
     *
     * @param g     2D graphics object.
     * @param image image to zoom.
     */
    private void drawZoom(Graphics2D g, Image image) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int imgX = (int) ((zoomPoint.getX() / getWidth()) * image.getWidth(null));
        int imgY = (int) ((zoomPoint.getY() / getHeight()) * image.getHeight(null));
        g.drawImage(image,
                getWidth() - 210, getHeight() - 210,
                getWidth() - 10, getHeight() - 10,
                imgX - 25, imgY - 25,
                imgX + 25, imgY + 25,
                null);
    }

    private synchronized void setFrame(int number) {
        decoder.seekToFrame(number);
        videoFrame = decoder.getCurrentFrame();
        timeline.setTime(videoFrame.getTimeStamp());
        repaint();
    }
}
