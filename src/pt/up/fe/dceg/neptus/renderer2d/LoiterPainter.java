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
package pt.up.fe.dceg.neptus.renderer2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import pt.up.fe.dceg.neptus.mp.maneuvers.Loiter;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.util.GuiUtils;

public class LoiterPainter implements Renderer2DPainter {

	private Loiter loiterManeuver = null;
	
	public LoiterPainter() {
		setLoiterManeuver(new Loiter());
	}
	
	public LoiterPainter(Loiter loiterManeuver) {
		setLoiterManeuver(loiterManeuver);
	}
	
	public static void paint(Loiter loiterManeuver, Graphics2D g, double zoom, double rotation, boolean fastRendering) {
//		 x marks the spot...
		g.drawLine(-4, -4, 4, 4);
		g.drawLine(-4, 4, 4, -4);
		
		double bearing = -loiterManeuver.getBearing() - rotation;
		double radius = loiterManeuver.getRadius() * zoom;
		double length = loiterManeuver.getLength() * zoom;
		
		// display bearing		
		g.rotate(bearing);
		//g.draw(new Line2D.Double(0, 0, 0, -radius));		
		
		boolean isClockwise = loiterManeuver.getDirection().equalsIgnoreCase("Counter-Clockwise")? true : false;
		
		if (loiterManeuver.getLoiterType().equalsIgnoreCase("circular")) {	
			double rt = loiterManeuver.getRadiusTolerance() * zoom;
			
			
			if (!fastRendering) {
				g.setColor(new Color(255,255,255,100));
				Area outer = new Area(new Ellipse2D.Double(-radius-rt, -radius-rt, (radius+rt)*2, (radius+rt)*2));
				Area inner = new Area(new Ellipse2D.Double(-radius+rt, -radius+rt, (radius-rt)*2, (radius-rt)*2));
				
				outer.subtract(inner);
				
				g.fill(outer);
			}
			g.setColor(Color.RED);
			
			g.draw(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
			
			g.translate(0, -radius);
			if (isClockwise) {
				g.drawLine(5, 5, 0, 0);
				g.drawLine(5, -5, 0, 0);
			}
			else {
				g.drawLine(-5, 5, 0, 0);
				g.drawLine(-5, -5, 0, 0);
			}
			return;
		}
		
		if (loiterManeuver.getLoiterType().equalsIgnoreCase("racetrack")) {	
			
			double rt = loiterManeuver.getRadiusTolerance() * zoom;
			
			g.setColor(new Color(255,255,255,100));
			
			Area outer = new Area(new Rectangle2D.Double(-length/2, -radius-rt, length, (radius+rt)*2));
			outer.add(new Area(new Ellipse2D.Double(-radius-rt-length/2, -radius-rt, (radius+rt)*2, (radius+rt)*2)));
			outer.add(new Area(new Ellipse2D.Double(-radius-rt+length/2, -radius-rt, (radius+rt)*2, (radius+rt)*2)));
			
			Area inner = new Area(new Rectangle2D.Double(-length/2, -radius+rt, length, (radius-rt)*2));
			inner.add(new Area(new Ellipse2D.Double(-radius+rt-length/2, -radius+rt, (radius-rt)*2, (radius-rt)*2)));
			inner.add(new Area(new Ellipse2D.Double(-radius+rt+length/2, -radius+rt, (radius-rt)*2, (radius-rt)*2)));
			
			outer.subtract(inner);
			
			g.fill(outer);			
			g.setColor(Color.RED);
			
			Area a = new Area();
			a.add(new Area(new Ellipse2D.Double(-radius-length/2,-radius,radius*2, radius*2)));
			a.add(new Area(new Ellipse2D.Double(-radius+length/2,-radius,radius*2, radius*2)));
			a.add(new Area(new Rectangle2D.Double(-length/2, -radius, length, radius*2)));
			
			g.draw(a);
			
			g.translate(0, -radius);
			if (isClockwise) {
				g.drawLine(5, 5, 0, 0);
				g.drawLine(5, -5, 0, 0);
			}
			else {
				g.drawLine(-5, 5, 0, 0);
				g.drawLine(-5, -5, 0, 0);
			}
			return;
		}
		
		if (loiterManeuver.getLoiterType().equalsIgnoreCase("Figure 8")) {	
			
			double rt = loiterManeuver.getRadiusTolerance() * zoom;
			
			g.setColor(new Color(255,255,255,100));
			
			Area outer = new Area();
			
			outer.add(new Area(new Ellipse2D.Double(-radius-rt-length/2, -radius-rt, (radius+rt)*2, (radius+rt)*2)));
			outer.add(new Area(new Ellipse2D.Double(-radius-rt+length/2, -radius-rt, (radius+rt)*2, (radius+rt)*2)));
			
			outer.subtract(new Area(new Rectangle2D.Double(-length/2, -radius-rt, length, (radius+rt)*2)));
			
			Area inner = new Area(new Rectangle2D.Double(-length/2, -radius+rt, length, (radius-rt)*2));
			inner.add(new Area(new Ellipse2D.Double(-radius+rt-length/2, -radius+rt, (radius-rt)*2, (radius-rt)*2)));
			inner.add(new Area(new Ellipse2D.Double(-radius+rt+length/2, -radius+rt, (radius-rt)*2, (radius-rt)*2)));
			
			outer.subtract(inner);
			
			
			
			GeneralPath p = new GeneralPath();
			p.moveTo(-length/2, -radius-rt);
			p.lineTo(length/2, radius-rt);
			p.lineTo(length/2, radius+rt);
			p.lineTo(-length/2, -radius+rt);
			p.closePath();
			
			outer.add(new Area(p));
			
			p = new GeneralPath();
			p.moveTo(-length/2, radius-rt);
			p.lineTo(length/2, -radius-rt);
			p.lineTo(length/2, -radius+rt);
			p.lineTo(-length/2, radius+rt);
			p.closePath();
			
			outer.add(new Area(p));
			
			g.fill(outer);			
			
			g.setColor(Color.RED);
			
			Area a = new Area();
			a.add(new Area(new Ellipse2D.Double(-radius-length/2,-radius,radius*2, radius*2)));
			a.add(new Area(new Ellipse2D.Double(-radius+length/2,-radius,radius*2, radius*2)));
			a.subtract(new Area(new Rectangle2D.Double(-length/2, -radius, length, radius*2)));
			
			p = new GeneralPath();
			p.moveTo(-length/2-1, -radius);
			p.lineTo(length/2+1, radius);
			p.lineTo(length/2+1, -radius);
			p.lineTo(-length/2-1, radius);
			p.closePath();
			a.add(new Area(p));
			
			g.draw(a);
			
			g.translate(0, -radius);
			if (isClockwise) {
				g.drawLine(5, 5, 0, 0);
				g.drawLine(5, -5, 0, 0);
			}
			else {
				g.drawLine(-5, 5, 0, 0);
				g.drawLine(-5, -5, 0, 0);
			}
			return;
		}
		
		if (loiterManeuver.getLoiterType().equalsIgnoreCase("Hover")) {	
			g.setColor(new Color(255,255,255,100));			
			g.fill(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
			return;
		}
		
		g.setColor(new Color(255,255,255,100));			
		g.fill(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
		g.rotate(-bearing);
		g.setColor(Color.RED);
		g.drawString("?", 5, 10);
		
	}
	
	public void paint(Graphics2D g, StateRenderer2D renderer) {
		//g.setTransform(new AffineTransform());
		//Point2D pt = renderer.getScreenPosition(loiterManeuver.getLocation());
		//g.translate(pt.getX(), pt.getY());
		paint(loiterManeuver, g, renderer.getZoom(), renderer.getRotation(), renderer.isFastRendering());		
	}

	public void setLoiterManeuver(Loiter loiterManeuver) {
		this.loiterManeuver = loiterManeuver;
	}
	
	private static MapLegend legend = new MapLegend();
	
	public static Image previewLoiter(Loiter loiter, int width, int height) {
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		System.out.println("width="+width);
		Graphics2D g = (Graphics2D) bi.getGraphics();
		g.translate(width/2.0, height/2.0);		
		paint(loiter, g, 1.0, 0.0, false);
		
		g.setTransform(new AffineTransform());		
		legend.paint(g, width, height, true);
		
		return bi;
	}
	
	
	public static void main(String[] args) {
		StateRenderer2D r2d = new StateRenderer2D(MapGroup.getNewInstance(new CoordinateSystem()));
		Loiter loiter = new Loiter();
		LocationType lt = new LocationType();
		lt.setOffsetEast(150);
		loiter.getManeuverLocation().setLocation(lt);
		loiter.setLength(50);
		loiter.setLoiterType("Circular");
		loiter.setBearing(Math.toRadians(45));
		loiter.setDirection("Clockwise");
		r2d.addPostRenderPainter(new LoiterPainter(loiter), "Loiter Painter");
		
		GuiUtils.testFrame(r2d, "Testing loiter painter...");
		
	}

}
