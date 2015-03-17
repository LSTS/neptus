/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * May 19, 2013
 */
package pt.lsts.neptus.mra;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.colormap.DataDiscretizer;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class WorldImage {

    private DataDiscretizer dd;    
    private ColorMap cmap;
    private LocationType sw = null, ne = null, ref = null;
    private int defaultWidth = 1024;
    private int defaultHeight = 768;
    
    private Double minVal = null, maxVal = null; 

    public final void setMinVal(Double minVal) {
        this.minVal = minVal;
    }

    public final void setMaxVal(Double maxVal) {
        this.maxVal = maxVal;
    }

    public WorldImage(int cellWidth, ColorMap cmap) {
        this.cmap = cmap;
        this.dd = new DataDiscretizer(cellWidth);
    }
    
    public void addPoint(LocationType loc, double value) {
        if (ref == null)
            ref = new LocationType(loc);
        
        double[] offsets = loc.getOffsetFrom(ref);
        
        dd.addPoint(offsets[1], -offsets[0], value);
    }
    
    public LocationType getSouthWest() {
        return sw;
    }
    
    public LocationType getNorthEast() {
        return ne;
    }
    
    public BufferedImage processData() {
        double maxX = dd.maxX + 5;
        double maxY = dd.maxY + 5;
        double minX = dd.minX - 5;
        double minY = dd.minY - 5;
 
        //width/height
        double dx = maxX - minX;
        double dy = maxY - minY;

        double ratio1 = (double)defaultWidth/(double)defaultHeight;
        double ratio2 = dx/dy;

        if (ratio2 < ratio1)        
            dx = dy * ratio1;
        else
            dy = dx/ratio1;

        //center
        double cx = (maxX + minX)/2;
        double cy = (maxY + minY)/2;

        Rectangle2D bounds = new Rectangle2D.Double(cx-dx/2, cy-dy/2, dx, dy);

        BufferedImage img = new BufferedImage(defaultWidth,defaultHeight,BufferedImage.TYPE_INT_ARGB);
        
        try {
            double max = maxVal == null ? dd.maxVal[0]*1.005 : maxVal;
            double min = minVal == null ? dd.minVal[0]*0.995 : minVal;
            ColorMapUtils.generateInterpolatedColorMap(bounds, dd.getDataPoints(), 0, img.createGraphics(), img.getWidth(), img.getHeight(), 255, cmap, min, max);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
        
        ne = new LocationType(ref);
        ne.translatePosition(-maxY, maxX, 0);
        
        sw = new LocationType(ref);
        sw.translatePosition(-minY, minX, 0);
        
        return img;
    }
}
