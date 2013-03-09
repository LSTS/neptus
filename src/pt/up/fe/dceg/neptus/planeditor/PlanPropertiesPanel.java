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
package pt.up.fe.dceg.neptus.planeditor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.tablelayout.TableLayout;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

import com.l2fprod.common.propertysheet.DefaultProperty;
/**
 * @author ZP
 */
public class PlanPropertiesPanel extends JPanel implements ActionListener {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LinkedHashMap<String, Maneuver> pivotTable = new LinkedHashMap<String, Maneuver>();
	private LinkedHashMap<String, DefaultProperty> props = new LinkedHashMap<String, DefaultProperty>();
	private LinkedHashMap<String, PropertyEditor> propEditors = new LinkedHashMap<String, PropertyEditor>();
	
	private static Vector<String> forbiddenProps = new Vector<String>();
	
	static {
		forbiddenProps.add("ID");
		forbiddenProps.add("Initial Maneuver");
	}
	
	public PlanPropertiesPanel(PlanType plan) {
		for (Maneuver man : plan.getGraph().getAllManeuvers()) {
			
			if (!pivotTable.containsKey(man.getType())) {
				pivotTable.put(man.getType(), (Maneuver)man.clone());
				
				for (DefaultProperty dp : man.getProperties()) {
					if (!forbiddenProps.contains(dp.getName()) && !props.containsKey(dp.getName()))
						props.put(dp.getName(), dp);					
				}
			}
		}
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		String[] keys = props.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		
		for (int i = 0; i < keys.length; i++) {
			JPanel tmp = new JPanel(new TableLayout(new double[][]{{3,150,3,150,3,90,3},{3,30,3}}));
			
			PropertyEditor editor = PropertiesEditor.getPropertyEditorRegistry().getEditor(props.get(keys[i]));
			propEditors.put(keys[i], editor);
			tmp.add (new JLabel(props.get(keys[i]).getDisplayName()),"1,1");
			editor.setValue(props.get(keys[i]).getValue());
			
			Component comp = editor.getCustomEditor();
			if (comp instanceof JFormattedTextField) {
				((JFormattedTextField)comp).setLocale(Locale.US);
			}
			tmp.add(editor.getCustomEditor(), "3,1");
			JButton apply = new JButton("Apply");
			apply.setActionCommand(keys[i]);
			apply.addActionListener(this);
			tmp.add(apply, "5,1");
			
			if (i % 2 == 1) {
				tmp.setBackground(tmp.getBackground().darker());
			}
			add(tmp);						
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String propName = e.getActionCommand();
		PropertyEditor editor = propEditors.get(propName);		
		System.out.println(propName+" = "+editor.getValue());
	}
}
