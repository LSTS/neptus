/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.colormap;

import java.awt.Color;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.GuiUtils;

public class ColorMapFactory {

    public static Vector<String> colorMapNamesList = new Vector<String>();

    static {
        colorMapNamesList.add("Autumn");
        colorMapNamesList.add("Bone");
        colorMapNamesList.add("Cool");
        colorMapNamesList.add("Copper");
        colorMapNamesList.add("Gray");
        colorMapNamesList.add("Hot");
        colorMapNamesList.add("Jet");
        colorMapNamesList.add("Pink");
        colorMapNamesList.add("Spring");
        colorMapNamesList.add("Summer");
        colorMapNamesList.add("Winter");
        colorMapNamesList.add("AllColors");
        colorMapNamesList.add("RedGreenBlue");
        colorMapNamesList.add("BlueToRed");
        colorMapNamesList.add("White");
        colorMapNamesList.add("Rainbow");
        colorMapNamesList.add("Bronze");
        colorMapNamesList.add("StoreData");

        Collections.sort(colorMapNamesList);
    }

    private ColorMapFactory() {
    }

    public static ColorMap getColorMapByName(String name) {
        if ("autumn".equalsIgnoreCase(name))
            return createAutumnColorMap();
        else if ("bone".equalsIgnoreCase(name))
            return createBoneColorMap();
        else if ("cool".equalsIgnoreCase(name))
            return createCoolColorMap();
        else if ("copper".equalsIgnoreCase(name))
            return createCopperColorMap();
        else if ("gray".equalsIgnoreCase(name))
            return createGrayScaleColorMap();
        else if ("hot".equalsIgnoreCase(name))
            return createHotColorMap();
        else if ("jet".equalsIgnoreCase(name))
            return createJetColorMap();
        else if ("pink".equalsIgnoreCase(name))
            return createPinkColorMap();
        else if ("spring".equalsIgnoreCase(name))
            return createSpringColorMap();
        else if ("summer".equalsIgnoreCase(name))
            return createSummerColorMap();
        else if ("winter".equalsIgnoreCase(name))
            return createWinterColorMap();
        else if ("allColors".equalsIgnoreCase(name))
            return createAllColorsColorMap();
        else if ("redGreenBlue".equalsIgnoreCase(name))
            return createRedGreenBlueColorMap();
        else if ("blueToRed".equalsIgnoreCase(name))
            return createRedGreenBlueColorMap();
        else if ("white".equalsIgnoreCase(name))
            return createWhiteColorMap();
        else if ("greenRadar".equalsIgnoreCase(name))
            return createGreenRadarColorMap();
        else if ("rainbow".equalsIgnoreCase(name))
            return createRainbowColormap();
        else if ("redYellowGreen".equalsIgnoreCase(name))
            return createRedYellowGreenColorMap();
        else if ("bronze".equalsIgnoreCase(name))
            return createBronzeColormap();

        else {
            for (int i = 0; i < ColorMap.cmaps.length; i++) {
                if (ColorMap.cmaps[i].toString().equalsIgnoreCase(name))
                    return ColorMap.cmaps[i];
            }
        }
        return createJetColorMap();
    }

    private static ColorMap storeDataColormap = null;

    public static ColorMap createStoreDataColormap() {

        if (storeDataColormap == null)
            storeDataColormap = new InterpolationColorMap(I18n.text("Store Data Colormap"), new double[] { 0.0,
                    0.333333, 0.666666, 1.0 }, new Color[] { new Color(0, 0, 0), new Color(255, 0, 0),
                    new Color(255, 255, 0), new Color(255, 255, 255) });
        return storeDataColormap;
    }

    private static ColorMap grayscale = null;

    public static ColorMap createGrayScaleColorMap() {

        if (grayscale == null)
            grayscale = new InterpolationColorMap(I18n.textc("Gray scale", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] {
                    new Color(0, 0, 0), new Color(255, 255, 255) });
        /*
         * grayscale = new ColorMap() { public Color getColor(double value) { int rgbVal = (int) (value * 255); rgbVal =
         * Math.min(rgbVal, 255); rgbVal = Math.max(rgbVal, 0); return new Color(rgbVal, rgbVal, rgbVal); }
         * 
         * @Override public String toString() { return "Gray scale"; } };
         */
        return grayscale;
    }

    private static ColorMap white = null;

    public static ColorMap createWhiteColorMap() {
        if (white == null)
            white = new InterpolationColorMap(I18n.textc("All white", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] {
                    Color.white, Color.white });
        return white;
    }

    private static ColorMap hot = null;

    public static ColorMap createHotColorMap() {
        if (hot == null) {
            InputStreamReader isr = new InputStreamReader(
                    ClassLoader.getSystemResourceAsStream("colormaps/hot.colormap"));
            try {
                hot = new TabulatedColorMap(isr);
            }
            catch (Exception e) {
                NeptusLog.pub().debug(e.getMessage());
                hot = new InterpolationColorMap(I18n.textc("Hot", "Colormap name"), new double[] { 0.0, 0.3333333, 0.66666666, 1.0 },
                        new Color[] { Color.BLACK, Color.RED, Color.YELLOW, Color.WHITE });
            }
        }
        return hot;
    }

    private static ColorMap allColors = null;

    public static ColorMap createAllColorsColorMap() {
        if (allColors == null)
            allColors = new InterpolationColorMap(I18n.textc("All Colors", "Colormap name"), new double[] { 0.0, 0.3333333, 0.66666666,
                    1.0 }, new Color[] { Color.BLACK, Color.BLUE, Color.YELLOW, Color.WHITE });
        return allColors;
    }

    private static ColorMap rgb = null;

    public static ColorMap createRedGreenBlueColorMap() {
        if (rgb == null)
            rgb = new InterpolationColorMap(I18n.textc("RGB", "Colormap name"), new double[] { 0.0, 0.5, 1.0 }, new Color[] {
                    new Color(255, 0, 0), new Color(0, 255, 0), new Color(0, 0, 255) });
        return rgb;
    }

    private static ColorMap bluered = null;

    public static ColorMap createBlueToRedColorMap() {
        if (bluered == null)
            bluered = new InterpolationColorMap(I18n.textc("Blue to Red", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] {
                    Color.BLUE, Color.RED });
        return bluered;
    }

    private static ColorMap autumn = null;

    public static ColorMap createAutumnColorMap() {
        if (autumn == null)
            autumn = new InterpolationColorMap(I18n.textc("Autumn", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] { Color.RED,
                    Color.YELLOW });
        return autumn;
    }

    private static ColorMap cool = null;

    public static ColorMap createCoolColorMap() {
        if (cool == null)
            cool = new InterpolationColorMap(I18n.textc("Cool", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] { Color.CYAN,
                    Color.MAGENTA });
        return cool;
    }

    private static ColorMap jet = null;

    public static ColorMap createJetColorMap() {
        if (jet == null) {
            InputStreamReader isr = new InputStreamReader(
                    ClassLoader.getSystemResourceAsStream("colormaps/jet.colormap"));
            try {
                jet = new TabulatedColorMap(isr);
            }
            catch (Exception e) {
                NeptusLog.pub().debug(e.getMessage());
                jet = new InterpolationColorMap(I18n.textc("Jet", "Colormap name"), new double[] { 0.0, 0.25, 0.5, 0.75, 1.0 },
                        new Color[] { Color.blue, Color.cyan, Color.yellow, Color.red, new Color(128, 0, 0) });
            }
        }

        return jet;
    }

    private static ColorMap spring = null;

    public static ColorMap createSpringColorMap() {
        if (spring == null)
            spring = new InterpolationColorMap(I18n.textc("Spring", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] {
                    Color.magenta, Color.yellow });
        return spring;
    }

    private static ColorMap bone = null;

    public static ColorMap createBoneColorMap() {
        if (bone == null)
            bone = new InterpolationColorMap(I18n.textc("Bone", "Colormap name"), new double[] { 0.0, 0.375, 0.75, 1.0 }, new Color[] {
                    new Color(0, 0, 1), new Color(81, 81, 113), new Color(166, 198, 198), Color.white });
        return bone;
    }

    private static ColorMap copper = null;

    public static ColorMap createCopperColorMap() {
        if (copper == null)
            copper = new InterpolationColorMap(I18n.textc("Copper", "Colormap name"), new double[] { 0.0, 0.7869, 0.8125, 1.0 },
                    new Color[] { Color.black, new Color(253, 158, 100), new Color(255, 161, 103),
                            new Color(255, 199, 127) });
        return copper;
    }

    private static ColorMap summer = null;

    public static ColorMap createSummerColorMap() {
        if (summer == null)
            summer = new InterpolationColorMap(I18n.textc("Summer", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] {
                    new Color(0, 128, 102), new Color(255, 255, 102) });
        return summer;
    }

    private static ColorMap winter = null;

    public static ColorMap createWinterColorMap() {
        if (winter == null)
            winter = new InterpolationColorMap(I18n.textc("Winter", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] {
                    Color.blue, new Color(0, 255, 128) });
        return winter;
    }

    private static ColorMap pink = null;

    public static ColorMap createPinkColorMap() {
        if (pink == null)
            pink = new InterpolationColorMap(I18n.textc("Pink", "Colormap name"), new double[] { 0.0, 1 / 64.0, 2 / 64.0, 3 / 64.0,
                    24 / 64.0, 48 / 64.0, 1.0 }, new Color[] { new Color(30, 0, 0), new Color(50, 26, 26),
                    new Color(64, 37, 37), new Color(75, 45, 45), new Color(194, 126, 126), new Color(232, 232, 180),
                    new Color(255, 255, 255) });
        return pink;
    }

    private static ColorMap greenradar = null;

    public static ColorMap createGreenRadarColorMap() {
        if (greenradar == null)
            greenradar = new InterpolationColorMap(I18n.textc("GreenRadar", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] {
                    Color.black, Color.green });
        return greenradar;
    }

    private static ColorMap redYellowGreen = null;

    public static ColorMap createRedYellowGreenColorMap() {
        if (redYellowGreen == null)
            redYellowGreen = new InterpolationColorMap(I18n.textc("RedYellowBlue", "Colormap name"), new double[] { 0.0, 0.5, 1.0 },
                    new Color[] { Color.red, Color.yellow, Color.green });
        return redYellowGreen;
    }

    private static ColorMap rainbow = null;

    public static ColorMap createRainbowColormap() {
        if (rainbow == null)
            rainbow = new InterpolationColorMap(I18n.textc("Rainbow", "Colormap name"), new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7,
                    0.8, 1.0 }, new Color[] { Color.magenta.darker().darker().darker(), Color.magenta, Color.blue,
                    Color.cyan, Color.green, Color.yellow, Color.orange, Color.red,
                    Color.red.darker().darker().darker() });
        return rainbow;
    }

    private static ColorMap bronze = null;

    public static ColorMap createBronzeColormap() {
        if (bronze == null) {
            InputStreamReader isr = new InputStreamReader(
                    ClassLoader.getSystemResourceAsStream("colormaps/bronze.colormap"));
            try {
                bronze = new TabulatedColorMap(isr);
                ((InterpolationColorMap) bronze).setName(I18n.textc("Bronze", "Colormap name"));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bronze;
    }

    private static ColorMap sscanmap = null;

    public static ColorMap createSideScanColorMap() {
        if (sscanmap == null)
            sscanmap = new InterpolationColorMap(I18n.textc("Sidescan", "Colormap name"), new double[] { 0.0, 1.0 }, new Color[] {
                    new Color(0xFFFFFF00, true), new Color(0xFFFFFFFF, true) });
        return sscanmap;
    }

    public static ColorMap createInvertedColorMap(InterpolationColorMap original) {

        double[] inv = new double[original.getValues().length];
        Color[] colors = new Color[inv.length];

        int j = inv.length;
        for (int i = 0; i < inv.length; i++) {
            inv[i] = original.getValues()[i];
            colors[i] = original.getColor(Math.max(0.001, original.getValues()[--j]));

            // NeptusLog.pub().info("<###> "+i+"->"+colors[i].toString());
        }

        // for (int i = 0; i < inv.length; i++)
        // inv[i] = 1-inv[i];

        // for (int i = 0; i < inv.length; i++)
        // colors[i] = original.getColor(1-inv[i]);

        return new InterpolationColorMap(I18n.text("Inverted") + " " + original.toString(), inv, colors);
    }

    public static void main(String[] args) {
        ColorBar bar = new ColorBar(ColorBar.HORIZONTAL_ORIENTATION,
                ColorMapFactory.createInvertedColorMap((InterpolationColorMap) ColorMapFactory.createAutumnColorMap()));
        ColorBar bar2 = new ColorBar(ColorBar.HORIZONTAL_ORIENTATION, ColorMapFactory.createAutumnColorMap());
        GuiUtils.testFrame(bar, bar.getCmap().toString());
        GuiUtils.testFrame(bar2, bar2.getCmap().toString());

    }
}
