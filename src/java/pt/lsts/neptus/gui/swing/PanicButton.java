/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 14/10/2006
 */
package pt.lsts.neptus.gui.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;

import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Paulo Dias
 */
public class PanicButton extends JRoundButton {
    private static final long serialVersionUID = -4027845692644064678L;

    protected Color backgroundOutter = new Color(255,230,63); //Yellowish
    protected Color backgroundInner  = new Color(232,28,28);  //Reddish
    protected Paint backgroundOutterPaint = null, backgroundOutterPaintDisabled = null;

    protected Dimension dim = null; // For update the backgroundOutterPaint

    /**
     * 
     */
    public PanicButton() {
        super();
        setText("Abort");
        initCmp();
    }

    /**
     * @param icon
     */
    public PanicButton(Icon icon) {
        super(icon);
        initCmp();
    }

    /**
     * @param text
     */
    public PanicButton(String text) {
        super(text);
        initCmp();
    }

    /**
     * @param text
     * @param icon
     */
    public PanicButton(String text, Icon icon) {
        super(text, icon);
        initCmp();
    }

    /**
     * 
     */
    protected void initCmp() {
        setPreferredSize(new Dimension(80,80));
        initTextureOfButton();
        setBackground(backgroundInner);
        setForeground(getBackgroundOutter());
        setContentAreaFilled(false);
        this.setFocusPainted(false);
        this.addMouseListener(new MouseAdapter() {
                public void mouseExited(MouseEvent e) {
                    super.mouseExited(e);
                }

                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                }
            });
    }

    protected void initTextureOfButton() {
    	if (dim == null)
    		dim = getSize();
    	if (dim.equals(getSize()))
    		return;
    	else
    		dim = getSize();
    	Paint[] paints = createStripesEnableDisablePaints(dim, backgroundOutter);
    	backgroundOutterPaint = paints[0];
    	backgroundOutterPaintDisabled = paints[1];
        
        try {
			Graphics2D g2 = (Graphics2D) this.getGraphics();
			AffineTransform pre = g2.getTransform();
			g2.scale((double)getWidth()/80, (double)getHeight()/80);
			//Font oldF = getFont();
			Font newF = getFont().deriveFont(g2.getTransform());
			g2.setTransform(pre);
			setFont(newF);
		} catch (Exception e) {
			//e.printStackTrace();
		}
    }

    public static Paint[] createStripesEnableDisablePaints(Dimension dim, Color primeColor) {
        Paint[] paints = new Paint[2];
        paints[0] = ColorUtils.createStripesPaint(dim, primeColor);
        paints[1] = ColorUtils.createStripesPaintDisabled(dim, primeColor);
        return paints;
    }

    /**
     * @return
     */
    public Color getBackgroundOutter() {
        return backgroundOutter;
    }

    /**
     * @param background_outter
     */
    public void setBackgroundOutter(Color backgroundOutter) {
        this.backgroundOutter = backgroundOutter;
        initTextureOfButton();
    }

    boolean dp = false;
    @Override
    protected void paintComponent(Graphics g) {
    	if (dp)
    		return;
    	//NeptusLog.pub().info("<###>## " + getText());
    	dp = true;
    	initTextureOfButton();
    	dp = false;
        //NeptusLog.pub().info("<###>paint1");
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        
        //Paint outter circle
        Arc2D.Double arc = new Arc2D.Double(0, 0, getSize().width - 1,
                getSize().height - 1, 0, 360, Arc2D.CHORD);
        if (isEnabled())
        	g2.setPaint(backgroundOutterPaint);
        else
        	g2.setPaint(backgroundOutterPaintDisabled);
        g2.fill(arc);

        super.paintComponent(g);
    }
    
    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        
        //Paint Outter Circle
        Color col1 = getBackgroundOutter().darker();
        if (!getModel().isEnabled())
        	col1 = Color.LIGHT_GRAY.darker();
        Color col2 = getBackgroundOutter().brighter();
        if (!getModel().isEnabled())
        	col2 = Color.LIGHT_GRAY.brighter();
        Stroke s = new BasicStroke(3);
        g2.setStroke(s);
        Arc2D.Double arc = new Arc2D.Double(1, 1, getSize().width - 3,
                getSize().height - 3, 0, 360, Arc2D.CHORD);

        Point2D.Double pt1 = new Point2D.Double(0, 0);
        Point2D.Double pt2 = new Point2D.Double(getSize().width - 1,
                getSize().height - 1);
        GradientPaint gp = null;
        //gp = new GradientPaint(pt1, col1, pt2, col2, true);
        gp = new GradientPaint(pt1, col2, pt2, col1, true);
        g2.setPaint(gp);
        g2.draw(arc);

        super.paintBorder(g);
    }
    
    @Override
	public boolean contains(int x, int y) {
		// shape = new Ellipse2D.Float(getWidth()/4, getHeight()/4,
		//          getWidth()/2, getHeight()/2);
		shape = new Ellipse2D.Float(getWidth() / 6, getHeight() / 6,
					getWidth() * 2 / 3, getHeight() * 2 / 3);
		return shape.contains(x, y);
	}

	@Override
	protected boolean isDoubleCircle() {
		return true;
	}
    
    /**
     * @param args
     */
    public static void main(String[] args){
        GuiUtils.setLookAndFeel();
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new PanicButton("cool"));
        //frame.getContentPane().setBackground(Color.ORANGE);
        
        JButton but = new PanicButton();
        but.setPreferredSize(new Dimension(100,80));
        ((PanicButton)but).setCircular(true);
        frame.getContentPane().add(but);
        
        frame.getContentPane().add(new JButton("Hello"));
        frame.getContentPane().add(new JRoundButton("Hello"));
        
        but = new JRoundButton("Hi");
        but.setBackground(Color.YELLOW);
        frame.getContentPane().add(but);

        but = new JButton("Hi1");
        but.setBackground(Color.YELLOW);
        frame.getContentPane().add(but);

        but = new PanicButton("Disable");
        but.setEnabled(false);
        frame.getContentPane().add(but);

        but = new JRoundButton("Disable");
        but.setEnabled(false);
        frame.getContentPane().add(but);

        but = new JRoundButton("Disable");
        but.setBackground(Color.YELLOW);
        but.setEnabled(false);
        frame.getContentPane().add(but);

        but = new JButton("Disable");
        but.setEnabled(false);
        frame.getContentPane().add(but);

        but = new JButton("Disable");
        but.setBackground(Color.YELLOW);
        but.setEnabled(false);
        frame.getContentPane().add(but);

        PanicButton pb = new PanicButton();
        pb.setCircular(true);
        pb.setPreferredSize(new Dimension(200, 300));
        frame.getContentPane().add(pb);

        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
        try
        {
            Thread.sleep(2000);
            frame.getContentPane().setBackground(Color.CYAN);
            Thread.sleep(2000);
            frame.getContentPane().setBackground(Color.ORANGE);
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
