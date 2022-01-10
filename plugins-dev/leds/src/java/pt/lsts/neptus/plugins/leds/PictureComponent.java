/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Zhao
 * 31 de Ago de 2013
 */
package pt.lsts.neptus.plugins.leds;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;

/**
 * @author hfq
 */
public class PictureComponent extends JPanel {
    private static final long serialVersionUID = 1L;

    JComponent parent;
    private JLabel picture;

    /**
     * @param parent
     */
    public PictureComponent(JComponent parent) {
        this.setLayout(new MigLayout());
        this.parent = parent;
        // this.setSize(LedsUtils.PANEL_WIDTH, LedsUtils.PANEL_WIDTH);
        this.setOpaque(false);
        createPictureComp();
    }

    /**
     * Create a set properties of JLabel that will contain the image loaded
     */
    private void createPictureComp() {
        picture = new JLabel();
        picture.setHorizontalAlignment(JLabel.CENTER);
        picture.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        picture.setOpaque(false);
        picture.setToolTipText(I18n.text("Image representing leds being setted!"));

        updatePicture(0); // Display the leds picture without any leds beeing setted
        this.add(picture);
    }

    /**
     * Updates the picture on
     * 
     * @param picNumber
     */
    public void updatePicture(int picNumber) {
        switch (picNumber) {
            case 0:
                if (LedsUtils.ICON_NONE != null)
                    picture.setIcon(LedsUtils.ICON_NONE);
                else {
                    NeptusLog.pub().info("Picture not found");
                    picture.setName("Not Found");
                }
                break;
            case 1:
                if (LedsUtils.ICON_LEDS1 != null)
                    picture.setIcon(LedsUtils.ICON_LEDS1);
                else {
                    NeptusLog.pub().info("Picture not found");
                    picture.setName("Not Found");
                }
                break;
            case 2:
                if (LedsUtils.ICON_LEDS2 != null)
                    picture.setIcon(LedsUtils.ICON_LEDS2);
                else {
                    NeptusLog.pub().info("Picture not found");
                    picture.setName("Not Found");
                }
                break;
            case 3:
                if (LedsUtils.ICON_LEDS3 != null)
                    picture.setIcon(LedsUtils.ICON_LEDS3);
                else {
                    NeptusLog.pub().info("Picture not found");
                    picture.setName("Not Found");
                }
                break;
            case 4:
                if (LedsUtils.ICON_LEDS4 != null)
                    picture.setIcon(LedsUtils.ICON_LEDS4);
                else {
                    NeptusLog.pub().info("Picture not found");
                    picture.setName("Not Found");
                }
                break;
            case 5:
                if (LedsUtils.ICON_LEDS_ALL != null)
                    picture.setIcon(LedsUtils.ICON_LEDS_ALL);
                else {
                    NeptusLog.pub().info("Picture not found");
                    picture.setName("Not Found");
                }
                break;
        }
    }
}
