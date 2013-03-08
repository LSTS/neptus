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
 * $Id:: VehiclePainter.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;

import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ImageUtils;

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
