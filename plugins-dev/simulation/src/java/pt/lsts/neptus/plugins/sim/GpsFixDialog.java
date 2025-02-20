/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Miguel Carvalho
 * 12/Feb/2025
 */
package pt.lsts.neptus.plugins.sim;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.gui.LocationCopyPastePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

public class GpsFixDialog extends JDialog {

    private SimulationActionsPlugin plugin;
    private Dimension prefSize = new Dimension(350, 160);
    private JComboBox<String> comboBox = new JComboBox<>();
    private JPanel setLatLonPanel;
    private JTextField latTextField = new JTextField(6);
    private JTextField lonTextField = new JTextField(6);

    public GpsFixDialog(SimulationActionsPlugin plugin) {
        super();
        this.plugin = plugin;
        initComponents();
        this.setAlwaysOnTop(true);
    }

    private void initComponents() {
        setLayout(new MigLayout("fill", "[]", "[]"));
        setSize(prefSize);
        this.setTitle(I18n.text("GPS Fix"));

        createComboPanel();
        createCoordinatePanel();
        createButtonsPanel();
        GuiUtils.centerOnScreen(this);
    }

    private void createComboPanel() {
        JPanel comboPanel = new JPanel(new MigLayout("insets 10 10 0 10, fill", "[][]", "[]"));
        JLabel comboLabel = new JLabel("GPS Fix to:");
        String[] options = {"Center of Map", "Coordinates"};
        comboBox = new JComboBox<>(options);
        comboPanel.add(comboLabel);
        comboPanel.add(comboBox);
        add(comboPanel, "align center, wrap");

        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboBox.getSelectedIndex() == 1) {
                    setLatLonPanel.setVisible(true);
                }
                else {
                    setLatLonPanel.setVisible(false);
                }
                revalidate();
                repaint();
            }
        });
    }

    private void createCoordinatePanel() {
        setLatLonPanel = new JPanel(new MigLayout("insets 0 10 5 10, fill", "[][]", "[]"));
        JLabel latLabel = new JLabel(I18n.text("Latitude:"));
        setLatLonPanel.add(latLabel);
        setLatLonPanel.add(latTextField, "");
        JLabel lonLabel = new JLabel(I18n.text("Longitude:"));
        setLatLonPanel.add(lonLabel);
        setLatLonPanel.add(lonTextField, "");
        LocationCopyPastePanel copyPastePanel = new LocationCopyPastePanel() {
            @Override
            public void setLocationType(LocationType locationType) {
                LocationType loc = locationType.getNewAbsoluteLatLonDepth();
                latTextField.setText(String.valueOf(loc.getLatitudeDegs()));
                latTextField.setCaretPosition(0);
                lonTextField.setText(String.valueOf(loc.getLongitudeDegs()));
                lonTextField.setCaretPosition(0);
            }

            @Override
            public LocationType getLocationType() {
                LocationType lt = null;
                try {
                    double latDeg = Double.parseDouble(latTextField.getText());
                    double lonDeg = Double.parseDouble(lonTextField.getText());
                    lt = new LocationType(latDeg, lonDeg);
                }
                catch (Exception exp) {
                    System.out.println("Unable to convert values to latitude and longitude");
                }
                return lt;
            }
        };
        copyPastePanel.setBorder(null);
        setLatLonPanel.add(copyPastePanel);
        setLatLonPanel.setVisible(false);
        add(setLatLonPanel, "align center, wrap, hidemode 3");
    }

    private void createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0 10 10 10, fill", "[][]", "[]"));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton, "sizegroup btn, gapright 5");
        buttonPanel.add(cancelButton, "sizegroup btn, gapleft 5");
        add(buttonPanel, "align center");

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (comboBox.getSelectedItem().equals("Coordinates")) {
                    String latText = latTextField.getText();
                    String lonText = lonTextField.getText();
                    if (Objects.equals(latText, "") || Objects.equals(lonText, "")) {
                        throw new IllegalArgumentException("Latitude and longitude cannot be empty when setting a position.");
                    }
                    LocationType lt = null;
                    try {
                        double latDeg = Double.parseDouble(latTextField.getText());
                        double lonDeg = Double.parseDouble(lonTextField.getText());
                        lt = new LocationType(latDeg, lonDeg);
                    }
                    catch (Exception exp) {
                        System.out.println("Unable to convert values to latitude and longitude");
                    }
                    if (lt != null) {
                        plugin.sendGpsFix(lt);
                    }
                }
                else {
                    plugin.sendGpsFixToCenter();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });

        GuiUtils.reactEnterKeyPress(okButton);
        GuiUtils.reactEscapeKeyPress(cancelButton);
    }
}
