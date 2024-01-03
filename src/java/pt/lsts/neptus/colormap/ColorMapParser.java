/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author pdias
 *
 */
class ColorMapParser {
    
     static boolean debug = false;
     
     /**
      * @see #loadRGBColorTable(String, InputStream).      
      * * 
      * @param name
      * @param file
      * @return
      */
     public static InterpolationColorMap loadRGBColorTable(String name, File file) {
         try (FileInputStream fis = new FileInputStream(file)) {
             return loadRGBColorTable(name, fis);
         }
         catch (Exception e) {
             e.printStackTrace();
         }
         return null;
     }

     /**
      * Loads RGB Color Table files.
      * 
      * @param name
      * @param inStream
      * @return
      */
     public static InterpolationColorMap loadRGBColorTable(String name, InputStream inStream) {
         try {
             InputStreamReader reader = new InputStreamReader(inStream);
             return new InterpolationColorMap(name, reader);
         }
         catch (Exception e) {
             e.printStackTrace();
             return null;
         }
     }

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
     * @see #loadCPTColorTable(String, InputStream).
     * 
     * @param name
     * @param file
     * @return
     */
    public static InterpolationColorMap loadCPTColorTable(String name, File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return loadCPTColorTable(name, fis);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Loads CPT Color Table files.
     * 
     * CPT indicates a color palette format used by the Generic Mapping Tools program. The format defines a number of
     * solid color and/or gradient bands between the colorbar extrema rather than a finite number of distinct colors.
     * 
     * <code>
     * <br/>
     * # COLOR_MODEL = RGB <br/>
     * z0   R0    G0    B0    z1 R1a G1a B1a [A] [;label] <br/>
     * z1   R1b   G1b   B1b   z2 R2a G2a B2a [A] [;label] <br/>
     * ... <br/>
     * zn-1 Rn-1b Gn-1b Bn-1b zn Rn  Gn  Bn [A] [;label] <br/> 
     * B Rb Gb Bb <br/>
     * F Rf Gf Bf <br/>
     * N Rn Gn Bn <br/>
     * <br/>
     * </code>
     * 
     * Comments in the color palette file are marked by lines starting with a #. The only comment that isn’t ignored is
     * a comment line that specifies the color model: RGB, CMYK, or HSV. The example above shows the RGB format.
     * 
     * The main color definition section of the file has 8 (RGB, HSV) or 10 (CMYK) columns, plus two optional columns.
     * Each color block is assigned a lower and upper data value (z), and color values to match these bounds. If the
     * lower and upper colors are the same, then that color is assigned to the full range. If the colors are different,
     * then a linear gradient between the two colors results. The optional A flag and semicolon-plus-text column tell
     * GMT how to annotate the color scale. This function ignores those optional columns.
     * 
     * The lines beginning with B, F, and N indicate the colors used to shade background data values (z < z0), foreground
     * data values (z > zn), and NaN data values, respectively. These three colors aren’t used  when creating a
     * colormap, but they can be returned as optional output values.
     * 
     * @param name
     * @param inStream
     * @return
     */
    public static InterpolationColorMap loadCPTColorTable(String name, InputStream inStream) {
        InputStreamReader reader = new InputStreamReader(inStream); 
        BufferedReader br = new BufferedReader(reader);
        String line;
        List<Pair<Double, Color>> colorsPairV = new ArrayList<>();;

        Color cOutMin = null;
        Color cOutMax = null;
        Color cOutNaN = null;

        try {
            while ((line = br.readLine()) != null) {
                if (line.trim().charAt(0) == '#') {
                    String l = line.trim();
                    l = l.replace("#", "").trim();
                    l = l.replace(" ", "").trim();
                    if (l.startsWith("COLOR_MODEL") && !"COLOR_MODEL=RGB".equalsIgnoreCase(l)) {
                        if (debug)
                            System.out.println(name + " is not a valid CPT RGB colormap.");
                        return null;
                    }
                    
                    continue;
                }
                
                line = line.trim();
                if (!line.matches("^[BFN].*")) {
                    String[] parts = line.split("[ \t,]+");
                    if (parts.length < 8)
                        continue;
                    try {
                        double v1 = Double.parseDouble(parts[0]);
                        int r1 = Integer.parseInt(parts[1]);
                        int g1 = Integer.parseInt(parts[2]);
                        int b1 = Integer.parseInt(parts[3]);
                        colorsPairV.add(Pair.of(v1, new Color(r1, g1, b1)));

                        double v2 = Double.parseDouble(parts[4]);
                        int r2 = Integer.parseInt(parts[5]);
                        int g2 = Integer.parseInt(parts[6]);
                        int b2 = Integer.parseInt(parts[7]);
                        colorsPairV.add(Pair.of(v2, new Color(r2, g2, b2)));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    String[] parts = line.split("[ \t,]+");
                    if (parts.length < 4)
                        continue;
                    try {
                        String bfn = parts[0];
                        int r = Integer.parseInt(parts[1]);
                        int g = Integer.parseInt(parts[2]);
                        int b = Integer.parseInt(parts[3]);
                        Color c = new Color(r, g, b);
                        switch (bfn.trim()) {
                            case "B":
                            case "b":
                                cOutMin = c;
                                break;
                            case "F":
                            case "f":
                                cOutMax = c;
                                break;
                            case "N":
                            case "n":
                                cOutNaN = c;
                                break;
                            default:
                                break;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
            
            if (debug)
                System.out.println(colorsPairV.size() + " colors");
            if (debug) {
                System.out.println("B  " + cOutMin);
                System.out.println("F  " + cOutMax);
                System.out.println("N  " + cOutNaN);
            }
            
            double minV = colorsPairV.stream().reduce((r, e) -> r.getLeft() < e.getLeft() ? r : e).get().getLeft();
            double maxV = colorsPairV.stream().reduce((r, e) -> r.getLeft() > e.getLeft() ? r : e).get().getLeft();;
            if (debug)
                System.out.println("minV=" + minV + "  maxV=" + maxV);
            List<Double> valuesV = new ArrayList<>();
            colorsPairV.stream().forEach(e -> {
                double v = (e.getLeft() - minV) / (maxV - minV);
                valuesV.add(v);
            });
            if (cOutMin != null && cOutMax != null) {
                valuesV.add(0, 0 - Double.MIN_NORMAL);
                valuesV.add(1 + Double.MIN_NORMAL * 2);
            }
            
            List<Color> colorsV = new ArrayList<>();
            colorsPairV.forEach(e -> colorsV.add(e.getRight()));
            if (cOutMin != null && cOutMax != null) {
                colorsV.add(0, cOutMin);
                colorsV.add(cOutMax);
            }
            
            Color[] colors = colorsV.toArray(new Color[colorsV.size()]);
            double[] values = ArrayUtils.toPrimitive(valuesV.toArray(new Double[valuesV.size()]));
            
            if (debug)
                System.out.println(ArrayUtils.toString(values));
            
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
        loadRGBColorTable("RGB", new File("conf/colormaps/MPL_ocean.rgb"));
        loadCPTColorTable("CPT", new File("conf/colormaps/GMT_globe.cpt"));
        loadCPTColorTable("CPT", new File("conf/colormaps/GIST_heat.cpt"));
    }
}
