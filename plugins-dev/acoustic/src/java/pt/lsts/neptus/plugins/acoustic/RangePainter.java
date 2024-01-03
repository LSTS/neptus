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
 * 2010/06/10
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Timer;
import java.util.TimerTask;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 * 
 */
public abstract class RangePainter implements Renderer2DPainter {
    protected Timer timer = new Timer(RangePainter.class.getSimpleName() + ":" + RangePainter.this.hashCode());
	protected TimerTask ttRange = null;
	protected LocationType curLoc = null;
	protected double range = Double.NaN;
	protected boolean accepted = true;
	protected boolean atSurface = false;
	
	private int secondsToDisplayRanges = 10;
	private Color squareColor = new Color(255, 128, 0, 100);
	private Color acceptedColor = Color.ORANGE;
	private Color rejectedColor = Color.RED;
	private Color surfaceColor = Color.BLUE;
	
    private boolean drawRangeUpOrDownThePoint = true;

	private boolean hideOrFadeRange = true;
	private boolean hideFadeTrigger = false;
    private String reason;
	
	public RangePainter(LocationType curLoc) {
		this.curLoc = curLoc;
	}

	/**
	 * @return the drawRangeUpOrDown
	 */
	public boolean isDrawRangeUpOrDownThePoint() {
		return drawRangeUpOrDownThePoint;
	}
	
	/**
	 * @param drawRangeUpOrDownThePoint the drawRangeUpOrDownThePoint to set
	 */
	public void setDrawRangeUpOrDownThePoint(boolean drawRangeUpOrDownThePoint) {
		this.drawRangeUpOrDownThePoint = drawRangeUpOrDownThePoint;
	}
	
	/**
	 * @return the secondsToDisplayRanges
	 */
	public int getSecondsToDisplayRanges() {
		return secondsToDisplayRanges;
	}
	
	/**
	 * @param secondsToDisplayRanges the secondsToDisplayRanges to set
	 */
	public void setSecondsToDisplayRanges(int secondsToDisplayRanges) {
		this.secondsToDisplayRanges = secondsToDisplayRanges;
	}
	
	/**
     * @return the hideOrFadeRange
     */
    public boolean isHideOrFadeRange() {
        return hideOrFadeRange;
    }
    
    /**
     * @param hideOrFadeRange the hideOrFadeRange to set
     */
    public void setHideOrFadeRange(boolean hideOrFadeRange) {
        this.hideOrFadeRange = hideOrFadeRange;
    }
	
	/**
	 * @return the squareColor
	 */
	public Color getSquareColor() {
		return squareColor;
	}
	
	/**
	 * @param squareColor the squareColor to set
	 */
	public void setSquareColor(Color squareColor) {
		this.squareColor = ColorUtils.setTransparencyToColor(squareColor, 100);
	}
	
	/**
	 * @return the acceptedColor
	 */
	public Color getAcceptedColor() {
		return acceptedColor;
	}
	
	/**
	 * @param acceptedColor the acceptedColor to set
	 */
	public void setAcceptedColor(Color acceptedColor) {
		this.acceptedColor = acceptedColor;
	}
	
	/**
	 * @return the rejectedColor
	 */
	public Color getRejectedColor() {
		return rejectedColor;
	}
	
	/**
	 * @param rejectedColor the rejectedColor to set
	 */
	public void setRejectedColor(Color rejectedColor) {
		this.rejectedColor = rejectedColor;
	}
	
	/**
     * @param surfaceColor the surfaceColor to set
     */
    public void setSurfaceColor(Color surfaceColor) {
        this.surfaceColor = surfaceColor;
    }
    
	/**
	 * @param reason
	 */
	public void setRejectionReason(String reason) {
	    this.reason = reason;
	}
	
	/**
	 * @param range
	 *            the range to set
	 */
	public void setRange(double range) {
		this.range = range;
	}

	/**
	 * @param accepted
	 *            the accepted to set
	 */
	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}

	
	public abstract void callParentRepaint();
	
	/**
     * @param curLoc the curLoc to set
     */
    public void setCurLoc(LocationType curLoc) {
        this.curLoc = curLoc;
    }

    public void updateGraphics(final double newRange, boolean accepted, String reason) {
		// setCurLoc(curLoc);
		setRange(newRange);
		setAccepted(accepted);
		setRejectionReason(reason);
		hideFadeTrigger = false;
		
		//LBLTrackerRanger.this.repaint();
		callParentRepaint();

		if (ttRange != null)
			ttRange.cancel();

		if (secondsToDisplayRanges > 0) {
		    ttRange = new TimerTask() {
		        public void run() {
		            ttRange = null;
		            if (hideOrFadeRange) {
		                hideFadeTrigger = false;
		                setRange(Double.NaN);
		            }
		            else {
		                hideFadeTrigger = true;
		            }
		            //LBLTrackerRanger.this.repaint();
		            //for (ILayerPainter str2d : renderers) {
		            //	((SubPanel) str2d).repaint();
		            //}
		            callParentRepaint();
		        }
		    };
		    timer.schedule(ttRange, secondsToDisplayRanges * 1000);
		}
	}

	public void cleanup() {
	    try {
	        timer.cancel();
	    }
	    catch (Exception e) {
	        e.printStackTrace();
        }
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D
	 * , pt.lsts.neptus.renderer2d.StateRenderer2D)
	 */
	public void paint(Graphics2D g, StateRenderer2D renderer) {
		if (curLoc != null) {

			g = (Graphics2D) g.create();
			
			// if (Double.isNaN(range) || Double.isInfinite(range))
			// return;
			float zoom = renderer.getZoom();
			// double rotAngle = renderer.getRotationAngle();
			// boolean fastRendering = renderer.isFastRendering();

			double radius;
			if (Double.isNaN(range) || Double.isInfinite(range)) {
				radius = 0;
				return;
			} else
				radius = range * zoom;

			//g.setTransform(new AffineTransform());
			Point2D pt = renderer.getScreenPosition(curLoc);
			g.setColor(squareColor /*new Color(255, 128, 0, 100)*/);
			g.fill(new Rectangle2D.Double(pt.getX() - 3, pt.getY() - 3, 6, 6));

			
			Color color; // Temporary color variable
			
            if(accepted)
                color = acceptedColor;
            else if(reason != null && reason.contains("SURFACE")) 
                color = surfaceColor;
            else
                color = rejectedColor;
            
			
            if (!hideFadeTrigger)
                g.setColor(color);
            else
                g.setColor(ColorUtils.setTransparencyToColor(color, 70));

			g.draw(new Rectangle2D.Double(pt.getX() - 3, pt.getY() - 3, 6, 6));

			Ellipse2D rC = new Ellipse2D.Double(pt.getX() - radius, pt.getY()
					- radius, radius * 2, radius * 2);
			Stroke oldStroke = g.getStroke();
			Stroke newStroke = new BasicStroke(5);
			g.setStroke(newStroke);
			g.draw(rC);
			g.setStroke(oldStroke);
			String rangeTxt = "" + MathMiscUtils.round(range, 1) + I18n.textc("m", "meters");
			Color oldColor = g.getColor();
			g.setColor(ColorUtils.setTransparencyToColor(Color.WHITE, oldColor.getAlpha()));
            g.drawString(rangeTxt, (int) (pt.getX() - 5 + 1), (int) (pt.getY() - 10 * (isDrawRangeUpOrDownThePoint() ? 1
                    : -2) + 1));
            g.setColor(oldColor);
            g.drawString(rangeTxt, (int) (pt.getX() - 5), (int) (pt.getY() - 10 * (isDrawRangeUpOrDownThePoint() ? 1
                    : -2)));
		
			if(accepted)
			    color = acceptedColor;
			else if(reason != null && reason.contains("SURFACE")) 
			    color = surfaceColor;
			else
			    color = rejectedColor;
			    
            if (!hideFadeTrigger)
                g.setColor(ColorUtils.setTransparencyToColor(color, 40)/* new Color(255, 0, 0, 40) */);
            else
                g.setColor(ColorUtils.setTransparencyToColor(color, 20));

            g.fill(rC);

			g.setColor(Color.BLACK);
		}
	}	
}
