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
 * Author: Pedro Gonçalves
 * Nov 19, 2015
 */
package pt.lsts.neptus.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;

/**
 * @author pedrog
 * @version 1.0
 * @category OpenCV-Vision
 *
 */
public class UtilCv {

    private UtilCv() {
    }

    /**  Convert a Mat image to bufferedImage */
    public static BufferedImage matToBufferedImage(Mat matrix) {
        int cols = matrix.cols();
        int rows = matrix.rows();
        int elemSize = (int) matrix.elemSize();
        byte[] data = new byte[cols * rows * elemSize];
        int type;
        matrix.get(0, 0, data);
        switch (matrix.channels()) {
            case 1:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;
                // bgr to rgb
                byte b;
                for (int i = 0; i < data.length; i = i + 3) {
                    b = data[i];
                    data[i] = data[i + 2];
                    data[i + 2] = b;
                }
                break;
            default:
                return null;
        }
        BufferedImage image2 = new BufferedImage(cols, rows, type);
        image2.getRaster().setDataElements(0, 0, cols, rows, data);
        return image2;
    }

    /**  Convert bufferedImage to Mat */
    public static Mat bufferedImageToMat(BufferedImage in) {
        Mat out;

        if (in.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
            byte[] pixels = ((DataBufferByte) in.getRaster().getDataBuffer()).getData();
            out.put(0, 0, pixels);
        }
        else if (in.getType() == BufferedImage.TYPE_INT_RGB) {
            out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
            byte[] data = new byte[in.getWidth() * in.getHeight() * (int) out.elemSize()];
            int[] dataBuff = in.getRGB(0, 0, in.getWidth(), in.getHeight(), null, 0, in.getWidth());
            for (int i = 0; i < dataBuff.length; i++) {
                data[i * 3] = (byte) ((dataBuff[i] >> 16) & 0xFF);
                data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
                data[i * 3 + 2] = (byte) ((dataBuff[i] >> 0) & 0xFF);
            }
            out.put(0, 0, data);
        }
        else {
            out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC1);
            byte[] pixels = ((DataBufferByte) in.getRaster().getDataBuffer()).getData();
            out.put(0, 0, pixels);
        }
        return out;
    }

    /**  Resize Buffered Image */
    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    /**  Add text to Buffered Image */
    public static BufferedImage addText(BufferedImage old, String text, Color textColor, int posX, int posY) {
        BufferedImage img = new BufferedImage(old.getWidth(), old.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1d = img.createGraphics();
        g1d.drawImage(old, 0, 0, null);
        g1d.setPaint(new Color(Color.DARK_GRAY.getRed(), Color.DARK_GRAY.getGreen(),
                Color.DARK_GRAY.getBlue(), textColor.getAlpha()));
        g1d.setFont(new Font("Serif", Font.BOLD, 10));
        FontMetrics fm = g1d.getFontMetrics();
        g1d.drawString(text, posX - fm.stringWidth(text) - 10, posY);

        g1d.setPaint(textColor);
        g1d.setFont(new Font("Serif", Font.BOLD, 10));
        fm = g1d.getFontMetrics();
        g1d.drawString(text, posX - fm.stringWidth(text) - 12, posY);
        g1d.dispose();
        return img;
    }

    /**  Histogram equalizer */
    public static BufferedImage histogramCv(BufferedImage original) {
        Mat matGray = null;
        Mat matColor = null;
        Mat mR = null;
        Mat mG = null;
        Mat mB = null;
        if (original.getWidth() > 0 && original.getHeight() > 0) {
            BufferedImage tmp = original;
            try {
                if (original.getType() == BufferedImage.TYPE_INT_RGB
                        || original.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                    matColor = new Mat(original.getHeight(), original.getWidth(), CvType.CV_8UC3);
                    List<Mat> lRgb = new ArrayList<Mat>(3);
                    Core.split(bufferedImageToMat(original), lRgb);
                    mR = lRgb.get(0);
                    Imgproc.equalizeHist(mR, mR);
                    lRgb.set(0, mR);
                    mG = lRgb.get(1);
                    Imgproc.equalizeHist(mG, mG);
                    lRgb.set(1, mG);
                    mB = lRgb.get(2);
                    Imgproc.equalizeHist(mB, mB);
                    lRgb.set(2, mB);
                    Core.merge(lRgb, matColor);
                    original = matToBufferedImage(matColor);
                    matColor.release();
                    mR.release();
                    mG.release();
                    mB.release();
                }
                else if (original.getType() == BufferedImage.TYPE_BYTE_GRAY) {
                    matGray = new Mat(original.getHeight(), original.getWidth(), CvType.CV_8UC1);
                    Imgproc.equalizeHist(matGray, matGray);
                    original = matToBufferedImage(matGray);
                    matGray.release();
                }
            }
            catch (Exception e) {
                NeptusLog.pub().warn(I18n.text("Histogram equalizer ERROR: ") + e.getMessage());
                if (matColor != null)
                    matColor.release();
                if (mR != null)
                    mR.release();
                if (mG != null)
                    mG.release();
                if (mB != null)
                    mB.release();
                if (matGray != null)
                    matGray.release();

                return tmp;
            }
        }
        else
            NeptusLog.pub().warn(I18n.text("Histogram equalizer ERROR"));

        return original;
    }

    /**  Save a snapshot to disk */
    public static void saveSnapshot(BufferedImage image, String snapshotdir) {
        Date date = new Date();
        String dateFolder = String.format("%tT", date);
        String imageJpeg = String.format("%s/%s.png", snapshotdir, dateFolder.replace(":", "-"));
        File outputfile = new File(imageJpeg);
        try {
            File pDir = outputfile.getParentFile();
            if (!pDir.exists())
                pDir.mkdirs();
            ImageIO.write(image, "png", outputfile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
