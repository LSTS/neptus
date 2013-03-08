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
 * 2010/05/28
 * $Id:: AuthoritySymbol.java 9873 2013-02-06 14:51:27Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem.IMCAuthorityState;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class AuthoritySymbol extends SymbolLabel {

    private ImcSystem.IMCAuthorityState authorityType = IMCAuthorityState.NONE;
    
    protected boolean fullOrNoneOnly = false;
    
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#initialize()
	 */
	@Override
	protected void initialize() {
		setSize(10, 10);
		setPreferredSize(new Dimension(10, 10));
		super.initialize();
	}
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#setActive(boolean)
	 */
	@Override
	public void setActive(boolean active) {
	    super.setActive(active);
	    if (!active)
	        authorityType = IMCAuthorityState.OFF;
	    else {
	        if (authorityType == IMCAuthorityState.OFF)
	            authorityType = IMCAuthorityState.SYSTEM_MONITOR;
	    }
	}
	
	/**
     * @return the authorityType
     */
    public ImcSystem.IMCAuthorityState getAuthorityType() {
        return authorityType;
    }
    
    /**
     * @param authorityType the authorityType to set
     */
    public void setAuthorityType(ImcSystem.IMCAuthorityState authorityType) {
        boolean changeValue = (this.authorityType != authorityType);
        this.authorityType = authorityType;
        if (authorityType == IMCAuthorityState.OFF)
            active = false;
        else
            active = true;
        if (blinkOnChange && changeValue)
            blink(true);
    }
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
	 */
	@Override
	public void paint(Graphics2D g, JXPanel c, int width, int height) {
		Graphics2D g2 = (Graphics2D)g.create();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.scale(width/10.0, height/10.0);

		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,10,10, 0,0);
		g2.setColor(new Color(0,0,0,0));
		g2.fill(rect);
		
	    if (getAuthorityType() != ImcSystem.IMCAuthorityState.NONE) { // draw hat
	        GeneralPath sp = new GeneralPath();
	        sp.moveTo(2, 4);
            // sp.moveTo(2, 1);
            sp.lineTo(5, 1);
            sp.lineTo(8, 4);
            // sp.lineTo(8, 1);
	        g2.setColor(getActiveColor());
	        g2.draw(sp);
	    }

		if (!isActive()) {
		    GeneralPath sp = new GeneralPath();
		    sp.moveTo(8, 2);
		    sp.lineTo(2, 8);
            sp.moveTo(2, 2);
            sp.lineTo(8, 8);
		    g2.setColor(getActiveColor());
		    g2.draw(sp);
		}

		g2.setColor(getActiveColor());
		g2.translate(5, 6);

		if (!isActive() || isActive() && (getAuthorityType() == ImcSystem.IMCAuthorityState.NONE) || fullOrNoneOnly) {
		    Ellipse2D el1 = new Ellipse2D.Double(-2.5, -2.5, 5d, 5d);
		    // Ellipse2D el1 = new Ellipse2D.Double(-4, -4, 8d, 8d);
		    g2.fill(el1);
		}

		if (isActive() && !fullOrNoneOnly) {
            if (getAuthorityType() != ImcSystem.IMCAuthorityState.NONE
                    && getAuthorityType() != ImcSystem.IMCAuthorityState.OFF) {
			    String tt = "F";
			    switch (getAuthorityType()) {
                    case SYSTEM_FULL:
                        tt = "F";
                        break;
                    case SYSTEM_MONITOR:
                        tt = "M";
                        break;
                    case PAYLOAD:
                        tt = "P";
                        break;
                    case PAYLOAD_MONITOR:
                        tt = "PM";
                        break;
                    default:
                        tt = "?";
                        break;
                }

			    g2.setFont(new Font("Arial", Font.BOLD, 7));
			    Rectangle2D sB1 = g2.getFontMetrics().getStringBounds(tt, g2);
			    double sw0 = sB1.getWidth() / 2;
			    g2.drawString(tt, -Math.round(sw0), 3);
			}
		}
	}
	
	public static void main(String[] args) {
		AuthoritySymbol symb1 = new AuthoritySymbol();
		symb1.setSize(20, 50);
		symb1.setActive(true);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.BLACK);
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel, "", 300, 300);
		
		try { Thread.sleep(2000); } catch (InterruptedException e) { }
		symb1.setAuthorityType(IMCAuthorityState.SYSTEM_FULL);
		symb1.repaint();
        try { Thread.sleep(2000); } catch (InterruptedException e) { }
        symb1.setAuthorityType(IMCAuthorityState.SYSTEM_MONITOR);
        symb1.repaint();
        try { Thread.sleep(2000); } catch (InterruptedException e) { }
        symb1.setAuthorityType(IMCAuthorityState.PAYLOAD);
        symb1.repaint();
        try { Thread.sleep(2000); } catch (InterruptedException e) { }
        symb1.setAuthorityType(IMCAuthorityState.PAYLOAD_MONITOR);
        symb1.repaint();
        try { Thread.sleep(2000); } catch (InterruptedException e) { }
        symb1.setAuthorityType(IMCAuthorityState.NONE);
        symb1.repaint();
        try { Thread.sleep(2000); } catch (InterruptedException e) { }
        symb1.setActive(false);
        symb1.repaint();
        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setAuthorityType(IMCAuthorityState.SYSTEM_FULL);
//        symb1.setActive(true);
//        symb1.repaint();

	}
}
