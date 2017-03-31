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

    private final JLabel minLatLabel = new JLabel("Min Lat", SwingConstants.CENTER);
    private final JLabel maxLatLabel = new JLabel("Max Lat", SwingConstants.CENTER);
    private final JLabel minLonLabel = new JLabel("Min Lon", SwingConstants.CENTER);
    private final JLabel maxLonLabel = new JLabel("Max Lon", SwingConstants.CENTER);

    private final JTextField minLatDegr = new JTextField("a");
    private final JTextField maxLatDegr = new JTextField("b");

    private final JTextField minLogDegr = new JTextField("c");
    private final JTextField maxLonDegr = new JTextField("d");

    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;

    private final JPanel contentPanel = new JPanel();
    private JPanel parent;

    public AreaSelectionDialog(JPanel parent) {
        this.parent = parent;
        contentPanel.setSize(new Dimension(200, 300));
        contentPanel.setLayout(new GridLayout(2, 4));
        
        contentPanel.add(minLatLabel);
        contentPanel.add(minLatDegr);

        contentPanel.add(maxLatLabel);
        contentPanel.add(maxLatDegr);

        contentPanel.add(minLonLabel);
        contentPanel.add(minLogDegr);

        contentPanel.add(maxLonLabel);
        contentPanel.add(maxLonDegr);

        contentPanel.setVisible(false);
    }

    public int getInput() {
        contentPanel.setVisible(true);
        int result = JOptionPane.showOptionDialog(parent, contentPanel  , "Filter by coordinates",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[]{"OK", "cancel"}, null);

        if(result != JOptionPane.YES_OPTION)
            return CLOSED;

        try {
            minLat = Double.valueOf(minLatDegr.getText());
            maxLat = Double.valueOf(maxLatDegr.getText());
            minLon = Double.valueOf(minLogDegr.getText());
            maxLon = Double.valueOf(maxLonDegr.getText());
        }
        catch (NumberFormatException e) {
            return INVALID_INPUT;
        }

        if(!validCoordinates(minLat, maxLat, minLon, maxLon))
            return INVALID_INPUT;

        return VALID_INPUT;
    }

    public boolean validCoordinates(double minLat, double maxLat, double minLon, double maxLon) {
        if(!validLatitude(minLat) || !validLatitude(maxLat))
            return false;

        if(minLat >= maxLat)
            return false;

        if(!validLongitude(minLon) || !validLongitude(maxLon))
            return false;

        if(minLon >= maxLon)
            return false;

        return true;
    }

    private boolean validLatitude(double latDegrees) {
        return latDegrees >= -90 && latDegrees <= 90;
    }

    private boolean validLongitude(double lonDegrees) {
        return lonDegrees >= -180 && lonDegrees <= 180;
    }

    /**
     * Get area's min latitude and longitude, in radians
     * */
    public double[] getMinCoordinatesRad() {
        return new double[]{Math.toRadians(minLat), Math.toRadians(minLon)};
    }

    /**
     * Get area's max right latitude and longitude, in radians
     * */
    public double[] getMaxCoordinatesRad() {
        return new double[]{Math.toRadians(maxLat), Math.toRadians(maxLon)};
    }
}
