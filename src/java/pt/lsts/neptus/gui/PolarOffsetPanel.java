/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 9/03/2005
 */
package pt.lsts.neptus.gui;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTextField;

import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zecarlos
 * @author pdias
 */
@SuppressWarnings("serial")
public class PolarOffsetPanel extends ParametersPanel {

    private static final String HELP_IMAGE = "/images/neptus_NED.png";
    
    private JLabel jLabel = null;
    private JTextField distanceField = null;
    private JLabel jLabel1 = null;
    private JTextField azimuthField = null;
    private JLabel jLabel2 = null;
    private JTextField zenithField = null;
    private boolean editable;
    private String sphericalCSHelpImage;
    private JLabel jLabel3 = null;
    private JLabel jLabel4 = null;
    private JLabel jLabel5 = null;

    /**
     * This method initializes
     * 
     */
    public PolarOffsetPanel() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        // sphericalCSHelpImage =
        // ConfigFetch.resolvePath("images/neptus_NED.png");
        sphericalCSHelpImage = PolarOffsetPanel.class.getResource(HELP_IMAGE)
                .toString();

        jLabel5 = new JLabel();
        jLabel4 = new JLabel();
        jLabel3 = new JLabel();
        jLabel2 = new JLabel();
        jLabel1 = new JLabel();
        jLabel = new JLabel();
        this.setSize(406, 29);
        jLabel.setText(I18n.text("Distance") + ":");
        jLabel1.setText("   " + I18n.text("Azimuth") + ":");
        jLabel2.setText("   " + I18n.text("Zenith") + ":");
        jLabel3.setText(I18n.textc("m", "meters"));
        jLabel4.setText("" + CoordinateUtil.CHAR_DEGREE);
        jLabel4.setFont(new Font("Dialog", Font.BOLD, 14));
        jLabel5.setText("" + CoordinateUtil.CHAR_DEGREE);
        jLabel5.setFont(new Font("Dialog", Font.BOLD, 14));
        this.add(jLabel, null);
        this.add(getDistanceField(), null);
        this.add(jLabel3, null);
        this.add(jLabel1, null);
        this.add(getAzimuthField(), null);
        this.add(jLabel4, null);
        this.add(jLabel2, null);
        this.add(getZenithField(), null);
        this.add(jLabel5, null);
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getDistanceField() {
        if (distanceField == null) {
            distanceField = new JTextField();
            distanceField.setColumns(5);
            distanceField.setText("0");
            // distanceField.setToolTipText("<html><img src=\"file:" +
            // sphericalCSHelpImage + "\"></html>");
            distanceField.setToolTipText("<html><img src=\"" + sphericalCSHelpImage + "\"></html>");
            distanceField.addFocusListener(new SelectAllFocusListener());
        }
        return distanceField;
    }

    /**
     * This method initializes jTextField1
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getAzimuthField() {
        if (azimuthField == null) {
            azimuthField = new JTextField();
            azimuthField.setColumns(5);
            azimuthField.setText("0");
            // azimuthField.setToolTipText("<html><img src=\"file:" +
            // sphericalCSHelpImage + "\"></html>");
            azimuthField.setToolTipText("<html><img src=\"" + sphericalCSHelpImage + "\"></html>");
            azimuthField.addFocusListener(new SelectAllFocusListener());
        }
        return azimuthField;
    }

    /**
     * This method initializes jTextField2
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getZenithField() {
        if (zenithField == null) {
            zenithField = new JTextField();
            zenithField.setColumns(5);
            zenithField.setText("0");
            // zenithField.setToolTipText("<html><img src=\"file:" +
            // sphericalCSHelpImage + "\"></html>");
            zenithField.setToolTipText("<html><img src=\"" + sphericalCSHelpImage + "\"></html>");
            zenithField.addFocusListener(new SelectAllFocusListener());
        }
        return zenithField;
    }

    public String getErrors() {
        try {
            Double.parseDouble(getZenithField().getText());
            Double.parseDouble(getAzimuthField().getText());
            Double.parseDouble(getDistanceField().getText());
        }
        catch (Exception e) {
            return I18n.text("The text entered does not represent valid numbers");
        }
        return null;
    }

    public double getAzimuthOffset() {
        return Double.parseDouble(getAzimuthField().getText());
    }

    public double getZenithOffset() {
        return Double.parseDouble(getZenithField().getText());
    }

    public double getDistanceOffset() {
        return Double.parseDouble(getDistanceField().getText());
    }

    public void setAzimuth(double value) {
        getAzimuthField().setText(String.valueOf(value));
    }

    public void setZenith(double value) {
        getZenithField().setText(String.valueOf(value));
    }

    public void setDistance(double value) {
        getDistanceField().setText(String.valueOf(value));
    }

    public void setLocationType(LocationType location) {
        setDistance(location.getOffsetDistance());
        setZenith(location.getZenith());
        setAzimuth(location.getAzimuth());
    }

    public void setEditable(boolean value) {
        this.editable = value;
        getDistanceField().setEditable(editable);
        getAzimuthField().setEditable(editable);
        getZenithField().setEditable(editable);
    }
}
