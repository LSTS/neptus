/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 17 Oct 2016
 */
package pt.lsts.neptus.plugins.interfaces;

import java.awt.Color;
import java.awt.Dimension;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 * @author tsm
 *
 */
@SuppressWarnings("serial")
public abstract class SonarWatefallViewer<T> extends JPanel {

    /* abstract methods */

    // how to display sonar data
    protected abstract void updateImage();
    // allow derived classes to handle viewer parameters update
    public abstract void onViewerPropertiesUpdate();

    // Parameters
    protected ColorMap colorMap = null;

    // GUI
    protected JPanel viewer = null;

    protected BufferedImage dataImage = null;
    protected BufferedImage dataImageTmp = null;
    protected BufferedImage dataLayer = null;

    protected int lastImgSizeH = -1;
    protected int lastImgSizeW = -1;

    // Data
    protected List<T> dataList = Collections.synchronizedList(new ArrayList<T>());
    protected List<T> queuedData = Collections.synchronizedList(new ArrayList<T>());

    // y pos of a given swath, in the waterfall viewer
    private Map<T, Integer> yPos = new ConcurrentHashMap<>();

    protected ExecutorService threadExecutor;

    public interface LayerPainter {
        void paint(Graphics g, BufferedImage layer);
    }

    protected LayerPainter preLayerPainter = null;
    protected LayerPainter postLayerPainter = null;
    protected LayerPainter overPainter = null;

    public SonarWatefallViewer(Class<?> clazz) {
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

        viewer = createViewerPanel();

        setLayout(new MigLayout("ins 0, gap 0", "[][grow]", "[top][grow]"));
        add(viewer, "w 100%, h 100%,  grow");
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

                    if (dataImage != null && dataLayer != null) {
                        g.drawImage(dataImage, 0, 0, null); // Draw data image

                        Graphics2D lg2d = (Graphics2D) dataLayer.getGraphics();
                        lg2d.setBackground(new Color(255, 255, 255, 0));
                        lg2d.clearRect(0, 0, dataLayer.getWidth(), dataLayer.getHeight()); // Clear layer image

                        if (preLayerPainter != null)
                            preLayerPainter.paint(dataLayer.getGraphics(), dataLayer);

                        if (postLayerPainter != null)
                            postLayerPainter.paint(dataLayer.getGraphics(), dataLayer);

                        g.drawImage(dataLayer, 0, 0, null); // Draw layer

                        if (overPainter != null)
                            overPainter.paint(g, dataLayer);
                    }
                    else if(dataImage == null)
                        createImages();
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
                    createImages();
                }
            }
        });
        vPanel.setPreferredSize(new Dimension(this.getWidth(), this.getHeight()));
        return vPanel;
    }

    // Create images and layer to display the data
    private void createImages() {
        synchronized (dataList) {
            dataImage = ImageUtils.createCompatibleImage(viewer.getWidth(), viewer.getHeight(), Transparency.OPAQUE);
            dataImageTmp = ImageUtils.createCompatibleImage(viewer.getWidth(), viewer.getHeight(), Transparency.OPAQUE);
            dataLayer = ImageUtils.createCompatibleImage(viewer.getWidth(), viewer.getHeight(), Transparency.TRANSLUCENT);
        }
    }

    /**
     * Set Y position in the viewer of the given data line
     * */
    protected void setYPos(T data, int y) {
        yPos.put(data, y);
    }

    /**
     * Get Y position in the viewer of the given data line
     * */
    protected int getYPos(T data) {
        return yPos.get(data);
    }

    /**
     * Remove Y positions of all the given data lines
     * */
    protected void removeYPos(ArrayList<BathymetrySwath> data) {
        data.stream()
                .forEach(dataLine -> yPos.remove(dataLine));
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
     * Exposing the panel with the data image waterfall.
     *
     * @return
     */
    public JPanel getDataImagePanel() {
        return viewer;
    }

    /**
     * @return the BufferedImage where the data
     * is painted
     */
    public BufferedImage getDataImage() {
        return dataImage;
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
        synchronized (queuedData) {
            queuedData.clear();
        }
        synchronized (dataList) {
            dataList.clear();
            dataImage.getGraphics().clearRect(0, 0, dataImage.getWidth(), dataImage.getHeight());
            dataImageTmp.getGraphics().clearRect(0, 0, dataImage.getWidth(), dataImage.getHeight());
        }
    }

    @SuppressWarnings("unchecked")
    public void addNewData(T... data) {
        if (data.length == 0)
            return;
        addNewData(Arrays.asList(data));
    }

    public void addNewData(List<T> lineList) {
        if (lineList.size() == 0)
            return;

        threadExecutor.execute(() -> {
            synchronized (queuedData) {
                queuedData.addAll(lineList);
            }
        });
    }

    public void updateRequest() {
        if(colorMap == null)
            return;
        updateImage();
        SwingUtilities.invokeLater(() -> viewer.repaint());
    }
}
