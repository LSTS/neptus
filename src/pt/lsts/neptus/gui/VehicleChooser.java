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
 * 2005/02/3
 */
package pt.lsts.neptus.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Zé Carlos
 * @author pdias
 */
public class VehicleChooser extends ParametersPanel implements ActionListener {

	static final long serialVersionUID = 4;

	private JDialog dialog = null;
	private javax.swing.JPanel jContentPane = null;
	private JLabel jLabel = null;
	private JComboBox<String> vehicleList = null;
	private JButton ok = null;
	private JButton cancel = null;
	private VehicleType vehicle = null;
	private Vector<VehicleType> vehiclesVector = new Vector<VehicleType>();
	
	private Vector<VehicleType> forbiddenVehicles = null;
	
	/**
	 * @param MPlanner
	 * @param title
	 * @param modal
	 * @throws java.awt.HeadlessException
	 */
	public VehicleChooser()	throws HeadlessException {
		initialize();			
	}
	
	public VehicleChooser(Vector<VehicleType> forbiddenVehicles) throws HeadlessException {
		this.forbiddenVehicles = forbiddenVehicles;
		initialize();
	}
	
	public String getErrors() {
        // TODO Auto-generated method stub
        return null;
    }
	
	public JDialog getDialog() {
	    
		dialog = new JDialog(new JFrame(), I18n.text("Choose a vehicle"), true);
	    
	    dialog.add(this);
	    dialog.setSize(310, 200);
	    dialog.setResizable(false);
	    dialog.setAlwaysOnTop(true);
	    GuiUtils.centerOnScreen(dialog);
	    dialog.setVisible(true);
	    return dialog;
	}

	public JDialog getDialog(Component parentComponent) {
	    String title = I18n.text("Choose a vehicle");	  
	        
	    Window window = SwingUtilities.getWindowAncestor(parentComponent);
	    if (window == null && parentComponent instanceof Frame)
        	window = (Window) parentComponent;        
       
        if (window instanceof Frame) {
            dialog = new JDialog((Frame)window, title, true);	
        } 
        else {
            dialog = new JDialog((Dialog)window, title, true);
        }

        //NeptusLog.pub().info("<###>Ancestor: "+window);
        
	    dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
	    dialog.add(this);
	    dialog.setSize(310, 200);
	    dialog.setResizable(false);
	    //dialog.setAlwaysOnTop(true);
	    GuiUtils.centerOnScreen(dialog);
	    dialog.setVisible(true);
	    return dialog;
	}

	/**
	 * This method initializes jComboBox	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<String> getVehicleList() {
		if (vehicleList == null) {
		    vehicleList = new JComboBox<String>();
		    vehicleList.setBounds(45, 52, 201, 25);
			Iterator<VehicleType> it = VehiclesHolder.getVehiclesList().values().iterator();
            while(it.hasNext()) {
				Object tmp = it.next();
				try {				 
				    VehicleType tmpVehicle = (VehicleType) tmp;
				    if (forbiddenVehicles == null || !forbiddenVehicles.contains(tmpVehicle)) {
				    	vehicleList.addItem(tmpVehicle.getName());
				    	vehiclesVector.add(tmpVehicle);
				    }
					
				} catch(Exception e) {
					//TODO catch this properly
				}
			}
            if (vehiclesVector.size() == 0) {
            	GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("No more vehicles"), 
            	        I18n.text("All vehicles have been added to this mission"));
            	return null;
            }
		}
		return vehicleList;
	}
	
	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getOk() {
		if (ok == null) {
			ok = new JButton();
			ok.setText(I18n.text("OK"));
			ok.setLocation(107, 124);
			ok.setSize(80, 30);
			ok.setActionCommand("ok");
			ok.addActionListener(this);
		}
		return ok;
	}
	
	/**
	 * This method initializes jButton1	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getCancel() {
		if (cancel == null) {
			cancel = new JButton();
			cancel.setText(I18n.text("Cancel"));
			cancel.setLocation(197, 124);
			cancel.setSize(80, 30);
			cancel.setActionCommand("cancel");
			cancel.addActionListener(this);
		}
		return cancel;
	}
      	
	public void actionPerformed(ActionEvent action) {
		
		if ("ok".equals(action.getActionCommand())) {
			this.vehicle = (VehicleType) vehiclesVector.get(vehicleList.getSelectedIndex());
			//NeptusLog.pub().info("<###>The chosen vehicle is "+vt);
			
			this.setVisible(false);
			dialog.dispose();
		}

		if ("cancel".equals(action.getActionCommand())) {
			this.vehicle = null;
			this.setVisible(false);
			dialog.dispose();
		}
	}
	
	/**
	 * This method initializes this component
	 * 
	 */
	private void initialize() {
		if(jContentPane == null) {
			jLabel = new JLabel();
			this.setLayout(null);
			jLabel.setBounds(14, 19, 188, 30);
			jLabel.setText(I18n.text("Choose a vehicle" + ":"));
			this.add(jLabel, null);
			JComboBox<String> vlist = getVehicleList();
			if (vlist != null)
				this.add(vlist, null);
			this.add(getOk(), null);
			this.add(getCancel(), null);
			GuiUtils.reactEnterKeyPress(getOk());
			GuiUtils.reactEscapeKeyPress(getCancel());
		}
	}
	
    public VehicleType getVehicle() {
        return vehicle;
    }
    
    public static VehicleType showVehicleDialog(Vector<VehicleType> forbiddenVehicles) {
		VehicleChooser vc = new VehicleChooser(forbiddenVehicles);
		if (vc.vehiclesVector.isEmpty())
			return null;
		else
			vc.getDialog();
		
	    return vc.getVehicle();
	}

	public static VehicleType showVehicleDialog(Vector<VehicleType> forbiddenVehicles, Component parentComponent) {
		VehicleChooser vc = new VehicleChooser(forbiddenVehicles);
		if (vc.vehiclesVector.isEmpty())
			return null;
		else
			vc.getDialog(parentComponent);
		
	    return vc.getVehicle();
	}

	public static VehicleType showVehicleDialog() {
	    VehicleChooser vc = new VehicleChooser();
	    vc.getDialog();
	    return vc.getVehicle();
	}

	public static VehicleType showVehicleDialog(Component parentComponent) {
	    VehicleChooser vc = new VehicleChooser();
	    vc.getDialog(parentComponent);
	    return vc.getVehicle();
	}

	private static VehicleType showVehicleDialog(String initialSelection) {
	    VehicleChooser vc = new VehicleChooser();
	    vc.vehicleList.setSelectedItem(initialSelection);
	    vc.getDialog();
	    return vc.getVehicle();
	}

	private static VehicleType showVehicleDialog(String initialSelection, Component parentComponent) {
	    VehicleChooser vc = new VehicleChooser();
	    vc.vehicleList.setSelectedItem(initialSelection);
	    vc.getDialog(parentComponent);
	    return vc.getVehicle();
	}

	public static VehicleType showVehicleDialog(VehicleType initialSelection) {
	   return showVehicleDialog(initialSelection.getName());
	}

	public static VehicleType showVehicleDialog(VehicleType initialSelection, Component parentComponent) {
	    return showVehicleDialog(initialSelection.getName(), parentComponent);
	}

	static void main(String[] args) {
	    ConfigFetch.initialize();
      	VehicleChooser.showVehicleDialog();
	}
}