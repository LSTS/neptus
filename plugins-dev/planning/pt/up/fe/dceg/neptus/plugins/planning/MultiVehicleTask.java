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
 * May 14, 2010
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@LayerPriority(priority=50)
public class MultiVehicleTask implements Renderer2DPainter, Comparable<MultiVehicleTask> {

	public enum TASK_TYPE {LOITER, GOTO, SCAN};
	public enum TASK_STATE {EDITING, ADDED, ATTACHED, INPROGRESS, DONE};
	
	protected TASK_TYPE type = TASK_TYPE.LOITER;
	protected TASK_STATE state = TASK_STATE.EDITING;
	protected LocationType center = null;
	protected double width, length, depth, rotation;
	
	protected long timeOfCreation;
	public MultiVehicleTask() {
		timeOfCreation = System.currentTimeMillis();
	}
	
	@Override
	public int compareTo(MultiVehicleTask o) {
		return (int) (timeOfCreation - o.timeOfCreation);
	}
	
	protected Vector<LocationType> edges = new Vector<LocationType>();
	
	public boolean containsPoint(StateRenderer2D renderer, Point2D click) {
		switch (type) {
		case LOITER:
			Point2D pt = renderer.getScreenPosition(center);
			return pt.distance(click) < width*renderer.getZoom();
		case GOTO:
			Point2D p = renderer.getScreenPosition(center);
			return p.distance(click) < 5;
		default:
			//TODO
			break;
		}
		return false;
	}
	
	@Override
	public void paint(Graphics2D g, StateRenderer2D renderer) {
		Point2D pt = renderer.getScreenPosition(center);
		g.translate(pt.getX(), pt.getY());
		double zoom = renderer.getZoom();
		
		Color opaque = Color.white;
		switch (state) {
		case ADDED:
			opaque = Color.yellow;
			break;
		case ATTACHED:
			opaque = Color.green;
			break;
		case INPROGRESS:
			opaque = Color.blue;
			break;
		case DONE:
			opaque = Color.cyan;
			break;
		default:
			opaque = Color.red;
			break;
		}
		
		Color transp = new Color(opaque.getRed(), opaque.getGreen(), opaque.getBlue(), 100);
		
		switch (type) {
		case LOITER:
			g.setColor(transp);
			Ellipse2D e = new Ellipse2D.Double(-getWidth()*zoom, -getWidth()*zoom, getWidth()*2*zoom, getWidth()*2*zoom); 
			g.fill(e);
			g.setColor(opaque);
			g.draw(e);
			g.draw(new Line2D.Double(-5, -5, 5, 5));
			g.draw(new Line2D.Double(-5, 5, 5, -5));
			break;
		case GOTO:
			g.setColor(opaque);
			g.draw(new Line2D.Double(-5, -5, 5, 5));
			g.draw(new Line2D.Double(-5, 5, 5, -5));
			break;
		case SCAN:
			g.rotate(rotation);
			//g.scale(renderer.getZoom(), renderer.getZoom());
			Rectangle2D r = new Rectangle2D.Double(-getWidth()/2*zoom, -getLength()/2*zoom, getWidth()*zoom, getLength()*zoom);
			g.setColor(transp);
			g.fill(r);
			g.setColor(opaque);
			g.draw(r);			
			break;
		default:
			break;
		}
	}

	/**
	 * @return the type
	 */
	public TASK_TYPE getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(TASK_TYPE type) {
		this.type = type;
	}
	
	/**
	 * @return the state
	 */
	public TASK_STATE getState() {
		return state;
	}
	
	/**
	 * @param state the state to set
	 */
	public void setState(TASK_STATE state) {
		this.state = state;
	}

	/**
	 * @return the center
	 */
	public LocationType getCenter() {
		return center;
	}

	/**
	 * @param center the center to set
	 */
	public void setCenter(LocationType center) {
		this.center = center;
	}

	/**
	 * @return the width
	 */
	public double getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(double width) {
		this.width = width;
	}

	/**
	 * @return the length
	 */
	public double getLength() {
		return length;
	}

	/**
	 * @param length the length to set
	 */
	public void setLength(double length) {
		this.length = length;
	}

	/**
	 * @return the depth
	 */
	public double getDepth() {
		return depth;
	}

	/**
	 * @param depth the depth to set
	 */
	public void setDepth(double depth) {
		this.depth = depth;
	}

	/**
	 * @return the rotation
	 */
	public double getRotation() {
		return rotation;
	}

	/**
	 * @param rotation the rotation to set
	 */
	public void setRotation(double rotation) {
		this.rotation = rotation;
	}	
}
