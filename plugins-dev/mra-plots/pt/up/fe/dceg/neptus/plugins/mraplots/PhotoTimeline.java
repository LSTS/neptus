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
 * Author: José Pinto
 * Dec 9, 2012
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
public class PhotoTimeline extends JPanel {

    private static final long serialVersionUID = 1L;
    protected MraPhotosVisualization display;
    protected JToggleButton playToggle, oneToggle, twoToggle, fourToggle, grayToggle, sharpenToggle, wbalanceToggle,
            contrastToggle, brightToggle, legendToggle;

    protected JButton nextButton, prevButton;
    protected JSlider slider;
    protected File[] allFiles;
    protected double startTime, endTime;
    protected JLabel timeLabel;
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    public PhotoTimeline(MraPhotosVisualization display) {
        this.display = display;
        allFiles = display.getPhotosDir().listFiles();
        Arrays.sort(allFiles);
        startTime = display.timestampOf(allFiles[0]);
        endTime = display.timestampOf(allFiles[allFiles.length - 1]);
        initialize();
    }

    public void fileChanged(File newFile) {
        timeLabel.setText(fmt.format(new Date((long) (display.curTime * 1000))));
        slider.setValue((int) ((display.curTime - startTime) * 1000));
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

        slider = new JSlider(0, (int) ((endTime - startTime) * 1000));
        slider.setMajorTickSpacing(240);
        slider.setMinorTickSpacing(240);
        slider.setValue(0);
        slider.setPreferredSize(new Dimension(250, 20));
        slider.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                setTime(startTime + slider.getValue() / 1000.0);
            }
        });

        slider.addMouseListener(new MouseAdapter() {

            boolean wasPlaying = false;

            @Override
            public void mousePressed(MouseEvent e) {
                if (playToggle.isSelected()) {
                    playToggle.doClick();
                    wasPlaying = true;
                }
                else {
                    wasPlaying = false;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!wasPlaying)
                    setTime(startTime + slider.getValue() / 1000.0);
                else {
                    playToggle.doClick();
                }

            }
        });
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

        playToggle = new JToggleButton("Play");
        playToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (playToggle.isSelected()) {
                    play();
                    prevButton.setEnabled(false);
                    nextButton.setEnabled(false);
                }
                else {
                    pause();
                    prevButton.setEnabled(true);
                    nextButton.setEnabled(true);
                }
            }
        });
        playToggle.setToolTipText(I18n.text("Slideshow"));

        oneToggle = new JToggleButton("1X");
        oneToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (oneToggle.isSelected())
                    setSpeed(1.0);
            }
        });
        
        oneToggle.setToolTipText(I18n.text("Real-time speed multiplier"));

        twoToggle = new JToggleButton("2X");
        twoToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (twoToggle.isSelected())
                    setSpeed(2.0);
            }
        });
        
        twoToggle.setToolTipText(I18n.text("2 X speed multiplier"));

        fourToggle = new JToggleButton("4X");
        fourToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (fourToggle.isSelected())
                    setSpeed(4.0);
            }
        });
        
        fourToggle.setToolTipText(I18n.text("4 X speed multiplier"));

        grayToggle = new JToggleButton("G");
        grayToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                display.grayscale = grayToggle.isSelected();
                if (playToggle.isSelected()) {
                    playToggle.doClick();
                    playToggle.doClick();
                }
                else {
                    display.setCurFile(display.getCurFile());
                }
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

                if (playToggle.isSelected()) {
                    playToggle.doClick();
                    playToggle.doClick();
                }
                else {
                    display.setCurFile(display.getCurFile());
                }
            }
        });
        brightToggle.setToolTipText(I18n.text("Toggle brightness filter"));

        add(brightToggle);

        sharpenToggle = new JToggleButton("S");
        sharpenToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                display.sharpen = sharpenToggle.isSelected();
                if (playToggle.isSelected()) {
                    playToggle.doClick();
                    playToggle.doClick();
                }
                else {
                    display.setCurFile(display.getCurFile());
                }
            }
        });
        sharpenToggle.setToolTipText(I18n.text("Toggle sharpen filter"));

        add(sharpenToggle);

        contrastToggle = new JToggleButton("C");
        contrastToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                display.contrast = contrastToggle.isSelected();
                if (playToggle.isSelected()) {
                    playToggle.doClick();
                    playToggle.doClick();
                }
                else {
                    display.setCurFile(display.getCurFile());
                }
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
                if (playToggle.isSelected()) {
                    playToggle.doClick();
                    playToggle.doClick();
                }
                else {
                    display.setCurFile(display.getCurFile());
                }
            }
        });
        wbalanceToggle.setToolTipText(I18n.text("Toggle white balance filter"));

        add(wbalanceToggle);

        legendToggle = new JToggleButton("L");
        legendToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                display.showLegend = legendToggle.isSelected();
                if (playToggle.isSelected()) {
                    playToggle.doClick();
                    playToggle.doClick();
                }
                else {
                    display.setCurFile(display.getCurFile());
                }
            }
        });
        legendToggle.setSelected(true);
        legendToggle.setToolTipText(I18n.text("Show legend"));
        add(legendToggle);

        timeLabel = new JLabel(fmt.format(new Date((long) (startTime * 1000))));

        ButtonGroup bg = new ButtonGroup();
        bg.add(oneToggle);
        bg.add(twoToggle);
        bg.add(fourToggle);
        oneToggle.setSelected(true);

        add(prevButton);
        add(playToggle);
        add(nextButton);
        add(oneToggle);
        add(twoToggle);
        add(fourToggle);
        add(slider);
        add(timeLabel);

        setTime(startTime);
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

    protected void setSpeed(double speed) {
        display.setSpeedMultiplier(speed);
    }

    protected void play() {
        display.play(slider.getValue() / 1000.0 + startTime);
    }

    protected void pause() {
        display.stop();
    }
}
