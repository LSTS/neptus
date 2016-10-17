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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsm
 * 17 Oct 2016
 */
package pt.lsts.neptus.plugins.interfaces;

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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.mra.api.SidescanGuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 * @author tsm
 *
 */
@SuppressWarnings("serial")
public abstract class RealTimeWatefallViewer<T> extends JPanel {
    
    // abstract methods
    protected abstract void updateImage();
    
    
    
    protected static final int MAX_RULER_SIZE = 15;
    
    // Parameters
    protected ColorMap colorMap = ColorMapFactory.createBronzeColormap();

    // GUI
    protected JPanel ruller = null;
    protected JPanel viewer = null;
    
    protected BufferedImage ssImage = null;
    protected BufferedImage ssImageTmp = null;
    protected BufferedImage ssLayer = null;
    
    // Data
    protected List<T> dataList = Collections.synchronizedList(new ArrayList<T>());
    protected List<T> queuedData = Collections.synchronizedList(new ArrayList<T>());
    
    protected int rangeForRulerMeters = 30;
    protected int rangeForRulerStepMeters = 10;
    
    protected ExecutorService threadExecutor;
    
    public interface LayerPainter {
        public void paint(Graphics g, BufferedImage layer);
    }
    
    protected LayerPainter preLayerPainter = null;
    protected LayerPainter postLayerPainter = null;
    protected LayerPainter overPainter = null;
    
    public RealTimeWatefallViewer(Class<?> clazz) {
        threadExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            String nameBase = new StringBuilder().append(clazz.getSimpleName())
                    .append("::").append(Integer.toHexString(clazz.hashCode()))
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
        initialize();
    }
    
    protected void initialize() {
        removeAll();
        
        ruller = createRullerPanel();
        viewer = createViewerPanel();
        
        setLayout(new MigLayout("ins 0, gap 0", "[][grow]", "[top][grow]"));
        add(ruller, "w 100%, h " + MAX_RULER_SIZE + "px, wrap");
        add(viewer, "w 100%, grow");
    }
    
    protected JPanel createRullerPanel() {
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
    protected JPanel createViewerPanel() {
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
                    synchronized (dataList) {
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
    
    protected void setRangeForRuler(int rangeForRuler) {
        this.rangeForRulerMeters = rangeForRuler;
        this.rangeForRulerStepMeters = SidescanGuiUtils.calcStepForRangeForRuler(rangeForRulerMeters);
    }
    
    /**
     * This returns the sidescan lines.
     * Please synchronize it on use!
     * 
     * @return the lineList
     */
    public List<T> getDataList() {
        return Collections.unmodifiableList(dataList);
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
     * @param overPainter the overPainter to set
     */
    public void setOverPainter(LayerPainter overPainter) {
        this.overPainter = overPainter;
    }
    
    public void clearLines() {
        synchronized (dataList) {
            dataList.clear();
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

    /**
     * @param line
     */
    public void addNewDataine(T... data) {
        if (data.length == 0)
            return;
        addNewDataLine(Arrays.asList(data));
    }
    
    public void addNewDataLine(List<T> lineList) {
        if (lineList.size() == 0)
            return;
        
        threadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (queuedData) {
                    queuedData.addAll(lineList);
                }
            }
        });
    }
    
    public void updateRequest() {
        updateImage();
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                viewer.repaint();
                ruller.repaint();
            }
        });
    }
}
