/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 */
public class JRoundButton extends JButton {
    private static final long serialVersionUID = 1672225468260288000L;

    protected Shape shape = null;
    protected boolean pressed = false;
    
    protected boolean isCircular = false;

    /**
     * Creates a button with no set text or icon.
     */
    public JRoundButton() {
        this(null, null);
    }

    /**
     * Creates a button with an icon.
     * 
     * @param icon
     *            the Icon image to display on the button
     */
    public JRoundButton(Icon icon) {
        this(null, icon);
    }

    /**
     * Creates a button with text.
     * 
     * @param text
     *            the text of the button
     */
    public JRoundButton(String text) {
        this(text, null);
    }

    public JRoundButton(String text, Icon icon) {
        super(text, icon);

        Dimension size = getPreferredSize();
        size.width = size.height = Math.max(size.width, size.height);
        setPreferredSize(size);
        setFocusPainted(icon == null);

        setContentAreaFilled(true);
    }

    public JRoundButton(String imageURL, String toolTipText, String actionCommand) {
		super(new ImageIcon(ImageUtils.getImage(imageURL)));
		
		setToolTipText(toolTipText);
		setActionCommand(actionCommand);
	}

	protected void paintComponent(Graphics g) {
		//setCircular(isCircular());
        //NeptusLog.pub().info("<###>paint");
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(getBackground());

        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        //        RenderingHints.VALUE_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_RENDERING,
        //        RenderingHints.VALUE_RENDER_QUALITY);

        Rectangle ret = g2.getClipBounds();
        if (isDoubleCircle())
            ret = new Rectangle(getWidth() / 6, getHeight() / 6,
                    getWidth() * 2 / 3, getHeight() * 2 / 3);
        else
            ret = new Rectangle(0, 0, getWidth(), getHeight());
        
        pressed = getModel().isPressed();
        Arc2D.Double arc = new Arc2D.Double(ret.x, ret.y, ret.width - 1,
                ret.height - 1, 0, 360, Arc2D.CHORD);
        if (pressed)
            g2.setColor(getBackground().darker());
        g2.fill(arc);

        super.paintComponent(g);
        
        if (isContentAreaFilled()) {
            Color backColor = null;
            if (getParent() != null)
                backColor = getParent().getBackground();
            else
                backColor = new JButton().getBackground();
            
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OUT);
            BufferedImage buffImg = new BufferedImage(getWidth(), getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D gbi = buffImg.createGraphics();
            gbi.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            gbi.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            gbi.setComposite(ac);
            gbi.setColor(backColor);
            gbi.fillRect(0, 0, getSize().width, getSize().height);
            Color fc = getBackground().darker();
            if (!getModel().isEnabled())
            	fc = Color.GRAY;
            gbi.setColor(fc);
            gbi.fillOval(0, 0, getSize().width, getSize().height);
            g2.drawImage(buffImg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    protected void paintBorder(Graphics g) {
    	//if (!isBorderPainted())
    	//	return;
    	
        Graphics2D g2 = (Graphics2D) g;
        Rectangle ret = g2.getClipBounds();
        if (isDoubleCircle())
            ret = new Rectangle(getWidth() / 6, getHeight() / 6,
                    getWidth() * 2 / 3, getHeight() * 2 / 3);
        else
            ret = new Rectangle(0, 0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        Color col1 = getBackground().darker();
        Color col2 = getBackground().brighter();

//        if (!getModel().isEnabled())
//        	col1 = Color.LIGHT_GRAY.darker();
//        if (!getModel().isEnabled())
//        	col2 = Color.LIGHT_GRAY.brighter();

//    	if (!isBorderPainted()) {
//    		col1 = getBackground();
//            col2 = getBackground();
//    	}

        Stroke s = new BasicStroke(3);
        g2.setStroke(s);

        Arc2D.Double arc = new Arc2D.Double(ret.x + 1, ret.y + 1,
                ret.width - 3, ret.height - 3, 0, 360, Arc2D.CHORD);

        Point2D.Double pt1 = new Point2D.Double(ret.x, ret.y);
        Point2D.Double pt2 = new Point2D.Double(ret.width - 1,
                ret.height - 1);
        GradientPaint gp = null;

        if (pressed)
            gp = new GradientPaint(pt1, col1, pt2, col2, true);
        else
            gp = new GradientPaint(pt1, col2, pt2, col1, true);

        g2.setPaint(gp);
        g2.draw(arc);
    }

    public boolean contains(int x, int y) {
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new Ellipse2D.Float(0, 0, getWidth(), getHeight());
        }
        return shape.contains(x, y);
    }

    
    /**
     * @return
     */
    protected boolean isDoubleCircle() {
        return false;
    }
    
    
    /**
	 * @return the isCircular
	 */
	public boolean isCircular() {
		return isCircular;
	}

	/**
	 * @param isCircular the isCircular to set
	 */
	public void setCircular(boolean isCircular) {
		boolean oldIsCircular = this.isCircular;
		this.isCircular = isCircular;
		if (isCircular && !oldIsCircular) {
			setSize(getSize());
			setPreferredSize(getPreferredSize());
		}
	}

    @Override
    public void setBounds(int x, int y, int width, int height) {
    	//NeptusLog.pub().info("<###>SetBounds");
    	if (isCircular())
    		width = height = Math.min(width, height);
    	super.setBounds(x, y, width, height);
    }

	
	@Override
	public void setSize(Dimension d) {
		if (isCircular()) {
			int  min = (int) Math.min(d.getWidth(), d.getHeight());
			d.setSize(min, min);
		}
		super.setSize(d);
	}
	
	@Override
	public void setSize(int width, int height) {
		if (isCircular()) {
			int  min = Math.min(width, height);
			width = height = min;
		}
		super.setSize(width, height);
	}
	
	@Override
	public void setPreferredSize(Dimension preferredSize) {
		if (isCircular()) {
			int min = (int) Math.min(preferredSize.getWidth(), preferredSize
					.getHeight());
			preferredSize.setSize(min, min);
		}
		super.setPreferredSize(preferredSize);
	}
	
	public static void main(String[] args) {
		//GuiUtils.setSystemLookAndFeel();
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JRoundButton("cool", null));
        
        JRoundButton but = new JRoundButton("STOP");
        but.setSize(300, 250);
        but.setBackground(Color.YELLOW);
        frame.getContentPane().add(but);

        frame.getContentPane().add(new JButton("cool", null));
        JButton but1 = new JButton("STOP");
        but1.setSize(300, 250);
        but1.setBackground(Color.YELLOW);
        frame.getContentPane().add(but1);

        frame.setSize(150, 150);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}