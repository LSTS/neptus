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
package pt.lsts.neptus.planeditor;

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

import com.l2fprod.common.propertysheet.DefaultProperty;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.types.mission.plan.PlanType;
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
		NeptusLog.pub().info("<###> "+propName+" = "+editor.getValue());
	}
}
