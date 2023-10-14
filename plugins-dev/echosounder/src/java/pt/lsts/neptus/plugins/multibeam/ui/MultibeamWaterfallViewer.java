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
 * Author: tsm
 * 12 Oct 2016
 */
package pt.lsts.neptus.plugins.multibeam.ui;

import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.plugins.interfaces.SonarWatefallViewer;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.ImageUtils;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author tsm
 *
 */
@SuppressWarnings("serial")
public class MultibeamWaterfallViewer extends SonarWatefallViewer<BathymetrySwath> {
    private static final int MAX_COLORBAR_SIZE = 15;

    // Max depth defined by the user
    private double maxDepth = Double.MIN_VALUE;

    // adapt max depth to maximum depth found in the data
    // min value is maxDepth
    private boolean useAdaptiveMaxDepth = false;

    // max depth found in the data
    private double adaptiveMaxDepth = Double.MIN_VALUE;

    // color bar panel
    private ColorBar colorBar;

    /**
     * @param clazz
     */
    public MultibeamWaterfallViewer() {
        super(MultibeamWaterfallViewer.class);
        createColorBar();
    }

    private void createColorBar() {
        colorBar = new ColorBar(ColorBar.HORIZONTAL_ORIENTATION, this.colorMap) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);

                Graphics g2 = g.create();

                g2.setColor(ColorUtils.invertColor(this.getCmap().getColor(0)));
                g2.drawString("0m", 2, colorBar.getHeight() - 3);

                long maxVal = Math.round(useAdaptiveMaxDepth ? adaptiveMaxDepth : maxDepth);
                long medVal = Math.round(maxVal / 2d);
                if (maxVal != medVal && this.getWidth() > 150) {
                    String medString = String.valueOf(medVal) + "m";
                    Rectangle2D strBnds = g2.getFontMetrics().getStringBounds(medString, g2);
                    g2.setColor(ColorUtils.invertColor(this.getCmap().getColor(0.5)));
                    g2.drawString(medString, (int) (colorBar.getWidth() / 2d - strBnds.getWidth() / 2d), colorBar.getHeight() - 3);
                }
                
                String maxString = String.valueOf(maxVal) + "m";
                Rectangle2D strBnds = g2.getFontMetrics().getStringBounds(maxString, g2);
                g2.setColor(ColorUtils.invertColor(this.getCmap().getColor(1)));
                g2.drawString(maxString, (int) (colorBar.getWidth() - strBnds.getWidth() - 2), colorBar.getHeight() - 3);
                
                g2.dispose();
            }
        };

        remove(viewer);

        add(colorBar, "w 100%, h " + MAX_COLORBAR_SIZE + "px, wrap");
        add(viewer, "w 100%, grow");
    }

    // code adapted from mra API's
    @Override
    public void updateImage() {
        if (dataImage == null)
            return;

        ArrayList<BathymetrySwath> addList = new ArrayList<>();
        ArrayList<BathymetrySwath> removeList = new ArrayList<>();

        synchronized (queuedData) {
            addList.addAll(queuedData);
            queuedData.clear();
        }

        synchronized (dataList) {
            int yRef = addList.size();

            boolean dataImageChanged = lastImgSizeH != dataImage.getHeight()
                    || lastImgSizeW != dataImage.getWidth();
            lastImgSizeH = dataImage.getHeight();
            lastImgSizeW = dataImage.getWidth();

            boolean someChangesToImageMade = dataImageChanged;

            initNewData(addList, yRef);
            removeUndisplayedData(addList, removeList, yRef);

            // This check is to prevent negative array indexes (from dragging too much)
            if (yRef <= dataImage.getHeight()) {
                if (!dataImageChanged && yRef > 0) {
                    someChangesToImageMade = true;
                    ImageUtils.copySrcIntoDst(dataImage, dataImageTmp, 0, 0, dataImage.getWidth(),
                            dataImage.getHeight() - yRef, 0, yRef, dataImage.getWidth(), dataImage.getHeight());
                }
            }

            if(someChangesToImageMade)
                applyDataChanges(dataImageChanged, addList);

            addList.clear();
            removeList.clear();
            
            if (useAdaptiveMaxDepth)
                repaintColorBar();
        }
    }

    private BufferedImage datatToImage(BathymetrySwath data) {
        BathymetryPoint[] points = data.getData();
        BufferedImage image = new BufferedImage(points.length, 1, BufferedImage.TYPE_INT_RGB);

        double max;
        // apply color map
        for(int i = 0; i < points.length; i++) {
            if (points[i] != null) {
                // compute new max depth
                if(useAdaptiveMaxDepth) {
                    if(points[i].depth > adaptiveMaxDepth)
                        adaptiveMaxDepth = points[i].depth;

                    max = adaptiveMaxDepth;
                }
                else {
                    max = maxDepth;
                }
                image.setRGB(i, 0, colorMap.getColor(points[i].depth / max).getRGB());
            }
        }

        return image;
    }

    /**
     * Removes from memory data that won't be displayed in the viewer (because
     * it won't fit)
     * */
    private void removeUndisplayedData(ArrayList<BathymetrySwath> addList, ArrayList<BathymetrySwath> removeList, int yRef) {
        BathymetrySwath swath;
        Iterator<BathymetrySwath> i = dataList.iterator(); // Must be in synchronized block
        while (i.hasNext()) {
            swath = i.next();
            setYPos(swath, getYPos(swath) + yRef);
            if (getYPos(swath) > dataImage.getHeight())
                removeList.add(swath);
        }
        dataList.addAll(addList);
        dataList.removeAll(removeList);
        removeYPos(removeList);
    }

    /**
     * Sets Y position and BufferedImage for the
     * data in addList
     * */
    private void initNewData(List<BathymetrySwath> addList, int yRef) {
        int d = 0;
        for (BathymetrySwath swath : addList) {
            setYPos(swath, yRef - d);
            swath.setImage(datatToImage(swath));
            d++;
        }
    }

    private void applyDataChanges(boolean dataImageChanged, List<BathymetrySwath> addList) {
        Graphics2D g2d = (Graphics2D) dataImageTmp.getGraphics();
        // either paint everything again, or just the new data
        for (BathymetrySwath swath: (dataImageChanged ? dataList : addList)) {
            g2d.drawImage(ImageUtils.getScaledImage(swath.asBufferedImage(), dataImage.getWidth(), 1, true), 0,
                    getYPos(swath), null);
        }

        Graphics2D g3d = (Graphics2D) dataImage.getGraphics();
        g3d.drawImage(dataImageTmp, 0, 0, null);
    }

    public void setMaxDepth(double maxDepth) {
        this.maxDepth = maxDepth;
        adaptiveMaxDepth = maxDepth;
    }

    public void useAdaptiveMaxDepth(boolean value) {
        useAdaptiveMaxDepth = value;
    }

    @Override
    public void onViewerPropertiesUpdate() {
        repaintColorBar();
    }

    private void repaintColorBar() {
        if(colorBar != null) {
            colorBar.setCmap(this.colorMap);
            colorBar.revalidate();
            colorBar.repaint();
        }
    }
}
