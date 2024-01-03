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
 * 10/Jun/2005
 */
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.CoordinateSystemPanel;
import pt.lsts.neptus.gui.ImagePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author Paulo Dias
 * 
 */
public class TranspondersPositionHelper extends JPanel {
    private static final long serialVersionUID = -104139574067820066L;

    private JPanel transPanel = null;
    private ImagePanel imagePanel = null;

    private JPanel jPanel = null;

    private JLabel latLabel = null;
    private JLabel latValue = null;
    private JLabel lonValue = null;
    private JLabel lonLabel = null;
    private JPanel jPanel1 = null;
    private JLabel jLabel = null;
    private JTextField t1DepthValue = null;
    private JPanel pointPa = null;
    private JLabel pointPaXLabel = null;
    private JTextField pointPaX = null;
    private JLabel pointPaYLabel = null;
    private JTextField pointPaY = null;
    private JPanel pointPb = null;
    private JLabel pointPbXLabel = null;
    private JTextField pointPbX = null;
    private JLabel pointPbYLabel = null;
    private JTextField pointPbY = null;
    private JPanel distances = null;
    private JLabel da1Label = null;
    private JLabel db1Label = null;
    private JTextField da1Value = null;
    private JTextField db1Value = null;
    private JPanel jPanel2 = null;
    private JButton calcButton = null;
    private JButton jButton = null;
    private JButton jButton1 = null;
    private JPanel jPanel3 = null;
    private JTextArea resultsTextArea = null;

    private double t1Depth = 0;
    private double paY = 0;
    private double pbY = 0;
    private double da1 = 0;
    private double daH1 = 0;
    private double db1 = 0;
    private double dbH1 = 0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetNorth = 0;
    private double offsetEast = 0;
    private double yawHR = 0;
    private String lat = "";
    private String lon = "";
    private double legacyOffsetDistance = 0;
    private double legacyTheta = 0;
    private double legacyOffsetNorth = 0;
    private double legacyOffsetEast = 0;

    private LocationType location = null;
    private boolean calculatedOnceOk = false;

    private JPanel jContentPane = null;
    private JDialog jDialog = null; // @jve:decl-index=0:visual-constraint="468,34"
    private JLabel jLabel1 = null;
    private JLabel jLabel2 = null;
    private JLabel legOffsetNLabel = null;
    private JLabel legOffsetELabel = null;
    private JLabel jLabel5 = null;
    private JLabel jLabel6 = null;
    private JLabel jLabel7 = null;
    private JLabel legOffsetDLabel = null;
    private JLabel legAziLabel = null;
    private JLabel legPsiLabel = null;
    private JPanel jContentPane1 = null;
    private JFrame jFrame = null;

    /**
     * 
     */
    public TranspondersPositionHelper(CoordinateSystem cs) {
        super();
        lat = cs.getLatitudeStr();
        lon = cs.getLongitudeStr();
        yawHR = cs.getYaw();
        double[] cyl = CoordinateUtil.sphericalToCylindricalCoordinates(cs.getOffsetDistance(), cs.getAzimuth(),
                cs.getZenith());
        legacyOffsetDistance = MathMiscUtils.round(cyl[0], 3);
        legacyTheta = MathMiscUtils.round(Math.toDegrees(cyl[1]), 3);
        legacyOffsetNorth = cs.getOffsetNorth();
        legacyOffsetEast = cs.getOffsetEast();
        initialize();
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getTransPanel() {
        if (transPanel == null) {
            transPanel = new JPanel();
            transPanel.setLayout(null);
            transPanel.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Transponders Location Helper"),
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION, null, null));
            transPanel.add(getImagePanel(), null);
            transPanel.add(getJPanel(), null);
            transPanel.add(getJPanel1(), null);
            transPanel.add(getJPanel2(), null);
            transPanel.add(getJPanel3(), null);
        }
        return transPanel;
    }

    /**
     * This method initializes imagePanel
     * 
     * @return pt.lsts.neptus.gui.ImagePanel
     */
    private ImagePanel getImagePanel() {
        if (imagePanel == null) {
            Image image = ImageUtils.getImage("images/transloc.png");
            imagePanel = new ImagePanel(image);
            imagePanel.setPreferredSize(new java.awt.Dimension(232, 169));
            imagePanel.setLocation(9, 19);
            imagePanel.setSize(232, 169);
        }
        return imagePanel;
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setSize(423, 560);
        this.add(getTransPanel(), null);
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel() {
        if (jPanel == null) {
            legPsiLabel = new JLabel();
            legAziLabel = new JLabel();
            legOffsetDLabel = new JLabel();
            jLabel7 = new JLabel();
            jLabel6 = new JLabel();
            jLabel5 = new JLabel();
            legOffsetELabel = new JLabel();
            legOffsetNLabel = new JLabel();
            jLabel2 = new JLabel();
            jLabel1 = new JLabel();
            jPanel = new JPanel();
            lonLabel = new JLabel();
            lonValue = new JLabel();
            latLabel = new JLabel();
            latValue = new JLabel();
            jPanel.setLayout(null);
            jPanel.setBounds(251, 19, 160, 167);
            jPanel.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Axis origin (Home Ref.)"),
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION, null, null));
            latLabel.setText(I18n.text("Lat"));
            latLabel.setBounds(5, 19, 21, 19);
            latValue.setBounds(30, 19, 128, 19);
            latValue.setText(lat);
            lonValue.setBounds(30, 43, 128, 19);
            lonValue.setText(lon);
            lonLabel.setBounds(5, 43, 21, 19);
            lonLabel.setText(I18n.text("Lon"));
            jLabel1.setBounds(5, 64, 45, 19);
            jLabel1.setText(I18n.text("OffsetN"));
            jLabel2.setBounds(5, 86, 45, 19);
            jLabel2.setText(I18n.text("OffsetE"));
            legOffsetNLabel.setBounds(55, 62, 104, 20);
            legOffsetNLabel.setText(Double.toString(legacyOffsetNorth));
            legOffsetELabel.setBounds(55, 86, 105, 20);
            legOffsetELabel.setText(Double.toString(legacyOffsetEast));
            jLabel5.setBounds(5, 108, 45, 15);
            jLabel5.setText(I18n.text("Off.Dist"));
            jLabel6.setBounds(5, 127, 48, 17);
            jLabel6.setText(I18n.text("Azimuth"));
            jLabel7.setBounds(5, 146, 32, 14);
            jLabel7.setText(I18n.text("Psi"));
            legOffsetDLabel.setBounds(55, 108, 106, 17);
            legOffsetDLabel.setText(Double.toString(legacyOffsetDistance));
            legAziLabel.setBounds(55, 128, 104, 15);
            legAziLabel.setText(Double.toString(legacyTheta) + "\u00B0");
            legPsiLabel.setBounds(55, 146, 105, 17);
            legPsiLabel.setText(Double.toString(yawHR) + "\u00B0");
            jPanel.add(lonLabel, null);
            jPanel.add(latLabel, null);
            jPanel.add(latValue, null);
            jPanel.add(lonValue, null);
            jPanel.add(jLabel1, null);
            jPanel.add(jLabel2, null);
            jPanel.add(legOffsetNLabel, null);
            jPanel.add(legOffsetELabel, null);
            jPanel.add(jLabel5, null);
            jPanel.add(jLabel6, null);
            jPanel.add(jLabel7, null);
            jPanel.add(legOffsetDLabel, null);
            jPanel.add(legAziLabel, null);
            jPanel.add(legPsiLabel, null);
        }
        return jPanel;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel1() {
        if (jPanel1 == null) {
            jPanel1 = new JPanel();
            jLabel = new JLabel();
            jPanel1.setLayout(null);
            jPanel1.setBounds(10, 196, 402, 230);
            jPanel1.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Inputs"),
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION, null, null));
            jLabel.setText(I18n.text("Transponder depth (m):"));
            jLabel.setBounds(10, 19, 135, 16);
            jPanel1.add(jLabel, null);
            jPanel1.add(getT1DepthValue(), null);
            jPanel1.add(getPointPa(), null);
            jPanel1.add(getPointPb(), null);
            jPanel1.add(getDistancies(), null);
        }
        return jPanel1;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getT1DepthValue() {
        if (t1DepthValue == null) {
            t1DepthValue = new JTextField();
            t1DepthValue.setBounds(150, 18, 84, 20);
            t1DepthValue.setText("0");
        }
        return t1DepthValue;
    }

    /**
     * This method initializes jPanel2
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getPointPa() {
        if (pointPa == null) {
            pointPa = new JPanel();
            pointPaXLabel = new JLabel();
            pointPaYLabel = new JLabel();
            pointPa.setBounds(7, 43, 384, 56);
            pointPa.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Point Pa"),
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION, null, null));
            pointPaXLabel.setText(I18n.text("x (m):"));
            pointPaXLabel.setPreferredSize(new java.awt.Dimension(35, 19));
            pointPaYLabel.setText("      " + I18n.text("y (m):"));
            pointPaYLabel.setPreferredSize(new java.awt.Dimension(50, 19));
            pointPa.add(pointPaXLabel, null);
            pointPa.add(getPointPaX(), null);
            pointPa.add(pointPaYLabel, null);
            pointPa.add(getPointPaY(), null);
        }
        return pointPa;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getPointPaX() {
        if (pointPaX == null) {
            pointPaX = new JTextField();
            pointPaX.setPreferredSize(new Dimension(84, 20));
            pointPaX.setText("0");
            pointPaX.setEnabled(true);
            pointPaX.setEditable(false);
        }
        return pointPaX;
    }

    /**
     * This method initializes jTextField1
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getPointPaY() {
        if (pointPaY == null) {
            pointPaY = new JTextField();
            pointPaY.setPreferredSize(new java.awt.Dimension(84, 20));
            pointPaY.setText("0");
        }
        return pointPaY;
    }

    /**
     * This method initializes jPanel2
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getPointPb() {
        if (pointPb == null) {
            pointPb = new JPanel();
            pointPbXLabel = new JLabel();
            pointPbYLabel = new JLabel();
            pointPb.setBounds(7, 104, 384, 56);
            pointPb.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Point Pb"),
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION, null, null));
            pointPbXLabel.setText(I18n.text("x (m):"));
            pointPbXLabel.setPreferredSize(new java.awt.Dimension(35, 19));
            pointPbYLabel.setText("      " + I18n.text("y (m):"));
            pointPbYLabel.setPreferredSize(new java.awt.Dimension(50, 19));
            pointPb.add(pointPbXLabel, null);
            pointPb.add(getPointPbX(), null);
            pointPb.add(pointPbYLabel, null);
            pointPb.add(getPointPbY(), null);
        }
        return pointPb;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getPointPbX() {
        if (pointPbX == null) {
            pointPbX = new JTextField();
            pointPbX.setPreferredSize(new java.awt.Dimension(84, 20));
            pointPbX.setText("0");
            pointPbX.setEnabled(true);
            pointPbX.setEditable(false);
        }
        return pointPbX;
    }

    /**
     * This method initializes jTextField1
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getPointPbY() {
        if (pointPbY == null) {
            pointPbY = new JTextField();
            pointPbY.setPreferredSize(new java.awt.Dimension(84, 20));
            pointPbY.setText("0");
        }
        return pointPbY;
    }

    /**
     * This method initializes jPanel2
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getDistancies() {
        if (distances == null) {
            distances = new JPanel();
            da1Label = new JLabel();
            db1Label = new JLabel();
            distances.setBounds(7, 165, 384, 56);
            distances.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Distances"),
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION, null, null));
            da1Label.setText(I18n.text("da1 (m):"));
            da1Label.setPreferredSize(new java.awt.Dimension(50, 19));
            db1Label.setText("     " + I18n.text("db1 (m):"));
            db1Label.setPreferredSize(new java.awt.Dimension(65, 19));
            distances.add(da1Label, null);
            distances.add(getDa1Value(), null);
            distances.add(db1Label, null);
            distances.add(getDb1Value(), null);
        }
        return distances;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getDa1Value() {
        if (da1Value == null) {
            da1Value = new JTextField();
            da1Value.setPreferredSize(new java.awt.Dimension(84, 20));
            da1Value.setText("0");
        }
        return da1Value;
    }

    /**
     * This method initializes jTextField1
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getDb1Value() {
        if (db1Value == null) {
            db1Value = new JTextField();
            db1Value.setPreferredSize(new java.awt.Dimension(84, 20));
            db1Value.setText("0");
        }
        return db1Value;
    }

    /**
     * This method initializes jPanel2
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel2() {
        if (jPanel2 == null) {
            jPanel2 = new JPanel();
            FlowLayout flowLayout1 = new FlowLayout();
            jPanel2.setLayout(flowLayout1);
            jPanel2.setBounds(13, 510, 402, 39);
            flowLayout1.setAlignment(FlowLayout.RIGHT);
            jPanel2.add(getCalcButton(), null);
            jPanel2.add(getJButton(), null);
            jPanel2.add(getJButton1(), null);
        }
        return jPanel2;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getCalcButton() {
        if (calcButton == null) {
            calcButton = new JButton();
            calcButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            calcButton.setText(I18n.text("Calculate"));
            calcButton.setPreferredSize(new java.awt.Dimension(87, 26));
            calcButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    calculate();
                }
            });
        }
        return calcButton;
    }

    /**
     * 
     */
    private boolean calculate() {
        boolean ret = collectVariables();
        if (!ret) {
            JOptionPane.showMessageDialog(getTransPanel().getParent(),
                    "<html>" + I18n.text("The calculation was not possible.<br> Some inputs are not valid!")
                            + "</html>");
            // getResultsTextArea().setText("The calculation was not possible.\n" +
            // "Some inputs are not valid!");
            return false;
        }
        daH1 = Math.sqrt(Math.pow(da1, 2) - Math.pow(t1Depth, 2));
        dbH1 = Math.sqrt(Math.pow(db1, 2) - Math.pow(t1Depth, 2));
        offsetY = (Math.pow(daH1, 2) - Math.pow(dbH1, 2) + Math.pow(pbY, 2) - Math.pow(paY, 2)) / (2 * pbY - 2 * paY);
        offsetX = Math.sqrt(Math.pow(daH1, 2) - Math.pow(offsetY - paY, 2));

        double[] offsetsIne = CoordinateUtil.bodyFrameToInertialFrame(offsetX, offsetY, 0, 0, 0, Math.toRadians(yawHR));
        offsetNorth = MathMiscUtils.round(offsetsIne[0], 3) + legacyOffsetNorth;
        offsetEast = MathMiscUtils.round(offsetsIne[1], 3) + legacyOffsetEast;

        calculatedOnceOk = true;

        presentResults();

        return true;
    }

    /**
     * 
     */
    private void presentResults() {
        String res = "";
        res += I18n.text("Lat") + ": " + lat;
        res += "    " + I18n.text("Lon") + ": " + lon;
        res += "    " + I18n.text("Depth") + ": " + t1Depth;
        res += "\n" + I18n.text("Offset north") + ": " + offsetNorth;
        res += "    " + I18n.text("Offset east") + ": " + offsetEast;
        res += "\n" + I18n.text("Offset distance") + ": " + legacyOffsetDistance;
        res += "    " + I18n.text("Azimuth") + ": " + legacyTheta + "\u00B0";
        getResultsTextArea().setText(res);
    }

    /**
     * @return
     */
    private boolean collectVariables() {
        try {
            paY = Double.parseDouble(getPointPaY().getText());
            pbY = Double.parseDouble(getPointPbY().getText());
            da1 = Double.parseDouble(getDa1Value().getText());
            db1 = Double.parseDouble(getDb1Value().getText());
            t1Depth = Double.parseDouble(getT1DepthValue().getText());
        }
        catch (NumberFormatException e) {
            NeptusLog.pub().error(this + ": " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setText(I18n.text("Ok"));
            jButton.setPreferredSize(new java.awt.Dimension(87, 26));
            jButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (calculatedOnceOk) {
                        LocationType loc = new LocationType();
                        loc.setLatitudeStr(lat);
                        loc.setLongitudeStr(lon);
                        loc.setDepth(t1Depth);
                        loc.setOffsetNorth(offsetNorth);
                        loc.setOffsetEast(offsetEast);
                        loc.setOffsetDistance(legacyOffsetDistance);
                        loc.setAzimuth(legacyTheta);
                        location = loc;
                    }
                    else
                        location = null;

                    jDialog.setVisible(false);
                    jDialog.dispose();
                }
            });
        }
        return jButton;
    }

    /**
     * This method initializes jButton1
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButton1() {
        if (jButton1 == null) {
            jButton1 = new JButton();
            jButton1.setText(I18n.text("Cancel"));
            jButton1.setPreferredSize(new java.awt.Dimension(87, 26));
            jButton1.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    location = null;
                    if (jDialog != null) {
                        jDialog.setVisible(false);
                        jDialog.dispose();
                    }
                    if (jFrame != null) {
                        jFrame.setVisible(false);
                        jFrame.dispose();
                    }
                }
            });
        }
        return jButton1;
    }

    /**
     * This method initializes jPanel3
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel3() {
        if (jPanel3 == null) {
            jPanel3 = new JPanel();
            jPanel3.setLayout(new BorderLayout());
            jPanel3.setBounds(10, 433, 402, 71);
            jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, I18n.text("Result"),
                    javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                    javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
            jPanel3.add(getResultsTextArea(), java.awt.BorderLayout.CENTER);
        }
        return jPanel3;
    }

    /**
     * This method initializes jTextArea
     * 
     * @return javax.swing.JTextArea
     */
    private JTextArea getResultsTextArea() {
        if (resultsTextArea == null) {
            resultsTextArea = new JTextArea();
            resultsTextArea.setEditable(false);
        }
        return resultsTextArea;
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
        }
        return jContentPane;
    }

    /**
     * This method initializes jDialog
     * 
     * @return javax.swing.JDialog
     */
    private JDialog getJDialog(JComponent parent) {
        Window windowParent;
        Window tmpP = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        if (tmpP != null)
            windowParent = tmpP;
        else
            windowParent = new JFrame();

        if (jDialog == null) {
            jDialog = new JDialog(windowParent);
            jDialog.setContentPane(getJContentPane());
            jDialog.setTitle(I18n.text("Triangulating transponder position"));
            jDialog.setSize(this.getWidth() + 5, this.getHeight() + 35);
            jDialog.setLayout(new BorderLayout());
            jDialog.getContentPane().add(this, BorderLayout.CENTER);
            // jDialog.setModal(true);
            jDialog.setModalityType(ModalityType.DOCUMENT_MODAL);
            jDialog.setAlwaysOnTop(false);
            GuiUtils.centerOnScreen(jDialog);
            jDialog.setResizable(false);
            jDialog.setAlwaysOnTop(false);

            jDialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    location = null;
                    jDialog.setVisible(false);
                    jDialog.dispose();
                }
            });
            jDialog.setVisible(true);
        }
        return jDialog;
    }

    /**
     * @return null if not calculated.
     */
    public LocationType getLocationType() {
        return location;
    }

    /**
     * @param homeRef
     * @return
     */
    public static LocationType showTranspondersPositionHelperDialog(CoordinateSystem homeRef, JComponent parent) {
        Window windowParent;
        Window tmpP = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        if (tmpP != null)
            windowParent = tmpP;
        else
            windowParent = new JFrame();

        CoordinateSystem cs = new CoordinateSystem();
        cs.setCoordinateSystem(homeRef);
        if (1 == JOptionPane.showConfirmDialog(windowParent,
                I18n.text("Do you want to base the triangulation on home reference?"), "", JOptionPane.YES_NO_OPTION))
            cs = CoordinateSystemPanel.showCoordinateSystemDialog(I18n.text("Set reference"), cs, parent);
        if (cs == null)
            return null;
        TranspondersPositionHelper tph = new TranspondersPositionHelper(cs);
        tph.getJDialog(parent);
        return tph.getLocationType();
    }

    /**
     * @param homeRef
     * @return
     */
    public static LocationType showTranspondersPositionHelperFrame(CoordinateSystem homeRef, JComponent parent) {
        Window windowParent;
        Window tmpP = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        if (tmpP != null)
            windowParent = tmpP;
        else
            windowParent = new JFrame();

        CoordinateSystem cs = new CoordinateSystem();
        cs.setCoordinateSystem(homeRef);
        if (1 == JOptionPane.showConfirmDialog(windowParent,
                I18n.text("Do you want to base the triangulation on home reference?"), "", JOptionPane.YES_NO_OPTION))
            cs = CoordinateSystemPanel.showCoordinateSystemDialog(I18n.text("Set reference"), cs, parent);
        if (cs == null)
            return null;
        TranspondersPositionHelper tph = new TranspondersPositionHelper(cs);
        tph.getJFrame();
        return tph.getLocationType();
        // return tph.c
    }

    /**
     * This method initializes jContentPane1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane1() {
        if (jContentPane1 == null) {
            jContentPane1 = new JPanel();
            jContentPane1.setLayout(new BorderLayout());
        }
        return jContentPane1;
    }

    /**
     * This method initializes jFrame
     * 
     * @return javax.swing.JFrame
     */
    private JFrame getJFrame() {
        if (jFrame == null) {
            jFrame = new JFrame();
            jFrame.setContentPane(getJContentPane1());
            jFrame.setTitle("");
            jFrame.setSize(this.getWidth() + 5, this.getHeight() + 35);
            jFrame.setIconImage(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/images/neptus-icon.png"))).getImage());
            jFrame.setLayout(new BorderLayout());
            jFrame.getContentPane().add(this, BorderLayout.CENTER);
            jFrame.setAlwaysOnTop(true);
            GuiUtils.centerOnScreen(jFrame);
            jFrame.setVisible(true);
            jFrame.setResizable(false);
            jFrame.setAlwaysOnTop(true);
            jFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    location = null;
                    jFrame.setVisible(false);
                    jFrame.dispose();
                }
            });

        }
        return jFrame;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // 163,5
        CoordinateSystem cs = new CoordinateSystem();
        cs.setYaw(163.5 - 90);
        // GuiUtils.testFrame(new TranspondersPositionHelper(cs), "");
        cs = CoordinateSystemPanel.showCoordinateSystemDialog("Set the HomeRef", cs, null);
        showTranspondersPositionHelperDialog(cs, null);
        // showTranspondersPositionHelperFrame(cs);
        NeptusLog.pub().info("<###>dfs");
    }
}
