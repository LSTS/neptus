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
 * 2010/05/28
 */
package pt.lsts.neptus.gui.system;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;

import pt.lsts.neptus.gui.system.SystemDisplay.BlinkingStateEnum;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class SymbolLabel extends JXPanel implements Painter<JXPanel>, IPeriodicUpdates {

	// Blinking related variables
    protected BlinkingStateEnum blinkingState = BlinkingStateEnum.NOT_BLINKING;
	protected int blinkTime = 5000;
	private long time = -1;
	
	private int symbolWidth = 48 ;
	private int symbolHeight = 48;
	private Color color = Color.WHITE;
	private Color colorHover = Color.WHITE;
	
	protected boolean active = false;
	protected boolean blinkOnChange = true;

	private Timer timer = null;
	private TimerTask updaterTask = null;
	
	public SymbolLabel() {
		initialize();
	}
	
	protected void initialize() {
		setOpaque(false);
		this.setBackgroundPainter(this);
		this.addMouseListener(new MouseAdapter() {
			@Override
            public void mouseClicked(MouseEvent e) {
                if (SymbolLabel.this.getParent() != null) {
                    for (MouseListener ml : SymbolLabel.this.getParent().getMouseListeners()) {
                        ml.mouseClicked(e);
                    }
                }
            }
			@Override
			public void mouseEntered(MouseEvent e) {
	            if (SymbolLabel.this.isRightClickable()) {
                    SymbolLabel.this.setBorder(BorderFactory.createLineBorder(colorHover,
                            (int) (SymbolLabel.this.getWidth() * 0.05)));
                }
			}
			@Override
			public void mouseExited(MouseEvent e) {
			    SymbolLabel.this.setBorder(null);
			}
        });
	}

	/**
	 * Call this to dispose of the component. 
	 */
	public void dispose() {
	    //PeriodicUpdatesService.deregister(this);
	    revokeScheduleUpdateTask();
	}
	
	public void blink(boolean blink) {
		synchronized (blinkingState) {
		    if (blink)
		        time = System.currentTimeMillis();
//            NeptusLog.pub().info("<###>Test if PeriodicUpdatesService.register(this) " + blinkingState);
		    if (blink && (blinkingState == BlinkingStateEnum.NOT_BLINKING)) {
		        blinkingState = BlinkingStateEnum.BLINKING_NORMAL;
//		        NeptusLog.pub().info("<###>PeriodicUpdatesService.register(this)");
		        // PeriodicUpdatesService.register(this);
		        scheduleUpdateTask();
		    }
		    else if (!blink) {
		        revokeScheduleUpdateTask();
		        blinkingState = BlinkingStateEnum.NOT_BLINKING;
		    }
        }
	}
	
	/**
     * @return the blinkOnChange
     */
    public boolean isBlinkOnChange() {
        return blinkOnChange;
    }
    
    /**
     * @param blinkOnChange the blinkOnChange to set
     */
    public void setBlinkOnChange(boolean blinkOnChange) {
        this.blinkOnChange = blinkOnChange;
    }
	
	/**
	 * @return the blinkTime
	 */
	public int getBlinkTime() {
		return blinkTime;
	}
	
	/**
	 * @param blinkTime the blinkTime to set.
	 *        The 0 value will set for not stop blinking
	 */
	public void setBlinkTime(int blinkTime) {
		this.blinkTime = blinkTime;
	}

	/**
	 * @return the symbolWidth
	 */
	public int getSymbolWidth() {
		return symbolWidth;
	}

	/**
	 * @param symbolWidth the symbolWidth to set
	 */
	public void setSymbolWidth(int symbolWidth) {
		this.symbolWidth = symbolWidth;
	}

	/**
	 * @return the symbolHeight
	 */
	public int getSymbolHeight() {
		return symbolHeight;
	}

	/**
	 * @param symbolHeight the symbolHeight to set
	 */
	public void setSymbolHeight(int symbolHeight) {
		this.symbolHeight = symbolHeight;
	}

	/**
	 * @return the connected
	 */
	public boolean isActive() {
		return active;
	}
	
	/**
	 * @param active the connected to set
	 */
	public void setActive(boolean active) {
		boolean changeValue = (this.active != active);
		this.active = active;
		if (blinkOnChange && changeValue)
			blink(true);
	}
	
	/**
	 * Toggles the boolean value
	 */
	public boolean toggleActive() {
		setActive(!active);
		return active;
	}

	/**
	 * @return the color
	 */
    public Color getActiveColor() {
        synchronized (blinkingState) {
            if (blinkingState == BlinkingStateEnum.BLINKING_BRILLIANT) {
                return (color == Color.WHITE) ? color.darker() : color.brighter();
            }

            return color;
        }
	}
	
	/**
	 * @return the color
	 */
	public Color getColor() {
		return color;
	}
	
	/**
	 * @param color the color to set
	 */
	public void setColor(Color color) {
		this.color = color;
	}
	
	/**
     * @return the colorHover
     */
    public Color getColorHover() {
        return colorHover;
    }
    
    /**
     * @param colorHover the colorHover to set
     */
    public void setColorHover(Color colorHover) {
        this.colorHover = colorHover;
    }
    
	private void scheduleUpdateTask() {
	    synchronized (blinkingState) {
	        if (updaterTask == null) {
	            if (timer == null)
	                timer = new Timer(this.getClass().getSimpleName() + " updater: ", true);
	            updaterTask = createUpdaterTask();
	            timer.scheduleAtFixedRate(updaterTask, 0, 700);
	        }
        }
	}

	/**
     * @return
     */
    private TimerTask createUpdaterTask() {
        return new TimerTask() {
            @Override
            public void run() {
                synchronized (blinkingState) {
//                    NeptusLog.pub().info("<###> "+blinkingState);
                    if (blinkTime > 0 && (System.currentTimeMillis() - time > blinkTime)) {
                        blinkingState = BlinkingStateEnum.NOT_BLINKING;
                    }
                    if (blinkingState == BlinkingStateEnum.NOT_BLINKING) {
                        repaint();
                        revokeScheduleUpdateTask();
                    }
                    
                    if (blinkingState == BlinkingStateEnum.BLINKING_NORMAL)
                        blinkingState = BlinkingStateEnum.BLINKING_BRILLIANT;
                    else if (blinkingState == BlinkingStateEnum.BLINKING_BRILLIANT)
                        blinkingState = BlinkingStateEnum.BLINKING_NORMAL;
                    
                    repaint();
                } 
            }
        };
    }

    private void revokeScheduleUpdateTask() {
        synchronized (blinkingState) {
            if (updaterTask != null) {
                updaterTask.cancel();
                updaterTask = null;
            }
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
	 */
	@Override
	public long millisBetweenUpdates() {
	    synchronized (blinkingState) {
	        if (blinkTime > 0 && (System.currentTimeMillis() - time > blinkTime)) {
	            blinkingState = BlinkingStateEnum.NOT_BLINKING;
	            repaint();
	        }
	        
	        switch (blinkingState) {
	            case NOT_BLINKING:
	                return 0;
	            case BLINKING_NORMAL:
	                return 700;
	            case BLINKING_BRILLIANT:
	                return 700;
	            default:
	                return 0;
	        }
	    }
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
	 */
	@Override
	public boolean update() {
//        if (blinkingState == BlinkingStateEnum.BLINKING_BRILLIANT) {
//            NeptusLog.pub().info("<###>Update FuelLevel state: " + blinkingState + "  ::   " + (System.currentTimeMillis() - time) + "ms");
//        }

	    synchronized (blinkingState) {
	        if (blinkTime > 0 && (System.currentTimeMillis() - time > blinkTime)) {
	            blinkingState = BlinkingStateEnum.NOT_BLINKING;
	        }
	        if (blinkingState == BlinkingStateEnum.NOT_BLINKING) {
	            repaint();
	            return false;
	        }
	        
	        if (blinkingState == BlinkingStateEnum.BLINKING_NORMAL)
	            blinkingState = BlinkingStateEnum.BLINKING_BRILLIANT;
	        else
	            blinkingState = BlinkingStateEnum.BLINKING_NORMAL;
	    }
	    
	    repaint();
	    return true;
	}

	/* (non-Javadoc)
	 * @see org.jdesktop.swingx.painter.Painter#paint(java.awt.Graphics2D, java.lang.Object, int, int)
	 */
	@Override
	public void paint(Graphics2D g, JXPanel c, int width, int height) {
		Color c1 = getActiveColor();
		Graphics2D g2 = (Graphics2D)g.create();

		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,10,10, 0,0);
		g2.setColor(new Color(0,0,0,0));
		g2.fill(rect);

		g2.setColor(c1);

		//g2.scale(CONV_MILIMETER_2_PIXELS, CONV_MILIMETER_2_PIXELS);
		g2.scale(width/10, height/10);
		//g.translate(width/2, height/2);
		g2.setStroke(new BasicStroke(0.3f));
		GeneralPath sp = new GeneralPath();
		sp.moveTo(0, 4);
		sp.lineTo(4, 4);
		sp.lineTo(4, 0);
		sp.moveTo(0, 6);
		sp.lineTo(4, 6);
		sp.lineTo(4, 10);
		sp.moveTo(6, 0);
		sp.lineTo(6, 4);
		sp.lineTo(10, 4);
		sp.moveTo(6, 10);
		sp.lineTo(6, 6);
		sp.lineTo(10, 6);
		g2.draw(sp);
		
//		NeptusLog.pub().info("<###>  .... "+width+" by "+height);
	}

	/**
	 * @param g2
	 * @param string
	 */
	protected void drawText(Graphics2D g2, String text) {
		g2.setColor(getActiveColor());
		g2.setFont(new Font("Arial", Font.BOLD, 10));
		String tt = text;
		Rectangle2D sB1 = g2.getFontMetrics().getStringBounds(tt, g2);
		double sw0 = 10.0 / sB1.getWidth();
		double sh0 = 10.0 / sB1.getHeight();
		g2.translate(5, 5);
		g2.scale(sw0, sh0);			
        g2.drawString(text, (int) (-sB1.getWidth() / 2.0), (int) (sB1.getHeight() / 2.0));
	}

	/**
     * @return the isRightClickable
     */
    public boolean isRightClickable() {
        return false;
    }
	
    /**
     * To be overridden.
     * @param e
     */
    void mouseClicked(MouseEvent e) {
        // Null implementation
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SymbolLabel symb1 = new SymbolLabel();
		symb1.setSize(20, 50);
		JXPanel panel = new JXPanel();
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel);
		
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(100); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(100); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(200); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(300); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(400); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(650); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(100); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(100); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(300); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(400); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();
//
//        try { Thread.sleep(10650); } catch (InterruptedException e) { }
//        symb1.toggleActive();
//        symb1.repaint();

	}
}
