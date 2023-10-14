/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2006/06/13
 */
package pt.lsts.neptus.renderer2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
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
		loc.append(""+CoordinateUtil.latitudeAsPrettyString(latLonDepth[0]));
		loc.append(" / "+CoordinateUtil.longitudeAsPrettyString(latLonDepth[1]));

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
