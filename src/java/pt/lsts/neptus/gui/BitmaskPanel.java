/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import pt.lsts.neptus.messages.Bitmask;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

public class BitmaskPanel extends JPanel {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private long oldValue = 0;
	private boolean cancelled = false;
	private JDialog dialog = null;

	private LinkedHashMap<Long, JCheckBox> checks = new LinkedHashMap<Long, JCheckBox>();
    private JPanel mainContent;		

	private BitmaskPanel(Bitmask bitmask, JDialog dialog) {
		this.oldValue = bitmask.getCurrentValue();
		this.dialog = dialog;

		setLayout(new BorderLayout());

		mainContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		for (long l : bitmask.getPossibleValues().keySet()) {
			String bitName = bitmask.getPossibleValues().get(l);
			JCheckBox check = new JCheckBox(bitName);
			check.setPreferredSize(new Dimension(130, 30));
			check.setSelected(bitmask.isSet(bitName));
			checks.put(l, check);
			mainContent.add(check);
		}
		add(mainContent, BorderLayout.CENTER);
		
        if (this.dialog != null) {
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton okBtn = new JButton("OK");
            okBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    BitmaskPanel.this.dialog.setVisible(false);
                    BitmaskPanel.this.dialog.dispose();
                }
            });

            okBtn.setPreferredSize(new Dimension(80, 25));

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancelled = true;
                    BitmaskPanel.this.dialog.setVisible(false);
                    BitmaskPanel.this.dialog.dispose();
                }
            });

            cancelBtn.setPreferredSize(new Dimension(80, 25));

            controls.add(okBtn);
            controls.add(cancelBtn);
            add(controls, BorderLayout.SOUTH);
        }

	}
	
	public void setMainComponentLayout(LayoutManager l) {
	    this.mainContent.setLayout(l);
	    this.mainContent.repaint();
	}

	public long getValue() {
		if (cancelled)
			return oldValue;
		long value = 0;
		for (long l : checks.keySet()) {
			if (checks.get(l).isSelected())
				value = value | l; 
		}
		return value;
	}
	
	public static BitmaskPanel getBitmaskPanel(Bitmask bitmask) {
	    BitmaskPanel bpanel = new BitmaskPanel(bitmask, null);
	    return bpanel;
	}

	public static Bitmask showBitmaskDialog(Bitmask bitmask) {
		
		JDialog dialog = new JDialog(ConfigFetch.getSuperParentAsFrame(), true);
		dialog.setAlwaysOnTop(true);
		BitmaskPanel bpanel = new BitmaskPanel(bitmask, dialog);
		
		dialog.setContentPane(bpanel);
		dialog.setTitle("Bitmask Editor");
		dialog.setSize(135*4, (bitmask.getPossibleValues().size()/4+2)*30+50);
		GuiUtils.centerOnScreen(dialog);
		dialog.setVisible(true);
		
		bitmask.setCurrentValue(bpanel.getValue());
		
		return bitmask; 
		
	}
}
