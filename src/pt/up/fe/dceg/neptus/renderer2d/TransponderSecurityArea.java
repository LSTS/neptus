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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
/**
 * 
 * @author ZP
 *
 */
@LayerPriority(priority=-10)
public class TransponderSecurityArea implements Renderer2DPainter {

    
    
    
    
	public void paint(Graphics2D g, StateRenderer2D renderer) {
		g.setColor(Color.WHITE);
		MapGroup mg = renderer.getMapGroup();
		AbstractElement[] objs = mg.getAllObjects();
		
		TransponderElement trans1 = null, trans2 = null;
		
		int i = 0;
		for (; i < objs.length; i++) {
			if (objs[i] instanceof TransponderElement) {
				trans1 = (TransponderElement) objs[i];
				break;
			}
		}
		i++;
		for (; i < objs.length; i++) {
			if (objs[i] instanceof TransponderElement) {
				trans2 = (TransponderElement) objs[i];
				break;
			}
		}
		
		if (trans1 != null && trans2 != null) {
			LocationType lt1 = trans1.getCenterLocation();
			LocationType lt2 = trans2.getCenterLocation();
			
			double angle = lt1.getXYAngle(lt2) - renderer.getRotation();
			double blDistance = lt1.getDistanceInMeters(lt2);
			
			Point2D pt1 = renderer.getScreenPosition(lt1);
			pt1.setLocation(pt1.getX(),pt1.getY());
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] {5,5}, 0));			
			Rectangle2D.Double rect = new Rectangle2D.Double(0,-blDistance *renderer.getZoom(),blDistance*0.75*renderer.getZoom(), blDistance*renderer.getZoom());
			g.translate(pt1.getX(), pt1.getY());
			g.rotate(angle);
			g.translate(blDistance*0.25*renderer.getZoom(), 0);
			g.draw(rect);
			g.translate(-blDistance*1.25*renderer.getZoom(), 0);
			g.draw(rect);
			g.setStroke(new BasicStroke());
		}
	}
}
