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
 * Author: José Pinto
 * Dec 8, 2012
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ProgressMonitor;
import javax.swing.plaf.basic.BasicSliderUI;

import org.imgscalr.Scalr;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.Timeline;
import pt.lsts.neptus.gui.TimelineChangeListener;
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
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.VideoCreator;
import pt.lsts.neptus.util.llf.LsfReportProperties;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Photos", icon = "images/downloader/camera.png")
public class MraPhotosVisualization extends JComponent implements MRAVisualization, LogMarkerListener {
    private static final long serialVersionUID = 1L;
    protected File photosDir;
    protected LsfIndex index;
    protected Image imageToDisplay = null;
    protected double curTime = 0;
    protected File curFile = null;
    protected PhotoToolbar toolbar = null;
    protected double speedMultiplier = 1.0;
    protected boolean brighten = false;
    protected boolean grayscale = false;
    protected boolean contrast = false;
    protected boolean sharpen = false;
    protected boolean grayHist = false;
    protected boolean colorHist = false;
    protected boolean showLegend = true;
    protected BufferedImage bufferedTempOriginal = null;
    protected Mat matGray;
    protected Mat matGrayTemp;
    protected Mat matColor;
    protected List<Mat> lRgb;
    protected BufferedImageOp contrastOp = ImageUtils.contrastOp();
    protected BufferedImageOp sharpenOp = ImageUtils.sharpenOp();
    protected BufferedImageOp brightenOp = ImageUtils.brightenOp(1.2f, 0);
    protected BufferedImageOp grayscaleOp = ImageUtils.grayscaleOp();
    protected BufferedImageOp whiteBalanceOp = null;
    protected LinkedHashMap<File, SystemPositionAndAttitude> states = null;
    protected MRAPanel panel;
    protected MraVehiclePosHud hud;
    protected Point2D zoomPoint = null;
    protected boolean fullRes = false;
    protected boolean rotateToPaintImage = false;
    protected Vector<LogMarker> markers = new Vector<>();
    
    private File[] allFiles;
    long startTime;
    long endTime;
    Timeline timeline;

    public MraPhotosVisualization(MRAPanel panel) {
        this.panel = panel;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                    showPopup(curFile, e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                    return;

                if (imageToDisplay == null)
                    return;
                zoomPoint = e.getPoint();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                    return;

                zoomPoint = null;
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                    return;

                if (imageToDisplay == null)
                    return;

                zoomPoint = e.getPoint();
                repaint();
            }
        });
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        this.photosDir = source.getFile("Photos");
        this.index = source.getLsfIndex();
        this.hud = new MraVehiclePosHud(source, 150, 150);
        allFiles = listPhotos(getPhotosDir());

        this.startTime = (long) (timestampOf(allFiles[0]) * 1000.0);
        this.endTime = (long) (timestampOf(allFiles[allFiles.length - 1]) * 1000.0);

        timeline = new Timeline(0, (int) (endTime - startTime), 7, 1000, false);
        timeline.addTimelineChangeListener(new TimelineChangeListener() {
            @Override
            public void timelineChanged(int value) {
                setTime((startTime + value) / 1000.0);
            }
        });

        timeline.getSlider().setValue(0);

        timeline.getSlider().setUI(new BasicSliderUI(timeline.getSlider()) {
            @Override
            public void paintTicks(Graphics g) {
                super.paintTicks(g);
                for (LogMarker m : markers) {
                    long mtime = Double.valueOf(m.getTimestamp()).longValue();
                    g.drawLine(xPositionForValue((int) (mtime - startTime)), 0,
                            xPositionForValue((int) (mtime - startTime)), timeline.getSlider().getHeight() / 2);
                }
            }
        });

        toolbar = new PhotoToolbar(this);

        loadStates();

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

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getFile("Photos") != null && source.getFile("Photos").isDirectory();
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
        if (photosDir == null)
            return;

        double sliderValue = marker.getTimestamp() - startTime;
        timeline.getSlider().setValue((int)sliderValue);
    }

    public File getCurFile() {
        return curFile;
    }

    public void setCurFile(File curFile) {
        this.curFile = curFile;

        try {
            imageToDisplay = loadImage(curFile, false);
            curTime = timestampOf(curFile);
            timeline.setTime((long)(curTime * 1000));
            repaint();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File[] listPhotos(File photosDir) {
        File[] files = photosDir.listFiles();
        Vector<File> allF = new Vector<>();

        for (File f : files) {
            if (!f.isDirectory())
                allF.add(f);
            else
                allF.addAll(Arrays.asList(f.listFiles()));
        }

        for (int i = 0; i < allF.size(); i++) {
            if (!allF.get(i).getName().endsWith(".jpg"))
                allF.remove(i--);
        }

        files = allF.toArray(new File[0]);
        Arrays.sort(files);

        return files;
    }

    public double getCurTime() {
        return curTime;
    }

    protected void loadStates() {
        File[] files = listPhotos(getPhotosDir());

        int lastIndex = 0;
        int stateId = index.getDefinitions().getMessageId("EstimatedState");
        int lastBDistanceIndex = 0;
        int bdistId = index.getDefinitions().getMessageId("BottomDistance");
        int dvlId = index.getEntityId("DVL");

        states = new LinkedHashMap<>();
        for (int i = 0; i < files.length; i++) {
            int msgIndex = index.getMessageAtOrAfer(stateId, 0xFF, lastIndex, timestampOf(files[i]));

            if (msgIndex == -1) {
                states.put(files[i], null);
            }
            else {
                lastIndex = msgIndex;
                IMCMessage m = index.getMessage(msgIndex);
                LocationType loc = new LocationType(Math.toDegrees(m.getDouble("lat")), Math.toDegrees(m
                        .getDouble("lon")));
                loc.setDepth(m.getDouble("depth"));
                loc.translatePosition(m.getDouble("x"), m.getDouble("y"), 0);
                loc.convertToAbsoluteLatLonDepth();
                SystemPositionAndAttitude state = new SystemPositionAndAttitude(loc, m.getDouble("phi"),
                        m.getDouble("theta"), m.getDouble("psi"));
                if (m.getTypeOf("alt") == null) {
                    state.setW(Double.NaN);
                    int bdIndex = index.getMessageAtOrAfer(bdistId, dvlId, lastBDistanceIndex, timestampOf(files[i]));

                    if (bdIndex != -1) {
                        state.setW(index.getMessage(bdIndex).getDouble("value"));
                        lastBDistanceIndex = bdIndex;
                    }
                }
                else {
                    state.setW(m.getDouble("alt"));
                }

                state.setU(m.getDouble("u"));
                states.put(files[i], state);
            }
        }
    }

    protected void showPopup(final File f, MouseEvent evt) {
        JPopupMenu popup = new JPopupMenu();
        popup.add("Add marker").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (LsfReportProperties.generatingReport == true) {
                    GuiUtils.infoMessage(panel.getRootPane(), I18n.text("Can not add Marks"),
                            I18n.text("Can not add Marks - Generating Report."));
                    return;
                }

                String resp = JOptionPane.showInputDialog(panel, "Enter marker name", "Add marker",
                        JOptionPane.QUESTION_MESSAGE);
                if (resp == null)
                    return;
                SystemPositionAndAttitude state = states.get(f);
                LogMarker marker = new LogMarker(resp, timestampOf(f) * 1000, state.getPosition().getLatitudeRads(),
                        state.getPosition().getLongitudeRads());
                panel.addMarker(marker);
            }
        });

        popup.add(I18n.text("Save image")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File output = new File(photosDir.getParentFile(), FileUtil.getFileNameWithoutExtension(f) + ".png");

                try {
                    boolean prevFullRes = fullRes;
                    fullRes = true;
                    Image m = loadImage(f, true);
                    fullRes = prevFullRes;
                    BufferedImage tmp = new BufferedImage(m.getWidth(null), m.getHeight(null),
                            BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = (Graphics2D) tmp.getGraphics();
                    Image watermark = ImageUtils.getImage("pt/lsts/neptus/plugins/mraplots/lsts-watermark.png");

                    Graphics2D g1 = (Graphics2D) g.create();
                    if (rotateToPaintImage) {
                        g1 = rotateGraphics(g1, tmp.getWidth(), tmp.getHeight());
                    }
                    g1.drawImage(m, 0, 0, tmp.getWidth(), tmp.getHeight(), 0, 0,
                            m.getWidth(null), m.getHeight(null), null);
                    g1.dispose();

                    if (showLegend) {
                        drawLegend(g, f);
                        g.drawImage(hud.getImage(timestampOf(f)), 10, tmp.getHeight() - 160, null);
                        g.drawImage(watermark, tmp.getWidth() - 55, tmp.getHeight() - 55, null);
                    }

                    ImageIO.write(tmp, "PNG", output);
                    GuiUtils.infoMessage(panel, I18n.text("Image saved"),
                            I18n.textf("Image saved in %filename", output.getAbsolutePath()));
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(MraPhotosVisualization.this, ex);
                }
            }
        });

        JMenuItem exportVideo = new JMenuItem(I18n.text("Export video"),
                ImageUtils.getIcon("pt/lsts/neptus/plugins/mraplots/film.png"));
        popup.add(exportVideo);
        exportVideo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final File[] allFiles = listPhotos(getPhotosDir());
                final double startTime = timestampOf(allFiles[0]);
                try {

                    final VideoCreator creator = new VideoCreator(new File(photosDir.getParentFile(), "Video.mp4"),
                            800, 600);
                    final ProgressMonitor monitor = new ProgressMonitor(panel, I18n.text("Creating video"),
                            I18n.text("Starting up"), 0, allFiles.length);

                    Thread videoMaker = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            fullRes = true;
                            Image watermark = null;

                            if (new File("conf/mra-watermark.png").canRead()) {
                                watermark = ImageUtils.getImage(new File("conf/mra-watermark.png").getPath());
                            }

                            BufferedImage tmp = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g = tmp.createGraphics();
                            int count = 0;
                            double speedMult = speedMultiplier;

                            for (File f : allFiles) {
                                if (monitor.isCanceled())
                                    break;
                                try {
                                    Image m = loadImage(f, false);

                                    Graphics2D g1 = (Graphics2D) g.create();
                                    if (rotateToPaintImage) {
                                        g1 = rotateGraphics(g1, tmp.getWidth(), tmp.getHeight());
                                    }
                                    g1.drawImage(m, 0, 0, tmp.getWidth(), tmp.getHeight(), 0, 0, m.getWidth(null),
                                            m.getHeight(null), null);
                                    g1.dispose();

                                    if (showLegend) {
                                        drawLegend(g, f);
                                        g.drawImage(hud.getImage(timestampOf(f)), 10, tmp.getHeight() - 160, null);
                                        if (watermark != null)
                                            g.drawImage(watermark, tmp.getWidth() - 55, tmp.getHeight() - 55, null);
                                    }
                                    monitor.setNote("Processing " + f.getName());
                                    monitor.setProgress(count++);
                                    creator.addFrame(tmp, (long) ((timestampOf(f) - startTime) * 1000 / speedMult));
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                }
                            }
                            creator.closeStreams();
                            monitor.close();
                            fullRes = false;
                            GuiUtils.infoMessage(panel, I18n.text("Production completed"),
                                    I18n.textf("Video saved in %filename", new File(photosDir.getParentFile(), "Video.mp4").getAbsolutePath()));
                        }
                    });
                    videoMaker.setName("Video maker thread");
                    videoMaker.start();

                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(panel, ex);
                }
            }
        });
        
        popup.show(MraPhotosVisualization.this, evt.getX(), evt.getY());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Image copy = imageToDisplay;

        if (copy != null) {
            Graphics2D g1 = (Graphics2D) g.create();
            if (rotateToPaintImage) {
                g1 = rotateGraphics(g1, getWidth(), getHeight());
            }
            g1.drawImage(copy, 0, 0, getWidth(), getHeight(), 0, 0,
                    copy.getWidth(this), copy.getHeight(this), this);
            g1.dispose();
        }

        int countMarkers = 0;
        for (int i = 0; i < markers.size(); i++) {

            if (markers.get(i).getTimestamp() / 1000 > curTime + 2)
                break;
            if (markers.get(i).getTimestamp() / 1000 < curTime - 2)
                continue;

            countMarkers++;
            int alpha = (int) (127.5 * Math.abs(markers.get(i).getTimestamp() / 1000 - curTime));

            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 255, 128, 255 - alpha));
            g.drawString(markers.get(i).getLabel(), 10, 150 + countMarkers * 15);
        }

        if (showLegend) {
            drawLegend((Graphics2D) g, curFile);
            g.drawImage(hud.getImage(curTime), 10, getHeight() - 160, null);
        }

        if (zoomPoint != null && imageToDisplay != null) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int imgX = (int) ((zoomPoint.getX() / getWidth()) * imageToDisplay.getWidth(null));
            int imgY = (int) ((zoomPoint.getY() / getHeight()) * imageToDisplay.getHeight(null));
            Graphics2D g1 = (Graphics2D) g.create();
            if (rotateToPaintImage) {
                g1 = rotateGraphics(g1, getWidth(), getHeight());
                g1.drawImage(imageToDisplay, 210, 210, 10, 10,
                        getWidth() - (imgX - 25), getHeight() - (imgY - 25 + 25),
                        getWidth() - (imgX + 25), getHeight() - (imgY + 25 + 25), null);
            } else {
                g1.drawImage(imageToDisplay, getWidth() - 210, getHeight() - 210, getWidth() - 10,
                        getHeight() - 10, imgX - 25, imgY - 25, imgX + 25, imgY + 25, null);
            }
            g1.dispose();
        }
    }

    private Graphics2D rotateGraphics(Graphics2D g, int width, int height) {
        g.translate(width / 2.0, height / 2.0);
        g.rotate(Math.toRadians(180));
        g.translate(-width / 2.0, -height / 2.0);
        return g;
    }

    public double timestampOf(File f) {
        return Double.parseDouble(f.getName().substring(0, f.getName().lastIndexOf('.')));
    }

    protected Image loadImage(File f, boolean useFullResolution) throws IOException {
        Vector<BufferedImageOp> ops = new Vector<>();
        if (whiteBalanceOp != null)
            ops.add(whiteBalanceOp);
        if (contrast)
            ops.add(contrastOp);
        if (sharpen)
            ops.add(sharpenOp);
        if (brighten)
            ops.add(brightenOp);
        if (grayscale)
            ops.add(grayscaleOp);

        BufferedImageOp[] operations = ops.toArray(new BufferedImageOp[0]);

        BufferedImage original = ImageIO.read(f);
        if (fullRes || useFullResolution) {
            if (ops.isEmpty()) {
                operations = new BufferedImageOp[] {ImageUtils.identityOp()};
            }
            original = Scalr.apply(original, operations);
        }
        else {
            original = Scalr.resize(original, getWidth(), getHeight(), operations);
        }
        
        try {
            if(grayHist && !colorHist)
            {
                
                matGrayTemp = new Mat(original.getHeight(), original.getWidth(), CvType.CV_8UC1);
                matGray = new Mat(original.getHeight(), original.getWidth(), CvType.CV_8UC3);
                Imgproc.cvtColor(bufferedImageToMat(original), matGrayTemp, Imgproc.COLOR_RGB2GRAY);
                Imgproc.equalizeHist(matGrayTemp, matGrayTemp);
                Imgproc.cvtColor(matGrayTemp, matGray, Imgproc.COLOR_GRAY2RGB);
                original = matToBufferedImage(matGray);
            }
            
            if(colorHist && !grayHist)
            {
                matColor = new Mat(original.getHeight(), original.getWidth(), CvType.CV_8UC3);
                lRgb = new ArrayList<Mat>(3);
                Core.split(bufferedImageToMat(original), lRgb);
                Mat mR = lRgb.get(0);
                Imgproc.equalizeHist(mR, mR);
                lRgb.set(0, mR);
                Mat mG = lRgb.get(1);
                Imgproc.equalizeHist(mG, mG);
                lRgb.set(1, mG);
                Mat mB = lRgb.get(2);
                Imgproc.equalizeHist(mB, mB);
                lRgb.set(2, mB);
                Core.merge(lRgb, matColor);
                original = matToBufferedImage(matColor);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        return original;
    }
    
    /**  
     * Converts/writes a Mat into a BufferedImage.  
     * @param matrix Mat of type CV_8UC3 or CV_8UC1  
     * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY  
     */  
    protected BufferedImage matToBufferedImage(Mat matrix) {
        int cols = matrix.cols();  
        int rows = matrix.rows();  
        int elemSize = (int)matrix.elemSize();  
        byte[] data = new byte[cols * rows * elemSize];  
        int type;  
        matrix.get(0, 0, data);  
        switch (matrix.channels()) {  
            case 1:  
                type = BufferedImage.TYPE_BYTE_GRAY;  
                break;  
            case 3:  
                type = BufferedImage.TYPE_3BYTE_BGR;  
                // bgr to rgb  
                byte b;  
                for(int i=0; i<data.length; i=i+3) {  
                    b = data[i];  
                    data[i] = data[i+2];  
                    data[i+2] = b;  
                }  
                break;  
        default:  
            return null;  
        }
        BufferedImage image2 = new BufferedImage(cols, rows, type);  
        image2.getRaster().setDataElements(0, 0, cols, rows, data);  
        return image2;
    }
    
    //!Convert bufferedImage to Mat
    protected Mat bufferedImageToMat(BufferedImage in)
    {
          Mat out;
          byte[] data;
          int r, g, b;

          if(in.getType() == BufferedImage.TYPE_INT_RGB)
          {
              out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
              data = new byte[in.getWidth() * in.getHeight() * (int)out.elemSize()];
              int[] dataBuff = in.getRGB(0, 0, in.getWidth(), in.getHeight(), null, 0, in.getWidth());
              for(int i = 0; i < dataBuff.length; i++)
              {
                  data[i*3] = (byte) ((dataBuff[i] >> 16) & 0xFF);
                  data[i*3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
                  data[i*3 + 2] = (byte) ((dataBuff[i] >> 0) & 0xFF);
              }
          }
          else
          {
              out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC1);
              data = new byte[in.getWidth() * in.getHeight() * (int)out.elemSize()];
              int[] dataBuff = in.getRGB(0, 0, in.getWidth(), in.getHeight(), null, 0, in.getWidth());
              for(int i = 0; i < dataBuff.length; i++)
              {
                r = (byte) ((dataBuff[i] >> 16) & 0xFF);
                g = (byte) ((dataBuff[i] >> 8) & 0xFF);
                b = (byte) ((dataBuff[i] >> 0) & 0xFF);
                data[i] = (byte)((0.21 * r) + (0.71 * g) + (0.07 * b)); //luminosity
              }
           }
           out.put(0, 0, data);
           return out;
     } 
    
    protected Integer legendWidth = null;

    protected int getLegendWidth(Vector<String> strs, Graphics2D g) {
        if (legendWidth == null) {
            int maxSize = 0;
            for (String s : strs) {
                Rectangle2D r = g.getFontMetrics(g.getFont()).getStringBounds(s, g);
                if (r.getWidth() > maxSize)
                    maxSize = (int) Math.ceil(r.getWidth());
            }
            legendWidth = maxSize;
        }
        return legendWidth;
    }

    protected void drawLegend(Graphics2D g, File f) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        SystemPositionAndAttitude state = states.get(f);
        if (state == null)
            return;
        long timeUTC = (long) (timestampOf(f) * 1000);
        String depth = GuiUtils.getNeptusDecimalFormat(1).format(state.getPosition().getDepth());
        String alt = Double.isNaN(state.getW()) ? "N/A" : GuiUtils.getNeptusDecimalFormat(1).format(state.getW());
        String speed = GuiUtils.getNeptusDecimalFormat(1).format(state.getU());
        int roll = (int) Math.toDegrees(state.getRoll());
        int pitch = (int) Math.toDegrees(state.getPitch());
        int yaw = (int) Math.toDegrees(state.getYaw());
        String lat = CoordinateUtil.latitudeAsString(state.getPosition().getLatitudeDegs(), false, 2);
        String lon = CoordinateUtil.longitudeAsString(state.getPosition().getLongitudeDegs(), false, 2);

        Vector<String> details = new Vector<>();
        details.add(lat);
        details.add(lon);
        details.add(I18n.text("Time") + ": " + DateTimeUtil.timeFormatterUTC.format(new Date(timeUTC)));
        details.add(I18n.text("Depth") + ": " + depth);
        details.add(I18n.text("Altitude") + ": " + alt);
        details.add(I18n.text("Roll") + ": " + roll);
        details.add(I18n.text("Pitch") + ": " + pitch);
        details.add(I18n.text("Yaw") + ": " + yaw);
        details.add(I18n.text("Speed") + ": " + speed);

        double width = getLegendWidth(details, g) + 20;

        g.setColor(new Color(0, 0, 0, 128));
        g.fill(new RoundRectangle2D.Double(10, 10, width, 155, 20, 20));
        g.setColor(Color.white);

        for (int y = 30; !details.isEmpty(); y += 15) {
            g.drawString(details.firstElement(), 20, y);
            details.remove(0);
        }
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
    }

    public final double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public final void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public File getPhotosDir() {
        return photosDir;
    }

    protected synchronized void setTime(double time) {
        for (int i = 0; i < allFiles.length; i++) {
            if (timestampOf(allFiles[i]) >= time) {
                setCurFile(allFiles[i]);
                return;
            }
        }
    }
}
