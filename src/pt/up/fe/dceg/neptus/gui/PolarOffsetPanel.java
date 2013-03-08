/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 9/03/2005
 * $Id:: PolarOffsetPanel.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTextField;

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

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
        jLabel3.setText("m");
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
