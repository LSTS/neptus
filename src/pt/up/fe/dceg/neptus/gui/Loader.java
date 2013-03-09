/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * Author: Paulo Dias
 * 12/12/2004
 */
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * This class shows a Neptus splash screen during load phases.
 * 
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
            jLabel.setText(" Loading...");
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
    public void actionPerformed(ActionEvent action) {
        System.out.println("end");
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

        this.setTitle("Neptus loader...");
        this.setIconImages(ConfigFetch.getIconImagesForFrames());
        this.addKeyListener(new java.awt.event.KeyAdapter() {
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