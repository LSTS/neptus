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
 * $Id:: BitmaskPanel.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.messages.Bitmask;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

public class BitmaskPanel extends JPanel {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private long oldValue = 0;
	private boolean cancelled = false;
	private JDialog dialog = null;

	private LinkedHashMap<Long, JCheckBox> checks = new LinkedHashMap<Long, JCheckBox>();		

	private BitmaskPanel(Bitmask bitmask, JDialog dialog) {
		this.oldValue = bitmask.getCurrentValue();
		this.dialog = dialog;

		setLayout(new BorderLayout());

		JPanel mainContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		for (long l : bitmask.getPossibleValues().keySet()) {
			String bitName = bitmask.getPossibleValues().get(l);
			JCheckBox check = new JCheckBox(bitName);
			check.setPreferredSize(new Dimension(130, 30));
			check.setSelected(bitmask.isSet(bitName));
			checks.put(l, check);
			mainContent.add(check);
		}
		add(mainContent, BorderLayout.CENTER);

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
