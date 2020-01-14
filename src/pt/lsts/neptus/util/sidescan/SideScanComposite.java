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
 * Author: José Correia
 * May 22, 2012
 */
package pt.lsts.neptus.util.sidescan;

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

    public enum MODE {
        NONE,
        ADD,
        MAX,
        AVERAGE,
        AGE
    }
    
    private MODE mode = MODE.MAX;
    
    public SideScanComposite() {
    }
    
    public SideScanComposite(MODE mode) {
        this.mode = mode;
    }
    
    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new CompositeContext() {

            @Override
            public void dispose() {

            }

            @Override
            public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                int width = Math.min(src.getWidth(), dstIn.getWidth());
                int height = Math.min(src.getHeight(), dstIn.getHeight());

                int[] srcPixel = new int[4];
                int[] dstPixel = new int[4];
                int[] srcPixels = new int[width];
                int[] dstPixels = new int[width];

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

                        switch (mode) {
                            case ADD:
                                if(srcPixel[0]>dstPixel[0]) {
                                    System.arraycopy(srcPixel, 0, dstPixel, 0, 4); 
                                }
                                dstPixels[x] = (dstPixel[3] << 24) + (dstPixel[0] << 16) + (dstPixel[1] << 8) + (dstPixel[2]);
                                break;
                            case MAX:
                                if (dstPixels[x] == 0)
                                    System.arraycopy(srcPixel, 0, dstPixel, 0, 4);
                                else {
                                    for (int i = 0; i < 3; i++)
                                        dstPixel[i] = Math.max(srcPixel[i], dstPixel[i]);
                                    dstPixel[3] = 255;
                                }
                                dstPixels[x] = (dstPixel[3] << 24) + (dstPixel[0] << 16) + (dstPixel[1] << 8) + (dstPixel[2]);
                                break;
                            case AVERAGE:
                                if (dstPixels[x] == 0)
                                    System.arraycopy(srcPixel, 0, dstPixel, 0, 4);
                                else {
                                    for (int i = 0; i < 3; i++)
                                        dstPixel[i] = Math.max(dstPixel[i], (srcPixel[i] + dstPixel[i]) / 2);
                                    dstPixel[3] = 255;
                                }
                                dstPixels[x] = (dstPixel[3] << 24) + (dstPixel[0] << 16) + (dstPixel[1] << 8) + (dstPixel[2]);
                                break;
                            case AGE:
                                if (srcPixel[3]>dstPixel[3]) {
                                    System.arraycopy(srcPixel, 0, dstPixel, 0, 3);
                                    dstPixel[3] = 255;
                                }                                    
                                dstPixels[x] = (dstPixel[3] << 24) + (dstPixel[0] << 16) + (dstPixel[1] << 8) + (dstPixel[2]);
                                break;
                            default:
                                // NONE
                                break;
                        }                       
                    }
                    dstOut.setDataElements(0, y, width, 1, dstPixels);
                }
            }
        };
    }
}
