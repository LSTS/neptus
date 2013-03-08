/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2005/02/3
 * $Id:: VehicleChooser.java 9857 2013-02-04 15:09:29Z pdias              $:
 */
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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

        //System.out.println("Ancestor: "+window);
        
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
			//System.out.println("The chosen vehicle is "+vt);
			
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