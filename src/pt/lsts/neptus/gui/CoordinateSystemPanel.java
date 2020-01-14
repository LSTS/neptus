/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 10, 2005
 */
package pt.lsts.neptus.gui;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
/**
 * @author zecarlos
 */
public class CoordinateSystemPanel extends ParametersPanel implements ActionListener {

    private static final long serialVersionUID = 8236508411812031285L;
    private static final String HELP_IMAGE = "/images/rpy.png";
    private JPanel changeHomePanel = null;
	private JPanel jPanel1 = null;
	private JLabel jLabel = null;
	private JButton changeCenter = null;
	private JLabel jLabel1 = null;
	private JTextField yawField = null;
	private LocationType centerLocation = new LocationType();
	private CoordinateSystem originalCoordinateSystem = null;
	private JPanel controlsPanel = null;
	private JButton cancelBtn = null;
	private JButton okBtn = null;
	private boolean userCancel = false;
	private JDialog dialog = null;
	private JLabel jLabel4 = null;
    private boolean editable = true;
    private String rpyHelpImage;
    private JCheckBox applyToMisson = null;
    private CoordinateSystem oldValue = null;
	/**
	 * This method initializes
	 *
	 */
	public CoordinateSystemPanel() {
		super();
		initialize();
	}

	public CoordinateSystemPanel(CoordinateSystem cs) {
		super();
		originalCoordinateSystem = cs;

		if (cs != null)
			this.centerLocation.setLocation( cs);

		initialize();

		if (cs != null) {
			this.getYawField().setText(String.valueOf(cs.getYaw()));
			oldValue = new CoordinateSystem();
			oldValue.setLocation(cs);
			oldValue.setYaw(cs.getYaw());
			//this.getRollField().setText(String.valueOf(cs.getRoll()));
			//this.getPitchField().setText(String.valueOf(cs.getPitch()));
		}
	}

	
	@Override
    public Dimension getPreferredSize() {
		return new Dimension(400,300);
	}

	/**
	 * This method initializes this component
	 */
	private void initialize() {
	    //rpyHelpImage = ConfigFetch.resolvePath("images/rpy.png");
        rpyHelpImage = this.getClass().getResource(HELP_IMAGE).toString();
	    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setSize(376, 103);
        this.setLocation(2, 0);
        this.add(getChangeHomePanel(), null);
        this.add(getJPanel1(), null);

	}
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.mme.objects.ParametersPanel#getErrors()
	 */
	@Override
    public String getErrors() {
		try {
			//Double.parseDouble(getYawField().getText());
			//Double.parseDouble(getPitchField().getText());
			Double.parseDouble(getYawField().getText());
		}
		catch (Exception e) {
			return I18n.text("The angle offset is invalid!");
		}
		return null;
	}

	@Override
    public void actionPerformed(ActionEvent e) {

		if ("changecenter".equals(e.getActionCommand())) {
            LocationType tmp = LocationPanel.showLocationDialog(CoordinateSystemPanel.this,
                    I18n.text("Change the reference point"), centerLocation, null, editable);

			if (tmp != null)
				centerLocation = tmp;
		}
		if ("ok".equals(e.getActionCommand())) {
			if (getErrors() != null) {
				JOptionPane.showMessageDialog(dialog, getErrors(), I18n.text("Errors in the parameters"), JOptionPane.ERROR_MESSAGE);
			}
			else {			
				dialog.setVisible(false);
				dialog.dispose();
			}
		}

		if ("cancel".equals(e.getActionCommand())) {
			userCancel = true;
			dialog.setVisible(false);
			dialog.dispose();
		}
	}


	/**
	 * This method initializes jPanel
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getChangeHomePanel() {
		if (changeHomePanel == null) {
			FlowLayout flowLayout3 = new FlowLayout();
			jLabel = new JLabel();
			changeHomePanel = new JPanel();
			changeHomePanel.setLayout(flowLayout3);
			jLabel.setText("Central Location:");
			flowLayout3.setAlignment(java.awt.FlowLayout.LEFT);
			changeHomePanel.add(jLabel, null);
			changeHomePanel.add(getChangeCenter(), null);
			changeHomePanel.add(getApplyToMisson(), null);
		}
		return changeHomePanel;
	}
	/**
	 * This method initializes jPanel1
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
            TitledBorder titledBorder1 = javax.swing.BorderFactory.createTitledBorder(null, I18n.text("Axis attitude"),
                    javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                    javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null);
			jLabel4 = new JLabel();
			jLabel1 = new JLabel();
			jPanel1 = new JPanel();
			jPanel1.setEnabled(true);
			jPanel1.setBorder(titledBorder1);
			jLabel1.setText(I18n.text("Rotation over the vertical axis:"));
			jLabel4.setText(" (" + I18n.text("degrees") + ")");
			//jLabel4.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
			titledBorder1.setTitle(I18n.text("Offset Angle North"));
			jPanel1.add(jLabel1, null);
			jPanel1.add(getYawField(), null);
			jPanel1.add(jLabel4, null);
		}
		return jPanel1;
	}

	/**
	 * This method initializes jButton
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getChangeCenter() {
		if (changeCenter == null) {
			changeCenter = new JButton();
			changeCenter.setText(I18n.text("Change..."));
			changeCenter.setActionCommand("changecenter");
			changeCenter.addActionListener(this);
		}
		return changeCenter;
	}

	/**
	 * This method initializes jTextField
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getYawField() {
		if (yawField == null) {
			yawField = new JTextField("0");
			yawField.setColumns(7);
			//yawField.setToolTipText("<html><img src=\"file:" + rpyHelpImage + "\"></html>");
            yawField.setToolTipText("<html><img src=\"" + rpyHelpImage + "\"></html>");
			yawField.addFocusListener(new SelectAllFocusListener());
		}
		return yawField;
	}

	public void getDialog(String title, JComponent parent) {
	    Window windowParent;
	    if (parent == null)
	        parent = this;
        Window tmpP = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
	    if (tmpP != null)
	        windowParent = tmpP;
	    else
	        windowParent = new JFrame();
		dialog = new JDialog(windowParent, title);
		dialog.getContentPane().add(this);
		dialog.setSize(getWidth() + 5, getHeight()+80);
		//dialog.setModal(true);
		dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
		dialog.setAlwaysOnTop(false);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
            public void windowClosing(WindowEvent e) {
				userCancel = true;
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		GuiUtils.centerOnScreen(dialog);
		//this.add(getControlsPanel(), java.awt.BorderLayout.SOUTH);

		this.add(getControlsPanel());
		dialog.setVisible(true);
	}

	public CoordinateSystem getCoordinateSystem(){
		if (userCancel)
			return this.originalCoordinateSystem;
		else {
			CoordinateSystem cs = new CoordinateSystem();
			cs.setLocation(centerLocation);

			//cs.setRoll(Double.parseDouble(getRollField().getText()));
			cs.setRoll(0);
			//cs.setPitch(Double.parseDouble(getPitchField().getText()));
			cs.setPitch(0);

			cs.setYaw(Double.parseDouble(getYawField().getText()));

			if (cs.getDistanceInMeters(new LocationType(originalCoordinateSystem)) != 0 && getApplyToMisson().isSelected()) {
				//JOptionPane.showOptionDialog(this, "How to move the mission?", "Propagate...", JOptionPane.)
				//NeptusLog.pub().info("<###>Propagating...");
				// Translate all objects (maps, plans) in the mission
				MapGroup missonMaps = MapGroup.getMapGroupInstance(getMissionType());
                TreeMap<String, PlanType> plans = getMissionType().getIndividualPlansList();
				
				double[] offsets = cs.getOffsetFrom(originalCoordinateSystem);
				double offsetNorth = offsets[0], offsetEast = offsets[1], offsetDown = offsets[2];
				//NeptusLog.pub().info("<###>OffsetNorth: "+offsetNorth+", offsetEast: "+offsetEast+", offsetDown: "+offsetDown+", distance: "+cs.getDistanceInMeters(new LocationType(originalCoordinateSystem)));
				
				for (MapType map : missonMaps.maps.values()) {
					//NeptusLog.pub().info("<###>Translating map "+map.getMapID()+"...");
					NeptusLog.pub().debug("Translating map "+map.getId()+"...");
					for (AbstractElement mo : map.getObjects()) {
						//NeptusLog.pub().info("<###>\tTranslating the "+mo.getClass().getSimpleName()+" "+mo.getId()+"...");
						NeptusLog.pub().debug("\tTranslating the "+mo.getClass().getSimpleName()+" "+mo.getId()+"...");
						LocationType lt = mo.getCenterLocation();						
						//System.out.print("\t > northing: "+lt.getOffsetNorth()+", easting: "+lt.getOffsetEast()+", down: "+lt.getOffsetDown()+" ---> ");
						lt.translatePosition(offsetNorth, offsetEast, offsetDown);
						//NeptusLog.pub().info("<###>northing: "+lt.getOffsetNorth()+", easting: "+lt.getOffsetEast()+", down: "+lt.getOffsetDown()+".");
						mo.setCenterLocation(lt);
					}
					map.warnChangeListeners(new MapChangeEvent(MapChangeEvent.MAP_RESET));
				}
				
				for (PlanType plan : plans.values()) {
					NeptusLog.pub().debug("Translating plan "+plan.getId()+"...");
					for (Maneuver man :  plan.getGraph().getAllManeuvers()) {
						if (man instanceof LocatedManeuver) {
							NeptusLog.pub().debug("\tTranslating the "+man.getClass().getSimpleName()+" "+man.getId()+"...");							
							LocatedManeuver transMan = (LocatedManeuver) man;							
							transMan.translate(offsetNorth, offsetEast, offsetDown);
						}
					}
					
                    plan.getPlanElements().getPlanElements().stream()
                            .forEach(pe -> pe.translate(offsetNorth, offsetEast, offsetDown));
				}
			}
			
			return cs;
		}
	}


	public static CoordinateSystem showCoordinateSystemDialog(String title, JComponent parent) {
		CoordinateSystemPanel csp = new CoordinateSystemPanel();
		csp.getDialog(title, parent);
		return csp.getCoordinateSystem();
	}


	public static CoordinateSystem showCoordinateSystemDialog(String title, CoordinateSystem cs, JComponent parent) {
		CoordinateSystemPanel csp = new CoordinateSystemPanel(cs);	
		csp.getDialog(title, parent);
		return csp.getCoordinateSystem();
	}
	
	public static CoordinateSystem showHomeRefDialog(String title, HomeReference cs, JComponent parent) {
		CoordinateSystemPanel csp = new CoordinateSystemPanel(cs);
		csp.setMissionType(cs.getMission());
		csp.getDialog(title, parent);
		return csp.getCoordinateSystem();
	}

	public static CoordinateSystem showCoordinateSystemDialogNoEdit(String title, CoordinateSystem cs, JComponent parent) {
		CoordinateSystemPanel csp = new CoordinateSystemPanel(cs);
		csp.setEditable(false);
		csp.getDialog(title, parent);
		return csp.getCoordinateSystem();
	}

	/**
	 * This method initializes jPanel2
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getControlsPanel() {
		if (controlsPanel == null) {
			FlowLayout flowLayout2 = new FlowLayout();
			controlsPanel = new JPanel();
			controlsPanel.setLayout(flowLayout2);
			flowLayout2.setAlignment(java.awt.FlowLayout.RIGHT);
			controlsPanel.add(getOkBtn(), null);
			controlsPanel.add(getCancelBtn(), null);
		}
		return controlsPanel;
	}
	/**
	 * This method initializes jButton
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getCancelBtn() {
		if (cancelBtn == null) {
			cancelBtn = new JButton();
			cancelBtn.setText(I18n.text("Cancel"));
			cancelBtn.setActionCommand("cancel");
			cancelBtn.addActionListener(this);
		}
		return cancelBtn;
	}
	/**
	 * This method initializes jButton1
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getOkBtn() {
		if (okBtn == null) {
			okBtn = new JButton();
			okBtn.setText(I18n.text("OK"));
			okBtn.setActionCommand("ok");
			okBtn.addActionListener(this);
		}
		return okBtn;
	}


	public void setChangeHomeVisible(boolean value) {
	    getChangeHomePanel().setVisible(value);
	}

	@Override
    public void setEditable(boolean value) {
	    this.editable = value;
        getYawField().setEditable(editable);
        //getPitchField().setEditable(editable);
        //getRollField().setEditable(editable);
        getCancelBtn().setVisible(editable);
        getApplyToMisson().setEnabled(false);
	}


	private JCheckBox getApplyToMisson() {
	    if (applyToMisson == null) {
	        applyToMisson = new JCheckBox(I18n.text("Propagate"));
	        applyToMisson.setSelected(false);
	        applyToMisson.setToolTipText(I18n.text("Translate all mission accordingly to changes"));				
	    }

	    return applyToMisson;
	}

	public static void main(String args[]) {
	    CoordinateSystem cs = CoordinateSystemPanel.showCoordinateSystemDialog("Teste unitário", null);
	    NeptusLog.pub().info("<###> "+cs);
	}
}
