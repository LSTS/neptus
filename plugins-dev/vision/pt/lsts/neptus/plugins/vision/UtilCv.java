/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Pedro Gonçalves
 * Nov 19, 2015
 */
package pt.lsts.neptus.plugins.vision;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/** 
 * @author pedrog
 * @version 1.0
 * @category OpenCV-Vision
 *
 */
public class UtilCv {
    
    static Mat matGray;
    static Mat matGrayTemp;
    static Mat matColor;
    static List<Mat> lRgb = new ArrayList<Mat>(3);;
    
    private UtilCv() {
    }
    
    /**  
     * Converts/writes a Mat into a BufferedImage.  
     * @param matrix Mat of type CV_8UC3 or CV_8UC1  
     * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY  
     */  
    public static BufferedImage matToBufferedImage(Mat matrix) {
        int cols = matrix.cols();  
        int rows = matrix.rows();  
        int elemSize = (int)matrix.elemSize();  
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
                for(int i=0; i<data.length; i=i+3) {  
                    b = data[i];  
                    data[i] = data[i+2];  
                    data[i+2] = b;  
                }  
                break;  
        default:  
            return null;  
        }
        BufferedImage image2 = new BufferedImage(cols, rows, type);  
        image2.getRaster().setDataElements(0, 0, cols, rows, data);  
        return image2;
    }
    
    //!Convert bufferedImage to Mat
    public static Mat bufferedImageToMat(BufferedImage in) {
        Mat out;

        if(in.getType() == BufferedImage.TYPE_INT_RGB || in.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
            byte[] pixels = ((DataBufferByte) in.getRaster().getDataBuffer()).getData();
            out.put(0, 0, pixels);
        }
        else {
            out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC1);
            byte[] pixels = ((DataBufferByte) in.getRaster().getDataBuffer()).getData();
            out.put(0, 0, pixels);
        }
        return out;
     }
    
    //!Resize Buffered Image
    public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }
    
    //Hist
    public static BufferedImage histogramCv(BufferedImage original)
    {   
        if(original.getType() == BufferedImage.TYPE_INT_RGB || original.getType() == BufferedImage.TYPE_3BYTE_BGR)
        {            
            matColor = new Mat(original.getHeight(), original.getWidth(), CvType.CV_8UC3);
            Core.split(bufferedImageToMat(original), lRgb);
            Mat mR = lRgb.get(0);
            Imgproc.equalizeHist(mR, mR);
            lRgb.set(0, mR);
            Mat mG = lRgb.get(1);
            Imgproc.equalizeHist(mG, mG);
            lRgb.set(1, mG);
            Mat mB = lRgb.get(2);
            Imgproc.equalizeHist(mB, mB);
            lRgb.set(2, mB);
            Core.merge(lRgb, matColor);
            original = matToBufferedImage(matColor);
            matColor.release();
            mR.release();
            mG.release();
            mB.release();
        }
        else
        {
            matGrayTemp = new Mat(original.getHeight(), original.getWidth(), CvType.CV_8UC1);
            matGray = new Mat(original.getHeight(), original.getWidth(), CvType.CV_8UC3);
            Imgproc.cvtColor(bufferedImageToMat(original), matGrayTemp, Imgproc.COLOR_RGB2GRAY);
            Imgproc.equalizeHist(matGrayTemp, matGrayTemp);
            Imgproc.cvtColor(matGrayTemp, matGray, Imgproc.COLOR_GRAY2RGB);
            original = matToBufferedImage(matGray);
            matGrayTemp.release();
            matGray.release();
        }
        return original;
    }
}
