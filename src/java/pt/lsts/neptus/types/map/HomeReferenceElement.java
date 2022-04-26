/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 21, 2005
 */
package pt.lsts.neptus.types.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import pt.lsts.neptus.gui.CoordinateSystemPanel;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zecarlos
 */
public class HomeReferenceElement extends AbstractElement {

	private CoordinateSystem coordinateSystem = new CoordinateSystem();
	private CoordinateSystemPanel params = null;
	
	public HomeReferenceElement(MapGroup mg, MapType map) {
		super(mg, map);
		
		if (mg != null && mg.getCoordinateSystem() != null)
			setCoordinateSystem(mg.getCoordinateSystem());
		
		setYawDeg(getCoordinateSystem().getYaw());
		setPitchDeg(getCoordinateSystem().getPitch());
		setRollDeg(getCoordinateSystem().getRoll());
	}
	
	public HomeReferenceElement() {
	    super();
	}
	
	public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {

        Point2D tt = renderer.getScreenPosition(getCenterLocation());
        g.translate(tt.getX(), tt.getY());
        g.rotate(getYawRad()-renderer.getRotation());
        
		if (!isSelected())
			g.setColor(Color.RED);
		else
			g.setColor(Color.WHITE);
		
		g.drawOval(-5,-5,10,10);
		
		if (!isSelected())
			g.setColor(Color.WHITE);
		else
			g.setColor(Color.RED);
		
		g.drawLine(-8, 0, 8, 0);
		g.drawLine(0, -8, 0, 8);
				
	}


	public ParametersPanel getParametersPanel(boolean editable, MapType map) {
		params = new CoordinateSystemPanel(getCoordinateSystem());
		params.setEditable(false);
		return params;
	}

	public void initialize(ParametersPanel paramsPanel) {
		//Since the parameters panel is not editable... 
		// no changes could have been performed
	}

	public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {

        double distance = getCenterLocation().getHorizontalDistanceInMeters(lt);
        
        if (renderer == null)
            return distance < 5;
        else
            return (distance * renderer.getZoom()) < 10;
	}

	public LocationType getCenterLocation() {
		LocationType lt = new LocationType();
		lt.setLocation(getCoordinateSystem());
		return lt;
	}

	public void setCenterLocation(LocationType l) {
	    centerLocation = l;
		// System.err.println("Use setCoordinateSystem() instead"); ????
	}

	public CoordinateSystem getCoordinateSystem() {
		return coordinateSystem;
	}
	
	public void setCoordinateSystem(CoordinateSystem coordinateSystem) {
		this.coordinateSystem.setCoordinateSystem(coordinateSystem);
	}
	
	@Override
	public double getYawRad() {
		return Math.toRadians(coordinateSystem.getYaw());
	}
	
	@Override
	public double getYawDeg() {
		return coordinateSystem.getYaw();
	}
	
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.mme.objects.MapObject#getLayerPriority()
	 */
	public int getLayerPriority() {
		return 8;
	}
	
	@Override
	public String getType() {
		return "Home Reference";
	}
	
	@Override
	public String getTypeAbbrev() {
	    return "href";
	}
	
	@Override
	public ELEMENT_TYPE getElementType() {
	    return ELEMENT_TYPE.TYPE_HOMEREFERENCE;
	}
	
}
