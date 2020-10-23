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
 * 2009/04/30
 */
package pt.lsts.neptus.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class ClockCounter extends JPanel {

	public static final Color COLOR_BACK = new Color(190, 220, 240); //new Color(130, 160, 130);
	public static final Color COLOR_FORE = new Color(30, 30, 30);

    public static final String HOURS_SEPARATOR = I18n.textc("h", "Chronometer hour separator");
    public static final String MINUTES_SEPARATOR = I18n.textc("m", "Chronometer minutes separator");
    public static final String SECONDS_SEPARATOR = I18n.textc("s", "Chronometer seconds separator");

	protected static final Polygon SHAPE_PLAY = new Polygon(new int[] {-4,-4,-1}, new int[] {1,3,2}, 3);
	protected static final Polygon SHAPE_STOP = new Polygon(new int[] {-4,-4,-1,-1}, new int[] {1,3,3,1}, 4);
	protected static final Polygon SHAPE_PAUSE1 = new Polygon(new int[] {-4,-4,-3,-3}, new int[] {1,3,3,1}, 4);
	protected static final Polygon SHAPE_PAUSE2 = new Polygon(new int[] {-2,-2,-1,-1}, new int[] {1,3,3,1}, 4);

	public static enum ClockState {NONE, STOP, START, PAUSE};
	
	private long secs = 0;
	private ClockState state = ClockState.NONE;
    
	public ClockCounter() {
		initialize();
	}
	
	private void initialize() {
		setBackground(COLOR_BACK);
		setForeground(COLOR_FORE);
	}

	public long getSecs() {
		return secs;
	}
	
	public void setSecs(long secs) {
		this.secs = secs;
		repaint();
	}
	
	public ClockState getState() {
		return state;
	}
	
	public void setState(ClockState state) {
		this.state = state;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		int w = getWidth(), h = getHeight();
		
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Paint pt = new LinearGradientPaint(0,0,getWidth()/2,getHeight(),new float[]{0f,1f}, new Color[]{getBackground().brighter(), getBackground().darker()});
		g2d.setPaint(pt);
		RoundRectangle2D rrect = new RoundRectangle2D.Double(2,2,getWidth()-4,getHeight()-4, 10, 10);
		g2d.fill(rrect);

		g2d.setFont(new Font("Arial", Font.BOLD, 10));
		
		String tt = "00" + HOURS_SEPARATOR +"00" + MINUTES_SEPARATOR +"00" + SECONDS_SEPARATOR;
		Rectangle2D sB1 = g2d.getFontMetrics().getStringBounds(tt, g2d);

		long hr = (long) (getSecs()/60.0/60.0);
		long mi = (long) ((getSecs()/60.0)%60.0);
		long sec = (long) (getSecs()%60.0);
		String hrS = Long.toString(hr);
		String miS = Long.toString(mi);
		String secS = Long.toString(sec);
		if (hrS.length() == 1)
			hrS = "0" + hrS;
		if (miS.length() == 1)
			miS = "0" + miS;
		if (secS.length() == 1)
			secS = "0" + secS;
		String time = " " +hrS + HOURS_SEPARATOR + miS + MINUTES_SEPARATOR + secS + SECONDS_SEPARATOR;

		Rectangle2D sB2 = g2d.getFontMetrics().getStringBounds(time, g2d);

		double scale;
		double sw0 = w / sB1.getWidth();
		double sh0 = h / sB1.getHeight();
		scale = (sw0 < sh0)?sw0:sh0;
		w = (int) (w * 1/scale);
		h = (int) (h * 1/scale);
		
		//AffineTransform pre = g2d.getTransform();
		g2d.scale(scale, scale);

		g2d.translate(w/2, h/2);

		//Ellipse2D ellis = new Ellipse2D.Double(0, 0, 2, 2);
		//g2d.setColor(Color.CYAN);
		//g2d.fill(ellis);
		
		g2d.setColor(COLOR_FORE);
		g.drawString(time, (int) (-sB2.getWidth()/2), (int) (sB2.getHeight()/2));
		
		g2d.translate(w/2, -h/2);
		if (getState() == ClockState.START)
			g2d.fill(SHAPE_PLAY);
		else if (getState() == ClockState.STOP)
			g2d.fill(SHAPE_STOP);
		else if (getState() == ClockState.PAUSE) {
			g2d.fill(SHAPE_PAUSE1);
			g2d.fill(SHAPE_PAUSE2);
		}
			
		//g2d.scale(1/scale, 1/scale);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final ClockCounter cp = new ClockCounter();
		
		GuiUtils.testFrame(cp, "Testing the clock panel", 300, 100);
		TimerTask tt = new TimerTask() {
			
			int val = 0;
			
			@Override
			public void run() {
				val += 1;
				//val = (int) (System.currentTimeMillis()/1E3);
				if (val % 60 == 0)
					cp.setBackground(Color.RED);
				else
					cp.setBackground(ClockCounter.COLOR_BACK);
					
				cp.setSecs(val);
			}
		};
		
		Timer t = new Timer("Clock Counter");
		t.scheduleAtFixedRate(tt, 0, 1000);		
	}
}
