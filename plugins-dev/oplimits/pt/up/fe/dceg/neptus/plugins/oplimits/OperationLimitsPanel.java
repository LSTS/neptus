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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Sep 24, 2010
 */
package pt.up.fe.dceg.neptus.plugins.oplimits;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.ParallelepipedElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class OperationLimitsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	protected OperationLimits limits;// = new OperationLimits();
	
	JCheckBox minSpeedCheck, maxSpeedCheck, minAltitudeCheck, maxAltitudeCheck, areaCheck, maxDepthCheck, maxVRateCheck;
	JFormattedTextField minSpeedField, maxSpeedField, minAltitudeField, maxAltitudeField, maxDepthField, maxVRateField;
	MissionType mt = null;
	
	public OperationLimitsPanel(MissionType mt, boolean editArea) {
		this.mt = mt;
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		maxDepthCheck = new JCheckBox(I18n.text("Maximum Depth (m)"));
		minAltitudeCheck = new JCheckBox(I18n.text("Minimum Altitude (m)"));
		maxAltitudeCheck = new JCheckBox(I18n.text("Maximum Altitude (m)"));
		minSpeedCheck = new JCheckBox(I18n.text("Minimum Speed (m/s)"));
		maxSpeedCheck = new JCheckBox(I18n.text("Maximum Speed (m/s))"));
		areaCheck = new JCheckBox(I18n.text("Area Limits"));
		maxVRateCheck = new JCheckBox(I18n.text("Maximum Vertical Rate (m/s)"));
			
		maxDepthField = new JFormattedTextField(NumberFormat.getInstance());
		maxAltitudeField = new JFormattedTextField(NumberFormat.getInstance());
		minAltitudeField = new JFormattedTextField(NumberFormat.getInstance());
		maxSpeedField = new JFormattedTextField(NumberFormat.getInstance());
		minSpeedField = new JFormattedTextField(NumberFormat.getInstance());
		maxVRateField = new JFormattedTextField(NumberFormat.getInstance());
		
		JPanel tmp = new JPanel(new GridLayout(0, 2,2,10));
		tmp.add(maxDepthCheck);
		tmp.add(maxDepthField);
		
		tmp.add(maxAltitudeCheck);
		tmp.add(maxAltitudeField);
		
		tmp.add(minAltitudeCheck);
		tmp.add(minAltitudeField);
		
		tmp.add(maxSpeedCheck);
		tmp.add(maxSpeedField);
		
		tmp.add(minSpeedCheck);
		tmp.add(minSpeedField);
		
		tmp.add(maxVRateCheck);
		tmp.add(maxVRateField);
		
		tmp.add(areaCheck);
		
		JButton b = new JButton(I18n.text("Select..."));
		b.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent e) {				
				RectangleEditor editor = new RectangleEditor(OperationLimitsPanel.this.mt);
				if (limits.opAreaLat != null) {
					editor.pp = new ParallelepipedElement(MapGroup.getMapGroupInstance(OperationLimitsPanel.this.mt), null);
					editor.pp.setWidth(limits.opAreaWidth);
					editor.pp.setLength(limits.opAreaLength);
					editor.pp.setYawDeg(Math.toDegrees(limits.opRotationRads));
					LocationType lt = new LocationType();
					lt.setLatitude(limits.opAreaLat);
					lt.setLongitude(limits.opAreaLon);
					editor.pp.setCenterLocation(lt);
					editor.pp.setMyColor(Color.red);
					editor.btnOk.setEnabled(true);
				}
				ParallelepipedElement rectangle = editor.showDialog(OperationLimitsPanel.this);
				if (rectangle != null) {
					double lld[] = rectangle.getCenterLocation().getAbsoluteLatLonDepth();
					limits.opAreaLat = lld[0];
					limits.opAreaLon = lld[1];
					limits.opAreaLength = rectangle.getLength();
					limits.opAreaWidth = rectangle.getWidth();
					limits.opRotationRads = rectangle.getYawRad();
				}				
			}
		});
		tmp.add(b);
		if (!editArea)
		    b.setEnabled(false);
		add(tmp);		
	}
	
	public void setLimits(OperationLimits limits) {
		this.limits = limits;
		this.minSpeedCheck.setSelected(limits.getMinSpeed() != null);
		this.maxSpeedCheck.setSelected(limits.getMaxSpeed() != null);
		this.minAltitudeCheck.setSelected(limits.getMinAltitude() != null);
		this.maxAltitudeCheck.setSelected(limits.getMaxAltitude() != null);
		this.maxDepthCheck.setSelected(limits.getMaxDepth() != null);
		this.areaCheck.setSelected(limits.getOpAreaLat() != null);
		this.maxVRateCheck.setSelected(limits.getMaxVertRate() != null);
		
		this.minSpeedField.setText(limits.getMinSpeed() == null? "" : ""+limits.getMinSpeed());
		this.maxSpeedField.setText(limits.getMaxSpeed() == null? "" : ""+limits.getMaxSpeed());
		this.minAltitudeField.setText(limits.getMinAltitude() == null? "" : ""+limits.getMinAltitude());
		this.maxAltitudeField.setText(limits.getMaxAltitude() == null? "" : ""+limits.getMaxAltitude());
		this.maxDepthField.setText(limits.getMaxDepth() == null? "" : ""+limits.getMaxDepth());	
		this.maxVRateField.setText(limits.getMaxVertRate() == null? "" : ""+limits.getMaxVertRate());
	}
	
	public OperationLimits getLimits() {
		if (minSpeedCheck.isSelected())
			limits.setMinSpeed(Double.valueOf(minSpeedField.getText()));
		else
			limits.setMinSpeed(null);
		
		if (maxSpeedCheck.isSelected())
			limits.setMaxSpeed(Double.valueOf(maxSpeedField.getText()));
		else
			limits.setMaxSpeed(null);
		
		if (minAltitudeCheck.isSelected())
			limits.setMinAltitude(Double.valueOf(minAltitudeField.getText()));
		else
			limits.setMinAltitude(null);
		
		if (maxAltitudeCheck.isSelected())
			limits.setMaxAltitude(Double.valueOf(maxAltitudeField.getText()));
		else
			limits.setMaxAltitude(null);
		
        if (maxVRateCheck.isSelected())
            limits.setMaxVertRate(Double.valueOf(maxVRateField.getText()));
        else
            limits.setMaxVertRate(null);

		if (maxDepthCheck.isSelected())
			limits.setMaxDepth(Double.valueOf(maxDepthField.getText()));
		else
			limits.setMaxDepth(null);
		
		if (!areaCheck.isSelected())
			limits.opAreaLat = limits.opAreaLon = limits.opAreaLength = limits.opAreaWidth = limits.opRotationRads = null;
		
		return limits;
	}
	
	
	public static void main(String[] args) {
		GuiUtils.setLookAndFeel();
		OperationLimits limits = new OperationLimits();
		limits.setMaxSpeed(100d);
		OperationLimitsPanel panel = new OperationLimitsPanel(new MissionType(), true); 
		panel.setLimits(limits);
		
		GuiUtils.testFrame(panel);
	}
}
