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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsm
 * 12 Oct 2016
 */
package pt.lsts.neptus.plugins.multibeam.ui;

import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.plugins.interfaces.RealTimeWatefallViewer;
import pt.lsts.neptus.util.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * @author tsm
 *
 */
@SuppressWarnings("serial")
public class MultibeamWaterfallViewer extends RealTimeWatefallViewer<BathymetrySwath> {
    /**
     * @param clazz
     */
    public MultibeamWaterfallViewer() {
        super(MultibeamWaterfallViewer.class);
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
        }
    }


    private BufferedImage datatToImage(BathymetrySwath data) {
        BathymetryPoint[] points = data.getData();
        BufferedImage image = new BufferedImage(points.length, 1, BufferedImage.TYPE_INT_RGB);

        // calculate max depth in order to normalize data
        double max = Double.MIN_VALUE;
        for(int j = 0; j < points.length; j++) {
            if(points[j] != null && points[j].depth > max)
                max = points[j].depth;
        }

        // apply color map
        for(int i = 0; i < points.length; i++)
            if (points[i] != null)
                image.setRGB(i, 0, colorMap.getColor(points[i].depth / max).getRGB());

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
}
