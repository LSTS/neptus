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
 * Author: Paulo Dias
 * 2010/05/19
 */

package pt.lsts.neptus.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author pdias
 *
 */
public class ColorUtils {

    public final static Color STRIPES_YELLOW = new Color(255, 230, 63);
            
    private static final float U_OFF = .436f;
    private static final float V_OFF = .615f;
    private static final long RAND_SEED = 0;
    private static Random rand = new Random(RAND_SEED); 
    
    public static final Color setTransparencyToColor(Color c, int transparency) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), transparency);
    }

    public static final Color invertColor(Color c) {
        return invertColor(c, 255);
    }

    public static final Color invertColor(Color c, int transparency) {
        return new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue(), transparency);
    }

    /**
     * Returns an array of ncolors RGB triplets such that each is as unique from the rest as possible and each color has
     * at least one component greater than minComponent and one less than maxComponent. Use min == 1 and max == 0 to
     * include the full RGB color range.
     * 
     * Warning: O N^2 algorithm blows up fast for more than 100 colors.
     */
    public static Color[] generateVisuallyDistinctColors(int ncolors, float minComponent, float maxComponent) {
        rand.setSeed(RAND_SEED); // So that we get consistent results for each combination of inputs

        float[][] yuv = new float[ncolors][3];

        // initialize array with random colors
        for (int got = 0; got < ncolors;) {
            System.arraycopy(randYUVinRGBRange(minComponent, maxComponent), 0, yuv[got++], 0, 3);
        }
        // continually break up the worst-fit color pair until we get tired of searching
        for (int c = 0; c < ncolors * 1000; c++) {
            float worst = 8888;
            int worstID = 0;
            for (int i = 1; i < yuv.length; i++) {
                for (int j = 0; j < i; j++) {
                    float dist = sqrdist(yuv[i], yuv[j]);
                    if (dist < worst) {
                        worst = dist;
                        worstID = i;
                    }
                }
            }
            float[] best = randYUVBetterThan(worst, minComponent, maxComponent, yuv);
            if (best == null)
                break;
            else
                yuv[worstID] = best;
        }

        Color[] rgbs = new Color[yuv.length];
        for (int i = 0; i < yuv.length; i++) {
            float[] rgb = new float[3];
            yuv2rgb(yuv[i][0], yuv[i][1], yuv[i][2], rgb);
            rgbs[i] = new Color(rgb[0], rgb[1], rgb[2]);
            // System.out.println(rgb[i][0] + "\t" + rgb[i][1] + "\t" + rgb[i][2]);
        }

        return rgbs;
    }

    public static void hsv2rgb(float h, float s, float v, float[] rgb) {
        // H is given on [0->6] or -1. S and V are given on [0->1].
        // RGB are each returned on [0->1].
        float m, n, f;
        int i;

        float[] hsv = new float[3];

        hsv[0] = h;
        hsv[1] = s;
        hsv[2] = v;
        System.out.println("H: " + h + " S: " + s + " V:" + v);
        if (hsv[0] == -1) {
            rgb[0] = rgb[1] = rgb[2] = hsv[2];
            return;
        }
        i = (int) (Math.floor(hsv[0]));
        f = hsv[0] - i;
        if (i % 2 == 0)
            f = 1 - f; // if i is even
        m = hsv[2] * (1 - hsv[1]);
        n = hsv[2] * (1 - hsv[1] * f);
        switch (i) {
            case 6:
            case 0:
                rgb[0] = hsv[2];
                rgb[1] = n;
                rgb[2] = m;
                break;
            case 1:
                rgb[0] = n;
                rgb[1] = hsv[2];
                rgb[2] = m;
                break;
            case 2:
                rgb[0] = m;
                rgb[1] = hsv[2];
                rgb[2] = n;
                break;
            case 3:
                rgb[0] = m;
                rgb[1] = n;
                rgb[2] = hsv[2];
                break;
            case 4:
                rgb[0] = n;
                rgb[1] = m;
                rgb[2] = hsv[2];
                break;
            case 5:
                rgb[0] = hsv[2];
                rgb[1] = m;
                rgb[2] = n;
                break;
        }
    }

    // From http://en.wikipedia.org/wiki/YUV#Mathematical_derivations_and_formulas
    public static void yuv2rgb(float y, float u, float v, float[] rgb) {
        rgb[0] = 1 * y + 0 * u + 1.13983f * v;
        rgb[1] = 1 * y + -.39465f * u + -.58060f * v;
        rgb[2] = 1 * y + 2.03211f * u + 0 * v;
    }

    public static void rgb2yuv(float r, float g, float b, float[] yuv) {
        yuv[0] = .299f * r + .587f * g + .114f * b;
        yuv[1] = -.14713f * r + -.28886f * g + .436f * b;
        yuv[2] = .615f * r + -.51499f * g + -.10001f * b;
    }

    private static float[] randYUVinRGBRange(float minComponent, float maxComponent) {
        while (true) {
            float y = rand.nextFloat(); // * YFRAC + 1-YFRAC);
            float u = rand.nextFloat() * 2 * U_OFF - U_OFF;
            float v = rand.nextFloat() * 2 * V_OFF - V_OFF;
            float[] rgb = new float[3];
            yuv2rgb(y, u, v, rgb);
            float r = rgb[0], g = rgb[1], b = rgb[2];
            if (0 <= r && r <= 1 && 0 <= g && g <= 1 && 0 <= b && b <= 1
                    && (r > minComponent || g > minComponent || b > minComponent) && // don't want all dark components
                    (r < maxComponent || g < maxComponent || b < maxComponent)) // don't want all light components

                return new float[] { y, u, v };
        }
    }

    private static float sqrdist(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    private static float[] randYUVBetterThan(float bestDistSqrd, float minComponent, float maxComponent, float[][] in) {
        for (int attempt = 1; attempt < 100 * in.length; attempt++) {
            float[] candidate = randYUVinRGBRange(minComponent, maxComponent);
            boolean good = true;
            for (int i = 0; i < in.length; i++)
                if (sqrdist(candidate, in[i]) < bestDistSqrd)
                    good = false;
            if (good)
                return candidate;
        }
        return null; // after a bunch of passes, couldn't find a candidate that beat the best.
    }

    /**
     * To create a stripe paint with the color primeColor and black.
     * 
     * @param primeColor The color of the one stripe, the other is black.
     * @return
     */
    public static Paint createStripesPaint(Color primeColor) {
        return createStripesPaintWorker(new Dimension(60, 60), primeColor, Color.BLACK, false);
    }

    public static Paint createStripesPaint(Color primeColor, Color secondColor) {
        return createStripesPaintWorker(new Dimension(60, 60), primeColor, secondColor, false);
    }

    /**
     * To create a stripe paint with the color primeColor and black.
     * 
     * @param dim The size of the image to create the pattern. 
     * @param primeColor The color of the one stripe, the other is black.
     * @return
     */
    public static Paint createStripesPaint(Dimension dim, Color primeColor) {
        return createStripesPaintWorker(dim, primeColor, Color.BLACK, false);
    }

    /**
     * To create a stripe paint with the color primeColor and black, as disabled (grey colors).
     * 
     * @param primeColor The color of the one stripe, the other is black.
     * @return
     */
    public static Paint createStripesPaintDisabled(Color primeColor) {
        return createStripesPaintWorker(new Dimension(60, 60), primeColor, Color.BLACK, true);
    }

    public static Paint createStripesPaintDisabled(Color primeColor, Color secondColor) {
        return createStripesPaintWorker(new Dimension(60, 60), primeColor, secondColor, true);
    }

    /**
     * To create a stripe paint with the color primeColor and black, as disabled (grey colors).
     * 
     * @param dim The size of the image to create the pattern. 
     * @param primeColor The color of the one stripe, the other is black.
     * @return
     */
    public static Paint createStripesPaintDisabled(Dimension dim, Color primeColor) {
        return createStripesPaintWorker(dim, primeColor, Color.BLACK, true);
    }

    /**
     * To create a stripe paint with the color primeColor and black.
     * 
     * @param dim The size of the image to create the pattern. 
     * @param primeColor The color of the one stripe.
     * @param secondColor The color of the other stripe.
     * @param isDisabled For the paint to be grey scaled.
     * @return
     */
    private static Paint createStripesPaintWorker(Dimension dim, Color primeColor, Color secondColor,
            boolean isDisabled) {
        double mS = Math.min(dim.width, dim.height);
        mS = (mS == 0) ? 80 : mS;
        int refSize = 80, refTexSize = 25, refStrokeSize = 10;
        int size = (int) (mS * refTexSize / refSize);
        int stroke = (int) (mS * refStrokeSize / refSize);

        BufferedImage buffImg;
        if (!isDisabled) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            buffImg = gc.createCompatibleImage(size, size, Transparency.BITMASK); 
        }
        else {
            buffImg = new BufferedImage(size, size, BufferedImage.TYPE_USHORT_GRAY);
        }

        Graphics2D gbi = buffImg.createGraphics();
        gbi.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        gbi.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        gbi.setColor(primeColor);
        gbi.fillRect(0, 0, size, size);
        gbi.setColor(secondColor);
        BasicStroke s = new BasicStroke(stroke);
        gbi.setStroke(s);
        gbi.drawLine(0, 0, size, size);
        gbi.drawLine(-size, 0, size, size*2);
        gbi.drawLine(0, -size, size*2, size);
        
        Rectangle r = new Rectangle(0, 0, size, size);
        TexturePaint paint = new TexturePaint(buffImg, r);
        
        return paint;
    }

    public static void main(String[] args) {
        
        System.out.println(getHtmlColor(Color.red));
        System.out.println(getHtmlColor(Color.cyan.darker()));
        JPanel t1 = new JPanel();
        // t1.setLayout(new BoxLayout(t1, BoxLayout.PAGE_AXIS));

        Color [] colors = generateVisuallyDistinctColors(100, 0.5f, 0.5f);
        
        for (int i = 0; i < colors.length; i++) {
            JLabel lbl = new JLabel("" + i);
            lbl.setBackground(colors[i]);            
            lbl.setOpaque(true);
            t1.add(lbl);
        }

        GuiUtils.testFrame(t1);
    }
    
    public static String getHtmlColor(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
