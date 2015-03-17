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
 * Author: Paulo Dias
 * 5/03/2005
 */
package pt.lsts.neptus.gui;

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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 */
public class AboutPanel extends JFrame {
    private static final long serialVersionUID = -6597059494859637226L;

    private static final String IMG_FILE_NAME = "images/nep-about.jpg";
    private static final String VERSION_FILE_NAME = "/version.txt";

    private static final String COPY_YEARS = "2004-2015";

    private String mainDevelopers = "Paulo Dias, José Pinto";
    private String contributersDevelopers = "Ricardo Martins, Sérgio Ferreira, José Loureiro";
    private String pastDevelopers = "Hugo Queirós, Margarida Faria,José Correia, Hugo Dias, Rui Gonçalves, Eduardo Marques";

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
     * @return pt.lsts.neptus.gui.ImagePanel
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
                versionString += ", ";
                versionString += prop.getProperty("SCM_REV", "?");
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
