/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

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
	
	public static LocationType showHorizontalLocationDialog(LocationType previousLocation, String title, boolean editable,
            Component parentComponent) {
        LocationType location = new LocationType();

        location.setLocation(previousLocation.convertToAbsoluteLatLonDepth());

        SimpleLocationPanel locPanel = new SimpleLocationPanel(location);
        locPanel.selector.setZSelectable(false);
        locPanel.getLocationDialog(title, parentComponent);
        if (locPanel.isUserCancel())
            return null; 
        return locPanel.getLocationType();
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
        lt = showHorizontalLocationDialog(lt, "Testing LocationDialog", true, null);
        NeptusLog.pub().info("<###> "+lt);
    }
}
