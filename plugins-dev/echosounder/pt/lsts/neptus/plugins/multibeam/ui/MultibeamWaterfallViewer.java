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

import java.awt.image.BufferedImage;
import java.util.ArrayList;

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


    private BufferedImage datatToImage(BathymetrySwath data) {
        BathymetryPoint[] points = data.getData();
        BufferedImage image = new BufferedImage(points.length, 1, BufferedImage.TYPE_INT_RGB);

        double max = Double.MIN_VALUE;
        for(int j = 0; j < points.length; j++) {
            if(points[j] == null && points[j].depth > max)
                max = points[j].depth;
        }

        // apply color map
        for(int i = 0; i < points.length; i++)
            if (points[i] != null)
                image.setRGB(i, 0, colorMap.getColor(1 - points[i].depth / max).getRGB());

        return image;
    }

    @Override
    public void updateImage() {
        if(getDataImage() == null)
            return;

        // new swaths to draw (for the first time)
        ArrayList<BathymetrySwath> queuedSwaths = new ArrayList<>();

        // fetch new swaths to draw
        synchronized (queuedData) {
            queuedSwaths.addAll(queuedData);
            queuedData.clear();
        }

        // draw old + new data
        synchronized(dataList) {
            int allowedNData = getDataImage().getHeight();

            // don't allow stored data to get too big
            // clean undisplayed data
            if(dataList.size() >= 2 * allowedNData) {
                dataList.subList(0, allowedNData).clear();
                System.out.println("** Trimmed data");
            }

            // new data at the beginning of the list
            dataList.addAll(0, queuedSwaths);
            queuedSwaths.clear();

            // draw swaths
            for(int i = 0; i < dataList.size(); i++) {
                BufferedImage swathImg = datatToImage(dataList.get(i));
                getDataImage().getGraphics().drawImage(ImageUtils.getScaledImage(swathImg, dataImage.getWidth(),
                        swathImg.getHeight(), true), 0, i, null);
            }
            queuedSwaths.clear();
        }
    }
}
