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
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

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
