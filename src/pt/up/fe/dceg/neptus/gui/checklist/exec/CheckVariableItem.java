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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.gui.checklist.exec;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import pt.up.fe.dceg.neptus.gui.ChartPanel;
import pt.up.fe.dceg.neptus.gui.checklist.CheckItemPanel;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoVarIntervalItem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.EntitiesResolver;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

public class CheckVariableItem extends CheckSubItemExe implements MessageListener<MessageInfo, IMCMessage> {
    private static final long serialVersionUID = 1L;
	private CheckAutoVarIntervalItem checkSubItem;
	private ChartPanel chart;
	private Color bg = null;
	private String system;
	 
	// [0] - Message Name
	// [1] - Entity Name - '*' for all
	// [2] - Field Name
	String path[];
	
	public CheckVariableItem(String system, CheckAutoVarIntervalItem ci) {
		super();
		checkSubItem = ci;
		this.system = system;
		initialize();
		path = checkSubItem.getVarPath().split(".");
	}

	private void initialize() {
	    //this.setBorder(new LineBorder(Color.BLACK));
		//this.setBorder(new LoweredBorder());
        this.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.setMaximumSize(new Dimension(2000, 150));
		this.setMinimumSize(new Dimension(0, 150));
		this.bg = this.getBackground();
		if (checkSubItem.isChecked())
			this.setBackground(CheckItemPanel.CHECK_COLOR);

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		this.add(new JLabel(checkSubItem.getVarName()));

		this.add(getChart(), BorderLayout.CENTER);
	
		ImcMsgManager.getManager().addListener(this, system);
	}

    @Override
    public void close() {
        super.close();
        ImcMsgManager.getManager().removeListener(this);
    }

    public ChartPanel getChart() {
		if (chart == null) {
			chart = new ChartPanel();
			chart.setOpaque(false);
			// chart.setSize(new Dimension(100,200));
			chart.setPreferredSize(new Dimension(100, 200));
			chart.setBorder(BorderFactory
					.createBevelBorder(BevelBorder.LOWERED));
		}

		return chart;
	}

	
	
	

	public void setValue(double value) {
		System.err.println("Variable:"+checkSubItem.getVarName()+"\nmin:"+checkSubItem.getStartInterval()+"\nmax:"+checkSubItem.getEndInterval());
		boolean min = false;
		if (checkSubItem.getStartInterval() != null) {
			this.chart.addValue("Min.",
					System.currentTimeMillis()/*-timeZero*/, checkSubItem
							.getStartInterval().doubleValue(), Color.BLUE);
			if (checkSubItem.isStartInclusion())
				if (value >= checkSubItem.getStartInterval().doubleValue())
						min = true;
			else
				if (value > checkSubItem.getStartInterval().doubleValue())
						min = true;

		} else
			min = true;

		boolean max = false;
		if (checkSubItem.getEndInterval() != null) {
			this.chart.addValue("Max.",
					System.currentTimeMillis()/*-timeZero*/, checkSubItem
							.getEndInterval().doubleValue(), Color.BLUE);
			if(checkSubItem.isEndInclusion())
				if (value <= checkSubItem.getEndInterval().doubleValue())
					max = true;
			else
				if (value < checkSubItem.getEndInterval().doubleValue())
					max = true;
				
		} else
			max = true;

		if (min && max) {
			checkSubItem.setChecked(true);
			System.out.println("ok");
			this.setBackground(CheckItemPanel.CHECK_COLOR);
			this.chart.addValue(checkSubItem.getVarName(), System
					.currentTimeMillis()/*-timeZero*/, value, Color.GREEN);
			checkSubItem.setVarValue(value);
		} else {
			
			
			checkSubItem.setChecked(false);
			this.setBackground(bg);
			this.chart.addValue(checkSubItem.getVarName(), System
					.currentTimeMillis()/*-timeZero*/, value, Color.RED);
			checkSubItem.setVarValue(value);
		}
		this.warnCheckSubItemProviders();

	}
	
	@Override
	public boolean isCheck() {
		return checkSubItem.isChecked();
	}

    @Override
    public void onMessage(MessageInfo info, IMCMessage msg) {
        // If this is the system we want
        if(msg.getAbbrev().equals(path[0])) { 
            String entityName = EntitiesResolver.resolveName(system, msg.getHeader().getInteger("src_ent"));

            // This is the entity we want or '*' 
            if(entityName.equals(path[1]) || path[1].equals("*"))
                this.setValue(msg.getDouble(path[2]));
        }
    }
}
