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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Zhao
 * 31 de Ago de 2013
 */
package pt.up.fe.dceg.neptus.plugins.leds;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author hfq FIXME - add the other 4 images and link them to actions on leds slider panel
 */
public class PictureComponent extends JPanel {
    private static final long serialVersionUID = 1L;
    
    JComponent parent;
    
    private JLabel picture;
    private ImageIcon icon;

    public PictureComponent(JComponent parent) {
        this.setLayout(new MigLayout());
        this.parent = parent;
        // this.setSize(LedsUtils.PANEL_WIDTH, LedsUtils.PANEL_WIDTH);
        this.setOpaque(false);
        createPictureComp();
    }

    /**
     * 
     */
    private void createPictureComp() {
        picture = new JLabel();
        picture.setHorizontalAlignment(JLabel.CENTER);
        picture.setAlignmentX(Component.CENTER_ALIGNMENT);
        picture.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        picture.setOpaque(false);

        updatePicture(0); // Display the leds picture without any leds beeing setted
        this.add(picture);
    }

    /**
     * Updates the picture on
     * 
     * @param picNumber
     */
    private void updatePicture(int picNumber) {
        if ((icon = new ImageIcon(ImageUtils.getImage("images/leds/leds_clean.png"))) != null)
            picture.setIcon(icon);
        else {
            NeptusLog.pub().info("Picture not found");
            picture.setName("Not Found");
        }
    }
}
