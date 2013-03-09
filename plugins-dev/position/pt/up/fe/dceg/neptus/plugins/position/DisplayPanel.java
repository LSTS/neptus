/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * ??/??/2009
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.painter.Painter;

import pt.up.fe.dceg.neptus.gui.painters.SubPanelTitlePainter;
import pt.up.fe.dceg.neptus.util.GuiUtils;


@SuppressWarnings("serial")
public class DisplayPanel extends JXLabel implements Painter<JXLabel>{

	public final static int DEFAULT_FONT_SIZE = 16;
	
	private String title = "";
	private String txt = "";
	private int fontSize = 16;
	private static Font titleFont = new Font("Sans", Font.ITALIC+Font.BOLD, 14);
	
//	private Color color = new Color(235,245,255);
	
	private SubPanelTitlePainter sPPainter = new SubPanelTitlePainter(); 
	
	//private JXLabel display = new JXLabel(text);
	
	public void setFontColor(Color color) {
		setForeground(color);
	}
	
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}
	
	public DisplayPanel(String title) {		
		super();
		this.title = title;
        setBackgroundPainter(this);
		setForeground(Color.black);
		setHorizontalTextPosition(JLabel.CENTER);
		setHorizontalAlignment(JLabel.CENTER);
		if (title != null && title.length() > 0)
			setVerticalAlignment(JLabel.BOTTOM);
		if (fontSize != 0)
			setFont(new Font("Arial", Font.BOLD, fontSize));
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		setPreferredSize(new Dimension(118, 29));
		setMinimumSize(new Dimension(10, 10));
		setSize(118, 29);
	}
	
	@Override
	public void paint(Graphics2D g, JXLabel c, int width, int height) {
//		RoundRectangle2D rect = new RoundRectangle2D.Double(2,2,c.getWidth()-4,c.getHeight()-4, 10, 10);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
        sPPainter.setTitle(title);
		sPPainter.paint(g, c, width, height);
		
//		arg0.setPaint(new LinearGradientPaint(0,0,getWidth()/2,getHeight(),new float[]{0f,1f}, new Color[]{color, color.darker()}));
//		arg0.fill(rect);
//		if (title != null) {
//			g.setFont(titleFont);
//			g.setColor(Color.blue.darker().darker());
//			g.drawString(title, 0, 10);
//		}
		
		if (txt != null) {
			
			if (fontSize != 0) {
				g.setFont(new Font("Arial", Font.BOLD, fontSize));
				g.translate(10, getHeight()-10);
			}
			else {
				Rectangle2D r = g.getFontMetrics(titleFont).getStringBounds(txt, g);
				double scale = r.getWidth()/(double)(getWidth());
				double scale2 = r.getHeight()/(double)getHeight();
				if (title != null)
					scale2 = r.getHeight()/(double)(getHeight()-10);
				scale = Math.min(1/scale, 1/scale2);
				g.translate(5, getHeight()-5);
				g.scale(scale, scale);
			}
			g.setColor(getForeground());
			g.drawString(txt, 0, 0);
		}
		
	}
	
	@Override
	public void setText(String arg0) {
		this.txt = arg0;
		repaint();
	}
	
	@Override
	public String getText() {
		return "";
	}
	
	public void setTitle(String title) {
		this.title = title;
		if (title != null && title.length() > 0)
			setVerticalAlignment(JLabel.BOTTOM);
		else
			setVerticalAlignment(JLabel.CENTER);
		repaint();
	}
	
	/**
     * @return the active
     */
    public boolean isActive() {
        return sPPainter.isActive();
    }
    
    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        sPPainter.setActive(active);
    }
	
	public static void main(String[] args) {
		OrientationIcon icon = new OrientationIcon(20,2);
		JPanel panel = new JPanel(new GridLayout(0, 1));
		DisplayPanel lat = new DisplayPanel("latitude");
		lat.setIcon(icon);
		lat.setFontSize(0);
		lat.setText("41N35'45.234''");
		panel.add(lat);
		
		OrientationIcon icon1 = new OrientationIcon(20,2);
		DisplayPanel lon = new DisplayPanel("longitude");
		lon.setIcon(icon1);
		lon.setText("8W23'11.092''");
		panel.add(lon);
		DisplayPanel alt = new DisplayPanel("depth");
		alt.setTitle(null);
		alt.setText("203.34 m");
		panel.add(alt);
		
		GuiUtils.testFrame(panel);
	}
}
