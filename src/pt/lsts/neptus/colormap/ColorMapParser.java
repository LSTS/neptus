/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * 24/04/2018
 */
package pt.lsts.neptus.colormap;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

/**
 * @author pdias
 *
 */
class ColorMapParser {
    
     static boolean debug = false;

    /**
     * @see #loadAdobeColorTable(String, InputStream).
     * 
     * @param name
     * @param file
     * @return
     */
    public static InterpolationColorMap loadAdobeColorTable(String name, File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return loadAdobeColorTable(name, fis);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Loads Adobe Color Table files.
     * 
     * ACT files (or special GCT with 2 extra outlier min and max colors.
     * 
     * There is no version number written in the file. The file is 768 or 772 bytes long and contains 256 RGB
     * colors. The first color in the table is index zero. There are three bytes per color in the order red, green,
     * blue. If the file is 772 bytes long there are 4 additional bytes remaining. Two bytes for the number of
     * colors to use. Two bytes for the color index with the transparency color to use. If loaded into the Colors
     * palette, the colors will be installed in the color swatch list as RGB colors.
     * 
     * @param name
     * @param inStream
     * @return
     */
    public static InterpolationColorMap loadAdobeColorTable(String name, InputStream inStream) {
        List<Color> colorsV = new ArrayList<>();
        int counter = 0;
        int numColorToUse = 256;
        Color cOutMin = null;
        Color cOutMax = null;
        int colorsCount = 0;
        try {
            while (inStream.available() > 0) {
                if (counter < 768 && inStream.available() >= 3) {
                    byte[] b3 = new byte[3];
                    inStream.read(b3);
                    int r = b3[0] & 0xFF;
                    int g = b3[1] & 0xFF;
                    int b = b3[2] & 0xFF;
                    Color c = new Color(r, g, b);
                    colorsCount++;
                    if (debug)
                        System.out.println(c + "   " + colorsCount);
                    colorsV.add(c);
                    counter += 3;
                }
                else if (counter >= 768 && counter < 770 && inStream.available() >= 2) {
                    byte[] b2 = new byte[2];
                    inStream.read(b2);
                    int ctu = ((b2[0] & 0xFF) << 8) | (b2[1] & 0xFF);
                    if (ctu != 0)
                        numColorToUse = ctu;
                    if (debug)
                        System.out.println("C>" + numColorToUse);
                    counter += 2;
                }
                else if (counter >= 770 && counter < 772 && inStream.available() >= 2) {
                    byte[] b2 = new byte[2];
                    inStream.read(b2);
                    int ctu = ((b2[0] & 0xFF) << 8) | (b2[1] & 0xFF);
                    if (debug)
                        System.out.println("T>" + ctu); // we don't use this
                    counter += 2;
                }
                else if (counter >= 772 && counter < 778 && inStream.available() >= 6) {
                    // For tc files an extra 2 extra outlier colors
                    byte[] b6 = new byte[6];
                    inStream.read(b6);
                    int r1 = b6[0] & 0xFF;
                    int g1 = b6[1] & 0xFF;
                    int b1 = b6[2] & 0xFF;
                    cOutMin = new Color(r1, g1, b1);
                    int r2 = b6[3] & 0xFF;
                    int g2 = b6[4] & 0xFF;
                    int b2 = b6[5] & 0xFF;
                    cOutMax = new Color(r2, g2, b2);
                    if (debug)
                        System.out.println("minC>" + cOutMin + "   maxC>" + cOutMax); // we don't use this
                    counter += 6;
                }
                else {
                    break;
                }
            }
            
            if (counter < 768) {
                return null;
            }

            if (debug)
                System.out.println(numColorToUse + "  " + colorsV.size());
            if (colorsV.size() > numColorToUse)
                colorsV = colorsV.subList(0, numColorToUse);
            if (debug)
                System.out.println(numColorToUse + "  " + colorsV.size());
            
            if (cOutMin != null && cOutMax != null) {
                colorsV.add(0, cOutMin);
                colorsV.add(cOutMax);
            }
            List<Double> valuesV = new ArrayList<>();
            for (int i = 0; i < numColorToUse; i++)
                valuesV.add((double) i / (double) (numColorToUse - 1));
            if (cOutMin != null && cOutMax != null) {
                valuesV.add(0, 0 - Double.MIN_NORMAL);
                valuesV.add(1 + Double.MIN_NORMAL * 2);
            }
            
            Color[] colors = colorsV.toArray(new Color[colorsV.size()]);
            double[] values = ArrayUtils.toPrimitive(valuesV.toArray(new Double[valuesV.size()]));
            
            return new InterpolationColorMap(name, values, colors);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        loadAdobeColorTable("ACT", new File("conf/colormaps/NEO_amsre_sst.act"));
        loadAdobeColorTable("ACT", new File("conf/colormaps/NEO_wind_spd_anom.act"));
        loadAdobeColorTable("GCT", new File("conf/colormaps/NCDC_temp_anom.gct"));
    }
}
