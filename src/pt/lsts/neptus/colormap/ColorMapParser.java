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

/**
 * @author pdias
 *
 */
class ColorMapParser {

    public static InterpolationColorMap loadAdobeColorTable(String name, File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return loadAdobeColorTable(name, fis);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InterpolationColorMap loadAdobeColorTable(String name, InputStream inStream) {
        // ACT files
        // There is no version number written in the file. The file is 768 or 772 bytes long and contains 256 RGB
        // colors. The first color in the table is index zero. There are three bytes per color in the order red, green,
        // blue. If the file is 772 bytes long there are 4 additional bytes remaining. Two bytes for the number of
        // colors to use. Two bytes for the color index with the transparency color to use. If loaded into the Colors
        // palette, the colors will be installed in the color swatch list as RGB colors.
        List<Color> colorsV = new ArrayList<>();
        int counter = 0;
        int colorsCount = 0;
        int numColorToUse = 256;
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
                    System.out.println("C>" + numColorToUse);
                    counter += 2;
                }
                else if (counter >= 770 && inStream.available() >= 2) {
                    byte[] b2 = new byte[2];
                    inStream.read(b2);
                    int ctu = ((b2[0] & 0xFF) << 8) | (b2[1] & 0xFF);
                    System.out.println("T>" + ctu); // we don't use this
                    counter += 2;
                }
            }

            System.out.println(numColorToUse + "  " + colorsV.size());
            if (colorsV.size() > numColorToUse)
                colorsV = colorsV.subList(0, numColorToUse);
            System.out.println(numColorToUse + "  " + colorsV.size());
            
            Color[] colors = colorsV.toArray(new Color[0]);        
            double[] values = new double[colorsV.size()];
            for (int i = 0; i < values.length; i++)
                values[i] = (double) i / (double) (values.length - 1);
            
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
    }
}
