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
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * 
 * @author RJPG
 * Chart Bars Panel  
 */
public class ChartBarPanel extends JPanel{
    private static final long serialVersionUID = 1L;

    public float max=10f,min=0f;
	
	public Hashtable<Integer, Float> bars=new Hashtable<Integer, Float>();
	public Hashtable<Integer,Color> colors=new Hashtable<Integer,Color>();
	
	public Graphics2D g2=null;  
	private BufferedImage bi;
	
	protected String label="bars";
	Rectangle2D stringBounds=null; 
	protected boolean drawlabel=false;
	
	public ChartBarPanel()
	{
		super();
		initialize();
	}
	
	private void initialize() {
		this.setLayout(null);
	}
	
	public void addBar(int id,Color c,float value)
	{
		if (value>max)
			bars.put(id,max);
		else
			bars.put(id,value);
		colors.put(id,c);
		this.repaint();
	}
	
	public void setColor(int id,Color c)
	{
		colors.put(id,c);
		this.repaint();
	}
	
	public void setBar(int id,float value)
	{
		if (value>max)
			bars.put(id,max);
		else
			bars.put(id,value);
		this.repaint();
	}

	public void removeBar(int id)
	{		
		bars.remove(id);
		//	colors.remove(id);
		this.repaint();
	}
	
	@Override
	public void paint(Graphics arg0) {
		super.paint(arg0);
		update(arg0);
	}
	
	public void reset()
	{
		min=0;
		max=10;
		bars.clear();
		colors.clear();
		this.repaint();
	}
	
	public void update(Graphics arg0) {
		try {
			
			this.g2 = (Graphics2D)arg0;
	 		
	 		if (g2 == null)
	 			return;
	 		
			//this.g2  = (Graphics2D)this.getGraphics();
		 	if (bi == null || bi.getWidth() < getWidth() || bi.getHeight() < bi.getHeight())
		 	{
		 		bi = (BufferedImage)createImage(getWidth(),getHeight());		 		
		 	}
			Graphics2D g=(Graphics2D) bi.getGraphics();
			
			g.setPaintMode();
			g.clearRect(0,0,getWidth(),getHeight());
			
			int spacing=getWidth()/bars.size();
			int follow=0;
			int zero=(int)Math.abs((min*(float)getHeight())/max);
			
			float leght=max-min;
			
			Enumeration<Integer> e=bars.keys();
			while (e.hasMoreElements()) {
				int key = e.nextElement();
				
				
				float heightaux=bars.get(key).intValue()-min;
				
				
				int height=(int)((heightaux*(float)getHeight())/leght);
				
				
				g.setColor(colors.get(key));
				g.fillRect(follow,(getHeight()-height),spacing,getHeight()-zero);
				follow+=spacing;
			}
			
			if(drawlabel)
			{
				//g.setXORMode(Color.BLACK);
				g.setColor(Color.BLACK);
				Font font = new Font("Arial", Font.PLAIN,
						2 * getWidth()/ 10);
				g.setFont(font);
			
				stringBounds = g.getFontMetrics().getStringBounds(label, g);
				
				g.drawString(label, getWidth()/20, getWidth()/20
						+ (int) stringBounds.getHeight());
			}
			g2.drawImage(bi,0,0,this);
		}
		catch (Exception e) {
		    e.printStackTrace();
		}
	}
	
	public void drawImage(Graphics2D g, Image img, int imgx, int imgy,
			  int x, int y, int w, int h) {
	Graphics2D ng = (Graphics2D) g.create();
	ng.clipRect(x, y, w, h);
	ng.drawImage(img, imgx, imgy, this);
   }
	
	
	public static void main (String []arg)
	{
		ChartBarPanel bar=new ChartBarPanel();
		GuiUtils.testFrame(bar,"bargr");
		bar.addBar(1,Color.BLUE,5);
		bar.addBar(2,Color.RED,6);
		bar.addBar(3,Color.GREEN,1f);
		
		
	}

	public float getMax() {
		return max;
	}

	public void setMax(float max) {
		this.max = max;
		this.repaint();
	}

	public float getMin() {
		return min;
	}

	public void setMin(float min) {
		this.min = min;
		this.repaint();
	}

	public boolean isDrawlabel() {
		return drawlabel;
	}

	public void setDrawlabel(boolean drawlabel) {
		this.drawlabel = drawlabel;
		this.repaint();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
		this.repaint();
	}
}
