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
 * Author: zp
 * Jan 24, 2014
 */
package pt.lsts.neptus.plugins.ctd;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class JImagePanel extends JPanel {

    private static final long serialVersionUID = -8755419622843900439L;
    private BufferedImage bi;
    private JLabel lbl;
    
    public JImagePanel(int imgWidth, int imgHeight) {
        this.bi = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        bi.getGraphics().setColor(Color.red);
        bi.getGraphics().fillRect(0, 0, imgWidth, imgHeight);
        bi.getGraphics().setColor(Color.black);
        setLayout(new BorderLayout());
        lbl = new JLabel(new ImageIcon(bi));
        add(new JScrollPane(lbl), BorderLayout.CENTER);       
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add("Save image").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JFileChooser chooser = GuiUtils.getFileChooser((String) null, I18n.text("PNG files"), "png");
                            if (chooser.showSaveDialog(JImagePanel.this) != JFileChooser.APPROVE_OPTION)
                                return;
                            try {
                                ImageIO.write(bi, "PNG", chooser.getSelectedFile());
                                GuiUtils.infoMessage(JImagePanel.this, "Save image", "Image saved to "+chooser.getSelectedFile());
                            }
                            catch (Exception ex) {
                                GuiUtils.errorMessage(JImagePanel.this, ex);                                
                            }
                        }
                    });
                    popup.show(lbl, e.getX(), e.getY());
                }
            }
        });
    }
    
    /**
     * @return the bi
     */
    public BufferedImage getBi() {
        return bi;
    }

    public static void main(String[] args) {
        GuiUtils.testFrame(new JImagePanel(1024, 768));
    }
}
