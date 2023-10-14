/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * This class provides a panel that show the properties of a given PropertiesProvider
 * @author zp
 *
 */
public class PropertiesTable extends JPanel implements PropertyChangeListener {
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    PropertySheetPanel psp = new PropertySheetPanel();
	PropertiesProvider pp = null;
	JLabel title = new JLabel("");
	private final Vector<PropertyChangeListener> propertyListeners = new Vector<PropertyChangeListener>();	
	
	@Override
	public void setEnabled(boolean enabled) {
	    super.setEnabled(enabled);
	    psp.setEnabled(enabled);
	    psp.getTable().setEnabled(enabled);
	}
	
	public PropertiesTable() {
		setLayout(new BorderLayout());
		psp.setBorder(BorderFactory.createEmptyBorder());
		psp.remove(0);
		psp.addPropertySheetChangeListener(this);		
		psp.setDescriptionVisible(true);
		
		psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
		editProperties(null);
		add(psp);
		add(title, BorderLayout.NORTH);
		
	}
	
	public void setDescriptionVisible(boolean isDescriptionVisible) {
		psp.setDescriptionVisible(isDescriptionVisible);
	}
	
	public void update() {
		if (pp == null)
			return;
		
		title.setText(pp.getPropertiesDialogTitle());
		
		try {
			psp.setProperties(pp.getProperties());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
	}
	
	@SuppressWarnings("deprecation")
    public void editProperties(PropertiesProvider pp) {
		if (pp != null) {
			psp.setEditorRegistry(PropertiesEditor.getPropertyEditorRegistry());

			psp.setProperties(pp.getProperties());		
			
			title.setText(pp.getPropertiesDialogTitle());
			
		}
		else {
			psp.setProperties(new Property[] {});
			title.setText("");
		}
		psp.setEnabled(isEnabled());
		this.pp = pp;
	}
	
	@Override
    public void propertyChange(PropertyChangeEvent evt) {
		if (pp != null) {			
			pp.setProperties(psp.getProperties());
		}
		for (PropertyChangeListener pcl : propertyListeners) {
			pcl.propertyChange(evt);
		}
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(20, 20);
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(20, 20);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertiesTable pt = new PropertiesTable();
		pt.editProperties(new GeneralPreferences());

		GuiUtils.testFrame(pt, "properties table");
	}

	@Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (!propertyListeners.contains(listener))
			propertyListeners.add(listener);			
	}
	
	@Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyListeners.remove(listener);
	}
}
