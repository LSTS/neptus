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
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * 
 * @author RJPG
 * Setble scale and display size Manometer (similar to manometer cars)
 *
 */
public class Manometer extends JPanel {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Graphics2D g2=null;  
	
	public float maxvalue=200.f;
	public float minvalue=00.f;
	public float minredlight=-190.f;
	public float maxredlight=150.f;
	public float value=10.f;
	public float step=20.f;
	public float substep=2;
	public String label="Motor 4";
	public boolean digital=true;
	
    public int precision = 0;    
    protected NumberFormat nf = GuiUtils.getNeptusDecimalFormat(precision);
    
	public boolean isDigital() {
		return digital;
	}

	public void setDigital(boolean digital) {
		this.digital = digital;
	}

	public int getDigprecision() {
		return digprecision;
	}

	public void setDigprecision(int digprecision) {
		this.digprecision = digprecision;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public float getSubstep() {
		return substep;
	}

	public void setSubstep(float substep) {
		this.substep = substep;
	}

	/**
	 * This is the default constructor
	 */
	public Manometer() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(100, 100);	
	}
	
	@Override
	public void paint(Graphics arg0) {
		super.paint(arg0);
		update(arg0);
	}
	
	private BufferedImage bi = null;//ew BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
	Graphics2D g=null;
	 public void update(Graphics arg0) {
		 
		 if (getWidth() == 0)
			 return;
		 
		 	//System.out.println("chamou newvalues");
	     	//falha a 1 acaba  
	     	//se passa à segunda condição ok -->
		 	//NumberFormat nf = GuiUtils.getNeptusDecimalFormat(precision);
	     	try {
	     		
	     		this.g2 = (Graphics2D)arg0;
	     		
	     		if (g2 == null)
	     			return;
	     		
	     		double width=this.getWidth();
	     		double height=this.getHeight();
	     		
	     		double bordery=height/20;
	     		double borderx=width/20;
	     		
	     		double axisx=width/2;
	     		double axisy=height-bordery*3;
	     		
	    		//-------------initialize gr------------------------
		     		//Image bi = (BufferedImage)createImage(width,height);
		     	if (bi == null || bi.getWidth() < width || bi.getHeight() < height)
		     			bi = (BufferedImage)createImage((int)width,(int)height);
		     		//		bi =new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		     		
		     	//	MediaTracker mt = new MediaTracker(this);
		     	//	mt.addImage(bi, 0);
		     	//	mt.waitForAll();
		     	
		     			g=(Graphics2D) bi.getGraphics();
		     	//	g.setBackground(new Color(1,1,1));
		     	//	g.setColor(new Color(1,1,1));
		     	g.clearRect(0,0,(int)width,(int)height);
		     		
		     		
		     		
		     		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		     		RenderingHints.VALUE_ANTIALIAS_ON);
		     		g.setRenderingHint(RenderingHints.KEY_RENDERING,
		     		RenderingHints.VALUE_RENDER_QUALITY);
		     		g.setPaintMode();
		     		//g.clearRect(0,0,width,height);
		     		g.translate(axisx,axisy);
		     		g.rotate(Math.toRadians(-10));
		     		//---------------------------------------------------
		     	
	     		double radius=axisy-(bordery);
	     		if(radius>(axisx-borderx))
	     			radius=axisx-borderx;
	     		double lenght=((maxvalue-minvalue));
	     		double passos=lenght/this.step;
	     		Font font=new Font("Arial", Font.PLAIN,2*(int) (radius/passos));
	     		g.setFont(font);
	     		
	     		//angle-=Math.toRadians((200*this.minvalue)/(maxvalue-minvalue));
	     			//((this.value)*Math.toRadians(200.))
	     			//		/(maxvalue-minvalue);
	     		
	     		
	     		//angle=angle-Math.toRadians(180);
	     		double angle=Math.toRadians(-180);
	     		angle+=Math.toRadians((200*(this.value-this.minvalue))/((maxvalue-minvalue)));
	     		
	     		double pointerx = (Math.cos(angle)*radius);
	     		double pointery = (Math.sin(angle)*radius);
	     		double pointerxaux = (Math.cos(angle)*radius/10);
	     		double pointeryaux = (Math.sin(angle)*radius/10);
	     		
	     		for (float d=this.minvalue;d<=this.maxvalue;d+=this.step/this.substep)
	     		{
		     		if(d<this.minredlight  || d>this.maxredlight)
	     				g.setColor( Color.red );
	     			else
	     				g.setColor( Color.black );
		     		
	     			angle=Math.toRadians(-180);
		     		
		     		angle+=Math.toRadians((200*(d-this.minvalue))/((maxvalue-minvalue)));
		     		
		     			     	
		     		pointerx=(Math.cos(angle)*radius*0.5);
		     		pointery=(Math.sin(angle)*radius*0.5);
		     		pointerxaux=(Math.cos(angle)*radius*0.525);
		     		pointeryaux=(Math.sin(angle)*radius*0.525);
		     		g.draw(new Line2D.Double(pointerxaux,pointeryaux,pointerx,pointery));
		     
		     		
		     	
	     		}
	     		
	     	for (float d=this.minvalue;d<=this.maxvalue;d+=this.step)
	     		{
	     			if(d<this.minredlight  || d>this.maxredlight)
	     				g.setColor( Color.red );
	     			else
	     				g.setColor( Color.black );
	     			angle=-Math.PI;
		     		
		     		angle+=Math.toRadians((200*(d-this.minvalue))/((maxvalue-minvalue)));
		     		
		     			     	
		     		pointerx = (Math.cos(angle)*radius*0.5);
		     		pointery = (Math.sin(angle)*radius*0.5);
		     		pointerxaux = (Math.cos(angle)*radius*0.6);
		     		pointeryaux = (Math.sin(angle)*radius*0.6);
		     		g.draw(new Line2D.Double(pointerxaux,pointeryaux,pointerx,pointery));
		     
		     		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(nf.format(d), g);
		     		
		     		pointerx=(Math.cos(angle)*radius*0.8);
		     		pointery=(Math.sin(angle)*radius*0.8);
		     		
		     		g.translate(pointerx-(int)stringBounds.getCenterX(),pointery);
		     		g.rotate(Math.toRadians(10));
		     		g.drawString(nf.format(d),0,0);
		     		g.rotate(Math.toRadians(-10));
		     		g.translate(-(pointerx-(int)stringBounds.getCenterX()),-(pointery));
	     		}
	     	
	     	
	    	g.setColor( Color.blue );
	     	if(label!=null)
	     	{
	     	Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(this.label, g);
	     	
     		g.rotate(Math.toRadians(10));
     		g.drawString(this.label,-(int)stringBounds.getCenterX(),-(int)(radius*0.25));
     		g.rotate(Math.toRadians(-10));
	     	}
	     	
	     	if(digital)
	     	{
	     		nf = GuiUtils.getNeptusDecimalFormat(digprecision);
	     		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(nf.format(this.value), g);
	     	
	     		g.rotate(Math.toRadians(10));
	     		g.drawString(nf.format(this.value),-(int)stringBounds.getCenterX(),-(int)(radius*0.1));
	     		g.rotate(Math.toRadians(-10));
	     	}

	     		
	     		angle=Math.toRadians(-180);
	     		angle+=Math.toRadians((200*(this.value-this.minvalue))/((maxvalue-minvalue)));
	     		//angle=Math.toRadians(-180);
	     		pointerx=(int)(Math.cos(angle)*radius);
	     		pointery=(int)(Math.sin(angle)*radius);
	     		
	     		
	     		
				
				
				
				
	     	
	     		//Ellipse2D.Double tmp = new Ellipse2D.Double(-2,-2, 4, 4);
				//g.draw(tmp);
				
				
				
	     		
				g.draw(new Line2D.Double(0,0,pointerx,pointery));
				
	     		//g.drawArc(borderx,bordery,width,height,-190,190);
				//System.out.println(g2);
				//System.out.println(bi);
				
				g2.drawImage(bi,0,0,this);
	     	}
	     	catch (Exception e) {
	     			e.printStackTrace();
				}
	     }
	
	public static void main (String []arg)
	{
		
		Manometer m=new Manometer();
		GuiUtils.testFrame(m,"manometre");
		
		for(int i=00;;i+=2)
		{
			//motor.reDraw();
			try {//nada
				//System.out.println("espera...");
				m.setValue(i);
				Thread.sleep(100);
				//System.out.println("esperou");
		}
			catch (Exception e){
			    e.printStackTrace();
			}
			
		}
	}

	public float getMaxvalue() {
		return maxvalue;
	}

	public void setMaxvalue(float maxvalue) {
		this.maxvalue = maxvalue;
		this.repaint();
	}

	public float getMinvalue() {
		return minvalue;
	}

	public void setMinvalue(float minvalue) {
		this.minvalue = minvalue;
		this.repaint();
		
	}

	
	public float getStep() {
		return step;
	}

	public void setStep(float step) {
		this.step = step;
		this.repaint();
		
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
		this.repaint();
	}

	public float getMaxredlight() {
		return maxredlight;
	}

	public void setMaxredlight(float maxredlight) {
		this.maxredlight = maxredlight;
	}

	public float getMinredlight() {
		return minredlight;
	}

	public void setMinredlight(float minredlight) {
		this.minredlight = minredlight;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
        nf = GuiUtils.getNeptusDecimalFormat(precision);
		this.repaint();
		
	}
	
	public int digprecision=0;
	public int getDigPrecision() {
		return digprecision;
	}

	public void setDigPrecision(int precision) {
		this.digprecision = precision;
		this.repaint();
		
	}
}
