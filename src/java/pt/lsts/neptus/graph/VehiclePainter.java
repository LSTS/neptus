/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;

import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ImageUtils;

public class VehiclePainter implements GraphPainter {

	private Image vehicleImage = null;
	public static int maxWidth = 200;
	public static int maxHeight = 200;
	private double scale = 1.0;
	
	private PlanType plan = null;
		
	
	public VehiclePainter(PlanType plan) {
		
		this.plan = plan;
	}
	
	
	private void createImage(ImageObserver observer) {
		try {
			this.vehicleImage = ImageUtils.getImage(plan.getVehicleType().getSideImageHref());
		}
		catch (Exception e) {
			
		}
		
		scale = Math.min(scale, maxWidth/(double)vehicleImage.getWidth(null));
		scale = Math.min(scale, maxHeight/(double)vehicleImage.getHeight(null));
		
		if (scale != 1.0) {
			vehicleImage = vehicleImage.getScaledInstance((int)(vehicleImage.getWidth(null)*scale), (int) (vehicleImage.getHeight(null)*scale), Image.SCALE_SMOOTH);
		}		
	}
	public void paint(Graphics2D g, NeptusGraph<?, ?> graph) {
		
		if (vehicleImage == null)
			createImage(graph);
		
		g.drawImage(vehicleImage, 0, 0, graph);
		
		g.setColor(new Color(255,255,255,200));
		g.fillRect(0, 0, 200, 200);
	}

	public int compareTo(GraphPainter o) {		
		return 0;
	}

}
