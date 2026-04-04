/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Nov 17, 2011
 */
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.text.NumberFormat;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.SelectAllFocusListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.util.GuiUtils;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CustomParametersPanel extends ParametersPanel {
	private static final long serialVersionUID = 6373633755033930713L;

    private NumberFormat df = GuiUtils.getNeptusDecimalFormat();

    private AbstractElement element = null;

    private boolean isCenterLocationEditable = true;
    private LocationType centerLocation = new LocationType();
    private JButton changeCenterLocationButton = null;
    private LocationPanel locationPanel = null;

    private boolean isRollEditable = false;
    private boolean isPitchEditible = false;
    private boolean isYawEditable = false;
    private double rollDegs = Double.NaN;
    private double pitchDegs = Double.NaN;
    private double yawDegs = Double.NaN;
    private JFormattedTextField rollField = null;
    private JFormattedTextField pitchField = null;
    private JFormattedTextField yawField = null;

    protected PropertySheetPanel psp = new PropertySheetPanel();

    public CustomParametersPanel(AbstractElement element, Property[] properties) {
        this(element, properties, true, false, false, false);
    }

    public CustomParametersPanel(Property[] properties) {
        this(null, properties, true, false, false, false);
    }

    public CustomParametersPanel(AbstractElement element, Property[] properties, boolean isCenterLocationEditable,
                                 boolean isRollEditable, boolean isPitchEditible, boolean isYawEditable) {
        this.isCenterLocationEditable = isCenterLocationEditable;
        this.isRollEditable = isRollEditable;
        this.isPitchEditible = isPitchEditible;
        this.isYawEditable = isYawEditable;
        this.element = element;

		psp.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        for (Property property : properties) {
            if (isCenterLocationEditable && property.getName().equals("centerLocation"))
                continue;
            else if (property.getName().equals("obstacle"))
                continue;

            psp.addProperty(property);
        }
        
        setLayout(new BorderLayout());

        JPanel generalPanel = new JPanel(new MigLayout("fillx, wrap"));
        boolean generalPanelUsable = false;

        if (isCenterLocationEditable) {
            JPanel locPanel = new JPanel(new MigLayout());
            JLabel centerLocationLabel = new JLabel(I18n.text("Center Location:"));
            locPanel.add(centerLocationLabel, "gap 5");
            locPanel.add(getChangeCenterLocationButton(), "gap 10");
            generalPanel.add(locPanel, "leading");
            generalPanelUsable = true;
            getLocationPanel().setMissionType(getMissionType());
        }

        if (isRollEditable || isPitchEditible || isYawEditable) {
            JPanel rotationPanel = new JPanel(new MigLayout("center, wrap 6", "[]5[]20[]5[]20[]5[]", "[]"));
            JLabel rollLabel = new JLabel();
            JLabel pitchLabel = new JLabel();
            JLabel yawLabel = new JLabel();
            rollLabel.setText(I18n.text("Roll:"));
            pitchLabel.setText(I18n.text("Pitch:"));
            yawLabel.setText(I18n.text("Yaw:"));
            rotationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null,
                    I18n.textf("Rotation (%angleSymbol)", CoordinateUtil.CHAR_DEGREE) ,
                    javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
                    null, null));
            rollField = getRollPitchYawField();
            pitchField = getRollPitchYawField();
            yawField = getRollPitchYawField();
            if (isRollEditable) {
                rotationPanel.add(rollLabel, "sg labels");
                rotationPanel.add(rollField, "sg values");
            }
            if (isPitchEditible) {
                rotationPanel.add(pitchLabel, "sg labels");
                rotationPanel.add(pitchField, "sg values");
            }
            if (isYawEditable) {
                rotationPanel.add(yawLabel, "sg labels");
                rotationPanel.add(yawField, "sg values");
            }

            generalPanel.add(rotationPanel, "growx");
            generalPanelUsable = true;
        }

        if (generalPanelUsable)
            add(generalPanel, BorderLayout.NORTH);
        add(psp, BorderLayout.CENTER);
	}

	public Property[] getProperties() {
	    return psp.getProperties();
	}

    @Override
	public String getErrors() {
        String[] errors = null;
        if (element != null) {
            errors = PluginUtils.validatePluginProperties(element, psp.getProperties());
        }

        if (errors == null || errors.length == 0)
            return null;

        StringBuilder ret = new StringBuilder();
        for (String err : errors)
            ret.append(err).append("\n");

        return ret.toString();
    }

    public LocationType getCenterLocation() {
        return centerLocation;
    }

    public void setCenterLocation(LocationType centerLoc) {
        this.centerLocation = centerLoc;
    }

    public double getRollDegs() {
        if (rollField == null)
            return rollDegs;
        rollDegs = Double.parseDouble(rollField.getText());
        return rollDegs;
    }

    public void setRollDegs(double rollDegs) {
        if (!isRollEditable || Double.isNaN(rollDegs))
            return;
        this.rollDegs = rollDegs;
        if (this.rollField == null)
            return;
        this.rollField.setText(String.valueOf(rollDegs));
        this.rollField.setCaretPosition(0);
    }

    public double getPitchDegs() {
        if (pitchField == null)
            return pitchDegs;
        pitchDegs = Double.parseDouble(pitchField.getText());
        return pitchDegs;
    }

    public void setPitchDegs(double pitchDegs) {
        if (!isPitchEditible || Double.isNaN(pitchDegs))
            return;
        this.pitchDegs = pitchDegs;
        if (this.pitchField == null)
            return;
        this.pitchField.setText(String.valueOf(pitchDegs));
        this.pitchField.setCaretPosition(0);
    }

    public double getYawDegs() {
        if (yawField == null)
            return yawDegs;
        yawDegs = Double.parseDouble(yawField.getText());
        return yawDegs;
    }

    public void setYawDegs(double yawDegs) {
        if (!isYawEditable || Double.isNaN(yawDegs))
            return;
        this.yawDegs = yawDegs;
        if (this.yawField == null)
            return;
        this.yawField.setText(String.valueOf(yawDegs));
        this.yawField.setCaretPosition(0);
    }

    // --------------------- UI --------------------- //

    private LocationPanel getLocationPanel() {
        if (locationPanel == null) {
            locationPanel = new LocationPanel(getMissionType());
            locationPanel.hideButtons();
            locationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
        }
        return locationPanel;
    }

    private JButton getChangeCenterLocationButton() {
        if (changeCenterLocationButton == null) {
            changeCenterLocationButton = new JButton();
            changeCenterLocationButton.setText(I18n.text("Change..."));
            changeCenterLocationButton.addActionListener(e -> {
                LocationType tmp = LocationPanel.showLocationDialog(
                        I18n.text("Set the object center location"),
                        centerLocation, getMissionType(), isEditable());
                if (tmp != null)
                    setCenterLocation(tmp);
            });
        }
        return changeCenterLocationButton;
    }

    private JFormattedTextField getRollPitchYawField() {
        JFormattedTextField rpyField = new JFormattedTextField(df);
        rpyField.setPreferredSize(new java.awt.Dimension(80,20));
        rpyField.setText("0.0");
        rpyField.setHorizontalAlignment(JTextField.LEADING);
        rpyField.addFocusListener(new SelectAllFocusListener());
        return rpyField;
    }
}