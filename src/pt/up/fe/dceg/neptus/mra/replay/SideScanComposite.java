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
 * May 22, 2012
 * $Id:: SideScanComposite.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * @author jqcorreia
 *
 */
public class SideScanComposite implements Composite {

    /* (non-Javadoc)
     * @see java.awt.Composite#createContext(java.awt.image.ColorModel, java.awt.image.ColorModel, java.awt.RenderingHints)
     */
    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new CompositeContext() {
            
            @Override
            public void dispose() {
                
            }
            
            public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                int width = Math.min(src.getWidth(), dstIn.getWidth());
                int height = Math.min(src.getHeight(), dstIn.getHeight());

                int[] srcPixel = new int[4];
                int[] dstPixel = new int[4];
                int[] srcPixels = new int[width];
                int[] dstPixels = new int[width];

//                System.out.println(src.getWidth() + " " + src.getHeight() + " " + dstIn.getWidth() + " " + dstIn.getHeight());

                for (int y = 0; y < height; y++) {
                    src.getDataElements(0, y, width, 1, srcPixels);
                    dstIn.getDataElements(0, y, width, 1, dstPixels);
                    for (int x = 0; x < width; x++) {
                        // pixels are stored as INT_ARGB
                        // our arrays are [R, G, B, A]
                        int pixel = srcPixels[x];
                        srcPixel[0] = (pixel >> 16) & 0xFF;
                        srcPixel[1] = (pixel >>  8) & 0xFF;
                        srcPixel[2] = (pixel      ) & 0xFF;
                        srcPixel[3] = (pixel >> 24) & 0xFF;

                        pixel = dstPixels[x];
                        dstPixel[0] = (pixel >> 16) & 0xFF;
                        dstPixel[1] = (pixel >>  8) & 0xFF;
                        dstPixel[2] = (pixel      ) & 0xFF;
                        dstPixel[3] = (pixel >> 24) & 0xFF;

                        if(srcPixel[0]>dstPixel[0]) {
                            System.arraycopy(srcPixel, 0, dstPixel, 0, 4); 
                        }
                        dstPixels[x] = (dstPixel[3] << 24) + (dstPixel[0] << 16) + (dstPixel[1] << 8) + (dstPixel[2]); 
//                        // mixes the result with the opacity
//                        dstPixels[x] = ((int) (dstPixel[3] + (result[3] - dstPixel[3]) * alpha) & 0xFF) << 24 |
//                                       ((int) (dstPixel[0] + (result[0] - dstPixel[0]) * alpha) & 0xFF) << 16 |
//                                       ((int) (dstPixel[1] + (result[1] - dstPixel[1]) * alpha) & 0xFF) <<  8 |
//                                        (int) (dstPixel[2] + (result[2] - dstPixel[2]) * alpha) & 0xFF;
                    }
                    dstOut.setDataElements(0, y, width, 1, dstPixels);
                }
            }
        };
    }
    int pixelIntensity(int[] pixel) {
        return (pixel[0]+pixel[1]+pixel[2])/2;
    }
}
