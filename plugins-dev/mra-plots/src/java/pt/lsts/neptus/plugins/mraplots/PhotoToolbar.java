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

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.SearchOpenCv;

/**
 * @author zp
 * 
 */
public class PhotoToolbar extends JPanel {

    private static final long serialVersionUID = 1L;
    protected MraPhotosVisualization display;
    protected JToggleButton grayToggle, sharpenToggle, wbalanceToggle,
    contrastToggle, brightToggle, legendToggle, histGrayFilter, histColorFilter;

    protected JToggleButton rotateImageToggle;

    protected JButton nextButton, prevButton;
    protected File[] allFiles;
    protected double startTime, endTime;
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
    
    protected static boolean hasOcv = false;
    
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
        legendToggle.setSelected(display.rotateToPaintImage);
        legendToggle.setToolTipText(I18n.text("Show legend"));
        add(legendToggle);

        rotateImageToggle = new JToggleButton("R");
        rotateImageToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                display.rotateToPaintImage = rotateImageToggle.isSelected();
                display.repaint();
            }
        });
        rotateImageToggle.setSelected(false);
        rotateImageToggle.setToolTipText(I18n.text("Rotate image"));
        add(rotateImageToggle);

        //!Find OPENCV JNI
        hasOcv = SearchOpenCv.searchJni();
        
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
        if (hasOcv)
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
        if (hasOcv) {
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
