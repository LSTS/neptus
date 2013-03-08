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
 * 20??/??/??
 * $Id:: SimpleLocationPanel.java 9779 2013-01-28 14:48:22Z pdias         $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

@SuppressWarnings("serial")
public class SimpleLocationPanel extends JPanel {

    protected JDialog dialog = new JDialog();
	protected LocationType location = new LocationType();
	protected PointSelector selector = new PointSelector();
	protected boolean userCancel = false;
	
	protected JPanel getButtonsPanel() {
		JButton okButton = new JButton(I18n.text("OK"));
		JButton cancelButton = new JButton(I18n.text("Cancel"));
		
		okButton.setPreferredSize(new Dimension(80, 25));
		cancelButton.setPreferredSize(new Dimension(80, 25));
		
		JPanel tmp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		tmp.add(okButton);
		tmp.add(cancelButton);
		
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (getErrors() != null) {
					GuiUtils.errorMessage(SimpleLocationPanel.this, I18n.text("Invalid input"), getErrors());					
					return;
				}
				
				dialog.setVisible(false);
				dialog.dispose();
				location = selector.getLocationType();
			}
		});
		
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
				location = null;
				setUserCancel(true); 
			}
		});
		
		return tmp;
	}
	
	
	public SimpleLocationPanel(LocationType lt) {
		setLayout(new BorderLayout());
		add(selector, BorderLayout.CENTER);		
		add(getButtonsPanel(), BorderLayout.SOUTH);		
		setLocation(lt);		
	}
	
	public void setLocation(LocationType location) {
		this.location.setLocation(location);
		this.location.convertToAbsoluteLatLonDepth();
		selector.setLocationType(this.location);
	}
	
	public static LocationType showLocationDialog(LocationType previousLocation, String title, boolean editable,
	        Component parentComponent) {
		LocationType location = new LocationType();

		location.setLocation(previousLocation.convertToAbsoluteLatLonDepth());

		SimpleLocationPanel locPanel = new SimpleLocationPanel(location);
		locPanel.getLocationDialog(title, parentComponent);
		if (locPanel.isUserCancel())
			return null; 
		return locPanel.getLocationType();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(410, 250);		
	}
	
	protected JDialog getLocationDialog(String title, Component parentComponent) {
		Window window = parentComponent == null ? null : SwingUtilities.windowForComponent(parentComponent);
	    dialog = new JDialog(window);
	    dialog.setTitle(title);
		dialog.setSize(getPreferredSize());
		dialog.setLayout(new BorderLayout());
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
			    
				location = null;
				setUserCancel(true);
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		dialog.setAlwaysOnTop(true);
		GuiUtils.centerOnScreen(dialog);
		dialog.setResizable(false);
		dialog.setAlwaysOnTop(true);
		dialog.setModalityType(window != null ? ModalityType.DOCUMENT_MODAL : ModalityType.APPLICATION_MODAL);
		dialog.setVisible(true);
		
		return dialog;
	}
	
	public boolean isUserCancel() {
		return userCancel;
	}

	public void setUserCancel(boolean userCancel) {
		this.userCancel = userCancel;
	}
	
	public LocationType getLocationType() {
		if (isUserCancel())
			return null;
		else
			return selector.getLocationType();
	}
	
	protected String getErrors() {
		return selector.getErrors();
	}
	
   public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        LocationType lt = new LocationType();
        lt = showLocationDialog(lt, "Testing LocationDialog", true, null);
        System.out.println(lt);
    }
}
