/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 30 Mar 2017
 */
package pt.lsts.neptus.plugins.logs.search;

import javax.swing.*;
import java.awt.*;

public class AreaSelectionDialog {
    public static final int VALID_INPUT = 0;
    public static final int INVALID_INPUT = -1;
    public static final int CLOSED = 1;

    private String areaSelectedId;

    private final JLabel minLabel = new JLabel("Top Left");
    private final JLabel maxLabel = new JLabel("Bottom Right");

    private final JTextField topLeftLatDegr = new JTextField(2);
    private final JTextField topLeftLonDegr = new JTextField(2);

    private final JTextField bottomRightLatDegr = new JTextField(2);
    private final JTextField bottomRightDegr = new JTextField(2);

    private double topLeftLat;
    private double topLefLon;
    private double bottomRightLat;
    private double bottomRightLon;

    private final JPanel contentPanel = new JPanel();
    private JPanel parent;

    public AreaSelectionDialog(JPanel parent) {
        this.parent = parent;
        contentPanel.setSize(new Dimension(200, 300));
        contentPanel.setLayout(new GridLayout(2, 3));
        
        contentPanel.add(minLabel);
        contentPanel.add(topLeftLatDegr);
        contentPanel.add(topLeftLonDegr);

        contentPanel.add(maxLabel);
        contentPanel.add(bottomRightLatDegr);
        contentPanel.add(bottomRightDegr);

        contentPanel.setVisible(false);
    }

    public int getInput() {
        contentPanel.setVisible(true);
        int result = JOptionPane.showOptionDialog(parent, contentPanel  , "Enter a Number",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[]{"OK", "cancel"}, null);

        if(result != JOptionPane.YES_OPTION)
            return CLOSED;

        try {
            topLeftLat = Double.valueOf(topLeftLatDegr.getText());
            topLefLon = Double.valueOf(topLeftLonDegr.getText());
            bottomRightLat = Double.valueOf(bottomRightLatDegr.getText());
            bottomRightLon = Double.valueOf(bottomRightDegr.getText());
        }
        catch (NumberFormatException e) {
            return INVALID_INPUT;
        }

        if(!validCoordinates(topLeftLat, topLefLon, bottomRightLat, bottomRightLon))
            return INVALID_INPUT;

        return VALID_INPUT;
    }

    public boolean validCoordinates(double topLeftLat, double topLeftLon, double bottomRightLat, double bottomRightLon) {
        if(topLeftLat < -90 || topLeftLat > 90)
            return false;

        if(bottomRightLat < -90 || bottomRightLat > 90)
            return false;

        if(topLeftLat <= bottomRightLat)
            return false;

        if(topLeftLon < -180 || topLeftLon > 180)
            return false;

        if(bottomRightLon < -180 || bottomRightLon > 180)
            return false;

        if(topLeftLon > bottomRightLon)
            return false;

        return true;
    }

    /**
     * Get area's top left latitude and longitude, in radians
     * */
    public double[] getTopLeftCoordinatesRad() {
        return new double[]{Math.toRadians(topLeftLat), Math.toRadians(topLefLon)};
    }

    /**
     * Get area's bottom right latitude and longitude, in radians
     * */
    public double[] getBottomRightCoordinatesRad() {
        return new double[]{Math.toRadians(bottomRightLat), Math.toRadians(bottomRightLon)};
    }
}
