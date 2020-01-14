/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 08/10/2016
 */
package pt.lsts.neptus.plugins.sidescan.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.imgscalr.Scalr;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.SonarData;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanGuiUtils;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanUtil;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.LsfLogSource;
import pt.lsts.neptus.util.sidescan.SlantRangeImageFilter;

/**
 * This panel is to be used for sidescan waterfall viewing.
 * You should feed new {@link SidescanLine}s and setup the parameters for 
 * 
 * Lines need to be already normalized. Any value <0 and >1 are saturated to 0 and 1.
 * 
 * Also you should call {@link #updateRequest()} in a period of your choosing
 * in order to the new lines be added to the image.
 * 
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class SidescanViewerPanel extends JPanel {

    private static final int MAX_RULER_SIZE = 15;

    // Parameters
    private ColorMap colorMap = ColorMapFactory.createBronzeColormap();
    private boolean slantRangeCorrection = false;
    private boolean speedCorrection = true;

    // GUI
    private JPanel ruller = null;
    private JPanel viewer = null;
    
    private BufferedImage ssImage = null;
    private BufferedImage ssImageTmp = null;
    private BufferedImage ssLayer = null;
    
    // Data
    private List<SidescanLine> lineList = Collections.synchronizedList(new ArrayList<SidescanLine>());
    private List<SidescanLine> queuedlines = Collections.synchronizedList(new ArrayList<SidescanLine>());
    
    private int rangeForRulerMeters = 30;
    private int rangeForRulerStepMeters = 10;

    private int lastImgSizeH = -1;
    private int lastImgSizeW = -1;
    
    private ExecutorService threadExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        String nameBase = new StringBuilder().append(SidescanViewerPanel.class.getSimpleName())
                .append("::").append(Integer.toHexString(SidescanViewerPanel.this.hashCode()))
                        .append("::").toString();
        ThreadGroup group = new ThreadGroup(nameBase);
        AtomicLong c = new AtomicLong(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread td = new Thread(group, r, nameBase + c.getAndIncrement());
            td.setDaemon(true);
            return td;
        }
    });

    public interface LayerPainter {
        public void paint(Graphics g, BufferedImage layer);
    }
    
    private LayerPainter preLayerPainter = null;
    private LayerPainter postLayerPainter = null;
    private LayerPainter overPainter = null;
    
    public SidescanViewerPanel() {
        initialize();
    }

    private void initialize() {
        removeAll();
        
        ruller = createRullerPanel();
        viewer = createViewerPanel();
        
        setLayout(new MigLayout("ins 0, gap 0", "[][grow]", "[top][grow]"));
        add(ruller, "w 100%, h " + MAX_RULER_SIZE + "px, wrap");
        add(viewer, "w 100%, grow");
    }

    /**
     * @return
     */
    private JPanel createRullerPanel() {
        JPanel rPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                try {
                    super.paintComponent(g);
                    SidescanGuiUtils.drawRuler(g, this.getWidth(), MAX_RULER_SIZE, rangeForRulerMeters,
                            rangeForRulerStepMeters);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        return rPanel;
    }

    /**
     * @return
     */
    private JPanel createViewerPanel() {
        JPanel vPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                try {
                    super.paintComponent(g);

                    if (ssImage != null && ssLayer != null) {
                        g.drawImage(ssImage, 0, 0, null); // Draw sidescan image

                        Graphics2D lg2d = (Graphics2D) ssLayer.getGraphics();
                        lg2d.setBackground(new Color(255, 255, 255, 0));
                        lg2d.clearRect(0, 0, ssLayer.getWidth(), ssLayer.getHeight()); // Clear layer image

                        if (preLayerPainter != null)
                            preLayerPainter.paint(ssLayer.getGraphics(), ssLayer);
                        
                        // SidescanGuiUtils.drawRuler(ssLayer, MAX_RULER_SIZE, rangeForRulerMeters, rangeForRulerStepMeters);;

                        if (postLayerPainter != null)
                            postLayerPainter.paint(ssLayer.getGraphics(), ssLayer);

                        g.drawImage(ssLayer, 0, 0, null); // Draw layer
                        
                        if (overPainter != null)
                            overPainter.paint(g, ssLayer);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        // Deal with panel resize by recreating the image buffers
        vPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (e.getID() == ComponentEvent.COMPONENT_RESIZED) {
                    synchronized (lineList) {
                        ssImage = ImageUtils.createCompatibleImage(viewer.getWidth(), viewer.getHeight(), Transparency.OPAQUE);
                        ssImageTmp = ImageUtils.createCompatibleImage(viewer.getWidth(), viewer.getHeight(), Transparency.OPAQUE);
                        ssLayer = ImageUtils.createCompatibleImage(viewer.getWidth(), viewer.getHeight(), Transparency.TRANSLUCENT);
//                    clearLines();
                    }
                }
            }
        });
        
        return vPanel;
    }

    /**
     * This returns the sidescan lines.
     * Please synchronize it on use!
     * 
     * @return the lineList
     */
    public List<SidescanLine> getLineList() {
        return Collections.unmodifiableList(lineList);
    }
    
    /**
     * Exposing the panel with the sidescan image waterfall.
     * 
     * @return
     */
    public JPanel getSsImamagePanel() {
        return viewer;
    }
    
    /**
     * @return the ssImage
     */
    public BufferedImage getSsImage() {
        return ssImage;
    }
    
    /**
     * @return the colorMap
     */
    public ColorMap getColorMap() {
        return colorMap;
    }

    /**
     * @param colorMap the colorMap to set
     */
    public void setColorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
    }

    /**
     * @return the slantRangeCorrection
     */
    public boolean isSlantRangeCorrection() {
        return slantRangeCorrection;
    }

    /**
     * @param slantRangeCorrection the slantRangeCorrection to set
     */
    public void setSlantRangeCorrection(boolean slantRangeCorrection) {
        this.slantRangeCorrection = slantRangeCorrection;
    }

    /**
     * @return the speedCorrection
     */
    public boolean isSpeedCorrection() {
        return speedCorrection;
    }

    /**
     * @param speedCorrection the speedCorrection to set
     */
    public void setSpeedCorrection(boolean speedCorrection) {
        this.speedCorrection = speedCorrection;
    }

    /**
     * @param preLayerPainter the preLayerPainter to set
     */
    public void setPreLayerPainter(LayerPainter preLayerPainter) {
        this.preLayerPainter = preLayerPainter;
    }
    
    /**
     * @param postLayerPainter the postLayerPainter to set
     */
    public void setPostLayerPainter(LayerPainter postLayerPainter) {
        this.postLayerPainter = postLayerPainter;
    }
    
    /**
     * @param overPainter the overPainter to set
     */
    public void setOverPainter(LayerPainter overPainter) {
        this.overPainter = overPainter;
    }
    
    public void clearLines() {
        synchronized (queuedlines) {
            queuedlines.clear();
        }
        synchronized (lineList) {
            lineList.clear();
            ssImage.getGraphics().clearRect(0, 0, ssImage.getWidth(), ssImage.getHeight());
            ssImageTmp.getGraphics().clearRect(0, 0, ssImage.getWidth(), ssImage.getHeight());
        }
    }
    
    /**
     * @return the rangeForRulerMeters
     */
    public int getRangeForRulerMeters() {
        return rangeForRulerMeters;
    }
    
    private void setRangeForRuler(int rangeForRuler) {
        this.rangeForRulerMeters = rangeForRuler;
        this.rangeForRulerStepMeters = SidescanGuiUtils.calcStepForRangeForRuler(rangeForRulerMeters);
    }

    /**
     * @param line
     */
    public void addNewSidescanLine(SidescanLine... line) {
        if (line.length == 0)
            return;
        addNewSidescanLine(Arrays.asList(line));
    }

    /**
     * @param line
     */
    public void addNewSidescanLine(List<SidescanLine> lineList) {
        if (lineList.size() == 0)
            return;
        
        threadExecutor.execute(() -> {
            synchronized (queuedlines) {
                queuedlines.addAll(lineList);
            }
        });
    }

    /**
     * To allow a more smooth processing the update of the SSS image is triggered from outside.
     */
    public void updateRequest() {
        updateImage();
    }
    
    private void updateImage() {
        if (ssImage == null)
            return;
        
        ArrayList<SidescanLine> addList = new ArrayList<SidescanLine>();
        ArrayList<SidescanLine> removeList = new ArrayList<SidescanLine>();

        synchronized (queuedlines) {
            addList.addAll(queuedlines);
            queuedlines.clear();
        }
    
        synchronized (lineList) {
            long prevPingTime = lineList.size() == 0 ? -1 : lineList.get(lineList.size() - 1).getTimestampMillis();
            int yRef = 0;
            
            int originalLinesSize = lineList.size();

            boolean ssImageChanged = lastImgSizeH != ssImage.getHeight()
                    || lastImgSizeW != ssImage.getWidth();
            lastImgSizeH = ssImage.getHeight();
            lastImgSizeW = ssImage.getWidth();
            
            boolean someChangesToImageMade = ssImageChanged;
            
            for (SidescanLine l : addList) {
                someChangesToImageMade = true;
                
                // Update the rangeMax to the ruler
                if (l.getRange() != rangeForRulerMeters) {
                    setRangeForRuler(Math.round(l.getRange()));
                }
                
                // Deal with speed correction here, because this would be repeated code in the various parsers
                if (speedCorrection) {
                    double horizontalScale = ssImage.getWidth() / (l.getRange() * 2f);
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
                yRef += l.getYSize();
            }
            
            int d = 0;
            for (SidescanLine sidescanLine : addList) {
                sidescanLine.setYPos(yRef - d);
                d += sidescanLine.getYSize();
                sidescanLine.setImage(new BufferedImage(sidescanLine.getData().length, 1, BufferedImage.TYPE_INT_RGB),
                        false);

                // Apply colormap to data
                for (int c = 0; c < sidescanLine.getData().length; c++) {
                    sidescanLine.getImage().setRGB(c, 0, colorMap.getColor(sidescanLine.getData()[c]).getRGB());
                }

                if (slantRangeCorrection) {
                    sidescanLine.setImage(Scalr.apply(sidescanLine.getImage(),
                            new SlantRangeImageFilter(sidescanLine.getState().getAltitude(), sidescanLine.getRange(),
                                    sidescanLine.getImage().getWidth())), true);
                }
            }

            {
                SidescanLine sidescanLine;
                Iterator<SidescanLine> i = lineList.iterator(); // Must be in synchronized block
                while (i.hasNext()) {
                    sidescanLine = i.next();
                    sidescanLine.setYPos(sidescanLine.getYPos() + yRef);
                    if (sidescanLine.getYPos() > ssImage.getHeight())
                        removeList.add(sidescanLine);
                }
                lineList.addAll(addList);
                lineList.removeAll(removeList);
            }

            // This check is to prevent negative array indexes (from dragging too much)
            if (yRef <= ssImage.getHeight()) {
                if (!ssImageChanged && originalLinesSize > 0 && yRef > 0) {
                    someChangesToImageMade = true;
                    ImageUtils.copySrcIntoDst(ssImage, ssImageTmp, 0, 0, ssImage.getWidth(), ssImage.getHeight() - yRef,
                            0, yRef, ssImage.getWidth(), ssImage.getHeight());
                }
            }
            else {
                yRef = ssImage.getHeight() - 1;
            }

            if (someChangesToImageMade) {
                Graphics2D g2d = (Graphics2D) ssImageTmp.getGraphics();
                for (SidescanLine ssl : (ssImageChanged ? lineList : addList)) {
                    g2d.drawImage(ImageUtils.getScaledImage(ssl.getImage(), ssImage.getWidth(), ssl.getYSize(), true), 0,
                            ssl.getYPos(), null);
                }
                
                Graphics2D g3d = (Graphics2D) ssImage.getGraphics();
                g3d.drawImage(ssImageTmp, 0, 0, null);
            }
            
            addList.clear();
            removeList.clear();
        }
        SwingUtilities.invokeLater(() -> {
            viewer.repaint();
            ruller.repaint();
        });
    }
    
    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        SidescanViewerPanel ssPanel = new SidescanViewerPanel();
        GuiUtils.testFrame(ssPanel, ssPanel.getName(), 800, 500);

        Timer timer = new Timer("Timer", true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ssPanel.updateRequest();
            }
        };
        timer.scheduleAtFixedRate(task, 10, 50);
        
//        System.in.read();

        ArrayList<String> fxStrList = new ArrayList<>();
        fxStrList.add("D:\\LSTS-Logs\\TestsSpeedsSideScan\\NP1\\113431_ss-speed-test-np1\\Data.lsf");
        fxStrList.add("D:\\LSTS-Logs\\2015-05-28-netmar\\lauv-np3-20150528-075930_sss_np3\\Data.lsf");

        int repeatTimes = 1;

        for (String fxStr : fxStrList) {
            LsfLogSource ls = new LsfLogSource(fxStr, null);
            LsfIndex index = ls.getLsfIndex();
            SonarData sd1 = index.getFirst(SonarData.class);
            //csl.setMainSystem(sd1.getSourceName());
            IMraLog eState = ls.getLog(EstimatedState.class.getSimpleName());
            SidescanParameters sidescanParams = new SidescanParameters(0.2, 280);
            for (int i = 0; i < repeatTimes; i++) {
                for (SonarData sd : index.getIterator(SonarData.class)) {
                    if (sd.getType() != SonarData.TYPE.SIDESCAN) {
                        continue;
                    }
                    
                    EstimatedState estState = (EstimatedState) eState.getEntryAtOrAfter(sd.getTimestampMillis()); 
                    SystemPositionAndAttitude pose;
                    if (estState == null
                            || Math.abs(estState.getTimestampMillis() - sd.getTimestampMillis()) > 500) {
                        pose = new SystemPositionAndAttitude();
                        // return;
                    }
                    else {
                        pose = new SystemPositionAndAttitude(estState);
                    }
                    SidescanLine line = SidescanUtil.getSidescanLine(sd, pose, sidescanParams);
                    if (line != null)
                        ssPanel.addNewSidescanLine(line);
//                  System.out.println(sd.getTimestampMillis());
                    Thread.sleep(5);
                    Thread.yield();
                }
            }
        }
    }
}
