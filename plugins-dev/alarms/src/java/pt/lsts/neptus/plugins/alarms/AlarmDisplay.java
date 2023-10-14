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
 * Author: José Pinto
 * 2009/06/06
 */
package pt.lsts.neptus.plugins.alarms;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.Painter;

import pt.lsts.neptus.console.plugins.AlarmProviderOld;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author ZP
 *
 */
@SuppressWarnings("serial")
public class AlarmDisplay extends JXPanel implements Painter<JXLabel>, IPeriodicUpdates {

	private Color color = Color.black.darker();
	private JXLabel display = new JXLabel("Heartbeat");
	private int state = AlarmProviderOld.LEVEL_NONE;
	private static final int NOT_BLINKING = 0, BLINKING_NORMAL = 1, BLINKING_BRILLIANT = 2;
	
	private int blinkingState = NOT_BLINKING;
	
	@Override
	public long millisBetweenUpdates() {
		switch (blinkingState) {
		case NOT_BLINKING:
			return 0;
		case BLINKING_NORMAL:
			return 100;
		case BLINKING_BRILLIANT:
			return 900;
		default:
			return 0;
		}
	}
	
	@Override
	public boolean update() {
		if (blinkingState == NOT_BLINKING)
			return false;
		
		if (blinkingState == BLINKING_NORMAL)
			blinkingState = BLINKING_BRILLIANT;
		else
			blinkingState = BLINKING_NORMAL;
		repaint();
		return true;
	}
	
	public void setText(String text) {
		display.setText(text);
	}
	
	private LinkedHashMap<Integer, Color> colors = new LinkedHashMap<Integer, Color>();	
	{
		colors.put(-1, Color.black);
		colors.put(-2, Color.gray.darker());
		colors.put(0, Color.green.darker());
		colors.put(1, Color.blue.darker());
		colors.put(2, Color.yellow.darker());
		colors.put(3, Color.orange.darker());
		colors.put(4, Color.red.darker());
	}
	
	public void setState(int state) {
		int oldState = this.state;
		this.state = state;
		if (oldState != state) {
			if (state == 4) {
				blinkingState = BLINKING_NORMAL;
				PeriodicUpdatesService.register(this);				
			}
			else {
				blinkingState = NOT_BLINKING;
			}
			repaint();
		}
	}
	
	public AlarmDisplay() {
		setLayout(new BorderLayout());
		display.setBackgroundPainter(new CompoundPainter<JXLabel>(this, new GlossPainter()));
		display.setHorizontalTextPosition(JLabel.CENTER);
		display.setHorizontalAlignment(JLabel.CENTER);
		display.setFont(new Font("Arial", Font.BOLD, 14));
		display.setForeground(Color.white);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		add(display, BorderLayout.CENTER);
	}
	
	@Override
	public void paint(Graphics2D arg0, JXLabel arg1, int arg2, int arg3) {
		
		color = colors.get(state);
		if (color == null)
			color = Color.black;
		
		if (blinkingState == BLINKING_BRILLIANT)
			color = color.brighter();
		
		RoundRectangle2D rect = new RoundRectangle2D.Double(2,2,arg1.getWidth()-4,arg1.getHeight()-4, 10, 10);
		arg0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		arg0.setPaint(new LinearGradientPaint(0,0,getWidth(),getHeight(),new float[]{0f,1f}, new Color[]{color, color.darker()}));
		arg0.fill(rect);
		
		arg0.setStroke(new BasicStroke(2f));
		arg0.setColor(color);
		arg0.draw(rect);
	}
	
	
	public static void main(String[] args) {
		final AlarmDisplay disp = new AlarmDisplay();
		final Timer t = new Timer("AlarmDisplay");

		
		GuiUtils.testFrame(disp);
		
		TimerTask changeColor = new TimerTask() {
			int i = 0;
			@Override
			public void run() {
				if (i < 1) {
					disp.setState(4);
					i ++;
				}
				else {
					disp.setState(4);
					i = 0;
				}
				
			}
		};
		
		t.schedule(changeColor, 1000);
	}
	
	
}
