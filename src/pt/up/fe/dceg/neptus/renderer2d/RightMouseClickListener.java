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
 * $Id:: RightMouseClickListener.java 9616 2012-12-30 23:23:22Z pdias     $:
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

public interface RightMouseClickListener {
	/**
	 * The option that will be shown in a menu to the user on right clicks.<br>
	 * If the user selects this option, the {@link #itemSelected(Renderer, Point2D, LocationType)} method is called
	 * @return
	 */
	public String getPresentationName();
	
	/**
	 * @param source The renderer that generated this event
	 * @param screenCoords The AWT coordinates of the click
	 * @param clickedPoint The real world location that has been clicked (may also be <b>null</b>!)
	 */
	public void itemSelected(Renderer source, Point2D screenCoords, LocationType realCoords);
}
