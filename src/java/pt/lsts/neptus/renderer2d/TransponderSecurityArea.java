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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.renderer2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
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
			double blDistance = lt1.getPixelDistanceTo(lt2, renderer.getLevelOfDetail());
			
			if (blDistance > 5000)
			    return;
			
			Point2D pt1 = renderer.getScreenPosition(lt1);
			pt1.setLocation(pt1.getX(),pt1.getY());
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] {5,5}, 0));			
			Rectangle2D.Double rect = new Rectangle2D.Double(0,-blDistance ,blDistance*0.75, blDistance);
			g.translate(pt1.getX(), pt1.getY());
			g.rotate(angle);
			g.translate(blDistance*0.25, 0);
			g.draw(rect);
			g.translate(-blDistance*1.25, 0);
			g.draw(rect);
			g.setStroke(new BasicStroke());
		}
	}
}
