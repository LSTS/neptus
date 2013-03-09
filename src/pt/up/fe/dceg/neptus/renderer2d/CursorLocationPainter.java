/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zepinto
 * 2006/06/13
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
/**
 * 
 * @author ZP
 *
 */
@LayerPriority(priority=100)
public class CursorLocationPainter implements Renderer2DPainter {

	private StateRenderer2D r2d = null;
	private LocationType curLocation = null;
	JLabel lbl = new JLabel();
	boolean visible = true;
	
	public CursorLocationPainter() {
		lbl.setBackground(new Color(255,255,255,100));
		lbl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		lbl.setOpaque(true);
		lbl.setBounds(0, 0, 0, 0);
	}
	
	Point2D curLoc = null;
	
	public void paint(Graphics2D g, StateRenderer2D renderer) {
		if (this.r2d == null) {
			r2d = renderer;			
			renderer.addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseMoved(java.awt.event.MouseEvent e) {		
					//Dimension ldim = lbl.getPreferredSize(); 
					curLoc = e.getPoint();
					//curLocation = r2d.getRealWorldLocation(e.getPoint());
					//r2d.getGraphics().drawImage(r2d.cache, 10, r2d.getHeight() - ldim.height - 10, ldim.width+10, r2d.getHeight()-10, 10, r2d.getHeight() - ldim.height - 10, ldim.width+10, r2d.getHeight()-10, null);
					//paint((Graphics2D)r2d.getGraphics(), r2d);
					
				}				
			});
			
			renderer.addMouseListener(new MouseAdapter() {
				/* (non-Javadoc)
				 * @see java.awt.event.MouseAdapter#mouseExited(java.awt.event.MouseEvent)
				 */
				@Override
				public void mouseExited(MouseEvent e) {
					curLocation = null;
					r2d.repaint();
				}
			});
		}
		
        if (!isVisible())
            return;

        
		if (curLoc != null) {
			curLocation = r2d.getRealWorldLocation(curLoc);
//			if (r2d.getMapGroup().getCoordinateSystem().getLatitudeAsDoubleValue()==0)
//				return;
			lbl.setText(getPrettyLocation());					
		}
		else {
			lbl.setText("");
		}
		Dimension ldim = lbl.getPreferredSize(); 
		
		lbl.setBounds(0, 0, ldim.width, ldim.height);
		//g.setTransform(new AffineTransform());
		g.translate(10, r2d.getHeight() - ldim.height - 10);
		if (!lbl.getText().equals(""))
			lbl.paint(g);
	}
	
	 
	private String getPrettyLocation() {
		if (curLocation == null)
			return "";
		
		StringBuilder loc = new StringBuilder();
		loc.append("<html>");
		double[] latLonDepth = curLocation.getAbsoluteLatLonDepth();		
		loc.append(""+CoordinateUtil.latitudeAsString(latLonDepth[0], true));
		loc.append(" / "+CoordinateUtil.longitudeAsString(latLonDepth[1], true));

		loc.append("</html>");
		
		return loc.toString();
	}

	/**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param cursorShown
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
