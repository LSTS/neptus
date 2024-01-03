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
 * Author: 
 * 22/09/2016
 */
package pt.lsts.neptus.console.plugins.kml;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.gui.ImageFileChooser;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * Taken from http://www.javaxt.com/ by By Peter Borissow released under MIT Licence
 * "Used to skew an image. Adapted from 2 image processing classes developed by Jerry Huxtable
 * (http://www.jhlabs.com) and released under the Apache License, Version 2.0."
 * 
 * Used to skew an image by updating the corner coordinates. Coordinates are 
 * supplied in clockwise order starting from the upper left corner.
 * 
 * SkewImage skew = new SkewImage(this.bufferedImage);
 * this.bufferedImage = SkewImage.setCorners(x0, y0, // UL
 *                                           x1, y1, // UR
 *                                           x2, y2, // LR
 *                                           x3, y3  // LL);
 *
 */
public class SkewImage {

    public final static int ZERO = 0;
    public final static int CLAMP = 1;
    public final static int WRAP = 2;

    public final static int NEAREST_NEIGHBOUR = 0;
    public final static int BILINEAR = 1;

    protected int edgeAction = ZERO;
    protected int interpolation = BILINEAR;

    protected Rectangle transformedSpace;
    protected Rectangle originalSpace;

    private float x0, y0, x1, y1, x2, y2, x3, y3;
    private float dx1, dy1, dx2, dy2, dx3, dy3;
    private float A, B, C, D, E, F, G, H, I;

    private BufferedImage src;
    private BufferedImage dst;

    public SkewImage(BufferedImage src) {
        this.src = src;
        this.dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB /*src.getType()*/);
    }

    /**
     * Used to skew an image by updating the corner coordinates. Coordinates are 
     * supplied in clockwise order starting from the upper left corner.
     *   
     * Skew skew = new Skew(this.bufferedImage);
     * this.bufferedImage = skew.setCorners(x0, y0, x1, y1, x2, y2, x3, y3);
     * 
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     * @return
     */
    public BufferedImage setCorners(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;

        dx1 = x1 - x2;
        dy1 = y1 - y2;
        dx2 = x3 - x2;
        dy2 = y3 - y2;
        dx3 = x0 - x1 + x2 - x3;
        dy3 = y0 - y1 + y2 - y3;

        float a11, a12, a13, a21, a22, a23, a31, a32;

        if (dx3 == 0 && dy3 == 0) {
            a11 = x1 - x0;
            a21 = x2 - x1;
            a31 = x0;
            a12 = y1 - y0;
            a22 = y2 - y1;
            a32 = y0;
            a13 = a23 = 0;
        }
        else {
            a13 = (dx3 * dy2 - dx2 * dy3) / (dx1 * dy2 - dy1 * dx2);
            a23 = (dx1 * dy3 - dy1 * dx3) / (dx1 * dy2 - dy1 * dx2);
            a11 = x1 - x0 + a13 * x1;
            a21 = x3 - x0 + a23 * x3;
            a31 = x0;
            a12 = y1 - y0 + a13 * y1;
            a22 = y3 - y0 + a23 * y3;
            a32 = y0;
        }

        A = a22 - a32 * a23;
        B = a31 * a23 - a21;
        C = a21 * a32 - a31 * a22;
        D = a32 * a13 - a12;
        E = a11 - a31 * a13;
        F = a31 * a12 - a11 * a32;
        G = a12 * a23 - a22 * a13;
        H = a21 * a13 - a11 * a23;
        I = a11 * a22 - a21 * a12;

        return filter(src, dst);
    }

    protected void transformSpace(Rectangle rect) {
        rect.x = (int) Math.min(Math.min(x0, x1), Math.min(x2, x3));
        rect.y = (int) Math.min(Math.min(y0, y1), Math.min(y2, y3));
        rect.width = (int) Math.max(Math.max(x0, x1), Math.max(x2, x3)) - rect.x;
        rect.height = (int) Math.max(Math.max(y0, y1), Math.max(y2, y3)) - rect.y;
    }

    public float getOriginX() {
        return x0 - (int) Math.min(Math.min(x0, x1), Math.min(x2, x3));
    }

    public float getOriginY() {
        return y0 - (int) Math.min(Math.min(y0, y1), Math.min(y2, y3));
    }

    private BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();
        // int type = src.getType();
        // WritableRaster srcRaster = src.getRaster();

        originalSpace = new Rectangle(0, 0, width, height);
        transformedSpace = new Rectangle(0, 0, width, height);
        transformSpace(transformedSpace);

        if (dst == null) {
            ColorModel dstCM = src.getColorModel();
            dst = new BufferedImage(dstCM,
                    dstCM.createCompatibleWritableRaster(transformedSpace.width, transformedSpace.height),
                    dstCM.isAlphaPremultiplied(), null);
        }
        // WritableRaster dstRaster = dst.getRaster();

        int[] inPixels = getRGB(src, 0, 0, width, height, null);

        if (interpolation == NEAREST_NEIGHBOUR)
            return filterPixelsNN(dst, width, height, inPixels, transformedSpace);

        int srcWidth = width;
        int srcHeight = height;
        int srcWidth1 = width - 1;
        int srcHeight1 = height - 1;
        int outWidth = transformedSpace.width;
        int outHeight = transformedSpace.height;
        int outX, outY;
        // int index = 0;
        int[] outPixels = new int[outWidth];

        outX = transformedSpace.x;
        outY = transformedSpace.y;
        float[] out = new float[2];

        for (int y = 0; y < outHeight; y++) {
            for (int x = 0; x < outWidth; x++) {
                transformInverse(outX + x, outY + y, out);
                int srcX = (int) Math.floor(out[0]);
                int srcY = (int) Math.floor(out[1]);
                float xWeight = out[0] - srcX;
                float yWeight = out[1] - srcY;
                int nw, ne, sw, se;

                if (srcX >= 0 && srcX < srcWidth1 && srcY >= 0 && srcY < srcHeight1) {
                    // Easy case, all corners are in the image
                    int i = srcWidth * srcY + srcX;
                    nw = inPixels[i];
                    ne = inPixels[i + 1];
                    sw = inPixels[i + srcWidth];
                    se = inPixels[i + srcWidth + 1];
                }
                else {
                    // Some of the corners are off the image
                    nw = getPixel(inPixels, srcX, srcY, srcWidth, srcHeight);
                    ne = getPixel(inPixels, srcX + 1, srcY, srcWidth, srcHeight);
                    sw = getPixel(inPixels, srcX, srcY + 1, srcWidth, srcHeight);
                    se = getPixel(inPixels, srcX + 1, srcY + 1, srcWidth, srcHeight);
                }
                outPixels[x] = bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se);
            }
            setRGB(dst, 0, y, transformedSpace.width, 1, outPixels);
        }
        return dst;
    }

    final private int getPixel(int[] pixels, int x, int y, int width, int height) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            switch (edgeAction) {
                case ZERO:
                default:
                    return 0;
                case WRAP:
                    return pixels[(mod(y, height) * width) + mod(x, width)];
                case CLAMP:
                    return pixels[(clamp(y, 0, height - 1) * width) + clamp(x, 0, width - 1)];
            }
        }
        return pixels[y * width + x];
    }

    protected BufferedImage filterPixelsNN(BufferedImage dst, int width, int height, int[] inPixels,
            Rectangle transformedSpace) {
        int srcWidth = width;
        int srcHeight = height;
        int outWidth = transformedSpace.width;
        int outHeight = transformedSpace.height;
        int outX, outY, srcX, srcY;
        int[] outPixels = new int[outWidth];

        outX = transformedSpace.x;
        outY = transformedSpace.y;
        int[] rgb = new int[4];
        float[] out = new float[2];

        for (int y = 0; y < outHeight; y++) {
            for (int x = 0; x < outWidth; x++) {
                transformInverse(outX + x, outY + y, out);
                srcX = (int) out[0];
                srcY = (int) out[1];
                // int casting rounds towards zero, so we check out[0] < 0, not srcX < 0
                if (out[0] < 0 || srcX >= srcWidth || out[1] < 0 || srcY >= srcHeight) {
                    int p;
                    switch (edgeAction) {
                        case ZERO:
                        default:
                            p = 0;
                            break;
                        case WRAP:
                            p = inPixels[(mod(srcY, srcHeight) * srcWidth) + mod(srcX, srcWidth)];
                            break;
                        case CLAMP:
                            p = inPixels[(clamp(srcY, 0, srcHeight - 1) * srcWidth) + clamp(srcX, 0, srcWidth - 1)];
                            break;
                    }
                    outPixels[x] = p;
                }
                else {
                    int i = srcWidth * srcY + srcX;
                    rgb[0] = inPixels[i];
                    outPixels[x] = inPixels[i];
                }
            }
            setRGB(dst, 0, y, transformedSpace.width, 1, outPixels);
        }
        return dst;
    }

    protected void transformInverse(int x, int y, float[] out) {
        out[0] = originalSpace.width * (A * x + B * y + C) / (G * x + H * y + I);
        out[1] = originalSpace.height * (D * x + E * y + F) / (G * x + H * y + I);
    }

    /*
     * public Rectangle2D getBounds2D( BufferedImage src ) { return new Rectangle(0, 0, src.getWidth(),
     * src.getHeight()); }
     * 
     * public Point2D getPoint2D( Point2D srcPt, Point2D dstPt ) { if ( dstPt == null ) dstPt = new Point2D.Double();
     * dstPt.setLocation( srcPt.getX(), srcPt.getY() ); return dstPt; }
     */

    /**
     * A convenience method for getting ARGB pixels from an image. This tries to avoid the performance penalty of
     * BufferedImage.getRGB unmanaging the image.
     */
    public int[] getRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB)
            return (int[]) image.getRaster().getDataElements(x, y, width, height, pixels);
        return image.getRGB(x, y, width, height, pixels, 0, width);
    }

    /**
     * A convenience method for setting ARGB pixels in an image. This tries to avoid the performance penalty of
     * BufferedImage.setRGB unmanaging the image.
     */
    public void setRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB)
            image.getRaster().setDataElements(x, y, width, height, pixels);
        else
            image.setRGB(x, y, width, height, pixels, 0, width);
    }

    /**
     * Clamp a value to an interval.
     * 
     * @param a the lower clamp threshold
     * @param b the upper clamp threshold
     * @param x the input parameter
     * @return the clamped value
     */
    @SuppressWarnings("unused")
    private float clamp(float x, float a, float b) {
        return (x < a) ? a : (x > b) ? b : x;
    }

    /**
     * Clamp a value to an interval.
     * 
     * @param a the lower clamp threshold
     * @param b the upper clamp threshold
     * @param x the input parameter
     * @return the clamped value
     */
    private int clamp(int x, int a, int b) {
        return (x < a) ? a : (x > b) ? b : x;
    }

    /**
     * Return a mod b. This differs from the % operator with respect to negative numbers.
     * 
     * @param a the dividend
     * @param b the divisor
     * @return a mod b
     */
    @SuppressWarnings("unused")
    private double mod(double a, double b) {
        int n = (int) (a / b);

        a -= n * b;
        if (a < 0)
            return a + b;
        return a;
    }

    /**
     * Return a mod b. This differs from the % operator with respect to negative numbers.
     * 
     * @param a the dividend
     * @param b the divisor
     * @return a mod b
     */
    @SuppressWarnings("unused")
    private float mod(float a, float b) {
        int n = (int) (a / b);

        a -= n * b;
        if (a < 0)
            return a + b;
        return a;
    }

    /**
     * Return a mod b. This differs from the % operator with respect to negative numbers.
     * 
     * @param a the dividend
     * @param b the divisor
     * @return a mod b
     */
    private int mod(int a, int b) {
        int n = a / b;

        a -= n * b;
        if (a < 0)
            return a + b;
        return a;
    }

    /**
     * Bilinear interpolation of ARGB values.
     * 
     * @param x the X interpolation parameter 0..1
     * @param y the y interpolation parameter 0..1
     * @param rgb array of four ARGB values in the order NW, NE, SW, SE
     * @return the interpolated value
     */
    private int bilinearInterpolate(float x, float y, int nw, int ne, int sw, int se) {
        float m0, m1;
        int a0 = (nw >> 24) & 0xff;
        int r0 = (nw >> 16) & 0xff;
        int g0 = (nw >> 8) & 0xff;
        int b0 = nw & 0xff;
        int a1 = (ne >> 24) & 0xff;
        int r1 = (ne >> 16) & 0xff;
        int g1 = (ne >> 8) & 0xff;
        int b1 = ne & 0xff;
        int a2 = (sw >> 24) & 0xff;
        int r2 = (sw >> 16) & 0xff;
        int g2 = (sw >> 8) & 0xff;
        int b2 = sw & 0xff;
        int a3 = (se >> 24) & 0xff;
        int r3 = (se >> 16) & 0xff;
        int g3 = (se >> 8) & 0xff;
        int b3 = se & 0xff;

        float cx = 1.0f - x;
        float cy = 1.0f - y;

        m0 = cx * a0 + x * a1;
        m1 = cx * a2 + x * a3;
        int a = (int) (cy * m0 + y * m1);

        m0 = cx * r0 + x * r1;
        m1 = cx * r2 + x * r3;
        int r = (int) (cy * m0 + y * m1);

        m0 = cx * g0 + x * g1;
        m1 = cx * g2 + x * g3;
        int g = (int) (cy * m0 + y * m1);

        m0 = cx * b0 + x * b1;
        m1 = cx * b2 + x * b3;
        int b = (int) (cy * m0 + y * m1);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void main(String[] args) throws Exception {
        File f = ImageFileChooser.showOpenImageDialog();
        if (f != null) {
            BufferedImage origImage = ImageIO.read(f);
            BufferedImage skewImage = ImageIO.read(f);
            if (origImage != null) {
                JLabel imgOrig = new JLabel();
                imgOrig.setOpaque(false);
                imgOrig.setPreferredSize(new Dimension(220,220));
                imgOrig.setHorizontalAlignment(JLabel.CENTER);
                imgOrig.setVerticalAlignment(JLabel.CENTER);
                imgOrig.setIcon(new ImageIcon(ImageUtils.getScaledImage(origImage, 200,200,false)));
                
                SkewImage skew = new SkewImage(skewImage);
                
                int w = skewImage.getWidth();
                int h = skewImage.getHeight();
                skewImage = skew.setCorners(
                        20, 70,         //UL
                        w - 70, 0,      //UR
                        w + 20, h - 50, //LR
                        50, h);         //LL);

                JLabel imgSkew = new JLabel();
                imgSkew.setOpaque(false);
                imgSkew.setPreferredSize(new Dimension(220,220));
                imgSkew.setHorizontalAlignment(JLabel.CENTER);
                imgSkew.setVerticalAlignment(JLabel.CENTER);
                imgSkew.setIcon(new ImageIcon(ImageUtils.getScaledImage(skewImage, 200,200,false)));

                JPanel holderPanel = new JPanel(new MigLayout());
                holderPanel.add(imgOrig, "");
                holderPanel.add(imgSkew, "");
                
                GuiUtils.testFrame(holderPanel, "Skew Test", 500, 250);
            }
        }
    }
}
