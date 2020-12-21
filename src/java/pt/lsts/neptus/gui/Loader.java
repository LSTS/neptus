/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 12/12/2004
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * This class shows a Neptus splash screen during load phases.
 * IT IS PROHIBITED TO USE {@link I18n} CALLS IN THIS CLASS.  
 * @author Paulo Dias
 */
public class Loader extends JFrame implements ActionListener {
    public static final long serialVersionUID = 237536234;

    private JPanel jContentPane = null;
    private JLabel jLabel = null;
    private JLabel jLabel2 = null;
    private String imgFileName = "images/neptus_version1.png";
    private String typedString = "";
    private Color backColor = new Color(242, 251, 254);
    private Color frontColor = Color.GRAY;

    // private Timer cycler = null;

    /**
     * This is the default constructor
     */
    public Loader() {
        super();
        initialize();
    }

    /**
     * @param splashImageURL The image to show on the background
     */
    public Loader(String splashImageURL) {
        super();
        this.imgFileName = splashImageURL;
        initialize();
    }

    /**
     * This method initializes jLabel
     * 
     * @return javax.swing.JLabel
     */
    private JLabel getJLabel() {
        if (jLabel == null) {
            jLabel = new JLabel();
            jLabel.setText("...");
            jLabel.setForeground(frontColor);
            jLabel.setBackground(backColor);
            jLabel.setAutoscrolls(true);
        }
        return jLabel;
    }

    /**
     * This method initializes jLabel
     * 
     * @return javax.swing.JLabel
     */
    private JLabel getJLabel2() {
        if (jLabel2 == null) {
            jLabel2 = new JLabel();
            jLabel2.setText("Neptus " + ConfigFetch.getNeptusVersion());
            jLabel2.setFont(new Font("Arial", Font.PLAIN, 10));
            jLabel2.setForeground(new Color(150, 150, 150));
            jLabel2.setBackground(backColor);
            jLabel2.setAutoscrolls(true);
        }
        return jLabel2;
    }

    /**
     * Starts the display of the loader.
     */
    public void start() {
        this.setVisible(true);
    }

    /**
     * The splash screen is displayed during the given time and then is destroyed
     * 
     * @param milliseconds The time (is milliseconds) to display the splash screen
     */
    public void waitMoreAndEnd(int milliseconds) {
        this.end();
    }

    /**
     * This method will receive an ActionEvent when it has to be closed
     */
    @Override
    public void actionPerformed(ActionEvent action) {
        NeptusLog.pub().info("<###>end");
        ((Timer) action.getSource()).stop();
        this.end();
    }

    /**
     * Ends the display of the loader by calling the dispose method.
     */
    public void end() {
        // cycler.stop();
        this.setVisible(false);
        this.dispose();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {

        this.setSize(500, 230);
        this.setBackground(backColor);
        this.setContentPane(getJContentPane());

        this.setTitle("Neptus ...");
        this.setIconImages(ConfigFetch.getIconImagesForFrames());
        this.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                typedString += e.getKeyChar();
            }
        });

        this.setLocationRelativeTo(null);
        this.setAlwaysOnTop(false);
        this.setUndecorated(true);
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {

        Image image = ImageUtils.getImage(imgFileName);
        ImagePanel ipan = new ImagePanel(image);
        ipan.setBackground(backColor);
        ipan.setSize(500, 213);

        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new java.awt.BorderLayout());
            jContentPane.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, backColor));
            jContentPane.setBackground(backColor);
            jContentPane.add(ipan, java.awt.BorderLayout.CENTER);
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBackground(backColor);
            JProgressBar bar = new JProgressBar(JProgressBar.HORIZONTAL);
            bar.setIndeterminate(true);
            bar.setBackground(backColor);
            bar.setForeground(frontColor);
            bottomPanel.add(bar, BorderLayout.SOUTH);
            bottomPanel.add(getJLabel(), java.awt.BorderLayout.WEST);
            bottomPanel.add(getJLabel2(), java.awt.BorderLayout.EAST);
            jContentPane.add(bottomPanel, java.awt.BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * @param text The text to display.
     * @param delayAfter The time that the text will be shown in the screen.
     * @return
     */
    public boolean setText(String text, long delayAfter) {
        setText(text);
        try {
            Thread.sleep(delayAfter);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * @param text The text to display.
     * @return
     */
    public boolean setText(String text) {
        getJLabel().setText(" ".concat(text));
        return true;
    }

    /**
     * @param text The text to display.
     * @param delayAfter The time that the text will be shown in the screen.
     * @return
     */
    public boolean appendText(String text, long delayAfter) {
        appendText(text);
        try {
            Thread.sleep(delayAfter);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * @param text The text to display.
     * @return
     */
    public boolean appendText(String text) {
        getJLabel().setText(getJLabel().getText().concat(text));
        return true;
    }

    public String getTypedString() {
        return typedString;
    }

    public static void main(String[] args) throws InterruptedException {
        File fx = new File(".").getAbsoluteFile();
        Loader ld = new Loader();
        ld.start();
        Thread.sleep(2000);
        ld.setText("Connecting... " + fx.getAbsoluteFile(), 5000);
        ld.end();
    }
}