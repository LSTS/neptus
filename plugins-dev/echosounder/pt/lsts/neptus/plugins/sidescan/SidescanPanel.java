/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
import java.awt.Graphics;
import java.awt.Graphics2D;
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

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
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

/**
 * @author jqcorreia
 *
 */
public class SidescanPanel extends JPanel implements MouseListener, MouseMotionListener 
{
    private static final long serialVersionUID = 1L;

    SidescanAnalyzer parent;
    SidescanConfig config = new SidescanConfig();
    SidescanToolbar toolbar = new SidescanToolbar(this);

    SidescanParameters sidescanParams = new SidescanParameters(0, 0); // Initialize it to zero for now

    enum InteractionMode {
        NONE, ZOOM, INFO, MARK, MEASURE;
    }

    InteractionMode imode = InteractionMode.INFO;

    MraVehiclePosHud posHud;

    JPanel view = new JPanel() {
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

                    if (zoom)
                        drawZoom(layer.getGraphics()); // UPdate layer with zoom information
                    if (info)
                        drawInfo(layer.getGraphics()); // update layer with location information
                    if (measure) {
                        drawMeasure(layer.getGraphics());
                    }

                    drawMarks(layer.getGraphics());
                    drawRuler(layer.getGraphics());

                    g.drawImage(layer, 0, 0, null);

                    if(config.showPositionHud) {
                        posHud.setPathColor(config.pathColor);
                        g.drawImage(posHud.getImage((firstPingTime + currentTime) / 1000.0), 0, getHeight() - config.hudSize, null);
                    }

                    if(record) {
                        creator.addFrame(image, firstPingTime + currentTime);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    BufferedImage image;
    BufferedImage layer;
    Graphics2D g2d;

    long firstPingTime;
    long lastPingTime;

    long currentTime;

    long prevPingTime;

    float range = 0;

    // Misc
    BufferedImage bufferedCache;
    byte[] prevData = null;
    int[] iData = null;

    // Zoom
    boolean zoom = false;

    // Measure
    boolean measure = false;

    // Info
    boolean info = false;
    ArrayList<SidescanPoint> pointList = new ArrayList<SidescanPoint>();

    // Marking
    boolean marking = false;
    int initialX;
    int initialY;

    // Mouse related fields
    boolean mousePressed = false;
    boolean mouseEntered = false;

    int mouseX, mouseY;
    SidescanPoint mouseSidescanPoint; // Mouse position geographical location
    SidescanLine mouseSidescanLine;

    BufferedImage mouseLocationImage = ImageUtils.createCompatibleImage(110, 60, Transparency.BITMASK);

    List<SidescanLine> lineList = Collections.synchronizedList(new ArrayList<SidescanLine>());
    ArrayList<SidescanLine> drawList = new ArrayList<SidescanLine>();
    ArrayList<SidescanLine> removeList = new ArrayList<SidescanLine>();

    NumberFormat altFormat = GuiUtils.getNeptusDecimalFormat(2);

    SidescanParser ssParser;

    String altStr = I18n.text("Altitude");
    String depthStr = I18n.text("Depth");
    String rollStr = I18n.text("Roll");

    int rangeStep;

    SlantRangeImageFilter filter;

    int subsystem;

    VideoCreator creator;

    protected boolean record = false;


    public SidescanPanel(SidescanAnalyzer analyzer, SidescanParser parser, int subsystem) {
        this.parent = analyzer;
        ssParser = parser;
        initialize();
        this.subsystem = subsystem;

        posHud = new MraVehiclePosHud(analyzer.mraPanel.getSource().getLsfIndex(), config.hudSize, config.hudSize);
    }

    public void initialize() {
        firstPingTime = ssParser.firstPingTimestamp();
        prevPingTime = firstPingTime;

        // Deal with panel resize by recreating the image buffers
        view.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                image = ImageUtils.createCompatibleImage(view.getWidth(), view.getHeight(),
                        Transparency.OPAQUE);
                bufferedCache = ImageUtils.createCompatibleImage(view.getWidth(), view.getHeight(),
                        Transparency.OPAQUE);
                g2d = (Graphics2D) image.getGraphics();
                layer = ImageUtils.createCompatibleImage(view.getWidth(), view.getHeight(),
                        Transparency.TRANSLUCENT);
            }
        });

        // Create SlantRangeFilter
        // filter = new SlantRangeImageFilter(0, pingParser.firstLogEntry().getDouble("max_range"), pingParser.firstLogEntry().getRawData("data").length);
        view.addMouseListener(this);
        view.addMouseMotionListener(this);

        setLayout(new MigLayout("ins 0, gap 0"));
        add(toolbar, "w 100%, wrap");
        add(view, "w 100%, h 100%");
    }

    int tcount = 0;
    int lcount = 0;

    public void record(boolean r) {
        record = r;
        if(r) {
            try {
                creator = new VideoCreator(new File(parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/Sidescan_" + subsystem + ".mp4"), 800, 600);
                System.out.println("RECORDING TO Sidescan.mp4");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            creator.closeStreams();
        }
    }

    public void updateImage(long currentTime, long lastUpdateTime) {
        int yref = 0;
        this.currentTime = currentTime;

        sidescanParams.setNormalization(config.normalization);
        sidescanParams.setTvgGain(config.tvgGain);

        ArrayList<SidescanLine> list = ssParser.getLinesBetween(firstPingTime + lastUpdateTime, firstPingTime + currentTime, subsystem, sidescanParams);

        drawList.addAll(list);

        for(SidescanLine l : drawList) {
            if(l.range != getRange()) {
                setRange(l.range);
            }

            // Deal with speed correction here, because this would be repeated code in the various parsers
            if(config.speedCorrection) {
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
            yref+= l.ysize;
        }


        // This check is to prevent negative array indexes (from dragging too much)
        if (yref <= image.getHeight()) {
            ImageUtils.copySrcIntoDst(image, bufferedCache, 0, 0, image.getWidth(), image.getHeight() - yref, 0, 0, image.getWidth(), image.getHeight());
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

            g2d.drawImage(ImageUtils.getScaledImage(sidescanLine.image, image.getWidth(), sidescanLine.ysize, true), 0, sidescanLine.ypos, null);
            //            g2d.drawImage(sidescanLine.image, 0, sidescanLine.ypos, null);
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

    void drawZoom(Graphics g) {
        mouseX = (int) MathMiscUtils.clamp(mouseX, 50, image.getWidth() - 50);
        mouseY = (int) MathMiscUtils.clamp(mouseY, 50, image.getHeight() - 50);
        BufferedImage zoomImage = image.getSubimage(mouseX - 50, mouseY - 50, 100, 100);
        g.drawImage(ImageUtils.getFasterScaledInstance(zoomImage, 300, 300), image.getWidth() - 301,
                image.getHeight() - 301, null);
    }


    void drawInfo(Graphics g) {

        if (mouseSidescanLine != null) {
            LocationType loc = mouseSidescanLine.calcPointForCoord((int)(mouseX * (mouseSidescanLine.xsize / (float)image.getWidth()))).location;

            Graphics2D location2d = (Graphics2D) mouseLocationImage.getGraphics();
            location2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            location2d.clearRect(0, 0, mouseLocationImage.getWidth(), mouseLocationImage.getHeight());
            location2d.drawString(CoordinateUtil.dmToLatString(CoordinateUtil.decimalDegreesToDM(loc.getLatitudeAsDoubleValue())), 5, 15);
            location2d.drawString(CoordinateUtil.dmToLonString(CoordinateUtil.decimalDegreesToDM(loc.getLongitudeAsDoubleValue())), 5, 26);
            location2d.drawString(altStr+": " + altFormat.format(mouseSidescanLine.state.getAltitude()), 5, 37);
            location2d.drawString(rollStr+": " + altFormat.format(Math.toDegrees(mouseSidescanLine.state.getRoll())), 5, 48);

            g.drawImage(mouseLocationImage, 10, 10, null);
        }
    }

    void drawMeasure(Graphics g) {
        int c = 0;
        double distance;
        SidescanPoint prevPoint = null;
        g.setColor(Color.GREEN);

        for (SidescanPoint point : pointList) {
            if (c == 0) {
                g.drawRect(point.x - 3, point.y - 3, 6, 6);
            }
            else {
                distance = prevPoint.location.getDistanceInMeters(point.location);
                distance = (int)(distance * 1000) / 1000.0;

                g.drawLine(prevPoint.x, prevPoint.y, point.x, point.y);
                g.drawRect(point.x - 3, point.y - 3, 6, 6);
                g.setColor(Color.white);
                g.drawString(distance + "m", (prevPoint.x + point.x) / 2 + 3, (prevPoint.y + point.y) / 2 - 1);
                g.setColor(Color.GREEN);
                g.drawString(distance + "m", (prevPoint.x + point.x) / 2 + 4, (prevPoint.y + point.y) / 2);

            }
            prevPoint = point;
            c++;
        }
    }

    void drawMarks(Graphics g) {
        if (marking) {
            int x = Math.min(initialX, mouseX);
            int y = Math.min(initialY, mouseY);
            int w = Math.max(initialX, mouseX) - Math.min(initialX, mouseX);
            int h = Math.max(initialY, mouseY) - Math.min(initialY, mouseY);
            g.drawRect(x, y, w, h);
        }

        SidescanLine old = null;
        for (LogMarker m : parent.getMarkerList()) {
            long timestamp = new Double(m.timestamp).longValue();

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
                                g.drawRect(x - (slm.w / 2), line.ypos - (slm.h / 2), slm.w, slm.h);
                                g.drawString(m.label, x - (slm.w / 2), line.ypos - (slm.h / 2) - 10);
                            }
                            else {
                                g.fillRect(0, line.ypos - 1, 10, 2);
                                g.fillRect(line.image.getWidth(null) - 10, line.ypos - 1, 10, 2);
                                g.drawString(m.label, 0, line.ypos - 10);
                            }
                            break; // ??
                        }
                    }
                    old = line;
                }
            }
        }
    }

    void drawRuler(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Draw Horizontal Line
        g2d.drawLine(0, 0, layer.getWidth(), 0);

        // Draw the zero
        g2d.drawLine(layer.getWidth() / 2, 0, layer.getWidth() / 2, 10);
        g2d.drawString("0", layer.getWidth() / 2 - 10, 10);

        //Draw the maxes
        g2d.drawLine(0, 0, 0, 10);
        g2d.drawString("" + (int)range, 2 , 10);

        g2d.drawLine(layer.getWidth()-1, 0, layer.getWidth()-1, 10);
        g2d.drawString("" + (int)range, layer.getWidth() - 20, 10);

        double step = (layer.getWidth() / ((range * 2) / rangeStep));
        double r = rangeStep;

        int c1 = (int) (layer.getWidth() / 2 - step);
        int c2 = (int) (layer.getWidth() / 2 + step);

        for(; c1 > 0; c1 -= step, c2 += step, r += rangeStep) {
            g2d.drawLine(c1, 0, c1, 10);
            g2d.drawLine(c2, 0, c2, 10);
            g2d.drawString("" + (int)r, c1 + 5, 10);
            g2d.drawString("" + (int)r, c2 - 20, 10);
        }

    }

    /**
     * @return the range
     */
    public float getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(float range) {
        this.range = range;
        rangeStep = 10;
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
     * @param imode mode to set (see InteractionMode enum)
     */
    public void setInteractionMode(InteractionMode imode) {
        // For now clear Measure Interaction Mode structures here //FIXME
        measure = false;
        pointList.clear();
        this.imode = imode;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int y = e.getY();
        synchronized (lineList) {
            Iterator<SidescanLine> i = lineList.iterator(); // Must be in synchronized block
            while (i.hasNext()) {
                SidescanLine line = i.next();
                if (y >= line.ypos && y <= (line.ypos + line.ysize)) {
                    mouseSidescanLine = line;
                    ((JPanel) e.getSource()).repaint();
                }
            }
        }
    };

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
                }
            }

            if (imode == InteractionMode.MARK && !parent.getTimeline().isRunning()) {
                marking = true;
                initialX = mouseX;
                initialY = mouseY;
            }
            else if (imode == InteractionMode.ZOOM) {
                zoom = true;
            }
            else if (imode == InteractionMode.MEASURE && !parent.getTimeline().isRunning()) {
                measure = true;
                pointList.add(mouseSidescanLine.calcPointForCoord(mouseX));
            }
            else if (imode == InteractionMode.INFO){
                info = true;
            }
            ((JPanel) e.getSource()).repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        zoom = false;
        info = false;

        if (marking) {
            String res = JOptionPane.showInputDialog("Insert marker name");

            // Check for a valid response 
            if(res != null) {

                // Calc the center of the rectangle
                int x = (int) (((mouseX + initialX) / 2) * (mouseSidescanLine.xsize / (float)image.getWidth()));
                int y = (mouseY + initialY) / 2;

                // Find the corresponding SidescanLine object
                SidescanLine l = null;
                for (SidescanLine line : lineList) {
                    if (y >= line.ypos && y <= (line.ypos + line.ysize)) {
                        l = line;
                    }
                }

                SidescanPoint point = l.calcPointForCoord(x);

                // Distance to line center point, negative values mean portboard
                double distanceToNadir = l.state.getPosition().getDistanceInMeters(point.location); 
                distanceToNadir *= (x > l.xsize / 2 ? 1 : -1);

                parent.mraPanel.addMarker(new SidescanLogMarker(res, l.timestampMillis, point.location
                        .getLatitudeAsDoubleValueRads(), point.location.getLongitudeAsDoubleValueRads(), distanceToNadir, y, Math
                        .abs(mouseX - initialX), Math.abs(mouseY - initialY)));
            }
            marking = false;
        }

        if (imode != InteractionMode.MEASURE) {
            measure = false;
            pointList.clear();
        }

        ((JPanel) e.getSource()).repaint();
    };

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {  

    }

    @Override
    public void mouseExited(MouseEvent e) {  

    }
}
