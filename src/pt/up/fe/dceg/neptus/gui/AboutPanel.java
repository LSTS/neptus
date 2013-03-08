/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 5/Mar/2005
 * $Id:: AboutPanel.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 */
public class AboutPanel extends JFrame {
    private static final long serialVersionUID = -6597059494859637226L;

    private static final String IMG_FILE_NAME = "images/nep-about.jpg";
    private static final String VERSION_FILE_NAME = "/version.txt";

    private static final String COPY_YEARS = "2004-2013";

    private String mainDevelopers = "Paulo Dias, José Pinto, Sérgio Ferreira, José Correia, Hugo Dias, Margarida Faria";
    private String contributersDevelopers = "Ricardo Martins";
    private String pastDevelopers = "Rui Gonçalves, Eduardo Marques";

    private JPanel contentPanel = null;
    private ImagePanel imagePanel = null;
    private JEditorPane htmlPane = null;

    public AboutPanel() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/neptus-icon.png")));
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setBackground(Color.WHITE);
        this.setSize(506, 542);
        this.setContentPane(getContentPanel());
        this.setTitle(I18n.text("About"));
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AboutPanel.this.dispose();
            }
        });
        this.fillText();
        GuiUtils.centerOnScreen(this);
    }

    /**
     * This method initializes imagePanel
     * 
     * @return pt.up.fe.dceg.neptus.gui.ImagePanel
     */
    private ImagePanel getImagePanel() {
        if (imagePanel == null) {
            Image image = ImageUtils.getImage(IMG_FILE_NAME);
            imagePanel = new ImagePanel(image);
            imagePanel.setPreferredSize(new Dimension(500, 264));
        }
        return imagePanel;
    }

    /**
     * @return the htmlPane
     */
    public JEditorPane getHtmlPane() {
        if (htmlPane == null) {
            htmlPane = new JEditorPane();
            htmlPane.setEditable(false);
            htmlPane.setContentType("text/html");
            htmlPane.setBackground(Color.WHITE);
            // htmlPane.setOpaque(false);
            htmlPane.setForeground(Color.GRAY);
        }
        return htmlPane;
    }
    
    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getContentPanel() {
        if (contentPanel == null) {
            contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setPreferredSize(new Dimension(500, 450));
            contentPanel.add(getImagePanel(), null);
            contentPanel.add(getHtmlPane(), null);
        }
        return contentPanel;
    }

    private void fillText() {
        Properties prop = new Properties();
        String versionString = "";
        InputStream ist = getClass().getResourceAsStream(VERSION_FILE_NAME);

        if (ist != null) {
            try {
                prop.load(ist);
                versionString = I18n.text("Version") + " ";
                versionString += prop.getProperty("VERSION", "");
                versionString += " (";
                versionString += prop.getProperty("DATE", "");
                versionString += ", r";
                versionString += prop.getProperty("SVN_REV", "?");
                versionString += ")";
            }
            catch (IOException e) {
                NeptusLog.pub().debug(this, e);
            }
        }

        String htmlString = "<html><head>"
                + "<style type=\"text/css\">"
                + "<!--"
                + "body {"
                + "    font-family: Sans-Serif;"
                + "    font-size: 11pt;"
                + "    color: #808080;"
                + "}"
                + ".align-right {"
                + "    text-align: right;"
                + "}"
                + "-->"
                + "</style>"
                + "</head>"
                + "<body><br/>" 
                + "<b>" + I18n.text("Main Developers:") + "</b>"+ "<br/>&nbsp;&nbsp;&nbsp;"
                + mainDevelopers 
                + "<br><br>"
                + "<b>" + I18n.text("Contributers:") + "</b>"  + "<br/>&nbsp;&nbsp;&nbsp;" 
                + contributersDevelopers + "<br/><br/>"
                + "<b>" + I18n.text("Past Developers:") + "</b><br/>" 
                + "&nbsp;&nbsp;&nbsp;" + pastDevelopers + "<br/><br/>" 
                + "<b>" + I18n.text("Contact info:") + "</b>" 
                + "<br/>" + "   " 
                + I18n.text("URL:") + " http://lsts.fe.up.pt/" + "&nbsp;&nbsp;&nbsp;<br/>" 
                + I18n.text("URL:") + " http://whale.fe.up.pt/neptus" 
                + "<br/><br/>"
                + "<div class=\"align-right\">"
                + versionString + "<br/>"
                + "\u00A9 " + COPY_YEARS + " FEUP-LSTS and Developers, All Rights Reserved" 
                + "</div>" 
                + "</body></html>";
        
        getHtmlPane().setText(htmlString);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new AboutPanel().setVisible(true);
    }
}
