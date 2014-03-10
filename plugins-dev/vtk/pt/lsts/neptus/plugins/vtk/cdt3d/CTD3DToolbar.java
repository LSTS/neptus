/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk.cdt3d;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.vtk.CTD3D;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author hfq
 *
 */
public class CTD3DToolbar extends JToolBar {
    private static final long serialVersionUID = 1L;

    private static final short ICON_SIZE = 18;

    private static final ImageIcon ICON_TEMP = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/ctd/thermometer.png", ICON_SIZE, ICON_SIZE);
    private static final ImageIcon ICON_SALINITY = ImageUtils.getScaledIcon(
            "images/menus/wizard.png", ICON_SIZE, ICON_SIZE);

    private JToggleButton tempToggle;
    private JToggleButton salinityToggle;

    private CTD3D ctd3dInit;

    /**
     * 
     * @param ctd3dInit
     */
    public CTD3DToolbar(CTD3D ctd3dInit) {
        this.ctd3dInit = ctd3dInit;
    }

    public void createtoolBar() {
        setOrientation(JToolBar.VERTICAL);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder()));

        tempToggle = new JToggleButton();
        tempToggle.setToolTipText(I18n.text("See Temperature color map") + ".");
        tempToggle.setIcon(ICON_TEMP);

        salinityToggle = new JToggleButton();
        salinityToggle.setToolTipText(I18n.text("See Salinity color map") + ".");
        salinityToggle.setIcon(ICON_SALINITY);

        ButtonGroup groupToggles = new ButtonGroup();
        groupToggles.add(tempToggle);
        groupToggles.add(salinityToggle);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphic2d = (Graphics2D) g;
        Color color1 = getBackground();
        Color color2 = Color.GRAY;
        GradientPaint gradPaint = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
        graphic2d.setPaint(gradPaint);
        graphic2d.fillRect(0, 0, getWidth(), getHeight());
    }
}
