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
 * Author: José Pinto
 * 4 de Ago de 2010
 */
package pt.lsts.neptus.plugins.teleoperation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zepinto
 *
 */
public class SliderPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    protected int margin = 3, rectWidth=20;
	protected double sliderPosition = 0.5;
	protected double min = -1, max = 1, mean = 0;
	protected NumberFormat nf = GuiUtils.getNeptusDecimalFormat(1);
	protected JLabel valText = new JLabel(""), title = new JLabel();
	protected Vector<ISliderPanelListener> listeners = new Vector<ISliderPanelListener>();
	protected boolean hasFocus = false;
	
	public SliderPanel() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		setLayout(new GridLayout(1, 3));
		valText.setHorizontalAlignment(JLabel.CENTER);
		title.setHorizontalAlignment(JLabel.CENTER);
		title.setForeground(Color.blue.darker());
		valText.setText("tsd");
		add(title);
		add(valText);
		add(new JLabel(""));
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				super.mouseDragged(e);
				double val = e.getX()/(double)getWidth();
				sliderPosition = Math.min(1, Math.max(0, val));
				for (ISliderPanelListener l : listeners)
					l.SliderChanged(SliderPanel.this);
				repaint();
			}
		});
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				
				double val = e.getX()/(double)getWidth();
				sliderPosition = Math.min(1, Math.max(0, val));
				for (ISliderPanelListener l : listeners)
					l.SliderChanged(SliderPanel.this);
				repaint();
				requestFocusInWindow();
			}
		});
		
		
		setFocusable(true);
		
		addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				//NeptusLog.pub().info("<###> "+title.getText()+": focus lost!");
				hasFocus = false;
				repaint();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				//NeptusLog.pub().info("<###> "+title.getText()+": focus gained!");
				hasFocus = true;
				repaint();
			}
		});
		
		addKeyListener(new KeyAdapter() {
			/* (non-Javadoc)
			 * @see java.awt.event.KeyAdapter#keyPressed(java.awt.event.KeyEvent)
			 */
			@Override
			public void keyPressed(KeyEvent e) {
				double step = ((max-min)/100.0);
				double val = getValue();
				
				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:						
					setValue(mean);
					repaint();
					break;
				case KeyEvent.VK_LEFT:
					val -= step;
					if (val < min) val = min;
					setValue(val);
					repaint();
					break;
				case KeyEvent.VK_RIGHT:
					val += step;
					if (val > max) val = max;
					setValue(val);
					repaint();
					break;
					
				}			
			}
		});
	}
	
	public void setTitle(String title) {
		this.title.setText(title);
	}
	
	@Override
	public void paint(Graphics arg0) {
		valText.setText(nf.format((max-min)*sliderPosition+min));
		super.paint(arg0);
		Graphics2D g = (Graphics2D)arg0;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.gray);
		
		double pos = ((mean-min)/(max-min))*(getWidth()-margin*2)+margin;
		
		g.draw(new Line2D.Double(pos, 2, pos, getHeight()-2));
		if (hasFocus)
			g.setColor(new Color(100,175, 200, 200));
		else
			g.setColor(new Color(180,180, 180, 150));
		double usableWidth = getWidth()-margin*2-rectWidth;
		double rectX = sliderPosition*usableWidth+margin;
		g.fill(new RoundRectangle2D.Double(rectX,margin,20,getHeight()-margin*2, 6, 6));
		g.setColor(Color.black);
		g.draw(new RoundRectangle2D.Double(rectX,margin,20,getHeight()-margin*2, 6, 6));
	}
	
	/**
	 * @return the sliderPosition
	 */
	public double getSliderPosition() {
		return sliderPosition;
	}

	/**
	 * @param sliderPosition the sliderPosition to set
	 */
	public void setSliderPosition(double sliderPosition) {
		this.sliderPosition = sliderPosition;
	}

	/**
	 * @return the min
	 */
	public double getMin() {
		return min;
	}

	/**
	 * @param min the min to set
	 */
	public void setMin(double min) {
		this.min = min;
	}

	/**
	 * @return the max
	 */
	public double getMax() {
		return max;
	}

	/**
	 * @param max the max to set
	 */
	public void setMax(double max) {
		this.max = max;
	}

	public void setValue(double value) {
		value = Math.min(value, max);
		value = Math.max(value, min);
		sliderPosition = (value-min) / (max-min);
		for (ISliderPanelListener l : listeners)
			l.SliderChanged(SliderPanel.this);		
	}
	
	public double getValue() {
		return (max-min)*sliderPosition+min;
	}
	
	/**
	 * @return the mean
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * @param mean the mean to set
	 */
	public void setMean(double mean) {
		this.mean = mean;
	}
	
	public void addSliderListener(ISliderPanelListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	public void removeSliderListener(ISliderPanelListener listener) {
		listeners.remove(listener);
	}
	
	
	

	public static void main(String[] args) {
		JPanel testPanel = new JPanel();
		testPanel.setLayout(new GridLayout(3, 1));
		SliderPanel min = new SliderPanel(), max = new SliderPanel(), mean = new SliderPanel();
		min.setTitle("min");
		max.setTitle("max");
		mean.setTitle("mean");
		
		testPanel.add(min);
		testPanel.add(mean);
		testPanel.add(max);
		GuiUtils.testFrame(testPanel);
	}
	
	
}
