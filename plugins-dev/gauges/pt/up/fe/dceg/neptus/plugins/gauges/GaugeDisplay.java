/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/09/15
 */
package pt.up.fe.dceg.neptus.plugins.gauges;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.Painter;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@SuppressWarnings("serial")
public class GaugeDisplay extends JXPanel implements Painter<JXLabel>{

	private String text = "";
	
	private JXLabel gauge = new JXLabel(text);
	private ColorMap colormap = ColorMapFactory.createRedYellowGreenColorMap();
	private double value;
	protected boolean enabled = true;
	
	public GaugeDisplay() {
		setLayout(new BorderLayout());
		gauge.setBackgroundPainter(new CompoundPainter<JXLabel>(this, new GlossPainter()));
		gauge.setHorizontalTextPosition(JLabel.CENTER);
		gauge.setHorizontalAlignment(JLabel.CENTER);
		gauge.setFont(new Font("Arial", Font.BOLD, 14));
		gauge.setForeground(Color.black);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		add(gauge, BorderLayout.CENTER);
	}
	
	@Override
	public void paint(Graphics2D arg0, JXLabel arg1, int arg2, int arg3) {
		
		//arg0.fillRect(0, 0, getWidth(), getHeight());
		Color color = colormap.getColor(value);
		double max = arg1.getWidth()-4;
		
		double width = max * value;
		
		if (!isEnabled()) {
			color = Color.gray.darker();
			width = max;	
		}
		
		RoundRectangle2D rect = new RoundRectangle2D.Double(2,2,width,arg1.getHeight()-4, 4, 4);
		arg0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		arg0.setPaint(new LinearGradientPaint(0,0,getWidth(),getHeight(),new float[]{0f,1f}, new Color[]{color, color.darker()}));
		arg0.fill(rect);
		
		rect = new RoundRectangle2D.Double(2,2,arg1.getWidth()-4,arg1.getHeight()-4, 4, 4);
		arg0.draw(rect);
		
		arg0.setColor(Color.black);
		arg0.setStroke(new BasicStroke(1.5f));
		arg0.setColor(Color.black);
		arg0.draw(rect);
	}
	
	public void setText(String text) {
		this.text = text;
		gauge.setText(text);
		repaint();
	}
	
	/**
	 * @return the colormap
	 */
	public ColorMap getColormap() {
		return colormap;
	}

	/**
	 * @param colormap the colormap to set
	 */
	public void setColormap(ColorMap colormap) {
		this.colormap = colormap;
		repaint();
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * @return the value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(double value) {
		this.value = value;
		this.text = GuiUtils.getNeptusDecimalFormat(1).format(value*100)+"%";
		gauge.setText(text);
		repaint();
	}

	
	public static void main(String[] args) {
		GuiUtils.setLookAndFeel();
		JLabel lbl = new JLabel(ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/gauges/hdd.png"));
		GaugeDisplay gd = new GaugeDisplay();
		gd.setText("10%");
		gd.setValue(0.1);
		gd.setEnabled(false);
		gd.setToolTipText("my text");
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		p.add(gd, BorderLayout.CENTER);
		p.add(lbl, BorderLayout.WEST);
		GuiUtils.testFrame(p);
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(120, 18);
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;		
	}
}
