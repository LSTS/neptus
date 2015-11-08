/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Dec 9, 2012
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
public class PhotoToolbar extends JPanel {

    private static final long serialVersionUID = 1L;
    protected MraPhotosVisualization display;
    protected JToggleButton grayToggle, sharpenToggle, wbalanceToggle,
    contrastToggle, brightToggle, legendToggle, histGrayFilter, histColorFilter;

    protected JButton nextButton, prevButton;
    protected File[] allFiles;
    protected double startTime, endTime;
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    public PhotoToolbar(MraPhotosVisualization display) {
        this.display = display;
        allFiles = MraPhotosVisualization.listPhotos(display.getPhotosDir()); 
        startTime = display.timestampOf(allFiles[0]);
        endTime = display.timestampOf(allFiles[allFiles.length - 1]);
        initialize();
    }

    protected synchronized void setTime(double time) {

        for (int i = 0; i < allFiles.length; i++) {
            if (display.timestampOf(allFiles[i]) >= time) {
                display.setCurFile(allFiles[i]);
                return;
            }
        }
    }

    protected void initialize() {

        nextButton = new JButton(">");
        nextButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                next();
            }
        });
        nextButton.setToolTipText(I18n.text("Next photo"));

        prevButton = new JButton("<");
        prevButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                prev();
            }
        });
        prevButton.setToolTipText(I18n.text("Previous photo"));

        grayToggle = new JToggleButton("G");
        grayToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                display.grayscale = grayToggle.isSelected();
                display.setCurFile(display.getCurFile());
            }
        });
        grayToggle.setToolTipText(I18n.text("Toggle grayscale filter"));

        add(grayToggle);

        brightToggle = new JToggleButton("B");
        brightToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (brightToggle.isSelected())
                    display.brighten = true;
                else
                    display.brighten = false;

                display.setCurFile(display.getCurFile());
            }
        });
        brightToggle.setToolTipText(I18n.text("Toggle brightness filter"));

        add(brightToggle);

        sharpenToggle = new JToggleButton("S");
        sharpenToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                display.sharpen = sharpenToggle.isSelected();
                display.setCurFile(display.getCurFile());
            }
        });
        sharpenToggle.setToolTipText(I18n.text("Toggle sharpen filter"));

        add(sharpenToggle);

        contrastToggle = new JToggleButton("C");
        contrastToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                display.contrast = contrastToggle.isSelected();
                display.setCurFile(display.getCurFile());
            }
        });
        contrastToggle.setToolTipText(I18n.text("Toggle contrast enhancement"));

        add(contrastToggle);

        wbalanceToggle = new JToggleButton("W");
        wbalanceToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!wbalanceToggle.isSelected()) {
                    display.whiteBalanceOp = null;
                }
                else {
                    Color c = JColorChooser.showDialog(display, "Select neutral white",
                            brightestPixel((BufferedImage) display.imageToDisplay));
                    if (c != null)
                        display.whiteBalanceOp = ImageUtils.whiteBalanceOp(c.getRed(), c.getGreen(), c.getBlue());
                    else
                        wbalanceToggle.setSelected(false);
                }
                display.setCurFile(display.getCurFile());
            }
        });
        wbalanceToggle.setToolTipText(I18n.text("Toggle white balance filter"));

        add(wbalanceToggle);

        legendToggle = new JToggleButton("L");
        legendToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                display.showLegend = legendToggle.isSelected();
                display.setCurFile(display.getCurFile());
            }
        });
        legendToggle.setSelected(true);
        legendToggle.setToolTipText(I18n.text("Show legend"));
        add(legendToggle);
        
        //!Find OPENCV JNI
        boolean has_ocv = false;
        String libOpencv = new String();
        File dir = new File("/usr/lib/jni");
        String[] children = dir.list();
        if (children == null) {
            NeptusLog.pub().error("/usr/lib/jni not exist to search Opencv jni");
        }
        else {
           for (int i = 0; i < children.length; i++) {
              String filename = children[i];
              if(filename.equalsIgnoreCase("libopencv_java240.so"))
                  libOpencv = "opencv_java240";
              else if(filename.equalsIgnoreCase("libopencv_java241.so"))
                  libOpencv = "opencv_java241";
              else if(filename.equalsIgnoreCase("libopencv_java242.so"))
                  libOpencv = "opencv_java242";
              else if(filename.equalsIgnoreCase("libopencv_java243.so"))
                  libOpencv = "opencv_java243";
              else if(filename.equalsIgnoreCase("libopencv_java244.so"))
                  libOpencv = "opencv_java244";
              else if(filename.equalsIgnoreCase("libopencv_java245.so"))
                  libOpencv = "opencv_java245";
              else if(filename.equalsIgnoreCase("libopencv_java246.so"))
                  libOpencv = "opencv_java246";
              else if(filename.equalsIgnoreCase("libopencv_java247.so"))
                  libOpencv = "opencv_java247";
              else if(filename.equalsIgnoreCase("libopencv_java248.so"))
                  libOpencv = "opencv_java248";
              else if(filename.equalsIgnoreCase("libopencv_java249.so"))
                  libOpencv = "opencv_java249";
              else if(filename.equalsIgnoreCase("libopencv_java2410.so"))
                  libOpencv = "opencv_java2410";
              else if(filename.equalsIgnoreCase("libopencv_java2411.so"))
                  libOpencv = "opencv_java2411";
              else if(filename.equalsIgnoreCase("libopencv_java2412.so"))
                  libOpencv = "opencv_java2412";
           }
        }
        
        try {
            System.loadLibrary(libOpencv);
            has_ocv = true;
        }
        catch (Exception e) {
            try {
                System.loadLibrary("opencv_java2411");
                System.loadLibrary("libopencv_core2411");
                System.loadLibrary("libopencv_highgui2411");
                try {
                    System.loadLibrary("opencv_ffmpeg2411_64");
                    }
                catch (Exception e1) {
                    System.loadLibrary("opencv_ffmpeg2411");
                }
                catch (Error e1) {
                    System.loadLibrary("opencv_ffmpeg2411");
                }
                has_ocv = true;
            }
            catch (Exception e1) {
                NeptusLog.pub().error("Opencv not found - please install libopencv2.4-jni and dependencies");
            }
        }
        catch (Error e) {
            try {
                System.loadLibrary("opencv_java2411");
                System.loadLibrary("libopencv_core2411");
                System.loadLibrary("libopencv_highgui2411");
                try {
                    System.loadLibrary("opencv_ffmpeg2411_64");
                    }
                catch (Exception e1) {
                    System.loadLibrary("opencv_ffmpeg2411");
                }
                catch (Error e1) {
                    System.loadLibrary("opencv_ffmpeg2411");
                }
                has_ocv = true;
            }
            catch (Error e1) {
                NeptusLog.pub().error("Opencv not found - please install libopencv2.4-jni and dependencies");
            }
        }
        
        histGrayFilter = new JToggleButton("H/G");
        histGrayFilter.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (histGrayFilter.isSelected())
                    display.grayHist = true;
                else
                    display.grayHist = false;

                display.setCurFile(display.getCurFile());
            }
        });
        if (has_ocv)
            histGrayFilter.setToolTipText(I18n.text("Histogram Equalization Gray Filter"));
        else {
            histGrayFilter.setEnabled(false);
            histGrayFilter.setToolTipText(I18n.text("OpenCV was not detected."));
        }
        
        add(histGrayFilter);
        
        histColorFilter = new JToggleButton("H/C");
        histColorFilter.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (histColorFilter.isSelected())
                    display.colorHist = true;
                else
                    display.colorHist = false;

                display.setCurFile(display.getCurFile());
            }
        });
        if (has_ocv) {
            histColorFilter.setToolTipText(I18n.text("Histogram Equalization Color Filter"));
        }
        else {
            histColorFilter.setEnabled(false);
            histColorFilter.setToolTipText(I18n.text("OpenCV was not detected."));
        }        
        
        add(histColorFilter);
        add(prevButton);
        add(nextButton);
    }

    protected Color brightestPixel(BufferedImage img) {
        int brightest = 0;
        Color bColor = Color.black;
        Color tmp = new Color(0);

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                tmp = new Color(img.getRGB(x, y));
                if (tmp.getBlue() + tmp.getGreen() + tmp.getRed() > brightest) {
                    brightest = tmp.getBlue() + tmp.getGreen() + tmp.getRed();
                    bColor = tmp;
                }
            }
        }
        return bColor;
    }

    protected Color averageColor(BufferedImage img) {
        double sumRed = 0, sumGreen = 0, sumBlue = 0;
        Color tmp;
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                tmp = new Color(img.getRGB(x, y));
                sumRed += tmp.getRed();
                sumGreen += tmp.getGreen();
                sumBlue += tmp.getBlue();
            }
        }

        int r = (int) (sumRed / (img.getWidth() * img.getHeight()));
        int g = (int) (sumGreen / (img.getWidth() * img.getHeight()));
        int b = (int) (sumBlue / (img.getWidth() * img.getHeight()));

        return new Color(r, g, b);
    }

    protected void next() {

        File current = display.getCurFile();
        final int index = Arrays.binarySearch(allFiles, current);
        new Thread(new Runnable() {

            @Override
            public void run() {
                display.setCurFile(allFiles[index + 1]);
            }
        }).start();
    }

    protected void prev() {
        File current = display.getCurFile();
        final int index = Arrays.binarySearch(allFiles, current);
        new Thread(new Runnable() {

            @Override
            public void run() {
                display.setCurFile(allFiles[index - 1]);
            }
        }).start();
    }
}
