/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2010/05/28
 */
package pt.lsts.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.JXPanel;

import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class TaskSymbol extends SymbolLabel {

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.system.SymbolLabel#initialize()
	 */
	@Override
	protected void initialize() {
		setSize(10, 10);
		setPreferredSize(new Dimension(10, 10));
		super.initialize();
	}
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.gui.system.SymbolLabel#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
	 */
	@Override
	public void paint(Graphics2D g, JXPanel c, int width, int height) {
		//Color c1 = Color.BLUE.darker();
		Graphics2D g2 = (Graphics2D)g.create();
		//g2.setColor(c1);

//		RoundRectangle2D rect = new RoundRectangle2D.Double(2,2,c.getWidth()-4,c.getHeight()-4, 10, 10);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		
//		g2.setPaint(new LinearGradientPaint(0,0,getWidth(),getHeight(),new float[]{0f,1f}, new Color[]{Color.orange, Color.orange.darker()}));
//		g2.fill(rect);
//		
//		g2.setStroke(new BasicStroke(2f));
//		g2.setColor(Color.orange);
//		g2.draw(rect);

		
		//g2.scale(CONV_MILIMETER_2_PIXELS, CONV_MILIMETER_2_PIXELS);
		g2.scale(width/10.0, height/10.0);
		//g.translate(width/2, height/2);
		//g2.setStroke(new BasicStroke(1f));

		//g2.translate(5, 5);
		
		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,10,10, 0,0);
		g2.setColor(new Color(0,0,0,0));
		g2.fill(rect);
		
		if (isActive()) {
			GeneralPath sp = new GeneralPath();
			sp.moveTo(2, 8);
			sp.lineTo(8, 2);
			g2.setColor(getActiveColor());
			g2.draw(sp);
	
			g2.translate(6, 4);
			g2.rotate(Math.toRadians(45));
			sp = new GeneralPath();
			sp.moveTo(-2.5, .25);
			sp.lineTo(2.5, .25);
			sp.lineTo(2.5, -1.75);
			sp.lineTo(-2.5, -1.75);
			sp.closePath();
			g2.fill(sp);		
			g2.draw(sp);
		}

//		sp = new GeneralPath();
//		sp.moveTo(3,3);
//		sp.lineTo(5,1);
//		sp.lineTo(8, 4);
//		sp.lineTo(8, 7);
//		sp.closePath();
//		g2.fill(sp);		
//		g2.draw(sp);		

	}
	
	public static void main(String[] args) {
		TaskSymbol symb1 = new TaskSymbol();
		symb1.setSize(50, 50);
		symb1.setActive(true);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.GRAY.darker());
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel,"",400,400);

	}

}
