/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Nov 30, 2012
 * $Id:: SidescanPanel.java 9934 2013-02-15 14:05:45Z jqcorreia                 $:
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;

/**
 * @author jqcorreia
 *
 */
public class SidescanPanel extends JPanel implements MouseListener, MouseMotionListener{
    private static final long serialVersionUID = 1L;
    
    SidescanAnalyzer parent;
    
    BufferedImage image;
    BufferedImage layer;
    Graphics2D g2d;

    ColorMap colormap = ColorMapFactory.createBronzeColormap();

    long firstPingTime;
    long lastPingTime;

    long currentTime;

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
    //NumberFormat coordFormat = GuiUtils.getNeptusDecimalFormat(6);
    
    SidescanParser ssParser;
    
    //FIXME this properties are duplicate from SideScanAnalyzer (!)
    // Processing flags
    @NeptusProperty
    public boolean verticalBlending = false;
    @NeptusProperty
    public boolean slantRangeCorrection = false;
    @NeptusProperty
    public boolean timeVariableGain = false;
   
    String altStr = I18n.text("Altitude");
    String depthStr = I18n.text("Depth");
    String rollStr = I18n.text("Roll");

    SlantRangeImageFilter filter;
    public double sums[] = null;
    
    int subsystem;
    
    public void calcIntensities(IMraLogGroup source) {
        int count = 0;
        String msg = "SonarData";
        if (source.getLog("SonarData") == null)
            msg = "SidescanPing";
        for (IMCMessage ping : source.getLsfIndex().getIterator(msg)) {
            if(ping.getInteger("type") != SonarData.TYPE.SIDESCAN.value())
                continue;
            
            byte[] data = ping.getRawData("data");
            if (sums == null)
                sums = new double[data.length];
            
            for (int i = 0; i < data.length; i++)
                sums[i] += data[i] & 0xFF;
            
            count++;
            
            if (count % 100 == 0) {
                double[] tmp = new double[sums.length];
                for (int i = 0; i < sums.length; i++)
                    tmp[i] = sums[i] / count;
            }
        }
        
        for (int i = 0; i < sums.length; i++)
            sums[i] = sums[i] / count; 
    }
    
    public SidescanPanel(SidescanAnalyzer analyzer, SidescanParser parser, int subsystem) {
        this.parent = analyzer;
        ssParser = parser;
        initialize();
        this.subsystem = subsystem;
    }
    
    public void initialize() {
        firstPingTime = ssParser.firstPingTimestamp();
       
        // Deal with panel resize by recreating the image buffers
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                image = ImageUtils.createCompatibleImage(getWidth(), getHeight(),
                        Transparency.OPAQUE);
                bufferedCache = ImageUtils.createCompatibleImage(getWidth(), getHeight(),
                        Transparency.OPAQUE);
                g2d = (Graphics2D) image.getGraphics();
                layer = ImageUtils.createCompatibleImage(getWidth(), getHeight(),
                        Transparency.TRANSLUCENT);
            }
        });

        // Create SlantRangeFilter
//        filter = new SlantRangeImageFilter(0, pingParser.firstLogEntry().getDouble("max_range"), pingParser.firstLogEntry().getRawData("data").length);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }
    
    public IMCMessage getNextMessageWithFrequency(IMraLog parser, double freq) {
        IMCMessage msg;
        while((msg = parser.nextLogEntry()) != null) {
            if(msg.getDouble("frequency") == freq && msg.getInteger("type") == SonarData.TYPE.SIDESCAN.value()) {
                return msg;
            }
        }
        return null;
    }
    
    public void updateImage(long currentTime, long lastUpdateTime) {
        int yref = 0;
        drawList.addAll(ssParser.getLinesBetween(firstPingTime + lastUpdateTime, firstPingTime + currentTime, image.getWidth(), subsystem));
        for(SidescanLine l : drawList) {
            yref +=  l.ysize;
        }

        // This check is to prevent negative array indexes (from dragging too much)
        if (yref <= image.getHeight()) {
//            bufferedCache = Scalr.crop(image, image.getWidth(), image.getHeight() - yref, (BufferedImageOp)null);
            ImageUtils.copySrcIntoDst(image, bufferedCache, 0, 0, image.getWidth(), image.getHeight() - yref, 0, 0, image.getWidth(), image.getHeight());
            g2d.drawImage(bufferedCache, 0, yref, null);
        }
        else {
            yref = image.getHeight() - 1;
        }

        for (SidescanLine sidescanLine : drawList) {
            sidescanLine.ypos = yref - sidescanLine.ypos;
            g2d.drawImage(sidescanLine.image, 0, sidescanLine.ypos, null);
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
                    drawLocation(layer.getGraphics()); // update layer with location information
                if (measure) {
                    drawMeasure(layer.getGraphics());
                }
                layer.getGraphics().setColor(Color.GREEN.brighter());
                layer.getGraphics().drawString(""+subsystem, 10, 10);
                drawMarks(layer.getGraphics());
                g.drawImage(layer, 0, 0, null);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void drawZoom(Graphics g) {
        mouseX = (int) MathMiscUtils.clamp(mouseX, 50, image.getWidth() - 50);
        mouseY = (int) MathMiscUtils.clamp(mouseY, 50, image.getHeight() - 50);
        BufferedImage zoomImage = image.getSubimage(mouseX - 50, mouseY - 50, 100, 100);
        g.drawImage(ImageUtils.getFasterScaledInstance(zoomImage, 300, 300), image.getWidth() - 301,
                image.getHeight() - 301, null);
    }

    
    void drawLocation(Graphics g) {

        if (mouseSidescanLine != null) {
            Graphics2D location2d = (Graphics2D) mouseLocationImage.getGraphics();
            location2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            location2d.clearRect(0, 0, mouseLocationImage.getWidth(), mouseLocationImage.getHeight());
            location2d.drawString(CoordinateUtil.dmToLatString(CoordinateUtil.decimalDegreesToDM(mouseSidescanLine
                    .calcPointForCoord(mouseX).location.getLatitudeAsDoubleValue())), 5, 15);
            location2d.drawString(CoordinateUtil.dmToLonString(CoordinateUtil.decimalDegreesToDM(mouseSidescanLine
                    .calcPointForCoord(mouseX).location.getLongitudeAsDoubleValue())), 5, 26);
            location2d.drawString(altStr+": " + altFormat.format(mouseSidescanLine.state.getDouble("alt")), 5, 37);
            location2d.drawString(rollStr+": " + altFormat.format(Math.toDegrees(mouseSidescanLine.state.getDouble("phi"))), 5, 48);

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
                        if (timestamp >= old.state.getTimestampMillis() && timestamp <= line.state.getTimestampMillis()) {
                            if (m.x == 0 && m.y == 0) {
                                g.fillRect(0, line.ypos - 1, 10, 2);
                                g.fillRect(line.image.getWidth(null) - 10, line.ypos - 1, 10, 2);
                                g.drawString(m.label, m.x - (m.w / 2), line.ypos - (m.h / 2) - 10);
                            }
                            else {
                                g.drawRect(m.x - (m.w / 2), line.ypos - (m.h / 2), m.w, m.h);
                                g.drawString(m.label, m.x - (m.w / 2), line.ypos - (m.h / 2) - 10);
                            }
                            break;
                        }
                    }
                    old = line;
                }
            }
        }
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public void clearLines() {
        lineList.clear();
        
        image.getGraphics().clearRect(0, 0, image.getWidth(), image.getHeight());
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        int y = e.getY();
        for (SidescanLine line : lineList) {
            if (y >= line.ypos && y <= (line.ypos + line.ysize)) {
                mouseSidescanLine = line;
                ((JPanel) e.getSource()).repaint();
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
            if (e.isShiftDown() && e.isControlDown() && !parent.getTimeline().isRunning()) {
                marking = true;
                initialX = mouseX;
                initialY = mouseY;
            }
            else if (e.isShiftDown()) {
                zoom = true;
            }
            else if (e.isControlDown() && !parent.getTimeline().isRunning()) {
                measure = true;
                pointList.add(mouseSidescanLine.calcPointForCoord(mouseX));
            }
            else {
                info = true;
            }
            ((JPanel) e.getSource()).repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        zoom = false;
        info = false;

        if (marking) {
            String res = JOptionPane.showInputDialog("Insert marker name");
            // Check for a valid response
            if(res != null) {
                int x = (mouseX + initialX) / 2;
                int y = (mouseY + initialY) / 2;
                SidescanLine l = null;
                for (SidescanLine line : lineList) {
                    if (y >= line.ypos && y <= (line.ypos + line.ysize)) {
                        l = line;
                    }
                }
                SidescanPoint point = l.calcPointForCoord(x);
                parent.mraPanel.addMarker(new LogMarker(res, l.state.getTimestampMillis(), point.location
                        .getLatitudeAsDoubleValueRads(), point.location.getLongitudeAsDoubleValueRads(), x, y, Math
                        .abs(mouseX - initialX), Math.abs(mouseY - initialY)));
            }
            marking = false;
        }

        if (!e.isControlDown()) {
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
